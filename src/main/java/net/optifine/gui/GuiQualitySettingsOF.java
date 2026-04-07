package net.optifine.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

public class GuiQualitySettingsOF extends GuiOFSettingsBase
{
    private static final GameSettings.Options[] OPTIONS = {
        GameSettings.Options.MIPMAP_LEVELS,        GameSettings.Options.MIPMAP_TYPE,
        GameSettings.Options.AF_LEVEL,             GameSettings.Options.AA_LEVEL,
        GameSettings.Options.CLEAR_WATER,          GameSettings.Options.RANDOM_ENTITIES,
        GameSettings.Options.BETTER_GRASS,         GameSettings.Options.BETTER_SNOW,
        GameSettings.Options.CUSTOM_FONTS,         GameSettings.Options.CUSTOM_COLORS,
        GameSettings.Options.CONNECTED_TEXTURES,   GameSettings.Options.NATURAL_TEXTURES,
        GameSettings.Options.CUSTOM_SKY,           GameSettings.Options.CUSTOM_ITEMS,
        GameSettings.Options.CUSTOM_ENTITY_MODELS, GameSettings.Options.CUSTOM_GUIS,
        GameSettings.Options.EMISSIVE_TEXTURES,
    };

    private final TooltipManager tooltipManager = new TooltipManager(this, new TooltipProviderOptions());

    public GuiQualitySettingsOF(GuiScreen guiscreen, GameSettings gamesettings) {
        this.prevScreen = guiscreen;
        this.settings   = gamesettings;
    }

    @Override
    protected GameSettings.Options[] getOptions() { return OPTIONS; }

    @Override
    protected String buildTitle() { return I18n.format("of.options.qualityTitle"); }

    @Override
    protected void onActionPerformed(GuiButton btn) {
        if (btn.id != GameSettings.Options.AA_LEVEL.ordinal()) {
            ScaledResolution sr = new ScaledResolution(this.mc);
            this.setWorldAndResolution(this.mc, sr.getScaledWidth(), sr.getScaledHeight());
        }
    }

    @Override
    public void drawScreen(int x, int y, float f) {
        super.drawScreen(x, y, f);
        this.tooltipManager.drawTooltips(x, y, this.buttonList);
    }
}
