package com.dndcoop.rules;

import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterClass;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.Condition;
import com.dndcoop.model.Item;
import com.dndcoop.model.LogEntry;
import com.dndcoop.model.Monster;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.model.Race;
import com.dndcoop.model.RollResult;
import com.dndcoop.model.Skill;
import com.dndcoop.model.Spell;

import java.util.UUID;

public class RulesEngine {

    private final DiceRoller diceRoller;

    public RulesEngine(DiceRoller diceRoller) {
        this.diceRoller = diceRoller;
    }

    public DiceRoller getDiceRoller() {
        return diceRoller;
    }

    public boolean hasAttackAdvantage(CharacterSheet attacker, Monster target) {
        boolean adv = attacker.getConditions().contains(Condition.HIDDEN)
                || attacker.getConditions().contains(Condition.HELPED);
        if (target != null) {
            if (target.getConditions().contains(Condition.PRONE) && !attacker.getEquippedWeapon().isRanged()) {
                adv = true;
            }
            if (target.getConditions().contains(Condition.BLINDED)
                    || target.getConditions().contains(Condition.RESTRAINED)
                    || target.getConditions().contains(Condition.STUNNED)) {
                adv = true;
            }
        }
        return adv;
    }

    public boolean hasAttackDisadvantage(CharacterSheet attacker, Monster target) {
        boolean dis = attacker.getConditions().contains(Condition.POISONED)
                || attacker.getConditions().contains(Condition.FRIGHTENED)
                || attacker.getConditions().contains(Condition.BLINDED)
                || attacker.getConditions().contains(Condition.PRONE)
                || attacker.getConditions().contains(Condition.RESTRAINED);
        if (target != null && target.getConditions().contains(Condition.DODGING)) {
            dis = true;
        }
        return dis;
    }

    public boolean hasCheckDisadvantage(CharacterSheet actor) {
        return actor.getConditions().contains(Condition.POISONED)
                || actor.getConditions().contains(Condition.FRIGHTENED);
    }

    public PendingRoll buildAbilityCheckRoll(CharacterSheet actor, Skill skill, Ability fallbackAbility,
                                             int dc, String label, String actionText) {
        PendingRoll pr = new PendingRoll();
        pr.setId(UUID.randomUUID().toString());
        pr.setActorId(actor.getId());
        pr.setActorName(actor.getName());
        pr.setRollType(PendingRoll.RollType.ABILITY_CHECK);
        pr.setSkill(skill);
        pr.setAbility(skill != null ? skill.getGoverningAbility() : fallbackAbility);
        pr.setDiceNotation("1d20");
        int mod = skill != null ? actor.getSkillModifier(skill) : actor.getAbilityModifier(fallbackAbility);
        if (actor.getConditions().contains(Condition.BLESSED)) {
            mod += diceRoller.rollDie(4);
        }
        pr.setModifier(mod);
        pr.setTargetDcOrAc(dc);
        pr.setAdvantage(actor.getConditions().contains(Condition.HELPED));
        pr.setDisadvantage(hasCheckDisadvantage(actor));
        pr.setLabel(label);
        pr.setContextActionText(actionText);
        return pr;
    }

    public PendingRoll buildSavingThrowRoll(CharacterSheet actor, Ability ability, int dc, String label) {
        PendingRoll pr = new PendingRoll();
        pr.setId(UUID.randomUUID().toString());
        pr.setActorId(actor.getId());
        pr.setActorName(actor.getName());
        pr.setRollType(PendingRoll.RollType.SAVING_THROW);
        pr.setAbility(ability);
        pr.setDiceNotation("1d20");
        int mod = actor.getSavingThrowModifier(ability);
        if (actor.getConditions().contains(Condition.BLESSED)) {
            mod += diceRoller.rollDie(4);
        }
        pr.setModifier(mod);
        pr.setTargetDcOrAc(dc);
        boolean adv = (ability == Ability.DEX && actor.getConditions().contains(Condition.DODGING));
        pr.setAdvantage(adv);
        pr.setLabel(label);
        return pr;
    }

