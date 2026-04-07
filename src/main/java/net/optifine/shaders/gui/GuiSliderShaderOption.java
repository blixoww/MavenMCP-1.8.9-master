package net.optifine.shaders.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import net.optifine.shaders.config.ShaderOption;

public class GuiSliderShaderOption extends GuiButtonShaderOption
{
    private float sliderValue = 1.0F;
    public boolean dragging;
    private ShaderOption shaderOption = null;

    public GuiSliderShaderOption(int buttonId, int x, int y, int w, int h,
            ShaderOption shaderOption, String text)
    {
        super(buttonId, x, y, w, h, shaderOption, text);
        this.shaderOption = shaderOption;
        this.sliderValue  = shaderOption.getIndexNormalized();
        this.displayString = GuiShaderOptions.getButtonText(shaderOption, this.width);
    }

    @Override
    protected int getHoverState(boolean mouseOver)
    {
        return 0;
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
        // Math update only — no texture drawing
        if (this.visible && this.dragging && !GuiScreen.isShiftKeyDown())
        {
            this.sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
            this.sliderValue = MathHelper.clamp_float(this.sliderValue, 0.0F, 1.0F);
            this.shaderOption.setIndexNormalized(this.sliderValue);
            this.sliderValue = this.shaderOption.getIndexNormalized();
            this.displayString = GuiShaderOptions.getButtonText(this.shaderOption, this.width);
        }
    }

    @Override
    public void drawButton(Minecraft mc, int mx, int my)
    {
        if (!this.visible) return;

        this.hovered = mx >= xPosition && my >= yPosition
                && mx < xPosition + width && my < yPosition + height;

        // Update value first
        this.mouseDragged(mc, mx, my);

        // Hover animation (inherited hoverFade/lastTime from GuiButtonShaderOption
        // are private — drive our own local copy here via the parent's fields...
        // Actually the parent override handles hoverFade. We call super.drawButton
        // but that would texture-render. Instead replicate the animation inline.)
        float sv = MathHelper.clamp_float(this.sliderValue, 0f, 1f);
        int fillW = (int)(sv * (width - 8));

        // Smooth hover animation using parent's protected fields
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

        // Background
        int bgA = enabled ? (int)(25 + t * 35) : 12;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, (bgA << 24) | 0x1A1A1A);

        // Filled track
        if (enabled && fillW > 0)
        {
            Gui.drawRect(xPosition + 1, yPosition + 1,
                    xPosition + 1 + fillW, yPosition + height - 1,
                    0x60991111);
        }

        // Knob
        int knobX = xPosition + fillW;
        int knobBg = enabled ? GuiRenderUtils.colorLerp(0xFF2A2A2A, 0xFFCC2222, t) : 0xFF1E1E1E;
        Gui.drawRect(knobX, yPosition + 1, knobX + 8, yPosition + height - 1, knobBg);
        if (enabled)
            GuiRenderUtils.drawRectOutline(knobX, yPosition + 1, 8, height - 2, 0x55FFFFFF);

        // Border
        int border = enabled ? GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFCC2222, t) : 0x18FFFFFF;
        GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, border);

        // Text
        int textCol = enabled ? GuiRenderUtils.colorLerp(0xFFBBBBBB, 0xFFFFFFFF, t) : 0xFF444444;
        this.drawCenteredString(mc.fontRendererObj, displayString,
                xPosition + width / 2, yPosition + (height - 8) / 2, textCol);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        if (super.mousePressed(mc, mouseX, mouseY))
        {
            this.sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
            this.sliderValue = MathHelper.clamp_float(this.sliderValue, 0.0F, 1.0F);
            this.shaderOption.setIndexNormalized(this.sliderValue);
            this.displayString = GuiShaderOptions.getButtonText(this.shaderOption, this.width);
            this.dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY)
    {
        this.dragging = false;
    }

    @Override
    public void valueChanged()
    {
        this.sliderValue = this.shaderOption.getIndexNormalized();
    }

    @Override
    public boolean isSwitchable()
    {
        return false;
    }
}
