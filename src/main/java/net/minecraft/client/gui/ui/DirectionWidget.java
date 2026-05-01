package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public class DirectionWidget extends BaseWidget {
    public DirectionWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 140;
        this.height = 12;
    }

    @Override public boolean supportsLabelColor() { return true; }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;
        float yaw = mc.thePlayer.rotationYaw;
        int dirIndex = (MathHelper.floor_double((yaw * 4.0F / 360.0F) + 0.5D) & 3);
        String dir;
        switch (dirIndex) {
            case 1:  dir = "Ouest"; break;
            case 2:  dir = "Nord";  break;
            case 3:  dir = "Est";   break;
            default: dir = "Sud";   break;
        }
        drawLabelValue(fr, "Direction: ", dir, 0, 0);
    }
}
