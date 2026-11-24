package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InitiativeEntry {

    private String participantId;
    private String name;
    private boolean player;
    private int initiativeTotal;
    private int dexModifier;
    private boolean actionUsed;
    private boolean bonusActionUsed;
    private boolean disengaged;

    public InitiativeEntry() {
    }

    public InitiativeEntry(String participantId, String name, boolean player, int initiativeTotal, int dexModifier) {
        this.participantId = participantId;
        this.name = name;
        this.player = player;
        this.initiativeTotal = initiativeTotal;
        this.dexModifier = dexModifier;
    }

    public void resetTurnEconomy() {
        this.actionUsed = false;
        this.bonusActionUsed = false;
        this.disengaged = false;
    }

    public String getParticipantId() { return participantId; }
    public void setParticipantId(String participantId) { this.participantId = participantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isPlayer() { return player; }
    public void setPlayer(boolean player) { this.player = player; }
    public int getInitiativeTotal() { return initiativeTotal; }
    public void setInitiativeTotal(int initiativeTotal) { this.initiativeTotal = initiativeTotal; }
    public int getDexModifier() { return dexModifier; }
    public void setDexModifier(int dexModifier) { this.dexModifier = dexModifier; }
    public boolean isActionUsed() { return actionUsed; }
    public void setActionUsed(boolean actionUsed) { this.actionUsed = actionUsed; }
    public boolean isBonusActionUsed() { return bonusActionUsed; }
    public void setBonusActionUsed(boolean bonusActionUsed) { this.bonusActionUsed = bonusActionUsed; }
    public boolean isDisengaged() { return disengaged; }
    public void setDisengaged(boolean disengaged) { this.disengaged = disengaged; }
}
