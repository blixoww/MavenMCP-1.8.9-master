package net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * GuiMainMenu — RedConflict Client
 *
 * Inspiration visuelle : Badlion Client / Lunar Client
 * Caractéristiques :
 *   • Overlay sombre uniforme (sans bandes latérales)
 *   • Logo avec aura rouge ambiante (glow multi-couches)
 *   • Particules ambiantes (drift fluide, taille variable, fade naturel)
 *   • Panel "verre" derrière les boutons
 *   • Barre de status en haut + footer avec séparateur 1px
 *   • Animations d'entrée échelonnées par élément (staggered)
 *   • Hover delta-time (fluide à tout FPS)
 */
public class GuiMainMenu extends GuiScreen
{
    // ── Panorama ────────────────────────────────────────────────────────────
    private int panoramaTimer;
    private DynamicTexture viewportTexture;
    private ResourceLocation backgroundTexture;

    // Responsive layout cache
    private int prevWidth = -1;
    private int prevHeight = -1;
    private float logoScale = 4f; // échelle du logo, recalculée

    // layout calculé
    private int cx;
    private int row3Y;

    private static final ResourceLocation[] PANORAMA = {
            new ResourceLocation("textures/gui/title/background/panorama_0.png"),
            new ResourceLocation("textures/gui/title/background/panorama_1.png"),
            new ResourceLocation("textures/gui/title/background/panorama_2.png"),
            new ResourceLocation("textures/gui/title/background/panorama_3.png"),
            new ResourceLocation("textures/gui/title/background/panorama_4.png"),
            new ResourceLocation("textures/gui/title/background/panorama_5.png")
    };

    // ── Timing ──────────────────────────────────────────────────────────────
    private long openTime = -1L;
    /** Durée totale du fade-in global (ms) */
    private static final float FADEIN_TOTAL = 1100f;

    // ── Particules ──────────────────────────────────────────────────────────
    private final List<AmbientParticle> particles = new ArrayList<>();
    private final Random rng = new Random();
    /** Nombre max de particules simultanées */
    private static final int PARTICLE_MAX = 120;

    // ── Layout (calculé à l'init) ───────────────────────────────────────────
    private int logoY, btnStartY, btnW, btnH, btnGap, smallBtnW;

    // Largeur du logo en pixels-écran (approximée, mise à jour au premier rendu)
    private float logoPxHalfWidth = 130f;

    // ── Constantes de couleur ───────────────────────────────────────────────
    private static final int RED        = 0xDD2222;
    private static final int RED_BRIGHT = 0xFF3333;
    private static final int RED_DIM    = 0xAA1414;

    public GuiMainMenu() {}

    // =========================================================================
    //   LIFECYCLE
    // =========================================================================

