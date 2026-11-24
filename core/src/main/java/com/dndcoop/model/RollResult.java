package com.dndcoop.model;

import java.util.List;

public class RollResult {

    private final List<Integer> rawRolls;
    private final int chosenRoll;
    private final int modifier;
    private final int total;
    private final boolean natural20;
    private final boolean natural1;
    private final boolean advantage;
    private final boolean disadvantage;
    private final boolean physicalDiceUsed;
    private final Integer targetDcOrAc;
    private final boolean succeeded;
    private final String formattedFormula;

    public RollResult(List<Integer> rawRolls, int chosenRoll, int modifier, int total,
                      boolean natural20, boolean natural1, boolean advantage, boolean disadvantage,
                      boolean physicalDiceUsed, Integer targetDcOrAc, boolean succeeded, String formattedFormula) {
        this.rawRolls = rawRolls;
        this.chosenRoll = chosenRoll;
        this.modifier = modifier;
        this.total = total;
        this.natural20 = natural20;
        this.natural1 = natural1;
        this.advantage = advantage;
        this.disadvantage = disadvantage;
        this.physicalDiceUsed = physicalDiceUsed;
        this.targetDcOrAc = targetDcOrAc;
        this.succeeded = succeeded;
        this.formattedFormula = formattedFormula;
    }

    public List<Integer> getRawRolls() { return rawRolls; }
    public int getChosenRoll() { return chosenRoll; }
    public int getModifier() { return modifier; }
    public int getTotal() { return total; }
    public boolean isNatural20() { return natural20; }
    public boolean isNatural1() { return natural1; }
    public boolean isAdvantage() { return advantage; }
    public boolean isDisadvantage() { return disadvantage; }
    public boolean isPhysicalDiceUsed() { return physicalDiceUsed; }
    public Integer getTargetDcOrAc() { return targetDcOrAc; }
    public boolean isSucceeded() { return succeeded; }
    public String getFormattedFormula() { return formattedFormula; }
}
