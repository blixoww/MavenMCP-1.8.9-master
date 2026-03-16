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

    private final List<Waypoint> waypoints = new ArrayList<Waypoint>();
    private File saveFile;

    private WaypointManager() {}

    /**
     * Initialise le fichier de sauvegarde et charge les waypoints.
     */
    public void init() {
        File gameDir = Minecraft.getMinecraft().mcDataDir;
        this.saveFile = new File(gameDir, "waypoints.json");
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
}

