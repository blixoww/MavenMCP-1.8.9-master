package net.minecraft.client.gui.ui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseWidget implements UIElement {
    protected final String id;
    protected int x, y;
    protected int width = 60, height = 12;
    protected boolean enabled = true;
    protected int color = 0xFFFFFFFF;
    protected boolean rgbMode = false;

    protected final Map<String, Object> props = new HashMap<>();

    // Position relative : stockée en fraction de l'écran lors d'un setPosition() explicite
    // -1 = non initialisée (utiliser x/y absolus tels quels)
    protected double relX = -1.0d, relY = -1.0d;
    // Résolution de référence au moment du dernier setPosition()
    protected int refW = -1, refH = -1;
    // Taille du widget au moment du positionnement (pour un recalcul exact)
    protected int refWidgetW = -1, refWidgetH = -1;

    // Alignement (non utilisé pour le calcul mais conservé pour compatibilité sauvegarde)
    protected String alignX = "LEFT";
    protected String alignY = "TOP";

    protected int lastMouseX = 0;
    protected int lastMouseY = 0;

    public BaseWidget(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    @Override public String getId() { return id; }
    @Override public int getX() { return x; }
    @Override public int getY() { return y; }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        // Mémoriser la fraction uniquement si on a accès à la résolution
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            try {
                ScaledResolution sr = new ScaledResolution(mc);
                int sw = sr.getScaledWidth();
                int sh = sr.getScaledHeight();
                if (sw > 0 && sh > 0) {
                    int maxX = Math.max(1, sw - this.getWidth());
                    int maxY = Math.max(1, sh - this.getHeight());
                    this.relX = (double) x / (double) maxX;
                    this.relY = (double) y / (double) maxY;
                    this.refW = sw;
                    this.refH = sh;
                    this.refWidgetW = this.getWidth();
                    this.refWidgetH = this.getHeight();
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }

    @Override
    public boolean containsPoint(int mx, int my) {
        return mx >= x && my >= y && mx <= x + getWidth() && my <= y + getHeight();
    }

    public void updateAlignment() {
        if (relX < 0 || relY < 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        try {
            ScaledResolution sr = new ScaledResolution(mc);
            int sw = sr.getScaledWidth();
            int sh = sr.getScaledHeight();
            if (sw > 0 && sh > 0) {
                // Utiliser la taille actuelle du widget, ou celle mémorisée si le recalc n'est pas encore fait
                int w = this.getWidth();
                int h = this.getHeight();
                if (refWidgetW > 0) w = refWidgetW;
                if (refWidgetH > 0) h = refWidgetH;

                int maxX = Math.max(1, sw - w);
                int maxY = Math.max(1, sh - h);
                this.x = (int) Math.round(relX * maxX);
                this.y = (int) Math.round(relY * maxY);
            }
        } catch (Throwable ignored) {}
    }

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @Override
    public int getColor() {
        if (rgbMode) {
            long t = System.currentTimeMillis();
            float hue = (t % 10000L) / 10000.0f;
            int c = java.awt.Color.HSBtoRGB(hue, 0.8f, 0.9f);
            return (color & 0xFF000000) | (c & 0x00FFFFFF);
        }
        return color;
    }

    @Override public void setColor(int rgba) { this.color = rgba; }
    @Override public boolean isRGBMode() { return rgbMode; }
    @Override public void setRGBMode(boolean v) { this.rgbMode = v; }

    public Object getProp(String key) { return props.get(key); }
    public Object getPropOrDefault(String key, Object def) { return props.getOrDefault(key, def); }
    public void setProp(String key, Object value) { props.put(key, value); }
    public Map<String, Object> getProps() { return props; }

    public void setAlignment(String ax, String ay) {
        this.alignX = ax;
        this.alignY = ay;
    }

    // Met à jour relX/relY depuis la position absolue courante
    public void updateRelativePosition() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            try {
                ScaledResolution sr = new ScaledResolution(mc);
                int sw = sr.getScaledWidth();
                int sh = sr.getScaledHeight();
                if (sw > 0 && sh > 0) {
                    int maxX = Math.max(1, sw - this.getWidth());
                    int maxY = Math.max(1, sh - this.getHeight());
                    this.relX = (double) this.x / (double) maxX;
                    this.relY = (double) this.y / (double) maxY;
                    this.refW = sw;
                    this.refH = sh;
                }
            } catch (Throwable ignored) {}
        }
    }

    // Recalcule la position absolue depuis relX/relY si la résolution a changé
    public void updateAbsolutePosition() {
        if (relX < 0.0d || relY < 0.0d) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            try {
                ScaledResolution sr = new ScaledResolution(mc);
                int sw = sr.getScaledWidth();
                int sh = sr.getScaledHeight();
                if (sw > 0 && sh > 0 && (sw != refW || sh != refH)) {
                    int newMaxX = Math.max(1, sw - this.getWidth());
                    int newMaxY = Math.max(1, sh - this.getHeight());
                    int nx = (int) Math.round(this.relX * newMaxX);
                    int ny = (int) Math.round(this.relY * newMaxY);
                    nx = Math.max(0, Math.min(sw - this.getWidth(), nx));
                    ny = Math.max(0, Math.min(sh - this.getHeight(), ny));
                    this.x = nx;
                    this.y = ny;
                    this.refW = sw;
                    this.refH = sh;
                    this.refWidgetW = this.getWidth();
                    this.refWidgetH = this.getHeight();
                }
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (!enabled) return;

        boolean editorActive = false;
        try { editorActive = UIManager.getInstance().isEditorActive(); } catch (Throwable ignored) {}

        // Adapter la position si la résolution a changé (seulement hors éditeur)
        if (!editorActive) {
            updateAbsolutePosition();
        }

        // Rainbow mode
        if (rgbMode) {
            long t = System.currentTimeMillis();
            float hue = (t % 10000L) / 10000.0f;
            int c = java.awt.Color.HSBtoRGB(hue, 0.8f, 0.9f);
            // ne pas modifier this.color, juste afficher
        }

        boolean showBg = Boolean.TRUE.equals(getPropOrDefault("showBackground", Boolean.FALSE));
        if (showBg) {
            int drawColor = getColor();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            Gui.drawRect(this.x - 2, this.y - 2, this.x + getWidth() + 2, this.y + getHeight() + 2, drawColor);
            Gui.drawRect(this.x - 2, this.y - 2, this.x - 1, this.y + getHeight() + 2, 0xFF000000);
            Gui.drawRect(this.x + getWidth() + 1, this.y - 2, this.x + getWidth() + 2, this.y + getHeight() + 2, 0xFF000000);
            Gui.drawRect(this.x - 2, this.y - 2, this.x + getWidth() + 2, this.y - 1, 0xFF000000);
            Gui.drawRect(this.x - 2, this.y + getHeight() + 1, this.x + getWidth() + 2, this.y + getHeight() + 2, 0xFF000000);
            GlStateManager.disableBlend();
        }

        try { draw(); } catch (Exception e) { e.printStackTrace(); }
        // Toujours restaurer la couleur OpenGL à blanc opaque après le dessin du widget
        // pour éviter que les couleurs RGB/custom contaminent d'autres rendus (HUD vanilla, textures, etc.)
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected abstract void draw();
}