    public PendingRoll buildWeaponAttackRoll(CharacterSheet attacker, Monster target, String actionText, boolean partyHasFrontliner) {
        Item weapon = attacker.getEquippedWeapon();
        PendingRoll pr = new PendingRoll();
        pr.setId(UUID.randomUUID().toString());
        pr.setActorId(attacker.getId());
        pr.setActorName(attacker.getName());
        pr.setRollType(PendingRoll.RollType.ATTACK_ROLL);
        pr.setDiceNotation("1d20");
        int bonus = attacker.getWeaponAttackBonus(weapon);
        if (attacker.getConditions().contains(Condition.BLESSED)) {
            bonus += diceRoller.rollDie(4);
        }
        pr.setModifier(bonus);
        pr.setTargetDcOrAc(target.getArmorClass());
        boolean adv = hasAttackAdvantage(attacker, target);
        boolean dis = hasAttackDisadvantage(attacker, target);
        pr.setAdvantage(adv);
        pr.setDisadvantage(dis);
        pr.setWeaponId(weapon.getId());
        pr.setTargetId(target.getId());
        pr.setTargetName(target.getName());
        pr.setDamageType(weapon.getDamageType());
        pr.setLabel(attacker.getName() + " attacks " + target.getName() + " with " + weapon.getName());
        pr.setContextActionText(actionText);
        boolean sneak = attacker.getCharacterClass() == CharacterClass.ROGUE
                && (weapon.isFinesse() || weapon.isRanged())
                && !dis && (adv || partyHasFrontliner);
        pr.setSneakAttackEligible(sneak);
        return pr;
    }

    public PendingRoll buildSpellAttackRoll(CharacterSheet caster, Spell spell, Monster target, String actionText) {
        PendingRoll pr = new PendingRoll();
        pr.setId(UUID.randomUUID().toString());
        pr.setActorId(caster.getId());
        pr.setActorName(caster.getName());
        pr.setRollType(PendingRoll.RollType.SPELL_ATTACK);
        pr.setDiceNotation("1d20");
        int bonus = caster.getSpellAttackBonus();
        if (caster.getConditions().contains(Condition.BLESSED)) {
            bonus += diceRoller.rollDie(4);
        }
        pr.setModifier(bonus);
        pr.setTargetDcOrAc(target.getArmorClass());
        pr.setAdvantage(hasAttackAdvantage(caster, target));
        pr.setDisadvantage(hasAttackDisadvantage(caster, target));
        pr.setSpellId(spell.getId());
        pr.setTargetId(target.getId());
        pr.setTargetName(target.getName());
        pr.setDamageType(spell.getDamageType());
        pr.setLabel(caster.getName() + " casts " + spell.getName() + " at " + target.getName());
        pr.setContextActionText(actionText);
        return pr;
    }

    public PendingRoll buildDamageRoll(CharacterSheet attacker, Monster target, String diceNotation,
                                       int modifier, String damageType, boolean criticalHit,
                                       boolean sneakAttack, String sourceName) {
        PendingRoll pr = new PendingRoll();
        pr.setId(UUID.randomUUID().toString());
        pr.setActorId(attacker.getId());
        pr.setActorName(attacker.getName());
        pr.setRollType(PendingRoll.RollType.DAMAGE_ROLL);
        pr.setDiceNotation(diceNotation);
        pr.setModifier(modifier);
        pr.setCriticalHit(criticalHit);
        pr.setTargetId(target.getId());
        pr.setTargetName(target.getName());
        pr.setDamageType(damageType);
        pr.setSneakAttackEligible(sneakAttack);
        String critTag = criticalHit ? " [CRITICAL HIT!]" : "";
        String sneakTag = sneakAttack ? " + Sneak Attack" : "";
        pr.setLabel("Damage for " + sourceName + " -> " + target.getName() + critTag + sneakTag);
        return pr;
    }

