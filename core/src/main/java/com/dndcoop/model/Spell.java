package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Spell {

    public enum CastMode {
        SPELL_ATTACK,
        SAVING_THROW,
        AUTO_HIT_DAMAGE,
        HEAL,
        BUFF
    }

    private String id;
    private String name;
    private int level; // 0 = Cantrip
    private String school;
    private boolean bonusAction;
    private CastMode castMode;
    private Ability saveAbility;
    private String diceNotation;
    private String damageType;
    private boolean areaOfEffect;
    private Condition appliedCondition;
    private String description;

    public Spell() {
    }

    public Spell(String id, String name, int level, String school, boolean bonusAction,
                 CastMode castMode, Ability saveAbility, String diceNotation, String damageType,
                 boolean areaOfEffect, Condition appliedCondition, String description) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.school = school;
        this.bonusAction = bonusAction;
        this.castMode = castMode;
        this.saveAbility = saveAbility;
        this.diceNotation = diceNotation;
        this.damageType = damageType;
        this.areaOfEffect = areaOfEffect;
        this.appliedCondition = appliedCondition;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public boolean isBonusAction() { return bonusAction; }
    public void setBonusAction(boolean bonusAction) { this.bonusAction = bonusAction; }
    public CastMode getCastMode() { return castMode; }
    public void setCastMode(CastMode castMode) { this.castMode = castMode; }
    public Ability getSaveAbility() { return saveAbility; }
    public void setSaveAbility(Ability saveAbility) { this.saveAbility = saveAbility; }
    public String getDiceNotation() { return diceNotation; }
    public void setDiceNotation(String diceNotation) { this.diceNotation = diceNotation; }
    public String getDamageType() { return damageType; }
    public void setDamageType(String damageType) { this.damageType = damageType; }
    public boolean isAreaOfEffect() { return areaOfEffect; }
    public void setAreaOfEffect(boolean areaOfEffect) { this.areaOfEffect = areaOfEffect; }
    public Condition getAppliedCondition() { return appliedCondition; }
    public void setAppliedCondition(Condition appliedCondition) { this.appliedCondition = appliedCondition; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
