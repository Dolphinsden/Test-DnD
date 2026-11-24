package com.dndcoop.model;

public enum Ability {
    STR("Strength", "STR"),
    DEX("Dexterity", "DEX"),
    CON("Constitution", "CON"),
    INT("Intelligence", "INT"),
    WIS("Wisdom", "WIS"),
    CHA("Charisma", "CHA");

    private final String displayName;
    private final String shortName;

    Ability(String displayName, String shortName) {
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public static int modifierForScore(int score) {
        return Math.floorDiv(score - 10, 2);
    }

    public static String formatModifier(int mod) {
        return mod >= 0 ? "+" + mod : String.valueOf(mod);
    }
}
