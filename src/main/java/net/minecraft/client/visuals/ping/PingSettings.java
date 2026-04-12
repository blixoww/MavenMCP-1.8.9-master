package net.minecraft.client.visuals.ping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Paramètres persistants du système de ping (config/ping_settings.json).
 *
 * <p>Chaque joueur a ses propres paramètres : il voit les pings des autres
 * selon SES réglages de couleur/taille/style, pas ceux de l'expéditeur.
 */
public final class PingSettings {

    private static final Logger LOGGER = LogManager.getLogger("PingSystem");
    static final File FILE = new File("config/ping_settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Activation / comportement ─────────────────────────────────────────────
    /** Version du schéma de config — incrémenté à chaque ajout de champ. */
    public int    configVersion           = 4;
    public boolean enabled                = true;
    public double  maxRange               = 50.0;
    public long    durationMs             = 5000L;
    public long    cooldownMs             = 2000L;
    public boolean soundEnabled           = true;
    public boolean showTeamPings          = true;
    public boolean showAllyPings          = true;
    public boolean showFriendPings        = true;
    public boolean showOffScreenIndicator = true;

    // ── Visuel (paramètres du VIEWER – chaque joueur voit selon SES réglages) ──
    /** Échelle globale du marqueur 3D. */
    public float   scale         = 1.0f;
    /** Couleur du marqueur en ARGB (0 = couleur par défaut #55AAFF). */
    public int     color         = 0;
    /** Afficher le nom de l'expéditeur sous le marqueur. */
    public boolean showSenderName = true;
    /**
     * Style du marqueur :
     *  0 = Anneau + croix
     *  1 = Point seul (discret)
     *  2 = Losange / diamant
     */
    public int     markerStyle   = 0;
    /** Épaisseur du trait de l'anneau. */
    public float   ringThickness = 2.5f;
    /** Afficher la distance en blocs sur le marqueur 3D. */
    public boolean showDistance  = true;

    // ── Scale à distance ──────────────────────────────────────────────────────
    /**
     * Si true, plus le ping est loin, plus le marqueur 3D est grand
     * (taille angulaire quasi-constante → toujours visible de loin).
     * Si false, le marqueur garde une taille fixe.
     */
    public boolean distanceScaleEnabled = true;
    /**
     * Coefficient multiplicateur du scale à distance.
     * La formule est : scale_total = scale * (1 + min(dist/20, 2.5) * distanceScaleFactor)
     * Défaut = 0.4 (le marqueur double environ à 50 blocs).
     */
    public float   distanceScaleFactor  = 0.4f;

    // ── Glow / surbrillance ───────────────────────────────────────────────────
    /** Activer l'effet de surbrillance (halo lumineux autour du marqueur). */
    public boolean glowEnabled   = true;
    /** Intensité du glow (0.0 – 1.0). Plus fort = plus visible de loin. */
    public float   glowIntensity = 0.65f;
    /** Nombre de passes de glow (1–4). Plus = plus épais mais plus coûteux. */
    public int     glowLayers    = 3;
    /** Si true, la couleur du glow suit automatiquement la distance (blanc → couleur). */
    public boolean glowColorAuto = true;

    // ── Touche ───────────────────────────────────────────────────────────────
    /**
     * Code touche LWJGL pour placer un ping.
     * -98 = clic molette (Mouse button 2, converti en keyCode = button - 100 = -98).
     */
    public int     keyCode       = -98;

    // ─────────────────────────────────────────────────────────────────────────

    private PingSettings() {}

    public static PingSettings load() {
        if (FILE.exists()) {
            try (Reader r = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
                PingSettings s = GSON.fromJson(r, PingSettings.class);
                if (s != null) {
                    s.applyDefaults();
                    return s;
                }
            } catch (Exception e) {
                LOGGER.warn("[PingSystem] Impossible de lire ping_settings.json – valeurs par défaut.", e);
            }
        }
        PingSettings d = new PingSettings();
        d.save();
        return d;
    }

    /**
     * Applique les valeurs par défaut pour les champs qui n'existaient pas
     * dans un ancien fichier JSON (Gson via Unsafe ne passe pas par l'initialiseur).
     */
    private void applyDefaults() {
        boolean changed = false;
        if (configVersion < 2) {
            showAllyPings = true;
            showFriendPings = true;
            showTeamPings = true;
            showOffScreenIndicator = true;
            showSenderName = true;
            soundEnabled = true;
            enabled = true;
            configVersion = 2;
            changed = true;
        }
        if (configVersion < 3) {
            // Champs ajoutés en v3 : glow + showDistance par défaut true + ringThickness 2.5
            glowEnabled   = true;
            glowIntensity = 0.65f;
            glowLayers    = 3;
            glowColorAuto = true;
            showDistance  = true;
            ringThickness = 2.5f;
            configVersion = 3;
            changed = true;
        }
        if (configVersion < 4) {
            // Champs ajoutés en v4 : scale à distance
            distanceScaleEnabled = true;
            distanceScaleFactor  = 0.4f;
            configVersion = 4;
            changed = true;
        }
        if (changed) save();
    }

    /**
     * Crée une instance avec les valeurs par défaut (sans toucher au fichier).
     */
    public static PingSettings createDefault() {
        return new PingSettings();
    }

    /**
     * Réinitialise le fichier de configuration aux valeurs par défaut et renvoie l'instance.
     */
    public static PingSettings resetToDefaultAndSave() {
        PingSettings d = new PingSettings();
        d.save();
        return d;
    }

    public void save() {
        try {
            FILE.getParentFile().mkdirs();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (Exception e) {
            LOGGER.warn("[PingSystem] Impossible de sauvegarder ping_settings.json", e);
        }
    }

    /**
     * Retourne la couleur effective en ARGB (opaque).
     * Si {@code color == 0}, retourne la couleur par défaut du type.
     */
    public int getEffectiveColor() {
        return (color != 0) ? (color | 0xFF000000) : (PingType.PING.defaultColor | 0xFF000000);
    }

    /** Composante rouge de la couleur effective. */
    public int getR() { return (getEffectiveColor() >> 16) & 0xFF; }
    /** Composante verte de la couleur effective. */
    public int getG() { return (getEffectiveColor() >>  8) & 0xFF; }
    /** Composante bleue de la couleur effective. */
    public int getB() { return  getEffectiveColor()        & 0xFF; }
}
