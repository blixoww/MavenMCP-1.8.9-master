package net.minecraft.client.custompackets.data;

/**
 * Informations de faction d'un joueur distant, reçues du serveur.
 *
 * <ul>
 *   <li>relation 0 = même faction  → vert §a</li>
 *   <li>relation 1 = allié          → rose/magenta §d</li>
 *   <li>relation 2 = ennemi         → rouge §c</li>
 *   <li>relation 3 = neutre         → blanc §f</li>
 * </ul>
 */
public final class FactionInfo {

    /** Tag de la faction (ex. "BLD"), jamais null, peut être vide si sans faction. */
    public final String tag;
    /** 0=own, 1=ally, 2=enemy, 3=neutral */
    public final int relation;
    /** Timestamp de réception (ms) pour expiration. */
    public final long receivedAt;

    public FactionInfo(String tag, int relation) {
        this.tag        = tag;
        this.relation   = relation;
        this.receivedAt = System.currentTimeMillis();
    }

    /** Retourne le tag formaté avec la couleur appropriée (sans crochets). */
    public String getColoredTag() {
        String color;
        switch (relation) {
            case 0:  color = "\u00a7a"; break; // own   – vert
            case 1:  color = "\u00a7d"; break; // ally  – rose
            case 2:  color = "\u00a7c"; break; // enemy – rouge
            default: color = "\u00a7f"; break; // neutral – blanc
        }
        return color + tag;
    }

    /** Retourne true si l'info a plus de 10 secondes (expirée). */
    public boolean isExpired() {
        return System.currentTimeMillis() - receivedAt > 10_000L;
    }
}

