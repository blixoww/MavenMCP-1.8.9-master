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

/**
 * Interface Hôtel des Ventes — refonte complète.
 *
 * Onglets : [Acheter] [Mes annonces] [Vendre]
 *
 * Protocole binaire S2C aligné avec HdvManager.serializeListing (serveur) :
 *   VarInt id | String sellerName | readItemStackFromBuffer | Long price | VarInt qty | Long expiresAt
 */
public class GuiHDV extends GuiScreen {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final int C_BG      = 0xF0050A10;
    private static final int C_BORDER  = 0xFF1A3A6A;
    private static final int C_ACCENT  = 0xFF2277EE;
    private static final int C_ACCENT2 = 0xFF55AAFF;
    private static final int C_HEADER  = 0xFF060D1A;
    private static final int C_GOLD    = 0xFFFFD700;
    private static final int C_GREEN   = 0xFF33CC77;
    private static final int C_RED     = 0xFFEE3333;
    private static final int C_GRAY    = 0xFF778899;
    private static final int C_WHITE   = 0xFFFFFFFF;
    private static final int C_SEL     = 0x441A4A2A;
    private static final int C_HOV     = 0x221A3060;

    // ── Onglets ───────────────────────────────────────────────────────────────
    private static final int TAB_BUY  = 0;
    private static final int TAB_MINE = 1;
    private static final int TAB_SELL = 2;
    private int activeTab = TAB_BUY;

    // ── Layout ────────────────────────────────────────────────────────────────
    private int px, py, pw, ph;

    // ── Données ───────────────────────────────────────────────────────────────
    private final List<HdvListing> listings = new ArrayList<>();
    private long    playerBalance = 0L;
    private boolean loading       = true;
    private String  statusMsg     = "";
    private int     statusTimer   = 0;
    private boolean statusOk      = true;

    // ── Achat ─────────────────────────────────────────────────────────────────
    private HdvListing confirmListing = null;

    // ── Vente ─────────────────────────────────────────────────────────────────
    private ItemStack    sellItem  = null;
    private int          sellSlot  = -1;
    private int          sellQty   = 1;
    private long         sellPrice = 100L;
    private GuiTextField priceField;
    private GuiTextField qtyField;

    // ── Recherche ──────��──────────────────────────────────────────────────────
    private GuiTextField searchField;

    // ── Scroll ────────────────────────────────────────────────────────────────
    private int scroll = 0;
    private static final int ROW_H = 32;
    private int listY0, listH;

    // ── Hitboxes [x, y, w, h] — w=0 invalide ─────────────────────────────────
    private final int[]   hbClose     = new int[4];
    private final int[]   hbTabBuy    = new int[4];
    private final int[]   hbTabMine   = new int[4];
    private final int[]   hbTabSell   = new int[4];
    private final int[]   hbSearchBtn = new int[4];
    private final int[]   hbCollect   = new int[4];
    private final int[]   hbSellBtn   = new int[4];
    private final int[]   hbQtyM10    = new int[4];
    private final int[]   hbQtyM1     = new int[4];
    private final int[]   hbQtyP1     = new int[4];
    private final int[]   hbQtyP10    = new int[4];
    private final int[]   hbPrM1000   = new int[4];
    private final int[]   hbPrM100    = new int[4];
    private final int[]   hbPrM10     = new int[4];
    private final int[]   hbPrP10     = new int[4];
    private final int[]   hbPrP100    = new int[4];
    private final int[]   hbPrP1000   = new int[4];
    private final int[]   hbPrFld     = new int[4];
    private final int[]   hbOkBuy     = new int[4];
    private final int[]   hbCancelBuy = new int[4];
    private final int[][] hbRows      = new int[16][4];
    private final int[][] hbCancelRow = new int[16][4];
    private final int[][] hbInvSlots  = new int[36][4];

    // ════════════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void initGui() {
        // S'assurer que le système de packets est initialisé et les canaux enregistrés
        net.minecraft.client.custompackets.CustomPacketSystem.init();

        pw = Math.min(740, width  - 16);
        ph = Math.min(460, height - 16);
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        searchField = new GuiTextField(0, fontRendererObj, px + 10, py + 34, pw - 90, 12);
        searchField.setMaxStringLength(48);

        priceField = new GuiTextField(1, fontRendererObj, 0, 0, 80, 12);
        priceField.setMaxStringLength(10);
        priceField.setText(String.valueOf(sellPrice));

        qtyField = new GuiTextField(2, fontRendererObj, 0, 0, 40, 12); // Champ quantité
        qtyField.setMaxStringLength(4);
        qtyField.setText(String.valueOf(sellQty));

        invalidateHb();

