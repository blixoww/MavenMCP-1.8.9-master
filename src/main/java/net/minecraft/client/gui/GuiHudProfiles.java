package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ui.HudProfileManager;
import net.minecraft.client.gui.ui.UIManager;
import net.minecraft.client.gui.ui.UITheme;
import net.minecraft.util.MathHelper;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

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
    private static final int COL_BG_OVERLAY  = 0xB2050303;
    private static final int COL_PANEL_BG    = 0xF50C0909;  // Deep dark panel
    private static final int COL_PANEL_BRD   = 0x2BFFFFFF;
    private static final int COL_ACCENT_LINE = 0xFFE02828;  // Vibrant red top border
    private static final int COL_ACCENT      = 0xFFE02828;  // Vibrant red
    private static final int COL_ACCENT_DIM  = 0xFF6E1212;  // Dim red detail
    private static final int COL_CARD_BG     = 0xFF0E0808;
    private static final int COL_CARD_HOV    = 0xFF1A1010;
    private static final int COL_CARD_ACT    = 0xFF1E0808;  // Dark red for active card
    private static final int COL_CARD_BRD    = 0x20FFFFFF;
    private static final int COL_CARD_BRD_A  = 0xFFE02828;  // Red border for active card
    private static final int COL_TXT_PRI     = 0xFFF4F4F4;
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

    private static final int CARD_H    = 52;   // hauteur d'une carte (2 rangées)
    private static final int CARD_TOP  = 30;   // hauteur de la zone badge/nom
    private static final int CARD_GAP  = 5;    // espace entre cartes
    private static final int PAD       = 10;   // padding panneau
    private static final int HDR_H     = 30;   // hauteur zone titre
    private static final int BTN_H     = 15;   // hauteur des boutons dans le footer
    private static final int BTN_ROW_H = 12;   // hauteur des boutons dans la rangée bas de carte
    private static final int BTN_SAVE  = 46;   // largeur fixe bouton Sauver
    private static final int BTN_LOAD  = 46;   // largeur fixe bouton Charger
    private static final int BTN_RELOAD = 60;  // largeur fixe bouton Recharger (profil actif)
    private static final int BTN_DEL   = 20;   // largeur fixe bouton Supprimer (carré)
    private static final int BTN_CONF  = 58;   // largeur bouton "Sur ?"
    private static final int BTN_RES   = 54;   // largeur bouton Restaurer
    private static final int BTN_GAP   = 4;    // espace entre boutons
    private static final int BADGE_S   = 22;   // taille du badge numéro
    /** Hauteur de la zone footer (bouton Fermer + marges) */
    private static final int FOOTER_H  = BTN_H + PAD * 2; // 35px

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

    /** Scroll vertical de la zone cartes (en pixels). */
    private int scrollY = 0;

    public GuiHudProfiles(GuiScreen parent) {
        this.parent = parent;
    }

    // --- Lifecycle ------------------------------------------------

    @Override
    public void initGui() {
        this.lastTime = -1L;
        this.openAnim = 0f;
        this.confirmDeleteSlot = -1;
        this.scrollY = 0;
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

    /** Hauteur des parties fixes du panel (hors cartes). */
    private int fixedH() {
        return PAD + HDR_H + PAD   // en-tête
             + PAD + FOOTER_H;     // footer (PAD de séparation + zone bouton)
    }

    /** Hauteur totale de toutes les cartes (sans contrainte). */
    private int totalCardsH() {
        int n = HudProfileManager.MAX_PROFILES;
        return n * CARD_H + (n - 1) * CARD_GAP;
    }

    /** Hauteur totale du panneau, plafonnée à la hauteur écran - 12px. */
    private int panelH() {
        int full = fixedH() + totalCardsH();
        int maxH = Math.max(110, this.height - 12);
        return Math.min(full, maxH);
    }

    /** Hauteur de la zone visible des cartes (avec scroll si besoin). */
    private int cardsAreaH() {
        return panelH() - fixedH();
    }

    /** Scroll max possible (0 si tout tient). */
    private int maxScrollY() {
        return Math.max(0, totalCardsH() - cardsAreaH());
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

    /** Y de début de la zone cartes. */
    private int cardsStartY(int panelY) {
        return panelY + PAD + HDR_H + PAD;
    }

    /** Y du bouton Fermer (depuis le bas du panneau). */
    private int footerBtnY(int pY) {
        return pY + panelH() - FOOTER_H + PAD;
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
        int areaH = cardsAreaH();

        GlStateManager.enableBlend();

        // === Panneau principal ===
        GuiRenderUtils.drawShadow(pX, pY, panelW, pH, 12, (int)(ease * 100));
        Gui.drawRect(pX, pY, pX + panelW, pY + pH, COL_PANEL_BG);
        GuiRenderUtils.drawRectOutline(pX, pY, panelW, pH, UITheme.primary(0x35));
        // Top accent (3px) + glow dégradé
        Gui.drawRect(pX, pY, pX + panelW, pY + 3, UITheme.getPrimary());
        GuiRenderUtils.drawGradientRect(pX, pY + 3, pX + panelW, pY + 12, UITheme.primary(0x22), 0x00000000);
        // Gradient header background
        GuiRenderUtils.drawGradientRect(pX, pY + 3, pX + panelW, pY + PAD + HDR_H + 2, 0xFF120808, COL_PANEL_BG);
        // Ligne latérale gauche colorée
        Gui.drawRect(pX, pY + 3, pX + 1, pY + pH, UITheme.primary(0x44));

        // === En-tête ===
        drawHeader(pX, pY + PAD, pm);

        // Séparateur sous l'en-tête
        GuiRenderUtils.drawGradientRect(pX + PAD, pY + PAD + HDR_H - 2, pX + panelW - PAD, pY + PAD + HDR_H, 0x00000000, 0x28FFFFFF);
        Gui.drawRect(pX + PAD, pY + PAD + HDR_H, pX + panelW - PAD, pY + PAD + HDR_H + 1, 0x55FFFFFF);
        GuiRenderUtils.drawGradientRect(pX + PAD, pY + PAD + HDR_H + 1, pX + panelW - PAD, pY + PAD + HDR_H + 4, 0x18000000, 0x00000000);

        // === Zone cartes avec scissor ===
        boolean needsScroll = maxScrollY() > 0;
        // Largeur de la zone cartes (réduite si scrollbar)
        int cardsInnerW = needsScroll ? panelW - PAD * 2 - 6 : panelW - PAD * 2;

        // Activer le scissor pour clipper les cartes
        ScaledResolution sr = new ScaledResolution(this.mc);
        int sf = sr.getScaleFactor();
        int sciX = (pX + PAD) * sf;
        int sciY = this.mc.displayHeight - (csY + areaH) * sf;
        int sciW = cardsInnerW * sf;
        int sciH = areaH * sf;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sciX, sciY, sciW, sciH);

        for (int i = 0; i < HudProfileManager.MAX_PROFILES; i++) {
            int cardY = csY + i * (CARD_H + CARD_GAP) - scrollY;
            drawCard(i, pX + PAD, cardY, cardsInnerW, mouseX, mouseY, pm);
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Dégradés de fondu haut/bas pour indiquer le scroll
        if (scrollY > 0) {
            GuiRenderUtils.drawGradientRect(pX + PAD, csY, pX + PAD + cardsInnerW, csY + 10, 0x60000000, 0x00000000);
        }
        if (scrollY < maxScrollY()) {
            GuiRenderUtils.drawGradientRect(pX + PAD, csY + areaH - 10, pX + PAD + cardsInnerW, csY + areaH, 0x00000000, 0x60000000);
        }

        // === Scrollbar ===
        if (needsScroll) {
            int sbX = pX + panelW - PAD - 3;
            float ratio = (float) cardsAreaH() / totalCardsH();
            int thumbH = Math.max(16, (int)(ratio * areaH));
            int thumbY = csY + (maxScrollY() > 0
                    ? (int)((float) scrollY / maxScrollY() * (areaH - thumbH))
                    : 0);
            Gui.drawRect(sbX, csY, sbX + 3, csY + areaH, 0x15FFFFFF);
            GuiRenderUtils.drawGradientRect(sbX, thumbY, sbX + 3, thumbY + thumbH, 0x66FFFFFF, 0x44FFFFFF);
        }

        // Séparateur footer
        GuiRenderUtils.drawGradientRect(pX + PAD * 2, pY + pH - FOOTER_H - 1, pX + panelW - PAD * 2, pY + pH - FOOTER_H, 0x00000000, 0x1EFFFFFF);
        Gui.drawRect(pX + PAD * 2, pY + pH - FOOTER_H, pX + panelW - PAD * 2, pY + pH - FOOTER_H + 1, 0x22FFFFFF);

        // === Bouton Fermer — discret (link style) ===
        int fY  = footerBtnY(pY);
        String closeLabel = "\u2190  Fermer";
        int clW = fontRendererObj.getStringWidth(closeLabel);
        int bW  = clW + 14;
        int bX  = pX + (panelW - bW) / 2;
        boolean hBack = inRect(mouseX, mouseY, bX, fY, bW, BTN_H);
        if (hBack) {
            Gui.drawRect(bX, fY, bX + bW, fY + BTN_H, 0x1AFFFFFF);
        }
        // Underline animé
        int ulAlpha = hBack ? 0x88 : 0x30;
        Gui.drawRect(bX + 4, fY + BTN_H, bX + bW - 4, fY + BTN_H + 1, (ulAlpha << 24) | (UITheme.getPrimary() & 0xFFFFFF));
        int closeCol = hBack ? 0xCCFFFFFF : 0x66FFFFFF;
        fontRendererObj.drawString(closeLabel, bX + 7, fY + (BTN_H - 8) / 2, closeCol);

        // Toast
        drawToast(now, pY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int delta = wheel > 0 ? -(CARD_H + CARD_GAP) : (CARD_H + CARD_GAP);
            scrollY = MathHelper.clamp_int(scrollY + delta, 0, maxScrollY());
        }
    }

    // --- En-tête -----------------------------------------------

    private void drawHeader(int pX, int hY, HudProfileManager pm) {
        // Barre accent gauche (3px) + glow dégradé
        Gui.drawRect(pX + PAD, hY + 3, pX + PAD + 3, hY + HDR_H - 3, UITheme.getPrimary());
        GuiRenderUtils.drawGradientRect(pX + PAD + 3, hY + 3, pX + PAD + 14, hY + HDR_H - 3, UITheme.primary(0x66), 0x00000000);

        // Titre bicolore
        String fullHdr = "PROFILS HUD";
        int ty = hY + (HDR_H - 8) / 2 - 3;
        int hx = pX + PAD + 10;
        fontRendererObj.drawStringWithShadow(fullHdr.substring(0, 1), hx, ty, UITheme.getPrimary());
        fontRendererObj.drawStringWithShadow(fullHdr.substring(1), hx + fontRendererObj.getStringWidth(fullHdr.substring(0, 1)), ty, UITheme.getSecondary());

        // Sous-titre : nombre de profils utilisés
        int usedCount = 0;
        for (int i = 0; i < HudProfileManager.MAX_PROFILES; i++) if (pm.isSlotUsed(i)) usedCount++;
        String countStr = "\u00A78\u25A0 " + usedCount + "/" + HudProfileManager.MAX_PROFILES + " profil" + (usedCount > 1 ? "s" : "");
        fontRendererObj.drawString(countStr, hx + 1, ty + 11, 0);

        // Profil actif (droite) — badge pill stylisé
        int active = pm.getActiveProfile();
        if (active >= 0) {
            String activeName = pm.getProfileName(active);
            // Contenu : indicateur vert + "Actif" + nom du profil
            String label   = "\u00A7a\u25CF \u00A77Actif \u00A7f" + activeName;
            int labelW     = fontRendererObj.getStringWidth(label);
            int pillPadH   = 5;
            int pillPadV   = 3;
            int pillW      = labelW + pillPadH * 2;
            int pillH      = 8 + pillPadV * 2;
            int pillX      = pX + panelW - PAD - pillW;
            int pillY      = hY + (HDR_H - pillH) / 2;

            // Fond dégradé rouge sombre
            GuiRenderUtils.drawGradientRect(pillX, pillY, pillX + pillW, pillY + pillH,
                    0xFF1E0808, 0xFF160606);
            // Bordure gauche accent (couleur primaire)
            Gui.drawRect(pillX, pillY + 1, pillX + 2, pillY + pillH - 1, UITheme.getPrimary());
            // Contour léger
            GuiRenderUtils.drawRectOutline(pillX, pillY, pillW, pillH, UITheme.primary(0x66));
            // Highlight supérieur
            Gui.drawRect(pillX + 1, pillY + 1, pillX + pillW - 1, pillY + 2, 0x18FFFFFF);
            // Texte
            fontRendererObj.drawStringWithShadow(label, pillX + pillPadH, pillY + pillPadV, 0);
        } else {
            String sub = "\u00A77Aucun profil actif";
            int subX = pX + panelW - PAD - fontRendererObj.getStringWidth(sub);
            fontRendererObj.drawString(sub, subX, hY + (HDR_H - 8) / 2, COL_TXT_MUT);
        }
    }

    // --- Carte --------------------------------------------------

    private void drawCard(int slot, int x, int y, int w,
                          int mx, int my, HudProfileManager pm) {
        boolean active  = pm.getActiveProfile() == slot;
        boolean used    = pm.isSlotUsed(slot);
        boolean hovCard = inRect(mx, my, x, y, w, CARD_H);

        // Fond
        if (active) {
            GuiRenderUtils.drawGradientRect(x, y, x + w, y + CARD_H, 0xFF200A0A, COL_CARD_ACT);
            GuiRenderUtils.drawGradientRect(x, y, x + 6, y + CARD_H, UITheme.primary(0x18), 0x00000000);
        } else if (hovCard) {
            GuiRenderUtils.drawGradientRect(x, y, x + w, y + CARD_H, COL_CARD_HOV, 0xFF0E0808);
        } else {
            Gui.drawRect(x, y, x + w, y + CARD_H, used ? COL_CARD_BG : 0xFF0A0606);
        }
        GuiRenderUtils.drawRectOutline(x, y, w, CARD_H,
                active ? UITheme.getPrimary() : (hovCard ? 0x44FFFFFF : COL_CARD_BRD));

        // Left accent stripe
        if (active) {
            Gui.drawRect(x, y + 1, x + 3, y + CARD_H - 1, UITheme.getPrimary());
            GuiRenderUtils.drawGradientRect(x + 3, y + 1, x + 10, y + CARD_H - 1, UITheme.primary(0x55), 0x00000000);
        } else if (hovCard && used) {
            Gui.drawRect(x, y + 1, x + 2, y + CARD_H - 1, UITheme.primary(0x88));
        }

        // --- RANGÉE HAUTE : Badge + Nom + État (zone CARD_TOP) ---
        int bx = x + 7, by = y + (CARD_TOP - BADGE_S) / 2;
        if (active) {
            GuiRenderUtils.drawGradientRect(bx, by, bx + BADGE_S, by + BADGE_S, 0xFF3A0808, 0xFF280606);
            GuiRenderUtils.drawRectOutline(bx, by, BADGE_S, BADGE_S, UITheme.getPrimary());
            Gui.drawRect(bx, by, bx + 4, by + 1, UITheme.getPrimary());
            Gui.drawRect(bx, by, bx + 1, by + 4, UITheme.getPrimary());
        } else if (used) {
            Gui.drawRect(bx, by, bx + BADGE_S, by + BADGE_S, 0x22FFFFFF);
            GuiRenderUtils.drawRectOutline(bx, by, BADGE_S, BADGE_S, hovCard ? 0x40FFFFFF : 0x22FFFFFF);
        } else {
            Gui.drawRect(bx, by, bx + BADGE_S, by + BADGE_S, 0x0AFFFFFF);
            GuiRenderUtils.drawRectOutline(bx, by, BADGE_S, BADGE_S, 0x12FFFFFF);
        }
        String num = String.valueOf(slot + 1);
        fontRendererObj.drawString(num,
            bx + (BADGE_S - fontRendererObj.getStringWidth(num)) / 2,
            by + (BADGE_S - 8) / 2,
            active ? 0xFFFF9999 : (used ? 0xFFCCCCCC : 0xFF445566));

        int nameX   = x + 7 + BADGE_S + 7;
        int topRowY = y + (CARD_TOP - 8 * 2 - 3) / 2; // nom+état centrés verticalement dans CARD_TOP
        // Largeur disponible pour le nom (plus besoin d'éviter les boutons)
        int maxNameW = w - (BADGE_S + 16) - 14;

        if (renamingSlot == slot) {
            String cur  = (System.currentTimeMillis() / 500 % 2 == 0) ? "|" : "";
            int fieldW  = Math.min(maxNameW, 110);
            Gui.drawRect(nameX - 1, topRowY - 1, nameX + fieldW + 1, topRowY + 10, 0x22000000);
            GuiRenderUtils.drawRectOutline(nameX - 1, topRowY - 1, fieldW + 2, 11, UITheme.getPrimary());
            fontRendererObj.drawString(clipText(renameBuffer + cur, fieldW), nameX + 1, topRowY, COL_TXT_PRI);
        } else {
            String name    = used ? pm.getProfileName(slot) : "Disponible";
            int    nameCol = active ? 0xFFFFAAAA : (used ? COL_TXT_PRI : COL_TXT_MUT);
            fontRendererObj.drawString(clipText(name, maxNameW), nameX, topRowY, nameCol);

            if (used) {
                int clippedW = fontRendererObj.getStringWidth(clipText(name, maxNameW));
                int penX = nameX + clippedW + 4;
                boolean hovPen = inRect(mx, my, penX - 1, topRowY - 1, 11, 11);
                fontRendererObj.drawString("\u270E", penX, topRowY,
                    hovPen ? UITheme.getPrimary() : COL_TXT_MUT);
            }

            int stY = topRowY + 11;
            if (active) {
                fontRendererObj.drawString("\u00A7a\u25CF \u00A77Actif", nameX, stY, 0);
            } else if (used) {
                String st = pm.getProfileDescription(slot);
                if (!st.isEmpty()) fontRendererObj.drawString("\u00A78" + st, nameX, stY, COL_TXT_MUT);
            } else {
                fontRendererObj.drawString("\u00A78\u25E6 Emplacement libre", nameX, stY, 0);
            }
        }

        // Séparateur entre les deux rangées
        Gui.drawRect(x + 4, y + CARD_TOP, x + w - 4, y + CARD_TOP + 1,
                hovCard ? 0x22FFFFFF : 0x0FFFFFFF);

        // --- RANGÉE BASSE : Boutons centrés ---
        drawCardActions(slot, x, y, w, mx, my, pm, used, active, hovCard);
    }

    /** Largeur réelle en pixels de la rangée de boutons (sans marges). */
    private int calcBtnRowWidth(boolean used, boolean active, int slot) {
        if (!used && (slot == 0 || slot == 1)) return BTN_RES;
        int total = BTN_SAVE;
        if (used) {
            total += BTN_GAP + (active ? BTN_RELOAD : BTN_LOAD);
            total += BTN_GAP + (confirmDeleteSlot == slot ? BTN_CONF : BTN_DEL);
        }
        return total;
    }

    /**
     * Largeur totale occupée par les boutons d'action d'une carte.
     * Utilisé pour calculer l'espace disponible pour le texte.
     */
    private int computeActionsWidth(boolean used, boolean active, int slot) {
        return calcBtnRowWidth(used, active, slot) + 4;
    }

    private void drawCardActions(int slot, int cx, int cy, int cw,
                                  int mx, int my,
                                  HudProfileManager pm, boolean used, boolean active,
                                  boolean hovCard) {
        int bottomH = CARD_H - CARD_TOP - 1;
        int btnY    = cy + CARD_TOP + 1 + (bottomH - BTN_ROW_H) / 2;
        float dim   = hovCard ? 1.0f : 0.22f;

        // Boutons alignés à droite (right-to-left)
        int rx = cx + cw - 6;

        if (!used && (slot == 0 || slot == 1)) {
            rx -= BTN_RES;
            boolean h = inRect(mx, my, rx, btnY, BTN_RES, BTN_ROW_H);
            drawSmallBtn(rx, btnY, BTN_RES, BTN_ROW_H, "Restaurer", h, COL_RES, dim);
        } else {
            // Supprimer / Confirmer (extrême droite)
            if (used) {
                int dw = confirmDeleteSlot == slot ? BTN_CONF : BTN_DEL;
                rx -= dw;
                boolean h = inRect(mx, my, rx, btnY, dw, BTN_ROW_H);
                drawSmallBtn(rx, btnY, dw, BTN_ROW_H,
                        confirmDeleteSlot == slot ? "Sur ?" : "\u2715", h, COL_DEL, dim);
                rx -= BTN_GAP;
            }
            // Charger / Recharger
            if (active && used) {
                rx -= BTN_RELOAD;
                boolean h = inRect(mx, my, rx, btnY, BTN_RELOAD, BTN_ROW_H);
                drawSmallBtn(rx, btnY, BTN_RELOAD, BTN_ROW_H, "\u21BA Recharger", h, COL_LOAD, dim);
                rx -= BTN_GAP;
            } else if (used) {
                rx -= BTN_LOAD;
                boolean h = inRect(mx, my, rx, btnY, BTN_LOAD, BTN_ROW_H);
                drawSmallBtn(rx, btnY, BTN_LOAD, BTN_ROW_H, "Charger", h, COL_LOAD, dim);
                rx -= BTN_GAP;
            }
            // Sauver (toujours, plus à gauche)
            rx -= BTN_SAVE;
            boolean h = inRect(mx, my, rx, btnY, BTN_SAVE, BTN_ROW_H);
            drawSmallBtn(rx, btnY, BTN_SAVE, BTN_ROW_H, "Sauver", h, COL_SAVE, dim);
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

    /** Bouton à pleine opacité (footer Fermer, Restaurer depuis drawScreen). */
    private void drawSmallBtn(int x, int y, int w, int h, String text, boolean hov, int col) {
        drawSmallBtn(x, y, w, h, text, hov, col, 1.0f);
    }

    /**
     * @param dim facteur d'opacité 0.0→1.0 :
     *            0.22 = fantôme (carte non survolée),
     *            1.0  = pleine visibilité (carte survolée ou hover bouton).
     */
    private void drawSmallBtn(int x, int y, int w, int h,
                               String text, boolean hov, int col, float dim) {
        // Fond dégradé haut → bas
        int bgA  = (int)((hov ? 0x72 : 0x20) * dim);
        int bgA2 = bgA / 2;
        GuiRenderUtils.drawGradientRect(x, y, x + w, y + h,
                (bgA  << 24) | (col & 0xFFFFFF),
                (bgA2 << 24) | (col & 0xFFFFFF));

        // Bordure
        int brdA = (int)((hov ? 0xCC : 0x44) * dim);
        GuiRenderUtils.drawRectOutline(x, y, w, h, (brdA << 24) | (col & 0xFFFFFF));

        // Highlight supérieur (effet "surélevé") — seulement si assez visible
        if (hov) Gui.drawRect(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);

        // Couleur du texte
        int txtA = (int)((hov ? 0xFF : 0xCC) * dim);
        int tc   = (txtA << 24) | (hov ? 0xFFFFFF : (col & 0xFFFFFF));

        // Bouton suppression — croix pixel-art 2px d'épaisseur avec ombre
        if ("\u2715".equals(text)) {
            // Les blocs 2×2px décalent le centre visuel de +1px → compenser avec (w-2)/2
            int cx = x + (w - 2) / 2;
            int cy = y + (h - 2) / 2;
            int shadow = (int)(0x55 * dim) << 24;
            for (int i = -3; i <= 3; i++) {
                Gui.drawRect(cx + i + 1, cy + i + 1, cx + i + 3, cy + i + 3, shadow);
                Gui.drawRect(cx + i + 1, cy - i + 1, cx + i + 3, cy - i + 3, shadow);
            }
            for (int i = -3; i <= 3; i++) {
                Gui.drawRect(cx + i, cy + i, cx + i + 2, cy + i + 2, tc);
                Gui.drawRect(cx + i, cy - i, cx + i + 2, cy - i + 2, tc);
            }
            return;
        }

        String clipped = clipText(text, w - 4);
        int textW = fontRendererObj.getStringWidth(clipped);
        int tx = x + (w - textW) / 2;
        int ty = y + (h - 8) / 2;
        if (dim > 0.5f) fontRendererObj.drawString(clipped, tx + 1, ty + 1, 0x33000000);
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
        int rawY = (this.height - panelH()) / 2;
        rawY = Math.max(4, Math.min(this.height - panelH() - 4, rawY));
        int pY  = rawY;
        int csY = cardsStartY(pY);
        int areaH = cardsAreaH();

        // Bouton Fermer (footer) — hit zone
        int fY = footerBtnY(pY);
        String closeLabel = "\u2190  Fermer";
        int clW = fontRendererObj.getStringWidth(closeLabel);
        int bW  = clW + 14;
        int pX  = panelX();
        int bX = pX + (panelW - bW) / 2;
        if (inRect(mx, my, bX, fY, bW, BTN_H + 2)) {
            this.mc.displayGuiScreen(parent); return;
        }

        // Fermeture renommage si clic hors du champ
        if (renamingSlot >= 0) {
            int fieldX = pX + PAD + 7 + BADGE_S + 7 - 1;
            int topRowY0 = csY + renamingSlot * (CARD_H + CARD_GAP) - scrollY
                           + (CARD_TOP - 8 * 2 - 3) / 2;
            int fieldY = topRowY0 - 1;
            if (!inRect(mx, my, fieldX, fieldY, 112, 11)) finishRenaming();
            return;
        }

        // Cartes — positions décalées par scrollY
        boolean needsScroll = maxScrollY() > 0;
        int cardsInnerW = needsScroll ? panelW - PAD * 2 - 6 : panelW - PAD * 2;

        for (int i = 0; i < HudProfileManager.MAX_PROFILES; i++) {
            int cy = csY + i * (CARD_H + CARD_GAP) - scrollY;
            int cx = pX + PAD;

            if (cy + CARD_H <= csY || cy >= csY + areaH) continue;
            if (!inRect(mx, my, cx, cy, cardsInnerW, CARD_H)) continue;

            boolean used   = pm.isSlotUsed(i);
            boolean active = pm.getActiveProfile() == i;

            // --- Détection de clic sur les boutons (rangée basse, right-to-left) ---
            int bottomH = CARD_H - CARD_TOP - 1;
            int btnY2   = cy + CARD_TOP + 1 + (bottomH - BTN_ROW_H) / 2;
            int rx      = cx + cardsInnerW - 6;

            if (!used && (i == 0 || i == 1)) {
                rx -= BTN_RES;
                if (inRect(mx, my, rx, btnY2, BTN_RES, BTN_ROW_H)) {
                    if (i == 0) pm.initDefaultPvPProfile(); else pm.initDefaultExplorationProfile();
                    showToast("Profil restauré", COL_RES); return;
                }
            } else {
                // Supprimer / Confirmer
                if (used) {
                    int dw = confirmDeleteSlot == i ? BTN_CONF : BTN_DEL;
                    rx -= dw;
                    if (inRect(mx, my, rx, btnY2, dw, BTN_ROW_H)) {
                        if (confirmDeleteSlot == i) {
                            pm.deleteSlot(i); confirmDeleteSlot = -1; showToast("Profil supprimé", COL_DEL);
                        } else {
                            confirmDeleteSlot = i; confirmDeleteTime = Minecraft.getSystemTime();
                        }
                        return;
                    }
                    rx -= BTN_GAP;
                }
                // Charger / Recharger
                if (active && used) {
                    rx -= BTN_RELOAD;
                    if (inRect(mx, my, rx, btnY2, BTN_RELOAD, BTN_ROW_H)) {
                        pm.loadFromSlot(i); confirmDeleteSlot = -1;
                        showToast("\"" + pm.getProfileName(i) + "\" rechargé", COL_LOAD); return;
                    }
                    rx -= BTN_GAP;
                } else if (used) {
                    rx -= BTN_LOAD;
                    if (inRect(mx, my, rx, btnY2, BTN_LOAD, BTN_ROW_H)) {
                        pm.loadFromSlot(i); confirmDeleteSlot = -1;
                        showToast("\"" + pm.getProfileName(i) + "\" chargé", COL_LOAD); return;
                    }
                    rx -= BTN_GAP;
                }
                // Sauver
                rx -= BTN_SAVE;
                if (inRect(mx, my, rx, btnY2, BTN_SAVE, BTN_ROW_H)) {
                    pm.saveToSlot(i); confirmDeleteSlot = -1;
                    showToast("\"" + pm.getProfileName(i) + "\" sauvegardé", COL_SAVE); return;
                }
            }

            // --- Crayon renommage (rangée haute) ---
            if (used) {
                int nameX   = cx + 7 + BADGE_S + 7;
                String name = pm.getProfileName(i);
                int maxNW   = cardsInnerW - (BADGE_S + 16) - 14;
                String clipped = clipText(name, maxNW);
                int cw2    = fontRendererObj.getStringWidth(clipped);
                int penX   = nameX + cw2 + 4;
                int topRowY = cy + (CARD_TOP - 8 * 2 - 3) / 2;
                if (inRect(mx, my, penX - 1, topRowY - 1, 11, 11) && renamingSlot == -1) {
                    renamingSlot = i; renameBuffer = name; return;
                }
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
