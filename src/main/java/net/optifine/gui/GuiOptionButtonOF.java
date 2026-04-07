package net.optifine.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;

public class GuiOptionButtonOF extends GuiOptionButton implements IOptionControl
{
    private GameSettings.Options option = null;
    private float hoverFade = 0f;
    private long lastTime   = -1L;

    public GuiOptionButtonOF(int id, int x, int y, GameSettings.Options option, String text)
    {
        super(id, x, y, option, text);
        this.option = option;
    }

    public GameSettings.Options getOption()
    {
        return this.option;
    }

    @Override
    public void drawButton(Minecraft mc, int mx, int my)
    {
        if (!this.visible) return;

        this.hovered = mx >= xPosition && my >= yPosition
                && mx < xPosition + width && my < yPosition + height;

        long now  = Minecraft.getSystemTime();
        float dt  = (lastTime < 0L) ? 0f : (float)(now - lastTime);
        lastTime  = now;
        float step = dt / 100f;
        if (enabled) hoverFade = MathHelper.clamp_float(hoverFade + (hovered ? step : -step), 0f, 1f);
        else         hoverFade = MathHelper.clamp_float(hoverFade - step * 2f, 0f, 1f);
        float t = hoverFade * hoverFade * (3f - 2f * hoverFade);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Background
        int bgA = enabled ? (int)(25 + t * 35) : 12;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, (bgA << 24) | 0x1A1A1A);

        // Border: grey → red on hover
        int border = enabled ? GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFCC2222, t) : 0x18FFFFFF;
        GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, border);

        // Top shimmer on hover
        if (t > 0f && enabled)
        {
            int shimA = (int)(20 * t);
            GuiRenderUtils.drawGradientRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + 3,
                    (shimA << 24) | 0xFFFFFF, 0x00FFFFFF);
        }

        // Text
        int textCol = enabled ? GuiRenderUtils.colorLerp(0xFFAAAAAA, 0xFFFFFFFF, t) : 0xFF444444;
        this.drawCenteredString(mc.fontRendererObj, displayString,
                xPosition + width / 2, yPosition + (height - 8) / 2, textCol);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
