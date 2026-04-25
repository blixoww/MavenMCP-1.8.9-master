package net.minecraft.client.waypoint;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton qui gère la liste des waypoints : ajout, suppression, sauvegarde/chargement JSON.
 */
public class WaypointManager {

    public static final WaypointManager INSTANCE = new WaypointManager();

    // ── Paramètres globaux ────────────────────────────────────────────────────

    /**
     * Paramètres globaux de rendu des waypoints.
     * Sauvegardés dans waypoint_settings.json.
     */
    public static class WaypointSettings {
        /**
         * Si true : le label grossit proportionnellement à la distance.
         * Si false : taille fixe (définie par fixedScale).
         */
        public boolean distanceScaleEnabled = true;

        /**
         * Distance de référence (blocs) : à cette distance, le label a la taille baseScale.
         * Ex : refDistance=50 → à 50 blocs taille 100%, à 100 blocs taille 200%, etc.
         */
        public float refDistance = 50.0f;

        /**
         * Taille de base à la distance de référence.
         * 0.02 = petite, 0.04 = normale, 0.08 = grande.
         */
        public float baseScale = 0.04f;

        /**
         * Taille minimum (évite labels microscopiques à très courte distance).
         */
        public float minScale = 0.015f;

        /**
         * Taille maximum (évite labels gigantesques à très longue distance).
         */
        public float maxScale = 0.35f;

        /**
         * Scale fixe utilisé quand distanceScaleEnabled = false.
         */
        public float fixedScale = 0.04f;
    }

    private WaypointSettings settings = new WaypointSettings();
    private final List<Waypoint> waypoints = new ArrayList<Waypoint>();
    private File saveFile;
    private File settingsFile;

    private WaypointManager() {}

    public WaypointSettings getSettings() {
        return settings;
    }

    /**
     * Initialise le fichier de sauvegarde et charge les waypoints.
     */
    public void init() {
        File gameDir = Minecraft.getMinecraft().mcDataDir;
        this.saveFile = new File(gameDir, "waypoints.json");
        this.settingsFile = new File(gameDir, "config/waypoint_settings.json");
        loadSettings();
        load();
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void addWaypoint(Waypoint wp) {
        waypoints.add(wp);
        save();
    }

    public void removeWaypoint(Waypoint wp) {
        waypoints.remove(wp);
        save();
    }

    public void removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
            save();
        }
    }

    public void save() {
        try {
            JsonArray arr = new JsonArray();
            for (Waypoint wp : waypoints) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", wp.getName());
                obj.addProperty("x", wp.getX());
                obj.addProperty("y", wp.getY());
                obj.addProperty("z", wp.getZ());
                obj.addProperty("r", wp.getColorR());
                obj.addProperty("g", wp.getColorG());
                obj.addProperty("b", wp.getColorB());
                obj.addProperty("beam", wp.isBeamVisible());
                obj.addProperty("coords", wp.isCoordsVisible());
                obj.addProperty("label", wp.isLabelVisible());
                obj.addProperty("enabled", wp.isEnabled());
                obj.addProperty("textSize", wp.getTextSize().name());
                arr.add(obj);
            }
            FileWriter writer = new FileWriter(saveFile);
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(arr));
            writer.close();
        } catch (IOException e) {
            System.err.println("[Waypoints] Failed to save: " + e.getMessage());
        }
    }

    public void load() {
        waypoints.clear();
        if (saveFile == null || !saveFile.exists()) return;
        try {
            FileReader reader = new FileReader(saveFile);
            JsonParser parser = new JsonParser();
            JsonElement root = parser.parse(reader);
            reader.close();

            if (root.isJsonArray()) {
                for (JsonElement elem : root.getAsJsonArray()) {
                    JsonObject obj = elem.getAsJsonObject();
                    String name = obj.has("name") ? obj.get("name").getAsString() : "Waypoint";
                    int x = obj.has("x") ? obj.get("x").getAsInt() : 0;
                    int y = obj.has("y") ? obj.get("y").getAsInt() : 64;
                    int z = obj.has("z") ? obj.get("z").getAsInt() : 0;
                    int r = obj.has("r") ? obj.get("r").getAsInt() : 255;
                    int g = obj.has("g") ? obj.get("g").getAsInt() : 255;
                    int b = obj.has("b") ? obj.get("b").getAsInt() : 255;
                    Waypoint wp = new Waypoint(name, x, y, z, r, g, b);
                    wp.setBeamVisible(obj.has("beam") ? obj.get("beam").getAsBoolean() : true);
                    wp.setCoordsVisible(obj.has("coords") ? obj.get("coords").getAsBoolean() : true);
                    wp.setLabelVisible(obj.has("label") ? obj.get("label").getAsBoolean() : true);
                    wp.setEnabled(obj.has("enabled") ? obj.get("enabled").getAsBoolean() : true);
                    if (obj.has("textSize")) {
                        try {
                            wp.setTextSize(Waypoint.TextSize.valueOf(obj.get("textSize").getAsString()));
                        } catch (IllegalArgumentException ignored) {
                            wp.setTextSize(Waypoint.TextSize.MEDIUM);
                        }
                    }
                    waypoints.add(wp);
                }
            }
        } catch (Exception e) {
            System.err.println("[Waypoints] Failed to load: " + e.getMessage());
        }
    }

    public void saveSettings() {
        try {
            if (settingsFile == null) return;
            settingsFile.getParentFile().mkdirs();
            FileWriter w = new FileWriter(settingsFile);
            w.write(new GsonBuilder().setPrettyPrinting().create().toJson(settings));
            w.close();
        } catch (IOException e) {
            System.err.println("[Waypoints] Failed to save settings: " + e.getMessage());
        }
    }

    private void loadSettings() {
        if (settingsFile == null || !settingsFile.exists()) {
            saveSettings();
            return;
        }
        try {
            FileReader reader = new FileReader(settingsFile);
            WaypointSettings loaded = new GsonBuilder().create().fromJson(reader, WaypointSettings.class);
            reader.close();
            if (loaded != null) {
                // Migration : champs invalides (0 ou négatifs) → valeurs par défaut
                if (loaded.refDistance   <= 0f) loaded.refDistance   = 50f;
                if (loaded.baseScale     <= 0f) loaded.baseScale     = 0.04f;
                if (loaded.minScale      <= 0f) loaded.minScale      = 0.015f;
                if (loaded.maxScale      <= 0f) loaded.maxScale      = 0.35f;
                if (loaded.fixedScale    <= 0f) loaded.fixedScale    = 0.04f;
                // Migration v2 : anciens défauts trop petits → forcer les nouveaux
                if (loaded.maxScale      < 0.15f) loaded.maxScale    = 0.35f;
                if (loaded.baseScale     < 0.03f) loaded.baseScale   = 0.04f;
                if (loaded.refDistance   > 80f)   loaded.refDistance = 50f;
                settings = loaded;
            }
        } catch (Exception e) {
            System.err.println("[Waypoints] Failed to load settings: " + e.getMessage());
        }
        // Sauvegarder pour mettre à jour le fichier avec les nouveaux champs
        saveSettings();
    }
}
