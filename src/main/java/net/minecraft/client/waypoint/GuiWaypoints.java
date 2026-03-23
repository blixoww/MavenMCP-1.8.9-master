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
    private static final int ENTRY_HEIGHT = 38;
    private static final int VISIBLE_ENTRIES = 7;

    @Override
    public void initGui() {
        this.buttonList.clear();
        // Bouton Nouveau Waypoint personnalisé (style plat vert, plus large)
        this.buttonList.add(new GuiButton(0,
                this.width / 2 - 100, this.height - 38, 200, 24,
                "\u00a7l+ NOUVEAU WAYPOINT") {
            @Override
            public void drawButton(Minecraft mc, int mouseX, int mouseY) {
                if (this.visible) {
                    // Couleurs Vertes
                    int bgNorm = 0x90228822;
                    int bgHov  = 0xD033AA33;
                    int border = 0xFF55FF55;
                    
                    GuiWaypoints.this.drawBtn(mouseX, mouseY, 
                            this.xPosition, this.yPosition, 
                            this.xPosition + this.width, this.yPosition + this.height, 
                            bgNorm, bgHov, border, this.displayString);
                }
            }
        });
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
                    "\u00a77Aucun waypoint.", this.width / 2, this.height / 2 - 20, 0x888888);
            this.drawCenteredString(this.fontRendererObj,
                    "\u00a78(Cliquez sur le bouton vert pour commencer)", this.width / 2, this.height / 2, 0x666666);
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
                        this.height - 52, 0xAAAAAA);
            }
        }

        drawHorizontalLine(10, this.width - 10, this.height - 45, 0x55FFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ── Dessin d'une entrée ─────────────────────────────────────────────────

    private void drawEntry(int mx, int my, Waypoint wp, int idx, int y) {
        int x  = 10;
        int x2 = this.width - 10;
        int yb = y + ENTRY_HEIGHT - 2;

        // Fond alternant
        drawRect(x, y, x2, yb, idx % 2 == 0 ? 0x1AFFFFFF : 0x0DFFFFFF);

        // Colonne 1: Case à cocher "Activé"
        int checkX1 = x + 5, checkY1 = y + (ENTRY_HEIGHT / 2) - 6, checkX2 = checkX1 + 12, checkY2 = checkY1 + 12;
        boolean checkHov = inRect(mx, my, checkX1, checkY1, checkX2, checkY2);
        drawRect(checkX1, checkY1, checkX2, checkY2, checkHov ? 0xFF777777 : 0xFF555555);
        drawBorder(checkX1, checkY1, checkX2, checkY2, 0xFF999999);
        if (wp.isEnabled()) {
            this.drawCenteredString(this.fontRendererObj, "\u00a7a\u2714", (checkX1 + checkX2) / 2, checkY1 + 2, 0xFFFFFF);
        }

        // Colonne 2: Carré de couleur + Nom + Coordonnées
        int leftColX = checkX2 + 8;
        int wpRgb = 0xFF000000 | (wp.getColorR() << 16) | (wp.getColorG() << 8) | wp.getColorB();
        drawRect(leftColX, y + 4, leftColX + 28, yb - 4, wpRgb);
        drawBorder(leftColX, y + 4, leftColX + 28, yb - 4, 0x99FFFFFF);

        String nameColor = wp.isEnabled() ? "\u00a7f" : "\u00a78";
        this.fontRendererObj.drawStringWithShadow(nameColor + wp.getName(), leftColX + 34, y + 6, 0xFFFFFF);
        this.fontRendererObj.drawString("\u00a77" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ(), leftColX + 34, y + 18, 0x888888);

        // Boutons à droite
        int bRight = x2 - 4;

        // Bouton Supprimer
        int delW = 30;
        drawBtn(mx, my, bRight - delW, y + 5, bRight, yb - 5, 0x80881111, 0xBBCC1111, 0xFFFF3333, "\u00a7c\u2716"); // Croix
        bRight -= delW + 4;

        // Bouton Modifier
        int editW = 60;
        drawBtn(mx, my, bRight - editW, y + 5, bRight, yb - 5, 0x80116688, 0xBB1188AA, 0xFF33CCFF, "\u00a7b\u270E Modifier"); // Crayon
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
        int startY = 25;
        int x = 10;
        int x2 = this.width - 10;

        for (int i = scrollOffset; i < wps.size() && i < scrollOffset + VISIBLE_ENTRIES; i++) {
            Waypoint wp = wps.get(i);
            int y  = startY + (i - scrollOffset) * ENTRY_HEIGHT;
            int yb = y + ENTRY_HEIGHT - 2;

            // Clic sur la case à cocher
            int checkX1 = x + 5, checkY1 = y + (ENTRY_HEIGHT / 2) - 6, checkX2 = checkX1 + 12, checkY2 = checkY1 + 12;
            if (inRect(mx, my, checkX1, checkY1, checkX2, checkY2)) {
                wp.setEnabled(!wp.isEnabled());
                WaypointManager.INSTANCE.save();
                return;
            }

            // Clic sur les boutons
            int bRight = x2 - 4;

            // Supprimer
            int delW = 30;
            if (inRect(mx, my, bRight - delW, y + 5, bRight, yb - 5)) {
                WaypointManager.INSTANCE.removeWaypoint(i);
                if (scrollOffset > 0 && scrollOffset >= wps.size() - 1) scrollOffset--;
                return;
            }
            bRight -= delW + 4;

            // Modifier
            int editW = 60;
            if (inRect(mx, my, bRight - editW, y + 5, bRight, yb - 5)) {
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
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) this.mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
