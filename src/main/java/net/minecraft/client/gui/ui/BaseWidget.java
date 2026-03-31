package net.minecraft.client.gui.ui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.GuiRenderUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseWidget implements UIElement {
    protected final String id;
    protected int x, y;
    protected int width = 60, height = 12;
    protected int minWidth = 20, minHeight = 10, maxWidth = 400, maxHeight = 300;
    public int defaultWidth = 60;
    public int defaultHeight = 12;
    protected boolean enabled = true;
    protected int color = 0xFFFFFFFF;
    protected boolean rgbMode = false;
    protected float scale = 1.0f;

    protected final Map<String, Object> props = new HashMap<>();

    protected double relX = -1.0d, relY = -1.0d;
    protected int refW = -1, refH = -1;
    protected int refWidgetW = -1, refWidgetH = -1;

    protected String alignX = "LEFT";
    protected String alignY = "TOP";

    protected int lastMouseX = 0;
    protected int lastMouseY = 0;
    // absolute screen position of this widget saved during render so draw() can use it
    protected int renderAbsX = 0;
    protected int renderAbsY = 0;

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

    @Override public int getWidth() { return (int) (width * scale); }
    @Override public int getHeight() { return (int) (height * scale); }

    @Override
    public void setWidth(int w) {
        this.width = Math.max(minWidth, Math.min(maxWidth, w));
    }

    @Override
    public void setHeight(int h) {
        this.height = Math.max(minHeight, Math.min(maxHeight, h));
    }

    @Override
    public float getScale() {
        return scale;
    }

    @Override
    public void setScale(float scale) {
        this.scale = Math.max(0.5f, Math.min(2.0f, scale));
    }

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
                int maxX = Math.max(1, sw - this.getWidth());
                int maxY = Math.max(1, sh - this.getHeight());
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

    public int getMinWidth() { return minWidth; }
    public int getMinHeight() { return minHeight; }
    public int getMaxWidth() { return maxWidth; }
    public int getMaxHeight() { return maxHeight; }

    public Object getProp(String key) { return props.get(key); }
    public Object getPropOrDefault(String key, Object def) { return props.getOrDefault(key, def); }
    public void setProp(String key, Object value) { props.put(key, value); }
    public Map<String, Object> getProps() { return props; }

    public void updateAbsolutePosition() {
        if (relX < 0.0d || relY < 0.0d) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            try {
                ScaledResolution sr = new ScaledResolution(mc);
                int sw = sr.getScaledWidth();
                int sh = sr.getScaledHeight();
                if (sw > 0 && sh > 0) {
                    // Only recalculate when the screen resolution has changed since last save/positioning.
                    if (this.refW == sw && this.refH == sh) {
                        return;
                    }
                    int curW = this.getWidth();
                    int curH = this.getHeight();
                    int newMaxX = Math.max(1, sw - curW);
                    int newMaxY = Math.max(1, sh - curH);
                    int nx = (int) Math.round(this.relX * newMaxX);
                    int ny = (int) Math.round(this.relY * newMaxY);
                    this.x = Math.max(0, Math.min(sw - curW, nx));
                    this.y = Math.max(0, Math.min(sh - curH, ny));
                    // Update screen reference and record the widget size at time of recalculation
                    this.refW = sw;
                    this.refH = sh;
                    this.refWidgetW = curW;
                    this.refWidgetH = curH;
                 }
             } catch (Throwable ignored) {}
         }
     }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        if (!enabled) return;

        boolean editorActive = UIManager.getInstance().isEditorActive();
        if (!editorActive) {
            updateAbsolutePosition();
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(this.x, this.y, 0);
        GlStateManager.scale(this.scale, this.scale, 1.0f);

        boolean showBg = Boolean.TRUE.equals(getPropOrDefault("showBackground", Boolean.FALSE));
        if (showBg) {
            int bgCol = 0x88000000; // Default dark semi-transparent
            int outlineCol = getColor();

            GlStateManager.enableBlend();
            Gui.drawRect(-2, -2, width + 2, height + 2, bgCol);
            GuiRenderUtils.drawRectOutline(-2, -2, width + 4, height + 4, (0xAA << 24) | (outlineCol & 0xFFFFFF));
        }

        try { 
            // We pass 0,0 as base coordinates for internal draw call. Save absolute position for draw()
            int oldX = this.x;
            int oldY = this.y;
            this.renderAbsX = oldX;
            this.renderAbsY = oldY;
            this.x = 0;
            this.y = 0;
            draw();
            this.x = oldX;
            this.y = oldY;
        } catch (Exception e) {
            e.printStackTrace(); 
        }
        
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected abstract void draw();
}
