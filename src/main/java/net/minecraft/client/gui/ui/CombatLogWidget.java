package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.combatlog.CombatLogManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class CombatLogWidget extends BaseWidget {

    // Fallback local : expire time en ms si le client détecte un coup localement
    private long localCombatExpire = 0L;
    private float lastHealth = -1f;

    // Détection de dégâts de chute — pour ne pas déclencher le combat sur une chute
    private boolean wasOnGround = true;
    private float prevFallDistance = 0f;
    private long lastLandedWithFallTime = 0L;

    public CombatLogWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 80;
        this.height = 16;
        if (getPropOrDefault("originalDesign", null) == null) setProp("originalDesign", Boolean.FALSE);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            float currentHealth = mc.thePlayer.getHealth();
            if (lastHealth < 0f) lastHealth = currentHealth;

            boolean onGround = mc.thePlayer.onGround;
            float fallDist = mc.thePlayer.fallDistance;

            // Détecter un atterrissage depuis une chute significative
            if (!wasOnGround && onGround && prevFallDistance > 2.5f) {
                lastLandedWithFallTime = System.currentTimeMillis();
            }
            wasOnGround = onGround;
            prevFallDistance = fallDist;

            // Déclenche le timer local uniquement si ce n'est pas une chute récente
            boolean recentFall = (System.currentTimeMillis() - lastLandedWithFallTime) < 1500L;
            if (!recentFall && (currentHealth < lastHealth || mc.thePlayer.hurtTime > 0)) {
                this.localCombatExpire = System.currentTimeMillis() + 30000L;
            }
            lastHealth = currentHealth;
        }

        boolean serverInCombat = CombatLogManager.INSTANCE.isInCombat();
        boolean editor = UIManager.getInstance().isEditorActive();
        boolean localInCombat = localCombatExpire > System.currentTimeMillis();

        boolean visible = serverInCombat || localInCombat || editor;
        if (!visible) return;
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        boolean original = Boolean.TRUE.equals(getPropOrDefault("originalDesign", false));
        long serverRemaining = CombatLogManager.INSTANCE.getRemainingMillis();

        // La détection locale est gérée dans render() — on lit juste le résultat ici
        long localRemaining = Math.max(0L, this.localCombatExpire - System.currentTimeMillis());

        // On prend la valeur maximale (serveur prioritaire si présent)
        long remaining = Math.max(serverRemaining, localRemaining);

        // Preview in editor
        if (remaining <= 0 && UIManager.getInstance().isEditorActive()) {
            remaining = 15500L; // 15.5s
        }

        // Durée max utilisée pour l'affichage du progrès : 30s (cohérent avec le plugin serveur)
        float maxCombatTime = 30000f;
        float progress = Math.min(1.0f, (float) remaining / maxCombatTime);

        if (original) {
            drawOriginalDesign(mc, remaining, progress);
        } else {
            drawClassicDesign(mc, remaining);
        }
    }

    private void drawClassicDesign(Minecraft mc, long remaining) {
        String timeStr = remaining <= 0 ? "Combat: 0.0s" : String.format("Combat: %.1fs", remaining / 1000.0);
        FontRenderer fr = mc.fontRendererObj;
        if (!Boolean.TRUE.equals(getPropOrDefault("customSize", false))) {
            this.width = fr.getStringWidth(timeStr);
            this.height = 9;
        }

        int color = getColor();
        // S'assurer que la couleur est opaque (0xFFxxxxxx)
        if ((color >> 24 & 255) == 0) {
            color = 0xFFFFFFFF;
        }

        fr.drawStringWithShadow(timeStr, 0, 0, color);
    }

    private void drawOriginalDesign(Minecraft mc, long remaining, float progress) {
        FontRenderer fr = mc.fontRendererObj;
        String timeStr = formatTimeMillis(remaining);

        int radius = 18;
        if (!Boolean.TRUE.equals(getPropOrDefault("customSize", false))) {
            this.width = radius * 2;
            this.height = radius * 2;
        }

        int centerX = radius;
        int centerY = radius;

        // Couleur dynamique : Rouge (30s) -> Jaune (15s) -> Vert (0s)
        int color;
        if (isRGBMode()) {
            color = java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 5000L) / 5000.0f, 0.8f, 1.0f);
        } else {
            float t = 1.0f - progress; // 0.0 à 30s, 1.0 à 0s
            if (t < 0.5f) {
                // Rouge (0.0) vers Jaune (0.5)
                color = colorLerp(0xFFFF4444, 0xFFFFCC44, t * 2f);
            } else {
                // Jaune (0.5) vers Vert (1.0)
                color = colorLerp(0xFFFFCC44, 0xFF44FF88, (t - 0.5f) * 2f);
            }
        }

        // Dessin du cercle de fond (ombre légère)
        drawArc(centerX, centerY, radius, 0, 360, 0x66000000, 3);

        // Dessin de l'anneau de progression
        // On dessine l'arc correspondant au temps restant
        if (progress > 0) {
            drawArc(centerX, centerY, radius, 0, (int)(progress * 360), color, 3);
        }

        // Texte au centre (Toujours blanc opaque pour lisibilité)
        fr.drawStringWithShadow(timeStr, centerX - fr.getStringWidth(timeStr) / 2f, centerY - 4, 0xFFFFFFFF);
    }

    private void drawArc(int x, int y, int radius, int startAngle, int endAngle, int color, int thickness) {
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        GL11.glLineWidth(thickness);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_LINE_STRIP);

        for (int i = startAngle; i <= endAngle; i++) {
            double angle = (i - 90) * Math.PI / 180.0;
            GL11.glVertex2d(x + Math.cos(angle) * radius, y + Math.sin(angle) * radius);
        }

        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private int colorLerp(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((int)(a1 + (a2 - a1) * t) << 24) | ((int)(r1 + (r2 - r1) * t) << 16) | ((int)(g1 + (g2 - g1) * t) << 8) | (int)(b1 + (b2 - b1) * t);
    }

    private String formatTimeMillis(long millis) {
        if (millis <= 0) return "0.0";
        double s = millis / 1000.0;
        return String.format("%.1fs", s);
    }
}
