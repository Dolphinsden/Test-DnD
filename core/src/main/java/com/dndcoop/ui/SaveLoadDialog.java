package com.dndcoop.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.dndcoop.model.CampaignState;
import com.dndcoop.persistence.ChronicleExporter;
import com.dndcoop.persistence.SaveSlotInfo;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class SaveLoadDialog extends Dialog {

    private final DndCoopGame game;
    private final boolean allowSaving;
    private final Consumer<CampaignState> onLoaded;

    private Table slotsTable;
    private TextField saveNameField;
    private Label feedbackLabel;

    public SaveLoadDialog(DndCoopGame game, boolean allowSaving, Consumer<CampaignState> onLoaded) {
        super(allowSaving ? "Save / Load Session" : "Load Saved Session", game.getSkin());
        this.game = game;
        this.allowSaving = allowSaving;
        this.onLoaded = onLoaded;
        buildUi();
    }

    private void buildUi() {
        GrimoireSkin skin = game.getSkin();
        Table content = getContentTable();
        content.pad(14).defaults().pad(6).left();

        Label pathLabel = new Label("Local Save Directory: " + game.getSaveManager().getSavesDir().toAbsolutePath(), skin, "muted");
        pathLabel.setWrap(true);
        content.add(pathLabel).width(660).row();

        if (allowSaving && game.getController().getState() != null) {
            Table saveRow = new Table();
            saveRow.defaults().pad(4);
            saveNameField = new TextField(game.getController().getState().getSaveName(), skin);
            saveNameField.setMessageText("Enter checkpoint name...");
            TextButton saveBtn = new TextButton("Save Named Slot", skin, "gold");
            saveBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        Path saved = game.getSaveManager().saveNamedSlot(game.getController().getState(), saveNameField.getText());
                        feedbackLabel.setText("Saved session to " + saved.getFileName());
                        refreshSlots();
                    } catch (Exception e) {
                        feedbackLabel.setText("Save failed: " + e.getMessage());
                    }
                }
            });
            TextButton exportBtn = new TextButton("Export MD Chronicle", skin, "emerald");
            exportBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        Path out = ChronicleExporter.exportToFile(
                                game.getController().getState(),
                                game.getSaveManager().getChroniclesDir());
                        feedbackLabel.setText("Exported Chronicle: " + out.toAbsolutePath());
                    } catch (Exception e) {
                        feedbackLabel.setText("Export failed: " + e.getMessage());
                    }
                }
            });
            saveRow.add(saveNameField).expandX().fillX().height(34);
            saveRow.add(saveBtn).height(34);
            saveRow.add(exportBtn).height(34);
            content.add(saveRow).width(660).row();
        }

        slotsTable = new Table();
        slotsTable.top().left().defaults().pad(4).fillX().expandX();
        ScrollPane scroll = new ScrollPane(slotsTable, skin);
        scroll.setFadeScrollBars(false);
        content.add(scroll).width(660).height(310).row();

        feedbackLabel = new Label("Auto-save updates automatically after every turn.", skin, "mono");
        feedbackLabel.setWrap(true);
        content.add(feedbackLabel).width(660).row();

        TextButton closeBtn = new TextButton("Close", skin, "default");
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        getButtonTable().pad(8).add(closeBtn).width(120).height(34);

        refreshSlots();
    }

    private void refreshSlots() {
        GrimoireSkin skin = game.getSkin();
        slotsTable.clear();
        List<SaveSlotInfo> saves = game.getSaveManager().listSaves();
        if (saves.isEmpty()) {
            slotsTable.add(new Label("No local save files found yet.", skin, "muted")).pad(16).row();
            return;
        }
        for (SaveSlotInfo info : saves) {
            Table card = new Table(skin);
            card.setBackground(skin.getDrawable("card"));
            card.pad(10);

            Table meta = new Table();
            meta.left();
            String title = info.getSaveName() + (info.isInCombat() ? " [IN COMBAT]" : "");
            meta.add(new Label(title, skin, info.isInCombat() ? "crimson" : "gold")).left().row();
            meta.add(new Label("Act " + info.getAct() + " — " + info.getLocationName()
                    + " | Party: " + info.getPartySize() + " | Turns: " + info.getTurnCount(), skin, "small")).left().row();
            meta.add(new Label("File: " + info.getFileName() + " | Updated: " + info.getUpdatedAt(), skin, "muted")).left();

            TextButton loadBtn = new TextButton("Load", skin, "gold");
            loadBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        CampaignState loaded = game.getSaveManager().loadSlot(info.getFileName());
                        hide();
                        if (onLoaded != null) {
                            onLoaded.accept(loaded);
                        }
                    } catch (Exception e) {
                        feedbackLabel.setText("Load failed: " + e.getMessage());
                    }
                }
            });

            TextButton delBtn = new TextButton("Delete", skin, "crimson");
            delBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.getSaveManager().deleteSlot(info.getFileName());
                    feedbackLabel.setText("Deleted " + info.getFileName());
                    refreshSlots();
                }
            });

            card.add(meta).expandX().fillX().left();
            card.add(loadBtn).width(82).height(32).padLeft(8);
            card.add(delBtn).width(82).height(32).padLeft(6);
            slotsTable.add(card).expandX().fillX().row();
        }
    }

    public void showCentered(Stage stage) {
        show(stage);
        setPosition(Math.round((stage.getWidth() - getWidth()) / 2f),
                Math.round((stage.getHeight() - getHeight()) / 2f));
    }
}
