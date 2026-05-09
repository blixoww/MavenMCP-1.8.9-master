package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiCraftGuide;
import net.minecraft.client.gui.ui.UITheme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GuiWiki — Wiki client expliquant widgets, systemes, events et commandes.
 * Style sobre, dark, coherent avec le reste du client (Lunar-like).
 */
public class GuiWiki extends GuiScreen {

    private final GuiScreen parent;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int headerH  = 38;
    private int footerH  = 42;
    private int sidebarW = 120;
    private static final int SEARCH_BAR_H = 26;
    private int contentTop, contentBottom, contentH;
    private int contentX, contentW;

    // Sections
    private final List<Section> sections = new ArrayList<>();
    private int currentSection = 0;

    // Search
    private GuiTextField searchField;
    private final List<SearchResult> searchResults = new ArrayList<>();

    // Scroll
    private float scrollOffset = 0;
    private float scrollTarget = 0;
    private int   maxScroll    = 0;
    private boolean dragScroll = false;

    // Animation
    private float openAnim   = 0f;
    private long  lastTime   = -1L;
    private float sectionFade = 1f;

    // Colors
    private static final int BG_PANEL     = 0xF20A0808;
    private static final int BG_HEADER    = 0xFF0E0606;
    private static final int BG_SIDEBAR   = 0xFF080404;
    private static final int BG_CONTENT   = 0xFF0C0808;
    private static final int TEXT_PRIMARY = 0xFFF2F2F2;
    private static final int TEXT_SECOND  = 0xFFB0B6C0;
    private static final int TEXT_BODY    = 0xFF8A9AB0;
    private static final int TEXT_MUTED   = 0xFF445060;

    public GuiWiki(GuiScreen parent) {
        this.parent = parent;
        buildSections();
    }

    // ── Data model ────────────────────────────────────────────────────────────
    private static class Entry {
        final String title, body;
        Entry(String t, String b) { title = t; body = b; }
    }
    private static class Section {
        final String name;
        final List<Entry> entries;
        Section(String name, List<Entry> entries) { this.name = name; this.entries = entries; }
    }
    private static class SearchResult {
        final String sectionName;
        final Entry  entry;
        SearchResult(String s, Entry e) { sectionName = s; entry = e; }
    }

