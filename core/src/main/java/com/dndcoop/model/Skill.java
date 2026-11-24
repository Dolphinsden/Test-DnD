package com.dndcoop.model;

public enum Skill {
    ACROBATICS("Acrobatics", Ability.DEX),
    ANIMAL_HANDLING("Animal Handling", Ability.WIS),
    ARCANA("Arcana", Ability.INT),
    ATHLETICS("Athletics", Ability.STR),
    DECEPTION("Deception", Ability.CHA),
    HISTORY("History", Ability.INT),
    INSIGHT("Insight", Ability.WIS),
    INTIMIDATION("Intimidation", Ability.CHA),
    INVESTIGATION("Investigation", Ability.INT),
    MEDICINE("Medicine", Ability.WIS),
    NATURE("Nature", Ability.INT),
    PERCEPTION("Perception", Ability.WIS),
    PERFORMANCE("Performance", Ability.CHA),
    PERSUASION("Persuasion", Ability.CHA),
    RELIGION("Religion", Ability.INT),
    SLEIGHT_OF_HAND("Sleight of Hand", Ability.DEX),
    STEALTH("Stealth", Ability.DEX),
    SURVIVAL("Survival", Ability.WIS);

    private final String displayName;
    private final Ability governingAbility;

    Skill(String displayName, Ability governingAbility) {
        this.displayName = displayName;
        this.governingAbility = governingAbility;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Ability getGoverningAbility() {
        return governingAbility;
    }

    public static Skill fromName(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase().replace('-', ' ').replace('_', ' ');
        for (Skill skill : values()) {
            if (skill.displayName.toLowerCase().equals(normalized)
                    || skill.name().toLowerCase().replace('_', ' ').equals(normalized)) {
                return skill;
            }
        }
        return null;
    }
}
