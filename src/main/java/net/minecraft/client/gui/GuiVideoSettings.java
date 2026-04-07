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

    // Options displayed in the left 2-column grid
    private static final GameSettings.Options[] videoOptions = {
        GameSettings.Options.GRAPHICS,          GameSettings.Options.RENDER_DISTANCE,
        GameSettings.Options.AMBIENT_OCCLUSION, GameSettings.Options.FRAMERATE_LIMIT,
        GameSettings.Options.AO_LEVEL,          GameSettings.Options.VIEW_BOBBING,
        GameSettings.Options.GUI_SCALE,         GameSettings.Options.USE_VBO,
        GameSettings.Options.GAMMA,             GameSettings.Options.BLOCK_ALTERNATIVES,
        GameSettings.Options.DYNAMIC_LIGHTS,    GameSettings.Options.DYNAMIC_FOV,
    };

    private static final int BTN_DONE        = 200;
    private static final int BTN_DETAILS     = 201;
    private static final int BTN_QUALITY     = 202;
    private static final int BTN_ANIMATIONS  = 211;
    private static final int BTN_PERFORMANCE = 212;
    private static final int BTN_OTHER       = 222;
    private static final int BTN_SHADERS     = 231;

    private float animation = 0.0f;
    private long  lastTime  = -1L;
    private static final int ACCENT = new Color(220, 30, 30).getRGB();

    // ── Layout constants ─────────────────────────────────────────────────────
    // Header bar (full-width, title)
    private static final int HDR_H       = 28;
    // Footer bar (full-width, version info)
    private static final int FTR_H       = 22;
    // Inner padding of the panel
    private static final int PAD         = 8;
    // Button dimensions
    private static final int BTN_H       = 20;
    private static final int BTN_GAP     = 3;
    // Section label height (icon + text block + spacing below)
    private static final int SEC_H       = 18;
    // Left section: 2-column option grid — must stay 150px (slider hard-codes it)
    private static final int OPT_COL_W   = 150;
    private static final int OPT_COL_GAP = 10;
    // Gap between the two sections (vertical divider lives in the middle)
    private static final int SPLIT_GAP   = 12;
    // Right section: single-column category navigation
    private static final int CAT_COL_W   = 150;

    // ── Computed positions (set in initGui, consumed in drawScreen) ──────────
    private int panelX, panelY, panelW, panelH;
    private int colOptL, colOptR, colCat;
    private int secY, gridY;

    private final TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiVideoSettings(GuiScreen parent, GameSettings settings)
    {
        this.parentGuiScreen = parent;
        this.guiGameSettings = settings;
    }

    // ── initGui ──────────────────────────────────────────────────────────────
    @Override
    public void initGui()
    {
        this.screenTitle = I18n.format("options.videoTitle");
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();

        int rows  = (videoOptions.length + 1) / 2;          // 6 rows
        int leftW = OPT_COL_W * 2 + OPT_COL_GAP;           // 310 px — option grid

        // Panel width:  padding + left(310) + split(12) + right(150) + padding  = 488
        panelW = PAD + leftW + SPLIT_GAP + CAT_COL_W + PAD;

        // Panel height: top-pad + section-header + 6 option rows + gap + done + bot-pad
        // = 8 + 18 + (6×23 − 3) + 6 + 20 + 8 = 195
        panelH = PAD + SEC_H + rows * (BTN_H + BTN_GAP) - BTN_GAP + 6 + BTN_H + PAD;

        // Center panel horizontally; center vertically in the space between header and footer
        panelX = this.width  / 2 - panelW / 2;
        int available = this.height - HDR_H - FTR_H;
        panelY = HDR_H + Math.max(4, (available - panelH) / 2);

        // Column x-positions
        colOptL = panelX + PAD;
        colOptR = colOptL + OPT_COL_W + OPT_COL_GAP;
        colCat  = colOptR + OPT_COL_W + SPLIT_GAP;

        // Content y-positions
        secY  = panelY + PAD;
        gridY = secY + SEC_H;

        // ── Option buttons (12 options, 2 cols, 6 rows) ──────────────────────
        for (int i = 0; i < videoOptions.length; i++)
        {
            GameSettings.Options opt = videoOptions[i];
            int bx = (i % 2 == 0) ? colOptL : colOptR;
            int by = gridY + (i / 2) * (BTN_H + BTN_GAP);
            if (opt.getEnumFloat())
                this.buttonList.add(new GuiOptionSliderOF(opt.returnEnumOrdinal(), bx, by, opt));
            else
                this.buttonList.add(new GuiOptionButtonOF(opt.returnEnumOrdinal(), bx, by, opt,
                        this.guiGameSettings.getKeyBinding(opt)));
        }

        // ── Category buttons (6 items, single column) ────────────────────────
        int[] catIds = { BTN_SHADERS, BTN_QUALITY, BTN_DETAILS, BTN_PERFORMANCE, BTN_ANIMATIONS, BTN_OTHER };
        String[] catLabels = {
            Lang.get("of.options.shaders",     "Shaders..."),
            Lang.get("of.options.quality",     "Quality..."),
            Lang.get("of.options.details",     "Details..."),
            Lang.get("of.options.performance", "Performance..."),
            Lang.get("of.options.animations",  "Animations..."),
            Lang.get("of.options.other",        "Other..."),
        };
        for (int i = 0; i < catIds.length; i++)
            this.buttonList.add(new GuiMenuButton(catIds[i], colCat,
                    gridY + i * (BTN_H + BTN_GAP), CAT_COL_W, BTN_H, catLabels[i]));

        // ── Done button (full panel width, anchored to panel bottom) ──────────
        int doneY = panelY + panelH - PAD - BTN_H;
        int doneW = panelW - PAD * 2;
        this.buttonList.add(new GuiMenuButton(BTN_DONE, panelX + PAD, doneY, doneW, BTN_H,
                I18n.format("gui.done"), true));
    }

    // ── actionPerformed ──────────────────────────────────────────────────────
    @Override
    protected void actionPerformed(GuiButton button) throws IOException { actionDir(button, 1); }

    @Override
    protected void actionPerformedRightClick(GuiButton button)
    {
        if (button.id == GameSettings.Options.GUI_SCALE.ordinal()) actionDir(button, -1);
    }

    private void actionDir(GuiButton button, int dir)
    {
        if (!button.enabled) return;
        int prevScale = this.guiGameSettings.guiScale;

        if (button.id < 200 && button instanceof GuiOptionButtonOF)
        {
            GameSettings.Options opt = ((GuiOptionButtonOF) button).returnEnumOptions();
            this.guiGameSettings.setOptionValue(opt, dir);
            button.displayString = this.guiGameSettings.getKeyBinding(opt);
        }

        if (this.guiGameSettings.guiScale != prevScale)
        {
            ScaledResolution sr = new ScaledResolution(this.mc);
            this.setWorldAndResolution(this.mc, sr.getScaledWidth(), sr.getScaledHeight());
        }

        switch (button.id)
        {
            case BTN_DONE:
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(parentGuiScreen);
                break;
            case BTN_DETAILS:
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiDetailSettingsOF(this, guiGameSettings));
                break;
            case BTN_QUALITY:
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiQualitySettingsOF(this, guiGameSettings));
                break;
            case BTN_ANIMATIONS:
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiAnimationSettingsOF(this, guiGameSettings));
                break;
            case BTN_PERFORMANCE:
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiPerformanceSettingsOF(this, guiGameSettings));
                break;
            case BTN_OTHER:
                this.mc.gameSettings.saveOptions();
                this.mc.displayGuiScreen(new GuiOtherSettingsOF(this, guiGameSettings));
                break;
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = Minecraft.getSystemTime();
        if (lastTime >= 0)
            animation = MathHelper.clamp_float(animation + (now - lastTime) / 1000.0f * 5.0f, 0.0f, 1.0f);
        lastTime = now;
        float e = animation * animation * (3.0f - 2.0f * animation);
        int a = (int)(e * 255) << 24;

        this.drawDefaultBackground();
        // Léger overlay sombre animé par-dessus le fond monde
        Gui.drawRect(0, 0, this.width, this.height, (int)(e * 140) << 24 | 0x050505);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - e) * 8, 0);

        // ── Header bar ───────────────────────────────────────────────────────
        Gui.drawRect(0, 0, this.width, HDR_H, (int)(e * 220) << 24 | 0x0C0C0C);
        Gui.drawRect(0, 0, this.width, 1, a | (ACCENT & 0xFFFFFF));
        Gui.drawRect(0, HDR_H - 1, this.width, HDR_H, (int)(e * 40) << 24 | 0xFFFFFF);

        // Title: "RED CONFLICT | <screen title>"
        String t1 = "§c§lRED ", t2 = "§f§lCONFLICT", sep = " §8| §7";
        int w1 = fr(t1), w2 = fr(t2), w3 = fr(sep), w4 = fr(screenTitle);
        int titleX = this.width / 2 - (w1 + w2 + w3 + w4) / 2;
        int titleY = (HDR_H - 8) / 2;
        this.fontRendererObj.drawStringWithShadow(t1,          titleX,              titleY, a | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(t2,          titleX + w1,         titleY, a | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(sep,         titleX + w1 + w2,    titleY, a | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(screenTitle, titleX + w1+w2+w3,   titleY, a | 0xFFFFFF);

        // ── Main panel ───────────────────────────────────────────────────────
        GuiRenderUtils.drawShadow(panelX, panelY, panelW, panelH, 6, (int)(e * 100));
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, (int)(e * 200) << 24 | 0x080808);
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 1, a | (ACCENT & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(panelX, panelY, panelW, panelH, (int)(e * 35) << 24 | 0xFFFFFF);

        // Vertical divider between option grid (left) and category list (right)
        int divX = colCat - SPLIT_GAP / 2;
        // Calculer la position du Done pour éviter que le diviseur ne chevauche le bouton
        int doneY = panelY + panelH - PAD - BTN_H;
        int dividerTop = panelY + PAD * 2;
        int dividerBottom = Math.max(dividerTop, doneY - 6); // laisse une marge de 6px au-dessus du bouton Done
        Gui.drawRect(divX, dividerTop, divX + 1, dividerBottom,
                (int)(e * 30) << 24 | 0xFFFFFF);

        // ── Section headers ───────────────────────────────────────────────────
        int leftW = OPT_COL_W * 2 + OPT_COL_GAP;
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, colOptL, secY, leftW,
                I18n.format("options.videoTitle"), ACCENT);
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, colCat, secY, CAT_COL_W,
                "Settings", ACCENT);

        // ── Buttons + tooltips ────────────────────────────────────────────────
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttonList);

        // ── Footer bar ────────────────────────────────────────────────────────
        int footY = this.height - FTR_H;
        Gui.drawRect(0, footY, this.width, this.height, (int)(e * 160) << 24 | 0x0A0A0A);
        Gui.drawRect(0, footY, this.width, footY + 1, (int)(e * 50) << 24 | 0xFFFFFF);

        int ta = (int)(e * 150) << 24;
        String verStr = "§8Minecraft 1.8.9 | OptiFine";
        int verW = this.fontRendererObj.getStringWidth(verStr);
        this.fontRendererObj.drawString(verStr, this.width / 2 - verW / 2,
                footY + (FTR_H - 8) / 2, ta | 0xFFFFFF);

        GlStateManager.popMatrix();
    }

    private int fr(String s) { return this.fontRendererObj.getStringWidth(s); }

    // ── Static helpers required by GuiScreenOF ────────────────────────────────
    public static int    getButtonWidth(GuiButton btn)  { return btn.width; }
    public static int    getButtonHeight(GuiButton btn) { return btn.height; }
    public static void   drawGradientRect(GuiScreen s, int x1, int y1, int x2, int y2, int c1, int c2) { s.drawGradientRect(x1, y1, x2, y2, c1, c2); }
    public static String getGuiChatText(GuiChat chat)   { return chat.inputField.getText(); }
}
