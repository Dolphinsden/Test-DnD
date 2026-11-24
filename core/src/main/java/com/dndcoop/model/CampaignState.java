package com.dndcoop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignState {

    private String sessionId = UUID.randomUUID().toString();
    private String campaignTitle = "The Ashen Lantern of Hollowbrook";
    private String saveName = "Autosave";
    private String createdAt = Instant.now().toString();
    private String updatedAt = Instant.now().toString();

    private int currentAct = 1;
    private String currentLocationId = "gilded_stag_tavern";
    private Set<String> visitedLocations = new HashSet<>();
    private Map<String, Boolean> storyFlags = new HashMap<>();
    private Set<String> defeatedEncounters = new HashSet<>();
    private Set<String> lootedCaches = new HashSet<>();

    private List<CharacterSheet> party = new ArrayList<>();
    private String activeCharacterId;

    private CombatState combat = new CombatState();
    private PendingRoll pendingRoll;

    private List<LogEntry> log = new ArrayList<>();
    private int turnCount = 0;
    private boolean campaignCompleted = false;

    public CampaignState() {
    }

    @JsonIgnore
    public CharacterSheet getActiveCharacter() {
        if (party.isEmpty()) {
            return null;
        }
        if (activeCharacterId != null) {
            for (CharacterSheet c : party) {
                if (activeCharacterId.equals(c.getId())) {
                    return c;
                }
            }
        }
        activeCharacterId = party.get(0).getId();
        return party.get(0);
    }

    @JsonIgnore
    public CharacterSheet findHeroById(String id) {
        if (id == null) return null;
        for (CharacterSheet c : party) {
            if (id.equals(c.getId())) {
                return c;
            }
        }
        return null;
    }

    @JsonIgnore
    public CharacterSheet findHeroByName(String name) {
        if (name == null) return null;
        String lower = name.trim().toLowerCase();
        for (CharacterSheet c : party) {
            if (c.getName().toLowerCase().equals(lower) || c.getName().toLowerCase().contains(lower)) {
                return c;
            }
        }
        return null;
    }

    @JsonIgnore
    public boolean hasFlag(String key) {
        return Boolean.TRUE.equals(storyFlags.get(key));
    }

    public void setFlag(String key, boolean value) {
        storyFlags.put(key, value);
    }

    public void addLog(LogEntry entry) {
        if (entry != null) {
            log.add(entry);
            updatedAt = Instant.now().toString();
        }
    }

    @JsonIgnore
    public boolean isPartyWiped() {
        if (party.isEmpty()) return false;
        for (CharacterSheet hero : party) {
            if (!hero.isDead() && hero.getCurrentHp() > 0) {
                return false;
            }
        }
        return true;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getCampaignTitle() { return campaignTitle; }
    public void setCampaignTitle(String campaignTitle) { this.campaignTitle = campaignTitle; }
    public String getSaveName() { return saveName; }
    public void setSaveName(String saveName) { this.saveName = saveName; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public int getCurrentAct() { return currentAct; }
    public void setCurrentAct(int currentAct) { this.currentAct = currentAct; }
    public String getCurrentLocationId() { return currentLocationId; }
    public void setCurrentLocationId(String currentLocationId) { this.currentLocationId = currentLocationId; }
    public Set<String> getVisitedLocations() { return visitedLocations; }
    public void setVisitedLocations(Set<String> visitedLocations) { this.visitedLocations = visitedLocations != null ? new HashSet<>(visitedLocations) : new HashSet<>(); }
    public Map<String, Boolean> getStoryFlags() { return storyFlags; }
    public void setStoryFlags(Map<String, Boolean> storyFlags) { this.storyFlags = storyFlags != null ? new HashMap<>(storyFlags) : new HashMap<>(); }
    public Set<String> getDefeatedEncounters() { return defeatedEncounters; }
    public void setDefeatedEncounters(Set<String> defeatedEncounters) { this.defeatedEncounters = defeatedEncounters != null ? new HashSet<>(defeatedEncounters) : new HashSet<>(); }
    public Set<String> getLootedCaches() { return lootedCaches; }
    public void setLootedCaches(Set<String> lootedCaches) { this.lootedCaches = lootedCaches != null ? new HashSet<>(lootedCaches) : new HashSet<>(); }
    public List<CharacterSheet> getParty() { return party; }
    public void setParty(List<CharacterSheet> party) { this.party = party != null ? new ArrayList<>(party) : new ArrayList<>(); }
    public String getActiveCharacterId() { return activeCharacterId; }
    public void setActiveCharacterId(String activeCharacterId) { this.activeCharacterId = activeCharacterId; }
    public CombatState getCombat() { return combat; }
    public void setCombat(CombatState combat) { this.combat = combat != null ? combat : new CombatState(); }
    public PendingRoll getPendingRoll() { return pendingRoll; }
    public void setPendingRoll(PendingRoll pendingRoll) { this.pendingRoll = pendingRoll; }
    public List<LogEntry> getLog() { return log; }
    public void setLog(List<LogEntry> log) { this.log = log != null ? new ArrayList<>(log) : new ArrayList<>(); }
    public int getTurnCount() { return turnCount; }
    public void setTurnCount(int turnCount) { this.turnCount = turnCount; }
    public boolean isCampaignCompleted() { return campaignCompleted; }
    public void setCampaignCompleted(boolean campaignCompleted) { this.campaignCompleted = campaignCompleted; }
}
