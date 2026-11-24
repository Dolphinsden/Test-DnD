package com.dndcoop.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.dndcoop.controller.GameSessionController;
import com.dndcoop.dm.DmSettings;
import com.dndcoop.model.CampaignState;
import com.dndcoop.persistence.SaveManager;

public class DndCoopGame extends Game {

    private SaveManager saveManager;
    private DmSettings settings;
    private GameSessionController controller;
    private GrimoireSkin skin;

    @Override
    public void create() {
        this.saveManager = new SaveManager();
        this.settings = saveManager.loadSettings();
        this.controller = new GameSessionController(saveManager, settings, r -> Gdx.app.postRunnable(r));
        this.skin = new GrimoireSkin(settings.getFontScale());

        // Async initial probe for local Ollama/LLM availability without blocking startup
        Thread probeThread = new Thread(() -> controller.getHybridDm().probeConnection(settings), "llm-probe");
        probeThread.setDaemon(true);
        probeThread.start();

        showTitleScreen();
    }

    public void showTitleScreen() {
        switchScreen(new TitleScreen(this));
    }

    public void showPartySetupScreen() {
        switchScreen(new PartySetupScreen(this));
    }

    public void showGameScreen(CampaignState state) {
        if (state != null) {
            controller.loadCampaignState(state);
        }
        switchScreen(new GameScreen(this));
    }

    public void cycleCouchFontScale() {
        settings.setFontScale(settings.getFontScale().next());
        saveManager.saveSettings(settings);
        rebuildSkinAndCurrentScreen();
    }

    public void rebuildSkinAndCurrentScreen() {
        Screen current = getScreen();
        GrimoireSkin oldSkin = this.skin;
        this.skin = new GrimoireSkin(settings.getFontScale());
        if (current instanceof GameScreen) {
            switchScreen(new GameScreen(this));
        } else if (current instanceof PartySetupScreen) {
            switchScreen(new PartySetupScreen(this));
        } else {
            switchScreen(new TitleScreen(this));
        }
        if (oldSkin != null) {
            oldSkin.dispose();
        }
    }

    private void switchScreen(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) {
            previous.dispose();
        }
    }

    public SaveManager getSaveManager() { return saveManager; }
    public DmSettings getSettings() { return settings; }
    public GameSessionController getController() { return controller; }
    public GrimoireSkin getSkin() { return skin; }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            current.dispose();
        }
        if (controller != null) {
            controller.autoSaveQuietly();
            controller.shutdown();
        }
        if (skin != null) {
            skin.dispose();
        }
        super.dispose();
    }
}
