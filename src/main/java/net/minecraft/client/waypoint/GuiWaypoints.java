package net.minecraft.client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.List;

/**
 * GUI listant tous les waypoints.
 * Chaque ligne affiche : couleur, nom, coords, et des boutons d'action.
 */
public class GuiWaypoints extends GuiScreen {

    private int scrollOffset = 0;
    private static final int ENTRY_HEIGHT   = 32;
    private static final int VISIBLE_ENTRIES = 8;
    private static final int LIST_TOP        = 24;

    // Couleurs UI
    private static final int COL_BG_EVEN = 0x18FFFFFF;
    private static final int COL_BG_ODD  = 0x0CFFFFFF;
    private static final int COL_BORDER  = 0x25FFFFFF;

    @Override
    public void initGui() {
        this.buttonList.clear();

        int bY = this.height - 30;
        // Bouton Nouveau Waypoint (centré, large)
        this.buttonList.add(new GuiButton(0,
                this.width / 2 - 115, bY, 180, 22,
                "\u00a7l\u00a7a+ Nouveau Waypoint") {
            @Override
            public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                if (this.visible) {
                    GuiWaypoints.this.drawBtn(mouseX, mouseY,
                            this.xPosition, this.yPosition,
                            this.xPosition + this.width, this.yPosition + this.height,
                            0x90228822, 0xD033AA33, 0xFF55FF55, this.displayString);
                }
            }
        });

