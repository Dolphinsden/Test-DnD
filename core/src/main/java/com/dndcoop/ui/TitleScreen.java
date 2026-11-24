package com.dndcoop.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.dndcoop.model.CampaignState;
import com.dndcoop.persistence.SaveManager;

public class TitleScreen extends ScreenAdapter {

    private final DndCoopGame game;
    private Stage stage;
    private Label dmStatusLabel;

    public TitleScreen(DndCoopGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        GrimoireSkin skin = game.getSkin();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(32);

        // Top right quick font toggle
        Table topBar = new Table();
        TextButton fontBtn = new TextButton("Font: " + game.getSettings().getFontScale().getLabel(), skin);
        fontBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.cycleCouchFontScale();
            }
        });
        topBar.add().expandX();
        topBar.add(fontBtn).height(34).pad(4);
        root.add(topBar).expandX().fillX().row();

        // Center Grimoire Card
        Table card = new Table(skin);
        card.setBackground(skin.getDrawable("panel-gold"));
        card.pad(36, 48, 36, 48);

        Label emblem = new Label("D&D 5TH EDITION  •  COUCH CO-OP GRIMOIRE", skin, "gold");
        emblem.setAlignment(Align.center);
        card.add(emblem).padBottom(8).row();

        Label title = new Label("The Ashen Lantern of Hollowbrook", skin, "title");
        title.setAlignment(Align.center);
        card.add(title).padBottom(12).row();

        Label subtitle = new Label(
                "Free-form text adventure out of combat  |  Strict D&D 5E Initiative & Tactical Combat\n"
                        + "Hybrid Local Dungeon Master (Ollama / LM Studio / Offline Fallback)  |  Digital & Physical Dice",
                skin, "serif");
        subtitle.setAlignment(Align.center);
        subtitle.setWrap(true);
        card.add(subtitle).width(680).padBottom(28).row();

        Table buttons = new Table();
        buttons.defaults().width(340).height(46).pad(7);

        if (game.getSaveManager().hasAutosave()) {
            TextButton continueBtn = new TextButton("Continue Last Session (Auto-Save)", skin, "gold");
            continueBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        CampaignState loaded = game.getSaveManager().loadSlot(SaveManager.AUTOSAVE_FILE);
                        game.showGameScreen(loaded);
                    } catch (Exception e) {
                        dmStatusLabel.setText("Could not load autosave: " + e.getMessage());
                    }
                }
            });
            buttons.add(continueBtn).row();
        }

        TextButton newPartyBtn = new TextButton("Assemble New Couch Co-op Party", skin,
                game.getSaveManager().hasAutosave() ? "default" : "gold");
        newPartyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showPartySetupScreen();
            }
        });
        buttons.add(newPartyBtn).row();

        TextButton loadBtn = new TextButton("Load Saved Session", skin, "default");
        loadBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SaveLoadDialog dlg = new SaveLoadDialog(game, false, game::showGameScreen);
                dlg.showCentered(stage);
            }
        });
        buttons.add(loadBtn).row();

        TextButton dmSettingsBtn = new TextButton("Local Dungeon Master Settings", skin, "default");
        dmSettingsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DmSettingsDialog dlg = new DmSettingsDialog(game, () ->
                        dmStatusLabel.setText("DM Engine: " + game.getController().getHybridDm().getLastStatusLabel()));
                dlg.showCentered(stage);
            }
        });
        buttons.add(dmSettingsBtn).row();

        TextButton quitBtn = new TextButton("Quit to Desktop", skin, "crimson");
        quitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        buttons.add(quitBtn).row();

        card.add(buttons).padBottom(20).row();

        dmStatusLabel = new Label("DM Engine: " + game.getController().getHybridDm().getLastStatusLabel()
                + "  |  Saves: " + game.getSaveManager().getSavesDir().toAbsolutePath(), skin, "muted");
        dmStatusLabel.setAlignment(Align.center);
        dmStatusLabel.setWrap(true);
        card.add(dmStatusLabel).width(680);

        root.add(card).expand().center();
        stage.addActor(root);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(GrimoireSkin.BG_OBSIDIAN.r, GrimoireSkin.BG_OBSIDIAN.g, GrimoireSkin.BG_OBSIDIAN.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
    }
}
