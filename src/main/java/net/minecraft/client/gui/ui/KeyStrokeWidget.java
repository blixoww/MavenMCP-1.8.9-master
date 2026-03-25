package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.util.ArrayList;
import java.util.List;

public class KeyStrokeWidget extends BaseWidget {

    // Indices physiques dans keys[]
    // 0=Fwd(Z), 1=Left(Q), 2=Back(S), 3=Right(D), 4=Jump(Space), 5=Sneak, 6=Sprint, 7=Attack(LMB), 8=Use(RMB)
    private static final int IDX_SPACE = 4;
    private static final int IDX_LMB = 7;
    private static final int IDX_RMB = 8;

    // Flash : très court et très discret
    private static final int FLASH_DURATION = 120; // ms
    private static final float FLASH_MAX = 0.22f; // opacité max

    // Couleurs des cases (noir transparent)
    private static final int BOX_NORMAL = 0xB0000000; // noir semi-transparent
    private static final int BOX_PRESSED = 0xC8202020; // légèrement plus clair quand pressé
    private static final int BOX_BORDER = 0xB0000000; // bordure noire subtile
    private static final int BOX_BORDER_PRESSED = 0xAAFFFFFF; // bordure plus vive si pressé

    private final KeyBinding[] keys;
    private boolean[] prevDown;

    private final List<Long> leftClickTimes = new ArrayList<>();
    private final List<Long> rightClickTimes = new ArrayList<>();
    private boolean prevLmbDown = false;
    private boolean prevRmbDown = false;

    private final long[] lastKeyPressTime = new long[9];
    private long lastLmbPress = 0L;
    private long lastRmbPress = 0L;
    private long lastSpacePress = 0L;

    public KeyStrokeWidget(String id, int x, int y, KeyBinding[] keys) {
        super(id, x, y);
        this.keys = keys;
        this.width = 88;
        this.height = 96;
        if (getPropOrDefault("showBackground", null) == null) setProp("showBackground", Boolean.FALSE);
        if (getPropOrDefault("initialized", null) == null) setProp("initialized", Boolean.FALSE);
        if (getPropOrDefault("showSpaceRainbow", null) == null) setProp("showSpaceRainbow", Boolean.FALSE);
        try {
            this.setRGBMode(true);
        } catch (Throwable ignored) {
        }
    }

    // ── Accesseurs éditeur ────────────────────────────────────────────────────

    public int getKeyCount() {
        return 7;
    }

    public String getKeyLabel(int i) {
        switch (i) {
            case 0:
                return getBinding(0, "Avancer");
            case 1:
                return getBinding(1, "Gauche");
            case 2:
                return getBinding(2, "Reculer");
            case 3:
                return getBinding(3, "Droite");
            case 4:
                return "Clic gauche";
            case 5:
                return "Clic droit";
            case 6:
                return "Espace";
            default:
                return "";
        }
    }

