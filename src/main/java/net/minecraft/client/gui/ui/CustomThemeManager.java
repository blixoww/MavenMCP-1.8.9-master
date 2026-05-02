package net.minecraft.client.gui.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.*;

/**
 * Gestionnaire des thèmes personnalisés (jusqu'à 9 slots).
 * Sauvegarde dans config/custom_themes.json.
 */
public class CustomThemeManager {

    public static final int MAX = 9;
    private static final String PATH = "config/custom_themes.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final CustomThemeManager INSTANCE = new CustomThemeManager();

    // ---- Modèle de thème (2 couleurs comme les palettes prédéfinies) ----
    public static class CustomTheme {
        public String name  = "Mon Theme";
        public String desc  = "Personnalise";
        /** Valeur : couleur des chiffres / textes principaux. */
        public int value    = 0xFFFFFFFF;
        /** Prefix : couleur des étiquettes / libellés. */
        public int prefix   = 0xFFAAAAAA;

        public CustomTheme copy() {
            CustomTheme t = new CustomTheme();
            t.name   = this.name;   t.desc   = this.desc;
            t.value  = this.value;  t.prefix = this.prefix;
            return t;
        }
    }

    private final CustomTheme[] themes = new CustomTheme[MAX];

    private CustomThemeManager() { load(); }

    public static CustomThemeManager getInstance() { return INSTANCE; }

    public CustomTheme get(int idx) {
        return (idx >= 0 && idx < MAX) ? themes[idx] : null;
    }

    public void set(int idx, CustomTheme t) {
        if (idx >= 0 && idx < MAX) themes[idx] = t;
    }

    public void delete(int idx) {
        if (idx >= 0 && idx < MAX) themes[idx] = null;
    }

    /** Retourne le premier slot vide, ou -1 si tous pris. */
    public int firstEmptySlot() {
        for (int i = 0; i < MAX; i++) if (themes[i] == null) return i;
        return -1;
    }

    /**
     * Applique ce thème à tous les widgets via UIManager.
     * Méthode statique utilisable depuis HudProfileManager lors du chargement de profil.
     */
    public static void applyToWidgets(CustomTheme ct, UIManager ui) {
        if (ct == null || ui == null) return;
        for (UIElement el : ui.all()) {
            if (!(el instanceof BaseWidget)) continue;
            BaseWidget bw = (BaseWidget) el;
            if (bw instanceof FactionZoneWidget) continue; // FactionZone non géré par 2 couleurs
            bw.setRGBMode(false);
            bw.setColor(ct.value);
            if (bw.supportsLabelColor()) {
                bw.getProps().put("colorLabel", ct.prefix);
                bw.getProps().put("syncColors", false);
            }
        }
        UITheme.set(ct.prefix, ct.value);
    }

    // ---- Persistance ----

    public void save() {
        try {
            File f = new File(Minecraft.getMinecraft().mcDataDir, PATH);
            f.getParentFile().mkdirs();
            List<Object> list = new ArrayList<>();
            for (CustomTheme t : themes) {
                if (t == null) { list.add(null); continue; }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name",   t.name);   m.put("desc",   t.desc);
                m.put("value",  t.value);  m.put("prefix", t.prefix);
                list.add(m);
            }
            try (Writer w = new FileWriter(f)) { GSON.toJson(list, w); }
        } catch (Exception e) {
            System.err.println("[CustomThemeManager] save failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void load() {
        try {
            File f = new File(Minecraft.getMinecraft().mcDataDir, PATH);
            if (!f.exists()) return;
            List<?> list;
            try (Reader r = new FileReader(f)) { list = GSON.fromJson(r, List.class); }
            if (list == null) return;
            for (int i = 0; i < Math.min(list.size(), MAX); i++) {
                Object raw = list.get(i);
                if (!(raw instanceof Map)) continue;
                Map<String, Object> m = (Map<String, Object>) raw;
                CustomTheme t = new CustomTheme();
                if (m.containsKey("name"))   t.name   = String.valueOf(m.get("name"));
                if (m.containsKey("desc"))   t.desc   = String.valueOf(m.get("desc"));
                if (m.containsKey("value"))  t.value  = ((Number) m.get("value")).intValue();
                if (m.containsKey("prefix")) t.prefix = ((Number) m.get("prefix")).intValue();
                themes[i] = t;
            }
        } catch (Exception e) {
            System.err.println("[CustomThemeManager] load failed: " + e.getMessage());
        }
    }
}
