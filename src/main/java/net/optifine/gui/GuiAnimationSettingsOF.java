package net.optifine.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMenuButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.optifine.Lang;

public class GuiAnimationSettingsOF extends GuiOFSettingsBase
{
    private static final GameSettings.Options[] OPTIONS = {
        GameSettings.Options.ANIMATED_WATER,      GameSettings.Options.ANIMATED_LAVA,
        GameSettings.Options.ANIMATED_FIRE,        GameSettings.Options.ANIMATED_PORTAL,
        GameSettings.Options.ANIMATED_REDSTONE,    GameSettings.Options.ANIMATED_EXPLOSION,
        GameSettings.Options.ANIMATED_FLAME,       GameSettings.Options.ANIMATED_SMOKE,
        GameSettings.Options.VOID_PARTICLES,       GameSettings.Options.WATER_PARTICLES,
        GameSettings.Options.RAIN_SPLASH,          GameSettings.Options.PORTAL_PARTICLES,
        GameSettings.Options.POTION_PARTICLES,     GameSettings.Options.DRIPPING_WATER_LAVA,
        GameSettings.Options.ANIMATED_TERRAIN,     GameSettings.Options.ANIMATED_TEXTURES,
        GameSettings.Options.FIREWORK_PARTICLES,   GameSettings.Options.PARTICLES,
    };

    private static final int BTN_ALL_ON  = 210;
    private static final int BTN_ALL_OFF = 211;

    public GuiAnimationSettingsOF(GuiScreen guiscreen, GameSettings gamesettings) {
        this.prevScreen = guiscreen;
        this.settings   = gamesettings;
    }

    @Override
    protected GameSettings.Options[] getOptions() { return OPTIONS; }

    @Override
    protected String buildTitle() { return I18n.format("of.options.animationsTitle"); }

    @Override
    protected int extraButtonRows() { return 1; }

    @Override
    protected void addExtraButtons(int doneX, int doneY, int doneW) {
        // Boutons All On / All Off juste au-dessus du Done
        int halfW = (doneW - COL_GAP) / 2;
        this.buttonList.add(new GuiMenuButton(BTN_ALL_ON,  doneX,                       doneY - BTN_H - BTN_GAP, halfW, BTN_H, Lang.get("of.options.animation.allOn")));
        this.buttonList.add(new GuiMenuButton(BTN_ALL_OFF, doneX + halfW + COL_GAP,     doneY - BTN_H - BTN_GAP, halfW, BTN_H, Lang.get("of.options.animation.allOff")));
    }

    @Override
    protected void onActionPerformed(GuiButton btn) {
        if (btn.id == BTN_ALL_ON)  this.mc.gameSettings.setAllAnimations(true);
        if (btn.id == BTN_ALL_OFF) this.mc.gameSettings.setAllAnimations(false);

        ScaledResolution sr = new ScaledResolution(this.mc);
        this.setWorldAndResolution(this.mc, sr.getScaledWidth(), sr.getScaledHeight());
    }
}
