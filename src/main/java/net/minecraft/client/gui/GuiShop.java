package net.minecraft.client.gui;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.custompackets.CustomPacketSystem;
import net.minecraft.client.custompackets.handler.PlayerDataHandler;
import net.minecraft.client.custompackets.handler.ShopPacketHandler;
import net.minecraft.client.custompackets.data.PlayerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiShop extends GuiScreen {

    // ─── Palette ─────────────────────────────────────────────────────────────
    private static final int C_BG = 0xF2030811;
    private static final int C_PANEL = 0xFF060C17;
    private static final int C_HEADER = 0xFF020A15;
    private static final int C_BORDER = 0xFF1A3A6A;
    private static final int C_BORDER2 = 0xFF0D2040;
    private static final int C_ACCENT = 0xFF2277EE;
    private static final int C_ACCENT2 = 0xFF55AAFF;
    private static final int C_GOLD = 0xFFFFD700;
    private static final int C_GREEN = 0xFF33CC77;
    private static final int C_RED = 0xFFEE4444;
    private static final int C_ORANGE = 0xFFFF8800;
    private static final int C_GRAY = 0xFF778899;
    private static final int C_LGRAY = 0xFFAABBCC;
    private static final int C_WHITE = 0xFFFFFFFF;
    private static final int C_FROZEN = 0xFF4488CC;

    private static final DecimalFormat FMT;

    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.FRANCE);
        s.setGroupingSeparator(' ');
        s.setDecimalSeparator(',');
        FMT = new DecimalFormat("#,##0.00", s);
    }

    // ─── Navigation ──────────────────────────────────────────────────────────
    private enum Screen {CATEGORIES, ITEMS, DETAIL}

    private Screen screen = Screen.CATEGORIES;

    private enum DetailTab {BUY, SELL}

    private DetailTab detailTab = DetailTab.BUY;

    // ─── Données ─────────────────────────────────────────────────────────────
    private final List<ShopPacketHandler.ShopCategory> categories = new ArrayList<>();
    private final List<ShopPacketHandler.ShopItem> items = new ArrayList<>();
    private final List<ShopPacketHandler.ShopItem> filtered = new ArrayList<>();
    private final List<ShopPacketHandler.MarketEntry> topBought = new ArrayList<>();
    private final List<ShopPacketHandler.MarketEntry> topSold = new ArrayList<>();
    private final List<ShopPacketHandler.ShopItem> allItemsCache = new ArrayList<>();

    private ShopPacketHandler.ShopItem selectedItem = null;
    private int selectedCatId = -1;
    private String selectedCatName = "";
    private boolean loadingCats = true;
    private boolean loadingItems = false;
    private long playerBalance = 0L;

    // ─── Recherche ───────────────────────────────────────────────────────────
    private String searchQuery = "";
    private boolean searchFocused = false;
    private boolean searchMode = false;

    // -── Saisie quantité ──────────────────────────────────────────────────────
    private boolean qtyInputFocused = false; // focus sur le champ de saisie
    private String qtyInputStr = "";    // texte en cours de saisie
    private boolean qtyInputIsBuy = true;  // pour quel panneau (achat ou vente)

    // ─── Status ──────────────────────────────────────────────────────────────
    private String statusMsg = "";
    private int statusTimer = 0;
    private boolean statusOk = true;

    // ─── Quantité ────────────────────────────────────────────────────────────
    private int buyQty = 1;
    private int sellQty = 1;

    // ─── Scroll ──────────────────────────────────────────────────────────────
    private int scroll = 0;
    private static final int ROW_H = 28;

    // ─── Scrollbar drag ──────────────────────────────────────────────────────
    private boolean draggingScroll = false;
    private int lastScrollX = 0, lastScrollY = 0, lastScrollW = 0, lastScrollH = 0;
    private int lastScrollTotal = 0, lastScrollVisible = 0;
    private int lastThumbY = 0, lastThumbH = 0;

    // ─── Layout ──────────────────────────────────────────────────────────────
    private int px, py, pw, ph;

    // ─── Tooltip ─────────────────────────────────────────────────────────────
    private List<String> tooltipLines = null;
    private int tooltipX, tooltipY;

    // ─── Hit-boxes ───────────────────────────────────────────────────────────
    private static final int MAX_CATS = 12;
    private static final int MAX_ROWS = 24;
    private final int[][] hbCats = new int[MAX_CATS][4];
    private final int[][] hbRows = new int[MAX_ROWS][4];
    private final int[] hbBack = new int[4];
    private final int[] hbClose = new int[4];
    private final int[] hbTabBuy = new int[4];
    private final int[] hbTabSell = new int[4];
    private final int[] hbAction = new int[4];
    private final int[] hbSellAll = new int[4];
    private final int[] hbQM10 = new int[4];
    private final int[] hbQM1 = new int[4];
    private final int[] hbQP1 = new int[4];
    private final int[] hbQP10 = new int[4];
    private final int[] hbQMax = new int[4];
    private final int[] hbSearchBox = new int[4];
    private final int[] hbScrollUp = new int[4];
    private final int[] hbScrollDn = new int[4];
    private final int[] hbQtyInput = new int[4];

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDU ITEM SAFE
    // ═════════════════════════════════════════════════════════════════════════

    private boolean renderIconSafe(ItemStack stack, int ix, int iy) {
        if (stack == null) return false;
        Tessellator tess = Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tess.getWorldRenderer();
        try {
            if (wr.isDrawing) tess.draw();
            GlStateManager.pushMatrix();
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.zLevel = 100.0F;
            itemRender.renderItemIntoGUI(stack, ix, iy);
            itemRender.zLevel = 0.0F;
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();
            if (wr.isDrawing) tess.draw();
            return true;
        } catch (Exception e) {
            try {
                RenderHelper.disableStandardItemLighting();
            } catch (Exception ignored) {
            }
            try {
                GlStateManager.disableDepth();
            } catch (Exception ignored) {
            }
            try {
                GlStateManager.popMatrix();
            } catch (Exception ignored) {
            }
            try {
                if (wr.isDrawing) tess.draw();
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  INIT
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void initGui() {
        try {
            CustomPacketSystem.init();
            pw = Math.min(820, width - 16);
            ph = Math.min(500, height - 16);
            px = (width - pw) / 2;
            py = (height - ph) / 2;
            clearHb();

            ShopPacketHandler.setCategoriesListener(cats ->
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        categories.clear();
                        categories.addAll(cats);
                        loadingCats = false;
                        if (!cats.isEmpty()) ShopPacketHandler.requestItems(-1);
                    }));

            ShopPacketHandler.setItemsListener(its ->
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        // Si c'est un détail (1 item avec historique) — mettre à jour selectedItem
                        if (its.size() == 1 && its.get(0).getBuyHistory().size() > 0 && selectedItem != null
                                && its.get(0).getId() == selectedItem.getId()) {
                            selectedItem = its.get(0);
                            // Ne pas écraser la liste complète, juste mettre à jour le cache
                            for (int i = 0; i < allItemsCache.size(); i++) {
                                if (allItemsCache.get(i).getId() == selectedItem.getId()) {
                                    allItemsCache.set(i, selectedItem);
                                    break;
                                }
                            }
                            for (int i = 0; i < items.size(); i++) {
                                if (items.get(i).getId() == selectedItem.getId()) {
                                    items.set(i, selectedItem);
                                    break;
                                }
                            }
                            for (int i = 0; i < filtered.size(); i++) {
                                if (filtered.get(i).getId() == selectedItem.getId()) {
                                    filtered.set(i, selectedItem);
                                    break;
                                }
                            }
                            return; // Ne pas appeler applyFilter — on reste sur le détail
                        }

                        // Sinon c'est une liste complète de catégorie
                        items.clear();
                        items.addAll(its);
                        for (ShopPacketHandler.ShopItem it : its) {
                            boolean found = false;
                            for (ShopPacketHandler.ShopItem c : allItemsCache)
                                if (c.getId() == it.getId()) {
                                    found = true;
                                    break;
                                }
                            if (!found) allItemsCache.add(it);
                        }
                        loadingItems = false;
                        scroll = 0;
                        applyFilter();
                    }));

            ShopPacketHandler.setTransactionListener((ok, msg, bal) ->
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        statusMsg = msg;
                        statusTimer = 220;
                        statusOk = ok;
                        playerBalance = bal;
                        if (ok && screen == Screen.DETAIL) {
                            // Rafraîchir la liste après transaction mais garder le détail ouvert
                            ShopPacketHandler.requestItems(selectedCatId);
                        }
                    }));

            ShopPacketHandler.setMarketStatsListener((bought, sold) ->
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        topBought.clear();
                        topBought.addAll(bought);
                        topSold.clear();
                        topSold.addAll(sold);
                    }));

            PlayerDataHandler.setListener(d -> playerBalance = d.getBalance());
            PlayerData pd = PlayerDataHandler.getCachedData();
            if (pd != null) playerBalance = pd.getBalance();

            List<ShopPacketHandler.ShopCategory> cc = ShopPacketHandler.getCachedCategories();
            if (!cc.isEmpty()) {
                categories.clear();
                categories.addAll(cc);
                loadingCats = false;
            }
            List<ShopPacketHandler.ShopItem> ci = ShopPacketHandler.getCachedItems();
            if (!ci.isEmpty()) {
                allItemsCache.clear();
                allItemsCache.addAll(ci);
            }
            topBought.addAll(ShopPacketHandler.getCachedTopBought());
            topSold.addAll(ShopPacketHandler.getCachedTopSold());

            ShopPacketHandler.requestCategories();
        } catch (Exception e) {
            System.out.println("[GuiShop] Exception initGui : " + e);
            e.printStackTrace();
        }
    }

    private void clearHb() {
        for (int[] h : hbCats) h[2] = 0;
        for (int[] h : hbRows) h[2] = 0;
        hbBack[2] = hbClose[2] = hbTabBuy[2] = hbTabSell[2] = 0;
        hbAction[2] = hbSellAll[2] = 0;
        hbQM10[2] = hbQM1[2] = hbQP1[2] = hbQP10[2] = hbQMax[2] = 0;
        hbSearchBox[2] = hbScrollUp[2] = hbScrollDn[2] = 0;
    }

    private void applyFilter() {
        filtered.clear();
        List<ShopPacketHandler.ShopItem> src = searchMode ? allItemsCache : items;
        String q = searchQuery.toLowerCase().trim();
        if (q.isEmpty()) {
            filtered.addAll(src);
            return;
        }
        for (ShopPacketHandler.ShopItem it : src)
            if (it.getDisplayName().toLowerCase().contains(q)
                    || it.getCategory().toLowerCase().contains(q))
                filtered.add(it);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDU PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xCC000814);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        drawRect(px + 3, py + 3, px + pw + 3, py + ph + 3, 0x66000000);
        drawRect(px + 2, py + 2, px + pw + 2, py + ph + 2, 0x44000000);
        drawRect(px, py, px + pw, py + ph, C_BG);
        drawRect(px, py, px + pw, py + 3, C_ACCENT);
        drawRect(px, py, px + 3, py + ph, C_BORDER);
        drawRect(px + pw - 3, py, px + pw, py + ph, C_BORDER);
        drawRect(px, py + ph - 2, px + pw, py + ph, C_BORDER2);

        tooltipLines = null;
        drawHeader(mx, my);
        drawSearchBar(mx, my);

        if (searchMode && !searchQuery.isEmpty()) {
            drawSearchResults(mx, my);
        } else {
            switch (screen) {
                case CATEGORIES:
                    drawCategories(mx, my);
                    break;
                case ITEMS:
                    drawItems(mx, my);
                    break;
                case DETAIL:
                    drawDetail(mx, my);
                    break;
            }
        }

        drawStatusBar();
        if (tooltipLines != null && !tooltipLines.isEmpty())
            renderTooltip(tooltipLines, tooltipX, tooltipY);
        GlStateManager.disableBlend();
        super.drawScreen(mx, my, pt);
    }

    // ─── Header ──────────────────────────────────────────────────────────────

    private void drawHeader(int mx, int my) {
        drawRect(px, py, px + pw, py + 28, C_HEADER);
        drawRect(px, py + 27, px + pw, py + 28, C_ACCENT);
        drawRect(px + 5, py + 6, px + 19, py + 22, 0xFF1A3A6A);
        drawRect(px + 6, py + 7, px + 18, py + 21, C_PANEL);
        drawRect(px + 8, py + 10, px + 16, py + 19, 0xFF2277EE);
        drawRect(px + 9, py + 11, px + 15, py + 18, 0xFF0D1E38);
        drawRect(px + 11, py + 13, px + 13, py + 16, 0xFF55AAFF);

        String title = "§b§lSHOP";
        if (screen == Screen.DETAIL && selectedItem != null) {
            title += " §8/ §7" + selectedItem.getDisplayName();
            // Ne pas afficher la recherche en mode détail
        } else {
            if (screen == Screen.ITEMS && !searchMode) title += " §8/ §7" + selectedCatName;
            if (searchMode && !searchQuery.isEmpty()) title += " §8/ §7Recherche : §f\"" + searchQuery + "\"";
        }
        fontRendererObj.drawStringWithShadow(title, px + 23, py + 10, C_WHITE);

        String bal = "§7Solde : §6" + fmtC(playerBalance) + " §e$";
        int bw = fontRendererObj.getStringWidth(bal);
        int bx2 = px + pw - bw - 50;
        drawRect(bx2 - 4, py + 5, bx2 + bw + 6, py + 22, 0x1AFFFFFF);
        drawRect(bx2 - 4, py + 5, bx2 + bw + 6, py + 6, 0x88FFCC00);
        fontRendererObj.drawStringWithShadow(bal, bx2, py + 9, C_WHITE);

        int cx = px + pw - 22, cy = py + 6;
        boolean ch = inR(mx, my, cx, cy, 16, 16);
        drawRect(cx, cy, cx + 16, cy + 16, ch ? 0xCCBB2222 : 0x77331100);
        drawRect(cx, cy, cx + 16, cy + 1, ch ? C_RED : 0xFF662211);
        drawRect(cx, cy, cx + 1, cy + 16, ch ? C_RED : 0xFF662211);
        fontRendererObj.drawString("§cx", cx + 5, cy + 4, ch ? C_WHITE : 0xFFAA6655);
        hb(hbClose, cx, cy, 16, 16);
    }

    // ─── Search bar ──────────────────────────────────────────────────────────

    private void drawSearchBar(int mx, int my) {
        int sbX = px + pw / 2 - 100, sbY = py + 32, sbW = 200, sbH = 14;
        boolean hov = inR(mx, my, sbX, sbY, sbW, sbH);
        int bc = searchFocused ? C_ACCENT : (hov ? C_BORDER : C_BORDER2);
        drawRect(sbX - 1, sbY - 1, sbX + sbW + 1, sbY + sbH + 1, bc);
        drawRect(sbX, sbY, sbX + sbW, sbY + sbH, searchFocused ? 0xFF040E1E : 0xFF030C18);
        fontRendererObj.drawString("§7⌕", sbX + 3, sbY + 3, C_GRAY);
        String display = searchQuery.isEmpty() && !searchFocused
                ? "§8Rechercher un item ou une catégorie..."
                : "§f" + searchQuery + (searchFocused ? "§7|" : "");
        fontRendererObj.drawString(display, sbX + 14, sbY + 3, C_WHITE);
        if (!searchQuery.isEmpty()) {
            boolean hC = inR(mx, my, sbX + sbW - 12, sbY + 2, 10, 10);
            fontRendererObj.drawString(hC ? "§c✖" : "§8✖", sbX + sbW - 12, sbY + 3, C_WHITE);
        }
        hb(hbSearchBox, sbX, sbY, sbW, sbH);
    }

    private void drawSearchResults(int mx, int my) {
        int y0 = py + 50;
        String qLabel = filtered.isEmpty()
                ? "§cAucun résultat pour §f\"" + searchQuery + "\""
                : "§7" + filtered.size() + " résultat(s) pour §f\"" + searchQuery + "\"";
        fontRendererObj.drawStringWithShadow(qLabel, px + 12, y0 + 2, C_WHITE);
        if (filtered.isEmpty()) return;
        drawItemTable(mx, my, px + 8, y0 + 14, pw - 16, ph - (y0 + 14 - py) - 8);
    }

    private void drawBack(int mx, int my, int x, int y) {
        boolean hov = inR(mx, my, x, y, 56, 14);
        drawRect(x, y, x + 56, y + 14, hov ? 0xFF0D2545 : 0xFF050C18);
        drawRect(x, y, x + 56, y + 1, hov ? C_ACCENT2 : C_BORDER2);
        drawRect(x, y, x + 1, y + 14, hov ? C_ACCENT2 : C_BORDER2);
        fontRendererObj.drawStringWithShadow("§7← Retour", x + 5, y + 3, hov ? C_WHITE : C_LGRAY);
        hb(hbBack, x, y, 56, 14);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ÉCRAN 1 — CATÉGORIES
    // ═════════════════════════════════════════════════════════════════════════

    private void drawCategories(int mx, int my) {
        int y0 = py + 50;
        if (loadingCats) {
            drawCtr("§7Chargement des catégories…", y0 + 80);
            return;
        }
        if (categories.isEmpty()) {
            drawCtr("§cAucune catégorie disponible.", y0 + 80);
            return;
        }

        fontRendererObj.drawStringWithShadow("§e§lCatégories", px + 14, y0 + 2, C_WHITE);
        GuiRenderUtils.drawGradientRect(px + 14, y0 + 13, px + pw - 14, y0 + 14, C_ACCENT, 0x00000000);

        int cols = 5, margin = 12, gap = 6;
        int btnW = (pw - 2 * margin - (cols - 1) * gap) / cols;
        int btnH = 62;
        int sx = px + margin, sy = y0 + 20;

        for (int i = 0; i < categories.size() && i < MAX_CATS; i++) {
            ShopPacketHandler.ShopCategory cat = categories.get(i);
            int col = i % cols, row = i / cols;
            int bx = sx + col * (btnW + gap);
            int by = sy + row * (btnH + gap);
            boolean hov = inR(mx, my, bx, by, btnW, btnH);

            drawRect(bx, by, bx + btnW, by + btnH, hov ? 0xFF0F2848 : 0xFF070F22);
            int catCol = catColor(cat.getName());
            drawRect(bx, by, bx + btnW, by + 3, hov ? catCol : blendColor(catCol, 0xFF000000, 0.5f));
            drawRect(bx, by, bx + 1, by + btnH, hov ? blendColor(catCol, 0xFF000000, 0.4f) : C_BORDER2);
            drawRect(bx + btnW - 1, by, bx + btnW, by + btnH, C_BORDER2);
            drawRect(bx, by + btnH - 1, bx + btnW, by + btnH, C_BORDER2);
            if (hov)
                GuiRenderUtils.drawGradientRect(bx + 1, by + 3, bx + btnW - 1, by + btnH - 1, 0x0FAACCFF, 0x00000000);

            ItemStack icon = resolveIcon(cat.getIconItem());
            boolean rendered = renderIconSafe(icon, bx + (btnW - 16) / 2, by + 8);
            if (!rendered) {
                drawRect(bx + btnW / 2 - 8, by + 8, bx + btnW / 2 + 8, by + 24, catCol & 0x88FFFFFF | 0x44000000);
                drawRect(bx + btnW / 2 - 7, by + 9, bx + btnW / 2 + 7, by + 23, catCol);
                String ini = cat.getName().substring(0, Math.min(2, cat.getName().length())).toUpperCase();
                fontRendererObj.drawString(ini, bx + btnW / 2 - fontRendererObj.getStringWidth(ini) / 2, by + 13, C_WHITE);
            }

            String nm = cat.getName();
            fontRendererObj.drawStringWithShadow(nm, bx + (btnW - fontRendererObj.getStringWidth(nm)) / 2, by + 36, hov ? C_WHITE : C_LGRAY);
            hb(hbCats[i], bx, by, btnW, btnH);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ÉCRAN 2 — LISTE ITEMS
    // ═════════════════════════════════════════════════════════════════════════

    private void drawItems(int mx, int my) {
        int topY = py + 50;
        drawBack(mx, my, px + 8, topY + 2);
        fontRendererObj.drawStringWithShadow(
                "§e§l" + selectedCatName + " §r§8— §7" + filtered.size() + " article(s)",
                px + 70, topY + 5, C_WHITE);
        drawItemTable(mx, my, px + 8, topY + 18, pw - 16, ph - (topY + 18 - py) - 8);
    }

    private void drawItemTable(int mx, int my, int tx, int ty, int tw, int th) {
        drawRect(tx, ty, tx + tw, ty + 16, 0xFF030A15);
        drawRect(tx, ty + 15, tx + tw, ty + 16, 0x66AACCFF);
        fontRendererObj.drawString("§8Item", tx + 36, ty + 4, 0xFF9AAABB);
        fontRendererObj.drawString("§8Achat", tx + tw - 260, ty + 4, 0xFF9AAABB);
        fontRendererObj.drawString("§8Vente", tx + tw - 180, ty + 4, 0xFF9AAABB);
        fontRendererObj.drawString("§8Évolution", tx + tw - 105, ty + 4, 0xFF9AAABB);
        fontRendererObj.drawString("§8Statut", tx + tw - 40, ty + 4, 0xFF9AAABB);

        int listTop = ty + 16;
        int listH = th - 16;
        int maxR = Math.max(1, listH / ROW_H);

        if (loadingItems) {
            drawCtr("§7Chargement…", listTop + 50);
            return;
        }
        if (filtered.isEmpty()) {
            drawCtr(searchMode ? "§cAucun résultat." : "§cAucun article.", listTop + 50);
            return;
        }

        int maxScroll = Math.max(0, filtered.size() - maxR);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = 0; i < maxR; i++) {
            int idx = i + scroll;
            if (idx >= filtered.size()) break;
            ShopPacketHandler.ShopItem it = filtered.get(idx);
            int ry = listTop + i * ROW_H;
            boolean hov = inR(mx, my, tx, ry, tw - 12, ROW_H - 1);

            drawRect(tx, ry, tx + tw - 12, ry + ROW_H - 1, (idx % 2 == 0) ? 0xFF050B17 : 0xFF060D1B);
            if (hov) drawRect(tx, ry, tx + tw - 12, ry + ROW_H - 1, 0x0D1A4070);
            if (hov) drawRect(tx, ry, tx + 2, ry + ROW_H - 1, catColor(it.getCategory()));

            ItemStack icon = resolveIcon(it.getMinecraftItem());
            boolean rendered = renderIconSafe(icon, tx + 4, ry + (ROW_H - 16) / 2);
            if (!rendered) {
                int fc = catColor(it.getCategory());
                drawRect(tx + 4, ry + (ROW_H - 14) / 2, tx + 20, ry + (ROW_H + 2) / 2, fc);
                String ini2 = it.getDisplayName().substring(0, Math.min(2, it.getDisplayName().length())).toUpperCase();
                fontRendererObj.drawString(ini2, tx + 5, ry + (ROW_H - 7) / 2, C_WHITE);
            }

            String nm = it.getDisplayName() + (it.isFrozen() ? " §b❅" : "");
            fontRendererObj.drawStringWithShadow("§f" + nm, tx + 26, ry + (ROW_H - 8) / 2, C_WHITE);
            fontRendererObj.drawString("§6" + fmtC(it.getBuyPrice()) + "$", tx + tw - 260, ry + (ROW_H - 8) / 2, C_GOLD);
            fontRendererObj.drawString("§a" + fmtC(it.getSellPrice()) + "$", tx + tw - 180, ry + (ROW_H - 8) / 2, C_GREEN);

            // Chart ASCII — filtrer les textes verbeux
            String chart = it.getAsciiChart();
            if (chart != null && !chart.isEmpty()) {
                if (chart.contains("stable") || chart.contains("━")
                        || chart.contains("Pas encore") || chart.contains("données")
                        || chart.equals("§7—")) {
                    chart = "§8—";
                } else {
                    // Tronquer en comptant uniquement les chars visibles
                    int visLen = 0, cutIdx = chart.length();
                    for (int ci = 0; ci < chart.length() - 1; ci++) {
                        if (chart.charAt(ci) == '§') {
                            ci++;
                            continue;
                        }
                        if (++visLen > 12) {
                            cutIdx = ci;
                            break;
                        }
                    }
                    chart = chart.substring(0, cutIdx);
                }
                fontRendererObj.drawString(chart, tx + tw - 105, ry + (ROW_H - 8) / 2, C_WHITE);
            }

            if (it.isFrozen())
                fontRendererObj.drawString("§b❅", tx + tw - 38, ry + (ROW_H - 8) / 2, C_FROZEN);
            else if (it.getCeil() > 0 || it.getFloor() > 0)
                fontRendererObj.drawString("§e↔", tx + tw - 38, ry + (ROW_H - 8) / 2, C_ORANGE);
            else
                fontRendererObj.drawString("§a✓", tx + tw - 38, ry + (ROW_H - 8) / 2, C_GREEN);

            hb(hbRows[i], tx, ry, tw - 12, ROW_H - 1);

            if (hov) {
                List<String> tip = new ArrayList<>();
                tip.add("§f" + it.getDisplayName() + (searchMode ? " §8(" + it.getCategory() + ")" : ""));
                tip.add("§7Achat : §6" + fmtC(it.getBuyPrice()) + " $");
                tip.add("§7Vente : §a" + fmtC(it.getSellPrice()) + " $");
                if (it.getFloor() > 0) tip.add("§7Plancher §c▼ : §f" + fmtC(it.getFloor()) + " $");
                if (it.getCeil() > 0) tip.add("§7Plafond §a▲ : §f" + fmtC(it.getCeil()) + " $");
                if (it.isFrozen()) tip.add("§b❅ Cours gelé par le staff");
                tip.add("§8Clic pour le détail");
                tooltipLines = tip;
                tooltipX = mx;
                tooltipY = my;
            }
        }

        drawScrollbar(tx + tw - 10, listTop, 8, listH, scroll, filtered.size(), maxR, mx, my);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ÉCRAN 3 — DÉTAIL
    //  Layout : panneau infos gauche | graphique + top-3 droite
    //  - Onglet ACHETER : graphique cours achat + top-3 achetés
    //  - Onglet VENDRE  : graphique cours vente + top-3 vendus
    // ═════════════════════════════════════════════════════════════════════════

    private void drawDetail(int mx, int my) {
        if (selectedItem == null) {
            screen = Screen.ITEMS;
            return;
        }
        int topY = py + 50;
        drawBack(mx, my, px + 8, topY + 2);

        int infoX = px + 8, infoW = 190, infoY = topY + 20;
        int infoH = ph - (infoY - py) - 8;
        int rightX = infoX + infoW + 6;
        int rightW = pw - infoW - 20;

        // Graphique prend ~60% de la hauteur droite, top-3 le reste
        int graphH = (int) (infoH * 0.58);
        int top3Y = infoY + graphH + 4;
        int top3H = (infoY + infoH) - top3Y;

        // ─── Panneau gauche ──────────────────────────────────────────────
        drawRect(infoX, infoY, infoX + infoW, infoY + infoH, C_PANEL);
        drawRect(infoX, infoY, infoX + infoW, infoY + 2, catColor(selectedItem.getCategory()));
        drawRect(infoX, infoY, infoX + 1, infoY + infoH, catColor(selectedItem.getCategory()));
        drawRect(infoX + infoW - 1, infoY, infoX + infoW, infoY + infoH, C_BORDER2);

        ItemStack bigIcon = resolveIcon(selectedItem.getMinecraftItem());
        boolean bigRendered = renderIconSafe(bigIcon, infoX + infoW / 2 - 8, infoY + 6);
        if (!bigRendered) {
            int fc = catColor(selectedItem.getCategory());
            drawRect(infoX + infoW / 2 - 14, infoY + 6, infoX + infoW / 2 + 14, infoY + 34, fc);
            String ini3 = selectedItem.getDisplayName().substring(0, Math.min(3, selectedItem.getDisplayName().length())).toUpperCase();
            fontRendererObj.drawString(ini3, infoX + infoW / 2 - fontRendererObj.getStringWidth(ini3) / 2, infoY + 16, C_WHITE);
        }

        int ty = infoY + 44;
        String nm = selectedItem.getDisplayName() + (selectedItem.isFrozen() ? " §b❅" : "");
        drawCtrAt("§f§l" + nm, infoX + infoW / 2, ty);
        ty += 12;

        // Badge catégorie (normalisé)
        String catBadge = normalizeCatName(selectedItem.getCategory());
        int cbw = fontRendererObj.getStringWidth(catBadge) + 8;
        int cbx = infoX + (infoW - cbw) / 2;
        drawRect(cbx, ty, cbx + cbw, ty + 10, catColor(selectedItem.getCategory()) & 0x33FFFFFF | 0x33000000);
        drawRect(cbx, ty, cbx + cbw, ty + 1, catColor(selectedItem.getCategory()));
        fontRendererObj.drawString("§8" + catBadge, cbx + 4, ty + 1, C_LGRAY);
        ty += 14;

        drawSep(infoX + 6, ty, infoW - 12);
        ty += 5;
        drawInfoRow(infoX, ty, "Achat :", "§6" + fmtC(selectedItem.getBuyPrice()) + "$", infoW);
        ty += 12;
        drawInfoRow(infoX, ty, "Vente :", "§a" + fmtC(selectedItem.getSellPrice()) + "$", infoW);
        ty += 12;
        drawSep(infoX + 6, ty, infoW - 12);
        ty += 5;
        if (selectedItem.getFloor() > 0) {
            drawInfoRow(infoX, ty, "Plancher :", "§c▼ " + fmtC(selectedItem.getFloor()) + "$", infoW);
            ty += 11;
        }
        if (selectedItem.getCeil() > 0) {
            drawInfoRow(infoX, ty, "Plafond :", "§a▲ " + fmtC(selectedItem.getCeil()) + "$", infoW);
            ty += 11;
        }
        if (selectedItem.isFrozen()) {
            drawCtrAt("§b❅ Cours gelé", infoX + infoW / 2, ty);
            ty += 11;
        }
        drawSep(infoX + 6, ty, infoW - 12);
        ty += 5;
        drawInfoRow(infoX, ty, "Vol. achat :", "§f" + selectedItem.getTotalBuyVolume(), infoW);
        ty += 11;
        drawInfoRow(infoX, ty, "Vol. vente :", "§f" + selectedItem.getTotalSellVolume(), infoW);
        ty += 11;
        drawSep(infoX + 6, ty, infoW - 12);
        ty += 5;
        drawInfoRow(infoX, ty, "Solde :", "§6" + fmtC(playerBalance) + "$", infoW);

        // ─── Onglets ─────────────────────────────────────────────────────
        int tabY = infoY + infoH - 102;
        int tabW = (infoW - 2) / 2;
        drawDetailTab(mx, my, infoX, tabY, tabW, "§a§l▶ ACHETER", detailTab == DetailTab.BUY, true, hbTabBuy);
        drawDetailTab(mx, my, infoX + tabW + 2, tabY, tabW, "§c§l▶ VENDRE", detailTab == DetailTab.SELL, false, hbTabSell);

        int actY = tabY + 18;
        drawRect(infoX, actY, infoX+infoW, infoY+infoH-2, 0xFF040C18);
        if (detailTab == DetailTab.BUY) drawBuyPanel(mx, my, infoX, actY, infoW);
        else drawSellPanel(mx, my, infoX, actY, infoW);

        // ─── Zone droite selon onglet ─────────────────────────────────────
        if (detailTab == DetailTab.BUY) {
            // Graphique cours d'achat
            drawChart(rightX, infoY, rightW, graphH, mx, my,
                    selectedItem.getBuyHistory(), "§6COURS ACHAT",
                    selectedItem.getBuyPrice(), C_GOLD, 0xFF33AA66,
                    selectedItem.getFloor(), selectedItem.getCeil());
            // Top achetés en bas
            if (top3H > 30) drawTop3Panel(rightX, top3Y, rightW, top3H, mx, my, true);
        } else {
            // Graphique cours de vente
            drawChart(rightX, infoY, rightW, graphH, mx, my,
                    selectedItem.getSellHistory(), "§aREVENTE",
                    selectedItem.getSellPrice(), C_GREEN, 0xFF2266AA, 0, 0);
            // Top vendus en bas
            if (top3H > 30) drawTop3Panel(rightX, top3Y, rightW, top3H, mx, my, false);
        }
    }

    private void drawDetailTab(int mx, int my, int x, int y, int w, String label,
                               boolean active, boolean isBuy, int[] hbArr) {
        int bg = active ? (isBuy ? 0xFF0C2E1E : 0xFF2E0C0C) : 0xFF050C18;
        int top = active ? (isBuy ? C_GREEN : C_RED) : C_BORDER2;
        drawRect(x, y, x + w, y + 18, bg);
        drawRect(x, y, x + w, y + 2, top);
        if (active) GuiRenderUtils.drawGradientRect(x, y + 2, x + w, y + 18, 0x14FFFFFF, 0x00000000);
        int lw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, x + (w - lw) / 2, y + 5, active ? C_WHITE : C_LGRAY);
        hb(hbArr, x, y, w, 18);
    }

    private void drawBuyPanel(int mx, int my, int x, int y, int w) {
        int qy = y + 5;
        fontRendererObj.drawString("§8Quantité :", x + 6, qy, C_GRAY);
        qy += 11;

        // Ligne de boutons quantité
        drawQtyRow(mx, my, x + 4, qy, w - 8, buyQty, selectedItem.getMaxStack(), true);
        qy += 17;

        // Champ saisie manuelle
        drawQtyInput(mx, my, x + 4, qy, w - 8, buyQty, true);
        qy += 14;

        double total = selectedItem.getBuyPrice() / 100.0 * buyQty;
        drawCtrAt("§8Total : §6" + FMT.format(total) + " §e$", x + w / 2, qy);
        qy += 13;
        boolean canBuy = playerBalance >= (long) (selectedItem.getBuyPrice() * (long) buyQty);
        int bx = x + 4, bw = w - 8, bh = 15;
        boolean hov = inR(mx, my, bx, qy, bw, bh);
        drawRect(bx, qy, bx + bw, qy + bh, hov ? 0xFF165533 : (canBuy ? 0xFF0C2E1E : 0xFF161616));
        drawRect(bx, qy, bx + bw, qy + 1, canBuy ? C_GREEN : 0xFF334433);
        if (hov && canBuy) GuiRenderUtils.drawGradientRect(bx, qy + 1, bx + bw, qy + bh, 0x18FFFFFF, 0x00000000);
        drawCtrAt("ACHETER ×" + buyQty, bx + bw / 2, qy + 4, canBuy ? C_WHITE : 0xFF556655);
        hb(hbAction, bx, qy, bw, bh);
        if (hov && !canBuy) {
            tooltipLines = new ArrayList<>();
            tooltipLines.add("§cSolde insuffisant !");
            tooltipLines.add("§7Requis : §6" + FMT.format(total) + " $");
            tooltipX = mx;
            tooltipY = my;
        }
    }

    private void drawSellPanel(int mx, int my, int x, int y, int w) {
        int qy = y + 5;
        fontRendererObj.drawString("§8Quantité :", x + 6, qy, C_GRAY);
        qy += 11;

        // Ligne de boutons quantité
        drawQtyRow(mx, my, x + 4, qy, w - 8, sellQty, 64, false);
        qy += 17;

        // Champ saisie manuelle
        drawQtyInput(mx, my, x + 4, qy, w - 8, sellQty, false);
        qy += 14;

        double gains = selectedItem.getSellPrice() / 100.0 * sellQty;
        drawCtrAt("§8Gains : §a" + FMT.format(gains) + " §e$", x + w / 2, qy);
        qy += 13;

        // Deux boutons côte à côte
        int gap = 3;
        int halfW = (w - 8 - gap) / 2;
        int bx1 = x + 4, bx2 = bx1 + halfW + gap, bh = 14;

        // Bouton VENDRE
        boolean hov1 = inR(mx, my, bx1, qy, halfW, bh);
        drawRect(bx1, qy, bx1+halfW, qy+bh, hov1 ? 0xFF4A1A1A : 0xFF2E0C0C);
        drawRect(bx1, qy, bx1+halfW, qy+1, C_RED);
        if (hov1) GuiRenderUtils.drawGradientRect(bx1, qy+1, bx1+halfW, qy+bh, 0x18FFFFFF, 0x00000000);
        drawCtrAt("§cVENDRE §f×"+sellQty, bx1+halfW/2, qy+4, C_WHITE);
        String sellLbl = "§cVENDRE §f×" + sellQty;
        int slw = fontRendererObj.getStringWidth(sellLbl);
        fontRendererObj.drawStringWithShadow(sellLbl, bx1 + (halfW - slw) / 2, qy + 4, C_WHITE);
        hb(hbAction, bx1, qy, halfW, bh);

        // Bouton TOUT VENDRE
        boolean hov2 = inR(mx, my, bx2, qy, halfW, bh);
        drawRect(bx2, qy, bx2 + halfW, qy + bh, hov2 ? 0xFF553300 : 0xFF2E1500);
        drawRect(bx2, qy, bx2 + halfW, qy + 1, C_ORANGE);
        if (hov2) GuiRenderUtils.drawGradientRect(bx2, qy + 1, bx2 + halfW, qy + bh, 0x18FFFFFF, 0x00000000);
        String allLbl = hov2 ? "§6TOUT" : "§7TOUT";
        drawCtrAt(allLbl, bx2 + halfW / 2, qy + 4, hov2 ? C_ORANGE : 0xFFAA7733);
        hb(hbSellAll, bx2, qy, halfW, bh);
    }

    private void drawQtyInput(int mx, int my, int x, int y, int w, int currentQty, boolean isBuy) {
        boolean focused = qtyInputFocused && qtyInputIsBuy == isBuy;
        boolean hov = inR(mx, my, x, y, w, 12);
        drawRect(x, y, x + w, y + 12, focused ? 0xFF040E1E : (hov ? 0xFF080F1C : 0xFF050A14));
        drawRect(x, y, x + w, y + 1, focused ? (isBuy ? C_GREEN : C_RED) : C_BORDER2);
        drawRect(x, y, x + 1, y + 12, focused ? (isBuy ? C_GREEN : C_RED) : C_BORDER2);
        drawRect(x + w - 1, y, x + w, y + 12, C_BORDER2);
        drawRect(x, y + 11, x + w, y + 12, C_BORDER2);

        String label = "§8Saisie : ";
        fontRendererObj.drawString(label, x + 3, y + 2, C_GRAY);
        int lw = fontRendererObj.getStringWidth(label);

        String inputText = focused
                ? (qtyInputStr.isEmpty() ? "" : "§f" + qtyInputStr) + "§7|"
                : "§7" + currentQty;
        fontRendererObj.drawString(inputText, x + 3 + lw, y + 2, C_WHITE);

        // Hitbox pour focus
        // On réutilise hbQP10 temporairement — non, on utilise un tableau dédié
        // → Géré dans mouseClicked via coordonnées directes
        if (hov && !focused) {
            tooltipLines = new ArrayList<>();
            tooltipLines.add("§7Clic pour saisir une quantité");
            tooltipX = mx;
            tooltipY = my;
        }
        hb(hbQtyInput, x, y, w, 12);
    }

    private void drawQtyRow(int mx, int my, int x, int y, int w, int qty, int maxQ, boolean isBuy) {
        int bs = 14, sp = 2;
        int total = 5 * bs + 4 * sp;
        int sx = x + (w - total) / 2;
        drawSmBtn(mx, my, sx, y, bs, "-10", hbQM10);
        drawSmBtn(mx, my, sx + bs + sp, y, bs, "-1", hbQM1);
        drawRect(sx + 2 * (bs + sp), y, sx + 2 * (bs + sp) + bs, y + bs, 0xFF040D1C);
        drawRect(sx + 2 * (bs + sp), y, sx + 2 * (bs + sp) + bs, y + 1, C_BORDER);
        String qs = "§f" + qty;
        int qw = fontRendererObj.getStringWidth(qs);
        fontRendererObj.drawStringWithShadow(qs, sx + 2 * (bs + sp) + (bs - qw) / 2, y + 3, C_WHITE);
        drawSmBtn(mx, my, sx + 3 * (bs + sp), y, bs, "+1", hbQP1);
        drawSmBtn(mx, my, sx + 4 * (bs + sp), y, bs, "MAX", hbQMax);
    }

    private void drawSmBtn(int mx, int my, int x, int y, int sz, String lbl, int[] arr) {
        boolean hov = inR(mx, my, x, y, sz, sz);
        drawRect(x, y, x + sz, y + sz, hov ? 0xFF1A3A6A : 0xFF0C1C38);
        drawRect(x, y, x + sz, y + 1, hov ? C_ACCENT2 : C_BORDER);
        int lw = fontRendererObj.getStringWidth(lbl);
        fontRendererObj.drawString(lbl, x + (sz - lw) / 2, y + (sz - 7) / 2, hov ? C_WHITE : C_GRAY);
        hb(arr, x, y, sz, sz);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  GRAPHIQUE GÉNÉRIQUE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * @param hist       liste des prix en euros
     * @param title      titre avec codes couleur
     * @param currentVal prix courant en centimes
     * @param lineColor  couleur montée de la ligne
     * @param fillColor  couleur remplissage sous la courbe
     * @param floor      plancher en centimes (0 = désactivé)
     * @param ceil       plafond en centimes (0 = désactivé)
     */
    private void drawChart(int gx, int gy, int gw, int gh, int mx, int my,
                           List<Double> hist, String title,
                           long currentVal, int lineColor, int fillColor,
                           long floor, long ceil) {
        drawRect(gx, gy, gx + gw, gy + gh, C_PANEL);
        drawRect(gx, gy, gx + gw, gy + 2, lineColor & 0x00FFFFFF | 0xFF000000);
        drawRect(gx + gw - 1, gy, gx + gw, gy + gh, C_BORDER2);
        drawRect(gx, gy, gx + gw, gy + 15, 0xFF020910);
        fontRendererObj.drawString(title, gx + 6, gy + 4, C_WHITE);
        String curP = "§e" + FMT.format(currentVal / 100.0) + " §6$";
        fontRendererObj.drawString(curP, gx + gw - fontRendererObj.getStringWidth(curP) - 6, gy + 4, lineColor);

        int cx = gx + 32, cy = gy + 17, cw = gw - 40, ch = gh - 24;
        drawRect(cx, cy, cx + cw, cy + ch, 0xFF010609);

        if (hist == null || hist.size() < 2) {
            fontRendererObj.drawStringWithShadow("§8Données insuffisantes (< 2 mesures)",
                    cx + cw / 2 - 70, cy + ch / 2 - 4, C_GRAY);
            return;
        }

        double minV = hist.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxV = hist.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        if (maxV == minV) maxV = minV * 1.1 + 0.01;
        double range = maxV - minV;
        double dmin = minV - range * 0.05, dmax = maxV + range * 0.12, drange = dmax - dmin;

        // Grille
        for (int g = 0; g <= 4; g++) {
            int ly = cy + ch - (int) ((g / 4.0) * ch);
            for (int xx = cx; xx < cx + cw; xx += 3) drawRect(xx, ly, xx + 1, ly + 1, 0x1AAACCFF);
            String lbl = FMT.format(dmin + (g / 4.0) * drange);
            if (lbl.length() > 7) lbl = lbl.substring(0, 7);
            fontRendererObj.drawString("§8" + lbl, gx + 1, ly - 3, 0xFF445566);
        }

        int n = hist.size();
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = cx + (n <= 1 ? cw / 2 : (int) ((double) i / (n - 1) * (cw - 4)) + 2);
            ys[i] = cy + ch - (int) (((hist.get(i) - dmin) / drange) * (ch - 4)) - 2;
        }

        // Remplissage
        int fc1 = (fillColor & 0x00FFFFFF) | 0x33000000;
        int fc2 = (fillColor & 0x00FFFFFF) | 0x1E000000;
        int fc3 = (fillColor & 0x00FFFFFF) | 0x0A000000;
        for (int i = 0; i < n - 1; i++) {
            int x1 = xs[i], x2 = xs[i + 1], y1 = ys[i], y2 = ys[i + 1];
            for (int px2 = x1; px2 <= x2; px2++) {
                float t = (x2 == x1) ? 0 : (float) (px2 - x1) / (x2 - x1);
                int py2 = (int) (y1 + (y2 - y1) * t);
                int fillH = cy + ch - py2;
                if (fillH > 0) {
                    int h1 = fillH / 3, h2 = fillH - h1 * 2;
                    if (h1 > 0) drawRect(px2, py2, px2 + 1, py2 + h1, fc1);
                    if (h2 > 0) drawRect(px2, py2 + h1, px2 + 1, py2 + h1 + h2, fc2);
                    if (h1 > 0) drawRect(px2, py2 + h1 + h2, px2 + 1, cy + ch, fc3);
                }
            }
        }

        // Plancher / plafond
        if (floor > 0) {
            double fv = floor / 100.0;
            if (fv > dmin && fv < dmax) {
                int fy = cy + ch - (int) (((fv - dmin) / drange) * (ch - 4)) - 2;
                for (int px2 = cx; px2 < cx + cw; px2 += 4) drawRect(px2, fy, px2 + 2, fy + 1, 0x99EE4444);
                fontRendererObj.drawString("§c▼", cx + cw - 8, fy - 5, C_RED);
            }
        }
        if (ceil > 0) {
            double cv2 = ceil / 100.0;
            if (cv2 > dmin && cv2 < dmax) {
                int cy2 = cy + ch - (int) (((cv2 - dmin) / drange) * (ch - 4)) - 2;
                for (int px2 = cx; px2 < cx + cw; px2 += 4) drawRect(px2, cy2, px2 + 2, cy2 + 1, 0x9933CC77);
                fontRendererObj.drawString("§a▲", cx + cw - 8, cy2 + 1, C_GREEN);
            }
        }

        // Ligne
        for (int i = 0; i < n - 1; i++)
            drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1],
                    ys[i + 1] < ys[i] ? (lineColor | 0xFF000000) : 0xFFEE4444, 2);

        // Points
        for (int i = 0; i < n; i++) {
            boolean last = (i == n - 1);
            int pr = last ? 3 : 2;
            drawRect(xs[i] - pr, ys[i] - pr, xs[i] + pr, ys[i] + pr, 0xFF010609);
            drawRect(xs[i] - pr + 1, ys[i] - pr + 1, xs[i] + pr - 1, ys[i] + pr - 1, last ? C_GOLD : 0xFFCCDDFF);
        }

        // Tooltip survol
        if (inR(mx, my, cx, cy, cw, ch)) {
            int closest = 0, minD = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                int d = Math.abs(mx - xs[i]);
                if (d < minD) {
                    minD = d;
                    closest = i;
                }
            }
            if (minD < 20) {
                drawRect(xs[closest] - 4, ys[closest] - 4, xs[closest] + 4, ys[closest] + 4, 0x44FFDD00);
                drawRect(xs[closest] - 3, ys[closest] - 3, xs[closest] + 3, ys[closest] + 3, 0xBBFFDD00);
                tooltipLines = new ArrayList<>();
                tooltipLines.add("§f" + selectedItem.getDisplayName());
                tooltipLines.add("§7Mesure §f#" + (closest + 1) + " §7/ " + n);
                tooltipLines.add("§7Prix : §6" + FMT.format(hist.get(closest)) + " $");
                if (closest > 0) {
                    double diff = hist.get(closest) - hist.get(closest - 1);
                    tooltipLines.add((diff >= 0 ? "§a▲ " : "§c▼ ") + FMT.format(Math.abs(diff)) + " $ vs précédent");
                }
                tooltipX = mx;
                tooltipY = my;
            }
        }

        // Évolution globale
        if (n >= 2) {
            double diff = hist.get(n - 1) - hist.get(0);
            String arrow = diff >= 0 ? "§a▲" : "§c▼";
            String pct = String.format("%.1f%%", Math.abs(diff / Math.max(0.01, hist.get(0)) * 100));
            fontRendererObj.drawString(arrow + " " + pct,
                    gx + gw - fontRendererObj.getStringWidth(arrow + " " + pct) - 4, gy + gh - 10, C_WHITE);
        }
    }

    private void drawLine(int x1, int y1, int x2, int y2, int color, int thick) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy, cx = x1, cy2 = y1, h = thick / 2;
        while (true) {
            drawRect(cx - h, cy2 - h, cx + h + 1, cy2 + h + 1, color);
            if (cx == x2 && cy2 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                cx += sx;
            }
            if (e2 < dx) {
                err += dx;
                cy2 += sy;
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TOP-3 (achetés OU vendus selon l'onglet)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * @param showBought true = top achetés, false = top vendus
     */
    private void drawTop3Panel(int gx, int gy, int gw, int gh, int mx, int my, boolean showBought) {
        drawRect(gx, gy, gx + gw, gy + gh, C_PANEL);
        drawRect(gx, gy, gx + gw, gy + 2, showBought ? C_GOLD : C_RED);
        drawRect(gx, gy, gx + 1, gy + gh, C_BORDER2);
        drawRect(gx + gw - 1, gy, gx + gw, gy + gh, C_BORDER2);
        drawRect(gx, gy, gx + gw, gy + 13, 0xFF020910);

        List<ShopPacketHandler.MarketEntry> entries = showBought ? topBought : topSold;
        String title = showBought ? "§6§lTop achetés" : "§c§lTop vendus";
        fontRendererObj.drawString(title, gx + 6, gy + 3, C_WHITE);

        if (entries.isEmpty()) {
            fontRendererObj.drawString("§8Aucune donnée encore.", gx + 6, gy + 18, C_GRAY);
            return;
        }

        String[] medals = {"§6①", "§7②", "§e③"};
        int color = showBought ? C_GOLD : C_RED;
        int rowH = Math.max(18, (gh - 16) / Math.max(1, Math.min(3, entries.size())));
        int y = gy + 16;

        for (int i = 0; i < entries.size() && i < 3; i++) {
            ShopPacketHandler.MarketEntry e = entries.get(i);
            boolean hov = inR(mx, my, gx + 2, y, gw - 4, rowH - 1);
            drawRect(gx + 2, y, gx + gw - 2, y + rowH - 1, hov ? 0xFF0A1C38 : 0xFF050D1E);
            if (hov) drawRect(gx + 2, y, gx + 4, y + rowH - 1, color);

            ItemStack icon = resolveIcon(e.getMinecraftItem());
            boolean rendered = renderIconSafe(icon, gx + 5, y + (rowH - 16) / 2);
            if (!rendered) {
                drawRect(gx + 5, y + (rowH - 12) / 2, gx + 21, y + (rowH + 12) / 2, color & 0x55FFFFFF | 0x33000000);
            }

            fontRendererObj.drawStringWithShadow(medals[i] + " §f" + e.getDisplayName(), gx + 23, y + 2, C_WHITE);
            String volStr = showBought
                    ? "§6" + fmtC(e.getBuyPrice()) + "$ §8| §7×" + e.getVolume()
                    : "§a" + fmtC(e.getSellPrice()) + "$ §8| §7×" + e.getVolume();
            fontRendererObj.drawString(volStr, gx + 23, y + rowH / 2 + 2, C_LGRAY);

            if (hov) {
                tooltipLines = new ArrayList<>();
                tooltipLines.add("§f" + e.getDisplayName());
                tooltipLines.add("§7Achat : §6" + fmtC(e.getBuyPrice()) + " $");
                tooltipLines.add("§7Vente : §a" + fmtC(e.getSellPrice()) + " $");
                tooltipLines.add("§7Volume : §f" + e.getVolume());
                tooltipX = mx;
                tooltipY = my;
            }
            y += rowH;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  STATUS BAR
    // ═════════════════════════════════════════════════════════════════════════

    private void drawStatusBar() {
        if (statusTimer <= 0) return;
        statusTimer--;
        int alpha = Math.min(255, statusTimer * 5);
        int col = ((statusOk ? C_GREEN : C_RED) & 0x00FFFFFF) | (alpha << 24);
        int sw = fontRendererObj.getStringWidth(statusMsg);
        int sx = px + (pw - sw) / 2 - 4;
        int sy = py + ph - 13;
        drawRect(sx - 2, sy - 2, sx + sw + 6, sy + 10, ((alpha / 4) << 24) | 0x000000);
        fontRendererObj.drawStringWithShadow(statusMsg, sx + 2, sy, col);
    }

    private void drawScrollbar(int x, int y, int w, int h, int scroll, int total, int visible, int mx, int my) {
        if (total <= visible) return;
        drawRect(x, y, x + w, y + h, 0xFF030A14);
        int trackH = h - 4;
        int thumbH = Math.max(14, trackH * visible / total);
        int thumbY = y + 2 + (trackH - thumbH) * scroll / Math.max(1, total - visible);
        boolean hov = inR(mx, my, x, thumbY, w, thumbH);
        drawRect(x + 1, thumbY, x + w - 1, thumbY + thumbH, hov ? C_ACCENT2 : C_ACCENT);
        drawRect(x + 1, thumbY, x + w - 1, thumbY + 1, hov ? C_WHITE : C_ACCENT2);
        hb(hbScrollUp, x, y, w, 8);
        hb(hbScrollDn, x, y + h - 8, w, 8);
        lastScrollX = x;
        lastScrollY = y;
        lastScrollW = w;
        lastScrollH = h;
        lastScrollTotal = total;
        lastScrollVisible = visible;
        lastThumbY = thumbY;
        lastThumbH = thumbH;
    }

    private void renderTooltip(List<String> lines, int mx, int my) {
        if (lines.isEmpty()) return;
        int tw = 0;
        for (String l : lines) tw = Math.max(tw, fontRendererObj.getStringWidth(l));
        int th = lines.size() * 10 + 6;
        int tx = Math.min(mx + 12, width - tw - 10);
        int ty = Math.max(my - th - 6, 4);
        drawRect(tx - 4, ty - 4, tx + tw + 6, ty + th + 4, 0xF0080816);
        drawRect(tx - 4, ty - 4, tx + tw + 6, ty - 3, C_ACCENT);
        drawRect(tx - 4, ty - 4, tx - 3, ty + th + 4, C_ACCENT);
        drawRect(tx - 4, ty + th + 3, tx + tw + 6, ty + th + 4, C_BORDER2);
        for (int i = 0; i < lines.size(); i++)
            fontRendererObj.drawStringWithShadow(lines.get(i), tx, ty + 2 + i * 10, i == 0 ? C_WHITE : C_LGRAY);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CLICS / CLAVIER / SOURIS
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        // Scrollbar drag
        if (lastScrollTotal > 0 && inR(mx, my, lastScrollX + 1, lastThumbY, lastScrollW - 2, lastThumbH)) {
            draggingScroll = true;
            return;
        }
        if (lastScrollTotal > 0 && inR(mx, my, lastScrollX, lastScrollY, lastScrollW, lastScrollH)) {
            int trackH = lastScrollH - 4;
            int rel = my - (lastScrollY + 2);
            int maxScroll = Math.max(0, lastScrollTotal - lastScrollVisible);
            int thumbSpace = Math.max(1, trackH - lastThumbH);
            scroll = (int) (Math.max(0f, Math.min(1f, rel / (float) thumbSpace)) * maxScroll);
            return;
        }

        if (inH(mx, my, hbClose)) {
            mc.displayGuiScreen(null);
            return;
        }

        if (inH(mx, my, hbSearchBox)) {
            int sbX = px + pw / 2 - 100, sbW = 200;
            if (!searchQuery.isEmpty() && mx >= sbX + sbW - 12 && mx <= sbX + sbW) {
                searchQuery = "";
                searchMode = false;
                applyFilter();
            } else {
                searchFocused = true;
            }
            return;
        } else {
            searchFocused = false;
        }

        if (screen == Screen.DETAIL && selectedItem != null) {
            if (inH(mx, my, hbQtyInput)) {
                qtyInputFocused = true;
                qtyInputIsBuy = (detailTab == DetailTab.BUY);
                qtyInputStr = "";
                return;
            } else if (qtyInputFocused) {
                qtyInputFocused = false;
                applyQtyInput();
            }
        }

        if (inH(mx, my, hbBack)) {
            if (searchMode) {
                searchMode = false;
                searchQuery = "";
                applyFilter();
            } else if (screen == Screen.DETAIL) {
                screen = Screen.ITEMS;
                selectedItem = null;
                // Recharger les items de la catégorie
                loadingItems = true;
                ShopPacketHandler.requestItems(selectedCatId);
            } else {
                screen = Screen.CATEGORIES;
            }
            return;
        }

        // Clics en mode recherche
        if (searchMode && !searchQuery.isEmpty()) {
            for (int i = 0; i < MAX_ROWS; i++) {
                int idx = i + scroll;
                if (idx >= filtered.size()) break;
                if (inH(mx, my, hbRows[i])) {
                    openDetail(filtered.get(idx));
                    return;
                }
            }
            if (inH(mx, my, hbScrollUp)) {
                scroll = Math.max(0, scroll - 1);
                return;
            }
            if (inH(mx, my, hbScrollDn)) {
                int maxR = Math.max(1, (ph - 68) / ROW_H);
                scroll = Math.min(scroll + 1, Math.max(0, filtered.size() - maxR));
                return;
            }
            return;
        }

        switch (screen) {
            case CATEGORIES:
                for (int i = 0; i < categories.size() && i < MAX_CATS; i++) {
                    if (inH(mx, my, hbCats[i])) {
                        selectedCatId = categories.get(i).getId();
                        selectedCatName = categories.get(i).getName();
                        screen = Screen.ITEMS;
                        loadingItems = true;
                        scroll = 0;
                        buyQty = sellQty = 1;
                        ShopPacketHandler.requestItems(selectedCatId);
                        applyFilter();
                        return;
                    }
                }
                break;
            case ITEMS:
                if (inH(mx, my, hbScrollUp)) {
                    scroll = Math.max(0, scroll - 1);
                    return;
                }
                if (inH(mx, my, hbScrollDn)) {
                    int maxR = Math.max(1, (ph - 68) / ROW_H);
                    scroll = Math.min(scroll + 1, Math.max(0, filtered.size() - maxR));
                    return;
                }
                for (int i = 0; i < MAX_ROWS; i++) {
                    int idx = i + scroll;
                    if (idx >= filtered.size()) break;
                    if (inH(mx, my, hbRows[i])) {
                        openDetail(filtered.get(idx));
                        return;
                    }
                }
                break;
            case DETAIL:
                if (selectedItem == null) break;
                if (inH(mx, my, hbTabBuy)) {
                    detailTab = DetailTab.BUY;
                    return;
                }
                if (inH(mx, my, hbTabSell)) {
                    detailTab = DetailTab.SELL;
                    return;
                }
                if (detailTab == DetailTab.BUY) {
                    if (inH(mx, my, hbQM10)) {
                        buyQty = Math.max(1, buyQty - 10);
                        return;
                    }
                    if (inH(mx, my, hbQM1)) {
                        buyQty = Math.max(1, buyQty - 1);
                        return;
                    }
                    if (inH(mx, my, hbQP1)) {
                        buyQty = Math.min(selectedItem.getMaxStack(), buyQty + 1);
                        return;
                    }
                    if (inH(mx, my, hbQP10)) {
                        buyQty = Math.min(selectedItem.getMaxStack(), buyQty + 10);
                        return;
                    }
                    if (inH(mx, my, hbQMax)) {
                        buyQty = selectedItem.getMaxStack();
                        return;
                    }
                    if (inH(mx, my, hbAction)) {
                        ShopPacketHandler.buyItem(selectedItem.getId(), buyQty);
                        return;
                    }
                } else {
                    if (inH(mx, my, hbQM10)) {
                        sellQty = Math.max(1, sellQty - 10);
                        return;
                    }
                    if (inH(mx, my, hbQM1)) {
                        sellQty = Math.max(1, sellQty - 1);
                        return;
                    }
                    if (inH(mx, my, hbQP1)) {
                        sellQty = Math.min(64, sellQty + 1);
                        return;
                    }
                    if (inH(mx, my, hbQP10)) {
                        sellQty = Math.min(64, sellQty + 10);
                        return;
                    }
                    if (inH(mx, my, hbQMax)) {
                        sellQty = 64;
                        return;
                    }
                    if (inH(mx, my, hbAction)) {
                        ShopPacketHandler.sellItem(selectedItem.getId(), sellQty);
                        return;
                    }
                    if (inH(mx, my, hbSellAll)) {
                        ShopPacketHandler.sellAll(selectedItem.getId());
                        return;
                    }
                }
                break;
        }
    }

    private void applyQtyInput() {
        if (qtyInputStr.isEmpty()) return;
        try {
            int val = Integer.parseInt(qtyInputStr);
            if (val < 1) val = 1;
            if (qtyInputIsBuy) {
                buyQty = Math.min(val, selectedItem != null ? selectedItem.getMaxStack() : 64);
            } else {
                sellQty = Math.min(val, 64);
            }
        } catch (NumberFormatException ignored) {
        }
        qtyInputStr = "";
    }

    private void openDetail(ShopPacketHandler.ShopItem it) {
        selectedItem = it;
        screen = Screen.DETAIL;
        detailTab = DetailTab.BUY;
        buyQty = sellQty = 1;
        searchMode = false;
        ShopPacketHandler.requestItemDetail(it.getId());
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        draggingScroll = false;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        super.mouseClickMove(mx, my, btn, time);
        if (draggingScroll && btn == 0 && lastScrollTotal > 0) {
            int trackH = lastScrollH - 4;
            int maxScroll = Math.max(0, lastScrollTotal - lastScrollVisible);
            int rel = my - (lastScrollY + 2);
            int thumbSpace = Math.max(1, trackH - lastThumbH);
            scroll = (int) (Math.max(0f, Math.min(1f, rel / (float) thumbSpace)) * maxScroll);
        }
    }

    @Override
    protected void keyTyped(char ch, int key) throws IOException {
        super.keyTyped(ch, key);
        if (qtyInputFocused) {
            if (key == 14) {
                if (!qtyInputStr.isEmpty())
                    qtyInputStr = qtyInputStr.substring(0, qtyInputStr.length() - 1);
            } else if (key == 28 || key == 156) { // Entrée
                applyQtyInput();
                qtyInputFocused = false;
            } else if (key == 1) {
                qtyInputStr = "";
                qtyInputFocused = false;
            } else if (ch >= '0' && ch <= '9' && qtyInputStr.length() < 5) {
                qtyInputStr += ch;
            }
            return;
        }
        if (searchFocused) {
            if (key == 14) {
                if (!searchQuery.isEmpty())
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            } else if (key == 1) {
                searchFocused = false;
                if (searchQuery.isEmpty()) searchMode = false;
            } else if (key == 30 && isCtrlKeyDown()) {
                searchQuery = "";
                searchMode = false;
                applyFilter();
            } else if (ch >= 32 && searchQuery.length() < 32) {
                searchQuery += ch;
            }
            searchMode = !searchQuery.isEmpty();
            applyFilter();
        } else if (key == 1) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int delta = org.lwjgl.input.Mouse.getEventDWheel();
        if (delta != 0) {
            int maxR = Math.max(1, (ph - 68) / ROW_H);
            int maxScroll = Math.max(0, filtered.size() - maxR);
            if (delta < 0) scroll = Math.min(scroll + 1, maxScroll);
            else scroll = Math.max(0, scroll - 1);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private String fmtC(long centimes) {
        return FMT.format(centimes / 100.0);
    }

    private void drawCtr(String text, int cy2) {
        fontRendererObj.drawStringWithShadow(text, px + (pw - fontRendererObj.getStringWidth(text)) / 2, cy2, C_GRAY);
    }

    private void drawCtrAt(String text, int cx2, int cy2) {
        fontRendererObj.drawStringWithShadow(text, cx2 - fontRendererObj.getStringWidth(text) / 2, cy2, C_WHITE);
    }

    private void drawCtrAt(String text, int cx2, int cy2, int color) {
        fontRendererObj.drawStringWithShadow(text, cx2 - fontRendererObj.getStringWidth(text) / 2, cy2, color);
    }

    private void drawInfoRow(int panelX, int y, String lbl, String val, int panelW) {
        fontRendererObj.drawString("§8" + lbl, panelX + 8, y, C_GRAY);
        fontRendererObj.drawStringWithShadow(val, panelX + panelW - 8 - fontRendererObj.getStringWidth(val), y, C_WHITE);
    }

    private void drawSep(int x, int y, int w) {
        drawRect(x, y, x + w, y + 1, 0x33AACCFF);
    }

    private boolean inR(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean inH(int mx, int my, int[] h) {
        return h[2] > 0 && inR(mx, my, h[0], h[1], h[2], h[3]);
    }

    private void hb(int[] arr, int x, int y, int w, int h) {
        arr[0] = x;
        arr[1] = y;
        arr[2] = w;
        arr[3] = h;
    }

    /**
     * Normalise le nom de catégorie pour l'affichage (Minerais → Minérais)
     */
    private String normalizeCatName(String cat) {
        if (cat == null) return "";
        // Uniformise les variantes orthographiques
        if (cat.equalsIgnoreCase("minerais") || cat.equalsIgnoreCase("mineraux")
                || cat.equalsIgnoreCase("minéraux")) return "Minérais";
        return cat;
    }

    private ItemStack resolveIcon(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            String raw = name.trim().replace(' ', '_');
            String base = raw;
            String metaStr = null;

            if (raw.contains("#")) {
                String[] sp = raw.split("#", 2);
                base = sp[0];
                metaStr = sp[1];
            } else if (raw.contains("@")) {
                String[] sp = raw.split("@", 2);
                base = sp[0];
                metaStr = sp[1];
            } else if (raw.contains("|")) {
                String[] sp = raw.split("\\|", 2);
                base = sp[0];
                metaStr = sp[1];
            } else if (raw.contains(":")) {
                int lc = raw.lastIndexOf(':');
                String left = raw.substring(0, lc);
                String right = raw.substring(lc + 1);
                if (!left.contains(":")) {
                    base = left;
                    metaStr = right;
                } else {
                    base = raw;
                    metaStr = null;
                }
            }

            try {
                Item it = Item.getByNameOrId(raw.toLowerCase());
                if (it != null) return new ItemStack(it, 1, metaStr != null ? parseMeta(metaStr) : 0);
            } catch (Exception ignored) {
            }
            try {
                Block b = Block.getBlockFromName(raw.toLowerCase());
                if (b != null) {
                    int meta = metaStr != null ? parseMeta(metaStr) : 0;
                    Item ib = Item.getItemFromBlock(b);
                    return ib != null ? new ItemStack(ib, 1, meta) : new ItemStack(b, 1, meta);
                }
            } catch (Exception ignored) {
            }

            String baseKey = base.toLowerCase();
            for (String color : new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"}) {
                if (baseKey.equals(color + "_wool") || baseKey.equals("wool_" + color)) {
                    Item wool = Item.getByNameOrId("minecraft:wool");
                    if (wool != null) return new ItemStack(wool, 1, parseMeta(color));
                }
            }

            Item item = Item.getByNameOrId(baseKey);
            if (item == null) item = Item.getByNameOrId("minecraft:" + baseKey);
            if (item != null) return new ItemStack(item, 1, metaStr != null ? parseMeta(metaStr) : 0);

            Block block = Block.getBlockFromName(baseKey);
            if (block == null) block = Block.getBlockFromName("minecraft:" + baseKey);
            if (block != null) {
                int meta = metaStr != null ? parseMeta(metaStr) : 0;
                Item ib = Item.getItemFromBlock(block);
                return ib != null ? new ItemStack(ib, 1, meta) : new ItemStack(block, 1, meta);
            }

            for (int id = 0; id < 4096; id++) {
                Item maybe = Item.getItemById(id);
                if (maybe == null) continue;
                String uname = maybe.getUnlocalizedName();
                if (uname != null && uname.toLowerCase().contains(baseKey))
                    return new ItemStack(maybe, 1, metaStr != null ? parseMeta(metaStr) : 0);
            }
        } catch (Exception e) {
            System.out.println("[GuiShop] Icon introuvable: " + name);
        }
        return null;
    }

    private int parseMeta(String s) {
        if (s == null) return 0;
        s = s.trim().toLowerCase();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
        }
        switch (s.replace(' ', '_')) {
            case "white":
            case "blanc":
                return 0;
            case "orange":
                return 1;
            case "magenta":
                return 2;
            case "light_blue":
                return 3;
            case "yellow":
            case "jaune":
                return 4;
            case "lime":
                return 5;
            case "pink":
            case "rose":
                return 6;
            case "gray":
            case "grey":
            case "gris":
                return 7;
            case "silver":
            case "light_gray":
            case "lightgray":
            case "gris_clair":
                return 8;
            case "cyan":
                return 9;
            case "purple":
            case "violet":
                return 10;
            case "blue":
            case "bleu":
                return 11;
            case "brown":
            case "marron":
                return 12;
            case "green":
            case "vert":
                return 13;
            case "red":
            case "rouge":
                return 14;
            case "black":
            case "noir":
                return 15;
        }
        return 0;
    }

    private int catColor(String cat) {
        if (cat == null) return 0xFF445566;
        switch (cat) {
            case "Agriculture":
                return 0xFF3DB35E;
            case "Élevage":
                return 0xFFCC8833;
            case "Bois":
                return 0xFF8B5E3C;
            case "Terrain":
                return 0xFF888888;
            case "Minérais":
                return 0xFF4488CC;
            case "Nether":
                return 0xFFCC4422;
            case "Redstone":
                return 0xFFCC2222;
            case "Déco":
                return 0xFF8866AA;
            case "Divers":
                return 0xFF5588AA;
            case "Moddé":
                return 0xFFE0AA22;
            default:
                return 0xFF445566;
        }
    }

    private int blendColor(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16 & 0xFF) * (1 - t)) + ((c2 >> 16 & 0xFF) * t));
        int g = (int) (((c1 >> 8 & 0xFF) * (1 - t)) + ((c2 >> 8 & 0xFF) * t));
        int b = (int) (((c1 & 0xFF) * (1 - t)) + ((c2 & 0xFF) * t));
        int a = (int) (((c1 >> 24 & 0xFF) * (1 - t)) + ((c2 >> 24 & 0xFF) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}