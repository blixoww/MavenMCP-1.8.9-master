package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;

public class CoordsWidget extends BaseWidget {
    public CoordsWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 140;
        this.height = 12;
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        int px = (int) mc.thePlayer.posX;
        int py = (int) mc.thePlayer.posY;
        int pz = (int) mc.thePlayer.posZ;
        String s = "XYZ: " + px + "," + py + "," + pz;
        int col = getColor();
        if ((col & 0x00FFFFFF) == 0) col = 0x00FFFFFF;
        mc.fontRendererObj.drawStringWithShadow(s, 0, 0, col & 0x00FFFFFF);
    }
}
