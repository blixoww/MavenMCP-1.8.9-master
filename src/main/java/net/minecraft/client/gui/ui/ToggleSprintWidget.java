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

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        FontRenderer fr = mc.fontRendererObj;
        GameSettings gs = mc.gameSettings;

        boolean featureEnabled = gs.toggleSprintEnabled;
        boolean sprinting = featureEnabled && gs.isToggleSprintActive;

        String text = sprinting ? "Sprinting" : (featureEnabled ? "Sprint: ON" : "Sprint: OFF");
        int col = featureEnabled ? getColor() : 0xFF888888;
        if ((col & 0x00FFFFFF) == 0) col = 0xFFFFFFFF;
        fr.drawStringWithShadow(text, 0, 0, col);
    }
}
