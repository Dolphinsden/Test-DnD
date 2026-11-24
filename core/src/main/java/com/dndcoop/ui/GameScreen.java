package com.dndcoop.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.dndcoop.campaign.AshenLanternCampaign;
import com.dndcoop.campaign.LocationNode;
import com.dndcoop.campaign.NpcProfile;
import com.dndcoop.controller.GameSessionController;
import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterClass;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.CombatState;
import com.dndcoop.model.Condition;
import com.dndcoop.model.InitiativeEntry;
import com.dndcoop.model.Item;
import com.dndcoop.model.LogEntry;
import com.dndcoop.model.Monster;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.model.Skill;
import com.dndcoop.model.Spell;
import com.dndcoop.persistence.ChronicleExporter;
import com.dndcoop.rules.DiceRoller;
import com.dndcoop.rules.LevelingSystem;
import com.dndcoop.rules.Spellbook;

import java.nio.file.Path;
import java.util.Map;

public class GameScreen extends ScreenAdapter {

    private final DndCoopGame game;
    private Stage stage;

    private Label topTitleLabel;
    private TextButton dmStatusButton;

    private Table leftPaneContent;
    private Table logTable;
    private ScrollPane logScroll;
    private Table diceBannerContainer;
    private Table combatQuickBar;
    private Label speakerPromptLabel;
    private TextField actionInputField;
    private TextButton sendButton;

    private Table rightInspectorContent;
    private TextButton worldTabBtn;
    private TextButton sheetTabBtn;
    private boolean showWorldTab = true;
    private String inspectedHeroId;

    private int renderedLogCount = -1;

