package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Monster {

    private String id;
    private String templateId;
    private String name;
    private String cr;
    private int maxHp;
    private int currentHp;
    private int armorClass;
    private Map<Ability, Integer> abilityScores = new EnumMap<>(Ability.class);
    private String attackName;
    private int attackBonus;
    private String damageDice;
    private String damageType;
    private int xpValue;
    private String specialAbilityName;
    private String specialAbilityDescription;
    private Condition onHitCondition;
    private int onHitSaveDc;
    private Ability onHitSaveAbility;
    private Set<Condition> conditions = EnumSet.noneOf(Condition.class);
    private String description;

    public Monster() {
        for (Ability a : Ability.values()) {
            abilityScores.put(a, 10);
        }
    }

    @JsonIgnore
    public boolean isAlive() {
        return currentHp > 0;
    }

    @JsonIgnore
    public int getAbilityModifier(Ability ability) {
        return Ability.modifierForScore(abilityScores.getOrDefault(ability, 10));
    }

    @JsonIgnore
    public int getInitiativeModifier() {
        return getAbilityModifier(Ability.DEX);
    }

    public void applyDamage(int damage) {
        if (damage <= 0) {
            return;
        }
        currentHp = Math.max(0, currentHp - damage);
    }

    @JsonIgnore
    public String getHealthStatusLabel() {
        if (currentHp <= 0) {
            return "Defeated";
        }
        double ratio = (double) currentHp / Math.max(1, maxHp);
        if (ratio >= 0.85) return "Unharmed";
        if (ratio >= 0.5) return "Wounded";
        if (ratio >= 0.25) return "Bloodied";
        return "Critical";
    }

    public Monster copyWithId(String instanceId, String instanceName) {
        Monster m = new Monster();
        m.id = instanceId;
        m.templateId = templateId;
        m.name = instanceName;
        m.cr = cr;
        m.maxHp = maxHp;
        m.currentHp = maxHp;
        m.armorClass = armorClass;
        m.abilityScores = new EnumMap<>(abilityScores);
        m.attackName = attackName;
        m.attackBonus = attackBonus;
        m.damageDice = damageDice;
        m.damageType = damageType;
        m.xpValue = xpValue;
        m.specialAbilityName = specialAbilityName;
        m.specialAbilityDescription = specialAbilityDescription;
        m.onHitCondition = onHitCondition;
        m.onHitSaveDc = onHitSaveDc;
        m.onHitSaveAbility = onHitSaveAbility;
        m.description = description;
        return m;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCr() { return cr; }
    public void setCr(String cr) { this.cr = cr; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public int getArmorClass() { return armorClass; }
    public void setArmorClass(int armorClass) { this.armorClass = armorClass; }
    public Map<Ability, Integer> getAbilityScores() { return abilityScores; }
    public void setAbilityScores(Map<Ability, Integer> abilityScores) {
        this.abilityScores = new EnumMap<>(Ability.class);
        if (abilityScores != null) {
            this.abilityScores.putAll(abilityScores);
        }
    }
    public String getAttackName() { return attackName; }
    public void setAttackName(String attackName) { this.attackName = attackName; }
    public int getAttackBonus() { return attackBonus; }
    public void setAttackBonus(int attackBonus) { this.attackBonus = attackBonus; }
    public String getDamageDice() { return damageDice; }
    public void setDamageDice(String damageDice) { this.damageDice = damageDice; }
    public String getDamageType() { return damageType; }
    public void setDamageType(String damageType) { this.damageType = damageType; }
    public int getXpValue() { return xpValue; }
    public void setXpValue(int xpValue) { this.xpValue = xpValue; }
    public String getSpecialAbilityName() { return specialAbilityName; }
    public void setSpecialAbilityName(String specialAbilityName) { this.specialAbilityName = specialAbilityName; }
    public String getSpecialAbilityDescription() { return specialAbilityDescription; }
    public void setSpecialAbilityDescription(String specialAbilityDescription) { this.specialAbilityDescription = specialAbilityDescription; }
    public Condition getOnHitCondition() { return onHitCondition; }
    public void setOnHitCondition(Condition onHitCondition) { this.onHitCondition = onHitCondition; }
    public int getOnHitSaveDc() { return onHitSaveDc; }
    public void setOnHitSaveDc(int onHitSaveDc) { this.onHitSaveDc = onHitSaveDc; }
    public Ability getOnHitSaveAbility() { return onHitSaveAbility; }
    public void setOnHitSaveAbility(Ability onHitSaveAbility) { this.onHitSaveAbility = onHitSaveAbility; }
    public Set<Condition> getConditions() { return conditions; }
    public void setConditions(Set<Condition> conditions) {
        this.conditions = EnumSet.noneOf(Condition.class);
        if (conditions != null) {
            this.conditions.addAll(conditions);
        }
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
