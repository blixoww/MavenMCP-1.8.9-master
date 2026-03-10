package net.minecraft.client.gui.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class UIManager {
    private static final String CONFIG = "config/ui_widgets.json";
    private final Map<String, UIElement> widgets = new LinkedHashMap<>();
    private final Gson gson = new Gson();
    private static final UIManager INSTANCE = new UIManager();
    private boolean editorActive = false;
    private boolean needsPositionRecalc = false;

    public static UIManager getInstance() {
        return INSTANCE;
    }

    public void setEditorActive(boolean v) {
        this.editorActive = v;
    }

    public boolean isEditorActive() {
        return this.editorActive;
    }

    private UIManager() {
        SimpleTextWidget fpsW = new SimpleTextWidget("fps", 10, 10, "FPS: 0");
        fpsW.setColor(0xFFFFFFFF);
        register(fpsW);
        SimpleTextWidget pingW = new SimpleTextWidget("ping", 10, 24, "Ping: --");
        pingW.setColor(0xFFFFFFFF);
        register(pingW);
        BiomeWidget biomeW = new BiomeWidget("biome", 10, 38);
        biomeW.setColor(0xFFFFFFFF);
        register(biomeW);
        CoordsWidget coordsW = new CoordsWidget("coords", 10, 52);
        coordsW.setColor(0xFFFFFFFF);
        register(coordsW);
        DirectionWidget dirW = new DirectionWidget("dir", 10, 66);
        dirW.setColor(0xFFFFFFFF);
        register(dirW);
        DateWidget dateW = new DateWidget("date", 10, 80);
        dateW.setColor(0xFFFFFFFF);
        register(dateW);
        HeldItemDurabilityWidget heldW = new HeldItemDurabilityWidget("helditem", 10, 94);
        heldW.setColor(0xFFFFFFFF);
        register(heldW);
        ArmorGroupWidget armorW = new ArmorGroupWidget("armor_group", 10, 122);
        armorW.setProp("layout", "vertical");
        armorW.setProp("displayPercent", Boolean.TRUE);
        register(armorW);
        PotionStatusWidget potW = new PotionStatusWidget("potions", 10, 194);
        potW.setColor(0xFFFFFFFF);
        register(potW);
        // CPS widget
        register(new CPSWidget("cps", 10, 170));
        ToggleSneakWidget toggleSneakW = new ToggleSneakWidget("toggle_sneak", 10, 210);
        toggleSneakW.setColor(0xFF44EE77);
        register(toggleSneakW);
        ToggleSprintWidget toggleSprintW = new ToggleSprintWidget("toggle_sprint", 90, 210);
        toggleSprintW.setColor(0xFF44EE77);
        register(toggleSprintW);
        boolean loaded = loadConfig();
        if (!loaded) {
            // Par defaut: tout desactive sauf FPS
            for (UIElement e : widgets.values()) {
                if (!"fps".equals(e.getId())) {
                    e.setEnabled(false);
                }
            }
            autoArrangeDefaults();
        }
        for (UIElement e : widgets.values()) {
            if (e instanceof BaseWidget) {
                BaseWidget bw = (BaseWidget) e;
                try {
                    bw.getProps().remove("editorPreview");
                    bw.getProps().remove("previewEffect");
                    bw.getProps().remove("preview");
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void autoArrangeDefaults() {
        List<UIElement> list = new ArrayList<>(widgets.values());
        for (int i = 0; i < list.size(); i++) {
            UIElement a = list.get(i);
            if (!a.isEnabled()) continue;
            int ax = a.getX();
            int ay = a.getY();
            int ah = a.getHeight();
            for (int j = 0; j < i; j++) {
                UIElement b = list.get(j);
                if (!b.isEnabled()) continue;
                int bx = b.getX();
                int by = b.getY();
                int bh = b.getHeight();
                // if overlap in X range (simple), and vertical overlap
                if (ax < bx + b.getWidth() && ax + a.getWidth() > bx) {
                    if (ay < by + bh && ay + ah > by) {
                        ay = by + bh + 4;
                    }
                }
            }
            a.setPosition(ax, ay);
        }
    }

    public void register(UIElement widget) {
        widgets.put(widget.getId(), widget);
    }

    public void updateAllWidgetAlignment() {
        for (UIElement e : widgets.values()) {
            if (e instanceof BaseWidget) {
                ((BaseWidget) e).updateAlignment();
            }
        }
    }

    public UIElement get(String id) {
        return widgets.get(id);
    }

    public Collection<UIElement> all() {
        return widgets.values();
    }

    public void renderAll(int mouseX, int mouseY, float partialTicks) {
        // Recalculate proportional positions after config load (resolution is now known)
        if (needsPositionRecalc) {
            needsPositionRecalc = false;
            for (UIElement e : widgets.values()) {
                if (e instanceof BaseWidget) {
                    ((BaseWidget) e).updateAbsolutePosition();
                }
            }
        }

        // update dynamic values for simple widgets
        try {
            Minecraft mc = Minecraft.getMinecraft();
            UIElement fpsEl = widgets.get("fps");
            if (fpsEl instanceof SimpleTextWidget) {
                ((SimpleTextWidget) fpsEl).setText("FPS: " + Minecraft.getDebugFPS());
            }
            UIElement pingEl = widgets.get("ping");
            if (pingEl instanceof SimpleTextWidget) {
                String pingStr = "--";
                try {
                    if (mc != null && mc.getNetHandler() != null && mc.thePlayer != null) {
                        try {
                            java.util.Collection<?> col = mc.getNetHandler().getPlayerInfoMap();
                            Object info = null;
                            try {
                                info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                            } catch (Throwable t) {
                                info = null;
                            }
                            if (info == null && col != null && !col.isEmpty()) info = col.iterator().next();
                            if (info != null) {
                                try {
                                    java.lang.reflect.Method mresp = info.getClass().getMethod("getResponseTime");
                                    Object v = mresp.invoke(info);
                                    pingStr = String.valueOf(v);
                                } catch (NoSuchMethodException ns) {
                                    // fallback: try field 'responseTime'
                                    try {
                                        java.lang.reflect.Field f = info.getClass().getDeclaredField("responseTime");
                                        f.setAccessible(true);
                                        Object v = f.get(info);
                                        pingStr = String.valueOf(v);
                                    } catch (Throwable ex) {
                                        pingStr = "--";
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            pingStr = "--";
                        }
                    }
                } catch (Throwable t) {
                    pingStr = "--";
                }
                ((SimpleTextWidget) pingEl).setText("Ping: " + pingStr);
            }
        } catch (Throwable t) { /* ignore */ }

        for (UIElement w : widgets.values()) {
            try {
                w.render(mouseX, mouseY, partialTicks);
            } catch (Throwable t) { /* log minimal */
                System.err.println("UI widget render error: " + t.getMessage());
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public void saveConfig() {
        try {
            File out = new File(Minecraft.getMinecraft().mcDataDir, CONFIG);
            out.getParentFile().mkdirs();
            Map<String, Map<String, Object>> map = new LinkedHashMap<>();
            for (UIElement e : widgets.values()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("x", e.getX());
                m.put("y", e.getY());
                m.put("enabled", e.isEnabled());
                m.put("color", e.getColor());
                m.put("rgb", e.isRGBMode());
                // Sauvegarde alignement proportionnel si BaseWidget
                if (e instanceof BaseWidget) {
                    BaseWidget bw = (BaseWidget) e;
                    m.put("relX", bw.relX);
                    m.put("relY", bw.relY);
                    m.put("refW", bw.refW);
                    m.put("refH", bw.refH);
                    m.put("refWidgetW", bw.refWidgetW);
                    m.put("refWidgetH", bw.refWidgetH);
                }
                // persist props if BaseWidget
                if (e instanceof BaseWidget) {
                    // create a sanitized copy of props: do not persist editor-only transient keys
                    Map<String, Object> propsCopy = new LinkedHashMap<>();
                    Map<String, Object> original = ((BaseWidget) e).getProps();
                    for (Map.Entry<String, Object> pe : original.entrySet()) {
                        String k = pe.getKey();
                        if ("editorPreview".equals(k) || "previewEffect".equals(k)) continue; // transient
                        propsCopy.put(k, pe.getValue());
                    }
                    m.put("props", propsCopy);
                }
                map.put(e.getId(), m);
            }
            try (Writer w = new FileWriter(out)) {
                gson.toJson(map, w);
            }
        } catch (Exception e) {
            System.err.println("Failed to save ui widgets: " + e.getMessage());
        }
    }

    public boolean loadConfig() {
        try {
            File in = new File(Minecraft.getMinecraft().mcDataDir, CONFIG);
            if (!in.exists()) return false;
            Type t = new TypeToken<Map<String, Map<String, Object>>>() {
            }.getType();
            Map<String, Map<String, Object>> map;
            try (Reader r = new FileReader(in)) {
                map = gson.fromJson(r, t);
            }
            if (map == null) return false;
            for (Map.Entry<String, Map<String, Object>> en : map.entrySet()) {
                UIElement e = widgets.get(en.getKey());
                if (e == null) continue;
                Map<String, Object> m = en.getValue();
                if (m.containsKey("x") && m.containsKey("y")) {
                    Number nx = (Number) m.get("x");
                    Number ny = (Number) m.get("y");
                    e.setPosition(nx.intValue(), ny.intValue());
                }
                if (e instanceof BaseWidget) {
                    BaseWidget bw = (BaseWidget) e;
                    if (m.containsKey("relX")) bw.relX = ((Number) m.get("relX")).doubleValue();
                    if (m.containsKey("relY")) bw.relY = ((Number) m.get("relY")).doubleValue();
                    if (m.containsKey("refW")) bw.refW = ((Number) m.get("refW")).intValue();
                    if (m.containsKey("refH")) bw.refH = ((Number) m.get("refH")).intValue();
                    if (m.containsKey("refWidgetW")) bw.refWidgetW = ((Number) m.get("refWidgetW")).intValue();
                    if (m.containsKey("refWidgetH")) bw.refWidgetH = ((Number) m.get("refWidgetH")).intValue();
                    // Ne pas appeler updateAbsolutePosition ici : la résolution n'est pas encore connue
                }
                if (m.containsKey("enabled")) e.setEnabled(Boolean.parseBoolean(String.valueOf(m.get("enabled"))));
                if (m.containsKey("color")) e.setColor(((Number) m.get("color")).intValue());
                if (m.containsKey("rgb")) e.setRGBMode(Boolean.parseBoolean(String.valueOf(m.get("rgb"))));
                if (m.containsKey("props") && e instanceof BaseWidget) {
                    try {
                        Object obj = m.get("props");
                        if (obj instanceof Map) {
                            Map<?, ?> pm = (Map<?, ?>) obj;
                            for (Map.Entry<?, ?> pe : pm.entrySet()) {
                                String key = String.valueOf(pe.getKey());
                                Object val = pe.getValue();
                                if ("preview".equals(key) || "editorPreview".equals(key) || "previewEffect".equals(key)) {
                                    continue;
                                }
                                ((BaseWidget) e).setProp(key, val);
                            }
                        }

                    } catch (Throwable ex) { /* ignore */ }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        needsPositionRecalc = true;
        return true;
    }
}