        // Bouton Paramètres
        this.buttonList.add(new GuiButton(1,
                this.width / 2 + 70, bY, 60, 22,
                "\u00a77\u2699 Options") {
            @Override
            public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                if (this.visible) {
                    GuiWaypoints.this.drawBtn(mouseX, mouseY,
                            this.xPosition, this.yPosition,
                            this.xPosition + this.width, this.yPosition + this.height,
                            0x80334455, 0xBB445566, 0xFF8899BB, this.displayString);
                }
            }
        });
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        // ── En-tête ──────────────────────────────────────────────────────────
        this.drawCenteredString(this.fontRendererObj,
                "\u00a7l\u00a76\u2605 Waypoints \u2605", this.width / 2, 7, 0xFFFFFF);
        drawHorizontalLine(8, this.width - 8, 19, 0x44FFFFFF);

        List<Waypoint> wps = WaypointManager.INSTANCE.getWaypoints();
        int listBottom = this.height - 36;

        if (wps.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj,
                    "\u00a77Aucun waypoint enregistr\u00e9.", this.width / 2, this.height / 2 - 16, 0x888888);
            this.drawCenteredString(this.fontRendererObj,
                    "\u00a78Cliquez sur \u00a7a+ Nouveau Waypoint\u00a78 pour commencer.", this.width / 2, this.height / 2, 0x666666);
        } else {
            for (int i = scrollOffset; i < wps.size() && i < scrollOffset + VISIBLE_ENTRIES; i++) {
                Waypoint wp = wps.get(i);
                int entryY = LIST_TOP + (i - scrollOffset) * ENTRY_HEIGHT;
                if (entryY + ENTRY_HEIGHT > listBottom) break;
                drawEntry(mouseX, mouseY, wp, i, entryY);
            }

            // Indicateur de scroll
            if (wps.size() > VISIBLE_ENTRIES) {
                int shown = Math.min(scrollOffset + VISIBLE_ENTRIES, wps.size());
                String info = "\u00a78" + (scrollOffset + 1) + "\u2013" + shown + " / " + wps.size() + "  \u2195";
                int iw = this.fontRendererObj.getStringWidth(info);
                this.fontRendererObj.drawString(info, this.width / 2 - iw / 2, listBottom - 10, 0xAAAAAA);
            }
        }

        drawHorizontalLine(8, this.width - 8, this.height - 36, 0x44FFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ── Dessin d'une entrée ─────────────────────────────────────────────────

    private void drawEntry(int mx, int my, Waypoint wp, int idx, int y) {
        int x  = 8;
        int x2 = this.width - 8;
        int yb = y + ENTRY_HEIGHT - 1;
        int mid = y + ENTRY_HEIGHT / 2;

        // Fond alternant + bordure basse
        drawRect(x, y, x2, yb, idx % 2 == 0 ? COL_BG_EVEN : COL_BG_ODD);
        drawHorizontalLine(x, x2, yb, COL_BORDER);

        // ── Case à cocher Activé ──────────────────────────────────────────────
        int ck = 11;
        int ckX1 = x + 4, ckY1 = mid - ck/2, ckX2 = ckX1 + ck, ckY2 = ckY1 + ck;
        boolean ckHov = inRect(mx, my, ckX1, ckY1, ckX2, ckY2);
        drawRect(ckX1, ckY1, ckX2, ckY2, ckHov ? 0xFF888888 : 0xFF444444);
        drawBorder(ckX1, ckY1, ckX2, ckY2, 0xFFAAAAAA);
        if (wp.isEnabled()) {
            // Coche verte
            this.drawCenteredString(this.fontRendererObj, "\u00a7a\u2714", (ckX1 + ckX2) / 2, ckY1 + 1, 0xFFFFFF);
        }

        // ── Carré de couleur ──────────────────────────────────────────────────
        int sq = 20;
        int sqX = ckX2 + 6, sqY = mid - sq / 2;
        int wpRgb = 0xFF000000 | (wp.getColorR() << 16) | (wp.getColorG() << 8) | wp.getColorB();
        drawRect(sqX, sqY, sqX + sq, sqY + sq, wpRgb);
        drawBorder(sqX, sqY, sqX + sq, sqY + sq, 0x88FFFFFF);

        // Icône beam (petite flamme bleue si beam actif)
        if (wp.isBeamVisible()) {
            this.fontRendererObj.drawStringWithShadow("\u00a7b\u25b2", sqX + sq / 2 - 3, sqY + sq / 2 - 4, 0xFFFFFF);
        }

        // ── Nom + Coordonnées ─────────────────────────────────────────────────
        int textX = sqX + sq + 6;
        String nameColor = wp.isEnabled() ? "\u00a7f" : "\u00a78";
        this.fontRendererObj.drawStringWithShadow(nameColor + wp.getName(), textX, y + 5, 0xFFFFFF);
        this.fontRendererObj.drawString("\u00a77" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ(),
                textX, y + 17, 0x888888);

        // ── Boutons à droite ──────────────────────────────────────────────────
        int bRight = x2 - 4;
        int bY1 = y + 5, bY2 = y + ENTRY_HEIGHT - 5;

        // Bouton Supprimer
        int delW = 22;
        drawBtn(mx, my, bRight - delW, bY1, bRight, bY2, 0x80881111, 0xBBCC1111, 0xFFFF4444, "\u00a7c\u2716");
        bRight -= delW + 3;

        // Bouton Modifier
        int editW = 55;
        drawBtn(mx, my, bRight - editW, bY1, bRight, bY2, 0x80116688, 0xBB1188AA, 0xFF44CCFF, "\u00a7b\u270E\u00a7f Edit");
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
        drawBorder(x1, y1, x2, y2, hov ? border : (border & 0x66FFFFFF));
        this.drawCenteredString(fontRendererObj, label, (x1 + x2) / 2, y1 + (y2 - y1 - 8) / 2, 0xFFFFFF);
    }

    /** Bordure 1 px. */
    private void drawBorder(int x1, int y1, int x2, int y2, int col) {
        drawHorizontalLine(x1, x2, y1, col);
        drawHorizontalLine(x1, x2, y2, col);
        drawVerticalLine(x1, y1, y2, col);
        drawVerticalLine(x2, y1, y2, col);
    }

    // ── Hit-tests ───────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);

        List<Waypoint> wps = WaypointManager.INSTANCE.getWaypoints();
        int listBottom = this.height - 36;

        for (int i = scrollOffset; i < wps.size() && i < scrollOffset + VISIBLE_ENTRIES; i++) {
            Waypoint wp = wps.get(i);
            int y  = LIST_TOP + (i - scrollOffset) * ENTRY_HEIGHT;
            if (y + ENTRY_HEIGHT > listBottom) break;

            int x  = 8;
            int x2 = this.width - 8;

            // Case à cocher
            int ck = 11;
            int mid = y + ENTRY_HEIGHT / 2;
            int ckX1 = x + 4, ckY1 = mid - ck/2;
            if (inRect(mx, my, ckX1, ckY1, ckX1 + ck, ckY1 + ck)) {
                wp.setEnabled(!wp.isEnabled());
                WaypointManager.INSTANCE.save();
                return;
            }

            // Boutons
            int bRight = x2 - 4;
            int bY1 = y + 5, bY2 = y + ENTRY_HEIGHT - 5;

            // Supprimer
            int delW = 22;
            if (inRect(mx, my, bRight - delW, bY1, bRight, bY2)) {
                WaypointManager.INSTANCE.removeWaypoint(i);
                int max = Math.max(0, WaypointManager.INSTANCE.getWaypoints().size() - VISIBLE_ENTRIES);
                if (scrollOffset > max) scrollOffset = max;
                return;
            }
            bRight -= delW + 3;

            // Modifier
            int editW = 55;
            if (inRect(mx, my, bRight - editW, bY1, bRight, bY2)) {
                this.mc.displayGuiScreen(new GuiWaypointEdit(this, wp, i));
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
        } else if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiWaypointSettings(this));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) this.mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
