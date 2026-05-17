package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.custompackets.data.BountyCache;
import net.minecraft.client.custompackets.data.FactionZoneTracker;
import net.minecraft.client.custompackets.data.KillstreakCache;
import net.minecraft.client.custompackets.data.PlayerData;
import net.minecraft.client.custompackets.handler.PlayerDataHandler;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.gui.ui.UITheme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Locale;

/**
 * Ecran profil — palette « Red Conflict » et lecture live des caches
 * (PlayerDataHandler / KillstreakCache / BountyCache / FactionZoneTracker)
 * en mode self pour rester sync avec le serveur.
 */
public class GuiProfil extends GuiScreen {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final int C_OVERLAY     = 0xCC07060B;
    private static final int C_PANEL       = 0xF20D0A12;
    private static final int C_HEADER      = 0xFF120C18;
    private static final int C_PANEL_INNER = 0xFF14101C;
    private static final int C_CARD_BG     = 0xCC0E0A14;
    private static final int C_CARD_EDGE   = 0x33FFFFFF;
    private static final int C_TEXT_MAIN   = 0xFFF4F1F6;
    private static final int C_TEXT_SOFT   = 0xFFB8A8B4;
    private static final int C_TEXT_MUTED  = 0xFF7A6B7A;

    // Brand "Red Conflict"
    private static final int C_BRAND       = 0xFFE0303D; // rouge cramoisi
    private static final int C_BRAND_DEEP  = 0xFF7A1620; // rouge profond
    private static final int C_BRAND_GLOW  = 0xFFFF5566; // halo
    private static final int C_GOLD        = 0xFFE7CF65;

    // ── Donnees fixes (passees en constructeur, utilisees pour les autres joueurs) ──
    private final String targetName;
    private final boolean selfProfile;
    private final String fixedFaction;
    private final String fixedRank;
    private final int    fixedKills;
    private final int    fixedDeaths;
    private final int    fixedPlayTimeMin;
    private final long   fixedBalance;
    private final int    fixedStreak;
    private final long   fixedBounty;
    private final int    fixedPB;
    /** Relation de l'observateur envers la faction affichée. 0=own(vert),1=ally(violet),2=truce(violet),3=enemy(rouge),4=neutral */
    private final int    fixedFactionRelation;

    // ── Layout ───────────────────────────────────────────────────────────────
    private int panelW, panelH, panelX, panelY;
    private static final int PAD = 12;
    private static final int TITLE_H = 32;

    // ── Animation ────────────────────────────────────────────────────────────
    private float fadeIn = 0f;
    private float fadeInRaw = 0f;
    private float slideY = 12f;
    private long  openedAt = 0L;
    private float lookX = 0f;
    private float lookY = 0f;

    // Mouse cache (utilise par le card identite)
    private int lastMouseX;
    private int lastMouseY;

    public GuiProfil(String targetName, String faction, String rank,
                     int kills, int deaths, int playTimeMin,
                     long balance, int streak, long bounty, boolean selfProfile) {
        this(targetName, faction, rank, kills, deaths, playTimeMin, balance, streak, bounty, selfProfile, selfProfile ? 0 : 4, 0);
    }

    public GuiProfil(String targetName, String faction, String rank,
                     int kills, int deaths, int playTimeMin,
                     long balance, int streak, long bounty, boolean selfProfile, int factionRelation) {
        this(targetName, faction, rank, kills, deaths, playTimeMin, balance, streak, bounty, selfProfile, factionRelation, 0);
    }

    public GuiProfil(String targetName, String faction, String rank,
                     int kills, int deaths, int playTimeMin,
                     long balance, int streak, long bounty, boolean selfProfile, int factionRelation, int pb) {
        this.targetName           = targetName != null ? targetName : "Inconnu";
        this.selfProfile          = selfProfile;
        this.fixedFaction         = faction != null ? faction : "";
        this.fixedRank            = rank;
        this.fixedKills           = kills;
        this.fixedDeaths          = deaths;
        this.fixedPlayTimeMin     = playTimeMin;
        this.fixedBalance         = balance;
        this.fixedStreak          = streak;
        this.fixedBounty          = bounty;
        this.fixedFactionRelation = factionRelation;
        this.fixedPB              = pb;
    }

    public static GuiProfil forSelf() {
        Minecraft mc = Minecraft.getMinecraft();
        String name = mc.thePlayer != null ? mc.thePlayer.getName() : "Inconnu";
        PlayerData cached = PlayerDataHandler.getCachedData();
        int pb = cached != null ? cached.getPb() : 0;
        return new GuiProfil(name, "", "Joueur", 0, 0, 0, 0L, 0, 0L, true, 0, pb);
    }

