package net.minecraft.client.gui.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Gestionnaire de profils HUD. Supporte 5 profils (slots 0-4).
 * Chaque profil sauvegarde : position, taille, couleur, mode RGB, etat active, et props de chaque widget.
 */
public class HudProfileManager {

    public static final int MAX_PROFILES = 5;
    private static final String CONFIG_PATH = "config/hud_profiles.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final HudProfileManager INSTANCE = new HudProfileManager();

    private final List<Map<String, Map<String, Object>>> profiles = new ArrayList<>();
    private final List<String> profileNames = new ArrayList<>();
    private int activeProfile = -1;

    private HudProfileManager() {
        for (int i = 0; i < MAX_PROFILES; i++) {
            profiles.add(null);
            profileNames.add(getDefaultName(i));
        }
        load();
    }

    public static HudProfileManager getInstance() { return INSTANCE; }

    private String getDefaultName(int slot) {
        if (slot == 0) return "PvP";
        if (slot == 1) return "Exploration";
        return "Profil " + (slot + 1);
    }

    public String getProfileName(int slot) {
        if (slot < 0 || slot >= MAX_PROFILES) return "";
        return profileNames.get(slot);
    }

    public void setProfileName(int slot, String name) {
        if (slot < 0 || slot >= MAX_PROFILES) return;
        profileNames.set(slot, name);
    }

    public boolean isSlotUsed(int slot) {
        if (slot < 0 || slot >= MAX_PROFILES) return false;
        return profiles.get(slot) != null;
    }

    public int getActiveProfile() { return activeProfile; }

    public String getProfileDescription(int slot) {
        if (slot < 0 || slot >= MAX_PROFILES) return "";
        Map<String, Map<String, Object>> profile = profiles.get(slot);
        if (profile == null) return "";
        int count = 0;
        for (Map.Entry<String, Map<String, Object>> entry : profile.entrySet()) {
            Map<String, Object> w = entry.getValue();
            if (Boolean.parseBoolean(String.valueOf(w.getOrDefault("enabled", false)))) {
                count++;
            }
        }
        return count + " widgets actifs";
    }

    public void saveToSlot(int slot) {
        if (slot < 0 || slot >= MAX_PROFILES) return;
        UIManager ui = UIManager.getInstance();
        int[] screen = getScreenSize();
        int sw = screen[0], sh = screen[1];

        // Construire un nouveau profil COMPLET : tous les widgets connus sont inclus.
        // Cela garantit que lorsqu'on charge ce profil, les anciens éléments sont bien
        // remis à leur état correct (désactivés s'ils n'étaient pas visibles).
        Map<String, Map<String, Object>> profileData = new LinkedHashMap<>();

        for (UIElement e : ui.all()) {
            Map<String, Object> widgetData = new LinkedHashMap<>();
            int px = e.getX();
            int py = e.getY();
            widgetData.put("x", px);
            widgetData.put("y", py);
            widgetData.put("enabled", e.isEnabled());
            widgetData.put("color", e.getColor());
            widgetData.put("rgb", e.isRGBMode());
            widgetData.put("scale", e.getScale());

            if (e instanceof BaseWidget) {
                BaseWidget bw = (BaseWidget) e;
                int w = bw.getWidth();
                int h = bw.getHeight();
                double rx = bw.relX;
                double ry = bw.relY;
                // Recalculer relX/relY s'ils ne sont pas encore définis
                if (rx < 0 || ry < 0) {
                    int maxX = Math.max(1, sw - w);
                    int maxY = Math.max(1, sh - h);
                    rx = Math.max(0.0, Math.min(1.0, (double) px / maxX));
                    ry = Math.max(0.0, Math.min(1.0, (double) py / maxY));
                }
                widgetData.put("relX", rx);
                widgetData.put("relY", ry);
                widgetData.put("refW", sw);
                widgetData.put("refH", sh);
                widgetData.put("width", bw.width);
                widgetData.put("height", bw.height);

                Map<String, Object> propsCopy = new LinkedHashMap<>();
                for (Map.Entry<String, Object> pe : bw.getProps().entrySet()) {
                    String k = pe.getKey();
                    if ("editorPreview".equals(k) || "previewEffect".equals(k) || "preview".equals(k)) continue;
                    propsCopy.put(k, pe.getValue());
                }
                widgetData.put("props", propsCopy);
            }
            profileData.put(e.getId(), widgetData);
        }

        profiles.set(slot, profileData);
        activeProfile = slot;
        save();
    }

