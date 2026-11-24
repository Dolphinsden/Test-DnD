package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PendingRoll {

    public enum RollType {
        ABILITY_CHECK,
        SAVING_THROW,
        INITIATIVE,
        ATTACK_ROLL,
        DAMAGE_ROLL,
        SPELL_ATTACK,
        DEATH_SAVE,
        HEAL_ROLL
    }

    private String id;
    private String actorId;
    private String actorName;
    private RollType rollType;
    private String label;
    private String diceNotation = "1d20";
    private int modifier;
    private Integer targetDcOrAc;
    private boolean advantage;
    private boolean disadvantage;
    private boolean criticalHit;

    private String contextActionText;
    private String targetId;
    private String targetName;
    private String spellId;
    private String weaponId;
    private Skill skill;
    private Ability ability;

    private String successNarrative;
    private String failureNarrative;
    private String flagOnSuccess;
    private int xpOnSuccess;
    private int goldOnSuccess;
    private String rewardItemOnSuccess;
    private String moveLocationOnSuccess;
    private String startEncounterOnFailure;
    private String damageType;
    private boolean sneakAttackEligible;
    private boolean divineSmite;

    public PendingRoll() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public RollType getRollType() { return rollType; }
    public void setRollType(RollType rollType) { this.rollType = rollType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDiceNotation() { return diceNotation; }
    public void setDiceNotation(String diceNotation) { this.diceNotation = diceNotation; }
    public int getModifier() { return modifier; }
    public void setModifier(int modifier) { this.modifier = modifier; }
    public Integer getTargetDcOrAc() { return targetDcOrAc; }
    public void setTargetDcOrAc(Integer targetDcOrAc) { this.targetDcOrAc = targetDcOrAc; }
    public boolean isAdvantage() { return advantage; }
    public void setAdvantage(boolean advantage) { this.advantage = advantage; }
    public boolean isDisadvantage() { return disadvantage; }
    public void setDisadvantage(boolean disadvantage) { this.disadvantage = disadvantage; }
    public boolean isCriticalHit() { return criticalHit; }
    public void setCriticalHit(boolean criticalHit) { this.criticalHit = criticalHit; }
    public String getContextActionText() { return contextActionText; }
    public void setContextActionText(String contextActionText) { this.contextActionText = contextActionText; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getSpellId() { return spellId; }
    public void setSpellId(String spellId) { this.spellId = spellId; }
    public String getWeaponId() { return weaponId; }
    public void setWeaponId(String weaponId) { this.weaponId = weaponId; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public Ability getAbility() { return ability; }
    public void setAbility(Ability ability) { this.ability = ability; }
    public String getSuccessNarrative() { return successNarrative; }
    public void setSuccessNarrative(String successNarrative) { this.successNarrative = successNarrative; }
    public String getFailureNarrative() { return failureNarrative; }
    public void setFailureNarrative(String failureNarrative) { this.failureNarrative = failureNarrative; }
    public String getFlagOnSuccess() { return flagOnSuccess; }
    public void setFlagOnSuccess(String flagOnSuccess) { this.flagOnSuccess = flagOnSuccess; }
    public int getXpOnSuccess() { return xpOnSuccess; }
    public void setXpOnSuccess(int xpOnSuccess) { this.xpOnSuccess = xpOnSuccess; }
    public int getGoldOnSuccess() { return goldOnSuccess; }
    public void setGoldOnSuccess(int goldOnSuccess) { this.goldOnSuccess = goldOnSuccess; }
    public String getRewardItemOnSuccess() { return rewardItemOnSuccess; }
    public void setRewardItemOnSuccess(String rewardItemOnSuccess) { this.rewardItemOnSuccess = rewardItemOnSuccess; }
    public String getMoveLocationOnSuccess() { return moveLocationOnSuccess; }
    public void setMoveLocationOnSuccess(String moveLocationOnSuccess) { this.moveLocationOnSuccess = moveLocationOnSuccess; }
    public String getStartEncounterOnFailure() { return startEncounterOnFailure; }
    public void setStartEncounterOnFailure(String startEncounterOnFailure) { this.startEncounterOnFailure = startEncounterOnFailure; }
    public String getDamageType() { return damageType; }
    public void setDamageType(String damageType) { this.damageType = damageType; }
    public boolean isSneakAttackEligible() { return sneakAttackEligible; }
    public void setSneakAttackEligible(boolean sneakAttackEligible) { this.sneakAttackEligible = sneakAttackEligible; }
    public boolean isDivineSmite() { return divineSmite; }
    public void setDivineSmite(boolean divineSmite) { this.divineSmite = divineSmite; }
}
