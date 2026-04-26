package net.minecraft.client.combatlog;

import net.minecraft.client.custompackets.PacketChannel;
import net.minecraft.client.custompackets.PacketDispatcher;

/**
 * Gère les données de combat reçues du serveur.
 */
public class CombatLogManager {
    public static final CombatLogManager INSTANCE = new CombatLogManager();

    private long combatTimeRemaining = 0;
    private long lastUpdateTime = 0;

    private CombatLogManager() {}

    /**
     * Initialise le gestionnaire en enregistrant son handler de paquets.
     */
    public void init() {
        PacketDispatcher.register(PacketChannel.COMBATLOG, 1, buf -> {
            try {
                this.combatTimeRemaining = buf.readLong();
                this.lastUpdateTime = System.currentTimeMillis();
            } catch (Exception e) {
                // Si erreur de lecture, on reset
                this.combatTimeRemaining = 0;
            }
        });
    }

    /**
     * Retourne le temps de combat restant estimé en millisecondes.
     */
    public long getRemainingMillis() {
        if (combatTimeRemaining <= 0) return 0;
        
        long elapsed = System.currentTimeMillis() - lastUpdateTime;
        long remaining = combatTimeRemaining - elapsed;
        
        return Math.max(0, remaining);
    }
    
    public boolean isInCombat() {
        return getRemainingMillis() > 0;
    }

    public void reset() {
        this.combatTimeRemaining = 0;
        this.lastUpdateTime = 0;
    }
}