    // ── Content ───────────────────────────────────────────────────────────────
    private void buildSections() {

        // ── WIDGETS ──────────────────────────────────────────────────────────
        sections.add(new Section("WIDGETS", Arrays.asList(
            new Entry("FPS",                  "Affiche le nombre d'images par seconde rendues. Indicateur de performance en temps reel."),
            new Entry("Ping",                 "Latence reseau client / serveur en millisecondes. Plus c'est bas, mieux c'est."),
            new Entry("Biome",                "Affiche le biome actuel (Plaines, Foret, Desert, Nether, ...)."),
            new Entry("Coordonnees",          "Affiche votre position X / Y / Z dans le monde."),
            new Entry("Direction",            "Direction cardinale et angle de vue (N, S, E, W et degres)."),
            new Entry("Date",                 "Date et heure systeme de la machine locale."),
            new Entry("Objet tenu",           "Nom et durabilite de l'objet en main, avec barre coloree de durabilite restante."),
            new Entry("Armure",               "Affiche les 4 pieces d'armure equipees avec leur durabilite en pourcentage."),
            new Entry("Effets de Potion",     "Liste les effets de potion actifs et leur duree restante."),
            new Entry("CPS",                  "Clicks Per Second. Vitesse de clic gauche et droit en temps reel."),
            new Entry("Toggle Sneak",         "Indicateur du mode Sneak permanent active."),
            new Entry("Toggle Sprint",        "Indicateur du mode Sprint permanent active."),
            new Entry("Auto Armor",           "Indicateur visuel de l'equipement automatique d'armure."),
            new Entry("Combat Tag",           "Compte a rebours indiquant que vous etes en combat."),
            new Entry("Keystrokes",           "Affiche en direct les touches Z Q S D, espace et clics souris."),
            new Entry("Reach Display",        "Distance en blocs au dernier joueur touche. Mesure votre allonge en PvP."),
            new Entry("Boussole HUD",         "Bande de boussole horizontale avec directions cardinales, angle precis et Waypoints."),
            new Entry("Barre de vie joueurs", "Affiche la barre de vie au-dessus des joueurs visibles, avec leur nom et armure."),
            new Entry("Zone Faction",         "Affiche le nom du territoire claim et la relation (own / ally / truce / enemy / neutral).")
        )));

        // ── SYSTEMES ─────────────────────────────────────────────────────────
        sections.add(new Section("SYSTEMES", Arrays.asList(
            new Entry("Profil joueur",
                "Vue detaillee du profil : kills, deaths, killstreak, prime, temps de jeu, solde, rang et faction. " +
                "Ouverture via l'inventaire (tete joueur) ou /profil <joueur>."),
            new Entry("Guide Craft",
                "Catalogue complet des recettes : crafting, four et brassage (potions vanilla + custom). " +
                "Barre de recherche integree. Ouverture via l'inventaire (livre bleu) ou le bouton en bas du Wiki."),
            new Entry("Themes UI",
                "Personnalisez la couleur d'accent de toute l'interface via le menu Themes dans l'Editeur HUD. " +
                "S'applique a tous les widgets, menus et indicateurs du client."),
            new Entry("Waypoints",
                "Marqueurs 3D posables dans le monde. Ils sont visibles dans le widget Boussole HUD sous forme de reperes " +
                "colores avec label. Couleur et nom personnalisables."),
            new Entry("Visuals",
                "Options visuelles avancees : particules de hit personnalisees, ping à la counter strike " +
                ", particules de potions. "),
            new Entry("Shaders",
                "Le client supporte les shaderpacks compatibles OptiFine/GLSL. Changez de shaderpack dans les " +
                "parametres graphiques.")
        )));

        // ── ÉVÉNEMENTS ───────────────────────────────────────────────────────
        sections.add(new Section("EVENEMENTS", Arrays.asList(
            new Entry("KOTH - King of the Hill",
                "Une zone delimitee doit etre capturee en restant a l'interieur. " +
                "Un timer (~5 min) se declenche. Sortir ou etre tue remet le timer a zero. " +
                "Duree max ~30 min."),
            new Entry("Totem - Defense de structure",
                "Une structure (Totem) est placee sur la map. Les joueurs doivent détruire les blocks de la structure. " +
                "Victoire pour la faction qui a détruit tous les blocks (~30 min)."),
            new Entry("LMS — Last Man Standing",
                "Combat en arene : chaque faction envoie un seul representant. Aucune alliance. " +
                "Le dernier joueur en vie remporte l'event et des recompenses majeures."),
            new Entry("Domination - Controle de zones",
                "Plusieurs zones a capturer simultanement. Rester dans une zone accumule des points. " +
                "Les kills donnent des bonus. Multiplicateur dynamique si zone tres disputee. " +
                "La faction avec le plus de points en fin de timer gagne."),
            new Entry("Purge - 30 min de chaos",
                "30 minutes : les portes dans les AP s'ouvrent pour tous. Tuer en warzone rapporte argent et items. " +
                "Un seul joueur UNIQUE compte par kill. Objectif : maximiser le nombre de joueurs differents tues.")
        )));

        // ── COMMANDES ────────────────────────────────────────────────────────
        sections.add(new Section("COMMANDES", Arrays.asList(
            new Entry("/duel <joueur>",        "Defier un joueur en duel avec votre propre equipement."),
            new Entry("/duelk <joueur>",       "Defier un joueur en duel avec un kit defini (egal pour les deux)."),
            new Entry("/duelrandom",           "Duel aleatoire avec votre equipement : file d'attente automatique."),
            new Entry("/duelkrandom",          "Duel aleatoire avec un kit defini."),
            new Entry("/ks",                   "Affiche vos kills, morts, ratio K/D et temps de jeu."),
            new Entry("/profil <joueur>",      "Ouvre le profil complet d'un joueur : kills, deaths, killstreak, bounty, rang."),
            new Entry("/ct",                   "Verifie votre statut de Combat Tag actuel."),
            new Entry("/hdv",                  "Ouvre l'Hotel des Ventes : achetez et vendez des items entre joueurs."),
            new Entry("/shop",                 "Ouvre la boutique du serveur."),
            new Entry("/baltop",               "Classement des joueurs les plus riches du serveur."),
            new Entry("/prime <joueur> <$>",   "Pose une prime sur un joueur. /prime list pour les primes actives."),
            new Entry("/loto <montant>",       "Pariez sur le loto. /loto next pour le prochain tirage."),
            new Entry("/friend add <joueur>",  "Ajoute un joueur en ami."),
            new Entry("/friend list",          "Liste de vos amis."),
            new Entry("/trade <joueur>",       "Echange securise d'items avec un autre joueur."),
            new Entry("/plannings",            "Affiche les prochains evenements planifies."),
            new Entry("/repairall",            "Repare tous vos items. Cooldown de 24h."),
            new Entry("/cobble",               "Toggle le filtre cobblestone (la cobble va dans une case dediee)."),
            new Entry("/furnace this|all",     "Cuit des items sans four."),
            new Entry("/bottlexp",             "Embouteille vos niveaux d'XP."),
            new Entry("/poubelle",             "Ouvre une poubelle virtuelle."),
            new Entry("/vision",               "Toggle la vision nocturne permanente."),
            new Entry("Boutons inventaire",    "Dans l'inventaire, 3 boutons sous la table de craft : Wiki (rouge), Profil (tete) et Guide Craft (bleu).")
        )));

        // ── ECONOMIE ─────────────────────────────────────────────────────────
        sections.add(new Section("ECONOMIE", Arrays.asList(
            new Entry("HDV — Hotel des Ventes",
                "Marche entre joueurs : listez vos items a vendre, achetez aux autres. Interface graphique complete. " +
                "Commande : /hdv. Notifications en temps reel quand un item est vendu."),
            new Entry("Shop — Boutique serveur",
                "Achetez et vendez des ressources aux prix fixes du serveur. Categories par type. Commande : /shop."),
            new Entry("/baltop",
                "Classement des joueurs les plus riches du serveur. Consultez qui domine l'economie."),
            new Entry("Loto",
                "Systeme de loterie : pariez avec /loto <montant>. Le jackpot grossit a chaque mise. " +
                "Tirage periodique automatique. /loto next pour connaitre le prochain tirage. /loto help pour l'aide."),
            new Entry("Coinflip",
                "Duel economique pile-ou-face : deux joueurs misent la meme somme. Le vainqueur remporte la mise des deux. " +
                "Accessible via commande ou menu economie."),
            new Entry("Prime (/prime)",
                "Posez une prime sur un joueur avec /prime <joueur> <montant>. " +
                "Le joueur qui l'elimine empoche la prime. /prime list pour voir les primes actives."),
            new Entry("Box Acier",
                "Coffre de base au spawn, necessite une Cle Acier. Contient des ressources communes : outils, " +
                "armures fer / chainmail, nourriture, materiaux de construction."),
            new Entry("Box Emeraude",
                "Coffre intermediaire. Necessite une Cle Emeraude. Recompenses ameliorees : enchants, " +
                "equipements diamant, potions, ressources rares."),
            new Entry("Box Ruby",
                "Coffre haut niveau. Necessite une Cle Ruby. Recompenses premiums : sets diamant enchantes, " +
                "potions puissantes, items rares et monnaie."),
            new Entry("Box Cobalt",
                "Coffre ultime. Necessite une Cle Cobalt. Meilleures recompenses du serveur : equipements " +
                "maximalises, grosses sommes d'argent, items exclusifs de rang.")
        )));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();
        this.openAnim = 0f;

        panelW = MathHelper.clamp_int(this.width - 60, 380, 540);
        panelH = MathHelper.clamp_int(this.height - 40, 260, 420);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        contentTop    = panelY + headerH;
        contentBottom = panelY + panelH - footerH;
        contentH      = contentBottom - contentTop;
        contentX      = panelX + sidebarW;
        contentW      = panelW - sidebarW;

        // Search field (inside content area, top strip)
        String prevSearch = (searchField != null) ? searchField.getText() : "";
        searchField = new GuiTextField(10, this.fontRendererObj,
                contentX + 20, contentTop + 6, contentW - 30, 13);
        searchField.setMaxStringLength(40);
        searchField.setText(prevSearch);
        if (!prevSearch.isEmpty()) updateSearch();

        recomputeMaxScroll();
    }

