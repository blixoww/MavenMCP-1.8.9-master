package net.minecraft.client.gui;

import java.awt.Color;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.src.Config;
import net.minecraft.util.MathHelper;
import net.optifine.Lang;
import net.optifine.gui.GuiAnimationSettingsOF;
import net.optifine.gui.GuiDetailSettingsOF;
import net.optifine.gui.GuiOptionButtonOF;
import net.optifine.gui.GuiOptionSliderOF;
import net.optifine.gui.GuiOtherSettingsOF;
import net.optifine.gui.GuiPerformanceSettingsOF;
import net.optifine.gui.GuiQualitySettingsOF;
import net.optifine.gui.GuiScreenOF;
import net.optifine.gui.TooltipManager;
import net.optifine.gui.TooltipProviderOptions;
import net.optifine.shaders.gui.GuiShaders;

public class GuiVideoSettings extends GuiScreenOF
{
    private final GuiScreen parentGuiScreen;
    protected String screenTitle = "Video Settings";
    private final GameSettings guiGameSettings;

    // ── Options grille principale (2 col) ────────────────────────────────────
    private static final GameSettings.Options[] videoOptions = {
        GameSettings.Options.GRAPHICS,          GameSettings.Options.RENDER_DISTANCE,
        GameSettings.Options.AMBIENT_OCCLUSION, GameSettings.Options.FRAMERATE_LIMIT,
        GameSettings.Options.AO_LEVEL,          GameSettings.Options.VIEW_BOBBING,
        GameSettings.Options.GUI_SCALE,         GameSettings.Options.USE_VBO,
        GameSettings.Options.GAMMA,             GameSettings.Options.BLOCK_ALTERNATIVES,
        GameSettings.Options.DYNAMIC_LIGHTS,    GameSettings.Options.DYNAMIC_FOV,
    };

    // ── Catégories (6 boutons en 3 lignes × 2 col) ──────────────────────────
    private static final int BTN_DONE        = 200;
    private static final int BTN_DETAILS     = 201;
    private static final int BTN_QUALITY     = 202;
    private static final int BTN_ANIMATIONS  = 211;
    private static final int BTN_PERFORMANCE = 212;
    private static final int BTN_OTHER       = 222;
    private static final int BTN_SHADERS     = 231;

    // Couleurs et animation
    private float animation = 0.0f;
    private long  lastTime  = -1L;
    private static final int ACCENT = new Color(220, 30, 30).getRGB();

    // Layout (calculé dans initGui)
    private int panelX, panelY, panelW, panelH;
    private int catStartY;

    // Constantes layout
    private static final int HEADER_H = 36;
    private static final int BTN_H    = 20;
    private static final int BTN_GAP  = 3;
    private static final int COL_W    = 150;
    private static final int COL_GAP  = 10;
    private static final int SECTION_H = 14;
    private static final int PAD       = 8;  // padding interne panel

    private final TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiVideoSettings(GuiScreen parentScreenIn, GameSettings gameSettingsIn) {
        this.parentGuiScreen  = parentScreenIn;
        this.guiGameSettings  = gameSettingsIn;
    }

