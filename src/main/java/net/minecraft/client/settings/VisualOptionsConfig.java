package net.minecraft.client.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class VisualOptionsConfig {

    private static VisualOptionsConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final File configFile;

    // Feature 1: Hit Marker
    public boolean hitMarkerEnabled = true;
    public String hitMarkerSize = "Moyen"; // Petit, Moyen, Grand
    public int hitMarkerColor = 0xFFFFFFFF;
    public float hitMarkerOpacity = 1.0f;
    public boolean hitMarkerSound = true;

    // Feature 2: Damage Indicator
    public boolean damageIndicatorEnabled = true;
    public boolean damageIndicatorPlayers = true;
    public boolean damageIndicatorMobs = true;
    public float damageIndicatorScale = 1.0f;
    public boolean damageIndicatorShowHeal = true;
    public int damageIndicatorDuration = 1000;

    // Feature 3: Combo Counter
    public boolean comboCounterEnabled = true;
    public int comboCounterX = 0; // Relative to crosshair
    public int comboCounterY = 0;
    public float comboCounterScale = 1.0f;
    public int comboThresholdOrange = 5;
    public int comboThresholdRed = 10;
    public int comboThresholdGold = 20;

    // Feature 4: Hit Color
    public boolean vignetteEnabled = true;
    public int vignetteColor = 0xFF0000;
    public float vignetteIntensity = 0.5f;
    public boolean hitFlashEnabled = true;
    public String hitFlashColorType = "Vanilla"; // Vanilla, Blanc, Custom
    public int hitFlashCustomColor = 0xFFFFFF;

    // Feature 5: PvP Particles
    public boolean bloodParticlesEnabled = true;
    public int bloodParticlesAmount = 10;
    public boolean swordTrailEnabled = true;
    public int swordTrailColor = 0xFFFFFF;
    public boolean killParticlesEnabled = true;
    public int killParticlesColor = 0xFF0000;

    private VisualOptionsConfig() {
        this.configFile = new File(Minecraft.getMinecraft().mcDataDir, "visual_options.json");
    }

    public static VisualOptionsConfig getInstance() {
        if (instance == null) {
            instance = new VisualOptionsConfig();
            instance.load();
        }
        return instance;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                VisualOptionsConfig loaded = GSON.fromJson(reader, VisualOptionsConfig.class);
                if (loaded != null) {
                    this.hitMarkerEnabled = loaded.hitMarkerEnabled;
                    this.hitMarkerSize = loaded.hitMarkerSize;
                    this.hitMarkerColor = loaded.hitMarkerColor;
                    this.hitMarkerOpacity = loaded.hitMarkerOpacity;
                    this.hitMarkerSound = loaded.hitMarkerSound;

                    this.damageIndicatorEnabled = loaded.damageIndicatorEnabled;
                    this.damageIndicatorPlayers = loaded.damageIndicatorPlayers;
                    this.damageIndicatorMobs = loaded.damageIndicatorMobs;
                    this.damageIndicatorScale = loaded.damageIndicatorScale;
                    this.damageIndicatorShowHeal = loaded.damageIndicatorShowHeal;
                    this.damageIndicatorDuration = loaded.damageIndicatorDuration;

                    this.comboCounterEnabled = loaded.comboCounterEnabled;
                    this.comboCounterX = loaded.comboCounterX;
                    this.comboCounterY = loaded.comboCounterY;
                    this.comboCounterScale = loaded.comboCounterScale;
                    this.comboThresholdOrange = loaded.comboThresholdOrange;
                    this.comboThresholdRed = loaded.comboThresholdRed;
                    this.comboThresholdGold = loaded.comboThresholdGold;

                    this.vignetteEnabled = loaded.vignetteEnabled;
                    this.vignetteColor = loaded.vignetteColor;
                    this.vignetteIntensity = loaded.vignetteIntensity;
                    this.hitFlashEnabled = loaded.hitFlashEnabled;
                    this.hitFlashColorType = loaded.hitFlashColorType;
                    this.hitFlashCustomColor = loaded.hitFlashCustomColor;

                    this.bloodParticlesEnabled = loaded.bloodParticlesEnabled;
                    this.bloodParticlesAmount = loaded.bloodParticlesAmount;
                    this.swordTrailEnabled = loaded.swordTrailEnabled;
                    this.swordTrailColor = loaded.swordTrailColor;
                    this.killParticlesEnabled = loaded.killParticlesEnabled;
                    this.killParticlesColor = loaded.killParticlesColor;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }
}
