package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;

public class CoordsWidget extends BaseWidget {
    public CoordsWidget(String id, int x, int y) {
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
        int px = (int) mc.thePlayer.posX;
        int py = (int) mc.thePlayer.posY;
        int pz = (int) mc.thePlayer.posZ;
        drawLabelValue(fr, "XYZ: ", px + "," + py + "," + pz, 0, 0);
    }
}
