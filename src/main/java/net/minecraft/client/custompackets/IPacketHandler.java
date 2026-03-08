package net.minecraft.client.custompackets;

import net.minecraft.network.PacketBuffer;

/**
 * Interface à implémenter pour traiter un type de packet entrant (serveur → client).
 *
 * <p>Enregistrer un handler via {@link PacketDispatcher#register(String, int, IPacketHandler)}.
 */
public interface IPacketHandler {

    /**
     * Appelé sur le thread client principal lorsqu'un packet correspondant
     * au canal + packetId est reçu.
     *
     * @param buf le buffer de données (le VarInt packetId a déjà été consommé)
     */
    void handle(PacketBuffer buf);
}

