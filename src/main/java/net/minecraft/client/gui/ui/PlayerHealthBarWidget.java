package net.minecraft.client.gui.ui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

/**
 * Widget barre de vie au-dessus de la tête des joueurs (rendu en monde 3D).
 *
 * Aucun rendu sur le HUD — le rendu réel est assuré par {@link #renderAboveHead}
 * appelé depuis {@code RenderPlayer}. Le widget HUD maintient uniquement des
 * dimensions pour la zone cliquable de l'éditeur.
 *
 * Props de configuration :
 *  - "barWidth"    (Number,  défaut 40)       largeur de la barre en px
 *  - "barHeight"   (Number,  défaut 4)        hauteur de la barre en px
 *  - "showText"    (Boolean, défaut false)    affiche "x/y" sous la barre
 *  - "border"      (Boolean, défaut true)     contour noir 1px
 *  - "colorHP1"    (Number,  défaut vert)     couleur > 75 % HP
 *  - "colorHP2"    (Number,  défaut jaune)    couleur > 50 % HP
 *  - "colorHP3"    (Number,  défaut orange)   couleur > 25 % HP
 *  - "colorHP4"    (Number,  défaut rouge)    couleur ≤ 25 % HP
 *  - isRGBMode()   (via BaseWidget)           active le mode rainbow
 */
public class PlayerHealthBarWidget extends BaseWidget {

    // ── Couleurs par défaut des 4 stades ──────────────────────────────────────
    public static final int DEFAULT_HP1 = 0xFF44DD22;  // > 75 %  vert
    public static final int DEFAULT_HP2 = 0xFFCCCC00;  // > 50 %  jaune
    public static final int DEFAULT_HP3 = 0xFFFF7700;  // > 25 %  orange
    public static final int DEFAULT_HP4 = 0xFFDD2222;  // ≤ 25 %  rouge

    public PlayerHealthBarWidget(String id, int x, int y) {
        super(id, x, y);
        this.width         = 58;
        this.height        = 18;
        this.defaultWidth  = 58;
        this.defaultHeight = 18;
    }

    // ── Accesseurs de propriétés ───────────────────────────────────────────────

    public int getBarWidth() {
        Object o = getPropOrDefault("barWidth", 40);
        return o instanceof Number ? Math.max(10, ((Number) o).intValue()) : 40;
    }

    public int getBarHeight() {
        Object o = getPropOrDefault("barHeight", 4);
        return o instanceof Number ? Math.max(2, ((Number) o).intValue()) : 4;
    }

    public boolean isShowText() {
        return Boolean.TRUE.equals(getPropOrDefault("showText", Boolean.FALSE));
    }

    public boolean isBorderEnabled() {
        return !Boolean.FALSE.equals(getPropOrDefault("border", Boolean.TRUE));
    }

    private int getStageColorProp(String key, int def) {
        Object v = getPropOrDefault(key, def);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    /**
     * Retourne la couleur de remplissage pour le ratio HP donné.
     * Priorité : mode Rainbow > stades configurables.
     */
    public int getFillColor(float ratio) {
        if (isRGBMode()) {
            // Arc-en-ciel qui tourne lentement
            float hue = (System.currentTimeMillis() % 5000L) / 5000.0f;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f);
            return 0xFF000000 | (rgb & 0xFFFFFF);
        }
        if (ratio > 0.75f) return getStageColorProp("colorHP1", DEFAULT_HP1);
        if (ratio > 0.50f) return getStageColorProp("colorHP2", DEFAULT_HP2);
        if (ratio > 0.25f) return getStageColorProp("colorHP3", DEFAULT_HP3);
        return getStageColorProp("colorHP4", DEFAULT_HP4);
    }

    /** Couleur statique par ratio (rétrocompatibilité). */
    public static int colorForRatio(float r) {
        if (r > 0.75f) return DEFAULT_HP1;
        if (r > 0.50f) return DEFAULT_HP2;
        if (r > 0.25f) return DEFAULT_HP3;
        return DEFAULT_HP4;
    }

    // ── Aperçu éditeur ────────────────────────────────────────────────────────

    /**
     * Le widget ne s'affiche PAS sur le HUD — uniquement via {@link #renderAboveHead}.
     * On met à jour les dimensions pour que l'éditeur puisse afficher une zone cliquable.
     */
    @Override
    protected void draw() {
        int bw = getBarWidth();
        int bh = getBarHeight();
        this.height = bh + 4;
        this.width  = Math.max(55, bw + 4);
    }


    // ── Rendu 3D au-dessus de la tête ─────────────────────────────────────────