    // ── Lecture dynamique en mode self ──────────────────────────────────────

    private String currentFaction() {
        return selfProfile ? FactionZoneTracker.getOwnFactionName() : fixedFaction;
    }
    /** 0=own(vert), 1=ally, 2=truce, 3=enemy(rouge), 4=neutral, -1=sans faction */
    private int currentFactionRelation() {
        String fac = currentFaction();
        if (fac == null || fac.isEmpty()) return -1;
        return selfProfile ? 0 : fixedFactionRelation;
    }
    /** Retourne la couleur Minecraft (§x) correspondant à la relation. */
    private String factionColor(int relation) {
        switch (relation) {
            case 0:  return "§a"; // own   – vert
            case 1:
            case 2:  return "§5"; // ally/truce – violet
            case 3:  return "§c"; // enemy – rouge
            default: return "§f"; // neutral / inconnu – blanc
        }
    }

    private String currentRank() {
        if (selfProfile) {
            PlayerData d = PlayerDataHandler.getCachedData();
            return d != null && d.getRank() != null ? d.getRank() : (fixedRank != null ? fixedRank : "Joueur");
        }
        return fixedRank != null ? fixedRank : "Joueur";
    }
    private int currentKills() {
        if (selfProfile) {
            PlayerData d = PlayerDataHandler.getCachedData();
            return d != null ? d.getKills() : fixedKills;
        }
        return fixedKills;
    }
    private int currentDeaths() {
        if (selfProfile) {
            PlayerData d = PlayerDataHandler.getCachedData();
            return d != null ? d.getDeaths() : fixedDeaths;
        }
        return fixedDeaths;
    }
    private int currentPlayTime() {
        if (selfProfile) {
            PlayerData d = PlayerDataHandler.getCachedData();
            return d != null ? d.getPlayTimeMinutes() : fixedPlayTimeMin;
        }
        return fixedPlayTimeMin;
    }
    private long currentBalance() {
        if (selfProfile) {
            PlayerData d = PlayerDataHandler.getCachedData();
            return d != null ? d.getBalance() : fixedBalance;
        }
        return fixedBalance;
    }
    private int currentStreak() {
        return selfProfile ? KillstreakCache.getCount() : fixedStreak;
    }
    private long currentBounty() {
        return selfProfile ? BountyCache.getSelfBounty() : fixedBounty;
    }

    // ── Cycle de vie ─────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        buttonList.clear();
        openedAt = System.currentTimeMillis();
        fadeIn = 0f;
        fadeInRaw = 0f;
        slideY = 20f;

