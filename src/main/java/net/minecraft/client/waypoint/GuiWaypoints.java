package net.minecraft.client.waypoint;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.List;

/**
 * GUI listant tous les waypoints.
 * Chaque ligne affiche : couleur, nom, coords, taille texte + boutons d'action.
 */
public class GuiWaypoints extends GuiScreen {

    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT = 38;   // un peu plus haut pour loger le bouton Texte
    private static final int VISIBLE_ENTRIES = 7;

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0,
                this.width / 2 - 80, this.height - 30, 160, 22,
                "\u00a7a\u00a7l+ Nouveau Waypoint"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // ── Titre ──────────────────────────────────────────────────────────────
        this.drawCenteredString(this.fontRendererObj,
                "\u00a7l\u00a76\u2605 Waypoints \u2605", this.width / 2, 7, 0xFFFFFF);
        drawHorizontalLine(10, this.width - 10, 20, 0x55FFFFFF);

        List<Waypoint> wps = WaypointManager.INSTANCE.getWaypoints();

        if (wps.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj,
                    "\u00a77Aucun waypoint.", this.width / 2, this.height / 2, 0x888888);
        } else {
            int startY = 25;
            for (int i = scrollOffset; i < wps.size() && i < scrollOffset + VISIBLE_ENTRIES; i++) {
                Waypoint wp = wps.get(i);
                drawEntry(mouseX, mouseY, wp, i, startY + (i - scrollOffset) * ENTRY_HEIGHT);
            }

            if (wps.size() > VISIBLE_ENTRIES) {
                String info = (scrollOffset + 1) + "–"
                        + Math.min(scrollOffset + VISIBLE_ENTRIES, wps.size())
                        + " / " + wps.size() + "  \u2195 molette";
                this.fontRendererObj.drawString("\u00a78" + info,
                        this.width / 2 - this.fontRendererObj.getStringWidth("\u00a78" + info) / 2,
                        this.height - 44, 0xAAAAAA);
            }
        }

        drawHorizontalLine(10, this.width - 10, this.height - 35, 0x55FFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ── Dessin d'une entrée ─────────────────────────────────────────────────

    private void drawEntry(int mx, int my, Waypoint wp, int idx, int y) {
        int x  = 10;
        int x2 = this.width - 10;
        int yb = y + ENTRY_HEIGHT - 2;  // bas de la ligne

        // Fond alternant
        drawRect(x, y, x2, yb, idx % 2 == 0 ? 0x1AFFFFFF : 0x0DFFFFFF);

        // ── Colonne gauche : carré couleur WP + nom + coords ────────────────
        int wpRgb = 0xFF000000
                | (wp.getColorR() << 16)
                | (wp.getColorG() << 8)
                |  wp.getColorB();
        drawRect(x + 3, y + 3, x + 17, y + 17, 0xFF111111);  // ombre
        drawRect(x + 4, y + 4, x + 16, y + 16, wpRgb);       // couleur

        // Nom
        String nameColor = wp.isEnabled() ? "\u00a7a" : "\u00a78";
        this.fontRendererObj.drawStringWithShadow(
                nameColor + wp.getName(), x + 21, y + 4, 0xFFFFFF);

        // Coordonnées
        this.fontRendererObj.drawString(
                "\u00a77[" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + "]",
                x + 21, y + 15, 0x888888);

        // ── Boutons à droite ─────────────────────────────────────────────────
        // Layout (de droite à gauche) :
        //   [DEL 26px] [XYZ 32px] [BEAM+couleur 50px] [TEXTE 46px] [ON/OFF 36px]
        int bRight = x2 - 4;

        // Bouton DEL
        int delW = 26;
        drawBtn(mx, my, bRight - delW, y + 4, bRight, yb - 4,
                0x80881111, 0xBBCC1111, 0xFFFF3333,
                "\u00a7c\u2715");
        bRight -= delW + 4;

        // Bouton XYZ
        int xyzW = 32;
        boolean coords = wp.isCoordsVisible();
        drawBtn(mx, my, bRight - xyzW, y + 4, bRight, yb - 4,
                coords ? 0x80886600 : 0x50333333,
                coords ? 0xBBAA8800 : 0x70555555,
                coords ? 0xFFFFCC00 : 0xFF888888,
                coords ? "\u00a7eXYZ" : "\u00a78XYZ");
        bRight -= xyzW + 4;

        // Bouton BEAM — fond coloré si actif
        int beamW = 50;
        boolean beam = wp.isBeamVisible();
        int bx1 = bRight - beamW, by1 = y + 4, bx2 = bRight, by2 = yb - 4;
        boolean beamHov = mx >= bx1 && mx <= bx2 && my >= by1 && my <= by2;
        int beamFill   = beam ? (0xA0000000 | (wp.getColorR() << 16) | (wp.getColorG() << 8) | wp.getColorB()) : 0x50333333;
        int beamBorder = beam
                ? (beamHov
                    ? (0xFF000000 | (Math.min(255, wp.getColorR() + 60) << 16) | (Math.min(255, wp.getColorG() + 60) << 8) | Math.min(255, wp.getColorB() + 60))
                    : (0xC0000000 | (wp.getColorR() << 16) | (wp.getColorG() << 8) | wp.getColorB()))
                : (beamHov ? 0xFF666666 : 0x50555555);
        drawRect(bx1, by1, bx2, by2, beamFill);
        drawBorder(bx1, by1, bx2, by2, beamBorder);
        if (beam) {
            drawRect(bx1 + 3, by1 + 3, bx1 + 11, by2 - 3, 0xFF111111);
            drawRect(bx1 + 4, by1 + 4, bx1 + 10, by2 - 4,
                    0xFF000000 | (wp.getColorR() << 16) | (wp.getColorG() << 8) | wp.getColorB());
            this.fontRendererObj.drawString("\u00a7fBeam", bx1 + 14, (by1 + by2) / 2 - 4, 0xFFFFFF);
        } else {
            this.fontRendererObj.drawString("\u00a78Beam",
                    bx1 + (beamW - this.fontRendererObj.getStringWidth("\u00a78Beam")) / 2,
                    (by1 + by2) / 2 - 4, 0xFFFFFF);
        }
        bRight -= beamW + 4;

        // Bouton TEXTE (cycle taille : Petit → Moyen → Grand)
        int txtW = 46;
        Waypoint.TextSize ts = wp.getTextSize();
        String tsLabel = ts == Waypoint.TextSize.LARGE  ? "\u00a7aTxt:G"
                       : ts == Waypoint.TextSize.MEDIUM ? "\u00a7eTxt:M"
                       :                                  "\u00a77Txt:P";
        int tsBgN = ts == Waypoint.TextSize.LARGE  ? 0x80005500
                  : ts == Waypoint.TextSize.MEDIUM ? 0x80555500
                  :                                  0x50333333;
        int tsBgH = ts == Waypoint.TextSize.LARGE  ? 0xBB007700
                  : ts == Waypoint.TextSize.MEDIUM ? 0xBB888800
                  :                                  0x70555555;
        int tsBd  = ts == Waypoint.TextSize.LARGE  ? 0xFF00FF44
                  : ts == Waypoint.TextSize.MEDIUM ? 0xFFFFDD00
                  :                                  0xFF888888;
        drawBtn(mx, my, bRight - txtW, y + 4, bRight, yb - 4, tsBgN, tsBgH, tsBd, tsLabel);
        bRight -= txtW + 4;

        // Bouton ON/OFF
        int onW = 36;
        boolean en = wp.isEnabled();
        drawBtn(mx, my, bRight - onW, y + 4, bRight, yb - 4,
                en ? 0x8000AA00 : 0x80AA0000,
                en ? 0xBB00CC00 : 0xBBCC0000,
                en ? 0xFF00FF44 : 0xFFFF4444,
                en ? "\u00a7aON" : "\u00a7cOFF");
    }

    /**
     * Dessine un bouton avec fond/hover/bordure et texte centré.
     */
    private void drawBtn(int mx, int my,
                         int x1, int y1, int x2, int y2,
                         int bgNorm, int bgHov, int border,
                         String label) {
        boolean hov = mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
        drawRect(x1, y1, x2, y2, hov ? bgHov : bgNorm);
        drawBorder(x1, y1, x2, y2, hov ? border : (border & 0x80FFFFFF));
        int lw = this.fontRendererObj.getStringWidth(label);
        this.fontRendererObj.drawString(label,
                x1 + (x2 - x1 - lw) / 2,
                y1 + (y2 - y1) / 2 - 4,
                0xFFFFFF);
    }

    /** Bordure 1 px. */
    private void drawBorder(int x1, int y1, int x2, int y2, int col) {
        drawHorizontalLine(x1, x2, y1, col);
        drawHorizontalLine(x1, x2, y2, col);
        drawVerticalLine(x1, y1, y2, col);
        drawVerticalLine(x2, y1, y2, col);
    }

    // ── Hit-tests (identiques au layout de drawEntry) ──────────────────────

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);

        List<Waypoint> wps = WaypointManager.INSTANCE.getWaypoints();
        int startY = 25;
        int x2 = this.width - 10;

        for (int i = scrollOffset; i < wps.size() && i < scrollOffset + VISIBLE_ENTRIES; i++) {
            Waypoint wp = wps.get(i);
            int y  = startY + (i - scrollOffset) * ENTRY_HEIGHT;
            int yb = y + ENTRY_HEIGHT - 2;
            int by1 = y + 4, by2 = yb - 4;

            int bRight = x2 - 4;

            // DEL
            int delW = 26;
            if (inRect(mx, my, bRight - delW, by1, bRight, by2)) {
                WaypointManager.INSTANCE.removeWaypoint(i);
                if (scrollOffset > 0 && scrollOffset >= wps.size() - 1) scrollOffset--;
                return;
            }
            bRight -= delW + 4;

            // XYZ
            int xyzW = 32;
            if (inRect(mx, my, bRight - xyzW, by1, bRight, by2)) {
                wp.setCoordsVisible(!wp.isCoordsVisible());
                WaypointManager.INSTANCE.save();
                return;
            }
            bRight -= xyzW + 4;

            // BEAM
            int beamW = 50;
            if (inRect(mx, my, bRight - beamW, by1, bRight, by2)) {
                wp.setBeamVisible(!wp.isBeamVisible());
                WaypointManager.INSTANCE.save();
                return;
            }
            bRight -= beamW + 4;

            // TEXTE (cycle taille)
            int txtW = 46;
            if (inRect(mx, my, bRight - txtW, by1, bRight, by2)) {
                wp.setTextSize(wp.getTextSize().next());
                WaypointManager.INSTANCE.save();
                return;
            }
            bRight -= txtW + 4;

            // ON/OFF
            int onW = 36;
            if (inRect(mx, my, bRight - onW, by1, bRight, by2)) {
                wp.setEnabled(!wp.isEnabled());
                WaypointManager.INSTANCE.save();
                return;
            }
        }
    }

    private boolean inRect(int mx, int my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getDWheel();
        int max = Math.max(0, WaypointManager.INSTANCE.getWaypoints().size() - VISIBLE_ENTRIES);
        if (wheel < 0 && scrollOffset < max) scrollOffset++;
        else if (wheel > 0 && scrollOffset > 0) scrollOffset--;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(new GuiWaypointEdit(this, null, -1));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) this.mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
