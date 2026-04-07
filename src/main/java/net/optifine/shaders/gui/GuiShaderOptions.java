package net.optifine.shaders.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
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
import net.optifine.gui.TooltipProviderShaderOptions;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.config.ShaderOption;
import net.optifine.shaders.config.ShaderOptionProfile;
import net.optifine.shaders.config.ShaderOptionScreen;

public class GuiShaderOptions extends GuiScreenOF
{
    private GuiScreen prevScreen;
    protected String title;
    private GameSettings settings;
    private TooltipManager tooltipManager;
    private String screenName;
    private String screenText;
    private boolean changed;
    public static final String OPTION_PROFILE = "<profile>";
    public static final String OPTION_EMPTY = "<empty>";
    public static final String OPTION_REST = "*";

    // Style animé
    private float animation = 0.0f;
    private long lastTime = -1L;
    private static final int ACCENT = new java.awt.Color(220, 30, 30).getRGB();
    private static final int HDR_H = 32;
    private static final int BTN_ROW_H = 28; // hauteur réservée en bas pour Done/Reset

    public GuiShaderOptions(GuiScreen guiscreen, GameSettings gamesettings)
    {
        this.tooltipManager = new TooltipManager(this, new TooltipProviderShaderOptions());
        this.screenName = null;
        this.screenText = null;
        this.changed = false;
        this.title = "Shader Options";
        this.prevScreen = guiscreen;
        this.settings = gamesettings;
    }

    public GuiShaderOptions(GuiScreen guiscreen, GameSettings gamesettings, String screenName)
    {
        this(guiscreen, gamesettings);
        this.screenName = screenName;
        if (screenName != null)
            this.screenText = Shaders.translate("screen." + screenName, screenName);
    }

    public void initGui()
    {
        this.title = I18n.format("of.options.shaderOptionsTitle");
        this.lastTime = -1L;
        this.animation = 0.0f;

        int btnH = 20;
        int btnW = 120;
        int k = HDR_H + 10; // startY options
        int l = 20;          // row height for options
        int k1 = Shaders.getShaderPackColumns(this.screenName, 2);
        ShaderOption[] ashaderoption = Shaders.getShaderPackOptions(this.screenName);

        if (ashaderoption != null)
        {
            int l1 = MathHelper.ceiling_double_int((double)ashaderoption.length / 9.0D);
            if (k1 < l1) k1 = l1;

            for (int i2 = 0; i2 < ashaderoption.length; ++i2)
            {
                ShaderOption shaderoption = ashaderoption[i2];
                if (shaderoption != null && shaderoption.isVisible())
                {
                    int j2 = i2 % k1;
                    int k2 = i2 / k1;
                    int l2 = Math.min(this.width / k1, 200);
                    int j = (this.width - l2 * k1) / 2;
                    int i3 = j2 * l2 + 5 + j;
                    int j3 = k + k2 * l;
                    int k3 = l2 - 10;
                    String s = getButtonText(shaderoption, k3);
                    GuiButtonShaderOption guibuttonshaderoption;
                    if (Shaders.isShaderPackOptionSlider(shaderoption.getName()))
                        guibuttonshaderoption = new GuiSliderShaderOption(100 + i2, i3, j3, k3, btnH, shaderoption, s);
                    else
                        guibuttonshaderoption = new GuiButtonShaderOption(100 + i2, i3, j3, k3, btnH, shaderoption, s);
                    guibuttonshaderoption.enabled = shaderoption.isEnabled();
                    this.buttonList.add(guibuttonshaderoption);
                }
            }
        }

        // Boutons Reset et Done centrés en bas, dans la zone BTN_ROW_H
        int botY = this.height - BTN_ROW_H + 4;
        int cx   = this.width / 2;
        this.buttonList.add(new GuiButton(201, cx - btnW - 5,  botY, btnW, btnH, I18n.format("controls.reset")));
        this.buttonList.add(new GuiButton(200, cx + 5,         botY, btnW, btnH, I18n.format("gui.done")));
    }

    public static String getButtonText(ShaderOption so, int btnWidth)
    {
        String s = so.getNameText();

        if (so instanceof ShaderOptionScreen)
        {
            ShaderOptionScreen shaderoptionscreen = (ShaderOptionScreen)so;
            return s + "...";
        }
        else
        {
            FontRenderer fontrenderer = Config.getMinecraft().fontRendererObj;

            for (int i = fontrenderer.getStringWidth(": " + Lang.getOff()) + 5; fontrenderer.getStringWidth(s) + i >= btnWidth && s.length() > 0; s = s.substring(0, s.length() - 1))
            {
                ;
            }

            String s1 = so.isChanged() ? so.getValueColor(so.getValue()) : "";
            String s2 = so.getValueText(so.getValue());
            return s + ": " + s1 + s2;
        }
    }

