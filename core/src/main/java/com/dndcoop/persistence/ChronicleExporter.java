package com.dndcoop.persistence;

import com.dndcoop.campaign.AshenLanternCampaign;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.LogEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ChronicleExporter {

    private ChronicleExporter() {
    }

    public static String buildMarkdown(CampaignState state) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(state.getCampaignTitle()).append(" — Adventure Chronicle\n\n");
        md.append("- **Save Name:** ").append(state.getSaveName()).append("\n");
        md.append("- **Current Act:** Act ").append(state.getCurrentAct()).append("\n");
        md.append("- **Location:** ").append(AshenLanternCampaign.getLocation(state.getCurrentLocationId()).getName()).append("\n");
        md.append("- **Turns Played:** ").append(state.getTurnCount()).append("\n");
        md.append("- **Exported:** ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");

        md.append("## The Party\n\n");
        for (CharacterSheet hero : state.getParty()) {
            md.append("### ").append(hero.getName()).append(" (").append(hero.getPlayerName()).append(")\n");
            md.append("- **").append(hero.getRace().getDisplayName()).append(" ")
                    .append(hero.getCharacterClass().getDisplayName()).append(" — Level ").append(hero.getLevel())
                    .append("** (XP: ").append(hero.getXp()).append(")\n");
            md.append("- **HP:** ").append(hero.getCurrentHp()).append("/").append(hero.getMaxHp())
                    .append(" | **AC:** ").append(hero.getArmorClass())
                    .append(" | **Gold:** ").append(hero.getGold()).append(" gp\n");
            if (hero.getBackstory() != null && !hero.getBackstory().isBlank()) {
                md.append("- *Backstory:* ").append(hero.getBackstory()).append("\n");
            }
            md.append("\n");
        }

        md.append("## Quest Journal\n\n");
        for (String line : AshenLanternCampaign.getQuestJournalLines(state)) {
            md.append("- ").append(line).append("\n");
        }

        md.append("\n## Session Log\n\n");
        for (LogEntry entry : state.getLog()) {
            switch (entry.getType()) {
                case DM:
                    md.append("> **").append(entry.getSpeaker()).append(":** ")
                            .append(entry.getText().replace("\n", "\n> ")).append("\n\n");
                    break;
                case PLAYER:
                    md.append("**").append(entry.getSpeaker()).append(":** *\"").append(entry.getText()).append("\"*\n\n");
                    break;
                case DICE:
                    md.append("- 🎲 **").append(entry.getSpeaker()).append(":** ").append(entry.getText());
                    if (entry.getDetail() != null && !entry.getDetail().isBlank()) {
                        md.append(" (`").append(entry.getDetail()).append("`)");
                    }
                    md.append("\n\n");
                    break;
                case COMBAT:
                    md.append("⚔️ **").append(entry.getSpeaker()).append("** — ").append(entry.getText()).append("\n\n");
                    break;
                case SYSTEM:
                    md.append("*[").append(entry.getText()).append("]*\n\n");
                    break;
            }
        }
        return md.toString();
    }

    public static Path exportToFile(CampaignState state, Path chroniclesDir) throws IOException {
        Files.createDirectories(chroniclesDir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safe = SaveManager.sanitizeFileName(state.getSaveName() != null ? state.getSaveName() : "chronicle");
        Path out = chroniclesDir.resolve(safe + "_" + stamp + ".md");
        Files.writeString(out, buildMarkdown(state), StandardCharsets.UTF_8);
        return out;
    }
}
