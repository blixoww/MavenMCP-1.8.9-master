package net.minecraft.client.gui;

import net.minecraft.client.custompackets.data.BoutiqueData;
import net.minecraft.client.custompackets.handler.BoutiquePacketHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Interface boutique PB v3.0 — style GuiWiki + palette GuiHDV.
 */
public class GuiBoutique extends GuiScreen {

    // ── Palette (m\u00eame style que GuiHDV) ──────────────────────────────────────
    private static final int C_BG       = 0xF2040912;
    private static final int C_HEADER   = 0xFF030B16;
    private static final int C_FOOTER   = 0xFF030B16;
    private static final int C_PANEL    = 0xFF070D18;
    private static final int C_TAB_BG   = 0xFF07101E;
    private static final int C_TAB_ACT  = 0x22FFFFFF;
    private static final int C_TAB_HOV  = 0x12FFFFFF;
    private static final int C_BORDER   = 0xFF1A3A6A;
    private static final int C_BORDER2  = 0xFF0A2040;
    private static final int C_SEP      = 0xFF0A1528;
    private static final int C_ROW_ODD  = 0;
    private static final int C_ROW_EVEN = 0x11FFFFFF;
    private static final int C_ROW_HOV  = 0x22FFFFFF;
    private static final int C_TEXT     = 0xFFFFFFFF;
    private static final int C_SOFT     = 0xFFAADDFF;
    private static final int C_MUTED    = 0xFF445060;
    private static final int C_MUTED2   = 0xFF778899;
    private static final int C_GOLD     = 0xFFFFD700;
    private static final int C_PB       = 0xFFFFEE55;
    private static final int C_OK       = 0xFF33CC77;
    private static final int C_KO       = 0xFFEE3333;
    private static final int C_BADGE    = 0x301A3A6A;
    private static final int C_ACCENT   = 0xFF2277EE;
    private static final int C_ACCENT2  = 0xFF55AAFF;

