package net.minecraft.client.custompackets.handler;

import net.minecraft.client.custompackets.PacketChannel;
import net.minecraft.client.custompackets.PacketDispatcher;
import net.minecraft.client.custompackets.PacketId;
import net.minecraft.client.custompackets.PacketSender;
import net.minecraft.client.custompackets.data.HdvListing;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gère tous les packets entrants/sortants liés à l'HDV.
 * Format item : writeItemStackToBuffer / readItemStackFromBuffer (vanilla 1.8.9 standard).
 */
public final class HdvPacketHandler {

    private static final List<HdvListing> cachedListings = new ArrayList<>();
    private static final List<HdvListing> cachedMyListings = new ArrayList<>();
    private static long cachedPendingEarnings = 0L;

    private static ListListener      listListener;
    private static ActionListener    actionListener;
    private static MyListingsListener myListingsListener;

    public interface ListListener       { void onListReceived(List<HdvListing> listings); }
    public interface ActionListener     { void onActionResult(boolean success, String message); }
    public interface MyListingsListener { void onMyListingsReceived(List<HdvListing> listings, long pendingEarnings); }

    // ── Enregistrement des handlers S2C ─────────────────────────────────────

    public static void registerHandlers() {
        PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_LIST_RESPONSE, buf -> {
            try {
                int count = buf.readVarIntFromBuffer();
                List<HdvListing> listings = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    HdvListing l = HdvListing.readFrom(buf);
                    if (l != null) listings.add(l);
                }
                cachedListings.clear();
                cachedListings.addAll(listings);
                final List<HdvListing> snapshot = Collections.unmodifiableList(new ArrayList<>(cachedListings));
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (listListener != null) listListener.onListReceived(snapshot);
                    net.minecraft.client.gui.GuiScreen currentScreen = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
                    if (currentScreen instanceof net.minecraft.client.gui.GuiHDV) {
                        ((net.minecraft.client.gui.GuiHDV) currentScreen).onListingsReceived(snapshot);
                    }
                });
            } catch (Exception e) {
                // ignored
            }
        });

        PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_ACTION_RESULT, buf -> {
            try {
                boolean success = buf.readBoolean();
                String  message = buf.readStringFromBuffer(256);
                if (actionListener != null) {
                    final boolean s = success; final String m = message;
                    net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(
                            () -> actionListener.onActionResult(s, m)
                    );
                }
            } catch (Exception e) {
                // ignored
            }
        });

        // Packet 0x24 : mes annonces (actives + vendues)
        PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_MY_LISTINGS_RESPONSE, buf -> {
            try {
                int count = buf.readVarIntFromBuffer();
                long earnings = buf.readLong();
                List<HdvListing> listings = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    HdvListing l = HdvListing.readMyListing(buf);
                    if (l != null) listings.add(l);
                }
                if (earnings >= 0) cachedPendingEarnings = earnings;
                cachedMyListings.clear();
                cachedMyListings.addAll(listings);
                final List<HdvListing> snapshot = Collections.unmodifiableList(new ArrayList<>(cachedMyListings));
                final long earn = cachedPendingEarnings;
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (myListingsListener != null) myListingsListener.onMyListingsReceived(snapshot, earn);
                    net.minecraft.client.gui.GuiScreen currentScreen = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
                    if (currentScreen instanceof net.minecraft.client.gui.GuiHDV) {
                        ((net.minecraft.client.gui.GuiHDV) currentScreen).onMyListingsReceived(snapshot, earn);
                    }
                });
            } catch (Exception e) {
                // ignored
            }
        });
    }

    // ── API sortante C2S ─────────────────────────────────────────────────────

    /** Demande la liste des annonces (page 0-indexée, filtre texte optionnel). */
    public static void requestList(int page, String filter) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.HDV_LIST_REQUEST);
        buf.writeVarIntToBuffer(page);
        buf.writeString(filter == null ? "" : filter);
        PacketSender.send(PacketChannel.HDV_C2S, buf);
    }

    /** Achète 1 unité d'un listing. */
    public static void buyItem(int listingId) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.HDV_BUY_ITEM);
        buf.writeVarIntToBuffer(listingId);
        PacketSender.send(PacketChannel.HDV_C2S, buf);
    }

    /**
     * Poste une annonce de vente.
     * L'item est sérialisé avec writeItemStackToBuffer (format vanilla Minecraft 1.8.9).
     * Le serveur le lit avec readMinecraftItemStack() → CraftItemStack.asBukkitCopy().
     */
    public static void postOffer(ItemStack item, long totalPrice, int quantity) {
        if (item == null) return;
        try {
            PacketBuffer buf = PacketSender.newBuffer();
            buf.writeVarIntToBuffer(PacketId.HDV_POST_OFFER);
            buf.writeItemStackToBuffer(item);
            buf.writeLong(totalPrice);
            buf.writeVarIntToBuffer(quantity);
            PacketSender.send(PacketChannel.HDV_C2S, buf);
        } catch (Exception e) {
            // ignored
        }
    }

    /** Annule une annonce propre. */
    public static void cancelOffer(int listingId) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.HDV_CANCEL_OFFER);
        buf.writeVarIntToBuffer(listingId);
        PacketSender.send(PacketChannel.HDV_C2S, buf);
    }

    /** Réclame les gains en attente. */
    public static void collectEarnings() {
        PacketSender.sendSimple(PacketChannel.HDV_C2S, PacketId.HDV_COLLECT);
    }

    /** Demande ses propres annonces (actives + vendues) au serveur */
    public static void requestMyListings() {
        PacketSender.sendSimple(PacketChannel.HDV_C2S, PacketId.HDV_MY_LISTINGS_REQUEST);
    }

    // ── Listeners ────────────────────────────────────────────────────────────

    public static void setListListener(ListListener l)               { listListener = l; }
    public static void setActionListener(ActionListener l)           { actionListener = l; }
    public static void setMyListingsListener(MyListingsListener l)   { myListingsListener = l; }

    public static List<HdvListing> getCachedListings()   { return Collections.unmodifiableList(cachedListings); }
    public static List<HdvListing> getCachedMyListings() { return Collections.unmodifiableList(cachedMyListings); }
    public static long getCachedPendingEarnings()        { return cachedPendingEarnings; }
}
