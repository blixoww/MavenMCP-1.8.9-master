package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ui.HudProfileManager;
import net.minecraft.client.gui.ui.UIManager;
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
    // Couleurs et constantes utilisées pour l'interface
    private static final int COL_BG_OVERLAY = 0xA0060608;
    private static final int COL_PANEL_BG   = 0xF0101016;
    private static final int COL_PANEL_BRD  = 0x28FFFFFF;
    private static final int COL_ACCENT     = 0xFF4488FF;
    private static final int COL_CARD_BG    = 0xFF13131A;
    private static final int COL_CARD_HOV   = 0xFF181820;
    private static final int COL_CARD_ACT   = 0xFF10101E;
    private static final int COL_CARD_BRD   = 0x18FFFFFF;
    private static final int COL_CARD_BRD_A = 0xFF4488FF;
    private static final int COL_TXT_PRI    = 0xFFDDDDEE;
    private static final int COL_TXT_SEC    = 0xFF888899;
    private static final int COL_TXT_MUT    = 0xFF3A3A4A;
    private static final int COL_SAVE       = 0xFF27AE60;
    private static final int COL_LOAD       = 0xFF2980B9;
    private static final int COL_DEL        = 0xFFC0392B;
    private static final int COL_RES        = 0xFFD35400;

    // --- Géométrie ------------------------------------------------
    // Largeur du panneau — calculée dynamiquement dans computePanelW()
    // pour s'adapter à la résolution (min 300, max 460, environ 78% de la largeur écran)
    private int panelW = 400;

    private static final int CARD_H    = 44;   // hauteur d'une carte
    private static final int CARD_GAP  = 5;    // espace entre cartes
    private static final int PAD       = 10;   // padding panneau
    private static final int HDR_H     = 28;   // hauteur zone titre
    private static final int FTR_H     = 24;   // hauteur zone footer
    private static final int BTN_H     = 13;   // hauteur des boutons d'action
    private static final int BTN_SAVE  = 46;   // largeur fixe bouton Sauver
    private static final int BTN_LOAD  = 46;   // largeur fixe bouton Charger
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
        this.lastTime = Minecraft.getSystemTime();
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
        if (lastTime > 0) {
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
        Gui.drawRect(pX, pY, pX + panelW, pY + pH, COL_PANEL_BG);
        GuiRenderUtils.drawRectOutline(pX, pY, panelW, pH, COL_PANEL_BRD);
        // Trait d'accent en haut
        Gui.drawRect(pX + 1, pY, pX + panelW - 1, pY + 2, COL_ACCENT);

        // En-tête
        drawHeader(pX, pY + PAD, pm);

        // Séparateur sous l'en-tête
        Gui.drawRect(pX + PAD, pY + PAD + HDR_H, pX + panelW - PAD, pY + PAD + HDR_H + 1, COL_PANEL_BRD);

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
        drawSmallBtn(bX, fY, bW, BTN_H + 2, "\u2190 Retour", hBack, COL_ACCENT);

        // Toast
        drawToast(now, pY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // --- En-tête -----------------------------------------------

    private void drawHeader(int pX, int hY, HudProfileManager pm) {
        String title = "Profils HUD";
        fontRendererObj.drawStringWithShadow(title, pX + PAD, hY + 2, 0xFFCCDDFF);

        int active = pm.getActiveProfile();
        String sub = active >= 0
            ? "\u00A77Actif : \u00A7b" + pm.getProfileName(active)
            : "\u00A77Aucun profil actif";
        // Sous-titre aligné à droite dans le header
        int subX = pX + panelW - PAD - fontRendererObj.getStringWidth(sub);
        fontRendererObj.drawStringWithShadow(sub, subX, hY + 2, COL_TXT_SEC);
    }

    // --- Carte --------------------------------------------------

    private void drawCard(int slot, int x, int y, int w,
                          int mx, int my, HudProfileManager pm) {
        boolean active  = pm.getActiveProfile() == slot;
        boolean used    = pm.isSlotUsed(slot);
        boolean hovCard = inRect(mx, my, x, y, w, CARD_H);

        // Fond
        int bg = active ? COL_CARD_ACT : (hovCard ? COL_CARD_HOV : COL_CARD_BG);
        Gui.drawRect(x, y, x + w, y + CARD_H, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, CARD_H, active ? COL_CARD_BRD_A : COL_CARD_BRD);

        // Trait gauche actif
        if (active) Gui.drawRect(x, y + 1, x + 2, y + CARD_H - 1, COL_ACCENT);

        // Ligne haute : badge + nom + état
        int topRowY = y + 6;
        // Badge
        int bx = x + 6, by = y + (CARD_H - BADGE_S) / 2;
        Gui.drawRect(bx, by, bx + BADGE_S, by + BADGE_S,
                     active ? 0xFF1A4080 : (used ? 0x20FFFFFF : 0x0CFFFFFF));
        if (active) GuiRenderUtils.drawRectOutline(bx, by, BADGE_S, BADGE_S, COL_ACCENT);
        String num = String.valueOf(slot + 1);
        fontRendererObj.drawString(num,
            bx + (BADGE_S - fontRendererObj.getStringWidth(num)) / 2,
            by + (BADGE_S - 8) / 2, active ? 0xFFAAD4FF : 0xFF888899);

        // Nom / champ de renommage
        int nameX = x + 6 + BADGE_S + 6;
        int maxNameW = w - (BADGE_S + 14) - computeActionsWidth(used, active, slot) - 6;

        if (renamingSlot == slot) {
            String cur = (System.currentTimeMillis() / 500 % 2 == 0) ? "|" : "";
            String disp = renameBuffer + cur;
            int fieldW = Math.min(maxNameW, 110);
            Gui.drawRect(nameX - 1, topRowY - 1, nameX + fieldW + 1, topRowY + 10, 0x22000000);
            GuiRenderUtils.drawRectOutline(nameX - 1, topRowY - 1, fieldW + 2, 11, COL_ACCENT);
            fontRendererObj.drawString(clipText(disp, fieldW), nameX + 1, topRowY, COL_TXT_PRI);
        } else {
            // Nom
            String name = used ? pm.getProfileName(slot) : "Vide";
            int nameCol = active ? 0xFFAAD4FF : (used ? COL_TXT_PRI : COL_TXT_MUT);
            fontRendererObj.drawString(clipText(name, maxNameW), nameX, topRowY, nameCol);

            // Crayon (renommage) juste après le nom si utilisé
            if (used) {
                int clippedW = fontRendererObj.getStringWidth(clipText(name, maxNameW));
                int penX = nameX + clippedW + 4;
                boolean hovPen = inRect(mx, my, penX - 1, topRowY - 1, 11, 11);
                fontRendererObj.drawString("\u270E", penX, topRowY,
                    hovPen ? COL_ACCENT : COL_TXT_MUT);
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
            // Bouton charger
            if (used && !active) {
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
        int a = (int)(t * 210);
        String msg = toastMsg;
        int tw = fontRendererObj.getStringWidth(msg) + 14;
        int tx = (this.width - tw) / 2;
        int ty = pY - 20;
        if (ty < 2) ty = 2;
        Gui.drawRect(tx, ty, tx + tw, ty + 14, (a << 24) | (COL_PANEL_BG & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(tx, ty, tw, 14, (a << 24) | (toastCol & 0xFFFFFF));
        fontRendererObj.drawString(msg, tx + 7, ty + 3, (a << 24) | (toastCol & 0xFFFFFF));
    }

    // --- Bouton générique compact --------------------------------

    private void drawSmallBtn(int x, int y, int w, int h,
                               String text, boolean hov, int col) {
        int bgA  = hov ? 0x66 : 0x20;
        int brdA = hov ? 0xCC : 0x40;
        Gui.drawRect(x, y, x + w, y + h, (bgA << 24) | (col & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(x, y, w, h, (brdA << 24) | (col & 0xFFFFFF));
        int tc = hov ? 0xFFFFFFFF : ((0xBB << 24) | (col & 0xFFFFFF));
        String clipped = clipText(text, w - 4);
        int textW = fontRendererObj.getStringWidth(clipped);
        int textH = fontRendererObj.FONT_HEIGHT;
        int tx = x + (w - textW) / 2;
        int ty = y + (h - textH) / 2;
        // Petite correction visuelle pour icônes d'un seul caractère (croix, etc.)
        if (clipped.length() == 1) ty += 1;
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
            int fieldX = pX + PAD + BADGE_S + 18;
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
                // Charger
                if (used && !active) {
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
                int nameX  = cx + 6 + BADGE_S + 6;
                String name = pm.getProfileName(i);
                int maxNW  = cw - (BADGE_S + 14) - computeActionsWidth(used, active, i) - 6;
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