        panelW = GuiRenderUtils.clamp(width - 90, 360, 500);
        panelH = GuiRenderUtils.clamp(height - 70, 240, 310);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void updateScreen() {
        long elapsed = System.currentTimeMillis() - openedAt;
        fadeInRaw = Math.min(1f, elapsed / 250f);
        fadeIn = GuiRenderUtils.smoothStep(fadeInRaw);
        slideY = GuiRenderUtils.lerp(slideY, 0f, 0.22f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        int py = panelY + (int) slideY;
        int accent = getAccent();

        drawRect(0, 0, width, height, withAlpha(C_OVERLAY, fadeIn));

        GuiRenderUtils.drawRoundedPanel(panelX, py, panelW, panelH, C_PANEL, C_HEADER, TITLE_H, accent);
        drawRect(panelX + 1, py + TITLE_H + 1, panelX + panelW - 1, py + panelH - 1, C_PANEL_INNER);

        // Séparateur discret sous le header (sans rouge)
        drawRect(panelX + PAD, py + TITLE_H, panelX + panelW - PAD, py + TITLE_H + 1, 0x33FFFFFF);

        drawHeader(py, accent);
        drawContent(py, accent);
        drawCloseButton(py, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private void drawHeader(int py, int accent) {
        String title = selfProfile ? "Profil" : "Profil joueur";
        drawRect(panelX + PAD - 1, py + 11, panelX + PAD + 3, py + 21, accent);
        fontRendererObj.drawStringWithShadow(title, panelX + PAD + 8, py + 12, C_TEXT_MAIN);

        // Badge "Red Conflict" en haut à droite
        String part1 = "Red";
        String part2 = " Conflict";
        int w1 = fontRendererObj.getStringWidth(part1);
        int w2 = fontRendererObj.getStringWidth(part2);
        int totalW = w1 + w2 + 12;
        int bh = 14;
        int bx = panelX + panelW - PAD - totalW;
        int by = py + 10;

        drawRect(bx, by, bx + totalW, by + bh, 0xAA0A0008);
        GuiRenderUtils.drawRectOutline(bx, by, totalW, bh, 0xAACC2030);
        fontRendererObj.drawStringWithShadow(part1, bx + 6, by + 3, 0xFFE03040);
        fontRendererObj.drawStringWithShadow(part2, bx + 6 + w1, by + 3, 0xFFF0EEF0);
    }

    // ── Contenu ──────────────────────────────────────────────────────────────

    private void drawContent(int py, int accent) {
        int contentY = py + TITLE_H + 10;
        int leftW = 142;
        int gap = 10;
        int leftX = panelX + PAD;
        int rightX = leftX + leftW + gap;
        int rightW = panelW - PAD - rightX + panelX - PAD;
        int bodyH = panelH - TITLE_H - 40;

        drawIdentityCard(leftX, contentY, leftW, bodyH, accent, lastMouseX, lastMouseY);

        int topCardH = (bodyH - 8) / 2;
        drawCombatCard(rightX, contentY, rightW, topCardH, accent);
        drawProfileCard(rightX, contentY + topCardH + 8, rightW, bodyH - topCardH - 8, accent);
    }

    private void drawIdentityCard(int x, int y, int w, int h, int accent, int mouseX, int mouseY) {
        drawCard(x, y, w, h);
        GuiRenderUtils.drawSectionHeader(fontRendererObj, x, y + 6, w, "Identite", accent);

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer ep = null;
        if (mc.thePlayer != null && mc.thePlayer.getName().equals(targetName)) ep = mc.thePlayer;
        else if (mc.theWorld != null) ep = mc.theWorld.getPlayerEntityByName(targetName);

        int modelAreaY = y + 22;
        int modelAreaH = 88;
        // Fond neutre sombre (sans rouge) pour la zone skin
        drawRect(x + 8, modelAreaY, x + w - 8, modelAreaY + modelAreaH, 0x88080810);
        GuiRenderUtils.drawGradientRect(x + 8, modelAreaY, x + w - 8, modelAreaY + modelAreaH,
                0x660C0E18, 0x880A0C14);
        GuiRenderUtils.drawRectOutline(x + 8, modelAreaY, w - 16, modelAreaH, 0x33FFFFFF);

        if (ep != null) {
            float targetLookX = (x + w / 2f) - mouseX;
            float targetLookY = (modelAreaY + modelAreaH / 2f) - mouseY;
            lookX = GuiRenderUtils.lerp(lookX, targetLookX, 0.25f);
            lookY = GuiRenderUtils.lerp(lookY, targetLookY, 0.25f);

            GlStateManager.enableDepth();
            GuiInventory.drawEntityOnScreen(x + w / 2, modelAreaY + modelAreaH - 6, 28, lookX, lookY, ep);
            GlStateManager.disableDepth();
        } else {
            String initials = targetName.substring(0, Math.min(2, targetName.length())).toUpperCase(Locale.ROOT);
            drawRect(x + w / 2 - 18, modelAreaY + 26, x + w / 2 + 18, modelAreaY + 62, 0xAA15101C);
            GuiRenderUtils.drawRectOutline(x + w / 2 - 18, modelAreaY + 26, 36, 36, accent);
            drawCenteredString(fontRendererObj, "§b§l" + initials, x + w / 2, modelAreaY + 39, 0xFFFFFFFF);
        }

        int textY = modelAreaY + modelAreaH + 8;
        fontRendererObj.drawStringWithShadow("§f§l" + trim(targetName, w - 20), x + 10, textY, C_TEXT_MAIN);

        String fac = currentFaction();
        String facColored = fac.isEmpty() ? "§8Aucune faction" : factionColor(currentFactionRelation()) + fac;
        fontRendererObj.drawStringWithShadow(facColored, x + 10, textY + 12, C_TEXT_SOFT);

        fontRendererObj.drawStringWithShadow(formatRank(currentRank()), x + 10, textY + 23, C_TEXT_MUTED);
    }

    private void drawCombatCard(int x, int y, int w, int h, int accent) {
        drawCard(x, y, w, h);
        GuiRenderUtils.drawSectionHeader(fontRendererObj, x, y + 6, w, "Combat", accent);

        int kills = currentKills();
        int deaths = currentDeaths();
        int streak = currentStreak();
        double kd = kdRatio(kills, deaths);

        int rowY = y + 24;
        rowY = drawStatRow(x, w, rowY, "Kills",      "§a" + formatNum(kills));
        rowY = drawStatRow(x, w, rowY, "Morts",      "§c" + formatNum(deaths));
        rowY = drawStatRow(x, w, rowY, "Ratio K/D",  colorKD(kd));
        drawStatRow(x, w, rowY, "Killstreak", streak > 0 ? "§6" + streak : "§8Aucun");
    }

    private void drawProfileCard(int x, int y, int w, int h, int accent) {
        drawCard(x, y, w, h);
        GuiRenderUtils.drawSectionHeader(fontRendererObj, x, y + 6, w, "Profil", accent);

        long balance = currentBalance();
        int playTime = currentPlayTime();
        String fac = currentFaction();
        long bounty = currentBounty();

        int rowY = y + 24;
        rowY = drawStatRow(x, w, rowY, "Monnaie",      "§e" + formatNum(balance) + "$");
        // PB : prend la valeur reçue dans le packet profil (toujours fraîche côté
        // serveur via PBManager.get(uuid)). Pour soi, le cache local peut être plus
        // récent si un /pb add tombe pendant l'affichage → on prend le max.
        int pb = fixedPB;
        if (selfProfile) {
            PlayerData pdPb = PlayerDataHandler.getCachedData();
            if (pdPb != null && pdPb.getPb() > pb) pb = pdPb.getPb();
        }
        rowY = drawStatRow(x, w, rowY, "Points Boutique", "§e" + formatNum(pb) + " §7PB");
        rowY = drawStatRow(x, w, rowY, "Temps de jeu", "§f" + formatPlaytime(playTime));
        int rel = currentFactionRelation();
        rowY = drawStatRow(x, w, rowY, "Faction", rel < 0 ? "§8Aucune faction" : factionColor(rel) + fac);
        drawStatRow(x, w, rowY, "Prime active", bounty > 0 ? "§6" + formatNum(bounty) + "$" : "§8Aucune");
    }

    private void drawCard(int x, int y, int w, int h) {
        drawRect(x, y, x + w, y + h, C_CARD_BG);
        GuiRenderUtils.drawRectOutline(x, y, w, h, C_CARD_EDGE);
        // Petit reflet en haut
        GuiRenderUtils.drawGradientRect(x + 1, y + 1, x + w - 1, y + 4, 0x18FFFFFF, 0);
    }

    private int drawStatRow(int x, int w, int y, String label, String value) {
        fontRendererObj.drawStringWithShadow("§7" + label, x + 10, y, C_TEXT_SOFT);
        int valueW = fontRendererObj.getStringWidth(value);
        fontRendererObj.drawStringWithShadow(value, x + w - 10 - valueW, y, C_TEXT_MAIN);
        return y + 13;
    }

    private void drawCloseButton(int py, int mouseX, int mouseY) {
        int bx = panelX + panelW - PAD - 72;
        int by = py + panelH - 22;
        boolean hovered = inside(mouseX, mouseY, bx, by, 72, 14);

        GuiRenderUtils.drawStyledButton(bx, by, 72, 14, 0xCC1A0A12, hovered ? C_BRAND : 0xFF552028, hovered);
        drawCenteredString(fontRendererObj, "§fFermer", bx + 36, by + 3, hovered ? 0xFFFFE5E7 : 0xFFC6BCC2);
    }


    private int getAccent() {
        return UITheme.getPrimary();
    }

    // ── Utils ────────────────────────────────────────────────────────────────

    private double kdRatio(int kills, int deaths) {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    private String colorKD(double v) {
        String f = String.format(Locale.ROOT, "%.2f", v);
        return "§e" + f;
    }

    private String formatRank(String rank) {
        if (rank == null || rank.isEmpty()) return "§8Joueur";
        // Convertir les codes &x (Bukkit) en §x (Minecraft client)
        String translated = rank.replace('&', '§');
        return translated.indexOf('§') >= 0 ? translated : "§7" + translated;
    }

    private String formatPlaytime(int minutes) {
        if (minutes < 60) return minutes + " min";
        int h = minutes / 60;
        int m = minutes % 60;
        if (h < 24) return h + " h" + (m > 0 ? " " + m + " m" : "");
        int d = h / 24;
        int rh = h % 24;
        return d + " j " + rh + " h";
    }

    private String formatNum(long n) {
        return String.format(Locale.US, "%,d", n).replace(',', ' ');
    }

    private static int withAlpha(int color, float t) {
        int a = (color >> 24) & 0xFF;
        return ((int) (a * Math.min(1f, Math.max(0f, t))) << 24) | (color & 0x00FFFFFF);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String trim(String s, int maxPx) {
        if (fontRendererObj.getStringWidth(s) <= maxPx) return s;
        return fontRendererObj.trimStringToWidth(s, Math.max(0, maxPx - fontRendererObj.getStringWidth("..."))) + "...";
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        int py = panelY + (int) slideY;
        int bx = panelX + panelW - PAD - 72;
        int by = py + panelH - 22;
        if (button == 0 && inside(mouseX, mouseY, bx, by, 72, 14)) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
