package net.minecraft.client.gui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.util.List;

/**
 * GuiRenderUtils — Shared rendering helpers for Lunar Client-style GUIs.
 * MCP 1.8.9 — Enhanced quality and ergonomics.
 */
public class GuiRenderUtils {

    // ── Color Utilities ──────────────────────────────────────────────────────

    public static int colorLerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float smoothStep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ── Drawing Helpers ──────────────────────────────────────────────────────

    public static void drawShadow(int x, int y, int w, int h, int layers, int baseAlpha) {
        for (int i = layers; i >= 1; i--) {
            int alpha = baseAlpha / (i + 1);
            int color = (alpha << 24) | 0x000008;
            Gui.drawRect(x + i, y + i, x + w + i, y + h + i, color);
        }
    }

    public static void drawRoundedPanel(int x, int y, int w, int h, int bgColor, int headerColor, int headerH, int accentColor) {
        drawShadow(x, y, w, h, 6, 0x80);

        Gui.drawRect(x, y, x + w, y + h, bgColor);

        if (headerH > 0) {
            int headerDark = colorLerp(headerColor, bgColor, 0.4f);
            drawGradientRect(x, y, x + w, y + headerH, headerColor, headerDark);
            Gui.drawRect(x, y + headerH, x + w, y + headerH + 1, accentColor);
        }

        Gui.drawRect(x, y, x + 1, y + 1, 0x00000000);
        Gui.drawRect(x + w - 1, y, x + w, y + 1, 0x00000000);
        Gui.drawRect(x, y + h - 1, x + 1, y + h, 0x00000000);
        Gui.drawRect(x + w - 1, y + h - 1, x + w, y + h, 0x00000000);

        drawGradientRect(x + 1, y, x + w - 1, y + 1, (0x66 << 24) | (accentColor & 0xFFFFFF), (0x22 << 24) | (accentColor & 0xFFFFFF));
        drawRectOutline(x, y, w, h, 0x1AFFFFFF);
    }

