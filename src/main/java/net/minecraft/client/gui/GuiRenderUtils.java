package net.minecraft.client.gui;

import net.minecraft.client.renderer.GlStateManager;

/**
 * GuiRenderUtils — Shared rendering helpers for Lunar Client-style GUIs.
 * MCP 1.8.9 — All procedural (no texture files).
 */
public class GuiRenderUtils {

    // ── Color Utilities ──────────────────────────────────────────────────────

    /** Linearly interpolate between two ARGB colors */
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

    /** Smoothstep easing function */
    public static float smoothStep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    /** Lerp float */
    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** Clamp integer */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ── Drawing Helpers ──────────────────────────────────────────────────────

    /** Multi-layer drop shadow behind a rectangle */
    public static void drawShadow(int x, int y, int w, int h, int layers, int baseAlpha) {
        for (int i = layers; i >= 1; i--) {
            int alpha = baseAlpha / (i + 1);
            int color = (alpha << 24) | 0x000008;
            Gui.drawRect(x + i, y + i, x + w + i, y + h + i, color);
        }
    }

    /** Panel with rounded corner simulation + shadow + gradient header */
    public static void drawRoundedPanel(int x, int y, int w, int h, int bgColor, int headerColor, int headerH, int accentColor) {
        // Shadow
        drawShadow(x, y, w, h, 4, 0x60);

        // Main background
        Gui.drawRect(x, y, x + w, y + h, bgColor);

        // Header gradient
        if (headerH > 0) {
            int headerDark = colorLerp(headerColor, bgColor, 0.6f);
            drawGradientRect(x + 1, y + 1, x + w - 1, y + headerH, headerColor, headerDark);
            // Accent line under header
            Gui.drawRect(x, y + headerH, x + w, y + headerH + 1, accentColor);
        }

        // Rounded corners (1px cut)
        Gui.drawRect(x, y, x + 1, y + 1, 0x00000000); // top-left
        Gui.drawRect(x + w - 1, y, x + w, y + 1, 0x00000000); // top-right
        Gui.drawRect(x, y + h - 1, x + 1, y + h, 0x00000000); // bottom-left
        Gui.drawRect(x + w - 1, y + h - 1, x + w, y + h, 0x00000000); // bottom-right

        // Subtle top edge highlight
        Gui.drawRect(x + 2, y, x + w - 2, y + 1, accentColor);
        // Left accent stripe
        Gui.drawRect(x, y + 1, x + 2, y + h - 1, accentColor);
        // Bottom + right subtle border
        Gui.drawRect(x + 2, y + h - 1, x + w - 2, y + h, 0xFF1A1A2A);
        Gui.drawRect(x + w - 1, y + 1, x + w, y + h - 1, 0xFF1A1A2A);
    }