    private void recomputeMaxScroll() {
        int usableH = contentH - SEARCH_BAR_H;
        int total = 6;
        if (isSearchActive()) {
            String lastSec = null;
            for (SearchResult r : searchResults) {
                if (!r.sectionName.equals(lastSec)) { total += 16; lastSec = r.sectionName; }
                total += entryHeight(r.entry) + 8;
            }
        } else {
            for (Entry e : sections.get(currentSection).entries) {
                total += entryHeight(e) + 8;
            }
        }
        maxScroll = Math.max(0, total - usableH + 6);
        scrollTarget = MathHelper.clamp_float(scrollTarget, 0, maxScroll);
    }

    @SuppressWarnings("unchecked")
    private int entryHeight(Entry e) {
        int wrapW = contentW - 24;
        List<String> body = (List<String>) this.fontRendererObj.listFormattedStringToWidth(e.body, wrapW);
        return 12 + 2 + body.size() * 10 + 4;
    }

    private boolean isSearchActive() {
        return searchField != null && !searchField.getText().trim().isEmpty();
    }

    private void updateSearch() {
        searchResults.clear();
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        if (!q.isEmpty()) {
            for (Section s : sections) {
                for (Entry e : s.entries) {
                    if (e.title.toLowerCase().contains(q) || e.body.toLowerCase().contains(q)) {
                        searchResults.add(new SearchResult(s.name, e));
                    }
                }
            }
        }
        scrollTarget = 0;
        scrollOffset = 0;
        recomputeMaxScroll();
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime > 0) {
            float dt = (now - lastTime) / 1000f;
            openAnim = MathHelper.clamp_float(openAnim + dt * 5f, 0f, 1f);
        }
        lastTime = now;
        float ease = openAnim * openAnim * (3f - 2f * openAnim);

