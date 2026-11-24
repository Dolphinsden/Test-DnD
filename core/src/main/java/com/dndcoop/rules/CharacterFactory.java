package com.dndcoop.rules;

import com.dndcoop.model.Ability;
import com.dndcoop.model.CharacterClass;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.Item;
import com.dndcoop.model.Race;
import com.dndcoop.model.Skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class CharacterFactory {

    private CharacterFactory() {
    }

    public static List<CharacterSheet> createPreMadeRoster() {
        List<CharacterSheet> roster = new ArrayList<>();
        roster.add(createThorneFighter());
        roster.add(createLyraWizard());
        roster.add(createPipRogue());
        roster.add(createSeraphinaCleric());
        roster.add(createKaelenRanger());
        roster.add(createGarrickPaladin());
        return roster;
    }

    public static CharacterSheet createThorneFighter() {
        CharacterSheet c = baseHero("hero_thorne", "Thorne Ironshield", "Player 1", "Dwarven Vanguard",
                Race.DWARF, CharacterClass.FIGHTER,
                stats(16, 12, 16, 10, 13, 8),
                EnumSet.of(Skill.ATHLETICS, Skill.INTIMIDATION, Skill.PERCEPTION, Skill.SURVIVAL),
                "A stout veteran of the Ironridge watch who never turns down a pint or a shield-wall. He came to Hollowbrook after hearing his cousin vanished near the old reliquary.");
        c.addItem(Item.weapon("warhammer", "Warhammer", "1d8", "bludgeoning", false, false, 0, 15, "Heavy dwarven warhammer forged in Ironridge."));
        c.addItem(Item.weapon("handaxe", "Throwing Handaxe", "1d6", "slashing", false, true, 0, 5, "Balanced iron handaxe."));
        c.addItem(Item.armor("chain_mail", "Chain Mail", 16, false, 0, 75, "Heavy interlocking iron rings (AC 16)."));
        c.addItem(Item.shield("iron_shield", "Oak & Iron Shield", 2, 10, "+2 AC when equipped."));
        c.addItem(Item.potion("healing_potion", "Potion of Healing", "2d4+2", 2, 50, "Restores 2d4+2 HP."));
        c.addItem(Item.gear("torch", "Torch", 3, 1, "Casts bright light for 20 feet."));
        c.setEquippedWeaponId("warhammer");
        c.setEquippedArmorId("chain_mail");
        c.setEquippedShieldId("iron_shield");
        c.setGold(25);
        c.setClassResourceName("Second Wind");
        c.setClassResourceMax(1);
        c.setClassResourceCurrent(1);
        return c;
    }

    public static CharacterSheet createLyraWizard() {
        CharacterSheet c = baseHero("hero_lyra", "Lyra Dawnwhisper", "Player 2", "High Elf Evoker",
                Race.ELF, CharacterClass.WIZARD,
                stats(8, 15, 13, 16, 12, 10),
                EnumSet.of(Skill.ARCANA, Skill.HISTORY, Skill.INVESTIGATION, Skill.PERCEPTION),
                "An arcane scholar from the Silver Spire obsessed with ancient necromantic wards. She suspects the Ashen Lantern holds a fragment of forgotten star-fire.");
        c.addItem(Item.weapon("quarterstaff", "Runed Quarterstaff", "1d6", "bludgeoning", false, false, 0, 10, "Carved silverwood staff."));
        c.addItem(Item.weapon("dagger", "Silvered Dagger", "1d4", "piercing", true, false, 0, 12, "Light etched blade."));
        c.addItem(Item.armor("mage_robes", "Arcane Robes", 11, true, 99, 20, "Reinforced scholar robes (AC 11 + DEX)."));
        c.addItem(Item.potion("healing_potion", "Potion of Healing", "2d4+2", 1, 50, "Restores 2d4+2 HP."));
        c.addItem(Item.gear("spellbook", "Leather Spellbook", 1, 50, "Contains arcane formulae and margin notes."));
        c.setEquippedWeaponId("quarterstaff");
        c.setEquippedArmorId("mage_robes");
        c.setKnownSpellIds(Arrays.asList("fire_bolt", "ray_of_frost", "magic_missile", "burning_hands", "shield", "thunderwave"));
        c.setGold(30);
        c.setClassResourceName("Arcane Recovery");
        c.setClassResourceMax(1);
        c.setClassResourceCurrent(1);
        return c;
    }

    public static CharacterSheet createPipRogue() {
        CharacterSheet c = baseHero("hero_pip", "Pip Quickfingers", "Player 3", "Halfling Shadowfoot",
                Race.HALFLING, CharacterClass.ROGUE,
                stats(8, 17, 13, 12, 10, 15),
                EnumSet.of(Skill.STEALTH, Skill.SLEIGHT_OF_HAND, Skill.ACROBATICS, Skill.DECEPTION, Skill.PERCEPTION, Skill.INVESTIGATION),
                "A cheerful lockpick and former tavern card-shark with an uncanny knack for dodging trouble—and finding every hidden chest in a dungeon.");
        c.addItem(Item.weapon("rapier", "Balanced Rapier", "1d8", "piercing", true, false, 0, 25, "Finesse dueling blade."));
        c.addItem(Item.weapon("shortbow", "Shortbow", "1d6", "piercing", false, true, 0, 25, "Quick nimble bow."));
        c.addItem(Item.armor("leather_armor", "Studded Leather", 12, true, 99, 45, "Supple dark leather (AC 12 + DEX)."));
        c.addItem(Item.potion("healing_potion", "Potion of Healing", "2d4+2", 1, 50, "Restores 2d4+2 HP."));
        c.addItem(Item.gear("thieves_tools", "Thieves' Tools", 1, 25, "Lockpicks, tension wrenches, and trap needles."));
        c.setEquippedWeaponId("rapier");
        c.setEquippedArmorId("leather_armor");
        c.setGold(35);
        c.setClassResourceName("Sneak Attack");
        c.setClassResourceMax(1);
        c.setClassResourceCurrent(1);
        return c;
    }

    public static CharacterSheet createSeraphinaCleric() {
        CharacterSheet c = baseHero("hero_seraphina", "Seraphina Vale", "Player 4", "Cleric of the Dawn",
                Race.HUMAN, CharacterClass.CLERIC,
                stats(14, 11, 15, 11, 16, 13),
                EnumSet.of(Skill.MEDICINE, Skill.RELIGION, Skill.INSIGHT, Skill.PERSUASION),
                "A warm-hearted healer of the Dawnbringer Abbey who traveled to Hollowbrook to banish the unnatural blight creeping out of the Whispering Mire.");
        c.addItem(Item.weapon("mace", "Sunburst Mace", "1d6", "bludgeoning", false, false, 0, 10, "Flanged brass mace blessed with dawn-oil."));
        c.addItem(Item.armor("scale_mail", "Scale Mail", 14, true, 2, 50, "Polished bronze scales (AC 14 + DEX max 2)."));
        c.addItem(Item.shield("holy_shield", "Dawn Emblem Shield", 2, 15, "+2 AC; doubles as a holy focus."));
        c.addItem(Item.potion("healing_potion", "Potion of Healing", "2d4+2", 2, 50, "Restores 2d4+2 HP."));
        c.setEquippedWeaponId("mace");
        c.setEquippedArmorId("scale_mail");
        c.setEquippedShieldId("holy_shield");
        c.setKnownSpellIds(Arrays.asList("sacred_flame", "guidance", "cure_wounds", "healing_word", "guiding_bolt", "bless"));
        c.setGold(25);
        c.setClassResourceName("Channel Divinity");
        c.setClassResourceMax(1);
        c.setClassResourceCurrent(1);
        return c;
    }

    public static CharacterSheet createKaelenRanger() {
        CharacterSheet c = baseHero("hero_kaelen", "Kaelen Mossstride", "Player 5", "Mireward Ranger",
                Race.ELF, CharacterClass.RANGER,
                stats(12, 16, 14, 10, 15, 8),
                EnumSet.of(Skill.SURVIVAL, Skill.NATURE, Skill.PERCEPTION, Skill.STEALTH, Skill.ANIMAL_HANDLING),
                "A silent woodsman who knows every bog trail and goblin ambush ridge around Hollowbrook. Speaks little, misses less.");
        c.addItem(Item.weapon("longbow", "Yew Longbow", "1d8", "piercing", false, true, 0, 50, "Long-range elven hunting bow."));
        c.addItem(Item.weapon("shortsword", "Twin Shortsword", "1d6", "piercing", true, false, 0, 10, "Light forest blade."));
        c.addItem(Item.armor("studded_leather", "Forest Studded Leather", 12, true, 99, 45, "Camouflaged leather (AC 12 + DEX)."));
        c.addItem(Item.potion("healing_potion", "Potion of Healing", "2d4+2", 1, 50, "Restores 2d4+2 HP."));
        c.setEquippedWeaponId("longbow");
        c.setEquippedArmorId("studded_leather");
        c.setKnownSpellIds(Arrays.asList("hunters_mark", "cure_wounds"));
        c.setGold(20);
        c.setClassResourceName("Favored Mark");
        c.setClassResourceMax(1);
        c.setClassResourceCurrent(1);
        return c;
    }

    public static CharacterSheet createGarrickPaladin() {
        CharacterSheet c = baseHero("hero_garrick", "Sir Garrick Oathbound", "Player 6", "Oath of Devotion Paladin",
                Race.HUMAN, CharacterClass.PALADIN,
                stats(16, 10, 15, 9, 12, 15),
                EnumSet.of(Skill.ATHLETICS, Skill.PERSUASION, Skill.RELIGION, Skill.INSIGHT),
                "A resolute knight sworn to protect border villages from the creeping dark. His blade gleams brightest when allies stand shoulder to shoulder.");
        c.addItem(Item.weapon("longsword", "Knight's Longsword", "1d8", "slashing", false, false, 0, 15, "Tempered steel blade inscribed with an oath."));
        c.addItem(Item.armor("chain_mail_pal", "Burnished Chain Mail", 16, false, 0, 75, "Heavy knightly chain mail (AC 16)."));
        c.addItem(Item.shield("kite_shield", "Silver Kite Shield", 2, 15, "+2 AC."));
        c.addItem(Item.potion("healing_potion", "Potion of Healing", "2d4+2", 1, 50, "Restores 2d4+2 HP."));
        c.setEquippedWeaponId("longsword");
        c.setEquippedArmorId("chain_mail_pal");
        c.setEquippedShieldId("kite_shield");
        c.setKnownSpellIds(Arrays.asList("divine_smite", "cure_wounds", "bless"));
        c.setGold(25);
        c.setClassResourceName("Lay on Hands HP");
        c.setClassResourceMax(5);
        c.setClassResourceCurrent(5);
        return c;
    }

    private static CharacterSheet baseHero(String id, String name, String playerName, String archetype,
                                           Race race, CharacterClass cls, Map<Ability, Integer> scores,
                                           EnumSet<Skill> skills, String backstory) {
        CharacterSheet c = new CharacterSheet();
        c.setId(id);
        c.setName(name);
        c.setPlayerName(playerName);
        c.setArchetypeTitle(archetype);
        c.setRace(race);
        c.setCharacterClass(cls);
        c.setLevel(1);
        c.setXp(0);
        c.setBackstory(backstory);
        c.setAbilityScores(scores);
        c.setSkillProficiencies(skills);
        c.setSavingThrowProficiencies(cls.getSavingThrowProficiencies());
        c.setSpeed(race.getSpeed());

        int maxHp = Math.max(4, cls.getHitDie() + c.getAbilityModifier(Ability.CON));
        c.setMaxHp(maxHp);
        c.setCurrentHp(maxHp);
        c.setHitDiceRemaining(1);

        int[] slots = cls.getSpellSlotsForLevel(1);
        c.setSpellSlotsMax(slots);
        c.setSpellSlotsCurrent(slots.clone());
        return c;
    }

    private static Map<Ability, Integer> stats(int str, int dex, int con, int intel, int wis, int cha) {
        Map<Ability, Integer> map = new EnumMap<>(Ability.class);
        map.put(Ability.STR, str);
        map.put(Ability.DEX, dex);
        map.put(Ability.CON, con);
        map.put(Ability.INT, intel);
        map.put(Ability.WIS, wis);
        map.put(Ability.CHA, cha);
        return map;
    }
}
