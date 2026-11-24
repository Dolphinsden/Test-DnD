package com.dndcoop.model;

import java.util.EnumSet;
import java.util.Set;

public enum CharacterClass {
    FIGHTER("Fighter", 10, Ability.STR, null,
            EnumSet.of(Ability.STR, Ability.CON),
            "Second Wind (1d10 + level HP bonus action, short rest); Action Surge at level 2."),
    WIZARD("Wizard", 6, Ability.INT, Ability.INT,
            EnumSet.of(Ability.INT, Ability.WIS),
            "Arcane Spellcasting (INT), Arcane Recovery on short rest."),
    ROGUE("Rogue", 8, Ability.DEX, null,
            EnumSet.of(Ability.DEX, Ability.INT),
            "Sneak Attack (+1d6 extra damage with advantage or ally nearby; scales with level), Cunning Action."),
    CLERIC("Cleric", 8, Ability.WIS, Ability.WIS,
            EnumSet.of(Ability.WIS, Ability.CHA),
            "Divine Spellcasting (WIS), Life Domain Healing (+2 bonus HP on healing spells)."),
    RANGER("Ranger", 10, Ability.DEX, Ability.WIS,
            EnumSet.of(Ability.STR, Ability.DEX),
            "Favored Enemy & Natural Explorer; Spellcasting (WIS) unlocked at level 1/2."),
    PALADIN("Paladin", 10, Ability.STR, Ability.CHA,
            EnumSet.of(Ability.WIS, Ability.CHA),
            "Lay on Hands (5 x level HP healing pool), Divine Smite (+2d8 radiant via spell slot).");

    private final String displayName;
    private final int hitDie;
    private final Ability primaryAbility;
    private final Ability spellcastingAbility;
    private final Set<Ability> savingThrowProficiencies;
    private final String featuresSummary;

    CharacterClass(String displayName, int hitDie, Ability primaryAbility, Ability spellcastingAbility,
                   Set<Ability> savingThrowProficiencies, String featuresSummary) {
        this.displayName = displayName;
        this.hitDie = hitDie;
        this.primaryAbility = primaryAbility;
        this.spellcastingAbility = spellcastingAbility;
        this.savingThrowProficiencies = savingThrowProficiencies;
        this.featuresSummary = featuresSummary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getHitDie() {
        return hitDie;
    }

    public Ability getPrimaryAbility() {
        return primaryAbility;
    }

    public Ability getSpellcastingAbility() {
        return spellcastingAbility;
    }

    public boolean isSpellcaster() {
        return spellcastingAbility != null;
    }

    public Set<Ability> getSavingThrowProficiencies() {
        return EnumSet.copyOf(savingThrowProficiencies);
    }

    public String getFeaturesSummary() {
        return featuresSummary;
    }

    public int[] getSpellSlotsForLevel(int level) {
        // Index 1 = 1st-level slots, Index 2 = 2nd-level slots, Index 3 = 3rd-level slots
        int[] slots = new int[4];
        if (this == WIZARD || this == CLERIC) {
            if (level == 1) {
                slots[1] = 2;
            } else if (level == 2) {
                slots[1] = 3;
            } else if (level == 3) {
                slots[1] = 4;
                slots[2] = 2;
            } else if (level == 4) {
                slots[1] = 4;
                slots[2] = 3;
            } else {
                slots[1] = 4;
                slots[2] = 3;
                slots[3] = 2;
            }
        } else if (this == PALADIN || this == RANGER) {
            // Half-caster progression (grant 1 starter slot at level 1 so couch players can cast right away, matching SRD progression from level 2+)
            if (level == 1) {
                slots[1] = 1;
            } else if (level == 2) {
                slots[1] = 2;
            } else if (level <= 4) {
                slots[1] = 3;
            } else {
                slots[1] = 4;
                slots[2] = 2;
            }
        }
        return slots;
    }
}
