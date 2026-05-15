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
        int cw = 340;
        int ch = computeModalHeight();
        int cx = px + pw / 2 - cw / 2, cy = py + ph / 2 - ch / 2;
        int bw = 140, bh = 24, by = cy + ch - bh - 10;
        if (inBox(mx, my, cx + 16, by, bw, bh))                { confirmOpen = false; }
        else if (inBox(mx, my, cx + cw - 16 - bw, by, bw, bh)) { doConfirm(); confirmOpen = false; }
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
                // Nom du pack
                fontRendererObj.drawStringWithShadow(e.nom != null ? fmt(e.nom) : "", textX, rowY + 5, C_GOLD);

                // Badge économie + prix original — alignés à droite, sur la ligne du nom
                int badgeRightEdge = pbBtnX - 6;
                if (e.prixPBP > 0 && e.prixPB > 0) {
                    int saved = e.prixPBP - e.prixPB;
                    int pct   = (int)(100L * saved / e.prixPBP);
                    String saveStr = "-" + pct + "%";
                    int bw2 = fontRendererObj.getStringWidth(saveStr) + 8;
                    int bx2 = badgeRightEdge - bw2;
                    int by2 = rowY + 4;
                    drawRect(bx2, by2, bx2 + bw2, by2 + 11, 0x88003300);
                    GuiRenderUtils.drawRectOutline(bx2, by2, bw2, 11, C_OK);
                    fontRendererObj.drawString("§a" + saveStr, bx2 + 4, by2 + 2, 0);
                    // Prix original barré
                    String origStr = FMT.format(e.prixPBP) + " PB";
                    int ow = fontRendererObj.getStringWidth(origStr);
                    int ox = bx2 - ow - 5;
                    fontRendererObj.drawString("§8" + origStr, ox, by2 + 2, 0);
                    drawRect(ox, by2 + 5, ox + ow, by2 + 6, 0x88888888);
                    badgeRightEdge = ox - 6;
                }

                // Contenu : description sur la ligne du bas, trimée à gauche du badge
                if (e.description != null && !e.description.isEmpty()) {
                    int descTrimW = Math.max(40, badgeRightEdge - textX);
                    StringBuilder sb = new StringBuilder();
                    for (String dl : e.description) {
                        String cl = stripColor(fmt(dl));
                        if (!cl.isEmpty()) { if (sb.length() > 0) sb.append("  ·  "); sb.append(cl); }
                    }
                    String contenu = fontRendererObj.trimStringToWidth(sb.toString(), descTrimW);
                    fontRendererObj.drawString("§8" + contenu, textX, rowY + 18, 0);
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
        drawRect(0, 0, width, height, 0xCC000011);

        int cw = 340;
        int ch = computeModalHeight();
        int cx = px + pw / 2 - cw / 2, cy = py + ph / 2 - ch / 2;
        int bw = 140, bh = 24, by = cy + ch - bh - 10;
        int padX = 16;

        int[] tabCols = { 0xFF9944CC, 0xFFCC4488, 0xFF2299CC, 0xFF44BB66, 0xFFFF8800, 0xFFCCAA00 };
        int accentCol = (confirmOffre != null) ? 0xFFCCAA00
                : tabCols[Math.min(activeTab.ordinal(), tabCols.length - 1)];

        // Fond sobre + bordure accent
        drawRect(cx, cy, cx + cw, cy + ch, 0xF2040912);
        drawRect(cx, cy, cx + cw, cy + 3, accentCol);
        GuiRenderUtils.drawGradientRect(cx, cy + 3, cx + cw, cy + 12,
                (accentCol & 0x00FFFFFF) | 0x40000000, 0x00000000);
        GuiRenderUtils.drawRectOutline(cx, cy, cw, ch, (accentCol & 0x00FFFFFF) | 0x88000000);
        GuiRenderUtils.drawRectOutline(cx + 1, cy + 1, cw - 2, ch - 2, 0x18FFFFFF);

        int ly = cy + 14;

        // ── Icone + Nom ──────────────────────────────────────────────────────
        ItemStack hdrIcon = confirmEntry != null ? getIconStack(confirmEntry)
                : (confirmOffre != null && confirmOffre.item != null ? confirmOffre.item : null);
        if (hdrIcon != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(hdrIcon, cx + padX, ly);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
        }
        String nom = confirmOffre != null ? fmt(confirmOffre.nom)
                : (confirmEntry != null ? fmt(confirmEntry.nom) : "?");
        int nomMaxW = cw - padX - 22 - padX;
        if (fontRendererObj.getStringWidth(nom) > nomMaxW)
            nom = fontRendererObj.trimStringToWidth(nom, nomMaxW) + "..";
        fontRendererObj.drawStringWithShadow("§f§l" + nom, cx + padX + 22, ly + 2, C_TEXT);
        fontRendererObj.drawString("§8" + getCategoryLabel(), cx + padX + 22, ly + 12, 0);
        String subtitle = getSubtitle();
        if (subtitle != null) {
            int sw = fontRendererObj.getStringWidth("§8" + subtitle);
            fontRendererObj.drawString("§8" + subtitle, cx + cw - padX - sw, ly + 2, 0);
        }
        ly += 26;

        // Séparateur
        drawRect(cx + padX, ly, cx + cw - padX, ly + 1, 0x28FFFFFF);
        ly += 8;

        // ── Bloc Prix / Durée ────────────────────────────────────────────────
        int halfW = (cw - padX * 2) / 2;
        int boxH = 32;
        drawRect(cx + padX, ly, cx + cw - padX, ly + boxH, 0x22000000);
        GuiRenderUtils.drawRectOutline(cx + padX, ly, cw - padX * 2, boxH, 0x22FFFFFF);
        drawRect(cx + padX + halfW, ly + 4, cx + padX + halfW + 1, ly + boxH - 4, 0x20FFFFFF);

        boolean isPBPay = confirmPayPB;
        String priceVal = (isPBPay ? "§e§l" : "§6§l") + FMT.format(confirmPrice)
                + (isPBPay ? " §ePB" : " §6$");
        fontRendererObj.drawString("§8Prix", cx + padX + 8, ly + 4, 0);
        fontRendererObj.drawStringWithShadow(priceVal, cx + padX + 8, ly + 15, C_TEXT);

        String durStr = confirmTemporary ? "§b§l1 mois" : "§a§lPermanent";
        fontRendererObj.drawString("§8Duree", cx + padX + halfW + 8, ly + 4, 0);
        fontRendererObj.drawStringWithShadow(durStr, cx + padX + halfW + 8, ly + 15, C_TEXT);
        ly += boxH + 8;

        // ── Contenu pack / teaser grade ──────────────────────────────────────
        if (activeTab == Tab.PACKS && confirmEntry != null
                && confirmEntry.description != null && !confirmEntry.description.isEmpty()) {
            int nLines = Math.min(5, confirmEntry.description.size());
            int dboxH = 12 + nLines * 11 + 4;
            drawRect(cx + padX, ly, cx + cw - padX, ly + dboxH, 0x28000000);
            drawRect(cx + padX, ly, cx + padX + 2, ly + dboxH, accentCol);
            fontRendererObj.drawString("§7§lContenu :", cx + padX + 8, ly + 3, 0);
            int lly = ly + 13;
            for (int idx = 0; idx < nLines; idx++) {
                fontRendererObj.drawString("§7+ §f" + stripColor(fmt(confirmEntry.description.get(idx))),
                        cx + padX + 8, lly, 0);
                lly += 11;
            }
            ly += dboxH + 6;
            if (confirmEntry.prixPBP > confirmEntry.prixPB) {
                int saved = confirmEntry.prixPBP - confirmEntry.prixPB;
                int pct = (int)(100L * saved / confirmEntry.prixPBP);
                fontRendererObj.drawString("§a-" + pct + "% §7eco. §8("
                        + FMT.format(confirmEntry.prixPBP) + " -> §e"
                        + FMT.format(confirmEntry.prixPB) + " PB§8)",
                        cx + padX + 4, ly, 0);
                ly += 13;
            }
        } else if ((activeTab == Tab.GRADES || activeTab == Tab.KITS)
                && confirmEntry != null && confirmEntry.description != null
                && !confirmEntry.description.isEmpty()) {
            String teaser = fontRendererObj.trimStringToWidth(
                    stripColor(fmt(confirmEntry.description.get(0))), cw - padX * 2 - 12);
            fontRendererObj.drawString("§8" + teaser, cx + padX + 4, ly, 0);
            ly += 13;
        }

        // ── Solde après achat ────────────────────────────────────────────────
        BoutiqueData d = BoutiquePacketHandler.getCached();
        boolean enough = true;
        if (d != null) {
            long after = isPBPay
                    ? Math.max(0L, (long) d.pb - confirmPrice)
                    : Math.max(0L, d.balance - confirmPrice);
            enough = isPBPay ? (long) d.pb >= confirmPrice : d.balance >= confirmPrice;
            String afterStr = "§7Solde apres : " + (isPBPay ? "§e" : "§6")
                    + FMT.format(after) + (isPBPay ? " §7PB" : " §7$");
            fontRendererObj.drawStringWithShadow(afterStr, cx + padX, ly, C_TEXT);
            if (!enough) {
                int aw = fontRendererObj.getStringWidth(afterStr);
                int badgeX = cx + padX + aw + 8;
                drawRect(badgeX, ly - 1, badgeX + 56, ly + 9, 0x66330000);
                GuiRenderUtils.drawRectOutline(badgeX, ly - 1, 56, 10, 0xFFCC3333);
                fontRendererObj.drawString("§c INSUF.", badgeX + 4, ly, 0);
            }
        }

        // ── Boutons ──────────────────────────────────────────────────────────
        boolean hovNo = inBox(mx, my, cx + padX, by, bw, bh);
        drawRect(cx + padX, by, cx + padX + bw, by + bh, hovNo ? 0xFF5C1E1E : 0xFF2E0E0E);
        GuiRenderUtils.drawRectOutline(cx + padX, by, bw, bh, hovNo ? 0xFFDD4444 : 0xFF602020);
        if (hovNo) drawRect(cx + padX, by, cx + padX + bw, by + 1, 0x55FFFFFF);
        drawCenteredStr("§cAnnuler", cx + padX + bw / 2, by + (bh - 8) / 2, C_TEXT);

        boolean hovYes = inBox(mx, my, cx + cw - padX - bw, by, bw, bh);
        int yBg   = enough ? (hovYes ? 0xFF1E5C30 : 0xFF0E2E18) : 0xFF1E1E1E;
        int yBord = enough ? (hovYes ? 0xFF55DD77 : 0xFF206030) : 0xFF333333;
        drawRect(cx + cw - padX - bw, by, cx + cw - padX, by + bh, yBg);
        GuiRenderUtils.drawRectOutline(cx + cw - padX - bw, by, bw, bh, yBord);
        if (enough && hovYes) drawRect(cx + cw - padX - bw, by, cx + cw - padX, by + 1, 0x55FFFFFF);
        String confirmLabel = enough ? "§aConfirmer" : "§8Solde insuf.";
        drawCenteredStr(confirmLabel, cx + cw - padX - bw + bw / 2, by + (bh - 8) / 2,
                enough ? C_TEXT : C_MUTED2);

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

    /** Affiche un panneau tooltip premium pour l'article survole. */
    private void drawEntryTooltip(BoutiqueData.Entry e, int mx, int my) {
        if (e == null) return;

        int[] tabColors = { 0xFF9944CC, 0xFFCC4488, 0xFF2299CC, 0xFF44BB66, 0xFFFF8800, 0xFFCCAA00 };
        int barCol = tabColors[Math.min(activeTab.ordinal(), tabColors.length - 1)];

        String title = e.nom != null ? stripColor(fmt(e.nom)) : "";
        String catLabel = getCategoryLabel();

        // Description — chaque ligne conserve ses codes couleur
        int wrapW = 190;
        List<String>  descLines = new ArrayList<>();
        List<Boolean> descIsCont = new ArrayList<>();
        if (e.description != null) {
            for (String dl : e.description) {
                String clean = fmt(dl);
                if (!clean.isEmpty()) {
                    List<String> wrapped = fontRendererObj.listFormattedStringToWidth(clean, wrapW - 8);
                    for (int wi = 0; wi < wrapped.size(); wi++) {
                        descLines.add(wrapped.get(wi));
                        descIsCont.add(wi > 0);
                    }
                }
            }
        }

        // Accroche marketing selon catégorie (si desc vide ou complément)
        String hook = null;
        if (descLines.isEmpty()) {
            switch (activeTab) {
                case GRADES:    hook = "§7Débloquez de nouveaux avantages en jeu !"; break;
                case KITS:      hook = "§7Partez prêt au combat dès le spawn !"; break;
                case COMMANDES: hook = "§7Gagnez du temps et de la facilité !"; break;
                case SPAWNERS:  hook = "§7Générez des ressources automatiquement !"; break;
                case PACKS:     hook = "§7La meilleure offre du moment !"; break;
                default: break;
            }
        }

        // Prix selon le toggle actuel
        boolean usePerma = !buyTemporary && (e.prixMonnaieP > 0 || e.prixPBP > 0);
        long showMoney = usePerma ? e.prixMonnaieP : e.prixMonnaie;
        int  showPB    = usePerma ? e.prixPBP      : e.prixPB;
        boolean hasPerm = (e.prixMonnaieP > 0 || e.prixPBP > 0);

        String durInfo;
        boolean isDefinitif = (e.mob != null && !e.mob.isEmpty()) || e.duree <= 0;
        if (isDefinitif) {
            durInfo = "§a✔ §7Achat §adéfinitif";
        } else {
            String dur = (e.duree >= 2500000) ? "1 mois" : formatDuration(e.duree);
            durInfo = "§7⏱ §7Durée : §f" + dur + (hasPerm && buyTemporary ? "  §a(perma dispo)" : "");
        }

        // Dimensions
        int padX = 10, padTop = 8, padBot = 8;
        int catBadgeH = 12;
        int lineH = 10;
        int descCount = descLines.size() + (hook != null ? 1 : 0);

        // Largeur maximale
        int titleW     = fontRendererObj.getStringWidth("§l" + title);
        int catLabelW  = fontRendererObj.getStringWidth(catLabel) + 10;
        int maxDescW   = 0;
        for (String l : descLines) maxDescW = Math.max(maxDescW, fontRendererObj.getStringWidth(l));
        if (hook != null) maxDescW = Math.max(maxDescW, fontRendererObj.getStringWidth(stripColor(hook)));
        int priceLineW = 0;
        if (showMoney > 0) priceLineW = Math.max(priceLineW, fontRendererObj.getStringWidth("§6" + FMT.format(showMoney) + " $"));
        if (showPB    > 0) priceLineW = Math.max(priceLineW, fontRendererObj.getStringWidth("§e" + FMT.format(showPB)    + " PB"));
        int priceCount = (showMoney > 0 ? 1 : 0) + (showPB > 0 ? 1 : 0);
        int footW      = fontRendererObj.getStringWidth(stripColor(durInfo));

        int tw = Math.max(Math.max(titleW + catLabelW + 12, maxDescW + 8),
                          Math.max(Math.max(140, footW), priceLineW)) + padX * 2 + 8;
        tw = Math.min(tw, wrapW + padX * 2 + 12);

        int th = padTop
                + catBadgeH + 4          // badge catégorie
                + 10 + 4                 // titre + séparateur
                + (descCount > 0 ? descCount * lineH + 2 : 0)
                + (descCount > 0 ? 6 : 0)
                 + (priceCount > 0 ? 17 + priceCount * lineH + 4 : 0) // encadré prix
                + 1 + 4 + 9              // séparateur + footer durée
                + padBot;

        // Position (évite les bords)
        int tx = mx + 14;
        int ty = my - th / 2;
        if (tx + tw > width  - 4) tx = mx - tw - 6;
        if (ty + th > height - 4) ty = height - th - 4;
        if (ty < 4) ty = 4;

        GlStateManager.disableDepth();

        // ── Fond panel ───────────────────────────────────────────────────────
        GuiRenderUtils.drawShadow(tx, ty, tw, th, 6, 0x80);
        drawRect(tx, ty, tx + tw, ty + th, 0xF4030B16);
        drawRect(tx, ty, tx + 3, ty + th, barCol);                              // barre gauche
        drawRect(tx + 3, ty, tx + tw, ty + 2, (barCol & 0x00FFFFFF) | 0x55000000); // ligne top
        GuiRenderUtils.drawRectOutline(tx, ty, tw, th, 0x22FFFFFF);

        int ly = ty + padTop;

        // ── Badge catégorie ──────────────────────────────────────────────────
        int badgeW = fontRendererObj.getStringWidth(catLabel) + 8;
        drawRect(tx + padX + 4, ly, tx + padX + 4 + badgeW, ly + catBadgeH,
                (barCol & 0x00FFFFFF) | 0x3A000000);
        GuiRenderUtils.drawRectOutline(tx + padX + 4, ly, badgeW, catBadgeH,
                (barCol & 0x00FFFFFF) | 0x77000000);
        fontRendererObj.drawString(catLabel, tx + padX + 8, ly + 2,
                (barCol & 0x00FFFFFF) | 0xDD000000);
        ly += catBadgeH + 4;

        // ── Titre ────────────────────────────────────────────────────────────
        fontRendererObj.drawStringWithShadow("§l" + title, tx + padX + 4, ly, barCol);
        ly += 10;

        // ── Séparateur simple ────────────────────────────────────────────────
        drawRect(tx + padX + 2, ly + 1, tx + tw - padX, ly + 2, (barCol & 0x00FFFFFF) | 0x44000000);
        ly += 5;

        // ── Description avec puces colorées ──────────────────────────────────
        if (descCount > 0) {
            int bulletW = fontRendererObj.getStringWidth("§7✦ ");
            for (int di = 0; di < descLines.size(); di++) {
                String l = descLines.get(di);
                boolean isCont = di < descIsCont.size() && descIsCont.get(di);
                if (isCont) {
                    // Ligne de continuation : indentation alignée avec le texte de la puce
                    fontRendererObj.drawString(l, tx + padX + 4 + bulletW, ly, 0xFFCCDDFF);
                } else {
                    fontRendererObj.drawString("§7✦ ", tx + padX + 4, ly, 0);
                    fontRendererObj.drawString(l, tx + padX + 4 + bulletW, ly, 0xFFCCDDFF);
                }
                ly += lineH;
            }
            if (hook != null) {
                fontRendererObj.drawString("§7» §o" + stripColor(hook), tx + padX + 4, ly, 0xFFAABBCC);
                ly += lineH;
            }
            ly += 6;
        }

        // ── Encadré Prix ─────────────────────────────────────────────────────
        if (priceCount > 0) {
            int pboxH = 13 + priceCount * lineH + 2;
            drawRect(tx + padX + 2, ly, tx + tw - padX - 2, ly + pboxH, 0x22000000);
            GuiRenderUtils.drawRectOutline(tx + padX + 2, ly, tw - padX * 2 - 4, pboxH, 0x20FFFFFF);
            fontRendererObj.drawString("§8Prix :", tx + padX + 6, ly + 3, 0);
            int ply = ly + 13;
            if (showMoney > 0) {
                fontRendererObj.drawStringWithShadow("§6✦ §6" + FMT.format(showMoney) + " §7$ monnaie",
                        tx + padX + 6, ply, C_TEXT);
                ply += lineH;
            }
            if (showPB > 0) {
                fontRendererObj.drawStringWithShadow("§e✦ §e" + FMT.format(showPB) + " §7PB",
                        tx + padX + 6, ply, C_TEXT);
                ply += lineH;
            }
            ly += pboxH + 4;
        }

        // ── Séparateur footer ─────────────────────────────────────────────────
        drawRect(tx + padX + 2, ly, tx + tw - padX - 2, ly + 1, 0x18FFFFFF);
        ly += 4;

        // ── Footer durée ──────────────────────────────────────────────────────
        fontRendererObj.drawString(durInfo, tx + padX + 4, ly, 0);

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
