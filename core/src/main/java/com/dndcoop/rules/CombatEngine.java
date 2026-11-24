package com.dndcoop.rules;

import com.dndcoop.campaign.EncounterTemplate;
import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterClass;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.CombatState;
import com.dndcoop.model.Condition;
import com.dndcoop.model.InitiativeEntry;
import com.dndcoop.model.Item;
import com.dndcoop.model.LogEntry;
import com.dndcoop.model.Monster;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.model.RollResult;
import com.dndcoop.model.Spell;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CombatEngine {

    private final RulesEngine rules;

    public CombatEngine(RulesEngine rules) {
        this.rules = rules;
    }

    public void startEncounter(CampaignState state, EncounterTemplate template, boolean autoRollInitiative) {
        CombatState combat = new CombatState();
        combat.setActive(true);
        combat.setEncounterId(template.getId());
        combat.setEncounterName(template.getName());
        combat.setRoundNumber(1);
        combat.setVictoryFlag(template.getVictoryFlag());
        combat.setBonusGoldReward(template.getGoldReward());
        combat.setRewardItemId(template.getRewardItemId());

        List<Monster> spawned = template.spawnMonsters();
        combat.setEnemies(spawned);

        // Roll enemy initiatives immediately
        for (Monster enemy : spawned) {
            RollResult init = rules.getDiceRoller().rollD20(enemy.getInitiativeModifier(), false, false, null, false, false);
            combat.getTurnOrder().add(new InitiativeEntry(enemy.getId(), enemy.getName(), false, init.getTotal(), enemy.getInitiativeModifier()));
        }

        // Queue living party members for initiative rolls
        List<String> pendingHeroes = new ArrayList<>();
        for (CharacterSheet hero : state.getParty()) {
            if (!hero.isDead()) {
                pendingHeroes.add(hero.getId());
            }
        }
        combat.setPendingInitiativeHeroIds(pendingHeroes);
        state.setCombat(combat);

        StringBuilder enemyNames = new StringBuilder();
        for (int i = 0; i < spawned.size(); i++) {
            if (i > 0) enemyNames.append(", ");
            enemyNames.append(spawned.get(i).getName()).append(" (AC ").append(spawned.get(i).getArmorClass()).append(")");
        }
        state.addLog(LogEntry.combat("COMBAT INITIATED — " + template.getName(),
                template.getIntroNarrative() + "\nFoes: " + enemyNames + "\nRoll for Initiative!"));

        if (autoRollInitiative) {
            while (!combat.getPendingInitiativeHeroIds().isEmpty()) {
                String heroId = combat.getPendingInitiativeHeroIds().remove(0);
                CharacterSheet hero = state.findHeroById(heroId);
                if (hero != null) {
                    PendingRoll initRoll = rules.buildInitiativeRoll(hero);
                    RollResult res = rules.executePendingRoll(initRoll, hero, null);
                    recordHeroInitiative(state, hero, res);
                }
            }
            finalizeInitiativeOrder(state);
        } else {
            promptNextHeroInitiative(state);
        }
    }

    public void promptNextHeroInitiative(CampaignState state) {
        CombatState combat = state.getCombat();
        if (!combat.isActive()) return;
        if (!combat.getPendingInitiativeHeroIds().isEmpty()) {
            String heroId = combat.getPendingInitiativeHeroIds().get(0);
            CharacterSheet hero = state.findHeroById(heroId);
            if (hero != null) {
                state.setActiveCharacterId(hero.getId());
                state.setPendingRoll(rules.buildInitiativeRoll(hero));
            }
        } else {
            finalizeInitiativeOrder(state);
        }
    }

    public void resolveHeroInitiative(CampaignState state, CharacterSheet hero, RollResult result) {
        CombatState combat = state.getCombat();
        combat.getPendingInitiativeHeroIds().remove(hero.getId());
        recordHeroInitiative(state, hero, result);
        if (combat.getPendingInitiativeHeroIds().isEmpty()) {
            finalizeInitiativeOrder(state);
        } else {
            promptNextHeroInitiative(state);
        }
    }

    private void recordHeroInitiative(CampaignState state, CharacterSheet hero, RollResult result) {
        state.getCombat().getTurnOrder().add(new InitiativeEntry(
                hero.getId(), hero.getName(), true, result.getTotal(), hero.getInitiativeModifier()));
        state.addLog(LogEntry.dice(hero.getName(),
                hero.getName() + " rolled Initiative: " + result.getTotal(),
                result.getFormattedFormula()));
    }

    public void finalizeInitiativeOrder(CampaignState state) {
        CombatState combat = state.getCombat();
        combat.getTurnOrder().sort(Comparator
                .comparingInt(InitiativeEntry::getInitiativeTotal)
                .thenComparingInt(InitiativeEntry::getDexModifier)
                .reversed());
        combat.setCurrentTurnIndex(0);
        combat.setRoundNumber(1);

        StringBuilder orderText = new StringBuilder("Strict D&D 5E Initiative Order (Round 1):\n");
        for (int i = 0; i < combat.getTurnOrder().size(); i++) {
            InitiativeEntry e = combat.getTurnOrder().get(i);
            orderText.append(i + 1).append(". ").append(e.getName())
                    .append(" (Init ").append(e.getInitiativeTotal()).append(")")
                    .append(e.isPlayer() ? " [Hero]" : " [Enemy]").append("\n");
        }
        state.addLog(LogEntry.combat("Initiative Locked", orderText.toString().trim()));
        beginCurrentTurn(state);
    }

    public void beginCurrentTurn(CampaignState state) {
        CombatState combat = state.getCombat();
        if (!combat.isActive() || checkCombatEnd(state)) {
            return;
        }

        int safety = 0;
        while (safety < combat.getTurnOrder().size() * 2) {
            safety++;
            InitiativeEntry entry = combat.getCurrentTurnEntry();
            if (entry == null) return;
            entry.resetTurnEconomy();

            if (entry.isPlayer()) {
                CharacterSheet hero = state.findHeroById(entry.getParticipantId());
                if (hero == null || hero.isDead()) {
                    advanceIndexOnly(combat);
                    continue;
                }
                // Clear start-of-turn temporary defensive states
                hero.getConditions().remove(Condition.DODGING);
                hero.getConditions().remove(Condition.SHIELDED);
                state.setActiveCharacterId(hero.getId());

                if (hero.getCurrentHp() <= 0 && !hero.isStable()) {
                    state.addLog(LogEntry.combat("Round " + combat.getRoundNumber() + " — " + hero.getName() + " (Unconscious)",
                            hero.getName() + " is unconscious at 0 HP and must make a Death Saving Throw!"));
                    state.setPendingRoll(rules.buildDeathSaveRoll(hero));
                    return;
                }
                if (hero.getCurrentHp() <= 0 && hero.isStable()) {
                    state.addLog(LogEntry.combat(hero.getName() + " (Stable)",
                            hero.getName() + " remains stable at 0 HP. Turn passes."));
                    advanceIndexOnly(combat);
                    continue;
                }
                if (hero.getConditions().contains(Condition.STUNNED)) {
                    hero.getConditions().remove(Condition.STUNNED);
                    state.addLog(LogEntry.combat(hero.getName() + " (Stunned)",
                            hero.getName() + " shakes off the Stunned condition, losing this turn."));
                    advanceIndexOnly(combat);
                    continue;
                }
                state.addLog(LogEntry.combat("Round " + combat.getRoundNumber() + " — " + hero.getName() + "'s Turn",
                        "It is " + hero.getName() + "'s turn (" + hero.getCharacterClass().getDisplayName()
                                + ", HP " + hero.getCurrentHp() + "/" + hero.getMaxHp() + ", AC " + hero.getArmorClass()
                                + "). Describe your action in free text or use a combat button."));
                return;
            } else {
                Monster enemy = combat.findEnemyById(entry.getParticipantId());
                if (enemy == null || !enemy.isAlive()) {
                    advanceIndexOnly(combat);
                    continue;
                }
                executeEnemyTurn(state, enemy);
                if (checkCombatEnd(state)) {
                    return;
                }
                advanceIndexOnly(combat);
            }
        }
    }

    public void endCurrentHeroTurn(CampaignState state) {
        CombatState combat = state.getCombat();
        if (!combat.isActive()) return;
        if (checkCombatEnd(state)) return;
        advanceIndexOnly(combat);
        beginCurrentTurn(state);
    }

    private void advanceIndexOnly(CombatState combat) {
        int next = combat.getCurrentTurnIndex() + 1;
        if (next >= combat.getTurnOrder().size()) {
            next = 0;
            combat.setRoundNumber(combat.getRoundNumber() + 1);
        }
        combat.setCurrentTurnIndex(next);
    }

    private void executeEnemyTurn(CampaignState state, Monster enemy) {
        if (enemy.getConditions().contains(Condition.STUNNED)) {
            enemy.getConditions().remove(Condition.STUNNED);
            state.addLog(LogEntry.combat(enemy.getName(), enemy.getName() + " is paralyzed/stunned and recovers at the end of its turn."));
            return;
        }
        enemy.getConditions().remove(Condition.DODGING);

        List<CharacterSheet> consciousHeroes = new ArrayList<>();
        for (CharacterSheet h : state.getParty()) {
            if (h.isConscious()) {
                consciousHeroes.add(h);
            }
        }
        if (consciousHeroes.isEmpty()) {
            return;
        }

        // Pick a target (preferring heroes who aren't hidden; slight preference for frontliners)
        CharacterSheet target = consciousHeroes.get(rules.getDiceRoller().rollDie(consciousHeroes.size()) - 1);
        for (CharacterSheet h : consciousHeroes) {
            if (!h.getConditions().contains(Condition.HIDDEN)
                    && (h.getCharacterClass() == CharacterClass.FIGHTER || h.getCharacterClass() == CharacterClass.PALADIN)) {
                if (rules.getDiceRoller().rollDie(100) <= 60) {
                    target = h;
                    break;
                }
            }
        }

        boolean dis = target.getConditions().contains(Condition.DODGING)
                || enemy.getConditions().contains(Condition.POISONED)
                || enemy.getConditions().contains(Condition.FRIGHTENED)
                || enemy.getConditions().contains(Condition.BLINDED);
        boolean adv = target.getConditions().contains(Condition.PRONE)
                || target.getConditions().contains(Condition.RESTRAINED)
                || target.getConditions().contains(Condition.STUNNED);

        RollResult atk = rules.getDiceRoller().rollD20(enemy.getAttackBonus(), adv, dis, target.getArmorClass(), true, false);
        if (atk.isSucceeded()) {
            RollResult dmg = rules.getDiceRoller().rollDiceNotation(enemy.getDamageDice(), 0, atk.isNatural20());
            target.applyDamage(dmg.getTotal());
            StringBuilder msg = new StringBuilder();
            msg.append(enemy.getName()).append(" strikes ").append(target.getName())
                    .append(" with ").append(enemy.getAttackName()).append(" for ")
                    .append(dmg.getTotal()).append(" ").append(enemy.getDamageType()).append(" damage! ")
                    .append("(").append(target.getName()).append(" HP: ").append(target.getCurrentHp())
                    .append("/").append(target.getMaxHp()).append(")");
            if (target.getCurrentHp() == 0) {
                msg.append("\n").append(target.getName()).append(" drops UNCONSCIOUS!");
            } else if (enemy.getOnHitCondition() != null && enemy.getOnHitSaveAbility() != null) {
                boolean dwarfPoisonAdv = enemy.getOnHitCondition() == Condition.POISONED
                        && target.getRace() == com.dndcoop.model.Race.DWARF;
                RollResult save = rules.getDiceRoller().rollD20(
                        target.getSavingThrowModifier(enemy.getOnHitSaveAbility()),
                        dwarfPoisonAdv, false, enemy.getOnHitSaveDc(), false, false);
                if (!save.isSucceeded()) {
                    target.getConditions().add(enemy.getOnHitCondition());
                    msg.append("\n").append(target.getName()).append(" failed a ")
                            .append(enemy.getOnHitSaveAbility().getShortName())
                            .append(" save (").append(save.getTotal()).append(" vs DC ").append(enemy.getOnHitSaveDc())
                            .append(") and becomes ").append(enemy.getOnHitCondition().getDisplayName()).append("!");
                }
            }
            state.addLog(LogEntry.combat(enemy.getName() + " Attacks!", msg.toString()));
            state.addLog(LogEntry.dice(enemy.getName(), "Attack HIT -> " + dmg.getTotal() + " dmg",
                    atk.getFormattedFormula() + " | Damage: " + dmg.getFormattedFormula()));
        } else {
            state.addLog(LogEntry.combat(enemy.getName() + " Misses!",
                    enemy.getName() + " swings " + enemy.getAttackName() + " at " + target.getName()
                            + ", but the blow glances harmlessly off their defenses (AC " + target.getArmorClass() + ")."));
            state.addLog(LogEntry.dice(enemy.getName(), "Attack MISSED", atk.getFormattedFormula()));
        }
    }

    public String executeStandardCombatAction(CampaignState state, CharacterSheet hero, String actionType,
                                              Monster targetEnemy, CharacterSheet targetAlly) {
        InitiativeEntry turn = state.getCombat().getCurrentTurnEntry();
        if (turn == null || !hero.getId().equals(turn.getParticipantId())) {
            return "Hold on! In combat, actions strictly follow D&D 5E Initiative order. It is currently "
                    + (turn != null ? turn.getName() : "another combatant") + "'s turn.";
        }
        String norm = actionType.toLowerCase();
        switch (norm) {
            case "dodge":
                hero.getConditions().add(Condition.DODGING);
                turn.setActionUsed(true);
                state.addLog(LogEntry.combat(hero.getName() + " takes the Dodge Action",
                        hero.getName() + " adopts a defensive stance. Attacks against them have Disadvantage and DEX saves have Advantage until their next turn."));
                endCurrentHeroTurn(state);
                return "Dodge";
            case "dash":
                turn.setActionUsed(true);
                state.addLog(LogEntry.combat(hero.getName() + " takes the Dash Action",
                        hero.getName() + " sprints across the battlefield, doubling movement speed (" + (hero.getSpeed() * 2) + " ft) to seize high ground."));
                endCurrentHeroTurn(state);
                return "Dash";
            case "disengage":
                turn.setActionUsed(true);
                turn.setDisengaged(true);
                hero.getConditions().remove(Condition.GRAPPLED);
                state.addLog(LogEntry.combat(hero.getName() + " Disengages",
                        hero.getName() + " carefully withdraws from melee reach without provoking opportunity attacks."));
                endCurrentHeroTurn(state);
                return "Disengage";
            case "help":
                CharacterSheet ally = targetAlly;
                if (ally == null) {
                    for (CharacterSheet h : state.getParty()) {
                        if (!h.getId().equals(hero.getId()) && h.isConscious()) {
                            ally = h;
                            break;
                        }
                    }
                }
                if (ally == null) {
                    ally = hero;
                }
                ally.getConditions().add(Condition.HELPED);
                turn.setActionUsed(true);
                state.addLog(LogEntry.combat(hero.getName() + " takes the Help Action",
                        hero.getName() + " creates a tactical opening for " + ally.getName() + ", granting them Advantage on their next attack or check!"));
                endCurrentHeroTurn(state);
                return "Help";
            case "hide":
                turn.setActionUsed(true);
                PendingRoll stealth = rules.buildAbilityCheckRoll(hero, com.dndcoop.model.Skill.STEALTH,
                        Ability.DEX, 13, hero.getName() + " attempts to Hide (Stealth DC 13)", "Hide in combat");
                stealth.setSuccessNarrative(hero.getName() + " slips into the shadows — gaining Hidden (Advantage on next attack)!");
                stealth.setFailureNarrative(hero.getName() + " tries to duck behind cover, but the enemies keep eyes on them.");
                state.setPendingRoll(stealth);
                return "Hide";
            case "second_wind":
                if (hero.getCharacterClass() != CharacterClass.FIGHTER || hero.getClassResourceCurrent() <= 0) {
                    return "Second Wind is not available.";
                }
                RollResult sw = rules.getDiceRoller().rollDiceNotation("1d10", hero.getLevel(), false);
                int healed = hero.applyHealing(sw.getTotal());
                hero.setClassResourceCurrent(hero.getClassResourceCurrent() - 1);
                turn.setBonusActionUsed(true);
                state.addLog(LogEntry.dice(hero.getName(),
                        hero.getName() + " uses Second Wind (Bonus Action), recovering " + healed + " HP!",
                        sw.getFormattedFormula()));
                return "Second Wind";
            case "lay_on_hands":
                if (hero.getCharacterClass() != CharacterClass.PALADIN || hero.getClassResourceCurrent() <= 0) {
                    return "Lay on Hands pool is empty.";
                }
                CharacterSheet healTarget = targetAlly != null ? targetAlly : hero;
                int amount = Math.min(hero.getClassResourceCurrent(), Math.max(1, healTarget.getMaxHp() - healTarget.getCurrentHp()));
                if (amount <= 0) amount = Math.min(5, hero.getClassResourceCurrent());
                int restored = healTarget.applyHealing(amount);
                hero.setClassResourceCurrent(hero.getClassResourceCurrent() - amount);
                turn.setActionUsed(true);
                state.addLog(LogEntry.combat(hero.getName() + " uses Lay on Hands",
                        hero.getName() + " channels golden light into " + healTarget.getName() + ", restoring " + restored + " HP."));
                endCurrentHeroTurn(state);
                return "Lay on Hands";
            default:
                return null;
        }
    }

    public boolean castSpellInCombatOrExploration(CampaignState state, CharacterSheet caster, Spell spell,
                                                  Monster targetEnemy, CharacterSheet targetAlly, String actionText) {
        if (!caster.hasSpellSlot(spell.getLevel())) {
            state.addLog(LogEntry.system(caster.getName() + " has no Level " + spell.getLevel()
                    + " spell slots remaining to cast " + spell.getName() + "!"));
            return false;
        }

        InitiativeEntry turn = state.getCombat().isActive() ? state.getCombat().getCurrentTurnEntry() : null;
        if (turn != null) {
            if (spell.isBonusAction()) {
                if (turn.isBonusActionUsed()) {
                    state.addLog(LogEntry.system(caster.getName() + " already used their Bonus Action this turn."));
                    return false;
                }
                turn.setBonusActionUsed(true);
            } else {
                if (turn.isActionUsed()) {
                    state.addLog(LogEntry.system(caster.getName() + " already used their Action this turn."));
                    return false;
                }
                turn.setActionUsed(true);
            }
        }

        caster.consumeSpellSlot(spell.getLevel());

        switch (spell.getCastMode()) {
            case HEAL: {
                CharacterSheet recipient = targetAlly != null ? targetAlly : findMostWoundedHero(state, caster);
                int mod = caster.getCharacterClass().isSpellcaster()
                        ? caster.getAbilityModifier(caster.getCharacterClass().getSpellcastingAbility()) : 0;
                if (caster.getCharacterClass() == CharacterClass.CLERIC) {
                    mod += 2; // Life Domain bonus
                }
                RollResult healRoll = rules.getDiceRoller().rollDiceNotation(spell.getDiceNotation(), mod, false);
                int actual = recipient.applyHealing(healRoll.getTotal());
                if ("lesser_restoration".equals(spell.getId())) {
                    recipient.getConditions().remove(Condition.POISONED);
                    recipient.getConditions().remove(Condition.BLINDED);
                    recipient.getConditions().remove(Condition.FRIGHTENED);
                }
                state.addLog(LogEntry.dice(caster.getName(),
                        caster.getName() + " casts " + spell.getName() + " on " + recipient.getName()
                                + ", restoring " + actual + " HP (" + recipient.getCurrentHp() + "/" + recipient.getMaxHp() + ")!",
                        healRoll.getFormattedFormula()));
                if (state.getCombat().isActive() && !spell.isBonusAction()) {
                    endCurrentHeroTurn(state);
                }
                return true;
            }
            case BUFF: {
                if (spell.isAreaOfEffect()) {
                    for (CharacterSheet h : state.getParty()) {
                        if (h.isConscious()) h.getConditions().add(spell.getAppliedCondition());
                    }
                    state.addLog(LogEntry.combat(caster.getName() + " casts " + spell.getName(),
                            "Golden radiance washes over the party — everyone gains " + spell.getAppliedCondition().getDisplayName() + "!"));
                } else {
                    CharacterSheet recipient = targetAlly != null ? targetAlly : caster;
                    recipient.getConditions().add(spell.getAppliedCondition());
                    state.addLog(LogEntry.combat(caster.getName() + " casts " + spell.getName(),
                            recipient.getName() + " gains " + spell.getAppliedCondition().getDisplayName() + "!"));
                }
                if (state.getCombat().isActive() && !spell.isBonusAction()) {
                    endCurrentHeroTurn(state);
                }
                return true;
            }
            case AUTO_HIT_DAMAGE: {
                Monster foe = targetEnemy != null ? targetEnemy : firstLivingEnemy(state);
                if (foe == null) {
                    state.addLog(LogEntry.system("There is no hostile target to strike with " + spell.getName() + "."));
                    return true;
                }
                RollResult dmg = rules.getDiceRoller().rollDiceNotation(spell.getDiceNotation(), 0, false);
                foe.applyDamage(dmg.getTotal());
                state.addLog(LogEntry.dice(caster.getName(),
                        caster.getName() + " casts " + spell.getName() + "! Unerring force darts slam into "
                                + foe.getName() + " for " + dmg.getTotal() + " " + spell.getDamageType() + " damage!"
                                + (!foe.isAlive() ? " " + foe.getName() + " collapses!" : ""),
                        dmg.getFormattedFormula()));
                if (!checkCombatEnd(state) && state.getCombat().isActive() && !spell.isBonusAction()) {
                    endCurrentHeroTurn(state);
                }
                return true;
            }
            case SAVING_THROW: {
                List<Monster> targets = new ArrayList<>();
                if (spell.isAreaOfEffect()) {
                    targets.addAll(state.getCombat().getLivingEnemies());
                } else {
                    Monster foe = targetEnemy != null ? targetEnemy : firstLivingEnemy(state);
                    if (foe != null) targets.add(foe);
                }
                if (targets.isEmpty()) {
                    state.addLog(LogEntry.system("No hostile targets in range for " + spell.getName() + "."));
                    return true;
                }
                int dc = caster.getSpellSaveDc();
                RollResult baseDmg = spell.getDiceNotation() != null && !spell.getDiceNotation().startsWith("0")
                        ? rules.getDiceRoller().rollDiceNotation(spell.getDiceNotation(), 0, false)
                        : null;
                StringBuilder summary = new StringBuilder(caster.getName() + " unleashes " + spell.getName()
                        + " (Spell Save DC " + dc + " " + spell.getSaveAbility().getShortName() + "):\n");
                for (Monster foe : targets) {
                    RollResult save = rules.getDiceRoller().rollD20(foe.getAbilityModifier(spell.getSaveAbility()), false, false, dc, false, false);
                    int dealt = 0;
                    if (baseDmg != null) {
                        dealt = save.isSucceeded() ? baseDmg.getTotal() / 2 : baseDmg.getTotal();
                        foe.applyDamage(dealt);
                    }
                    summary.append("• ").append(foe.getName()).append(" ")
                            .append(save.isSucceeded() ? "SAVED (" + save.getTotal() + ")" : "FAILED (" + save.getTotal() + ")");
                    if (dealt > 0) {
                        summary.append(" -> ").append(dealt).append(" ").append(spell.getDamageType()).append(" dmg");
                    }
                    if (!save.isSucceeded() && spell.getAppliedCondition() != null && foe.isAlive()) {
                        foe.getConditions().add(spell.getAppliedCondition());
                        summary.append(" [").append(spell.getAppliedCondition().getDisplayName()).append("]");
                    }
                    if (!foe.isAlive()) {
                        summary.append(" [DEFEATED]");
                    }
                    summary.append("\n");
                }
                state.addLog(LogEntry.dice(caster.getName(), summary.toString().trim(),
                        baseDmg != null ? "Damage Roll: " + baseDmg.getFormattedFormula() : "Condition Spell"));
                if (!checkCombatEnd(state) && state.getCombat().isActive() && !spell.isBonusAction()) {
                    endCurrentHeroTurn(state);
                }
                return true;
            }
            case SPELL_ATTACK: {
                Monster foe = targetEnemy != null ? targetEnemy : firstLivingEnemy(state);
                if (foe == null) {
                    state.addLog(LogEntry.system("There is no target to cast " + spell.getName() + " at."));
                    return true;
                }
                PendingRoll pr = rules.buildSpellAttackRoll(caster, spell, foe, actionText);
                state.setPendingRoll(pr);
                return true;
            }
            default:
                return true;
        }
    }

    public boolean checkCombatEnd(CampaignState state) {
        CombatState combat = state.getCombat();
        if (!combat.isActive()) {
            return false;
        }
        if (combat.getLivingEnemies().isEmpty()) {
            int totalXp = 0;
            for (Monster m : combat.getEnemies()) {
                totalXp += m.getXpValue();
            }
            combat.setActive(false);
            state.setPendingRoll(null);
            if (combat.getEncounterId() != null) {
                state.getDefeatedEncounters().add(combat.getEncounterId());
            }
            if (combat.getVictoryFlag() != null && !combat.getVictoryFlag().isBlank()) {
                state.setFlag(combat.getVictoryFlag(), true);
            }
            // Clear combat-only conditions
            for (CharacterSheet hero : state.getParty()) {
                hero.getConditions().remove(Condition.DODGING);
                hero.getConditions().remove(Condition.HIDDEN);
                hero.getConditions().remove(Condition.HELPED);
                hero.getConditions().remove(Condition.SHIELDED);
                hero.getConditions().remove(Condition.BLESSED);
                if (hero.getCurrentHp() == 0 && !hero.isDead()) {
                    hero.applyHealing(1);
                }
            }
            if (combat.getBonusGoldReward() > 0 && !state.getParty().isEmpty()) {
                int split = Math.max(1, combat.getBonusGoldReward() / state.getParty().size());
                for (CharacterSheet h : state.getParty()) {
                    h.setGold(h.getGold() + split);
                }
            }
            String rewardNote = "";
            if (combat.getRewardItemId() != null && !state.getParty().isEmpty()) {
                Item reward = com.dndcoop.campaign.AshenLanternCampaign.createRewardItem(combat.getRewardItemId());
                if (reward != null) {
                    state.getParty().get(0).addItem(reward);
                    rewardNote = "\nLoot Claimed: " + reward.getName() + " (added to " + state.getParty().get(0).getName() + "'s pack).";
                }
            }
            state.addLog(LogEntry.combat("VICTORY — " + combat.getEncounterName(),
                    "Every foe falls! The battlefield goes quiet.\nRewards: " + totalXp + " XP"
                            + (combat.getBonusGoldReward() > 0 ? ", " + combat.getBonusGoldReward() + " gp split among the party" : "")
                            + "." + rewardNote));
            LevelingSystem.awardPartyXp(state, totalXp);
            return true;
        }
        if (state.isPartyWiped()) {
            combat.setActive(false);
            state.setPendingRoll(null);
            state.addLog(LogEntry.combat("DEFEAT — The Party Has Fallen",
                    "Darkness claims the party. Load a saved checkpoint or rally at the Gilded Stag Tavern to try again."));
            return true;
        }
        return false;
    }

    public Monster findTargetEnemy(CampaignState state, String freeText) {
        List<Monster> living = state.getCombat().getLivingEnemies();
        if (living.isEmpty()) return null;
        if (freeText != null && !freeText.isBlank()) {
            String lower = freeText.toLowerCase();
            for (Monster m : living) {
                String mName = m.getName().toLowerCase();
                if (lower.contains(mName)) {
                    return m;
                }
                for (String token : mName.split("\\s+")) {
                    if (token.length() >= 3 && lower.contains(token)) {
                        return m;
                    }
                }
            }
        }
        return living.get(0);
    }

    public boolean partyHasOtherConsciousHero(CampaignState state, CharacterSheet hero) {
        for (CharacterSheet other : state.getParty()) {
            if (!other.getId().equals(hero.getId()) && other.isConscious()) {
                return true;
            }
        }
        return false;
    }

    private Monster firstLivingEnemy(CampaignState state) {
        List<Monster> living = state.getCombat().getLivingEnemies();
        return living.isEmpty() ? null : living.get(0);
    }

    private CharacterSheet findMostWoundedHero(CampaignState state, CharacterSheet fallback) {
        CharacterSheet best = fallback;
        double lowestRatio = 2.0;
        for (CharacterSheet h : state.getParty()) {
            if (h.isDead()) continue;
            double ratio = (double) h.getCurrentHp() / Math.max(1, h.getMaxHp());
            if (ratio < lowestRatio) {
                lowestRatio = ratio;
                best = h;
            }
        }
        return best;
    }
}
