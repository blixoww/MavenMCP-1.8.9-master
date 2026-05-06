package net.minecraft.client.custompackets.handler;

import net.minecraft.client.custompackets.PacketChannel;
import net.minecraft.client.custompackets.PacketDispatcher;
import net.minecraft.client.custompackets.PacketId;
import net.minecraft.client.custompackets.PacketSender;
import net.minecraft.client.custompackets.data.PlayerData;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gère les packets liés aux données du joueur (balance, rang, stats).
 *
 * <h3>Utilisation</h3>
 * <pre>
 *   PlayerDataHandler.setListener(data -> {
 *       // Mettre à jour l'affichage HUD
 *   });
 *   PlayerDataHandler.requestData();
 * </pre>
 */
public final class PlayerDataHandler {

    private static final Logger LOGGER = LogManager.getLogger("PlayerData");

    // ── Cache ────────────────────────────────────────────────────────────────

    private static PlayerData cachedData;

    // ── Listener ─────────────────────────────────────────────────────────────

    private static DataListener listener;

    public interface DataListener {
        void onDataReceived(PlayerData data);
    }

    // ── Enregistrement ────────────────────────────────────────────────────────

    public static void registerHandlers() {

        PacketDispatcher.register(PacketChannel.PLAYER_DATA_S2C, PacketId.PLAYER_BALANCE, buf -> {
            long balance = buf.readLong();
            if (cachedData == null) cachedData = new PlayerData();
            cachedData.setBalance(balance);
            LOGGER.debug("[PlayerData] Balance mise à jour : {}", balance);
            notifyListener();
        });

        PacketDispatcher.register(PacketChannel.PLAYER_DATA_S2C, PacketId.PLAYER_RANK, buf -> {
            String rank = buf.readStringFromBuffer(32);
            if (cachedData == null) cachedData = new PlayerData();
            cachedData.setRank(rank);
            LOGGER.debug("[PlayerData] Rang mis à jour : {}", rank);
            notifyListener();
        });

        PacketDispatcher.register(PacketChannel.PLAYER_DATA_S2C, PacketId.PLAYER_STATS, buf -> {
            cachedData = PlayerData.readFrom(buf);
            LOGGER.debug("[PlayerData] Données complètes reçues : {}", cachedData);
            notifyListener();
        });
    }

    // ── API sortante ──────────────────────────────────────────────────────────

    /** Demande au serveur l'ensemble des données du joueur */
    public static void requestData() {
        PacketSender.sendSimple(PacketChannel.PLAYER_DATA_C2S, PacketId.PLAYER_DATA_REQUEST);
    }

    /**
     * Demande au serveur d'ouvrir le profil du joueur local.
     * Le serveur répondra par un packet PROFILE_OPEN qui ouvrira l'écran
     * avec des données fraîches (kills, morts, killstreak, bounty, etc.).
     */
    public static void requestProfile() {
        PacketSender.sendSimple(PacketChannel.PLAYER_DATA_C2S, PacketId.PROFILE_REQUEST_OWN);
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public static PlayerData getCachedData() { return cachedData; }

    public static void setListener(DataListener l) { listener = l; }

    private static void notifyListener() {
        if (listener != null && cachedData != null) listener.onDataReceived(cachedData);
    }
}

