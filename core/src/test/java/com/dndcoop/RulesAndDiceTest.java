package com.dndcoop;

import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.Condition;
import com.dndcoop.model.RollResult;
import com.dndcoop.model.Skill;
import com.dndcoop.rules.CharacterFactory;
import com.dndcoop.rules.DiceRoller;
import com.dndcoop.rules.LevelingSystem;
import com.dndcoop.rules.RulesEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RulesAndDiceTest {

    @Test
    void abilityModifierAndCharacterSheetStatsFollow5eRules() {
        assertEquals(-1, Ability.modifierForScore(8));
        assertEquals(0, Ability.modifierForScore(10));
        assertEquals(3, Ability.modifierForScore(16));
        assertEquals(3, Ability.modifierForScore(17));

        CharacterSheet thorne = CharacterFactory.createThorneFighter();
        // Dwarf Fighter: STR 16 (+3), CON 16 (+3), Chain Mail 16 + Shield 2 = AC 18, Level 1 HP = 10 + 3 = 13
        assertEquals(18, thorne.getArmorClass());
        assertEquals(13, thorne.getMaxHp());
        assertEquals(2, thorne.getProficiencyBonus());
        assertEquals(5, thorne.getSkillModifier(Skill.ATHLETICS)); // +3 STR +2 Prof
        assertEquals(5, thorne.getWeaponAttackBonus(thorne.getEquippedWeapon()));

        // Shield spell buff grants +5 AC temporarily
        thorne.getConditions().add(Condition.SHIELDED);
        assertEquals(23, thorne.getArmorClass());
    }

    @Test
    void physicalDiceAndAdvantageCriticalMathAreExact() {
        DiceRoller roller = new DiceRoller(new Random(42L));

        // Physical d20 natural 20 always hits AC 25 on attack roll
        RollResult nat20 = roller.resolveManualD20(20, 3, false, false, 25, true);
        assertTrue(nat20.isNatural20());
        assertTrue(nat20.isPhysicalDiceUsed());
        assertTrue(nat20.isSucceeded());
        assertEquals(23, nat20.getTotal());

        // Physical d20 natural 1 always misses AC 5 on attack roll
        RollResult nat1 = roller.resolveManualD20(1, 10, false, false, 5, true);
        assertTrue(nat1.isNatural1());
        assertFalse(nat1.isSucceeded());

        // Critical hit doubles dice count (1d8 -> 2d8)
        RollResult critDmg = roller.resolveManualDice("1d8", 12, 3, true);
        assertEquals(15, critDmg.getTotal());
        assertTrue(critDmg.getFormattedFormula().contains("2d8"));

        // Advantage & Disadvantage together cancel out to a single die
        RollResult canceled = roller.rollD20(2, true, true, 10, false, false);
        assertFalse(canceled.isAdvantage());
        assertFalse(canceled.isDisadvantage());
        assertEquals(1, canceled.getRawRolls().size());
    }

    @Test
    void deathSavingThrowsFollowOfficial5eRules() {
        RulesEngine rules = new RulesEngine(new DiceRoller(new Random(7L)));
        CharacterSheet lyra = CharacterFactory.createLyraWizard();
        lyra.applyDamage(100);
        assertEquals(0, lyra.getCurrentHp());
        assertTrue(lyra.getConditions().contains(Condition.UNCONSCIOUS));

        // 1 failure (roll 5)
        rules.applyDeathSaveResult(lyra, rules.getDiceRoller().resolveManualD20(5, 0, false, false, 10, false));
        assertEquals(1, lyra.getDeathSaveFailures());
        assertFalse(lyra.isDead());

        // Nat 20 revives hero with 1 HP and clears death saves
        rules.applyDeathSaveResult(lyra, rules.getDiceRoller().resolveManualD20(20, 0, false, false, 10, false));
        assertEquals(1, lyra.getCurrentHp());
        assertFalse(lyra.getConditions().contains(Condition.UNCONSCIOUS));
        assertEquals(0, lyra.getDeathSaveFailures());

        // Drop again -> Nat 1 causes 2 failures, then another fail kills
        lyra.applyDamage(10);
        rules.applyDeathSaveResult(lyra, rules.getDiceRoller().resolveManualD20(1, 0, false, false, 10, false));
        assertEquals(2, lyra.getDeathSaveFailures());
        rules.applyDeathSaveResult(lyra, rules.getDiceRoller().resolveManualD20(4, 0, false, false, 10, false));
        assertTrue(lyra.isDead());
    }

    @Test
    void shortRestLongRestAndLevelUpWorkDeterministically() {
        RulesEngine rules = new RulesEngine(new DiceRoller(new Random(123L)));
        CampaignState state = new CampaignState();
        CharacterSheet lyra = CharacterFactory.createLyraWizard();
        state.setParty(List.of(lyra));

        lyra.applyDamage(4);
        assertTrue(lyra.consumeSpellSlot(1));
        assertEquals(1, lyra.getSpellSlotsCurrent()[1]);

        rules.applyShortRest(state);
        assertTrue(lyra.getCurrentHp() > lyra.getMaxHp() - 4);
        // Arcane Recovery restored the 1st-level slot
        assertEquals(2, lyra.getSpellSlotsCurrent()[1]);

        lyra.applyDamage(5);
        lyra.consumeSpellSlot(1);
        rules.applyLongRest(state);
        assertEquals(lyra.getMaxHp(), lyra.getCurrentHp());
        assertEquals(lyra.getSpellSlotsMax()[1], lyra.getSpellSlotsCurrent()[1]);

        // Award 300 XP -> Level 2
        List<String> ups = LevelingSystem.awardPartyXp(state, 300);
        assertEquals(1, ups.size());
        assertEquals(2, lyra.getLevel());
        assertEquals(3, lyra.getSpellSlotsMax()[1]);
    }
}
