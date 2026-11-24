package com.dndcoop.rules;

import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterClass;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.LogEntry;

import java.util.ArrayList;
import java.util.List;

public final class LevelingSystem {

    // Official D&D 5E XP thresholds for levels 1..5
    private static final int[] XP_THRESHOLDS = {0, 0, 300, 900, 2700, 6500};

    private LevelingSystem() {
    }

    public static int xpForNextLevel(int currentLevel) {
        if (currentLevel < 1) return 300;
        if (currentLevel >= 5) return 6500;
        return XP_THRESHOLDS[currentLevel + 1];
    }

    public static List<String> awardPartyXp(CampaignState state, int totalXp) {
        List<String> levelUps = new ArrayList<>();
        if (state == null || state.getParty().isEmpty() || totalXp <= 0) {
            return levelUps;
        }
        int share = Math.max(10, totalXp / state.getParty().size());
        for (CharacterSheet hero : state.getParty()) {
            if (hero.isDead()) {
                continue;
            }
            hero.setXp(hero.getXp() + share);
            while (hero.getLevel() < 5 && hero.getXp() >= XP_THRESHOLDS[hero.getLevel() + 1]) {
                String msg = applyLevelUp(hero);
                levelUps.add(msg);
                state.addLog(LogEntry.system(msg));
            }
        }
        return levelUps;
    }

    public static String applyLevelUp(CharacterSheet hero) {
        int newLevel = hero.getLevel() + 1;
        hero.setLevel(newLevel);
        CharacterClass cls = hero.getCharacterClass();
        int hpGain = Math.max(1, (cls.getHitDie() / 2 + 1) + hero.getAbilityModifier(Ability.CON));
        hero.setMaxHp(hero.getMaxHp() + hpGain);
        hero.setCurrentHp(Math.min(hero.getMaxHp(), hero.getCurrentHp() + hpGain));
        hero.setHitDiceRemaining(newLevel);

        int[] newSlots = cls.getSpellSlotsForLevel(newLevel);
        int[] currentSlots = hero.getSpellSlotsCurrent();
        for (int i = 1; i < newSlots.length; i++) {
            int delta = Math.max(0, newSlots[i] - hero.getSpellSlotsMax()[i]);
            currentSlots[i] = Math.min(newSlots[i], currentSlots[i] + delta);
        }
        hero.setSpellSlotsMax(newSlots);
        hero.setSpellSlotsCurrent(currentSlots);

        if (cls == CharacterClass.PALADIN) {
            hero.setClassResourceMax(newLevel * 5);
            hero.setClassResourceCurrent(newLevel * 5);
        }
        if (newLevel == 3) {
            if (cls == CharacterClass.WIZARD && !hero.getKnownSpellIds().contains("scorching_ray")) {
                hero.getKnownSpellIds().add("scorching_ray");
            } else if (cls == CharacterClass.CLERIC && !hero.getKnownSpellIds().contains("lesser_restoration")) {
                hero.getKnownSpellIds().add("lesser_restoration");
            }
        }

        return "LEVEL UP! " + hero.getName() + " reached Level " + newLevel
                + " (+" + hpGain + " Max HP, Proficiency +" + hero.getProficiencyBonus() + ")!";
    }
}
