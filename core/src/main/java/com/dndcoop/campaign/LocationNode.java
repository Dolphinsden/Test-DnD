package com.dndcoop.campaign;

import com.dndcoop.model.Skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LocationNode {

    private final String id;
    private final String name;
    private final int act;
    private final String description;
    private final String ambientHint;
    private final boolean safeForLongRest;
    private final Map<String, String> exits = new LinkedHashMap<>(); // targetLocationId -> exit label
    private final Map<String, String> lockedExitRequiredFlags = new LinkedHashMap<>();
    private final List<NpcProfile> npcs = new ArrayList<>();
    private String encounterId;

    // Optional exploration discovery / trap / lockpick hook
    private Skill investigateSkill;
    private int investigateDc;
    private String investigateFlag;
    private String investigateSuccessText;
    private String investigateFailureText;
    private String investigateRewardItemId;
    private int investigateGold;
    private int investigateXp;

    public LocationNode(String id, String name, int act, String description, String ambientHint, boolean safeForLongRest) {
        this.id = id;
        this.name = name;
        this.act = act;
        this.description = description;
        this.ambientHint = ambientHint;
        this.safeForLongRest = safeForLongRest;
    }

    public LocationNode addExit(String targetId, String label) {
        exits.put(targetId, label);
        return this;
    }

    public LocationNode addLockedExit(String targetId, String label, String requiredFlag) {
        exits.put(targetId, label);
        lockedExitRequiredFlags.put(targetId, requiredFlag);
        return this;
    }

    public LocationNode addNpc(NpcProfile npc) {
        npcs.add(npc);
        return this;
    }

    public LocationNode withEncounter(String encounterId) {
        this.encounterId = encounterId;
        return this;
    }

    public LocationNode withDiscovery(Skill skill, int dc, String flag, String successText,
                                      String failureText, String rewardItemId, int gold, int xp) {
        this.investigateSkill = skill;
        this.investigateDc = dc;
        this.investigateFlag = flag;
        this.investigateSuccessText = successText;
        this.investigateFailureText = failureText;
        this.investigateRewardItemId = rewardItemId;
        this.investigateGold = gold;
        this.investigateXp = xp;
        return this;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAct() { return act; }
    public String getDescription() { return description; }
    public String getAmbientHint() { return ambientHint; }
    public boolean isSafeForLongRest() { return safeForLongRest; }
    public Map<String, String> getExits() { return exits; }
    public Map<String, String> getLockedExitRequiredFlags() { return lockedExitRequiredFlags; }
    public List<NpcProfile> getNpcs() { return npcs; }
    public String getEncounterId() { return encounterId; }
    public Skill getInvestigateSkill() { return investigateSkill; }
    public int getInvestigateDc() { return investigateDc; }
    public String getInvestigateFlag() { return investigateFlag; }
    public String getInvestigateSuccessText() { return investigateSuccessText; }
    public String getInvestigateFailureText() { return investigateFailureText; }
    public String getInvestigateRewardItemId() { return investigateRewardItemId; }
    public int getInvestigateGold() { return investigateGold; }
    public int getInvestigateXp() { return investigateXp; }
}
