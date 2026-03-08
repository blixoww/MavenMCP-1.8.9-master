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

/**
 * Gère les packets liés au shop (boutique côté serveur).
 *
 * <h3>Modèle de données simplifié</h3>
 * <ul>
 *   <li>{@link ShopCategory} – catégorie du shop (Armes, Armures, Ressources…)</li>
 *   <li>{@link ShopItem}     – item achetable/vendable avec son prix</li>
 * </ul>
 */
public final class ShopPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger("Shop");

    // ── Modèles internes ─────────────────────────────────────────────────────

    public static class ShopCategory {
        private final int id;
        private final String name;
        private final String iconItemName; // nom de l'item Minecraft utilisé comme icône

        public ShopCategory(int id, String name, String iconItemName) {
            this.id = id;
            this.name = name;
            this.iconItemName = iconItemName;
        }

        public static ShopCategory readFrom(PacketBuffer buf) {
            int    id   = buf.readVarIntFromBuffer();
            String name = buf.readStringFromBuffer(64);
            String icon = buf.readStringFromBuffer(64);
            return new ShopCategory(id, name, icon);
        }

        public int getId()           { return id; }
        public String getName()      { return name; }
        public String getIconItem()  { return iconItemName; }
    }

    public static class ShopItem {
        private final int id;
        private final String displayName;
        private final String minecraftItemName;
        private final long buyPrice;
        private final long sellPrice;
        private final int maxStack;

        public ShopItem(int id, String displayName, String minecraftItemName, long buyPrice, long sellPrice, int maxStack) {
            this.id = id;
            this.displayName = displayName;
            this.minecraftItemName = minecraftItemName;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
            this.maxStack = maxStack;
        }

        public static ShopItem readFrom(PacketBuffer buf) {
            int    id       = buf.readVarIntFromBuffer();
            String display  = buf.readStringFromBuffer(64);
            String mcItem   = buf.readStringFromBuffer(64);
            long   buy      = buf.readLong();
            long   sell     = buf.readLong();
            int    maxStack = buf.readVarIntFromBuffer();
            return new ShopItem(id, display, mcItem, buy, sell, maxStack);
        }

        public int    getId()              { return id; }
        public String getDisplayName()     { return displayName; }
        public String getMinecraftItem()   { return minecraftItemName; }
        public long   getBuyPrice()        { return buyPrice; }
        public long   getSellPrice()       { return sellPrice; }
        public int    getMaxStack()        { return maxStack; }
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    private static final List<ShopCategory> cachedCategories = new ArrayList<>();
    private static final List<ShopItem>     cachedItems      = new ArrayList<>();

    // ── Listeners ─────────────────────────────────────────────────────────────

    private static CategoriesListener categoriesListener;
    private static ItemsListener       itemsListener;
    private static TransactionListener transactionListener;

    public interface CategoriesListener {
        void onCategoriesReceived(List<ShopCategory> categories);
    }
    public interface ItemsListener {
        void onItemsReceived(List<ShopItem> items);
    }
    public interface TransactionListener {
        void onTransactionResult(boolean success, String message, long newBalance);
    }

    // ── Enregistrement ────────────────────────────────────────────────────────

    public static void registerHandlers() {

        PacketDispatcher.register(PacketChannel.SHOP_S2C, PacketId.SHOP_CATEGORIES_RESPONSE, buf -> {
            int count = buf.readVarIntFromBuffer();
            cachedCategories.clear();
            for (int i = 0; i < count; i++) cachedCategories.add(ShopCategory.readFrom(buf));
            LOGGER.debug("[Shop] {} catégorie(s) reçue(s)", count);
            if (categoriesListener != null)
                categoriesListener.onCategoriesReceived(Collections.unmodifiableList(cachedCategories));
        });

        PacketDispatcher.register(PacketChannel.SHOP_S2C, PacketId.SHOP_ITEMS_RESPONSE, buf -> {
            int count = buf.readVarIntFromBuffer();
            cachedItems.clear();
            for (int i = 0; i < count; i++) cachedItems.add(ShopItem.readFrom(buf));
            LOGGER.debug("[Shop] {} item(s) reçu(s)", count);
            if (itemsListener != null)
                itemsListener.onItemsReceived(Collections.unmodifiableList(cachedItems));
        });

        PacketDispatcher.register(PacketChannel.SHOP_S2C, PacketId.SHOP_TRANSACTION_RESULT, buf -> {
            boolean success    = buf.readBoolean();
            String  message    = buf.readStringFromBuffer(256);
            long    newBalance = buf.readLong();
            LOGGER.debug("[Shop] Transaction: success={} msg='{}'", success, message);
            if (transactionListener != null)
                transactionListener.onTransactionResult(success, message, newBalance);
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

    // ── Listeners setters ─────────────────────────────────────────────────────

    public static void setCategoriesListener(CategoriesListener l)   { categoriesListener = l; }
    public static void setItemsListener(ItemsListener l)             { itemsListener = l; }
    public static void setTransactionListener(TransactionListener l) { transactionListener = l; }

    // ── Accesseurs cache ──────────────────────────────────────────────────────

    public static List<ShopCategory> getCachedCategories() { return Collections.unmodifiableList(cachedCategories); }
    public static List<ShopItem>     getCachedItems()      { return Collections.unmodifiableList(cachedItems); }
}