    public void loadFromSlot(int slot) {
        if (slot < 0 || slot >= MAX_PROFILES) return;
        Map<String, Map<String, Object>> profileData = profiles.get(slot);
        if (profileData == null) return;

        UIManager ui = UIManager.getInstance();

        // Étape 1 : désactiver TOUS les widgets pour partir d'une ardoise vierge.
        // Ainsi, les widgets qui n'étaient pas dans le profil sauvegardé seront cachés.
        for (UIElement e : ui.all()) {
            e.setEnabled(false);
        }

        // Étape 2 : appliquer les données du profil
        for (Map.Entry<String, Map<String, Object>> en : profileData.entrySet()) {
            UIElement e = ui.get(en.getKey());
            if (e == null) continue;
            Map<String, Object> m = en.getValue();

            if (m.containsKey("enabled")) e.setEnabled(Boolean.parseBoolean(String.valueOf(m.get("enabled"))));
            if (m.containsKey("color")) e.setColor(((Number) m.get("color")).intValue());
            if (m.containsKey("rgb")) e.setRGBMode(Boolean.parseBoolean(String.valueOf(m.get("rgb"))));
            if (m.containsKey("scale")) e.setScale(((Number) m.get("scale")).floatValue());

            if (e instanceof BaseWidget) {
                BaseWidget bw = (BaseWidget) e;
                if (m.containsKey("relX")) bw.relX = ((Number) m.get("relX")).doubleValue();
                if (m.containsKey("relY")) bw.relY = ((Number) m.get("relY")).doubleValue();
                if (m.containsKey("refW")) bw.refW = ((Number) m.get("refW")).intValue();
                if (m.containsKey("refH")) bw.refH = ((Number) m.get("refH")).intValue();
                if (m.containsKey("width")) {
                    try { bw.setWidth(((Number) m.get("width")).intValue()); } catch (Throwable ignored) {}
                }
                if (m.containsKey("height")) {
                    try { bw.setHeight(((Number) m.get("height")).intValue()); } catch (Throwable ignored) {}
                }
                if (m.containsKey("props")) {
                    try {
                        Object obj = m.get("props");
                        if (obj instanceof Map) {
                            Map<?, ?> pm = (Map<?, ?>) obj;
                            for (Map.Entry<?, ?> pe : pm.entrySet()) {
                                String key = String.valueOf(pe.getKey());
                                if ("preview".equals(key) || "editorPreview".equals(key) || "previewEffect".equals(key)) continue;
                                bw.setProp(key, pe.getValue());
                            }
                        }
                    } catch (Throwable ignored) {}
                }
                // Forcer le recalcul de la position absolue au prochain render
                bw.markPositionDirty();
                bw.updateAbsolutePosition();
            }
        }
        activeProfile = slot;
        ui.saveConfig();
    }

    private int[] getScreenSize() {
        int sw = 427, sh = 240;
        try {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            if (sr.getScaledWidth() > 0) sw = sr.getScaledWidth();
            if (sr.getScaledHeight() > 0) sh = sr.getScaledHeight();
        } catch (Throwable ignored) {}
        return new int[]{sw, sh};
    }

