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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class GuiShop extends GuiScreen {

    // ─── Palette ─────────────────────────────────────────────────────────────
    private static final int C_BG = 0xF2050914;
    private static final int C_PANEL = 0xFF0B1421;
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
    private static final List<ShopPacketHandler.ShopItem> staticAllItemsCache = new ArrayList<>();

    private ShopPacketHandler.ShopItem selectedItem = null;
    private int selectedCatId = -1;
    private String selectedCatName = "";
    private boolean loadingCats = true;
    private boolean loadingItems = false;
    private long playerBalance = 0L;

    // ─── Ticker Persistant ───────────────────────────────────────────────────
    private static float tickerOffset = 0; 
    private static long lastTickTime = 0;
    
    // Pour chaque item (ID), on stocke le dernier prix affiché pour calculer la variation
    // <ID, Prix précédent>
    private static final Map<Integer, Double> tickerHistory = new HashMap<>();

    // ─── Recherche ───────────────────────────────────────────────────────────
    private String searchQuery = "";
    private boolean searchFocused = false;
    private boolean searchMode = false;

    // -── Saisie quantité ──────────────────────────────────────────────────────
    private boolean qtyInputFocused = false;
    private String qtyInputStr = "";
    private boolean qtyInputIsBuy = true;

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
            pw = Math.min(840, width - 16);
            ph = Math.min(520, height - 16);
            px = (width - pw) / 2;
            py = (height - ph) / 2;
            clearHb();
            lastTickTime = System.currentTimeMillis();

            ShopPacketHandler.setCategoriesListener(cats ->
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        categories.clear();
                        categories.addAll(cats);
                        loadingCats = false;
                        if (!cats.isEmpty()) ShopPacketHandler.requestItems(-1);
                    }));

            ShopPacketHandler.setItemsListener(its ->
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        // Check if it's a detail update
                        if (its.size() == 1 && its.get(0).getBuyHistory().size() > 0 && selectedItem != null
                                && its.get(0).getId() == selectedItem.getId()) {
                            selectedItem = its.get(0);
                            for (int i = 0; i < staticAllItemsCache.size(); i++) {
                                if (staticAllItemsCache.get(i).getId() == selectedItem.getId()) {
                                    staticAllItemsCache.set(i, selectedItem);
                                    break;
                                }
                            }
                            // Don't refresh the full list view logic if we are just updating details
                            return;
                        }
                        
                        items.clear();
                        items.addAll(its);
                        
                        for (ShopPacketHandler.ShopItem it : its) {
                            boolean found = false;
                            for (int i=0; i<staticAllItemsCache.size(); i++) {
                                if (staticAllItemsCache.get(i).getId() == it.getId()) {
                                    staticAllItemsCache.set(i, it);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) staticAllItemsCache.add(it);
                        }
                        
                        loadingItems = false;
                        // scroll = 0; // REMOVED: Do not reset scroll on item update chunks
                        applyFilter();
                    }));

            ShopPacketHandler.setTransactionListener((ok, msg, bal) ->
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        statusMsg = msg;
                        statusTimer = 220;
                        statusOk = ok;
                        playerBalance = bal;
                        if (ok && screen == Screen.DETAIL) {
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
                for(ShopPacketHandler.ShopItem i : ci) {
                    if(staticAllItemsCache.stream().noneMatch(x -> x.getId() == i.getId())) {
                        staticAllItemsCache.add(i);
                    }
                }
            }
            topBought.addAll(ShopPacketHandler.getCachedTopBought());
            topSold.addAll(ShopPacketHandler.getCachedTopSold());

            ShopPacketHandler.requestCategories();
        } catch (Exception e) {
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
        hbQtyInput[2] = 0;
    }

    private void applyFilter() {
        filtered.clear();
        List<ShopPacketHandler.ShopItem> src = searchMode ? staticAllItemsCache : items;
        String q = fixString(searchQuery).toLowerCase().trim();
        if (q.isEmpty()) {
            filtered.addAll(src);
        } else {
            for (ShopPacketHandler.ShopItem it : src)
                if (fixString(it.getDisplayName()).toLowerCase().contains(q)
                        || fixString(it.getCategory()).toLowerCase().contains(q))
                    filtered.add(it);
        }
        
        // Si on est dans Décoration sans recherche spécifique, on affiche tout pour s'assurer d'avoir les vitres
        if (!searchMode && selectedCatName != null) {
            String sel = selectedCatName.toLowerCase();
             if (sel.contains("deco") || sel.contains("déco") || sel.contains("décoration")) {
                 for (ShopPacketHandler.ShopItem it : staticAllItemsCache) {
                     // Force l'ajout des vitres si elles sont manquantes
                     if (fixString(it.getCategory()).toLowerCase().contains("déco") && filtered.stream().noneMatch(x -> x.getId() == it.getId())) {
                         filtered.add(it);
                     }
                 }
             }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDU PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xDD030508);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Ombre portée principale
        drawGradientRect(px - 4, py + 10, px + pw + 4, py + ph + 8, 0x88000000, 0x00000000);

        // Fond Panneau Principal
        drawRect(px, py, px + pw, py + ph, C_BG);
        drawGradientRect(px, py, px + pw, py + ph / 2, 0x10FFFFFF, 0x00000000);

        // Bordures high-tech
        drawRect(px, py, px + pw, py + 1, C_BORDER2); // Top
        drawRect(px, py + ph - 1, px + pw, py + ph, C_BORDER2); // Bottom
        drawRect(px, py, px + 1, py + ph, C_BORDER2); // Left
        drawRect(px + pw - 1, py, px + pw, py + ph, C_BORDER2); // Right

        // Ligne d'accent supérieure
        drawRect(px, py, px + pw, py + 2, C_ACCENT);

        tooltipLines = null;
        drawHeader(mx, my);
        drawSearchBar(mx, my);

        int contentY = py + 50;
        int contentH = ph - 68;

        if (searchMode && !searchQuery.isEmpty()) {
            drawSearchResults(mx, my, contentY, contentH);
        } else {
            switch (screen) {
                case CATEGORIES:
                    drawMainDashboard(mx, my, contentY, contentH);
                    break;
                case ITEMS:
                    drawItems(mx, my, contentY, contentH);
                    break;
                case DETAIL:
                    drawDetail(mx, my, contentY, contentH);
                    break;
            }
        }

        if (screen != Screen.DETAIL) {
            drawTicker(py + ph - 16, pw);
        }
        
        drawStatusBar();

        if (tooltipLines != null && !tooltipLines.isEmpty())
            renderTooltip(tooltipLines, tooltipX, tooltipY);

        GlStateManager.disableBlend();
        super.drawScreen(mx, my, pt);
    }

    // ─── Ticker Boursier ───────────────────────────────────────────

    private void drawTicker(int y, int w) {
        drawRect(px, y, px + w, y + 16, 0xFF020509);
        drawRect(px, y, px + w, y + 1, 0xFF152535);

        // Filtrage : uniquement les items avec volume d'échange > 0 ou gelés
        List<ShopPacketHandler.ShopItem> source = staticAllItemsCache.isEmpty() ? items : staticAllItemsCache;
        List<ShopPacketHandler.ShopItem> activeItems = new ArrayList<>();
        
        for (ShopPacketHandler.ShopItem it : source) {
            long vol = it.getTotalBuyVolume() + it.getTotalSellVolume();
            if (vol > 0 || it.isFrozen()) activeItems.add(it);
        }

        // Tri par volume
        Collections.sort(activeItems, (a, b) -> Long.compare(
                b.getTotalBuyVolume() + b.getTotalSellVolume(),
                a.getTotalBuyVolume() + a.getTotalSellVolume()));

        if (activeItems.isEmpty()) {
            fontRendererObj.drawString("§8En attente de mouvements boursiers...", px + 35, y + 5, C_GRAY);
            drawRect(px, y, px + 30, y + 16, 0xFFCC2222);
            fontRendererObj.drawString("§fLIVE", px + 4, y + 4, C_WHITE);
            return;
        }

        StringBuilder tickerText = new StringBuilder();
        int limit = 0;
        final double EPS = 0.001;

        for (ShopPacketHandler.ShopItem it : activeItems) {
            if (limit++ > 30) break;
            
            String name = fixString(it.getDisplayName());
            String priceStr = fmtC(it.getBuyPrice());
            String arrow = "§7-";

            List<Double> hist = it.getBuyHistory();
            if (hist != null && hist.size() >= 2) {
                double currentPrice = hist.get(hist.size() - 1);
                double prevPrice = hist.get(hist.size() - 2);
                
                if (currentPrice > prevPrice + EPS) {
                    arrow = "§a▲";
                } else if (currentPrice < prevPrice - EPS) {
                    arrow = "§c▼";
                }
            }

            tickerText.append("   §f").append(name).append(" §6").append(priceStr).append("$ ").append(arrow).append("   §8|");
        }

        String fullText = tickerText.toString();
        int textW = fontRendererObj.getStringWidth(fullText);
        
        // --- Correction Scissor Clipping ---
        int scale = new ScaledResolution(mc).getScaleFactor();
        int sx = (px + 30) * scale;
        int sy = (mc.displayHeight - (y + 16) * scale);
        int sw = (w - 30) * scale;
        int sh = 16 * scale;
        
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, sw, sh);

        // Update offset
        long now = System.currentTimeMillis();
        long dt = now - lastTickTime;
        lastTickTime = now;
        tickerOffset += (dt * 0.03f);

        if (textW > 0) {
            int currentX = (int) -tickerOffset + 35; // +35 pour commencer après le label
            // Reset offset pour boucle infinie propre
            if (currentX < -textW) {
                tickerOffset -= textW;
                currentX += textW;
            }

            int renderX = currentX;
            while (renderX < w) {
                if (renderX + textW > 30) {
                    fontRendererObj.drawStringWithShadow(fullText, px + renderX, y + 4, C_WHITE);
                }
                renderX += textW;
            }
        }
        
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        
        // Label LIVE dessiné par-dessus
        drawRect(px, y, px + 30, y + 16, 0xFFCC2222);
        drawGradientRect(px + 30, y, px + 40, y + 16, 0xFF020509, 0x00020509);
        fontRendererObj.drawString("§fLIVE", px + 4, y + 4, C_WHITE);
    }

        // ─── Header ──────────────────────────────────────────────────────────────

    private void drawHeader(int mx, int my) {
        drawRect(px, py, px + pw, py + 28, C_HEADER);
        drawGradientRect(px, py, px + pw, py + 28, 0x222277EE, 0x00000000);
        
        drawRect(px + 6, py + 6, px + 22, py + 22, C_ACCENT);
        drawRect(px + 7, py + 7, px + 21, py + 21, C_PANEL);
        fontRendererObj.drawString("§b$", px + 11, py + 9, C_WHITE);

        String title = "§b§lMARKET§f§lPLACE";
        if (screen == Screen.DETAIL && selectedItem != null) {
            title += " §8/ §7" + fixString(selectedItem.getDisplayName());
        } else {
            if (screen == Screen.ITEMS && !searchMode) title += " §8/ §7" + selectedCatName;
            if (searchMode && !searchQuery.isEmpty()) title += " §8/ §7Recherche : §f\"" + fixString(searchQuery) + "\"";
        }
        fontRendererObj.drawStringWithShadow(title, px + 28, py + 10, C_WHITE);

        String bal = "§7Solde : §6" + fmtC(playerBalance) + " §e$";
        int bw = fontRendererObj.getStringWidth(bal);
        int bx2 = px + pw - bw - 50;
        
        drawRect(bx2 - 6, py + 5, bx2 + bw + 10, py + 23, 0xFF15202B);
        drawRect(bx2 - 6, py + 5, bx2 + bw + 10, py + 6, 0xFF2A4055);
        fontRendererObj.drawStringWithShadow(bal, bx2, py + 9, C_WHITE);

        int cx = px + pw - 22, cy = py + 6;
        boolean ch = inR(mx, my, cx, cy, 16, 16);
        drawRect(cx, cy, cx + 16, cy + 16, ch ? 0xCCBB2222 : 0x44AA2222);
        drawRect(cx + 1, cy + 1, cx + 15, cy + 15, ch ? 0xFFEE4444 : 0xFF220505);
        fontRendererObj.drawString("x", cx + 5, cy + 3, C_WHITE);
        hb(hbClose, cx, cy, 16, 16);
    }

    private void drawSearchBar(int mx, int my) {
        int sbX = px + pw / 2 - 120, sbY = py + 32, sbW = 240, sbH = 16;
        boolean hov = inR(mx, my, sbX, sbY, sbW, sbH);
        
        int borderColor = searchFocused ? C_ACCENT : (hov ? C_ACCENT2 : C_BORDER);
        drawRect(sbX - 1, sbY - 1, sbX + sbW + 1, sbY + sbH + 1, borderColor);
        drawRect(sbX, sbY, sbX + sbW, sbY + sbH, 0xFF050A10);
        
        fontRendererObj.drawString("§7⌕", sbX + 5, sbY + 4, C_GRAY);
        String display = searchQuery.isEmpty() && !searchFocused
                ? "§8Rechercher un item (ex: obsidienne)..."
                : "§f" + fixString(searchQuery) + (searchFocused ? "§b_" : "");
        fontRendererObj.drawString(display, sbX + 18, sbY + 4, C_WHITE);
        
        if (!searchQuery.isEmpty()) {
            fontRendererObj.drawString("§c✖", sbX + sbW - 12, sbY + 4, C_WHITE);
        }
        hb(hbSearchBox, sbX, sbY, sbW, sbH);
    }

    // ─── DASHBOARD PRINCIPAL ─────────────────────────────────────────────────

    private void drawMainDashboard(int mx, int my, int y, int h) {
        if (loadingCats) {
            drawCtr("§7Connexion au marché...", y + h / 2);
            return;
        }

        int splitX = px + (int)(pw * 0.70);
        drawCategoriesGrid(mx, my, px + 10, y + 10, splitX - px - 20, h - 20);
        drawRect(splitX, y + 10, splitX + 1, y + h - 10, C_BORDER2);
        drawMarketWatch(mx, my, splitX + 10, y + 10, px + pw - splitX - 20, h - 20);
    }

    private void drawCategoriesGrid(int mx, int my, int x, int y, int w, int h) {
        fontRendererObj.drawStringWithShadow("§9§lCATÉGORIES", x, y, C_WHITE);
        drawRect(x, y + 12, x + 40, y + 13, C_ACCENT);

        if (categories.isEmpty()) {
            fontRendererObj.drawString("§cAucune catégorie.", x, y + 30, C_GRAY);
            return;
        }

        int cols = 4;
        int gap = 8;
        int btnW = (w - (cols - 1) * gap) / cols;
        int btnH = 55;
        int startY = y + 25;

        for (int i = 0; i < categories.size() && i < MAX_CATS; i++) {
            ShopPacketHandler.ShopCategory cat = categories.get(i);
            int col = i % cols, row = i / cols;
            int bx = x + col * (btnW + gap);
            int by = startY + row * (btnH + gap);
            boolean hov = inR(mx, my, bx, by, btnW, btnH);

            drawRect(bx, by, bx + btnW, by + btnH, hov ? 0xFF162436 : 0xFF0C1520);
            
            String rawCat = fixString(cat.getName());
            int catCol = catColor(rawCat);
            drawRect(bx, by + btnH - 2, bx + btnW, by + btnH, catCol);
            
            if (hov) {
                drawRect(bx, by, bx + btnW, by + 1, C_ACCENT2);
                drawRect(bx, by, bx + 1, by + btnH, C_ACCENT2);
                drawRect(bx + btnW - 1, by, bx + btnW, by + btnH, C_ACCENT2);
                drawGradientRect(bx, by, bx + btnW, by + btnH, 0x15FFFFFF, 0x00000000);
            } else {
                drawRect(bx, by, bx + btnW, by + 1, C_BORDER2);
                drawRect(bx, by, bx + 1, by + btnH, C_BORDER2);
                drawRect(bx + btnW - 1, by, bx + btnW, by + btnH, C_BORDER2);
            }

            ItemStack icon = resolveIcon(cat.getIconItem());
            boolean rendered = renderIconSafe(icon, bx + (btnW - 16) / 2, by + 10);
            if (!rendered) {
                drawRect(bx + btnW/2 - 10, by + 10, bx + btnW/2 + 10, by + 30, catCol & 0x55FFFFFF);
                String letter = rawCat.substring(0, 1).toUpperCase();
                drawCtrAt(letter, bx + btnW/2, by + 14);
            }

            String nm = normalizeCatName(rawCat);
            int txtCol = hov ? C_WHITE : C_LGRAY;
            drawCtrAt(nm, bx + btnW / 2, by + 38, txtCol);
            
            hb(hbCats[i], bx, by, btnW, btnH);
        }
    }

    private void drawMarketWatch(int mx, int my, int x, int y, int w, int h) {
        fontRendererObj.drawStringWithShadow("§6§lAPERÇU DU MARCHÉ", x, y, C_WHITE);
        drawRect(x, y + 12, x + 60, y + 13, C_GOLD);

        // Section 1 : Top DEMANDE (Achats)
        int listY = y + 25;
        fontRendererObj.drawString("§aTop Demande (Achat)", x, listY, C_GREEN);
        drawRect(x + 100, listY + 4, x + w, listY + 5, 0xFF335533);
        listY += 12;

        List<ShopPacketHandler.MarketEntry> buys = new ArrayList<>(topBought);
        Collections.sort(buys, (a, b) -> Long.compare(b.getVolume(), a.getVolume()));
        
        int entryH = 22;
        int maxItems = 3;
        
        for (int i = 0; i < Math.min(maxItems, buys.size()); i++) {
            ShopPacketHandler.MarketEntry e = buys.get(i);
            drawMarketEntry(e, x, listY + i * (entryH + 2), w, entryH, true);
        }
        
        // Section 2 : Top OFFRE (Ventes)
        int sellY = listY + maxItems * (entryH + 2) + 10;
        fontRendererObj.drawString("§cTop Offre (Vente)", x, sellY, C_RED);
        drawRect(x + 100, sellY + 4, x + w, sellY + 5, 0xFF553333);
        sellY += 12;
        
        List<ShopPacketHandler.MarketEntry> sells = new ArrayList<>(topSold);
        Collections.sort(sells, (a, b) -> Long.compare(b.getVolume(), a.getVolume()));
        
        for (int i = 0; i < Math.min(maxItems, sells.size()); i++) {
            ShopPacketHandler.MarketEntry e = sells.get(i);
            drawMarketEntry(e, x, sellY + i * (entryH + 2), w, entryH, false);
        }
    }
    
    private void drawMarketEntry(ShopPacketHandler.MarketEntry e, int x, int y, int w, int h, boolean isBuy) {
        drawRect(x, y, x + w, y + h, 0xFF09111A);
        renderIconSafe(resolveIcon(e.getMinecraftItem()), x + 2, y + 3);
        
        fontRendererObj.drawString(fixString(e.getDisplayName()), x + 22, y + 3, C_WHITE);
        String price = (isBuy ? "§6" : "§a") + fmtC(isBuy ? e.getBuyPrice() : e.getSellPrice()) + "$";
        fontRendererObj.drawString(price, x + 22, y + 12, C_GRAY);
        
        String vol = "§7Vol: §f" + e.getVolume();
        fontRendererObj.drawString(vol, x + w - fontRendererObj.getStringWidth(vol) - 2, y + 7, C_GRAY);
    }

    // ─── LISTE ITEMS ─────────────────────────────────────────────────────────

    private void drawItems(int mx, int my, int y, int h) {
        drawBack(mx, my, px + 10, y);
        fontRendererObj.drawStringWithShadow(
                "§e§l" + selectedCatName + " §r§8— §7" + filtered.size() + " article(s)",
                px + 75, y + 3, C_WHITE);
        drawItemTable(mx, my, px + 10, y + 20, pw - 20, h - 20);
    }

    private void drawSearchResults(int mx, int my, int y, int h) {
        String qLabel = filtered.isEmpty()
                ? "§cAucun résultat pour §f\"" + fixString(searchQuery) + "\""
                : "§7" + filtered.size() + " résultat(s) pour §f\"" + fixString(searchQuery) + "\"";
        fontRendererObj.drawStringWithShadow(qLabel, px + 12, y + 2, C_WHITE);
        if (filtered.isEmpty()) return;
        drawItemTable(mx, my, px + 10, y + 16, pw - 20, h - 16);
    }

    private void drawItemTable(int mx, int my, int tx, int ty, int tw, int th) {
        drawRect(tx, ty, tx + tw, ty + 16, 0xFF0C1520);
        drawRect(tx, ty + 15, tx + tw, ty + 16, C_BORDER);
        
        fontRendererObj.drawString("ITEM", tx + 36, ty + 4, 0xFF557799);
        fontRendererObj.drawString("ACHAT", tx + tw - 260, ty + 4, 0xFF557799);
        fontRendererObj.drawString("VENTE", tx + tw - 180, ty + 4, 0xFF557799);
        fontRendererObj.drawString("EVOL. 7J", tx + tw - 105, ty + 4, 0xFF557799);
        fontRendererObj.drawString("ETAT", tx + tw - 40, ty + 4, 0xFF557799);

        int listTop = ty + 18;
        int listH = th - 18;
        int maxR = Math.max(1, listH / ROW_H);

        if (loadingItems) {
            drawCtr("§7Chargement des cours...", listTop + 50);
            return;
        }
        if (filtered.isEmpty()) {
            drawCtr(searchMode ? "§cAucun résultat." : "§cAucun article dans cette catégorie.", listTop + 50);
            return;
        }

        int maxScroll = Math.max(0, filtered.size() - maxR);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = 0; i < maxR; i++) {
            int idx = i + scroll;
            if (idx >= filtered.size()) break;
            ShopPacketHandler.ShopItem it = filtered.get(idx);
            int ry = listTop + i * ROW_H;
            boolean hov = inR(mx, my, tx, ry, tw - 12, ROW_H - 2);

            int bgCol = (idx % 2 == 0) ? 0x00000000 : 0x08FFFFFF;
            if (hov) bgCol = 0x202277EE;
            drawRect(tx, ry, tx + tw - 12, ry + ROW_H - 2, 0xFF05090E);
            drawRect(tx, ry, tx + tw - 12, ry + ROW_H - 2, bgCol);

            String itCat = fixString(it.getCategory());
            drawRect(tx, ry, tx + 2, ry + ROW_H - 2, catColor(itCat));

            ItemStack icon = resolveIcon(it.getMinecraftItem());
            boolean rendered = renderIconSafe(icon, tx + 6, ry + (ROW_H - 18) / 2);
            if (!rendered) {
                int fc = catColor(itCat);
                drawRect(tx + 6, ry + 6, tx + 22, ry + 22, fc);
            }

            String nm = fixString(it.getDisplayName());
            fontRendererObj.drawStringWithShadow("§f" + nm, tx + 36, ry + 9, C_WHITE);
            
            fontRendererObj.drawString("§6" + fmtC(it.getBuyPrice()) + "$", tx + tw - 260, ry + 9, C_GOLD);
            fontRendererObj.drawString("§a" + fmtC(it.getSellPrice()) + "$", tx + tw - 180, ry + 9, C_GREEN);

            String chart = it.getAsciiChart();
            if (chart != null) {
                if (chart.contains("stable") || chart.length() < 3) chart = "§8━━━━";
                else if (chart.length() > 10) chart = chart.substring(0, 10);
                fontRendererObj.drawString(chart, tx + tw - 105, ry + 9, C_WHITE);
            }

            String status = "§a●";
            if (it.isFrozen()) status = "§b❄";
            else if (it.getCeil() > 0 || it.getFloor() > 0) status = "§e↔";
            fontRendererObj.drawString(status, tx + tw - 36, ry + 9, C_WHITE);

            hb(hbRows[i], tx, ry, tw - 12, ROW_H - 2);

            if (hov) {
                List<String> tip = new ArrayList<>();
                tip.add("§f" + nm);
                tip.add("§7Catégorie: §b" + normalizeCatName(itCat));
                tip.add("§8Clic pour voir le graphique et échanger");
                tooltipLines = tip;
                tooltipX = mx;
                tooltipY = my;
            }
        }

        drawScrollbar(tx + tw - 8, listTop, 6, listH, scroll, filtered.size(), maxR, mx, my);
    }

    // ─── DÉTAIL ──────────────────────────────────────────────────────────────

    private void drawDetail(int mx, int my, int y, int h) {
        if (selectedItem == null) {
            screen = Screen.ITEMS;
            return;
        }
        drawBack(mx, my, px + 10, y);

        int infoX = px + 10, infoW = 200, infoY = y + 20;
        int infoH = h - 20;
        int rightX = infoX + infoW + 10;
        int rightW = pw - infoW - 30;

        int graphH = (int) (infoH * 0.55);
        int top3Y = infoY + graphH + 10;
        int top3H = infoH - graphH - 10;

        drawRect(infoX, infoY, infoX + infoW, infoY + infoH, C_PANEL);
        drawGradientRect(infoX, infoY, infoX + infoW, infoY + infoH, 0x08FFFFFF, 0x00000000);
        drawRect(infoX, infoY, infoX + infoW, infoY + 1, C_BORDER2);
        drawRect(infoX, infoY + infoH - 1, infoX + infoW, infoY + infoH, C_BORDER2);
        drawRect(infoX, infoY, infoX + 1, infoY + infoH, C_BORDER2);
        drawRect(infoX + infoW - 1, infoY, infoX + infoW, infoY + infoH, C_BORDER2);

        String selCat = fixString(selectedItem.getCategory());
        int headerH = 50;
        int catCol = catColor(selCat);
        drawRect(infoX + 1, infoY + 1, infoX + infoW - 1, infoY + headerH, catCol & 0x44FFFFFF);
        
        ItemStack bigIcon = resolveIcon(selectedItem.getMinecraftItem());
        renderIconSafe(bigIcon, infoX + infoW / 2 - 8, infoY + 8);
        
        String nm = fixString(selectedItem.getDisplayName());
        drawCtrAt("§f§l" + nm, infoX + infoW / 2, infoY + 28);
        drawCtrAt("§8" + normalizeCatName(selCat), infoX + infoW / 2, infoY + 38);

        int ty = infoY + headerH + 10;
        drawInfoRow(infoX, ty, "Prix Achat", "§6" + fmtC(selectedItem.getBuyPrice()) + "$", infoW); ty += 14;
        drawInfoRow(infoX, ty, "Prix Vente", "§a" + fmtC(selectedItem.getSellPrice()) + "$", infoW); ty += 14;
        drawSep(infoX + 10, ty, infoW - 20); ty += 8;
        
        drawInfoRow(infoX, ty, "Vol. Achat", "§f" + selectedItem.getTotalBuyVolume(), infoW); ty += 14;
        drawInfoRow(infoX, ty, "Vol. Vente", "§f" + selectedItem.getTotalSellVolume(), infoW); ty += 14;
        
        int tabY = infoY + infoH - 110;
        int tabW = (infoW - 4) / 2;
        drawDetailTab(mx, my, infoX + 1, tabY, tabW, "§aACHETER", detailTab == DetailTab.BUY, true, hbTabBuy);
        drawDetailTab(mx, my, infoX + tabW + 3, tabY, tabW, "§cVENDRE", detailTab == DetailTab.SELL, false, hbTabSell);

        int actY = tabY + 20;
        drawRect(infoX + 1, actY, infoX + infoW - 1, infoY + infoH - 1, 0xFF080E15);
        
        if (detailTab == DetailTab.BUY) drawBuyPanel(mx, my, infoX, actY, infoW);
        else drawSellPanel(mx, my, infoX, actY, infoW);

        if (detailTab == DetailTab.BUY) {
            drawChart(rightX, infoY, rightW, graphH, mx, my,
                    selectedItem.getBuyHistory(), "§6COURS ACHAT (7J)",
                    selectedItem.getBuyPrice(), C_GOLD, 0xFF33AA66,
                    selectedItem.getFloor(), selectedItem.getCeil());
            drawTop3Panel(rightX, top3Y, rightW, top3H, mx, my, true);
        } else {
            drawChart(rightX, infoY, rightW, graphH, mx, my,
                    selectedItem.getSellHistory(), "§aHISTORIQUE VENTE",
                    selectedItem.getSellPrice(), C_GREEN, 0xFF2266AA, 0, 0);
            drawTop3Panel(rightX, top3Y, rightW, top3H, mx, my, false);
        }
    }

    private void drawBack(int mx, int my, int x, int y) {
        boolean hov = inR(mx, my, x, y, 60, 16);
        drawRect(x, y, x + 60, y + 16, hov ? C_ACCENT : 0xFF0D1520);
        drawRect(x, y, x + 60, y + 16, hov ? 0x44FFFFFF : 0x00000000);
        fontRendererObj.drawStringWithShadow("§f« Retour", x + 10, y + 4, C_WHITE);
        hb(hbBack, x, y, 60, 16);
    }

    // ─── HELPERS UI ──────────────────────────────────────────────────────────

    private void drawInfoRow(int panelX, int y, String lbl, String val, int panelW) {
        fontRendererObj.drawString("§7" + lbl, panelX + 10, y, C_GRAY);
        fontRendererObj.drawStringWithShadow(val, panelX + panelW - 10 - fontRendererObj.getStringWidth(val), y, C_WHITE);
    }

    private void drawDetailTab(int mx, int my, int x, int y, int w, String label, boolean active, boolean isBuy, int[] hbArr) {
        int bg = active ? (isBuy ? 0xFF0C301E : 0xFF300C0C) : 0xFF050A10;
        int border = active ? (isBuy ? C_GREEN : C_RED) : C_BORDER2;
        
        drawRect(x, y, x + w, y + 18, bg);
        if (active) drawRect(x, y, x + w, y + 2, border);
        else drawRect(x, y + 17, x + w, y + 18, C_BORDER2);
        
        drawCtrAt(label, x + w/2, y + 5, active ? C_WHITE : C_GRAY);
        hb(hbArr, x, y, w, 18);
    }

    private void drawBuyPanel(int mx, int my, int x, int y, int w) {
        int qy = y + 8;
        drawQtyRow(mx, my, x + 10, qy, w - 20, buyQty, selectedItem.getMaxStack(), true);
        qy += 20;
        drawQtyInput(mx, my, x + 10, qy, w - 20, buyQty, true);
        qy += 18;

        double total = selectedItem.getBuyPrice() / 100.0 * buyQty;
        drawCtrAt("§7Total : §6" + FMT.format(total) + " §e$", x + w / 2, qy);
        qy += 14;
        
        boolean canBuy = playerBalance >= (long) (selectedItem.getBuyPrice() * (long) buyQty);
        int bx = x + 10, bw = w - 20, bh = 18;
        boolean hov = inR(mx, my, bx, qy, bw, bh);
        
        int btnCol = canBuy ? 0xFF104020 : 0xFF202020;
        if (hov && canBuy) btnCol = 0xFF1A5530;
        drawRect(bx, qy, bx + bw, qy + bh, btnCol);
        drawRect(bx, qy, bx + bw, qy + 1, canBuy ? C_GREEN : 0xFF555555);
        drawCtrAt("CONFIRMER ACHAT", bx + bw / 2, qy + 5, canBuy ? C_WHITE : C_GRAY);
        
        hb(hbAction, bx, qy, bw, bh);
    }

    private void drawSellPanel(int mx, int my, int x, int y, int w) {
        int qy = y + 8;
        drawQtyRow(mx, my, x + 10, qy, w - 20, sellQty, 64, false);
        qy += 20;
        drawQtyInput(mx, my, x + 10, qy, w - 20, sellQty, false);
        qy += 18;

        double gains = selectedItem.getSellPrice() / 100.0 * sellQty;
        drawCtrAt("§7Gain : §a" + FMT.format(gains) + " §e$", x + w / 2, qy);
        qy += 14;

        int gap = 4;
        int halfW = (w - 20 - gap) / 2;
        int bx1 = x + 10, bx2 = bx1 + halfW + gap, bh = 18;

        boolean hov1 = inR(mx, my, bx1, qy, halfW, bh);
        drawRect(bx1, qy, bx1+halfW, qy+bh, hov1 ? 0xFF501010 : 0xFF300A0A);
        drawRect(bx1, qy, bx1+halfW, qy+1, C_RED);
        drawCtrAt("VENDRE", bx1+halfW/2, qy+5, C_WHITE);
        hb(hbAction, bx1, qy, halfW, bh);

        boolean hov2 = inR(mx, my, bx2, qy, halfW, bh);
        drawRect(bx2, qy, bx2+halfW, qy+bh, hov2 ? 0xFF604010 : 0xFF40250A);
        drawRect(bx2, qy, bx2+halfW, qy+1, C_ORANGE);
        drawCtrAt("TOUT", bx2+halfW/2, qy+5, C_ORANGE);
        hb(hbSellAll, bx2, qy, halfW, bh);
    }

    private void drawQtyInput(int mx, int my, int x, int y, int w, int currentQty, boolean isBuy) {
        boolean focused = qtyInputFocused && qtyInputIsBuy == isBuy;
        drawRect(x, y, x + w, y + 14, 0xFF030508);
        drawRect(x, y + 13, x + w, y + 14, focused ? (isBuy ? C_GREEN : C_RED) : C_BORDER2);
        
        String txt = focused ? (qtyInputStr.isEmpty() ? "" : qtyInputStr) + "_" : String.valueOf(currentQty);
        fontRendererObj.drawString("§7Saisie: §f" + txt, x + 4, y + 3, C_WHITE);
        
        if (inR(mx, my, x, y, w, 14) && !focused) {
            tooltipLines = new ArrayList<>();
            tooltipLines.add("§7Cliquez pour écrire");
            tooltipX = mx; tooltipY = my;
        }
        hb(hbQtyInput, x, y, w, 14);
    }

    private void drawQtyRow(int mx, int my, int x, int y, int w, int qty, int maxQ, boolean isBuy) {
        int bs = 16, sp = 3;
        int totalW = 5 * bs + 4 * sp;
        int sx = x + (w - totalW) / 2;
        
        drawSmBtn(mx, my, sx, y, bs, "-10", hbQM10);
        drawSmBtn(mx, my, sx + bs + sp, y, bs, "-1", hbQM1);
        
        drawRect(sx + 2*(bs+sp), y, sx + 2*(bs+sp) + bs, y + bs, 0xFF050A10);
        drawCtrAt(""+qty, sx + 2*(bs+sp) + bs/2, y + 4, C_WHITE);
        
        drawSmBtn(mx, my, sx + 3*(bs+sp), y, bs, "+1", hbQP1);
        drawSmBtn(mx, my, sx + 4*(bs+sp), y, bs, "ALL", hbQMax);
    }

    private void drawSmBtn(int mx, int my, int x, int y, int sz, String lbl, int[] arr) {
        boolean hov = inR(mx, my, x, y, sz, sz);
        drawRect(x, y, x + sz, y + sz, hov ? C_BORDER : 0xFF0C1520);
        drawRect(x, y, x + sz, y + 1, hov ? C_ACCENT2 : C_BORDER2);
        drawCtrAt(lbl, x + sz/2, y + 4, hov ? C_WHITE : C_GRAY);
        hb(arr, x, y, sz, sz);
    }

    private void drawChart(int gx, int gy, int gw, int gh, int mx, int my,
                           List<Double> hist, String title, long currentVal, int lineColor, int fillColor,
                           long floor, long ceil) {
        drawRect(gx, gy, gx + gw, gy + gh, 0xFF080C12);
        drawRect(gx, gy, gx + gw, gy + 1, lineColor);
        
        fontRendererObj.drawString(title, gx + 6, gy + 5, C_WHITE);
        String price = fmtC(currentVal) + "$";
        fontRendererObj.drawString(price, gx + gw - fontRendererObj.getStringWidth(price) - 5, gy + 5, lineColor);

        int cx = gx + 5, cy = gy + 20, cw = gw - 10, ch = gh - 25;
        drawRect(cx, cy, cx + cw, cy + ch, 0xFF020406);

        if (hist == null || hist.size() < 2) {
            drawCtrAt("§8Données insuffisantes", cx + cw / 2, cy + ch / 2 - 4);
            return;
        }

        double minV = hist.stream().mapToDouble(d->d).min().orElse(0);
        double maxV = hist.stream().mapToDouble(d->d).max().orElse(1);
        if (maxV == minV) maxV = minV * 1.1 + 0.01;
        double drange = maxV - minV; if (drange==0) drange=1;

        for(int i=1; i<4; i++) {
            int ly = cy + (ch * i / 4);
            drawRect(cx, ly, cx + cw, ly + 1, 0x11FFFFFF);
        }

        int n = hist.size();
        int[] xs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = cx + (int) ((double) i / (n - 1) * (cw - 2)) + 1;
            ys[i] = cy + ch - (int) (((hist.get(i) - minV) / drange) * (ch - 4)) - 2;
        }

        int fill = (lineColor & 0x00FFFFFF) | 0x22000000;
        for (int i = 0; i < n - 1; i++) {
            int x1 = xs[i], x2 = xs[i+1];
            int y1 = ys[i];
            if (x2 > x1) {
                drawRect(x1, y1, x1 + (x2-x1)+1, cy + ch, fill);
            }
        }

        for (int i = 0; i < n - 1; i++)
            drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1], lineColor, 1);

        drawRect(xs[n-1]-1, ys[n-1]-1, xs[n-1]+2, ys[n-1]+2, C_WHITE);

        // Interaction souris sur le graphique (Crosshair)
        if (inR(mx, my, cx, cy, cw, ch)) {
            int closest = -1;
            int dist = 999;
            for (int i = 0; i < n; i++) {
                int d = Math.abs(mx - xs[i]);
                if (d < dist) { dist = d; closest = i; }
            }
            
            if (closest != -1) {
                int px = xs[closest];
                int py = ys[closest];
                
                // Ligne verticale réticule
                drawRect(px, cy, px + 1, cy + ch, 0x44FFFFFF);
                drawRect(px - 2, py - 2, px + 3, py + 3, lineColor);
                
                // Tooltip dynamique
                List<String> chartTip = new ArrayList<>();
                chartTip.add("§6" + FMT.format(hist.get(closest)) + " $");
                if (closest > 0) {
                    double delta = hist.get(closest) - hist.get(closest-1);
                    chartTip.add((delta >= 0 ? "§a+" : "§c") + FMT.format(delta) + " $");
                } else {
                    chartTip.add("§7Début historique");
                }
                tooltipLines = chartTip;
                tooltipX = mx;
                tooltipY = my;
            }
        }
    }

    private void drawTop3Panel(int gx, int gy, int gw, int gh, int mx, int my, boolean showBought) {
        drawRect(gx, gy, gx + gw, gy + gh, 0xFF080C12);
        int col = showBought ? C_GOLD : C_RED;
        drawRect(gx, gy, gx + gw, gy + 1, col);
        
        fontRendererObj.drawString(showBought ? "Top Achats" : "Top Ventes", gx + 6, gy + 5, C_WHITE);

        List<ShopPacketHandler.MarketEntry> entries = showBought ? topBought : topSold;
        if (entries.isEmpty()) {
            drawCtrAt("§8Vide", gx + gw/2, gy + 20);
            return;
        }

        int rowH = 22;
        int y = gy + 18;
        for (int i = 0; i < Math.min(3, entries.size()); i++) {
            ShopPacketHandler.MarketEntry e = entries.get(i);
            drawRect(gx + 2, y, gx + gw - 2, y + rowH, 0xFF0E1620);
            
            renderIconSafe(resolveIcon(e.getMinecraftItem()), gx + 4, y + 3);
            fontRendererObj.drawString(fixString(e.getDisplayName()), gx + 22, y + 3, C_WHITE);
            fontRendererObj.drawString("§7x" + e.getVolume(), gx + 22, y + 12, C_GRAY);
            
            String p = fmtC(showBought ? e.getBuyPrice() : e.getSellPrice()) + "$";
            fontRendererObj.drawString(p, gx + gw - fontRendererObj.getStringWidth(p) - 4, y + 8, col);
            
            y += rowH + 2;
        }
    }

    private void drawLine(int x1, int y1, int x2, int y2, int color, int thick) {
         int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
         int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
         int err = dx - dy, cx = x1, cy2 = y1;
         while (true) {
             drawRect(cx, cy2, cx + 1, cy2 + 1, color);
             if (cx == x2 && cy2 == y2) break;
             int e2 = 2 * err;
             if (e2 > -dy) { err -= dy; cx += sx; }
             if (e2 < dx) { err += dx; cy2 += sy; }
         }
     }

     private String fmtC(long centimes) { return FMT.format(centimes / 100.0); }
     private void drawCtr(String text, int cy2) { fontRendererObj.drawStringWithShadow(text, px + (pw - fontRendererObj.getStringWidth(text)) / 2, cy2, C_GRAY); }
     private void drawCtrAt(String text, int cx2, int cy2) { fontRendererObj.drawStringWithShadow(text, cx2 - fontRendererObj.getStringWidth(text) / 2, cy2, C_WHITE); }
     private void drawCtrAt(String text, int cx2, int cy2, int color) { fontRendererObj.drawStringWithShadow(text, cx2 - fontRendererObj.getStringWidth(text) / 2, cy2, color); }
     private void drawSep(int x, int y, int w) { drawRect(x, y, x + w, y + 1, 0xFF1A3A6A); }
     private boolean inR(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
     private boolean inH(int mx, int my, int[] h) { return h[2] > 0 && inR(mx, my, h[0], h[1], h[2], h[3]); }
     private void hb(int[] arr, int x, int y, int w, int h) { arr[0] = x; arr[1] = y; arr[2] = w; arr[3] = h; }

     private String fixString(String s) {
         if (s == null) return "";
         boolean broken = false;
         int len = s.length();
         for (int i=0; i<len; i++) { if (s.charAt(i) == 'Ã') { broken = true; break; } }
         if (!broken) return s;
         try { return new String(s.getBytes("ISO-8859-1"), StandardCharsets.UTF_8); } catch (Exception e) { return s; }
     }

     private String normalizeCatName(String cat) {
         if (cat == null) return "";
         String c = fixString(cat).toLowerCase().trim();
         if (c.contains("minera") || c.contains("minéra")) return "Minerais";
         if (c.contains("elevage") || c.contains("élevage")) return "Élevage";
         if (c.contains("deco") || c.contains("déco")) return "Décoration";
         if (c.contains("modd")) return "Moddé";
         if (c.contains("agri")) return "Agriculture";
         return fixString(cat);
     }

     private ItemStack resolveIcon(String name) {
         if (name == null || name.isEmpty()) return null;
         try {
             // MAPPING DE NOM -> ID NUMERIQUE (1.8.9 Legacy IDs)
             String n = name.toLowerCase().replace(" ", "_");
             String metaStr = null;
             if (n.contains(":")) {
                 String[] split = n.split(":");
                 n = split[0];
                 metaStr = split[1];
             }
             int meta = (metaStr != null) ? parseMeta(metaStr) : 0;

             // Charbon de bois (263:1)
             if (n.contains("charcoal") || n.contains("charbon_de_bois")) return new ItemStack(Items.coal, 1, 1);

             // Saumon cuit (350:1)
             if (n.contains("cooked_salmon") || (n.contains("salmon") && n.contains("cooked"))) return new ItemStack(Items.cooked_fish, 1, 1);

             // Stone Bricks (98)
             if (n.contains("stonebrick") || n.contains("stone_brick")) {
                 if (n.contains("mossy")) return new ItemStack(Blocks.stonebrick, 1, 1);
                 if (n.contains("crack")) return new ItemStack(Blocks.stonebrick, 1, 2);
                 if (n.contains("chisel")) return new ItemStack(Blocks.stonebrick, 1, 3);
                 return new ItemStack(Blocks.stonebrick);
             }

             // Cobble Stairs (67)
             if (n.contains("cobble") && n.contains("stair")) return new ItemStack(Blocks.stone_stairs);

             // Terracotta / Stained Clay (159)
             if (n.contains("stained_hardened_clay") || n.contains("terracotta") || n.contains("terre_cuite")) {
                 if (meta == 0) meta = findColorMeta(n);
                 return new ItemStack(Blocks.stained_hardened_clay, 1, meta);
             }

             // Hardened Clay (172)
             if (n.equals("hardened_clay")) return new ItemStack(Blocks.hardened_clay);

             // Verre (20 ou 95)
             // Correction spécifique pour 1.8.9 : "glass_pane" (102) vs "stained_glass_pane" (160)
             if (n.contains("glass") || n.contains("verre")) {
                 if (meta == 0) meta = findColorMeta(n);
                 boolean pane = n.contains("pane") || n.contains("vitre");

                 boolean wantStained = meta != 0 || n.contains("stained") || n.contains("teinte") || containsColorWord(n);
                 if (pane) {
                     if (wantStained) return new ItemStack(Blocks.stained_glass_pane, 1, meta);
                     return new ItemStack(Blocks.glass_pane);
                 } else {
                     if (wantStained) return new ItemStack(Blocks.stained_glass, 1, meta);
                     return new ItemStack(Blocks.glass);
                 }
             }

             // Quartz (155)
             if (n.contains("quartz")) {
                 if (n.contains("ore")) return new ItemStack(Blocks.quartz_ore);
                 if (n.contains("pillar")) return new ItemStack(Blocks.quartz_block, 1, 2);
                 if (n.contains("chisel")) return new ItemStack(Blocks.quartz_block, 1, 1);
                 return new ItemStack(Blocks.quartz_block);
             }

             // Bois (Logs: 17/162, Planks: 5)
             if (n.contains("log") || n.contains("plank") || n.contains("planks") || n.contains("wood") || n.contains("bois")) {
                 boolean isLog = n.contains("log") || n.contains("buche");
                 // Si une meta explicite a été fournie via name:meta, on la respecte
                 int woodMeta = (metaStr != null) ? meta : 0;

                 // Détection stricte uniquement si aucune méta explicite
                 if (woodMeta == 0) {
                     if (n.contains("dark_oak") || n.contains("sombre") || n.contains("darkoak") || n.contains("chene_sombre")) woodMeta = 5;
                     else if (n.contains("acacia")) woodMeta = 4;
                     else if (n.contains("jungle") || n.contains("acajou")) woodMeta = 3;
                     else if (n.contains("birch") || n.contains("bouleau")) woodMeta = 2;
                     else if (n.contains("spruce") || n.contains("sapin")) woodMeta = 1;
                     else if (n.contains("oak") || n.contains("chene")) woodMeta = 0;
                 }

                 if (isLog) {
                     if (woodMeta >= 4) return new ItemStack(Blocks.log2, 1, Math.max(0, woodMeta - 4)); // Acacia/DarkOak
                     return new ItemStack(Blocks.log, 1, woodMeta);
                 } else {
                     return new ItemStack(Blocks.planks, 1, Math.max(0, woodMeta));
                 }
             }

             // Fallback générique
             Item it = Item.getByNameOrId(n);
             if (it != null) return new ItemStack(it, 1, meta);
             Block b = Block.getBlockFromName(n);
             if (b != null) return new ItemStack(b, 1, meta);

         } catch (Exception e) {}
         return null;
     }

     private int findColorMeta(String s) {
         for (String color : new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"}) {
             if (s.contains(color)) return parseMeta(color);
         }
         return 0;
     }

     private int parseMeta(String s) {
         if (s == null) return 0;
         try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { }
         switch (s.toLowerCase().replace(' ', '_')) {
             case "white": case "blanc": return 0;
             case "orange": return 1;
             case "magenta": return 2;
             case "light_blue": case "bleu_clair": return 3;
             case "yellow": case "jaune": return 4;
             case "lime": case "vert_clair": return 5;
             case "pink": case "rose": return 6;
             case "gray": case "grey": case "gris": return 7;
             case "silver": case "light_gray": case "gris_clair": return 8;
             case "cyan": return 9;
             case "purple": case "violet": return 10;
             case "blue": case "bleu": return 11;
             case "brown": case "marron": return 12;
             case "green": case "vert": return 13;
             case "red": case "rouge": return 14;
             case "black": case "noir": return 15;
         }
         return 0;
     }

     private int catColor(String cat) {
         if (cat == null) return 0xFF445566;
         String c = fixString(cat).toLowerCase();
         if (c.contains("agri")) return 0xFF3DB35E;
         if (c.contains("elevage") || c.contains("élevage")) return 0xFFCC8833;
         if (c.contains("bois")) return 0xFF8B5E3C;
         if (c.contains("terrain")) return 0xFF888888;
         if (c.contains("minera") || c.contains("minéra")) return 0xFF4488CC;
         if (c.contains("nether")) return 0xFFCC4422;
         if (c.contains("redstone")) return 0xFFCC2222;
         if (c.contains("deco") || c.contains("déco")) return 0xFF8866AA;
         if (c.contains("divers")) return 0xFF5588AA;
         if (c.contains("modd")) return 0xFFE0AA22;
         return 0xFF445566;
     }

     @Override
     public boolean doesGuiPauseGame() { return false; }

     @Override
     protected void mouseClicked(int mx, int my, int btn) throws IOException {
         super.mouseClicked(mx, my, btn);
         if (btn != 0) return;
         if (inH(mx, my, hbClose)) { mc.displayGuiScreen(null); return; }
         if (inH(mx, my, hbBack)) {
             if (searchMode) { searchMode = false; searchQuery = ""; applyFilter(); }
             else if (screen == Screen.DETAIL) { screen = Screen.ITEMS; selectedItem = null; loadingItems = true; ShopPacketHandler.requestItems(selectedCatId); }
             else { screen = Screen.CATEGORIES; }
             return;
         }
         if (lastScrollTotal > 0 && inR(mx, my, lastScrollX, lastScrollY, lastScrollW, lastScrollH)) { draggingScroll = true; return; }
         if (inH(mx, my, hbSearchBox)) { int sbX = px + pw/2 - 120; if (!searchQuery.isEmpty() && mx >= sbX + 240 - 12) { searchQuery = ""; searchMode = false; applyFilter(); } else searchFocused = true; return; } else searchFocused = false;

         if (screen == Screen.CATEGORIES) {
             for (int i = 0; i < categories.size() && i < MAX_CATS; i++) {
                 if (inH(mx, my, hbCats[i])) { selectedCatId = categories.get(i).getId(); selectedCatName = normalizeCatName(categories.get(i).getName()); screen = Screen.ITEMS; loadingItems = true; scroll = 0; ShopPacketHandler.requestItems(selectedCatId); applyFilter(); return; }
             }
         } else if (screen == Screen.ITEMS || (searchMode && !searchQuery.isEmpty())) {
             for (int i = 0; i < MAX_ROWS; i++) { int idx = i + scroll; if (idx >= filtered.size()) break; if (inH(mx, my, hbRows[i])) { openDetail(filtered.get(idx)); return; } }
         } else if (screen == Screen.DETAIL) {
             if (inH(mx, my, hbTabBuy)) detailTab = DetailTab.BUY;
             else if (inH(mx, my, hbTabSell)) detailTab = DetailTab.SELL;
             else if (inH(mx, my, hbQtyInput)) { qtyInputFocused = true; qtyInputIsBuy = (detailTab == DetailTab.BUY); qtyInputStr=""; }
             else if (qtyInputFocused) { qtyInputFocused = false; applyQtyInput(); }

             int q = (detailTab == DetailTab.BUY) ? buyQty : sellQty;
             if (inH(mx, my, hbQM10)) q = Math.max(1, q - 10);
             else if (inH(mx, my, hbQM1)) q = Math.max(1, q - 1);
             else if (inH(mx, my, hbQP1)) q = Math.min((detailTab==DetailTab.BUY ? selectedItem.getMaxStack() : 64), q + 1);
             else if (inH(mx, my, hbQP10)) q = Math.min((detailTab==DetailTab.BUY ? selectedItem.getMaxStack() : 64), q + 10);
             else if (inH(mx, my, hbQMax)) q = (detailTab==DetailTab.BUY ? selectedItem.getMaxStack() : 64);
             else if (inH(mx, my, hbAction)) { if (detailTab == DetailTab.BUY) ShopPacketHandler.buyItem(selectedItem.getId(), q); else ShopPacketHandler.sellItem(selectedItem.getId(), q); }
             else if (inH(mx, my, hbSellAll)) ShopPacketHandler.sellAll(selectedItem.getId());
             if (detailTab == DetailTab.BUY) buyQty = q; else sellQty = q;
         }
     }

     private void openDetail(ShopPacketHandler.ShopItem it) { selectedItem = it; screen = Screen.DETAIL; detailTab = DetailTab.BUY; buyQty = 1; sellQty = 1; searchMode = false; ShopPacketHandler.requestItemDetail(it.getId()); }
     private void applyQtyInput() { if (qtyInputStr.isEmpty()) return; try { int val = Integer.parseInt(qtyInputStr); if (val < 1) val = 1; if (qtyInputIsBuy) buyQty = Math.min(val, selectedItem != null ? selectedItem.getMaxStack() : 64); else sellQty = Math.min(val, 64); } catch (Exception e) {} qtyInputStr = ""; }

     @Override
     protected void keyTyped(char ch, int key) throws IOException {
         super.keyTyped(ch, key);
         if (qtyInputFocused) { if (key==1) { qtyInputFocused=false; qtyInputStr=""; } else if (key==28||key==156) { applyQtyInput(); qtyInputFocused=false; } else if (key==14 && !qtyInputStr.isEmpty()) qtyInputStr = qtyInputStr.substring(0, qtyInputStr.length()-1); else if (ch>='0' && ch<='9' && qtyInputStr.length()<5) qtyInputStr+=ch; return; }
         if (searchFocused) { if (key==1) { searchFocused=false; } else if (key==14 && !searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length()-1); else if (ch>=32 && searchQuery.length()<32) searchQuery+=ch; searchMode = !searchQuery.isEmpty(); applyFilter(); } else if (key==1) mc.displayGuiScreen(null);
     }

     @Override
     public void handleMouseInput() throws IOException { super.handleMouseInput(); int d = org.lwjgl.input.Mouse.getEventDWheel(); if (d != 0) { int maxR = Math.max(1, (ph-70)/ROW_H); int maxS = Math.max(0, filtered.size()-maxR); if (d < 0) scroll = Math.min(scroll+1, maxS); else scroll = Math.max(0, scroll-1); } }
     @Override
     protected void mouseReleased(int mx, int my, int state) { super.mouseReleased(mx, my, state); draggingScroll = false; }
     @Override
     protected void mouseClickMove(int mx, int my, int btn, long time) { if (draggingScroll && btn==0 && lastScrollTotal>0) { int th = lastThumbH, tr = lastScrollH-4; int rel = my - (lastScrollY+2); scroll = (int)(Math.max(0f, Math.min(1f, rel/(float)(tr-th))) * Math.max(0, lastScrollTotal-lastScrollVisible)); } }

     private void drawScrollbar(int x, int y, int w, int h, int scroll, int total, int visible, int mx, int my) { if (total <= visible) return; drawRect(x, y, x + w, y + h, 0xFF030A14); int trackH = h - 4; int thumbH = Math.max(14, trackH * visible / total); int thumbY = y + 2 + (trackH - thumbH) * scroll / Math.max(1, total - visible); boolean hov = inR(mx, my, x, thumbY, w, thumbH); drawRect(x + 1, thumbY, x + w - 1, thumbY + thumbH, hov ? C_ACCENT2 : C_ACCENT); drawRect(x + 1, thumbY, x + w - 1, thumbY + 1, hov ? C_WHITE : C_ACCENT2); hb(hbScrollUp, x, y, w, 8); hb(hbScrollDn, x, y + h - 8, w, 8); lastScrollX = x; lastScrollY = y; lastScrollW = w; lastScrollH = h; lastScrollTotal = total; lastScrollVisible = visible; lastThumbY = thumbY; lastThumbH = thumbH; }
     private void renderTooltip(List<String> lines, int mx, int my) { if (lines.isEmpty()) return; int tw = 0; for (String l : lines) tw = Math.max(tw, fontRendererObj.getStringWidth(l)); int th = lines.size() * 10 + 6; int tx = Math.min(mx + 12, width - tw - 10); int ty = Math.max(my - th - 6, 4); drawRect(tx - 4, ty - 4, tx + tw + 6, ty + th + 4, 0xF0080816); drawRect(tx - 4, ty - 4, tx + tw + 6, ty - 3, C_ACCENT); drawRect(tx - 4, ty - 4, tx - 3, ty + th + 4, C_ACCENT); drawRect(tx - 4, ty + th + 3, tx + tw + 6, ty + th + 4, C_BORDER2); for (int i = 0; i < lines.size(); i++) fontRendererObj.drawStringWithShadow(lines.get(i), tx, ty + 2 + i * 10, i == 0 ? C_WHITE : C_LGRAY); }
     private void drawStatusBar() { if (statusTimer <= 0) return; statusTimer--; int alpha = Math.min(255, statusTimer * 5); int col = ((statusOk ? C_GREEN : C_RED) & 0x00FFFFFF) | (alpha << 24); int sw = fontRendererObj.getStringWidth(statusMsg); int sx = px + (pw - sw) / 2 - 4; int sy = py + ph - 13; drawRect(sx - 2, sy - 2, sx + sw + 6, sy + 10, ((alpha / 4) << 24)); fontRendererObj.drawStringWithShadow(statusMsg, sx + 2, sy, col); }

    // Utilitaire : comparaison flottante tolérante
    private boolean approxEqual(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    // Utilitaire : détecte si une chaîne contient un nom de couleur connu (anglais/français)
    private boolean containsColorWord(String s) {
        if (s == null) return false;
        String lower = s.toLowerCase();
        String[] colors = new String[]{"white","blanc","orange","magenta","light_blue","bleu_clair","yellow","jaune","lime","vert_clair","pink","rose","gray","grey","gris","silver","light_gray","gris_clair","cyan","purple","violet","blue","bleu","brown","marron","green","vert","red","rouge","black","noir"};
        for (String c : colors) if (lower.contains(c)) return true;
        return false;
    }
}