    public GameScreen(DndCoopGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        GrimoireSkin skin = game.getSkin();

        CampaignState state = game.getController().getState();
        if (state != null && state.getActiveCharacter() != null) {
            inspectedHeroId = state.getActiveCharacter().getId();
        }

        Table root = new Table();
        root.setFillParent(true);
        root.pad(10).defaults().pad(5);

        // ===== TOP BAR =====
        Table topBar = new Table(skin);
        topBar.setBackground(skin.getDrawable("panel-gold"));
        topBar.pad(8, 12, 8, 12).defaults().pad(4);

        topTitleLabel = new Label("The Ashen Lantern of Hollowbrook", skin, "heading");
        topBar.add(topTitleLabel).left().expandX();

        dmStatusButton = new TextButton("DM: " + game.getController().getHybridDm().getLastStatusLabel(), skin);
        dmStatusButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                DmSettingsDialog dlg = new DmSettingsDialog(game, GameScreen.this::refreshAll);
                dlg.showCentered(stage);
            }
        });

        TextButton fontBtn = new TextButton("Font: " + game.getSettings().getFontScale().getLabel(), skin);
        fontBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.cycleCouchFontScale();
            }
        });

        TextButton shortRestBtn = new TextButton("Short Rest", skin);
        shortRestBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getController().performShortRest(GameScreen.this::refreshAll);
            }
        });

        TextButton longRestBtn = new TextButton("Long Rest", skin);
        longRestBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getController().performLongRest(GameScreen.this::refreshAll);
            }
        });

        TextButton saveLoadBtn = new TextButton("Save / Load", skin, "gold");
        saveLoadBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                SaveLoadDialog dlg = new SaveLoadDialog(game, true, loaded -> {
                    game.getController().loadCampaignState(loaded);
                    renderedLogCount = -1;
                    if (loaded.getActiveCharacter() != null) {
                        inspectedHeroId = loaded.getActiveCharacter().getId();
                    }
                    refreshAll();
                });
                dlg.showCentered(stage);
            }
        });

        TextButton exportBtn = new TextButton("Export Story", skin, "emerald");
        exportBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CampaignState s = game.getController().getState();
                if (s != null) {
                    try {
                        Path out = ChronicleExporter.exportToFile(s, game.getSaveManager().getChroniclesDir());
                        s.addLog(LogEntry.system("Exported Markdown Adventure Chronicle to: " + out.toAbsolutePath()));
                        refreshAll();
                    } catch (Exception e) {
                        s.addLog(LogEntry.system("Failed to export chronicle: " + e.getMessage()));
                        refreshAll();
                    }
                }
            }
        });

        TextButton menuBtn = new TextButton("Title", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getController().autoSaveQuietly();
                game.showTitleScreen();
            }
        });

        topBar.add(dmStatusButton).height(32);
        topBar.add(fontBtn).height(32);
        topBar.add(shortRestBtn).height(32);
        topBar.add(longRestBtn).height(32);
        topBar.add(saveLoadBtn).height(32);
        topBar.add(exportBtn).height(32);
        topBar.add(menuBtn).height(32);
        root.add(topBar).colspan(3).fillX().row();

        // ===== LEFT PANE: PARTY HUD & 5E INITIATIVE TRACKER =====
        Table leftPane = new Table(skin);
        leftPane.setBackground(skin.getDrawable("panel"));
        leftPane.pad(10).top().left();
        leftPaneContent = new Table();
        leftPaneContent.top().left().defaults().pad(4).expandX().fillX();
        ScrollPane leftScroll = new ScrollPane(leftPaneContent, skin);
        leftScroll.setFadeScrollBars(false);
        leftPane.add(leftScroll).expand().fill();

        // ===== CENTER PANE: STORYBOOK LOG + DICE BANNER + ACTION BAR =====
        Table centerPane = new Table(skin);
        centerPane.setBackground(skin.getDrawable("panel"));
        centerPane.pad(10);

        logTable = new Table();
        logTable.top().left().defaults().pad(5).expandX().fillX();
        logScroll = new ScrollPane(logTable, skin);
        logScroll.setFadeScrollBars(false);
        logScroll.setScrollingDisabled(true, false);
        centerPane.add(logScroll).expand().fill().row();

        diceBannerContainer = new Table();
        centerPane.add(diceBannerContainer).expandX().fillX().padTop(6).row();

        combatQuickBar = new Table();
        centerPane.add(combatQuickBar).expandX().fillX().padTop(4).row();

        // Input area
        Table inputBox = new Table(skin);
        inputBox.setBackground(skin.getDrawable("card"));
        inputBox.pad(8);

        speakerPromptLabel = new Label("Acting Hero:", skin, "gold");
        inputBox.add(speakerPromptLabel).left().colspan(2).padBottom(4).row();

        actionInputField = new TextField("", skin);
        actionInputField.setMessageText("Type any free-form action (e.g., 'I inspect the pedestal', 'I ask Martha about the lantern', 'I attack the goblin')...");
        actionInputField.setTextFieldListener((tf, c) -> {
            if (c == '\r' || c == '\n') {
                submitCurrentInput();
            }
        });

        sendButton = new TextButton("Send Action", skin, "gold");
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitCurrentInput();
            }
        });

        inputBox.add(actionInputField).expandX().fillX().height(38).padRight(8);
        inputBox.add(sendButton).width(130).height(38);
        centerPane.add(inputBox).expandX().fillX().padTop(6);

        // ===== RIGHT PANE: TABBED WORLD / QUESTS / CHARACTER SHEET INSPECTOR =====
        Table rightPane = new Table(skin);
        rightPane.setBackground(skin.getDrawable("panel"));
        rightPane.pad(10).top().left();

        Table tabRow = new Table();
        tabRow.defaults().height(32).expandX().fillX().pad(2);
        worldTabBtn = new TextButton("World & Quests", skin, "tab-active");
        sheetTabBtn = new TextButton("Character Sheet", skin, "default");
        worldTabBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showWorldTab = true;
                rebuildRightPane();
            }
        });
        sheetTabBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showWorldTab = false;
                rebuildRightPane();
            }
        });
        tabRow.add(worldTabBtn);
        tabRow.add(sheetTabBtn);
        rightPane.add(tabRow).expandX().fillX().padBottom(8).row();

        rightInspectorContent = new Table();
        rightInspectorContent.top().left().defaults().pad(4).expandX().fillX();
        ScrollPane rightScroll = new ScrollPane(rightInspectorContent, skin);
        rightScroll.setFadeScrollBars(false);
        rightScroll.setScrollingDisabled(true, false);
        rightPane.add(rightScroll).expand().fill();

        root.add(leftPane).width(305).expandY().fill();
        root.add(centerPane).expand().fill();
        root.add(rightPane).width(350).expandY().fill();

        stage.addActor(root);

        // Spacebar shortcut to roll digital dice when a dice prompt is active and user isn't typing in a TextField
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.SPACE && !(stage.getKeyboardFocus() instanceof TextField)) {
                    CampaignState st = game.getController().getState();
                    if (st != null && st.getPendingRoll() != null) {
                        game.getController().resolvePendingRoll(null, GameScreen.this::refreshAll);
                        return true;
                    }
                }
                return false;
            }
        });

        renderedLogCount = -1;
        refreshAll();
    }

    private void submitCurrentInput() {
        String text = actionInputField.getText();
        if (text == null || text.isBlank()) return;
        actionInputField.setText("");
        game.getController().submitFreeText(text, this::refreshAll);
    }

    public void refreshAll() {
        GameSessionController ctrl = game.getController();
        CampaignState state = ctrl.getState();
        if (state == null) return;

        LocationNode loc = AshenLanternCampaign.getLocation(state.getCurrentLocationId());
        String combatBadge = state.getCombat().isActive() ? "  [COMBAT — ROUND " + state.getCombat().getRoundNumber() + "]" : "";
        topTitleLabel.setText("Act " + state.getCurrentAct() + " — " + loc.getName() + combatBadge);
        dmStatusButton.setText("DM: " + (ctrl.isDmThinking() ? "Thinking..." : ctrl.getHybridDm().getLastStatusLabel()));

        CharacterSheet active = state.getActiveCharacter();
        if (state.getCombat().isActive()) {
            InitiativeEntry turn = state.getCombat().getCurrentTurnEntry();
            if (turn != null && turn.isPlayer()) {
                speakerPromptLabel.setText("STRICT 5E INITIATIVE TURN -> " + turn.getName()
                        + " (Free-form combat action or quick button)");
                inspectedHeroId = turn.getParticipantId();
            } else {
                speakerPromptLabel.setText("Combat Initiative in progress...");
            }
        } else if (active != null) {
            speakerPromptLabel.setText("Active Speaker: " + active.getName() + " (" + active.getPlayerName()
                    + ") — Click any hero on the left to switch couch speaker");
        }

        boolean inputLocked = ctrl.isDmThinking() || state.getPendingRoll() != null;
        actionInputField.setDisabled(inputLocked);
        sendButton.setDisabled(inputLocked);
        sendButton.setText(ctrl.isDmThinking() ? "DM Thinking..." : "Send Action");

        rebuildLeftPane();
        rebuildNarrativeLog();
        rebuildDiceBanner();
        rebuildCombatQuickBar();
        rebuildRightPane();
    }

    private void rebuildLeftPane() {
        GrimoireSkin skin = game.getSkin();
        CampaignState state = game.getController().getState();
        leftPaneContent.clear();

        // Strict 5E Initiative Tracker when combat is active
        CombatState combat = state.getCombat();
        if (combat.isActive()) {
            Table initBox = new Table(skin);
            initBox.setBackground(skin.getDrawable("card-combat"));
            initBox.pad(8).left();
            initBox.add(new Label("5E INITIATIVE — ROUND " + combat.getRoundNumber(), skin, "crimson")).left().padBottom(4).row();
            if (!combat.getPendingInitiativeHeroIds().isEmpty()) {
                initBox.add(new Label("Rolling Initiative...", skin, "gold")).left().row();
            } else {
                for (int i = 0; i < combat.getTurnOrder().size(); i++) {
                    InitiativeEntry e = combat.getTurnOrder().get(i);
                    boolean isCurrent = i == combat.getCurrentTurnIndex();
                    String suffix = "";
                    if (!e.isPlayer()) {
                        Monster m = combat.findEnemyById(e.getParticipantId());
                        if (m != null) {
                            suffix = " [" + m.getHealthStatusLabel() + ", AC " + m.getArmorClass() + "]";
                        }
                    }
                    String prefix = isCurrent ? "▶ " : "   ";
                    Label rowLbl = new Label(prefix + e.getInitiativeTotal() + "  " + e.getName() + suffix,
                            skin, isCurrent ? "gold" : (e.isPlayer() ? "small" : "muted"));
                    rowLbl.setWrap(true);
                    initBox.add(rowLbl).expandX().fillX().left().padTop(2).row();
                }
            }
            leftPaneContent.add(initBox).expandX().fillX().padBottom(8).row();
        }

        leftPaneContent.add(new Label("PARTY HUD", skin, "gold")).left().padBottom(4).row();

        for (CharacterSheet hero : state.getParty()) {
            boolean isActiveSpeaker = hero.getId().equals(state.getActiveCharacterId());
            Table card = new Table(skin);
            card.setBackground(skin.getDrawable(isActiveSpeaker ? "card-active" : "card"));
            card.pad(8);

            Table top = new Table();
            top.add(new Label(hero.getName(), skin, isActiveSpeaker ? "gold" : "bold")).left().expandX();
            top.add(new Label("AC " + hero.getArmorClass(), skin, "mono")).right();
            card.add(top).expandX().fillX().row();

            Label sub = new Label(hero.getPlayerName() + " • Lvl " + hero.getLevel() + " "
                    + hero.getRace().getDisplayName() + " " + hero.getCharacterClass().getDisplayName(), skin, "muted");
            sub.setWrap(true);
            card.add(sub).expandX().fillX().left().padBottom(4).row();

            // HP Bar
            float ratio = (float) hero.getCurrentHp() / Math.max(1, hero.getMaxHp());
            String hpStyle = ratio > 0.55f ? "hp-green" : (ratio > 0.25f ? "hp-amber" : "hp-red");
            ProgressBar hpBar = new ProgressBar(0, Math.max(1, hero.getMaxHp()), 1, false, skin, hpStyle);
            hpBar.setValue(hero.getCurrentHp());
            card.add(hpBar).expandX().fillX().height(10).padBottom(3).row();

            Label hpText = new Label("HP: " + hero.getCurrentHp() + "/" + hero.getMaxHp()
                    + "  |  XP: " + hero.getXp() + "/" + LevelingSystem.xpForNextLevel(hero.getLevel())
                    + "  |  " + hero.getGold() + " gp", skin, "small");
            card.add(hpText).left().row();

            if (hero.getCharacterClass().isSpellcaster()) {
                card.add(new Label("Slots: " + hero.formatSpellSlotsSummary(), skin, "arcane")).left().row();
            }
            if (!hero.getConditions().isEmpty()) {
                StringBuilder conds = new StringBuilder("Status: ");
                int i = 0;
                for (Condition c : hero.getConditions()) {
                    if (i++ > 0) conds.append(", ");
                    conds.append(c.getDisplayName());
                }
                Label condLbl = new Label(conds.toString(), skin, "crimson");
                condLbl.setWrap(true);
                card.add(condLbl).expandX().fillX().left().row();
            }

            Table btnRow = new Table();
            btnRow.defaults().height(26).padTop(4);
            if (!combat.isActive()) {
                TextButton speakBtn = new TextButton(isActiveSpeaker ? "Speaking" : "Speak", skin,
                        isActiveSpeaker ? "gold" : "default");
                speakBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        inspectedHeroId = hero.getId();
                        game.getController().selectActiveHero(hero.getId(), GameScreen.this::refreshAll);
                    }
                });
                btnRow.add(speakBtn).expandX().fillX().padRight(4);
            }
            TextButton sheetBtn = new TextButton("Inspect Sheet", skin, "default");
            sheetBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    inspectedHeroId = hero.getId();
                    showWorldTab = false;
                    rebuildRightPane();
                }
            });
            btnRow.add(sheetBtn).expandX().fillX();
            card.add(btnRow).expandX().fillX();

            leftPaneContent.add(card).expandX().fillX().row();
        }
    }

    private void rebuildNarrativeLog() {
        CampaignState state = game.getController().getState();
        if (state.getLog().size() == renderedLogCount) {
            return;
        }
        GrimoireSkin skin = game.getSkin();
        logTable.clear();
        for (LogEntry entry : state.getLog()) {
            Table card = new Table(skin);
            String bg = "card-system";
            String headerStyle = "muted";
            String bodyStyle = "small";
            switch (entry.getType()) {
                case DM:
                    bg = "card-dm";
                    headerStyle = "gold";
                    bodyStyle = "serif";
                    break;
                case PLAYER:
                    bg = "card-player";
                    headerStyle = "arcane";
                    bodyStyle = "default";
                    break;
                case DICE:
                    bg = "card-dice";
                    headerStyle = "gold";
                    bodyStyle = "bold";
                    break;
                case COMBAT:
                    bg = "card-combat";
                    headerStyle = "crimson";
                    bodyStyle = "default";
                    break;
                case SYSTEM:
                    bg = "card-system";
                    headerStyle = "muted";
                    bodyStyle = "small";
                    break;
            }
            card.setBackground(skin.getDrawable(bg));
            card.pad(10, 12, 10, 12);

            Label speakerLbl = new Label(entry.getSpeaker(), skin, headerStyle);
            card.add(speakerLbl).left().padBottom(3).row();

            Label textLbl = new Label(entry.getText(), skin, bodyStyle);
            textLbl.setWrap(true);
            card.add(textLbl).expandX().fillX().left().row();

            if (entry.getDetail() != null && !entry.getDetail().isBlank()) {
                Label detailLbl = new Label("5E Roll: " + entry.getDetail(), skin, "mono");
                detailLbl.setWrap(true);
                card.add(detailLbl).expandX().fillX().left().padTop(4);
            }
            logTable.add(card).expandX().fillX().row();
        }
        renderedLogCount = state.getLog().size();
        Gdx.app.postRunnable(() -> {
            logScroll.layout();
            logScroll.setScrollPercentY(1f);
        });
    }

    private void rebuildDiceBanner() {
        GrimoireSkin skin = game.getSkin();
        CampaignState state = game.getController().getState();
        diceBannerContainer.clear();

        PendingRoll pr = state.getPendingRoll();
        if (pr == null) {
            return;
        }

        Table banner = new Table(skin);
        banner.setBackground(skin.getDrawable("dice-banner"));
        banner.pad(10, 14, 10, 14);

        boolean isDamage = pr.getRollType() == PendingRoll.RollType.DAMAGE_ROLL;
        String diceSpec = pr.isCriticalHit() && isDamage
                ? DiceRoller.doubleDiceNotation(pr.getDiceNotation())
                : pr.getDiceNotation();
        StringBuilder breakdown = new StringBuilder();
        breakdown.append(diceSpec);
        if (pr.getModifier() != 0) {
            breakdown.append(" ").append(Ability.formatModifier(pr.getModifier()));
        }
        if (pr.getTargetDcOrAc() != null) {
            boolean isAtk = pr.getRollType() == PendingRoll.RollType.ATTACK_ROLL
                    || pr.getRollType() == PendingRoll.RollType.SPELL_ATTACK;
            breakdown.append(isAtk ? "  vs  AC " : "  vs  DC ").append(pr.getTargetDcOrAc());
        }
        if (pr.isAdvantage() && !pr.isDisadvantage()) breakdown.append("  [ADVANTAGE]");
        if (pr.isDisadvantage() && !pr.isAdvantage()) breakdown.append("  [DISADVANTAGE]");
        if (pr.isCriticalHit()) breakdown.append("  [CRITICAL 2x DICE!]");

        Label titleLbl = new Label("DICE ROLL REQUIRED: " + pr.getLabel(), skin, "gold");
        titleLbl.setWrap(true);
        banner.add(titleLbl).expandX().fillX().left().colspan(4).row();

        Label mathLbl = new Label("Formula: " + breakdown, skin, "mono");
        mathLbl.setWrap(true);
        banner.add(mathLbl).expandX().fillX().left().colspan(4).padBottom(8).row();

        TextButton rollDigitalBtn = new TextButton("Roll Digital Dice [Space]", skin, "gold");
        rollDigitalBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getController().resolvePendingRoll(null, GameScreen.this::refreshAll);
            }
        });

        Label physLbl = new Label("Or enter physical die result:", skin, "small");
        TextField physField = new TextField("", skin);
        physField.setMessageText(isDamage ? "raw dice sum" : "1-20");
        physField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());

        TextButton submitPhysBtn = new TextButton("Use Physical Roll", skin, "emerald");
        Runnable submitPhysAction = () -> {
            String raw = physField.getText().trim();
            if (raw.isEmpty()) return;
            try {
                int val = Integer.parseInt(raw);
                game.getController().resolvePendingRoll(val, GameScreen.this::refreshAll);
            } catch (NumberFormatException ignored) {
            }
        };
        submitPhysBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitPhysAction.run();
            }
        });
        physField.setTextFieldListener((tf, c) -> {
            if (c == '\r' || c == '\n') submitPhysAction.run();
        });

        banner.add(rollDigitalBtn).height(36).padRight(12);
        banner.add(physLbl).padRight(6);
        banner.add(physField).width(105).height(36).padRight(6);
        banner.add(submitPhysBtn).height(36);

        diceBannerContainer.add(banner).expandX().fillX();
    }

    private void rebuildCombatQuickBar() {
        GrimoireSkin skin = game.getSkin();
        CampaignState state = game.getController().getState();
        combatQuickBar.clear();

        if (!state.getCombat().isActive() || state.getPendingRoll() != null) {
            return;
        }
        InitiativeEntry turn = state.getCombat().getCurrentTurnEntry();
        if (turn == null || !turn.isPlayer()) {
            return;
        }
        CharacterSheet hero = state.findHeroById(turn.getParticipantId());
        if (hero == null) return;

        Table bar = new Table();
        bar.left().defaults().height(30).pad(2);

        addCombatBtn(bar, "Attack (" + hero.getEquippedWeapon().getName() + ")", "crimson", "attack");
        addCombatBtn(bar, "Dodge", "default", "dodge");
        addCombatBtn(bar, "Dash", "default", "dash");
        addCombatBtn(bar, "Disengage", "default", "disengage");
        addCombatBtn(bar, "Help Ally", "default", "help");
        addCombatBtn(bar, "Hide", "default", "hide");
        if (hero.getCharacterClass() == CharacterClass.FIGHTER && hero.getClassResourceCurrent() > 0) {
            addCombatBtn(bar, "Second Wind", "emerald", "second_wind");
        }
        if (hero.getCharacterClass() == CharacterClass.PALADIN && hero.getClassResourceCurrent() > 0) {
            addCombatBtn(bar, "Lay on Hands", "emerald", "lay_on_hands");
        }
        addCombatBtn(bar, "End Turn", "gold", "end_turn");

        combatQuickBar.add(bar).expandX().fillX();
    }

    private void addCombatBtn(Table bar, String label, String style, String actionKey) {
        TextButton btn = new TextButton(label, game.getSkin(), style);
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getController().performQuickCombatAction(actionKey, GameScreen.this::refreshAll);
            }
        });
        bar.add(btn);
    }

    private void rebuildRightPane() {
        GrimoireSkin skin = game.getSkin();
        CampaignState state = game.getController().getState();
        worldTabBtn.setStyle(skin.get(showWorldTab ? "tab-active" : "default", TextButton.TextButtonStyle.class));
        sheetTabBtn.setStyle(skin.get(showWorldTab ? "default" : "tab-active", TextButton.TextButtonStyle.class));

        rightInspectorContent.clear();

        if (showWorldTab) {
            LocationNode loc = AshenLanternCampaign.getLocation(state.getCurrentLocationId());
            rightInspectorContent.add(new Label("ACT " + loc.getAct() + " — " + loc.getName(), skin, "gold")).left().row();
            Label desc = new Label(loc.getDescription(), skin, "small");
            desc.setWrap(true);
            rightInspectorContent.add(desc).expandX().fillX().left().padBottom(6).row();

            Label hint = new Label("Tip: " + loc.getAmbientHint(), skin, "muted");
            hint.setWrap(true);
            rightInspectorContent.add(hint).expandX().fillX().left().padBottom(10).row();

            // Exits
            rightInspectorContent.add(new Label("EXITS & PATHS", skin, "gold")).left().row();
            for (Map.Entry<String, String> ex : loc.getExits().entrySet()) {
                String reqFlag = loc.getLockedExitRequiredFlags().get(ex.getKey());
                boolean locked = reqFlag != null && !state.hasFlag(reqFlag);
                String btnText = (locked ? "[LOCKED] " : "-> ") + ex.getValue();
                TextButton exitBtn = new TextButton(btnText, skin, locked ? "crimson" : "default");
                exitBtn.getLabel().setWrap(true);
                exitBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        game.getController().travelToLocation(ex.getKey(), GameScreen.this::refreshAll);
                    }
                });
                rightInspectorContent.add(exitBtn).expandX().fillX().minHeight(34).padBottom(4).row();
            }

            // NPCs
            if (!loc.getNpcs().isEmpty()) {
                rightInspectorContent.add(new Label("PEOPLE HERE", skin, "gold")).left().padTop(8).row();
                for (NpcProfile npc : loc.getNpcs()) {
                    TextButton npcBtn = new TextButton("Speak with " + npc.getName() + " (" + npc.getTitle() + ")", skin, "default");
                    npcBtn.getLabel().setWrap(true);
                    npcBtn.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            game.getController().submitFreeText("I ask " + npc.getName() + " about the Ashen Lantern.", GameScreen.this::refreshAll);
                        }
                    });
                    rightInspectorContent.add(npcBtn).expandX().fillX().minHeight(32).padBottom(4).row();
                }
            }

            // Quest Journal
            rightInspectorContent.add(new Label("QUEST JOURNAL", skin, "gold")).left().padTop(10).row();
            for (String line : AshenLanternCampaign.getQuestJournalLines(state)) {
                String style = line.startsWith("[DONE]") ? "emerald" : (line.startsWith("[ACTIVE]") ? "small" : "muted");
                Label q = new Label(line, skin, style);
                q.setWrap(true);
                rightInspectorContent.add(q).expandX().fillX().left().padBottom(3).row();
            }
        } else {
            // Full Character Sheet Inspector
            CharacterSheet hero = state.findHeroById(inspectedHeroId);
            if (hero == null) hero = state.getActiveCharacter();
            if (hero == null) return;
            final CharacterSheet h = hero;

            rightInspectorContent.add(new Label(h.getName(), skin, "heading")).left().row();
            rightInspectorContent.add(new Label("Lvl " + h.getLevel() + " " + h.getRace().getDisplayName() + " "
                    + h.getCharacterClass().getDisplayName() + "  |  Prof +" + h.getProficiencyBonus()
                    + "  |  Speed " + h.getSpeed() + "ft", skin, "muted")).left().row();
            rightInspectorContent.add(new Label("HP: " + h.getCurrentHp() + "/" + h.getMaxHp()
                    + "  |  AC: " + h.getArmorClass()
                    + "  |  Hit Dice: " + h.getHitDiceRemaining() + "d" + h.getCharacterClass().getHitDie()
                    + "  |  Gold: " + h.getGold() + " gp", skin, "bold")).left().padBottom(6).row();

            if (h.getClassResourceName() != null) {
                rightInspectorContent.add(new Label(h.getClassResourceName() + ": "
                        + h.getClassResourceCurrent() + "/" + h.getClassResourceMax(), skin, "emerald")).left().row();
            }

            // 6 Ability Score Boxes (2 rows of 3)
            Table abGrid = new Table();
            abGrid.defaults().pad(3).expandX().fillX();
            Ability[] abs = Ability.values();
            for (int i = 0; i < abs.length; i++) {
                Ability ab = abs[i];
                Table cell = new Table(skin);
                cell.setBackground(skin.getDrawable("card"));
                cell.pad(4);
                cell.add(new Label(ab.getShortName(), skin, "gold")).row();
                cell.add(new Label(h.getAbilityScore(ab) + " (" + Ability.formatModifier(h.getAbilityModifier(ab)) + ")", skin, "mono"));
                abGrid.add(cell);
                if (i == 2) abGrid.row();
            }
            rightInspectorContent.add(abGrid).expandX().fillX().padTop(4).padBottom(6).row();

            // Proficient Skills
            StringBuilder skText = new StringBuilder();
            for (Skill sk : h.getSkillProficiencies()) {
                if (skText.length() > 0) skText.append("  •  ");
                skText.append(sk.getDisplayName()).append(" ").append(Ability.formatModifier(h.getSkillModifier(sk)));
            }
            rightInspectorContent.add(new Label("PROFICIENT SKILLS", skin, "gold")).left().row();
            Label skLbl = new Label(skText.toString(), skin, "small");
            skLbl.setWrap(true);
            rightInspectorContent.add(skLbl).expandX().fillX().left().padBottom(8).row();

            // Spells
            if (!h.getKnownSpellIds().isEmpty()) {
                rightInspectorContent.add(new Label("SPELLBOOK (" + h.formatSpellSlotsSummary()
                        + " | DC " + h.getSpellSaveDc() + ")", skin, "arcane")).left().row();
                for (String spellId : h.getKnownSpellIds()) {
                    Spell sp = Spellbook.get(spellId);
                    if (sp == null) continue;
                    Table spRow = new Table(skin);
                    spRow.setBackground(skin.getDrawable("card"));
                    spRow.pad(6);
                    String lvlTag = sp.getLevel() == 0 ? "Cantrip" : "Lvl " + sp.getLevel();
                    Label spName = new Label(sp.getName() + " (" + lvlTag + ")", skin, "small");
                    spName.setWrap(true);
                    TextButton castBtn = new TextButton("Cast", skin, "gold");
                    castBtn.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            game.getController().castSpellDirectly(h, sp.getId(), GameScreen.this::refreshAll);
                        }
                    });
                    spRow.add(spName).expandX().fillX().left();
                    spRow.add(castBtn).width(58).height(26).padLeft(4);
                    rightInspectorContent.add(spRow).expandX().fillX().padBottom(3).row();
                }
            }

            // Equipment & Inventory
            rightInspectorContent.add(new Label("INVENTORY & EQUIPMENT", skin, "gold")).left().padTop(6).row();
            for (Item item : h.getInventory()) {
                Table iRow = new Table(skin);
                iRow.setBackground(skin.getDrawable("card"));
                iRow.pad(6);
                boolean equipped = item.getId().equals(h.getEquippedWeaponId())
                        || item.getId().equals(h.getEquippedArmorId())
                        || item.getId().equals(h.getEquippedShieldId());
                String qty = item.getQuantity() > 1 ? " x" + item.getQuantity() : "";
                String tag = equipped ? " [EQ]" : "";
                Label iLbl = new Label(item.getName() + qty + tag, skin, equipped ? "gold" : "small");
                iLbl.setWrap(true);
                iRow.add(iLbl).expandX().fillX().left();

                if (item.getType() == Item.ItemType.CONSUMABLE || item.getType() == Item.ItemType.WEAPON
                        || item.getType() == Item.ItemType.ARMOR || item.getType() == Item.ItemType.SHIELD) {
                    String actLabel = item.getType() == Item.ItemType.CONSUMABLE ? "Use" : (equipped ? "Equipped" : "Equip");
                    TextButton actBtn = new TextButton(actLabel, skin, item.getType() == Item.ItemType.CONSUMABLE ? "emerald" : "default");
                    actBtn.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            game.getController().useOrEquipItem(h, item.getId(), GameScreen.this::refreshAll);
                        }
                    });
                    iRow.add(actBtn).width(70).height(26).padLeft(4);
                }
                rightInspectorContent.add(iRow).expandX().fillX().padBottom(3).row();
            }

            // Backstory
            if (h.getBackstory() != null && !h.getBackstory().isBlank()) {
                rightInspectorContent.add(new Label("BACKSTORY", skin, "gold")).left().padTop(8).row();
                Label bs = new Label(h.getBackstory(), skin, "muted");
                bs.setWrap(true);
                rightInspectorContent.add(bs).expandX().fillX().left();
            }
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
