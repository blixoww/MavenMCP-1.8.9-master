package net.minecraft.client.custompackets.handler;

import net.minecraft.client.custompackets.PacketChannel;
import net.minecraft.client.custompackets.PacketDispatcher;
import net.minecraft.client.custompackets.PacketId;
import net.minecraft.client.custompackets.PacketSender;
import net.minecraft.client.custompackets.data.HdvListing;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gère tous les packets entrants/sortants liés à l'HDV.
 */
public final class HdvPacketHandler {

    private static final Logger LOGGER = LogManager.getLogger("Hdv");

    private static final List<HdvListing> cachedListings   = new ArrayList<>();
    private static final List<HdvListing> cachedMyListings = new ArrayList<>();
    private static long cachedPendingEarnings = 0L;

    private static ListListener      listListener;
    private static ActionListener    actionListener;
    private static MyListingsListener myListingsListener;

    public interface ListListener       { void onListReceived(List<HdvListing> listings); }
    public interface ActionListener     { void onActionResult(boolean success, String message); }
    public interface MyListingsListener { void onMyListingsReceived(List<HdvListing> mine, long pendingEarnings); }

    // ── Enregistrement des handlers S2C ─────────────────────────────────────

    public static void registerHandlers() {
        LOGGER.debug("[Hdv] Registering handlers");
        // Liste globale des annonces
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
                    net.minecraft.client.gui.GuiScreen cur = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
                    if (cur instanceof net.minecraft.client.gui.GuiHDV)
                        ((net.minecraft.client.gui.GuiHDV) cur).onListingsReceived(snapshot);
                });
            } catch (Exception ignored) {}
        });

        // Résultat d'une action (achat, vente, annulation, collecte…)
        PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_ACTION_RESULT, buf -> {
            try {
                boolean success = buf.readBoolean();
                String  message = buf.readStringFromBuffer(256);
                final boolean s = success; final String m = message;
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (actionListener != null) actionListener.onActionResult(s, m);
                });
            } catch (Exception ignored) {}
        });

        // Demande au client d'ouvrir l'interface HDV
        PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_OPEN, buf -> {
            LOGGER.info("[Hdv] HDV_OPEN packet reçu côté client, ouverture du GUI");
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() ->
                    net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
                            new net.minecraft.client.gui.GuiHDV()));
        });

        // Mes annonces (actives + vendues) + gains en attente
        PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_MY_LISTINGS_RESPONSE, buf -> {
            try {
                int count           = buf.readVarIntFromBuffer();
                long pendingEarnings = buf.readLong();
                List<HdvListing> mine = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    HdvListing l = HdvListing.readMyListing(buf);
                    if (l != null) mine.add(l);
                }
                // Mise à jour du cache
                cachedMyListings.clear();
                cachedMyListings.addAll(mine);
                if (pendingEarnings >= 0) cachedPendingEarnings = pendingEarnings;
                final List<HdvListing> snapshot = Collections.unmodifiableList(new ArrayList<>(cachedMyListings));
                final long ep = cachedPendingEarnings;
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (myListingsListener != null) myListingsListener.onMyListingsReceived(snapshot, ep);
                    net.minecraft.client.gui.GuiScreen cur = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
                    if (cur instanceof net.minecraft.client.gui.GuiHDV)
                        ((net.minecraft.client.gui.GuiHDV) cur).onMyListingsReceived(snapshot, ep);
                });
            } catch (Exception ignored) {}
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

    /** Demande la liste des propres annonces du joueur. */
    public static void requestMyListings() {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.HDV_MY_LISTINGS_REQUEST);
        PacketSender.send(PacketChannel.HDV_C2S, buf);
    }

    /** Achète un listing. */
    public static void buyItem(int listingId) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.HDV_BUY_ITEM);
        buf.writeVarIntToBuffer(listingId);
        PacketSender.send(PacketChannel.HDV_C2S, buf);
    }

    /** Poste une annonce de vente. */
    public static void postOffer(ItemStack item, long totalPrice, int quantity) {
        if (item == null) return;
        try {
            PacketBuffer buf = PacketSender.newBuffer();
            buf.writeVarIntToBuffer(PacketId.HDV_POST_OFFER);
            buf.writeItemStackToBuffer(item);
            buf.writeLong(totalPrice);
            buf.writeVarIntToBuffer(quantity);
            PacketSender.send(PacketChannel.HDV_C2S, buf);
        } catch (Exception ignored) {}
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

    // ── Listeners ────────────────────────────────────────────────────────────

    public static void setListListener(ListListener l)           { listListener = l; }
    public static void setActionListener(ActionListener l)       { actionListener = l; }
    public static void setMyListingsListener(MyListingsListener l) { myListingsListener = l; }

    public static List<HdvListing> getCachedListings()   { return Collections.unmodifiableList(cachedListings); }
    public static List<HdvListing> getCachedMyListings() { return Collections.unmodifiableList(cachedMyListings); }
    public static long getCachedPendingEarnings()         { return cachedPendingEarnings; }
}
