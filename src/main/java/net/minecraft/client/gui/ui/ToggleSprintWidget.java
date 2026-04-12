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
        boolean toggleActive   = featureEnabled && gs.isToggleSprintActive;
        boolean actuallySprint = toggleActive && mc.thePlayer != null && mc.thePlayer.isSprinting();

        String text;
        int col;
        if (!featureEnabled) {
            text = "Sprint: OFF";
            col  = 0xFF888888;
        } else if (actuallySprint) {
            text = "Sprinting";
            col  = getColor();
        } else if (toggleActive) {
            // Toggle actif mais arrêté momentanément (va reprendre dès qu'on avance)
            text = "Sprint: \u00a7aON";
            col  = getColor();
        } else {
            text = "Sprint: \u00a77OFF";
            col  = getColor();
        }
        if ((col & 0x00FFFFFF) == 0) col = 0xFFFFFFFF;
        fr.drawStringWithShadow(text, 0, 0, col);
    }
}
