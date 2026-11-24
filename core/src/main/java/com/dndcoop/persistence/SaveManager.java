package com.dndcoop.persistence;

import com.dndcoop.campaign.AshenLanternCampaign;
import com.dndcoop.dm.DmSettings;
import com.dndcoop.model.CampaignState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class SaveManager {

    public static final String AUTOSAVE_FILE = "autosave.json";

    private final Path rootDir;
    private final Path savesDir;
    private final Path chroniclesDir;
    private final Path settingsFile;
    private final ObjectMapper mapper;

    public SaveManager() {
        this(Paths.get(System.getProperty("user.home"), ".dnd5e-coop"));
    }

    public SaveManager(Path rootDir) {
        this.rootDir = rootDir;
        this.savesDir = rootDir.resolve("saves");
        this.chroniclesDir = rootDir.resolve("chronicles");
        this.settingsFile = rootDir.resolve("settings.json");
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ensureDirs();
    }

    private void ensureDirs() {
        try {
            Files.createDirectories(savesDir);
            Files.createDirectories(chroniclesDir);
        } catch (IOException ignored) {
        }
    }

    public Path getSavesDir() {
        return savesDir;
    }

    public Path getChroniclesDir() {
        return chroniclesDir;
    }

    public void saveAuto(CampaignState state) throws IOException {
        ensureDirs();
        state.setUpdatedAt(Instant.now().toString());
        if (state.getSaveName() == null || state.getSaveName().isBlank()) {
            state.setSaveName("Autosave");
        }
        mapper.writeValue(savesDir.resolve(AUTOSAVE_FILE).toFile(), state);
    }

    public Path saveNamedSlot(CampaignState state, String slotName) throws IOException {
        ensureDirs();
        String cleanLabel = (slotName == null || slotName.isBlank()) ? "Adventure Checkpoint" : slotName.trim();
        String safeFile = sanitizeFileName(cleanLabel) + ".json";
        if (AUTOSAVE_FILE.equalsIgnoreCase(safeFile)) {
            safeFile = "slot_" + safeFile;
        }
        state.setSaveName(cleanLabel);
        state.setUpdatedAt(Instant.now().toString());
        Path target = savesDir.resolve(safeFile);
        mapper.writeValue(target.toFile(), state);
        return target;
    }

    public CampaignState loadSlot(String fileName) throws IOException {
        Path path = savesDir.resolve(fileName);
        if (!Files.exists(path)) {
            throw new IOException("Save file not found: " + path);
        }
        return mapper.readValue(path.toFile(), CampaignState.class);
    }

    public boolean hasAutosave() {
        return Files.exists(savesDir.resolve(AUTOSAVE_FILE));
    }

    public boolean deleteSlot(String fileName) {
        try {
            return Files.deleteIfExists(savesDir.resolve(fileName));
        } catch (IOException e) {
            return false;
        }
    }

    public List<SaveSlotInfo> listSaves() {
        ensureDirs();
        List<SaveSlotInfo> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(savesDir)) {
            files.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    CampaignState s = mapper.readValue(p.toFile(), CampaignState.class);
                    String fileName = p.getFileName().toString();
                    String label = AUTOSAVE_FILE.equals(fileName) ? "Autosave (" + s.getSaveName() + ")" : s.getSaveName();
                    String locName = AshenLanternCampaign.getLocation(s.getCurrentLocationId()).getName();
                    results.add(new SaveSlotInfo(
                            fileName, label, s.getCampaignTitle(), s.getCurrentAct(),
                            locName, s.getParty().size(), s.getTurnCount(),
                            s.getCombat() != null && s.getCombat().isActive(),
                            s.getUpdatedAt()));
                } catch (Exception ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        results.sort(Comparator.comparing((SaveSlotInfo i) -> !AUTOSAVE_FILE.equals(i.getFileName()))
                .thenComparing(SaveSlotInfo::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return results;
    }

    public DmSettings loadSettings() {
        try {
            if (Files.exists(settingsFile)) {
                return mapper.readValue(settingsFile.toFile(), DmSettings.class);
            }
        } catch (Exception ignored) {
        }
        return new DmSettings();
    }

    public void saveSettings(DmSettings settings) {
        ensureDirs();
        try {
            mapper.writeValue(settingsFile.toFile(), settings);
        } catch (IOException ignored) {
        }
    }

    public static String sanitizeFileName(String name) {
        String safe = name.toLowerCase().replaceAll("[^a-z0-9\\-_]+", "_").replaceAll("^_+|_+$", "");
        return safe.isBlank() ? "save_slot" : safe;
    }
}
