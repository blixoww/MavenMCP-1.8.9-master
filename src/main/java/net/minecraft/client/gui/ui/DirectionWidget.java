package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class DirectionWidget extends BaseWidget {
    public DirectionWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 140;
        this.height = 12;
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        float yaw = mc.thePlayer.rotationYaw;
        int dirIndex = (MathHelper.floor_double((yaw * 4.0F / 360.0F) + 0.5D) & 3);
        String dir = "Sud";
        switch (dirIndex) {
            case 0:
                dir = "Sud";
                break;
            case 1:
                dir = "Ouest";
                break;
            case 2:
                dir = "Nord";
                break;
            case 3:
                dir = "Est";
                break;
        }
        int col = getColor();
        if ((col & 0x00FFFFFF) == 0) col = 0x00FFFFFF;
        mc.fontRendererObj.drawStringWithShadow("Direction: " + dir, 0, 0, col & 0x00FFFFFF);
    }
}
