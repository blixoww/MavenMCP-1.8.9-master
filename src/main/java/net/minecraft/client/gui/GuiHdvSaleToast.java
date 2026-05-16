package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

/** Toast affiché en haut-droite quand un de nos items est vendu à l'HDV. */
public final class GuiHdvSaleToast extends Gui {

    private static final int W = 180;
    private static final int H = 44;
    private static final int MARGIN_X = 6;
    private static final int MARGIN_Y = 6;

    private static final long SLIDE_IN_MS  = 350L;
    private static final long HOLD_MS      = 3200L;
    private static final long SLIDE_OUT_MS = 350L;
    private static final long TOTAL_MS     = SLIDE_IN_MS + HOLD_MS + SLIDE_OUT_MS;

    private static final int C_BG     = 0xF20A1220;
    private static final int C_BORDER = 0xFF1F5FBF;
    private static final int C_ACCENT = 0xFF2277EE;
    private static final int C_TITLE  = 0xFFFFD700;
    private static final int C_GREEN  = 0xFF55EE77;
    private static final int C_PB     = 0xFFE94CFF;
    private static final int C_MONEY  = 0xFFFFD700;
    private static final int C_WHITE  = 0xFFFFFFFF;
    private static final int C_GRAY   = 0xFFAAB3BF;

    private static final class Entry {
        final String  itemName;
        final int     qty;
        final long    price;
        final boolean isPB;
        final String  buyer;
        final ItemStack stack;
        long startTime = 0L;
        Entry(String itemName, int qty, long price, boolean isPB, String buyer, ItemStack stack) {
            this.itemName = itemName; this.qty = qty; this.price = price;
            this.isPB = isPB; this.buyer = buyer; this.stack = stack;
        }
    }

    private static final Deque<Entry> queue = new ArrayDeque<>();
    private static final GuiHdvSaleToast INSTANCE = new GuiHdvSaleToast();

    public static GuiHdvSaleToast getInstance() { return INSTANCE; }

    public static void enqueue(String itemName, int qty, long price, boolean isPB, String buyer, ItemStack stack) {
        synchronized (queue) {
            if (queue.size() >= 8) queue.pollFirst();
            queue.addLast(new Entry(itemName, qty, price, isPB, buyer, stack));
        }
    }

    public void render() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        Entry e;
        synchronized (queue) { e = queue.peekFirst(); }
        if (e == null) return;

        long now = Minecraft.getSystemTime();
        if (e.startTime == 0L) e.startTime = now;
        long elapsed = now - e.startTime;
        if (elapsed >= TOTAL_MS) {
            synchronized (queue) { queue.pollFirst(); }
            return;
        }

        float slide; // 0 = caché à droite, 1 = pleinement visible
        if (elapsed < SLIDE_IN_MS) {
            slide = ease(elapsed / (float) SLIDE_IN_MS);
        } else if (elapsed < SLIDE_IN_MS + HOLD_MS) {
            slide = 1f;
        } else {
            float t = (elapsed - SLIDE_IN_MS - HOLD_MS) / (float) SLIDE_OUT_MS;
            slide = 1f - ease(t);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int totalShift = W + MARGIN_X + 4;
        int x = sw + MARGIN_X - (int) (slide * totalShift);
        int y = MARGIN_Y;

        // Setup ortho projection en coordonnées GUI scaled (cf. GuiAchievement).
        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0D, (double) sw, (double) sh, 0.0D, 1000.0D, 3000.0D);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translate(0.0F, 0.0F, -2000.0F);

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.color(1f, 1f, 1f, 1f);

        // Ombre
        drawRect(x + 2, y + 2, x + W + 2, y + H + 2, 0x66000000);
        // Fond + bordure
        drawRect(x, y, x + W, y + H, C_BG);
        drawRect(x, y, x + W, y + 1, C_BORDER);
        drawRect(x, y + H - 1, x + W, y + H, C_BORDER);
        drawRect(x, y, x + 1, y + H, C_BORDER);
        drawRect(x + W - 1, y, x + W, y + H, C_BORDER);
        // Bande accent gauche
        drawRect(x + 1, y + 1, x + 3, y + H - 1, C_ACCENT);

        // Titre
        mc.fontRendererObj.drawStringWithShadow("§lHDV §r§e— §aVente !",
                x + 26, y + 4, C_TITLE);

        // Item icône
        RenderItem ri = mc.getRenderItem();
        if (e.stack != null && ri != null) {
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableColorMaterial();
            GlStateManager.enableDepth();
            ri.zLevel = 100f;
            ri.renderItemAndEffectIntoGUI(e.stack, x + 6, y + 14);
            ri.renderItemOverlays(mc.fontRendererObj, e.stack, x + 6, y + 14);
            ri.zLevel = 0f;
            GlStateManager.disableDepth();
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
        }

        // Nom item + quantité
        String name = e.itemName == null ? "?" : e.itemName;
        String nameLine = name + (e.qty > 1 ? " §7x" + e.qty : "");
        int textX = x + 26;
        String trimmed = trimToWidth(mc, nameLine, W - 30);
        mc.fontRendererObj.drawStringWithShadow(trimmed, textX, y + 16, C_WHITE);

        // Prix
        String priceStr;
        int priceColor;
        if (e.isPB) {
            priceStr = "+" + fmt(e.price) + " PB";
            priceColor = C_PB;
        } else {
            priceStr = "+" + fmt(e.price) + " $";
            priceColor = C_MONEY;
        }
        mc.fontRendererObj.drawStringWithShadow(priceStr, textX, y + 27, priceColor);

        // Acheteur
        String buyer = "§7par §f" + (e.buyer == null ? "?" : e.buyer);
        String buyerLine = trimToWidth(mc, buyer, W - 30 - mc.fontRendererObj.getStringWidth(priceStr) - 6);
        int buyerX = x + W - 5 - mc.fontRendererObj.getStringWidth(stripColor(buyerLine));
        if (buyerX < textX + mc.fontRendererObj.getStringWidth(priceStr) + 4) {
            buyerX = textX + mc.fontRendererObj.getStringWidth(priceStr) + 4;
        }
        mc.fontRendererObj.drawStringWithShadow(buyerLine, buyerX, y + 27, C_GRAY);

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private static float ease(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        // easeOutCubic
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static String fmt(long v) {
        if (v >= 1_000_000L) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000L)     return String.format("%.1fK", v / 1_000.0);
        return String.valueOf(v);
    }

    private static String trimToWidth(Minecraft mc, String s, int maxW) {
        if (s == null) return "";
        if (mc.fontRendererObj.getStringWidth(s) <= maxW) return s;
        String ell = "...";
        int ellW = mc.fontRendererObj.getStringWidth(ell);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(s.charAt(i));
            if (mc.fontRendererObj.getStringWidth(sb.toString()) + ellW > maxW) {
                sb.deleteCharAt(sb.length() - 1);
                break;
            }
        }
        sb.append(ell);
        return sb.toString();
    }

    private static String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "");
    }
}
