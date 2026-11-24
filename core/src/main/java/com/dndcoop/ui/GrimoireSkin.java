package com.dndcoop.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.dndcoop.dm.DmSettings;

import java.util.ArrayList;
import java.util.List;

public class GrimoireSkin extends Skin {

    public static final Color BG_OBSIDIAN = Color.valueOf("0D1017");
    public static final Color BG_PANEL = Color.valueOf("151A24");
    public static final Color BG_CARD = Color.valueOf("1D2432");
    public static final Color BG_CARD_ACTIVE = Color.valueOf("263044");
    public static final Color BG_DM_CARD = Color.valueOf("201C16");
    public static final Color BG_PLAYER_CARD = Color.valueOf("162231");
    public static final Color BG_DICE_CARD = Color.valueOf("1E192B");
    public static final Color BG_COMBAT_CARD = Color.valueOf("2A161A");
    public static final Color BG_INPUT = Color.valueOf("10141D");

    public static final Color GOLD = Color.valueOf("D4AF37");
    public static final Color GOLD_DIM = Color.valueOf("8A7029");
    public static final Color PARCHMENT = Color.valueOf("F0E5CF");
    public static final Color MUTED = Color.valueOf("A89D88");
    public static final Color BORDER_SUBTLE = Color.valueOf("2E374B");
    public static final Color CRIMSON = Color.valueOf("C74343");
    public static final Color EMERALD = Color.valueOf("3B9A66");
    public static final Color ARCANE = Color.valueOf("5B95E5");
    public static final Color AMBER = Color.valueOf("DDA13B");

    private final List<Texture> ownedTextures = new ArrayList<>();
    private final List<BitmapFont> ownedFonts = new ArrayList<>();

    public GrimoireSkin(DmSettings.FontScale scale) {
        float m = scale != null ? scale.getMultiplier() : 1.0f;

        BitmapFont titleFont = loadFont("fonts/LiberationSerif-Bold.ttf", Math.round(28 * m));
        BitmapFont headingFont = loadFont("fonts/LiberationSerif-Bold.ttf", Math.round(18 * m));
        BitmapFont bodySerifFont = loadFont("fonts/LiberationSerif-Regular.ttf", Math.round(15 * m));
        BitmapFont uiFont = loadFont("fonts/LiberationSans-Regular.ttf", Math.round(14 * m));
        BitmapFont uiBoldFont = loadFont("fonts/LiberationSans-Bold.ttf", Math.round(14 * m));
        BitmapFont smallFont = loadFont("fonts/LiberationSans-Regular.ttf", Math.round(12 * m));
        BitmapFont monoFont = loadFont("fonts/LiberationMono-Regular.ttf", Math.round(13 * m));

        add("title-font", titleFont, BitmapFont.class);
        add("heading-font", headingFont, BitmapFont.class);
        add("serif-font", bodySerifFont, BitmapFont.class);
        add("default-font", uiFont, BitmapFont.class);
        add("bold-font", uiBoldFont, BitmapFont.class);
        add("small-font", smallFont, BitmapFont.class);
        add("mono-font", monoFont, BitmapFont.class);

        // Drawables
        add("bg-obsidian", solidDrawable(BG_OBSIDIAN), Drawable.class);
        add("panel", borderedDrawable(BG_PANEL, BORDER_SUBTLE, 2), Drawable.class);
        add("panel-gold", borderedDrawable(BG_PANEL, GOLD_DIM, 2), Drawable.class);
        add("card", borderedDrawable(BG_CARD, BORDER_SUBTLE, 1), Drawable.class);
        add("card-active", borderedDrawable(BG_CARD_ACTIVE, GOLD, 2), Drawable.class);
        add("card-dm", borderedDrawable(BG_DM_CARD, GOLD_DIM, 2), Drawable.class);
        add("card-player", borderedDrawable(BG_PLAYER_CARD, ARCANE, 1), Drawable.class);
        add("card-dice", borderedDrawable(BG_DICE_CARD, GOLD, 2), Drawable.class);
        add("card-combat", borderedDrawable(BG_COMBAT_CARD, CRIMSON, 2), Drawable.class);
        add("card-system", borderedDrawable(BG_INPUT, BORDER_SUBTLE, 1), Drawable.class);
        add("dice-banner", borderedDrawable(Color.valueOf("241E15"), GOLD, 2), Drawable.class);

        // Label styles
        add("default", new Label.LabelStyle(uiFont, PARCHMENT));
        add("title", new Label.LabelStyle(titleFont, GOLD));
        add("heading", new Label.LabelStyle(headingFont, GOLD));
        add("serif", new Label.LabelStyle(bodySerifFont, PARCHMENT));
        add("bold", new Label.LabelStyle(uiBoldFont, PARCHMENT));
        add("gold", new Label.LabelStyle(uiBoldFont, GOLD));
        add("muted", new Label.LabelStyle(smallFont, MUTED));
        add("small", new Label.LabelStyle(smallFont, PARCHMENT));
        add("mono", new Label.LabelStyle(monoFont, AMBER));
        add("crimson", new Label.LabelStyle(uiBoldFont, CRIMSON));
        add("emerald", new Label.LabelStyle(uiBoldFont, EMERALD));
        add("arcane", new Label.LabelStyle(uiBoldFont, ARCANE));

        // Buttons
        add("default", makeButtonStyle(uiBoldFont, BG_CARD, Color.valueOf("293246"), Color.valueOf("131822"), BORDER_SUBTLE, PARCHMENT));
        add("gold", makeButtonStyle(uiBoldFont, Color.valueOf("3A2F17"), Color.valueOf("50401E"), Color.valueOf("292110"), GOLD, GOLD));
        add("crimson", makeButtonStyle(uiBoldFont, Color.valueOf("441D22"), Color.valueOf("5E272E"), Color.valueOf("2E1216"), CRIMSON, PARCHMENT));
        add("emerald", makeButtonStyle(uiBoldFont, Color.valueOf("193A2A"), Color.valueOf("23523B"), Color.valueOf("12291E"), EMERALD, PARCHMENT));
        add("tab-active", makeButtonStyle(uiBoldFont, Color.valueOf("2B2518"), Color.valueOf("3A3220"), Color.valueOf("201C12"), GOLD, GOLD));

        // TextField
        TextField.TextFieldStyle tfStyle = new TextField.TextFieldStyle();
        tfStyle.font = uiFont;
        tfStyle.fontColor = PARCHMENT;
        tfStyle.messageFontColor = MUTED;
        tfStyle.background = borderedDrawable(BG_INPUT, BORDER_SUBTLE, 2);
        tfStyle.focusedBackground = borderedDrawable(BG_INPUT, GOLD, 2);
        tfStyle.cursor = solidDrawable(GOLD);
        tfStyle.selection = solidDrawable(Color.valueOf("3B4B6B"));
        add("default", tfStyle);

        // CheckBox
        CheckBox.CheckBoxStyle cbStyle = new CheckBox.CheckBoxStyle();
        cbStyle.font = uiFont;
        cbStyle.fontColor = PARCHMENT;
        cbStyle.checkboxOff = borderedDrawable(BG_INPUT, MUTED, 2);
        cbStyle.checkboxOn = borderedDrawable(GOLD, PARCHMENT, 2);
        add("default", cbStyle);

        // ScrollPane
        ScrollPane.ScrollPaneStyle spStyle = new ScrollPane.ScrollPaneStyle();
        spStyle.vScroll = solidDrawable(Color.valueOf("10141D"));
        spStyle.vScrollKnob = borderedDrawable(GOLD_DIM, GOLD, 1);
        add("default", spStyle);

        // ProgressBar (HP Bar)
        ProgressBar.ProgressBarStyle hpStyle = new ProgressBar.ProgressBarStyle();
        hpStyle.background = solidDrawable(Color.valueOf("25171A"));
        hpStyle.knobBefore = solidDrawable(EMERALD);
        add("hp-green", hpStyle);

        ProgressBar.ProgressBarStyle hpWarnStyle = new ProgressBar.ProgressBarStyle();
        hpWarnStyle.background = solidDrawable(Color.valueOf("25171A"));
        hpWarnStyle.knobBefore = solidDrawable(AMBER);
        add("hp-amber", hpWarnStyle);

        ProgressBar.ProgressBarStyle hpLowStyle = new ProgressBar.ProgressBarStyle();
        hpLowStyle.background = solidDrawable(Color.valueOf("25171A"));
        hpLowStyle.knobBefore = solidDrawable(CRIMSON);
        add("hp-red", hpLowStyle);

        // Window / Dialog
        Window.WindowStyle winStyle = new Window.WindowStyle();
        winStyle.titleFont = headingFont;
        winStyle.titleFontColor = GOLD;
        winStyle.background = borderedDrawable(BG_PANEL, GOLD, 2);
        winStyle.stageBackground = solidDrawable(new Color(0f, 0f, 0f, 0.72f));
        add("default", winStyle);
    }

