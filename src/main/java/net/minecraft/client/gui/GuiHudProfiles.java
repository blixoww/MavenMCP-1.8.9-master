package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ui.HudProfileManager;
import net.minecraft.client.gui.ui.UIManager;
import net.minecraft.client.gui.ui.UITheme;
import net.minecraft.util.MathHelper;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Gestionnaire de profils HUD — interface sobre et compacte.
 *
 * Layout d'une carte (CARD_H = 44 px):
 *  - Badge (numéro)
 *  - Nom du profil (+ état)
 *  - Rangée d'actions : Sauver | Charger | Supprimer | Restaurer
 *
 * Les boutons ont des largeurs fixes pour éviter tout débordement.
 */
public class GuiHudProfiles extends GuiScreen {

    private final GuiScreen parent;

    // --- Palette -------------------------------------------------
    private static final int COL_BG_OVERLAY  = 0xA8050303;
    private static final int COL_PANEL_BG    = 0xF20A0808;  // Deep dark panel
    private static final int COL_PANEL_BRD   = 0x2BFFFFFF;
    private static final int COL_ACCENT_LINE = 0xFFE02828;  // Vibrant red top border
    private static final int COL_ACCENT      = 0xFFE02828;  // Vibrant red
    private static final int COL_ACCENT_DIM  = 0xFF6E1212;  // Dim red detail
    private static final int COL_CARD_BG     = 0xFF0D0808;
    private static final int COL_CARD_HOV    = 0xFF161010;
    private static final int COL_CARD_ACT    = 0xFF1A0808;  // Dark red for active card
    private static final int COL_CARD_BRD    = 0x18FFFFFF;
    private static final int COL_CARD_BRD_A  = 0xFFE02828;  // Red border for active card
    private static final int COL_TXT_PRI     = 0xFFF2F2F2;
    private static final int COL_TXT_SEC     = 0xFF8A9AB0;
    private static final int COL_TXT_MUT     = 0xFF445060;
    private static final int COL_SAVE        = 0xFF27AE60;
    private static final int COL_LOAD        = 0xFF2980B9;
    private static final int COL_DEL         = 0xFFCC3322;
    private static final int COL_RES         = 0xFFE07822;

    // --- Géométrie ------------------------------------------------
    // Largeur du panneau — calculée dynamiquement dans computePanelW()
    // pour s'adapter à la résolution (min 300, max 460, environ 78% de la largeur écran)
    private int panelW = 400;

    private static final int CARD_H    = 46;   // hauteur d'une carte
    private static final int CARD_GAP  = 6;    // espace entre cartes
    private static final int PAD       = 10;   // padding panneau
    private static final int HDR_H     = 30;   // hauteur zone titre
    private static final int FTR_H     = 26;   // hauteur zone footer
    private static final int BTN_H     = 15;   // hauteur des boutons d'action
    private static final int BTN_SAVE  = 46;   // largeur fixe bouton Sauver
    private static final int BTN_LOAD  = 46;   // largeur fixe bouton Charger
    private static final int BTN_RELOAD = 60;  // largeur fixe bouton Recharger (profil actif)
    private static final int BTN_DEL   = 20;   // largeur fixe bouton Supprimer (carré)
    private static final int BTN_CONF  = 58;   // largeur bouton "Sur ?"
    private static final int BTN_RES   = 54;   // largeur bouton Restaurer
    private static final int BTN_GAP   = 4;    // espace entre boutons
    private static final int BADGE_S   = 22;   // taille du badge numéro

    // --- État ----------------------------------------------------
    private float openAnim = 0f;
    private long  lastTime = -1L;

    private int    renamingSlot  = -1;
    private String renameBuffer  = "";

    private int  confirmDeleteSlot = -1;
    private long confirmDeleteTime = 0L;

    private String toastMsg  = "";
    private long   toastTime = 0L;
    private int    toastCol  = COL_SAVE;

    public GuiHudProfiles(GuiScreen parent) {
        this.parent = parent;
    }

    // --- Lifecycle ------------------------------------------------

    @Override
    public void initGui() {
        this.lastTime = -1L;
        this.openAnim = 0f;
        this.confirmDeleteSlot = -1;
        Keyboard.enableRepeatEvents(true);
        UIManager.getInstance().setEditorActive(true);
        panelW = computePanelW();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        UIManager.getInstance().setEditorActive(false);
    }