    // ── initGui ──────────────────────────────────────────────────────────────
    @Override
    public void initGui() {
        this.screenTitle = I18n.format("options.videoTitle");
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();

        int cx       = this.width  / 2;
        int colLeft  = cx - COL_W - COL_GAP / 2;
        int colRight = cx + COL_GAP / 2;
        int rows     = (videoOptions.length + 1) / 2;

        // Section "Video Options"
        int secOptY  = HEADER_H + PAD;
        int gridY    = secOptY + SECTION_H + 4;

        // ── Grille des options ───────────────────────────────────────────────
        for (int i = 0; i < videoOptions.length; i++) {
            GameSettings.Options opt = videoOptions[i];
            int bx = (i % 2 == 0) ? colLeft : colRight;
            int by = gridY + (i / 2) * (BTN_H + BTN_GAP);
            if (opt.getEnumFloat())
                this.buttonList.add(new GuiOptionSliderOF(opt.returnEnumOrdinal(), bx, by, opt));
            else
                this.buttonList.add(new GuiOptionButtonOF(opt.returnEnumOrdinal(), bx, by, opt,
                        this.guiGameSettings.getKeyBinding(opt)));
        }

        // ── Section catégories ───────────────────────────────────────────────
        int secCatY = gridY + rows * (BTN_H + BTN_GAP) + 6;
        catStartY   = secCatY + SECTION_H + 4;

        this.buttonList.add(new GuiMenuButton(BTN_SHADERS,     colLeft,  catStartY,                       COL_W, BTN_H, Lang.get("of.options.shaders",     "Shaders...")));
        this.buttonList.add(new GuiMenuButton(BTN_QUALITY,     colRight, catStartY,                       COL_W, BTN_H, Lang.get("of.options.quality",      "Quality...")));
        this.buttonList.add(new GuiMenuButton(BTN_DETAILS,     colLeft,  catStartY +   (BTN_H + BTN_GAP), COL_W, BTN_H, Lang.get("of.options.details",     "Details...")));
        this.buttonList.add(new GuiMenuButton(BTN_PERFORMANCE, colRight, catStartY +   (BTN_H + BTN_GAP), COL_W, BTN_H, Lang.get("of.options.performance", "Performance...")));
        this.buttonList.add(new GuiMenuButton(BTN_ANIMATIONS,  colLeft,  catStartY + 2*(BTN_H + BTN_GAP), COL_W, BTN_H, Lang.get("of.options.animations", "Animations...")));
        this.buttonList.add(new GuiMenuButton(BTN_OTHER,       colRight, catStartY + 2*(BTN_H + BTN_GAP), COL_W, BTN_H, Lang.get("of.options.other",       "Other...")));

        // ── Done ─────────────────────────────────────────────────────────────
        int doneY = catStartY + 3*(BTN_H + BTN_GAP) + 6;
        int doneW = COL_W * 2 + COL_GAP;
        this.buttonList.add(new GuiMenuButton(BTN_DONE, colLeft, doneY, doneW, BTN_H,
                I18n.format("gui.done"), true));

        // ── Coordonnées du panel global (autour de tout le contenu) ──────────
        panelX = colLeft  - PAD;
        panelY = HEADER_H + 2;
        panelW = doneW    + PAD * 2;
        panelH = doneY + BTN_H - panelY + PAD;
    }

    // ── actionPerformed ──────────────────────────────────────────────────────
    @Override
    protected void actionPerformed(GuiButton button) throws IOException { actionDir(button, 1); }

    @Override
    protected void actionPerformedRightClick(GuiButton button) {
        if (button.id == GameSettings.Options.GUI_SCALE.ordinal()) actionDir(button, -1);
    }

