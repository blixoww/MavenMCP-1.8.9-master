package net.minecraft.client.custompackets;

import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatche les packets entrants (serveur → client) vers les handlers enregistrés.
 *
 * <p>Ce dispatcher est appelé depuis {@link net.minecraft.client.network.NetHandlerPlayClient#handleCustomPayload}
 * pour chaque canal listé dans {@link PacketChannel}.
 *
 * <h3>Enregistrement d'un handler</h3>
 * <pre>
 *   PacketDispatcher.register(PacketChannel.HDV_S2C, PacketId.HDV_LIST_RESPONSE, buf -> {
 *       int count = buf.readVarIntFromBuffer();
 *       // traiter les données…
 *   });
 * </pre>
 */
public final class PacketDispatcher {

    private static final Logger LOGGER = LogManager.getLogger("CustomPackets");

    /** Clé composite : channel + "#" + packetId (en hex) */
    private static final Map<String, IPacketHandler> HANDLERS = new HashMap<>();

    private PacketDispatcher() {}

    // ── API publique ─────────────────────────────────────────────────────────

    /**
     * Enregistre un handler pour un canal et un identifiant de packet donnés.
     * Un seul handler par (canal, packetId) – le dernier enregistré gagne.
     */
    public static void register(String channel, int packetId, IPacketHandler handler) {
        String key = buildKey(channel, packetId);
        HANDLERS.put(key, handler);
        LOGGER.debug("[CustomPackets] Handler enregistré : canal='{}' id=0x{}", channel, Integer.toHexString(packetId));
    }

    /**
     * Dispatch un payload entrant.
     * La méthode lit d'abord le VarInt packetId puis route vers le handler.
     *
     * @param channel le nom du canal reçu
     * @param buf     le buffer brut (position 0, VarInt packetId non encore lu)
     */
    public static void dispatch(String channel, PacketBuffer buf) {
        if (!buf.isReadable()) {
            LOGGER.warn("[CustomPackets] Buffer vide reçu sur '{}'", channel);
            return;
        }

        int packetId = buf.readVarIntFromBuffer();
        String key = buildKey(channel, packetId);
        IPacketHandler handler = HANDLERS.get(key);

        if (handler == null) {
            LOGGER.warn("[CustomPackets] Aucun handler pour canal='{}' id=0x{}", channel, Integer.toHexString(packetId));
            return;
        }

        try {
            handler.handle(buf);
        } catch (Exception e) {
            LOGGER.error("[CustomPackets] Erreur dans le handler canal='{}' id=0x{}", channel, Integer.toHexString(packetId), e);
        }
    }

    /**
     * Retourne true si le canal donné est géré par ce dispatcher.
     */
    public static boolean handles(String channel) {
        return PacketChannel.CLIENT_TO_SERVER.equals(channel)
                || PacketChannel.SERVER_TO_CLIENT.equals(channel)
                || PacketChannel.HDV_S2C.equals(channel)
                || PacketChannel.HDV_C2S.equals(channel)
                || PacketChannel.CHAT_S2C.equals(channel)
                || PacketChannel.CHAT_C2S.equals(channel)
                || PacketChannel.SHOP_S2C.equals(channel)
                || PacketChannel.SHOP_C2S.equals(channel)
                || PacketChannel.PLAYER_DATA_S2C.equals(channel)
                || PacketChannel.PLAYER_DATA_C2S.equals(channel);
    }

    // ── Interne ──────────────────────────────────────────────────────────────

    private static String buildKey(String channel, int packetId) {
        return channel + "#" + packetId;
    }
}

