package com.dndcoop.rules;

import com.dndcoop.model.Ability;
import com.dndcoop.model.Condition;
import com.dndcoop.model.Spell;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Spellbook {

    private static final Map<String, Spell> SPELLS = new LinkedHashMap<>();

    static {
        // Cantrips (Level 0)
        register(new Spell("fire_bolt", "Fire Bolt", 0, "Evocation", false,
                Spell.CastMode.SPELL_ATTACK, null, "1d10", "fire", false, null,
                "Hurl a mote of fire at a foe within 120 ft (1d10 fire damage on a spell attack hit)."));
        register(new Spell("ray_of_frost", "Ray of Frost", 0, "Evocation", false,
                Spell.CastMode.SPELL_ATTACK, null, "1d8", "cold", false, null,
                "A frigid beam of blue-white light strikes for 1d8 cold damage and chills the target."));
        register(new Spell("sacred_flame", "Sacred Flame", 0, "Evocation", false,
                Spell.CastMode.SAVING_THROW, Ability.DEX, "1d8", "radiant", false, null,
                "Flame-like radiance descends on a creature; DEX save or take 1d8 radiant damage."));
        register(new Spell("guidance", "Guidance", 0, "Divination", false,
                Spell.CastMode.BUFF, null, "1d4", "buff", false, Condition.BLESSED,
                "Grant an ally divine guidance (+1d4 to their next ability check or roll)."));

        // Level 1 Spells
        register(new Spell("magic_missile", "Magic Missile", 1, "Evocation", false,
                Spell.CastMode.AUTO_HIT_DAMAGE, null, "3d4+3", "force", false, null,
                "Three glowing darts of magical force strike unerringly for 3d4+3 force damage."));
        register(new Spell("burning_hands", "Burning Hands", 1, "Evocation", false,
                Spell.CastMode.SAVING_THROW, Ability.DEX, "3d6", "fire", true, null,
                "A cone of roaring flames scorches all foes in front of you (3d6 fire, DEX save for half)."));
        register(new Spell("thunderwave", "Thunderwave", 1, "Evocation", false,
                Spell.CastMode.SAVING_THROW, Ability.CON, "2d8", "thunder", true, Condition.PRONE,
                "A wave of thunderous force erupts around you (2d8 thunder, CON save for half)."));
        register(new Spell("shield", "Shield", 1, "Abjuration", true,
                Spell.CastMode.BUFF, null, null, "abjuration", false, Condition.SHIELDED,
                "An invisible barrier of magical force grants +5 AC until the start of your next turn."));
        register(new Spell("cure_wounds", "Cure Wounds", 1, "Evocation", false,
                Spell.CastMode.HEAL, null, "1d8", "healing", false, null,
                "Touch a creature to restore 1d8 + spellcasting modifier HP."));
        register(new Spell("healing_word", "Healing Word", 1, "Evocation", true,
                Spell.CastMode.HEAL, null, "1d4", "healing", false, null,
                "Bonus Action: Speak a word of healing from afar, restoring 1d4 + spellcasting modifier HP."));
        register(new Spell("guiding_bolt", "Guiding Bolt", 1, "Evocation", false,
                Spell.CastMode.SPELL_ATTACK, null, "4d6", "radiant", false, null,
                "Flash of radiant light strikes for 4d6 radiant damage and illuminates the target for the next attack."));
        register(new Spell("bless", "Bless", 1, "Enchantment", false,
                Spell.CastMode.BUFF, null, "1d4", "buff", true, Condition.BLESSED,
                "Bless the party so allies add +1d4 to attack rolls and saving throws."));
        register(new Spell("hunters_mark", "Hunter's Mark", 1, "Divination", true,
                Spell.CastMode.BUFF, null, "1d6", "piercing", false, Condition.HELPED,
                "Bonus Action: Mark your quarry, sharpening your strike (Advantage on your next attack)."));
        register(new Spell("divine_smite", "Divine Smite", 1, "Evocation", false,
                Spell.CastMode.SPELL_ATTACK, null, "2d8", "radiant", false, null,
                "Channel radiant vengeance through your weapon for 2d8 radiant damage (+1d8 vs undead)."));

        // Level 2 Spells
        register(new Spell("scorching_ray", "Scorching Ray", 2, "Evocation", false,
                Spell.CastMode.SPELL_ATTACK, null, "6d6", "fire", false, null,
                "Hurl three fiery rays at your target for up to 6d6 fire damage."));
        register(new Spell("hold_person", "Hold Person", 2, "Enchantment", false,
                Spell.CastMode.SAVING_THROW, Ability.WIS, "0d4", "psychic", false, Condition.STUNNED,
                "Paralyze a humanoid foe on a failed WIS save."));
        register(new Spell("lesser_restoration", "Lesser Restoration", 2, "Abjuration", false,
                Spell.CastMode.HEAL, null, "2d6", "healing", false, null,
                "Cleanse Poisoned, Blinded, or Frightened conditions and soothe 2d6 HP."));
    }

    private Spellbook() {
    }

    private static void register(Spell spell) {
        SPELLS.put(spell.getId(), spell);
    }

    public static Spell get(String id) {
        if (id == null) return null;
        return SPELLS.get(id.toLowerCase());
    }

    public static Spell findByText(String text, Collection<String> allowedIds) {
        if (text == null || allowedIds == null) return null;
        String lower = text.toLowerCase();
        for (String id : allowedIds) {
            Spell s = SPELLS.get(id);
            if (s != null && (lower.contains(s.getName().toLowerCase()) || lower.contains(s.getId().replace('_', ' ')))) {
                return s;
            }
        }
        return null;
    }

    public static Collection<Spell> allSpells() {
        return Collections.unmodifiableCollection(SPELLS.values());
    }
}
