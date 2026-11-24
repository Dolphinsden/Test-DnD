package com.dndcoop.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.Skill;
import com.dndcoop.rules.CharacterFactory;
import com.dndcoop.rules.Spellbook;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PartySetupScreen extends ScreenAdapter {

    private final DndCoopGame game;
    private Stage stage;

    private final List<CharacterSheet> roster = CharacterFactory.createPreMadeRoster();
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private CharacterSheet focusedHero;

    private Table rosterListTable;
    private Table inspectorTable;
    private TextField campaignNameField;
    private Label partyCountLabel;

    public PartySetupScreen(DndCoopGame game) {
        this.game = game;
        // Pre-select a balanced 2-player couch starter party (Thorne + Lyra)
        if (roster.size() >= 2) {
            selectedIds.add(roster.get(0).getId());
            selectedIds.add(roster.get(1).getId());
        }
        focusedHero = roster.isEmpty() ? null : roster.get(0);
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        GrimoireSkin skin = game.getSkin();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(18).defaults().pad(8);

        // Header bar
        Table header = new Table();
        header.add(new Label("Assemble Your Couch Co-op Party (1–6 Quick-Start Heroes)", skin, "heading")).left().expandX();
        TextButton fontBtn = new TextButton("Font: " + game.getSettings().getFontScale().getLabel(), skin);
        fontBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.cycleCouchFontScale();
            }
        });
        TextButton backBtn = new TextButton("Back to Title", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTitleScreen();
            }
        });
        header.add(fontBtn).height(34).padRight(8);
        header.add(backBtn).height(34);
        root.add(header).colspan(2).fillX().row();

        // Left: Roster cards
        Table leftPane = new Table(skin);
        leftPane.setBackground(skin.getDrawable("panel"));
        leftPane.pad(12).top().left();
        leftPane.add(new Label("Curated 5E Hero Roster (Click to Inspect / Toggle)", skin, "gold")).left().padBottom(8).row();

        rosterListTable = new Table();
        rosterListTable.top().left().defaults().pad(5).expandX().fillX();
        ScrollPane rosterScroll = new ScrollPane(rosterListTable, skin);
        rosterScroll.setFadeScrollBars(false);
        leftPane.add(rosterScroll).expand().fill();

        // Right: Hero customization + full stat preview
        Table rightPane = new Table(skin);
        rightPane.setBackground(skin.getDrawable("panel-gold"));
        rightPane.pad(14).top().left();
        inspectorTable = new Table();
        inspectorTable.top().left();
        ScrollPane inspectorScroll = new ScrollPane(inspectorTable, skin);
        inspectorScroll.setFadeScrollBars(false);
        rightPane.add(inspectorScroll).expand().fill();

        root.add(leftPane).width(470).expandY().fill();
        root.add(rightPane).expand().fill().row();

        // Footer: Campaign Save Name + Start Button
        Table footer = new Table(skin);
        footer.setBackground(skin.getDrawable("panel"));
        footer.pad(12).defaults().pad(6);

        partyCountLabel = new Label("", skin, "gold");
        footer.add(partyCountLabel).left().padRight(16);

        footer.add(new Label("Session Name:", skin, "bold"));
        campaignNameField = new TextField("Hollowbrook Expedition", skin);
        footer.add(campaignNameField).width(300).height(36);

        footer.add().expandX();

        TextButton startBtn = new TextButton("Embark on Adventure ->", skin, "gold");
        startBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                List<CharacterSheet> chosen = new ArrayList<>();
                for (CharacterSheet hero : roster) {
                    if (selectedIds.contains(hero.getId())) {
                        chosen.add(hero);
                    }
                }
                if (chosen.isEmpty()) {
                    partyCountLabel.setText("Select at least 1 hero to begin!");
                    return;
                }
                CampaignState state = game.getController().startNewCampaign(chosen, campaignNameField.getText());
                game.showGameScreen(state);
            }
        });
        footer.add(startBtn).width(240).height(42);
        root.add(footer).colspan(2).fillX();

        stage.addActor(root);
        rebuildRoster();
        rebuildInspector();
    }

    private void rebuildRoster() {
        GrimoireSkin skin = game.getSkin();
        rosterListTable.clear();
        for (CharacterSheet hero : roster) {
            boolean inParty = selectedIds.contains(hero.getId());
            boolean isFocused = focusedHero != null && focusedHero.getId().equals(hero.getId());

            Table card = new Table(skin);
            card.setBackground(skin.getDrawable(isFocused ? "card-active" : "card"));
            card.pad(10);

            Table info = new Table();
            info.left();
            info.add(new Label(hero.getName(), skin, inParty ? "gold" : "bold")).left().row();
            info.add(new Label(hero.getRace().getDisplayName() + " " + hero.getCharacterClass().getDisplayName()
                    + "  •  " + hero.getArchetypeTitle(), skin, "small")).left().row();
            info.add(new Label("HP " + hero.getMaxHp() + "  |  AC " + hero.getArmorClass()
                    + "  |  " + hero.getPlayerName(), skin, "muted")).left();

            TextButton inspectBtn = new TextButton("Edit", skin, "default");
            inspectBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    focusedHero = hero;
                    rebuildRoster();
                    rebuildInspector();
                }
            });

            TextButton toggleBtn = new TextButton(inParty ? "In Party" : "+ Add", skin, inParty ? "emerald" : "gold");
            toggleBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (selectedIds.contains(hero.getId())) {
                        if (selectedIds.size() > 1) {
                            selectedIds.remove(hero.getId());
                        }
                    } else {
                        selectedIds.add(hero.getId());
                    }
                    focusedHero = hero;
                    rebuildRoster();
                    rebuildInspector();
                }
            });

            card.add(info).expandX().fillX().left();
            card.add(inspectBtn).width(66).height(32).padLeft(6);
            card.add(toggleBtn).width(92).height(32).padLeft(6);
            rosterListTable.add(card).expandX().fillX().row();
        }
        partyCountLabel.setText("Party Size: " + selectedIds.size() + " Hero" + (selectedIds.size() == 1 ? "" : "es"));
    }

    private void rebuildInspector() {
        GrimoireSkin skin = game.getSkin();
        inspectorTable.clear();
        if (focusedHero == null) return;

        CharacterSheet h = focusedHero;
        inspectorTable.defaults().pad(5).left();

        inspectorTable.add(new Label("Customize Hero: " + h.getArchetypeTitle(), skin, "heading")).colspan(2).row();

        inspectorTable.add(new Label("Character Name:", skin, "bold")).width(140);
        TextField nameField = new TextField(h.getName(), skin);
        nameField.setTextFieldListener((tf, c) -> {
            h.setName(tf.getText().isBlank() ? h.getArchetypeTitle() : tf.getText().trim());
            rebuildRoster();
        });
        inspectorTable.add(nameField).expandX().fillX().height(34).row();

        inspectorTable.add(new Label("Couch Player:", skin, "bold")).width(140);
        TextField playerField = new TextField(h.getPlayerName(), skin);
        playerField.setTextFieldListener((tf, c) -> {
            h.setPlayerName(tf.getText().trim());
            rebuildRoster();
        });
        inspectorTable.add(playerField).expandX().fillX().height(34).row();

        inspectorTable.add(new Label("Backstory:", skin, "bold")).top();
        TextArea backstoryArea = new TextArea(h.getBackstory(), skin);
        backstoryArea.setPrefRows(4);
        backstoryArea.setTextFieldListener((tf, c) -> h.setBackstory(tf.getText()));
        inspectorTable.add(backstoryArea).expandX().fillX().height(92).row();

        // 5E Statblock summary
        inspectorTable.add(new Label("D&D 5E Level 1 Statblock", skin, "gold")).colspan(2).padTop(10).row();
        Label vitals = new Label("Race: " + h.getRace().getDisplayName() + " (" + h.getRace().getTraitsSummary() + ")\n"
                + "Class: " + h.getCharacterClass().getDisplayName() + " — " + h.getCharacterClass().getFeaturesSummary() + "\n"
                + "Max HP: " + h.getMaxHp() + "  |  AC: " + h.getArmorClass() + "  |  Speed: " + h.getSpeed() + " ft  |  Proficiency: +" + h.getProficiencyBonus(),
                skin, "small");
        vitals.setWrap(true);
        inspectorTable.add(vitals).colspan(2).expandX().fillX().row();

        Table statRow = new Table();
        statRow.defaults().pad(4).expandX().fillX();
        for (Ability ab : Ability.values()) {
            Table box = new Table(skin);
            box.setBackground(skin.getDrawable("card"));
            box.pad(6);
            box.add(new Label(ab.getShortName(), skin, "gold")).row();
            box.add(new Label(String.valueOf(h.getAbilityScore(ab)), skin, "bold")).row();
            box.add(new Label(Ability.formatModifier(h.getAbilityModifier(ab)), skin, "mono"));
            statRow.add(box);
        }
        inspectorTable.add(statRow).colspan(2).expandX().fillX().padTop(6).row();

        // Skills & Gear
        StringBuilder skillLine = new StringBuilder("Proficient Skills: ");
        int idx = 0;
        for (Skill sk : h.getSkillProficiencies()) {
            if (idx++ > 0) skillLine.append(", ");
            skillLine.append(sk.getDisplayName()).append(" (").append(Ability.formatModifier(h.getSkillModifier(sk))).append(")");
        }
        Label skillsLbl = new Label(skillLine.toString(), skin, "small");
        skillsLbl.setWrap(true);
        inspectorTable.add(skillsLbl).colspan(2).expandX().fillX().padTop(6).row();

        StringBuilder gearLine = new StringBuilder("Starting Gear: ");
        for (int i = 0; i < h.getInventory().size(); i++) {
            if (i > 0) gearLine.append(", ");
            gearLine.append(h.getInventory().get(i).getName());
        }
        Label gearLbl = new Label(gearLine.toString(), skin, "muted");
        gearLbl.setWrap(true);
        inspectorTable.add(gearLbl).colspan(2).expandX().fillX().row();

        if (!h.getKnownSpellIds().isEmpty()) {
            StringBuilder spellLine = new StringBuilder("Prepared Spells (" + h.formatSpellSlotsSummary() + "): ");
            for (int i = 0; i < h.getKnownSpellIds().size(); i++) {
                if (i > 0) spellLine.append(", ");
                var sp = Spellbook.get(h.getKnownSpellIds().get(i));
                spellLine.append(sp != null ? sp.getName() : h.getKnownSpellIds().get(i));
            }
            Label spLbl = new Label(spellLine.toString(), skin, "arcane");
            spLbl.setWrap(true);
            inspectorTable.add(spLbl).colspan(2).expandX().fillX();
        }
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