    private BitmapFont loadFont(String assetPath, int pixelSize) {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(assetPath));
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
            param.size = Math.max(11, pixelSize);
            param.color = Color.WHITE;
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;
            BitmapFont font = gen.generateFont(param);
            font.getData().markupEnabled = false;
            ownedFonts.add(font);
            return font;
        } finally {
            gen.dispose();
        }
    }

    private TextButton.TextButtonStyle makeButtonStyle(BitmapFont font, Color up, Color over, Color down, Color border, Color text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = text;
        style.overFontColor = Color.WHITE;
        style.disabledFontColor = MUTED;
        style.up = borderedDrawable(up, border, 1);
        style.over = borderedDrawable(over, GOLD, 1);
        style.down = borderedDrawable(down, GOLD, 1);
        style.disabled = borderedDrawable(BG_INPUT, BORDER_SUBTLE, 1);
        return style;
    }

    private Drawable solidDrawable(Color color) {
        Pixmap pm = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        ownedTextures.add(tex);
        return new TextureRegionDrawable(new TextureRegion(tex));
    }

    private Drawable borderedDrawable(Color fill, Color border, int borderPx) {
        int size = 16;
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(border);
        pm.fill();
        pm.setColor(fill);
        pm.fillRectangle(borderPx, borderPx, size - borderPx * 2, size - borderPx * 2);
        Texture tex = new Texture(pm);
        pm.dispose();
        ownedTextures.add(tex);
        int split = Math.max(3, borderPx + 2);
        NinePatch patch = new NinePatch(tex, split, split, split, split);
        return new NinePatchDrawable(patch);
    }

    @Override
    public void dispose() {
        for (BitmapFont f : ownedFonts) {
            f.dispose();
        }
        ownedFonts.clear();
        for (Texture t : ownedTextures) {
            t.dispose();
        }
        ownedTextures.clear();
        super.dispose();
    }
}
