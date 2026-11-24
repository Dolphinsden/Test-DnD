package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Item {

    public enum ItemType {
        WEAPON,
        ARMOR,
        SHIELD,
        CONSUMABLE,
        QUEST,
        GEAR
    }

    private String id;
    private String name;
    private ItemType type;
    private String description;
    private int quantity = 1;
    private int valueGold;

    // Weapon properties
    private String damageDice;
    private String damageType;
    private boolean finesse;
    private boolean ranged;
    private int magicBonus;

    // Armor/Shield properties
    private int baseAc;
    private boolean addDexToAc;
    private int maxDexBonus = 99;
    private int acBonus;

    // Consumable properties
    private String healingDice;

    public Item() {
    }

    public static Item weapon(String id, String name, String damageDice, String damageType,
                              boolean finesse, boolean ranged, int magicBonus, int valueGold, String description) {
        Item item = new Item();
        item.id = id;
        item.name = name;
        item.type = ItemType.WEAPON;
        item.damageDice = damageDice;
        item.damageType = damageType;
        item.finesse = finesse;
        item.ranged = ranged;
        item.magicBonus = magicBonus;
        item.valueGold = valueGold;
        item.description = description;
        return item;
    }

    public static Item armor(String id, String name, int baseAc, boolean addDexToAc,
                             int maxDexBonus, int valueGold, String description) {
        Item item = new Item();
        item.id = id;
        item.name = name;
        item.type = ItemType.ARMOR;
        item.baseAc = baseAc;
        item.addDexToAc = addDexToAc;
        item.maxDexBonus = maxDexBonus;
        item.valueGold = valueGold;
        item.description = description;
        return item;
    }

    public static Item shield(String id, String name, int acBonus, int valueGold, String description) {
        Item item = new Item();
        item.id = id;
        item.name = name;
        item.type = ItemType.SHIELD;
        item.acBonus = acBonus;
        item.valueGold = valueGold;
        item.description = description;
        return item;
    }

    public static Item potion(String id, String name, String healingDice, int quantity, int valueGold, String description) {
        Item item = new Item();
        item.id = id;
        item.name = name;
        item.type = ItemType.CONSUMABLE;
        item.healingDice = healingDice;
        item.quantity = quantity;
        item.valueGold = valueGold;
        item.description = description;
        return item;
    }

    public static Item questItem(String id, String name, String description) {
        Item item = new Item();
        item.id = id;
        item.name = name;
        item.type = ItemType.QUEST;
        item.quantity = 1;
        item.description = description;
        return item;
    }

    public static Item gear(String id, String name, int quantity, int valueGold, String description) {
        Item item = new Item();
        item.id = id;
        item.name = name;
        item.type = ItemType.GEAR;
        item.quantity = quantity;
        item.valueGold = valueGold;
        item.description = description;
        return item;
    }

    public Item copy() {
        Item c = new Item();
        c.id = id;
        c.name = name;
        c.type = type;
        c.description = description;
        c.quantity = quantity;
        c.valueGold = valueGold;
        c.damageDice = damageDice;
        c.damageType = damageType;
        c.finesse = finesse;
        c.ranged = ranged;
        c.magicBonus = magicBonus;
        c.baseAc = baseAc;
        c.addDexToAc = addDexToAc;
        c.maxDexBonus = maxDexBonus;
        c.acBonus = acBonus;
        c.healingDice = healingDice;
        return c;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getValueGold() { return valueGold; }
    public void setValueGold(int valueGold) { this.valueGold = valueGold; }
    public String getDamageDice() { return damageDice; }
    public void setDamageDice(String damageDice) { this.damageDice = damageDice; }
    public String getDamageType() { return damageType; }
    public void setDamageType(String damageType) { this.damageType = damageType; }
    public boolean isFinesse() { return finesse; }
    public void setFinesse(boolean finesse) { this.finesse = finesse; }
    public boolean isRanged() { return ranged; }
    public void setRanged(boolean ranged) { this.ranged = ranged; }
    public int getMagicBonus() { return magicBonus; }
    public void setMagicBonus(int magicBonus) { this.magicBonus = magicBonus; }
    public int getBaseAc() { return baseAc; }
    public void setBaseAc(int baseAc) { this.baseAc = baseAc; }
    public boolean isAddDexToAc() { return addDexToAc; }
    public void setAddDexToAc(boolean addDexToAc) { this.addDexToAc = addDexToAc; }
    public int getMaxDexBonus() { return maxDexBonus; }
    public void setMaxDexBonus(int maxDexBonus) { this.maxDexBonus = maxDexBonus; }
    public int getAcBonus() { return acBonus; }
    public void setAcBonus(int acBonus) { this.acBonus = acBonus; }
    public String getHealingDice() { return healingDice; }
    public void setHealingDice(String healingDice) { this.healingDice = healingDice; }
}
