package net.minecraft.client.waypoint;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;

import java.io.IOException;

/**
 * GUI des paramètres globaux de rendu des waypoints.
 */
public class GuiWaypointSettings extends GuiScreen {

    private final GuiScreen parent;
    private final WaypointManager.WaypointSettings s;

    // Layout du panneau
    private int panelX, panelY, panelW, panelH;

    // Slider en cours de drag (-1 = aucun)
    private int draggingSlider = -1;

    // Couleurs
    private static final int ACCENT    = 0xFFCC2222;
    private static final int BG_DARK   = 0xF0101018;
    private static final int BG_HEADER = 0xFF1A1A24;
    private static final int BORDER    = 0x30FFFFFF;

    // Layout des sliders
    private static final int SW  = 100; // slider track width
    private static final int TW  = 46;  // value text width
    private static final int GAP = 8;
    private static final int ROW = 22;  // hauteur d'une ligne

    public GuiWaypointSettings(GuiScreen parent) {
        this.parent = parent;
        this.s = WaypointManager.INSTANCE.getSettings();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        panelW = Math.min(420, this.width - 30);
        panelH = 248;
        panelX = (this.width  - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        this.buttonList.add(new GuiButton(0,
                panelX + panelW / 2 - 60, panelY + panelH - 28, 120, 20,
                "Fermer"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawRect(0, 0, this.width, this.height, 0xAA000000);

        // Panneau
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_DARK);
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 1, ACCENT);
        Gui.drawRect(panelX, panelY + 1, panelX + panelW, panelY + 20, BG_HEADER);
        drawBorderRect(panelX, panelY, panelW, panelH, BORDER);

        // Titre
        String title = "\u00A7c\u00A7lPARAMETRES \u00A7f\u00A7lWAYPOINTS";
        fontRendererObj.drawStringWithShadow(title,
                panelX + panelW / 2.0f - fontRendererObj.getStringWidth(title) / 2.0f,
                panelY + 6, 0xFFFFFFFF);

        int x = panelX + 12;
        int w = panelW - 24;
        int y = panelY + 26;

        // ── Toggle taille adaptative ───────────────────────────────────────────
        y = drawToggle(x, y, w, "Taille adaptative (grossit avec la distance)", s.distanceScaleEnabled);
        y += 2;
        Gui.drawRect(x, y, x + w, y + 1, 0x20FFFFFF); y += 7;

        if (s.distanceScaleEnabled) {
            // ── Sliders mode adaptatif ─────────────────────────────────────────
            y = drawSlider(x, y, w, "Dist. de ref. (blocs)",
                    s.refDistance, 10f, 300f, 1, mouseX, mouseY, "%.0f");
            y = drawSlider(x, y, w, "Taille \u00e0 dist. ref.",
                    s.baseScale,   0.005f, 0.12f, 2, mouseX, mouseY, "%.3f");
            y = drawSlider(x, y, w, "Taille minimum",
                    s.minScale,    0.002f, 0.06f, 3, mouseX, mouseY, "%.3f");
            y = drawSlider(x, y, w, "Taille maximum",
                    s.maxScale,    0.05f,  0.50f, 4, mouseX, mouseY, "%.3f");

            // ── Aperçu visuel ──────────────────────────────────────────────────
            y += 4;
            int[] distances = {10, 50, 100, 250, 500};
            String preview = "\u00A78Aper\u00e7u : ";
            for (int dist : distances) {
                float raw  = s.baseScale * (dist / Math.max(s.refDistance, 1f));
                float scl  = Math.max(s.minScale, Math.min(s.maxScale, raw));
                int pct = Math.round(scl / s.baseScale * 100f);
                String col = dist <= s.refDistance * 0.5f ? "\u00A77"
                           : dist <= s.refDistance        ? "\u00A7e"
                           : dist <= s.refDistance * 2f   ? "\u00A7a"
                           :                                "\u00A7b";
                preview += col + dist + "m:" + pct + "%  ";
            }
            fontRendererObj.drawStringWithShadow(preview, x, y, 0xFFFFFFFF);
            y += 12;

            // Explication
            String hint = "\u00A77A " + (int)s.refDistance + " blocs : taille 100%. Plus loin = plus grand.";
            fontRendererObj.drawStringWithShadow(hint,
                    panelX + panelW / 2.0f - fontRendererObj.getStringWidth(hint) / 2.0f,
                    panelY + panelH - 42, 0xFFFFFFFF);
        } else {
            // ── Slider mode fixe ───────────────────────────────────────────────
            y = drawSlider(x, y, w, "Taille fixe du label",
                    s.fixedScale, 0.005f, 0.12f, 5, mouseX, mouseY, "%.3f");

            String hint = "\u00A77Taille constante quelle que soit la distance.";
            fontRendererObj.drawStringWithShadow(hint,
                    panelX + panelW / 2.0f - fontRendererObj.getStringWidth(hint) / 2.0f,
                    panelY + panelH - 42, 0xFFFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ── Widgets ───────────────────────────────────────────────────────────────

    private int drawToggle(int x, int y, int w, String label, boolean value) {
        // Tronquer si trop long
        String lbl = label;
        int maxLW = w - 32 - GAP - 4;
        while (fontRendererObj.getStringWidth(lbl) > maxLW && lbl.length() > 4)
            lbl = lbl.substring(0, lbl.length() - 4) + "...";
        fontRendererObj.drawStringWithShadow(lbl, x, y + 3, 0xFFCCCCCC);

        int tw = 28, th = 12;
        int tx = x + w - tw, ty = y + 1;
        Gui.drawRect(tx, ty, tx + tw, ty + th, value ? 0xFF1A7A4A : 0xFF2A2A3A);
        drawBorderRect(tx, ty, tw, th, 0x30FFFFFF);
        int knobX = value ? tx + tw - th + 1 : tx + 1;
        Gui.drawRect(knobX, ty + 1, knobX + th - 2, ty + th - 1, 0xFFEEEEEE);
        return y + ROW;
    }

    private int drawSlider(int x, int y, int w, String label,
                           float value, float min, float max,
                           int sliderId, int mx, int my, String fmt) {
        // Tronquer le label si trop long
        String lbl = label;
        int maxLW = w - SW - TW - GAP * 2 - 4;
        while (fontRendererObj.getStringWidth(lbl) > maxLW && lbl.length() > 4)
            lbl = lbl.substring(0, lbl.length() - 4) + "...";
        fontRendererObj.drawStringWithShadow(lbl, x, y + 3, 0xFFCCCCCC);

        // Valeur à droite
        String valStr = String.format(fmt, value);
        int tx = x + w - TW;
        fontRendererObj.drawStringWithShadow(valStr,
                tx + TW - fontRendererObj.getStringWidth(valStr), y + 3, 0xFFFF9966);

        // Track + curseur
        int sx = tx - SW - GAP;
        int sy = y + 8;
        Gui.drawRect(sx, sy, sx + SW, sy + 4, 0xFF2A2A3A);
        float ratio = MathHelper.clamp_float((value - min) / (max - min), 0f, 1f);
        int filled = (int)(SW * ratio);
        Gui.drawRect(sx, sy, sx + filled, sy + 4, ACCENT);
        Gui.drawRect(sx + filled - 3, sy - 2, sx + filled + 3, sy + 6, 0xFFEEEEEE);

        return y + ROW;
    }

    private void drawBorderRect(int x, int y, int w, int h, int col) {
        Gui.drawRect(x,         y,         x + w,     y + 1,     col);
        Gui.drawRect(x,         y + h - 1, x + w,     y + h,     col);
        Gui.drawRect(x,         y,         x + 1,     y + h,     col);
        Gui.drawRect(x + w - 1, y,         x + w,     y + h,     col);
    }

    // ── Positions de référence (doivent correspondre à drawScreen) ────────────

    private int toggleY()  { return panelY + 26; }
    private int sep1Y()    { return toggleY() + ROW + 2; }
    private int slider1Y() { return sep1Y() + 1 + 7; }

    // ── Interactions ──────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        int x = panelX + 12;
        int w = panelW - 24;

        // Clic toggle
        int ty = toggleY() + 1;
        int tx = x + w - 28;
        if (mx >= tx && mx < tx + 28 && my >= ty && my < ty + 12) {
            s.distanceScaleEnabled = !s.distanceScaleEnabled;
            WaypointManager.INSTANCE.saveSettings();
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long since) {
        if (btn != 0) return;
        int x = panelX + 12;
        int w = panelW - 24;
        int tx = x + w - TW;
        int sx = tx - SW - GAP;

        int y1 = slider1Y();

        if (draggingSlider == -1) {
            if (mx < sx || mx > sx + SW) return;
            // Détecter quel slider
            if (s.distanceScaleEnabled) {
                if      (my >= y1           && my < y1 + ROW) draggingSlider = 1;
                else if (my >= y1 + ROW     && my < y1 + ROW * 2) draggingSlider = 2;
                else if (my >= y1 + ROW * 2 && my < y1 + ROW * 3) draggingSlider = 3;
                else if (my >= y1 + ROW * 3 && my < y1 + ROW * 4) draggingSlider = 4;
            } else {
                if (my >= y1 && my < y1 + ROW) draggingSlider = 5;
            }
            if (draggingSlider == -1) return;
        }

        float ratio = MathHelper.clamp_float((float)(mx - sx) / SW, 0f, 1f);
        switch (draggingSlider) {
            case 1: s.refDistance  = 10f    + ratio * (300f   - 10f);    break;
            case 2: s.baseScale    = 0.005f + ratio * (0.12f  - 0.005f); break;
            case 3: s.minScale     = 0.002f + ratio * (0.06f  - 0.002f); break;
            case 4: s.maxScale     = 0.05f  + ratio * (0.50f  - 0.05f);  break;
            case 5: s.fixedScale   = 0.005f + ratio * (0.12f  - 0.005f); break;
        }
        // Garantir minScale <= maxScale
        if (draggingSlider == 3 && s.minScale > s.maxScale) s.minScale = s.maxScale;
        if (draggingSlider == 4 && s.maxScale < s.minScale) s.maxScale = s.minScale;
    }

    @Override
    protected void mouseReleased(int mx, int my, int btn) {
        super.mouseReleased(mx, my, btn);
        if (draggingSlider != -1) {
            WaypointManager.INSTANCE.saveSettings();
            draggingSlider = -1;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            WaypointManager.INSTANCE.saveSettings();
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (keyCode == 1) {
            WaypointManager.INSTANCE.saveSettings();
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
