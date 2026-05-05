package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.GameSettings;

public class ToggleSprintWidget extends BaseWidget {
    public ToggleSprintWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 80;
        this.height = 12;
        this.defaultWidth = 80;
        this.defaultHeight = 12;
    }

    @Override public boolean supportsLabelColor() { return true; }

    /**
     * Active/désactive à la fois le widget HUD ET la fonctionnalité Toggle Sprint dans GameSettings.
     * Ainsi, décocher "Active" dans l'éditeur arrête réellement le sprint automatique.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameSettings != null) {
            mc.gameSettings.toggleSprintEnabled = enabled;
            if (!enabled) {
                mc.gameSettings.isToggleSprintActive = false;
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

        boolean toggleActive = gs.toggleSprintEnabled && gs.isToggleSprintActive;
        String value = toggleActive ? "ON" : "OFF";
        drawLabelValue(fr, "Sprint: ", value, 0, 0);
    }
}
