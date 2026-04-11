package net.minecraft.client.visuals.ping;

/**
 * Représente un marqueur de ping actif.
 * Instance réutilisable via le pool de {@link PingManager}.
 *
 * <p>Le visuel (couleur, style, taille) est entièrement déterminé par les
 * {@link PingSettings} du viewer local — cet objet ne stocke que la position
 * et l'expéditeur.
 */
public final class Ping {

    // ── Données du marqueur ───────────────────────────────────────────────────
    public double x, y, z;
    public String senderName = "";
    public long   createdAt;
    public long   durationMs;
    /** Relation envoyée par le serveur : 0=faction,1=ally,2=friend, -1 local */
    public int    relation = -1;

    /** Géré uniquement par PingManager. */
    boolean inUse;

    // ── Préchargé par le renderer pour éviter les allocations ─────────────────
    /** Position relative caméra (mise à jour chaque frame dans renderWorld). */
    public float renderX, renderY, renderZ;
    /** Vrai si le ping était dans le frustum lors du dernier rendu monde. */
    public boolean visibleLastFrame;

    // Package-private : créé uniquement par PingManager
    Ping() {}

    // ─────────────────────────────────────────────────────────────────────────

    void init(double x, double y, double z, String sender, long duration) {
        this.x           = x;
        this.y           = y;
        this.z           = z;
        this.senderName  = sender != null ? sender : "";
        this.createdAt   = System.currentTimeMillis();
        this.durationMs  = duration;
        this.inUse       = true;
        this.visibleLastFrame = false;
        this.relation = -1;
    }

    void recycle() {
        inUse = false;
        senderName = "";
        relation = -1;
    }

    // ── Helpers animés ────────────────────────────────────────────────────────

    /**
     * Progression de 0.0 (création) à 1.0 (expiration).
     */
    public float getProgress() {
        long elapsed = System.currentTimeMillis() - createdAt;
        return Math.min(1.0f, (float) elapsed / (float) durationMs);
    }

    /**
     * Alpha tenant compte du fade-in (200 ms) et du fade-out (25 % de fin).
     */
    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - createdAt;
        float fadeIn   = Math.min(1.0f, elapsed / 200.0f);
        float progress = getProgress();
        float fadeOut  = (progress > 0.75f) ? Math.max(0f, 1.0f - ((progress - 0.75f) / 0.25f)) : 1.0f;
        return fadeIn * fadeOut;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt >= durationMs;
    }
}
