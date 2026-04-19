package net.minecraft.client.custompackets.handler;

import net.minecraft.client.custompackets.data.FactionInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache client des informations de faction des joueurs voisins.
 *
 * <p>Le serveur envoie périodiquement les données de faction pour les joueurs proches.
 * Ce cache permet au renderer de connaître le tag et la relation en temps réel.
 *
 * Clé = nom du joueur (case-sensitive, tel qu'envoyé par le serveur).
 */
public final class FactionDataCache {

    private static final Map<String, FactionInfo> CACHE = new ConcurrentHashMap<>();

    private FactionDataCache() {}

    /**
     * Met à jour (ou ajoute) l'entrée pour un joueur.
     *
     * @param playerName  nom du joueur
     * @param factionTag  tag de faction (peut être vide si sans faction)
     * @param relation    0=own, 1=ally, 2=enemy, 3=neutral
     */
    public static void update(String playerName, String factionTag, int relation) {
        if (factionTag == null || factionTag.isEmpty()) {
            CACHE.remove(playerName);
        } else {
            CACHE.put(playerName, new FactionInfo(factionTag, relation));
        }
    }

    /**
     * Retourne les infos de faction du joueur, ou null si inconnues / expirées.
     */
    public static FactionInfo get(String playerName) {
        FactionInfo info = CACHE.get(playerName);
        if (info != null && info.isExpired()) {
            CACHE.remove(playerName);
            return null;
        }
        return info;
    }

    /** Vide entièrement le cache (à appeler lors de la déconnexion). */
    public static void clear() {
        CACHE.clear();
    }
}