    protected void actionPerformed(GuiButton guibutton)
    {
        if (guibutton.enabled)
        {
            if (guibutton.id < 200 && guibutton instanceof GuiButtonShaderOption)
            {
                GuiButtonShaderOption guibuttonshaderoption = (GuiButtonShaderOption)guibutton;
                ShaderOption shaderoption = guibuttonshaderoption.getShaderOption();

                if (shaderoption instanceof ShaderOptionScreen)
                {
                    String s = shaderoption.getName();
                    GuiShaderOptions guishaderoptions = new GuiShaderOptions(this, this.settings, s);
                    this.mc.displayGuiScreen(guishaderoptions);
                    return;
                }

                if (isShiftKeyDown())
                {
                    shaderoption.resetValue();
                }
                else if (guibuttonshaderoption.isSwitchable())
                {
                    shaderoption.nextValue();
                }

                this.updateAllButtons();
                this.changed = true;
            }

            if (guibutton.id == 201)
            {
                ShaderOption[] ashaderoption = Shaders.getChangedOptions(Shaders.getShaderPackOptions());

                for (int i = 0; i < ashaderoption.length; ++i)
                {
                    ShaderOption shaderoption1 = ashaderoption[i];
                    shaderoption1.resetValue();
                    this.changed = true;
                }

                this.updateAllButtons();
            }

            if (guibutton.id == 200)
            {
                if (this.changed)
                {
                    Shaders.saveShaderPackOptions();
                    this.changed = false;
                    Shaders.uninit();
                }

                this.mc.displayGuiScreen(this.prevScreen);
            }
        }
    }

    protected void actionPerformedRightClick(GuiButton btn)
    {
        if (btn instanceof GuiButtonShaderOption)
        {
            GuiButtonShaderOption guibuttonshaderoption = (GuiButtonShaderOption)btn;
            ShaderOption shaderoption = guibuttonshaderoption.getShaderOption();

            if (isShiftKeyDown())
            {
                shaderoption.resetValue();
            }
            else if (guibuttonshaderoption.isSwitchable())
            {
                shaderoption.prevValue();
            }

            this.updateAllButtons();
            this.changed = true;
        }
    }

    public void onGuiClosed()
    {
        super.onGuiClosed();

        if (this.changed)
        {
            Shaders.saveShaderPackOptions();
            this.changed = false;
            Shaders.uninit();
        }
    }

    private void updateAllButtons()
    {
        for (GuiButton guibutton : this.buttonList)
        {
            if (guibutton instanceof GuiButtonShaderOption)
            {
                GuiButtonShaderOption guibuttonshaderoption = (GuiButtonShaderOption)guibutton;
                ShaderOption shaderoption = guibuttonshaderoption.getShaderOption();

                if (shaderoption instanceof ShaderOptionProfile)
                {
                    ShaderOptionProfile shaderoptionprofile = (ShaderOptionProfile)shaderoption;
                    shaderoptionprofile.updateProfile();
                }

                guibuttonshaderoption.displayString = getButtonText(shaderoption, guibuttonshaderoption.getButtonWidth());
                guibuttonshaderoption.valueChanged();
            }
        }
    }

    public void drawScreen(int x, int y, float f)
    {
        // Animation
        long now = net.minecraft.client.Minecraft.getSystemTime();
        if (lastTime >= 0)
            animation = MathHelper.clamp_float(animation + (now - lastTime) / 1000.0f * 5.0f, 0.0f, 1.0f);
        lastTime = now;
        float e = animation * animation * (3.0f - 2.0f * animation);

        // Fond : gradient monde derrière + léger overlay sombre
        this.drawDefaultBackground();
        Gui.drawRect(0, 0, this.width, this.height, (int)(e * 140) << 24 | 0x050505);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - e) * 8, 0);

        int ta = (int)(e * 255) << 24;

        // Header bar
        Gui.drawRect(0, 0, this.width, HDR_H, (int)(e * 220) << 24 | 0x0C0C0C);
        Gui.drawRect(0, 0, this.width, 1, ta | (ACCENT & 0xFFFFFF));
        Gui.drawRect(0, HDR_H - 1, this.width, HDR_H, (int)(e * 40) << 24 | 0xFFFFFF);

        // Titre centré dans le header
        String displayTitle = (this.screenText != null) ? this.screenText : this.title;
        int tw = this.fontRendererObj.getStringWidth(displayTitle);
        this.fontRendererObj.drawStringWithShadow(displayTitle,
                this.width / 2 - tw / 2, (HDR_H - 8) / 2, ta | 0xFFFFFF);

        // Footer bar pour les boutons Done/Reset
        int footY = this.height - BTN_ROW_H;
        Gui.drawRect(0, footY, this.width, this.height, (int)(e * 180) << 24 | 0x0A0A0A);
        Gui.drawRect(0, footY, this.width, footY + 1, (int)(e * 50) << 24 | 0xFFFFFF);

        super.drawScreen(x, y, f);
        GlStateManager.popMatrix();

        this.tooltipManager.drawTooltips(x, y, this.buttonList);
    }
}
