package net.minecraft.client.custompackets.handler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache client des joueurs marqués anonymes par le serveur.
 * Le serveur pousse l'état via packet ANONYMOUS_STATUS (0x82).
 * Clé = nom du joueur (case-sensitive).
 */
public final class AnonymousCache {

    private static final Set<String> ANONYMOUS =
            java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AnonymousCache() {}

    public static void set(String playerName, boolean anonymous) {
        if (playerName == null || playerName.isEmpty()) return;
        if (anonymous) ANONYMOUS.add(playerName);
        else           ANONYMOUS.remove(playerName);
    }

    public static boolean isAnonymous(String playerName) {
        if (playerName == null) return false;
        return ANONYMOUS.contains(playerName);
    }

    public static void clear() { ANONYMOUS.clear(); }
}