    public PendingRoll buildInitiativeRoll(CharacterSheet hero) {
        PendingRoll pr = new PendingRoll();
        pr.setId(UUID.randomUUID().toString());
        pr.setActorId(hero.getId());
        pr.setActorName(hero.getName());
        pr.setRollType(PendingRoll.RollType.INITIATIVE);
        pr.setDiceNotation("1d20");
        pr.setModifier(hero.getInitiativeModifier());
        pr.setLabel("Roll Initiative for " + hero.getName() + " (DEX " + Ability.formatModifier(hero.getInitiativeModifier()) + ")");
        return pr;
    }

    public PendingRoll buildDeathSaveRoll(CharacterSheet hero) {
        PendingRoll pr = new PendingRoll();
        pr.setId(UUID.randomUUID().toString());
        pr.setActorId(hero.getId());
        pr.setActorName(hero.getName());
        pr.setRollType(PendingRoll.RollType.DEATH_SAVE);
        pr.setDiceNotation("1d20");
        pr.setModifier(0);
        pr.setTargetDcOrAc(10);
        pr.setLabel("Death Saving Throw for " + hero.getName()
                + " (Successes: " + hero.getDeathSaveSuccesses() + "/3, Failures: " + hero.getDeathSaveFailures() + "/3)");
        return pr;
    }

    public RollResult executePendingRoll(PendingRoll pending, CharacterSheet actor, Integer physicalDieInput) {
        boolean isD20 = pending.getRollType() != PendingRoll.RollType.DAMAGE_ROLL
                && pending.getRollType() != PendingRoll.RollType.HEAL_ROLL;
        boolean isAttack = pending.getRollType() == PendingRoll.RollType.ATTACK_ROLL
                || pending.getRollType() == PendingRoll.RollType.SPELL_ATTACK;
        boolean halflingLucky = actor != null && actor.getRace() == Race.HALFLING;

        if (isD20) {
            if (physicalDieInput != null) {
                return diceRoller.resolveManualD20(physicalDieInput, pending.getModifier(),
                        pending.isAdvantage(), pending.isDisadvantage(), pending.getTargetDcOrAc(), isAttack);
            }
            return diceRoller.rollD20(pending.getModifier(), pending.isAdvantage(), pending.isDisadvantage(),
                    pending.getTargetDcOrAc(), isAttack, halflingLucky);
        } else {
            if (physicalDieInput != null) {
                return diceRoller.resolveManualDice(pending.getDiceNotation(), physicalDieInput,
                        pending.getModifier(), pending.isCriticalHit());
            }
            return diceRoller.rollDiceNotation(pending.getDiceNotation(), pending.getModifier(), pending.isCriticalHit());
        }
    }

    public String applyDeathSaveResult(CharacterSheet hero, RollResult result) {
        if (result.isNatural20()) {
            hero.applyHealing(1);
            return hero.getName() + " rolled a NATURAL 20 on their Death Save and regains 1 HP, springing back to consciousness!";
        }
        if (result.isNatural1()) {
            hero.setDeathSaveFailures(Math.min(3, hero.getDeathSaveFailures() + 2));
            if (hero.getDeathSaveFailures() >= 3) {
                hero.setDead(true);
                return hero.getName() + " rolled a Natural 1 (2 failures) and has fallen permanently.";
            }
            return hero.getName() + " rolled a Natural 1 — suffers 2 Death Save failures (" + hero.getDeathSaveFailures() + "/3).";
        }
        if (result.getTotal() >= 10) {
            hero.setDeathSaveSuccesses(hero.getDeathSaveSuccesses() + 1);
            if (hero.getDeathSaveSuccesses() >= 3) {
                hero.setStable(true);
                return hero.getName() + " reached 3 Death Save successes and is now STABLE at 0 HP.";
            }
            return hero.getName() + " succeeded on a Death Save (" + hero.getDeathSaveSuccesses() + "/3 successes).";
        } else {
            hero.setDeathSaveFailures(hero.getDeathSaveFailures() + 1);
            if (hero.getDeathSaveFailures() >= 3) {
                hero.setDead(true);
                return hero.getName() + " suffered a 3rd Death Save failure and has fallen.";
            }
            return hero.getName() + " failed a Death Save (" + hero.getDeathSaveFailures() + "/3 failures).";
        }
    }

