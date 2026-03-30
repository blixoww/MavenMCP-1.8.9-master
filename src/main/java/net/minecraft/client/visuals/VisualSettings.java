package net.minecraft.client.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Conteneur centralisé des paramètres pour tous les modules visuels.
 */
public class VisualSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File SETTINGS_FILE = new File(Minecraft.getMinecraft().mcDataDir, "config/visuals.json");

    // ── Combo ────────────────────────────────────────────────────────────
    public boolean comboEnabled = true;
    public float comboSize = 0.8f;
    public int comboColor = 0xFFFFFFFF;
    public float comboPosX = 0.12f;
    public float comboPosY = 0.12f;
    public int comboResetDelayMs = 2000;
    public boolean comboAnimScale = true;
    public boolean comboAnimFade = true;
    public boolean comboShowLabel = true;
    public int comboThreshold1 = 5;
    public int comboColor1 = 0xFFFFAA00;
    public int comboThreshold2 = 10;
    public int comboColor2 = 0xFFFF3333;
    public int comboThreshold3 = 20;
    public int comboColor3 = 0xFFFF00FF;

    // ── Hit Marker ───────────────────────────────────────────────────────
    public boolean hitMarkerEnabled = true;
    public float hitMarkerSize = 5.5f;        // Plus petit par défaut
    public float hitMarkerOpacity = 0.7f;     // Plus transparent
    public int hitMarkerColor = 0xFFFFFFFF;
    public int hitMarkerDurationMs = 250;     // Un peu plus rapide
    public boolean hitMarkerFade = true;

    // ── Particules ───────────────────────────────────────────────────────
    public boolean particlesEnabled = true;
    public int particleTrigger = 0;
    public int particleType = 0;
    public int particleColor1 = 0xFFFF0000;
    public int particleColor2 = 0xFFFFAA00;
    public int particleColor3 = 0xFFFFFF00;
    public int particleQuantity = 8;
    public float particleSize = 1.0f;
    public int particleFilter = 2;

    // ── Coeurs ───────────────────────────────────────────────────────────
    public boolean heartsEnabled = true;
    public int heartsColor = 0xFFFF3333;
    public boolean heartsShowDamage = true;
    public int heartsQuantity = 3;
    public int heartsFilter = 2;

    public void save() {
        try {
            if (!SETTINGS_FILE.getParentFile().exists()) SETTINGS_FILE.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(SETTINGS_FILE);
            GSON.toJson(this, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return new VisualSettings();
    }
}
