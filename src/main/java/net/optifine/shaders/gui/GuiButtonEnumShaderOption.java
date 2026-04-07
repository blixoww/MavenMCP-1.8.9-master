package net.optifine.shaders.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.config.EnumShaderOption;

public class GuiButtonEnumShaderOption extends GuiButton
{
    private EnumShaderOption enumShaderOption = null;
    private float hoverFade = 0f;
    private long  lastTime  = -1L;

    public GuiButtonEnumShaderOption(EnumShaderOption enumShaderOption, int x, int y, int widthIn, int heightIn)
    {
        super(enumShaderOption.ordinal(), x, y, widthIn, heightIn, getButtonText(enumShaderOption));
        this.enumShaderOption = enumShaderOption;
    }

    public EnumShaderOption getEnumShaderOption()
    {
        return this.enumShaderOption;
    }

    private static String getButtonText(EnumShaderOption eso)
    {
        String s = I18n.format(eso.getResourceKey(), new Object[0]) + ": ";

        switch (eso)
        {
            case ANTIALIASING:      return s + GuiShaders.toStringAa(Shaders.configAntialiasingLevel);
            case NORMAL_MAP:        return s + GuiShaders.toStringOnOff(Shaders.configNormalMap);
            case SPECULAR_MAP:      return s + GuiShaders.toStringOnOff(Shaders.configSpecularMap);
            case RENDER_RES_MUL:    return s + GuiShaders.toStringQuality(Shaders.configRenderResMul);
            case SHADOW_RES_MUL:    return s + GuiShaders.toStringQuality(Shaders.configShadowResMul);
            case HAND_DEPTH_MUL:    return s + GuiShaders.toStringHandDepth(Shaders.configHandDepthMul);
            case CLOUD_SHADOW:      return s + GuiShaders.toStringOnOff(Shaders.configCloudShadow);
            case OLD_HAND_LIGHT:    return s + Shaders.configOldHandLight.getUserValue();
            case OLD_LIGHTING:      return s + Shaders.configOldLighting.getUserValue();
            case SHADOW_CLIP_FRUSTRUM: return s + GuiShaders.toStringOnOff(Shaders.configShadowClipFrustrum);
            case TWEAK_BLOCK_DAMAGE:   return s + GuiShaders.toStringOnOff(Shaders.configTweakBlockDamage);
            default:                return s + Shaders.getEnumShaderOption(eso);
        }
    }

    public void updateButtonText()
    {
        this.displayString = getButtonText(this.enumShaderOption);
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
