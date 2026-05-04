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

    private static final ResourceLocation LOGO_TEXTURE    = new ResourceLocation("textures/logo/logo.png");
    private static final ResourceLocation ICON_SETTINGS   = new ResourceLocation("textures/logo/settings.png");
    private static final ResourceLocation ICON_LOGO_MINI  = new ResourceLocation("textures/logo/site.png");
    private static final ResourceLocation ICON_DISCORD    = new ResourceLocation("textures/logo/discord.png");
    private static final int BOTTOM_BAR_H = 30; // Changed from 0 to 30
    private int logoSize;

    // ── Timing ──────────────────────────────────────────────────────────────
    private long openTime = -1L;
    private static final float FADEIN_TOTAL = 800f;

    // FRAME_TIME global
    static long FRAME_TIME = -1L;

    // ── Particules ──────────────────────────────────────────────────────────
    private final List<AmbientParticle> particles = new ArrayList<>();
    private final List<AmbientParticle> particlePool = new ArrayList<>();
    private final Random rng = new Random();
    private static final int PARTICLE_MAX = 8; // Lunar style: very subtle ambient

    // ── Layout ──────────────────────────────────────────────────────────────
    private int logoY, btnStartY, btnW, btnH, btnGap, smallBtnW;
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

    // ── External links ──────────────────────────────────────────────────────
    private static final String DISCORD_URL = "https://discord.gg/redconflict";
    private static final String SITE_URL    = "https://redconflict.fr";

    public GuiMainMenu() {}

    // =========================================================================
    //   LIFECYCLE
    // =========================================================================

    @Override
    public void updateScreen()
    {
        ++this.panoramaTimer;
        // Particules désactivées
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

        // ── Boutons principaux (3 lignes pleines, sans Options) ──────────────
        this.buttonList.add(new MenuButton(1, cx - btnW / 2, btnStartY,
                btnW, btnH, "SINGLEPLAYER", MenuButton.Style.PRIMARY, 0));
        this.buttonList.add(new MenuButton(2, cx - btnW / 2, btnStartY + btnH + btnGap,
                btnW, btnH, "MULTIPLAYER",  MenuButton.Style.PRIMARY, 1));

        row3Y = btnStartY + (btnH + btnGap) * 2;
        this.buttonList.add(new MenuButton(4, cx - btnW / 2,
                row3Y, btnW, btnH, "QUIT", MenuButton.Style.QUIT, 2));

        // ── Boutons du bas (Discord, Settings, Site) ──────────────────────────
        int iconY = this.height - BOTTOM_BAR_H + (BOTTOM_BAR_H - BottomIconButton.SIZE) / 2;
        int iconGap = 8;
        int totalIconsWidth = BottomIconButton.SIZE * 3 + iconGap * 2;
        int startX = cx - totalIconsWidth / 2;

        this.buttonList.add(new BottomIconButton(10, startX, iconY, ICON_DISCORD, "Discord"));
        this.buttonList.add(new BottomIconButton(11, startX + BottomIconButton.SIZE + iconGap, iconY, ICON_SETTINGS, "Settings"));
        this.buttonList.add(new BottomIconButton(12, startX + (BottomIconButton.SIZE + iconGap) * 2, iconY, ICON_LOGO_MINI, "Site Web"));


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

        // Hauteur utilisable entre la topbar et la bottombar
        final int TOP  = 30;
        final int BOT  = BOTTOM_BAR_H; // Use the new BOTTOM_BAR_H
        int avail = Math.max(1, this.height - TOP - BOT);

        // Boutons : dimensionnés d'abord pour garantir qu'ils tiennent
        btnW   = Math.min(240, Math.max(100, this.width / 4));
        btnH   = MathHelper.clamp_int(avail / 18, 18, 26);
        btnGap = MathHelper.clamp_int(btnH / 4, 4, 8);
        int buttonsH = btnH * 3 + btnGap * 2; // 3 lignes de boutons

        // Logo : prend au plus 35% de l'espace dispo moins les boutons
        int spaceForLogo = avail - buttonsH - 60; // 60 = titre + sous-titre + marges
        logoSize = MathHelper.clamp_int(spaceForLogo, 64, 160);

        // Distribution verticale : remontée vers le tiers supérieur (légèrement plus haut)
        int contentH = logoSize + 60 + buttonsH; // 60 = titre + sous-titre + gap boutons
        int startY   = TOP + Math.max(4, (avail - contentH) / 2 - avail / 5);

        logoY     = startY;
        btnStartY = startY + logoSize + 60;

        // Garde-fou : les boutons ne doivent pas déborder sur la bottombar
        int maxBtnStart = this.height - BOT - buttonsH - 6;
        if (btnStartY > maxBtnStart) {
            btnStartY = maxBtnStart;
            // Si même ça ne suffit pas, réduire le logo
            int excess = btnStartY - (logoY + logoSize + 60);
            if (excess < 0) logoSize = Math.max(32, logoSize + excess);
        }
    }

    private void repositionButtons()
    {
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
                case 4:
                    b.xPosition = cx - btnW / 2; b.yPosition = row3Y;
                    b.width = btnW; b.height = btnH; break;
                case 10:
                case 11:
                case 12:
                    int iconY = this.height - BOTTOM_BAR_H + (BOTTOM_BAR_H - BottomIconButton.SIZE) / 2;
                    int iconGap = 8;
                    int totalIconsWidth = BottomIconButton.SIZE * 3 + iconGap * 2;
                    int startX = cx - totalIconsWidth / 2;
                    if (b.id == 10) b.xPosition = startX;
                    if (b.id == 11) b.xPosition = startX + BottomIconButton.SIZE + iconGap;
                    if (b.id == 12) b.xPosition = startX + (BottomIconButton.SIZE + iconGap) * 2;
                    b.yPosition = iconY;
                    b.width = BottomIconButton.SIZE;
                    b.height = BottomIconButton.SIZE;
                    break;
            }
        }

        ensureBtnCacheCapacity();
        for (int i = 0; i < this.buttonList.size(); i++) btnYCache[i] = this.buttonList.get(i).yPosition;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.id == 1) mc.displayGuiScreen(new GuiSelectWorld(this));
        if (button.id == 2) mc.displayGuiScreen(new GuiMultiplayer(this));
        if (button.id == 4) mc.shutdown();
        if (button.id == 10) openWebsite(DISCORD_URL);
        if (button.id == 11) mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
        if (button.id == 12) openWebsite(SITE_URL);
    }

    private static void openWebsite(String url)
    {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (Throwable t) {
            // Fallback : tentative via Sys (LWJGL)
            try { org.lwjgl.Sys.openURL(url); } catch (Throwable ignored) {}
        }
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
            if (b.id >= 10) continue; // bottom icons stay fixed
            float stagger = Math.min(1f, i * 0.1f);
            float per = easeOutQuart(1f - stagger);
            int offset = (int)(invEntrance * baseSlide * (0.5f + per * 0.6f));
            b.yPosition += offset;
        }

        // Draw buttons — pas de multiplicateur alpha global (il interfère avec les hover transitions)
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        super.drawScreen(mouseX, mouseY, partialTicks);
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

    /** Overlay sombre statique avec vignette douce (style Lunar Client) */
    private void renderAnimatedOverlay(float fade)
    {
        if (fade <= 0.01f) return;

        // Voile sombre principal — assombrit fortement le panorama (Lunar fait pareil)
        int baseAlpha = (int)(190f * fade);
        drawRect(0, 0, width, height, (baseAlpha << 24));

        // Gradient subtil bas → haut pour donner de la profondeur
        int bottomA = (int)(60f * fade);
        GuiRenderUtils.drawGradientRect(0, height / 2, width, height,
                0x00000000, (bottomA << 24));

        // Vignettes haut/bas (douces)
        int vigA = (int)(70f * fade);
        drawGradientRect(0, 0, width, 80, (vigA << 24), 0x00000000);
        drawGradientRect(0, height - 80, width, height, 0x00000000, (vigA << 24));
    }

    /** Scanlines — désactivé pour un look plus sobre (style Lunar Client) */
    private void renderScanlines(float fade)
    {
        // Volontairement vide : retiré pour l'esthétique sobre
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

    /** Glow — supprimé : le logo respire seul sur fond sombre */
    private void renderLogoGlow(float fade) { /* no-op */ }

    /** Logo image (goutte RedConflict) — pure, sans wordmark texte */
    private void renderLogo(float fade)
    {
        if (fade <= 0.01f) return;

        int x = width / 2 - logoSize / 2;
        int y = logoY;

        mc.getTextureManager().bindTexture(LOGO_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f, 1f, fade);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0f, 0f, logoSize, logoSize, logoSize, logoSize);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Titre "RED CONFLICT" + sous-titre "PvP Faction" sous le logo */
    private void renderSubtitle(float fade)
    {
        if (fade <= 0.1f) return;

        final String part1 = "RED";
        final String part2 = "CONFLICT";
        final float  SCALE = 1.8f;
        final int    SPACE = 3; // letterspacing entre chaque caractère

        int aFull   = (int)(255f * fade);
        int aShadow = (int)(140f * fade);

        // ── Calcul de la largeur totale ──────────────────────────────────
        // "RED" + espace 4px + "CONFLICT", chaque mot lettre par lettre
        int w1 = wordWidth(part1, SPACE);
        int w2 = wordWidth(part2, SPACE);
        int gap = 5; // espace entre "RED" et "CONFLICT" (à l'échelle 1.0)

        float totalW = (w1 + gap + w2) * SCALE;
        float startX = width / 2f - totalW / 2f;
        int titleY = logoY + logoSize + 8;

        // ── OMBRE (décalée de 2px en X et Y, à la même échelle) ─────────
        GlStateManager.pushMatrix();
        GlStateManager.translate(startX + 2f, titleY + 2f, 0f);
        GlStateManager.scale(SCALE, SCALE, 1f);
        int px = drawWord(part1, 0, aShadow, 0x000000, SPACE);
        drawWord(part2, px + gap, aShadow, 0x000000, SPACE);
        GlStateManager.popMatrix();

        // ── TEXTE PRINCIPAL ──────────────────────────────────────────────
        GlStateManager.pushMatrix();
        GlStateManager.translate(startX, (float)titleY, 0f);
        GlStateManager.scale(SCALE, SCALE, 1f);
        // "RED" en rouge vif
        px = drawWord(part1, 0, aFull, RED, SPACE);
        // "CONFLICT" en blanc pur
        drawWord(part2, px + gap, aFull, COLOR_WHITE & 0x00FFFFFF, SPACE);
        GlStateManager.popMatrix();

        // ── SOUS-TITRE "PVP FACTION" — taille native + lignes flanquantes ──
        String sub = CLIENT_EDITION.toUpperCase();
        final int SUB_SP = 2; // letterspacing subtil à scale 1.0 (texte net)
        int subRawW = 0;
        for (int i = 0; i < sub.length(); i++) {
            subRawW += fontRendererObj.getCharWidth(sub.charAt(i));
            if (i < sub.length() - 1) subRawW += SUB_SP;
        }
        int subY      = titleY + (int)(9 * SCALE) + 9;
        int subStartX = width / 2 - subRawW / 2;
        int aSub      = (int)(180f * fade);

        int sx = subStartX;
        for (int i = 0; i < sub.length(); i++) {
            char c = sub.charAt(i);
            fontRendererObj.drawString(String.valueOf(c), sx, subY,
                    (aSub << 24) | 0xC0C0C0, false);
            sx += fontRendererObj.getCharWidth(c) + SUB_SP;
        }

        // Lignes rouges flanquantes — alignées au milieu vertical du texte
        int lineY   = subY + 4;
        int linePad = 8;          // gap texte ↔ ligne
        int lineLen = 26;
        int lineA   = (int)(150f * fade);
        int leftEnd = subStartX - linePad;
        GuiRenderUtils.drawGradientRect(leftEnd - lineLen, lineY, leftEnd, lineY + 1,
                0x00000000, (lineA << 24) | RED_DIM);
        int rightStart = subStartX + subRawW + linePad;
        GuiRenderUtils.drawGradientRect(rightStart, lineY, rightStart + lineLen, lineY + 1,
                (lineA << 24) | RED_DIM, 0x00000000);
    }

    /** Largeur d'un mot avec letterspacing à l'échelle 1.0. */
    private int wordWidth(String s, int spacing)
    {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += fontRendererObj.getCharWidth(s.charAt(i));
            if (i < s.length() - 1) w += spacing;
        }
        return w;
    }

    /**
     * Dessine un mot lettre par lettre avec letterspacing et retourne la position X après le dernier caractère.
     * Doit être appelé dans un contexte GlStateManager.pushMatrix() à l'échelle voulue.
     */
    private int drawWord(String s, int startX, int alpha, int rgb, int spacing)
    {
        int x = startX;
        for (int i = 0; i < s.length(); i++) {
            fontRendererObj.drawString(String.valueOf(s.charAt(i)), x, 0,
                    (alpha << 24) | rgb, false);
            x += fontRendererObj.getCharWidth(s.charAt(i)) + spacing;
        }
        return x;
    }

    /** Séparateur — supprimé pour un look plus pur */
    private void renderAnimatedSeparator(float fade) { /* no-op */ }

    /** Panel des boutons — supprimé : les boutons sont autonomes (style Lunar pur) */
    private void renderButtonPanel(float fade) { /* no-op */ }

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

    /** Infos joueur intégrées dans renderFooter */
    private void renderPlayerInfo(float fade) {}


    /** Barre du bas — fond semi-transparent, joueur a droite, version a gauche */
    private void renderFooter(float fade)
    {
        if (fade <= 0.01f) return;

        int y0 = height - BOTTOM_BAR_H;

        // Fond
        int footBgA = (int)(50f * fade);
        drawRect(0, y0, width, height, (footBgA << 24));

        // Ligne separatrice en haut
        int lineA = (int)(22f * fade);
        GuiRenderUtils.drawGradientRect(0, y0, width / 2, y0 + 1,
                0x00000000, (lineA << 24) | (COLOR_WHITE & 0x00FFFFFF));
        GuiRenderUtils.drawGradientRect(width / 2, y0, width, y0 + 1,
                (lineA << 24) | (COLOR_WHITE & 0x00FFFFFF), 0x00000000);

        int textY = y0 + (BOTTOM_BAR_H - 8) / 2;

        // Gauche — version Minecraft
        int verA2 = (int)(75f * fade);
        fontRendererObj.drawString("Minecraft 1.8.9", 12, textY,
                (verA2 << 24) | (COLOR_WHITE & 0x00FFFFFF), false);

        // Droite — nom du joueur + point vert
        String playerName = mc.getSession().getUsername();
        int nameW2 = fontRendererObj.getStringWidth(playerName);
        int nameA2 = (int)(180f * fade);
        drawRect(width - nameW2 - 18, textY + 2, width - nameW2 - 14, textY + 6, (nameA2 << 24) | 0x44CC44);
        fontRendererObj.drawString(playerName, width - nameW2 - 10, textY,
                (nameA2 << 24) | (COLOR_WHITE & 0x00FFFFFF), false);
    }

    // =========================================================================
    //   BACKDROP PUBLIC — utilisé par GuiOptions pour afficher le panorama
    // =========================================================================

    /**
     * Rend le panorama + overlay sombre.
     * Appelé par les écrans secondaires (ex. GuiOptions) ouverts depuis le menu principal.
     */
    public void drawBackdrop(int mouseX, int mouseY, float partialTicks)
    {
        // S'assurer que le panoramaTimer avance bien côté backdrop
        GlStateManager.disableAlpha();
        renderSkybox(mouseX, mouseY, partialTicks);
        GlStateManager.enableAlpha();
        renderAnimatedOverlay(1f); // overlay plein (menu déjà entièrement apparu)
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
            this.size   = 0.6f + rng.nextFloat() * 1.4f;
            // Palette de couleurs variee : rouge, orange, blanc
            // Palette monochrome blanche/grise — style Lunar (sobre)
            int colorChoice = rng.nextInt(10);
            if (colorChoice < 8) this.color = 0xFFFFFFFF;       // blanc pur
            else this.color = 0xFFE63946;                        // rouge accent occasionnel
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
            this.size   = 0.6f + rng.nextFloat() * 1.4f;
            int colorChoice = rng.nextInt(10);
            if (colorChoice < 8) this.color = 0xFFFFFFFF;
            else this.color = 0xFFE63946;
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
            alpha = MathHelper.clamp_float(MathHelper.sin(lifeNorm * (float)Math.PI), 0f, 1f);
            alpha *= alpha; // Double pour un fade plus prononcé
        }

        boolean isDead() { return age >= life || alpha <= 0f || y < -10f; }
    }

    // =========================================================================
    //   CLASSE : BottomIconButton — très discret, style Lunar minimal
    //   Simple fade d'opacité + lift 2px. Fond à peine perceptible.
    // =========================================================================
    static class BottomIconButton extends GuiButton
    {
        static final int SIZE = 20;
        private final ResourceLocation icon;
        private final String tooltip;
        private float hover        = 0f;
        private float tooltipHover = 0f;
        private long  lastTime     = -1L;
        private static final float TRANS_MS = 200f;

        BottomIconButton(int id, int x, int y, ResourceLocation icon, String tooltip)
        {
            super(id, x, y, SIZE, SIZE, "");
            this.icon    = icon;
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
            hover        = MathHelper.clamp_float(hover + (hovered ? step : -step), 0f, 1f);
            tooltipHover = MathHelper.clamp_float(tooltipHover + (hovered ? step * 0.8f : -step * 2.5f), 0f, 1f);
            float t  = hover * hover * (3f - 2f * hover);         // smoothstep
            float tt = tooltipHover * tooltipHover * (3f - 2f * tooltipHover);

            int icx = xPosition + SIZE / 2;

            // ── FOND très subtil — un carré sombre quasi invisible ───────────
            if (t > 0.01f)
            {
                int bgA = (int)(22f * t); // max alpha 22/255 — quasi invisible
                drawRect(xPosition - 3, yPosition - 3, xPosition + SIZE + 3,
                        yPosition + SIZE + 3, (bgA << 24));
            }

            // ── ICÔNE — opacité 52%→100%, lift 0→2px ────────────────────────
            float iconAlpha = 0.52f + t * 0.48f;
            int   lift      = (int)(t * 2f);

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            mc.getTextureManager().bindTexture(icon);
            GlStateManager.color(1f, 1f, 1f, iconAlpha);
            Gui.drawModalRectWithCustomSizedTexture(xPosition, yPosition - lift,
                    0f, 0f, SIZE, SIZE, SIZE, SIZE);
            GlStateManager.color(1f, 1f, 1f, 1f);

            // ── POINT INDICATEUR en bas de l'icône (apparaît au hover) ───────
            if (t > 0.01f)
            {
                int dotA = (int)(180f * t);
                drawRect(icx - 1, yPosition + SIZE + 3, icx + 1,
                        yPosition + SIZE + 4, (dotA << 24) | 0xE63946);
            }

            // ── TOOLTIP — fade simple ─────────────────────────────────────────
            if (tt > 0.01f && !tooltip.isEmpty())
            {
                int tw = mc.fontRendererObj.getStringWidth(tooltip);
                int tx = icx - tw / 2;
                int ty = yPosition - 18;

                int bgA  = (int)(210f * tt);
                int txtA = (int)(220f * tt);

                drawRect(tx - 4,      ty - 2, tx + tw + 4,  ty + 10, (bgA << 24));
                drawRect(tx - 5,      ty - 1, tx - 4,       ty + 9,  (bgA << 24));
                drawRect(tx + tw + 4, ty - 1, tx + tw + 5,  ty + 9,  (bgA << 24));
                drawRect(tx - 4,      ty - 2, tx + tw + 4,  ty - 1,  ((int)(180f * tt) << 24) | 0xB22D38);
                mc.fontRendererObj.drawString(tooltip, tx, ty + 1,
                        (txtA << 24) | 0xCCCCCC, false);
            }
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
        {
            boolean res = super.mousePressed(mc, mouseX, mouseY);
            if (res) { hover = 1f; lastTime = GuiMainMenu.FRAME_TIME; }
            return res;
        }
    }

    // =========================================================================
    //   CLASSE : MenuButton — style Lunar Client épuré
    //   Fond sombre semi-transparent, bordure→rouge au hover,
    //   accent gauche permanent, highlight haut, texte blanc
    // =========================================================================
    static class MenuButton extends GuiButton
    {
        enum Style { PRIMARY, SECONDARY, QUIT }

        private final Style style;
        private float hover    = 0f;
        private long  lastTime = -1L;

        private static final float TRANS_MS = 220f;

        // ── Palette Lunar (dark glass + red accent) ──────────────────────────
        // Fond
        private static final int BG_IDLE     = 0x2A000000; // verre très sombre
        private static final int BG_HOV      = 0x50000000; // verre plus opaque
        private static final int BG_QUIT_HOV = 0x55AA1818; // rouge clairement visible au hover

        // Bordure
        private static final int BD_IDLE     = 0x22FFFFFF; // blanc très subtil
        private static final int BD_HOV_P    = 0xBBE63946; // rouge vif PRIMARY/SEC
        private static final int BD_HOV_Q    = 0xDDCC2222; // rouge sombre QUIT

        // Texte — discret au repos, soft au hover (jamais blanc pur)
        private static final int TXT_IDLE    = 0x4AFFFFFF; // blanc ~29% — très discret
        private static final int TXT_HOV     = 0xBBFFFFFF; // blanc ~73% — visible sans agresser
        private static final int TXT_HOV_Q   = 0xCCFF8888; // saumon doux QUIT

        // Accent gauche 2px — permanent, s'illumine au hover
        private static final int ACC_IDLE    = 0x35B22D38; // rouge dim
        private static final int ACC_HOV     = 0xEEE63946; // rouge vif

        MenuButton(int id, int x, int y, int w, int h, String text, Style style, int idx)
        {
            super(id, x, y, w, h, text);
            this.style = style;
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
            float t = hover * hover * (3f - 2f * hover); // smoothstep cubique

            // ── FOND (coins coupés 1px — look Lunar) ─────────────────────────
            int bgHov = (style == Style.QUIT) ? BG_QUIT_HOV : BG_HOV;
            int bg = GuiRenderUtils.colorLerp(BG_IDLE, bgHov, t);
            drawRect(xPosition + 1, yPosition,           xPosition + width - 1, yPosition + height,     bg);
            drawRect(xPosition,     yPosition + 1,       xPosition + 1,         yPosition + height - 1, bg);
            drawRect(xPosition + width - 1, yPosition + 1, xPosition + width,   yPosition + height - 1, bg);

            // ── HIGHLIGHT HAUT (ligne 1px du centre vers les bords, style Lunar)
            if (t > 0.01f && style != Style.QUIT)
            {
                int hlA = (int)(38f * t);
                int midX = xPosition + width / 2;
                GuiRenderUtils.drawGradientRect(xPosition + 2, yPosition, midX, yPosition + 1,
                        0x00FFFFFF, (hlA << 24) | 0xFFFFFF);
                GuiRenderUtils.drawGradientRect(midX, yPosition, xPosition + width - 2, yPosition + 1,
                        (hlA << 24) | 0xFFFFFF, 0x00FFFFFF);
            }

            // ── BORDURE ──────────────────────────────────────────────────────
            int bdHov    = (style == Style.QUIT) ? BD_HOV_Q : BD_HOV_P;
            int borderCol = GuiRenderUtils.colorLerp(BD_IDLE, bdHov, t);
            GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, borderCol);

            // ── ACCENT BARRE GAUCHE 2px (présent même au repos, s'illumine au hover)
            if (style != Style.QUIT)
            {
                int accColor = GuiRenderUtils.colorLerp(ACC_IDLE, ACC_HOV, t);
                drawRect(xPosition, yPosition + 1, xPosition + 2, yPosition + height - 1, accColor);
            }
            else // QUIT : barre rouge toujours présente, s'intensifie au hover
            {
                int accQIdle = 0x35882222;
                int accQHov  = 0xEECC2222;
                int accQCol  = GuiRenderUtils.colorLerp(accQIdle, accQHov, t);
                drawRect(xPosition, yPosition + 1, xPosition + 2, yPosition + height - 1, accQCol);

                // Overlay rouge supplémentaire sur tout le bouton au hover (effet danger)
                if (t > 0.01f)
                {
                    int ovA = (int)(28f * t);
                    drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1,
                            yPosition + height - 1, (ovA << 24) | 0xFF0000);
                }
            }

            // ── TEXTE ─────────────────────────────────────────────────────────
            int txtHov   = (style == Style.QUIT) ? TXT_HOV_Q : TXT_HOV;
            int txtColor = GuiRenderUtils.colorLerp(TXT_IDLE, txtHov, t);
            drawCenteredString(mc.fontRendererObj, displayString,
                    xPosition + width / 2,
                    yPosition + (height - 8) / 2,
                    txtColor);
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
        {
            boolean res = super.mousePressed(mc, mouseX, mouseY);
            if (res) { this.hover = 1f; this.lastTime = GuiMainMenu.FRAME_TIME; }
            return res;
        }
    }
 }