        // ── IMPORTANT : enregistrer les listeners AVANT requestList() ──────
        HdvPacketHandler.setListListener(nl -> {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                listings.clear();
                listings.addAll(nl);
                loading = false;
                scroll  = 0;
            });
        });
        HdvPacketHandler.setActionListener((ok, msg) -> {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                statusMsg   = msg;
                statusTimer = 120;
                statusOk    = ok;
                if (ok) { loading = true; HdvPacketHandler.requestList(0, ""); }
            });
        });
        PlayerDataHandler.setListener(data -> playerBalance = data.getBalance());
        PlayerData pd = PlayerDataHandler.getCachedData();
        if (pd != null) playerBalance = pd.getBalance();

        // Pré-charger le cache existant (annonces déjà reçues avant l'ouverture du GUI)
        List<HdvListing> cached = new ArrayList<>(HdvPacketHandler.getCachedListings());
        if (!cached.isEmpty()) {
            listings.clear();
            listings.addAll(cached);
            loading = false;
        }

        // Demander une mise à jour fraîche au serveur
        requestList();
    }

    private void requestList() {
        loading = true;
        scroll  = 0;
        String f = searchField != null ? searchField.getText().trim() : "";
        HdvPacketHandler.requestList(0, f);
    }

    /** Appelé directement par HdvPacketHandler quand des listings sont reçus. */
    public void onListingsReceived(java.util.List<HdvListing> nl) {
        listings.clear();
        listings.addAll(nl);
        loading = false;
        scroll  = 0;
    }

    private void invalidateHb() {
        hbClose[2] = hbTabBuy[2] = hbTabMine[2] = hbTabSell[2] = 0;
        hbSearchBtn[2] = hbCollect[2] = hbSellBtn[2] = 0;
        hbQtyM10[2] = hbQtyM1[2] = hbQtyP1[2] = hbQtyP10[2] = 0;
        hbPrM1000[2] = hbPrM100[2] = hbPrM10[2] = 0;
        hbPrP10[2]   = hbPrP100[2] = hbPrP1000[2] = 0;
        hbPrFld[2]   = hbOkBuy[2]  = hbCancelBuy[2] = 0;
        for (int[] h : hbRows)      h[2] = 0;
        for (int[] h : hbCancelRow) h[2] = 0;
        for (int[] h : hbInvSlots)  h[2] = 0;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DRAW PRINCIPAL
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0x99000000);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Panel
        drawRect(px, py, px + pw, py + ph, C_BG);
        drawBorder(px, py, pw, ph, C_BORDER);
        drawRect(px, py, px + pw, py + 1, C_ACCENT);
        drawRect(px, py + 1, px + pw, py + 2, 0x33AACCFF);

        drawHeader(mx, my);
        drawTabs(mx, my);

        switch (activeTab) {
            case TAB_BUY:  drawBuyTab(mx, my);  break;
            case TAB_MINE: drawMineTab(mx, my); break;
            case TAB_SELL: drawSellTab(mx, my); break;
        }

        // Message de statut
        if (statusTimer > 0) {
            statusTimer--;
            int alpha = Math.min(255, statusTimer * 6);
            int col = ((statusOk ? C_GREEN : C_RED) & 0x00FFFFFF) | (alpha << 24);
            int sw = fontRendererObj.getStringWidth(statusMsg);
            fontRendererObj.drawStringWithShadow(statusMsg, px + (pw - sw) / 2f, py + ph - 14, col);
        }

        if (confirmListing != null) drawConfirmOverlay(mx, my);

        GlStateManager.disableBlend();
        super.drawScreen(mx, my, pt);
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private void drawHeader(int mx, int my) {
        drawRect(px, py, px + pw, py + 26, C_HEADER);
        drawRect(px, py + 25, px + pw, py + 26, C_ACCENT);
        fontRendererObj.drawStringWithShadow("§b✦ §fHôtel des Ventes", px + 10, py + 8, C_WHITE);

        String bal = "§6" + fmtGold(playerBalance) + " §e✦";
        fontRendererObj.drawStringWithShadow(bal, px + pw - fontRendererObj.getStringWidth(bal) - 32, py + 9, C_WHITE);

        int cx = px + pw - 18, cy = py + 6;
        boolean ch = in(mx, my, cx, cy, 14, 14);
        drawRect(cx, cy, cx + 14, cy + 14, ch ? 0xCC882211 : 0x88441100);
        drawRect(cx, cy, cx + 14, cy + 1, ch ? C_RED : 0xFF882222);
        fontRendererObj.drawString("x", cx + 4, cy + 3, ch ? C_WHITE : 0xFFCC6655);
        hb(hbClose, cx, cy, 14, 14);
    }

    // ── Onglets ──────────────────────────────────────────────────────────────

    private void drawTabs(int mx, int my) {
        String[] labels = {"Acheter", "Mes annonces", "Vendre"};
        int[][] hbs = {hbTabBuy, hbTabMine, hbTabSell};
        int tw = 130, th = 18, ty = py + 28, startX = px + 8;

        for (int i = 0; i < 3; i++) {
            int tx = startX + i * (tw + 4);
            boolean active = (activeTab == i), hov = in(mx, my, tx, ty, tw, th);
            int bg  = active ? C_ACCENT : (hov ? 0xFF1A2A50 : 0xFF0A1225);
            int top = active ? C_ACCENT2 : (hov ? C_ACCENT : C_BORDER);
            int tc  = active ? C_WHITE : (hov ? 0xFFCCDDFF : C_GRAY);
            drawRect(tx, ty, tx + tw, ty + th, bg);
            drawRect(tx, ty, tx + tw, ty + 1, top);
            if (active) drawRect(tx, ty + th - 1, tx + tw, ty + th, C_BG);
            drawCentered(labels[i], tx + tw / 2, ty + 5, tc);
            hb(hbs[i], tx, ty, tw, th);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ONGLET ACHETER
    // ════════════════════════════════════════════════════════════════════════

    private void drawBuyTab(int mx, int my) {
        int cy = py + 48;

        // Barre de recherche
        int sfW = pw - 90;
        drawRect(px + 8, cy - 1, px + 8 + sfW + 2, cy + 13, 0xFF060D1A);
        drawBorder(px + 8, cy - 1, sfW + 2, 14,
                searchField != null && searchField.isFocused() ? C_ACCENT : C_BORDER);
        if (searchField != null) {
            searchField.xPosition = px + 10;
            searchField.yPosition = cy + 1;
            searchField.width     = sfW - 4;
            searchField.drawTextBox();
            if (searchField.getText().isEmpty())
                fontRendererObj.drawString("Rechercher...", px + 12, cy + 2, 0x44AAAAAA);
        }

        int bsx = px + 8 + sfW + 4, bsy = cy - 1;
        boolean bh = in(mx, my, bsx, bsy, 70, 14);
        drawRect(bsx, bsy, bsx + 70, bsy + 14, bh ? 0xFF1A3A6A : 0xFF0D2040);
        drawRect(bsx, bsy, bsx + 70, bsy + 1, bh ? C_ACCENT2 : C_BORDER);
        drawCentered("Chercher", bsx + 35, bsy + 3, bh ? C_WHITE : 0xFFAABBDD);
        hb(hbSearchBtn, bsx, bsy, 70, 14);
        cy += 16;

        // En-tête colonnes
        int lx = px + 8, tw = pw - 16;
        drawRect(lx, cy, lx + tw, cy + 14, 0xFF060D1A);
        drawRect(lx, cy + 13, lx + tw, cy + 14, 0x44FFFFFF);
        fontRendererObj.drawString("Item",       lx + 28,  cy + 3, C_GRAY);
        fontRendererObj.drawString("Vendeur",    lx + 220, cy + 3, C_GRAY);
        fontRendererObj.drawString("Qte",        lx + 340, cy + 3, C_GRAY);
        // Swap Price and Total in buy tab header to reflect the user's preference for 'Global' price focus
        fontRendererObj.drawString("Prix Total", lx + 400, cy + 3, C_GRAY);
        fontRendererObj.drawString("Unitaire",   lx + 490, cy + 3, C_GRAY);
        cy += 14;

        listY0 = cy;
        listH  = ph - (cy - py) - 22;
        int maxRows = Math.max(1, listH / ROW_H);

        if (loading) {
            int dots = (int) ((System.currentTimeMillis() / 500) % 4);
            StringBuilder sb = new StringBuilder("Chargement");
            for (int d = 0; d < dots; d++) sb.append('.');
            drawCentered(sb.toString(), px + pw / 2, listY0 + listH / 2 - 4, C_GRAY);
            return;
        }

        List<HdvListing> fl = getFiltered();
        if (scroll > Math.max(0, fl.size() - maxRows)) scroll = Math.max(0, fl.size() - maxRows);

        if (fl.isEmpty()) {
            drawCentered("Aucune annonce disponible.", px + pw / 2, listY0 + listH / 2 - 4, C_GRAY);
            for (int[] h : hbRows) h[2] = 0;
        } else {
            int y = listY0, end = Math.min(scroll + maxRows, fl.size());
            for (int i = scroll; i < end; i++) {
                drawBuyRow(mx, my, lx, y, tw, fl.get(i), i - scroll);
                y += ROW_H;
            }
            for (int i = end - scroll; i < hbRows.length; i++) hbRows[i][2] = 0;
        }

        // Flèches scroll
        if (!fl.isEmpty() && fl.size() > maxRows) {
            int ax = px + pw - 24;
            boolean canUp = scroll > 0, canDn = scroll < fl.size() - maxRows;
            drawRect(ax, listY0,           ax + 16, listY0 + 14,       canUp ? 0xFF1A2A50 : 0xFF080C14);
            fontRendererObj.drawString("^", ax + 5, listY0 + 3,         canUp ? 0xFFAADDFF : 0xFF333355);
            drawRect(ax, listY0 + listH - 14, ax + 16, listY0 + listH, canDn ? 0xFF1A2A50 : 0xFF080C14);
            fontRendererObj.drawString("v", ax + 5, listY0 + listH - 11, canDn ? 0xFFAADDFF : 0xFF333355);
        }

        fontRendererObj.drawString(fl.size() + " annonce(s)", px + 10, py + ph - 14, C_GRAY);
    }

    private void drawBuyRow(int mx, int my, int x, int y, int w, HdvListing l, int idx) {
        boolean hov = in(mx, my, x, y, w, ROW_H);
        boolean sel = confirmListing != null && l.getId() == confirmListing.getId();
        drawRect(x, y, x + w, y + ROW_H, sel ? C_SEL : (hov ? C_HOV : 0));
        drawRect(x, y + ROW_H - 1, x + w, y + ROW_H, 0x22FFFFFF);

        ItemStack item = l.getItem();
        if (item != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(item, x + 6, y + (ROW_H - 16) / 2);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            String name = item.getDisplayName();
            if (fontRendererObj.getStringWidth(name) > 148) name = name.substring(0, 14) + "...";
            // Centrage vertical du nom par rapport à l'icône : (ROW_H - 8) / 2, comme dans l'onglet "Mes annonces"
            fontRendererObj.drawStringWithShadow(name, x + 26, y + (float) (ROW_H - 8) / 2, C_WHITE);
        } else {
            fontRendererObj.drawString("Item invalide", x + 6, y + (ROW_H - 8) / 2, C_RED);
        }

        fontRendererObj.drawString(l.getSellerName(), x + 222, y + (ROW_H - 8) / 2, 0xFFAADDFF);
        fontRendererObj.drawString("" + l.getQuantity(), x + 342, y + (ROW_H - 8) / 2, C_WHITE);

        // Affichage inversé : Prix Total en premier (mis en valeur)
        // Note: l.getTotalPrice() retourne maintenant le champ brut reçu du serveur qui EST le prix total.
        fontRendererObj.drawStringWithShadow(fmtGold(l.getTotalPrice()), x + 400, y + (float) (ROW_H - 8) / 2, C_GOLD);
        fontRendererObj.drawString(fmtGold(l.getPricePerUnit()), x + 490, y + (ROW_H - 8) / 2, C_GRAY);

        boolean canAfford = playerBalance >= l.getTotalPrice(); // Check affordability based on total price logic if purchasing whole lot
        // NOTE: Server logic likely checks unitPrice * qty or totalPrice. Assuming standard behavior is buy whole lot.

        int bx = x + w - 68, by = y + (ROW_H - 14) / 2;
        boolean bh = in(mx, my, bx, by, 62, 14);
        drawRect(bx, by, bx + 62, by + 14, bh && canAfford ? 0xFF1A6030 : (canAfford ? 0xFF0D3515 : 0xFF2A1010));
        drawRect(bx, by, bx + 62, by + 1, bh && canAfford ? C_GREEN : (canAfford ? 0xFF1A6030 : C_RED));
        drawCentered(canAfford ? "Acheter" : "Insuf.", bx + 31, by + 3, canAfford ? (bh ? C_WHITE : 0xFF88EE99) : 0xFFCC5555);
        if (idx < hbRows.length) hb(hbRows[idx], x, y, w, ROW_H);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ONGLET MES ANNONCES
    // ════════════════════════════════════════════════════════════════════════

    private void drawMineTab(int mx, int my) {
        String myName = mc.thePlayer != null ? mc.thePlayer.getName() : "";
        List<HdvListing> mine = new ArrayList<>();
        for (HdvListing l : listings) if (myName.equals(l.getSellerName())) mine.add(l);

        int y = py + 50, lx = px + 8, tw = pw - 16;
        drawRect(lx, y, lx + tw, y + 14, 0xFF060D1A);
        drawRect(lx, y + 13, lx + tw, y + 14, 0x44FFFFFF);
        fontRendererObj.drawString("Item",          lx + 28, y + 3, C_GRAY);
        fontRendererObj.drawString("Qte restante",  lx + 250, y + 3, C_GRAY);
        fontRendererObj.drawString("Prix/u",         lx + 370, y + 3, C_GRAY);
        fontRendererObj.drawString("Expire",         lx + 470, y + 3, C_GRAY);
        y += 14;

        if (mine.isEmpty()) {
            drawCentered("Vous n'avez aucune annonce active.", px + pw / 2, py + ph / 2 - 10, C_GRAY);
        } else {
            for (int i = 0; i < mine.size() && i < hbCancelRow.length; i++) {
                HdvListing l = mine.get(i);
                boolean hov = in(mx, my, lx, y, tw, ROW_H);
                drawRect(lx, y, lx + tw, y + ROW_H, hov ? C_HOV : 0);
                drawRect(lx, y + ROW_H - 1, lx + tw, y + ROW_H, 0x22FFFFFF);

                if (l.getItem() != null) {
                    GlStateManager.enableDepth();
                    RenderHelper.enableGUIStandardItemLighting();
                    itemRender.renderItemAndEffectIntoGUI(l.getItem(), lx + 6, y + (ROW_H - 16) / 2);
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.disableDepth();
                    fontRendererObj.drawStringWithShadow(l.getItem().getDisplayName(),
                            lx + 26, y + (ROW_H - 8) / 2, C_WHITE);
                }
                fontRendererObj.drawString("" + l.getQuantity(), lx + 258, y + (ROW_H - 8) / 2, C_WHITE);
                fontRendererObj.drawStringWithShadow(fmtGold(l.getPricePerUnit()), lx + 370, y + (ROW_H - 8) / 2, C_GOLD);

                long rem = l.getExpiresAt() - System.currentTimeMillis() / 1000L;
                fontRendererObj.drawString(rem > 0 ? fmtTime(rem) : "Expiree", lx + 470, y + (ROW_H - 8) / 2, rem > 0 ? C_WHITE : C_RED);

                int bx = lx + tw - 68, by = y + (ROW_H - 14) / 2;
                boolean bh = in(mx, my, bx, by, 62, 14);
                drawRect(bx, by, bx + 62, by + 14, bh ? 0xCC441111 : 0x88220A0A);
                drawRect(bx, by, bx + 62, by + 1, bh ? C_RED : 0xFF661111);
                drawCentered("Retirer", bx + 31, by + 3, bh ? C_WHITE : 0xFFCC5555);
                hb(hbCancelRow[i], bx, by, 62, 14);
                y += ROW_H;
            }
        }

        // Bouton collecter
        int gx = px + pw / 2 - 90, gy = py + ph - 38;
        boolean gh = in(mx, my, gx, gy, 180, 22);
        drawRect(gx, gy, gx + 180, gy + 22, gh ? 0xFF1A4A25 : 0xFF0D2A15);
        drawBorder(gx, gy, 180, 22, gh ? C_GREEN : 0xFF1A6030);
        drawCentered("Collecter mes gains", gx + 90, gy + 7, gh ? C_WHITE : C_GREEN);
        hb(hbCollect, gx, gy, 180, 22);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ONGLET VENDRE
    // ════════════════════════════════════════════════════════════════════════

    private void drawSellTab(int mx, int my) {
        int margin = 8, halfW = (pw - margin * 3) / 2;
        int leftX  = px + margin, rightX = leftX + halfW + margin;
        int topY   = py + 50,    botH   = ph - 58;

        // ── Panneau gauche : inventaire ──────────────────────────────────────
        drawRect(leftX, topY, leftX + halfW, topY + botH, 0xCC080E1A);
        drawBorder(leftX, topY, halfW, botH, C_ACCENT);
        drawRect(leftX, topY, leftX + halfW, topY + 16, C_HEADER);
        drawRect(leftX, topY + 15, leftX + halfW, topY + 16, C_ACCENT);
        fontRendererObj.drawStringWithShadow("Inventaire", leftX + 6, topY + 4, C_ACCENT2);

        InventoryPlayer inv = mc.thePlayer.inventory;
        int sz = 18, gx = leftX + (halfW - 9 * sz) / 2, gy = topY + 20;

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawInvSlot(mx, my, gx + col * sz, gy + row * sz, sz, 9 + row * 9 + col, inv);

        int sepY = gy + 3 * sz + 3;
        drawRect(gx, sepY, gx + 9 * sz, sepY + 1, 0x44FFFFFF);
        for (int col = 0; col < 9; col++)
            drawInvSlot(mx, my, gx + col * sz, sepY + 4, sz, col, inv);

        // ── Panneau droit : configuration ────────────────────────────────────
        drawRect(rightX, topY, rightX + halfW, topY + botH, 0xCC080E12);
        drawBorder(rightX, topY, halfW, botH, 0xFF1A6030);
        drawRect(rightX, topY, rightX + halfW, topY + 16, C_HEADER);
        drawRect(rightX, topY + 15, rightX + halfW, topY + 16, 0xFF1A6030);
        fontRendererObj.drawStringWithShadow("Configuration vente", rightX + 6, topY + 4, C_GREEN);

        int ry = topY + 20;

        // Aperçu item sélectionné
        if (sellItem != null) {
            int pvX = rightX + halfW / 2 - 8;
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(sellItem, pvX, ry);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            drawCentered(sellItem.getDisplayName(), rightX + halfW / 2, ry + 18, C_WHITE);
            drawCentered("En inv. : " + countInInv(sellItem, inv), rightX + halfW / 2, ry + 28, C_GRAY);
        } else {
            drawCentered("<-- Cliquez sur un item", rightX + halfW / 2, ry + 10, 0xFF666688);
        }
        ry += 42;

        // ── Quantité ─────────────────────────────────────────────────────────
        fontRendererObj.drawString("Quantite :", rightX + 6, ry, C_GRAY);
        ry += 12;
        int bw = 22, bh = 13, qTot = bw * 4 + 3 * 3 + 28 + 20; // +20 pour le champ

        // Modifier l'affichage pour inclure le champ quantité au lieu du texte statique
        int qx = rightX + (halfW - qTot) / 2;
        miniBtn(mx, my, qx,           ry, bw, bh, "-10", 0xFF2A0808, 0xFF991111, C_RED,   hbQtyM10);
        miniBtn(mx, my, qx + bw + 3,  ry, bw, bh, "-1",  0xFF2A0808, 0xFF991111, C_RED,   hbQtyM1);

        // Champ quantité
        if (qtyField != null) {
            qtyField.xPosition = qx + (bw + 3) * 2;
            qtyField.yPosition = ry;
            qtyField.width     = 40;
            if (!qtyField.isFocused()) qtyField.setText(String.valueOf(sellQty));
            drawRect(qtyField.xPosition - 1, qtyField.yPosition - 1, qtyField.xPosition + qtyField.width + 1, qtyField.yPosition + 13, 0xFF060D1A);
            drawBorder(qtyField.xPosition - 1, qtyField.yPosition - 1, qtyField.width + 2, 14, qtyField.isFocused() ? C_ACCENT : C_BORDER);
            qtyField.drawTextBox();
        }

        int startPlusBtns = qx + (bw + 3) * 2 + 40 + 3; // après le champ (+ marge)
        miniBtn(mx, my, startPlusBtns, ry, bw, bh, "+1",  0xFF081808, 0xFF119911, C_GREEN, hbQtyP1);
        miniBtn(mx, my, startPlusBtns + bw + 3, ry, bw, bh, "+10", 0xFF081808, 0xFF119911, C_GREEN, hbQtyP10);
        ry += bh + 12;

        // ── Prix ─────────────────────────────────────────────────────────────
        fontRendererObj.drawString("Prix du lot :", rightX + 6, ry, C_GRAY); // Renommé
        ry += 12;
        int pfW = halfW - 14;
        if (priceField != null) {
            priceField.xPosition = rightX + 6;
            priceField.yPosition = ry + 1;
            priceField.width     = pfW - 16;
            if (!priceField.isFocused()) priceField.setText(String.valueOf(sellPrice));
        }
        drawRect(rightX + 4, ry - 1, rightX + 4 + pfW, ry + 13, 0xFF060D1A);
        drawBorder(rightX + 4, ry - 1, pfW, 14, priceField != null && priceField.isFocused() ? C_ACCENT : C_BORDER);
        if (priceField != null) priceField.drawTextBox();
        fontRendererObj.drawString("$", rightX + pfW - 4, ry + 1, C_GOLD);
        hb(hbPrFld, rightX + 4, ry - 1, pfW, 14);
        ry += 16;

        int pb = 28, pbH = 12, prTot = pb * 6 + 5 * 2;
        int prx = rightX + (halfW - prTot) / 2;
        miniBtn(mx, my, prx,              ry, pb, pbH, "-1K", 0xFF2A0808, 0xFF991111, C_RED,   hbPrM1000); // 1000 -> 1K pour place
        miniBtn(mx, my, prx + (pb+2),     ry, pb, pbH, "-100",  0xFF2A0808, 0xFF991111, C_RED,   hbPrM100);
        miniBtn(mx, my, prx + (pb+2)*2,   ry, pb, pbH, "-10",   0xFF2A0808, 0xFF991111, C_RED,   hbPrM10);
        miniBtn(mx, my, prx + (pb+2)*3,   ry, pb, pbH, "+10",   0xFF081808, 0xFF119911, C_GREEN, hbPrP10);
        miniBtn(mx, my, prx + (pb+2)*4,   ry, pb, pbH, "+100",  0xFF081808, 0xFF119911, C_GREEN, hbPrP100);
        miniBtn(mx, my, prx + (pb+2)*5,   ry, pb, pbH, "+1K", 0xFF081808, 0xFF119911, C_GREEN, hbPrP1000);
        ry += pbH + 10;

        // Afficher Prix Unitaire calculé
        long unitPrice = sellQty > 0 ? (sellPrice / sellQty) : 0;
        drawCentered("Unitaire : " + fmtGold(unitPrice) + " $", rightX + halfW / 2, ry, C_GRAY); // Affichage unitaire informatif

        // ── Bouton mettre en vente ────────────────────────────────────────────
        boolean canSell = sellItem != null && sellPrice > 0 && sellQty > 0;
        int sbX = rightX + 8, sbY = topY + botH - 28, sbW = halfW - 16;
        boolean sbH = in(mx, my, sbX, sbY, sbW, 20);
        drawRect(sbX, sbY, sbX + sbW, sbY + 20, sbH && canSell ? 0xFF1A6030 : (canSell ? 0xFF0D3518 : 0xFF1A1A25));
        drawBorder(sbX, sbY, sbW, 20, sbH && canSell ? C_GREEN : (canSell ? 0xFF1A6030 : 0xFF333344));
        drawCentered(canSell ? "Mettre en vente" : (sellItem != null ? "Prix ou quantite invalide" : "Selectionnez un item"),
                sbX + sbW / 2, sbY + 6, canSell ? (sbH ? C_WHITE : C_GREEN) : C_GRAY);
        hb(hbSellBtn, sbX, sbY, sbW, 20);
    }

    private void drawInvSlot(int mx, int my, int sx, int sy, int sz, int idx, InventoryPlayer inv) {
        ItemStack stack = (idx >= 0 && idx < inv.mainInventory.length) ? inv.mainInventory[idx] : null;
        boolean sel = (sellSlot == idx), hov = stack != null && in(mx, my, sx, sy, sz - 1, sz - 1);
        drawRect(sx, sy, sx + sz - 1, sy + sz - 1, sel ? 0xFF1A3A20 : (hov ? 0xFF1A2840 : 0xFF080E18));
        drawRect(sx, sy, sx + sz - 1, sy, sel ? C_GREEN : (hov ? C_ACCENT : 0xFF223355));
        drawRect(sx, sy, sx, sy + sz - 1, sel ? C_GREEN : (hov ? C_ACCENT : 0xFF223355));
        if (sel) {
            drawRect(sx + sz - 2, sy, sx + sz - 1, sy + sz - 1, C_GREEN);
            drawRect(sx, sy + sz - 2, sx + sz - 1, sy + sz - 1, C_GREEN);
        }
        if (stack != null) {
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(stack, sx + 1, sy + 1);
            itemRender.renderItemOverlayIntoGUI(fontRendererObj, stack, sx + 1, sy + 1, null);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
        }
        if (idx >= 0 && idx < 36) hb(hbInvSlots[idx], sx, sy, sz - 1, sz - 1);
    }

    // ── Overlay confirmation achat ────────────────────────────────────────────

    private void drawConfirmOverlay(int mx, int my) {
        drawRect(0, 0, width, height, 0xBB000000);
        int cw = 320, ch = 160, cx = width / 2 - cw / 2, cy = height / 2 - ch / 2;
        drawRect(cx, cy, cx + cw, cy + ch, 0xF0050A10);
        drawBorder(cx, cy, cw, ch, C_ACCENT);
        drawRect(cx, cy, cx + cw, cy + 20, C_HEADER);
        drawRect(cx, cy + 19, cx + cw, cy + 20, C_ACCENT);
        drawCentered("Confirmer l'achat", cx + cw / 2, cy + 6, C_ACCENT2);

        ItemStack item = confirmListing.getItem();
        fontRendererObj.drawString("Item : " + (item != null ? item.getDisplayName() : "?"), cx + 10, cy + 28, C_WHITE);
        fontRendererObj.drawString("Vendeur : " + confirmListing.getSellerName(), cx + 10, cy + 40, 0xFFAADDFF);

        // Show Total Price prominently in confirmation
        fontRendererObj.drawStringWithShadow("Prix Total : " + fmtGold(confirmListing.getTotalPrice()) + " $", cx + 10, cy + 52, C_GOLD);
        fontRendererObj.drawString("Unitaire : " + fmtGold(confirmListing.getPricePerUnit()) + " $", cx + 200, cy + 52, C_GRAY);

        boolean canAfford = playerBalance >= confirmListing.getTotalPrice();
        fontRendererObj.drawString("Solde : " + fmtGold(playerBalance) + " $", cx + 10, cy + 64, canAfford ? C_GREEN : C_RED);

        int bw = 120, by = cy + ch - 32;
        boolean c1 = in(mx, my, cx + 10, by, bw, 20);
        drawRect(cx + 10, by, cx + 10 + bw, by + 20, c1 ? 0xCC441111 : 0x88220A0A);
        drawBorder(cx + 10, by, bw, 20, c1 ? C_RED : 0xFF661111);
        drawCentered("Annuler", cx + 10 + bw / 2, by + 6, c1 ? C_WHITE : 0xFFCC5555);
        hb(hbCancelBuy, cx + 10, by, bw, 20);

        int bx2 = cx + cw - 10 - bw;
        boolean c2 = in(mx, my, bx2, by, bw, 20);
        drawRect(bx2, by, bx2 + bw, by + 20, c2 && canAfford ? 0xFF1A6030 : (canAfford ? 0xFF0D3518 : 0xFF2A1010));
        drawBorder(bx2, by, bw, 20, c2 && canAfford ? C_GREEN : (canAfford ? 0xFF1A6030 : C_RED));
        drawCentered(canAfford ? "Acheter" : "Fonds insuf.", bx2 + bw / 2, by + 6, canAfford ? (c2 ? C_WHITE : C_GREEN) : C_RED);
        hb(hbOkBuy, bx2, by, bw, 20);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  EVENEMENTS
    // ════════════════════════════════════════════════════════════════════════

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        // Overlay achat
        if (confirmListing != null) {
            if (ok(hbCancelBuy) && in(mx, my, hbCancelBuy)) { confirmListing = null; return; }
            if (ok(hbOkBuy)     && in(mx, my, hbOkBuy) && playerBalance >= confirmListing.getTotalPrice()) {
                HdvPacketHandler.buyItem(confirmListing.getId());
                confirmListing = null;
            }
            return;
        }

        if (ok(hbClose) && in(mx, my, hbClose))     { mc.displayGuiScreen(null); return; }
        if (!in(mx, my, px, py, pw, ph))              { mc.displayGuiScreen(null); return; }

        if (ok(hbTabBuy)  && in(mx, my, hbTabBuy)  && activeTab != TAB_BUY)  { activeTab = TAB_BUY;  requestList(); return; }
        if (ok(hbTabMine) && in(mx, my, hbTabMine) && activeTab != TAB_MINE) { activeTab = TAB_MINE; requestList(); return; }
        if (ok(hbTabSell) && in(mx, my, hbTabSell) && activeTab != TAB_SELL) {
            activeTab = TAB_SELL; sellItem = null; sellSlot = -1; sellQty = 1; sellPrice = 100;
            if (priceField != null) { priceField.setText("100"); priceField.setFocused(false); } return;
        }

        switch (activeTab) {
            case TAB_BUY:
                if (searchField != null) searchField.mouseClicked(mx, my, btn);
                if (ok(hbSearchBtn) && in(mx, my, hbSearchBtn)) { requestList(); return; }
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
                if (ok(hbCollect) && in(mx, my, hbCollect)) { HdvPacketHandler.collectEarnings(); return; }
                String myName = mc.thePlayer != null ? mc.thePlayer.getName() : "";
                List<HdvListing> mine = new ArrayList<>();
                for (HdvListing l : listings) if (myName.equals(l.getSellerName())) mine.add(l);
                for (int i = 0; i < mine.size() && i < hbCancelRow.length; i++) {
                    if (ok(hbCancelRow[i]) && in(mx, my, hbCancelRow[i])) { HdvPacketHandler.cancelOffer(mine.get(i).getId()); return; }
                }
                break;

            case TAB_SELL:
                for (int i = 0; i < 36; i++) {
                    if (ok(hbInvSlots[i]) && in(mx, my, hbInvSlots[i])) {
                        ItemStack st = (i < mc.thePlayer.inventory.mainInventory.length) ? mc.thePlayer.inventory.mainInventory[i] : null;
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
                if (ok(hbPrFld)   && in(mx, my, hbPrFld) && priceField != null) { priceField.setFocused(true); priceField.mouseClicked(mx, my, btn); return; }

                // Gestion clic champ quantité
                if (qtyField != null) {
                   // Hitbox du champ quantité : (x, y, w, h) -> (qtyField.xPosition, qtyField.yPosition, qtyField.width, 12)
                   boolean inQty = in(mx, my, qtyField.xPosition, qtyField.yPosition, qtyField.width, 12);
                   if (inQty) { qtyField.setFocused(true); qtyField.mouseClicked(mx, my, btn); return; }
                   else { qtyField.setFocused(false); }
                }

                if (ok(hbSellBtn) && in(mx, my, hbSellBtn)) { doSell(); return; }
                break;
        }
    }

    private void doSell() {
        if (sellItem == null) { status(false, "Selectionnez d'abord un item."); return; }
        if (priceField != null && priceField.isFocused()) {
            long v = parseLong(priceField.getText()); if (v > 0) sellPrice = v; priceField.setFocused(false);
        }
        if (qtyField != null && qtyField.isFocused()) {
            int v = (int)parseLong(qtyField.getText()); if (v > 0) sellQty = v; qtyField.setFocused(false);
        }

        if (sellPrice <= 0) { status(false, "Le prix lot doit etre > 0."); return; }
        int avail = countInInv(sellItem, mc.thePlayer.inventory);
        if (sellQty <= 0 || sellQty > avail) { status(false, "Quantite invalide (dispo : " + avail + ")"); return; }

        // Logique GLOBAL : on envoie maintenant le PRIX TOTAL DIRECTEMENT.
        // HdvPacketHandler.postOffer attend (item, totalPrice, qty).
        HdvPacketHandler.postOffer(sellItem, sellPrice, sellQty);
        status(true, "Annonce envoyee !");
        sellItem = null; sellSlot = -1; sellQty = 1; sellPrice = 100; syncPr();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (activeTab == TAB_BUY && confirmListing == null) {
            int dw = org.lwjgl.input.Mouse.getEventDWheel();
            if (dw != 0) {
                List<HdvListing> fl = getFiltered();
                int mr = Math.max(1, listH / ROW_H);
                if (dw > 0 && scroll > 0) scroll--;
                else if (dw < 0 && scroll < Math.max(0, fl.size() - mr)) scroll++;
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) {
            if (confirmListing != null) { confirmListing = null; return; }
            mc.displayGuiScreen(null); return;
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
                // Clamp entre 0 et max
                int max = sellItem != null ? countInInv(sellItem, mc.thePlayer.inventory) : 64;
                if (parsed > max) {
                    parsed = max;
                    qtyField.setText(String.valueOf(parsed)); // Update text field immediately
                }
                int v = (int) parsed;
                if (v > 0) sellQty = v;
            }
        }
    }

    @Override
    public void updateScreen() {
        if (searchField != null) searchField.updateCursorCounter();
        if (priceField  != null) priceField.updateCursorCounter();
        if (qtyField    != null) qtyField.updateCursorCounter();
        if (activeTab == TAB_SELL && sellSlot >= 0 && mc.thePlayer != null) {
            ItemStack cur = (sellSlot < mc.thePlayer.inventory.mainInventory.length) ? mc.thePlayer.inventory.mainInventory[sellSlot] : null;
            if (cur == null || (sellItem != null && cur.getItem() != sellItem.getItem())) { sellSlot = -1; sellItem = null; sellQty = 1; }
        }
        PlayerData pd = PlayerDataHandler.getCachedData();
        if (pd != null) playerBalance = pd.getBalance();
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

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
    private void status(boolean ok, String msg) { statusMsg = msg; statusTimer = 120; statusOk = ok; }

    private void hb(int[] h, int x, int y, int w, int wh) { h[0]=x; h[1]=y; h[2]=w; h[3]=wh; }
    private boolean ok(int[] h) { return h[2] > 0; }
    private boolean in(int mx, int my, int[] h) { return in(mx, my, h[0], h[1], h[2], h[3]); }
    private boolean in(int mx, int my, int x, int y, int w, int h) { return mx>=x && my>=y && mx<=x+w && my<=y+h; }

    private void drawBorder(int x, int y, int w, int h, int c) {
        drawRect(x,y,x+w,y+1,c); drawRect(x,y+h-1,x+w,y+h,c);
        drawRect(x,y,x+1,y+h,c); drawRect(x+w-1,y,x+w,y+h,c);
    }
    private void drawCentered(String s, int cx, int y, int col) {
        fontRendererObj.drawStringWithShadow(s, cx - fontRendererObj.getStringWidth(s) / 2f, y, col);
    }
    private void miniBtn(int mx, int my, int x, int y, int w, int h, String label, int bg, int bd, int tc, int[] hbT) {
        boolean hov = in(mx, my, x, y, w, h);
        drawRect(x, y, x+w, y+h, hov ? bg+0x181818 : bg);
        drawRect(x, y, x+w, y+1, hov ? tc : bd);
        drawRect(x, y, x+1, y+h, bd); drawRect(x+w-1, y, x+w, y+h, bd); drawRect(x, y+h-1, x+w, y+h, bd);
        int lw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawString(label, x+(w-lw)/2, y+(h-8)/2, hov ? tc : 0xFFAAAAAA);
        hb(hbT, x, y, w, h);
    }
    private String fmtGold(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v/1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fK", v/1_000.0);
        return String.valueOf(v);
    }
    private String fmtTime(long s) {
        if (s >= 86400) return (s/86400)+"j "+(s%86400/3600)+"h";
        if (s >= 3600)  return (s/3600)+"h "+(s%3600/60)+"m";
        return (s/60)+"m "+(s%60)+"s";
    }
    private long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; } }
}
