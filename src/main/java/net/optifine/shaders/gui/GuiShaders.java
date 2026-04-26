package net.optifine.shaders.gui;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMenuButton;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.src.Config;
import net.minecraft.util.MathHelper;
import net.optifine.Lang;
import net.optifine.gui.GuiScreenOF;
import net.optifine.gui.TooltipManager;
import net.optifine.gui.TooltipProviderEnumShaderOptions;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersTex;
import net.optifine.shaders.config.EnumShaderOption;
import org.lwjgl.Sys;

public class GuiShaders extends GuiScreenOF {
    protected GuiScreen parentGui;
    protected String screenTitle = "Shaders";
    private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderEnumShaderOptions());
    private int updateTimer = -1;
    private GuiSlotShaders shaderList;
    private boolean saved = false;
    private static float[] QUALITY_MULTIPLIERS = new float[]{0.5F, 0.6F, 0.6666667F, 0.75F, 0.8333333F, 0.9F, 1.0F, 1.1666666F, 1.3333334F, 1.5F, 1.6666666F, 1.8F, 2.0F};
    private static String[] QUALITY_MULTIPLIER_NAMES = new String[]{"0.5x", "0.6x", "0.66x", "0.75x", "0.83x", "0.9x", "1x", "1.16x", "1.33x", "1.5x", "1.66x", "1.8x", "2x"};
    private static float QUALITY_MULTIPLIER_DEFAULT = 1.0F;
    private static float[] HAND_DEPTH_VALUES = new float[]{0.0625F, 0.125F, 0.25F};
    private static String[] HAND_DEPTH_NAMES = new String[]{"0.5x", "1x", "2x"};
    private static float HAND_DEPTH_DEFAULT = 0.125F;
    public static final int EnumOS_UNKNOWN = 0;
    public static final int EnumOS_WINDOWS = 1;
    public static final int EnumOS_OSX = 2;
    public static final int EnumOS_SOLARIS = 3;
    public static final int EnumOS_LINUX = 4;

    // Style RED CONFLICT
    private float animation = 0.0f;
    private long lastTime = -1L;
    private final int accentColor = new Color(220, 30, 30).getRGB();

    // IDs boutons
    private static final int BTN_FOLDER = 201;
    private static final int BTN_DONE = 202;
    private static final int BTN_DOWNLOAD = 210;
    private static final int BTN_LOAD = 220;
    private static final int BTN_REFRESH = 221;

    // Layout
    private int listRight;
    private int optPanelX;
    private int optPanelY;
    private static final int OPT_BTN_W = 130;
    private static final int OPT_BTN_H = 20;
    private static final int OPT_GAP = 3;
    private static final int HEADER_H  = 36;
    private static final int FOOTER_H  = 32;

    public GuiShaders(GuiScreen par1GuiScreen, GameSettings par2GameSettings) {
        this.parentGui = par1GuiScreen;
    }

    public void initGui() {
        this.screenTitle = I18n.format("of.options.shadersTitle", new Object[0]);
        this.animation = 0.0f;
        this.lastTime  = Minecraft.getSystemTime();

        if (Shaders.shadersConfig == null) {
            Shaders.loadConfig();
        }

        // ── Layout ──────────────────────────────────────────────────────────
        int optPanelW = OPT_BTN_W + 20;
        optPanelX = this.width - optPanelW - 4;
        int listW = optPanelX - 8;
        listRight = optPanelX;

        int listTop    = HEADER_H + 22;
        int listBottom = this.height - FOOTER_H - 4;

        this.shaderList = new GuiSlotShaders(this, listW, this.height, listTop, listBottom, 20);
        this.shaderList.registerScrollButtons(7, 8);

        // ── Boutons options shaders (panneau droit) ──────────────────────────
        optPanelY = listTop + 4;
        int ox = optPanelX + 10;
        int oy = optPanelY + 22;

        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.ANTIALIASING, ox, oy, OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.NORMAL_MAP, ox, oy + (OPT_BTN_H + OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.SPECULAR_MAP, ox, oy + 2 * (OPT_BTN_H + OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.RENDER_RES_MUL, ox, oy + 3 * (OPT_BTN_H + OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.SHADOW_RES_MUL, ox, oy + 4 * (OPT_BTN_H + OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.HAND_DEPTH_MUL, ox, oy + 5 * (OPT_BTN_H + OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.OLD_HAND_LIGHT, ox, oy + 6 * (OPT_BTN_H + OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.OLD_LIGHTING, ox, oy + 7 * (OPT_BTN_H + OPT_GAP), OPT_BTN_W, OPT_BTN_H));

        // ── Boutons de bas de page (4 boutons) ───────────────────
        int botY = this.height - FOOTER_H + 6;
        int padding = 6;
        int btnW = Math.max(70, (this.width - 20 - padding * 3) / 4);
        int startX = (this.width - (btnW * 4 + padding * 3)) / 2;

        this.buttonList.add(new GuiMenuButton(BTN_FOLDER,         startX,                     botY, btnW, 20, Lang.get("of.options.shaders.shadersFolder")));
        this.buttonList.add(new GuiMenuButton(BTN_LOAD,           startX + (btnW+padding),    botY, btnW, 20, Lang.get("of.options.shaders.load", "Load")));
        this.buttonList.add(new GuiMenuButton(BTN_REFRESH,        startX + (btnW+padding)*2,  botY, btnW, 20, Lang.get("of.options.shaders.refresh", "Refresh")));
        this.buttonList.add(new GuiMenuButton(BTN_DONE,           startX + (btnW+padding)*3,  botY, btnW, 20, I18n.format("gui.done", new Object[0]), true));

        this.updateButtons();
    }

    public void updateButtons() {
        boolean flag = Config.isShaders();
        for (GuiButton guibutton : this.buttonList) {
            if (guibutton.id != BTN_FOLDER && guibutton.id != BTN_DONE && guibutton.id != BTN_DOWNLOAD
                    && guibutton.id != EnumShaderOption.ANTIALIASING.ordinal()) {
                guibutton.enabled = flag;
            }
        }
    }

    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.shaderList.handleMouseInput();
    }

    protected void actionPerformed(GuiButton button) {
        this.actionPerformed(button, false);
    }

    protected void actionPerformedRightClick(GuiButton button) {
        this.actionPerformed(button, true);
    }

    private void actionPerformed(GuiButton button, boolean rightClick) {
        if (button.enabled) {
            if (!(button instanceof GuiButtonEnumShaderOption)) {
                if (!rightClick) {
                    switch (button.id) {
                        case BTN_FOLDER:
                            switch (getOSType()) {
                                case 1:
                                    String s = String.format("cmd.exe /C start \"Open file\" \"%s\"", new Object[]{Shaders.shaderPacksDir.getAbsolutePath()});
                                    try {
                                        Runtime.getRuntime().exec(s);
                                        return;
                                    } catch (IOException ioexception) {
                                        ioexception.printStackTrace();
                                        break;
                                    }
                                case 2:
                                    try {
                                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", Shaders.shaderPacksDir.getAbsolutePath()});
                                        return;
                                    } catch (IOException ioexception1) {
                                        ioexception1.printStackTrace();
                                    }
                            }
                            boolean flag = false;
                            try {
                                Class oclass1 = Class.forName("java.awt.Desktop");
                                Object object1 = oclass1.getMethod("getDesktop", new Class[0]).invoke((Object) null, new Object[0]);
                                oclass1.getMethod("browse", new Class[]{URI.class}).invoke(object1, new Object[]{(new File(this.mc.mcDataDir, "shaderpacks")).toURI()});
                            } catch (Throwable throwable1) {
                                throwable1.printStackTrace();
                                flag = true;
                            }
                            if (flag) {
                                Sys.openURL("file://" + Shaders.shaderPacksDir.getAbsolutePath());
                            }
                            break;
                        case BTN_LOAD:
                            int sel = this.shaderList.getSelectedIndex();
                            if (sel >= 0 && sel < this.shaderList.getSize()) {
                                this.shaderList.elementClicked(sel, false, 0, 0);
                            }
                            break;
                        case BTN_REFRESH:
                            this.shaderList.updateList();
                            break;
                        case BTN_DONE:
                            Shaders.storeConfig();
                            this.saved = true;
                            this.mc.displayGuiScreen(this.parentGui);
                            break;
                        case BTN_DOWNLOAD:
                            try {
                                Class<?> oclass = Class.forName("java.awt.Desktop");
                                Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object) null, new Object[0]);
                                oclass.getMethod("browse", new Class[]{URI.class}).invoke(object, new Object[]{new URI("http://optifine.net/shaderPacks")});
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
                            break;
                        default:
                            this.shaderList.actionPerformed(button);
                    }
                }
            } else {
                GuiButtonEnumShaderOption guibuttonenumshaderoption = (GuiButtonEnumShaderOption) button;
                switch (guibuttonenumshaderoption.getEnumShaderOption()) {
                    case ANTIALIASING:
                        Shaders.nextAntialiasingLevel(!rightClick);
                        if (this.hasShiftDown()) Shaders.configAntialiasingLevel = 0;
                        Shaders.uninit();
                        break;
                    case NORMAL_MAP:
                        Shaders.configNormalMap = !Shaders.configNormalMap;
                        if (this.hasShiftDown()) Shaders.configNormalMap = true;
                        Shaders.uninit();
                        this.mc.scheduleResourcesRefresh();
                        break;
                    case SPECULAR_MAP:
                        Shaders.configSpecularMap = !Shaders.configSpecularMap;
                        if (this.hasShiftDown()) Shaders.configSpecularMap = true;
                        Shaders.uninit();
                        this.mc.scheduleResourcesRefresh();
                        break;
                    case RENDER_RES_MUL:
                        Shaders.configRenderResMul = this.getNextValue(Shaders.configRenderResMul, QUALITY_MULTIPLIERS, QUALITY_MULTIPLIER_DEFAULT, !rightClick, this.hasShiftDown());
                        Shaders.uninit();
                        Shaders.scheduleResize();
                        break;
                    case SHADOW_RES_MUL:
                        Shaders.configShadowResMul = this.getNextValue(Shaders.configShadowResMul, QUALITY_MULTIPLIERS, QUALITY_MULTIPLIER_DEFAULT, !rightClick, this.hasShiftDown());
                        Shaders.uninit();
                        Shaders.scheduleResizeShadow();
                        break;
                    case HAND_DEPTH_MUL:
                        Shaders.configHandDepthMul = this.getNextValue(Shaders.configHandDepthMul, HAND_DEPTH_VALUES, HAND_DEPTH_DEFAULT, !rightClick, this.hasShiftDown());
                        Shaders.uninit();
                        break;
                    case OLD_HAND_LIGHT:
                        Shaders.configOldHandLight.nextValue(!rightClick);
                        if (this.hasShiftDown()) Shaders.configOldHandLight.resetValue();
                        Shaders.uninit();
                        break;
                    case OLD_LIGHTING:
                        Shaders.configOldLighting.nextValue(!rightClick);
                        if (this.hasShiftDown()) Shaders.configOldLighting.resetValue();
                        Shaders.updateBlockLightLevel();
                        Shaders.uninit();
                        this.mc.scheduleResourcesRefresh();
                        break;
                }
                guibuttonenumshaderoption.updateButtonText();
            }
        }
    }

    public void onGuiClosed() {
        super.onGuiClosed();
        if (!this.saved) {
            Shaders.storeConfig();
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime >= 0)
            animation = MathHelper.clamp_float(animation + (now - lastTime) / 1000.0f * 5.0f, 0.0f, 1.0f);
        lastTime = now;
        float e = animation * animation * (3.0f - 2.0f * animation);

        this.drawDefaultBackground();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - e) * 8, 0);

        int textAlpha = (int)(e * 255) << 24;

        // Header
        GuiRenderUtils.drawGradientRect(0, 0, this.width, 42, (int)(e * 170) << 24 | 0x000000, 0x00000000);
        Gui.drawRect(0, 0, this.width, 1, textAlpha | (accentColor & 0xFFFFFF));

        // Titre
        String t1 = "\u00A7c\u00A7lRED ", t2 = "\u00A7f\u00A7lCONFLICT", sep = " \u00A78| \u00A77";
        int w1 = fr(t1), w2 = fr(t2), w3 = fr(sep), w4 = fr(this.screenTitle);
        int titleX = this.width / 2 - (w1 + w2 + w3 + w4) / 2;
        int titleY = 11;
        this.fontRendererObj.drawStringWithShadow(t1,               titleX,              titleY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(t2,               titleX + w1,         titleY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(sep,              titleX + w1 + w2,    titleY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(this.screenTitle, titleX + w1+w2+w3,   titleY, textAlpha | 0xFFFFFF);
        
        int divW2 = (int)((w1 + w2 + w3 + w4 + 20) * e);
        Gui.drawRect(this.width/2 - divW2/2, 23, this.width/2 + divW2/2, 24, (int)(e*60) << 24 | 0xFFFFFF);

        // GPU Info
        String gpuRaw = Shaders.glRendererString != null ? Shaders.glRendererString : "";
        if (!gpuRaw.isEmpty()) {
            String gpuStr = "\u00A78" + gpuRaw;
            int maxGpuW = this.width / 3;
            while (fr(gpuStr) > maxGpuW && gpuStr.length() > 4) gpuStr = gpuStr.substring(0, gpuStr.length() - 1);
            this.fontRendererObj.drawString(gpuStr, this.width - fr(gpuStr) - 8, titleY + 1, (int)(e * 140) << 24 | 0xFFFFFF);
        }

        // Panneau Liste
        int listPanelX = 4;
        int listPanelY = HEADER_H + 2;
        int listPanelW = listRight - listPanelX - 2;
        int listPanelH = this.height - FOOTER_H - listPanelY - 2;
        GuiRenderUtils.drawShadow(listPanelX, listPanelY, listPanelW, listPanelH, 4, (int)(e * 80));
        Gui.drawRect(listPanelX, listPanelY, listPanelX + listPanelW, listPanelY + listPanelH, (int)(e * 180) << 24 | 0x0A0A0A);
        GuiRenderUtils.drawRectOutline(listPanelX, listPanelY, listPanelW, listPanelH, (int)(e * 35) << 24 | 0xFFFFFF);

        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, listPanelX + 4, listPanelY + 4, listPanelW - 8, "Shader Packs", accentColor);
        this.shaderList.drawScreen(mouseX, mouseY, partialTicks);

        if (this.updateTimer <= 0) {
            this.shaderList.updateList();
            this.updateTimer += 20;
        }

        // Panneau Options
        int optPanelW2 = OPT_BTN_W + 20;
        int optPanelH  = this.height - FOOTER_H - (HEADER_H + 2) - 2;
        GuiRenderUtils.drawShadow(optPanelX, HEADER_H + 2, optPanelW2, optPanelH, 4, (int)(e * 80));
        Gui.drawRect(optPanelX, HEADER_H + 2, optPanelX + optPanelW2, HEADER_H + 2 + optPanelH, (int)(e * 180) << 24 | 0x080808);
        Gui.drawRect(optPanelX, HEADER_H + 2, optPanelX + optPanelW2, HEADER_H + 3, textAlpha | (accentColor & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(optPanelX, HEADER_H + 2, optPanelW2, optPanelH, (int)(e * 35) << 24 | 0xFFFFFF);

        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, optPanelX + 4, HEADER_H + 6, optPanelW2 - 8, "Options", accentColor);

        // Footer
        int footY = this.height - FOOTER_H;
        GuiRenderUtils.drawGradientRect(0, footY - 12, this.width, footY, 0x00000000, (int)(e * 130) << 24 | 0x000000);
        Gui.drawRect(0, footY, this.width, this.height, (int)(e * 160) << 24 | 0x080808);
        Gui.drawRect(0, footY, this.width, footY + 1, (int)(e * 40) << 24 | 0xFFFFFF);

        // Boutons
        for (int i = 0; i < this.buttonList.size(); ++i) {
            GuiButton guibutton = (GuiButton)this.buttonList.get(i);
            float stagger = MathHelper.clamp_float(animation * 1.5f - (i * 0.05f), 0.0f, 1.0f);
            if (stagger > 0) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0, (1.0f - stagger) * 4, 0);
                guibutton.drawButton(this.mc, mouseX, mouseY);
                GlStateManager.popMatrix();
            }
        }
        this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttonList);
        GlStateManager.popMatrix();
    }

    private int fr(String s) {
        return this.fontRendererObj.getStringWidth(s);
    }

    public void updateScreen() {
        super.updateScreen();
        --this.updateTimer;
    }

    public Minecraft getMc() {
        return this.mc;
    }

    public void drawCenteredString(String text, int x, int y, int color) {
        this.drawCenteredString(this.fontRendererObj, text, x, y, color);
    }

    public static String toStringOnOff(boolean value) {
        return value ? Lang.getOn() : Lang.getOff();
    }

    public static String toStringAa(int value) {
        return value == 2 ? "FXAA 2x" : (value == 4 ? "FXAA 4x" : Lang.getOff());
    }

    public static String toStringValue(float val, float[] values, String[] names) {
        int i = getValueIndex(val, values);
        return names[i];
    }

    private float getNextValue(float val, float[] values, float valDef, boolean forward, boolean reset) {
        if (reset) return valDef;
        int i = getValueIndex(val, values);
        if (forward) {
            i++;
            if (i >= values.length) i = 0;
        } else {
            i--;
            if (i < 0) i = values.length - 1;
        }
        return values[i];
    }

    public static int getValueIndex(float val, float[] values) {
        for (int i = 0; i < values.length; ++i) {
            if (values[i] >= val) return i;
        }
        return values.length - 1;
    }

    public static String toStringQuality(float val) {
        return toStringValue(val, QUALITY_MULTIPLIERS, QUALITY_MULTIPLIER_NAMES);
    }

    public static String toStringHandDepth(float val) {
        return toStringValue(val, HAND_DEPTH_VALUES, HAND_DEPTH_NAMES);
    }

    public static int getOSType() {
        String s = System.getProperty("os.name").toLowerCase();
        return s.contains("win") ? 1 : (s.contains("mac") ? 2 : (s.contains("solaris") ? 3 : (s.contains("sunos") ? 3 : (s.contains("linux") ? 4 : (s.contains("unix") ? 4 : 0)))));
    }

    public boolean hasShiftDown() {
        return isShiftKeyDown();
    }
}
