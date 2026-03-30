package net.minecraft.client.gui.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.VisualOptionsConfig;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiVisualEditor extends GuiScreen {
    private final GuiScreen parent;
    private VisualElement selected = null;
    private final List<VisualElement> elements = new ArrayList<>();
    
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;
    
    private float sidebarAnim = 0.0f;
    private int sidebarX, sidebarY, sidebarW = 180, sidebarH;
    
    // Simulation state
    private long lastSimHit = 0;
    private int simCombo = 0;

    public GuiVisualEditor(GuiScreen parent) {
        this.parent = parent;
        setupElements();
    }

    private void setupElements() {
        elements.clear();
        // Element proxy pour le Hit Marker
        elements.add(new VisualElement("hitmarker", "Hit Marker") {
            @Override public int getX() { return width / 2 + VisualOptionsConfig.getInstance().hitMarkerOffsetX; }
            @Override public int getY() { return height / 2 + VisualOptionsConfig.getInstance().hitMarkerOffsetY; }
            @Override public void setPos(int x, int y) {
                VisualOptionsConfig.getInstance().hitMarkerOffsetX = x - width / 2;
                VisualOptionsConfig.getInstance().hitMarkerOffsetY = y - height / 2;
            }
            @Override public int getW() { return 20; }
            @Override public int getH() { return 20; }
            @Override public void render(int mx, int my) {
                int color = (150 << 24) | (VisualOptionsConfig.getInstance().hitMarkerColor & 0xFFFFFF);
                HitMarkerRenderer.getInstance().renderAtScreen(getX(), getY(), 6, color);
            }
        });

        // Element proxy pour le Combo Counter
        elements.add(new VisualElement("combo", "Combo Counter") {
            @Override public int getX() { return ComboCounter.getInstance().getPreviewPosition(new ScaledResolution(mc))[0]; }
            @Override public int getY() { return ComboCounter.getInstance().getPreviewPosition(new ScaledResolution(mc))[1]; }
            @Override public void setPos(int x, int y) {
                VisualOptionsConfig.getInstance().comboCounterX = x - width / 2; // Simplifié pour la preview
                VisualOptionsConfig.getInstance().comboCounterY = y - height / 2;
            }
            @Override public int getW() { return 40; }
            @Override public int getH() { return 20; }
            @Override public void render(int mx, int my) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(getX(), getY(), 0);
                fontRendererObj.drawStringWithShadow("x7", 0, 0, 0xFFFFA500);
                GlStateManager.scale(0.6f, 0.6f, 1.0f);
                fontRendererObj.drawStringWithShadow("COMBO", 0, 12, 0xFFFFA500);
                GlStateManager.popMatrix();
            }
        });
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 26, 200, 20, "Terminer"));
        sidebarX = 10;
        sidebarY = 30;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawGrid(10, 0x0AFFFFFF);

        // Simulation des effets en arrière-plan
        updateSimulation();

        // Rendu des éléments éditables
        for (VisualElement e : elements) {
            e.render(mouseX, mouseY);
            if (selected == e) {
                drawSelectionHalo(e.getX(), e.getY(), e.getW(), e.getH());
            } else if (e.isHovered(mouseX, mouseY)) {
                drawRect(e.getX() - e.getW()/2, e.getY() - e.getH()/2, e.getX() + e.getW()/2, e.getY() + e.getH()/2, 0x22FFFFFF);
            }
        }

        // Rendu des indicateurs de dégâts (simulation)
        DamageIndicatorRenderer.getInstance().render(partialTicks);

        // Panneau latéral
        sidebarAnim = lerp(sidebarAnim, (selected != null ? 1.0f : 0.0f), 0.2f);
        if (sidebarAnim > 0.01f) {
            drawSidebar(mouseX, mouseY);
        }

        // Overlay Title
        String txt = "EDITEUR DE VISUELS PVP";
        fontRendererObj.drawStringWithShadow(txt, (width - fontRendererObj.getStringWidth(txt)) / 2, 10, 0xFFFF5555);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void updateSimulation() {
        long now = System.currentTimeMillis();
        if (now - lastSimHit > 1200) {
            lastSimHit = now;
            simCombo++;
            if (simCombo > 15) simCombo = 1;
            
            // Trigger Hitmarker & Particles
            HitMarkerRenderer.getInstance().onHit(simCombo % 5 == 0, simCombo % 3 == 0);
            
            // On simule un indicateur de dégâts aléatoire au centre
            if (VisualOptionsConfig.getInstance().damageIndicatorEnabled) {
                // Hack: on crée une fausse entité pour le renderer
                // On va plutôt appeler directement le renderer avec des coordonnées écran si possible
                // Mais pour l'instant on se concentre sur le HitMarker/Combo
            }
        }
    }

    private void drawSidebar(int mx, int my) {
        int x = (int) (sidebarX - (1.0f - sidebarAnim) * 200);
        int w = sidebarW;
        int h = height - 60;
        sidebarH = h;

        // Panel background (style pro sombre)
        drawRect(x, sidebarY, x + w, sidebarY + h, 0xEE0A0A0A);
        drawRect(x + w - 1, sidebarY, x + w, sidebarY + h, 0xFFCC3333); // Bordure droite rouge
        
        fontRendererObj.drawStringWithShadow("§l" + selected.name.toUpperCase(), x + 10, sidebarY + 10, 0xFFCC3333);
        
        int y = sidebarY + 30;
        VisualOptionsConfig cfg = VisualOptionsConfig.getInstance();

        if (selected.id.equals("hitmarker")) {
            drawOption(x, y, "Activé", cfg.hitMarkerEnabled); y += 25;
            drawOption(x, y, "Taille: " + cfg.hitMarkerSize, false); y += 25;
            drawOption(x, y, "Opacité: " + (int)(cfg.hitMarkerOpacity * 100) + "%", false); y += 25;
            drawOption(x, y, "Son Hit", cfg.hitMarkerSound); y += 25;
        } else if (selected.id.equals("combo")) {
            drawOption(x, y, "Activé", cfg.comboCounterEnabled); y += 25;
            drawOption(x, y, "Echelle: " + (int)(cfg.comboCounterScale * 100) + "%", false); y += 25;
            drawOption(x, y, "Ancre: " + cfg.comboAnchor, false); y += 25;
        }
    }

    private void drawOption(int x, int y, String name, boolean enabled) {
        fontRendererObj.drawStringWithShadow(name, x + 15, y + 2, 0xFFCCCCCC);
        if (enabled) drawRect(x + sidebarW - 30, y, x + sidebarW - 10, y + 10, 0xFF55FF55);
        else drawRect(x + sidebarW - 30, y, x + sidebarW - 10, y + 10, 0xFFFF5555);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        
        // Clic sur la sidebar
        if (selected != null && mx < sidebarX + sidebarW) {
            handleSidebarClick(mx, my);
            return;
        }

        // Sélection d'un élément
        for (VisualElement e : elements) {
            if (e.isHovered(mx, my)) {
                selected = e;
                dragging = true;
                dragOffsetX = mx - e.getX();
                dragOffsetY = my - e.getY();
                return;
            }
        }
        selected = null;
    }

    private void handleSidebarClick(int mx, int my) {
        VisualOptionsConfig cfg = VisualOptionsConfig.getInstance();
        int y = sidebarY + 30;
        if (selected.id.equals("hitmarker")) {
            if (my >= y && my < y + 20) cfg.hitMarkerEnabled = !cfg.hitMarkerEnabled;
            y += 25;
            if (my >= y && my < y + 20) {
                if (cfg.hitMarkerSize.equals("Petit")) cfg.hitMarkerSize = "Moyen";
                else if (cfg.hitMarkerSize.equals("Moyen")) cfg.hitMarkerSize = "Grand";
                else cfg.hitMarkerSize = "Petit";
            }
            y += 25;
            if (my >= y && my < y + 20) {
                cfg.hitMarkerOpacity += 0.2f;
                if (cfg.hitMarkerOpacity > 1.0f) cfg.hitMarkerOpacity = 0.2f;
            }
        }
        // etc pour les autres éléments...
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (dragging && selected != null) {
            selected.setPos(mx - dragOffsetX, my - dragOffsetY);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        dragging = false;
        VisualOptionsConfig.getInstance().save();
        super.mouseReleased(mx, my, state);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 200) mc.displayGuiScreen(parent);
    }

    @Override
    public void onGuiClosed() {
        VisualOptionsConfig.getInstance().save();
    }

    // Helper methods
    private void drawGrid(int step, int color) {
        for (int i = 0; i < width; i += step) drawRect(i, 0, i + 1, height, color);
        for (int i = 0; i < height; i += step) drawRect(0, i, width, i + 1, color);
    }

    private void drawSelectionHalo(int x, int y, int w, int h) {
        int color = 0xFFCC3333;
        drawRect(x - w/2 - 2, y - h/2 - 2, x + w/2 + 2, y - h/2 - 1, color); // Top
        drawRect(x - w/2 - 2, y + h/2 + 1, x + w/2 + 2, y + h/2 + 2, color); // Bottom
        drawRect(x - w/2 - 2, y - h/2 - 2, x - w/2 - 1, y + h/2 + 2, color); // Left
        drawRect(x + w/2 + 1, y - h/2 - 2, x + w/2 + 2, y + h/2 + 2, color); // Right
    }

    private float lerp(float a, float b, float f) { return a + f * (b - a); }

    abstract static class VisualElement {
        String id, name;
        VisualElement(String id, String name) { this.id = id; this.name = name; }
        abstract int getX(); abstract int getY();
        abstract void setPos(int x, int y);
        abstract int getW(); abstract int getH();
        abstract void render(int mx, int my);
        boolean isHovered(int mx, int my) {
            return mx >= getX() - getW()/2 && mx <= getX() + getW()/2 && my >= getY() - getH()/2 && my <= getY() + getH()/2;
        }
    }
}
