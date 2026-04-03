package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.custompackets.data.HdvListing;
import net.minecraft.client.custompackets.data.PlayerData;
import net.minecraft.client.custompackets.handler.HdvPacketHandler;
import net.minecraft.client.custompackets.handler.PlayerDataHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiHDV extends GuiScreen {

    // ── Palette de couleurs ───────────────────────────────────────────────────
    private static final int C_BG       = 0xF2040912;
    private static final int C_BORDER   = 0xFF1A3A6A;
    private static final int C_ACCENT   = 0xFF2277EE;
    private static final int C_ACCENT2  = 0xFF55AAFF;
    private static final int C_HEADER   = 0xFF030B16;
    private static final int C_GOLD     = 0xFFFFD700;
    private static final int C_GREEN    = 0xFF33CC77;
    private static final int C_RED      = 0xFFEE3333;
    private static final int C_GRAY     = 0xFF778899;
    private static final int C_WHITE    = 0xFFFFFFFF;
    private static final int C_SEL      = 0x441A4A2A;
    private static final int C_HOV      = 0x331A3060;
    private static final int C_SOLD_BG  = 0x33226A22;
    private static final int C_PANEL    = 0xFF070D18;

    // ── Onglets ───────────────────────────────────────────────────────────────
    private static final int TAB_BUY  = 0;
    private static final int TAB_MINE = 1;
    private static final int TAB_SELL = 2;
    private int activeTab = TAB_BUY;

    // ── Dimensions du panneau ─────────────────────────────────────────────────
    private int px, py, pw, ph;

    // ── Données ──────────────────────────────────────────────────────────────
    private final List<HdvListing> listings   = new ArrayList<>();
    private final List<HdvListing> myListings = new ArrayList<>();
    private long    pendingEarnings = 0L;
    private long    playerBalance   = 0L;
    private boolean loading         = true;
    private boolean loadingMine     = true;
    private String  statusMsg       = "";
    private int     statusTimer     = 0;
    private boolean statusOk        = true;

    // ── Filtrage & Tri ────────────────────────────────────────────────────────
    private int sortMode = 0; // 0: Recent, 1: Prix croissant, 2: Prix decroissant

    // ── Achat ─────────────────────────────────────────────────────────────────
    private HdvListing confirmListing = null;

    // ── Vente ─────────────────────────────────────────────────────────────────
    private ItemStack    sellItem  = null;
    private int          sellSlot  = -1;
    private int          sellQty   = 1;
    private long         sellPrice = 100L;
    private GuiTextField priceField;
    private GuiTextField qtyField;

    // ── Recherche ─────────────────────────────────────────────────────────────
    private GuiTextField searchField;

    // ── Scroll ────────────────────────────────────────────────────────────────
    private int scroll     = 0;
    private int scrollMine = 0;
    private static final int ROW_H = 28;
    private int listY0, listH;
    private int listMineY0, listMineH;

    private enum PendingAction { NONE, CANCEL, COLLECT }
    private PendingAction pendingAction = PendingAction.NONE;

    // ── Tooltip ───────────────────────────────────────────────────────────────
    private ItemStack tooltipItem = null;
    private int tooltipX, tooltipY;

    // ── Hit-boxes ─────────────────────────────────────────────────────────────
    private final int[]   hbClose        = new int[4];
    private final int[]   hbTabBuy       = new int[4];
    private final int[]   hbTabMine      = new int[4];
    private final int[]   hbTabSell      = new int[4];
    private final int[]   hbSearchBtn    = new int[4];
    private final int[]   hbClearSearch  = new int[4];
    private final int[]   hbSort         = new int[4];
    private final int[]   hbCollect      = new int[4];
    private final int[]   hbSellBtn      = new int[4];
    private final int[]   hbScrollUp     = new int[4];
    private final int[]   hbScrollDn     = new int[4];
    private final int[]   hbScrollMineUp = new int[4];
    private final int[]   hbScrollMineDn = new int[4];
    private final int[]   hbQtyM10       = new int[4];
    private final int[]   hbQtyM1        = new int[4];
    private final int[]   hbQtyP1        = new int[4];
    private final int[]   hbQtyP10       = new int[4];
    private final int[]   hbPrM1000      = new int[4];
    private final int[]   hbPrM100       = new int[4];
    private final int[]   hbPrM10        = new int[4];
    private final int[]   hbPrP10        = new int[4];
    private final int[]   hbPrP100       = new int[4];
    private final int[]   hbPrP1000      = new int[4];
    private final int[]   hbPrFld        = new int[4];
    private final int[]   hbOkBuy        = new int[4];
    private final int[]   hbCancelBuy    = new int[4];
    private final int[][] hbRows         = new int[20][4];
    private final int[][] hbCancelRow    = new int[20][4];
    private final int[][] hbInvSlots     = new int[36][4];

    @Override
    public void initGui() {
        net.minecraft.client.custompackets.CustomPacketSystem.init();

        pw = Math.min(800, width  - 16);
        ph = Math.min(490, height - 16);
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        searchField = new GuiTextField(0, fontRendererObj, px + 12, py + 36, pw - 180, 12);
        searchField.setMaxStringLength(48);

        priceField = new GuiTextField(1, fontRendererObj, 0, 0, 80, 12);
        priceField.setMaxStringLength(10);
        priceField.setText(String.valueOf(sellPrice));

        qtyField = new GuiTextField(2, fontRendererObj, 0, 0, 40, 12);
        qtyField.setMaxStringLength(4);
        qtyField.setText(String.valueOf(sellQty));

        invalidateHb();

        HdvPacketHandler.setListListener(nl -> Minecraft.getMinecraft().addScheduledTask(() -> {
            onListingsReceived(nl);
        }));

        HdvPacketHandler.setActionListener((ok, msg) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            statusMsg = msg; statusTimer = 160; statusOk = ok;
            PendingAction action = pendingAction;
            pendingAction = PendingAction.NONE;
            if (ok) {
                if (action == PendingAction.CANCEL || action == PendingAction.COLLECT) {
                    if (action == PendingAction.CANCEL) {
                        listings.clear(); loading = true;
                        HdvPacketHandler.requestList(0, "");
                    } else { // COLLECT
                        HdvPacketHandler.showCollectMessage(pendingEarnings);
                        myListings.clear(); loadingMine = true;
                        HdvPacketHandler.requestMyListings();
                    }
                } else {
                    listings.clear(); loading = true;
                    myListings.clear(); loadingMine = true;
                    HdvPacketHandler.requestList(0, "");
                    HdvPacketHandler.requestMyListings();
                }
            }
        }));

        HdvPacketHandler.setMyListingsListener((mine, earnings) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            onMyListingsReceived(mine, earnings);
        }));

        PlayerDataHandler.setListener(data -> playerBalance = data.getBalance());
        PlayerData pd = PlayerDataHandler.getCachedData();
        if (pd != null) playerBalance = pd.getBalance();

        List<HdvListing> cached = new ArrayList<>(HdvPacketHandler.getCachedListings());
        if (!cached.isEmpty()) { listings.clear(); listings.addAll(cached); loading = false; }

        List<HdvListing> cachedMine = new ArrayList<>(HdvPacketHandler.getCachedMyListings());
        if (!cachedMine.isEmpty()) {
            myListings.clear(); myListings.addAll(cachedMine);
            pendingEarnings = HdvPacketHandler.getCachedPendingEarnings(); loadingMine = false;
        }

        requestList();
        HdvPacketHandler.requestMyListings();
    }

    public void onListingsReceived(List<HdvListing> nl) {
        listings.clear(); listings.addAll(nl); loading = false; scroll = 0;
    }

    public void onMyListingsReceived(List<HdvListing> mine, long earnings) {
        myListings.clear(); myListings.addAll(mine); pendingEarnings = earnings; loadingMine = false;
    }

    private void requestList() {
        loading = true; scroll = 0;
        String f = searchField != null ? searchField.getText().trim() : "";
        HdvPacketHandler.requestList(0, f);
    }

    private void invalidateHb() {
        hbClose[2] = hbTabBuy[2] = hbTabMine[2] = hbTabSell[2] = 0;
        hbSearchBtn[2] = hbClearSearch[2] = hbSort[2] = hbCollect[2] = hbSellBtn[2] = 0;
        hbScrollUp[2] = hbScrollDn[2] = hbScrollMineUp[2] = hbScrollMineDn[2] = 0;
        hbQtyM10[2] = hbQtyM1[2] = hbQtyP1[2] = hbQtyP10[2] = 0;
        hbPrM1000[2] = hbPrM100[2] = hbPrM10[2] = 0;
        hbPrP10[2] = hbPrP100[2] = hbPrP1000[2] = 0;
        hbPrFld[2] = hbOkBuy[2] = hbCancelBuy[2] = 0;
        for (int[] h : hbRows)      h[2] = 0;
        for (int[] h : hbCancelRow) h[2] = 0;
        for (int[] h : hbInvSlots)  h[2] = 0;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xBB000011);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Ombre portée du panel
        drawRect(px + 4, py + 4, px + pw + 4, py + ph + 4, 0x44000000);
        drawRect(px, py, px + pw, py + ph, C_BG);
        
        // Bordures dégradées simulation
        drawRect(px, py, px + pw, py + 2, C_ACCENT);
        drawRect(px, py + ph - 2, px + pw, py + ph, C_BORDER);
        drawRect(px, py, px + 1, py + ph, C_BORDER);
        drawRect(px + pw - 1, py, px + pw, py + ph, C_BORDER);

        tooltipItem = null;
        drawHeader(mx, my);
        drawTabs(mx, my);

        switch (activeTab) {
            case TAB_BUY:  drawBuyTab(mx, my);  break;
            case TAB_MINE: drawMineTab(mx, my); break;
            case TAB_SELL: drawSellTab(mx, my); break;
        }

        if (statusTimer > 0) {
            statusTimer--;
            int alpha = Math.min(255, statusTimer * 5);
            int col = ((statusOk ? C_GREEN : C_RED) & 0x00FFFFFF) | (alpha << 24);
            int sw  = fontRendererObj.getStringWidth(statusMsg);
            int sx  = px + (pw - sw) / 2 - 6, sy = py + ph - 22;
            drawRect(sx - 4, sy - 3, sx + sw + 12, sy + 13, (alpha / 2 << 24));
            drawRect(sx - 4, sy - 3, sx + sw + 12, sy - 2, col);
            fontRendererObj.drawStringWithShadow(statusMsg, sx + 4, sy + 1, col);
        }

        if (confirmListing != null) drawConfirmOverlay(mx, my);
        
        // Rendu final des tooltips pour être au-dessus de tout
        if (tooltipItem != null) {
            renderToolTip(tooltipItem, tooltipX, tooltipY);
        }

        GlStateManager.disableBlend();
        super.drawScreen(mx, my, pt);
    }

    private void drawHeader(int mx, int my) {
        drawRect(px, py, px + pw, py + 28, C_HEADER);
        drawRect(px, py + 27, px + pw, py + 28, C_ACCENT);

        // Logo stylisé
        int lx = px + 10, ly = py + 6;
        drawRect(lx, ly, lx + 16, ly + 16, C_ACCENT);
        drawRect(lx + 2, ly + 2, lx + 14, ly + 14, C_BG);
        fontRendererObj.drawStringWithShadow("$", lx + 5, ly + 4, C_GOLD);

        fontRendererObj.drawStringWithShadow("\u00a7b\u00a7lHDV", px + 32, py + 10, C_WHITE);

        String bal = "\u00a77Votre solde : \u00a76" + fmtGold(playerBalance) + " $";
        int bw = fontRendererObj.getStringWidth(bal);
        int bx = px + pw - bw - 45;
        drawRect(bx, py + 6, px + pw - 25, py + 22, 0x22FFFFFF);
        drawRect(bx, py + 21, px + pw - 25, py + 22, C_GOLD);
        fontRendererObj.drawStringWithShadow(bal, bx + 6, py + 10, C_WHITE);

        // Bouton fermer
        int cx = px + pw - 20, cy = py + 7;
        boolean ch = in(mx, my, cx, cy, 14, 14);
        drawRect(cx, cy, cx + 14, cy + 14, ch ? 0xFFBB3333 : 0x44AA3333);
        drawBorder(cx, cy, 14, 14, ch ? C_RED : 0xFF662222);
        fontRendererObj.drawString("x", cx + 4, cy + 2, ch ? C_WHITE : 0xFFCC7777);
        hb(hbClose, cx, cy, 14, 14);
    }

    private void drawTabs(int mx, int my) {
        String[] labels = { "ACHETER", "MES ANNONCES", "VENDRE" };
        int[][] hbs = { hbTabBuy, hbTabMine, hbTabSell };
        int tw = (pw - 20) / 3, th = 22, ty = py + 32, startX = px + 10;

        for (int i = 0; i < 3; i++) {
            int tx = startX + i * (tw + 2);
            boolean active = (activeTab == i), hov = in(mx, my, tx, ty, tw, th);
            int bg = active ? 0xFF152545 : (hov ? 0x661A3060 : 0x33000000);
            int tc = active ? 0xFF55AAFF : (hov ? C_WHITE : C_GRAY);

            drawRect(tx, ty, tx + tw, ty + th, bg);
            drawRect(tx, active ? ty : ty + th - 1, tx + tw, active ? ty + 2 : ty + th, active ? C_ACCENT : C_BORDER);
            
            drawCentered(labels[i], tx + tw / 2, ty + 7, tc);
            
            // Badge notifications
            int count = (i == 0) ? listings.size() : (i == 1 ? myListings.size() : -1);
            if (count >= 0 && !loading && !(i==1 && loadingMine)) {
                String s = String.valueOf(count);
                int sw = fontRendererObj.getStringWidth(s);
                int bx = tx + tw - sw - 10;
                drawRect(bx - 2, ty + 6, bx + sw + 2, ty + 16, active ? C_ACCENT : 0xFF222222);
                fontRendererObj.drawString(s, bx, ty + 7, C_WHITE);
            }
            hb(hbs[i], tx, ty, tw, th);
        }
    }

    private void drawBuyTab(int mx, int my) {
        int cy = py + 60;

        // Barre de recherche optimisée
        int sfW = pw - 210;
        drawRect(px + 10, cy, px + 10 + sfW, cy + 20, C_PANEL);
        drawBorder(px + 10, cy, sfW, 20, searchField.isFocused() ? C_ACCENT : 0xFF1A2A45);
        
        searchField.xPosition = px + 15;
        searchField.yPosition = cy + 6;
        searchField.width = sfW - 35;
        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && !searchField.isFocused())
            fontRendererObj.drawString("\u00a78Rechercher un item...", px + 15, cy + 6, 0);

        // Bouton Clear Search
        if (!searchField.getText().isEmpty()) {
            int cx = px + 10 + sfW - 16, cy_ = cy + 6;
            boolean ch = in(mx, my, cx, cy_, 10, 10);
            fontRendererObj.drawString("\u00a7l\u00a7cx", cx, cy_ - 1, ch ? C_RED : C_GRAY);
            hb(hbClearSearch, cx - 2, cy_ - 2, 14, 14);
        } else hbClearSearch[2] = 0;

        // Bouton Refresh / Search
        int bx = px + 15 + sfW, bw = 85;
        boolean bh = in(mx, my, bx, cy, bw, 20);
        drawRect(bx, cy, bx + bw, cy + 20, bh ? 0xFF224488 : 0xFF152545);
        drawBorder(bx, cy, bw, 20, bh ? C_ACCENT2 : C_ACCENT);
        drawCentered("\u00a7lChercher", bx + bw / 2, cy + 6, bh ? C_WHITE : 0xFFAABBFF);
        hb(hbSearchBtn, bx, cy, bw, 20);

        // Bouton Tri
        int tx = bx + bw + 5, tw = 90;
        boolean th = in(mx, my, tx, cy, tw, 20);
        String[] sortNames = { "R\u00e9cent", "Prix \u25b2", "Prix \u25bc" };
        drawRect(tx, cy, tx + tw, cy + 20, th ? 0xFF225544 : 0xFF153525);
        drawBorder(tx, cy, tw, 20, th ? C_GREEN : 0xFF226644);
        drawCentered("\u00a7l" + sortNames[sortMode], tx + tw / 2, cy + 6, th ? C_WHITE : 0xFFBBFFCC);
        hb(hbSort, tx, cy, tw, 20);

        cy += 28;

        // Header de liste
        int lx = px + 10, lw = pw - 20;
        drawRect(lx, cy, lx + lw, cy + 16, 0x44000000);
        drawRect(lx, cy + 15, lx + lw, cy + 16, 0x882277EE);
        fontRendererObj.drawString("\u00a77Item",       lx + 35,  cy + 4, 0);
        fontRendererObj.drawString("\u00a77Vendeur",    lx + 230, cy + 4, 0);
        fontRendererObj.drawString("\u00a77Quantit\u00e9", lx + 350, cy + 4, 0);
        fontRendererObj.drawString("\u00a77Prix Total", lx + 430, cy + 4, 0);
        fontRendererObj.drawString("\u00a77Unit\u00e9",   lx + 530, cy + 4, 0);
        cy += 18;

        listY0 = cy;
        listH  = ph - (cy - py) - 25;
        int maxRows = listH / ROW_H;

        if (loading) { drawLoading(listY0, listH); return; }

        List<HdvListing> fl = getFiltered();
        if (fl.isEmpty()) {
            drawEmpty("Aucune annonce trouv\u00e9e.", listY0, listH);
        } else {
            int y = listY0, end = Math.min(scroll + maxRows, fl.size());
            for (int i = scroll; i < end; i++) {
                drawBuyRow(mx, my, lx, y, lw, fl.get(i), i - scroll);
                y += ROW_H;
            }
        }

        if (fl.size() > maxRows)
            drawScrollbar(px + pw - 18, listY0, listH, scroll, fl.size(), maxRows, mx, my, hbScrollUp, hbScrollDn);

        fontRendererObj.drawString("\u00a78" + fl.size() + " annonce(s) trouv\u00e9e(s)", px + 12, py + ph - 14, 0);
    }

    private void drawBuyRow(int mx, int my, int x, int y, int w, HdvListing l, int idx) {
        boolean hov = in(mx, my, x, y, w, ROW_H - 1);
        boolean canAfford = playerBalance >= l.getTotalPrice();
        
        int bg = hov ? 0x22FFFFFF : (idx % 2 == 0 ? 0x11FFFFFF : 0);
        drawRect(x, y, x + w, y + ROW_H - 1, bg);
        if (hov) drawRect(x, y, x + 2, y + ROW_H - 1, C_ACCENT);

        ItemStack item = l.getItem();
        if (item != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(item, x + 8, y + 4);
            itemRender.renderItemOverlayIntoGUI(fontRendererObj, item, x + 8, y + 4, null);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            if (hov) { tooltipItem = item; tooltipX = mx; tooltipY = my; }
            fontRendererObj.drawStringWithShadow(item.getDisplayName(), x + 35, y + 10, C_WHITE);
        }

        fontRendererObj.drawString(l.getSellerName(), x + 230, y + 10, 0xFFAADDFF);
        fontRendererObj.drawString("x" + l.getQuantity(), x + 350, y + 10, C_WHITE);
        fontRendererObj.drawStringWithShadow(fmtGold(l.getTotalPrice()) + " $", x + 430, y + 10, C_GOLD);
        fontRendererObj.drawString("\u00a77" + fmtGold(l.getPricePerUnit()) + "/u", x + 530, y + 10, C_GRAY);

        int bx = x + w - 75, by = y + 5, bw = 70, bh = 16;
        boolean bhov = in(mx, my, bx, by, bw, bh);
        if (canAfford) {
            drawRect(bx, by, bx + bw, by + bh, bhov ? 0xFF228844 : 0xFF154525);
            drawBorder(bx, by, bw, bh, bhov ? C_GREEN : 0xFF226644);
            drawCentered("ACHETER", bx + bw / 2, by + 4, bhov ? C_WHITE : 0xFFBBFFCC);
        } else {
            drawRect(bx, by, bx + bw, by + bh, 0xFF451515);
            drawCentered("TROP CHER", bx + bw / 2, by + 4, 0xFFFFAAAA);
        }
        
        if (idx < hbRows.length) hb(hbRows[idx], x, y, w, ROW_H);
    }

    private void drawMineTab(int mx, int my) {
        int lx = px + 10, lw = pw - 20, hy = py + 60;

        drawRect(lx, hy, lx + lw, hy + 16, 0x44000000);
        drawRect(lx, hy + 15, lx + lw, hy + 16, 0x882277EE);
        fontRendererObj.drawString("\u00a77Item",     lx + 35,  hy + 4, 0);
        fontRendererObj.drawString("\u00a77Quantit\u00e9", lx + 260, hy + 4, 0);
        fontRendererObj.drawString("\u00a77Prix Unit.", lx + 350, hy + 4, 0);
        fontRendererObj.drawString("\u00a77Statut / Temps", lx + 450, hy + 4, 0);

        listMineY0 = hy + 18;
        listMineH  = ph - (listMineY0 - py) - 60;
        int maxRows = listMineH / ROW_H;

        if (loadingMine) {
            drawLoading(listMineY0, listMineH);
        } else if (myListings.isEmpty()) {
            drawEmpty("Vous n'avez aucune annonce.", listMineY0, listMineH);
        } else {
            int y = listMineY0, end = Math.min(scrollMine + maxRows, myListings.size()), cIdx = 0;
            for (int i = scrollMine; i < end; i++) {
                HdvListing l = myListings.get(i);
                boolean hov = in(mx, my, lx, y, lw, ROW_H - 1);
                int bg = l.isSold() ? 0x2233AA33 : (hov ? 0x22FFFFFF : ((i-scrollMine)%2==0?0x11FFFFFF:0));
                drawRect(lx, y, lx + lw, y + ROW_H - 1, bg);

                if (l.getItem() != null) {
                    GlStateManager.enableDepth();
                    RenderHelper.enableGUIStandardItemLighting();
                    itemRender.renderItemAndEffectIntoGUI(l.getItem(), lx + 8, y + 4);
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.disableDepth();
                    if (hov) { tooltipItem = l.getItem(); tooltipX = mx; tooltipY = my; }
                    fontRendererObj.drawStringWithShadow(l.getItem().getDisplayName(), lx + 35, y + 10, C_WHITE);
                }

                fontRendererObj.drawString("x" + l.getQuantity(), lx + 260, y + 10, C_WHITE);
                fontRendererObj.drawStringWithShadow(fmtGold(l.getPricePerUnit()) + " $", lx + 350, y + 10, C_GOLD);

                int bx = lx + lw - 75, by = y + 5, bw = 70, bh = 16;
                boolean bhov = in(mx, my, bx, by, bw, bh);

                if (l.isSold()) {
                    fontRendererObj.drawString("\u00a7aVENDU", lx + 450, y + 10, 0);
                } else {
                    long rem = l.getExpiresAt() - System.currentTimeMillis() / 1000L;
                    if (rem > 0) {
                        fontRendererObj.drawString(fmtTime(rem), lx + 450, y + 10, C_WHITE);
                    } else {
                        fontRendererObj.drawString("\u00a7cExpir\u00e9", lx + 450, y + 10, C_RED);
                    }

                    drawRect(bx, by, bx + bw, by + bh, bhov ? 0xFF882222 : 0xFF451515);
                    drawBorder(bx, by, bw, bh, bhov ? C_RED : 0xFF662222);
                    drawCentered("RETIRER", bx + bw / 2, by + 4, bhov ? C_WHITE : 0xFFFFAAAA);
                    if (cIdx < hbCancelRow.length) hb(hbCancelRow[cIdx++], bx, by, bw, bh);
                }
                y += ROW_H;
            }
            while(cIdx < hbCancelRow.length) hbCancelRow[cIdx++][2] = 0;
            if (myListings.size() > maxRows)
                drawScrollbar(px + pw - 18, listMineY0, listMineH, scrollMine, myListings.size(), maxRows, mx, my, hbScrollMineUp, hbScrollMineDn);
        }

        // Bandeau Collecte
        int gy = py + ph - 45;
        int gx = px + 10, gw = pw - 20, gh = 30;
        boolean ghov = in(mx, my, gx, gy, gw, gh);
        if (pendingEarnings > 0) {
            drawRect(gx, gy, gx + gw, gy + gh, ghov ? 0xFF228844 : 0xFF154525);
            drawBorder(gx, gy, gw, gh, ghov ? C_GREEN : 0xFF226644);
            drawCentered("\u00a7fCollecter vos gains : \u00a76" + fmtGold(pendingEarnings) + " $ \u00a7a(cliquez)", gx + gw / 2, gy + 10, C_WHITE);
            hb(hbCollect, gx, gy, gw, gh);
        } else {
            drawRect(gx, gy, gx + gw, gy + gh, 0x33000000);
            drawBorder(gx, gy, gw, gh, 0x44FFFFFF);
            drawCentered("\u00a77Aucun gain en attente", gx + gw / 2, gy + 10, 0);
            hbCollect[2] = 0;
        }
    }

    private void drawSellTab(int mx, int my) {
        int mid = px + pw / 2;
        int pad = 15;
        
        // Côté Gauche : Inventaire
        int ix = px + pad, iy = py + 60, iw = (pw - pad * 3) / 2, ih = ph - 80;
        drawRect(ix, iy, ix + iw, iy + ih, 0x44000000);
        drawBorder(ix, iy, iw, ih, C_BORDER);
        drawRect(ix, iy, ix + iw, iy + 18, C_HEADER);
        fontRendererObj.drawStringWithShadow("VOTRE INVENTAIRE", ix + 8, iy + 5, C_ACCENT2);

        InventoryPlayer inv = mc.thePlayer.inventory;
        int sz = 20, gap = 2;
        int startX = ix + (iw - 9 * (sz + gap)) / 2;
        int startY = iy + 25;
        
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawInvSlot(mx, my, startX + col * (sz + gap), startY + row * (sz + gap), sz, 9 + row * 9 + col, inv);
        
        startY += 3 * (sz + gap) + 10;
        drawRect(startX, startY - 5, startX + 9 * (sz + gap), startY - 4, 0x44FFFFFF);
        for (int col = 0; col < 9; col++)
            drawInvSlot(mx, my, startX + col * (sz + gap), startY, sz, col, inv);

        // Côté Droit : Configuration
        int cx = mid + pad / 2, cy = iy, cw = iw, ch = ih;
        drawRect(cx, cy, cx + cw, cy + ch, 0x66000000);
        drawBorder(cx, cy, cw, ch, 0xFF1A4525);
        drawRect(cx, cy, cx + cw, cy + 18, 0xFF051505);
        fontRendererObj.drawStringWithShadow("CONFIGURATION VENTE", cx + 8, cy + 5, C_GREEN);

        if (sellItem != null) {
            int itemX = cx + cw / 2 - 10, itemY = cy + 30;
            drawRect(itemX - 4, itemY - 4, itemX + 24, itemY + 24, 0x44FFFFFF);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(sellItem, itemX, itemY);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            drawCentered(sellItem.getDisplayName(), cx + cw / 2, itemY + 28, C_WHITE);
            
            // Quantité
            int qy = cy + 85;
            fontRendererObj.drawStringWithShadow("Quantit\u00e9 :", cx + 20, qy, C_GRAY);
            qtyField.xPosition = cx + 80; qtyField.yPosition = qy - 2; qtyField.width = 50;
            qtyField.drawTextBox();
            miniBtn(mx, my, cx + 140, qy - 2, 25, 14, "-10", 0xFF451515, C_RED, C_WHITE, hbQtyM10);
            miniBtn(mx, my, cx + 170, qy - 2, 25, 14, "+10", 0xFF154515, C_GREEN, C_WHITE, hbQtyP10);
            
            // Prix
            int py_ = cy + 115;
            fontRendererObj.drawStringWithShadow("Prix Total :", cx + 20, py_, C_GRAY);
            priceField.xPosition = cx + 80; priceField.yPosition = py_ - 2; priceField.width = 100;
            priceField.drawTextBox();
            fontRendererObj.drawStringWithShadow("$", cx + 185, py_, C_GOLD);
            
            miniBtn(mx, my, cx + 20,  py_ + 20, 45, 14, "-1000", 0xFF451515, C_RED, C_WHITE, hbPrM1000);
            miniBtn(mx, my, cx + 70,  py_ + 20, 45, 14, "-100",  0xFF451515, C_RED, C_WHITE, hbPrM100);
            miniBtn(mx, my, cx + 120, py_ + 20, 45, 14, "+100",  0xFF154515, C_GREEN, C_WHITE, hbPrP100);
            miniBtn(mx, my, cx + 170, py_ + 20, 45, 14, "+1000", 0xFF154515, C_GREEN, C_WHITE, hbPrP1000);

            // Recap
            long unit = sellQty > 0 ? sellPrice / sellQty : 0;
            drawRect(cx + 10, cy + ch - 60, cx + cw - 10, cy + ch - 35, 0x22FFFFFF);
            fontRendererObj.drawString("\u00a77Prix unitaire : \u00a7e" + fmtGold(unit) + " $", cx + 20, cy + ch - 52, 0);

            // Bouton
            int bx = cx + 10, by = cy + ch - 30, bw = cw - 20, bh = 22;
            boolean bhov = in(mx, my, bx, by, bw, bh);
            drawRect(bx, by, bx + bw, by + bh, bhov ? 0xFF228844 : 0xFF154525);
            drawBorder(bx, by, bw, bh, bhov ? C_GREEN : 0xFF226644);
            drawCentered("METTRE EN VENTE", bx + bw / 2, by + 6, bhov ? C_WHITE : 0xFFBBFFCC);
            hb(hbSellBtn, bx, by, bw, bh);

        } else {
            drawCentered("\u00a78S\u00e9lectionnez un item", cx + cw / 2, cy + ch / 2 - 5, 0);
            hbSellBtn[2] = 0;
        }
    }

    private void drawInvSlot(int mx, int my, int sx, int sy, int sz, int idx, InventoryPlayer inv) {
        ItemStack stack = (idx >= 0 && idx < 36) ? inv.mainInventory[idx] : null;
        boolean sel = (sellSlot == idx), hov = stack != null && in(mx, my, sx, sy, sz, sz);
        drawRect(sx, sy, sx + sz, sy + sz, sel ? 0x6655FF55 : (hov ? 0x44FFFFFF : 0x22000000));
        drawBorder(sx, sy, sz, sz, sel ? C_GREEN : (hov ? C_WHITE : 0x44FFFFFF));
        if (stack != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(stack, sx + 2, sy + 2);
            itemRender.renderItemOverlayIntoGUI(fontRendererObj, stack, sx + 2, sy + 2, null);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            if (hov) { tooltipItem = stack; tooltipX = mx; tooltipY = my; }
        }
        if (idx >= 0 && idx < 36) hb(hbInvSlots[idx], sx, sy, sz, sz);
    }

    private void drawConfirmOverlay(int mx, int my) {
        drawRect(0, 0, width, height, 0xCC000000);
        int cw = 280, ch = 140, cx = (width - cw) / 2, cy = (height - ch) / 2;
        drawRect(cx, cy, cx + cw, cy + ch, C_BG);
        drawBorder(cx, cy, cw, ch, C_ACCENT);
        drawCentered("\u00a7b\u00a7lCONFIRMER L'ACHAT", cx + cw / 2, cy + 10, C_WHITE);

        ItemStack item = confirmListing.getItem();
        if (item != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(item, cx + 20, cy + 35);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            fontRendererObj.drawStringWithShadow(item.getDisplayName(), cx + 45, cy + 39, C_WHITE);
        }
        fontRendererObj.drawString("\u00a77Prix Total : \u00a76" + fmtGold(confirmListing.getTotalPrice()) + " $", cx + 20, cy + 65, 0);
        fontRendererObj.drawString("\u00a77Votre solde : \u00a7e" + fmtGold(playerBalance) + " $", cx + 20, cy + 80, 0);

        int bw = 110, bh = 20, by = cy + ch - 30;
        boolean h1 = in(mx, my, cx + 20, by, bw, bh);
        drawRect(cx + 20, by, cx + 20 + bw, by + bh, h1 ? 0xFF882222 : 0xFF451515);
        drawCentered("ANNULER", cx + 20 + bw / 2, by + 6, C_WHITE);
        hb(hbCancelBuy, cx + 20, by, bw, bh);

        boolean h2 = in(mx, my, cx + cw - bw - 20, by, bw, bh);
        boolean can = playerBalance >= confirmListing.getTotalPrice();
        drawRect(cx + cw - bw - 20, by, cx + cw - 20, by + bh, can ? (h2 ? 0xFF228844 : 0xFF154525) : 0xFF333333);
        drawCentered(can ? "CONFIRMER" : "SOLDE INSUFF.", cx + cw - bw - 20 + bw / 2, by + 6, can ? C_WHITE : C_GRAY);
        hb(hbOkBuy, cx + cw - bw - 20, by, bw, bh);
    }

    private void drawScrollbar(int ax, int ly0, int lh, int cur, int total, int maxRows, int mx, int my, int[] hbUp, int[] hbDn) {
        drawRect(ax, ly0, ax + 12, ly0 + lh, 0x22FFFFFF);
        int trackH = lh - 24;
        int thumbH = Math.max(10, trackH * maxRows / total);
        int thumbY = ly0 + 12 + (trackH - thumbH) * cur / Math.max(1, total - maxRows);
        drawRect(ax + 2, thumbY, ax + 10, thumbY + thumbH, C_ACCENT);
        hb(hbUp, ax, ly0, 12, 12);
        hb(hbDn, ax, ly0 + lh - 12, 12, 12);
    }

    private void drawLoading(int y0, int h) {
        int dots = (int)((System.currentTimeMillis() / 400) % 4);
        String s = "Chargement"; for(int i=0; i<dots; i++) s += ".";
        drawCentered(s, px + pw / 2, y0 + h / 2 - 4, C_GRAY);
    }

    private void drawEmpty(String msg, int y0, int h) {
        drawCentered(msg, px + pw / 2, y0 + h / 2 - 4, C_GRAY);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (confirmListing != null) {
            if (ok(hbCancelBuy) && in(mx, my, hbCancelBuy)) confirmListing = null;
            else if (ok(hbOkBuy) && in(mx, my, hbOkBuy) && playerBalance >= confirmListing.getTotalPrice()) {
                HdvPacketHandler.buyItem(confirmListing.getId());
                confirmListing = null;
            }
            return;
        }

        if (ok(hbClose) && in(mx, my, hbClose)) { mc.displayGuiScreen(null); return; }

        if (ok(hbTabBuy)  && in(mx, my, hbTabBuy))  { activeTab = TAB_BUY; requestList(); return; }
        if (ok(hbTabMine) && in(mx, my, hbTabMine)) { activeTab = TAB_MINE; loadingMine = true; HdvPacketHandler.requestMyListings(); return; }
        if (ok(hbTabSell) && in(mx, my, hbTabSell)) { activeTab = TAB_SELL; sellItem = null; sellSlot = -1; return; }

        switch (activeTab) {
            case TAB_BUY:
                searchField.mouseClicked(mx, my, btn);
                if (ok(hbClearSearch) && in(mx, my, hbClearSearch)) { searchField.setText(""); requestList(); }
                if (ok(hbSearchBtn) && in(mx, my, hbSearchBtn)) requestList();
                if (ok(hbSort) && in(mx, my, hbSort)) sortMode = (sortMode + 1) % 3;
                if (ok(hbScrollUp) && in(mx, my, hbScrollUp)) scroll = Math.max(0, scroll - 1);
                if (ok(hbScrollDn) && in(mx, my, hbScrollDn)) scroll = Math.min(Math.max(0, getFiltered().size() - (listH/ROW_H)), scroll + 1);
                
                List<HdvListing> fl = getFiltered();
                for (int i=0; i<hbRows.length; i++) {
                    if (ok(hbRows[i]) && in(mx, my, hbRows[i])) {
                        int idx = scroll + i;
                        if (idx < fl.size()) confirmListing = fl.get(idx);
                        return;
                    }
                }
                break;
            case TAB_MINE:
                if (ok(hbCollect) && in(mx, my, hbCollect)) {
                    pendingAction = PendingAction.COLLECT;
                    HdvPacketHandler.collectEarnings();
                }
                int cancelIdx = 0;
                for (int i=0; i<myListings.size(); i++) {
                    HdvListing l = myListings.get(i);
                    if (!l.isSold()) {
                        if (cancelIdx < hbCancelRow.length && ok(hbCancelRow[cancelIdx]) && in(mx, my, hbCancelRow[cancelIdx])) {
                            pendingAction = PendingAction.CANCEL;
                            HdvPacketHandler.cancelOffer(l.getId());
                            return;
                        }
                        cancelIdx++;
                    }
                }
                break;
            case TAB_SELL:
                for (int i=0; i<36; i++) {
                    if (ok(hbInvSlots[i]) && in(mx, my, hbInvSlots[i])) {
                        ItemStack s = mc.thePlayer.inventory.mainInventory[i];
                        if (s != null) {
                            sellSlot = i; sellItem = s.copy();
                            if (isShiftKeyDown()) sellQty = s.stackSize; else sellQty = 1;
                            qtyField.setText(String.valueOf(sellQty));
                        }
                        return;
                    }
                }
                qtyField.mouseClicked(mx, my, btn);
                priceField.mouseClicked(mx, my, btn);
                if (ok(hbQtyM10) && in(mx, my, hbQtyM10)) { sellQty = Math.max(1, sellQty - 10); qtyField.setText(String.valueOf(sellQty)); }
                if (ok(hbQtyP10) && in(mx, my, hbQtyP10)) { 
                    int max = sellItem != null ? countInInv(sellItem, mc.thePlayer.inventory) : 64;
                    sellQty = Math.min(max, sellQty + 10); qtyField.setText(String.valueOf(sellQty)); 
                }
                if (ok(hbPrM1000) && in(mx, my, hbPrM1000)) { sellPrice = Math.max(1, sellPrice - 1000); priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrM100)  && in(mx, my, hbPrM100))  { sellPrice = Math.max(1, sellPrice - 100);  priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrP100)  && in(mx, my, hbPrP100))  { sellPrice = Math.min(999999999, sellPrice + 100); priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrP1000) && in(mx, my, hbPrP1000)) { sellPrice = Math.min(999999999, sellPrice + 1000); priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbSellBtn) && in(mx, my, hbSellBtn)) doSell();
                break;
        }
    }

    private void doSell() {
        if (sellItem == null || sellPrice <= 0 || sellQty <= 0) return;
        HdvPacketHandler.postOffer(sellItem, sellPrice, sellQty);
        sellItem = null; sellSlot = -1;
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (k == 1) { mc.displayGuiScreen(null); return; }
        if (activeTab == TAB_BUY && searchField.isFocused()) {
            searchField.textboxKeyTyped(c, k);
            if (k == 28) requestList();
        } else if (activeTab == TAB_SELL) {
            if (qtyField.isFocused()) {
                qtyField.textboxKeyTyped(c, k);
                sellQty = (int)parseLong(qtyField.getText());
            } else if (priceField.isFocused()) {
                priceField.textboxKeyTyped(c, k);
                sellPrice = parseLong(priceField.getText());
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dw = org.lwjgl.input.Mouse.getEventDWheel();
        if (dw == 0) return;
        if (activeTab == TAB_BUY) {
            if (dw > 0) scroll = Math.max(0, scroll - 1);
            else scroll = Math.min(Math.max(0, getFiltered().size() - (listH/ROW_H)), scroll + 1);
        } else if (activeTab == TAB_MINE) {
            if (dw > 0) scrollMine = Math.max(0, scrollMine - 1);
            else scrollMine = Math.min(Math.max(0, myListings.size() - (listMineH/ROW_H)), scrollMine + 1);
        }
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
        qtyField.updateCursorCounter();
        priceField.updateCursorCounter();
        PlayerData pd = PlayerDataHandler.getCachedData();
        if (pd != null) playerBalance = pd.getBalance();
    }

    private List<HdvListing> getFiltered() {
        String f = searchField.getText().trim().toLowerCase();
        List<HdvListing> r = new ArrayList<>();
        for (HdvListing l : listings) {
            String n = l.getItem() != null ? l.getItem().getDisplayName().toLowerCase() : "";
            if (f.isEmpty() || n.contains(f) || l.getSellerName().toLowerCase().contains(f)) r.add(l);
        }
        r.sort((a, b) -> {
            if (sortMode == 1) return Long.compare(a.getTotalPrice(), b.getTotalPrice());
            if (sortMode == 2) return Long.compare(b.getTotalPrice(), a.getTotalPrice());
            return Long.compare(b.getExpiresAt(), a.getExpiresAt());
        });
        return r;
    }

    private int countInInv(ItemStack ref, InventoryPlayer inv) {
        int c = 0;
        for (ItemStack s : inv.mainInventory)
            if (s != null && s.getItem() == ref.getItem() && s.getItemDamage() == ref.getItemDamage()) c += s.stackSize;
        return c;
    }

    private void hb(int[] h, int x, int y, int w, int wh) { h[0]=x; h[1]=y; h[2]=w; h[3]=wh; }
    private boolean ok(int[] h) { return h[2] > 0; }
    private boolean in(int mx, int my, int[] h) { return mx>=h[0] && my>=h[1] && mx<=h[0]+h[2] && my<=h[1]+h[3]; }
    private boolean in(int mx, int my, int x, int y, int w, int h) { return mx>=x && my>=y && mx<=x+w && my<=y+h; }
    private void drawBorder(int x, int y, int w, int h, int c) {
        drawRect(x, y, x+w, y+1, c); drawRect(x, y+h-1, x+w, y+h, c);
        drawRect(x, y, x+1, y+h, c); drawRect(x+w-1, y, x+w, y+h, c);
    }
    private void drawCentered(String s, int cx, int y, int col) {
        fontRendererObj.drawStringWithShadow(s, cx - fontRendererObj.getStringWidth(s) / 2f, y, col);
    }
    private void miniBtn(int mx, int my, int x, int y, int w, int h, String l, int bg, int bd, int tc, int[] hbT) {
        boolean hov = in(mx, my, x, y, w, h);
        drawRect(x, y, x+w, y+h, hov ? bg : 0x44000000);
        drawBorder(x, y, w, h, hov ? tc : bd);
        drawCentered(l, x + w / 2, y + h / 2 - 4, hov ? tc : 0xFFAAAAAA);
        hb(hbT, x, y, w, h);
    }
    private String fmtGold(long v) {
        if (v >= 1000000) return String.format("%.1fM", v/1000000.0);
        if (v >= 1000) return String.format("%.1fK", v/1000.0);
        return String.valueOf(v);
    }
    private String fmtTime(long s) {
        if (s >= 86400) return (s / 86400) + "j " + (s % 86400 / 3600) + "h";
        return (s / 3600) + "h " + (s % 3600 / 60) + "m";
    }
    private long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; } }
}
