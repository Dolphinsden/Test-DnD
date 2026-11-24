package com.dndcoop.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dndcoop.dm.DmSettings;
import com.dndcoop.dm.LocalLlmClient;

public class DmSettingsDialog extends Dialog {

    private final DndCoopGame game;
    private final Runnable onSaved;

    private DmSettings.Provider selectedProvider;
    private TextField urlField;
    private TextField modelField;
    private CheckBox autoRollCheck;
    private Label statusLabel;
    private Label providerLabel;

    public DmSettingsDialog(DndCoopGame game, Runnable onSaved) {
        super("Local Dungeon Master Settings", game.getSkin());
        this.game = game;
        this.onSaved = onSaved;
        this.selectedProvider = game.getSettings().getProvider();
        buildUi();
    }

    private void buildUi() {
        GrimoireSkin skin = game.getSkin();
        DmSettings s = game.getSettings();
        Table content = getContentTable();
        content.pad(16).defaults().pad(6).left();

        Label intro = new Label("Choose a local LLM preset (Ollama is default) or run purely Offline.\nIf a local LLM server is unreachable, the Offline DM automatically takes over.", skin, "muted");
        intro.setWrap(true);
        content.add(intro).width(560).colspan(2).row();

        // Preset buttons
        Table presets = new Table();
        presets.defaults().pad(4).height(34);
        for (DmSettings.Provider p : DmSettings.Provider.values()) {
            String shortLabel = p == DmSettings.Provider.OLLAMA ? "Ollama"
                    : p == DmSettings.Provider.LM_STUDIO ? "LM Studio"
                    : p == DmSettings.Provider.OPENAI_COMPATIBLE ? "OpenAI-Compat"
                    : "Offline Only";
            TextButton btn = new TextButton(shortLabel, skin, p == selectedProvider ? "gold" : "default");
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    selectedProvider = p;
                    if (p != DmSettings.Provider.OFFLINE_ONLY) {
                        urlField.setText(p.getDefaultUrl());
                        modelField.setText(p.getDefaultModel());
                    }
                    providerLabel.setText("Selected Provider: " + p.getDisplayName());
                }
            });
            presets.add(btn).expandX().fillX();
        }
        content.add(presets).colspan(2).fillX().row();

        providerLabel = new Label("Selected Provider: " + selectedProvider.getDisplayName(), skin, "gold");
        content.add(providerLabel).colspan(2).padTop(4).row();

        content.add(new Label("Endpoint URL:", skin, "bold")).width(140);
        urlField = new TextField(s.getBaseUrl(), skin);
        urlField.setMessageText("http://localhost:11434");
        content.add(urlField).width(420).height(34).row();

        content.add(new Label("Model Name:", skin, "bold")).width(140);
        modelField = new TextField(s.getModelName(), skin);
        modelField.setMessageText("llama3.2");
        content.add(modelField).width(420).height(34).row();

        autoRollCheck = new CheckBox(" Auto-Roll Digital Dice (skip interactive digital/physical roll banner)", skin);
        autoRollCheck.setChecked(s.isAutoRollDice());
        content.add(autoRollCheck).colspan(2).padTop(6).row();

        statusLabel = new Label("Current status: " + game.getController().getHybridDm().getLastStatusLabel(), skin, "mono");
        statusLabel.setWrap(true);
        content.add(statusLabel).width(560).colspan(2).padTop(4).row();

        Table actions = getButtonTable();
        actions.pad(10).defaults().pad(6).height(36);

        TextButton testBtn = new TextButton("Test Connection & Detect Models", skin, "default");
        testBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DmSettings probe = snapshotForm();
                statusLabel.setText("Testing connection to " + probe.getBaseUrl() + "...");
                Thread t = new Thread(() -> {
                    LocalLlmClient.ConnectionTestResult res = game.getController().getHybridDm().probeConnection(probe);
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText(res.message);
                        if (res.connected && !res.detectedModels.isEmpty()
                                && (modelField.getText().isBlank() || "local-model".equals(modelField.getText()))) {
                            modelField.setText(res.detectedModels.get(0));
                        }
                    });
                }, "dm-test-conn");
                t.setDaemon(true);
                t.start();
            }
        });

        TextButton saveBtn = new TextButton("Save Settings", skin, "gold");
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DmSettings target = game.getSettings();
                target.setProvider(selectedProvider);
                target.setBaseUrl(urlField.getText().trim());
                target.setModelName(modelField.getText().trim());
                target.setAutoRollDice(autoRollCheck.isChecked());
                game.getSaveManager().saveSettings(target);
                Thread t = new Thread(() -> {
                    game.getController().getHybridDm().probeConnection(target);
                    Gdx.app.postRunnable(() -> {
                        if (onSaved != null) onSaved.run();
                    });
                }, "dm-reprobe");
                t.setDaemon(true);
                t.start();
                hide();
            }
        });

        TextButton closeBtn = new TextButton("Cancel", skin, "default");
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });

        actions.add(testBtn);
        actions.add(saveBtn);
        actions.add(closeBtn);
    }

    private DmSettings snapshotForm() {
        DmSettings copy = new DmSettings();
        copy.setProvider(selectedProvider);
        copy.setBaseUrl(urlField.getText().trim());
        copy.setModelName(modelField.getText().trim());
        copy.setAutoRollDice(autoRollCheck.isChecked());
        return copy;
    }

    public void showCentered(Stage stage) {
        show(stage);
        setPosition(Math.round((stage.getWidth() - getWidth()) / 2f),
                Math.round((stage.getHeight() - getHeight()) / 2f));
    }
}
