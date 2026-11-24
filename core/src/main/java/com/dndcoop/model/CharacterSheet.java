package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterSheet {

    private String id;
    private String name;
    private String playerName;
    private String archetypeTitle;
    private Race race;
    private CharacterClass characterClass;
    private int level = 1;
    private int xp = 0;
    private String backstory = "";

    private Map<Ability, Integer> abilityScores = new EnumMap<>(Ability.class);
    private Set<Skill> skillProficiencies = EnumSet.noneOf(Skill.class);
    private Set<Ability> savingThrowProficiencies = EnumSet.noneOf(Ability.class);

    private int maxHp;
    private int currentHp;
    private int tempHp;
    private int speed = 30;
    private int hitDiceRemaining = 1;

    private int deathSaveSuccesses;
    private int deathSaveFailures;
    private boolean stable;
    private boolean dead;

    private int[] spellSlotsMax = new int[4];
    private int[] spellSlotsCurrent = new int[4];
    private List<String> knownSpellIds = new ArrayList<>();

    private List<Item> inventory = new ArrayList<>();
    private String equippedWeaponId;
    private String equippedArmorId;
    private String equippedShieldId;
    private int gold;

    private Set<Condition> conditions = EnumSet.noneOf(Condition.class);

    // Simple class resource tracking (Second Wind, Lay on Hands, Arcane Recovery, etc.)
    private String classResourceName;
    private int classResourceMax;
    private int classResourceCurrent;

    public CharacterSheet() {
        for (Ability a : Ability.values()) {
            abilityScores.put(a, 10);
        }
    }

    @JsonIgnore
    public int getAbilityScore(Ability ability) {
        return abilityScores.getOrDefault(ability, 10);
    }

    @JsonIgnore
    public int getAbilityModifier(Ability ability) {
        return Ability.modifierForScore(getAbilityScore(ability));
    }

    @JsonIgnore
    public int getProficiencyBonus() {
        return 2 + (Math.max(1, level) - 1) / 4;
    }

    @JsonIgnore
    public int getSkillModifier(Skill skill) {
        int base = getAbilityModifier(skill.getGoverningAbility());
        return skillProficiencies.contains(skill) ? base + getProficiencyBonus() : base;
    }

    @JsonIgnore
    public int getSavingThrowModifier(Ability ability) {
        int base = getAbilityModifier(ability);
        return savingThrowProficiencies.contains(ability) ? base + getProficiencyBonus() : base;
    }

    @JsonIgnore
    public int getInitiativeModifier() {
        return getAbilityModifier(Ability.DEX);
    }

    @JsonIgnore
    public int getArmorClass() {
        int dexMod = getAbilityModifier(Ability.DEX);
        int ac = 10 + dexMod;
        Item armor = findItemById(equippedArmorId);
        if (armor != null && armor.getType() == Item.ItemType.ARMOR) {
            ac = armor.getBaseAc();
            if (armor.isAddDexToAc()) {
                ac += Math.min(dexMod, armor.getMaxDexBonus());
            }
        }
        Item shield = findItemById(equippedShieldId);
        if (shield != null && shield.getType() == Item.ItemType.SHIELD) {
            ac += shield.getAcBonus();
        }
        if (conditions.contains(Condition.SHIELDED)) {
            ac += 5;
        }
        return ac;
    }

    @JsonIgnore
    public Item getEquippedWeapon() {
        Item weapon = findItemById(equippedWeaponId);
        if (weapon != null && weapon.getType() == Item.ItemType.WEAPON) {
            return weapon;
        }
        for (Item item : inventory) {
            if (item.getType() == Item.ItemType.WEAPON) {
                return item;
            }
        }
        return Item.weapon("unarmed", "Unarmed Strike", "1d4", "bludgeoning", false, false, 0, 0, "Bare hands.");
    }

    @JsonIgnore
    public Ability getWeaponAttackAbility(Item weapon) {
        if (weapon == null) {
            return Ability.STR;
        }
        if (weapon.isRanged()) {
            return Ability.DEX;
        }
        if (weapon.isFinesse()) {
            return getAbilityModifier(Ability.DEX) >= getAbilityModifier(Ability.STR) ? Ability.DEX : Ability.STR;
        }
        return Ability.STR;
    }

    @JsonIgnore
    public int getWeaponAttackBonus(Item weapon) {
        Ability ab = getWeaponAttackAbility(weapon);
        int magic = weapon != null ? weapon.getMagicBonus() : 0;
        return getAbilityModifier(ab) + getProficiencyBonus() + magic;
    }

    @JsonIgnore
    public int getWeaponDamageModifier(Item weapon) {
        Ability ab = getWeaponAttackAbility(weapon);
        int magic = weapon != null ? weapon.getMagicBonus() : 0;
        return getAbilityModifier(ab) + magic;
    }

    @JsonIgnore
    public int getSpellSaveDc() {
        if (characterClass == null || !characterClass.isSpellcaster()) {
            return 8 + getProficiencyBonus();
        }
        return 8 + getProficiencyBonus() + getAbilityModifier(characterClass.getSpellcastingAbility());
    }

    @JsonIgnore
    public int getSpellAttackBonus() {
        if (characterClass == null || !characterClass.isSpellcaster()) {
            return getProficiencyBonus();
        }
        return getProficiencyBonus() + getAbilityModifier(characterClass.getSpellcastingAbility());
    }

    @JsonIgnore
    public boolean hasSpellSlot(int spellLevel) {
        if (spellLevel <= 0) {
            return true;
        }
        return spellLevel < spellSlotsCurrent.length && spellSlotsCurrent[spellLevel] > 0;
    }

    public boolean consumeSpellSlot(int spellLevel) {
        if (spellLevel <= 0) {
            return true;
        }
        if (hasSpellSlot(spellLevel)) {
            spellSlotsCurrent[spellLevel]--;
            return true;
        }
        return false;
    }

    @JsonIgnore
    public boolean isConscious() {
        return !dead && currentHp > 0 && !conditions.contains(Condition.UNCONSCIOUS);
    }

    @JsonIgnore
    public Item findItemById(String itemId) {
        if (itemId == null) {
            return null;
        }
        for (Item item : inventory) {
            if (itemId.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    public void addItem(Item newItem) {
        if (newItem == null) {
            return;
        }
        for (Item existing : inventory) {
            if (existing.getId().equals(newItem.getId())
                    && (existing.getType() == Item.ItemType.CONSUMABLE || existing.getType() == Item.ItemType.GEAR)) {
                existing.setQuantity(existing.getQuantity() + newItem.getQuantity());
                return;
            }
        }
        inventory.add(newItem.copy());
    }

    public boolean removeOneItem(String itemId) {
        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            if (item.getId().equals(itemId)) {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                } else {
                    inventory.remove(i);
                }
                return true;
            }
        }
        return false;
    }

    public void applyDamage(int damage) {
        if (damage <= 0 || dead) {
            return;
        }
        int remaining = damage;
        if (tempHp > 0) {
            int absorbed = Math.min(tempHp, remaining);
            tempHp -= absorbed;
            remaining -= absorbed;
        }
        if (currentHp <= 0 && remaining > 0) {
            // Taking damage while unconscious causes 1 death save failure (5E rule)
            deathSaveFailures = Math.min(3, deathSaveFailures + 1);
            stable = false;
            if (deathSaveFailures >= 3) {
                dead = true;
            }
            return;
        }
        currentHp = Math.max(0, currentHp - remaining);
        if (currentHp == 0) {
            conditions.add(Condition.UNCONSCIOUS);
            conditions.add(Condition.PRONE);
            stable = false;
        }
    }

    public int applyHealing(int amount) {
        if (amount <= 0 || dead) {
            return 0;
        }
        int before = currentHp;
        currentHp = Math.min(maxHp, currentHp + amount);
        if (currentHp > 0) {
            conditions.remove(Condition.UNCONSCIOUS);
            deathSaveSuccesses = 0;
            deathSaveFailures = 0;
            stable = false;
        }
        return currentHp - before;
    }

    @JsonIgnore
    public String formatSpellSlotsSummary() {
        if (characterClass == null || !characterClass.isSpellcaster()) {
            return "None";
        }
        StringBuilder sb = new StringBuilder();
        for (int lvl = 1; lvl < spellSlotsMax.length; lvl++) {
            if (spellSlotsMax[lvl] > 0) {
                if (sb.length() > 0) {
                    sb.append(" | ");
                }
                sb.append("L").append(lvl).append(": ").append(spellSlotsCurrent[lvl]).append("/").append(spellSlotsMax[lvl]);
            }
        }
        return sb.length() == 0 ? "Cantrips" : sb.toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getArchetypeTitle() { return archetypeTitle; }
    public void setArchetypeTitle(String archetypeTitle) { this.archetypeTitle = archetypeTitle; }
    public Race getRace() { return race; }
    public void setRace(Race race) { this.race = race; }
    public CharacterClass getCharacterClass() { return characterClass; }
    public void setCharacterClass(CharacterClass characterClass) { this.characterClass = characterClass; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public String getBackstory() { return backstory; }
    public void setBackstory(String backstory) { this.backstory = backstory; }
    public Map<Ability, Integer> getAbilityScores() { return abilityScores; }
    public void setAbilityScores(Map<Ability, Integer> abilityScores) {
        this.abilityScores = new EnumMap<>(Ability.class);
        if (abilityScores != null) {
            this.abilityScores.putAll(abilityScores);
        }
    }
    public Set<Skill> getSkillProficiencies() { return skillProficiencies; }
    public void setSkillProficiencies(Set<Skill> skillProficiencies) {
        this.skillProficiencies = EnumSet.noneOf(Skill.class);
        if (skillProficiencies != null) {
            this.skillProficiencies.addAll(skillProficiencies);
        }
    }
    public Set<Ability> getSavingThrowProficiencies() { return savingThrowProficiencies; }
    public void setSavingThrowProficiencies(Set<Ability> savingThrowProficiencies) {
        this.savingThrowProficiencies = EnumSet.noneOf(Ability.class);
        if (savingThrowProficiencies != null) {
            this.savingThrowProficiencies.addAll(savingThrowProficiencies);
        }
    }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    public int getTempHp() { return tempHp; }
    public void setTempHp(int tempHp) { this.tempHp = tempHp; }
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
    public int getHitDiceRemaining() { return hitDiceRemaining; }
    public void setHitDiceRemaining(int hitDiceRemaining) { this.hitDiceRemaining = hitDiceRemaining; }
    public int getDeathSaveSuccesses() { return deathSaveSuccesses; }
    public void setDeathSaveSuccesses(int deathSaveSuccesses) { this.deathSaveSuccesses = deathSaveSuccesses; }
    public int getDeathSaveFailures() { return deathSaveFailures; }
    public void setDeathSaveFailures(int deathSaveFailures) { this.deathSaveFailures = deathSaveFailures; }
    public boolean isStable() { return stable; }
    public void setStable(boolean stable) { this.stable = stable; }
    public boolean isDead() { return dead; }
    public void setDead(boolean dead) { this.dead = dead; }
    public int[] getSpellSlotsMax() { return spellSlotsMax; }
    public void setSpellSlotsMax(int[] spellSlotsMax) { this.spellSlotsMax = spellSlotsMax != null ? spellSlotsMax : new int[4]; }
    public int[] getSpellSlotsCurrent() { return spellSlotsCurrent; }
    public void setSpellSlotsCurrent(int[] spellSlotsCurrent) { this.spellSlotsCurrent = spellSlotsCurrent != null ? spellSlotsCurrent : new int[4]; }
    public List<String> getKnownSpellIds() { return knownSpellIds; }
    public void setKnownSpellIds(List<String> knownSpellIds) { this.knownSpellIds = knownSpellIds != null ? new ArrayList<>(knownSpellIds) : new ArrayList<>(); }
    public List<Item> getInventory() { return inventory; }
    public void setInventory(List<Item> inventory) { this.inventory = inventory != null ? new ArrayList<>(inventory) : new ArrayList<>(); }
    public String getEquippedWeaponId() { return equippedWeaponId; }
    public void setEquippedWeaponId(String equippedWeaponId) { this.equippedWeaponId = equippedWeaponId; }
    public String getEquippedArmorId() { return equippedArmorId; }
    public void setEquippedArmorId(String equippedArmorId) { this.equippedArmorId = equippedArmorId; }
    public String getEquippedShieldId() { return equippedShieldId; }
    public void setEquippedShieldId(String equippedShieldId) { this.equippedShieldId = equippedShieldId; }
    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }
    public Set<Condition> getConditions() { return conditions; }
    public void setConditions(Set<Condition> conditions) {
        this.conditions = EnumSet.noneOf(Condition.class);
        if (conditions != null) {
            this.conditions.addAll(conditions);
        }
    }
    public String getClassResourceName() { return classResourceName; }
    public void setClassResourceName(String classResourceName) { this.classResourceName = classResourceName; }
    public int getClassResourceMax() { return classResourceMax; }
    public void setClassResourceMax(int classResourceMax) { this.classResourceMax = classResourceMax; }
    public int getClassResourceCurrent() { return classResourceCurrent; }
    public void setClassResourceCurrent(int classResourceCurrent) { this.classResourceCurrent = classResourceCurrent; }
}
