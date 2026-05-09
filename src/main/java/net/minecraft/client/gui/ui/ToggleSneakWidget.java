package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;

public class ToggleSneakWidget extends BaseWidget {
    public ToggleSneakWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 80;
        this.height = 12;
        this.defaultWidth = 80;
        this.defaultHeight = 12;
    }

    @Override public boolean supportsLabelColor() { return true; }

    /**
     * Active/désactive à la fois le widget HUD ET la fonctionnalité Toggle Sneak dans GameSettings.
     * Ainsi, décocher "Active" dans l'éditeur arrête réellement le sneak automatique.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null) {
            mc.gameSettings.toggleSneakEnabled = enabled;
            if (!enabled) {
                mc.gameSettings.isToggleSneakActive = false;
            }
            try { mc.gameSettings.saveOptions(); } catch (Throwable ignored) {}
        }
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        FontRenderer fr = mc.fontRendererObj;
        GameSettings gs = mc.gameSettings;

        // Respect du prop showDisplay (feature inexistante ici — only display)
        if (Boolean.FALSE.equals(getPropOrDefault("showDisplay", Boolean.TRUE))) return;

        // isToggleSneakActive est mis à true/false par updateToggleKeys() en temps réel.
        // On n'utilise pas toggleSneakEnabled ici car il peut être désynchronisé au chargement.
        boolean sneaking = gs.toggleSneakEnabled;
        String value = sneaking ? "ON" : "OFF";
        drawLabelValue(fr, "Sneak: ", value, 0, 0);
    }
}