    /** Largeur du panneau adaptée à l'écran (300-460). */
    private int computePanelW() {
        return Math.max(300, Math.min(460, (int)(this.width * 0.78f)));
    }

    // --- Helpers layout ------------------------------------------

    /** Hauteur totale du panneau. */
    private int panelH() {
        int n = HudProfileManager.MAX_PROFILES;
        return PAD + HDR_H + PAD
             + n * CARD_H + (n - 1) * CARD_GAP
             + PAD + FTR_H + PAD;
    }

    /** X gauche du panneau (centré). */
    private int panelX() {
        return (this.width - panelW) / 2;
    }

    /** Y haut du panneau (centré, clampé). */
    private int panelY(int panelH, float ease) {
        int raw = (this.height - panelH) / 2;
        raw = Math.max(4, Math.min(this.height - panelH - 4, raw));
        return raw + (int)((1f - ease) * 12);
    }

    /** Y de début de la première carte. */
    private int cardsStartY(int panelY) {
        return panelY + PAD + HDR_H + PAD;
    }

    // --- Rendu principal ----------------------------------------

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime >= 0) {
            float dt = (now - lastTime) / 1000f;
            openAnim = MathHelper.clamp_float(openAnim + dt * 6f, 0f, 1f);
        }
        lastTime = now;
        float ease = openAnim * (2f - openAnim); // ease-out

        // Fond sombre
        Gui.drawRect(0, 0, this.width, this.height, blendAlpha(COL_BG_OVERLAY, ease));

        // Widgets HUD en arrière-plan (preview)
        UIManager.getInstance().renderAll(mouseX, mouseY, partialTicks);

        HudProfileManager pm = HudProfileManager.getInstance();
        if (confirmDeleteSlot >= 0 && now - confirmDeleteTime > 3000L) confirmDeleteSlot = -1;

        panelW = computePanelW();
        int pH  = panelH();
        int pX  = panelX();
        int pY  = panelY(pH, ease);
        int csY = cardsStartY(pY);

        GlStateManager.enableBlend();

        // Panneau principal
        GuiRenderUtils.drawShadow(pX, pY, panelW, pH, 8, (int)(ease * 80));
        Gui.drawRect(pX, pY, pX + panelW, pY + pH, COL_PANEL_BG);
        GuiRenderUtils.drawRectOutline(pX, pY, panelW, pH, UITheme.primary(0x2B));
        // Top accent (2px) + léger glow
        Gui.drawRect(pX, pY, pX + panelW, pY + 2, UITheme.getPrimary());
        GuiRenderUtils.drawGradientRect(pX, pY + 2, pX + panelW, pY + 8, UITheme.primary(0x18), 0x00000000);
        // Gradient header background
        GuiRenderUtils.drawGradientRect(pX, pY + 2, pX + panelW, pY + PAD + HDR_H, 0xFF0E0606, COL_PANEL_BG);

        // En-tête
        drawHeader(pX, pY + PAD, pm);

        // Séparateur sous l'en-tête avec glow subtil
        GuiRenderUtils.drawGradientRect(pX + PAD, pY + PAD + HDR_H - 2, pX + panelW - PAD, pY + PAD + HDR_H, 0x00000000, 0x20FFFFFF);
        Gui.drawRect(pX + PAD, pY + PAD + HDR_H, pX + panelW - PAD, pY + PAD + HDR_H + 1, 0x44FFFFFF);

        // Cartes
        for (int i = 0; i < HudProfileManager.MAX_PROFILES; i++) {
            drawCard(i, pX + PAD, csY + i * (CARD_H + CARD_GAP),
                     panelW - PAD * 2, mouseX, mouseY, pm);
        }

        // Bouton retour (footer)
        int fY  = csY + HudProfileManager.MAX_PROFILES * (CARD_H + CARD_GAP) - CARD_GAP + PAD;
        int bW  = 80;
        int bX  = pX + (panelW - bW) / 2;
        boolean hBack = inRect(mouseX, mouseY, bX, fY, bW, BTN_H + 2);
        drawSmallBtn(bX, fY, bW, BTN_H + 2, "\u2190 Retour", hBack, UITheme.getPrimary());

        // Toast
        drawToast(now, pY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // --- En-tête -----------------------------------------------

    private void drawHeader(int pX, int hY, HudProfileManager pm) {
        // Simple left accent bar (2px)
        Gui.drawRect(pX + PAD, hY + 5, pX + PAD + 2, hY + HDR_H - 5, UITheme.getPrimary());

        // Two-tone title: first letter primary, rest secondary
        String fullHdr = "PROFILS HUD";
        int ty = hY + (HDR_H - 8) / 2;
        int hx = pX + PAD + 7;
        fontRendererObj.drawStringWithShadow(fullHdr.substring(0, 1), hx, ty, UITheme.getPrimary());
        fontRendererObj.drawStringWithShadow(fullHdr.substring(1), hx + fontRendererObj.getStringWidth(fullHdr.substring(0, 1)), ty, UITheme.getSecondary());

        int active = pm.getActiveProfile();
        String sub = active >= 0
            ? "\u00A77Actif : \u00A7b" + pm.getProfileName(active)
            : "\u00A77Aucun profil actif";
        int subX = pX + panelW - PAD - fontRendererObj.getStringWidth(sub);
        fontRendererObj.drawStringWithShadow(sub, subX, ty, COL_TXT_SEC);
    }

    // --- Carte --------------------------------------------------

    private void drawCard(int slot, int x, int y, int w,
                          int mx, int my, HudProfileManager pm) {
        boolean active  = pm.getActiveProfile() == slot;
        boolean used    = pm.isSlotUsed(slot);
        boolean hovCard = inRect(mx, my, x, y, w, CARD_H);

        // Fond
        if (active) {
            GuiRenderUtils.drawGradientRect(x, y, x + w, y + CARD_H, 0xFF180808, COL_CARD_ACT);
        } else {
            Gui.drawRect(x, y, x + w, y + CARD_H, hovCard ? COL_CARD_HOV : COL_CARD_BG);
        }
        GuiRenderUtils.drawRectOutline(x, y, w, CARD_H, active ? UITheme.getPrimary() : COL_CARD_BRD);

        // Left accent stripe (2px) for active card only
        if (active) {
            Gui.drawRect(x, y + 1, x + 2, y + CARD_H - 1, UITheme.getPrimary());
        }

        // Ligne haute : badge + nom + état
        int topRowY = y + 6;
        // Badge
        int bx = x + 7, by = y + (CARD_H - BADGE_S) / 2;
        if (active) {
            Gui.drawRect(bx, by, bx + BADGE_S, by + BADGE_S, 0xFF300808);
            GuiRenderUtils.drawRectOutline(bx, by, BADGE_S, BADGE_S, UITheme.getPrimary());
        } else {
            Gui.drawRect(bx, by, bx + BADGE_S, by + BADGE_S, used ? 0x18FFFFFF : 0x0AFFFFFF);
            GuiRenderUtils.drawRectOutline(bx, by, BADGE_S, BADGE_S, used ? 0x22FFFFFF : 0x10FFFFFF);
        }
        String num = String.valueOf(slot + 1);
        fontRendererObj.drawString(num,
            bx + (BADGE_S - fontRendererObj.getStringWidth(num)) / 2,
            by + (BADGE_S - 8) / 2, active ? 0xFFFF8888 : (used ? 0xFFAAAAAA : 0xFF556066));

        // Nom / champ de renommage
        int nameX = x + 7 + BADGE_S + 7;
        int maxNameW = w - (BADGE_S + 16) - computeActionsWidth(used, active, slot) - 6;

        if (renamingSlot == slot) {
            String cur = (System.currentTimeMillis() / 500 % 2 == 0) ? "|" : "";
            String disp = renameBuffer + cur;
            int fieldW = Math.min(maxNameW, 110);
            Gui.drawRect(nameX - 1, topRowY - 1, nameX + fieldW + 1, topRowY + 10, 0x18000000);
            GuiRenderUtils.drawRectOutline(nameX - 1, topRowY - 1, fieldW + 2, 11, UITheme.getPrimary());
            fontRendererObj.drawString(clipText(disp, fieldW), nameX + 1, topRowY, COL_TXT_PRI);
        } else {
            // Nom
            String name = used ? pm.getProfileName(slot) : "Vide";
            int nameCol = active ? 0xFFFF8888 : (used ? COL_TXT_PRI : COL_TXT_MUT);
            fontRendererObj.drawString(clipText(name, maxNameW), nameX, topRowY, nameCol);

            // Crayon (renommage) juste après le nom si utilisé
            if (used) {
                int clippedW = fontRendererObj.getStringWidth(clipText(name, maxNameW));
                int penX = nameX + clippedW + 4;
                boolean hovPen = inRect(mx, my, penX - 1, topRowY - 1, 11, 11);
                fontRendererObj.drawString("\u270E", penX, topRowY,
                    hovPen ? UITheme.getPrimary() : COL_TXT_MUT);
            }

            // État (2e ligne)
            String st = active ? "\u00A7aACTIF" : (used ? pm.getProfileDescription(slot) : "");
            if (!st.isEmpty()) fontRendererObj.drawString(st, nameX, topRowY + 11, COL_TXT_SEC);
        }

        // Boutons d'action (alignés à droite, milieu vertical)
        drawCardActions(slot, x, y, w, mx, my, pm, used, active);
    }

    /**
     * Largeur totale occupée par les boutons d'action d'une carte.
     * Utilisé pour calculer l'espace disponible pour le texte.
     */
    private int computeActionsWidth(boolean used, boolean active, int slot) {
        int total = 0;
        if (!used && (slot == 0 || slot == 1)) {
            total += BTN_RES + BTN_GAP;
        } else {
            // Sauver toujours présent
            total += BTN_SAVE + BTN_GAP;
            if (active && used)  total += BTN_RELOAD + BTN_GAP;  // Recharger pour le profil actif
            if (used && !active) total += BTN_LOAD + BTN_GAP;
            if (used) total += (confirmDeleteSlot == slot ? BTN_CONF : BTN_DEL) + BTN_GAP;
        }
        return total + 4; // marge droite
    }

    private void drawCardActions(int slot, int cx, int cy, int cw,
                                  int mx, int my,
                                  HudProfileManager pm, boolean used, boolean active) {
        // Milieu vertical dans la carte
        int btnY = cy + (CARD_H - BTN_H) / 2;
        // Curseur partant du bord droit
        int rx = cx + cw - 6;

        if (!used && (slot == 0 || slot == 1)) {
            // Cas emplacement vide avec profil par défaut disponible
            rx -= BTN_RES;
            boolean h = inRect(mx, my, rx, btnY, BTN_RES, BTN_H);
            drawSmallBtn(rx, btnY, BTN_RES, BTN_H, "Restaurer", h, COL_RES);
        } else {
            // Bouton supprimer / confirmer
            if (used) {
                boolean isConf = confirmDeleteSlot == slot;
                int dw = isConf ? BTN_CONF : BTN_DEL;
                rx -= dw;
                boolean h = inRect(mx, my, rx, btnY, dw, BTN_H);
                drawSmallBtn(rx, btnY, dw, BTN_H,
                    isConf ? "Sur ?" : "\u2715", h, COL_DEL);
                rx -= BTN_GAP;
            }
            // Bouton charger (profil non actif) ou Recharger (profil actif)
            if (active && used) {
                rx -= BTN_RELOAD;
                boolean h = inRect(mx, my, rx, btnY, BTN_RELOAD, BTN_H);
                drawSmallBtn(rx, btnY, BTN_RELOAD, BTN_H, "\u21BA Recharger", h, COL_LOAD);
                rx -= BTN_GAP;
            } else if (used && !active) {
                rx -= BTN_LOAD;
                boolean h = inRect(mx, my, rx, btnY, BTN_LOAD, BTN_H);
                drawSmallBtn(rx, btnY, BTN_LOAD, BTN_H, "Charger", h, COL_LOAD);
                rx -= BTN_GAP;
            }
            // Bouton sauver (toujours)
            rx -= BTN_SAVE;
            boolean h = inRect(mx, my, rx, btnY, BTN_SAVE, BTN_H);
            drawSmallBtn(rx, btnY, BTN_SAVE, BTN_H, "Sauver", h, COL_SAVE);
        }
    }

    // --- Toast ---------------------------------------------------

    private void drawToast(long now, int pY) {
        if (now - toastTime >= 2200L) return;
        float t = 1f - (float)(now - toastTime) / 2200f;
        int a = (int)(t * 200);
        String msg = toastMsg;
        int tw = fontRendererObj.getStringWidth(msg) + 16;
        int tx = (this.width - tw) / 2;
        int ty = pY - 20;
        if (ty < 2) ty = 2;
        Gui.drawRect(tx, ty, tx + tw, ty + 14, (a << 24) | (COL_PANEL_BG & 0x00FFFFFF));
        GuiRenderUtils.drawRectOutline(tx, ty, tw, 14, (a << 24) | (toastCol & 0xFFFFFF));
        fontRendererObj.drawString(msg, tx + 8, ty + 3, (a << 24) | (toastCol & 0xFFFFFF));
    }

    // --- Bouton générique compact --------------------------------

    private void drawSmallBtn(int x, int y, int w, int h,
                               String text, boolean hov, int col) {
        int bgA  = hov ? 0x55 : 0x18;
        int brdA = hov ? 0xBB : 0x38;
        Gui.drawRect(x, y, x + w, y + h, (bgA << 24) | (col & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(x, y, w, h, (brdA << 24) | (col & 0xFFFFFF));
        int tc = hov ? 0xFFFFFFFF : ((0xBB << 24) | (col & 0xFFFFFF));

        // Le glyphe Unicode ✕ varie selon le renderer; on dessine une croix pixel-perfect centrée.
        if ("\u2715".equals(text)) {
            int cx = x + w / 2;
            int cy = y + h / 2;
            for (int i = -3; i <= 3; i++) {
                Gui.drawRect(cx + i, cy + i, cx + i + 1, cy + i + 1, tc);
                Gui.drawRect(cx + i, cy - i, cx + i + 1, cy - i + 1, tc);
            }
            return;
        }

        String clipped = clipText(text, w - 4);
        int textW = fontRendererObj.getStringWidth(clipped);
        int tx = x + (w - textW) / 2;
        int ty = y + (h - 8) / 2;
        fontRendererObj.drawString(clipped, tx, ty, tc);
    }

    // --- Utilitaires --------------------------------------------

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Tronque le texte avec "..." pour ne pas dépasser maxW pixels. */
    private String clipText(String s, int maxW) {
        if (fontRendererObj.getStringWidth(s) <= maxW) return s;
        String ellipsis = "...";
        int ew = fontRendererObj.getStringWidth(ellipsis);
        while (s.length() > 1 && fontRendererObj.getStringWidth(s) + ew > maxW)
            s = s.substring(0, s.length() - 1);
        return s + ellipsis;
    }

    /** Applique un alpha animé à une couleur ARGB. */
    private int blendAlpha(int col, float t) {
        int baseA = (col >> 24) & 0xFF;
        int a = (int)(baseA * t);
        return (a << 24) | (col & 0x00FFFFFF);
    }

    private void showToast(String msg, int col) {
        toastMsg  = msg;
        toastTime = Minecraft.getSystemTime();
        toastCol  = col;
    }

    // --- Input --------------------------------------------------

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;
        HudProfileManager pm = HudProfileManager.getInstance();

        panelW = computePanelW();
        int pH  = panelH();
        int pX  = panelX();
        // Même logique que drawScreen mais sans l'offset d'animation (openAnim = 1 ici normalement)
        int rawY = (this.height - pH) / 2;
        rawY = Math.max(4, Math.min(this.height - pH - 4, rawY));
        int pY  = rawY;
        int csY = cardsStartY(pY);

        // Bouton retour (footer)
        int fY = csY + HudProfileManager.MAX_PROFILES * (CARD_H + CARD_GAP) - CARD_GAP + PAD;
        int bW = 80, bX = pX + (panelW - bW) / 2;
        if (inRect(mx, my, bX, fY, bW, BTN_H + 2)) {
            this.mc.displayGuiScreen(parent); return;
        }

        // Fermeture renommage si clic hors du champ
        if (renamingSlot >= 0) {
            int fieldX = pX + PAD + BADGE_S + 18;  // cx + 7 + BADGE_S + 7 - 1 ≈ PAD + BADGE_S + 13
            int fieldY = csY + renamingSlot * (CARD_H + CARD_GAP) + 5;
            if (!inRect(mx, my, fieldX - 1, fieldY - 1, 112, 11)) finishRenaming();
            return;
        }

        // Cartes
        for (int i = 0; i < HudProfileManager.MAX_PROFILES; i++) {
            int cy = csY + i * (CARD_H + CARD_GAP);
            int cx = pX + PAD;
            int cw = panelW - PAD * 2;
            if (!inRect(mx, my, cx, cy, cw, CARD_H)) continue;

            boolean used   = pm.isSlotUsed(i);
            boolean active = pm.getActiveProfile() == i;

            int btnY2 = cy + (CARD_H - BTN_H) / 2;
            int rx = cx + cw - 6;

            if (!used && (i == 0 || i == 1)) {
                rx -= BTN_RES;
                if (inRect(mx, my, rx, btnY2, BTN_RES, BTN_H)) {
                    if (i == 0) pm.initDefaultPvPProfile(); else pm.initDefaultExplorationProfile();
                    showToast("Profil restauré", COL_RES); return;
                }
            } else {
                // Supprimer
                if (used) {
                    boolean isConf = confirmDeleteSlot == i;
                    int dw = isConf ? BTN_CONF : BTN_DEL;
                    rx -= dw;
                    if (inRect(mx, my, rx, btnY2, dw, BTN_H)) {
                        if (isConf) { pm.deleteSlot(i); confirmDeleteSlot = -1; showToast("Profil supprimé", COL_DEL); }
                        else        { confirmDeleteSlot = i; confirmDeleteTime = Minecraft.getSystemTime(); }
                        return;
                    }
                    rx -= BTN_GAP;
                }
                // Charger / Recharger
                if (active && used) {
                    rx -= BTN_RELOAD;
                    if (inRect(mx, my, rx, btnY2, BTN_RELOAD, BTN_H)) {
                        pm.loadFromSlot(i); confirmDeleteSlot = -1;
                        showToast("\"" + pm.getProfileName(i) + "\" rechargé", COL_LOAD); return;
                    }
                    rx -= BTN_GAP;
                } else if (used && !active) {
                    rx -= BTN_LOAD;
                    if (inRect(mx, my, rx, btnY2, BTN_LOAD, BTN_H)) {
                        pm.loadFromSlot(i); confirmDeleteSlot = -1;
                        showToast("\"" + pm.getProfileName(i) + "\" chargé", COL_LOAD); return;
                    }
                    rx -= BTN_GAP;
                }
                // Sauver
                rx -= BTN_SAVE;
                if (inRect(mx, my, rx, btnY2, BTN_SAVE, BTN_H)) {
                    pm.saveToSlot(i); confirmDeleteSlot = -1;
                    showToast("\"" + pm.getProfileName(i) + "\" sauvegardé", COL_SAVE); return;
                }
            }

            // Crayon renommage
            if (used) {
                int nameX  = cx + 7 + BADGE_S + 7;
                String name = pm.getProfileName(i);
                int maxNW  = cw - (BADGE_S + 16) - computeActionsWidth(used, active, i) - 6;
                String clipped = clipText(name, maxNW);
                int cw2    = fontRendererObj.getStringWidth(clipped);
                int penX   = nameX + cw2 + 4;
                int topRowY = cy + 6;
                if (inRect(mx, my, penX - 1, topRowY - 1, 11, 11) && renamingSlot == -1) {
                    renamingSlot = i; renameBuffer = name; return;
                }
                // Clic sur le texte du nom aussi
                if (inRect(mx, my, nameX, topRowY, cw2, 9) && renamingSlot == -1) {
                    renamingSlot = i; renameBuffer = name; return;
                }
            }
        }
        confirmDeleteSlot = -1;
    }

    private void finishRenaming() {
        if (renamingSlot >= 0) {
            String trimmed = renameBuffer.trim();
            if (!trimmed.isEmpty()) {
                HudProfileManager.getInstance().setProfileName(renamingSlot, trimmed);
                HudProfileManager.getInstance().save();
                showToast("Nom mis à jour", COL_SAVE);
            }
            renamingSlot = -1;
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (renamingSlot >= 0) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                finishRenaming();
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!renameBuffer.isEmpty())
                    renameBuffer = renameBuffer.substring(0, renameBuffer.length() - 1);
            } else if (renameBuffer.length() < 18 && c >= 32) {
                renameBuffer += c;
            }
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) this.mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