    /**
     * Dessine la barre de vie au-dessus de la tête de {@code entity}.
     *
     * @param rm        RenderManager (orientation caméra)
     * @param entity    cible à afficher
     * @param x, y, z  position relative dans le monde
     * @param textScale facteur d'échelle utilisé par renderLivingLabel
     * @return hauteur consommée (unités monde) pour empiler les labels
     */
    public double renderAboveHead(RenderManager rm, EntityLivingBase entity,
                                  double x, double y, double z, float textScale) {
        if (!isEnabled()) return 0.0;
        if (entity.getMaxHealth() <= 0f) return 0.0;

        float ratio = entity.getHealth() / entity.getMaxHealth();
        if (ratio < 0f) ratio = 0f;
        if (ratio > 1f) ratio = 1f;

        int bw   = getBarWidth();
        int bh   = getBarHeight();

        // ── Géométrie exacte pour éviter le décalage avec largeur impaire ──
        // left + right = bw (toujours), centrés sur 0
        int right = (bw + 1) / 2;   // ceil(bw/2)
        int left  = -(bw / 2);      // -floor(bw/2) → right - left = bw

        int fillR = left + Math.round(bw * ratio);  // bord droit du remplissage

        float f1 = 0.016666668F * 1.6F;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + entity.height + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate( rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        Tessellator  t  = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();

        // ── 1. Bordure noire — rendue AVANT le fond, taille fixée sur left/right ──
        //    Garantit 1 px de contour quelle que soit la largeur (paire ou impaire).
        if (isBorderEnabled()) {
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(left  - 1, -1,     0).color(0F, 0F, 0F, 0.92F).endVertex();
            wr.pos(left  - 1, bh + 1, 0).color(0F, 0F, 0F, 0.92F).endVertex();
            wr.pos(right + 1, bh + 1, 0).color(0F, 0F, 0F, 0.92F).endVertex();
            wr.pos(right + 1, -1,     0).color(0F, 0F, 0F, 0.92F).endVertex();
            t.draw();
        }

        // ── 2. Fond sombre ──
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(left,  0,  0).color(0.08F, 0.08F, 0.08F, 0.82F).endVertex();
        wr.pos(left,  bh, 0).color(0.06F, 0.06F, 0.06F, 0.82F).endVertex();
        wr.pos(right, bh, 0).color(0.06F, 0.06F, 0.06F, 0.82F).endVertex();
        wr.pos(right, 0,  0).color(0.08F, 0.08F, 0.08F, 0.82F).endVertex();
        t.draw();

        // ── 3. Remplissage avec gradient vertical (haut plus clair, bas plus sombre) ──
        if (fillR > left) {
            int col = getFillColor(ratio);
            float cr = ((col >> 16) & 0xFF) / 255F;
            float cg = ((col >>  8) & 0xFF) / 255F;
            float cb = (col          & 0xFF) / 255F;

            // Dégradé : haut à 80 %, bas à 55 % pour un effet de profondeur
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(left,  0,  0).color(cr * 0.80f, cg * 0.80f, cb * 0.80f, 1F).endVertex();
            wr.pos(left,  bh, 0).color(cr * 0.55f, cg * 0.55f, cb * 0.55f, 1F).endVertex();
            wr.pos(fillR, bh, 0).color(cr * 0.55f, cg * 0.55f, cb * 0.55f, 1F).endVertex();
            wr.pos(fillR, 0,  0).color(cr * 0.80f, cg * 0.80f, cb * 0.80f, 1F).endVertex();
            t.draw();

            // Highlight semi-transparent (1 px en haut) — donne un aspect "luisant"
            if (bh > 2) {
                float hlR = Math.min(1f, cr * 1.45f);
                float hlG = Math.min(1f, cg * 1.45f);
                float hlB = Math.min(1f, cb * 1.45f);
                wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(left,  0, 0).color(hlR, hlG, hlB, 0.70F).endVertex();
                wr.pos(left,  1, 0).color(cr,  cg,  cb,  0.40F).endVertex();
                wr.pos(fillR, 1, 0).color(cr,  cg,  cb,  0.40F).endVertex();
                wr.pos(fillR, 0, 0).color(hlR, hlG, hlB, 0.70F).endVertex();
                t.draw();
            }
        }

        GlStateManager.enableTexture2D();

        // ── 4. Texte optionnel "x/y" centré sous la barre ──
        if (isShowText()) {
            FontRenderer fr2 = rm.getFontRenderer();
            if (fr2 != null) {
                String s  = ((int) Math.ceil(entity.getHealth())) + "/" + ((int) entity.getMaxHealth());
                int    tw = fr2.getStringWidth(s);
                fr2.drawString(s, -tw / 2, bh + 2, 0xCCFFFFFF);
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();

        // Espacement fixe : bh + 4 px (2 px haut + 2 px bas), indépendant de l'état
        // de la bordure — garantit que le pseudo/faction reste à la même distance.
        int consumedPx = bh + 4 + (isShowText() ? 10 : 0);
        return (double) consumedPx * textScale * 1.15F;
    }
}
