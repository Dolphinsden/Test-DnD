package com.dndcoop.controller;

import com.dndcoop.campaign.AshenLanternCampaign;
import com.dndcoop.campaign.EncounterTemplate;
import com.dndcoop.campaign.LocationNode;
import com.dndcoop.dm.DmDirective;
import com.dndcoop.dm.DmSettings;
import com.dndcoop.dm.HybridDungeonMaster;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterClass;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.Condition;
import com.dndcoop.model.InitiativeEntry;
import com.dndcoop.model.Item;
import com.dndcoop.model.LogEntry;
import com.dndcoop.model.Monster;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.model.RollResult;
import com.dndcoop.model.Spell;
import com.dndcoop.persistence.SaveManager;
import com.dndcoop.rules.CombatEngine;
import com.dndcoop.rules.DiceRoller;
import com.dndcoop.rules.LevelingSystem;
import com.dndcoop.rules.RulesEngine;
import com.dndcoop.rules.Spellbook;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class GameSessionController {

    private final RulesEngine rulesEngine;
    private final CombatEngine combatEngine;
    private final HybridDungeonMaster hybridDm;
    private final SaveManager saveManager;
    private final ExecutorService dmExecutor;
    private final Consumer<Runnable> uiDispatcher;

    private CampaignState state;
    private DmSettings settings;
    private volatile boolean dmThinking = false;

    public GameSessionController(SaveManager saveManager, DmSettings settings, Consumer<Runnable> uiDispatcher) {
        this(new RulesEngine(new DiceRoller()), new HybridDungeonMaster(), saveManager, settings, uiDispatcher, true);
    }

    public GameSessionController(RulesEngine rulesEngine, HybridDungeonMaster hybridDm,
                                 SaveManager saveManager, DmSettings settings,
                                 Consumer<Runnable> uiDispatcher, boolean asyncDm) {
        this.rulesEngine = rulesEngine;
        this.combatEngine = new CombatEngine(rulesEngine);
        this.hybridDm = hybridDm;
        this.saveManager = saveManager;
        this.settings = settings != null ? settings : new DmSettings();
        this.uiDispatcher = uiDispatcher != null ? uiDispatcher : Runnable::run;
        this.dmExecutor = asyncDm
                ? Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "dm-worker");
                    t.setDaemon(true);
                    return t;
                })
                : null;
    }

    public CampaignState startNewCampaign(List<CharacterSheet> selectedParty, String saveName) {
        CampaignState newState = new CampaignState();
        newState.setCampaignTitle(AshenLanternCampaign.CAMPAIGN_TITLE);
        newState.setSaveName((saveName == null || saveName.isBlank()) ? "Hollowbrook Expedition" : saveName.trim());
        newState.setCurrentAct(1);
        newState.setCurrentLocationId(AshenLanternCampaign.START_LOCATION_ID);
        newState.getVisitedLocations().add(AshenLanternCampaign.START_LOCATION_ID);
        newState.setParty(selectedParty);
        if (!selectedParty.isEmpty()) {
            newState.setActiveCharacterId(selectedParty.get(0).getId());
        }
        newState.addLog(LogEntry.dm("Dungeon Master", AshenLanternCampaign.buildOpeningNarration(newState)));
        this.state = newState;
        autoSaveQuietly();
        return newState;
    }

    public void loadCampaignState(CampaignState loaded) {
        this.state = loaded;
        if (state != null && state.getActiveCharacterId() == null && !state.getParty().isEmpty()) {
            state.setActiveCharacterId(state.getParty().get(0).getId());
        }
    }

    public void submitFreeText(String rawText, Runnable onUpdate) {
        if (state == null || rawText == null || rawText.isBlank()) {
            return;
        }
        if (dmThinking) {
            return;
        }
        String text = rawText.trim();
        if (state.getPendingRoll() != null) {
            state.addLog(LogEntry.system("Finish the pending dice roll (" + state.getPendingRoll().getLabel() + ") before taking another action."));
            notifyUi(onUpdate);
            return;
        }

        state.setTurnCount(state.getTurnCount() + 1);

        if (state.getCombat().isActive()) {
            handleCombatFreeText(text, onUpdate);
            return;
        }

        // Out of combat: purely narrative free-form prompt from the selected speaker
        CharacterSheet actor = state.getActiveCharacter();
        if (actor == null) {
            return;
        }
        state.addLog(LogEntry.player(actor.getName(), text));

        // Check quick direct healing potion or known spell out of combat
        String lower = text.toLowerCase();
        if (lower.contains("potion") && (lower.contains("drink") || lower.contains("use") || lower.contains("heal"))) {
            CharacterSheet target = findMentionedHero(lower, actor);
            rulesEngine.useHealingPotion(state, actor, target);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        Spell mentionedSpell = Spellbook.findByText(lower, actor.getKnownSpellIds());
        if (mentionedSpell != null && (mentionedSpell.getCastMode() == Spell.CastMode.HEAL || mentionedSpell.getCastMode() == Spell.CastMode.BUFF)) {
            CharacterSheet target = findMentionedHero(lower, actor);
            combatEngine.castSpellInCombatOrExploration(state, actor, mentionedSpell, null, target, text);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }

        // Dispatch to Hybrid Local DM (Ollama/OpenAI-compatible -> Offline Fallback)
        Runnable dmTask = () -> {
            dmThinking = true;
            try {
                DmDirective directive = hybridDm.resolveExplorationTurn(settings, state, actor, text, rulesEngine);
                uiDispatcher.accept(() -> {
                    dmThinking = false;
                    applyExplorationDirective(directive, actor);
                    autoSaveQuietly();
                    if (onUpdate != null) onUpdate.run();
                });
            } catch (Exception e) {
                uiDispatcher.accept(() -> {
                    dmThinking = false;
                    state.addLog(LogEntry.system("DM encountered an error: " + e.getMessage()));
                    if (onUpdate != null) onUpdate.run();
                });
            }
        };

        if (dmExecutor != null) {
            dmThinking = true;
            notifyUi(onUpdate);
            dmExecutor.submit(dmTask);
        } else {
            dmTask.run();
        }
    }

    private void applyExplorationDirective(DmDirective directive, CharacterSheet actor) {
        if (directive.getNarrative() != null && !directive.getNarrative().isBlank()) {
            state.addLog(LogEntry.dm(directive.getDmSourceLabel(), directive.getNarrative()));
        }
        if (directive.getSetFlag() != null && !directive.getSetFlag().isBlank()) {
            state.setFlag(directive.getSetFlag(), true);
        }
        if (directive.getGrantGold() > 0) {
            actor.setGold(actor.getGold() + directive.getGrantGold());
            state.addLog(LogEntry.system(actor.getName() + " gained " + directive.getGrantGold() + " gp."));
        }
        if (directive.getGrantItemId() != null && !directive.getGrantItemId().isBlank()) {
            Item item = AshenLanternCampaign.createRewardItem(directive.getGrantItemId());
            if (item != null) {
                actor.addItem(item);
                state.addLog(LogEntry.system(actor.getName() + " obtained " + item.getName() + "!"));
            }
        }
        if (directive.getGrantXp() > 0) {
            LevelingSystem.awardPartyXp(state, directive.getGrantXp());
        }
        if ("short".equals(directive.getRestType())) {
            rulesEngine.applyShortRest(state);
        } else if ("long".equals(directive.getRestType())) {
            rulesEngine.applyLongRest(state);
        }
        if (directive.getMoveToLocationId() != null && !directive.getMoveToLocationId().isBlank()) {
            travelToLocationInternal(directive.getMoveToLocationId());
        }
        if (directive.getStartEncounterId() != null && !directive.getStartEncounterId().isBlank()) {
            EncounterTemplate enc = AshenLanternCampaign.getEncounter(directive.getStartEncounterId());
            if (enc != null && !state.getDefeatedEncounters().contains(enc.getId())) {
                combatEngine.startEncounter(state, enc, settings.isAutoRollDice());
            }
        }
        if (directive.getRequestedRoll() != null && !state.getCombat().isActive()) {
            state.setPendingRoll(directive.getRequestedRoll());
            if (settings.isAutoRollDice()) {
                resolvePendingRollInternal(null);
            }
        }
    }

    private void handleCombatFreeText(String text, Runnable onUpdate) {
        InitiativeEntry turn = state.getCombat().getCurrentTurnEntry();
        if (turn == null || !turn.isPlayer()) {
            state.addLog(LogEntry.system("Waiting for initiative order..."));
            notifyUi(onUpdate);
            return;
        }
        CharacterSheet actor = state.findHeroById(turn.getParticipantId());
        if (actor == null) {
            notifyUi(onUpdate);
            return;
        }
        state.setActiveCharacterId(actor.getId());
        state.addLog(LogEntry.player(actor.getName(), text));

        String lower = text.toLowerCase();

        // End turn
        if (lower.equals("end turn") || lower.equals("pass") || lower.equals("done") || lower.contains("end my turn")) {
            combatEngine.endCurrentHeroTurn(state);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }

        // Standard 5E combat actions
        if (lower.contains("dodge") || lower.contains("defend")) {
            combatEngine.executeStandardCombatAction(state, actor, "dodge", null, null);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        if (lower.contains("disengage") || lower.contains("retreat back") || lower.contains("step away safely")) {
            combatEngine.executeStandardCombatAction(state, actor, "disengage", null, null);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        if (lower.contains("dash") || lower.contains("sprint")) {
            combatEngine.executeStandardCombatAction(state, actor, "dash", null, null);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        if (lower.startsWith("help") || lower.contains("help ") || lower.contains("assist ") || lower.contains("distract")) {
            CharacterSheet ally = findMentionedHero(lower, null);
            if (ally != null && ally.getId().equals(actor.getId())) ally = null;
            combatEngine.executeStandardCombatAction(state, actor, "help", null, ally);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        if (lower.contains("hide") || lower.contains("stealth") || lower.contains("duck into shadow")) {
            combatEngine.executeStandardCombatAction(state, actor, "hide", null, null);
            if (settings.isAutoRollDice() && state.getPendingRoll() != null) {
                resolvePendingRollInternal(null);
            }
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        if (lower.contains("second wind")) {
            String res = combatEngine.executeStandardCombatAction(state, actor, "second_wind", null, null);
            if (!"Second Wind".equals(res)) {
                state.addLog(LogEntry.system(res));
            }
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        if (lower.contains("lay on hands")) {
            CharacterSheet ally = findMentionedHero(lower, actor);
            String res = combatEngine.executeStandardCombatAction(state, actor, "lay_on_hands", null, ally);
            if (!"Lay on Hands".equals(res)) {
                state.addLog(LogEntry.system(res));
            }
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }

        // Healing Potion
        if (lower.contains("potion")) {
            CharacterSheet target = findMentionedHero(lower, actor);
            rulesEngine.useHealingPotion(state, actor, target);
            turn.setActionUsed(true);
            combatEngine.endCurrentHeroTurn(state);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }

        // Spellcasting
        Spell spell = Spellbook.findByText(lower, actor.getKnownSpellIds());
        if (spell == null && (lower.contains("cast ") || lower.contains("spell")) && !actor.getKnownSpellIds().isEmpty()) {
            // Default to first offensive spell if they just wrote "cast a spell"
            spell = Spellbook.get(actor.getKnownSpellIds().get(0));
        }
        if (spell != null) {
            Monster foe = combatEngine.findTargetEnemy(state, lower);
            CharacterSheet ally = findMentionedHero(lower, null);
            combatEngine.castSpellInCombatOrExploration(state, actor, spell, foe, ally, text);
            if (settings.isAutoRollDice() && state.getPendingRoll() != null) {
                resolvePendingRollInternal(null);
            }
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }

        // Default combat free-form action -> Weapon Attack against mentioned or first living enemy!
        Monster foe = combatEngine.findTargetEnemy(state, lower);
        if (foe == null) {
            combatEngine.checkCombatEnd(state);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        turn.setActionUsed(true);
        boolean hasAlly = combatEngine.partyHasOtherConsciousHero(state, actor);
        PendingRoll atkRoll = rulesEngine.buildWeaponAttackRoll(actor, foe, text, hasAlly);
        if (actor.getCharacterClass() == CharacterClass.PALADIN && lower.contains("smite") && actor.hasSpellSlot(1)) {
            atkRoll.setDivineSmite(true);
        }
        state.setPendingRoll(atkRoll);
        if (settings.isAutoRollDice()) {
            resolvePendingRollInternal(null);
        }
        autoSaveQuietly();
        notifyUi(onUpdate);
    }

    public void resolvePendingRoll(Integer physicalDieInput, Runnable onUpdate) {
        if (state == null || state.getPendingRoll() == null) {
            return;
        }
        resolvePendingRollInternal(physicalDieInput);
        autoSaveQuietly();
        notifyUi(onUpdate);
    }

    private void resolvePendingRollInternal(Integer physicalDieInput) {
        PendingRoll pending = state.getPendingRoll();
        if (pending == null) return;
        CharacterSheet actor = state.findHeroById(pending.getActorId());
        RollResult result = rulesEngine.executePendingRoll(pending, actor, physicalDieInput);
        state.setPendingRoll(null);

        switch (pending.getRollType()) {
            case INITIATIVE:
                if (actor != null) {
                    combatEngine.resolveHeroInitiative(state, actor, result);
                    if (settings.isAutoRollDice() && state.getPendingRoll() != null) {
                        resolvePendingRollInternal(null);
                    }
                }
                break;

            case DEATH_SAVE:
                if (actor != null) {
                    String msg = rulesEngine.applyDeathSaveResult(actor, result);
                    state.addLog(LogEntry.dice(actor.getName(), msg, result.getFormattedFormula()));
                    combatEngine.endCurrentHeroTurn(state);
                }
                break;

            case ABILITY_CHECK:
            case SAVING_THROW:
                if (actor != null) {
                    actor.getConditions().remove(Condition.HELPED);
                }
                String outcomeHeader = pending.getLabel() + " -> " + (result.isSucceeded() ? "SUCCESS!" : "FAILED");
                state.addLog(LogEntry.dice(pending.getActorName(), outcomeHeader, result.getFormattedFormula()));
                if (result.isSucceeded()) {
                    if (pending.getSuccessNarrative() != null && !pending.getSuccessNarrative().isBlank()) {
                        state.addLog(LogEntry.dm("Dungeon Master", pending.getSuccessNarrative()));
                    }
                    if (pending.getFlagOnSuccess() != null && !pending.getFlagOnSuccess().isBlank()) {
                        state.setFlag(pending.getFlagOnSuccess(), true);
                    }
                    if (pending.getGoldOnSuccess() > 0 && actor != null) {
                        actor.setGold(actor.getGold() + pending.getGoldOnSuccess());
                        state.addLog(LogEntry.system(actor.getName() + " found " + pending.getGoldOnSuccess() + " gp!"));
                    }
                    if (pending.getRewardItemOnSuccess() != null && actor != null) {
                        Item item = AshenLanternCampaign.createRewardItem(pending.getRewardItemOnSuccess());
                        if (item != null) {
                            actor.addItem(item);
                            state.addLog(LogEntry.system(actor.getName() + " obtained " + item.getName() + "!"));
                        }
                    }
                    if (pending.getXpOnSuccess() > 0) {
                        LevelingSystem.awardPartyXp(state, pending.getXpOnSuccess());
                    }
                    if (state.getCombat().isActive() && "Hide in combat".equals(pending.getContextActionText()) && actor != null) {
                        actor.getConditions().add(Condition.HIDDEN);
                        combatEngine.endCurrentHeroTurn(state);
                    }
                } else {
                    if (pending.getFailureNarrative() != null && !pending.getFailureNarrative().isBlank()) {
                        state.addLog(LogEntry.dm("Dungeon Master", pending.getFailureNarrative()));
                    }
                    if (state.getCombat().isActive() && "Hide in combat".equals(pending.getContextActionText())) {
                        combatEngine.endCurrentHeroTurn(state);
                    }
                }
                break;

            case ATTACK_ROLL:
            case SPELL_ATTACK: {
                if (actor != null) {
                    actor.getConditions().remove(Condition.HIDDEN);
                    actor.getConditions().remove(Condition.HELPED);
                }
                Monster target = state.getCombat().findEnemyById(pending.getTargetId());
                if (result.isSucceeded() && target != null && target.isAlive()) {
                    boolean crit = result.isNatural20();
                    String hitLabel = (crit ? "CRITICAL HIT! " : "HIT! ") + pending.getLabel();
                    state.addLog(LogEntry.dice(pending.getActorName(), hitLabel, result.getFormattedFormula()));

                    // Build follow-up damage roll
                    String dmgDice;
                    int dmgMod;
                    String dmgType;
                    String sourceName;
                    if (pending.getRollType() == PendingRoll.RollType.SPELL_ATTACK) {
                        Spell spell = Spellbook.get(pending.getSpellId());
                        dmgDice = spell != null ? spell.getDiceNotation() : "1d8";
                        dmgMod = 0;
                        dmgType = spell != null ? spell.getDamageType() : "arcane";
                        sourceName = spell != null ? spell.getName() : "Spell";
                        if (spell != null && "guiding_bolt".equals(spell.getId())) {
                            // Next ally gets advantage -> mark target as illuminated via HELPED on next hero
                            for (CharacterSheet ally : state.getParty()) {
                                if (actor != null && !ally.getId().equals(actor.getId()) && ally.isConscious()) {
                                    ally.getConditions().add(Condition.HELPED);
                                    break;
                                }
                            }
                        }
                    } else {
                        Item weapon = actor != null ? actor.getEquippedWeapon() : null;
                        dmgDice = weapon != null ? weapon.getDamageDice() : "1d4";
                        dmgMod = actor != null ? actor.getWeaponDamageModifier(weapon) : 0;
                        dmgType = weapon != null ? weapon.getDamageType() : "bludgeoning";
                        sourceName = weapon != null ? weapon.getName() : "Attack";
                    }

                    PendingRoll dmgRoll = rulesEngine.buildDamageRoll(actor, target, dmgDice, dmgMod,
                            dmgType, crit, pending.isSneakAttackEligible(), sourceName);
                    dmgRoll.setDivineSmite(pending.isDivineSmite());
                    state.setPendingRoll(dmgRoll);

                    if (settings.isAutoRollDice()) {
                        resolvePendingRollInternal(null);
                    }
                } else {
                    String missLabel = (result.isNatural1() ? "CRITICAL MISS! " : "MISSED! ") + pending.getLabel();
                    state.addLog(LogEntry.dice(pending.getActorName(), missLabel, result.getFormattedFormula()));
                    combatEngine.endCurrentHeroTurn(state);
                }
                break;
            }

            case DAMAGE_ROLL: {
                Monster target = state.getCombat().findEnemyById(pending.getTargetId());
                if (target != null) {
                    int totalDmg = result.getTotal();
                    StringBuilder extraNotes = new StringBuilder();
                    if (pending.isSneakAttackEligible() && actor != null) {
                        int sneakDice = Math.max(1, (actor.getLevel() + 1) / 2);
                        RollResult sneak = rulesEngine.getDiceRoller().rollDiceNotation(sneakDice + "d6", 0, pending.isCriticalHit());
                        totalDmg += sneak.getTotal();
                        extraNotes.append(" + Sneak Attack ").append(sneak.getTotal());
                    }
                    if (pending.isDivineSmite() && actor != null && actor.consumeSpellSlot(1)) {
                        RollResult smite = rulesEngine.getDiceRoller().rollDiceNotation("2d8", 0, pending.isCriticalHit());
                        totalDmg += smite.getTotal();
                        extraNotes.append(" + Divine Smite ").append(smite.getTotal()).append(" radiant");
                    }
                    target.applyDamage(totalDmg);
                    String dmgText = pending.getActorName() + " deals " + totalDmg + " "
                            + (pending.getDamageType() != null ? pending.getDamageType() : "")
                            + " damage to " + target.getName() + extraNotes + "! "
                            + (target.isAlive()
                                    ? "(" + target.getName() + " is " + target.getHealthStatusLabel() + ")"
                                    : target.getName() + " is SLAIN!");
                    state.addLog(LogEntry.dice(pending.getActorName(), dmgText, result.getFormattedFormula()));
                }
                if (!combatEngine.checkCombatEnd(state)) {
                    combatEngine.endCurrentHeroTurn(state);
                }
                break;
            }

            default:
                break;
        }
    }

    public void travelToLocation(String targetLocationId, Runnable onUpdate) {
        if (state == null || dmThinking) return;
        if (state.getCombat().isActive()) {
            state.addLog(LogEntry.system("You cannot travel away while locked in combat!"));
            notifyUi(onUpdate);
            return;
        }
        if (state.getPendingRoll() != null) {
            state.addLog(LogEntry.system("Resolve the pending dice roll before traveling."));
            notifyUi(onUpdate);
            return;
        }
        LocationNode current = AshenLanternCampaign.getLocation(state.getCurrentLocationId());
        String reqFlag = current.getLockedExitRequiredFlags().get(targetLocationId);
        if (reqFlag != null && !state.hasFlag(reqFlag)) {
            if ("has_sun_key".equals(reqFlag) && partyHasItem("bronze_sun_key")) {
                state.setFlag("has_sun_key", true);
            } else {
                state.addLog(LogEntry.dm("Dungeon Master",
                        "That path is sealed! You need to unlock it first ("
                                + ("has_sun_key".equals(reqFlag) ? "find the Bronze Sun-Key in the Ossuary Cloister" : "complete the objective here")
                                + ")."));
                notifyUi(onUpdate);
                return;
            }
        }
        travelToLocationInternal(targetLocationId);
        autoSaveQuietly();
        notifyUi(onUpdate);
    }

    private void travelToLocationInternal(String targetLocationId) {
        LocationNode dest = AshenLanternCampaign.getLocation(targetLocationId);
        state.setCurrentLocationId(dest.getId());
        state.getVisitedLocations().add(dest.getId());
        if (dest.getAct() > state.getCurrentAct()) {
            state.setCurrentAct(dest.getAct());
            state.addLog(LogEntry.system("=== ENTERING ACT " + dest.getAct() + " ==="));
        }
        state.addLog(LogEntry.dm("Location — " + dest.getName(),
                dest.getDescription() + "\n\n" + dest.getAmbientHint()));

        if (dest.getEncounterId() != null && !state.getDefeatedEncounters().contains(dest.getEncounterId())) {
            EncounterTemplate enc = AshenLanternCampaign.getEncounter(dest.getEncounterId());
            if (enc != null) {
                combatEngine.startEncounter(state, enc, settings.isAutoRollDice());
            }
        }
    }

    public void performQuickCombatAction(String actionKey, Runnable onUpdate) {
        if (state == null || !state.getCombat().isActive() || state.getPendingRoll() != null) return;
        InitiativeEntry turn = state.getCombat().getCurrentTurnEntry();
        if (turn == null || !turn.isPlayer()) return;
        CharacterSheet hero = state.findHeroById(turn.getParticipantId());
        if (hero == null) return;

        if ("attack".equals(actionKey)) {
            submitFreeText("I attack with my " + hero.getEquippedWeapon().getName(), onUpdate);
            return;
        }
        if ("end_turn".equals(actionKey)) {
            combatEngine.endCurrentHeroTurn(state);
            autoSaveQuietly();
            notifyUi(onUpdate);
            return;
        }
        String res = combatEngine.executeStandardCombatAction(state, hero, actionKey, null, null);
        if (res != null && !res.equalsIgnoreCase(actionKey) && !res.equals("Second Wind") && !res.equals("Lay on Hands")) {
            state.addLog(LogEntry.system(res));
        }
        if (settings.isAutoRollDice() && state.getPendingRoll() != null) {
            resolvePendingRollInternal(null);
        }
        autoSaveQuietly();
        notifyUi(onUpdate);
    }

    public void castSpellDirectly(CharacterSheet caster, String spellId, Runnable onUpdate) {
        if (state == null || caster == null || state.getPendingRoll() != null) return;
        Spell spell = Spellbook.get(spellId);
        if (spell == null) return;
        if (state.getCombat().isActive()) {
            InitiativeEntry turn = state.getCombat().getCurrentTurnEntry();
            if (turn == null || !caster.getId().equals(turn.getParticipantId())) {
                state.addLog(LogEntry.system("Wait for " + caster.getName() + "'s turn in Initiative order!"));
                notifyUi(onUpdate);
                return;
            }
        }
        Monster foe = state.getCombat().isActive() ? combatEngine.findTargetEnemy(state, "") : null;
        combatEngine.castSpellInCombatOrExploration(state, caster, spell, foe, null, "Cast " + spell.getName());
        if (settings.isAutoRollDice() && state.getPendingRoll() != null) {
            resolvePendingRollInternal(null);
        }
        autoSaveQuietly();
        notifyUi(onUpdate);
    }

    public void useOrEquipItem(CharacterSheet hero, String itemId, Runnable onUpdate) {
        if (state == null || hero == null) return;
        Item item = hero.findItemById(itemId);
        if (item == null) return;
        switch (item.getType()) {
            case WEAPON:
                hero.setEquippedWeaponId(item.getId());
                state.addLog(LogEntry.system(hero.getName() + " equipped " + item.getName() + "."));
                break;
            case ARMOR:
                hero.setEquippedArmorId(item.getId());
                state.addLog(LogEntry.system(hero.getName() + " donned " + item.getName() + " (AC " + hero.getArmorClass() + ")."));
                break;
            case SHIELD:
                hero.setEquippedShieldId(item.getId().equals(hero.getEquippedShieldId()) ? null : item.getId());
                state.addLog(LogEntry.system(hero.getName() + " adjusted shield (AC " + hero.getArmorClass() + ")."));
                break;
            case CONSUMABLE:
                if (state.getCombat().isActive()) {
                    InitiativeEntry turn = state.getCombat().getCurrentTurnEntry();
                    if (turn == null || !hero.getId().equals(turn.getParticipantId())) {
                        state.addLog(LogEntry.system("Wait for " + hero.getName() + "'s turn in Initiative order to use an item!"));
                        notifyUi(onUpdate);
                        return;
                    }
                    rulesEngine.useHealingPotion(state, hero, hero);
                    turn.setActionUsed(true);
                    combatEngine.endCurrentHeroTurn(state);
                } else {
                    rulesEngine.useHealingPotion(state, hero, hero);
                }
                break;
            default:
                state.addLog(LogEntry.system(item.getName() + ": " + item.getDescription()));
                break;
        }
        autoSaveQuietly();
        notifyUi(onUpdate);
    }

    public void performShortRest(Runnable onUpdate) {
        if (state == null) return;
        rulesEngine.applyShortRest(state);
        autoSaveQuietly();
        notifyUi(onUpdate);
    }

    public void performLongRest(Runnable onUpdate) {
        if (state == null) return;
        LocationNode loc = AshenLanternCampaign.getLocation(state.getCurrentLocationId());
        if (!loc.isSafeForLongRest() && loc.getEncounterId() != null && !state.getDefeatedEncounters().contains(loc.getEncounterId())) {
            state.addLog(LogEntry.system("It is not safe to take a Long Rest here while foes are nearby."));
        } else {
            rulesEngine.applyLongRest(state);
        }
        autoSaveQuietly();
        notifyUi(onUpdate);
    }

    public void selectActiveHero(String heroId, Runnable onUpdate) {
        if (state == null || heroId == null) return;
        if (state.getCombat().isActive()) {
            // In combat, active turn is strictly enforced by Initiative; selecting only inspects sheet
            notifyUi(onUpdate);
            return;
        }
        state.setActiveCharacterId(heroId);
        notifyUi(onUpdate);
    }

    private boolean partyHasItem(String itemId) {
        for (CharacterSheet h : state.getParty()) {
            if (h.findItemById(itemId) != null) return true;
        }
        return false;
    }

    private CharacterSheet findMentionedHero(String lower, CharacterSheet fallback) {
        for (CharacterSheet h : state.getParty()) {
            String first = h.getName().toLowerCase().split("\\s+")[0];
            if (lower.contains(first)) {
                return h;
            }
        }
        return fallback;
    }

    public void autoSaveQuietly() {
        if (state != null && saveManager != null) {
            try {
                saveManager.saveAuto(state);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyUi(Runnable onUpdate) {
        if (onUpdate != null) {
            uiDispatcher.accept(onUpdate);
        }
    }

    public void shutdown() {
        if (dmExecutor != null) {
            dmExecutor.shutdownNow();
        }
    }

    public CampaignState getState() { return state; }
    public DmSettings getSettings() { return settings; }
    public void setSettings(DmSettings settings) { this.settings = settings; }
    public HybridDungeonMaster getHybridDm() { return hybridDm; }
    public RulesEngine getRulesEngine() { return rulesEngine; }
    public CombatEngine getCombatEngine() { return combatEngine; }
    public SaveManager getSaveManager() { return saveManager; }
    public boolean isDmThinking() { return dmThinking; }
}
