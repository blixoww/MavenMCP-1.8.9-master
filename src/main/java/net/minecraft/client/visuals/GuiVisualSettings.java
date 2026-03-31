package net.minecraft.client.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * GUI complète des paramètres visuels.
 */
public class GuiVisualSettings extends GuiScreen {

    private final GuiScreen parent;
    private VisualSettings settings;
    private int selectedCategory = 0;

    private static final String[] CATEGORIES = {"Combo", "Hit Marker", "Particules", "Coeurs"};
    private static final int ACCENT = 0xFFCC2222;
    private static final int BG_DARK = 0xF0101018;
    private static final int BG_HEADER = 0xFF1A1A24;
    private static final int BORDER = 0x30FFFFFF;

    private static final int[] PRESET_COLORS = {
        0xFFFFFFFF, 0xFFFF3333, 0xFFFF7F33, 0xFFFFFF33, 0xFF33FF33, 0xFF33FFFF, 0xFF3333FF, 0xFF8833FF, 0xFFFF33FF, 0xFF888888, 0xFF111111
    };

    // Layout constant pour les sliders
    private static final int SW = 100; // Slider Width
    private static final int TW = 40;  // Text Width
    private static final int GAP = 10; // Gap between label and control

    private int panelX, panelY, panelW, panelH;
    private int catW, settingsX, settingsW;

    private final float[] catHover = new float[4];
    private float animation = 0.0f;
    private long lastTime = -1L;

    private int scrollOffset = 0;
    private int maxScroll = 0;

    private long previewHitTime;
    private int previewComboCount;

    private int draggingSlider = -1;

    public GuiVisualSettings(GuiScreen parent) {
        this.parent = parent;
        this.settings = VisualManager.getInstance().getSettings();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();
        this.scrollOffset = 0;

        panelW = Math.min(400, this.width - 20);
        panelH = Math.min(280, this.height - 20);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        catW = Math.max(70, panelW / 5);
        settingsX = panelX + catW + 1;
        settingsW = panelW - catW - 1;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime > 0) {
            float dt = (now - lastTime) / 1000.0f;
            animation = MathHelper.clamp_float(animation + dt * 5.0f, 0.0f, 1.0f);
        }
        lastTime = now;

