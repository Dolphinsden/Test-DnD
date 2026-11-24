package com.dndcoop.persistence;

public class SaveSlotInfo {

    private final String fileName;
    private final String saveName;
    private final String campaignTitle;
    private final int act;
    private final String locationName;
    private final int partySize;
    private final int turnCount;
    private final boolean inCombat;
    private final String updatedAt;

    public SaveSlotInfo(String fileName, String saveName, String campaignTitle, int act,
                        String locationName, int partySize, int turnCount, boolean inCombat, String updatedAt) {
        this.fileName = fileName;
        this.saveName = saveName;
        this.campaignTitle = campaignTitle;
        this.act = act;
        this.locationName = locationName;
        this.partySize = partySize;
        this.turnCount = turnCount;
        this.inCombat = inCombat;
        this.updatedAt = updatedAt;
    }

    public String getFileName() { return fileName; }
    public String getSaveName() { return saveName; }
    public String getCampaignTitle() { return campaignTitle; }
    public int getAct() { return act; }
    public String getLocationName() { return locationName; }
    public int getPartySize() { return partySize; }
    public int getTurnCount() { return turnCount; }
    public boolean isInCombat() { return inCombat; }
    public String getUpdatedAt() { return updatedAt; }
}
