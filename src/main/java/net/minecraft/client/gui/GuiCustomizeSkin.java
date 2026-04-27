package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MathHelper;

public class GuiCustomizeSkin extends GuiScreen
{
    private static final int ACCENT = 0xFFDC1E1E;

    private final GuiScreen parentScreen;
    private String title;

    private float animation = 0f;
    private long  animLastTime = -1L;
    private int[] btnYCache;

    public GuiCustomizeSkin(GuiScreen parentScreenIn)
    {
        this.parentScreen = parentScreenIn;
    }

    public void initGui()
    {
        this.buttonList.clear();
        this.animation = 0f; this.animLastTime = -1L;
        this.title = I18n.format("options.skinCustomisation.title");

        int btnW = 150, btnH = 22, gap = 4;
        int i = 0;
        for (EnumPlayerModelParts part : EnumPlayerModelParts.values()) {
            int bx = this.width / 2 + (i % 2 == 0 ? -btnW - 5 : 5);
            int by = 50 + (i / 2) * (btnH + gap);
            this.buttonList.add(new GuiMenuButton(part.getPartId(), bx, by, btnW, btnH, getPartText(part)));
            ++i;
        }
        if (i % 2 == 1) ++i;
        int doneY = 50 + (i / 2) * (btnH + gap) + 8;
        this.buttonList.add(new GuiMenuButton(200, this.width / 2 - 75, doneY, 150, btnH, "DONE", true));
        this.btnYCache = new int[this.buttonList.size()];
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (!button.enabled) return;
        if (button.id == 200) {
            this.mc.gameSettings.saveOptions();
            this.mc.displayGuiScreen(this.parentScreen);
        } else {
            for (EnumPlayerModelParts part : EnumPlayerModelParts.values()) {
                if (part.getPartId() == button.id) {
                    this.mc.gameSettings.switchModelPartEnabled(part);
                    button.displayString = getPartText(part);
                    break;
                }
            }
        }
    }

    private String getPartText(EnumPlayerModelParts part)
    {
        boolean on = this.mc.gameSettings.getModelParts().contains(part);
        return part.func_179326_d().getFormattedText() + ": " + (on ? "§aON" : "§7OFF");
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = Minecraft.getSystemTime();
        if (animLastTime != -1L) animation = MathHelper.clamp_float(animation + (now - animLastTime) / 250f, 0f, 1f);
        animLastTime = now;
        float e = animation * animation * (3f - 2f * animation);

        this.drawDefaultBackground();

        // Bandeau discret style Chat
        GuiRenderUtils.drawGradientRect(0, 0, this.width, 38, (int)(e*160) << 24 | 0x000000, 0x00000000);
        // Fine ligne accent en haut
        Gui.drawRect(0, 0, this.width, 1, (int)(e * 255) << 24 | (ACCENT & 0xFFFFFF));

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1f - e) * 8, 0);

        int ta = (int)(e * 255) << 24;
        String t1 = "§c§lSKIN ";
        String t2 = "§f§lCUSTOMS";
        int tw = fontRendererObj.getStringWidth(t1) + fontRendererObj.getStringWidth(t2);
        int tx = this.width / 2 - tw / 2;
        fontRendererObj.drawStringWithShadow(t1, tx, 11, ta | 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(t2, tx + fontRendererObj.getStringWidth(t1), 11, ta | 0xFFFFFF);
        int dw = (int)((tw + 20) * e);
        Gui.drawRect(this.width/2 - dw/2, 23, this.width/2 + dw/2, 24, (int)(e*60) << 24 | 0xFFFFFF);

        if (btnYCache == null || btnYCache.length != this.buttonList.size()) btnYCache = new int[this.buttonList.size()];
        for (int i2 = 0; i2 < this.buttonList.size(); i2++) {
            GuiButton b = this.buttonList.get(i2);
            btnYCache[i2] = b.yPosition;
            float ba = MathHelper.clamp_float(animation * 2f - i2 * 0.08f, 0f, 1f);
            ba = ba * ba * (3f - 2f * ba);
            b.yPosition += (int)((1f - ba) * 12);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (int i2 = 0; i2 < this.buttonList.size(); i2++) this.buttonList.get(i2).yPosition = btnYCache[i2];

        GlStateManager.popMatrix();
    }
}
