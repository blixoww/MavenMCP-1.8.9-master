package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.renderer.GlStateManager;

public class CrosshairWidget extends BaseWidget {

    public CrosshairWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 32;
        this.height = 32;
        this.setEnabled(false); // désactivé par défaut → vanilla Minecraft
        setColor(0xFFFFFFFF);
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        GameSettings gs = mc.gameSettings;
        boolean editorActive = UIManager.getInstance().isEditorActive();

        // ── Couleur avec rainbow ─────────────────────────────────────────────
        int color = gs.crosshairColor;
        if ((color & 0xFF000000) == 0) color |= 0xFF000000;
        // Rainbow : priorité à gs.crosshairRainbow (contrôlé par l'éditeur)
        if (gs.crosshairRainbow) {
            float hue = (System.currentTimeMillis() % 3000L) / 3000.0f;
            color = 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0x00FFFFFF);
        }

        if (editorActive) {
            // En éditeur : aperçu à la position du widget
            int cx = this.x + this.width / 2;
            int cy = this.y + this.height / 2;
            drawCrosshairAt(cx, cy, gs, color);
        } else {
            if (!this.isEnabled()) return;
            ScaledResolution sr = new ScaledResolution(mc);
            int cx = sr.getScaledWidth() / 2;
            int cy = sr.getScaledHeight() / 2;
            drawCrosshairAt(cx, cy, gs, color);
        }
    }

    private void drawCrosshairAt(int cx, int cy, GameSettings gs, int color) {
        drawCrosshairAtStatic(cx, cy, gs, color);
    }

    // Méthode utilitaire statique pour être réutilisée depuis GuiIngame
    private static void drawCrosshairAtStatic(int cx, int cy, GameSettings gs, int color) {
        int size      = Math.max(1, gs.crosshairSize);
        int thickness = Math.max(1, gs.crosshairThickness);
        int gap       = Math.max(0, gs.crosshairGap);
        int type      = gs.crosshairType;
        int half      = Math.max(1, thickness / 2);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        switch (type) {
            case 1: // CS:GO : 4 branches + gap
                Gui.drawRect(cx - half, cy - size - gap, cx + half, cy - gap,        color);
                Gui.drawRect(cx - half, cy + gap,        cx + half, cy + size + gap,  color);
                Gui.drawRect(cx - size - gap, cy - half, cx - gap,  cy + half,        color);
                Gui.drawRect(cx + gap,        cy - half, cx + size + gap, cy + half,  color);
                break;
            case 2: // Point
                Gui.drawRect(cx - half, cy - half, cx + half, cy + half, color);
                break;
            default: // Vanilla modifiable (type 0)
                // Dessin inspiré du crosshair vanilla mais respectant taille/épaisseur/couleur
                Gui.drawRect(cx - half, cy - size, cx + half, cy + size, color);
                Gui.drawRect(cx - size, cy - half, cx + size, cy + half, color);
                break;
        }

        GlStateManager.disableBlend();
    }

    // Méthode publique réutilisable : dessine le crosshair centré selon game settings
    public static void renderFromSettings(GameSettings gs, int cx, int cy) {
        if (gs == null) return;
        int color = gs.crosshairColor;
        if ((color & 0xFF000000) == 0) color |= 0xFF000000;
        if (gs.crosshairRainbow) {
            float hue = (System.currentTimeMillis() % 3000L) / 3000.0f;
            color = 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0x00FFFFFF);
        }
        drawCrosshairAtStatic(cx, cy, gs, color);
        // Toujours restaurer la couleur GL à blanc opaque après le rendu du crosshair
        // pour éviter de teinter les textures HUD vanilla suivantes (coeurs, nourriture, armure)
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
