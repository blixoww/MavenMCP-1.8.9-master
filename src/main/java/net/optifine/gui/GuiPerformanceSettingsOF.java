package net.optifine.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

public class GuiPerformanceSettingsOF extends GuiOFSettingsBase
{
    private static final GameSettings.Options[] OPTIONS = {
        GameSettings.Options.SMOOTH_FPS,        GameSettings.Options.SMOOTH_WORLD,
        GameSettings.Options.FAST_RENDER,        GameSettings.Options.FAST_MATH,
        GameSettings.Options.CHUNK_UPDATES,      GameSettings.Options.CHUNK_UPDATES_DYNAMIC,
        GameSettings.Options.RENDER_REGIONS,     GameSettings.Options.LAZY_CHUNK_LOADING,
        GameSettings.Options.SMART_ANIMATIONS,
    };

    private final TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiPerformanceSettingsOF(GuiScreen guiscreen, GameSettings gamesettings) {
        this.prevScreen = guiscreen;
        this.settings   = gamesettings;
    }

    @Override
    protected GameSettings.Options[] getOptions() { return OPTIONS; }

    @Override
    protected String buildTitle() { return I18n.format("of.options.performanceTitle"); }

    @Override
    public void drawScreen(int x, int y, float f) {
        super.drawScreen(x, y, f);
        this.tooltipManager.drawTooltips(x, y, this.buttonList);
    }
}
