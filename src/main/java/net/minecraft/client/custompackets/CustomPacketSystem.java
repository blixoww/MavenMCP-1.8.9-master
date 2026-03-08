package net.minecraft.client.custompackets;

import net.minecraft.client.custompackets.handler.HdvPacketHandler;
import net.minecraft.client.custompackets.handler.PlayerDataHandler;
import net.minecraft.client.custompackets.handler.ShopPacketHandler;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Point d'entrée unique du système de packets custom.
 *
 * <h2>Initialisation</h2>
 * Appeler {@link #init()} une seule fois au démarrage du client, par exemple
 * depuis {@code Minecraft.startGame()} ou depuis votre classe principale de mod.
 *
 * <pre>
 *   CustomPacketSystem.init();
 * </pre>
 *
 * <h2>Réception (côté client)</h2>
 * Dans {@link net.minecraft.client.network.NetHandlerPlayClient#handleCustomPayload},
 * déléguer les canaux custom au dispatcher :
 *
 * <pre>
 *   String channel = packetIn.getChannelName();
 *   if (PacketDispatcher.handles(channel)) {
 *       PacketDispatcher.dispatch(channel, packetIn.getBufferData());
 *       return;
 *   }
 *   // ... traitement vanilla
 * </pre>
 *
 * <h2>Émission (côté client)</h2>
 * Utiliser directement les handlers :
 * <pre>
 *   HdvPacketHandler.requestList(0, "");
 *   ShopPacketHandler.buyItem(42, 16);
 *   PlayerDataHandler.requestData();
 * </pre>
 *
 * <h2>Architecture des canaux</h2>
 * <pre>
 *   CUSTOM:C2S         – usage général client→serveur
 *   CUSTOM:S2C         – usage général serveur→client
 *   CUSTOM:HDV_C2S     – HDV client→serveur
 *   CUSTOM:HDV_S2C     – HDV serveur→client
 *   CUSTOM:SHOP_C2S    – Shop client→serveur
 *   CUSTOM:SHOP_S2C    – Shop serveur→client
 *   CUSTOM:PDATA_C2S   – Données joueur client→serveur
 *   CUSTOM:PDATA_S2C   – Données joueur serveur→client
 *   CUSTOM:CHAT_C2S    – Chat/messagerie client→serveur
 *   CUSTOM:CHAT_S2C    – Chat/messagerie serveur→client
 * </pre>
 */
public final class CustomPacketSystem {

    private static final Logger LOGGER = LogManager.getLogger("CustomPackets");
    private static boolean initialized = false;

    private CustomPacketSystem() {}

    /**
     * Initialise le système en enregistrant tous les handlers de packets entrants.
     * Idempotent : appels multiples ignorés.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        LOGGER.info("[CustomPackets] ✓ Initialisation du système de packets custom...");

        HdvPacketHandler.registerHandlers();
        LOGGER.info("[CustomPackets] ✓ HdvPacketHandler enregistré");

        ShopPacketHandler.registerHandlers();
        PlayerDataHandler.registerHandlers();

        PacketDispatcher.register(PacketChannel.SERVER_TO_CLIENT, PacketId.PONG, buf -> {
            long sentAt = buf.readLong();
            long latency = System.currentTimeMillis() - sentAt;
            LOGGER.debug("[CustomPackets] Pong reçu – latence applicative : {} ms", latency);
        });

        // Envoi initial (si déjà connecté, sinon ce sera fait par NetHandlerPlayClient)
        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer != null) {
            sendRegisterPacket();
        }

        LOGGER.info("[CustomPackets] ✓ Système initialisé avec succès.");
    }

    /**
     * Envoie le packet "REGISTER" contenant la liste des canaux écoutés.
     * Doit être appelé à chaque connexion au serveur (NetHandlerPlayClient.handleJoinGame).
     */
    public static void sendRegisterPacket() {
        try {
            PacketBuffer buf = PacketSender.newBuffer();
            String channels = PacketChannel.HDV_S2C + "\0" + PacketChannel.HDV_C2S + "\0" +
                              PacketChannel.SHOP_S2C + "\0" + PacketChannel.SHOP_C2S + "\0" +
                              PacketChannel.PLAYER_DATA_S2C + "\0" + PacketChannel.PLAYER_DATA_C2S + "\0" +
                              PacketChannel.CLIENT_TO_SERVER + "\0" + PacketChannel.SERVER_TO_CLIENT;
            buf.writeBytes(channels.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            PacketSender.send("REGISTER", buf);
            LOGGER.info("[CustomPackets] ✓ Packet REGISTER envoyé pour : " + channels);
        } catch (Exception e) {
            LOGGER.error("[CustomPackets] Erreur envoi REGISTER", e);
        }
    }

    /**
     * Envoie un ping applicatif au serveur pour mesurer la latence custom.
     */
    public static void sendPing() {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.PING);
        buf.writeLong(System.currentTimeMillis());
        PacketSender.send(PacketChannel.CLIENT_TO_SERVER, buf);
    }

    public static boolean isInitialized() { return initialized; }
}

