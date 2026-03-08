package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

public class BiomeWidget extends BaseWidget
{
    public BiomeWidget(String id, int x, int y)
    {
        super(id, x, y);
        this.width = 120; this.height = 12;
    }

    @Override
    protected void draw()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;
        FontRenderer fr = mc.fontRendererObj;
        String biome = mc.theWorld.getBiomeGenForCoords(mc.thePlayer.getPosition()).biomeName;
        String s = "Biome: " + biome;
        int col = getColor(); if ((col & 0x00FFFFFF) == 0) col = 0x00FFFFFF;
        fr.drawStringWithShadow(s, x, y, col & 0x00FFFFFF);
     }
 }