        sectionFade  = GuiRenderUtils.lerp(sectionFade, 1f, 0.25f);
        scrollOffset = GuiRenderUtils.lerp(scrollOffset, scrollTarget, 0.25f);

        Gui.drawRect(0, 0, this.width, this.height, (int)(ease * 0xA0) << 24);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1f - ease) * 10, 0);

        GuiRenderUtils.drawShadow(panelX, panelY, panelW, panelH, 8, (int)(ease * 0x90));
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_PANEL);

        // Top accent
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 2, UITheme.getPrimary());
        GuiRenderUtils.drawGradientRect(panelX, panelY + 2, panelX + panelW, panelY + 8, UITheme.primary(0x18), 0x00000000);

        // Header
        GuiRenderUtils.drawGradientRect(panelX, panelY + 2, panelX + panelW, panelY + headerH, BG_HEADER, BG_PANEL);
        Gui.drawRect(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, 0x30FFFFFF);
        String title = "WIKI";
        int tw  = fontRendererObj.getStringWidth(title);
        int tw1 = fontRendererObj.getStringWidth("W");
        int ttx = panelX + (panelW - tw) / 2;
        int tty = panelY + (headerH - 14) / 2;
        fontRendererObj.drawStringWithShadow("W", ttx, tty, UITheme.getPrimary());
        fontRendererObj.drawStringWithShadow(title.substring(1), ttx + tw1, tty, 0xFF8A9AB0);
        String sub = "Guide officiel du serveur";
        int sw2 = fontRendererObj.getStringWidth(sub);
        fontRendererObj.drawString(sub, panelX + (panelW - sw2) / 2, tty + 10, TEXT_MUTED);

        // Sidebar
        Gui.drawRect(panelX, contentTop, panelX + sidebarW, contentBottom, BG_SIDEBAR);
        Gui.drawRect(panelX + sidebarW, contentTop, panelX + sidebarW + 1, contentBottom, 0x30FFFFFF);
        drawSidebar(mouseX, mouseY);

        // Content
        Gui.drawRect(panelX + sidebarW + 1, contentTop, panelX + panelW, contentBottom, BG_CONTENT);
        drawContent(mouseX, mouseY);

        // Footer
        Gui.drawRect(panelX, contentBottom, panelX + panelW, contentBottom + 1, 0x30FFFFFF);
        int btnH  = 24;
        int btnY  = contentBottom + (footerH - btnH) / 2;

        int btnRetourW = 120;
        int btnRetourX = panelX + panelW - btnRetourW - 10;
        boolean hoverDone = inRect(mouseX, mouseY, btnRetourX, btnY, btnRetourW, btnH);
        drawSecondaryButton(btnRetourX, btnY, btnRetourW, btnH, "RETOUR", hoverDone);

        int btnGuideW = 130;
        int btnGuideX = panelX + 10;
        boolean hoverGuide = inRect(mouseX, mouseY, btnGuideX, btnY, btnGuideW, btnH);
        drawSecondaryButton(btnGuideX, btnY, btnGuideW, btnH, "GUIDE CRAFT", hoverGuide);

        GuiRenderUtils.drawRectOutline(panelX, panelY, panelW, panelH, UITheme.primary(0x2B));

        GlStateManager.popMatrix();
    }

    private void drawSidebar(int mx, int my) {
        int rowH = 28;
        int y = contentTop + 8;
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            boolean active  = (i == currentSection) && !isSearchActive();
            boolean hovered = inRect(mx, my, panelX + 4, y, sidebarW - 8, rowH);

            if (active) {
                Gui.drawRect(panelX + 4, y, panelX + sidebarW - 4, y + rowH, 0x22FFFFFF);
                Gui.drawRect(panelX + 4, y, panelX + 7, y + rowH, UITheme.getPrimary());
                GuiRenderUtils.drawGradientRect(panelX + 7, y, panelX + 24, y + rowH, UITheme.primary(0x18), 0x00000000);
            } else if (hovered) {
                Gui.drawRect(panelX + 4, y, panelX + sidebarW - 4, y + rowH, 0x12FFFFFF);
                Gui.drawRect(panelX + 4, y, panelX + 6, y + rowH, UITheme.primary(0x55));
            }

            int col = active ? TEXT_PRIMARY : (hovered ? TEXT_SECOND : 0xFF6E7480);
            fontRendererObj.drawStringWithShadow(s.name, panelX + 14, y + (rowH - 8) / 2 - 3, col);

            String count = String.valueOf(s.entries.size());
            int cw = fontRendererObj.getStringWidth(count) + 6;
            int cx = panelX + sidebarW - 8 - cw;
            int cy = y + (rowH - 10) / 2;
            int badgeBg = active ? (UITheme.getPrimary() & 0x00FFFFFF | 0x30000000) : 0x14FFFFFF;
            Gui.drawRect(cx, cy, cx + cw, cy + 10, badgeBg);
            fontRendererObj.drawString(count, cx + 3, cy + 1,
                    active ? UITheme.primary(0xDD) : 0xFF556070);

            y += rowH;
        }

        String hint = isSearchActive() ? "Recherche globale" : "ESC pour fermer";
        fontRendererObj.drawString(hint, panelX + 8, contentBottom - 14, TEXT_MUTED);
    }

    private void drawSearchBar(int mx, int my) {
        // Strip background
        Gui.drawRect(contentX, contentTop, panelX + panelW, contentTop + SEARCH_BAR_H, 0x0A000000);
        Gui.drawRect(contentX, contentTop + SEARCH_BAR_H - 1, panelX + panelW, contentTop + SEARCH_BAR_H, 0x20FFFFFF);

        int fx = contentX + 20, fy = contentTop + 5, fw = contentW - 30, fh = 15;
        boolean focused = searchField != null && searchField.isFocused();

        // Field background + border
        Gui.drawRect(fx, fy, fx + fw, fy + fh, focused ? 0x18FFFFFF : 0x0CFFFFFF);
        GuiRenderUtils.drawRectOutline(fx, fy, fw, fh,
                focused ? UITheme.primary(0x66) : 0x25FFFFFF);

        // Magnifier icon (left of field)
        int ix = contentX + 8, iy = contentTop + (SEARCH_BAR_H - 8) / 2;
        int ic = focused ? UITheme.primary(0xAA) : 0x44FFFFFF;
        Gui.drawRect(ix,     iy,     ix + 5,  iy + 1,  ic);
        Gui.drawRect(ix,     iy + 4, ix + 5,  iy + 5,  ic);
        Gui.drawRect(ix,     iy,     ix + 1,  iy + 5,  ic);
        Gui.drawRect(ix + 4, iy,     ix + 5,  iy + 5,  ic);
        Gui.drawRect(ix + 3, iy + 4, ix + 5,  iy + 7,  ic); // handle

        // Text field
        if (searchField != null) searchField.drawTextBox();

        // Placeholder
        if (searchField != null && searchField.getText().isEmpty()) {
            fontRendererObj.drawString("Rechercher dans le Wiki...",
                    fx + 3, fy + (fh - 8) / 2, TEXT_MUTED);
        }

        // Result count badge
        if (isSearchActive()) {
            int count = searchResults.size();
            String badge = count + (count != 1 ? " resultats" : " resultat");
            int bw = fontRendererObj.getStringWidth(badge) + 8;
            int bx = panelX + panelW - bw - 8;
            int by = contentTop + (SEARCH_BAR_H - 10) / 2;
            Gui.drawRect(bx, by, bx + bw, by + 10, UITheme.primary(0x22));
            fontRendererObj.drawString(badge, bx + 4, by + 1, UITheme.primary(0xAA));
        }
    }

    @SuppressWarnings("unchecked")
    private void drawContent(int mx, int my) {
        // ── Search bar (drawn before scissor) ────────────────────────────────
        drawSearchBar(mx, my);

        int entryAreaTop = contentTop + SEARCH_BAR_H;
        int entryAreaH   = contentH   - SEARCH_BAR_H;

        // ── Scissor: clip to entry zone only ────────────────────────────────
        ScaledResolution sr = new ScaledResolution(mc);
        int factor = sr.getScaleFactor();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(
                (contentX + 1) * factor,
                (this.height - contentBottom) * factor,
                (contentW - 1) * factor,
                entryAreaH * factor
        );

        int alpha    = (int)(MathHelper.clamp_float(sectionFade, 0f, 1f) * 0xFF) << 24;
        int titleCol = (TEXT_PRIMARY & 0xFFFFFF) | alpha;
        int bodyCol  = (TEXT_BODY    & 0xFFFFFF) | alpha;

        int y = entryAreaTop + 6 - (int) scrollOffset;

        if (isSearchActive()) {
            // ── Search results across all sections ──────────────────────────
            String lastSec = null;
            int idx = 0;
            for (SearchResult r : searchResults) {
                // Section badge header
                if (!r.sectionName.equals(lastSec)) {
                    lastSec = r.sectionName;
                    if (y + 14 >= entryAreaTop - 10 && y <= contentBottom + 10) {
                        int bw = fontRendererObj.getStringWidth(r.sectionName) + 8;
                        Gui.drawRect(contentX + 10, y + 1, contentX + 10 + bw, y + 13,
                                UITheme.primary(0x22));
                        GuiRenderUtils.drawRectOutline(contentX + 10, y + 1, bw, 12,
                                UITheme.primary(0x33));
                        fontRendererObj.drawString(r.sectionName, contentX + 14, y + 3,
                                UITheme.primary(0xBB));
                    }
                    y += 16;
                }
                int eh = entryHeight(r.entry);
                if (y + eh >= entryAreaTop - 10 && y <= contentBottom + 10) {
                    renderEntry(r.entry, y, titleCol, bodyCol, (idx % 2 == 0));
                }
                y += eh + 8;
                idx++;
            }
            if (searchResults.isEmpty()) {
                String msg = "Aucun resultat pour \"" + searchField.getText().trim() + "\"";
                int mw = fontRendererObj.getStringWidth(msg);
                fontRendererObj.drawString(msg, contentX + (contentW - mw) / 2,
                        entryAreaTop + 30, TEXT_MUTED);
            }
        } else {
            // ── Normal section display ──────────────────────────────────────
            Section s = sections.get(currentSection);
            int idx = 0;
            for (Entry e : s.entries) {
                int eh = entryHeight(e);
                if (y + eh >= entryAreaTop - 10 && y <= contentBottom + 10) {
                    renderEntry(e, y, titleCol, bodyCol, (idx % 2 == 0));
                }
                y += eh + 8;
                idx++;
            }
        }

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);

        // ── Scrollbar ────────────────────────────────────────────────────────
        if (maxScroll > 0) {
            int sbW  = 5;
            int sbX  = panelX + panelW - sbW - 3;
            int sbTH = entryAreaH - 4;
            Gui.drawRect(sbX, entryAreaTop + 2, sbX + sbW, entryAreaTop + 2 + sbTH, 0x18FFFFFF);
            float ratio  = scrollOffset / (float) maxScroll;
            float thumbR = (float) entryAreaH / (entryAreaH + maxScroll);
            int   thumbH = Math.max(20, (int)(sbTH * thumbR));
            int   thumbY = (int)((sbTH - thumbH) * ratio);
            Gui.drawRect(sbX, entryAreaTop + 2 + thumbY, sbX + sbW,
                    entryAreaTop + 2 + thumbY + thumbH, UITheme.getPrimaryDim());
            Gui.drawRect(sbX, entryAreaTop + 2 + thumbY, sbX + sbW,
                    entryAreaTop + 3 + thumbY, UITheme.primary(0x99));
        }

        // ── Fade overlays ────────────────────────────────────────────────────
        if (scrollOffset > 2) {
            GuiRenderUtils.drawGradientRect(contentX + 1, entryAreaTop,
                    panelX + panelW - 1, entryAreaTop + 14, 0xCC0C0808, 0x000C0808);
        }
        if (scrollOffset < maxScroll - 2) {
            GuiRenderUtils.drawGradientRect(contentX + 1, contentBottom - 14,
                    panelX + panelW - 1, contentBottom, 0x000C0808, 0xCC0C0808);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderEntry(Entry e, int y, int titleCol, int bodyCol, boolean even) {
        int wrapW = contentW - 24;
        int eh    = entryHeight(e);
        Gui.drawRect(contentX + 8, y + 1, panelX + panelW - 8, y + eh,
                even ? 0x06FFFFFF : 0x03FFFFFF);
        Gui.drawRect(contentX + 10, y + 5, contentX + 14, y + 11, UITheme.getPrimary());
        Gui.drawRect(contentX + 10, y + 11, contentX + 12, y + 13, UITheme.primary(0x44));
        fontRendererObj.drawStringWithShadow(e.title, contentX + 20, y + 3, titleCol);
        List<String> body = (List<String>) fontRendererObj.listFormattedStringToWidth(e.body, wrapW);
        int by = y + 15;
        for (int i2 = 0; i2 < body.size(); i2++) {
            fontRendererObj.drawString(body.get(i2), contentX + 20, by + i2 * 10, bodyCol);
        }
        int sepY = y + eh - 1;
        GuiRenderUtils.drawGradientRect(contentX + 14, sepY, contentX + contentW - 18, sepY + 1,
                UITheme.primary(0x22), 0x00000000);
        GuiRenderUtils.drawGradientRect(contentX + 14, sepY, contentX + contentW - 18, sepY + 1,
                0x00000000, UITheme.primary(0x22));
    }

    private void drawSecondaryButton(int x, int y, int w, int h, String text, boolean hovered) {
        int bg = hovered ? 0x1EFFFFFF : 0x0A000000;
        Gui.drawRect(x, y, x + w, y + h, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, h, hovered ? 0x44FFFFFF : 0x28FFFFFF);
        if (hovered) Gui.drawRect(x, y + 1, x + 2, y + h - 1, UITheme.getPrimary());
        int tw = fontRendererObj.getStringWidth(text);
        fontRendererObj.drawStringWithShadow(text, x + (w - tw) / 2, y + (h - 8) / 2,
                hovered ? 0xFFFFFFFF : 0xFFCCCCDD);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;

        // Forward to search field first
        if (searchField != null) searchField.mouseClicked(mx, my, btn);

        // Footer buttons
        int btnH      = 24;
        int btnY      = contentBottom + (footerH - btnH) / 2;
        int btnRetourW = 120;
        int bX = panelX + panelW - btnRetourW - 10;
        if (inRect(mx, my, bX, btnY, btnRetourW, btnH)) {
            this.mc.displayGuiScreen(parent);
            return;
        }
        int btnGuideW = 130;
        if (inRect(mx, my, panelX + 10, btnY, btnGuideW, btnH)) {
            this.mc.displayGuiScreen(new GuiCraftGuide(this));
            return;
        }

        // Sidebar tabs (only when not in search mode)
        if (!isSearchActive()) {
            int rowH = 28;
            int y = contentTop + 8;
            for (int i = 0; i < sections.size(); i++) {
                if (inRect(mx, my, panelX + 4, y, sidebarW - 8, rowH)) {
                    if (i != currentSection) {
                        currentSection = i;
                        scrollTarget = 0; scrollOffset = 0; sectionFade = 0f;
                        recomputeMaxScroll();
                    }
                    return;
                }
                y += rowH;
            }
        }

        // Scrollbar drag
        if (maxScroll > 0) {
            int sbW = 5;
            int sbX = panelX + panelW - sbW - 3;
            int entryAreaTop = contentTop + SEARCH_BAR_H;
            if (mx >= sbX - 2 && mx <= sbX + sbW + 2 && my >= entryAreaTop && my < contentBottom) {
                dragScroll = true;
                updateScrollFromMouse(my);
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (dragScroll) updateScrollFromMouse(my);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        dragScroll = false;
        super.mouseReleased(mx, my, state);
    }

    private void updateScrollFromMouse(int my) {
        int entryAreaTop = contentTop + SEARCH_BAR_H;
        int entryAreaH   = contentH   - SEARCH_BAR_H;
        float ratio = (float)(my - entryAreaTop) / (float) entryAreaH;
        scrollTarget = MathHelper.clamp_float(ratio * maxScroll, 0, maxScroll);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (inRect(mx, my, contentX, contentTop, contentW, contentH)) {
                scrollTarget += scroll > 0 ? -24 : 24;
                scrollTarget = MathHelper.clamp_float(scrollTarget, 0, maxScroll);
            }
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            this.mc.displayGuiScreen(parent);
            return;
        }

        // Forward to search field
        if (searchField != null && searchField.textboxKeyTyped(c, keyCode)) {
            updateSearch();
            return;
        }

        // Arrow keys switch section (only when search not active)
        if (!isSearchActive()) {
            if (keyCode == 200) { // up
                if (currentSection > 0) {
                    currentSection--; scrollTarget = 0; scrollOffset = 0;
                    sectionFade = 0f; recomputeMaxScroll();
                }
            } else if (keyCode == 208) { // down
                if (currentSection < sections.size() - 1) {
                    currentSection++; scrollTarget = 0; scrollOffset = 0;
                    sectionFade = 0f; recomputeMaxScroll();
                }
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private boolean inRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx < rx + rw && my < ry + rh;
    }
}
