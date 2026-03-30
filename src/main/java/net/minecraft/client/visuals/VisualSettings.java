package net.minecraft.client.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Conteneur centralisé des paramètres pour tous les modules visuels.
 * Sauvegarde automatique en JSON, chargement au démarrage.
 */
public class VisualSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File SETTINGS_FILE = new File(Minecraft.getMinecraft().mcDataDir, "config/visuals.json");

    // ── Combo ────────────────────────────────────────────────────────────
    public boolean comboEnabled = true;
    public float comboSize = 0.8f;
    public int comboColor = 0xFFFFFFFF;
    public float comboPosX = 0.12f;  // léger offset droit depuis le crosshair
    public float comboPosY = 0.12f;  // léger offset bas depuis le crosshair
    public int comboResetDelayMs = 2000;
    public boolean comboAnimScale = true;
    public boolean comboAnimFade = true;
    public boolean comboShowLabel = true; // afficher le mot "Combo"
    // seuils de couleur : combo count -> couleur
    public int comboThreshold1 = 5;
    public int comboColor1 = 0xFFFFAA00;  // orange à 5
    public int comboThreshold2 = 10;
    public int comboColor2 = 0xFFFF3333;  // rouge à 10
    public int comboThreshold3 = 20;
    public int comboColor3 = 0xFFFF00FF;  // magenta à 20

    // ── Hit Marker ───────────────────────────────────────────────────────
    public boolean hitMarkerEnabled = true;
    public float hitMarkerSize = 8.0f;
    public float hitMarkerOpacity = 1.0f;
    public int hitMarkerColor = 0xFFFFFFFF;
    public int hitMarkerDurationMs = 300;
    public boolean hitMarkerFade = true;

    // ── Particules ───────────────────────────────────────────────────────
    public boolean particlesEnabled = true;
    public int particleTrigger = 0;  // 0=hit, 1=kill, 2=les deux
    public int particleType = 0;     // 0=redstone, 1=potion, 2=feux d'artifice
    public int particleColor1 = 0xFFFF0000;
    public int particleColor2 = 0xFFFFAA00;
    public int particleColor3 = 0xFFFFFF00;
    public int particleQuantity = 8;
    public float particleSize = 1.0f;
    public int particleFilter = 2;   // 0=joueurs, 1=mobs, 2=tous

    // ── Coeurs ───────────────────────────────────────────────────────────
    public boolean heartsEnabled = true;
    public int heartsColor = 0xFFFF3333;
    public boolean heartsShowDamage = true; // true=dégâts, false=vie restante
    public int heartsQuantity = 3;
    public int heartsFilter = 2; // 0=joueurs, 1=mobs, 2=tous

    // ── Sauvegarde / Chargement ─────────────────────────────────────────

    public void save() {
        try {
            SETTINGS_FILE.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(SETTINGS_FILE);
            GSON.toJson(this, writer);
            writer.close();
        } catch (Exception e) {
            // silencieux
        }
    }

    public static VisualSettings load() {
        try {
            if (SETTINGS_FILE.exists()) {
                FileReader reader = new FileReader(SETTINGS_FILE);
                VisualSettings s = GSON.fromJson(reader, VisualSettings.class);
                reader.close();
                if (s != null) return s;
            }
        } catch (Exception e) {
            // silencieux
        }
        return new VisualSettings();
    }
}