    private Map<String, Object> buildWidgetData(UIElement e, int px, int py, boolean enabled, int sw, int sh) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("x", px);
        data.put("y", py);
        data.put("enabled", enabled);
        data.put("color", e.getColor());
        data.put("rgb", e.isRGBMode());
        data.put("scale", e.getScale());
        if (e instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) e;
            int w = bw.getWidth();
            int h = bw.getHeight();
            int maxX = Math.max(1, sw - w);
            int maxY = Math.max(1, sh - h);
            data.put("relX", Math.max(0.0, Math.min(1.0, (double) px / maxX)));
            data.put("relY", Math.max(0.0, Math.min(1.0, (double) py / maxY)));
            data.put("refW", sw);
            data.put("refH", sh);
            data.put("width", bw.width);
            data.put("height", bw.height);
            Map<String, Object> propsCopy = new LinkedHashMap<>();
            for (Map.Entry<String, Object> pe : bw.getProps().entrySet()) {
                String k = pe.getKey();
                if ("editorPreview".equals(k) || "previewEffect".equals(k) || "preview".equals(k)) continue;
                propsCopy.put(k, pe.getValue());
            }
            data.put("props", propsCopy);
        }
        return data;
    }

    /**
     * Initialise le profil PvP par defaut (slot 0).
     */
    public void initDefaultPvPProfile() {
        UIManager ui = UIManager.getInstance();
        int[] screen = getScreenSize();
        int sw = screen[0], sh = screen[1];

        // Widgets actifs dans le profil PvP (sans CPS ni SNEAK)
        Set<String> pvpWidgets = new HashSet<>(Arrays.asList(
            "combatlog", "fps", "armor_group", "potions", "helditem",
            "ping", "dir", "toggle_sprint", "Reach", "Keystrokes"
        ));

        // ── Disposition sans chevauchement ──
        // Colonne gauche : stats texte empilées
        int leftX = 5;
        int topY  = 5;
        int lineH = 12; // hauteur d'une ligne de texte

        Map<String, int[]> positions = new LinkedHashMap<>();

        // Colonne gauche haut : fps / ping / reach / dir
        positions.put("fps",   new int[]{leftX, topY});
        positions.put("ping",  new int[]{leftX, topY + lineH});
        positions.put("Reach", new int[]{leftX, topY + lineH * 2});
        positions.put("dir",   new int[]{leftX, topY + lineH * 3});

        // Potions : milieu-gauche (sous les stats du haut, légèrement plus bas)
        positions.put("potions", new int[]{leftX, topY + lineH * 5 + 8});

        // Combat log en bas à gauche en mode circulaire
        // La taille d'un CombatLogWidget circulaire est ~48×48
        int combatlogSize = 48;
        positions.put("combatlog", new int[]{leftX, sh - combatlogSize - 5});

        // Toggle sprint juste au-dessus du combat log (hauteur ~10)
        positions.put("toggle_sprint", new int[]{leftX, sh - combatlogSize - 20});

        // Keystrokes en haut à droite (demande utilisateur)
        // Taille typique Keystrokes : ~60×52
        int ksW = 62, ksH = 54;
        positions.put("Keystrokes", new int[]{sw - ksW - 5, topY});

        // Armor group à droite, positionné en bas mais relevé légèrement (réhausser)
        // ArmorGroupWidget vertical : ~50×88 (4 armures × 22px)
        int armorH = 88;
        int armorX = sw - 55; // aligné vers la droite
        int armorY = Math.max(5, sh - armorH - 5 - 12); // remonter de 12px
        positions.put("armor_group", new int[]{armorX, armorY});

        // Item tenu : juste en dessous de l'armor_group, aligné à droite
        // HeldItemDurabilityWidget : ~80×10
        int heldW = 80;
        int heldH = 12; // estimation hauteur
        int heldX = sw - heldW - 5;
        int heldY = armorY + armorH + 4; // 4px d'espacement sous l'armure
        if (heldY + heldH > sh - 5) {
            // si dépasser, place le held item légèrement au-dessus du bas (fallback)
            heldY = Math.max(5, sh - heldH - 5);
        }
        positions.put("helditem", new int[]{heldX, heldY});

        Map<String, Map<String, Object>> profileData = new LinkedHashMap<>();
        for (UIElement e : ui.all()) {
            int[] pos = positions.get(e.getId());
            int px = pos != null ? pos[0] : e.getX();
            int py = pos != null ? pos[1] : e.getY();
            boolean enabled = pvpWidgets.contains(e.getId());

            Map<String, Object> data = buildWidgetData(e, px, py, enabled, sw, sh);

            // Activer le mode circulaire (originalDesign) pour combatlog
            if ("combatlog".equals(e.getId())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) data.get("props");
                if (props == null) { props = new LinkedHashMap<>(); data.put("props", props); }
                props.put("originalDesign", Boolean.TRUE);
            }

            profileData.put(e.getId(), data);
        }

        profiles.set(0, profileData);
        profileNames.set(0, "PvP");
        save();
    }

    public void initDefaultExplorationProfile() {
        UIManager ui = UIManager.getInstance();
        int[] screen = getScreenSize();
        int sw = screen[0], sh = screen[1];

        Set<String> explorationWidgets = new HashSet<>(Arrays.asList(
            "fps", "dir", "coords", "biome", "helditem", "date", "compass"
        ));

        Map<String, int[]> positions = new LinkedHashMap<>();
        int leftX = 5, topY = 5, lineH = 12;
        positions.put("fps",    new int[]{leftX, topY});
        positions.put("coords", new int[]{leftX, topY + lineH});
        positions.put("dir",    new int[]{leftX, topY + lineH * 2});
        positions.put("biome",  new int[]{leftX, topY + lineH * 3});
        positions.put("date",   new int[]{leftX, topY + lineH * 4});
        positions.put("helditem", new int[]{sw - 55, sh - 20});
        positions.put("compass", new int[]{sw / 2 - 50, 5});

        Map<String, Map<String, Object>> profileData = new LinkedHashMap<>();
        for (UIElement e : ui.all()) {
            int[] pos = positions.get(e.getId());
            int px = pos != null ? pos[0] : e.getX();
            int py = pos != null ? pos[1] : e.getY();
            boolean enabled = explorationWidgets.contains(e.getId());
            profileData.put(e.getId(), buildWidgetData(e, px, py, enabled, sw, sh));
        }

        profiles.set(1, profileData);
        profileNames.set(1, "Exploration");
        save();
    }

    public void save() {
        try {
            File out = new File(Minecraft.getMinecraft().mcDataDir, CONFIG_PATH);
            out.getParentFile().mkdirs();

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("activeProfile", activeProfile);
            root.put("profileNames", profileNames);
            root.put("profiles", profiles);

            try (Writer w = new FileWriter(out)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            System.err.println("Failed to save HUD profiles: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void load() {
        try {
            File in = new File(Minecraft.getMinecraft().mcDataDir, CONFIG_PATH);
            if (!in.exists()) return;

            Map<String, Object> root;
            try (Reader r = new FileReader(in)) {
                Type t = new TypeToken<Map<String, Object>>() {}.getType();
                root = GSON.fromJson(r, t);
            }
            if (root == null) return;

            if (root.containsKey("activeProfile")) {
                activeProfile = ((Number) root.get("activeProfile")).intValue();
            }
            if (root.containsKey("profileNames")) {
                List<?> names = (List<?>) root.get("profileNames");
                for (int i = 0; i < Math.min(names.size(), MAX_PROFILES); i++) {
                    profileNames.set(i, String.valueOf(names.get(i)));
                }
            }
            if (root.containsKey("profiles")) {
                List<?> rawProfiles = (List<?>) root.get("profiles");
                for (int i = 0; i < Math.min(rawProfiles.size(), MAX_PROFILES); i++) {
                    Object rawProfile = rawProfiles.get(i);
                    if (rawProfile instanceof Map) {
                        Map<String, Map<String, Object>> profileData = new LinkedHashMap<>();
                        Map<?, ?> rawMap = (Map<?, ?>) rawProfile;
                        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                            if (entry.getValue() instanceof Map) {
                                profileData.put(String.valueOf(entry.getKey()), (Map<String, Object>) entry.getValue());
                            }
                        }
                        profiles.set(i, profileData);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load HUD profiles: " + e.getMessage());
        }
    }

    public void deleteSlot(int slot) {
        if (slot < 0 || slot >= MAX_PROFILES) return;
        profiles.set(slot, null);
        profileNames.set(slot, getDefaultName(slot));
        if (activeProfile == slot) activeProfile = -1;
        save();
    }
}