    private static final DecimalFormat FMT;
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.FRANCE);
        s.setGroupingSeparator(' ');
        FMT = new DecimalFormat("#,##0", s);
    }

    // ── Onglets ──────────────────────────────────────────────────────────────
    enum Tab { GRADES, KITS, COMMANDES, SPAWNERS, PACKS, OFFRE }
    private Tab activeTab = Tab.GRADES;

    // ── Constantes layout ─────────────────────────────────────────────────────
    private static final int H_HEADER = 36;
    private static final int H_FOOTER = 28;
    private static final int W_TABS   = 128;
    private static final int ROW_H      = 30;
    private static final int PACK_ROW_H = 48;
    private static final int TAB_H    = 28;   // GuiWiki row height

    // ── Layout calcul\u00e9 ────────────────────────────────────────────────────────
    private int px, py, pw, ph;
    private int bodyY, bodyH;
    private int tabsX, tabsY, tabsW, tabsH;
    private int listX, listY, listW, listH;
    private int closeX, closeY;
    private static final int CLOSE_S = 14;

    // ── Scroll ────────────────────────────────────────────────────────────────
    private int scrollOffset = 0;

    // ── Hover ─────────────────────────────────────────────────────────────────
    private boolean hovClose;

    // ── Toggle dur\u00e9e (commandes uniquement) ──────────────────────────────────
    // false = PERMANENT (prix perm), true = 1 MOIS (prix mensuel)
    private boolean buyTemporary = true;

    // ── Confirmation ──────────────────────────────────────────────────────────
    private boolean confirmOpen = false;
    private BoutiqueData.Entry confirmEntry;
    private BoutiqueData.OffreData confirmOffre;
    private boolean confirmPayPB;
    private boolean confirmTemporary;
    private long confirmPrice;

    // ── Tooltip item (offre icone) ─────────────────────────────────────────────
    private ItemStack tooltipStack;
    private int tooltipMX, tooltipMY;

    // ── Tooltip description (survol ligne) ────────────────────────────────────
    private BoutiqueData.Entry hoveredEntry = null;
    private int hoveredMX, hoveredMY;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    public void initGui() {
        BoutiquePacketHandler.requestData();
        scrollOffset = 0;
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    public void onDataRefreshed() { scrollOffset = 0; }
    public void onResult() {}

    // =========================================================================
    // Layout
    // =========================================================================

    private void layout() {
        pw = Math.min(width - 40, 580);
        ph = Math.min(height - 30, 380);
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        bodyY = py + H_HEADER;
        bodyH = ph - H_HEADER - H_FOOTER;

        tabsX = px; tabsY = bodyY; tabsW = W_TABS; tabsH = bodyH;
        listX = px + W_TABS + 1;
        listY = bodyY;
        listW = pw - W_TABS - 1;
        listH = bodyH;

        closeX = px + pw - CLOSE_S - 6;
        closeY = py + (H_HEADER - CLOSE_S) / 2;
    }

    // =========================================================================
    // Input
    // =========================================================================

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == org.lwjgl.input.Keyboard.KEY_ESCAPE) {
            if (confirmOpen) { confirmOpen = false; return; }
            mc.displayGuiScreen(null);
            return;
        }
        if ((key == org.lwjgl.input.Keyboard.KEY_R || key == org.lwjgl.input.Keyboard.KEY_F5) && !confirmOpen)
            BoutiquePacketHandler.requestData();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0 && !confirmOpen && activeTab != Tab.OFFRE) {
            int rh = (activeTab == Tab.PACKS) ? PACK_ROW_H : ROW_H;
            int toggleH = (activeTab == Tab.COMMANDES || activeTab == Tab.GRADES || activeTab == Tab.KITS) ? 24 : 0;
            int effH = listH - toggleH;
            int max = Math.max(0, currentList().size() * rh - effH);
            int step = Math.max(12, rh / 2);
            scrollOffset = clamp(scrollOffset - (wheel > 0 ? step : -step), 0, max);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;
        if (confirmOpen) { handleConfirmClick(mx, my); return; }

        if (inBox(mx, my, closeX, closeY, CLOSE_S, CLOSE_S)) {
            mc.displayGuiScreen(null); return;
        }

        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            int ty = tabsY + 4 + i * TAB_H;
            if (i == 5) ty += 4;
            if (inBox(mx, my, tabsX + 4, ty, tabsW - 8, TAB_H)) {
                if (activeTab != tabs[i]) { activeTab = tabs[i]; scrollOffset = 0; buyTemporary = true; }
                return;
            }
        }

        if (!inBox(mx, my, listX, listY, listW, listH)) return;
        if (activeTab == Tab.OFFRE) { handleOfferClick(mx, my); return; }

        // Toggle PERMANENT / 1 MOIS (onglets Grades et Commandes)
        if (activeTab == Tab.COMMANDES || activeTab == Tab.GRADES || activeTab == Tab.KITS) {
            int tBtnW = 80, tBtnH = 18, tBtnY = listY + 3;
            int tTotal = tBtnW * 2 + 4;
            int tBtnX1 = listX + listW / 2 - tTotal / 2;
            int tBtnX2 = tBtnX1 + tBtnW + 4;
            if (inBox(mx, my, tBtnX1, tBtnY, tBtnW, tBtnH)) { buyTemporary = true;  scrollOffset = 0; return; }
            if (inBox(mx, my, tBtnX2, tBtnY, tBtnW, tBtnH)) { buyTemporary = false; scrollOffset = 0; return; }
        }

        List<BoutiqueData.Entry> list = currentList();
        int toggleH   = (activeTab == Tab.COMMANDES || activeTab == Tab.GRADES || activeTab == Tab.KITS) ? 24 : 0;
        int effectiveY = listY + toggleH;
        int effectiveH = listH - toggleH;
        int clickRowH = (activeTab == Tab.PACKS) ? PACK_ROW_H : ROW_H;
        for (int i = 0; i < list.size(); i++) {
            BoutiqueData.Entry e = list.get(i);
            int rowY = effectiveY + i * clickRowH - scrollOffset;
            if (rowY + clickRowH <= effectiveY || rowY >= effectiveY + effectiveH) continue;
            int btnW = 65, btnH = 17, btnY = rowY + (clickRowH - btnH) / 2;
            int pbX = listX + listW - btnW - 8;
            int mX  = pbX - btnW - 5;
            // Utiliser les prix permanents si toggle sur PERMANENT et qu'ils existent
            boolean usePerma = (activeTab == Tab.COMMANDES || activeTab == Tab.GRADES || activeTab == Tab.KITS) && !buyTemporary && e.prixMonnaieP > 0;
            long showMoney = usePerma ? e.prixMonnaieP : e.prixMonnaie;
            int  showPB    = usePerma ? e.prixPBP      : e.prixPB;
            boolean hasMoney  = showMoney > 0;
            boolean hasPBprix = showPB > 0;
            if (hasMoney && hasPBprix) {
                if (inBox(mx, my, mX,  btnY, btnW, btnH)) { openConfirm(e, null, false, showMoney); return; }
                if (inBox(mx, my, pbX, btnY, btnW, btnH)) { openConfirm(e, null, true,  showPB);    return; }
            } else if (hasMoney  && inBox(mx, my, pbX, btnY, btnW, btnH)) { openConfirm(e, null, false, showMoney); return; }
            else if   (hasPBprix && inBox(mx, my, pbX, btnY, btnW, btnH)) { openConfirm(e, null, true,  showPB);   return; }
        }
    }

    private void handleOfferClick(int mx, int my) {
        BoutiqueData d = BoutiquePacketHandler.getCached();
        if (d == null || d.offre == null) return;
        BoutiqueData.OffreData o = d.offre;
        int btnW = 110, btnH = 22;
        int b1x = listX + listW / 2 - btnW - 6;
        int b2x = listX + listW / 2 + 6;
        int by  = listY + listH - btnH - 14;
        if (o.prixMonnaie > 0 && inBox(mx, my, b1x, by, btnW, btnH))
            openConfirm(null, o, false, o.prixMonnaie);
        else if (o.prixPB > 0 && inBox(mx, my, b2x, by, btnW, btnH))
            openConfirm(null, o, true, o.prixPB);
    }

    private void handleConfirmClick(int mx, int my) {
        int cw = 360;
        int ch = computeModalHeight();
        int cx = px + pw / 2 - cw / 2, cy = py + ph / 2 - ch / 2;
        int bw = 148, bh = 26, by = cy + ch - bh - 10;
        if (inBox(mx, my, cx + 14, by, bw, bh))               { confirmOpen = false; }
        else if (inBox(mx, my, cx + cw - bw - 14, by, bw, bh)) { doConfirm(); confirmOpen = false; }
    }

    private void openConfirm(BoutiqueData.Entry e, BoutiqueData.OffreData o, boolean pb, long price) {
        confirmEntry = e; confirmOffre = o; confirmPayPB = pb; confirmPrice = price;
        confirmTemporary = buyTemporary && (activeTab == Tab.COMMANDES || activeTab == Tab.GRADES || activeTab == Tab.KITS) && e != null && e.duree > 0;
        confirmOpen = true;
    }

    private void doConfirm() {
        int cat; String id;
        if (confirmOffre != null)      { cat = 4; id = confirmOffre.id; }
        else if (confirmEntry != null) {
            if      (activeTab == Tab.GRADES)    cat = 0;
            else if (activeTab == Tab.KITS)      cat = 1;
            else if (activeTab == Tab.COMMANDES) cat = 2;
            else if (activeTab == Tab.PACKS)     cat = 5;
            else                                 cat = 3; // SPAWNERS
            id  = confirmEntry.id;
        } else return;
        BoutiquePacketHandler.sendBuy(cat, id, confirmPayPB, confirmTemporary);
    }

    private List<BoutiqueData.Entry> currentList() {
        BoutiqueData d = BoutiquePacketHandler.getCached();
        if (d == null) return new ArrayList<>();
        switch (activeTab) {
            case GRADES:    return d.grades;
            case KITS:      return d.kits;
            case COMMANDES: return d.commandes;
            case SPAWNERS:  return d.spawners;
            case PACKS:     return d.packs;
            default: return new ArrayList<>();
        }
    }

    // =========================================================================
    // Render
    // =========================================================================

    @Override
    public void drawScreen(int mx, int my, float pt) {
        layout();
        tooltipStack = null;
        hoveredEntry = null;

        drawRect(0, 0, width, height, 0xBB000011);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GuiRenderUtils.drawShadow(px, py, pw, ph, 8, 0x90);
        drawRect(px, py, px + pw, py + ph, C_BG);

        // Barre d'accent en haut (style GuiWiki)
        drawRect(px, py, px + pw, py + 2, C_ACCENT);
        GuiRenderUtils.drawGradientRect(px, py + 2, px + pw, py + 8, 0x182277EE, 0x00000000);
        GuiRenderUtils.drawRectOutline(px, py, pw, ph, 0x1AFFFFFF);

        drawHeader(mx, my);

        drawRect(px + W_TABS, bodyY, px + W_TABS + 1, bodyY + bodyH, 0x30FFFFFF);
        drawTabs(mx, my);

        drawRect(listX, listY, listX + listW, listY + listH, C_PANEL);
        if (activeTab == Tab.OFFRE)
            drawOffre(BoutiquePacketHandler.getCached(), mx, my);
        else
            drawList(mx, my);

        drawFooter(BoutiquePacketHandler.getCached());
        drawRect(px, py + ph - H_FOOTER, px + pw, py + ph - H_FOOTER + 1, 0x30FFFFFF);

        if (confirmOpen) drawConfirmModal(mx, my);

        GlStateManager.disableBlend();
        super.drawScreen(mx, my, pt);

        if (hoveredEntry != null && !confirmOpen) drawEntryTooltip(hoveredEntry, hoveredMX, hoveredMY);
        if (tooltipStack != null) renderToolTip(tooltipStack, tooltipMX, tooltipMY);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private void drawHeader(int mx, int my) {
        GuiRenderUtils.drawGradientRect(px, py + 2, px + pw, py + H_HEADER, C_HEADER, C_BG);
        drawRect(px, py + H_HEADER - 1, px + pw, py + H_HEADER, 0x30FFFFFF);

        // Titre "$BOUTIQUE" style GuiWiki (premi\u00e8re lettre color\u00e9e)
        String sym   = "$";
        String label = "BOUTIQUE";
        int symW = fontRendererObj.getStringWidth(sym);
        int titX = px + 12;
        int titY = py + (H_HEADER - 18) / 2;
        fontRendererObj.drawStringWithShadow(sym,   titX,            titY, C_GOLD);
        fontRendererObj.drawStringWithShadow(label, titX + symW + 2, titY, C_SOFT);
        fontRendererObj.drawString("Boutique du serveur", titX, titY + 11, C_MUTED);

        // Solde \u00e0 droite
        BoutiqueData d = BoutiquePacketHandler.getCached();
        if (d != null) {
            String bal = "§6" + FMT.format(d.balance) + " $ §7| §e" + FMT.format(d.pb) + " PB";
            int bw = fontRendererObj.getStringWidth(bal);
            int bx = px + pw - bw - 46;
            drawRect(bx - 4, py + 8, px + pw - 26, py + 22, 0x18FFFFFF);
            drawRect(bx - 4, py + 22, px + pw - 26, py + 23, 0x44444466);
            fontRendererObj.drawStringWithShadow(bal, bx, py + 12, C_TEXT);
        }

        // Bouton fermer
        hovClose = inBox(mx, my, closeX, closeY, CLOSE_S, CLOSE_S);
        GuiRenderUtils.drawCloseButton(closeX, closeY, CLOSE_S, hovClose);
        int fw = fontRendererObj.getStringWidth("x");
        fontRendererObj.drawString("x", closeX + (CLOSE_S - fw) / 2, closeY + 3,
                hovClose ? C_TEXT : 0xFFCC7777);
    }

    // ── Onglets (style GuiWiki sidebar) ──────────────────────────────────────

    private void drawTabs(int mx, int my) {
        drawRect(tabsX, tabsY, tabsX + tabsW, tabsY + tabsH, C_TAB_BG);

        Tab[] tabs = Tab.values();
        String[] names     = {"Grades", "Kits", "Commandes", "Spawners", "Packs", "Offre speciale"};
        int[]    tabColors = { 0xFF9944CC, 0xFFCC4488, 0xFF2299CC, 0xFF44BB66, 0xFFFF8800, 0xFFCCAA00 };
        BoutiqueData d = BoutiquePacketHandler.getCached();

        for (int i = 0; i < tabs.length; i++) {
            int ty = tabsY + 4 + i * TAB_H;
            if (i == 5) {
                drawRect(tabsX + 10, ty - 3, tabsX + tabsW - 10, ty - 2, 0x20FFFFFF);
                ty += 4;
            }

            boolean active = (tabs[i] == activeTab);
            boolean hov    = inBox(mx, my, tabsX + 4, ty, tabsW - 8, TAB_H);
            int col        = tabColors[i];

            if (active) {
                drawRect(tabsX + 4, ty, tabsX + tabsW - 4, ty + TAB_H, C_TAB_ACT);
                drawRect(tabsX + 4, ty, tabsX + 7, ty + TAB_H, col);
                GuiRenderUtils.drawGradientRect(tabsX + 7, ty, tabsX + 22, ty + TAB_H,
                        (col & 0x00FFFFFF) | 0x18000000, 0x00000000);
            } else if (hov) {
                drawRect(tabsX + 4, ty, tabsX + tabsW - 4, ty + TAB_H, C_TAB_HOV);
                drawRect(tabsX + 4, ty, tabsX + 6, ty + TAB_H, (col & 0x00FFFFFF) | 0x55000000);
            }

            int textCol = active ? C_TEXT : (hov ? C_SOFT : 0xFF6E7480);
            fontRendererObj.drawStringWithShadow(names[i], tabsX + 12, ty + (float) (TAB_H - 8) / 2, textCol);

            if (d != null && i < 5) {
                List<?> cat;
                if      (i == 0) cat = d.grades;
                else if (i == 1) cat = d.kits;
                else if (i == 2) cat = d.commandes;
                else if (i == 3) cat = d.spawners;
                else             cat = d.packs;
                if (!cat.isEmpty()) {
                    String cnt = String.valueOf(cat.size());
                    int cntW = fontRendererObj.getStringWidth(cnt) + 6;
                    int bx = tabsX + tabsW - cntW - 6;
                    int by = ty + (TAB_H - 10) / 2;
                    int badgeBg = active ? ((col & 0x00FFFFFF) | 0x30000000) : C_BADGE;
                    drawRect(bx, by, bx + cntW, by + 10, badgeBg);
                    fontRendererObj.drawString(cnt, bx + 3, by + 1,
                            active ? ((col & 0x00FFFFFF) | 0xDD000000) : 0xFF556070);
                }
            }
        }

        fontRendererObj.drawString("§8ESC pour fermer", tabsX + 8, tabsY + tabsH - 12, C_MUTED);
    }

    // ── Liste des articles ────────────────────────────────────────────────────

    private void drawList(int mx, int my) {
        List<BoutiqueData.Entry> list = currentList();

        if (list.isEmpty()) {
            String msg = BoutiquePacketHandler.getCached() == null
                    ? "§7Chargement des articles..."
                    : "§8Aucun article dans cette categorie.";
            drawCenteredStr(msg, listX + listW / 2, listY + listH / 2 - 4, C_MUTED2);
            return;
        }

        // Le toggle PERMANENT / 1 MOIS apparait sur les onglets Grades et Commandes
        boolean needsToggle = (activeTab == Tab.COMMANDES || activeTab == Tab.GRADES || activeTab == Tab.KITS);
        int toggleH = needsToggle ? 24 : 0;
        int itemsY  = listY + toggleH;
        int itemsH  = listH - toggleH;

        if (needsToggle) {
            int tBtnW = 80, tBtnH = 18;
            int tTotal = tBtnW * 2 + 4;
            int tBtnX1 = listX + listW / 2 - tTotal / 2;
            int tBtnX2 = tBtnX1 + tBtnW + 4;
            int tBtnY  = listY + 3;

            // Bouton 1 MOIS (buyTemporary=true) — affich\u00e9 en premier (option par d\u00e9faut)
            boolean tHov1 = inBox(mx, my, tBtnX1, tBtnY, tBtnW, tBtnH);
            drawRect(tBtnX1, tBtnY, tBtnX1 + tBtnW, tBtnY + tBtnH,
                    buyTemporary ? 0x22303060 : (tHov1 ? 0x12FFFFFF : 0x08FFFFFF));
            GuiRenderUtils.drawRectOutline(tBtnX1, tBtnY, tBtnW, tBtnH,
                    buyTemporary ? C_ACCENT : 0x28FFFFFF);
            if (buyTemporary) drawRect(tBtnX1, tBtnY, tBtnX1 + 2, tBtnY + tBtnH, C_ACCENT);
            drawCenteredStr(buyTemporary ? "§b1 MOIS" : "§71 Mois", tBtnX1 + tBtnW / 2, tBtnY + 5, C_TEXT);

            // Bouton PERMANENT (buyTemporary=false) — prix plus \u00e9lev\u00e9s
            boolean tHov2 = inBox(mx, my, tBtnX2, tBtnY, tBtnW, tBtnH);
            drawRect(tBtnX2, tBtnY, tBtnX2 + tBtnW, tBtnY + tBtnH,
                    !buyTemporary ? 0x221A4030 : (tHov2 ? 0x12FFFFFF : 0x08FFFFFF));
            GuiRenderUtils.drawRectOutline(tBtnX2, tBtnY, tBtnW, tBtnH,
                    !buyTemporary ? C_OK : 0x28FFFFFF);
            if (!buyTemporary) drawRect(tBtnX2, tBtnY, tBtnX2 + 2, tBtnY + tBtnH, C_OK);
            drawCenteredStr(!buyTemporary ? "§aPERMANENT" : "§7Permanent", tBtnX2 + tBtnW / 2, tBtnY + 5, C_TEXT);

            drawRect(listX, itemsY - 1, listX + listW, itemsY, 0x20FFFFFF);
        }

        // Clip scissor
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glScissor(listX * sf, mc.displayHeight - (itemsY + itemsH) * sf, listW * sf, itemsH * sf);

        int btnW = 65, btnH = 17;
        int pbBtnX = listX + listW - btnW - 8;
        int mBtnX  = pbBtnX - btnW - 5;

        int[] tabColors = { 0xFF9944CC, 0xFFCC4488, 0xFF2299CC, 0xFF44BB66, 0xFFFF8800, 0xFFCCAA00 };
        int accentCol = tabColors[Math.min(activeTab.ordinal(), tabColors.length - 1)];
        boolean isPacks = (activeTab == Tab.PACKS);
        int rowH = isPacks ? PACK_ROW_H : ROW_H;

        for (int i = 0; i < list.size(); i++) {
            BoutiqueData.Entry e = list.get(i);
            int rowY = itemsY + i * rowH - scrollOffset;
            if (rowY + rowH <= itemsY || rowY >= itemsY + itemsH) continue;

            boolean hov = inBox(mx, my, listX, rowY, listW - 4, rowH);
            int bg = hov ? C_ROW_HOV : ((i & 1) == 0 ? C_ROW_EVEN : C_ROW_ODD);
            drawRect(listX, rowY, listX + listW, rowY + rowH, bg);
            if (hov) {
                drawRect(listX, rowY, listX + 2, rowY + rowH, accentCol);
                hoveredEntry = e; hoveredMX = mx; hoveredMY = my;
            }
            if (!hov) drawRect(listX + 4, rowY + rowH - 1, listX + listW - 4, rowY + rowH, 0x1EFFFFFF);

            // Icone
            int iconX = listX + 6, iconY = rowY + (rowH - 16) / 2;
            ItemStack icon = getIconStack(e);
            if (icon != null) {
                GlStateManager.enableDepth();
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(icon, iconX, iconY);
                itemRender.renderItemOverlayIntoGUI(fontRendererObj, icon, iconX, iconY, null);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableDepth();
            } else {
                drawRect(iconX + 1, iconY + 1, iconX + 14, iconY + 14, accentCol);
                GuiRenderUtils.drawRectOutline(iconX + 1, iconY + 1, 13, 13, 0x33FFFFFF);
            }

            int textX = listX + 28;

            if (isPacks) {
                // ── Rendu pack ─────────────────────────────────────────────
                // Bande dor\u00e9e \u00e0 gauche
                GuiRenderUtils.drawGradientRect(listX, rowY, listX + 18, rowY + rowH,
                        (accentCol & 0x00FFFFFF) | 0x22000000, 0x00000000);

                // Nom du pack
                fontRendererObj.drawStringWithShadow(e.nom != null ? fmt(e.nom) : "", textX, rowY + 5, C_GOLD);

                // Contenu : premi\u00e8re ligne de description
                if (e.description != null && !e.description.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String dl : e.description) {
                        String cl = stripColor(fmt(dl));
                        if (!cl.isEmpty()) { if (sb.length() > 0) sb.append("  ·  "); sb.append(cl); }
                    }
                    String contenu = sb.toString();
                    contenu = fontRendererObj.trimStringToWidth(contenu, listW - textX + listX - 85);
                    fontRendererObj.drawString("§8" + contenu, textX, rowY + 17, 0);
                }

                // Prix original barr\u00e9 + \u00e9conomie
                if (e.prixPBP > 0 && e.prixPB > 0) {
                    int saved = e.prixPBP - e.prixPB;
                    // Badge \u00e9conomie
                    String saveStr = "-" + saved + " PB";
                    int bw2 = fontRendererObj.getStringWidth(saveStr) + 8;
                    int bx2 = listX + listW - btnW - 14 - bw2;
                    int by2 = rowY + rowH / 2 - 6;
                    drawRect(bx2, by2, bx2 + bw2, by2 + 11, 0x88003300);
                    GuiRenderUtils.drawRectOutline(bx2, by2, bw2, 11, C_OK);
                    fontRendererObj.drawString("§a" + saveStr, bx2 + 4, by2 + 2, 0);

                    // Prix original (simul\u00e9 barr\u00e9)
                    String origStr = FMT.format(e.prixPBP) + " PB";
                    int ow = fontRendererObj.getStringWidth(origStr);
                    int ox = bx2 - ow - 8;
                    int oy = rowY + rowH / 2 - 5;
                    fontRendererObj.drawString("§8" + origStr, ox, oy, 0);
                    drawRect(ox, oy + 4, ox + ow + (int)(fontRendererObj.getStringWidth("§8")), oy + 5, 0x88888888);
                }

            } else {
                // ── Rendu normal ──────────────────────────────────────────
                fontRendererObj.drawStringWithShadow(e.nom != null ? fmt(e.nom) : "", textX, rowY + 5, C_TEXT);
                String sub = buildSub(e, needsToggle && buyTemporary);
                if (!sub.isEmpty()) fontRendererObj.drawString(sub, textX, rowY + 16, C_MUTED2);
            }

            // Boutons prix
            int btnY = rowY + (rowH - btnH) / 2;
            boolean usePerma = needsToggle && !buyTemporary && e.prixMonnaieP > 0;
            long showMoney = usePerma ? e.prixMonnaieP : e.prixMonnaie;
            int  showPB    = usePerma ? e.prixPBP      : e.prixPB;
            // Pour les packs : toujours prixPB (jamais de prix monnaie ni de toggle)
            if (isPacks) { showMoney = 0; showPB = e.prixPB; }
            boolean hasMoney  = showMoney > 0;
            boolean hasPBprix = showPB > 0;
            if (hasMoney && hasPBprix) {
                drawPriceBtn(mBtnX,  btnY, btnW, btnH, inBox(mx, my, mBtnX,  btnY, btnW, btnH),
                        FMT.format(showMoney) + " $", C_GOLD);
                drawPriceBtn(pbBtnX, btnY, btnW, btnH, inBox(mx, my, pbBtnX, btnY, btnW, btnH),
                        FMT.format(showPB) + " PB", C_PB);
            } else if (hasMoney) {
                drawPriceBtn(pbBtnX, btnY, btnW, btnH, inBox(mx, my, pbBtnX, btnY, btnW, btnH),
                        FMT.format(showMoney) + " $", C_GOLD);
            } else if (hasPBprix) {
                drawPriceBtn(pbBtnX, btnY, btnW, btnH, inBox(mx, my, pbBtnX, btnY, btnW, btnH),
                        FMT.format(showPB) + " PB", C_PB);
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Scrollbar
        int totalH = list.size() * rowH;
        if (totalH > itemsH) {
            int sbX = listX + listW - 4;
            drawRect(sbX, itemsY, sbX + 3, itemsY + itemsH, 0x20FFFFFF);
            int barH = Math.max(20, itemsH * itemsH / totalH);
            int barY = itemsY + (int)((long)(itemsH - barH) * scrollOffset / Math.max(1, totalH - itemsH));
            drawRect(sbX, barY, sbX + 3, barY + barH, accentCol);
        }

        fontRendererObj.drawString("§8" + list.size() + " article(s)", listX + 5, itemsY + itemsH - 9, 0);
    }

    // ── Offre sp\u00e9ciale ────────────────────────────────────────────────────────

    private void drawOffre(BoutiqueData d, int mx, int my) {
        if (d == null || d.offre == null) {
            drawCenteredStr("§7Aucune offre speciale aujourd'hui.", listX + listW / 2, listY + listH / 2 - 8, C_SOFT);
            drawCenteredStr("§8Reviens demain !", listX + listW / 2, listY + listH / 2 + 4, C_MUTED2);
            return;
        }
        BoutiqueData.OffreData o = d.offre;

        int margin = 12;
        int cardX = listX + margin, cardY = listY + margin;
        int cardW = listW - margin * 2, cardH = listH - margin * 2;
        drawRect(cardX, cardY, cardX + cardW, cardY + cardH, C_PANEL);
        GuiRenderUtils.drawRectOutline(cardX, cardY, cardW, cardH, 0x28FFFFFF);

        // Étiquette "ÉDITION LIMITÉE"
        drawRect(cardX, cardY, cardX + cardW, cardY + 12, C_ACCENT);
        GuiRenderUtils.drawGradientRect(cardX, cardY + 12, cardX + cardW, cardY + 17,
                0x182277EE, 0x00000000);
        drawCenteredStr("§f§lEDITION LIMITEE", cardX + cardW / 2, cardY + 2, C_TEXT);

        // Item icon 32x32 — utilise l'item custom si dispo (ex: ruby au lieu d'\u00e9meraude)
        int iconX = cardX + 12, iconY = cardY + 18;
        ItemStack displayItem = resolveOfferIcon(o);
        if (displayItem != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(iconX, iconY, 0);
            GlStateManager.scale(2f, 2f, 1f);
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(displayItem, 0, 0);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            if (inBox(mx, my, iconX, iconY, 32, 32)) {
                tooltipStack = displayItem; tooltipMX = mx; tooltipMY = my;
            }
        }

        int tx = iconX + 38, ty = cardY + 18;
        fontRendererObj.drawStringWithShadow("§f§l" + fmt(o.nom), tx, ty, C_TEXT); ty += 13;
        if (o.lore != null) {
            for (String line : o.lore) {
                if (ty > cardY + cardH - 50) break;
                fontRendererObj.drawString(fmt(line), tx, ty, C_SOFT); ty += 10;
            }
        }

        ty = cardY + cardH - 55;
        long remaining = Math.max(0L, (o.expiresAtLocal - System.currentTimeMillis()) / 1000L);
        fontRendererObj.drawStringWithShadow("§7Stock : §f" + o.stock + " §7/ " + o.stockInitial, cardX + 12, ty, C_TEXT);
        fontRendererObj.drawStringWithShadow("§7Expire dans : §c" + formatDuration((int) remaining), cardX + 12, ty + 11, C_TEXT);

        int barX = cardX + 12, barY2 = ty + 24, barW = cardW - 24, barH2 = 5;
        drawRect(barX, barY2, barX + barW, barY2 + barH2, 0x33FFFFFF);
        int fill = o.stockInitial > 0 ? barW * o.stock / o.stockInitial : 0;
        int barColor = fill > barW / 2 ? C_OK : (fill > barW / 4 ? 0xFFFFC548 : C_KO);
        drawRect(barX, barY2, barX + fill, barY2 + barH2, barColor);

        int btnW = 110, btnH = 22;
        int b1x = listX + listW / 2 - btnW - 6;
        int b2x = listX + listW / 2 + 6;
        int bby = listY + listH - btnH - 14;
        if (o.prixMonnaie > 0)
            drawPriceBtn(b1x, bby, btnW, btnH, inBox(mx, my, b1x, bby, btnW, btnH),
                    FMT.format(o.prixMonnaie) + " $", C_GOLD);
        if (o.prixPB > 0)
            drawPriceBtn(b2x, bby, btnW, btnH, inBox(mx, my, b2x, bby, btnW, btnH),
                    FMT.format(o.prixPB) + " PB", C_PB);
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private void drawFooter(@SuppressWarnings("unused") BoutiqueData d) {
        int fy = py + ph - H_FOOTER;
        GuiRenderUtils.drawGradientRect(px, fy, px + pw, py + ph, C_FOOTER, C_BG);

        fontRendererObj.drawString("§810 PB = 1 euro", px + 10, fy + 10, 0);

        String hint = "§8[R] Actualiser";
        int sw = fontRendererObj.getStringWidth(hint);
        fontRendererObj.drawString(hint, px + pw - sw - 10, fy + 10, 0);

        long age = System.currentTimeMillis() - BoutiquePacketHandler.getLastResultAt();
        String msg = BoutiquePacketHandler.getLastResultMessage();
        if (age < 3000 && msg != null && !msg.isEmpty()) {
            boolean ok  = BoutiquePacketHandler.isLastResultSuccess();
            String full = (ok ? "§a+ " : "§c- ") + msg;
            int rw = fontRendererObj.getStringWidth(full);
            int rx = px + pw / 2 - rw / 2;
            drawRect(rx - 4, fy + 4, rx + rw + 4, fy + 14, ok ? 0x554FCB7B : 0x55EE4444);
            fontRendererObj.drawStringWithShadow(full, rx, fy + 5, C_TEXT);
        }
    }

    // ── Modal de confirmation ─────────────────────────────────────────────────

    private int computeModalHeight() {
        if (activeTab == Tab.PACKS && confirmEntry != null) {
            int n = confirmEntry.description != null ? confirmEntry.description.size() : 0;
            return 60 + (10 + Math.min(5, n) * 11 + 10) + 38 + 22 + 36 + 14;
        }
        return 60 + 18 + 38 + 18 + 36 + 14;
    }

    private void drawConfirmModal(int mx, int my) {
        drawRect(0, 0, width, height, 0xDD000011);

        int cw = 360;
        int ch = computeModalHeight();
        int cx = px + pw / 2 - cw / 2, cy = py + ph / 2 - ch / 2;
        int hdrH = 60;
        int bw = 148, bh = 26, by = cy + ch - bh - 10;

        // Couleur d'accent selon l'onglet / type
        int[] tabCols = { 0xFF9944CC, 0xFFCC4488, 0xFF2299CC, 0xFF44BB66, 0xFFFF8800, 0xFFCCAA00 };
        int hdrCol = (confirmOffre != null) ? 0xFFCCAA00
                : tabCols[Math.min(activeTab.ordinal(), tabCols.length - 1)];

        int hdrColSoft = (hdrCol & 0x00FFFFFF) | 0x33000000;
        int hdrColMed  = (hdrCol & 0x00FFFFFF) | 0x66000000;

        // Fond + ombres + bordures
        GuiRenderUtils.drawShadow(cx, cy, cw, ch, 14, 0xB0);
        drawRect(cx, cy, cx + cw, cy + ch, 0xF6030B16);
        GuiRenderUtils.drawRectOutline(cx - 1, cy - 1, cw + 2, ch + 2, hdrColSoft);
        GuiRenderUtils.drawRectOutline(cx, cy, cw, ch, 0x55FFFFFF);

        // Header
        drawRect(cx, cy, cx + cw, cy + 3, hdrCol);
        GuiRenderUtils.drawGradientRect(cx, cy + 3, cx + cw, cy + 18,
                (hdrCol & 0x00FFFFFF) | 0x55000000, 0x00000000);
        GuiRenderUtils.drawGradientRect(cx + 1, cy + 3, cx + cw - 1, cy + hdrH, hdrColMed, 0x00000000);
        GuiRenderUtils.drawGradientRect(cx + 1, cy + 3, cx + 90, cy + hdrH, hdrColMed, 0x00000000);
        drawRect(cx, cy + hdrH, cx + cw, cy + hdrH + 1, 0x66FFFFFF);
        GuiRenderUtils.drawGradientRect(cx, cy + hdrH + 1, cx + cw, cy + hdrH + 5, 0x33000000, 0x00000000);

        // Coins decoratifs
        drawRect(cx + 3, cy + 3, cx + 13, cy + 4, hdrCol);
        drawRect(cx + 3, cy + 3, cx + 4, cy + 13, hdrCol);
        drawRect(cx + cw - 13, cy + 3, cx + cw - 3, cy + 4, hdrCol);
        drawRect(cx + cw - 4, cy + 3, cx + cw - 3, cy + 13, hdrCol);

        // Cadre icone 32x32
        ItemStack hdrIcon = null;
        if (confirmEntry != null) hdrIcon = getIconStack(confirmEntry);
        else if (confirmOffre != null && confirmOffre.item != null) hdrIcon = confirmOffre.item;

        int iconBoxS = 36;
        int iconBoxX = cx + 12, iconBoxY = cy + (hdrH - iconBoxS) / 2;
        drawRect(iconBoxX, iconBoxY, iconBoxX + iconBoxS, iconBoxY + iconBoxS, 0xFF0A1424);
        GuiRenderUtils.drawRectOutline(iconBoxX, iconBoxY, iconBoxS, iconBoxS, hdrColMed);
        GuiRenderUtils.drawGradientRect(iconBoxX + 1, iconBoxY + 1, iconBoxX + iconBoxS - 1, iconBoxY + iconBoxS / 2,
                hdrColSoft, 0x00000000);
        if (hdrIcon != null) {
            GlStateManager.enableDepth();
            GlStateManager.pushMatrix();
            GlStateManager.translate(iconBoxX + 2, iconBoxY + 2, 0);
            GlStateManager.scale(2f, 2f, 1f);
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(hdrIcon, 0, 0);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            GlStateManager.disableDepth();
        }

        // Texte du header
        int textX = iconBoxX + iconBoxS + 12;

        // Badge categorie
        String catLabel = getCategoryLabel();
        int catLabelW = fontRendererObj.getStringWidth(catLabel);
        int catBadgeX = cx + cw - catLabelW - 22;
        int catBadgeY = cy + 14;
        drawRect(catBadgeX - 6, catBadgeY, catBadgeX + catLabelW + 6, catBadgeY + 14,
                (hdrCol & 0x00FFFFFF) | 0x88000000);
        GuiRenderUtils.drawRectOutline(catBadgeX - 6, catBadgeY, catLabelW + 12, 14,
                (hdrCol & 0x00FFFFFF) | 0xCC000000);
        drawRect(catBadgeX - 6, catBadgeY, catBadgeX - 4, catBadgeY + 14, hdrCol);
        fontRendererObj.drawStringWithShadow(catLabel, catBadgeX, catBadgeY + 3, hdrCol);

        // Nom de l'article
        String nomRaw = confirmOffre != null ? fmt(confirmOffre.nom)
                : (confirmEntry != null ? fmt(confirmEntry.nom) : "?");
        int maxNomW = catBadgeX - textX - 14;
        if (fontRendererObj.getStringWidth(nomRaw) > maxNomW)
            nomRaw = fontRendererObj.trimStringToWidth(nomRaw, maxNomW) + "..";
        fontRendererObj.drawStringWithShadow("§f§l" + nomRaw, textX, cy + 18, C_TEXT);

        // Sous-titre
        String subtitle = getSubtitle();
        if (subtitle != null) fontRendererObj.drawString("§7" + subtitle, textX, cy + 32, 0);

        // Confirmation tag
        fontRendererObj.drawString("§8>> CONFIRMATION D'ACHAT", textX, cy + 44, 0);

        // Corps
        int bodyX = cx + 14;
        int bodyW = cw - 28;
        int ly    = cy + hdrH + 10;

        boolean isPBPay = confirmPayPB;

        if (activeTab == Tab.PACKS && confirmEntry != null
                && confirmEntry.description != null && !confirmEntry.description.isEmpty()) {

            int nLines = Math.min(5, confirmEntry.description.size());
            int boxH = 14 + nLines * 11 + 6;
            drawRect(bodyX, ly, bodyX + bodyW, ly + boxH, 0x44000000);
            drawRect(bodyX, ly, bodyX + 3, ly + boxH, hdrCol);
            GuiRenderUtils.drawRectOutline(bodyX, ly, bodyW, boxH, (hdrCol & 0x00FFFFFF) | 0x55000000);
            fontRendererObj.drawString("§6§lCONTENU DU PACK", bodyX + 10, ly + 4, 0);
            int lly = ly + 16;
            for (int idx = 0; idx < nLines; idx++) {
                String clean = stripColor(fmt(confirmEntry.description.get(idx)));
                fontRendererObj.drawString("§e+ §f" + clean, bodyX + 12, lly, 0);
                lly += 11;
            }
            ly += boxH + 6;

            if (confirmEntry.prixPBP > confirmEntry.prixPB) {
                int saved = confirmEntry.prixPBP - confirmEntry.prixPB;
                int pct   = (int)(100L * saved / confirmEntry.prixPBP);

                int ecoH = 34;
                drawRect(bodyX, ly, bodyX + bodyW, ly + ecoH, 0x66003300);
                drawRect(bodyX, ly, bodyX + 3, ly + ecoH, C_OK);
                GuiRenderUtils.drawRectOutline(bodyX, ly, bodyW, ecoH, 0xAA33CC77);
                GuiRenderUtils.drawGradientRect(bodyX + 3, ly + 1, bodyX + bodyW - 1, ly + ecoH - 1,
                        0x3333CC77, 0x00000000);

                fontRendererObj.drawString("§a§l-" + pct + "%§r §7§lECONOMIE", bodyX + 10, ly + 4, 0);
                String prixCmp = "§8" + FMT.format(confirmEntry.prixPBP) + " PB §7-> §e§l" + FMT.format(confirmEntry.prixPB) + " §ePB";
                fontRendererObj.drawStringWithShadow(prixCmp, bodyX + 10, ly + 19, C_TEXT);
                String savedStr = "§a-" + saved + " PB";
                int svw = fontRendererObj.getStringWidth(savedStr);
                fontRendererObj.drawStringWithShadow(savedStr, bodyX + bodyW - svw - 10, ly + 19, C_OK);
                ly += ecoH + 4;
            }
        } else {
            if ((activeTab == Tab.GRADES || activeTab == Tab.KITS)
                    && confirmEntry != null && confirmEntry.description != null
                    && !confirmEntry.description.isEmpty()) {
                String teaser = stripColor(fmt(confirmEntry.description.get(0)));
                teaser = fontRendererObj.trimStringToWidth(teaser, bodyW - 16);
                drawRect(bodyX, ly + 3, bodyX + 3, ly + 11, hdrCol);
                fontRendererObj.drawString("§7" + teaser, bodyX + 10, ly + 4, 0);
                ly += 16;
            }

            int boxH = 36;
            drawRect(bodyX, ly, bodyX + bodyW, ly + boxH, 0x55000000);
            GuiRenderUtils.drawRectOutline(bodyX, ly, bodyW, boxH, 0x44FFFFFF);
            drawRect(bodyX + bodyW / 2, ly + 4, bodyX + bodyW / 2 + 1, ly + boxH - 4, 0x33FFFFFF);

            String priceVal = (isPBPay ? "§e§l" : "§6§l") + FMT.format(confirmPrice)
                    + (isPBPay ? " §ePB" : " §6$");
            fontRendererObj.drawString("§7PRIX", bodyX + 12, ly + 5, 0);
            fontRendererObj.drawStringWithShadow(priceVal, bodyX + 12, ly + 18, C_TEXT);

            String durStr = confirmTemporary ? "§b§l1 MOIS" : "§a§lPERMANENT";
            fontRendererObj.drawString("§7DUREE", bodyX + bodyW / 2 + 12, ly + 5, 0);
            fontRendererObj.drawStringWithShadow(durStr, bodyX + bodyW / 2 + 12, ly + 18, C_TEXT);

            ly += boxH + 6;
        }

        // Solde apres achat
        BoutiqueData d = BoutiquePacketHandler.getCached();
        boolean enough = true;
        if (d != null) {
            long after = isPBPay
                    ? Math.max(0L, (long) d.pb - confirmPrice)
                    : Math.max(0L, d.balance - confirmPrice);
            enough = isPBPay ? (long) d.pb >= confirmPrice : d.balance >= confirmPrice;
            String afterStr = "§7Solde apr\u00e8s :  " + (isPBPay ? "§e§l" : "§6§l")
                    + FMT.format(after) + (isPBPay ? " §7PB" : " §7$");
            fontRendererObj.drawStringWithShadow(afterStr, bodyX + 4, ly, C_TEXT);

            if (!enough) {
                String warn = "§lINSUFFISANT";
                int warnW = fontRendererObj.getStringWidth(warn);
                drawRect(bodyX + bodyW - warnW - 14, ly - 2, bodyX + bodyW - 2, ly + 10, 0x88440000);
                GuiRenderUtils.drawRectOutline(bodyX + bodyW - warnW - 14, ly - 2, warnW + 12, 12, 0xFFDD3333);
                fontRendererObj.drawStringWithShadow("§c" + warn, bodyX + bodyW - warnW - 8, ly, C_KO);
            } else {
                String ok = "§lOK";
                int okW = fontRendererObj.getStringWidth(ok);
                drawRect(bodyX + bodyW - okW - 12, ly - 2, bodyX + bodyW - 2, ly + 10, 0x55003300);
                GuiRenderUtils.drawRectOutline(bodyX + bodyW - okW - 12, ly - 2, okW + 10, 12, 0xFF33CC77);
                fontRendererObj.drawStringWithShadow("§a" + ok, bodyX + bodyW - okW - 7, ly, C_OK);
            }
        }

        // Boutons
        boolean hovNo = inBox(mx, my, cx + 14, by, bw, bh);
        int noBg   = hovNo ? 0xFF8A2828 : 0xFF3A1414;
        int noBord = hovNo ? 0xFFEE5555 : 0xFF883333;
        drawRect(cx + 14, by, cx + 14 + bw, by + bh, noBg);
        GuiRenderUtils.drawRectOutline(cx + 14, by, bw, bh, noBord);
        if (hovNo) {
            drawRect(cx + 14, by, cx + 17, by + bh, 0xFFEE5555);
            GuiRenderUtils.drawGradientRect(cx + 14, by, cx + 14 + bw, by + 4, 0x40FFFFFF, 0x00000000);
        }
        drawCenteredStr("§l§cANNULER", cx + 14 + bw / 2, by + (bh - 8) / 2, C_TEXT);

        boolean hovYes = inBox(mx, my, cx + cw - bw - 14, by, bw, bh);
        int yBg   = enough ? (hovYes ? 0xFF22A052 : 0xFF114526) : 0xFF252525;
        int yBord = enough ? (hovYes ? 0xFF66EE99 : 0xFF338855) : 0xFF444444;
        drawRect(cx + cw - bw - 14, by, cx + cw - 14, by + bh, yBg);
        GuiRenderUtils.drawRectOutline(cx + cw - bw - 14, by, bw, bh, yBord);
        if (enough) {
            if (hovYes) {
                drawRect(cx + cw - bw - 14, by, cx + cw - bw - 11, by + bh, 0xFF66EE99);
                GuiRenderUtils.drawGradientRect(cx + cw - bw - 14, by, cx + cw - 14, by + 4, 0x40FFFFFF, 0x00000000);
            }
            drawCenteredStr("§l§aCONFIRMER L'ACHAT", cx + cw - bw - 14 + bw / 2, by + (bh - 8) / 2, C_TEXT);
        } else {
            drawCenteredStr("§l§8SOLDE INSUFFISANT", cx + cw - bw - 14 + bw / 2, by + (bh - 8) / 2, C_MUTED2);
        }
    }

    private String getSubtitle() {
        if (confirmOffre != null) return "Article en \u00e9dition limit\u00e9e";
        switch (activeTab) {
            case GRADES:    return "Rang permanent du serveur";
            case KITS:      return "Equipement de combat";
            case COMMANDES: return "Commande utilitaire";
            case SPAWNERS:  return "G\u00e9n\u00e9rateur de mobs";
            case PACKS:     return "Pack avec rabais inclus";
            default:        return null;
        }
    }
    private String getCategoryLabel() {
        if (confirmOffre != null) return "OFFRE SPEC.";
        switch (activeTab) {
            case GRADES:    return "GRADE";
            case KITS:      return "KIT";
            case COMMANDES: return "COMMANDE";
            case SPAWNERS:  return "SPAWNER";
            case PACKS:     return "PACK";
            default:        return "ARTICLE";
        }
    }

    // =========================================================================
    // Helpers rendu
    // =========================================================================

    private void drawPriceBtn(int x, int y, int w, int h, boolean hov, String label, int textColor) {
        boolean isPB = (textColor == C_PB);
        // Monnaie ($) : vert \u00e9meraude sombre — lisible "transaction"
        // PB          : ambre chaud sombre   — lisible "premium"
        int bg     = isPB ? (hov ? 0xFF3A2C00 : 0xFF1D1600) : (hov ? 0xFF0D3520 : 0xFF061A10);
        int border = isPB ? (hov ? 0xFFCCAA00 : 0xFF775500) : (hov ? 0xFF44CC66 : 0xFF246634);
        drawRect(x, y, x + w, y + h, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, h, border);
        if (hov) GuiRenderUtils.drawGradientRect(x + 1, y + 1, x + w - 1, y + 3, 0x18FFFFFF, 0x00000000);
        int tw = fontRendererObj.getStringWidth(label);
        if (tw > w - 6) label = fontRendererObj.trimStringToWidth(label, w - 6);
        tw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, x + (w - tw) / 2, y + (h - 8) / 2, textColor);
    }

    private void drawCenteredStr(String s, int cx, int y, int col) {
        fontRendererObj.drawStringWithShadow(s, cx - fontRendererObj.getStringWidth(s) / 2f, y, col);
    }

    // =========================================================================
    // Helpers donn\u00e9es
    // =========================================================================

    private static ItemStack getIconStack(BoutiqueData.Entry e) {
        try {
            if (e.mob != null && !e.mob.isEmpty())
                return new ItemStack(net.minecraft.init.Blocks.mob_spawner);
        } catch (Exception ignored) {}
        if (e.icone == null || e.icone.isEmpty()) return null;
        String name = e.icone.toLowerCase();

        // Items custom MavenMCP (cobalt, ruby, tools sp\u00e9ciaux)
        try {
            switch (name) {
                case "cobalt_ingot":      return new ItemStack(net.minecraft.init.Items.cobalt_ingot);
                case "cobalt_sword":      return new ItemStack(net.minecraft.init.Items.cobalt_sword);
                case "cobalt_helmet":     return new ItemStack(net.minecraft.init.Items.cobalt_helmet);
                case "cobalt_chestplate": return new ItemStack(net.minecraft.init.Items.cobalt_chestplate);
                case "cobalt_leggings":   return new ItemStack(net.minecraft.init.Items.cobalt_leggings);
                case "cobalt_boots":      return new ItemStack(net.minecraft.init.Items.cobalt_boots);
                case "cobalt_pickaxe":    return new ItemStack(net.minecraft.init.Items.cobalt_pickaxe);
                case "cobalt_shovel":     return new ItemStack(net.minecraft.init.Items.cobalt_shovel);
                case "cobalt_axe":        return new ItemStack(net.minecraft.init.Items.cobalt_axe);
                case "cobalt_hoe":        return new ItemStack(net.minecraft.init.Items.cobalt_hoe);
                case "cobalt_bow":        return new ItemStack(net.minecraft.init.Items.cobalt_bow);
                case "cobalt_hammer":     return new ItemStack(net.minecraft.init.Items.cobalt_hammer);
                case "cobalt_apple":      return new ItemStack(net.minecraft.init.Items.cobalt_apple);
                case "ruby":              return new ItemStack(net.minecraft.init.Items.ruby);
                case "ruby_sword":        return new ItemStack(net.minecraft.init.Items.ruby_sword);
                case "ruby_helmet":       return new ItemStack(net.minecraft.init.Items.ruby_helmet);
                case "ruby_chestplate":   return new ItemStack(net.minecraft.init.Items.ruby_chestplate);
                case "ruby_leggings":     return new ItemStack(net.minecraft.init.Items.ruby_leggings);
                case "ruby_boots":        return new ItemStack(net.minecraft.init.Items.ruby_boots);
                case "ruby_pickaxe":      return new ItemStack(net.minecraft.init.Items.ruby_pickaxe);
                case "ruby_shovel":       return new ItemStack(net.minecraft.init.Items.ruby_shovel);
                case "ruby_axe":          return new ItemStack(net.minecraft.init.Items.ruby_axe);
                case "ruby_hoe":          return new ItemStack(net.minecraft.init.Items.ruby_hoe);
                case "ruby_bow":          return new ItemStack(net.minecraft.init.Items.ruby_bow);
                case "heal_stick":        return new ItemStack(net.minecraft.init.Items.heal_stick);
                case "multi_tool":        return new ItemStack(net.minecraft.init.Items.multi_tool);
                case "green_pumpkin_pie": return new ItemStack(net.minecraft.init.Items.green_pumpkin_pie);
            }
        } catch (Exception ignored) {}

        // Correction noms Bukkit → MCP
        if (name.equals("workbench")) name = "crafting_table";
        try {
            net.minecraft.block.Block b = net.minecraft.block.Block.getBlockFromName(name);
            if (b != null && b != net.minecraft.init.Blocks.air) return new ItemStack(b);
        } catch (Exception ignored) {}
        try {
            net.minecraft.item.Item it = net.minecraft.item.Item.getByNameOrId(name);
            if (it != null) return new ItemStack(it);
        } catch (Exception ignored) {}
        try {
            int id = Integer.parseInt(name.trim());
            net.minecraft.item.Item it = net.minecraft.item.Item.getItemById(id);
            if (it != null) return new ItemStack(it);
        } catch (Exception ignored) {}
        return null;
    }

    private static String buildSub(BoutiqueData.Entry e, boolean showTemporary) {
        if (e.mob != null && !e.mob.isEmpty()) return "§8Achat definitif";
        if (showTemporary) {
            if (e.duree > 0) {
                String dur = (e.duree >= 2500000) ? "1 mois" : formatDuration(e.duree);
                return "§8Duree : " + dur;
            }
            return "§8Achat definitif";
        } else {
            if (e.prixMonnaieP > 0 || e.prixPBP > 0) return "§aPermanent";
            return "§8Achat definitif";
        }
    }

    /** Dur\u00e9e affich\u00e9e dans la modale de confirmation. */
    private static String buildDurLabel(BoutiqueData.Entry e) {
        if (e == null) return "Definitif";
        if (e.mob != null && !e.mob.isEmpty()) return "Definitif";
        if (e.duree <= 0) return "Definitif";
        return (e.duree >= 2500000) ? "1 mois" : formatDuration(e.duree);
    }

    /** R\u00e9sout l'item \u00e0 afficher pour une offre sp\u00e9ciale (override pour items custom). */
    private static ItemStack resolveOfferIcon(BoutiqueData.OffreData o) {
        if (o == null) return null;
        // Ruby affich\u00e9 avec l'item ruby custom (pas l'\u00e9meraude vanille)
        String idLow = o.id != null ? o.id.toLowerCase() : "";
        String nomLow = stripColor(o.nom != null ? o.nom : "").toLowerCase();
        if (idLow.contains("ruby") || nomLow.contains("ruby")) {
            try { return new ItemStack(net.minecraft.init.Items.ruby); } catch (Exception ignored) {}
        }
        return o.item;
    }

    /** Affiche un panneau tooltip style GuiWiki pour l'article survol\u00e9. */
    private void drawEntryTooltip(BoutiqueData.Entry e, int mx, int my) {
        if (e == null) return;

        int[] tabColors = { 0xFF9944CC, 0xFFCC4488, 0xFF2299CC, 0xFF44BB66, 0xFFFF8800, 0xFFCCAA00 };
        int barCol = tabColors[Math.min(activeTab.ordinal(), tabColors.length - 1)];

        // Titre
        String title = e.nom != null ? stripColor(fmt(e.nom)) : "";

        // Description avec retour a la ligne automatique
        int wrapW = 180;
        List<String> descLines = new ArrayList<>();
        if (e.description != null) {
            for (String d : e.description) {
                String clean = fmt(d);
                if (!clean.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    List<String> wrapped = fontRendererObj.listFormattedStringToWidth(clean, wrapW);
                    descLines.addAll(wrapped);
                }
            }
        }

        // Pied : dur\u00e9e uniquement, sans redondance
        String footerLine;
        boolean hasPerm = (e.prixMonnaieP > 0 || e.prixPBP > 0);
        if ((e.mob != null && !e.mob.isEmpty()) || e.duree <= 0) {
            footerLine = "Achat definitif";
        } else {
            String dur = (e.duree >= 2500000) ? "1 mois" : formatDuration(e.duree);
            footerLine = "Duree : " + dur + (hasPerm ? "  ·  Permanent disponible" : "");
        }

        // Dimensions
        int padX = 10, padTop = 7, padBot = 7;
        int titleW = fontRendererObj.getStringWidth(title);
        int footW  = fontRendererObj.getStringWidth(footerLine);
        int maxDescW = 0;
        for (String l : descLines) maxDescW = Math.max(maxDescW, fontRendererObj.getStringWidth(l));
        int tw = Math.max(Math.max(titleW, maxDescW) + padX * 2, Math.max(140, footW + padX * 2 + 8));

        int lineH = 10;
        int th = padTop
               + 10                                           // titre
               + 4                                            // separateur titre
               + (descLines.isEmpty() ? 0 : descLines.size() * lineH + 4)
               + 1                                            // separateur pied
               + 4
               + 9                                            // pied
               + padBot;

        // Position (evite les bords de l'ecran)
        int tx = mx + 14;
        int ty = my - th / 2;
        if (tx + tw > width  - 4) tx = mx - tw - 6;
        if (ty + th > height - 4) ty = height - th - 4;
        if (ty < 4) ty = 4;

        // Fond
        GlStateManager.disableDepth();
        GuiRenderUtils.drawShadow(tx, ty, tw, th, 5, 0x70);
        drawRect(tx, ty, tx + tw, ty + th, 0xF2030B16);
        drawRect(tx, ty, tx + 2, ty + th, barCol);
        GuiRenderUtils.drawGradientRect(tx + 2, ty, tx + 16, ty + th,
                (barCol & 0x00FFFFFF) | 0x28000000, 0x00000000);
        GuiRenderUtils.drawRectOutline(tx, ty, tw, th, 0x30FFFFFF);

        int ly = ty + padTop;

        // Titre (couleur d'accent de l'onglet)
        fontRendererObj.drawStringWithShadow(title, tx + padX, ly, barCol);
        ly += 10;

        // Separateur sous le titre
        GuiRenderUtils.drawGradientRect(tx + padX, ly + 1, tx + tw - padX, ly + 2,
                (barCol & 0x00FFFFFF) | 0x55000000, 0x00000000);
        ly += 5;

        // Corps de la description
        for (String l : descLines) {
            fontRendererObj.drawString(l, tx + padX, ly, 0xFFB0B6C0);
            ly += lineH;
        }
        if (!descLines.isEmpty()) ly += 4;

        // Separateur avant le pied
        drawRect(tx + padX, ly, tx + tw - padX, ly + 1, 0x20FFFFFF);
        ly += 4;

        // Pied : petit carre accent + texte duree
        drawRect(tx + padX, ly + 2, tx + padX + 4, ly + 7,
                (barCol & 0x00FFFFFF) | 0xAA000000);
        fontRendererObj.drawString(footerLine, tx + padX + 8, ly, 0xFF667080);

        GlStateManager.enableDepth();
    }

    // =========================================================================
    // Utilitaires
    // =========================================================================

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static boolean inBox(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx <= x + w && my <= y + h;
    }

    private static String fmt(String s) {
        return s == null ? "" : s.replace('&', '\u00a7');
    }

    private static String stripColor(String s) {
        return s == null ? "" : s.replaceAll("(?i)[\u00a7&].", "");
    }

    private static String formatDuration(int seconds) {
        if (seconds <= 0) return "\u2014";
        long d = seconds / 86400, h = (seconds % 86400) / 3600,
             m = (seconds % 3600) / 60, s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("j ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0 && d == 0) sb.append(m).append("min ");
        if (d == 0 && h == 0 && m == 0) sb.append(s).append("s");
        String r = sb.toString().trim();
        return r.isEmpty() ? "\u2014" : r;
    }
}
