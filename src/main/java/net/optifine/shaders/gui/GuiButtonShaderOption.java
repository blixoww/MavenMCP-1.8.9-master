package net.optifine.shaders.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.optifine.shaders.config.ShaderOption;

public class GuiButtonShaderOption extends GuiButton
{
    private ShaderOption shaderOption = null;
    protected float hoverFade = 0f;
    protected long  lastTime  = -1L;

    public GuiButtonShaderOption(int buttonId, int x, int y, int widthIn, int heightIn,
            ShaderOption shaderOption, String text)
    {
        super(buttonId, x, y, widthIn, heightIn, text);
        this.shaderOption = shaderOption;
    }

    public ShaderOption getShaderOption()
    {
        return this.shaderOption;
    }

    public void valueChanged() {}

    public boolean isSwitchable()
    {
        return true;
    }

    @Override
    public void drawButton(Minecraft mc, int mx, int my)
    {
        if (!this.visible) return;

        this.hovered = mx >= xPosition && my >= yPosition
                && mx < xPosition + width && my < yPosition + height;

        long now   = Minecraft.getSystemTime();
        float dt   = (lastTime < 0L) ? 0f : (float)(now - lastTime);
        lastTime   = now;
        float step = dt / 100f;
        if (enabled) hoverFade = MathHelper.clamp_float(hoverFade + (hovered ? step : -step), 0f, 1f);
        else         hoverFade = MathHelper.clamp_float(hoverFade - step * 2f, 0f, 1f);
        float t = hoverFade * hoverFade * (3f - 2f * hoverFade);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int bgA = enabled ? (int)(25 + t * 35) : 12;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, (bgA << 24) | 0x1A1A1A);

        int border = enabled ? GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFCC2222, t) : 0x18FFFFFF;
        GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, border);

        if (t > 0f && enabled)
        {
            int shimA = (int)(20 * t);
            GuiRenderUtils.drawGradientRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + 3,
                    (shimA << 24) | 0xFFFFFF, 0x00FFFFFF);
        }

        int textCol = enabled ? GuiRenderUtils.colorLerp(0xFFAAAAAA, 0xFFFFFFFF, t) : 0xFF444444;
        this.drawCenteredString(mc.fontRendererObj, displayString,
                xPosition + width / 2, yPosition + (height - 8) / 2, textCol);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
