package com.dndcoop;

import com.dndcoop.dm.DmDirective;
import com.dndcoop.dm.DmSettings;
import com.dndcoop.dm.HybridDungeonMaster;
import com.dndcoop.dm.LocalLlmClient;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.model.Skill;
import com.dndcoop.rules.CharacterFactory;
import com.dndcoop.rules.DiceRoller;
import com.dndcoop.rules.RulesEngine;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LocalLlmIntegrationTest {

    @Test
    void ollamaLocalServerIntegrationAndAutomaticOfflineFallbackWork() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();

        server.createContext("/api/tags", exchange -> {
            String body = "{\"models\":[{\"name\":\"llama3.2:latest\"}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/api/chat", exchange -> {
            String innerJson = "{\\\"narrative\\\":\\\"The tavern rafters creak as Martha points toward the mist.\\\",\\\"skill_check\\\":{\\\"skill\\\":\\\"Perception\\\",\\\"dc\\\":13,\\\"label\\\":\\\"Spot the hooded watcher\\\",\\\"success_text\\\":\\\"You spot a shadowy figure slipping north!\\\",\\\"failure_text\\\":\\\"Only rain blurs the windowpanes.\\\"}}";
            String body = "{\"message\":{\"role\":\"assistant\",\"content\":\"" + innerJson + "\"}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.start();
        try {
            DmSettings settings = new DmSettings();
            settings.setProvider(DmSettings.Provider.OLLAMA);
            settings.setBaseUrl("http://127.0.0.1:" + port);
            settings.setModelName("llama3.2:latest");

            HybridDungeonMaster dm = new HybridDungeonMaster();
            LocalLlmClient.ConnectionTestResult probe = dm.probeConnection(settings);
            assertTrue(probe.connected);
            assertTrue(probe.detectedModels.contains("llama3.2:latest"));

            CampaignState state = new CampaignState();
            CharacterSheet lyra = CharacterFactory.createLyraWizard();
            state.setParty(List.of(lyra));
            state.setActiveCharacterId(lyra.getId());

            RulesEngine rules = new RulesEngine(new DiceRoller(new Random(5L)));
            DmDirective dir = dm.resolveExplorationTurn(settings, state, lyra, "I peer out the rainy tavern window.", rules);

            assertFalse(dir.isUsedFallback());
            assertTrue(dir.getNarrative().contains("Martha points toward the mist"));
            assertNotNull(dir.getRequestedRoll());
            assertEquals(PendingRoll.RollType.ABILITY_CHECK, dir.getRequestedRoll().getRollType());
            assertEquals(Skill.PERCEPTION, dir.getRequestedRoll().getSkill());
            assertEquals(13, dir.getRequestedRoll().getTargetDcOrAc());
        } finally {
            server.stop(0);
        }

        // Now verify graceful automatic fallback when server is stopped!
        DmSettings deadSettings = new DmSettings();
        deadSettings.setProvider(DmSettings.Provider.OLLAMA);
        deadSettings.setBaseUrl("http://127.0.0.1:" + port);
        deadSettings.setModelName("llama3.2:latest");

        HybridDungeonMaster dm = new HybridDungeonMaster();
        CampaignState state = new CampaignState();
        CharacterSheet thorne = CharacterFactory.createThorneFighter();
        state.setParty(List.of(thorne));
        DmDirective fallback = dm.resolveExplorationTurn(deadSettings, state, thorne, "I look around the tavern.",
                new RulesEngine(new DiceRoller(new Random(5L))));
        assertTrue(fallback.isUsedFallback());
        assertTrue(fallback.getDmSourceLabel().contains("Fallback"));
        assertNotNull(fallback.getNarrative());
    }
}