    /** Gradient rect helper (wrappers for convenience) */
    public static void drawGradientRect(int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        // Use GuiScreen's static method or direct GL
        // Since this is a static utility, we use a small GL approach
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();

        float a1 = ((colorTop >> 24) & 0xFF) / 255f;
        float r1 = ((colorTop >> 16) & 0xFF) / 255f;
        float g1 = ((colorTop >> 8) & 0xFF) / 255f;
        float b1 = (colorTop & 0xFF) / 255f;
        float a2 = ((colorBottom >> 24) & 0xFF) / 255f;
        float r2 = ((colorBottom >> 16) & 0xFF) / 255f;
        float g2 = ((colorBottom >> 8) & 0xFF) / 255f;
        float b2 = (colorBottom & 0xFF) / 255f;

        net.minecraft.client.renderer.WorldRenderer wr = net.minecraft.client.renderer.Tessellator.getInstance().getWorldRenderer();
        wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x2, y1, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(x1, y1, 0).color(r1, g1, b1, a1).endVertex();
        wr.pos(x1, y2, 0).color(r2, g2, b2, a2).endVertex();
        wr.pos(x2, y2, 0).color(r2, g2, b2, a2).endVertex();
        net.minecraft.client.renderer.Tessellator.getInstance().draw();

        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    /** Draw a smooth animated toggle switch */
    public static void drawSmoothToggle(int x, int y, boolean value, float animProgress) {
        int tw = 28, th = 12;
        float smooth = smoothStep(animProgress);

        // Background color interpolation
        int offBg = 0xFF2A2A3A;
        int onBg = 0xFF1A7A4A;
        int bg = colorLerp(offBg, onBg, smooth);

        // Shadow
        Gui.drawRect(x + 1, y + 1, x + tw + 1, y + th + 1, 0x33000000);

        // Track
        Gui.drawRect(x, y, x + tw, y + th, bg);

        // Top highlight
        int topHi = colorLerp(0xFF3A3A4A, 0xFF22AA66, smooth);
        Gui.drawRect(x, y, x + tw, y + 1, topHi);
        // Bottom shadow
        Gui.drawRect(x, y + th - 1, x + tw, y + th, 0xFF111122);

        // Knob position (animated)
        float knobX = lerp(x, x + tw - th, smooth);
        int kx = (int) knobX;

        // Knob shadow
        Gui.drawRect(kx + 1, y + 1, kx + th + 1, y + th + 1, 0x44000000);
        // Knob
        Gui.drawRect(kx, y, kx + th, y + th, 0xFFEEEEEE);
        // Knob top highlight
        Gui.drawRect(kx + 1, y + 1, kx + th - 1, y + 2, 0xFFFFFFFF);
        // Knob bottom shadow
        Gui.drawRect(kx + 1, y + th - 2, kx + th - 1, y + th - 1, 0xFFBBBBBB);

        // Active indicator dot on knob
        if (smooth > 0.5f) {
            int dotAlpha = (int)(255 * (smooth - 0.5f) * 2);
            int dotColor = (dotAlpha << 24) | 0x22CC66;
            Gui.drawRect(kx + th/2 - 1, y + th/2 - 1, kx + th/2 + 1, y + th/2 + 1, dotColor);
        }
    }

    /** Hover rectangle with animated alpha */
    public static void drawHoverRect(int x, int y, int w, int h, float hoverProgress, int accentColor) {
        if (hoverProgress <= 0.01f) return;
        float smooth = smoothStep(hoverProgress);
        int baseAlpha = (int)(0x20 * smooth);
        int topAlpha = (int)(0x40 * smooth);
        int accent = accentColor & 0x00FFFFFF;
        Gui.drawRect(x, y, x + w, y + h, (baseAlpha << 24) | accent);
        Gui.drawRect(x, y, x + w, y + 1, (topAlpha << 24) | accent);
    }

    /** Draw a simple magnifying glass search icon using rects */
    public static void drawSearchIcon(int x, int y, int color) {
        // Circle (approximate with small rects)
        Gui.drawRect(x + 2, y, x + 6, y + 1, color);     // top
        Gui.drawRect(x + 2, y + 7, x + 6, y + 8, color); // bottom
        Gui.drawRect(x, y + 2, x + 1, y + 6, color);     // left
        Gui.drawRect(x + 7, y + 2, x + 8, y + 6, color); // right
        Gui.drawRect(x + 1, y + 1, x + 2, y + 2, color); // TL corner
        Gui.drawRect(x + 6, y + 1, x + 7, y + 2, color); // TR corner
        Gui.drawRect(x + 1, y + 6, x + 2, y + 7, color); // BL corner
        Gui.drawRect(x + 6, y + 6, x + 7, y + 7, color); // BR corner
        // Handle
        Gui.drawRect(x + 6, y + 7, x + 8, y + 8, color);
        Gui.drawRect(x + 7, y + 8, x + 9, y + 9, color);
        Gui.drawRect(x + 8, y + 9, x + 10, y + 10, color);
    }

    /** Draw a chevron arrow (right-pointing) for recipe transitions */
    public static void drawChevronArrow(int cx, int cy, int size, int color, float animProgress) {
        int alpha = (int)(0x80 + 0x7F * animProgress);
        int c = ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
        int half = size / 2;
        for (int i = 0; i < half; i++) {
            Gui.drawRect(cx + i, cy - half + i, cx + i + 2, cy - half + i + 1, c);
            Gui.drawRect(cx + i, cy + half - i, cx + i + 2, cy + half - i + 1, c);
        }
        // Second chevron (dimmer)
        int c2 = (((alpha / 2) & 0xFF) << 24) | (color & 0x00FFFFFF);
        for (int i = 0; i < half; i++) {
            Gui.drawRect(cx + i + half + 2, cy - half + i, cx + i + half + 4, cy - half + i + 1, c2);
            Gui.drawRect(cx + i + half + 2, cy + half - i, cx + i + half + 4, cy + half - i + 1, c2);
        }
    }

    /** Section header with colored square icon + line */
    public static void drawSectionHeader(net.minecraft.client.gui.FontRenderer fr, int px, int y, int w, String label, int accent) {
        // Colored square icon
        Gui.drawRect(px + 6, y + 3, px + 11, y + 11, accent);
        Gui.drawRect(px + 6, y + 3, px + 11, y + 4, colorLerp(accent, 0xFFFFFFFF, 0.3f)); // highlight top

        // Label
        fr.drawStringWithShadow(label, px + 15, y + 3, 0xFFDDDDEE);

        // Separator line
        int textEnd = px + 15 + fr.getStringWidth(label) + 6;
        drawGradientRect(textEnd, y + 7, px + w - 6, y + 8, accent & 0x44FFFFFF, 0x00000000);
    }

    /** Styled close button (X) */
    public static void drawCloseButton(int x, int y, int size, boolean hovered) {
        int bg = hovered ? 0xFFCC2222 : 0xBB991111;
        int topBorder = hovered ? 0xFFFF4444 : 0xFF772222;

        Gui.drawRect(x, y, x + size, y + size, bg);
        Gui.drawRect(x, y, x + size, y + 1, topBorder);
        if (hovered) {
            Gui.drawRect(x, y, x + 1, y + size, 0xFFFF4444);
        }
    }

    /** Styled button with hover effect */
    public static void drawStyledButton(int x, int y, int w, int h, int bgColor, int borderColor, boolean hovered) {
        // Shadow
        if (hovered) {
            Gui.drawRect(x + 1, y + 1, x + w + 1, y + h + 1, 0x33000000);
        }
        // Background
        Gui.drawRect(x, y, x + w, y + h, hovered ? colorLerp(bgColor, 0xFF333350, 0.5f) : bgColor);
        // Top border
        Gui.drawRect(x, y, x + w, y + 1, hovered ? borderColor : colorLerp(borderColor, bgColor, 0.5f));
        // Subtle highlight gradient
        if (hovered) {
            drawGradientRect(x + 1, y + 1, x + w - 1, y + 3, 0x22FFFFFF, 0x00FFFFFF);
        }
    }

    /** Draw a damier/checkerboard background (for transparency preview) */
    public static void drawCheckerboard(int x, int y, int w, int h, int cellSize, int light, int dark) {
        for (int cx = x; cx < x + w; cx += cellSize) {
            for (int cy = y; cy < y + h; cy += cellSize) {
                boolean isLight = (((cx - x) / cellSize + (cy - y) / cellSize) % 2 == 0);
                int ex = Math.min(cx + cellSize, x + w);
                int ey = Math.min(cy + cellSize, y + h);
                Gui.drawRect(cx, cy, ex, ey, isLight ? light : dark);
            }
        }
    }
}

