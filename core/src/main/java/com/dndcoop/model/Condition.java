package com.dndcoop.model;

public enum Condition {
    BLINDED("Blinded", "Attack rolls have disadvantage; attacks against you have advantage."),
    CHARMED("Charmed", "Cannot attack the charmer; charmer has advantage on social checks."),
    FRIGHTENED("Frightened", "Disadvantage on ability checks and attack rolls while source of fear is in sight."),
    GRAPPLED("Grappled", "Speed becomes 0 until you break free."),
    POISONED("Poisoned", "Disadvantage on attack rolls and ability checks."),
    PRONE("Prone", "Disadvantage on attack rolls; melee attacks against you have advantage, ranged have disadvantage."),
    RESTRAINED("Restrained", "Speed 0; attack rolls have disadvantage; attacks against you have advantage."),
    STUNNED("Stunned", "Incapacitated, cannot move, automatically fails STR and DEX saves."),
    UNCONSCIOUS("Unconscious", "0 HP; incapacitated and prone; must make Death Saving Throws each turn."),
    DODGING("Dodging", "Until your next turn, attacks against you have disadvantage and DEX saves have advantage."),
    HIDDEN("Hidden", "Unseen by foes; your next attack roll has advantage."),
    HELPED("Helped", "An ally aided you; your next ability check or attack roll has advantage."),
    BLESSED("Blessed", "Add +1d4 to attack rolls and saving throws."),
    SHIELDED("Shielded", "+5 bonus to AC until the start of your next turn.");

    private final String displayName;
    private final String description;

    Condition(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