    public static void drawRectOutline(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + 1, color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, color);
        Gui.drawRect(x, y + 1, x + 1, y + h - 1, color);
        Gui.drawRect(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void drawGradientRect(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        float a1 = ((colorTop >> 24) & 0xFF) / 255f;
        float r1 = ((colorTop >> 16) & 0xFF) / 255f;
        float g1 = ((colorTop >> 8) & 0xFF) / 255f;
        float b1 = (colorTop & 0xFF) / 255f;
        float a2 = ((colorBottom >> 24) & 0xFF) / 255f;
        float r2 = ((colorBottom >> 16) & 0xFF) / 255f;
        float g2 = ((colorBottom >> 8) & 0xFF) / 255f;
        float b2 = (colorBottom & 0xFF) / 255f;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x2, y1, 0.0D).color(r1, g1, b1, a1).endVertex();
        worldrenderer.pos(x1, y1, 0.0D).color(r1, g1, b1, a1).endVertex();
        worldrenderer.pos(x1, y2, 0.0D).color(r2, g2, b2, a2).endVertex();
        worldrenderer.pos(x2, y2, 0.0D).color(r2, g2, b2, a2).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void drawTooltip(int x, int y, List<String> textLines) {
        if (textLines.isEmpty()) return;
        
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        
        FontRenderer font = net.minecraft.client.Minecraft.getMinecraft().fontRendererObj;
        int maxW = 0;
        for (String s : textLines) {
            int w = font.getStringWidth(s);
            if (w > maxW) maxW = w;
        }

        int tx = x + 12;
        int ty = y - 12;
        int th = 8;
        if (textLines.size() > 1) th += 2 + (textLines.size() - 1) * 10;

        int bg = 0xF0100010;
        Gui.drawRect(tx - 3, ty - 4, tx + maxW + 3, ty + th + 4, bg);
        drawRectOutline(tx - 3, ty - 4, maxW + 6, th + 8, 0x505000FF);
        
        for (int i = 0; i < textLines.size(); i++) {
            font.drawStringWithShadow(textLines.get(i), tx, ty, -1);
            ty += 10;
        }
        
        GlStateManager.enableLighting();
        GlStateManager.enableDepth();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableRescaleNormal();
    }

    public static void drawSelectionHalo(int x, int y, int w, int h, int color) {
        long time = System.currentTimeMillis();
        float wave = (float) (Math.sin(time / 250.0D) * 0.5D + 0.5D);
        int alpha = (int) (40 + wave * 60);

        for (int i = 1; i <= 3; i++) {
            int glowAlpha = alpha / (i * 2);
            drawRectOutline(x - i, y - i, w + i * 2, h + i * 2, (glowAlpha << 24) | (color & 0xFFFFFF));
        }
        
        drawRectOutline(x - 1, y - 1, w + 2, h + 2, (0xCC << 24) | 0xFFFFFF);
        drawRectOutline(x, y, w, h, (0xFF << 24) | (color & 0xFFFFFF));
        
        int cs = 4;
        Gui.drawRect(x - 1, y - 1, x - 1 + cs, y + 1, 0xFFFFFFFF);
        Gui.drawRect(x - 1, y - 1, x + 1, y - 1 + cs, 0xFFFFFFFF);
        Gui.drawRect(x + w + 1 - cs, y - 1, x + w + 1, y + 1, 0xFFFFFFFF);
        Gui.drawRect(x + w - 1, y - 1, x + w + 1, y - 1 + cs, 0xFFFFFFFF);
        Gui.drawRect(x - 1, y + h + 1 - cs, x + 1, y + h + 1, 0xFFFFFFFF);
        Gui.drawRect(x - 1, y + h - 1, x - 1 + cs, y + h + 1, 0xFFFFFFFF);
        Gui.drawRect(x + w + 1 - cs, y + h - 1, x + w + 1, y + h + 1, 0xFFFFFFFF);
        Gui.drawRect(x + w - 1, y + h + 1 - cs, x + w + 1, y + h + 1, 0xFFFFFFFF);
    }

    public static void drawSmoothToggle(int x, int y, boolean value, float animProgress) {
        int tw = 28, th = 12;
        float smooth = smoothStep(animProgress);
        int bg = colorLerp(0xFF2A2A3A, 0xFF1A7A4A, smooth);

        drawShadow(x, y, tw, th, 3, 0x40);
        Gui.drawRect(x, y, x + tw, y + th, bg);
        drawRectOutline(x, y, tw, th, 0x1AFFFFFF);

        float knobX = lerp(x + 1, x + tw - th + 1, smooth);
        int kx = (int) knobX;
        int ks = th - 2;
        Gui.drawRect(kx, y + 1, kx + ks, y + th - 1, 0xFFEEEEEE);
        Gui.drawRect(kx, y + 1, kx + ks, y + 2, 0xFFFFFFFF);
    }

    public static void drawSectionHeader(net.minecraft.client.gui.FontRenderer fr, int px, int y, int w, String label, int accent) {
        Gui.drawRect(px + 6, y + 3, px + 11, y + 11, accent);
        drawRectOutline(px + 6, y + 3, 5, 8, 0x44FFFFFF);
        fr.drawStringWithShadow(label, px + 15, y + 3, 0xFFEEEEEE);
        int textEnd = px + 15 + fr.getStringWidth(label) + 6;
        drawGradientRect(textEnd, y + 7, px + w - 6, y + 8, (0x44 << 24) | (accent & 0xFFFFFF), 0);
    }

    public static void drawCloseButton(int x, int y, int size, boolean hovered) {
        int bg = hovered ? 0xFFEE4444 : 0xAA992222;
        Gui.drawRect(x, y, x + size, y + size, bg);
        drawRectOutline(x, y, size, size, 0x33FFFFFF);
        if (hovered) drawShadow(x, y, size, size, 2, 0x40);
    }

    public static void drawStyledButton(int x, int y, int w, int h, int bgColor, int borderColor, boolean hovered) {
        int bg = hovered ? colorLerp(bgColor, 0xFF444466, 0.4f) : bgColor;
        Gui.drawRect(x, y, x + w, y + h, bg);
        drawRectOutline(x, y, w, h, hovered ? borderColor : colorLerp(borderColor, bgColor, 0.5f));
        if (hovered) drawGradientRect(x + 1, y + 1, x + w - 1, y + 3, 0x22FFFFFF, 0);
    }

    public static void drawCheckerboard(int x, int y, int w, int h, int cellSize, int light, int dark) {
        for (int cx = x; cx < x + w; cx += cellSize) {
            for (int cy = y; cy < y + h; cy += cellSize) {
                boolean isLight = (((cx - x) / cellSize + (cy - y) / cellSize) % 2 == 0);
                Gui.drawRect(cx, cy, Math.min(cx + cellSize, x + w), Math.min(cy + cellSize, y + h), isLight ? light : dark);
            }
        }
    }

    public static void drawSearchIcon(int x, int y, int color) {
        Gui.drawRect(x + 2, y, x + 6, y + 1, color);
        Gui.drawRect(x + 2, y + 7, x + 6, y + 8, color);
        Gui.drawRect(x, y + 2, x + 1, y + 6, color);
        Gui.drawRect(x + 7, y + 2, x + 8, y + 6, color);
        Gui.drawRect(x + 1, y + 1, x + 2, y + 2, color);
        Gui.drawRect(x + 6, y + 1, x + 7, y + 2, color);
        Gui.drawRect(x + 1, y + 6, x + 2, y + 7, color);
        Gui.drawRect(x + 6, y + 6, x + 7, y + 7, color);
        Gui.drawRect(x + 6, y + 7, x + 8, y + 8, color);
        Gui.drawRect(x + 7, y + 8, x + 9, y + 9, color);
        Gui.drawRect(x + 8, y + 9, x + 10, y + 10, color);
    }

    public static void drawChevronArrow(int cx, int cy, int size, int color, float animProgress) {
        int alpha = (int)(0x80 + 0x7F * animProgress);
        int c = ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
        int half = size / 2;
        for (int i = 0; i < half; i++) {
            Gui.drawRect(cx + i, cy - half + i, cx + i + 2, cy - half + i + 1, c);
            Gui.drawRect(cx + i, cy + half - i, cx + i + 2, cy + half - i + 1, c);
        }
    }
}
