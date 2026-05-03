package net.minecraft.client.custompackets.data;

/**
 * Cache client du killstreak reçu depuis le serveur.
 *
 * Le serveur envoie KILLSTREAK_UPDATE à chaque kill ou mort du joueur local.
 * {@link #getLastUpdateTime()} permet aux widgets de détecter un changement
 * et de déclencher une animation.
 */
public final class KillstreakCache {

    private KillstreakCache() {}

    private static volatile int  count          = 0;
    private static volatile long lastUpdateTime = 0L;

    public static void setCount(int c) {
        count          = Math.max(0, c);
        lastUpdateTime = System.currentTimeMillis();
    }

    public static int  getCount()          { return count; }
    public static long getLastUpdateTime() { return lastUpdateTime; }

    public static boolean hasStreak() { return count > 0; }

    public static void reset() {
        count          = 0;
        lastUpdateTime = System.currentTimeMillis();
    }
}