    @Override
    public void updateScreen()
    {
        ++this.panoramaTimer;

        // Spawn progressif de particules
        if (particles.size() < PARTICLE_MAX && rng.nextInt(2) == 0)
        {
            float spawnX = rng.nextFloat() * (this.width  > 0 ? this.width  : 854);
            float spawnY = (this.height > 0 ? this.height : 480) + 10f;
            particles.add(new AmbientParticle(spawnX, spawnY, rng));
        }

        // Mise à jour + nettoyage
        Iterator<AmbientParticle> it = particles.iterator();
        while (it.hasNext())
        {
            AmbientParticle p = it.next();
            p.tick();
            if (p.isDead()) it.remove();
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {}

    @Override
    public void initGui()
    {
        this.openTime = Minecraft.getSystemTime();
        this.viewportTexture  = new DynamicTexture(256, 256);
        this.backgroundTexture = this.mc.getTextureManager()
                .getDynamicTextureLocation("background", this.viewportTexture);

        computeLayout();

        this.buttonList.clear();

        // ── Boutons principaux ────────────────────────────────────────────
        this.buttonList.add(new MenuButton(1, cx - btnW / 2, btnStartY,
                btnW, btnH, "SINGLEPLAYER", MenuButton.Style.PRIMARY));
        this.buttonList.add(new MenuButton(2, cx - btnW / 2, btnStartY + btnH + btnGap,
                btnW, btnH, "MULTIPLAYER",  MenuButton.Style.PRIMARY));

        // ── Boutons secondaires côte à côte ───────────────────────────────
        smallBtnW = (btnW - btnGap) / 2;
        row3Y = btnStartY + (btnH + btnGap) * 2;
        this.buttonList.add(new MenuButton(0, cx - btnW / 2,
                row3Y, smallBtnW, btnH, "OPTIONS", MenuButton.Style.SECONDARY));
        this.buttonList.add(new MenuButton(4, cx - btnW / 2 + smallBtnW + btnGap,
                row3Y, smallBtnW, btnH, "QUIT",    MenuButton.Style.SECONDARY));

        // ── Boutons icône (top-right) ─────────────────────────────────────
        // Discord
        String discLabel = "Discord";
        int discW = fontRendererObj.getStringWidth(discLabel) + 16;
        this.buttonList.add(new IconButton(10, this.width - discW - 10, 7, discW, discLabel, 0xFF5865F2, ""));
    }

    /** Recompute layout values according to current width/height. Call when size changes. */
    private void computeLayout()
    {
        if (this.width == prevWidth && this.height == prevHeight && btnW != 0) return;
        prevWidth = this.width;
        prevHeight = this.height;

        // centre
        cx = this.width / 2;

        // responsive sizes
        btnW = Math.min(300, Math.max(120, this.width / 4));
        btnH = Math.max(20, this.height / 24);
        btnGap = Math.max(6, btnH / 3);

        // logo scale adaptative selon largeur
        float base = MathHelper.clamp_float(this.width / 480f, 1.0f, 6.0f);
        logoScale = 3.2f * base; // base scale
        // logo position : environ 26% down from top but never too low
        logoY = Math.max(24, (int)(this.height * 0.22f));

        // button start Y relative to height
        btnStartY = Math.min(this.height - 120, this.height / 2 + btnH);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0) mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
        if (button.id == 1) mc.displayGuiScreen(new GuiSelectWorld(this));
        if (button.id == 2) mc.displayGuiScreen(new GuiMultiplayer(this));
        if (button.id == 4) mc.shutdown();
        // id 10 : ouvrir Discord (non implémenté ici)
    }

    // =========================================================================
    //   DRAWSCREEN
    // =========================================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        // si la fenêtre a été redimensionnée, recalculer le layout et re-créer les boutons
        if (this.width != prevWidth || this.height != prevHeight)
        {
            computeLayout();
            this.buttonList.clear();
            // recreate buttons in same order (IDs kept)
            this.initGui();
            // Note: initGui recrée les boutons et repositionne en fonction de computeLayout
        }

        long  now     = Minecraft.getSystemTime();
        float elapsed = (openTime < 0L) ? FADEIN_TOTAL : Math.min(FADEIN_TOTAL, now - openTime);
        float rawT    = elapsed / FADEIN_TOTAL;  // 0→1 linéaire

        // Niveaux de fade échelonnés pour chaque couche
        float fadeBase    = easeOutQuart(rawT);                           // fond + particules
        float fadeLogo    = easeOutQuart(Math.min(1f, rawT * 1.4f));      // logo (arrive en premier)
        float fadeSep     = easeOutQuart(Math.max(0f, rawT - 0.1f) * 1.3f); // séparateur
        float fadeBtns    = easeOutQuart(Math.max(0f, rawT - 0.2f) * 1.4f); // boutons
        float fadeFooter  = easeOutQuart(Math.max(0f, rawT - 0.3f) * 1.5f); // footer

        // ── PANORAMA ─────────────────────────────────────────────────────
        GlStateManager.disableAlpha();
        renderSkybox(mouseX, mouseY, partialTicks);
        GlStateManager.enableAlpha();

        // ── OVERLAY UNIFORME (pas de bandes latérales) ────────────────────
        drawRect(0, 0, width, height, 0xAA000000);
        // Légère vignette haut/bas pour ancrer les éléments
        drawGradientRect(0, 0,           width, 44,     0x55000000, 0x00000000);
        drawGradientRect(0, height - 50, width, height, 0x00000000, 0x65000000);

        // ── PARTICULES ───────────────────────────────────────────────────
        renderParticles(fadeBase, partialTicks);

        // ── GLOW LOGO ────────────────────────────────────────────────────
        renderLogoGlow(fadeLogo);

        // ── LOGO ─────────────────────────────────────────────────────────
        renderLogo(fadeLogo);

        // ── SÉPARATEUR ───────────────────────────────────────────────────
        renderSeparator(fadeSep);

        // ── PANEL VERRE (derrière les boutons) ───────────────────────────
        renderButtonPanel(fadeBtns);

        // ── BOUTONS (slide-in depuis le bas) ─────────────────────────────
        float slideY = (1f - fadeBtns) * 14f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, slideY, 0f);
        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();

