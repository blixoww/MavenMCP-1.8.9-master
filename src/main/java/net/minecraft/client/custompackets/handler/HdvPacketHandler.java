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

    private static ListListener   listListener;
    private static ActionListener actionListener;

    public interface ListListener   { void onListReceived(List<HdvListing> listings); }
    public interface ActionListener { void onActionResult(boolean success, String message); }

    // ── Enregistrement des handlers S2C ─────────────────────────────────────

    public static void registerHandlers() {
        PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_LIST_RESPONSE, buf -> {
            try {
                int count = buf.readVarIntFromBuffer();
                List<HdvListing> listings = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    HdvListing l = HdvListing.readFrom(buf);
                    if (l != null) {
                        listings.add(l);
                    }
                }
                cachedListings.clear();
                cachedListings.addAll(listings);
                final List<HdvListing> snapshot = Collections.unmodifiableList(new ArrayList<>(cachedListings));
                net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
                    // Notifier le listener (GuiHDV)
                    if (listListener != null) {
                        listListener.onListReceived(snapshot);
                    }
                    // Fallback : si le GUI courant est GuiHDV, forcer la mise à jour directement
                    net.minecraft.client.gui.GuiScreen currentScreen = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
                    if (currentScreen instanceof net.minecraft.client.gui.GuiHDV) {
                        ((net.minecraft.client.gui.GuiHDV) currentScreen).onListingsReceived(snapshot);
                    }
                });
            } catch (Exception e) {
                // LOGGER supprimé
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
                // LOGGER supprimé
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
            buf.writeItemStackToBuffer(item);   // format standard 1.8.9
            buf.writeLong(totalPrice);          // Envoi du prix TOTAL
            buf.writeVarIntToBuffer(quantity);
            PacketSender.send(PacketChannel.HDV_C2S, buf);
        } catch (Exception e) {
            // LOGGER supprimé
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

    // ── Listeners ────────────────────────────────────────────────────────────

    public static void setListListener(ListListener l)     { listListener = l; }
    public static void setActionListener(ActionListener l) { actionListener = l; }

    public static List<HdvListing> getCachedListings() {
        return Collections.unmodifiableList(cachedListings);
    }
}
