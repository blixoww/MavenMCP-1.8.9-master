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
    private static long cachedPendingEarnings   = 0L;
    private static long cachedPendingPBEarnings = 0L;

    /** Nb d'items vendus lors de la dernière réponse. -1 = non initialisé (première co). */
    private static int previousSoldCount = -1;

    /** Barre de séparation utilisée dans les messages chat HDV. */
    private static final String HDV_BAR =
        "\u00a78\u00a7m                                                            \u00a7r";

    private static ListListener      listListener;
    private static ActionListener    actionListener;
    private static MyListingsListener myListingsListener;

    public interface ListListener       { void onListReceived(List<HdvListing> listings); }
    public interface ActionListener     { void onActionResult(boolean success, String message); }
    public interface MyListingsListener { void onMyListingsReceived(List<HdvListing> mine, long pendingEarnings, long pendingPBEarnings); }

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
                int count            = buf.readVarIntFromBuffer();
                long pendingEarnings = buf.readLong();
                long pendingPBEarnings = 0L;
                try { pendingPBEarnings = buf.readLong(); } catch (Exception ignored) {}
                List<HdvListing> mine = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    HdvListing l = HdvListing.readMyListing(buf);
                    if (l != null) mine.add(l);
                }
                cachedMyListings.clear();
                cachedMyListings.addAll(mine);
                if (pendingEarnings   >= 0) cachedPendingEarnings   = pendingEarnings;
                if (pendingPBEarnings >= 0) cachedPendingPBEarnings = pendingPBEarnings;
                final List<HdvListing> snapshot = Collections.unmodifiableList(new ArrayList<>(cachedMyListings));
                final long ep   = cachedPendingEarnings;
                final long epPB = cachedPendingPBEarnings;
                int sc = 0;
                for (HdvListing l : mine) { if (l.isSold()) sc++; }
                final int soldCount = sc;
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (myListingsListener != null) myListingsListener.onMyListingsReceived(snapshot, ep, epPB);
                    net.minecraft.client.gui.GuiScreen cur = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
                    if (cur instanceof net.minecraft.client.gui.GuiHDV)
                        ((net.minecraft.client.gui.GuiHDV) cur).onMyListingsReceived(snapshot, ep, epPB);
                    // Notification connexion : items vendus pendant la déconnexion
                    if (!(cur instanceof net.minecraft.client.gui.GuiHDV)
                            && previousSoldCount < 0 && soldCount > 0) {
                        chat(HDV_BAR);
                        chat("\u00a7f  \u00a76[HDV] \u00a7e" + soldCount + " vente(s) en attente !");
                        chat("\u00a77  Ouvrez l'HDV pour collecter \u00a76+" + fmtPrice(ep) + " $");
                        chat(HDV_BAR);
                    }
                    previousSoldCount = soldCount;
                });
            } catch (Exception ignored) {}
        });

        // Notification temps-réel : un item vient d'être vendu
        PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_SOLD_NOTIFICATION, buf -> {
            try {
                String itemName = buf.readStringFromBuffer(64);
                int    qty      = buf.readVarIntFromBuffer();
                long   price    = buf.readLong();
                boolean isPB    = false;
                try { isPB = buf.readBoolean(); } catch (Exception ignored) {}
                String buyer = "?";
                try { buyer = buf.readStringFromBuffer(64); } catch (Exception ignored) {}
                net.minecraft.item.ItemStack stack = null;
                try { stack = buf.readItemStackFromBuffer(); } catch (Exception ignored) {}
                final String n = itemName; final int q = qty; final long p = price;
                final boolean pb = isPB; final String b = buyer;
                final net.minecraft.item.ItemStack s = stack;
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    // Le toast top-droite suffit \u2014 pas de spam chat.
                    previousSoldCount = Math.max(0, previousSoldCount) + 1;
                    net.minecraft.client.gui.GuiHdvSaleToast.enqueue(n, q, p, pb, b, s);
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

    /** Achète un listing. buyerPayPB = true si l'acheteur choisit de payer en PB (annonce double-devise). */
    public static void buyItem(int listingId, boolean buyerPayPB) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.HDV_BUY_ITEM);
        buf.writeVarIntToBuffer(listingId);
        buf.writeBoolean(buyerPayPB);
        PacketSender.send(PacketChannel.HDV_C2S, buf);
    }

    /** Poste une annonce de vente. pricePB > 0 = annonce double-devise (acheteur choisit $ ou PB). */
    public static void postOffer(ItemStack item, long totalPrice, int quantity, boolean payPB, long pricePB) {
        if (item == null) return;
        try {
            PacketBuffer buf = PacketSender.newBuffer();
            buf.writeVarIntToBuffer(PacketId.HDV_POST_OFFER);
            buf.writeItemStackToBuffer(item);
            buf.writeLong(totalPrice);
            buf.writeVarIntToBuffer(quantity);
            buf.writeBoolean(payPB);
            buf.writeLong(pricePB);
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

    /** Demande au serveur de forcer l'expiration d'un listing (commande admin). */
    public static void expireItem(int listingId) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.HDV_ADMIN_EXPIRE);
        buf.writeVarIntToBuffer(listingId);
        PacketSender.send(PacketChannel.HDV_C2S, buf);
    }

    /** Affiche le message de collecte réussie dans le chat avec des barres alignées. */
    public static void showCollectMessage(long amount, long amountPB) {
        chat(HDV_BAR);
        chat("\u00a7f  \u00a76[HDV] \u00a7aCollecte reussie !");
        if (amount   > 0L) chat("\u00a77  Gains      : \u00a76+" + fmtPrice(amount)   + " $");
        if (amountPB > 0L) chat("\u00a77  Gains \u00a7ePB\u00a77   : \u00a7e+" + fmtPrice(amountPB) + " PB");
        chat(HDV_BAR);
    }

    /** Réinitialise l'état de session (à appeler à chaque connexion). */
    public static void resetSession() {
        previousSoldCount = -1;
    }

    // ── Listeners ────────────────────────────────────────────────────────────

    public static void setListListener(ListListener l)           { listListener = l; }
    public static void setActionListener(ActionListener l)       { actionListener = l; }
    public static void setMyListingsListener(MyListingsListener l) { myListingsListener = l; }

    public static List<HdvListing> getCachedListings()   { return Collections.unmodifiableList(cachedListings); }
    public static List<HdvListing> getCachedMyListings() { return Collections.unmodifiableList(cachedMyListings); }
    public static long getCachedPendingEarnings()         { return cachedPendingEarnings; }
    public static long getCachedPendingPBEarnings()       { return cachedPendingPBEarnings; }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private static void chat(String msg) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(msg));
        }
    }

    private static String fmtPrice(long v) {
        if (v >= 1_000_000L) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000L)     return String.format("%.1fK", v / 1_000.0);
        return String.valueOf(v);
    }
}
