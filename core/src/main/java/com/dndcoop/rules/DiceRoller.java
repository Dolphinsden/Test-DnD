package com.dndcoop.rules;

import com.dndcoop.model.Ability;
import com.dndcoop.model.RollResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiceRoller {

    private static final Pattern DICE_PATTERN = Pattern.compile("^\\s*(\\d+)d(\\d+)\\s*([+-]\\s*\\d+)?\\s*$", Pattern.CASE_INSENSITIVE);

    private final Random random;

    public DiceRoller() {
        this(new Random());
    }

    public DiceRoller(Random random) {
        this.random = random;
    }

    public int rollDie(int sides) {
        return random.nextInt(Math.max(1, sides)) + 1;
    }

    public RollResult rollD20(int modifier, boolean advantage, boolean disadvantage,
                              Integer targetDcOrAc, boolean isAttackRoll, boolean halflingLucky) {
        boolean netAdv = advantage && !disadvantage;
        boolean netDis = disadvantage && !advantage;

        List<Integer> rolls = new ArrayList<>();
        int r1 = rollDie(20);
        if (halflingLucky && r1 == 1) {
            r1 = rollDie(20);
        }
        rolls.add(r1);

        int chosen = r1;
        if (netAdv || netDis) {
            int r2 = rollDie(20);
            if (halflingLucky && r2 == 1) {
                r2 = rollDie(20);
            }
            rolls.add(r2);
            chosen = netAdv ? Math.max(r1, r2) : Math.min(r1, r2);
        }

        return buildD20Result(rolls, chosen, modifier, netAdv, netDis, false, targetDcOrAc, isAttackRoll);
    }

    public RollResult resolveManualD20(int physicalDieValue, int modifier, boolean advantage, boolean disadvantage,
                                       Integer targetDcOrAc, boolean isAttackRoll) {
        int clamped = Math.max(1, Math.min(20, physicalDieValue));
        boolean netAdv = advantage && !disadvantage;
        boolean netDis = disadvantage && !advantage;
        return buildD20Result(Collections.singletonList(clamped), clamped, modifier, netAdv, netDis, true, targetDcOrAc, isAttackRoll);
    }

    private RollResult buildD20Result(List<Integer> rolls, int chosen, int modifier,
                                      boolean netAdv, boolean netDis, boolean physical,
                                      Integer targetDcOrAc, boolean isAttackRoll) {
        int total = chosen + modifier;
        boolean nat20 = chosen == 20;
        boolean nat1 = chosen == 1;
        boolean succeeded;
        if (targetDcOrAc == null) {
            succeeded = true;
        } else if (isAttackRoll && nat20) {
            succeeded = true;
        } else if (isAttackRoll && nat1) {
            succeeded = false;
        } else {
            succeeded = total >= targetDcOrAc;
        }

        StringBuilder formula = new StringBuilder();
        formula.append(physical ? "[Physical d20: " : "d20(");
        for (int i = 0; i < rolls.size(); i++) {
            if (i > 0) formula.append(", ");
            formula.append(rolls.get(i));
        }
        formula.append(physical ? "]" : ")");
        if (netAdv) formula.append(" [ADV->").append(chosen).append("]");
        if (netDis) formula.append(" [DIS->").append(chosen).append("]");
        if (modifier != 0) {
            formula.append(" ").append(Ability.formatModifier(modifier));
        }
        formula.append(" = ").append(total);
        if (targetDcOrAc != null) {
            formula.append(isAttackRoll ? " vs AC " : " vs DC ").append(targetDcOrAc);
        }
        if (nat20) formula.append(" (NAT 20!)");
        if (nat1) formula.append(" (NAT 1!)");

        return new RollResult(rolls, chosen, modifier, total, nat20, nat1, netAdv, netDis, physical, targetDcOrAc, succeeded, formula.toString());
    }

    public RollResult rollDiceNotation(String notation, int extraModifier, boolean criticalDoubleDice) {
        ParsedDice parsed = parse(notation);
        int count = criticalDoubleDice ? parsed.count * 2 : parsed.count;
        int totalModifier = parsed.bonus + extraModifier;

        List<Integer> rawRolls = new ArrayList<>();
        int sum = 0;
        for (int i = 0; i < count; i++) {
            int val = rollDie(parsed.sides);
            rawRolls.add(val);
            sum += val;
        }
        int total = Math.max(1, sum + totalModifier);

        StringBuilder formula = new StringBuilder();
        formula.append(count).append("d").append(parsed.sides).append("(");
        for (int i = 0; i < rawRolls.size(); i++) {
            if (i > 0) formula.append("+");
            formula.append(rawRolls.get(i));
        }
        formula.append(")");
        if (totalModifier != 0) {
            formula.append(" ").append(Ability.formatModifier(totalModifier));
        }
        formula.append(" = ").append(total);
        if (criticalDoubleDice) {
            formula.append(" [CRIT 2x dice]");
        }

        return new RollResult(rawRolls, sum, totalModifier, total, false, false, false, false, false, null, true, formula.toString());
    }

    public RollResult resolveManualDice(String notation, int physicalRawSum, int extraModifier, boolean criticalDoubleDice) {
        ParsedDice parsed = parse(notation);
        int count = criticalDoubleDice ? parsed.count * 2 : parsed.count;
        int maxPossible = count * parsed.sides;
        int clamped = Math.max(count, Math.min(maxPossible, physicalRawSum));
        int totalModifier = parsed.bonus + extraModifier;
        int total = Math.max(1, clamped + totalModifier);

        StringBuilder formula = new StringBuilder();
        formula.append("[Physical ").append(count).append("d").append(parsed.sides).append(": ").append(clamped).append("]");
        if (totalModifier != 0) {
            formula.append(" ").append(Ability.formatModifier(totalModifier));
        }
        formula.append(" = ").append(total);
        return new RollResult(Collections.singletonList(clamped), clamped, totalModifier, total, false, false, false, false, true, null, true, formula.toString());
    }

    public static ParsedDice parse(String notation) {
        if (notation == null || notation.isBlank()) {
            return new ParsedDice(1, 6, 0);
        }
        Matcher matcher = DICE_PATTERN.matcher(notation);
        if (!matcher.matches()) {
            return new ParsedDice(1, 6, 0);
        }
        int count = Integer.parseInt(matcher.group(1));
        int sides = Integer.parseInt(matcher.group(2));
        int bonus = 0;
        if (matcher.group(3) != null) {
            bonus = Integer.parseInt(matcher.group(3).replaceAll("\\s+", ""));
        }
        return new ParsedDice(Math.max(1, count), Math.max(2, sides), bonus);
    }

    public static String doubleDiceNotation(String notation) {
        ParsedDice p = parse(notation);
        String base = (p.count * 2) + "d" + p.sides;
        if (p.bonus > 0) return base + "+" + p.bonus;
        if (p.bonus < 0) return base + p.bonus;
        return base;
    }

    public static final class ParsedDice {
        public final int count;
        public final int sides;
        public final int bonus;

        public ParsedDice(int count, int sides, int bonus) {
            this.count = count;
            this.sides = sides;
            this.bonus = bonus;
        }
    }
}
