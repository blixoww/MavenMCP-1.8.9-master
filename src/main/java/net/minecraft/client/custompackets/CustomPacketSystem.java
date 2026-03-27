package net.minecraft.client.custompackets;

import net.minecraft.client.custompackets.handler.HdvPacketHandler;
import net.minecraft.client.custompackets.handler.PlayerDataHandler;
import net.minecraft.client.custompackets.handler.ShopPacketHandler;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Point d'entrée unique du système de packets custom.
 */
public final class CustomPacketSystem {

    private static final Logger LOGGER = LogManager.getLogger("CustomPackets");
    private static boolean initialized = false;

    private CustomPacketSystem() {}

    /**
     * Initialise le système en enregistrant tous les handlers de packets entrants.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        LOGGER.info("[CustomPackets] ✓ Initialisation du système de packets custom...");

        HdvPacketHandler.registerHandlers();
        LOGGER.info("[CustomPackets] ✓ HdvPacketHandler enregistré");

        ShopPacketHandler.registerHandlers();
        PlayerDataHandler.registerHandlers();
        
        // Initialiser le combat log
        net.minecraft.client.combatlog.CombatLogManager.INSTANCE.init();
        LOGGER.info("[CustomPackets] ✓ CombatLogManager initialisé");

        PacketDispatcher.register(PacketChannel.SERVER_TO_CLIENT, PacketId.PONG, buf -> {
            long sentAt = buf.readLong();
            long latency = System.currentTimeMillis() - sentAt;
            LOGGER.debug("[CustomPackets] Pong reçu – latence applicative : {} ms", latency);
        });

        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer != null) {
            sendRegisterPacket();
        }

        LOGGER.info("[CustomPackets] ✓ Système initialisé avec succès.");
    }

    /**
     * Envoie le packet "REGISTER" contenant la liste des canaux écoutés.
     */
    public static void sendRegisterPacket() {
        try {
            PacketBuffer buf = PacketSender.newBuffer();
            String channels = PacketChannel.HDV_S2C + "\0" + PacketChannel.HDV_C2S + "\0" +
                              PacketChannel.SHOP_S2C + "\0" + PacketChannel.SHOP_C2S + "\0" +
                              PacketChannel.PLAYER_DATA_S2C + "\0" + PacketChannel.PLAYER_DATA_C2S + "\0" +
                              PacketChannel.CLIENT_TO_SERVER + "\0" + PacketChannel.SERVER_TO_CLIENT + "\0" +
                              PacketChannel.COMBATLOG;
            buf.writeBytes(channels.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            PacketSender.send("REGISTER", buf);
            LOGGER.info("[CustomPackets] ✓ Packet REGISTER envoyé pour : " + channels);
        } catch (Exception e) {
            LOGGER.error("[CustomPackets] Erreur envoi REGISTER", e);
        }
    }

    public static void sendPing() {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.PING);
        buf.writeLong(System.currentTimeMillis());
        PacketSender.send(PacketChannel.CLIENT_TO_SERVER, buf);
    }

    public static boolean isInitialized() { return initialized; }
}