    public boolean isEditorKeyVisible(int editorIdx) {
        return Boolean.TRUE.equals(getPropOrDefault("showKey" + editorIdx, Boolean.TRUE));
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        if (prevDown == null) {
            prevDown = new boolean[keys.length];
            for (int i = 0; i < 7; i++)
                if (getPropOrDefault("showKey" + i, null) == null) setProp("showKey" + i, Boolean.TRUE);
        }

        if (!Boolean.TRUE.equals(getPropOrDefault("initialized", Boolean.FALSE))) {
            ScaledResolution sr = new ScaledResolution(mc);
            if (this.x == 0 && this.y == 0) {
                this.x = Math.max(10, sr.getScaledWidth() - this.width - 10);
                this.y = 10;
            }
            setProp("initialized", Boolean.TRUE);
        }

        try {
            ScaledResolution sr = new ScaledResolution(mc);
            int sw = sr.getScaledWidth();
            int sh = sr.getScaledHeight();
            
            if (sw > 0 && sh > 0 && (sw != this.refW || sh != this.refH)) {
                if (this.refW > 0 && this.refH > 0) {
                    int maxX = Math.max(1, sw - this.width);
                    int maxY = Math.max(1, sh - this.height);
                    this.x = (int) Math.round(this.relX * maxX);
                    this.y = (int) Math.round(this.relY * maxY);
                    this.x = Math.max(0, Math.min(maxX, this.x));
                    this.y = Math.max(0, Math.min(maxY, this.y));
                }
                this.refW = sw; this.refH = sh;
                this.refWidgetW = this.width; this.refWidgetH = this.height;
                int currentMaxX = Math.max(1, sw - this.width);
                int currentMaxY = Math.max(1, sh - this.height);
                this.relX = (double) this.x / currentMaxX;
                this.relY = (double) this.y / currentMaxY;
            } else {
                int maxX = Math.max(1, sw - this.width);
                int maxY = Math.max(1, sh - this.height);
                this.relX = (double) this.x / maxX;
                this.relY = (double) this.y / maxY;
                this.refW = sw; this.refH = sh;
            }
        } catch (Throwable ignored) {}

        FontRenderer fr = mc.fontRendererObj;
        long now = System.currentTimeMillis();

        boolean lmbDown = false, rmbDown = false, spaceDown = false;
        if (IDX_LMB < keys.length) try { lmbDown = keys[IDX_LMB].isKeyDown(); } catch (Throwable t) {}
        if (IDX_RMB < keys.length) try { rmbDown = keys[IDX_RMB].isKeyDown(); } catch (Throwable t) {}
        if (IDX_SPACE < keys.length) try { spaceDown = keys[IDX_SPACE].isKeyDown(); } catch (Throwable t) {}

        if (lmbDown && !prevLmbDown) { leftClickTimes.add(now); lastLmbPress = now; }
        if (rmbDown && !prevRmbDown) { rightClickTimes.add(now); lastRmbPress = now; }
        if (spaceDown && prevDown.length > IDX_SPACE && !prevDown[IDX_SPACE]) lastSpacePress = now;

        prevLmbDown = lmbDown; prevRmbDown = rmbDown;
        leftClickTimes.removeIf(t -> t < now - 1000L);
        rightClickTimes.removeIf(t -> t < now - 1000L);

        for (int i = 0; i < Math.min(4, keys.length); i++) {
            boolean pressed = false;
            try { pressed = keys[i].isKeyDown(); } catch (Throwable t) {}
            if (pressed && !prevDown[i]) lastKeyPressTime[i] = now;
            prevDown[i] = pressed;
        }
        if (IDX_SPACE < keys.length) prevDown[IDX_SPACE] = spaceDown;

        int kW = 26, kH = 26, gap = 3;
        int rowW = kW * 3 + gap * 2;
        int mH = 26, spH = 10;
        this.width = rowW;
        this.height = kH + gap + kH + gap + mH + gap + spH;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int txtColor = isRGBMode() ? getRainbowColor() : getColor();
        int sX = this.x, sY = this.y;

        // Ligne 1
        if (isEditorKeyVisible(0) && 0 < keys.length) drawKey(fr, sX + kW + gap, sY, kW, kH, 0, txtColor, now);
        // Ligne 2
        int l2y = sY + kH + gap;
        if (isEditorKeyVisible(1) && 1 < keys.length) drawKey(fr, sX, l2y, kW, kH, 1, txtColor, now);
        if (isEditorKeyVisible(2) && 2 < keys.length) drawKey(fr, sX + kW + gap, l2y, kW, kH, 2, txtColor, now);
        if (isEditorKeyVisible(3) && 3 < keys.length) drawKey(fr, sX + 2 * (kW + gap), l2y, kW, kH, 3, txtColor, now);
        // Ligne 3
        int l3y = l2y + kH + gap;
        int mouseGap = 4, mouseW = (rowW - mouseGap) / 2;
        if (isEditorKeyVisible(4) && IDX_LMB < keys.length) drawMouse(fr, sX, l3y, mouseW, mH, IDX_LMB, txtColor, now, "LMB", leftClickTimes.size(), lastLmbPress);
        if (isEditorKeyVisible(5) && IDX_RMB < keys.length) drawMouse(fr, sX + mouseW + mouseGap, l3y, mouseW, mH, IDX_RMB, txtColor, now, "RMB", rightClickTimes.size(), lastRmbPress);
        // Ligne 4
        int l4y = l3y + mH + gap;
        if (isEditorKeyVisible(6)) drawSpaceBarImproved(sX, l4y, rowW, spH, now, txtColor);

        GlStateManager.disableBlend();
    }

