package com.dndcoop.dm;

import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.rules.RulesEngine;

public class HybridDungeonMaster {

    private final LocalLlmClient llmClient;
    private final OfflineDungeonMaster offlineDm;
    private volatile String lastStatusLabel = "Offline DM Ready";
    private volatile boolean llmReachable = false;

    public HybridDungeonMaster() {
        this(new LocalLlmClient(), new OfflineDungeonMaster());
    }

    public HybridDungeonMaster(LocalLlmClient llmClient, OfflineDungeonMaster offlineDm) {
        this.llmClient = llmClient;
        this.offlineDm = offlineDm;
    }

    public LocalLlmClient.ConnectionTestResult probeConnection(DmSettings settings) {
        LocalLlmClient.ConnectionTestResult res = llmClient.testConnection(settings);
        this.llmReachable = res.connected;
        if (settings.getProvider() == DmSettings.Provider.OFFLINE_ONLY) {
            this.lastStatusLabel = "Offline DM";
        } else if (res.connected) {
            this.lastStatusLabel = settings.getProvider().name() + " (" + settings.getModelName() + ")";
        } else {
            this.lastStatusLabel = "Offline DM (Fallback)";
        }
        return res;
    }

    public DmDirective resolveExplorationTurn(DmSettings settings, CampaignState state,
                                              CharacterSheet actor, String playerPrompt,
                                              RulesEngine rules) {
        if (settings != null && settings.getProvider() != DmSettings.Provider.OFFLINE_ONLY) {
            try {
                DmDirective llmResult = llmClient.generateExplorationTurn(settings, state, actor, playerPrompt, rules);
                this.llmReachable = true;
                this.lastStatusLabel = llmResult.getDmSourceLabel();
                return llmResult;
            } catch (Exception ignored) {
                this.llmReachable = false;
                this.lastStatusLabel = "Offline DM (Fallback)";
            }
        } else {
            this.llmReachable = false;
            this.lastStatusLabel = "Offline DM";
        }
        DmDirective fallback = offlineDm.processExplorationAction(state, actor, playerPrompt, rules);
        fallback.setDmSourceLabel(this.lastStatusLabel);
        return fallback;
    }

    public String getLastStatusLabel() {
        return lastStatusLabel;
    }

    public boolean isLlmReachable() {
        return llmReachable;
    }
}
