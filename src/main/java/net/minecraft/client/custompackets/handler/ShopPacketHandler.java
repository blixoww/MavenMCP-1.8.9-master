package net.minecraft.client.custompackets.handler;

import net.minecraft.client.custompackets.PacketChannel;
import net.minecraft.client.custompackets.PacketDispatcher;
import net.minecraft.client.custompackets.PacketId;
import net.minecraft.client.custompackets.PacketSender;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShopPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger("Shop");

    public static void requestItemDetail(int itemId) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.SHOP_ITEM_DETAIL_REQUEST);
        buf.writeVarIntToBuffer(itemId);
        PacketSender.send(PacketChannel.SHOP_C2S, buf);
    }

    // ── ShopCategory ─────────────────────────────────────────────────────────

    public static class ShopCategory {
        private final int id;
        private final String name;
        private final String iconItemName;

        public ShopCategory(int id, String name, String iconItemName) {
            this.id = id; this.name = name; this.iconItemName = iconItemName;
        }

        public static ShopCategory readFrom(PacketBuffer buf) {
            return new ShopCategory(
                    buf.readVarIntFromBuffer(),
                    buf.readStringFromBuffer(64),
                    buf.readStringFromBuffer(64));
        }

        public int    getId()          { return id; }
        public String getName()        { return name; }
        public String getIconItem()    { return iconItemName; }
    }

    // ── ShopItem ─────────────────────────────────────────────────────────────

    public static class ShopItem {
        private final int    id;
        private final String displayName;
        private final String minecraftItemName;
        private final long   buyPrice;    // centimes
        private final long   sellPrice;   // centimes
        private final int    maxStack;
        private final String category;
        private final boolean frozen;
        private final long   floor;       // centimes
        private final long   ceil;        // centimes
        private final String asciiChart;
        private final List<Double> buyHistory;   // historique achat en euros
        private final List<Double> sellHistory;  // historique vente en euros
        private final long   totalBuyVolume;
        private final long   totalSellVolume;

        public ShopItem(int id, String displayName, String minecraftItemName,
                        long buyPrice, long sellPrice, int maxStack,
                        String category, boolean frozen, long floor, long ceil,
                        String asciiChart,
                        List<Double> buyHistory, List<Double> sellHistory,
                        long totalBuyVolume, long totalSellVolume) {
            this.id = id;
            this.displayName = displayName;
            this.minecraftItemName = minecraftItemName;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.maxStack = maxStack;
            this.category = category;
            this.frozen = frozen;
            this.floor = floor;
            this.ceil = ceil;
            this.asciiChart = asciiChart;
            this.buyHistory = buyHistory;
            this.sellHistory = sellHistory;
            this.totalBuyVolume = totalBuyVolume;
            this.totalSellVolume = totalSellVolume;
        }

        public static ShopItem readFrom(PacketBuffer buf) {
            int    id       = buf.readVarIntFromBuffer();
            String display  = buf.readStringFromBuffer(64);
            String mcItem   = buf.readStringFromBuffer(64);
            long   buy      = buf.readLong();
            long   sell     = buf.readLong();
            int    maxStack = buf.readVarIntFromBuffer();
            String cat      = buf.readStringFromBuffer(64);
            boolean frozen  = buf.readBoolean();
            long   floor    = buf.readLong();
            long   ceil     = buf.readLong();
            String chart    = buf.readStringFromBuffer(256);

            // Historique achat
            int hBuySize = buf.readVarIntFromBuffer();
            List<Double> buyHist = new ArrayList<>();
            for (int i = 0; i < hBuySize; i++) buyHist.add(buf.readLong() / 100.0);

            // Historique vente
            int hSellSize = buf.readVarIntFromBuffer();
            List<Double> sellHist = new ArrayList<>();
            for (int i = 0; i < hSellSize; i++) sellHist.add(buf.readLong() / 100.0);

            // Volumes cumulatifs
            long totalBuy  = buf.readLong();
            long totalSell = buf.readLong();

            return new ShopItem(id, display, mcItem, buy, sell, maxStack,
                    cat, frozen, floor, ceil, chart,
                    buyHist, sellHist, totalBuy, totalSell);
        }

        public int    getId()               { return id; }
        public String getDisplayName()      { return displayName; }
        public String getMinecraftItem()    { return minecraftItemName; }
        public long   getBuyPrice()         { return buyPrice; }
        public long   getSellPrice()        { return sellPrice; }
        public double getBuyPriceDouble()   { return buyPrice  / 100.0; }
        public double getSellPriceDouble()  { return sellPrice / 100.0; }
        public int    getMaxStack()         { return maxStack; }
        public String getCategory()         { return category; }
        public boolean isFrozen()           { return frozen; }
        public long   getFloor()            { return floor; }
        public long   getCeil()             { return ceil; }
        public String getAsciiChart()       { return asciiChart; }
        public List<Double> getBuyHistory() { return buyHistory; }
        public List<Double> getSellHistory(){ return sellHistory; }
        public long   getTotalBuyVolume()   { return totalBuyVolume; }
        public long   getTotalSellVolume()  { return totalSellVolume; }

        /** Alias pour compatibilité — renvoie l'historique d'achat */
        public List<Double> getHistory()    { return buyHistory; }
    }

    // ── MarketEntry ───────────────────────────────────────────────────────────

    public static class MarketEntry {
        private final int    id;
        private final String displayName;
        private final String minecraftItem;
        private final long   buyPrice;
        private final long   sellPrice;
        private final long   volume; // volume cumulatif

        public MarketEntry(int id, String displayName, String minecraftItem,
                           long buyPrice, long sellPrice, long volume) {
            this.id = id; this.displayName = displayName; this.minecraftItem = minecraftItem;
            this.buyPrice = buyPrice; this.sellPrice = sellPrice; this.volume = volume;
        }

        public static MarketEntry readFrom(PacketBuffer buf) {
            return new MarketEntry(
                    buf.readVarIntFromBuffer(),
                    buf.readStringFromBuffer(64),
                    buf.readStringFromBuffer(64),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readLong());
        }

        public int    getId()           { return id; }
        public String getDisplayName()  { return displayName; }
        public String getMinecraftItem(){ return minecraftItem; }
        public long   getBuyPrice()     { return buyPrice; }
        public long   getSellPrice()    { return sellPrice; }
        public long   getVolume()       { return volume; }
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    private static final List<ShopCategory> cachedCategories = new ArrayList<>();
    private static final List<ShopItem>     cachedItems      = new ArrayList<>();
    private static final List<MarketEntry>  cachedTopBought  = new ArrayList<>();
    private static final List<MarketEntry>  cachedTopSold    = new ArrayList<>();

    // ── Listeners ─────────────────────────────────────────────────────────────

    public interface CategoriesListener  { void onCategoriesReceived(List<ShopCategory> cats); }
    public interface ItemsListener       { void onItemsReceived(List<ShopItem> items); }
    public interface TransactionListener { void onTransactionResult(boolean ok, String msg, long bal); }
    public interface MarketStatsListener { void onMarketStats(List<MarketEntry> bought, List<MarketEntry> sold); }

    private static CategoriesListener  categoriesListener;
    private static ItemsListener       itemsListener;
    private static TransactionListener transactionListener;
    private static MarketStatsListener marketStatsListener;

    // ── Enregistrement ────────────────────────────────────────────────────────

    public static void registerHandlers() {

        PacketDispatcher.register(PacketChannel.SHOP_S2C, PacketId.SHOP_OPEN, buf -> {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() ->
                    net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
                            new net.minecraft.client.gui.GuiShop()));
        });

        PacketDispatcher.register(PacketChannel.SHOP_S2C, PacketId.SHOP_CATEGORIES_RESPONSE, buf -> {
            int count = buf.readVarIntFromBuffer();
            cachedCategories.clear();
            for (int i = 0; i < count; i++) cachedCategories.add(ShopCategory.readFrom(buf));
            if (categoriesListener != null)
                categoriesListener.onCategoriesReceived(Collections.unmodifiableList(cachedCategories));
        });

        PacketDispatcher.register(PacketChannel.SHOP_S2C, PacketId.SHOP_ITEMS_RESPONSE, buf -> {
            int count = buf.readVarIntFromBuffer();
            cachedItems.clear();
            for (int i = 0; i < count; i++) cachedItems.add(ShopItem.readFrom(buf));
            if (itemsListener != null)
                itemsListener.onItemsReceived(Collections.unmodifiableList(cachedItems));
        });

        PacketDispatcher.register(PacketChannel.SHOP_S2C, PacketId.SHOP_TRANSACTION_RESULT, buf -> {
            boolean ok  = buf.readBoolean();
            String  msg = buf.readStringFromBuffer(256);
            long    bal = buf.readLong();
            if (transactionListener != null) transactionListener.onTransactionResult(ok, msg, bal);
        });

        PacketDispatcher.register(PacketChannel.SHOP_S2C, PacketId.SHOP_MARKET_STATS, buf -> {
            int nBought = buf.readVarIntFromBuffer();
            cachedTopBought.clear();
            for (int i = 0; i < nBought; i++) cachedTopBought.add(MarketEntry.readFrom(buf));
            int nSold = buf.readVarIntFromBuffer();
            cachedTopSold.clear();
            for (int i = 0; i < nSold; i++) cachedTopSold.add(MarketEntry.readFrom(buf));
            if (marketStatsListener != null)
                marketStatsListener.onMarketStats(
                        Collections.unmodifiableList(cachedTopBought),
                        Collections.unmodifiableList(cachedTopSold));
        });
    }

    // ── API sortante ──────────────────────────────────────────────────────────

    public static void requestCategories() {
        PacketSender.sendSimple(PacketChannel.SHOP_C2S, PacketId.SHOP_CATEGORIES_REQUEST);
    }

    public static void requestItems(int categoryId) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.SHOP_ITEMS_REQUEST);
        buf.writeVarIntToBuffer(categoryId);
        PacketSender.send(PacketChannel.SHOP_C2S, buf);
    }

    public static void buyItem(int itemId, int quantity) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.SHOP_BUY);
        buf.writeVarIntToBuffer(itemId);
        buf.writeVarIntToBuffer(quantity);
        PacketSender.send(PacketChannel.SHOP_C2S, buf);
    }

    public static void sellItem(int itemId, int quantity) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.SHOP_SELL);
        buf.writeVarIntToBuffer(itemId);
        buf.writeVarIntToBuffer(quantity);
        PacketSender.send(PacketChannel.SHOP_C2S, buf);
    }

    public static void sellAll(int itemId) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.SHOP_SELL);
        buf.writeVarIntToBuffer(itemId);
        buf.writeVarIntToBuffer(-1);
        PacketSender.send(PacketChannel.SHOP_C2S, buf);
    }

    public static void setCategoriesListener(CategoriesListener l)   { categoriesListener  = l; }
    public static void setItemsListener(ItemsListener l)             { itemsListener       = l; }
    public static void setTransactionListener(TransactionListener l) { transactionListener = l; }
    public static void setMarketStatsListener(MarketStatsListener l) { marketStatsListener = l; }

    public static List<ShopCategory> getCachedCategories() { return Collections.unmodifiableList(cachedCategories); }
    public static List<ShopItem>     getCachedItems()      { return Collections.unmodifiableList(cachedItems); }
    public static List<MarketEntry>  getCachedTopBought()  { return Collections.unmodifiableList(cachedTopBought); }
    public static List<MarketEntry>  getCachedTopSold()    { return Collections.unmodifiableList(cachedTopSold); }
}