    private void drawKey(FontRenderer fr, int x, int y, int w, int h, int physIdx, int txt, long now) {
        boolean pressed = prevDown[physIdx];
        Gui.drawRect(x, y, x + w, y + h, pressed ? BOX_PRESSED : BOX_NORMAL);
        drawBorder1px(x, y, w, h, pressed ? BOX_BORDER_PRESSED : BOX_BORDER);
        long elapsed = now - lastKeyPressTime[physIdx];
        if (elapsed < FLASH_DURATION) {
            float t = elapsed / (float) FLASH_DURATION;
            int fa = (int) (FLASH_MAX * (1f - t * t) * 255f);
            if (fa > 0) Gui.drawRect(x, y, x + w, y + h, (fa << 24) | 0xFFFFFF);
        }
        String label = GameSettings.getKeyDisplayString(keys[physIdx].getKeyCode());
        if (label == null || label.isEmpty()) label = keys[physIdx].getKeyDescription();
        fr.drawStringWithShadow(label, x + (w - fr.getStringWidth(label)) / 2.0f, y + (h - 8) / 2.0f, txt);
    }

    private void drawMouse(FontRenderer fr, int x, int y, int w, int h, int physIdx, int txt, long now, String label, int cps, long lastPress) {
        boolean pressed = prevDown[physIdx];
        Gui.drawRect(x, y, x + w, y + h, pressed ? BOX_PRESSED : BOX_NORMAL);
        drawBorder1px(x, y, w, h, pressed ? BOX_BORDER_PRESSED : BOX_BORDER);
        long elapsed = now - lastPress;
        if (elapsed < FLASH_DURATION) {
            float t = elapsed / (float) FLASH_DURATION;
            int fa = (int) (FLASH_MAX * (1f - t * t) * 255f);
            if (fa > 0) Gui.drawRect(x, y, x + w, y + h, (fa << 24) | 0xFFFFFF);
        }
        int labelY = y + (h - 16) / 2;
        fr.drawStringWithShadow(label, x + (w - fr.getStringWidth(label)) / 2.0f, labelY, txt);
        String cpsStr = cps + " CPS";
        fr.drawStringWithShadow(cpsStr, x + (w - fr.getStringWidth(cpsStr)) / 2.0f, labelY + 9, txt & 0x77FFFFFF);
    }

    private void drawSpaceBarImproved(int x, int y, int w, int h, long now, int txtColor) {
        boolean pressed = false;
        if (IDX_SPACE < keys.length) try { pressed = keys[IDX_SPACE].isKeyDown(); } catch (Throwable t) {}
        Gui.drawRect(x, y, x + w, y + h, pressed ? BOX_PRESSED : BOX_NORMAL);
        drawBorder1px(x, y, w, h, pressed ? BOX_BORDER_PRESSED : BOX_BORDER);

        boolean showRainbow = Boolean.TRUE.equals(getPropOrDefault("showSpaceRainbow", false));
        if (showRainbow) {
            int rgbH = 2, rgbW = w - 6;
            int rgbX = x + 3, rgbY = y + (h - rgbH) / 2;
            float timeOffset = (now % 4000L) / 4000.0f;
            for (int i = 0; i < rgbW; i++) {
                float hue = (timeOffset + i / (float) rgbW) % 1.0f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.7f, 1.0f) & 0x00FFFFFF;
                Gui.drawRect(rgbX + i, rgbY, rgbX + i + 1, rgbY + rgbH, 0xCC000000 | rgb);
            }
        } else {
            // Version sobre : petite ligne colorée simple ou juste vide
            int lineW = w / 3, lineH = 1;
            int lx = x + (w - lineW) / 2, ly = y + (h - lineH) / 2;
            Gui.drawRect(lx, ly, lx + lineW, ly + lineH, txtColor & 0x99FFFFFF);
        }

        long elapsed = now - lastSpacePress;
        if (elapsed < FLASH_DURATION) {
            float t = elapsed / (float) FLASH_DURATION;
            int fa = (int) (FLASH_MAX * (1f - t * t) * 255f);
            if (fa > 0) Gui.drawRect(x, y, x + w, y + h, (fa << 24) | 0xFFFFFF);
        }
    }

    private void drawBorder1px(int x, int y, int w, int h, int color) {
        Gui.drawRect(x, y, x + w, y + 1, color);
        Gui.drawRect(x, y + h - 1, x + w, y + h, color);
        Gui.drawRect(x, y, x + 1, y + h, color);
        Gui.drawRect(x + w - 1, y, x + w, y + h, color);
    }

    private int getRainbowColor() {
        float hue = (System.currentTimeMillis() % 8000L) / 8000.0f;
        int c = java.awt.Color.HSBtoRGB(hue, 0.6f, 1.0f);
        return (c & 0x00FFFFFF) | 0xFF000000;
    }

    private String getBinding(int idx, String fallback) {
        if (idx >= keys.length) return fallback;
        String s = GameSettings.getKeyDisplayString(keys[idx].getKeyCode());
        return (s != null && !s.isEmpty()) ? s : fallback;
    }
}
