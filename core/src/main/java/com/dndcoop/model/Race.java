package com.dndcoop.model;

public enum Race {
    HUMAN("Human", 30, false, "Versatile (+1 to all ability scores)"),
    ELF("Elf", 30, true, "Darkvision 60ft, Keen Senses (Perception), Fey Ancestry (+2 DEX, +1 INT/WIS)"),
    DWARF("Dwarf", 25, true, "Darkvision 60ft, Dwarven Resilience (Poison advantage), Stonecunning (+2 CON, +1 STR/WIS)"),
    HALFLING("Halfling", 25, false, "Lucky (reroll natural 1s on d20), Brave (advantage vs Frightened) (+2 DEX, +1 CHA)");

    private final String displayName;
    private final int speed;
    private final boolean darkvision;
    private final String traitsSummary;

    Race(String displayName, int speed, boolean darkvision, String traitsSummary) {
        this.displayName = displayName;
        this.speed = speed;
        this.darkvision = darkvision;
        this.traitsSummary = traitsSummary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean hasDarkvision() {
        return darkvision;
    }

    public String getTraitsSummary() {
        return traitsSummary;
    }
}