    public String applyShortRest(CampaignState state) {
        if (state.getCombat().isActive()) {
            return "You cannot take a Short Rest while in combat!";
        }
        StringBuilder sb = new StringBuilder("The party catches their breath for a 1-hour Short Rest:\n");
        for (CharacterSheet hero : state.getParty()) {
            if (hero.isDead()) continue;
            if (hero.getHitDiceRemaining() > 0 && hero.getCurrentHp() < hero.getMaxHp()) {
                int roll = diceRoller.rollDie(hero.getCharacterClass().getHitDie());
                int heal = Math.max(1, roll + hero.getAbilityModifier(Ability.CON));
                int actual = hero.applyHealing(heal);
                hero.setHitDiceRemaining(hero.getHitDiceRemaining() - 1);
                sb.append("• ").append(hero.getName()).append(" spent 1 Hit Die (+").append(actual).append(" HP -> ")
                        .append(hero.getCurrentHp()).append("/").append(hero.getMaxHp()).append(").\n");
            } else {
                sb.append("• ").append(hero.getName()).append(" tends their gear (HP ")
                        .append(hero.getCurrentHp()).append("/").append(hero.getMaxHp()).append(").\n");
            }
            if (hero.getCharacterClass() == CharacterClass.FIGHTER) {
                hero.setClassResourceCurrent(hero.getClassResourceMax());
            } else if (hero.getCharacterClass() == CharacterClass.WIZARD && hero.getClassResourceCurrent() > 0) {
                // Arcane Recovery restores 1 first-level slot
                if (hero.getSpellSlotsCurrent()[1] < hero.getSpellSlotsMax()[1]) {
                    hero.getSpellSlotsCurrent()[1]++;
                    hero.setClassResourceCurrent(0);
                    sb.append("  (").append(hero.getName()).append(" used Arcane Recovery to regain a 1st-level spell slot.)\n");
                }
            }
            hero.getConditions().remove(Condition.FRIGHTENED);
            hero.getConditions().remove(Condition.PRONE);
        }
        String msg = sb.toString().trim();
        state.addLog(LogEntry.system(msg));
        return msg;
    }

    public String applyLongRest(CampaignState state) {
        if (state.getCombat().isActive()) {
            return "You cannot take a Long Rest while enemies are attacking!";
        }
        StringBuilder sb = new StringBuilder("The party makes camp for an 8-hour Long Rest. Dawn breaks refreshed:\n");
        for (CharacterSheet hero : state.getParty()) {
            if (hero.isDead()) continue;
            hero.setCurrentHp(hero.getMaxHp());
            hero.setHitDiceRemaining(Math.min(hero.getLevel(), hero.getHitDiceRemaining() + Math.max(1, hero.getLevel() / 2)));
            hero.setSpellSlotsCurrent(hero.getSpellSlotsMax().clone());
            hero.setClassResourceCurrent(hero.getClassResourceMax());
            hero.getConditions().clear();
            hero.setDeathSaveSuccesses(0);
            hero.setDeathSaveFailures(0);
            hero.setStable(false);
            sb.append("• ").append(hero.getName()).append(": Full HP (").append(hero.getMaxHp())
                    .append("), all spell slots & class abilities restored.\n");
        }
        String msg = sb.toString().trim();
        state.addLog(LogEntry.system(msg));
        return msg;
    }

    public String useHealingPotion(CampaignState state, CharacterSheet actor, CharacterSheet target) {
        Item potion = actor.findItemById("healing_potion");
        if (potion == null) {
            return actor.getName() + " does not have a Potion of Healing.";
        }
        CharacterSheet recipient = target != null ? target : actor;
        RollResult roll = diceRoller.rollDiceNotation(potion.getHealingDice(), 0, false);
        int healed = recipient.applyHealing(roll.getTotal());
        actor.removeOneItem("healing_potion");
        String summary = actor.getName() + " uses a Potion of Healing on " + recipient.getName()
                + ", restoring " + healed + " HP (" + recipient.getCurrentHp() + "/" + recipient.getMaxHp() + ").";
        state.addLog(LogEntry.dice(actor.getName(), summary, roll.getFormattedFormula()));
        return summary;
    }
}
