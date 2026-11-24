package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CombatState {

    private boolean active;
    private String encounterId;
    private String encounterName;
    private int roundNumber = 1;
    private List<InitiativeEntry> turnOrder = new ArrayList<>();
    private int currentTurnIndex = 0;
    private List<Monster> enemies = new ArrayList<>();
    private List<String> pendingInitiativeHeroIds = new ArrayList<>();
    private String victoryFlag;
    private int bonusGoldReward;
    private String rewardItemId;

    public CombatState() {
    }

    @JsonIgnore
    public InitiativeEntry getCurrentTurnEntry() {
        if (!active || turnOrder.isEmpty() || currentTurnIndex < 0 || currentTurnIndex >= turnOrder.size()) {
            return null;
        }
        return turnOrder.get(currentTurnIndex);
    }

    @JsonIgnore
    public Monster findEnemyById(String id) {
        if (id == null) return null;
        for (Monster m : enemies) {
            if (id.equals(m.getId())) {
                return m;
            }
        }
        return null;
    }

    @JsonIgnore
    public List<Monster> getLivingEnemies() {
        List<Monster> living = new ArrayList<>();
        for (Monster m : enemies) {
            if (m.isAlive()) {
                living.add(m);
            }
        }
        return living;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getEncounterId() { return encounterId; }
    public void setEncounterId(String encounterId) { this.encounterId = encounterId; }
    public String getEncounterName() { return encounterName; }
    public void setEncounterName(String encounterName) { this.encounterName = encounterName; }
    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public List<InitiativeEntry> getTurnOrder() { return turnOrder; }
    public void setTurnOrder(List<InitiativeEntry> turnOrder) { this.turnOrder = turnOrder != null ? new ArrayList<>(turnOrder) : new ArrayList<>(); }
    public int getCurrentTurnIndex() { return currentTurnIndex; }
    public void setCurrentTurnIndex(int currentTurnIndex) { this.currentTurnIndex = currentTurnIndex; }
    public List<Monster> getEnemies() { return enemies; }
    public void setEnemies(List<Monster> enemies) { this.enemies = enemies != null ? new ArrayList<>(enemies) : new ArrayList<>(); }
    public List<String> getPendingInitiativeHeroIds() { return pendingInitiativeHeroIds; }
    public void setPendingInitiativeHeroIds(List<String> pendingInitiativeHeroIds) {
        this.pendingInitiativeHeroIds = pendingInitiativeHeroIds != null ? new ArrayList<>(pendingInitiativeHeroIds) : new ArrayList<>();
    }
    public String getVictoryFlag() { return victoryFlag; }
    public void setVictoryFlag(String victoryFlag) { this.victoryFlag = victoryFlag; }
    public int getBonusGoldReward() { return bonusGoldReward; }
    public void setBonusGoldReward(int bonusGoldReward) { this.bonusGoldReward = bonusGoldReward; }
    public String getRewardItemId() { return rewardItemId; }
    public void setRewardItemId(String rewardItemId) { this.rewardItemId = rewardItemId; }
}