        float ease = animation * animation * (3.0f - 2.0f * animation);
        this.drawRect(0, 0, this.width, this.height, (int)(ease * 180) << 24);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - ease) * 8, 0);

        GuiRenderUtils.drawShadow(panelX, panelY, panelW, panelH, 10, (int)(ease * 120));
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_DARK);
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 1, ACCENT);
        Gui.drawRect(panelX, panelY + 1, panelX + panelW, panelY + 18, BG_HEADER);

        String title = "\u00A7c\u00A7lPARAMETRES \u00A7f\u00A7lVISUELS";
        fontRendererObj.drawStringWithShadow(title, panelX + panelW / 2 - fontRendererObj.getStringWidth(title) / 2, panelY + 5, 0xFFFFFFFF);

        String resetText = "\u00A77[Reset]";
        int rtw = fontRendererObj.getStringWidth(resetText);
        int rtx = panelX + panelW - rtw - 4;
        boolean resetHover = mouseX >= rtx && mouseX < rtx + rtw && mouseY >= panelY + 3 && mouseY < panelY + 15;
        fontRendererObj.drawStringWithShadow(resetHover ? "\u00A7c[Reset]" : resetText, rtx, panelY + 5, 0xFFFFFFFF);

        GuiRenderUtils.drawRectOutline(panelX, panelY, panelW, panelH, BORDER);

        drawCategories(mouseX, mouseY);
        Gui.drawRect(panelX + catW, panelY + 18, panelX + catW + 1, panelY + panelH, 0x20FFFFFF);
        
        int previewH = Math.min(60, panelH / 4);
        int clipTop = panelY + 20;
        int clipBottom = panelY + panelH - previewH - 2;
        
        ScaledResolution sr = new ScaledResolution(mc);
        int factor = sr.getScaleFactor();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(settingsX * factor, (this.height - clipBottom) * factor, settingsW * factor, (clipBottom - clipTop) * factor);
        
        drawSettings(mouseX, mouseY);
        
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        
        drawPreview(mouseX, mouseY, partialTicks);

        GlStateManager.popMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCategories(int mx, int my) {
        int y = panelY + 20;
        int h = Math.min(22, (panelH - 22) / CATEGORIES.length);
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean hovered = mx >= panelX && mx < panelX + catW && my >= y && my < y + h;
            boolean selected = (i == selectedCategory);
            float target = (hovered || selected) ? 1.0f : 0.0f;
            catHover[i] += (target - catHover[i]) * 0.2f;
            float t = catHover[i];
            Gui.drawRect(panelX, y, panelX + catW, y + h, GuiRenderUtils.colorLerp(0x00000000, 0x30FFFFFF, t));
            if (selected) Gui.drawRect(panelX, y, panelX + 2, y + h, ACCENT);
            int textColor = selected ? 0xFFFFFFFF : GuiRenderUtils.colorLerp(0xFF888888, 0xFFFFFFFF, t);
            fontRendererObj.drawStringWithShadow(CATEGORIES[i], panelX + 6, y + (h - 8) / 2, textColor);
            boolean enabled = isModuleEnabled(i);
            Gui.drawRect(panelX + catW - 7, y + h / 2 - 2, panelX + catW - 3, y + h / 2 + 2, enabled ? 0xFF44CC44 : 0xFF663333);
            y += h;
        }
    }

    private void drawSettings(int mx, int my) {
        int x = settingsX + 6;
        int y = panelY + 22;
        int w = settingsW - 12;
        int previewH = Math.min(60, panelH / 4);
        int availH = panelH - 22 - previewH - 4;
        int currentY = y - scrollOffset;

        switch (selectedCategory) {
            case 0: currentY = drawComboSettings(x, currentY, w, mx, my, y, y + availH); break;
            case 1: currentY = drawHitMarkerSettings(x, currentY, w, mx, my, y, y + availH); break;
            case 2: currentY = drawParticleSettings(x, currentY, w, mx, my, y, y + availH); break;
            case 3: currentY = drawHeartSettings(x, currentY, w, mx, my, y, y + availH); break;
        }
        maxScroll = Math.max(0, (currentY + scrollOffset) - (y + availH));
    }

    private int drawComboSettings(int x, int y, int w, int mx, int my, int ct, int cb) {
        y = drawToggle(x, y, w, "Activ\u00e9", settings.comboEnabled, mx, my, ct, cb);
        y = drawToggle(x, y, w, "Afficher \"Combo\"", settings.comboShowLabel, mx, my, ct, cb);
        y = drawSlider(x, y, w, "Taille", settings.comboSize, 0.3f, 3.0f, mx, my, ct, cb);
        y = drawSlider(x, y, w, "Offset X", settings.comboPosX, -1.0f, 1.0f, mx, my, ct, cb);
        y = drawSlider(x, y, w, "Offset Y", settings.comboPosY, -1.0f, 1.0f, mx, my, ct, cb);
        y = drawSlider(x, y, w, "D\u00e9lai reset", settings.comboResetDelayMs / 1000.0f, 0.5f, 5.0f, mx, my, ct, cb);
        y = drawToggle(x, y, w, "Anim. \u00e9chelle", settings.comboAnimScale, mx, my, ct, cb);
        y = drawToggle(x, y, w, "Anim. fondu", settings.comboAnimFade, mx, my, ct, cb);
        y = drawColorSelector(x, y, w, "Couleur", settings.comboColor, mx, my, ct, cb, 0);
        y = drawSlider(x, y, w, "Seuil 1", settings.comboThreshold1, 2, 30, mx, my, ct, cb);
        y = drawColorSelector(x, y, w, "Couleur S1", settings.comboColor1, mx, my, ct, cb, 1);
        y = drawSlider(x, y, w, "Seuil 2", settings.comboThreshold2, 2, 50, mx, my, ct, cb);
        y = drawColorSelector(x, y, w, "Couleur S2", settings.comboColor2, mx, my, ct, cb, 2);
        y = drawSlider(x, y, w, "Seuil 3", settings.comboThreshold3, 2, 100, mx, my, ct, cb);
        y = drawColorSelector(x, y, w, "Couleur S3", settings.comboColor3, mx, my, ct, cb, 3);
        return y;
    }

    private int drawHitMarkerSettings(int x, int y, int w, int mx, int my, int ct, int cb) {
        y = drawToggle(x, y, w, "Activ\u00e9", settings.hitMarkerEnabled, mx, my, ct, cb);
        y = drawSlider(x, y, w, "Taille", settings.hitMarkerSize, 2.0f, 20.0f, mx, my, ct, cb);
        y = drawSlider(x, y, w, "Opacit\u00e9", settings.hitMarkerOpacity, 0.1f, 1.0f, mx, my, ct, cb);
        y = drawSlider(x, y, w, "Dur\u00e9e", settings.hitMarkerDurationMs / 1000.0f, 0.05f, 1.0f, mx, my, ct, cb);
        y = drawToggle(x, y, w, "Fondu", settings.hitMarkerFade, mx, my, ct, cb);
        y = drawColorSelector(x, y, w, "Couleur", settings.hitMarkerColor, mx, my, ct, cb, 4);
        return y;
    }

    private int drawParticleSettings(int x, int y, int w, int mx, int my, int ct, int cb) {
        y = drawToggle(x, y, w, "Activ\u00e9", settings.particlesEnabled, mx, my, ct, cb);
        y = drawEnum(x, y, w, "D\u00e9clencheur", new String[]{"Hit", "Kill", "Les deux"}, settings.particleTrigger, mx, my, ct, cb);
        y = drawEnum(x, y, w, "Type", new String[]{"Redstone", "Potion", "Feu d'artifice"}, settings.particleType, mx, my, ct, cb);
        y = drawSlider(x, y, w, "Quantit\u00e9", settings.particleQuantity, 1, 30, mx, my, ct, cb);
        y = drawSlider(x, y, w, "Taille", settings.particleSize, 0.2f, 3.0f, mx, my, ct, cb);
        y = drawEnum(x, y, w, "Filtre", new String[]{"Joueurs", "Mobs", "Tous"}, settings.particleFilter, mx, my, ct, cb);
        y = drawColorSelector(x, y, w, "Couleur 1", settings.particleColor1, mx, my, ct, cb, 5);
        y = drawColorSelector(x, y, w, "Couleur 2", settings.particleColor2, mx, my, ct, cb, 6);
        y = drawColorSelector(x, y, w, "Couleur 3", settings.particleColor3, mx, my, ct, cb, 7);
        return y;
    }

    private int drawHeartSettings(int x, int y, int w, int mx, int my, int ct, int cb) {
        y = drawToggle(x, y, w, "Activ\u00e9", settings.heartsEnabled, mx, my, ct, cb);
        y = drawToggle(x, y, w, "Afficher d\u00e9g\u00e2ts \u007C Vie Restante", settings.heartsShowDamage, mx, my, ct, cb);
        y = drawEnum(x, y, w, "Filtre", new String[]{"Joueurs", "Mobs", "Tous"}, settings.heartsFilter, mx, my, ct, cb);
        y = drawColorSelector(x, y, w, "Couleur", settings.heartsColor, mx, my, ct, cb, 8);
        return y;
    }

    private int drawToggle(int x, int y, int w, String label, boolean value, int mx, int my, int ct, int cb) {
        if (y + 16 < ct || y > cb) return y + 16;
        fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xFFCCCCCC);
        int tw = 22, th = 10;
        int tx = x + w - tw;
        int ty = y + 2;
        int bg = value ? 0xFF1A7A4A : 0xFF2A2A3A;
        Gui.drawRect(tx, ty, tx + tw, ty + th, bg);
        GuiRenderUtils.drawRectOutline(tx, ty, tw, th, 0x1AFFFFFF);
        int knobX = value ? tx + tw - th + 1 : tx + 1;
        Gui.drawRect(knobX, ty + 1, knobX + th - 2, ty + th - 1, 0xFFEEEEEE);
        return y + 16;
    }

    private int drawSlider(int x, int y, int w, String label, float value, float min, float max, int mx, int my, int ct, int cb) {
        if (y + 16 < ct || y > cb) return y + 16;
        fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xFFCCCCCC);
        
        String valStr = (max > 10 && min >= 1) ? String.valueOf((int) value) : String.format("%.2f", value);
        
        int sx = x + w - SW - TW - GAP;
        int sy = y + 7;
        
        // Texte fixe à droite
        int tx = x + w - TW;
        fontRendererObj.drawStringWithShadow(valStr, tx + (TW - fontRendererObj.getStringWidth(valStr)), y + 3, 0xFFFF8888);
        
        // Barre fixe
        Gui.drawRect(sx, sy, sx + SW, sy + 3, 0xFF2A2A3A);
        float ratio = MathHelper.clamp_float((value - min) / (max - min), 0, 1);
        int filledW = (int)(SW * ratio);
        Gui.drawRect(sx, sy, sx + filledW, sy + 3, ACCENT);
        Gui.drawRect(sx + filledW - 2, sy - 1, sx + filledW + 2, sy + 4, 0xFFEEEEEE);
        return y + 16;
    }

    private int drawEnum(int x, int y, int w, String label, String[] options, int current, int mx, int my, int ct, int cb) {
        if (y + 16 < ct || y > cb) return y + 16;
        fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xFFCCCCCC);
        String display = "< " + options[current] + " >";
        int dw = fontRendererObj.getStringWidth(display);
        int dx = x + w - dw;
        boolean hovered = mx >= dx && mx < dx + dw && my >= y && my < y + 14;
        fontRendererObj.drawStringWithShadow(display, dx, y + 3, hovered ? 0xFFFFFFFF : 0xFFFF8888);
        return y + 16;
    }

    private int drawColorSelector(int x, int y, int w, String label, int color, int mx, int my, int ct, int cb, int id) {
        if (y + 16 < ct || y > cb) return y + 16;
        fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xFFCCCCCC);
        int sz = 10;
        int cx = x + w - sz - 15;
        int cy = y + 2;
        String left = "<";
        String right = ">";
        int lw = fontRendererObj.getStringWidth(left);
        int rw = fontRendererObj.getStringWidth(right);
        int lx = cx - 14;
        int rx = cx + sz + 4;
        boolean hL = mx >= lx && mx < lx + 10 && my >= y && my < y + 14;
        boolean hR = mx >= rx && mx < rx + 10 && my >= y && my < y + 14;
        fontRendererObj.drawStringWithShadow(left, lx, y + 3, hL ? 0xFFFFFFFF : 0xFFAAAAAA);
        fontRendererObj.drawStringWithShadow(right, rx, y + 3, hR ? 0xFFFFFFFF : 0xFFAAAAAA);
        Gui.drawRect(cx, cy, cx + sz, cy + sz, color | 0xFF000000);
        GuiRenderUtils.drawRectOutline(cx, cy, sz, sz, 0x44FFFFFF);
        return y + 16;
    }

    private void drawPreview(int mx, int my, float partialTicks) {
        int previewH = Math.min(60, panelH / 4);
        int py = panelY + panelH - previewH;
        int px = settingsX;
        int pw = settingsW;

        Gui.drawRect(px + 4, py, px + pw - 4, py + 1, 0x20FFFFFF);
        fontRendererObj.drawStringWithShadow("\u00A77Aper\u00e7u", px + 6, py + 3, 0xFFAAAAAA);

        String simText = "[Tester]";
        int stw = fontRendererObj.getStringWidth(simText);
        int stx = px + pw - stw - 6;
        boolean simHover = mx >= stx && mx < stx + stw && my >= py + 2 && my < py + 14;
        fontRendererObj.drawStringWithShadow(simText, stx, py + 3, simHover ? 0xFFFFFFFF : ACCENT);

        int areaY = py + 14;
        int areaH = previewH - 16;
        Gui.drawRect(px + 4, areaY, px + pw - 4, areaY + areaH, 0x20000000);

        long now = System.currentTimeMillis();
        long elapsed = now - previewHitTime;

        if (elapsed > settings.comboResetDelayMs && previewComboCount > 0) {
            previewComboCount = 0;
        }

        switch (selectedCategory) {
            case 0: drawComboPreview(px, pw, areaY, areaH, elapsed); break;
            case 1: drawHitMarkerPreview(px, pw, areaY, areaH, elapsed); break;
            case 2: drawParticlesPreview(px + 4, areaY, pw - 8, areaH, elapsed); break;
            case 3: drawHeartsPreview(px, pw, areaY, areaH, elapsed); break;
        }
    }

    private void drawComboPreview(int px, int pw, int areaY, int areaH, long elapsed) {
        if (previewComboCount <= 0 || elapsed >= settings.comboResetDelayMs) return;
        float alpha = 1.0f;
        if (settings.comboAnimFade && elapsed > settings.comboResetDelayMs - 500) {
            alpha = 1.0f - (float)(elapsed - (settings.comboResetDelayMs - 500)) / 500.0f;
        }
        float scale = settings.comboSize;
        if (settings.comboAnimScale && elapsed < 200) {
            scale *= 1.0f + (1.0f - elapsed / 200.0f) * 0.4f;
        }
        int color = getPreviewComboColor();
        int alphaI = (int)(Math.max(0, alpha) * 255);
        color = (color & 0x00FFFFFF) | (alphaI << 24);
        int labelAlpha = (int)(Math.max(0, alpha) * 200);
        int labelColor = (color & 0x00FFFFFF) | (labelAlpha << 24);

        GlStateManager.pushMatrix();
        int cx = settingsX + pw / 2;
        int cy = areaY + areaH / 2;
        GlStateManager.translate(cx, cy, 0);
        GlStateManager.scale(scale, scale, 1);
        if (settings.comboShowLabel) {
            String comboLabel = "Combo";
            int lw = fontRendererObj.getStringWidth(comboLabel);
            fontRendererObj.drawStringWithShadow(comboLabel, -lw / 2.0f, -fontRendererObj.FONT_HEIGHT, labelColor);
        }
        String countText = "x" + previewComboCount;
        int cw = fontRendererObj.getStringWidth(countText);
        float countY = settings.comboShowLabel ? 1.0f : -fontRendererObj.FONT_HEIGHT / 2.0f;
        fontRendererObj.drawStringWithShadow(countText, -cw / 2.0f, countY, color);
        GlStateManager.popMatrix();
    }

    private void drawHitMarkerPreview(int px, int pw, int areaY, int areaH, long elapsed) {
        int cx = settingsX + pw / 2;
        int cy = areaY + areaH / 2;
        Gui.drawRect(cx - 5, cy, cx + 6, cy + 1, 0xAAFFFFFF);
        Gui.drawRect(cx, cy - 5, cx + 1, cy + 6, 0xAAFFFFFF);

        VisualManager.getInstance().getHitMarker().renderStatic(cx, cy, elapsed, settings);
    }

    private void drawParticlesPreview(int x, int y, int w, int h, long elapsed) {
        long cycle = elapsed % 1500;
        float t = cycle / 1500.0f;
        int[] colors = {settings.particleColor1, settings.particleColor2, settings.particleColor3};
        int count = Math.min(settings.particleQuantity, 15);
        float centerX = x + w * 0.5f;
        float centerY = y + h * 0.55f;
        String[] typeNames = {"Redstone", "Potion", "Feu d'artifice"};
        fontRendererObj.drawStringWithShadow("\u00A78" + typeNames[settings.particleType], x + 2, y + 2, 0xFF666666);
        GlStateManager.enableBlend();
        for (int i = 0; i < count; i++) {
            double angle = (i * 2.399) + i * 0.5;
            float speed = 0.4f + (float)(Math.sin(i * 1.7) * 0.3 + 0.3);
            float dist = t * w * 0.35f * speed;
            float px = centerX + (float) Math.cos(angle) * dist;
            float py = centerY + (float) Math.sin(angle) * dist;
            float alpha = Math.max(0, 1.0f - t * 1.2f);
            int color = colors[i % 3];
            int c = (color & 0x00FFFFFF) | ((int)(alpha * 255) << 24);
            int sz = Math.max(1, (int)(settings.particleSize * 2.0f));
            switch (settings.particleType) {
                case 0:
                    if (px >= x && px + sz <= x + w && py >= y && py + sz <= y + h)
                        Gui.drawRect((int) px, (int) py, (int) px + sz, (int) py + sz, c);
                    break;
                case 1:
                    py -= t * 10;
                    if (px >= x + 1 && px + sz <= x + w - 1 && py >= y && py + sz <= y + h)
                        Gui.drawRect((int) px, (int) py, (int) px + sz, (int) py + sz, c);
                    break;
                case 2:
                    py -= t * 15;
                    float trailLen = 3 + settings.particleSize;
                    if (px >= x && px + 1 <= x + w && py >= y && py + trailLen <= y + h)
                        Gui.drawRect((int) px, (int) py, (int) px + 1, (int)(py + trailLen), c);
                    break;
            }
        }
    }

    private void drawHeartsPreview(int px, int pw, int areaY, int areaH, long elapsed) {
        if (elapsed >= 1500) return;
        float age = (float) elapsed / 1500.0f;
        float alpha = age > 0.6f ? 1.0f - (age - 0.6f) / 0.4f : 1.0f;
        int color = (settings.heartsColor & 0x00FFFFFF) | ((int)(Math.max(0, alpha) * 255) << 24);
        float floatY = areaY + areaH / 2.0f - age * 15.0f;
        String heartText = "\u2764 4.5";
        int hw = fontRendererObj.getStringWidth(heartText);
        fontRendererObj.drawStringWithShadow(heartText, settingsX + pw / 2.0f - hw / 2.0f, floatY, color);
    }

    private int getPreviewComboColor() {
        if (previewComboCount >= settings.comboThreshold3) return settings.comboColor3;
        if (previewComboCount >= settings.comboThreshold2) return settings.comboColor2;
        if (previewComboCount >= settings.comboThreshold1) return settings.comboColor1;
        return settings.comboColor;
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        int rtw = fontRendererObj.getStringWidth("\u00A77[Reset]");
        int rtx = panelX + panelW - rtw - 4;
        if (mx >= rtx && mx < rtx + rtw && my >= panelY + 3 && my < panelY + 15) {
            settings = new VisualSettings();
            settings.save();
            VisualManager.getInstance().setSettings(settings);
            previewComboCount = 0;
            return;
        }

        int catY = panelY + 20;
        int catH = Math.min(22, (panelH - 22) / CATEGORIES.length);
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (mx >= panelX && mx < panelX + catW && my >= catY && my < catY + catH) {
                selectedCategory = i;
                scrollOffset = 0;
                return;
            }
            catY += catH;
        }

        int previewH = Math.min(60, panelH / 4);
        int ppy = panelY + panelH - previewH;
        int stw = fontRendererObj.getStringWidth("[Tester]");
        int stx = settingsX + settingsW - stw - 6;
        if (mx >= stx && mx < stx + stw && my >= ppy + 2 && my < ppy + 14) {
            previewHitTime = System.currentTimeMillis();
            previewComboCount++;
            return;
        }

        handleSettingsClick(mx, my);
    }

    private void handleSettingsClick(int mx, int my) {
        int x = settingsX + 6;
        int w = settingsW - 12;
        int previewH = Math.min(60, panelH / 4);
        int clipTop = panelY + 22;
        int clipBottom = panelY + panelH - previewH;
        if (my < clipTop || my > clipBottom) return;

        int baseY = panelY + 22 - scrollOffset;
        int relY = my - baseY;
        int row = relY / 16;
        if (row < 0) return;

        switch (selectedCategory) {
            case 0: handleComboClick(row, mx, x, w); break;
            case 1: handleHitMarkerClick(row, mx, x, w); break;
            case 2: handleParticleClick(row, mx, x, w); break;
            case 3: handleHeartClick(row, mx, x, w); break;
        }
        // settings.save() retiré d'ici pour éviter lag pendant drag
    }

    private void handleComboClick(int row, int mx, int x, int w) {
        if (isOverToggle(mx, x, w)) {
            if (row == 0) settings.comboEnabled = !settings.comboEnabled;
            if (row == 1) settings.comboShowLabel = !settings.comboShowLabel;
            if (row == 6) settings.comboAnimScale = !settings.comboAnimScale;
            if (row == 7) settings.comboAnimFade = !settings.comboAnimFade;
        }
        handleColorClick(row, mx, x, w, 8, 0);
        handleColorClick(row, mx, x, w, 10, 1);
        handleColorClick(row, mx, x, w, 12, 2);
        handleColorClick(row, mx, x, w, 14, 3);
    }

    private void handleHitMarkerClick(int row, int mx, int x, int w) {
        if (isOverToggle(mx, x, w)) {
            if (row == 0) settings.hitMarkerEnabled = !settings.hitMarkerEnabled;
            if (row == 4) settings.hitMarkerFade = !settings.hitMarkerFade;
        }
        handleColorClick(row, mx, x, w, 5, 4);
    }

    private void handleParticleClick(int row, int mx, int x, int w) {
        if (isOverToggle(mx, x, w) && row == 0) settings.particlesEnabled = !settings.particlesEnabled;
        if (isOverEnum(mx, x, w)) {
            if (row == 1) settings.particleTrigger = (settings.particleTrigger + 1) % 3;
            if (row == 2) settings.particleType = (settings.particleType + 1) % 3;
            if (row == 5) settings.particleFilter = (settings.particleFilter + 1) % 3;
        }
        handleColorClick(row, mx, x, w, 6, 5);
        handleColorClick(row, mx, x, w, 7, 6);
        handleColorClick(row, mx, x, w, 8, 7);
    }

    private void handleHeartClick(int row, int mx, int x, int w) {
        if (isOverToggle(mx, x, w)) {
            if (row == 0) settings.heartsEnabled = !settings.heartsEnabled;
            if (row == 1) settings.heartsShowDamage = !settings.heartsShowDamage;
        }
        if (isOverEnum(mx, x, w) && row == 2) settings.heartsFilter = (settings.heartsFilter + 1) % 3;
        handleColorClick(row, mx, x, w, 3, 8);
    }

    private boolean isOverToggle(int mx, int x, int w) {
        int tw = 22;
        int tx = x + w - tw;
        return mx >= tx && mx < tx + tw;
    }

    private boolean isOverEnum(int mx, int x, int w) {
        return mx >= x + w / 2;
    }

    private void handleColorClick(int row, int mx, int x, int w, int targetRow, int colorId) {
        if (row != targetRow) return;
        int cx = x + w - 10 - 15;
        int lx = cx - 14;
        int rx = cx + 10 + 4;
        if (mx >= lx && mx < lx + 10) cycleColor(colorId, -1);
        if (mx >= rx && mx < rx + 10) cycleColor(colorId, 1);
    }

    private void cycleColor(int id, int dir) {
        int current = getColorForId(id);
        int index = -1;
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if ((PRESET_COLORS[i] & 0xFFFFFF) == (current & 0xFFFFFF)) {
                index = i;
                break;
            }
        }
        if (index == -1) index = 0;
        int next = (index + dir + PRESET_COLORS.length) % PRESET_COLORS.length;
        setColorForId(id, PRESET_COLORS[next]);
        settings.save();
    }

    private int getColorForId(int id) {
        switch (id) {
            case 0: return settings.comboColor;
            case 1: return settings.comboColor1;
            case 2: return settings.comboColor2;
            case 3: return settings.comboColor3;
            case 4: return settings.hitMarkerColor;
            case 5: return settings.particleColor1;
            case 6: return settings.particleColor2;
            case 7: return settings.particleColor3;
            case 8: return settings.heartsColor;
            default: return 0xFFFFFFFF;
        }
    }

    private void setColorForId(int id, int color) {
        switch (id) {
            case 0: settings.comboColor = color; break;
            case 1: settings.comboColor1 = color; break;
            case 2: settings.comboColor2 = color; break;
            case 3: settings.comboColor3 = color; break;
            case 4: settings.hitMarkerColor = color; break;
            case 5: settings.particleColor1 = color; break;
            case 6: settings.particleColor2 = color; break;
            case 7: settings.particleColor3 = color; break;
            case 8: settings.heartsColor = color; break;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long timeSinceClick) {
        if (btn != 0) return;
        int x = settingsX + 6, w = settingsW - 12;
        int baseY = panelY + 22 - scrollOffset;
        int relY = my - baseY;
        int row = relY / 16;

        int sx = x + w - SW - TW - GAP;

        if (draggingSlider == -1) {
            // Ne démarre un drag que si le clic initial était sur la barre du slider,
            // évite de régler la valeur à 1 quand on clique sur le label (côté gauche)
            if (mx < sx) return;
            draggingSlider = row;
        }

        float ratio = MathHelper.clamp_float((float)(mx - sx) / SW, 0.0f, 1.0f);
        applySliderValue(selectedCategory, draggingSlider, ratio);
    }

    @Override
    protected void mouseReleased(int mx, int my, int btn) {
        super.mouseReleased(mx, my, btn);
        if (draggingSlider != -1) {
            settings.save();
            draggingSlider = -1;
        }
    }

    private void applySliderValue(int cat, int row, float ratio) {
        switch (cat) {
            case 0:
                switch (row) {
                    case 2: settings.comboSize = 0.3f + ratio * 2.7f; break;
                    case 3: settings.comboPosX = -1.0f + ratio * 2.0f; break;
                    case 4: settings.comboPosY = -1.0f + ratio * 2.0f; break;
                    case 5: settings.comboResetDelayMs = (int)(500 + ratio * 4500); break;
                    case 9: settings.comboThreshold1 = 2 + (int)(ratio * 28); break;
                    case 11: settings.comboThreshold2 = 2 + (int)(ratio * 48); break;
                    case 13: settings.comboThreshold3 = 2 + (int)(ratio * 98); break;
                }
                break;
            case 1:
                switch (row) {
                    case 1: settings.hitMarkerSize = 2.0f + ratio * 18.0f; break;
                    case 2: settings.hitMarkerOpacity = 0.1f + ratio * 0.9f; break;
                    case 3: settings.hitMarkerDurationMs = (int)(50 + ratio * 950); break;
                }
                break;
            case 2:
                switch (row) {
                    case 3: settings.particleQuantity = 1 + (int)(ratio * 29); break;
                    case 4: settings.particleSize = 0.2f + ratio * 2.8f; break;
                }
                break;
            case 3:
                if (row == 2) settings.heartsQuantity = 1 + (int)(ratio * 9);
                break;
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (keyCode == 1) {
            settings.save();
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            scrollOffset = MathHelper.clamp_int(scrollOffset + (scroll > 0 ? -16 : 16), 0, maxScroll);
        }
    }

    @Override
    public void onGuiClosed() {
        settings.save();
        VisualManager.getInstance().setSettings(settings);
    }

    private boolean isModuleEnabled(int cat) {
        switch (cat) {
            case 0: return settings.comboEnabled;
            case 1: return settings.hitMarkerEnabled;
            case 2: return settings.particlesEnabled;
            case 3: return settings.heartsEnabled;
            default: return false;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
