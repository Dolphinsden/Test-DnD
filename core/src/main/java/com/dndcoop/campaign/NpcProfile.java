package com.dndcoop.campaign;

import com.dndcoop.model.Skill;

public class NpcProfile {

    private final String id;
    private final String name;
    private final String title;
    private final String greeting;
    private final String secretInfo;
    private final Skill persuadeSkill;
    private final int persuadeDc;
    private final String flagGranted;
    private final String rewardItemId;

    public NpcProfile(String id, String name, String title, String greeting, String secretInfo,
                      Skill persuadeSkill, int persuadeDc, String flagGranted, String rewardItemId) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.greeting = greeting;
        this.secretInfo = secretInfo;
        this.persuadeSkill = persuadeSkill;
        this.persuadeDc = persuadeDc;
        this.flagGranted = flagGranted;
        this.rewardItemId = rewardItemId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTitle() { return title; }
    public String getGreeting() { return greeting; }
    public String getSecretInfo() { return secretInfo; }
    public Skill getPersuadeSkill() { return persuadeSkill; }
    public int getPersuadeDc() { return persuadeDc; }
    public String getFlagGranted() { return flagGranted; }
    public String getRewardItemId() { return rewardItemId; }
}
