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
    private long    pendingPBEarnings = 0L;
    private long    playerBalance   = 0L;
    private int     playerPB        = 0;
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
    private ItemStack    sellItem     = null;
    private int          sellSlot     = -1;
    private int          sellQty      = 1;
    private long         sellPrice    = 100L;
    /** 0 = monnaie ($), 1 = PB, 2 = Les deux (double-devise) */
    private int          sellCurrMode = 0;
    private long         sellPricePB  = 100L;
    private GuiTextField priceField;
    private GuiTextField pricePBField; // champ PB en mode double-devise
    private GuiTextField qtyField;

    // ── Recherche ─────────────────────────────────────────────────────────────
    private GuiTextField searchField;

    // ── Filtres prix ──────────────────────────────────────────────────────────
    private long filterMinMoney = -1L, filterMaxMoney = -1L;
    private long filterMinPB    = -1L, filterMaxPB    = -1L;
    private GuiTextField fldMinMoney, fldMaxMoney, fldMinPB, fldMaxPB;
    private final int[] hbFltReset = new int[4];

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
    private final int[]   hbPrM10000     = new int[4];
    private final int[]   hbPrM1000      = new int[4];
    private final int[]   hbPrM100       = new int[4];
    private final int[]   hbPrM10        = new int[4];
    private final int[]   hbPrP10        = new int[4];
    private final int[]   hbPrP100       = new int[4];
    private final int[]   hbPrP1000      = new int[4];
    private final int[]   hbPrP10000     = new int[4];
    private final int[]   hbPrFld        = new int[4];
    private final int[]   hbOkBuy        = new int[4];
    private final int[]   hbCancelBuy    = new int[4];
    private final int[]   hbConfirmPB    = new int[4]; // bouton "Payer en PB" (annonce dual)
    private final int[]   hbCurrencyToggle = new int[4]; // toggle $ / PB / Les deux
    // Boutons ajustement prix PB (mode double-devise)
    private final int[]   hbPrPBM1000    = new int[4];
    private final int[]   hbPrPBM100     = new int[4];
    private final int[]   hbPrPBP100     = new int[4];
    private final int[]   hbPrPBP1000    = new int[4];
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

        fldMinMoney = new GuiTextField(10, fontRendererObj, 0, 0, 50, 12); fldMinMoney.setMaxStringLength(12);
        fldMaxMoney = new GuiTextField(11, fontRendererObj, 0, 0, 50, 12); fldMaxMoney.setMaxStringLength(12);
        fldMinPB    = new GuiTextField(12, fontRendererObj, 0, 0, 50, 12); fldMinPB.setMaxStringLength(8);
        fldMaxPB    = new GuiTextField(13, fontRendererObj, 0, 0, 50, 12); fldMaxPB.setMaxStringLength(8);

        priceField = new GuiTextField(1, fontRendererObj, 0, 0, 80, 12);
        priceField.setMaxStringLength(10);
        priceField.setText(String.valueOf(sellPrice));

        pricePBField = new GuiTextField(3, fontRendererObj, 0, 0, 80, 12);
        pricePBField.setMaxStringLength(7);
        pricePBField.setText(String.valueOf(sellPricePB));

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
                        HdvPacketHandler.showCollectMessage(pendingEarnings, pendingPBEarnings);
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

        HdvPacketHandler.setMyListingsListener((mine, earnings, earningsPB) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            onMyListingsReceived(mine, earnings, earningsPB);
        }));

        PlayerDataHandler.setListener(data -> { playerBalance = data.getBalance(); playerPB = data.getPb(); });
        PlayerData pd = PlayerDataHandler.getCachedData();
        if (pd != null) { playerBalance = pd.getBalance(); playerPB = pd.getPb(); }

        List<HdvListing> cached = new ArrayList<>(HdvPacketHandler.getCachedListings());
        if (!cached.isEmpty()) { listings.clear(); listings.addAll(cached); loading = false; }

        List<HdvListing> cachedMine = new ArrayList<>(HdvPacketHandler.getCachedMyListings());
        if (!cachedMine.isEmpty()) {
            myListings.clear(); myListings.addAll(cachedMine);
            pendingEarnings   = HdvPacketHandler.getCachedPendingEarnings();
            pendingPBEarnings = HdvPacketHandler.getCachedPendingPBEarnings();
            loadingMine = false;
        }

        requestList();
        HdvPacketHandler.requestMyListings();
    }

    public void onListingsReceived(List<HdvListing> nl) {
        listings.clear(); listings.addAll(nl); loading = false; scroll = 0;
    }

    public void onMyListingsReceived(List<HdvListing> mine, long earnings, long earningsPB) {
        myListings.clear(); myListings.addAll(mine);
        pendingEarnings   = earnings;
        pendingPBEarnings = earningsPB;
        loadingMine = false;
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
        hbPrM10000[2] = hbPrM1000[2] = hbPrM100[2] = hbPrM10[2] = 0;
        hbPrP10[2] = hbPrP100[2] = hbPrP1000[2] = hbPrP10000[2] = 0;
        hbPrFld[2] = hbOkBuy[2] = hbCancelBuy[2] = hbConfirmPB[2] = 0;
        hbCurrencyToggle[2] = hbFltReset[2] = 0;
        hbPrPBM1000[2] = hbPrPBM100[2] = hbPrPBP100[2] = hbPrPBP1000[2] = 0;
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

        String bal = "\u00a76" + fmtGold(playerBalance) + " $ \u00a77| \u00a7e" + fmtGold(playerPB) + " PB";
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
        int lx = px + 10, lw = pw - 20;

        // ── Barre de recherche ─────────────────────────────────────────────────
        int sfW = pw - 210;
        drawRect(px + 10, cy, px + 10 + sfW, cy + 20, C_PANEL);
        drawBorder(px + 10, cy, sfW, 20, searchField.isFocused() ? C_ACCENT : 0xFF1A2A45);
        searchField.xPosition = px + 15;
        searchField.yPosition = cy + 6;
        searchField.width = sfW - 35;
        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && !searchField.isFocused())
            fontRendererObj.drawString("\u00a78Rechercher un item...", px + 15, cy + 6, 0);

        if (!searchField.getText().isEmpty()) {
            int csx = px + 10 + sfW - 16, csy = cy + 6;
            boolean csh = in(mx, my, csx, csy, 10, 10);
            fontRendererObj.drawString("\u00a7l\u00a7cx", csx, csy - 1, csh ? C_RED : C_GRAY);
            hb(hbClearSearch, csx - 2, csy - 2, 14, 14);
        } else hbClearSearch[2] = 0;

        int bx = px + 15 + sfW, bw = 85;
        boolean bh = in(mx, my, bx, cy, bw, 20);
        drawRect(bx, cy, bx + bw, cy + 20, bh ? 0xFF224488 : 0xFF152545);
        drawBorder(bx, cy, bw, 20, bh ? C_ACCENT2 : C_ACCENT);
        drawCentered("\u00a7lChercher", bx + bw / 2, cy + 6, bh ? C_WHITE : 0xFFAABBFF);
        hb(hbSearchBtn, bx, cy, bw, 20);

        int tx = bx + bw + 5, tw = 90;
        boolean th = in(mx, my, tx, cy, tw, 20);
        String[] sortNames = { "R\u00e9cent", "Prix \u25b2", "Prix \u25bc" };
        drawRect(tx, cy, tx + tw, cy + 20, th ? 0xFF225544 : 0xFF153525);
        drawBorder(tx, cy, tw, 20, th ? C_GREEN : 0xFF226644);
        drawCentered("\u00a7l" + sortNames[sortMode], tx + tw / 2, cy + 6, th ? C_WHITE : 0xFFBBFFCC);
        hb(hbSort, tx, cy, tw, 20);

        cy += 24;

        // ── Filtres de prix ────────────────────────────────────────────────────
        drawRect(lx, cy, lx + lw, cy + 20, 0x18FFFFFF);
        drawBorder(lx, cy, lw, 20, 0xFF1A2540);
        int fx = lx + 8;
        fontRendererObj.drawString("\u00a78Filtres :", fx, cy + 6, 0);
        fx += 44;
        // Min $
        fontRendererObj.drawString("\u00a77$ \u2265", fx, cy + 6, 0); fx += 20;
        fldMinMoney.xPosition = fx; fldMinMoney.yPosition = cy + 4; fldMinMoney.width = 52;
        drawRect(fx - 2, cy + 2, fx + 54, cy + 18, C_PANEL);
        drawBorder(fx - 2, cy + 2, 56, 16, fldMinMoney.isFocused() ? C_ACCENT : 0xFF1A2840);
        fldMinMoney.drawTextBox();
        fx += 58;
        // Max $
        fontRendererObj.drawString("\u00a77\u2264", fx, cy + 6, 0); fx += 11;
        fldMaxMoney.xPosition = fx; fldMaxMoney.yPosition = cy + 4; fldMaxMoney.width = 52;
        drawRect(fx - 2, cy + 2, fx + 54, cy + 18, C_PANEL);
        drawBorder(fx - 2, cy + 2, 56, 16, fldMaxMoney.isFocused() ? C_ACCENT : 0xFF1A2840);
        fldMaxMoney.drawTextBox();
        fx += 56;
        // Séparateur $
        fontRendererObj.drawString("\u00a76$", fx, cy + 6, 0); fx += 16;
        // Séparateur vertical
        drawRect(fx + 2, cy + 4, fx + 3, cy + 16, 0x44FFFFFF); fx += 10;
        // Min PB
        fontRendererObj.drawString("\u00a7ePB \u2265", fx, cy + 6, 0); fx += 28;
        fldMinPB.xPosition = fx; fldMinPB.yPosition = cy + 4; fldMinPB.width = 50;
        drawRect(fx - 2, cy + 2, fx + 52, cy + 18, C_PANEL);
        drawBorder(fx - 2, cy + 2, 54, 16, fldMinPB.isFocused() ? C_GOLD : 0xFF302218);
        fldMinPB.drawTextBox();
        fx += 55;
        // Max PB
        fontRendererObj.drawString("\u00a77\u2264", fx, cy + 6, 0); fx += 11;
        fldMaxPB.xPosition = fx; fldMaxPB.yPosition = cy + 4; fldMaxPB.width = 50;
        drawRect(fx - 2, cy + 2, fx + 52, cy + 18, C_PANEL);
        drawBorder(fx - 2, cy + 2, 54, 16, fldMaxPB.isFocused() ? C_GOLD : 0xFF302218);
        fldMaxPB.drawTextBox();
        // Bouton Reset
        {
            int rbx = lx + lw - 48, rby = cy + 1, rbw = 46, rbh = 18;
            boolean rh = in(mx, my, rbx, rby, rbw, rbh);
            drawRect(rbx, rby, rbx + rbw, rby + rbh, rh ? 0xFF661111 : 0x33CC2222);
            drawBorder(rbx, rby, rbw, rbh, rh ? C_RED : 0xFF442222);
            fontRendererObj.drawString(rh ? "\u00a7cReset \u00d7" : "\u00a77Reset \u00d7", rbx + 5, rby + 5, 0);
            hb(hbFltReset, rbx, rby, rbw, rbh);
        }

        cy += 24;

        // ── Header de liste ────────────────────────────────────────────────────
        drawRect(lx, cy, lx + lw, cy + 16, 0x44000000);
        drawRect(lx, cy + 15, lx + lw, cy + 16, 0x882277EE);
        fontRendererObj.drawString("\u00a77Item",      lx + 35,  cy + 4, 0);
        fontRendererObj.drawString("\u00a77Vendeur",   lx + 210, cy + 4, 0);
        fontRendererObj.drawString("\u00a77Qt\u00e9",  lx + 310, cy + 4, 0);
        fontRendererObj.drawString("\u00a76Prix $",    lx + 375, cy + 4, 0);
        fontRendererObj.drawString("\u00a7ePrix PB",   lx + 470, cy + 4, 0);
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

        fontRendererObj.drawString("\u00a78" + fl.size() + " annonce(s)", px + 12, py + ph - 14, 0);
    }

    private void drawBuyRow(int mx, int my, int x, int y, int w, HdvListing l, int idx) {
        boolean hov = in(mx, my, x, y, w, ROW_H - 1);
        boolean isDual = l.isDual();
        boolean canAfford;
        if (isDual) {
            canAfford = (playerBalance >= l.getTotalPrice()) || (playerPB >= l.getPricePB());
        } else if (l.isPayPB()) {
            canAfford = playerPB >= l.getTotalPrice();
        } else {
            canAfford = playerBalance >= l.getTotalPrice();
        }

        int bg = hov ? 0x22FFFFFF : (idx % 2 == 0 ? 0x11FFFFFF : 0);
        drawRect(x, y, x + w, y + ROW_H - 1, bg);
        int accentBar = isDual ? 0xFFAA55FF : (l.isPayPB() ? C_GOLD : C_ACCENT);
        if (hov) drawRect(x, y, x + 2, y + ROW_H - 1, accentBar);

        ItemStack item = l.getItem();
        if (item != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(item, x + 8, y + 4);
            itemRender.renderItemOverlayIntoGUI(fontRendererObj, item, x + 8, y + 4, null);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            if (hov) { tooltipItem = item; tooltipX = mx; tooltipY = my; }
            String dn = fontRendererObj.trimStringToWidth(item.getDisplayName(), 165);
            fontRendererObj.drawStringWithShadow(dn, x + 35, y + 10, C_WHITE);
        }

        String seller = fontRendererObj.trimStringToWidth(l.getSellerName(), 88);
        fontRendererObj.drawString(seller, x + 210, y + 10, 0xFFAADDFF);
        fontRendererObj.drawString("x" + l.getQuantity(), x + 308, y + 10, C_WHITE);

        // Colonne $ monnaie
        if (!l.isPayPB() || isDual) {
            fontRendererObj.drawStringWithShadow("\u00a76" + fmtGold(l.getTotalPrice()) + " $", x + 375, y + 10, C_GOLD);
        } else {
            fontRendererObj.drawString("\u00a78\u2014", x + 375, y + 10, C_GRAY);
        }

        // Colonne PB
        if (isDual) {
            fontRendererObj.drawStringWithShadow("\u00a7e" + fmtGold(l.getPricePB()) + " PB", x + 470, y + 10, C_GOLD);
        } else if (l.isPayPB()) {
            fontRendererObj.drawStringWithShadow("\u00a7e" + fmtGold(l.getTotalPrice()) + " PB", x + 470, y + 10, C_GOLD);
        } else {
            fontRendererObj.drawString("\u00a78\u2014", x + 470, y + 10, C_GRAY);
        }

        int bx = x + w - 75, by = y + 5, bw = 70, bh = 16;
        boolean bhov = in(mx, my, bx, by, bw, bh);
        if (canAfford) {
            int btnCol = isDual ? (bhov ? 0xFF553388 : 0xFF2A1A44) : (bhov ? 0xFF228844 : 0xFF154525);
            int brdCol = isDual ? (bhov ? 0xFFAA55FF : 0xFF661199) : (bhov ? C_GREEN : 0xFF226644);
            drawRect(bx, by, bx + bw, by + bh, btnCol);
            drawBorder(bx, by, bw, bh, brdCol);
            String btnLabel = isDual ? "CHOISIR" : "ACHETER";
            drawCentered(btnLabel, bx + bw / 2, by + 4, bhov ? C_WHITE : (isDual ? 0xFFCC88FF : 0xFFBBFFCC));
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
        fontRendererObj.drawString("\u00a77Item",        lx + 35,  hy + 4, 0);
        fontRendererObj.drawString("\u00a77Qt\u00e9",    lx + 248, hy + 4, 0);
        fontRendererObj.drawString("\u00a76Prix $",       lx + 302, hy + 4, 0);
        fontRendererObj.drawString("\u00a7ePrix PB",      lx + 398, hy + 4, 0);
        fontRendererObj.drawString("\u00a77Statut",      lx + 490, hy + 4, 0);

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
                    itemRender.renderItemOverlayIntoGUI(fontRendererObj, l.getItem(), lx + 8, y + 4, null);
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.disableDepth();
                    if (hov) { tooltipItem = l.getItem(); tooltipX = mx; tooltipY = my; }
                    String mn = fontRendererObj.trimStringToWidth(l.getItem().getDisplayName(), 165);
                    fontRendererObj.drawStringWithShadow(mn, lx + 35, y + 10, C_WHITE);
                }

                fontRendererObj.drawString("x" + l.getQuantity(), lx + 248, y + 10, C_WHITE);
                // Colonne Prix $
                if (!l.isPayPB() || l.isDual()) {
                    fontRendererObj.drawStringWithShadow("\u00a76" + fmtGold(l.getTotalPrice()) + " $", lx + 302, y + 10, C_WHITE);
                } else {
                    fontRendererObj.drawString("\u00a78\u2014", lx + 302, y + 10, C_GRAY);
                }
                // Colonne Prix PB
                if (l.isDual()) {
                    fontRendererObj.drawStringWithShadow("\u00a7e" + fmtGold(l.getPricePB()) + " PB", lx + 398, y + 10, C_WHITE);
                } else if (l.isPayPB()) {
                    fontRendererObj.drawStringWithShadow("\u00a7e" + fmtGold(l.getPricePB()) + " PB", lx + 398, y + 10, C_WHITE);
                } else {
                    fontRendererObj.drawString("\u00a78\u2014", lx + 398, y + 10, C_GRAY);
                }

                int bx = lx + lw - 75, by = y + 5, bw = 70, bh = 16;
                boolean bhov = in(mx, my, bx, by, bw, bh);

                if (l.isSold()) {
                    fontRendererObj.drawString("\u00a7aVENDU", lx + 490, y + 10, 0);
                } else {
                    long rem = l.getExpiresAt() - System.currentTimeMillis() / 1000L;
                    if (rem > 0) {
                        fontRendererObj.drawString(fmtTime(rem), lx + 490, y + 10, C_WHITE);
                    } else {
                        fontRendererObj.drawString("\u00a7cExpir\u00e9", lx + 490, y + 10, C_RED);
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
        if (pendingEarnings > 0 || pendingPBEarnings > 0) {
            drawRect(gx, gy, gx + gw, gy + gh, ghov ? 0xFF228844 : 0xFF154525);
            drawBorder(gx, gy, gw, gh, ghov ? C_GREEN : 0xFF226644);
            StringBuilder label = new StringBuilder("\u00a7fCollecter vos gains : ");
            if (pendingEarnings > 0) {
                label.append("\u00a76").append(fmtGold(pendingEarnings)).append(" $");
                if (pendingPBEarnings > 0) label.append(" \u00a77| ");
            }
            if (pendingPBEarnings > 0) {
                label.append("\u00a7e").append(fmtGold(pendingPBEarnings)).append(" PB");
            }
            label.append(" \u00a7a(cliquez)");
            drawCentered(label.toString(), gx + gw / 2, gy + 10, C_WHITE);
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

        // ── Inventaire (gauche) ───────────────────────────────────────────────
        int ix = px + pad, iy = py + 60, iw = (pw - pad * 3) / 2, ih = ph - 80;
        drawRect(ix, iy, ix + iw, iy + ih, C_PANEL);
        drawBorder(ix, iy, iw, ih, C_BORDER);
        drawRect(ix, iy, ix + iw, iy + 18, C_HEADER);
        fontRendererObj.drawStringWithShadow("§bVOTRE INVENTAIRE", ix + 8, iy + 5, C_ACCENT2);

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

        // ── Configuration vente (droite) ──────────────────────────────────────
        int cx = mid + pad / 2, cy = iy, cw = iw, ch = ih;
        drawRect(cx, cy, cx + cw, cy + ch, 0xFF070D18);
        drawBorder(cx, cy, cw, ch, 0xFF1A3050);
        drawRect(cx, cy, cx + cw, cy + 18, 0xFF030810);
        fontRendererObj.drawStringWithShadow("§b§lCONFIGURATION VENTE", cx + 8, cy + 5, C_ACCENT2);

        if (sellItem != null) {
            // Item affiché — layout horizontal compact pour gagner de la place verticale
            int sqX = cx + 10, sqY = cy + 22;
            drawRect(sqX, sqY, sqX + 24, sqY + 24, 0x33FFFFFF);
            drawBorder(sqX, sqY, 24, 24, C_ACCENT);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(sellItem, sqX + 4, sqY + 4);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            String dispName = fontRendererObj.trimStringToWidth(sellItem.getDisplayName(), cw - 50);
            fontRendererObj.drawStringWithShadow(dispName, sqX + 32, sqY + 8, C_WHITE);

            // ── Toggle devise ($ | PB | Les deux) ────────────────────────────
            int tw3 = (cw - 24) / 3;
            int t1x = cx + 8, t2x = cx + 8 + tw3 + 4, t3x = cx + 8 + 2 * (tw3 + 4);
            int tby = cy + 52, tbh = 16;
            boolean sel0 = (sellCurrMode == 0), sel1 = (sellCurrMode == 1), sel2 = (sellCurrMode == 2);
            // $ bouton
            boolean t1hov = in(mx, my, t1x, tby, tw3, tbh);
            drawRect(t1x, tby, t1x + tw3, tby + tbh, sel0 ? 0xFF1A3030 : (t1hov ? 0xFF0A1818 : 0xFF050C0C));
            drawBorder(t1x, tby, tw3, tbh, sel0 ? C_ACCENT2 : (t1hov ? 0xFF226644 : 0xFF0A2020));
            drawCentered(sel0 ? "§b§l$ ✔" : "§7$", t1x + tw3 / 2, tby + 4, sel0 ? C_WHITE : C_GRAY);
            // PB bouton
            boolean t2hov = in(mx, my, t2x, tby, tw3, tbh);
            drawRect(t2x, tby, t2x + tw3, tby + tbh, sel1 ? 0xFF2A2000 : (t2hov ? 0xFF100E00 : 0xFF080600));
            drawBorder(t2x, tby, tw3, tbh, sel1 ? C_GOLD : (t2hov ? 0xFF443300 : 0xFF1A1200));
            drawCentered(sel1 ? "§e§lPB ✔" : "§7PB", t2x + tw3 / 2, tby + 4, sel1 ? C_GOLD : C_GRAY);
            // Les deux bouton
            boolean t3hov = in(mx, my, t3x, tby, tw3, tbh);
            drawRect(t3x, tby, t3x + tw3, tby + tbh, sel2 ? 0xFF2A1A44 : (t3hov ? 0xFF150E22 : 0xFF080610));
            drawBorder(t3x, tby, tw3, tbh, sel2 ? 0xFFAA55FF : (t3hov ? 0xFF553388 : 0xFF1A0E33));
            drawCentered(sel2 ? "§d§l$ + PB ✔" : "§7$ + PB", t3x + tw3 / 2, tby + 4, sel2 ? 0xFFDD99FF : C_GRAY);
            hb(hbCurrencyToggle, t1x, tby, t3x + tw3 - t1x, tbh);

            // Séparateur
            drawRect(cx + 8, cy + 72, cx + cw - 8, cy + 73, 0x44FFFFFF);

            // ── Quantité ─────────────────────────────────────────────────────
            int qy = cy + 78;
            fontRendererObj.drawStringWithShadow("§7Quantité :", cx + 12, qy + 2, C_GRAY);
            qtyField.xPosition = cx + 75; qtyField.yPosition = qy - 1; qtyField.width = 44;
            qtyField.drawTextBox();
            miniBtn(mx, my, cx + 126, qy - 1, 18, 13, "-10", 0xFF451515, C_RED,   C_WHITE, hbQtyM10);
            miniBtn(mx, my, cx + 148, qy - 1, 18, 13, "+10", 0xFF154515, C_GREEN, C_WHITE, hbQtyP10);

            // ── Prix principal ────────────────────────────────────────────────
            int py_ = cy + 96;
            String currLabel = (sellCurrMode == 1) ? "§7Prix en §ePB :" : "§7Prix en §b$ :";
            fontRendererObj.drawStringWithShadow(currLabel, cx + 12, py_ + 2, C_GRAY);
            priceField.xPosition = cx + 90; priceField.yPosition = py_ - 1; priceField.width = 88;
            priceField.drawTextBox();
            fontRendererObj.drawStringWithShadow((sellCurrMode == 1) ? "§ePB" : "§b$", cx + 183, py_ + 2, (sellCurrMode == 1) ? C_GOLD : C_ACCENT2);

            // Boutons ajustement prix
            int bw4 = (cw - 36) / 4;
            int bminus = py_ + 17, bplus = py_ + 33;
            miniBtn(mx, my, cx + 8,                bminus, bw4, 13, "-10K",  0xFF451515, C_RED,   C_WHITE, hbPrM10000);
            miniBtn(mx, my, cx + 8 +   (bw4 + 4),  bminus, bw4, 13, "-1K",   0xFF451515, C_RED,   C_WHITE, hbPrM1000);
            miniBtn(mx, my, cx + 8 + 2*(bw4 + 4),  bminus, bw4, 13, "-100",  0xFF451515, C_RED,   C_WHITE, hbPrM100);
            miniBtn(mx, my, cx + 8 + 3*(bw4 + 4),  bminus, bw4, 13, "-10",   0xFF451515, C_RED,   C_WHITE, hbPrM10);
            miniBtn(mx, my, cx + 8,                bplus,  bw4, 13, "+10",   0xFF154515, C_GREEN, C_WHITE, hbPrP10);
            miniBtn(mx, my, cx + 8 +   (bw4 + 4),  bplus,  bw4, 13, "+100",  0xFF154515, C_GREEN, C_WHITE, hbPrP100);
            miniBtn(mx, my, cx + 8 + 2*(bw4 + 4),  bplus,  bw4, 13, "+1K",   0xFF154515, C_GREEN, C_WHITE, hbPrP1000);
            miniBtn(mx, my, cx + 8 + 3*(bw4 + 4),  bplus,  bw4, 13, "+10K",  0xFF154515, C_GREEN, C_WHITE, hbPrP10000);

            // ── Prix PB (mode double-devise) ──────────────────────────────────
            if (sellCurrMode == 2) {
                int pby_ = cy + 152;
                drawRect(cx + 8, pby_ - 5, cx + cw - 8, pby_ - 4, 0x33FFFFFF);
                fontRendererObj.drawStringWithShadow("§7Prix en §ePB :", cx + 12, pby_ + 2, C_GRAY);
                pricePBField.xPosition = cx + 90; pricePBField.yPosition = pby_ - 1; pricePBField.width = 88;
                pricePBField.drawTextBox();
                fontRendererObj.drawStringWithShadow("§ePB", cx + 183, pby_ + 2, C_GOLD);
                int bwPB = (cw - 36) / 4, pbBtnsY = pby_ + 17;
                miniBtn(mx, my, cx + 8,                pbBtnsY, bwPB, 13, "-10", 0xFF451515, C_RED,   C_WHITE, hbPrPBM1000);
                miniBtn(mx, my, cx + 8 + (bwPB + 4),  pbBtnsY, bwPB, 13, "-1",  0xFF451515, C_RED,   C_WHITE, hbPrPBM100);
                miniBtn(mx, my, cx + 8 + 2*(bwPB+4),  pbBtnsY, bwPB, 13, "+1",  0xFF154515, C_GREEN, C_WHITE, hbPrPBP100);
                miniBtn(mx, my, cx + 8 + 3*(bwPB+4),  pbBtnsY, bwPB, 13, "+10", 0xFF154515, C_GREEN, C_WHITE, hbPrPBP1000);
            } else {
                hbPrPBM1000[2] = hbPrPBM100[2] = hbPrPBP100[2] = hbPrPBP1000[2] = 0;
            }

            // Recap + bouton vente : ancrés dynamiquement et clampés pour rester dans le panel.
            // Bplus row se termine à cy+96+33+13 = cy+142 ; PB btns end à pby_+30 = cy+182
            int controlsEndY = (sellCurrMode == 2) ? (cy + 182) : (cy + 142);
            long unit = sellQty > 0 ? sellPrice / sellQty : 0;
            int sellBhh = 24;
            int sellMaxY = cy + ch - sellBhh - 4; // ne dépasse pas le panel en bas
            int recapMaxY = sellMaxY - 15;
            int recapY = Math.max(controlsEndY + 8, cy + ch - 54);
            recapY = Math.min(recapY, recapMaxY);
            drawRect(cx + 8, recapY, cx + cw - 8, recapY + 13, 0x22FFFFFF);
            String unitStr;
            if (sellCurrMode == 2) {
                long unitPB = sellQty > 0 ? sellPricePB / sellQty : 0;
                unitStr = "§7Unit.: §b" + fmtGold(unit) + " $ / §e" + fmtGold(unitPB) + " PB";
            } else if (sellCurrMode == 1) {
                unitStr = "§7Prix unit. : §e" + fmtGold(unit) + " PB";
            } else {
                unitStr = "§7Prix unit. : §b" + fmtGold(unit) + " $";
            }
            fontRendererObj.drawString(unitStr, cx + 14, recapY + 3, 0);

            // Bouton mettre en vente — juste sous le recap, clampé pour ne pas sortir du panel
            int bx = cx + 8, by = Math.min(Math.max(recapY + 15, cy + ch - 36), sellMaxY), bww = cw - 16, bhh = sellBhh;
            boolean bhov = in(mx, my, bx, by, bww, bhh);
            int btnBg, btnBorder;
            String sellLabel;
            if (sellCurrMode == 1) {
                btnBg = bhov ? 0xFF554400 : 0xFF2A2000; btnBorder = bhov ? C_GOLD : 0xFF664422;
                sellLabel = "METTRE EN VENTE (PB)";
            } else if (sellCurrMode == 2) {
                btnBg = bhov ? 0xFF553388 : 0xFF2A1A44; btnBorder = bhov ? 0xFFAA55FF : 0xFF551199;
                sellLabel = "METTRE EN VENTE ($ + PB)";
            } else {
                btnBg = bhov ? 0xFF1A3A55 : 0xFF0A1828; btnBorder = bhov ? C_ACCENT2 : 0xFF1A3550;
                sellLabel = "METTRE EN VENTE ($)";
            }
            drawRect(bx, by, bx + bww, by + bhh, btnBg);
            drawBorder(bx, by, bww, bhh, btnBorder);
            int sellTxtCol = bhov ? C_WHITE : (sellCurrMode == 1 ? 0xFFDDCC88 : (sellCurrMode == 2 ? 0xFFDD99FF : 0xFF88CCEE));
            drawCentered("§l" + sellLabel, bx + bww / 2, by + 8, sellTxtCol);
            hb(hbSellBtn, bx, by, bww, bhh);
        } else {
            drawCentered("§8Sélectionnez un item", cx + cw / 2, cy + ch / 2 - 5, 0);
            hbSellBtn[2] = 0;
            hbCurrencyToggle[2] = 0;
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
        boolean isDual = confirmListing.isDual();
        boolean isPB = confirmListing.isPayPB();
        drawRect(0, 0, width, height, 0xCC000000);
        int cw = 280, ch = isDual ? 175 : 140, cx = (width - cw) / 2, cy = (height - ch) / 2;
        drawRect(cx, cy, cx + cw, cy + ch, C_BG);
        drawBorder(cx, cy, cw, ch, C_ACCENT);
        drawCentered("\u00a7b\u00a7lCONFIRMER L'ACHAT", cx + cw / 2, cy + 10, C_WHITE);

        ItemStack item = confirmListing.getItem();
        if (item != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(item, cx + 20, cy + 30);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            fontRendererObj.drawStringWithShadow(item.getDisplayName(), cx + 45, cy + 34, C_WHITE);
        }

        int bw = 110, bh = 20;
        int by2 = 0;
        if (isDual) {
            fontRendererObj.drawString("\u00a77Prix \u00a7b$\u00a77 : \u00a76" + fmtGold(confirmListing.getTotalPrice()) + " $", cx + 20, cy + 58, 0);
            fontRendererObj.drawString("\u00a77Prix \u00a7ePB\u00a77 : \u00a7e" + fmtGold(confirmListing.getPricePB()) + " PB", cx + 20, cy + 73, 0);
            fontRendererObj.drawString("\u00a77Solde : \u00a76" + fmtGold(playerBalance) + "$ \u00a77| \u00a7e" + fmtGold(playerPB) + " PB", cx + 20, cy + 88, 0);

            // Ligne 1 : Annuler (centré)
            int by1 = cy + ch - 58;
            boolean h0 = in(mx, my, cx + cw / 2 - 55, by1, 110, bh);
            drawRect(cx + cw / 2 - 55, by1, cx + cw / 2 + 55, by1 + bh, h0 ? 0xFF882222 : 0xFF451515);
            drawCentered("ANNULER", cx + cw / 2, by1 + 6, C_WHITE);
            hb(hbCancelBuy, cx + cw / 2 - 55, by1, 110, bh);

            // Ligne 2 : Payer en $ | Payer en PB
            by2 = cy + ch - 30;
            boolean canDollar = playerBalance >= confirmListing.getTotalPrice();
            boolean h1 = in(mx, my, cx + 10, by2, bw, bh);
            drawRect(cx + 10, by2, cx + 10 + bw, by2 + bh, canDollar ? (h1 ? 0xFF228844 : 0xFF154525) : 0xFF333333);
            drawCentered(canDollar ? "PAYER EN $" : "INSUFF. $", cx + 10 + bw / 2, by2 + 6, canDollar ? C_WHITE : C_GRAY);
            hb(hbOkBuy, cx + 10, by2, bw, bh);

            boolean canPB = (long) playerPB >= confirmListing.getPricePB();
            boolean h2 = in(mx, my, cx + cw - 10 - bw, by2, bw, bh);
            drawRect(cx + cw - 10 - bw, by2, cx + cw - 10, by2 + bh, canPB ? (h2 ? 0xFF665500 : 0xFF332800) : 0xFF333333);
            drawBorder(cx + cw - 10 - bw, by2, bw, bh, canPB ? C_GOLD : 0xFF444444);
            drawCentered(canPB ? "PAYER EN PB" : "INSUFF. PB", cx + cw - 10 - bw + bw / 2, by2 + 6, canPB ? C_GOLD : C_GRAY);
            hb(hbConfirmPB, cx + cw - 10 - bw, by2, bw, bh);
        } else if (isPB) {
            fontRendererObj.drawString("\u00a77Prix : \u00a7e" + fmtGold(confirmListing.getTotalPrice()) + " PB", cx + 20, cy + 58, 0);
            fontRendererObj.drawString("\u00a78(offre PB uniquement)", cx + 20, cy + 73, 0);
            fontRendererObj.drawString("\u00a77Solde : \u00a76" + fmtGold(playerBalance) + " $ \u00a77| \u00a7e" + fmtGold(playerPB) + " PB", cx + 20, cy + 88, 0);
            int by = cy + ch - 30;
            boolean h1 = in(mx, my, cx + 20, by, bw, bh);
            drawRect(cx + 20, by, cx + 20 + bw, by + bh, h1 ? 0xFF882222 : 0xFF451515);
            drawCentered("ANNULER", cx + 20 + bw / 2, by + 6, C_WHITE);
            hb(hbCancelBuy, cx + 20, by, bw, bh);
            boolean can = (long) playerPB >= confirmListing.getTotalPrice();
            boolean h2 = in(mx, my, cx + cw - bw - 20, by, bw, bh);
            drawRect(cx + cw - bw - 20, by, cx + cw - 20, by + bh, can ? (h2 ? 0xFF665500 : 0xFF332800) : 0xFF333333);
            drawBorder(cx + cw - bw - 20, by, bw, bh, can ? C_GOLD : 0xFF444444);
            drawCentered(can ? "CONFIRMER (PB)" : "SOLDE INSUFF.", cx + cw - bw - 20 + bw / 2, by + 6, can ? C_GOLD : C_GRAY);
            hb(hbOkBuy, cx + cw - bw - 20, by, bw, bh);
            hbConfirmPB[2] = 0;
        } else {
            fontRendererObj.drawString("\u00a77Prix Total : \u00a76" + fmtGold(confirmListing.getTotalPrice()) + " $", cx + 20, cy + 58, 0);
            fontRendererObj.drawString("\u00a78(offre $ uniquement)", cx + 20, cy + 73, 0);
            fontRendererObj.drawString("\u00a77Solde : \u00a76" + fmtGold(playerBalance) + " $ \u00a77| \u00a7e" + fmtGold(playerPB) + " PB", cx + 20, cy + 88, 0);
            int by = cy + ch - 30;
            boolean h1 = in(mx, my, cx + 20, by, bw, bh);
            drawRect(cx + 20, by, cx + 20 + bw, by + bh, h1 ? 0xFF882222 : 0xFF451515);
            drawCentered("ANNULER", cx + 20 + bw / 2, by + 6, C_WHITE);
            hb(hbCancelBuy, cx + 20, by, bw, bh);
            boolean can = playerBalance >= confirmListing.getTotalPrice();
            boolean h2 = in(mx, my, cx + cw - bw - 20, by, bw, bh);
            drawRect(cx + cw - bw - 20, by, cx + cw - 20, by + bh, can ? (h2 ? 0xFF228844 : 0xFF154525) : 0xFF333333);
            drawCentered(can ? "CONFIRMER" : "SOLDE INSUFF.", cx + cw - bw - 20 + bw / 2, by + 6, can ? C_WHITE : C_GRAY);
            hb(hbOkBuy, cx + cw - bw - 20, by, bw, bh);
            hbConfirmPB[2] = 0;
        }
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
            else if (ok(hbOkBuy) && in(mx, my, hbOkBuy)) {
                if (confirmListing.isDual()) {
                    // Payer en $
                    if (playerBalance >= confirmListing.getTotalPrice()) {
                        HdvPacketHandler.buyItem(confirmListing.getId(), false);
                        confirmListing = null;
                    }
                } else if (confirmListing.isPayPB()) {
                    if ((long) playerPB >= confirmListing.getTotalPrice()) {
                        HdvPacketHandler.buyItem(confirmListing.getId(), true);
                        confirmListing = null;
                    }
                } else {
                    if (playerBalance >= confirmListing.getTotalPrice()) {
                        HdvPacketHandler.buyItem(confirmListing.getId(), false);
                        confirmListing = null;
                    }
                }
            } else if (ok(hbConfirmPB) && in(mx, my, hbConfirmPB) && confirmListing.isDual()) {
                // Payer en PB (dual)
                if ((long) playerPB >= confirmListing.getPricePB()) {
                    HdvPacketHandler.buyItem(confirmListing.getId(), true);
                    confirmListing = null;
                }
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
                fldMinMoney.mouseClicked(mx, my, btn);
                fldMaxMoney.mouseClicked(mx, my, btn);
                fldMinPB.mouseClicked(mx, my, btn);
                fldMaxPB.mouseClicked(mx, my, btn);
                if (ok(hbFltReset) && in(mx, my, hbFltReset)) {
                    fldMinMoney.setText(""); fldMaxMoney.setText("");
                    fldMinPB.setText(""); fldMaxPB.setText("");
                    filterMinMoney = filterMaxMoney = filterMinPB = filterMaxPB = -1L;
                }
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
                if (ok(hbPrM10000) && in(mx, my, hbPrM10000)) { sellPrice = Math.max(1, sellPrice - 10000); priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrM1000)  && in(mx, my, hbPrM1000))  { sellPrice = Math.max(1, sellPrice - 1000);  priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrM100)   && in(mx, my, hbPrM100))   { sellPrice = Math.max(1, sellPrice - 100);   priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrM10)    && in(mx, my, hbPrM10))    { sellPrice = Math.max(1, sellPrice - 10);    priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrP10)    && in(mx, my, hbPrP10))    { sellPrice = Math.min(999999999, sellPrice + 10);    priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrP100)   && in(mx, my, hbPrP100))   { sellPrice = Math.min(999999999, sellPrice + 100);   priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrP1000)  && in(mx, my, hbPrP1000))  { sellPrice = Math.min(999999999, sellPrice + 1000);  priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbPrP10000) && in(mx, my, hbPrP10000)) { sellPrice = Math.min(999999999, sellPrice + 10000); priceField.setText(String.valueOf(sellPrice)); }
                if (ok(hbCurrencyToggle) && in(mx, my, hbCurrencyToggle)) {
                    int rel = mx - hbCurrencyToggle[0];
                    int seg = hbCurrencyToggle[2] / 3;
                    sellCurrMode = rel < seg ? 0 : (rel < 2 * seg ? 1 : 2);
                }
                if (ok(hbPrPBM1000) && in(mx, my, hbPrPBM1000)) { sellPricePB = Math.max(1, sellPricePB - 10);   pricePBField.setText(String.valueOf(sellPricePB)); }
                if (ok(hbPrPBM100)  && in(mx, my, hbPrPBM100))  { sellPricePB = Math.max(1, sellPricePB - 1);    pricePBField.setText(String.valueOf(sellPricePB)); }
                if (ok(hbPrPBP100)  && in(mx, my, hbPrPBP100))  { sellPricePB = Math.min(9999999, sellPricePB + 1);   pricePBField.setText(String.valueOf(sellPricePB)); }
                if (ok(hbPrPBP1000) && in(mx, my, hbPrPBP1000)) { sellPricePB = Math.min(9999999, sellPricePB + 10);  pricePBField.setText(String.valueOf(sellPricePB)); }
                if (sellCurrMode == 2) pricePBField.mouseClicked(mx, my, btn);
                if (ok(hbSellBtn) && in(mx, my, hbSellBtn)) doSell();
                break;
        }
    }

    private void doSell() {
        if (sellItem == null || sellPrice <= 0 || sellQty <= 0) return;
        boolean payPB = (sellCurrMode == 1);
        long pricePB  = (sellCurrMode == 2) ? sellPricePB : 0L;
        HdvPacketHandler.postOffer(sellItem, sellPrice, sellQty, payPB, pricePB);
        sellItem = null; sellSlot = -1;
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (k == 1) { mc.displayGuiScreen(null); return; }
        if (activeTab == TAB_BUY) {
            if (searchField.isFocused()) {
                searchField.textboxKeyTyped(c, k);
                if (k == 28) requestList();
            } else if (fldMinMoney.isFocused()) {
                fldMinMoney.textboxKeyTyped(c, k);
                filterMinMoney = parseLongFilter(fldMinMoney.getText());
            } else if (fldMaxMoney.isFocused()) {
                fldMaxMoney.textboxKeyTyped(c, k);
                filterMaxMoney = parseLongFilter(fldMaxMoney.getText());
            } else if (fldMinPB.isFocused()) {
                fldMinPB.textboxKeyTyped(c, k);
                filterMinPB = parseLongFilter(fldMinPB.getText());
            } else if (fldMaxPB.isFocused()) {
                fldMaxPB.textboxKeyTyped(c, k);
                filterMaxPB = parseLongFilter(fldMaxPB.getText());
            }
        } else if (activeTab == TAB_SELL) {
            if (qtyField.isFocused()) {
                qtyField.textboxKeyTyped(c, k);
                sellQty = (int)parseLong(qtyField.getText());
            } else if (priceField.isFocused()) {
                priceField.textboxKeyTyped(c, k);
                sellPrice = parseLong(priceField.getText());
            } else if (pricePBField.isFocused()) {
                pricePBField.textboxKeyTyped(c, k);
                sellPricePB = parseLong(pricePBField.getText());
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
        pricePBField.updateCursorCounter();
        fldMinMoney.updateCursorCounter();
        fldMaxMoney.updateCursorCounter();
        fldMinPB.updateCursorCounter();
        fldMaxPB.updateCursorCounter();
        PlayerData pd = PlayerDataHandler.getCachedData();
        if (pd != null) { playerBalance = pd.getBalance(); playerPB = pd.getPb(); }
    }

    private List<HdvListing> getFiltered() {
        String f = searchField.getText().trim().toLowerCase();
        List<HdvListing> r = new ArrayList<>();
        for (HdvListing l : listings) {
            String n = l.getItem() != null ? l.getItem().getDisplayName().toLowerCase() : "";
            if (!f.isEmpty() && !n.contains(f) && !l.getSellerName().toLowerCase().contains(f)) continue;
            // Filtres prix $
            if (filterMinMoney >= 0 || filterMaxMoney >= 0) {
                long priceM = !l.isPayPB() ? l.getTotalPrice() : (l.isDual() ? l.getTotalPrice() : -1);
                if (priceM >= 0) {
                    if (filterMinMoney >= 0 && priceM < filterMinMoney) continue;
                    if (filterMaxMoney >= 0 && priceM > filterMaxMoney) continue;
                }
            }
            // Filtres prix PB
            if (filterMinPB >= 0 || filterMaxPB >= 0) {
                long pricePB = l.isPayPB() ? l.getTotalPrice() : (l.isDual() ? l.getPricePB() : -1);
                if (pricePB >= 0) {
                    if (filterMinPB >= 0 && pricePB < filterMinPB) continue;
                    if (filterMaxPB >= 0 && pricePB > filterMaxPB) continue;
                }
            }
            r.add(l);
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
    private long parseLongFilter(String s) { String t = s.trim(); if (t.isEmpty()) return -1L; try { return Long.parseLong(t); } catch (Exception e) { return -1L; } }
}
