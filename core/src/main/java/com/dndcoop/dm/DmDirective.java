package com.dndcoop.dm;

import com.dndcoop.model.PendingRoll;

public class DmDirective {

    private String dmSourceLabel = "Offline DM";
    private boolean usedFallback;
    private String narrative;
    private PendingRoll requestedRoll;
    private String moveToLocationId;
    private String startEncounterId;
    private String restType; // "short" or "long"
    private int grantGold;
    private int grantXp;
    private String grantItemId;
    private String setFlag;

    public String getDmSourceLabel() { return dmSourceLabel; }
    public void setDmSourceLabel(String dmSourceLabel) { this.dmSourceLabel = dmSourceLabel; }
    public boolean isUsedFallback() { return usedFallback; }
    public void setUsedFallback(boolean usedFallback) { this.usedFallback = usedFallback; }
    public String getNarrative() { return narrative; }
    public void setNarrative(String narrative) { this.narrative = narrative; }
    public PendingRoll getRequestedRoll() { return requestedRoll; }
    public void setRequestedRoll(PendingRoll requestedRoll) { this.requestedRoll = requestedRoll; }
    public String getMoveToLocationId() { return moveToLocationId; }
    public void setMoveToLocationId(String moveToLocationId) { this.moveToLocationId = moveToLocationId; }
    public String getStartEncounterId() { return startEncounterId; }
    public void setStartEncounterId(String startEncounterId) { this.startEncounterId = startEncounterId; }
    public String getRestType() { return restType; }
    public void setRestType(String restType) { this.restType = restType; }
    public int getGrantGold() { return grantGold; }
    public void setGrantGold(int grantGold) { this.grantGold = grantGold; }
    public int getGrantXp() { return grantXp; }
    public void setGrantXp(int grantXp) { this.grantXp = grantXp; }
    public String getGrantItemId() { return grantItemId; }
    public void setGrantItemId(String grantItemId) { this.grantItemId = grantItemId; }
    public String getSetFlag() { return setFlag; }
    public void setSetFlag(String setFlag) { this.setFlag = setFlag; }
}
