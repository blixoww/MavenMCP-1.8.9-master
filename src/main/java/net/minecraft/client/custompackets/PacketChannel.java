package net.minecraft.client.custompackets;

/**
 * Définit tous les canaux de communication Plugin Messaging utilisés
 * entre ce client modifié et le plugin Spigot.
 *
 * Convention : OUTGOING = client → serveur  |  INCOMING = serveur → client
 *
 * Format des packets : chaque payload commence toujours par un VarInt "packetId"
 * qui correspond aux constantes PacketId.<TYPE>.<valeur>.
 */
public final class PacketChannel {

    private PacketChannel() {}

    // ── Canal principal (bidirectionnel) ─────────────────────────────────────
    /** Canal principal du client modifié → serveur */
    public static final String CLIENT_TO_SERVER = "CUSTOM:C2S";

    /** Canal principal du serveur → client modifié */
    public static final String SERVER_TO_CLIENT = "CUSTOM:S2C";

    // ── Canal dédié à l'HDV (Hôtel des Ventes) ──────────────────────────────
    /** Canal spécifique à l'HDV – client → serveur */
    public static final String HDV_C2S = "CUSTOM:HDV_C2S";

    /** Canal spécifique à l'HDV – serveur → client */
    public static final String HDV_S2C = "CUSTOM:HDV_S2C";

    // ── Canal dédié au chat/messagerie privée ────────────────────────────────
    public static final String CHAT_C2S = "CUSTOM:CHAT_C2S";
    public static final String CHAT_S2C = "CUSTOM:CHAT_S2C";

    // ── Canal dédié au shop ──────────────────────────────────────────────────
    public static final String SHOP_C2S = "CUSTOM:SHOP_C2S";
    public static final String SHOP_S2C = "CUSTOM:SHOP_S2C";

    // ── Canal dédié aux données joueur (stats, rangs, économie…) ────────────
    public static final String PLAYER_DATA_S2C = "CUSTOM:PDATA_S2C";
    public static final String PLAYER_DATA_C2S = "CUSTOM:PDATA_C2S";

    // ── Canal dédié au CombatLog (nom court pour compatibilité Bukkit/Spigot)
    public static final String COMBATLOG = "OF_COMBAT";

    // ── Canal dédié au système de Ping ──────────────────────────────────────
    /** Ping – client → serveur (placement d'un ping) */
    public static final String PING_C2S = "CUSTOM:PING_C2S";
    /** Ping – serveur → client (diffusion aux membres de faction proches) */
    public static final String PING_S2C = "CUSTOM:PING_S2C";
}
