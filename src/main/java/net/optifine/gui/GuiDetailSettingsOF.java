package net.optifine.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

public class GuiDetailSettingsOF extends GuiOFSettingsBase
{
    private static final GameSettings.Options[] OPTIONS = {
        GameSettings.Options.CLOUDS,          GameSettings.Options.CLOUD_HEIGHT,
        GameSettings.Options.TREES,           GameSettings.Options.RAIN,
        GameSettings.Options.SKY,             GameSettings.Options.STARS,
        GameSettings.Options.SUN_MOON,        GameSettings.Options.SHOW_CAPES,
        GameSettings.Options.FOG_FANCY,       GameSettings.Options.FOG_START,
        GameSettings.Options.TRANSLUCENT_BLOCKS, GameSettings.Options.HELD_ITEM_TOOLTIPS,
        GameSettings.Options.DROPPED_ITEMS,   GameSettings.Options.ENTITY_SHADOWS,
        GameSettings.Options.VIGNETTE,        GameSettings.Options.ALTERNATE_BLOCKS,
        GameSettings.Options.SWAMP_COLORS,    GameSettings.Options.SMOOTH_BIOMES,
    };

    private final TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiDetailSettingsOF(GuiScreen guiscreen, GameSettings gamesettings) {
        this.prevScreen = guiscreen;
        this.settings   = gamesettings;
    }

    @Override
    protected GameSettings.Options[] getOptions() { return OPTIONS; }

    @Override
    protected String buildTitle() { return I18n.format("of.options.detailsTitle"); }

    @Override
    public void drawScreen(int x, int y, float f) {
        super.drawScreen(x, y, f);
        this.tooltipManager.drawTooltips(x, y, this.buttonList);
    }
}
