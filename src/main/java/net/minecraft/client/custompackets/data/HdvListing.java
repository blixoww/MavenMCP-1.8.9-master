package net.minecraft.client.custompackets.data;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

/**
 * Représente une annonce HDV côté client.
 *
 * Format binaire S2C (doit correspondre exactement à HdvManager.serializeListing) :
 *   VarInt  id
 *   String  sellerName  (VarInt len + UTF-8 bytes)
 *   ItemStack  via writeItemStackToBuffer / readItemStackFromBuffer (format vanilla 1.8.9)
 *   Long    pricePerUnit
 *   VarInt  quantity
 *   Long    expiresAt
 */
public class HdvListing {

    private int id;
    private String sellerName;
    private ItemStack item;
    private long totalPrice;
    private int quantity;
    private long expiresAt;
    private boolean sold; // true = vendu, non encore collecté

    public HdvListing() {}

    public static HdvListing readFrom(PacketBuffer buf) {
        try {
            HdvListing l = new HdvListing();
            l.id         = buf.readVarIntFromBuffer();
            l.sellerName = buf.readStringFromBuffer(64);
            // Format vanilla : auto-délimité, PAS de VarInt de longueur devant
            l.item       = buf.readItemStackFromBuffer();
            l.totalPrice = buf.readLong();
            l.quantity   = buf.readVarIntFromBuffer();
            l.expiresAt  = buf.readLong();
            l.sold       = false;
            return l;
        } catch (Exception e) {
            return null;
        }
    }

    /** Lecture d'une annonce "mes annonces" avec flag sold */
    public static HdvListing readMyListing(PacketBuffer buf) {
        try {
            HdvListing l = new HdvListing();
            l.id         = buf.readVarIntFromBuffer();
            l.sellerName = buf.readStringFromBuffer(64);
            l.item       = buf.readItemStackFromBuffer();
            l.totalPrice = buf.readLong();
            l.quantity   = buf.readVarIntFromBuffer();
            l.expiresAt  = buf.readLong();
            l.sold       = buf.readBoolean();
            return l;
        } catch (Exception e) {
            return null;
        }
    }

    public int getId()            { return id; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem()    { return item; }

    /** Renvoie le prix TOTAL du lot. */
    public long getTotalPrice()   { return totalPrice; }

    public int getQuantity()      { return quantity; }
    public long getExpiresAt()    { return expiresAt; }
    public boolean isSold()       { return sold; }

    /** Calcule le prix unitaire approximatif. */
    public long getPricePerUnit() {
        return quantity > 0 ? totalPrice / quantity : 0;
    }

    @Override
    public String toString() {
        return "HdvListing{id=" + id + ", seller='" + sellerName + "', item="
                + (item != null ? item.getDisplayName() : "null")
                + ", total=" + totalPrice + ", qty=" + quantity + ", sold=" + sold + '}';
    }
}
