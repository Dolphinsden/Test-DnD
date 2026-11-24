package com.dndcoop;

import com.dndcoop.controller.GameSessionController;
import com.dndcoop.dm.DmSettings;
import com.dndcoop.dm.HybridDungeonMaster;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.persistence.ChronicleExporter;
import com.dndcoop.persistence.SaveManager;
import com.dndcoop.persistence.SaveSlotInfo;
import com.dndcoop.rules.CharacterFactory;
import com.dndcoop.rules.DiceRoller;
import com.dndcoop.rules.RulesEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CampaignOfflineDmAndPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void offlineDmHandlesFreeFormExplorationChecksAndTravel() {
        SaveManager saveManager = new SaveManager(tempDir);
        DmSettings settings = new DmSettings();
        settings.setProvider(DmSettings.Provider.OFFLINE_ONLY);
        settings.setAutoRollDice(false);

        RulesEngine rules = new RulesEngine(new DiceRoller(new Random(55L)));
        GameSessionController ctrl = new GameSessionController(
                rules, new HybridDungeonMaster(), saveManager, settings, Runnable::run, false);

        CharacterSheet seraphina = CharacterFactory.createSeraphinaCleric();
        CharacterSheet thorne = CharacterFactory.createThorneFighter();
        CampaignState state = ctrl.startNewCampaign(List.of(seraphina, thorne), "Hollowbrook Run");

        // Free-form NPC persuasion prompt in tavern
        ctrl.submitFreeText("I ask Martha Brindle for any secret clues about who stole the Ashen Lantern.", null);
        assertNotNull(state.getPendingRoll(), "Asking Martha for clues should trigger a 5E Persuasion check");
        assertEquals(PendingRoll.RollType.ABILITY_CHECK, state.getPendingRoll().getRollType());

        // Succeed with physical d20 = 18
        ctrl.resolvePendingRoll(18, null);
        assertTrue(state.hasFlag("martha_secret"));
        assertTrue(seraphina.getXp() > 0);

        // Free-form travel prompt outside to Village Square
        ctrl.submitFreeText("We step outside into Hollowbrook Village Square.", null);
        assertEquals("hollowbrook_square", state.getCurrentLocationId());
    }

    @Test
    void saveLoadRoundTripPreservesCompleteSessionAndExportsChronicle() throws Exception {
        SaveManager saveManager = new SaveManager(tempDir);
        DmSettings settings = new DmSettings();
        settings.setProvider(DmSettings.Provider.OFFLINE_ONLY);
        settings.setAutoRollDice(false);

        GameSessionController ctrl = new GameSessionController(
                new RulesEngine(new DiceRoller(new Random(11L))),
                new HybridDungeonMaster(), saveManager, settings, Runnable::run, false);

        CampaignState state = ctrl.startNewCampaign(
                List.of(CharacterFactory.createThorneFighter(), CharacterFactory.createLyraWizard()),
                "Couch Night Session 1");
        ctrl.submitFreeText("I examine the tavern noticeboard carefully.", null);
        assertNotNull(state.getPendingRoll());

        // Save named slot while a PendingRoll is active!
        Path savedPath = saveManager.saveNamedSlot(state, "Before Noticeboard Roll");
        assertTrue(Files.exists(savedPath));

        List<SaveSlotInfo> slots = saveManager.listSaves();
        assertTrue(slots.size() >= 2, "Should list both autosave and named slot");

        CampaignState reloaded = saveManager.loadSlot(savedPath.getFileName().toString());
        assertEquals("Before Noticeboard Roll", reloaded.getSaveName());
        assertEquals(2, reloaded.getParty().size());
        assertEquals("Thorne Ironshield", reloaded.getParty().get(0).getName());
        assertNotNull(reloaded.getPendingRoll());
        assertEquals(state.getLog().size(), reloaded.getLog().size());

        // Export Markdown Chronicle
        Path chronicle = ChronicleExporter.exportToFile(reloaded, saveManager.getChroniclesDir());
        assertTrue(Files.exists(chronicle));
        String md = Files.readString(chronicle);
        assertTrue(md.contains("# The Ashen Lantern of Hollowbrook"));
        assertTrue(md.contains("Thorne Ironshield"));
        assertTrue(md.contains("Lyra Dawnwhisper"));
    }
}
