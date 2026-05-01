package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class BiomeWidget extends BaseWidget {
    public BiomeWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 120;
        this.height = 12;
    }

    @Override public boolean supportsLabelColor() { return true; }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;
        String biome = mc.theWorld.getBiomeGenForCoords(mc.thePlayer.getPosition()).biomeName;
        int col = getColor();
        if ((col & 0x00FFFFFF) == 0) col |= 0x00FFFFFF;
        drawLabelValue(fr, "Biome: ", biome, 0, 0);
    }
}
