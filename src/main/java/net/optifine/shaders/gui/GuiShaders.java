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

public class GuiShaders extends GuiScreenOF
{
    protected GuiScreen parentGui;
    protected String screenTitle = "Shaders";
    private TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderEnumShaderOptions());
    private int updateTimer = -1;
    private GuiSlotShaders shaderList;
    private boolean saved = false;
    private static float[] QUALITY_MULTIPLIERS = new float[] {0.5F, 0.6F, 0.6666667F, 0.75F, 0.8333333F, 0.9F, 1.0F, 1.1666666F, 1.3333334F, 1.5F, 1.6666666F, 1.8F, 2.0F};
    private static String[] QUALITY_MULTIPLIER_NAMES = new String[] {"0.5x", "0.6x", "0.66x", "0.75x", "0.83x", "0.9x", "1x", "1.16x", "1.33x", "1.5x", "1.66x", "1.8x", "2x"};
    private static float QUALITY_MULTIPLIER_DEFAULT = 1.0F;
    private static float[] HAND_DEPTH_VALUES = new float[] {0.0625F, 0.125F, 0.25F};
    private static String[] HAND_DEPTH_NAMES = new String[] {"0.5x", "1x", "2x"};
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
    private static final int BTN_FOLDER        = 201;
    private static final int BTN_DONE          = 202;
    private static final int BTN_SHADER_OPTIONS = 203;
    private static final int BTN_DOWNLOAD      = 210;

    // Layout
    private int listRight;  // x où la liste s'arrête (début du panneau options)
    private int optPanelX;
    private int optPanelY;
    private static final int OPT_BTN_W = 130;
    private static final int OPT_BTN_H = 20;
    private static final int OPT_GAP   = 3;
    private static final int HEADER_H  = 36;
    private static final int FOOTER_H  = 32;

    public GuiShaders(GuiScreen par1GuiScreen, GameSettings par2GameSettings)
    {
        this.parentGui = par1GuiScreen;
    }

    public void initGui()
    {
        this.screenTitle = I18n.format("of.options.shadersTitle", new Object[0]);
        this.lastTime = Minecraft.getSystemTime();

        if (Shaders.shadersConfig == null)
        {
            Shaders.loadConfig();
        }

        // ── Layout ──────────────────────────────────────────────────────────
        // Panneau options à droite: largeur = OPT_BTN_W + marges
        int optPanelW  = OPT_BTN_W + 20;
        optPanelX      = this.width - optPanelW - 4;
        int listW      = optPanelX - 8;
        listRight      = optPanelX;

        int listTop    = HEADER_H + 2;
        int listBottom = this.height - FOOTER_H - 2;

        this.shaderList = new GuiSlotShaders(this, listW, this.height, listTop, listBottom, 20);
        this.shaderList.registerScrollButtons(7, 8);

        // ── Boutons options shaders (panneau droit) ──────────────────────────
        optPanelY = listTop + 4;
        int ox = optPanelX + 10;
        int oy = optPanelY;

        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.ANTIALIASING,  ox, oy,                         OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.NORMAL_MAP,    ox, oy + (OPT_BTN_H + OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.SPECULAR_MAP,  ox, oy + 2*(OPT_BTN_H+OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.RENDER_RES_MUL,ox, oy + 3*(OPT_BTN_H+OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.SHADOW_RES_MUL,ox, oy + 4*(OPT_BTN_H+OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.HAND_DEPTH_MUL,ox, oy + 5*(OPT_BTN_H+OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.OLD_HAND_LIGHT,ox, oy + 6*(OPT_BTN_H+OPT_GAP), OPT_BTN_W, OPT_BTN_H));
        this.buttonList.add(new GuiButtonEnumShaderOption(EnumShaderOption.OLD_LIGHTING,  ox, oy + 7*(OPT_BTN_H+OPT_GAP), OPT_BTN_W, OPT_BTN_H));

        // ── Boutons de bas de page ───────────────────────────────────────────
        int botY     = this.height - FOOTER_H + 6;
        int halfList = listW / 2 - 2;

        // Bouton dossier (gauche)
        this.buttonList.add(new GuiMenuButton(BTN_FOLDER,  4,                     botY, halfList - 14, 20, Lang.get("of.options.shaders.shadersFolder")));
        // Bouton téléchargement (icône)
        this.buttonList.add(new GuiButtonDownloadShaders(BTN_DOWNLOAD, halfList - 8, botY));
        // Bouton Done (droite)
        this.buttonList.add(new GuiMenuButton(BTN_DONE,    halfList + 10,          botY, halfList - 2,  20, I18n.format("gui.done", new Object[0]), true));

        // Bouton shader options (panneau droit, bas)
        this.buttonList.add(new GuiMenuButton(BTN_SHADER_OPTIONS, ox, this.height - FOOTER_H - OPT_BTN_H - 4,
                OPT_BTN_W, OPT_BTN_H, Lang.get("of.options.shaders.shaderOptions")));

        this.updateButtons();
    }

    public void updateButtons()
    {
        boolean flag = Config.isShaders();

        for (GuiButton guibutton : this.buttonList)
        {
            if (guibutton.id != BTN_FOLDER && guibutton.id != BTN_DONE && guibutton.id != BTN_DOWNLOAD
                    && guibutton.id != EnumShaderOption.ANTIALIASING.ordinal())
            {
                guibutton.enabled = flag;
            }
        }
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        this.shaderList.handleMouseInput();
    }

    protected void actionPerformed(GuiButton button)
    {
        this.actionPerformed(button, false);
    }

    protected void actionPerformedRightClick(GuiButton button)
    {
        this.actionPerformed(button, true);
    }

    private void actionPerformed(GuiButton button, boolean rightClick)
    {
        if (button.enabled)
        {
            if (!(button instanceof GuiButtonEnumShaderOption))
            {
                if (!rightClick)
                {
                    switch (button.id)
                    {
                        case BTN_FOLDER:
                            switch (getOSType())
                            {
                                case 1:
                                    String s = String.format("cmd.exe /C start \"Open file\" \"%s\"", new Object[] {Shaders.shaderPacksDir.getAbsolutePath()});

                                    try
                                    {
                                        Runtime.getRuntime().exec(s);
                                        return;
                                    }
                                    catch (IOException ioexception)
                                    {
                                        ioexception.printStackTrace();
                                        break;
                                    }

                                case 2:
                                    try
                                    {
                                        Runtime.getRuntime().exec(new String[] {"/usr/bin/open", Shaders.shaderPacksDir.getAbsolutePath()});
                                        return;
                                    }
                                    catch (IOException ioexception1)
                                    {
                                        ioexception1.printStackTrace();
                                    }
                            }

                            boolean flag = false;

                            try
                            {
                                Class oclass1 = Class.forName("java.awt.Desktop");
                                Object object1 = oclass1.getMethod("getDesktop", new Class[0]).invoke((Object)null, new Object[0]);
                                oclass1.getMethod("browse", new Class[] {URI.class}).invoke(object1, new Object[] {(new File(this.mc.mcDataDir, "shaderpacks")).toURI()});
                            }
                            catch (Throwable throwable1)
                            {
                                throwable1.printStackTrace();
                                flag = true;
                            }

                            if (flag)
                            {
                                Config.dbg("Opening via system class!");
                                Sys.openURL("file://" + Shaders.shaderPacksDir.getAbsolutePath());
                            }

                            break;

                        case BTN_DONE:
                            Shaders.storeConfig();
                            this.saved = true;
                            this.mc.displayGuiScreen(this.parentGui);
                            break;

                        case BTN_SHADER_OPTIONS:
                            GuiShaderOptions guishaderoptions = new GuiShaderOptions(this, Config.getGameSettings());
                            Config.getMinecraft().displayGuiScreen(guishaderoptions);
                            break;

                        case BTN_DOWNLOAD:
                            try
                            {
                                Class<?> oclass = Class.forName("java.awt.Desktop");
                                Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object)null, new Object[0]);
                                oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, new Object[] {new URI("http://optifine.net/shaderPacks")});
                            }
                            catch (Throwable throwable)
                            {
                                throwable.printStackTrace();
                            }
                            break;

                        default:
                            this.shaderList.actionPerformed(button);
                    }
                }
            }
            else
            {
                GuiButtonEnumShaderOption guibuttonenumshaderoption = (GuiButtonEnumShaderOption)button;

                switch (guibuttonenumshaderoption.getEnumShaderOption())
                {
                    case ANTIALIASING:
                        Shaders.nextAntialiasingLevel(!rightClick);

                        if (this.hasShiftDown())
                        {
                            Shaders.configAntialiasingLevel = 0;
                        }

                        Shaders.uninit();
                        break;

                    case NORMAL_MAP:
                        Shaders.configNormalMap = !Shaders.configNormalMap;

                        if (this.hasShiftDown())
                        {
                            Shaders.configNormalMap = true;
                        }

                        Shaders.uninit();
                        this.mc.scheduleResourcesRefresh();
                        break;

                    case SPECULAR_MAP:
                        Shaders.configSpecularMap = !Shaders.configSpecularMap;

                        if (this.hasShiftDown())
                        {
                            Shaders.configSpecularMap = true;
                        }

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

                        if (this.hasShiftDown())
                        {
                            Shaders.configOldHandLight.resetValue();
                        }

                        Shaders.uninit();
                        break;

                    case OLD_LIGHTING:
                        Shaders.configOldLighting.nextValue(!rightClick);

                        if (this.hasShiftDown())
                        {
                            Shaders.configOldLighting.resetValue();
                        }

                        Shaders.updateBlockLightLevel();
                        Shaders.uninit();
                        this.mc.scheduleResourcesRefresh();
                        break;

                    case TWEAK_BLOCK_DAMAGE:
                        Shaders.configTweakBlockDamage = !Shaders.configTweakBlockDamage;
                        break;

                    case CLOUD_SHADOW:
                        Shaders.configCloudShadow = !Shaders.configCloudShadow;
                        break;

                    case TEX_MIN_FIL_B:
                        Shaders.configTexMinFilB = (Shaders.configTexMinFilB + 1) % 3;
                        Shaders.configTexMinFilN = Shaders.configTexMinFilS = Shaders.configTexMinFilB;
                        button.displayString = "Tex Min: " + Shaders.texMinFilDesc[Shaders.configTexMinFilB];
                        ShadersTex.updateTextureMinMagFilter();
                        break;

                    case TEX_MAG_FIL_N:
                        Shaders.configTexMagFilN = (Shaders.configTexMagFilN + 1) % 2;
                        button.displayString = "Tex_n Mag: " + Shaders.texMagFilDesc[Shaders.configTexMagFilN];
                        ShadersTex.updateTextureMinMagFilter();
                        break;

                    case TEX_MAG_FIL_S:
                        Shaders.configTexMagFilS = (Shaders.configTexMagFilS + 1) % 2;
                        button.displayString = "Tex_s Mag: " + Shaders.texMagFilDesc[Shaders.configTexMagFilS];
                        ShadersTex.updateTextureMinMagFilter();
                        break;

                    case SHADOW_CLIP_FRUSTRUM:
                        Shaders.configShadowClipFrustrum = !Shaders.configShadowClipFrustrum;
                        button.displayString = "ShadowClipFrustrum: " + toStringOnOff(Shaders.configShadowClipFrustrum);
                        ShadersTex.updateTextureMinMagFilter();
                }

                guibuttonenumshaderoption.updateButtonText();
            }
        }
    }

    public void onGuiClosed()
    {
        super.onGuiClosed();

        if (!this.saved)
        {
            Shaders.storeConfig();
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        // ── Animation ───────────────────────────────────────────────────────
        long now = Minecraft.getSystemTime();
        if (lastTime >= 0)
            animation = MathHelper.clamp_float(animation + (now - lastTime) / 1000.0f * 5.0f, 0.0f, 1.0f);
        lastTime = now;
        float e = animation * animation * (3.0f - 2.0f * animation);

        // ── Fond ─────────────────────────────────────────────────────────────
        this.drawDefaultBackground();
        Gui.drawRect(0, 0, this.width, this.height, (int)(e * 60) << 24);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - e) * 8, 0);

        int textAlpha = (int)(e * 255) << 24;

        // ── Header ───────────────────────────────────────────────────────────
        int hH = HEADER_H, hW = this.width - 8, hX = 4, hY = 2;
        GuiRenderUtils.drawShadow(hX, hY, hW, hH, 5, (int)(e * 90));
        Gui.drawRect(hX, hY, hX + hW, hY + hH, (int)(e * 210) << 24 | 0x0C0C0C);
        Gui.drawRect(hX, hY, hX + hW, hY + 1, textAlpha | (accentColor & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(hX, hY, hW, hH, (int)(e * 30) << 24 | 0xFFFFFF);

        // Titre "RED CONFLICT | Shaders"
        String t1 = "\u00a7c\u00a7lRED ", t2 = "\u00a7f\u00a7lCONFLICT", sep = " \u00a78| \u00a77";
        String t3 = this.screenTitle;
        int w1 = fr(t1), w2 = fr(t2), w3 = fr(sep);
        int tX = this.width / 2 - (w1 + w2 + w3 + fr(t3)) / 2;
        int tY = hY + (hH - 8) / 2;
        this.fontRendererObj.drawStringWithShadow(t1,  tX,           tY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(t2,  tX + w1,      tY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(sep, tX + w1 + w2, tY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(t3,  tX + w1 + w2 + w3, tY, textAlpha | 0xFFFFFF);

        // ── Panneau liste shaders ────────────────────────────────────────────
        int listPanelX = 4;
        int listPanelY = HEADER_H + 2;
        int listPanelW = listRight - listPanelX - 2;
        int listPanelH = this.height - FOOTER_H - listPanelY - 2;
        GuiRenderUtils.drawShadow(listPanelX, listPanelY, listPanelW, listPanelH, 4, (int)(e * 80));
        Gui.drawRect(listPanelX, listPanelY, listPanelX + listPanelW, listPanelY + listPanelH,
                (int)(e * 180) << 24 | 0x0A0A0A);
        GuiRenderUtils.drawRectOutline(listPanelX, listPanelY, listPanelW, listPanelH,
                (int)(e * 35) << 24 | 0xFFFFFF);

        // ── Label section liste + shader actif ──────────────────────────────
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, listPanelX + 4, listPanelY + 4,
                listPanelW - 8, "Shader Packs", accentColor);

        // Shader actif affiché en bas du panel liste (barre de statut)
        String activeName = Shaders.currentShaderName;
        if (activeName == null || activeName.equals("OFF")) activeName = Lang.get("of.options.shaders.packNone");
        else if (activeName.equals("(internal)")) activeName = Lang.get("of.options.shaders.packDefault");
        boolean shadersOn = Config.isShaders();
        int statusBarY = listPanelY + listPanelH - 16;
        Gui.drawRect(listPanelX + 1, statusBarY, listPanelX + listPanelW - 1, listPanelY + listPanelH - 1,
                (int)(e * 120) << 24 | 0x111111);
        Gui.drawRect(listPanelX + 1, statusBarY, listPanelX + listPanelW - 1, statusBarY + 1,
                (int)(e * 60) << 24 | 0xFFFFFF);
        // Badge "ON"/"OFF" pulsant
        long pulse = System.currentTimeMillis();
        float pulseA = (float)(Math.sin(pulse / 400.0) * 0.3 + 0.7);
        int badgeCol = shadersOn
                ? ((int)(e * pulseA * 255) << 24 | 0x22CC44)
                : ((int)(e * 180) << 24 | 0x555555);
        String badge = shadersOn ? "●  ON" : "○  OFF";
        this.fontRendererObj.drawStringWithShadow(badge, listPanelX + 6, statusBarY + 4, badgeCol);
        int badgeW = this.fontRendererObj.getStringWidth(badge);
        // Nom du shader tronqué
        int maxNameW = listPanelW - badgeW - 18;
        String displayName = activeName;
        while (displayName.length() > 3 && this.fontRendererObj.getStringWidth(displayName) > maxNameW)
            displayName = displayName.substring(0, displayName.length() - 1);
        if (!displayName.equals(activeName)) displayName += "…";
        this.fontRendererObj.drawStringWithShadow("§7" + displayName,
                listPanelX + badgeW + 12, statusBarY + 4, textAlpha | 0xFFFFFF);

        // ── Panneau options droite ────────────────────────────────────────────
        int optPanelW2 = OPT_BTN_W + 20;
        int optPanelH  = this.height - FOOTER_H - (HEADER_H + 2) - 2;
        GuiRenderUtils.drawShadow(optPanelX, HEADER_H + 2, optPanelW2, optPanelH, 4, (int)(e * 80));
        Gui.drawRect(optPanelX, HEADER_H + 2, optPanelX + optPanelW2, HEADER_H + 2 + optPanelH,
                (int)(e * 180) << 24 | 0x080808);
        Gui.drawRect(optPanelX, HEADER_H + 2, optPanelX + optPanelW2, HEADER_H + 3,
                (int)(e * 180) << 24 | (accentColor & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(optPanelX, HEADER_H + 2, optPanelW2, optPanelH,
                (int)(e * 35) << 24 | 0xFFFFFF);

        // Label section options + séparateur avant Shader Options
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, optPanelX + 4, HEADER_H + 6,
                optPanelW2 - 8, "Options", accentColor);
        // Trait séparateur avant le bouton Shader Options
        int sepOptY = this.height - FOOTER_H - OPT_BTN_H - 7;
        Gui.drawRect(optPanelX + 4, sepOptY, optPanelX + optPanelW2 - 4, sepOptY + 1,
                (int)(e * 40) << 24 | 0xFFFFFF);

        // ── Liste des shaders ────────────────────────────────────────────────
        this.shaderList.drawScreen(mouseX, mouseY, partialTicks);

        if (this.updateTimer <= 0)
        {
            this.shaderList.updateList();
            this.updateTimer += 20;
        }

        // ── Pied de page ─────────────────────────────────────────────────────
        int footY = this.height - FOOTER_H;
        Gui.drawRect(4, footY, this.width - 4, this.height - 2, (int)(e * 160) << 24 | 0x0A0A0A);
        Gui.drawRect(4, footY, this.width - 4, footY + 1, (int)(e * 80) << 24 | 0xFFFFFF);
        GuiRenderUtils.drawRectOutline(4, footY, this.width - 8, FOOTER_H - 2, (int)(e * 30) << 24 | 0xFFFFFF);

        // Info OpenGL : version + vendeur à gauche, renderer à droite
        String glLeft  = "§8GL " + Shaders.glVersionString + "  §7" + Shaders.glVendorString;
        String glRight = "§8" + Shaders.glRendererString;
        // Tronquer si trop long
        int maxRightW = this.width / 2 - 10;
        while (glRight.length() > 5 && this.fontRendererObj.getStringWidth(glRight) > maxRightW)
            glRight = glRight.substring(0, glRight.length() - 1);
        if (this.fontRendererObj.getStringWidth(glRight) != this.fontRendererObj.getStringWidth("§8" + Shaders.glRendererString))
            glRight += "…";
        this.fontRendererObj.drawStringWithShadow(glLeft, 8,
                footY + (FOOTER_H - 8) / 2, textAlpha | 0xFFFFFF);
        int rW = this.fontRendererObj.getStringWidth(glRight);
        this.fontRendererObj.drawStringWithShadow(glRight, this.width - rW - 8,
                footY + (FOOTER_H - 8) / 2, textAlpha | 0xFFFFFF);

        // ── Boutons + tooltips ────────────────────────────────────────────────
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.tooltipManager.drawTooltips(mouseX, mouseY, this.buttonList);

        GlStateManager.popMatrix();
    }

    private int fr(String s) { return this.fontRendererObj.getStringWidth(s); }

    public void updateScreen()
    {
        super.updateScreen();
        --this.updateTimer;
    }

    public Minecraft getMc()
    {
        return this.mc;
    }

    public void drawCenteredString(String text, int x, int y, int color)
    {
        this.drawCenteredString(this.fontRendererObj, text, x, y, color);
    }

    public static String toStringOnOff(boolean value)
    {
        String s = Lang.getOn();
        String s1 = Lang.getOff();
        return value ? s : s1;
    }

    public static String toStringAa(int value)
    {
        return value == 2 ? "FXAA 2x" : (value == 4 ? "FXAA 4x" : Lang.getOff());
    }

    public static String toStringValue(float val, float[] values, String[] names)
    {
        int i = getValueIndex(val, values);
        return names[i];
    }

    private float getNextValue(float val, float[] values, float valDef, boolean forward, boolean reset)
    {
        if (reset)
        {
            return valDef;
        }
        else
        {
            int i = getValueIndex(val, values);

            if (forward)
            {
                ++i;

                if (i >= values.length)
                {
                    i = 0;
                }
            }
            else
            {
                --i;

                if (i < 0)
                {
                    i = values.length - 1;
                }
            }

            return values[i];
        }
    }

    public static int getValueIndex(float val, float[] values)
    {
        for (int i = 0; i < values.length; ++i)
        {
            float f = values[i];

            if (f >= val)
            {
                return i;
            }
        }

        return values.length - 1;
    }

    public static String toStringQuality(float val)
    {
        return toStringValue(val, QUALITY_MULTIPLIERS, QUALITY_MULTIPLIER_NAMES);
    }

    public static String toStringHandDepth(float val)
    {
        return toStringValue(val, HAND_DEPTH_VALUES, HAND_DEPTH_NAMES);
    }

    public static int getOSType()
    {
        String s = System.getProperty("os.name").toLowerCase();
        return s.contains("win") ? 1 : (s.contains("mac") ? 2 : (s.contains("solaris") ? 3 : (s.contains("sunos") ? 3 : (s.contains("linux") ? 4 : (s.contains("unix") ? 4 : 0)))));
    }

    public boolean hasShiftDown()
    {
        return isShiftKeyDown();
    }
}
