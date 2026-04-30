package net.minecraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
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
 * GuiMainMenu — RedConflict Client (PvP Faction 1.8.9)
 *
 * Design moderne et fluide :
 *   - Panorama avec overlay gradient anime
 *   - Logo pulse avec glow multi-couches dynamique
 *   - Particules ambiantes (drift, taille variable, couleurs variees)
 *   - Boutons avec hover smooth, glow lateral, shimmer effect
 *   - Barre de statut avec infos joueur + version
 *   - Separateur anime avec onde sinusoidale
 *   - Animations d'entree echelonnees (staggered fade+slide)
 *   - Scanlines subtiles pour un look PvP agressif
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
    private float logoScale = 4f;

    // layout calcule
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
    private static final float FADEIN_TOTAL = 800f;

    // FRAME_TIME global
    static long FRAME_TIME = -1L;

    // ── Particules ──────────────────────────────────────────────────────────
    private final List<AmbientParticle> particles = new ArrayList<>();
    private final List<AmbientParticle> particlePool = new ArrayList<>();
    private final Random rng = new Random();
    private static final int PARTICLE_MAX = 40;

    // ── Layout ──────────────────────────────────────────────────────────────
    private int logoY, btnStartY, btnW, btnH, btnGap, smallBtnW;
    private float logoPxHalfWidth = 130f;
    private int[] btnYCache = new int[0];

    // ── Couleurs (palette restreinte : rouge, blanc, noir transparent)
    private static final int RED        = 0xE63946; // accent rouge
    private static final int RED_BRIGHT = 0xFF5E6A;
    private static final int RED_DIM    = 0xB22D38;
    private static final int RED_DARK   = 0x8D222B;
    private static final int COLOR_WHITE = 0x00FFFFFF; // RGB white (use with alpha)
    private static final int WHITE_SOLID = 0xFFFFFFFF; // ARGB white full
    private static final int WHITE_DIM   = 0xAAFFFFFF; // ARGB dim white
    private static final int COLOR_BLACK = 0x000000;   // RGB black

    // ── Client info ─────────────────────────────────────────────────────────
    private static final String CLIENT_NAME = "REDCONFLICT";
    private static final String CLIENT_VERSION = "v1.0.0";
    private static final String CLIENT_EDITION = "PvP Faction";

    public GuiMainMenu() {}

    // =========================================================================
    //   LIFECYCLE
    // =========================================================================

    @Override
    public void updateScreen()
    {
        ++this.panoramaTimer;

        // Spawn progressif de particules
        if (particles.size() < PARTICLE_MAX && rng.nextInt(4) == 0)
        {
            float spawnX = rng.nextFloat() * (this.width  > 0 ? this.width  : 854);
            float spawnY = (this.height > 0 ? this.height : 480) + 10f;

            AmbientParticle p;
            if (!particlePool.isEmpty()) {
                p = particlePool.remove(particlePool.size() - 1);
                p.reset(spawnX, spawnY, rng);
            } else {
                p = new AmbientParticle(spawnX, spawnY, rng);
            }
            particles.add(p);
        }

        // Mise a jour + nettoyage
        for (int i = particles.size() - 1; i >= 0; --i)
        {
            AmbientParticle p = particles.get(i);
            p.tick();
            if (p.isDead())
            {
                particles.remove(i);
                if (particlePool.size() < PARTICLE_MAX) particlePool.add(p);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {}

    @Override
    public void initGui()
    {
        if (this.openTime < 0L) this.openTime = Minecraft.getSystemTime();
        if (this.viewportTexture == null) this.viewportTexture  = new DynamicTexture(256, 256);
        if (this.backgroundTexture == null) this.backgroundTexture = this.mc.getTextureManager()
                .getDynamicTextureLocation("background", this.viewportTexture);

        computeLayout();

        this.buttonList.clear();

        // ── Boutons principaux ────────────────────────────────────────────
        this.buttonList.add(new MenuButton(1, cx - btnW / 2, btnStartY,
                btnW, btnH, "SINGLEPLAYER", MenuButton.Style.PRIMARY, 0));
        this.buttonList.add(new MenuButton(2, cx - btnW / 2, btnStartY + btnH + btnGap,
                btnW, btnH, "MULTIPLAYER",  MenuButton.Style.PRIMARY, 1));

        // ── Boutons secondaires ───────────────────────────────────────────
        smallBtnW = (btnW - btnGap) / 2;
        row3Y = btnStartY + (btnH + btnGap) * 2;
        this.buttonList.add(new MenuButton(0, cx - btnW / 2,
                row3Y, smallBtnW, btnH, "OPTIONS", MenuButton.Style.SECONDARY, 2));
        this.buttonList.add(new MenuButton(4, cx - btnW / 2 + smallBtnW + btnGap,
                row3Y, smallBtnW, btnH, "QUIT",    MenuButton.Style.QUIT, 3));

        // ── Bouton Discord (top-right) ───────────────────────────────────
        String discLabel = "Discord";
        int discW = fontRendererObj.getStringWidth(discLabel) + 16;
        this.buttonList.add(new IconButton(10, this.width - discW - 10, 7, discW, discLabel, 0xFF5865F2, "Join our Discord!"));

        // init cache
        ensureBtnCacheCapacity();
        for (int i = 0; i < this.buttonList.size(); i++) btnYCache[i] = this.buttonList.get(i).yPosition;
    }

    private void computeLayout()
    {
        if (this.width == prevWidth && this.height == prevHeight && btnW != 0) return;
        prevWidth = this.width;
        prevHeight = this.height;

        cx = this.width / 2;

        btnW = Math.min(280, Math.max(120, this.width / 4));
        btnH = Math.max(22, this.height / 22);
        btnGap = Math.max(6, btnH / 3);

        float base = MathHelper.clamp_float(this.width / 480f, 1.0f, 6.0f);
        logoScale = 3.0f * base;
        logoY = Math.max(24, (int)(this.height * 0.18f));

        int preferred = this.height / 2 + btnH - 48;
        btnStartY = Math.max((int)(logoY + 8 * logoScale) + 50, Math.min(this.height - 130, preferred));
    }

    private void repositionButtons()
    {
        smallBtnW = (btnW - btnGap) / 2;
        row3Y = btnStartY + (btnH + btnGap) * 2;

        for (GuiButton b : this.buttonList)
        {
            switch (b.id)
            {
                case 1:
                    b.xPosition = cx - btnW / 2; b.yPosition = btnStartY;
                    b.width = btnW; b.height = btnH; break;
                case 2:
                    b.xPosition = cx - btnW / 2; b.yPosition = btnStartY + btnH + btnGap;
                    b.width = btnW; b.height = btnH; break;
                case 0:
                    b.xPosition = cx - btnW / 2; b.yPosition = row3Y;
                    b.width = smallBtnW; b.height = btnH; break;
                case 4:
                    b.xPosition = cx - btnW / 2 + smallBtnW + btnGap; b.yPosition = row3Y;
                    b.width = smallBtnW; b.height = btnH; break;
                case 10:
                    int discW = fontRendererObj.getStringWidth(b.displayString) + 16;
                    b.xPosition = this.width - discW - 10;
                    b.yPosition = 7; b.width = discW; b.height = 18; break;
            }
        }

        ensureBtnCacheCapacity();
        for (int i = 0; i < this.buttonList.size(); i++) btnYCache[i] = this.buttonList.get(i).yPosition;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 0) mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
        if (button.id == 1) mc.displayGuiScreen(new GuiSelectWorld(this));
        if (button.id == 2) mc.displayGuiScreen(new GuiMultiplayer(this));
        if (button.id == 4) mc.shutdown();
    }

    // =========================================================================
    //   DRAWSCREEN
    // =========================================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        if (this.width != prevWidth || this.height != prevHeight)
        {
            computeLayout();
            repositionButtons();
        }

        FRAME_TIME = Minecraft.getSystemTime();

        long  now     = FRAME_TIME;
        float elapsed = (openTime < 0L) ? FADEIN_TOTAL : Math.min(FADEIN_TOTAL, now - openTime);
        float rawT    = elapsed / FADEIN_TOTAL;

        // Niveaux de fade echelonnes
        float fadeBase    = easeOutQuart(rawT);
        float fadeLogo    = easeOutQuart(Math.min(1f, rawT * 1.5f));
        float fadeSep     = easeOutQuart(Math.max(0f, rawT - 0.08f) * 1.3f);
        float fadeBtns    = easeOutQuart(Math.max(0f, rawT - 0.15f) * 1.4f);
        float fadeFooter  = easeOutQuart(Math.max(0f, rawT - 0.25f) * 1.5f);
        float fadeInfo    = easeOutQuart(Math.max(0f, rawT - 0.35f) * 1.6f);

        // ── PANORAMA ─────────────────────────────────────────────────────
        GlStateManager.disableAlpha();
        renderSkybox(mouseX, mouseY, partialTicks);
        GlStateManager.enableAlpha();

        // ── OVERLAY GRADIENT ANIME ───────────────────────────────────────
        renderAnimatedOverlay(fadeBase);

        // ── SCANLINES SUBTILES ───────────────────────────────────────────
        renderScanlines(fadeBase);

        // ── PARTICULES ───────────────────────────────────────────────────
        renderParticles(fadeBase, partialTicks);

        // ── GLOW LOGO (pulse dynamique) ──────────────────────────────────
        renderLogoGlow(fadeLogo);

        // ── LOGO ─────────────────────────────────────────────────────────
        renderLogo(fadeLogo);

        // ── SOUS-TITRE ──────────────────────────────────────────────────
        renderSubtitle(fadeLogo);

        // ── SEPARATEUR ANIME ─────────────────────────────────────────────
        renderAnimatedSeparator(fadeSep);

        // ── PANEL VERRE (derriere les boutons) ───────────────────────────
        renderButtonPanel(fadeBtns);

        // ── BOUTONS (slide-in staggered) ─────────────────────────────────
        float entrance = easeOutQuart(fadeBtns);
        float invEntrance = 1f - entrance;
        int baseSlide = 18;

        ensureBtnCacheCapacity();
        for (int i = 0; i < this.buttonList.size(); i++) {
            GuiButton b = this.buttonList.get(i);
            btnYCache[i] = b.yPosition;
            float stagger = Math.min(1f, i * 0.1f);
            float per = easeOutQuart(1f - stagger);
            int offset = (int)(invEntrance * baseSlide * (0.5f + per * 0.6f));
            b.yPosition += offset;
        }

        // Draw buttons with fade
        if (fadeBtns > 0.01f) {
            float btnAlpha = MathHelper.clamp_float(fadeBtns, 0f, 1f);
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, btnAlpha);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        // Reset couleur GL — important pour éviter les artefacts visuels sur les éléments suivants
        GlStateManager.color(1f, 1f, 1f, 1f);

        // Restore positions
        for (int i = 0; i < this.buttonList.size(); i++) {
            this.buttonList.get(i).yPosition = btnYCache[i];
        }

        // ── TOP BAR ──────────────────────────────────────────────────────
        renderTopBar(fadeLogo);

        // ── PLAYER INFO ──────────────────────────────────────────────────
        renderPlayerInfo(fadeInfo);

        // ── FOOTER ───────────────────────────────────────────────────────
        renderFooter(fadeFooter);
    }

    private void ensureBtnCacheCapacity() {
        if (btnYCache == null || btnYCache.length < this.buttonList.size()) {
            btnYCache = new int[Math.max(4, this.buttonList.size())];
        }
    }

    // =========================================================================
    //   ELEMENTS VISUELS
    // =========================================================================

    /** Overlay gradient anime - couleur sombre avec teinte rouge subtile qui pulse */
    private void renderAnimatedOverlay(float fade)
    {
        if (fade <= 0.01f) return;

        float time = (float)(System.currentTimeMillis() % 10000L) / 10000f;
        float pulse = (float)(Math.sin(time * Math.PI * 2.0) * 0.5 + 0.5);

        // Overlay sombre principal
        int baseAlpha = (int)(180f * fade);
        drawRect(0, 0, width, height, (baseAlpha << 24));

        // Gradient rouge subtil du bas vers le haut
        int redTint = (int)(15f * fade * pulse);
        GuiRenderUtils.drawGradientRect(0, height / 2, width, height,
                0x00000000, (redTint << 24) | RED_DARK);

        // Vignette haut
        int vigA = (int)(80f * fade);
        drawGradientRect(0, 0, width, 60, (vigA << 24), 0x00000000);
        // Vignette bas
        drawGradientRect(0, height - 60, width, height, 0x00000000, (vigA << 24));

        // Vignettes latérales — 5 tranches larges au lieu de 40 lignes (8× moins de draw calls)
        int sideA = (int)(30f * fade);
        int steps = 5;
        for (int i = 0; i < steps; i++) {
            int a = sideA * (steps - i) / steps;
            int x0 = 40 * i / steps;
            int x1 = 40 * (i + 1) / steps;
            drawRect(x0, 0, x1, height, (a << 24));
            drawRect(width - x1, 0, width - x0, height, (a << 24));
        }
    }

    /** Scanlines — un seul drawRect au lieu d'une boucle (~360 draw calls économisés/frame) */
    private void renderScanlines(float fade)
    {
        if (fade <= 0.3f) return;
        int a = (int)(4f * fade);
        if (a <= 0) return;
        // Un seul overlay au lieu de height/3 appels drawRect
        drawRect(0, 0, width, height, (a << 24));
    }

    /** Particules ambiantes ameliorees */
    private void renderParticles(float fade, float partialTicks)
    {
        if (particles.isEmpty() || fade <= 0.01f) return;

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.disableTexture2D();

        for (AmbientParticle p : particles)
        {
            float ix = p.prevX + (p.x - p.prevX) * partialTicks;
            float iy = p.prevY + (p.y - p.prevY) * partialTicks;

            float alphaFactor = p.alpha * fade;
            if (alphaFactor <= 0.02f) continue;

                    float a = MathHelper.clamp_float(alphaFactor, 0f, 1f);
                    int col = p.color & 0x00FFFFFF;
            int alpha = (int)(a * 255f);
            int color = (alpha << 24) | col;

            float size = Math.max(1f, p.size);
            int half = (int)(size / 2f);

            // Dessiner un petit carre pour la particule (mouvement ameliore dans tick)
            drawRect((int)(ix - half), (int)(iy - half),
                     (int)(ix + half), (int)(iy + half), color);

            // Glow subtil autour des particules plus grosses
            if (size > 2f && alpha > 40) {
                int glowA = alpha / 5;
                int glowCol = (glowA << 24) | col;
                drawRect((int)(ix - half - 1), (int)(iy - half - 1),
                         (int)(ix + half + 1), (int)(iy + half + 1), glowCol);
            }
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /** Glow dynamique avec pulsation sous le logo */
    private void renderLogoGlow(float fade)
    {
        if (fade <= 0.01f) return;

        float time = (float)(System.currentTimeMillis() % 4000L) / 4000f;
        float pulse = (float)(Math.sin(time * Math.PI * 2.0) * 0.3 + 0.7);

        int cx   = width / 2;
        int halfW = (int)logoPxHalfWidth;
        int cy   = logoY + 16;

        int layers = 3;
        for (int i = 1; i <= layers; i++)
        {
            float factor = 1f - (float)(i - 1) / layers;
            int a = (int)(24f * factor * factor * fade * pulse);
            if (a <= 0) continue;
            int c = (a << 24) | RED_DIM;
            int pad = i * 6;
            drawRect(cx - halfW - pad, cy - 18 - i * 4,
                    cx + halfW + pad, cy + 18 + i * 4, c);
        }

        // Ligne de lumiere horizontale sous le logo
        int lineA = (int)(35f * fade * pulse);
        GuiRenderUtils.drawGradientRect(cx - halfW, cy + 14, cx, cy + 16,
                0x00000000, (lineA << 24) | RED_BRIGHT);
        GuiRenderUtils.drawGradientRect(cx, cy + 14, cx + halfW, cy + 16,
                (lineA << 24) | RED_BRIGHT, 0x00000000);
    }

    /** Logo REDCONFLICT avec effet de profondeur */
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

        logoPxHalfWidth = (total * logoScale) / 2f;

        int aFull = (int)(255f * fade);
        int aShadow = (int)(60f * fade);

        // Ombre profonde (2 couches)
        fontRendererObj.drawString(t1, (int)(-total / 2f),      2, (aShadow << 24), false);
        fontRendererObj.drawString(t2, (int)(-total / 2f + w1), 2, (aShadow << 24), false);

        // Texte principal
        fontRendererObj.drawString(t1, (int)(-total / 2f),      0, (aFull << 24) | RED,        false);
        fontRendererObj.drawString(t2, (int)(-total / 2f + w1), 0, (aFull << 24) | (COLOR_WHITE & 0x00FFFFFF),   false);

        GlStateManager.popMatrix();
    }

    /** Sous-titre sous le logo */
    private void renderSubtitle(float fade)
    {
        if (fade <= 0.1f) return;
        int a = (int)(100f * fade);

        String sub = CLIENT_EDITION + " Edition";
        int sw = fontRendererObj.getStringWidth(sub);
        // Position ajustée pour être plus proche du titre
        int yOff = (int)(8 * logoScale) + 8;
        fontRendererObj.drawString(sub, width / 2 - sw / 2, logoY + yOff, (a << 24) | (COLOR_WHITE & 0x00FFFFFF), false);
    }

    /** Separateur anime — 2 drawGradientRect au lieu de ~250 drawRect pixel par pixel */
    private void renderAnimatedSeparator(float fade)
    {
        if (fade <= 0.01f) return;

        int cx    = width / 2;
        int halfW = (int)(logoPxHalfWidth * 0.7f);
        int y     = logoY + (int)(8 * logoScale) + 22;

        float time = (float)(System.currentTimeMillis() % 3000L) / 3000f;
        float breathe = (float)(Math.sin(time * Math.PI * 2.0) * 0.2 + 0.8);
        int a = (int)(200f * fade * breathe);

        // Bras gauche : gradient transparent → rouge (1 draw call)
        GuiRenderUtils.drawGradientRect(cx - halfW, y, cx - 4, y + 1,
                0x00000000, (a << 24) | RED);

        // Bras droit : gradient rouge → transparent (1 draw call)
        GuiRenderUtils.drawGradientRect(cx + 4, y, cx + halfW, y + 1,
                (a << 24) | RED, 0x00000000);

        // Point central anime (pulsation)
        float centerPulse = (float)(Math.sin(time * Math.PI * 4.0) * 0.3 + 0.7);
        int da = (int)(240f * fade * centerPulse);
        drawRect(cx - 2, y - 1, cx + 3, y + 2, (da << 24) | RED_BRIGHT);
        int ga = (int)(80f * fade * centerPulse);
        drawRect(cx - 3, y - 2, cx + 4, y + 3, (ga << 24) | RED);
    }

    /** Panel "verre" derriere la zone des boutons */
    private void renderButtonPanel(float fade)
    {
        if (fade <= 0.02f) return;
        int cx    = width / 2;
        int row3Y = btnStartY + (btnH + btnGap) * 2;
        int padX  = 22;
        int padY  = 16;
        int x1    = cx - btnW / 2 - padX;
        int y1    = btnStartY - padY;
        int x2    = cx + btnW / 2 + padX;
        int y2    = row3Y + btnH + padY;

        // Fond verre avec gradient subtil (noir transparent)
        int bgA = (int)(16f * fade);
        GuiRenderUtils.drawGradientRect(x1, y1, x2, y2,
                (bgA << 24) | (COLOR_BLACK & 0x00FFFFFF), ((bgA + 4) << 24) | (COLOR_BLACK & 0x00FFFFFF));

        // Bordure fine (blanc transparent)
        int borderA = (int)(24f * fade);
        GuiRenderUtils.drawRectOutline(x1, y1, x2 - x1, y2 - y1, (borderA << 24) | (COLOR_WHITE & 0x00FFFFFF));

        // Accent rouge en haut du panel — un seul drawRect pour éviter le pixel décalé au centre
        int accentA = (int)(60f * fade);
        drawRect(x1 + 1, y1, x2 - 1, y1 + 1, (accentA << 24) | RED);
    }

    /** Barre superieure avec badge client et infos */
    private void renderTopBar(float fade)
    {
        if (fade <= 0.01f) return;

        // Fond de la barre
        int barBgA = (int)(40f * fade);
        drawRect(0, 0, width, 28, (barBgA << 24));

        // Ligne separatrice avec gradient
        int lineA = (int)(35f * fade);
        GuiRenderUtils.drawGradientRect(0, 28, width / 2, 29,
                0x00000000, (lineA << 24) | (COLOR_WHITE & 0x00FFFFFF));
        GuiRenderUtils.drawGradientRect(width / 2, 28, width, 29,
                (lineA << 24) | (COLOR_WHITE & 0x00FFFFFF), 0x00000000);

        // Badge CLIENT avec style
        String lbl = CLIENT_NAME;
        int tw  = fontRendererObj.getStringWidth(lbl);
        int px  = 10, py = 6;
        int pX  = 6, pY = 4;
        int bdgA = (int)(255f * fade);

        // Fond du badge (noir transparent)
        drawRect(px, py, px + tw + pX * 2, py + 8 + pY * 2, (bdgA << 24) | (COLOR_BLACK & 0x00FFFFFF));
        // Bordure rouge
        GuiRenderUtils.drawRectOutline(px, py, tw + pX * 2, 8 + pY * 2, (bdgA << 24) | RED_DIM);
        // Accent gauche
        drawRect(px, py + 1, px + 2, py + 8 + pY * 2 - 1, (bdgA << 24) | RED);
        // Texte (blanc)
        fontRendererObj.drawString(lbl, px + pX + 1, py + pY, (bdgA << 24) | (COLOR_WHITE & 0x00FFFFFF));

        // Version a droite du badge (blanc atténué)
        String ver = CLIENT_VERSION;
        int verA = (int)(120f * fade);
        fontRendererObj.drawString(ver, px + tw + pX * 2 + 6, py + pY, (verA << 24) | (COLOR_WHITE & 0x00FFFFFF));
    }

    /** Affiche les infos du joueur en haut a droite, sans chevaucher le Discord */
    private void renderPlayerInfo(float fade)
    {
        if (fade <= 0.05f) return;

        String playerName = mc.getSession().getUsername();
        int nameW = fontRendererObj.getStringWidth(playerName);
        int a = (int)(200f * fade);
        // int dimA = (int)(80f * fade); // Non utilise

        // On prend en compte le bouton Discord (positionné à width - discW - 10)
        String discLabel = "Discord";
        int discW = fontRendererObj.getStringWidth(discLabel) + 16;

        int rx = width - nameW - discW - 35; // Positionné à gauche du Discord
        int ry = 12; // Aligné avec le texte du bouton Discord (7 + 5)

        // Petit indicateur "online" (point vert)
        drawRect(rx - 2, ry + 2, rx + 3, ry + 7, (a << 24) | 0x44CC44);

        // Nom du joueur (blanc)
        fontRendererObj.drawStringWithShadow(playerName, rx + 6, ry, (a << 24) | (COLOR_WHITE & 0x00FFFFFF));
    }

    /** Footer avec infos PvP et version Minecraft */
    private void renderFooter(float fade)
    {
        if (fade <= 0.01f) return;

        // Fond du footer
        int footBgA = (int)(30f * fade);
        drawRect(0, height - 24, width, height, (footBgA << 24));

        // Ligne separatrice avec gradient
        int lineA = (int)(30f * fade);
        GuiRenderUtils.drawGradientRect(0, height - 24, width / 2, height - 23,
                0x00000000, (lineA << 24) | (COLOR_WHITE & 0x00FFFFFF));
        GuiRenderUtils.drawGradientRect(width / 2, height - 24, width, height - 23,
                (lineA << 24) | (COLOR_WHITE & 0x00FFFFFF), 0x00000000);

        int py = height - 16;
        int a  = (int)(150f * fade);
        int dimA = (int)(70f * fade);

        // ── Gauche : puce rouge + version + edition ──────────────────────
        // Puce animee
        float time = (float)(System.currentTimeMillis() % 2000L) / 2000f;
        float pulse = (float)(Math.sin(time * Math.PI * 2.0) * 0.3 + 0.7);
        int pulseA = (int)(255f * pulse * fade);
        drawRect(10, py + 1, 14, py + 5, (pulseA << 24) | RED);

        String leftText = "\u00a78" + CLIENT_NAME + "  \u00a77" + CLIENT_VERSION + "  \u00a78| \u00a77" + CLIENT_EDITION;
        fontRendererObj.drawStringWithShadow(leftText, 18, py - 1, (a << 24) | (COLOR_WHITE & 0x00FFFFFF));

        // ── Droite : version Minecraft ───────────────────────────────────
        String mcVer = "Minecraft 1.8.9";
        int mcW = fontRendererObj.getStringWidth(mcVer);
        fontRendererObj.drawString(mcVer, width - mcW - 10, py - 1, (dimA << 24) | (COLOR_WHITE & 0x00FFFFFF), false);
    }

    // =========================================================================
    //   RENDU PANORAMA (logique vanilla)
    // =========================================================================

    private void renderSkybox(int mx, int my, float pt)
    {
        mc.getFramebuffer().unbindFramebuffer();
        GlStateManager.viewport(0, 0, 256, 256);
        drawPanorama(mx, my, pt);
        for (int i = 0; i < 4; i++) blurSkybox(pt);
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

    private void drawPanorama(int _mx, int _my, float _pt)
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
        int n = 6;
        for (int j = 0; j < n * n; ++j)
        {
            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    ((float)(j % n) / n - 0.5f) / 64f,
                    ((float)(j / n) / n - 0.5f) / 64f, 0f);
            GlStateManager.rotate(MathHelper.sin(((float)panoramaTimer + _pt) / 400f) * 25f + 20f, 1, 0, 0);
            GlStateManager.rotate(-((float)panoramaTimer + _pt) * 0.1f, 0, 1, 0);
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

    private void blurSkybox(float _pt)
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

    private static float easeOutQuart(float t)
    {
        t = MathHelper.clamp_float(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u * u;
    }

    private static float easeOutCubic(float t)
    {
        t = MathHelper.clamp_float(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    @Override
    public void onGuiClosed() {}

    // =========================================================================
    //   CLASSE : AmbientParticle
    // =========================================================================
    private static class AmbientParticle
    {
        float x, y;
        float prevX, prevY;
        float vx, vy;
        float alpha;
        float size;
        int color;
        int age;
        int life;
        float seed;

        AmbientParticle(float x, float y, Random rng)
        {
            this.prevX = this.x = x; this.prevY = this.y = y;
            this.vy = - (0.05f + rng.nextFloat() * 0.25f);
            this.vx = (rng.nextFloat() - 0.5f) * 0.15f;
            this.size   = 0.8f + rng.nextFloat() * 2.6f;
            // Palette de couleurs variee : rouge, orange, blanc
            int colorChoice = rng.nextInt(10);
            if (colorChoice < 5) this.color = 0xFFE63946;       // rouge
            else if (colorChoice < 7) this.color = 0xFFFF6633;  // orange
            else if (colorChoice < 9) this.color = 0xFFB22D38;  // rouge sombre
            else this.color = 0xFFFFFFAA;                        // blanc chaud
            this.age = 0;
            this.life = 100 + rng.nextInt(180);
            this.seed = rng.nextFloat() * 6.2831f;
            this.alpha = 1f;
        }

        void reset(float x, float y, Random rng)
        {
            this.prevX = this.x = x; this.prevY = this.y = y;
            this.vy = - (0.05f + rng.nextFloat() * 0.25f);
            this.vx = (rng.nextFloat() - 0.5f) * 0.15f;
            this.size   = 0.8f + rng.nextFloat() * 2.6f;
            int colorChoice = rng.nextInt(10);
            if (colorChoice < 5) this.color = 0xFFE63946;
            else if (colorChoice < 7) this.color = 0xFFFF6633;
            else if (colorChoice < 9) this.color = 0xFFB22D38;
            else this.color = 0xFFFFFFAA;
            this.age = 0;
            this.life = 100 + rng.nextInt(180);
            this.seed = rng.nextFloat() * 6.2831f;
            this.alpha = 1f;
        }

        void tick()
        {
            prevX = x; prevY = y;
            // Mouvement plus fluide et organique
            float wave = MathHelper.sin((float)age * 0.05f + seed);
            vx = vx * 0.99f + wave * 0.02f;

            // Pulsation legere de la taille
            size += MathHelper.sin((float)age * 0.1f) * 0.01f;

            x += vx;
            y += vy;
            age++;

            float lifeNorm = (float)age / (float)life;
            // Fade in/out en cloche pour plus de douceur
            alpha = MathHelper.sin(lifeNorm * (float)Math.PI);
            alpha *= alpha;
        }

        boolean isDead() { return age >= life || alpha <= 0f || y < -10f; }
    }

    // =========================================================================
    //   CLASSE : IconButton
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

            long  now = GuiMainMenu.FRAME_TIME;
            float dt  = (lastTime < 0L) ? 0f : (float)(now - lastTime);
            lastTime  = now;

            this.hovered = mx >= xPosition && my >= yPosition
                    && mx < xPosition + width && my < yPosition + height;

            float step = dt / TRANS_MS;
            hover = MathHelper.clamp_float(hover + (hovered ? step : -step), 0f, 1f);
            float t = hover * hover * (3 - 2 * hover);

            // Fond avec transition
            int bgA = (int)(GuiRenderUtils.lerp(18, 65, t));
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, (bgA << 24));

            // Bordure
            int border = GuiRenderUtils.colorLerp(0x28FFFFFF, 0xFF000000 | accent, t);
            GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, border);

            // Shimmer au hover
            if (t > 0f) {
                int shimA = (int)(15f * t);
                GuiRenderUtils.drawGradientRect(xPosition + 1, yPosition,
                        xPosition + width - 1, yPosition + 2,
                        (shimA << 24) | (COLOR_WHITE & 0x00FFFFFF), 0x00FFFFFF);
            }

            // Texte (de Gris -> Blanc, mais palette limitée à blanc)
            int txtCol = GuiRenderUtils.colorLerp(WHITE_DIM, WHITE_SOLID, t);
            drawCenteredString(mc.fontRendererObj, displayString,
                    xPosition + width / 2, yPosition + (height - 8) / 2, txtCol);

            // Tooltip
            if (hovered && !tooltip.isEmpty())
            {
                int tw = mc.fontRendererObj.getStringWidth(tooltip);
                int tx = xPosition + width / 2 - tw / 2;
                int ty = yPosition + height + 4;
                drawRect(tx - 4, ty - 2, tx + tw + 4, ty + 11, 0xDD000000);
                GuiRenderUtils.drawRectOutline(tx - 4, ty - 2, tw + 8, 13, (0x33 << 24) | (COLOR_WHITE & 0x00FFFFFF));
                mc.fontRendererObj.drawStringWithShadow(tooltip, tx, ty, WHITE_DIM);
            }
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
        {
            boolean res = super.mousePressed(mc, mouseX, mouseY);
            if (res) {
                this.hover = 1f;
                this.lastTime = GuiMainMenu.FRAME_TIME;
            }
            return res;
        }
     }

    // =========================================================================
    //   CLASSE : MenuButton
    // =========================================================================
    static class MenuButton extends GuiButton
    {
        enum Style { PRIMARY, SECONDARY, QUIT }

        private final Style style;
        private final int index;
        private float hover = 0f;
        private long  lastTime = -1L;

        private static final float TRANS_MS = 130f;

        // Palette
        private static final int BG_IDLE     = 0x14FFFFFF;
        private static final int BG_HOV      = 0x2CFFFFFF;
        private static final int BORDER_IDLE = 0x20FFFFFF;
        private static final int BORDER_HOV  = 0xFFE63946;
        private static final int ACCENT_COL  = 0xFFE63946;
        private static final int TXT_IDLE    = 0xFF909090;
        private static final int TXT_HOV     = 0xFFFFFFFF;

        // Quit button colors
        private static final int QUIT_BORDER_HOV = 0xFF882222;
        private static final int QUIT_BG_HOV     = 0x30FF2020;

        MenuButton(int id, int x, int y, int w, int h, String text, Style style, int index)
        {
            super(id, x, y, w, h, text);
            this.style = style;
            this.index = index;
        }

        @Override
        public void drawButton(Minecraft mc, int mx, int my)
        {
            if (!this.visible) return;

            long  now = GuiMainMenu.FRAME_TIME;
            float dt  = (lastTime < 0L) ? 0f : (float)(now - lastTime);
            lastTime  = now;

            this.hovered = mx >= xPosition && my >= yPosition
                    && mx < xPosition + width && my < yPosition + height;

            float step = dt / TRANS_MS;
            hover = MathHelper.clamp_float(hover + (hovered ? step : -step), 0f, 1f);
            float t = hover * hover * (3 - 2 * hover); // cubic smoothstep

            // ── Choix des couleurs selon le style ────────────────────────
            int borderHov = (style == Style.QUIT) ? QUIT_BORDER_HOV : BORDER_HOV;
            int bgHov     = (style == Style.QUIT) ? QUIT_BG_HOV : BG_HOV;

            // ── FOND (coins coupes 1px) ──────────────────────────────────
            int bg = GuiRenderUtils.colorLerp(BG_IDLE, bgHov, t);
            drawRect(xPosition + 1, yPosition,     xPosition + width - 1, yPosition + height,     bg);
            drawRect(xPosition,     yPosition + 1, xPosition + 1,          yPosition + height - 1, bg);
            drawRect(xPosition + width - 1, yPosition + 1, xPosition + width, yPosition + height - 1, bg);

            // ── BORDURE ──────────────────────────────────────────────────
            int borderCol = GuiRenderUtils.colorLerp(BORDER_IDLE, borderHov, t);
            GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, borderCol);

            // ── ACCENT LATERAL GAUCHE (PRIMARY) ──────────────────────────
            if (style == Style.PRIMARY && t > 0f)
            {
                int aA = (int)(255f * t);
                // Accent avec gradient vertical
                GuiRenderUtils.drawGradientRect(xPosition, yPosition + 1,
                        xPosition + 2, yPosition + height - 1,
                        (aA << 24) | RED_BRIGHT, (aA << 24) | RED_DIM);
            }

            // ── ACCENT LATERAL GAUCHE (QUIT) ────────────────────────────
            if (style == Style.QUIT && t > 0f)
            {
                int aA = (int)(180f * t);
                drawRect(xPosition, yPosition + 1,
                        xPosition + 2, yPosition + height - 1,
                        (aA << 24) | 0x882222);
            }

            // ── SHIMMER EN HAUT DU BOUTON ────────────────────────────────
            if (t > 0f)
            {
                int shimA = (int)(14f * t);
                drawGradientRect(
                        xPosition + 1, yPosition,
                        xPosition + width - 1, yPosition + 2,
                        (shimA << 24) | 0xFFFFFF, 0x00FFFFFF
                );
            }

            // ── GLOW SOUS LE BOUTON (hover) ──────────────────────────────
            if (t > 0.3f && style == Style.PRIMARY)
            {
                int glowA = (int)(6f * t);
                drawRect(xPosition + 2, yPosition + height,
                         xPosition + width - 2, yPosition + height + 2,
                         (glowA << 24) | RED_DIM);
            }

            // ── TEXTE ────────────────────────────────────────────────────
            int txtColor;
            if (style == Style.QUIT) {
                txtColor = GuiRenderUtils.colorLerp(TXT_IDLE, 0xFFFF6666, t);
            } else {
                txtColor = GuiRenderUtils.colorLerp(TXT_IDLE, TXT_HOV, t);
            }

            drawCenteredString(mc.fontRendererObj, displayString,
                    xPosition + width / 2,
                    yPosition + (height - 8) / 2,
                    txtColor);
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
        {
            boolean res = super.mousePressed(mc, mouseX, mouseY);
            if (res) {
                this.hover = 1f;
                this.lastTime = GuiMainMenu.FRAME_TIME;
            }
            return res;
        }
     }
 }
