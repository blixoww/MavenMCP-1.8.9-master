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