        // ── TOP BAR ──────────────────────────────────────────────────────
        renderTopBar(fadeLogo);

        // ── FOOTER ───────────────────────────────────────────────────────
        renderFooter(fadeFooter);
    }

    // =========================================================================
    //   ÉLÉMENTS VISUELS
    // =========================================================================

    /** Particules ambiantes : rendu interpolé pour smoothness */
    private void renderParticles(float fade, float partialTicks)
    {
        for (AmbientParticle p : particles)
        {
            // interp positions
            float ix = p.prevX + (p.x - p.prevX) * partialTicks;
            float iy = p.prevY + (p.y - p.prevY) * partialTicks;

            float alphaFactor = p.alpha * fade;
            if (alphaFactor <= 0.02f) continue;

            int a = (int)(MathHelper.clamp_float(alphaFactor, 0f, 1f) * 200f);
            int col = (a << 24) | (p.color & 0x00FFFFFF);

            int size = Math.max(1, Math.round(p.size));
            int px = Math.round(ix) - size/2;
            int py = Math.round(iy) - size/2;
            drawRect(px, py, px + size, py + size, col);
        }
    }

    /**
     * Aura rouge ambiante sous le logo.
     * Technique : 6 couches de rects semi-transparents, chaque couche
     * est légèrement plus grande et moins opaque que la précédente.
     */
    private void renderLogoGlow(float fade)
    {
        if (fade <= 0.01f) return;
        int cx   = width / 2;
        int halfW = (int)logoPxHalfWidth;
        int cy   = logoY + 16; // centre vertical du logo (32px hauteur / 2)

        // Couches de l'intérieur vers l'extérieur
        // alpha de base = ~22, divisé par sqrt(i) pour une falloff douce
        int layers = 7;
        for (int i = 1; i <= layers; i++)
        {
            float factor = 1f - (float)(i - 1) / layers;
            int a = (int)(22f * factor * factor * fade);
            if (a <= 0) continue;
            int c = (a << 24) | RED_DIM;
            int pad = i * 5;
            // Rect englobant chaque couche
            drawRect(cx - halfW - pad, cy - 16 - i * 3,
                    cx + halfW + pad, cy + 16 + i * 3, c);
        }
    }

    /** Rendu du logo REDCONFLICT (scale ×4, ombre légère) */
    private void renderLogo(float fade)
    {
        GlStateManager.pushMatrix();
        GlStateManager.translate(width / 2f, (float)logoY, 0f);
        GlStateManager.scale(logoScale, logoScale, logoScale);

        String t1 = "RED";
        String t2 = "CONFLICT";
        int w1    = fontRendererObj.getStringWidth(t1);
        int w2    = fontRendererObj.getStringWidth(t2);
        int total = w1 + w2;

        // Mettre à jour la demi-largeur en px pour le glow et le séparateur
        // (total * 4 car scale ×4, /2 car centré)
        // mettre à l'échelle correctement selon logoScale
        logoPxHalfWidth = (total * logoScale) / 2f;

        int aFull = (int)(255f * fade);
        int aShadow = (int)(40f * fade);

        // Ombre décalée +0.5 unité
        fontRendererObj.drawString(t1, (int)(-total / 2f),      1, (aShadow << 24), false);
        fontRendererObj.drawString(t2, (int)(-total / 2f + w1), 1, (aShadow << 24), false);

        // Texte principal — flat (pas de shadow builtin pour un look pro)
        fontRendererObj.drawString(t1, (int)(-total / 2f),      0, (aFull << 24) | RED,        false);
        fontRendererObj.drawString(t2, (int)(-total / 2f + w1), 0, (aFull << 24) | 0xF0F0F0,  false);

        GlStateManager.popMatrix();
    }

    /**
     * Séparateur ──────●──────
     * Aligné sur la largeur réelle du logo (80% de sa largeur).
     * Bras : dégradé rouge opaque → transparent vers le centre.
     * Point central : carré 3×3 blanc-rouge.
     */
    private void renderSeparator(float fade)
    {
        int cx    = width / 2;
        int halfW = (int)(logoPxHalfWidth * 0.75f);  // 75% de la demi-largeur logo
        int y     = logoY + 36;                        // juste sous le logo (32px hauteur + 4px marge)
        int a     = (int)(200f * fade);

        // Bras gauche : rouge opaque → transparent
        drawGradientRect(cx - halfW, y, cx - 5, y + 1,
                (a << 24) | RED, 0x00000000);
        // Bras droit : transparent → rouge opaque
        drawGradientRect(cx + 5, y, cx + halfW, y + 1,
                0x00000000, (a << 24) | RED);

        // Point central 3×3, légèrement plus clair
        int da = (int)(230f * fade);
        drawRect(cx - 1, y - 1, cx + 2, y + 2, (da << 24) | RED_BRIGHT);
    }

    /**
     * Panel "verre" semi-transparent derrière la zone des boutons.
     * Donne de la profondeur et isole visuellement les contrôles.
     */
    private void renderButtonPanel(float fade)
    {
        if (fade <= 0.02f) return;
        int cx    = width / 2;
        int row3Y = btnStartY + (btnH + btnGap) * 2;
        int padX  = 18;
        int padY  = 14;
        int x1    = cx - btnW / 2 - padX;
        int y1    = btnStartY - padY;
        int x2    = cx + btnW / 2 + padX;
        int y2    = row3Y + btnH + padY;

        // Fond verre
        int bgA = (int)(14f * fade);
        drawRect(x1, y1, x2, y2, (bgA << 24) | 0xFFFFFF);

        // Bordure fine (1px)
        int borderA = (int)(22f * fade);
        GuiRenderUtils.drawRectOutline(x1, y1, x2 - x1, y2 - y1, (borderA << 24) | 0xFFFFFF);

        // (Supprimé) Filet accent rouge en haut du panel — l'utilisateur préfère sans la longue barre
    }

    /**
     * Barre supérieure : badge CLIENT (gauche) + boutons icônes (droite) + ligne séparatrice.
     * Inspiré Lunar : ligne 1px subtile à y=28 pour ancrer la barre.
     */
    private void renderTopBar(float fade)
    {
        // Ligne séparatrice 1px
        int lineA = (int)(28f * fade);
        drawRect(0, 28, width, 29, (lineA << 24) | 0xFFFFFF);

        // Badge CLIENT
        String lbl = "CLIENT";
        int tw  = fontRendererObj.getStringWidth(lbl);
        int px  = 10, py = 8;
        int pX  = 5, pY = 3;
        int bdgA = (int)(255f * fade);
        // Fond sombre rouge
        drawRect(px, py, px + tw + pX * 2, py + 8 + pY * 2, (bdgA << 24) | 0x881010);
        // Bordure
        GuiRenderUtils.drawRectOutline(px, py, tw + pX * 2, 8 + pY * 2, (bdgA << 24) | RED);
        // Texte
        fontRendererObj.drawString(lbl, px + pX, py + pY, (bdgA << 24) | 0xFFFFFF);
    }

    /**
     * Footer : ligne séparatrice 1px + version (gauche).
     * Inspiré Badlion : propre, discret, aligné.
     */
    private void renderFooter(float fade)
    {
        // Ligne séparatrice horizontale
        int lineA = (int)(30f * fade);
        drawRect(0, height - 22, width, height - 21, (lineA << 24) | 0xFFFFFF);

        // ── Gauche : puce rouge + version ────────────────────────────────
        int a  = (int)(140f * fade);
        int py = height - 15;
        // Puce carrée 4×4
        drawRect(10, py + 2, 14, py + 6, (0xFF << 24) | RED);
        fontRendererObj.drawStringWithShadow(
                " §8REDCONFLICT  §7v1.0.0",
                18, py, (a << 24) | 0xFFFFFF
        );

        // ── Droite ────────────────────────────────────────────
        int ac = (int)(70f * fade);
        String copy = "2026";
        int tw = fontRendererObj.getStringWidth(copy);
        fontRendererObj.drawString(copy, width - tw - 10, py, (ac << 24) | 0xFFFFFF);
    }

    // =========================================================================
    //   RENDU PANORAMA (logique vanilla, inchangée)
    // =========================================================================

    private void renderSkybox(int mx, int my, float pt)
    {
        mc.getFramebuffer().unbindFramebuffer();
        GlStateManager.viewport(0, 0, 256, 256);
        drawPanorama(mx, my, pt);
        for (int i = 0; i < 7; i++) blurSkybox(pt);
        mc.getFramebuffer().bindFramebuffer(true);
        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);

        float f  = width > height ? 120f / width : 120f / height;
        float f1 = height * f / 256f, f2 = width * f / 256f;
        Tessellator tr = Tessellator.getInstance();
        WorldRenderer wr = tr.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        wr.pos(0,     height, zLevel).tex(0.5f - f1, 0.5f + f2).color(1f, 1f, 1f, 1f).endVertex();
        wr.pos(width, height, zLevel).tex(0.5f - f1, 0.5f - f2).color(1f, 1f, 1f, 1f).endVertex();
        wr.pos(width, 0,      zLevel).tex(0.5f + f1, 0.5f - f2).color(1f, 1f, 1f, 1f).endVertex();
        wr.pos(0,     0,      zLevel).tex(0.5f + f1, 0.5f + f2).color(1f, 1f, 1f, 1f).endVertex();
        tr.draw();
    }

    private void drawPanorama(int mx, int my, float pt)
    {
        Tessellator tr = Tessellator.getInstance();
        WorldRenderer wr = tr.getWorldRenderer();
        GlStateManager.matrixMode(5889); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        org.lwjgl.util.glu.Project.gluPerspective(120f, 1f, 0.05f, 10f);
        GlStateManager.matrixMode(5888); GlStateManager.pushMatrix(); GlStateManager.loadIdentity();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.rotate(180, 1, 0, 0); GlStateManager.rotate(90, 0, 0, 1);
        GlStateManager.enableBlend(); GlStateManager.disableAlpha();
        GlStateManager.disableCull(); GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        int n = 8;
        for (int j = 0; j < n * n; ++j)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    ((float)(j % n) / n - 0.5f) / 64f,
                    ((float)(j / n) / n - 0.5f) / 64f, 0f);
            GlStateManager.rotate(MathHelper.sin(((float)panoramaTimer + pt) / 400f) * 25f + 20f, 1, 0, 0);
            GlStateManager.rotate(-((float)panoramaTimer + pt) * 0.1f, 0, 1, 0);
            for (int k = 0; k < 6; ++k)
            {
                GlStateManager.pushMatrix();
                if (k == 1) GlStateManager.rotate( 90, 0, 1, 0);
                if (k == 2) GlStateManager.rotate(180, 0, 1, 0);
                if (k == 3) GlStateManager.rotate(-90, 0, 1, 0);
                if (k == 4) GlStateManager.rotate( 90, 1, 0, 0);
                if (k == 5) GlStateManager.rotate(-90, 1, 0, 0);
                mc.getTextureManager().bindTexture(PANORAMA[k]);
                wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                int l = 255 / (j + 1);
                wr.pos(-1, -1, 1).tex(0, 0).color(255, 255, 255, l).endVertex();
                wr.pos( 1, -1, 1).tex(1, 0).color(255, 255, 255, l).endVertex();
                wr.pos( 1,  1, 1).tex(1, 1).color(255, 255, 255, l).endVertex();
                wr.pos(-1,  1, 1).tex(0, 1).color(255, 255, 255, l).endVertex();
                tr.draw();
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
            GlStateManager.colorMask(true, true, true, false);
        }
        wr.setTranslation(0, 0, 0);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.matrixMode(5889); GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888); GlStateManager.popMatrix();
        GlStateManager.depthMask(true); GlStateManager.enableCull(); GlStateManager.enableDepth();
    }

    private void blurSkybox(float pt)
    {
        mc.getTextureManager().bindTexture(backgroundTexture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, 256, 256);
        GlStateManager.enableBlend(); GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.colorMask(true, true, true, false);
        Tessellator tr = Tessellator.getInstance();
        WorldRenderer wr = tr.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        GlStateManager.disableAlpha();
        for (int j = 0; j < 3; j++)
        {
            float f = (float)(j - 1) / 256f;
            float a = 1f / (j + 1);
            wr.pos(width, height, zLevel).tex(0 + f, 1).color(1f, 1f, 1f, a).endVertex();
            wr.pos(width, 0,      zLevel).tex(1 + f, 1).color(1f, 1f, 1f, a).endVertex();
            wr.pos(0,     0,      zLevel).tex(1 + f, 0).color(1f, 1f, 1f, a).endVertex();
            wr.pos(0,     height, zLevel).tex(0 + f, 0).color(1f, 1f, 1f, a).endVertex();
        }
        tr.draw();
        GlStateManager.enableAlpha();
        GlStateManager.colorMask(true, true, true, true);
    }

    // =========================================================================
    //   EASING
    // =========================================================================

    /** Accélère vite, décélère progressivement — sensation de légèreté */
    private static float easeOutQuart(float t)
    {
        t = MathHelper.clamp_float(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u * u;
    }

    @Override
    public void onGuiClosed() {}

    // =========================================================================
    //   CLASSE : AmbientParticle
    //   Petits points qui dérivent vers le haut avec un léger mouvement horizontal.
    //   Taille variable, vitesse et alpha aléatoires.
    // =========================================================================
    private static class AmbientParticle
    {
        float x, y;
        float prevX, prevY;
        float vx, vy;
        float alpha;     // 0→1
        float decay;     // alpha lost per tick
        float size;
        int color;

        AmbientParticle(float x, float y, Random rng)
        {
            this.x = x; this.y = y;
            this.prevX = x; this.prevY = y;
            // velocity
            this.vy = - (0.05f + rng.nextFloat() * 0.35f);
            this.vx = (rng.nextFloat() - 0.5f) * 0.24f;
            this.alpha  = 0.45f + rng.nextFloat() * 0.5f;
            this.decay  = 0.0006f + rng.nextFloat() * 0.0022f;
            this.size   = 0.9f + rng.nextFloat() * 1.6f;
            this.color = (rng.nextInt(5) == 0) ? 0xFFFFCC33 : 0xFFDD2222;
        }

        void tick()
        {
            // store prev
            prevX = x; prevY = y;
            // small random walk for vx
            vx += (MathHelper.sin(x * 0.01f) * 0.006f);
            // apply velocities
            x += vx;
            y += vy;
            // slowly decay alpha
            alpha -= decay;
        }

        boolean isDead() { return alpha <= 0f || y < -6f; }
    }

    // =========================================================================
    //   CLASSE : IconButton
    //   Bouton icône (top-right) — Discord.
    //   Hover delta-time, couleur d'accent personnalisée.
    // =========================================================================
    static class IconButton extends GuiButton
    {
        private final int   accent;
        private final String tooltip;
        private float hover = 0f;
        private long  lastTime = -1L;

        private static final float TRANS_MS = 120f;

        IconButton(int id, int x, int y, int w, String text, int accent, String tooltip)
        {
            super(id, x, y, w, 18, text);
            this.accent  = accent;
            this.tooltip = tooltip;
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my)
        {
            if (!this.visible) return;

            long  now = Minecraft.getSystemTime();
            float dt  = (lastTime < 0L) ? 0f : (float)(now - lastTime);
            lastTime  = now;

            this.hovered = mx >= xPosition && my >= yPosition
                    && mx < xPosition + width && my < yPosition + height;

            float step = dt / TRANS_MS;
            hover = MathHelper.clamp_float(hover + (hovered ? step : -step), 0f, 1f);
            float t = easeInOutQuad(hover);

            // Fond
            int bgA = (int)((hovered ? 60 : 28) * t + 18);
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height,
                    (bgA << 24) | 0x000000);

            // Bordure — gris → couleur accent
            int border = GuiRenderUtils.colorLerp(0x28FFFFFF, 0xFF000000 | accent, t);
            GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, border);

            // Texte centré
            int txtCol = GuiRenderUtils.colorLerp(0xFF888888, 0xFFFFFFFF, t);
            drawCenteredString(mc.fontRendererObj, displayString,
                    xPosition + width / 2, yPosition + (height - 8) / 2, txtCol);

            // Tooltip au survol
            if (hovered && !tooltip.isEmpty())
            {
                int tw = mc.fontRendererObj.getStringWidth(tooltip);
                int tx = xPosition + width / 2 - tw / 2;
                int ty = yPosition + height + 3;
                drawRect(tx - 3, ty - 1, tx + tw + 3, ty + 10, 0xCC000000);
                mc.fontRendererObj.drawString(tooltip, tx, ty, 0xFFCCCCCC);
            }
        }

        private static float easeInOutQuad(float t)
        {
            return t < 0.5f ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);
        }
    }

    // =========================================================================
    //   CLASSE : MenuButton
    //   Bouton principal avec hover delta-time, accent latéral (PRIMARY),
    //   fond verre, bordure animée rouge.
    // =========================================================================
    static class MenuButton extends GuiButton
    {
        enum Style { PRIMARY, SECONDARY }

        private final Style style;
        private float hover = 0f;
        private long  lastTime = -1L;

        /** Durée de transition hover (ms) */
        private static final float TRANS_MS = 140f;

        // Palette
        private static final int BG_IDLE     = 0x12FFFFFF;
        private static final int BG_HOV      = 0x28FFFFFF;
        private static final int BORDER_IDLE = 0x22FFFFFF;
        private static final int BORDER_HOV  = 0xFFCC2020;
        private static final int ACCENT_COL  = 0xFFDD2020;
        private static final int TXT_IDLE    = 0xFF909090;
        private static final int TXT_HOV     = 0xFFFFFFFF;
        private static final int ACCENT_W    = 1; // Corrigé : épaisseur 1 pour être uniforme avec la bordure droite

        MenuButton(int id, int x, int y, int w, int h, String text, Style style)
        {
            super(id, x, y, w, h, text);
            this.style = style;
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my)
        {
            if (!this.visible) return;

            // ── Delta-time ────────────────────────────────────────────────
            long  now = Minecraft.getSystemTime();
            float dt  = (lastTime < 0L) ? 0f : (float)(now - lastTime);
            lastTime  = now;

            this.hovered = mx >= xPosition && my >= yPosition
                    && mx < xPosition + width && my < yPosition + height;

            float step = dt / TRANS_MS;
            hover = MathHelper.clamp_float(hover + (hovered ? step : -step), 0f, 1f);

            // Easing pour l'affichage
            float t = easeInOutQuad(hover);

            // ── FOND (coins simulés, 1px coupés) ─────────────────────────
            int bg = GuiRenderUtils.colorLerp(BG_IDLE, BG_HOV, t);
            drawRect(xPosition + 1, yPosition,     xPosition + width - 1, yPosition + height,     bg);
            drawRect(xPosition,     yPosition + 1, xPosition + 1,          yPosition + height - 1, bg);
            drawRect(xPosition + width - 1, yPosition + 1, xPosition + width, yPosition + height - 1, bg);

            // ── BORDURE ───────────────────────────────────────────────────
            GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height,
                    GuiRenderUtils.colorLerp(BORDER_IDLE, BORDER_HOV, t));

            // ── ACCENT LATÉRAL GAUCHE (PRIMARY) ──────────────────────────
            if (style == Style.PRIMARY && t > 0f)
            {
                int aA = (int)(255f * t);
                drawRect(xPosition, yPosition + 1,
                        xPosition + ACCENT_W, yPosition + height - 1,
                        (aA << 24) | (ACCENT_COL & 0xFFFFFF));
            }

            // ── REFLET SUBTIL EN HAUT DU BOUTON (shimmer) ────────────────
            // Fine ligne dégradée de blanc translucide → transparent sur 1px de haut
            if (t > 0f)
            {
                int shimA = (int)(12f * t);
                drawGradientRect(
                        xPosition + 1, yPosition,
                        xPosition + width - 1, yPosition + 2,
                        (shimA << 24) | 0xFFFFFF, 0x00FFFFFF
                );
            }

            // ── TEXTE ─────────────────────────────────────────────────────
            drawCenteredString(mc.fontRendererObj, displayString,
                    xPosition + width / 2,
                    yPosition + (height - 8) / 2,
                    GuiRenderUtils.colorLerp(TXT_IDLE, TXT_HOV, t));
        }

        private static float easeInOutQuad(float t)
        {
            return t < 0.5f ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);
        }
    }
}
