package com.dndcoop;

import com.dndcoop.campaign.AshenLanternCampaign;
import com.dndcoop.controller.GameSessionController;
import com.dndcoop.dm.DmSettings;
import com.dndcoop.dm.HybridDungeonMaster;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.InitiativeEntry;
import com.dndcoop.model.Monster;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.persistence.SaveManager;
import com.dndcoop.rules.CharacterFactory;
import com.dndcoop.rules.DiceRoller;
import com.dndcoop.rules.RulesEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CombatAndInitiativeTest {

    @TempDir
    Path tempDir;

    @Test
    void combatEnforcesStrict5eInitiativeOrderAndInteractivePhysicalRolls() {
        SaveManager saveManager = new SaveManager(tempDir);
        DmSettings settings = new DmSettings();
        settings.setProvider(DmSettings.Provider.OFFLINE_ONLY);
        settings.setAutoRollDice(false);

        RulesEngine rules = new RulesEngine(new DiceRoller(new Random(99L)));
        GameSessionController ctrl = new GameSessionController(
                rules, new HybridDungeonMaster(), saveManager, settings, Runnable::run, false);

        CharacterSheet thorne = CharacterFactory.createThorneFighter();
        CharacterSheet pip = CharacterFactory.createPipRogue();
        CampaignState state = ctrl.startNewCampaign(List.of(thorne, pip), "Initiative Test");

        // Trigger Goblin Ambush encounter
        ctrl.getCombatEngine().startEncounter(state, AshenLanternCampaign.getEncounter("goblin_ambush"), false);
        assertTrue(state.getCombat().isActive());

        // First hero initiative prompt should be Thorne
        assertNotNull(state.getPendingRoll());
        assertEquals(PendingRoll.RollType.INITIATIVE, state.getPendingRoll().getRollType());
        assertEquals(thorne.getId(), state.getPendingRoll().getActorId());
        // Enter physical d20 = 19 for Thorne -> Init 20
        ctrl.resolvePendingRoll(19, null);

        // Next hero initiative prompt should be Pip
        assertNotNull(state.getPendingRoll());
        assertEquals(pip.getId(), state.getPendingRoll().getActorId());
        // Enter physical d20 = 20 for Pip (+3 DEX) -> Init 23 (highest in combat!)
        ctrl.resolvePendingRoll(20, null);

        // Initiative order must be strictly sorted descending!
        List<InitiativeEntry> order = state.getCombat().getTurnOrder();
        assertEquals(5, order.size()); // 2 heroes + 3 goblins
        for (int i = 0; i < order.size() - 1; i++) {
            assertTrue(order.get(i).getInitiativeTotal() >= order.get(i + 1).getInitiativeTotal(),
                    "Initiative order must be strictly descending");
        }
        assertEquals(pip.getId(), order.get(0).getParticipantId());
        assertEquals(pip.getId(), state.getCombat().getCurrentTurnEntry().getParticipantId());

        // Pip attacks via free-form text -> gets interactive Attack Roll prompt
        ctrl.submitFreeText("I lunge forward and stab the goblin archer with my rapier!", null);
        assertNotNull(state.getPendingRoll());
        assertEquals(PendingRoll.RollType.ATTACK_ROLL, state.getPendingRoll().getRollType());

        // Physical Natural 20 -> Critical Hit -> triggers DAMAGE_ROLL prompt
        ctrl.resolvePendingRoll(20, null);
        assertNotNull(state.getPendingRoll());
        assertEquals(PendingRoll.RollType.DAMAGE_ROLL, state.getPendingRoll().getRollType());
        assertTrue(state.getPendingRoll().isCriticalHit());

        Monster target = state.getCombat().findEnemyById(state.getPendingRoll().getTargetId());
        assertNotNull(target);
        // Physical crit damage roll of 14 -> slays 9 HP goblin
        ctrl.resolvePendingRoll(14, null);
        assertFalse(target.isAlive());
    }

    @Test
    void wizardSpellSlotsDecrementAndVictoryRewardsXpAndGold() {
        SaveManager saveManager = new SaveManager(tempDir);
        DmSettings settings = new DmSettings();
        settings.setProvider(DmSettings.Provider.OFFLINE_ONLY);
        settings.setAutoRollDice(true);

        RulesEngine rules = new RulesEngine(new DiceRoller(new Random(1234L)));
        GameSessionController ctrl = new GameSessionController(
                rules, new HybridDungeonMaster(), saveManager, settings, Runnable::run, false);

        CharacterSheet lyra = CharacterFactory.createLyraWizard();
        CampaignState state = ctrl.startNewCampaign(List.of(lyra), "Spell Combat Test");
        settings.setAutoRollDice(false);
        ctrl.getCombatEngine().startEncounter(state, AshenLanternCampaign.getEncounter("goblin_ambush"), false);
        ctrl.resolvePendingRoll(20, null); // Lyra acts first with Initiative 22
        settings.setAutoRollDice(true);
        assertTrue(state.getCombat().isActive());

        // Defeat two goblins quickly to test spell slot consumption + victory
        List<Monster> enemies = state.getCombat().getEnemies();
        enemies.get(0).applyDamage(100);
        enemies.get(1).applyDamage(100);
        enemies.get(2).setCurrentHp(5);

        int slotsBefore = lyra.getSpellSlotsCurrent()[1];
        ctrl.castSpellDirectly(lyra, "magic_missile", null);

        assertEquals(slotsBefore - 1, lyra.getSpellSlotsCurrent()[1]);
        assertFalse(state.getCombat().isActive(), "Magic Missile should finish the 5 HP boss and end combat");
        assertTrue(state.hasFlag("cleared_goblin_ambush"));
        assertTrue(lyra.getXp() >= 200);
        assertNotNull(lyra.findItemById("goblin_map"));
    }
}
