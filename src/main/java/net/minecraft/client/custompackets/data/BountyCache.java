package net.minecraft.client.custompackets.data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Cache client des primes (bounties) reçues depuis le serveur.
 *
 * Le serveur envoie :
 *   - BOUNTY_SELF   : la prime posée sur le joueur local
 *   - BOUNTY_PLAYER : la prime d'un joueur proche (pour les nametags)
 */
public final class BountyCache {

    private BountyCache() {}

    /** Prime posée sur le joueur local (0 = aucune). */
    private static volatile long selfBounty = 0L;

    /** Primes des autres joueurs (nom → montant). */
    private static final Map<String, Long> playerBounties = new ConcurrentHashMap<>();

    // ── Prime locale ─────────────────────────────────────────────────────────

    public static void setSelfBounty(long amount) {
        selfBounty = Math.max(0L, amount);
    }

    public static long getSelfBounty() {
        return selfBounty;
    }

    public static boolean hasSelfBounty() {
        return selfBounty > 0L;
    }

    // ── Primes des joueurs proches ───────────────────────────────────────────

    public static void setPlayerBounty(String name, long amount) {
        if (name == null || name.isEmpty()) return;
        if (amount <= 0L) {
            playerBounties.remove(name);
        } else {
            playerBounties.put(name, amount);
        }
    }

    public static long getPlayerBounty(String name) {
        if (name == null) return 0L;
        Long v = playerBounties.get(name);
        return v != null ? v : 0L;
    }

    public static boolean hasPlayerBounty(String name) {
        return name != null && playerBounties.containsKey(name);
    }

    public static void reset() {
        selfBounty = 0L;
        playerBounties.clear();
    }
}
