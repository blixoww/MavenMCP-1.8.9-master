package net.minecraft.client.visuals.ping;

/**
 * Un seul type de ping. La couleur/style/taille sont définis par les PingSettings
 * de chaque joueur qui visualise le marqueur.
 */
public enum PingType {

    PING(0xFF_55AAFF, "\u25CF", "note.pling", 0.8f, 1.2f);

    /** Couleur par défaut (utilisée si pas de colorOverride dans les settings). */
    public final int    defaultColor;
    /** Symbole par défaut (utilisé si symbolChar = 0 dans les settings). */
    public final String defaultSymbol;
    public final String sound;
    public final float  soundVol;
    public final float  soundPitch;

    PingType(int color, String symbol, String sound, float vol, float pitch) {
        this.defaultColor  = color;
        this.defaultSymbol = symbol;
        this.sound         = sound;
        this.soundVol      = vol;
        this.soundPitch    = pitch;
    }

    public static PingType fromOrdinal(int ordinal) {
        return PING; // Toujours PING, peu importe l'ordinal reçu
    }
}
