package net.optifine.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMenuButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

public class GuiOtherSettingsOF extends GuiOFSettingsBase implements GuiYesNoCallback
{
    private static final GameSettings.Options[] OPTIONS = {
        GameSettings.Options.LAGOMETER,       GameSettings.Options.PROFILER,
        GameSettings.Options.SHOW_FPS,         GameSettings.Options.ADVANCED_TOOLTIPS,
        GameSettings.Options.WEATHER,          GameSettings.Options.TIME,
        GameSettings.Options.USE_FULLSCREEN,   GameSettings.Options.FULLSCREEN_MODE,
        GameSettings.Options.ANAGLYPH,         GameSettings.Options.AUTOSAVE_TICKS,
        GameSettings.Options.SCREENSHOT_SIZE,  GameSettings.Options.SHOW_GL_ERRORS,
    };

    private static final int BTN_RESET = 210;
    private final TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiOtherSettingsOF(GuiScreen guiscreen, GameSettings gamesettings) {
        this.prevScreen = guiscreen;
        this.settings   = gamesettings;
    }

    @Override
    protected GameSettings.Options[] getOptions() { return OPTIONS; }

    @Override
    protected String buildTitle() { return I18n.format("of.options.otherTitle"); }

    @Override
    protected int extraButtonRows() { return 1; }

    @Override
    protected void addExtraButtons(int doneX, int doneY, int doneW) {
        // Bouton Reset juste au-dessus du Done
        this.buttonList.add(new GuiMenuButton(BTN_RESET, doneX, doneY - BTN_H - BTN_GAP, doneW, BTN_H,
                I18n.format("of.options.other.reset")));
    }

    @Override
    protected void onActionPerformed(GuiButton btn) {
        if (btn.id == BTN_RESET) {
            this.mc.gameSettings.saveOptions();
            GuiYesNo dlg = new GuiYesNo(this, I18n.format("of.message.other.reset"), "", 9999);
            this.mc.displayGuiScreen(dlg);
        }
    }

    @Override
    public void confirmClicked(boolean flag, int id) {
        if (flag) this.mc.gameSettings.resetSettings();
        this.mc.displayGuiScreen(this);
    }

    @Override
    public void drawScreen(int x, int y, float f) {
        super.drawScreen(x, y, f);
        this.tooltipManager.drawTooltips(x, y, this.buttonList);
    }
}
