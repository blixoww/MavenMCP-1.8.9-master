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

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        FontRenderer fr = mc.fontRendererObj;
        GameSettings gs = mc.gameSettings;

        boolean sneaking = gs.toggleSneakEnabled && gs.isToggleSneakActive;
        String value = sneaking ? "ON" : "OFF";
        drawLabelValue(fr, "Sneak: ", value, 0, 0);
    }
}
