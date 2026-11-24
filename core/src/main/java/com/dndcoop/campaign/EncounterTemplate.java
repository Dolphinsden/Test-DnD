package com.dndcoop.campaign;

import com.dndcoop.model.Monster;

import java.util.ArrayList;
import java.util.List;

public class EncounterTemplate {

    private final String id;
    private final String name;
    private final String introNarrative;
    private final List<Monster> monsterPrototypes;
    private final String victoryFlag;
    private final int goldReward;
    private final String rewardItemId;

    public EncounterTemplate(String id, String name, String introNarrative, List<Monster> monsterPrototypes,
                             String victoryFlag, int goldReward, String rewardItemId) {
        this.id = id;
        this.name = name;
        this.introNarrative = introNarrative;
        this.monsterPrototypes = monsterPrototypes;
        this.victoryFlag = victoryFlag;
        this.goldReward = goldReward;
        this.rewardItemId = rewardItemId;
    }

    public List<Monster> spawnMonsters() {
        List<Monster> list = new ArrayList<>();
        for (int i = 0; i < monsterPrototypes.size(); i++) {
            Monster proto = monsterPrototypes.get(i);
            list.add(proto.copyWithId(id + "_m" + (i + 1), proto.getName()));
        }
        return list;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getIntroNarrative() { return introNarrative; }
    public List<Monster> getMonsterPrototypes() { return monsterPrototypes; }
    public String getVictoryFlag() { return victoryFlag; }
    public int getGoldReward() { return goldReward; }
    public String getRewardItemId() { return rewardItemId; }
}
