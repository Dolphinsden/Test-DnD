package com.dndcoop.campaign;

import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.Condition;
import com.dndcoop.model.Item;
import com.dndcoop.model.Monster;
import com.dndcoop.model.Skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshenLanternCampaign {

    public static final String CAMPAIGN_TITLE = "The Ashen Lantern of Hollowbrook";
    public static final String START_LOCATION_ID = "gilded_stag_tavern";

    private static final Map<String, LocationNode> LOCATIONS = new LinkedHashMap<>();
    private static final Map<String, EncounterTemplate> ENCOUNTERS = new LinkedHashMap<>();

    static {
        initEncounters();
        initLocations();
    }

    private AshenLanternCampaign() {
    }

    private static void initEncounters() {
        // Act II Encounter 1: Goblin Ambush
        Monster gob1 = monster("goblin_raider", "Goblin Ambush Archer", "1/4", 9, 14,
                stats(8, 14, 10, 10, 8, 8), "Shortbow", 4, "1d6+2", "piercing", 50,
                null, 0, null, "A wiry goblin perched on a cypress root.");
        Monster gob2 = monster("goblin_skirmisher", "Goblin Mire-Blade", "1/4", 10, 14,
                stats(10, 14, 10, 8, 8, 8), "Rusty Scimitar", 4, "1d6+2", "slashing", 50,
                null, 0, null, "Splattered in bog mud and grinning wickedly.");
        Monster gobBoss = monster("goblin_boss", "Snarltooth the Torch-Thief", "1/2", 18, 15,
                stats(12, 14, 12, 10, 10, 10), "Jagged Cleaver", 5, "1d8+2", "slashing", 100,
                null, 0, null, "Wears a stolen brass lantern-ring around his neck.");
        ENCOUNTERS.put("goblin_ambush", new EncounterTemplate(
                "goblin_ambush",
                "Ambush on the Sunken Boardwalk",
                "Arrows whistle out of the hanging moss! Three bog-goblins leap onto the rotting timber walkway, led by Snarltooth, waving a stolen village lantern-hook!",
                Arrays.asList(gob1, gob2, gobBoss),
                "cleared_goblin_ambush", 45, "goblin_map"
        ));

        // Act II Encounter 2: Mire Guardians at Reliquary Steps
        Monster stalker = monster("mire_stalker", "Mire Stalker Hound", "1/2", 16, 13,
                stats(14, 14, 12, 6, 12, 6), "Venomous Bite", 4, "1d8+2", "piercing", 100,
                Condition.POISONED, 11, Ability.CON, "A gaunt shadow-hound dripping violet marsh-ichor.");
        Monster shambler = monster("bog_shambler", "Cinder-Bound Bog Shambler", "1", 28, 13,
                stats(16, 8, 16, 5, 10, 5), "Rotting Slam", 5, "2d6+2", "bludgeoning", 200,
                Condition.GRAPPLED, 12, Ability.STR, "A hulking tangle of roots animated by ash-magic.");
        ENCOUNTERS.put("mire_guardians", new EncounterTemplate(
                "mire_guardians",
                "Guardians of the Sunken Steps",
                "As you approach the submerged stone arch, the mud churns violently! A towering Bog Shambler rises from the reeds while a venomous Mire Stalker circles your flank!",
                Arrays.asList(stalker, shambler),
                "cleared_mire_guardians", 60, "healing_potion"
        ));

        // Act III Encounter: Reliquary Patrol
        Monster skel1 = monster("reliquary_skeleton", "Ashen Skeleton Guard", "1/4", 13, 13,
                stats(10, 14, 14, 6, 8, 5), "Notched Shortsword", 4, "1d6+2", "slashing", 50,
                null, 0, null, "Ancient warden bones held together by glowing embers.");
        Monster skel2 = monster("reliquary_archer", "Ashen Skeleton Bowman", "1/4", 12, 13,
                stats(10, 14, 12, 6, 8, 5), "Bone Longbow", 4, "1d8+2", "piercing", 50,
                null, 0, null, "Takes aim from behind a cracked stone sarcophagus.");
        Monster sentinel = monster("crypt_sentinel", "Oath-Broken Crypt Knight", "1", 26, 16,
                stats(16, 10, 14, 8, 10, 10), "Cinder Greatsword", 5, "1d10+3", "slashing", 200,
                Condition.FRIGHTENED, 12, Ability.WIS, "Armored guardian clutching the Bronze Sun-Key at its belt.");
        ENCOUNTERS.put("reliquary_patrol", new EncounterTemplate(
                "reliquary_patrol",
                "The Ossuary Watch",
                "Blue-violet fire ignites inside hollow eye sockets! Skeletal wardens step from the alcoves, led by an armored Crypt Knight bearing the Bronze Sun-Key!",
                Arrays.asList(skel1, skel2, sentinel),
                "cleared_ossuary_patrol", 80, "bronze_sun_key"
        ));

        // Act IV Boss: Malakor the Cinder Warden
        Monster wraith1 = monster("ash_wraith", "Ash Wraith Acolyte", "1/2", 18, 13,
                stats(8, 14, 12, 10, 10, 12), "Chilling Touch", 5, "1d8+2", "necrotic", 100,
                Condition.FRIGHTENED, 12, Ability.WIS, "Drifting silhouette woven from furnace smoke.");
        Monster malakor = monster("malakor_boss", "Malakor, the Cinder Warden", "3", 52, 16,
                stats(16, 13, 16, 16, 14, 16), "Searing Ash-Staff", 6, "2d8+3", "fire", 450,
                Condition.FRIGHTENED, 13, Ability.WIS, "A fallen archivist draped in gilded ash-robes, drawing starlight from the Ashen Lantern.");
        ENCOUNTERS.put("boss_malakor", new EncounterTemplate(
                "boss_malakor",
                "Showdown in the Ashen Sanctum",
                "\"The dawn of Hollowbrook belongs to the embers now!\" Malakor lifts the stolen Ashen Lantern overhead as violet lightning arcs across the vaulted ceiling, summoning an Ash Wraith to his side!",
                Arrays.asList(malakor, wraith1),
                "defeated_malakor", 200, "ashen_lantern"
        ));
    }

    private static void initLocations() {
        // ACT I
        LocationNode tavern = new LocationNode(
                "gilded_stag_tavern", "The Gilded Stag Tavern", 1,
                "Warm firelight dances across timber beams as rain lashes the leaded windows of Hollowbrook's favorite tavern. A crackling hearth dries traveling cloaks, yet the locals speak in hushed tones: three nights ago, the village's protective Ashen Lantern vanished from the square, and cold bog-mist has been creeping closer ever since.",
                "Talk to Martha the Innkeeper or Aldric the Apothecary, inspect the noticeboard, rest by the hearth, or head outside to Hollowbrook Square.",
                true)
                .addExit("hollowbrook_square", "Step outside to Hollowbrook Village Square")
                .addNpc(new NpcProfile("martha", "Martha Brindle", "Tavern Keeper",
                        "\"Thank the Dawn you've come! Without the Ashen Lantern, shadows from the Whispering Mire are creeping up to our doors. Captain Rowan found goblin tracks by the broken pedestal in the square. Bring the lantern back and drinks are on the house for life!\"",
                        "Martha leans in and whispers: \"Before it vanished, a tall hooded stranger bought every vial of grave-ash from Aldric and asked about the Sunken Reliquary north of the mire. Take this extra healing potion—you'll need it.\"",
                        Skill.PERSUASION, 11, "martha_secret", "healing_potion"))
                .addNpc(new NpcProfile("aldric", "Aldric Vane", "Village Apothecary",
                        "\"The lantern wasn't merely brass and wick—it held a spark of Dawnfire that keeps undead slumbering beneath the Sunken Reliquary. Whoever stole it knows ancient wards.\"",
                        "Aldric recognizes your sharp questions and hands over a blessed chalk talisman: \"Draw this symbol if you face the Cinder Warden—it disrupts necrotic focus!\"",
                        Skill.INSIGHT, 12, "aldric_ward_clue", "dawn_amulet"))
                .withDiscovery(Skill.INVESTIGATION, 11, "tavern_noticeboard",
                        "Pinned beneath a dagger on the noticeboard is a charcoal sketch of the Whispering Mire showing a forgotten Elven Weeping Shrine off the eastern boardwalk—a safe haven with old magic.",
                        "You skim the tavern noticeboard—mostly lost sheep bounties and ale prices, though a crumpled map corner catches your eye.",
                        null, 10, 40);

        LocationNode square = new LocationNode(
                "hollowbrook_square", "Hollowbrook Village Square", 1,
                "Cobblestones glisten under heavy mist. At the center stands an empty iron lantern-pedestal, its stone base scorched by violet flames. Lantern-bearing guards peer nervously north toward the treeline of the Whispering Mire.",
                "Speak with Captain Rowan, examine the scorched pedestal and tracks, return to the tavern, or march north into the Whispering Mire.",
                true)
                .addExit("gilded_stag_tavern", "Return inside The Gilded Stag Tavern")
                .addExit("mire_boardwalk", "Head north into the Whispering Mire (Act II)")
                .addNpc(new NpcProfile("rowan", "Captain Rowan", "Hollowbrook Watch Captain",
                        "\"Hold steady, adventurers. Goblins raided the square under cover of unnatural fog, likely hired by something worse out in the old reliquary. Hollowbrook offers 100 gold to whoever returns our Ashen Lantern.\"",
                        "Rowan salutes and slips you a spare flask of lantern-oil and tactical advice: \"Watch the sunken boardwalk—goblins love firing from the cypress roots!\"",
                        Skill.PERSUASION, 12, "rowan_blessing", "healing_potion"))
                .withDiscovery(Skill.SURVIVAL, 12, "square_tracks_found",
                        "Tracking the muddy bootprints and tiny clawed tracks north, you spot dropped iron caltrops and a charred crow feather—giving you sharp warning of an ambush along the Mire Boardwalk!",
                        "The rain has blurred most tracks around the pedestal, though muddy smears clearly lead north into the swamp.",
                        null, 0, 50);

        // ACT II
        LocationNode boardwalk = new LocationNode(
                "mire_boardwalk", "The Whispering Mire — Sunken Boardwalk", 2,
                "Creaking planks wind through waist-deep black water and drooping willow curtains. Strange lantern-flies drift like pale embers above the peat. Ahead, the path forks: east toward mossy elven ruins and north toward ancient sunken steps.",
                "Scan the cypress roots for ambushers, head east to the Weeping Shrine, push north to the Reliquary Steps, or retreat south to Hollowbrook.",
                false)
                .addExit("hollowbrook_square", "Retreat south to Hollowbrook Village Square")
                .addExit("weeping_shrine", "Follow the mossy stones east to the Weeping Shrine")
                .addExit("reliquary_steps", "Press north to the Steps of the Sunken Reliquary")
                .withEncounter("goblin_ambush")
                .withDiscovery(Skill.PERCEPTION, 13, "spotted_mire_cache",
                        "Beneath a loose plank off the boardwalk you spot a goblin stash: a waterproof pouch of silver coins and a gleaming Healing Potion!",
                        "You peer through the swirling marsh fog, seeing only gnarled roots and bubbling peat.",
                        "healing_potion", 25, 50);

        LocationNode shrine = new LocationNode(
                "weeping_shrine", "The Overgrown Weeping Shrine", 2,
                "A serene marble statue of a hooded elf cradles an empty basin beneath a dry canopy of silver-barked birch. Pale runes shimmer faintly around the plinth, keeping the swamp's chill at bay.",
                "Inspect or cleanse the ancient runes (Arcana/Religion), take a quiet rest, or return west to the Sunken Boardwalk.",
                true)
                .addExit("mire_boardwalk", "Return west to the Sunken Boardwalk")
                .withDiscovery(Skill.ARCANA, 12, "shrine_cleansed",
                        "Tracing the faded starlight runes, you rekindle a quiet golden chime! A hidden compartment beneath the basin slides open, revealing a Dawnblade Shortsword (+1) and filling the party with resolve!",
                        "The runes flicker briefly under your touch before dimming—perhaps another hero versed in Arcana or Religion could awaken them.",
                        "dawnblade", 20, 100);

        LocationNode steps = new LocationNode(
                "reliquary_steps", "Steps of the Sunken Reliquary", 2,
                "Moss-draped limestone stairs descend into a half-flooded temple facade carved with sun-and-lantern reliefs. Violet soot stains the archway where someone recently forced entry into the Hall of Cinders.",
                "Descend into the Hall of Cinders (Act III), investigate the carved reliefs, or fall back south to the Mire Boardwalk.",
                false)
                .addExit("mire_boardwalk", "Fall back south to the Sunken Boardwalk")
                .addExit("hall_of_cinders", "Descend into the Hall of Cinders (Act III)")
                .withEncounter("mire_guardians")
                .withDiscovery(Skill.HISTORY, 12, "reliquary_lore_read",
                        "Deciphering the sun-reliefs reveals the Reliquary's builder—Archivist Malakor, sealed away centuries ago for trying to drink the Lantern's starlight. The reliefs warn of dart traps inside the Hall of Cinders!",
                        "The weathered carvings depict robed figures bearing lanterns, though moss obscures the ancient script.",
                        null, 0, 60);

        // ACT III
        LocationNode hall = new LocationNode(
                "hall_of_cinders", "Hall of Cinders", 3,
                "Cold braziers line a grand vaulted stone hall. Ash drifts lazily down from cracks above. Three iron doors branch off the hall: east into the rattling Ossuary Cloister, west to the heavily padlocked Warden's Vault, and north to the colossal Sun-Sealed Sanctum Gate.",
                "Check the floor flagstones for traps (Perception/Sleight of Hand), explore the Ossuary east, try the Warden's Vault west, or unseal the northern Sanctum Gate (requires Bronze Sun-Key).",
                false)
                .addExit("reliquary_steps", "Climb back out to the Reliquary Steps")
                .addExit("ossuary_cloister", "Enter the Ossuary Cloister to the east")
                .addExit("warden_vault", "Approach the Warden's Vault to the west")
                .addLockedExit("ashen_sanctum", "Unseal the Northern Gate into the Ashen Sanctum (Act IV)", "has_sun_key")
                .withDiscovery(Skill.PERCEPTION, 13, "hall_trap_disarmed",
                        "Sharp eyes spot a hair-thin tripwire linked to wall dart vents! You deftly jam the iron gear with a piton—making the hall safe and uncovering a tucked-away ruby pouch!",
                        "The flagstones look dusty and uneven—step carefully near the braziers.",
                        null, 35, 80);

        LocationNode ossuary = new LocationNode(
                "ossuary_cloister", "The Ossuary Cloister", 3,
                "Rows of stone alcoves hold the resting bones of Hollowbrook's ancient wardens. At the far end, an iron reliquary rack glints with a heavy Bronze Sun-Key bathed in eerie violet mist.",
                "Search the alcoves for clues or loot, or return west to the Hall of Cinders.",
                false)
                .addExit("hall_of_cinders", "Return west to the Hall of Cinders")
                .withEncounter("reliquary_patrol")
                .withDiscovery(Skill.RELIGION, 12, "ossuary_blessing",
                        "You offer a respectful prayer over the fallen wardens. A warm golden aura settles over the party, mending bruised spirit and revealing a hidden cache of Potions of Healing!",
                        "The alcoves remain solemn and still beneath thick layers of grey dust.",
                        "healing_potion", 30, 80);

        LocationNode vault = new LocationNode(
                "warden_vault", "The Warden's Vault", 3,
                "A stout iron-banded chamber lined with velvet weapon racks and ancient ledgers. One chest sits chained with a tarnished brass lock, humming faintly with dawn-magic.",
                "Pick the chained chest lock (Sleight of Hand) or smash it open (Athletics), read the ledger, or head east back to the Hall of Cinders.",
                true)
                .addExit("hall_of_cinders", "Return east to the Hall of Cinders")
                .withDiscovery(Skill.SLEIGHT_OF_HAND, 13, "vault_chest_opened",
                        "Click! The ancient tumblers yield smoothly. Inside lies the Sunforged Warhammer (+1), gleaming with warm dawnlight, plus a velvet sack of 60 gold pieces!",
                        "The rusted brass lock resists your first attempt—keep at it or try sheer strength!",
                        "sunforged_warhammer", 60, 100);

        // ACT IV
        LocationNode sanctum = new LocationNode(
                "ashen_sanctum", "The Ashen Sanctum", 4,
                "The Bronze Sun-Key turns with a resonant boom! Beyond lies a circular obsidian cathedral. Suspended above a blazing brazier floats the Ashen Lantern of Hollowbrook, its golden fire twisted violet by the robed figure standing before it—Malakor, the Cinder Warden!",
                "Confront Malakor, try disrupting his ritual (Arcana/Religion) before striking, or fight to reclaim the Ashen Lantern!",
                false)
                .addExit("hall_of_cinders", "Fall back south to the Hall of Cinders")
                .addLockedExit("lantern_altar", "Ascend to the Rekindled Spire with the Ashen Lantern", "defeated_malakor")
                .withEncounter("boss_malakor")
                .withDiscovery(Skill.RELIGION, 13, "sanctum_ritual_disrupted",
                        "Chanting counter-verses of the Dawn, you fracture the ash-circle etched around the brazier—weakening the sanctum's dark resonance and inspiring the party!",
                        "The violet flames roar defiantly against your attempt to disrupt the circle.",
                        null, 0, 100);

        LocationNode altar = new LocationNode(
                "lantern_altar", "The Rekindled Spire", 4,
                "With Malakor vanquished, the violet haze dissolves into pure golden starlight! High above the Whispering Mire, dawn rays pierce the clouds as the Ashen Lantern warms in your hands. Hollowbrook's bells ring faintly in the distance.",
                "Claim the Ashen Lantern and return in triumph to Hollowbrook to complete the campaign, or share final roleplay words around the spire!",
                true)
                .addExit("ashen_sanctum", "Descend to the Ashen Sanctum")
                .addExit("gilded_stag_tavern", "Return triumphantly to The Gilded Stag Tavern");

        register(tavern, square, boardwalk, shrine, steps, hall, ossuary, vault, sanctum, altar);
    }

    private static void register(LocationNode... nodes) {
        for (LocationNode n : nodes) {
            LOCATIONS.put(n.getId(), n);
        }
    }

    public static LocationNode getLocation(String id) {
        return LOCATIONS.getOrDefault(id, LOCATIONS.get(START_LOCATION_ID));
    }

    public static EncounterTemplate getEncounter(String id) {
        return id == null ? null : ENCOUNTERS.get(id);
    }

    public static List<LocationNode> allLocations() {
        return Collections.unmodifiableList(new ArrayList<>(LOCATIONS.values()));
    }

    public static Item createRewardItem(String itemId) {
        if (itemId == null) return null;
        switch (itemId) {
            case "healing_potion":
                return Item.potion("healing_potion", "Potion of Healing", "2d4+2", 1, 50, "Restores 2d4+2 HP.");
            case "goblin_map":
                return Item.questItem("goblin_map", "Snarltooth's Crude Map", "Shows goblin marks pointing toward the Sunken Reliquary and the Ossuary Cloister.");
            case "bronze_sun_key":
                return Item.questItem("bronze_sun_key", "Bronze Sun-Key", "Heavy sun-shaped key that unseals the Northern Gate to the Ashen Sanctum.");
            case "dawn_amulet":
                return Item.questItem("dawn_amulet", "Aldric's Dawn Talisman", "Chalk-etched talisman glowing warmly near undead.");
            case "dawnblade":
                return Item.weapon("dawnblade", "Dawnblade Shortsword +1", "1d6", "piercing", true, false, 1, 250, "Elven shortsword (+1 attack & damage) glimmering with pale starlight.");
            case "sunforged_warhammer":
                return Item.weapon("sunforged_warhammer", "Sunforged Warhammer +1", "1d8", "bludgeoning", false, false, 1, 300, "Radiant warhammer (+1 attack & damage) forged by the First Wardens.");
            case "ashen_lantern":
                return Item.questItem("ashen_lantern", "The Ashen Lantern of Hollowbrook", "Relic lantern blazing with warm golden Dawnfire.");
            default:
                return null;
        }
    }

    public static List<String> getQuestJournalLines(CampaignState state) {
        List<String> lines = new ArrayList<>();
        boolean talked = state.hasFlag("martha_secret") || state.getVisitedLocations().contains("hollowbrook_square");
        boolean clearedGoblins = state.hasFlag("cleared_goblin_ambush");
        boolean hasKey = state.hasFlag("has_sun_key") || state.hasFlag("cleared_ossuary_patrol");
        boolean beatBoss = state.hasFlag("defeated_malakor");

        lines.add((talked ? "[DONE] " : "[ACTIVE] ") + "Act I: Investigate Hollowbrook & learn who stole the Ashen Lantern.");
        lines.add((clearedGoblins ? "[DONE] " : (talked ? "[ACTIVE] " : "[LOCKED] "))
                + "Act II: Brave the Whispering Mire & track the raiders to the Sunken Reliquary.");
        lines.add((hasKey ? "[DONE] " : (clearedGoblins ? "[ACTIVE] " : "[LOCKED] "))
                + "Act III: Explore the Sunken Reliquary & claim the Bronze Sun-Key in the Ossuary.");
        lines.add((beatBoss ? "[DONE] " : (hasKey ? "[ACTIVE] " : "[LOCKED] "))
                + "Act IV: Unseal the Ashen Sanctum, defeat Malakor, and reclaim the Ashen Lantern.");
        if (beatBoss) {
            lines.add((state.isCampaignCompleted() ? "[DONE] " : "[ACTIVE] ")
                    + "Epilogue: Ascend the Rekindled Spire or return to Hollowbrook in triumph!");
        }
        return lines;
    }

    public static String buildOpeningNarration(CampaignState state) {
        LocationNode start = getLocation(START_LOCATION_ID);
        StringBuilder partyNames = new StringBuilder();
        for (int i = 0; i < state.getParty().size(); i++) {
            if (i > 0) partyNames.append(i == state.getParty().size() - 1 ? " and " : ", ");
            partyNames.append(state.getParty().get(i).getName());
        }
        return "Welcome to " + CAMPAIGN_TITLE + "!\n\n"
                + start.getDescription() + "\n\n"
                + "Gathered around a scarred oak booth sit " + partyNames + ". "
                + "Martha Brindle sets down fresh mugs of cider with a trembling hand and looks to your party for aid.\n\n"
                + "Type any free-form action below (for example: \"I ask Martha what happened to the lantern\", "
                + "\"I inspect the noticeboard\", or \"We head out to the village square\").";
    }

    private static Monster monster(String templateId, String name, String cr, int hp, int ac,
                                   Map<Ability, Integer> scores, String atkName, int atkBonus,
                                   String dmgDice, String dmgType, int xp,
                                   Condition onHitCond, int saveDc, Ability saveAb, String desc) {
        Monster m = new Monster();
        m.setTemplateId(templateId);
        m.setName(name);
        m.setCr(cr);
        m.setMaxHp(hp);
        m.setCurrentHp(hp);
        m.setArmorClass(ac);
        m.setAbilityScores(scores);
        m.setAttackName(atkName);
        m.setAttackBonus(atkBonus);
        m.setDamageDice(dmgDice);
        m.setDamageType(dmgType);
        m.setXpValue(xp);
        m.setOnHitCondition(onHitCond);
        m.setOnHitSaveDc(saveDc);
        m.setOnHitSaveAbility(saveAb);
        m.setDescription(desc);
        return m;
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
