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

    // ── Flag action en cours (évite les double-refresh) ───────────────────────
    // NONE=achat/vente standard, CANCEL=retrait vendeur, COLLECT=collecte gains
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

    // ═══════════════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void initGui() {
        net.minecraft.client.custompackets.CustomPacketSystem.init();

        pw = Math.min(800, width  - 16);
        ph = Math.min(490, height - 16);
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        searchField = new GuiTextField(0, fontRendererObj, px + 12, py + 36, pw - 100, 12);
        searchField.setMaxStringLength(48);

        priceField = new GuiTextField(1, fontRendererObj, 0, 0, 80, 12);
        priceField.setMaxStringLength(10);
        priceField.setText(String.valueOf(sellPrice));

        qtyField = new GuiTextField(2, fontRendererObj, 0, 0, 40, 12);
        qtyField.setMaxStringLength(4);
        qtyField.setText(String.valueOf(sellQty));

        invalidateHb();

        HdvPacketHandler.setListListener(nl -> Minecraft.getMinecraft().addScheduledTask(() -> {
            listings.clear(); listings.addAll(nl); loading = false; scroll = 0;
        }));

        HdvPacketHandler.setActionListener((ok, msg) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            statusMsg = msg; statusTimer = 160; statusOk = ok;
            PendingAction action = pendingAction;
            pendingAction = PendingAction.NONE;
            if (ok) {
                if (action == PendingAction.CANCEL || action == PendingAction.COLLECT) {
                    // Retrait ou collecte : myListings déjà purgé localement + requestMyListings déjà envoyé
                    // On rafraîchit juste la liste globale si nécessaire (retrait retire l'item des listings)
                    if (action == PendingAction.CANCEL) {
                        listings.clear(); loading = true;
                        HdvPacketHandler.requestList(0, "");
                    }
                    // loadingMine=true déjà positionné, la réponse myListings mettra à jour
                } else {
                    // Achat ou mise en vente : rafraîchir tout
                    listings.clear(); loading = true;
                    myListings.clear(); loadingMine = true;
                    HdvPacketHandler.requestList(0, "");
                    HdvPacketHandler.requestMyListings();
                }
            }
        }));

        HdvPacketHandler.setMyListingsListener((mine, earnings) -> Minecraft.getMinecraft().addScheduledTask(() -> {
            myListings.clear(); myListings.addAll(mine); pendingEarnings = earnings; loadingMine = false;
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

    private void requestList() {
        loading = true; scroll = 0;
        String f = searchField != null ? searchField.getText().trim() : "";
        HdvPacketHandler.requestList(0, f);
    }

    public void onListingsReceived(List<HdvListing> nl) {
        listings.clear(); listings.addAll(nl); loading = false; scroll = 0;
    }

    public void onMyListingsReceived(List<HdvListing> mine, long earnings) {
        myListings.clear(); myListings.addAll(mine); pendingEarnings = earnings; loadingMine = false;
    }

    private void invalidateHb() {
        hbClose[2] = hbTabBuy[2] = hbTabMine[2] = hbTabSell[2] = 0;
        hbSearchBtn[2] = hbCollect[2] = hbSellBtn[2] = 0;
        hbScrollUp[2] = hbScrollDn[2] = hbScrollMineUp[2] = hbScrollMineDn[2] = 0;
        hbQtyM10[2] = hbQtyM1[2] = hbQtyP1[2] = hbQtyP10[2] = 0;
        hbPrM1000[2] = hbPrM100[2] = hbPrM10[2] = 0;
        hbPrP10[2] = hbPrP100[2] = hbPrP1000[2] = 0;
        hbPrFld[2] = hbOkBuy[2] = hbCancelBuy[2] = 0;
        for (int[] h : hbRows)      h[2] = 0;
        for (int[] h : hbCancelRow) h[2] = 0;
        for (int[] h : hbInvSlots)  h[2] = 0;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RENDU PRINCIPAL
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xBB000011);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        drawRect(px + 3, py + 3, px + pw + 3, py + ph + 3, 0x66000000);
        drawRect(px, py, px + pw, py + ph, C_BG);
        drawRect(px, py,     px + pw, py + 2, C_ACCENT);
        drawRect(px, py + 2, px + pw, py + 3, 0x44AACCFF);
        drawRect(px,          py, px + 1,  py + ph, C_BORDER);
        drawRect(px + pw - 1, py, px + pw, py + ph, C_BORDER);
        drawRect(px, py + ph - 1, px + pw, py + ph, C_BORDER);

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
            int sx  = px + (pw - sw) / 2 - 6, sy = py + ph - 20;
            int bgA = (alpha / 2) << 24;
            drawRect(sx - 2, sy - 3, sx + sw + 8, sy + 11, bgA);
            drawRect(sx - 2, sy - 3, sx + sw + 8, sy - 2,
                    (alpha / 3 << 24) | (statusOk ? 0x33CC77 : 0xEE3333));
            fontRendererObj.drawStringWithShadow(statusMsg, sx + 2, sy, col);
        }

        if (confirmListing != null) drawConfirmOverlay(mx, my);
        if (tooltipItem != null)    drawTooltip(tooltipItem, tooltipX, tooltipY);

        GlStateManager.disableBlend();
        super.drawScreen(mx, my, pt);
    }

    // ── Tooltip ──────────────────────────────────────────────────────────────

    private void drawTooltip(ItemStack item, int mx, int my) {
        List<String> lines = new ArrayList<>();
        lines.add(item.getDisplayName());
        if (item.stackSize > 1)   lines.add("\u00a77Quantite : \u00a7f" + item.stackSize);
        if (item.isItemDamaged()) lines.add("\u00a77Durabilite : \u00a7f" + (item.getMaxDamage() - item.getItemDamage()) + "/" + item.getMaxDamage());

        int tw = 0;
        for (String l : lines) tw = Math.max(tw, fontRendererObj.getStringWidth(l));
        int th = lines.size() * 10 + 6;
        int tx = Math.min(mx + 12, width  - tw - 10);
        int ty = Math.max(my - th - 6, 4);

        drawRect(tx - 3, ty - 3, tx + tw + 5, ty + th + 3, 0xEE080814);
        drawRect(tx - 3, ty - 3, tx + tw + 5, ty - 2, C_ACCENT);
        drawRect(tx - 3, ty + th + 2, tx + tw + 5, ty + th + 3, C_ACCENT);
        drawRect(tx - 3, ty - 2, tx - 2, ty + th + 2, 0xFF1A3A6A);
        for (int i = 0; i < lines.size(); i++)
            fontRendererObj.drawStringWithShadow(lines.get(i), tx, ty + 2 + i * 10, i == 0 ? C_WHITE : C_GRAY);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EN-TETE
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawHeader(int mx, int my) {
        drawRect(px, py, px + pw, py + 28, C_HEADER);
        drawRect(px, py + 27, px + pw, py + 28, C_ACCENT);

        drawRect(px + 7,  py + 8,  px + 15, py + 20, 0xFF2277EE);
        drawRect(px + 8,  py + 9,  px + 14, py + 19, C_BG);
        drawRect(px + 9,  py + 11, px + 13, py + 17, 0xFF55AAFF);

        fontRendererObj.drawStringWithShadow("\u00a7b\u00a7lHDV \u00a7r\u00a7fHotel des Ventes", px + 18, py + 10, C_WHITE);

        String bal = "\u00a77Solde : \u00a76" + fmtGold(playerBalance) + " $";
        int bw = fontRendererObj.getStringWidth(bal);
        drawRect(px + pw - bw - 42, py + 6, px + pw - 22, py + 22, 0x33FFFFFF);
        drawRect(px + pw - bw - 42, py + 6, px + pw - 22, py + 7,  0xFF1A4080);
        fontRendererObj.drawStringWithShadow(bal, px + pw - bw - 36, py + 10, C_WHITE);

        int cx = px + pw - 20, cy = py + 7;
        boolean ch = in(mx, my, cx, cy, 14, 14);
        drawRect(cx, cy, cx + 14, cy + 14, ch ? 0xCC993311 : 0x88441100);
        drawRect(cx, cy, cx + 14, cy + 1,  ch ? C_RED : 0xFF663322);
        drawRect(cx, cy, cx + 1,  cy + 14, ch ? C_RED : 0xFF663322);
        drawRect(cx + 13, cy, cx + 14, cy + 14, ch ? C_RED : 0xFF663322);
        drawRect(cx, cy + 13, cx + 14, cy + 14, ch ? C_RED : 0xFF663322);
        fontRendererObj.drawString("x", cx + 4, cy + 3, ch ? C_WHITE : 0xFFCC7755);
        hb(hbClose, cx, cy, 14, 14);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ONGLETS
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawTabs(int mx, int my) {
        String[] labels = { "Acheter", "Mes annonces", "Vendre" };
        String[] badges = {
            loading     ? "..." : String.valueOf(getFiltered().size()),
            loadingMine ? ""    : String.valueOf(myListings.size()),
            ""
        };
        int[][] hbs = { hbTabBuy, hbTabMine, hbTabSell };
        int tw = 146, th = 20, ty = py + 30, startX = px + 6;

        for (int i = 0; i < 3; i++) {
            int tx = startX + i * (tw + 3);
            boolean active = (activeTab == i), hov = in(mx, my, tx, ty, tw, th);
            int bg  = active ? 0xFF0F2040 : (hov ? 0xFF0A1830 : 0xFF060D1A);
            int top = active ? C_ACCENT   : (hov ? 0xFF1A3A6A : 0xFF0F1E30);
            int tc  = active ? C_WHITE    : (hov ? 0xFFCCDDFF : C_GRAY);

            drawRect(tx, ty, tx + tw, ty + th, bg);
            if (active) {
                drawRect(tx,     ty,     tx + tw,     ty + 2, C_ACCENT);
                drawRect(tx + 1, ty + 2, tx + tw - 1, ty + 3, 0x33AACCFF);
                drawRect(tx + 1, ty + th - 1, tx + tw - 1, ty + th, C_BG);
            } else {
                drawRect(tx, ty, tx + tw, ty + 1, top);
            }

            int labelX = tx + tw / 2 - (badges[i].isEmpty() ? 0 : 6);
            drawCentered(labels[i], labelX, ty + 6, tc);

            if (!badges[i].isEmpty()) {
                int bx = tx + tw - 20, by_ = ty + 4;
                drawRect(bx, by_, bx + 16, by_ + 12, active ? C_ACCENT : (hov ? 0xFF1A3A6A : 0xFF0F1E30));
                drawRect(bx, by_, bx + 16, by_ + 1, active ? C_ACCENT2 : C_BORDER);
                drawCentered(badges[i], bx + 8, by_ + 2, active ? C_WHITE : C_GRAY);
            }
            hb(hbs[i], tx, ty, tw, th);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ONGLET ACHETER
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawBuyTab(int mx, int my) {
        int cy = py + 52;

        // Barre de recherche
        int sfW = pw - 104;
        drawRect(px + 8, cy - 2, px + 8 + sfW + 2, cy + 14, C_PANEL);
        drawBorder(px + 8, cy - 2, sfW + 2, 16,
                searchField != null && searchField.isFocused() ? C_ACCENT : 0xFF0F2040);
        if (searchField != null) {
            searchField.xPosition = px + 12;
            searchField.yPosition = cy + 1;
            searchField.width     = sfW - 4;
            searchField.drawTextBox();
            if (searchField.getText().isEmpty())
                fontRendererObj.drawString("\u00a77Rechercher un item ou un vendeur...", px + 14, cy + 2, 0x44AAAAAA);
        }

        int bsx = px + 12 + sfW + 2, bsy = cy - 2;
        boolean bh = in(mx, my, bsx, bsy, 76, 16);
        drawRect(bsx, bsy, bsx + 76, bsy + 16, bh ? 0xFF1A4070 : 0xFF0D2545);
        drawRect(bsx, bsy, bsx + 76, bsy + 1,  bh ? C_ACCENT2  : C_BORDER);
        drawCentered("Chercher", bsx + 38, bsy + 4, bh ? C_WHITE : 0xFFAABBDD);
        hb(hbSearchBtn, bsx, bsy, 76, 16);
        cy += 20;

        int lx = px + 8, tw = pw - 16;
        drawRect(lx, cy, lx + tw, cy + 16, 0xFF040B18);
        drawRect(lx, cy + 15, lx + tw, cy + 16, 0x55AACCFF);
        fontRendererObj.drawString("\u00a77Item",       lx + 30,  cy + 4, C_GRAY);
        fontRendererObj.drawString("\u00a77Vendeur",    lx + 230, cy + 4, C_GRAY);
        fontRendererObj.drawString("\u00a77Qte",        lx + 355, cy + 4, C_GRAY);
        fontRendererObj.drawString("\u00a77Prix total", lx + 415, cy + 4, C_GRAY);
        fontRendererObj.drawString("\u00a77Prix/u",     lx + 505, cy + 4, C_GRAY);
        cy += 16;

        listY0 = cy;
        listH  = ph - (cy - py) - 24;
        int maxRows = Math.max(1, listH / ROW_H);

        if (loading) { drawLoading(listY0, listH); return; }

        List<HdvListing> fl = getFiltered();
        if (scroll > Math.max(0, fl.size() - maxRows)) scroll = Math.max(0, fl.size() - maxRows);

        if (fl.isEmpty()) {
            drawEmpty("Aucune annonce disponible.", listY0, listH);
            for (int[] h : hbRows) h[2] = 0;
        } else {
            int y = listY0, end = Math.min(scroll + maxRows, fl.size());
            for (int i = scroll; i < end; i++) {
                drawBuyRow(mx, my, lx, y, tw, fl.get(i), i - scroll);
                y += ROW_H;
            }
            for (int i = end - scroll; i < hbRows.length; i++) hbRows[i][2] = 0;
        }

        if (!fl.isEmpty() && fl.size() > maxRows)
            drawScrollbar(px + pw - 18, listY0, listH, scroll, fl.size(), maxRows, mx, my, hbScrollUp, hbScrollDn);

        fontRendererObj.drawString("\u00a77" + getFiltered().size() + " annonce(s)", px + 10, py + ph - 14, C_GRAY);
    }

    private void drawBuyRow(int mx, int my, int x, int y, int w, HdvListing l, int idx) {
        boolean hov = in(mx, my, x, y, w, ROW_H);
        boolean sel = confirmListing != null && l.getId() == confirmListing.getId();
        int rowBg = sel ? C_SEL : (hov ? C_HOV : (idx % 2 == 0 ? 0x110A1420 : 0x00000000));
        drawRect(x, y, x + w, y + ROW_H, rowBg);
        drawRect(x, y + ROW_H - 1, x + w, y + ROW_H, 0x1AFFFFFF);
        if (hov || sel) drawRect(x, y, x + 2, y + ROW_H, sel ? C_GREEN : C_ACCENT);

        ItemStack item = l.getItem();
        if (item != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(item, x + 6, y + (ROW_H - 16) / 2);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            if (hov) { tooltipItem = item; tooltipX = mx; tooltipY = my; }
            String name = item.getDisplayName();
            if (fontRendererObj.getStringWidth(name) > 160) name = name.substring(0, 15) + "\u00a77...";
            fontRendererObj.drawStringWithShadow(name, x + 27, y + (ROW_H - 8) / 2, C_WHITE);
        } else {
            fontRendererObj.drawString("\u00a7cItem invalide", x + 6, y + (ROW_H - 8) / 2, C_RED);
        }

        fontRendererObj.drawString("\u00a7b" + l.getSellerName(), x + 230, y + (ROW_H - 8) / 2, 0xFFAADDFF);
        fontRendererObj.drawString("\u00a7f" + l.getQuantity(),   x + 357, y + (ROW_H - 8) / 2, C_WHITE);
        fontRendererObj.drawStringWithShadow("\u00a76" + fmtGold(l.getTotalPrice()) + " $", x + 415, y + (ROW_H - 8) / 2, C_GOLD);
        fontRendererObj.drawString("\u00a77" + fmtGold(l.getPricePerUnit()) + "$/u",         x + 505, y + (ROW_H - 8) / 2, C_GRAY);

        boolean canAfford = playerBalance >= l.getTotalPrice();
        int bx = x + w - 72, by = y + (ROW_H - 14) / 2;
        boolean bhovr = in(mx, my, bx, by, 66, 14);
        if (canAfford) {
            drawRect(bx, by, bx + 66, by + 14, bhovr ? 0xFF1A6030 : 0xFF0D3515);
            drawRect(bx, by, bx + 66, by + 1, bhovr ? C_GREEN : 0xFF1A6030);
            drawCentered(bhovr ? "\u00a7fAcheter" : "\u00a7aAcheter", bx + 33, by + 3, C_WHITE);
        } else {
            drawRect(bx, by, bx + 66, by + 14, 0xFF2A1010);
            drawRect(bx, by, bx + 66, by + 1, C_RED);
            drawCentered("\u00a7cInsuffisant", bx + 33, by + 3, 0xFFCC5555);
        }
        if (idx < hbRows.length) hb(hbRows[idx], x, y, w, ROW_H);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ONGLET MES ANNONCES
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawMineTab(int mx, int my) {
        int lx = px + 8, tw = pw - 16, headerY = py + 52;

        drawRect(lx, headerY, lx + tw, headerY + 16, 0xFF040B18);
        drawRect(lx, headerY + 15, lx + tw, headerY + 16, 0x55AACCFF);
        fontRendererObj.drawString("\u00a77Item",     lx + 30,  headerY + 4, C_GRAY);
        fontRendererObj.drawString("\u00a77Quantite", lx + 260, headerY + 4, C_GRAY);
        fontRendererObj.drawString("\u00a77Prix/u",   lx + 375, headerY + 4, C_GRAY);
        fontRendererObj.drawString("\u00a77Statut",   lx + 475, headerY + 4, C_GRAY);

        listMineY0 = headerY + 16;
        listMineH  = ph - (listMineY0 - py) - 54;
        int maxRows = Math.max(1, listMineH / ROW_H);

        if (loadingMine) {
            drawLoading(listMineY0, listMineH);
        } else if (myListings.isEmpty()) {
            drawEmpty("Vous n'avez aucune annonce active.", listMineY0, listMineH);
        } else {
            if (scrollMine > Math.max(0, myListings.size() - maxRows))
                scrollMine = Math.max(0, myListings.size() - maxRows);

            int y = listMineY0, cancelIdx = 0;
            int end = Math.min(scrollMine + maxRows, myListings.size());
            for (int i = scrollMine; i < end; i++) {
                HdvListing l = myListings.get(i);
                boolean sold = l.isSold(), hov = in(mx, my, lx, y, tw, ROW_H);
                int rowIdx   = i - scrollMine;

                int rowBg = sold ? C_SOLD_BG : (hov ? C_HOV : (rowIdx % 2 == 0 ? 0x110A1420 : 0x00000000));
                drawRect(lx, y, lx + tw, y + ROW_H, rowBg);
                drawRect(lx, y, lx + 3, y + ROW_H, sold ? 0xFF22AA55 : 0xFF2255AA);
                drawRect(lx, y + ROW_H - 1, lx + tw, y + ROW_H, 0x1AFFFFFF);

                if (l.getItem() != null) {
                    GlStateManager.enableDepth();
                    RenderHelper.enableGUIStandardItemLighting();
                    itemRender.renderItemAndEffectIntoGUI(l.getItem(), lx + 8, y + (ROW_H - 16) / 2);
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.disableDepth();
                    if (hov) { tooltipItem = l.getItem(); tooltipX = mx; tooltipY = my; }
                    String name = l.getItem().getDisplayName();
                    if (fontRendererObj.getStringWidth(name) > 195) name = name.substring(0, 17) + "\u00a7f...";
                    fontRendererObj.drawStringWithShadow(name, lx + 28, y + (ROW_H - 8) / 2, sold ? 0xFFAAFFBB : C_WHITE);
                } else {
                    fontRendererObj.drawString("\u00a77Item ?", lx + 28, y + (ROW_H - 8) / 2, C_GRAY);
                }

                fontRendererObj.drawString("\u00a7f" + l.getQuantity(), lx + 268, y + (ROW_H - 8) / 2, sold ? 0xFFBBFFCC : C_WHITE);
                fontRendererObj.drawStringWithShadow("\u00a76" + fmtGold(l.getPricePerUnit()) + " $", lx + 375, y + (ROW_H - 8) / 2, C_GOLD);

                int bx = lx + tw - 74, by = y + (ROW_H - 14) / 2;
                boolean bhovr = in(mx, my, bx, by, 68, 14);

                if (sold) {
                    fontRendererObj.drawStringWithShadow("\u00a7aVENDU", lx + 475, y + (ROW_H - 8) / 2, C_GREEN);
                    if (pendingEarnings > 0 && cancelIdx < hbCancelRow.length) {
                        drawRect(bx, by, bx + 68, by + 14, bhovr ? 0xFF1A6030 : 0xFF0D3518);
                        drawRect(bx, by, bx + 68, by + 1,  bhovr ? C_GREEN : 0xFF1A6030);
                        drawCentered(bhovr ? "\u00a7fCollecter" : "\u00a7aCollecter", bx + 34, by + 3, C_WHITE);
                        hb(hbCancelRow[cancelIdx], bx, by, 68, 14);
                        cancelIdx++;
                    }
                } else {
                    long rem = l.getExpiresAt() - System.currentTimeMillis() / 1000L;
                    String timeStr = rem > 0 ? "\u00a77" + fmtTime(rem) : "\u00a7cExpiree";
                    fontRendererObj.drawString(timeStr, lx + 475, y + (ROW_H - 8) / 2, rem > 0 ? 0xFFAABBCC : C_RED);
                    if (cancelIdx < hbCancelRow.length) {
                        drawRect(bx, by, bx + 68, by + 14, bhovr ? 0xCC551111 : 0x88220A0A);
                        drawRect(bx, by, bx + 68, by + 1,  bhovr ? C_RED : 0xFF661111);
                        drawCentered(bhovr ? "\u00a7fRetirer" : "\u00a7cRetirer", bx + 34, by + 3, bhovr ? C_WHITE : 0xFFCC5555);
                        hb(hbCancelRow[cancelIdx], bx, by, 68, 14);
                        cancelIdx++;
                    }
                }
                y += ROW_H;
            }
            for (int i = cancelIdx; i < hbCancelRow.length; i++) hbCancelRow[i][2] = 0;

            if (myListings.size() > maxRows)
                drawScrollbar(px + pw - 18, listMineY0, listMineH, scrollMine, myListings.size(), maxRows, mx, my, hbScrollMineUp, hbScrollMineDn);
        }

        // ── Bandeau "Collecter mes gains" ─────────────────────────────────────
        int gy = py + ph - 50;
        boolean hasPending = pendingEarnings > 0;
        drawRect(px + 8, gy - 6, px + pw - 8, gy - 5, 0x33AACCFF);

        int gx = px + pw / 2 - 140;
        boolean gh = in(mx, my, gx, gy, 280, 32);

        if (hasPending) {
            long t = System.currentTimeMillis() / 600;
            boolean pulse = (t % 2 == 0);
            drawRect(gx, gy, gx + 280, gy + 32, gh ? 0xFF1A5028 : (pulse ? 0xFF0F2A18 : 0xFF0D2515));
            drawBorder(gx, gy, 280, 32, gh ? C_GREEN : (pulse ? 0xFF22AA55 : 0xFF1A6030));
            drawRect(gx + 8,  gy + 8,  gx + 24, gy + 24, 0xFFCC9900);
            drawRect(gx + 9,  gy + 9,  gx + 23, gy + 23, 0xFF0D2515);
            drawRect(gx + 11, gy + 11, gx + 21, gy + 21, 0xFFFFDD00);
            String txt = gh
                ? "\u00a7fCollecter \u00a76" + fmtGold(pendingEarnings) + " $ \u00a7a(cliquez)"
                : "\u00a7aGains disponibles : \u00a76" + fmtGold(pendingEarnings) + " $";
            fontRendererObj.drawStringWithShadow(txt, gx + 30, gy + 12, gh ? C_WHITE : C_GREEN);
        } else {
            drawRect(gx, gy, gx + 280, gy + 32, 0xFF0D1220);
            drawBorder(gx, gy, 280, 32, 0xFF1A2A35);
            drawCentered("\u00a77Aucun gain en attente", gx + 140, gy + 12, C_GRAY);
        }
        hb(hbCollect, gx, gy, 280, 32);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ONGLET VENDRE
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawSellTab(int mx, int my) {
        int margin = 8, halfW = (pw - margin * 3) / 2;
        int leftX  = px + margin, rightX = leftX + halfW + margin;
        int topY   = py + 52, botH = ph - 60;

        // ── Panneau inventaire ────────────────────────────────────────────────
        drawRect(leftX, topY, leftX + halfW, topY + botH, C_PANEL);
        drawBorder(leftX, topY, halfW, botH, C_ACCENT);
        drawRect(leftX, topY, leftX + halfW, topY + 18, C_HEADER);
        drawRect(leftX, topY + 17, leftX + halfW, topY + 18, C_ACCENT);
        fontRendererObj.drawStringWithShadow("\u00a7b\u00a7lInventaire", leftX + 8, topY + 5, C_ACCENT2);

        InventoryPlayer inv = mc.thePlayer.inventory;
        int sz = 18, gx = leftX + (halfW - 9 * sz) / 2, gy = topY + 22;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawInvSlot(mx, my, gx + col * sz, gy + row * sz, sz, 9 + row * 9 + col, inv);
        int sepY = gy + 3 * sz + 5;
        drawRect(gx, sepY, gx + 9 * sz, sepY + 1, 0x55AACCFF);
        for (int col = 0; col < 9; col++)
            drawInvSlot(mx, my, gx + col * sz, sepY + 6, sz, col, inv);

        fontRendererObj.drawString("\u00a77Cliquez sur un item pour le selectionner",
                leftX + 6, topY + botH - 12, 0x44AAAAAA);

        // ── Panneau configuration vente ───────────────────────────────────────
        drawRect(rightX, topY, rightX + halfW, topY + botH, 0xCC080E12);
        drawBorder(rightX, topY, halfW, botH, 0xFF1A6030);
        drawRect(rightX, topY, rightX + halfW, topY + 18, C_HEADER);
        drawRect(rightX, topY + 17, rightX + halfW, topY + 18, 0xFF1A6030);
        fontRendererObj.drawStringWithShadow("\u00a7a\u00a7lConfiguration vente", rightX + 8, topY + 5, C_GREEN);

        int ry = topY + 22;

        if (sellItem != null) {
            int pvX = rightX + halfW / 2 - 8;
            drawRect(pvX - 5, ry - 3, pvX + 21, ry + 21, 0xFF0A1020);
            drawBorder(pvX - 5, ry - 3, 26, 24, 0xFF1A4060);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(sellItem, pvX, ry);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            drawCentered(sellItem.getDisplayName(), rightX + halfW / 2, ry + 22, C_WHITE);
            int avail = countInInv(sellItem, inv);
            drawCentered("\u00a77En stock : \u00a7f" + avail + "  \u00a77Slot #" + (sellSlot + 1), rightX + halfW / 2, ry + 33, C_GRAY);
        } else {
            drawRect(rightX + halfW / 2 - 34, ry, rightX + halfW / 2 + 34, ry + 28, 0xFF080E18);
            drawBorder(rightX + halfW / 2 - 34, ry, 68, 28, 0xFF0F2030);
            drawCentered("\u00a77<- Selectionnez un item", rightX + halfW / 2, ry + 10, 0xFF556688);
        }
        ry += 48;

        drawRect(rightX + 8, ry, rightX + halfW - 8, ry + 1, 0x22AACCFF);
        ry += 8;

        // ── Quantite ──────────────────────────────────────────────────────────
        fontRendererObj.drawStringWithShadow("\u00a77Quantite :", rightX + 8, ry, C_GRAY);
        ry += 12;
        int bw = 22, bh = 14, qTot = bw * 4 + 3 * 3 + 44 + 20;
        int qx = rightX + (halfW - qTot) / 2;
        miniBtn(mx, my, qx,              ry, bw, bh, "-10", 0xFF2A0808, 0xFF991111, C_RED,   hbQtyM10);
        miniBtn(mx, my, qx + bw + 3,     ry, bw, bh, "-1",  0xFF2A0808, 0xFF991111, C_RED,   hbQtyM1);
        if (qtyField != null) {
            qtyField.xPosition = qx + (bw + 3) * 2;
            qtyField.yPosition = ry;
            qtyField.width     = 44;
            if (!qtyField.isFocused()) qtyField.setText(String.valueOf(sellQty));
            drawRect(qtyField.xPosition - 2, qtyField.yPosition - 2,
                     qtyField.xPosition + qtyField.width + 2, qtyField.yPosition + 14, C_PANEL);
            drawBorder(qtyField.xPosition - 2, qtyField.yPosition - 2, qtyField.width + 4, 16,
                     qtyField.isFocused() ? C_ACCENT : 0xFF0F2040);
            qtyField.drawTextBox();
        }
        int startPlus = qx + (bw + 3) * 2 + 44 + 3;
        miniBtn(mx, my, startPlus,          ry, bw, bh, "+1",  0xFF081808, 0xFF119911, C_GREEN, hbQtyP1);
        miniBtn(mx, my, startPlus + bw + 3, ry, bw, bh, "+10", 0xFF081808, 0xFF119911, C_GREEN, hbQtyP10);
        ry += bh + 14;

        drawRect(rightX + 8, ry, rightX + halfW - 8, ry + 1, 0x22AACCFF);
        ry += 8;

        // ── Prix du lot ───────────────────────────────────────────────────────
        fontRendererObj.drawStringWithShadow("\u00a77Prix :", rightX + 8, ry, C_GRAY);
        ry += 12;
        int pfW = halfW - 16;
        if (priceField != null) {
            priceField.xPosition = rightX + 8;
            priceField.yPosition = ry + 1;
            priceField.width     = pfW - 20;
            if (!priceField.isFocused()) priceField.setText(String.valueOf(sellPrice));
        }
        drawRect(rightX + 6, ry - 2, rightX + 6 + pfW, ry + 14, C_PANEL);
        drawBorder(rightX + 6, ry - 2, pfW, 16,
                priceField != null && priceField.isFocused() ? C_ACCENT : 0xFF0F2040);
        if (priceField != null) priceField.drawTextBox();
        fontRendererObj.drawStringWithShadow("\u00a76$", rightX + pfW - 6, ry + 1, C_GOLD);
        hb(hbPrFld, rightX + 6, ry - 2, pfW, 16);
        ry += 18;

        int pb = 30, pbH = 13, prTot = pb * 6 + 5 * 2;
        int prx = rightX + (halfW - prTot) / 2;
        miniBtn(mx, my, prx,            ry, pb, pbH, "-1K",  0xFF2A0808, 0xFF991111, C_RED,   hbPrM1000);
        miniBtn(mx, my, prx + (pb+2),   ry, pb, pbH, "-100", 0xFF2A0808, 0xFF991111, C_RED,   hbPrM100);
        miniBtn(mx, my, prx + (pb+2)*2, ry, pb, pbH, "-10",  0xFF2A0808, 0xFF991111, C_RED,   hbPrM10);
        miniBtn(mx, my, prx + (pb+2)*3, ry, pb, pbH, "+10",  0xFF081808, 0xFF119911, C_GREEN, hbPrP10);
        miniBtn(mx, my, prx + (pb+2)*4, ry, pb, pbH, "+100", 0xFF081808, 0xFF119911, C_GREEN, hbPrP100);
        miniBtn(mx, my, prx + (pb+2)*5, ry, pb, pbH, "+1K",  0xFF081808, 0xFF119911, C_GREEN, hbPrP1000);
        ry += pbH + 10;

        // Recapitulatif prix
        long unitPrice = sellQty > 0 ? sellPrice / sellQty : 0;
        drawRect(rightX + 8, ry, rightX + halfW - 8, ry + 26, 0xFF060C18);
        drawBorder(rightX + 8, ry, halfW - 16, 26, 0xFF0F2040);
        fontRendererObj.drawStringWithShadow("\u00a77Unitaire : \u00a76" + fmtGold(unitPrice) + " $",
                rightX + 14, ry + 4, C_GOLD);
        fontRendererObj.drawStringWithShadow("\u00a77Total    : \u00a76" + fmtGold(sellPrice) + " $",
                rightX + 14, ry + 15, 0xFFCCAA00);

        // Bouton Vendre
        boolean canSell = sellItem != null && sellPrice > 0 && sellQty > 0;
        int sbX = rightX + 8, sbY = topY + botH - 26, sbW = halfW - 16;
        boolean sbH = in(mx, my, sbX, sbY, sbW, 20);
        if (canSell) {
            drawRect(sbX, sbY, sbX + sbW, sbY + 20, sbH ? 0xFF1A6030 : 0xFF0D3518);
            drawBorder(sbX, sbY, sbW, 20, sbH ? C_GREEN : 0xFF1A6030);
            drawCentered(sbH ? "\u00a7f>> Mettre en vente <<" : "\u00a7aMise en vente", sbX + sbW / 2, sbY + 6, sbH ? C_WHITE : C_GREEN);
        } else {
            drawRect(sbX, sbY, sbX + sbW, sbY + 20, 0xFF141820);
            drawBorder(sbX, sbY, sbW, 20, 0xFF252A35);
            drawCentered(sellItem != null ? "\u00a77Prix ou quantite invalide" : "\u00a77Selectionnez un item a vendre",
                    sbX + sbW / 2, sbY + 6, 0xFF445566);
        }
        hb(hbSellBtn, sbX, sbY, sbW, 20);
    }

    private void drawInvSlot(int mx, int my, int sx, int sy, int sz, int idx, InventoryPlayer inv) {
        ItemStack stack = (idx >= 0 && idx < inv.mainInventory.length) ? inv.mainInventory[idx] : null;
        boolean sel = (sellSlot == idx), hov = stack != null && in(mx, my, sx, sy, sz - 1, sz - 1);
        int slotBg = sel ? 0xFF1A3A20 : (hov ? 0xFF1A2840 : 0xFF080E18);
        drawRect(sx, sy, sx + sz - 1, sy + sz - 1, slotBg);
        int borderCol = sel ? C_GREEN : (hov ? C_ACCENT : 0xFF1A2A40);
        drawRect(sx,       sy,       sx + sz - 1, sy + 1,       borderCol);
        drawRect(sx,       sy,       sx + 1,       sy + sz - 1, borderCol);
        drawRect(sx + sz - 2, sy, sx + sz - 1, sy + sz - 1, sel ? C_GREEN : 0xFF0F1E30);
        drawRect(sx, sy + sz - 2, sx + sz - 1, sy + sz - 1, sel ? C_GREEN : 0xFF0F1E30);
        if (stack != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(stack, sx + 1, sy + 1);
            itemRender.renderItemOverlayIntoGUI(fontRendererObj, stack, sx + 1, sy + 1, null);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            if (hov) { tooltipItem = stack; tooltipX = mx; tooltipY = my; }
        }
        if (idx >= 0 && idx < 36) hb(hbInvSlots[idx], sx, sy, sz - 1, sz - 1);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  OVERLAY CONFIRMATION ACHAT
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawConfirmOverlay(int mx, int my) {
        drawRect(0, 0, width, height, 0xCC000011);
        int cw = 350, ch = 190, cx = width / 2 - cw / 2, cy = height / 2 - ch / 2;

        drawRect(cx + 4, cy + 4, cx + cw + 4, cy + ch + 4, 0x66000000);
        drawRect(cx, cy, cx + cw, cy + ch, 0xF2040A14);
        drawBorder(cx, cy, cw, ch, C_ACCENT);
        drawRect(cx, cy,     cx + cw, cy + 2,  C_ACCENT);
        drawRect(cx, cy + 2, cx + cw, cy + 3,  0x33AACCFF);
        drawRect(cx, cy,     cx + cw, cy + 22, C_HEADER);
        drawRect(cx, cy + 21, cx + cw, cy + 22, C_ACCENT);

        drawCentered("\u00a7b\u00a7lConfirmer l'achat", cx + cw / 2, cy + 7, C_ACCENT2);

        ItemStack item = confirmListing.getItem();
        if (item != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(item, cx + 12, cy + 30);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            fontRendererObj.drawStringWithShadow("\u00a7f" + item.getDisplayName(), cx + 32, cy + 34, C_WHITE);
        }
        fontRendererObj.drawString("\u00a77Vendeur : \u00a7b" + confirmListing.getSellerName(), cx + 12, cy + 52, 0xFFAADDFF);
        fontRendererObj.drawStringWithShadow("\u00a77Total : \u00a76" + fmtGold(confirmListing.getTotalPrice()) + " $", cx + 12, cy + 64, C_GOLD);
        fontRendererObj.drawString("\u00a77Unitaire : \u00a7f" + fmtGold(confirmListing.getPricePerUnit()) + " $/u", cx + cw / 2 + 10, cy + 64, C_GRAY);
        fontRendererObj.drawString("\u00a77Quantite : \u00a7f" + confirmListing.getQuantity(), cx + 12, cy + 76, C_GRAY);

        boolean canAfford = playerBalance >= confirmListing.getTotalPrice();
        String soldeCol = canAfford ? "\u00a7a" : "\u00a7c";
        fontRendererObj.drawString("\u00a77Votre solde : " + soldeCol + fmtGold(playerBalance) + " $",
                cx + 12, cy + 88, canAfford ? C_GREEN : C_RED);
        if (!canAfford) {
            long manque = confirmListing.getTotalPrice() - playerBalance;
            fontRendererObj.drawString("\u00a7cIl vous manque \u00a7f" + fmtGold(manque) + " $", cx + 12, cy + 100, C_RED);
        }

        int btnW = 130, by = cy + ch - 36;
        boolean c1 = in(mx, my, cx + 12, by, btnW, 22);
        drawRect(cx + 12, by, cx + 12 + btnW, by + 22, c1 ? 0xCC551111 : 0x88220A0A);
        drawBorder(cx + 12, by, btnW, 22, c1 ? C_RED : 0xFF661111);
        drawCentered(c1 ? "\u00a7fAnnuler" : "\u00a7cAnnuler", cx + 12 + btnW / 2, by + 7, c1 ? C_WHITE : 0xFFCC5555);
        hb(hbCancelBuy, cx + 12, by, btnW, 22);

        int bx2 = cx + cw - 12 - btnW;
        boolean c2 = in(mx, my, bx2, by, btnW, 22);
        drawRect(bx2, by, bx2 + btnW, by + 22, c2 && canAfford ? 0xFF1A6030 : (canAfford ? 0xFF0D3518 : 0xFF2A1010));
        drawBorder(bx2, by, btnW, 22, c2 && canAfford ? C_GREEN : (canAfford ? 0xFF1A6030 : C_RED));
        drawCentered(canAfford ? (c2 ? "\u00a7fAcheter" : "\u00a7aAcheter") : "\u00a7cFonds insuffisants",
                bx2 + btnW / 2, by + 7, canAfford ? (c2 ? C_WHITE : C_GREEN) : C_RED);
        hb(hbOkBuy, bx2, by, btnW, 22);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SCROLLBAR
    // ═══════════════════════════════════════════════════════════════════════════

    private void drawScrollbar(int ax, int ly0, int lh, int cur, int total,
                               int maxRows, int mx, int my, int[] hbUp, int[] hbDn) {
        boolean canUp = cur > 0, canDn = cur < total - maxRows;

        int upBg = canUp ? (in(mx, my, ax, ly0, 16, 14) ? 0xFF1A3A6A : 0xFF0F1E35) : 0xFF080C14;
        drawRect(ax, ly0, ax + 16, ly0 + 14, upBg);
        drawRect(ax, ly0, ax + 16, ly0 + 1, canUp ? C_BORDER : 0xFF0F1E30);
        fontRendererObj.drawString("^", ax + 5, ly0 + 3, canUp ? 0xFFAADDFF : 0xFF2A3A55);
        hb(hbUp, ax, ly0, 16, 14);

        int dnBg = canDn ? (in(mx, my, ax, ly0 + lh - 14, 16, 14) ? 0xFF1A3A6A : 0xFF0F1E35) : 0xFF080C14;
        drawRect(ax, ly0 + lh - 14, ax + 16, ly0 + lh, dnBg);
        drawRect(ax, ly0 + lh - 15, ax + 16, ly0 + lh - 14, canDn ? C_BORDER : 0xFF0F1E30);
        fontRendererObj.drawString("v", ax + 5, ly0 + lh - 11, canDn ? 0xFFAADDFF : 0xFF2A3A55);
        hb(hbDn, ax, ly0 + lh - 14, 16, 14);

        if (total > maxRows) {
            int trackH = lh - 28;
            int thumbH = Math.max(14, trackH * maxRows / total);
            int thumbY = ly0 + 14 + (trackH - thumbH) * cur / (total - maxRows);
            drawRect(ax, ly0 + 14, ax + 16, ly0 + lh - 14, 0xFF050A12);
            drawRect(ax + 3, thumbY, ax + 13, thumbY + thumbH, 0xFF1A3A6A);
            drawRect(ax + 3, thumbY, ax + 13, thumbY + 1, C_ACCENT);
        }
    }

    private void drawLoading(int y0, int h) {
        int dots = (int)((System.currentTimeMillis() / 400) % 4);
        StringBuilder sb = new StringBuilder("\u00a77Chargement");
        for (int d = 0; d < dots; d++) sb.append('.');
        drawCentered(sb.toString(), px + pw / 2, y0 + h / 2 - 4, C_GRAY);
    }

    private void drawEmpty(String msg, int y0, int h) {
        drawCentered("\u00a77" + msg, px + pw / 2, y0 + h / 2 - 4, C_GRAY);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  EVENEMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (confirmListing != null) {
            if (ok(hbCancelBuy) && in(mx, my, hbCancelBuy)) { confirmListing = null; return; }
            if (ok(hbOkBuy) && in(mx, my, hbOkBuy) && playerBalance >= confirmListing.getTotalPrice()) {
                HdvPacketHandler.buyItem(confirmListing.getId());
                confirmListing = null;
            }
            return;
        }

        if (ok(hbClose) && in(mx, my, hbClose)) { mc.displayGuiScreen(null); return; }
        if (!in(mx, my, px, py, pw, ph))         { mc.displayGuiScreen(null); return; }

        if (ok(hbTabBuy)  && in(mx, my, hbTabBuy)  && activeTab != TAB_BUY)  { activeTab = TAB_BUY; requestList(); return; }
        if (ok(hbTabMine) && in(mx, my, hbTabMine) && activeTab != TAB_MINE) {
            activeTab = TAB_MINE; loadingMine = true; scrollMine = 0;
            HdvPacketHandler.requestMyListings(); return;
        }
        if (ok(hbTabSell) && in(mx, my, hbTabSell) && activeTab != TAB_SELL) {
            activeTab = TAB_SELL; sellItem = null; sellSlot = -1; sellQty = 1; sellPrice = 100;
            if (priceField != null) { priceField.setText("100"); priceField.setFocused(false); }
            return;
        }

        switch (activeTab) {

            case TAB_BUY:
                if (searchField != null) searchField.mouseClicked(mx, my, btn);
                if (ok(hbSearchBtn) && in(mx, my, hbSearchBtn)) { requestList(); return; }
                if (ok(hbScrollUp) && in(mx, my, hbScrollUp) && scroll > 0) { scroll--; return; }
                if (ok(hbScrollDn) && in(mx, my, hbScrollDn)) {
                    List<HdvListing> fl2 = getFiltered();
                    if (scroll < fl2.size() - Math.max(1, listH / ROW_H)) { scroll++; return; }
                }
                List<HdvListing> fl = getFiltered();
                int mr = Math.max(1, listH / ROW_H);
                for (int i = scroll; i < Math.min(scroll + mr, fl.size()); i++) {
                    int idx = i - scroll;
                    if (idx < hbRows.length && ok(hbRows[idx]) && in(mx, my, hbRows[idx])) {
                        confirmListing = fl.get(i); return;
                    }
                }
                break;

            case TAB_MINE:
                if (ok(hbScrollMineUp) && in(mx, my, hbScrollMineUp) && scrollMine > 0) { scrollMine--; return; }
                if (ok(hbScrollMineDn) && in(mx, my, hbScrollMineDn)) {
                    if (scrollMine < myListings.size() - Math.max(1, listMineH / ROW_H)) { scrollMine++; return; }
                }
                // Bouton global collecter
                if (ok(hbCollect) && in(mx, my, hbCollect) && pendingEarnings > 0) {
                    // Purge immédiate côté client
                    myListings.removeIf(HdvListing::isSold);
                    pendingEarnings = 0L;
                    loadingMine = true;
                    if (scrollMine >= myListings.size()) scrollMine = Math.max(0, myListings.size() - 1);
                    // Envoyer au serveur (la réponse myListings arrivera et confirmera)
                    pendingAction = PendingAction.COLLECT;
                    HdvPacketHandler.collectEarnings();
                    HdvPacketHandler.requestMyListings();
                    return;
                }
                // Boutons par ligne
                int cancelIdx = 0;
                int mrMine = Math.max(1, listMineH / ROW_H);
                int endMine = Math.min(scrollMine + mrMine, myListings.size());
                for (int i = scrollMine; i < endMine; i++) {
                    HdvListing l = myListings.get(i);
                    if (cancelIdx >= hbCancelRow.length) break;
                    boolean hasBtnRow = !l.isSold() || pendingEarnings > 0;
                    if (hasBtnRow) {
                        if (ok(hbCancelRow[cancelIdx]) && in(mx, my, hbCancelRow[cancelIdx])) {
                            if (l.isSold()) {
                                // Collecte via bouton par ligne
                                myListings.removeIf(HdvListing::isSold);
                                pendingEarnings = 0L;
                                loadingMine = true;
                                if (scrollMine >= myListings.size()) scrollMine = Math.max(0, myListings.size() - 1);
                                pendingAction = PendingAction.COLLECT;
                                HdvPacketHandler.collectEarnings();
                                HdvPacketHandler.requestMyListings();
                            } else {
                                // Retrait immédiat de l'annonce
                                int lid = l.getId();
                                myListings.removeIf(listing -> listing.getId() == lid);
                                if (scrollMine > 0 && scrollMine >= myListings.size()) scrollMine--;
                                loadingMine = true;
                                pendingAction = PendingAction.CANCEL;
                                HdvPacketHandler.cancelOffer(lid);
                                HdvPacketHandler.requestMyListings();
                                status(true, "Annonce retiree — item restitue.");
                            }
                            return;
                        }
                        cancelIdx++;
                    }
                }
                break;

            case TAB_SELL:
                for (int i = 0; i < 36; i++) {
                    if (ok(hbInvSlots[i]) && in(mx, my, hbInvSlots[i])) {
                        ItemStack st = (i < mc.thePlayer.inventory.mainInventory.length)
                                ? mc.thePlayer.inventory.mainInventory[i] : null;
                        if (st != null) {
                            if (sellSlot == i) { sellSlot = -1; sellItem = null; sellQty = 1; }
                            else { sellSlot = i; sellItem = st.copy(); sellItem.stackSize = 1; sellQty = 1; }
                        }
                        return;
                    }
                }
                if (ok(hbQtyM10) && in(mx, my, hbQtyM10)) { sellQty = Math.max(1, sellQty - 10); return; }
                if (ok(hbQtyM1)  && in(mx, my, hbQtyM1))  { sellQty = Math.max(1, sellQty - 1);  return; }
                if (ok(hbQtyP1)  && in(mx, my, hbQtyP1))  { int max = sellItem != null ? countInInv(sellItem, mc.thePlayer.inventory) : 64; sellQty = Math.min(max, sellQty + 1);  return; }
                if (ok(hbQtyP10) && in(mx, my, hbQtyP10)) { int max = sellItem != null ? countInInv(sellItem, mc.thePlayer.inventory) : 64; sellQty = Math.min(max, sellQty + 10); return; }
                if (ok(hbPrM1000) && in(mx, my, hbPrM1000)) { sellPrice = Math.max(1, sellPrice - 1000); syncPr(); return; }
                if (ok(hbPrM100)  && in(mx, my, hbPrM100))  { sellPrice = Math.max(1, sellPrice - 100);  syncPr(); return; }
                if (ok(hbPrM10)   && in(mx, my, hbPrM10))   { sellPrice = Math.max(1, sellPrice - 10);   syncPr(); return; }
                if (ok(hbPrP10)   && in(mx, my, hbPrP10))   { sellPrice = Math.min(999_999_999L, sellPrice + 10);   syncPr(); return; }
                if (ok(hbPrP100)  && in(mx, my, hbPrP100))  { sellPrice = Math.min(999_999_999L, sellPrice + 100);  syncPr(); return; }
                if (ok(hbPrP1000) && in(mx, my, hbPrP1000)) { sellPrice = Math.min(999_999_999L, sellPrice + 1000); syncPr(); return; }
                if (ok(hbPrFld) && in(mx, my, hbPrFld) && priceField != null) { priceField.setFocused(true); priceField.mouseClicked(mx, my, btn); return; }
                if (qtyField != null) {
                    boolean inQty = in(mx, my, qtyField.xPosition, qtyField.yPosition, qtyField.width, 12);
                    if (inQty) { qtyField.setFocused(true); qtyField.mouseClicked(mx, my, btn); return; }
                    else qtyField.setFocused(false);
                }
                if (ok(hbSellBtn) && in(mx, my, hbSellBtn)) { doSell(); return; }
                break;
        }
    }

    private void doSell() {
        if (sellItem == null) { status(false, "Selectionnez d'abord un item."); return; }
        if (priceField != null && priceField.isFocused()) {
            long v = parseLong(priceField.getText()); if (v > 0) sellPrice = v;
            priceField.setFocused(false);
        }
        if (qtyField != null && qtyField.isFocused()) {
            int v = (int) parseLong(qtyField.getText()); if (v > 0) sellQty = v;
            qtyField.setFocused(false);
        }
        if (sellPrice <= 0) { status(false, "Le prix doit etre superieur a 0."); return; }
        int avail = countInInv(sellItem, mc.thePlayer.inventory);
        if (sellQty <= 0 || sellQty > avail) { status(false, "Quantite invalide (dispo : " + avail + ")"); return; }
        HdvPacketHandler.postOffer(sellItem, sellPrice, sellQty);
        status(true, "Annonce envoyee avec succes !");
        sellItem = null; sellSlot = -1; sellQty = 1; sellPrice = 100; syncPr();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dw = org.lwjgl.input.Mouse.getEventDWheel();
        if (dw == 0) return;
        if (activeTab == TAB_BUY && confirmListing == null) {
            List<HdvListing> fl = getFiltered();
            int mr = Math.max(1, listH / ROW_H);
            if (dw > 0 && scroll > 0) scroll--;
            else if (dw < 0 && scroll < Math.max(0, fl.size() - mr)) scroll++;
        }
        if (activeTab == TAB_MINE) {
            int mr = Math.max(1, listMineH / ROW_H);
            if (dw > 0 && scrollMine > 0) scrollMine--;
            else if (dw < 0 && scrollMine < Math.max(0, myListings.size() - mr)) scrollMine++;
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) {
            if (confirmListing != null) { confirmListing = null; return; }
            mc.displayGuiScreen(null);
            return;
        }
        if (activeTab == TAB_BUY && searchField != null) {
            searchField.textboxKeyTyped(c, key);
            if (key == 28) requestList();
        }
        if (activeTab == TAB_SELL) {
            if (priceField != null && priceField.isFocused()) {
                priceField.textboxKeyTyped(c, key);
                long v = parseLong(priceField.getText()); if (v > 0) sellPrice = v;
            }
            if (qtyField != null && qtyField.isFocused()) {
                qtyField.textboxKeyTyped(c, key);
                long parsed = parseLong(qtyField.getText());
                int max = sellItem != null ? countInInv(sellItem, mc.thePlayer.inventory) : 64;
                if (parsed > max) { parsed = max; qtyField.setText(String.valueOf(parsed)); }
                int v = (int) parsed; if (v > 0) sellQty = v;
            }
        }
    }

    @Override
    public void updateScreen() {
        if (searchField != null) searchField.updateCursorCounter();
        if (priceField  != null) priceField.updateCursorCounter();
        if (qtyField    != null) qtyField.updateCursorCounter();
        if (activeTab == TAB_SELL && sellSlot >= 0 && mc.thePlayer != null) {
            ItemStack cur = (sellSlot < mc.thePlayer.inventory.mainInventory.length)
                    ? mc.thePlayer.inventory.mainInventory[sellSlot] : null;
            if (cur == null || (sellItem != null && cur.getItem() != sellItem.getItem())) {
                sellSlot = -1; sellItem = null; sellQty = 1;
            }
        }
        PlayerData pd = PlayerDataHandler.getCachedData();
        if (pd != null) playerBalance = pd.getBalance();
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════

    private List<HdvListing> getFiltered() {
        String f = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        if (f.isEmpty()) return new ArrayList<>(listings);
        List<HdvListing> r = new ArrayList<>();
        for (HdvListing l : listings) {
            String name = l.getItem() != null ? l.getItem().getDisplayName().toLowerCase() : "";
            String sel  = l.getSellerName() != null ? l.getSellerName().toLowerCase() : "";
            if (name.contains(f) || sel.contains(f)) r.add(l);
        }
        return r;
    }

    private int countInInv(ItemStack ref, InventoryPlayer inv) {
        if (ref == null || inv == null) return 0;
        int c = 0;
        for (ItemStack s : inv.mainInventory)
            if (s != null && s.getItem() == ref.getItem() && s.getItemDamage() == ref.getItemDamage()) c += s.stackSize;
        return c;
    }

    private void syncPr() { if (priceField != null) { priceField.setFocused(false); priceField.setText(String.valueOf(sellPrice)); } }
    private void status(boolean ok, String msg) { statusMsg = msg; statusTimer = 160; statusOk = ok; }

    private void hb(int[] h, int x, int y, int w, int wh) { h[0]=x; h[1]=y; h[2]=w; h[3]=wh; }
    private boolean ok(int[] h) { return h[2] > 0; }
    private boolean in(int mx, int my, int[] h) { return in(mx, my, h[0], h[1], h[2], h[3]); }
    private boolean in(int mx, int my, int x, int y, int w, int h) { return mx>=x && my>=y && mx<=x+w && my<=y+h; }

    private void drawBorder(int x, int y, int w, int h, int c) {
        drawRect(x,     y,     x+w,   y+1,   c);
        drawRect(x,     y+h-1, x+w,   y+h,   c);
        drawRect(x,     y,     x+1,   y+h,   c);
        drawRect(x+w-1, y,     x+w,   y+h,   c);
    }
    private void drawCentered(String s, int cx, int y, int col) {
        fontRendererObj.drawStringWithShadow(s, cx - fontRendererObj.getStringWidth(s) / 2f, y, col);
    }
    private void miniBtn(int mx, int my, int x, int y, int w, int h, String label, int bg, int bd, int tc, int[] hbT) {
        boolean hov = in(mx, my, x, y, w, h);
        drawRect(x, y, x+w, y+h, hov ? bg + 0x202020 : bg);
        drawRect(x, y, x+w, y+1, hov ? tc : bd);
        drawRect(x,     y, x+1,   y+h, bd);
        drawRect(x+w-1, y, x+w,   y+h, bd);
        drawRect(x,     y+h-1, x+w, y+h, bd);
        int lw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawString(label, x + (w - lw) / 2, y + (h - 8) / 2, hov ? tc : 0xFFAAAAAA);
        hb(hbT, x, y, w, h);
    }
    private String fmtGold(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fK", v / 1_000.0);
        return String.valueOf(v);
    }
    private String fmtTime(long s) {
        if (s >= 86400) return (s / 86400) + "j " + (s % 86400 / 3600) + "h";
        if (s >= 3600)  return (s / 3600) + "h " + (s % 3600 / 60) + "m";
        return (s / 60) + "m " + (s % 60) + "s";
    }
    private long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }
}