    private void actionDir(GuiButton button, int dir) {
        if (!button.enabled) return;
        int prevScale = this.guiGameSettings.guiScale;

        if (button.id < 200 && button instanceof GuiOptionButtonOF) {
            GameSettings.Options opt = ((GuiOptionButtonOF) button).returnEnumOptions();
            this.guiGameSettings.setOptionValue(opt, dir);
            button.displayString = this.guiGameSettings.getKeyBinding(opt);
        }

        if (this.guiGameSettings.guiScale != prevScale) {
            ScaledResolution sr = new ScaledResolution(this.mc);
            this.setWorldAndResolution(this.mc, sr.getScaledWidth(), sr.getScaledHeight());
        }

        switch (button.id) {
            case BTN_DONE:        this.mc.gameSettings.saveOptions(); this.mc.displayGuiScreen(parentGuiScreen); break;
            case BTN_DETAILS:     this.mc.gameSettings.saveOptions(); this.mc.displayGuiScreen(new GuiDetailSettingsOF(this, guiGameSettings)); break;
            case BTN_QUALITY:     this.mc.gameSettings.saveOptions(); this.mc.displayGuiScreen(new GuiQualitySettingsOF(this, guiGameSettings)); break;
            case BTN_ANIMATIONS:  this.mc.gameSettings.saveOptions(); this.mc.displayGuiScreen(new GuiAnimationSettingsOF(this, guiGameSettings)); break;
            case BTN_PERFORMANCE: this.mc.gameSettings.saveOptions(); this.mc.displayGuiScreen(new GuiPerformanceSettingsOF(this, guiGameSettings)); break;
            case BTN_OTHER:       this.mc.gameSettings.saveOptions(); this.mc.displayGuiScreen(new GuiOtherSettingsOF(this, guiGameSettings)); break;
            case BTN_SHADERS:
                if (Config.isAntialiasing() || Config.isAntialiasingConfigured())
                { Config.showGuiMessage(Lang.get("of.message.shaders.aa1","Cannot use Shaders"), Lang.get("of.message.shaders.aa2","Antialiasing must be disabled.")); return; }
                if (Config.isAnisotropicFiltering())
                { Config.showGuiMessage(Lang.get("of.message.shaders.af1","Cannot use Shaders"), Lang.get("of.message.shaders.af2","Anisotropic Filtering must be disabled.")); return; }
                if (Config.isFastRender())
                { Config.showGuiMessage(Lang.get("of.message.shaders.fr1","Cannot use Shaders"), Lang.get("of.message.shaders.fr2","Fast Render must be disabled.")); return; }
                if (Config.getGameSettings().anaglyph)
                { Config.showGuiMessage(Lang.get("of.message.shaders.an1","Cannot use Shaders"), Lang.get("of.message.shaders.an2","3D Anaglyph must be disabled.")); return; }
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiShaders(this, guiGameSettings));
                break;
        }
    }

    // ── drawScreen ───────────────────────────────────────────────────────────
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime >= 0)
            animation = MathHelper.clamp_float(animation + (now - lastTime) / 1000.0f * 5.0f, 0.0f, 1.0f);
        lastTime = now;
        float e = animation * animation * (3.0f - 2.0f * animation);

        this.drawDefaultBackground();
        Gui.drawRect(0, 0, this.width, this.height, (int)(e * 70) << 24);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - e) * 10, 0);

        int alpha  = (int)(e * 255) << 24;
        int cx     = this.width / 2;

        // ── Header ───────────────────────────────────────────────────────────
        int hW = this.width - 8;
        GuiRenderUtils.drawShadow(4, 2, hW, HEADER_H, 5, (int)(e * 90));
        Gui.drawRect(4, 2, 4 + hW, 2 + HEADER_H, (int)(e * 210) << 24 | 0x0C0C0C);
        Gui.drawRect(4, 2, 4 + hW, 3, alpha | (ACCENT & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(4, 2, hW, HEADER_H, (int)(e * 30) << 24 | 0xFFFFFF);

        String t1 = "§c§lRED ", t2 = "§f§lCONFLICT", sep = " §8| §7";
        int w1 = fr(t1), w2 = fr(t2), w3 = fr(sep);
        int tX = cx - (w1 + w2 + w3 + fr(screenTitle)) / 2;
        int tY = 2 + (HEADER_H - 8) / 2;
        this.fontRendererObj.drawStringWithShadow(t1,   tX,                 tY, alpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(t2,   tX + w1,            tY, alpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(sep,  tX + w1 + w2,       tY, alpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(screenTitle, tX + w1 + w2 + w3, tY, alpha | 0xFFFFFF);

        // ── Panel principal ──────────────────────────────────────────────────
        GuiRenderUtils.drawShadow(panelX, panelY, panelW, panelH, 8, (int)(e * 100));
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, (int)(e * 200) << 24 | 0x080808);
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 1, alpha | (ACCENT & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(panelX, panelY, panelW, panelH, (int)(e * 35) << 24 | 0xFFFFFF);

        // ── Section "Video Options" ──────────────────────────────────────────
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj,
                panelX + PAD, HEADER_H + PAD,
                panelW - PAD * 2, I18n.format("options.videoTitle"), ACCENT);

        // ── Séparateur entre options et catégories ───────────────────────────
        int sepY = catStartY - SECTION_H - 6;
        int sepX = cx - (COL_W + COL_GAP / 2);
        Gui.drawRect(sepX, sepY + 5, sepX + COL_W * 2 + COL_GAP, sepY + 6,
                (int)(e * 40) << 24 | 0xFFFFFF);

        // ── Section "Settings" ───────────────────────────────────────────────
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj,
                panelX + PAD, catStartY - SECTION_H - 2,
                panelW - PAD * 2, "Settings", ACCENT);

        // ── Boutons + Tooltips ───────────────────────────────────────────────
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttonList);

        // ── Pied de page ─────────────────────────────────────────────────────
        String verL = "OptiFine HD M6_pre2 Ultra";
        String verR = "Minecraft 1.8.9";
        this.drawString(fontRendererObj, "§8" + verL, 2, this.height - 10, 0xFFFFFF);
        this.drawString(fontRendererObj, "§8" + verR, this.width - fr(verR) - 2, this.height - 10, 0xFFFFFF);

        GlStateManager.popMatrix();
    }

    private int fr(String s) { return this.fontRendererObj.getStringWidth(s); }

    // ── Statics GuiScreenOF ──────────────────────────────────────────────────
    public static int    getButtonWidth(GuiButton btn)  { return btn.width; }
    public static int    getButtonHeight(GuiButton btn) { return btn.height; }
    public static void   drawGradientRect(GuiScreen s, int x1, int y1, int x2, int y2, int c1, int c2) { s.drawGradientRect(x1, y1, x2, y2, c1, c2); }
    public static String getGuiChatText(GuiChat chat)   { return chat.inputField.getText(); }
}
