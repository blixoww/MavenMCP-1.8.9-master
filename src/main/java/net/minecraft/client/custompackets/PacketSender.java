package net.minecraft.client.custompackets;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utilitaire bas niveau pour envoyer des {@link C17PacketCustomPayload} vers le serveur.
 *
 * <p>Usage type :
 * <pre>
 *   PacketBuffer buf = PacketSender.newBuffer();
 *   buf.writeVarIntToBuffer(PacketId.HDV_BUY_ITEM);
 *   buf.writeVarIntToBuffer(listingId);
 *   PacketSender.send(PacketChannel.HDV_C2S, buf);
 * </pre>
 */
public final class PacketSender {

    private static final Logger LOGGER = LogManager.getLogger("CustomPackets");

    private PacketSender() {}

    /**
     * Crée un nouveau buffer vide prêt à être rempli.
     */
    public static PacketBuffer newBuffer() {
        return new PacketBuffer(Unpooled.buffer());
    }

    /**
     * Envoie un payload sur le canal donné.
     *
     * @param channel le canal (voir {@link PacketChannel})
     * @param buf     le buffer déjà rempli
     */
    public static void send(String channel, PacketBuffer buf) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) {
            LOGGER.warn("[CustomPackets] Tentative d'envoi sur '{}' sans connexion active.", channel);
            return;
        }
        try {
            mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload(channel, buf));
            LOGGER.debug("[CustomPackets] Packet envoyé sur '{}' ({} octets)", channel, buf.writerIndex());
        } catch (Exception e) {
            LOGGER.error("[CustomPackets] Erreur lors de l'envoi sur '{}'", channel, e);
        }
    }

    /**
     * Raccourci : crée un buffer, écrit le packetId en premier et l'envoie.
     *
     * @param channel  le canal
     * @param packetId l'identifiant du packet (voir {@link PacketId})
     * @return le buffer (après écriture du packetId) si l'appelant doit rajouter des données
     *         avant envoi – dans ce cas ne pas appeler {@code send()} une seconde fois.
     *
     * <b>Ne pas utiliser ce raccourci si d'autres champs doivent être écrits.</b>
     * Dans ce cas utiliser {@link #newBuffer()}, remplir le buffer, puis {@link #send(String, PacketBuffer)}.
     */
    public static void sendSimple(String channel, int packetId) {
        PacketBuffer buf = newBuffer();
        buf.writeVarIntToBuffer(packetId);
        send(channel, buf);
    }
}

