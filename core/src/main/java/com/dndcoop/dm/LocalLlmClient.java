package com.dndcoop.dm;

import com.dndcoop.campaign.AshenLanternCampaign;
import com.dndcoop.campaign.LocationNode;
import com.dndcoop.campaign.NpcProfile;
import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.LogEntry;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.model.Skill;
import com.dndcoop.rules.RulesEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LocalLlmClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public static final class ConnectionTestResult {
        public final boolean connected;
        public final String message;
        public final List<String> detectedModels;

        public ConnectionTestResult(boolean connected, String message, List<String> detectedModels) {
            this.connected = connected;
            this.message = message;
            this.detectedModels = detectedModels;
        }
    }

    public ConnectionTestResult testConnection(DmSettings settings) {
        if (settings == null || settings.getProvider() == DmSettings.Provider.OFFLINE_ONLY) {
            return new ConnectionTestResult(false, "Offline Fallback DM mode selected.", List.of());
        }
        String base = trimTrailingSlash(settings.getBaseUrl());
        if (base.isBlank()) {
            return new ConnectionTestResult(false, "No local endpoint URL configured.", List.of());
        }
        try {
            String probeUrl = settings.getProvider() == DmSettings.Provider.OLLAMA
                    ? base + "/api/tags"
                    : base + "/v1/models";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(probeUrl))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                List<String> models = extractModels(resp.body(), settings.getProvider());
                String detail = models.isEmpty()
                        ? "Connected to " + base + " (no models listed yet)."
                        : "Connected! Models found: " + String.join(", ", models.subList(0, Math.min(5, models.size())));
                return new ConnectionTestResult(true, detail, models);
            }
            return new ConnectionTestResult(false, "Server responded HTTP " + resp.statusCode() + " at " + probeUrl, List.of());
        } catch (Exception e) {
            return new ConnectionTestResult(false, "Could not reach " + base + " (" + e.getClass().getSimpleName() + "). Offline DM will be used automatically.", List.of());
        }
    }

    public DmDirective generateExplorationTurn(DmSettings settings, CampaignState state,
                                               CharacterSheet actor, String playerPrompt,
                                               RulesEngine rules) throws Exception {
        String base = trimTrailingSlash(settings.getBaseUrl());
        String systemPrompt = buildSystemPrompt(state);
        String userPrompt = buildUserPrompt(state, actor, playerPrompt);

        String rawContent;
        if (settings.getProvider() == DmSettings.Provider.OLLAMA) {
            rawContent = callOllamaChat(base, settings.getModelName(), systemPrompt, userPrompt, settings.getTimeoutSeconds());
        } else {
            rawContent = callOpenAiCompatibleChat(base, settings.getModelName(), systemPrompt, userPrompt, settings.getTimeoutSeconds());
        }

        return parseDirective(rawContent, settings, state, actor, playerPrompt, rules);
    }

    private String callOllamaChat(String base, String model, String systemPrompt, String userPrompt, int timeoutSec) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model.isBlank() ? "llama3.2" : model);
        root.put("stream", false);
        root.put("format", "json");
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(base + "/api/chat"))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("Ollama HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode json = mapper.readTree(resp.body());
        return json.path("message").path("content").asText("");
    }

    private String callOpenAiCompatibleChat(String base, String model, String systemPrompt, String userPrompt, int timeoutSec) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model.isBlank() ? "local-model" : model);
        root.put("temperature", 0.7);
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);

        String endpoint = base.endsWith("/v1") ? base + "/chat/completions" : base + "/v1/chat/completions";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(timeoutSec))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IllegalStateException("OpenAI-compat HTTP " + resp.statusCode() + ": " + resp.body());
        }
        JsonNode json = mapper.readTree(resp.body());
        return json.path("choices").path(0).path("message").path("content").asText("");
    }

    private DmDirective parseDirective(String rawContent, DmSettings settings, CampaignState state,
                                       CharacterSheet actor, String playerPrompt, RulesEngine rules) {
        DmDirective dir = new DmDirective();
        String providerTag = settings.getProvider() == DmSettings.Provider.OLLAMA ? "Ollama" : "Local LLM";
        dir.setDmSourceLabel("DM [" + providerTag + ": " + settings.getModelName() + "]");
        dir.setUsedFallback(false);

        String extractedJson = extractJsonObject(rawContent);
        if (extractedJson != null) {
            try {
                JsonNode node = mapper.readTree(extractedJson);
                String narrative = node.path("narrative").asText("").trim();
                if (!narrative.isBlank()) {
                    dir.setNarrative(narrative);
                }
                String moveId = node.path("move_to_location_id").asText("").trim();
                if (!moveId.isBlank() && isValidExit(state, moveId)) {
                    dir.setMoveToLocationId(moveId);
                }
                String encId = node.path("start_encounter_id").asText("").trim();
                if (!encId.isBlank() && AshenLanternCampaign.getEncounter(encId) != null
                        && !state.getDefeatedEncounters().contains(encId)) {
                    dir.setStartEncounterId(encId);
                }
                String rest = node.path("rest_type").asText("").trim().toLowerCase();
                if ("short".equals(rest) || "long".equals(rest)) {
                    dir.setRestType(rest);
                }
                dir.setGrantGold(Math.max(0, Math.min(100, node.path("grant_gold").asInt(0))));
                dir.setGrantXp(Math.max(0, Math.min(200, node.path("grant_xp").asInt(0))));

                JsonNode check = node.path("skill_check");
                if (check.isObject() && !check.path("skill").asText("").isBlank()) {
                    Skill skill = Skill.fromName(check.path("skill").asText(""));
                    if (skill != null) {
                        int dc = Math.max(8, Math.min(20, check.path("dc").asInt(12)));
                        String label = check.path("label").asText(actor.getName() + " makes a " + skill.getDisplayName() + " check");
                        PendingRoll pr = rules.buildAbilityCheckRoll(actor, skill, Ability.WIS, dc, label, playerPrompt);
                        pr.setSuccessNarrative(check.path("success_text").asText("Your effort succeeds brilliantly!"));
                        pr.setFailureNarrative(check.path("failure_text").asText("Despite your best effort, the attempt falls short."));
                        pr.setXpOnSuccess(25);
                        dir.setRequestedRoll(pr);
                    }
                }
                if (dir.getNarrative() != null && !dir.getNarrative().isBlank()) {
                    return dir;
                }
            } catch (Exception ignored) {
                // Fall through to raw prose narrative
            }
        }
        String fallbackText = rawContent == null ? "" : rawContent.trim();
        if (fallbackText.isBlank()) {
            throw new IllegalStateException("Local LLM returned an empty response.");
        }
        dir.setNarrative(fallbackText);
        return dir;
    }

    private boolean isValidExit(CampaignState state, String targetId) {
        LocationNode current = AshenLanternCampaign.getLocation(state.getCurrentLocationId());
        if (!current.getExits().containsKey(targetId)) {
            return false;
        }
        String reqFlag = current.getLockedExitRequiredFlags().get(targetId);
        return reqFlag == null || state.hasFlag(reqFlag);
    }

    private String buildSystemPrompt(CampaignState state) {
        LocationNode loc = AshenLanternCampaign.getLocation(state.getCurrentLocationId());
        StringBuilder sb = new StringBuilder();
        sb.append("You are the Dungeon Master for a couch co-op D&D 5E campaign titled '")
                .append(state.getCampaignTitle()).append("'.\n");
        sb.append("Act: ").append(state.getCurrentAct()).append(" | Location: ").append(loc.getName())
                .append(" (id=").append(loc.getId()).append(")\n");
        sb.append("Location Description: ").append(loc.getDescription()).append("\n");
        sb.append("Available Exits:\n");
        for (Map.Entry<String, String> ex : loc.getExits().entrySet()) {
            String req = loc.getLockedExitRequiredFlags().get(ex.getKey());
            boolean unlocked = req == null || state.hasFlag(req);
            sb.append("  - id='").append(ex.getKey()).append("': ").append(ex.getValue())
                    .append(unlocked ? " [UNLOCKED]" : " [LOCKED - needs " + req + "]").append("\n");
        }
        if (!loc.getNpcs().isEmpty()) {
            sb.append("NPCs here: ");
            for (NpcProfile npc : loc.getNpcs()) {
                sb.append(npc.getName()).append(" (").append(npc.getTitle()).append("), ");
            }
            sb.append("\n");
        }
        if (loc.getEncounterId() != null && !state.getDefeatedEncounters().contains(loc.getEncounterId())) {
            sb.append("Undefeated encounter lurking here: id='").append(loc.getEncounterId()).append("'\n");
        }
        sb.append("Party:\n");
        for (CharacterSheet h : state.getParty()) {
            sb.append("  - ").append(h.getName()).append(" (").append(h.getRace().getDisplayName())
                    .append(" ").append(h.getCharacterClass().getDisplayName())
                    .append(" Lvl ").append(h.getLevel())
                    .append(", HP ").append(h.getCurrentHp()).append("/").append(h.getMaxHp())
                    .append(", AC ").append(h.getArmorClass()).append(")\n");
        }
        sb.append("\nRespond ONLY with a valid JSON object matching this schema:\n");
        sb.append("{\n");
        sb.append("  \"narrative\": \"2-4 vivid sentences of D&D 5E DM narration reacting directly to the player.\",\n");
        sb.append("  \"move_to_location_id\": \"optional unlocked exit id if player travels there, else empty string\",\n");
        sb.append("  \"start_encounter_id\": \"optional undefeated encounter id if combat should erupt now, else empty string\",\n");
        sb.append("  \"rest_type\": \"optional 'short' or 'long' if resting, else empty string\",\n");
        sb.append("  \"skill_check\": null OR {\"skill\":\"Perception\",\"dc\":12,\"label\":\"...\",\"success_text\":\"...\",\"failure_text\":\"...\"}\n");
        sb.append("}\n");
        sb.append("Never roll dice yourself; if an uncertain action needs a D&D 5E roll, put it in skill_check so the game engine lets the player roll.");
        return sb.toString();
    }

    private String buildUserPrompt(CampaignState state, CharacterSheet actor, String playerPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recent Adventure Log:\n");
        List<LogEntry> log = state.getLog();
        int start = Math.max(0, log.size() - 6);
        for (int i = start; i < log.size(); i++) {
            LogEntry e = log.get(i);
            sb.append("[").append(e.getSpeaker()).append("] ").append(e.getText()).append("\n");
        }
        sb.append("\nActing Hero: ").append(actor.getName()).append(" (").append(actor.getCharacterClass().getDisplayName()).append(")\n");
        sb.append("Player Action: \"").append(playerPrompt).append("\"");
        return sb.toString();
    }

    private List<String> extractModels(String body, DmSettings.Provider provider) {
        List<String> list = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode arr = provider == DmSettings.Provider.OLLAMA ? root.path("models") : root.path("data");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String name = n.has("name") ? n.path("name").asText() : n.path("id").asText("");
                    if (!name.isBlank()) list.add(name);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private String extractJsonObject(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return null;
    }

    private String trimTrailingSlash(String url) {
        if (url == null) return "";
        String t = url.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
