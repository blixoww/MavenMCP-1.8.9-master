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
    private int headerH = 38;
    private int footerH = 42;
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
    private int maxScroll = 0;
    private boolean dragScroll = false;

    // Animation
    private float openAnim = 0f;
    private long lastTime = -1L;
    private float sectionFade = 1f;

    // Colors
    private static final int BG_PANEL = 0xF20A0808;
    private static final int BG_HEADER = 0xFF0E0606;
    private static final int BG_SIDEBAR = 0xFF080404;
    private static final int BG_CONTENT = 0xFF0C0808;
    private static final int TEXT_PRIMARY = 0xFFF2F2F2;
    private static final int TEXT_SECOND = 0xFFB0B6C0;
    private static final int TEXT_BODY = 0xFF8A9AB0;
    private static final int TEXT_MUTED = 0xFF445060;

    // Couleur d'accent par section
    private static final int COL_WIDGETS = 0xFFE02828;
    private static final int COL_SYSTEMES = 0xFF2299CC;
    private static final int COL_EVENTS = 0xFFE06020;
    private static final int COL_COMMANDES = 0xFF33BB55;
    private static final int COL_ECONOMIE = 0xFFCCAA00;
    private static final int COL_GRADES = 0xFF9944CC;

    public GuiWiki(GuiScreen parent) {
        this.parent = parent;
        buildSections();
    }

    // ── Data model ────────────────────────────────────────────────────────────
    private static class Entry {
        final String title, body;

        Entry(String t, String b) {
            title = t;
            body = b;
        }
    }

    private static class Section {
        final String name;
        final List<Entry> entries;
        final int color;

        Section(String name, int color, List<Entry> entries) {
            this.name = name;
            this.color = color;
            this.entries = entries;
        }
    }

    private static class SearchResult {
        final String sectionName;
        final Entry entry;

        SearchResult(String s, Entry e) {
            sectionName = s;
            entry = e;
        }
    }

    // ── Content ───────────────────────────────────────────────────────────────
    private void buildSections() {

        // ── WIDGETS ──────────────────────────────────────────────────────────
        sections.add(new Section("WIDGETS", COL_WIDGETS, Arrays.asList(
                new Entry("FPS",
                        "§7Nombre d'images par seconde. §cBas = lag§7 · §ahaut = fluide§7. Indicateur de performance en temps réel."),
                new Entry("Ping",
                        "§7Latence réseau en §6ms§7. §a< 50 ms§7 = idéal · §e50–100 ms§7 = correct · §c> 150 ms§7 = élevé."),
                new Entry("Biome",
                        "§7Biome actuel : §2Forêt§7, §6Désert§7, §bOcéan§7, §cNether§7, §dEnd§7..."),
                new Entry("Coordonnées",
                        "§7Position §eX §7/ §eY §7/ §eZ§7 dans le monde. Idéal pour partager un emplacement précis."),
                new Entry("Direction",
                        "§7Direction cardinale §e(N, S, E, W)§7 et angle de vue en degrés."),
                new Entry("Date",
                        "§7Date et heure §fsystème§7 de votre machine locale."),
                new Entry("Objet tenu",
                        "§7Nom et §edurabilité§7 de l'objet en main. Barre de progression §6colorée§7 selon l'état."),
                new Entry("Armure",
                        "§74 pièces d'armure avec leur durabilité restante en §6%§7. Layout horizontal ou vertical."),
                new Entry("Effets de Potion",
                        "§7Liste les effets actifs avec le temps restant. §abuffs§7 vs §cdebuffs§7 différenciés par couleur."),
                new Entry("CPS",
                        "§fClicks Per Second§7. CPS §egauche§7 & §edroit§7 en temps réel — essentiel en §cPvP§7."),
                new Entry("Toggle Sneak",
                        "§7HUD du §eSneak permanent§7. Un appui = sneak activé jusqu'au prochain appui."),
                new Entry("Toggle Sprint",
                        "§7HUD du §eSprint permanent§7. Un appui = sprint activé automatiquement."),
                new Entry("Auto Armor",
                        "§7Indique si l'§eauto-équipement d'armure§7 est actif. Équipe le meilleur set disponible."),
                new Entry("Combat Tag",
                        "§cCompte à rebours§7 actif dès qu'on entre en combat. §cQuitter le serveur = mort instantanée§7."),
                new Entry("Keystrokes",
                        "§7Touches §eZ Q S D§7, espace et clics en direct. Utile pour §fstream / replay§7."),
                new Entry("Reach Display",
                        "§7Distance en §6blocs§7 au dernier joueur touché. Mesure votre §eallonge§7 effective en PvP."),
                new Entry("Boussole HUD",
                        "§7Bande horizontale avec §edirections cardinales§7, angle précis et §6Waypoints§7 visibles en temps réel."),
                new Entry("Barre de vie joueurs",
                        "§fBarre de vie§7 au-dessus des joueurs à portée, avec leur nom et armure."),
                new Entry("Zone Faction",
                        "§7Territoire actuel. Relation : §aown§7 / §aallie§7 · §etruce§7 · §cenemy§7 · §7neutral."),
                new Entry("Boutons inventaire", "§7Sous la table de craft : §cwiki§7 (rouge) · §6profil§7 · §bguide craft§7 (bleu).")

        )));

        // ── SYSTÈMES ─────────────────────────────────────────────────────────
        sections.add(new Section("SYSTEMES", COL_SYSTEMES, Arrays.asList(
                new Entry("Profil joueur",
                        "§7Vue détaillée : §ekills§7, §edeaths§7, §6killstreak§7, §cprime§7, solde, rang et faction. " +
                                "Ouverture via §e/profil <joueur>§7 ou le bouton §ftête§7 dans l'inventaire."),
                new Entry("Guide Craft",
                        "§7Catalogue complet des recettes : §ecrafting§7, §6four§7, §bbrassage§7 (potions vanilla & custom). " +
                                "Barre de recherche intégrée. Bouton §flivre bleu§7 dans l'inventaire."),
                new Entry("Thèmes UI",
                        "§7Couleur de l'interface HUD, modifiable via §fThèmes§7 dans l'Éditeur HUD. " +
                                "S'applique à tous les §ewidgets§7, menus et indicateurs du client."),
                new Entry("Waypoints",
                        "§6Marqueurs 3D§7 posables dans le monde, visibles dans la §eBoussole HUD§7. " +
                                "Couleur et nom §fpersonnalisables§7."),
                new Entry("Visuals",
                        "§7Options visuelles avancées : §eparticules de hit§7, §bping CS:GO§7 3D in-world, " +
                                "§eparticules de potions§7. Activables dans les paramètres client."),
                new Entry("Shaders",
                        "§7Shaderpacks §fcompatibles OptiFine / GLSL§7. Changez de pack dans §eles paramètres graphiques§7. " +
                                "§cPeut impacter les performances.§7")
        )));

        // ── ÉVÉNEMENTS ───────────────────────────────────────────────────────
        sections.add(new Section("EVENEMENTS", COL_EVENTS, Arrays.asList(
                new Entry("KOTH - King of the Hill",
                        "§7Capturez une zone en y restant et en repoussant les autres joueurs. "),
                new Entry("Totem - destruction de structure",
                        "§7Une structure §e(Totem)§7 est placée sur la map. Détruisez les blocks pour gagner."),
                new Entry("LMS - Last Man Standing",
                        "§fEn solo ou en duo par faction§7 en arène. §cAucune alliance§7. " +
                                "Le §adernier en vie§7 remporte l'event et des §6récompenses majeures§7. §cMourir = élimination définitive§7."),
                new Entry("Domination - Contrôle de zones",
                        "§7Capturez plusieurs zones simultanément. §aRester dans une zone§7 = §6points§7. " +
                                "§eKills§7 = bonus. §fMultiplicateur§7 dynamique. §aLa faction avec le plus de points§7 gagne."),
                new Entry("Purge - 30 min de chaos",
                        "§630 minutes§7 : les portes des §cAP s'ouvrent§7 pour tous. " +
                                "§eKill en warzone§7 = argent + items. §fUn seul joueur unique§7 comptabilisé. " +
                                "Objectif : §amaximiser§7 les joueurs différents éliminés."),
                new Entry("Nexus - Destruction de cristal",
                        "§fEnder Crystal§7 (§eNexus§7) placé sur la map : §cdétruisez-le. " +
                                "§6Récompenses§7 pour la faction qui fait le plus de dégâts.")
        )));

        // ── COMMANDES ────────────────────────────────────────────────────────
        sections.add(new Section("COMMANDES", COL_COMMANDES, Arrays.asList(
                new Entry("/duel <joueur>", "§7Défiez un joueur en §eduel§7 avec votre équipement. Le joueur doit §aaccepter§7."),
                new Entry("/duelk <joueur>", "§7Duel avec §6kit défini§7 identique pour les deux. Requiert §aacceptation§7."),
                new Entry("/duelrandom", "§7Duel §ealéatoire§7 avec votre équipement : entrée en §ffile d'attente§7 automatique."),
                new Entry("/duelkrandom", "§7Duel §ealéatoire§7 avec §6kit défini§7. Même principe que §e/duelrandom§7."),
                new Entry("/ks", "§7Vos §ekills§7, §edeaths§7, ratio §6K/D§7 et temps de jeu."),
                new Entry("/profil <joueur>", "§7Profil complet : §ekills§7, §edeaths§7, §6killstreak§7, §cprime§7, rang, faction."),
                new Entry("/ct", "§7Vérifie votre §6Combat Tag§7. §cActif = ne pas quitter§7 le serveur."),
                new Entry("/hdv", "§7Ouvre l'§eHôtel des Ventes§7 : listez, achetez et vendez entre joueurs."),
                new Entry("/shop", "§7Boutique serveur aux §6prix fixes§7 par catégorie."),
                new Entry("/baltop", "§fClassement§7 des joueurs les plus §6riches§7 du serveur."),
                new Entry("/prime <joueur> <$>", "§7Posez une §cprime§7 sur un joueur. §e/prime list§7 = primes actives."),
                new Entry("/loto <montant>", "§7Participez au §6loto§7. §e/loto next§7 = prochain tirage · §e/loto help§7 = aide."),
                new Entry("/friend add <joueur>", "§7Ajoutez un §eami§7. Les amis ne se font §apas de dégâts§7 mutuels."),
                new Entry("/friend list", "§7Liste de vos §famis§7 actuels."),
                new Entry("/trade <joueur>", "§fÉchange sécurisé§7 d'items. Les deux parties doivent §aconfirmer§7."),
                new Entry("/plannings", "§fÉvénements§7 à venir : §eKOTH§7, §eTotem§7, §eLMS§7, §ePurge§7, §6Loto§7..."),
                new Entry("/repairall", "§aRépare§7 tous vos items. §cCooldown : §624h§7."),
                new Entry("/cobble", "§7Toggle le filtre cobblestone. La cobble va dans une §ecases dédiée§7."),
                new Entry("/furnace this|all", "§aCuit§7 sans four. §e/furnace this§7 = main · §e/furnace all§7 = inventaire."),
                new Entry("/bottlexp", "§7Embouteille vos §6niveaux d'XP§7 dans une bouteille récupérable."),
                new Entry("/poubelle", "§cPoubelle virtuelle§7 : tout ce qui est déposé est §csupprimé§7 à la fermeture."),
                new Entry("/vision", "§7Toggle la §fvision nocturne§7 permanente sans potion."),
                new Entry("/wiki", "§7Ouvre le §fWiki§7 depuis le chat. Équivalent au §cbouton rouge§7 dans l'inventaire.")
        )));

        // ── ÉCONOMIE ─────────────────────────────────────────────────────────
        sections.add(new Section("ECONOMIE", COL_ECONOMIE, Arrays.asList(
                new Entry("HDV — Hôtel des Ventes",
                        "§fMarché§7 entre joueurs via §e/hdv§7. Listez vos objets à vendre, achetez aux autres. " +
                                "§aNotification§7 en temps réel quand un item est vendu."),
                new Entry("Shop — Boutique serveur",
                        "§7Achetez / vendez des ressources aux §6prix fixes§7 du serveur via §e/shop§7. " +
                                "Catégories par type de ressource."),
                new Entry("/baltop",
                        "§fClassement§7 économique. Qui domine le §6marché§7 du serveur ?"),
                new Entry("Loto",
                        "§7Pariez avec §e/loto <montant>§7. Le §6jackpot§7 grossit à chaque mise. " +
                                "Tirage §fpériodique§7. §e/loto next§7 = prochain tirage."),
                new Entry("Coinflip",
                        "§6Pile ou face§7 : deux joueurs misent la même somme. §aLe vainqueur§7 remporte §f×2 la mise§7."),
                new Entry("Prime (/prime)",
                        "§e/prime <joueur> <montant>§7 : posez une prime. §aLe tueur§7 l'empoche. " +
                                "§e/prime list§7 = primes actives."),
                new Entry("Box Acier",
                        "§7Coffre §fniveau 1§7. Clé §fAcier§7 requise. Ressources communes : outils, armures fer, matériaux."),
                new Entry("Box Émeraude",
                        "§7Coffre §aniveau 2§7. Clé §aÉmeraude§7 requise. Enchants, équipements diamant, potions."),
                new Entry("Box Ruby",
                        "§7Coffre §cniveau 3§7. Clé §cRuby§7 requise. Sets diamant enchantés, potions puissantes, items rares."),
                new Entry("Box Cobalt",
                        "§7Coffre §bniveau 4§7. Clé §bCobalt§7 requise. §fMeilleures récompenses§7 : équipements max, §6grosses sommes§7, " +
                                "items §ddisponibles exclusivement§7 ici.")
        )));

        // ── GRADES ───────────────────────────────────────────────────────────
        sections.add(new Section("GRADES", COL_GRADES, Arrays.asList(
                new Entry("Guerrier — Grade de base",
                        "§7Grade attribué à §ftous§7 les joueurs à l'inscription. " +
                                "Avantages : §e/cobble§7 · §e/kit guerrier§7 · §62 homes§7."),
                new Entry("Elite",
                        "§7Grade §asupérieur§7 débloquant de nouvelles commandes. " +
                                "Avantages : §e/back§7 · §e/craft§7 · §e/furnace§7 · §e/vision§7 · §e/kit elite§7 · §66 homes§7."),
                new Entry("Immortel",
                        "§7Grade §6premium§7 — le plus élevé du serveur. " +
                                "Avantages : §e/near§7 · §e/ec§7 · §e/repairall§7 · §e/kit immortel§7 · §610 homes§7.")
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

        contentTop = panelY + headerH;
        contentBottom = panelY + panelH - footerH;
        contentH = contentBottom - contentTop;
        contentX = panelX + sidebarW;
        contentW = panelW - sidebarW;

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
                if (!r.sectionName.equals(lastSec)) {
                    total += 16;
                    lastSec = r.sectionName;
                }
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

        sectionFade = GuiRenderUtils.lerp(sectionFade, 1f, 0.25f);
        scrollOffset = GuiRenderUtils.lerp(scrollOffset, scrollTarget, 0.25f);

        Gui.drawRect(0, 0, this.width, this.height, (int) (ease * 0xA0) << 24);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1f - ease) * 10, 0);

        GuiRenderUtils.drawShadow(panelX, panelY, panelW, panelH, 8, (int) (ease * 0x90));
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_PANEL);

        // Top accent
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 2, UITheme.getPrimary());
        GuiRenderUtils.drawGradientRect(panelX, panelY + 2, panelX + panelW, panelY + 8, UITheme.primary(0x18), 0x00000000);

        // Header
        GuiRenderUtils.drawGradientRect(panelX, panelY + 2, panelX + panelW, panelY + headerH, BG_HEADER, BG_PANEL);
        Gui.drawRect(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, 0x30FFFFFF);
        String title = "WIKI";
        int tw = fontRendererObj.getStringWidth(title);
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
        int btnH = 24;
        int btnY = contentBottom + (footerH - btnH) / 2;

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
            boolean active = (i == currentSection) && !isSearchActive();
            boolean hovered = inRect(mx, my, panelX + 4, y, sidebarW - 8, rowH);

            if (active) {
                Gui.drawRect(panelX + 4, y, panelX + sidebarW - 4, y + rowH, 0x22FFFFFF);
                Gui.drawRect(panelX + 4, y, panelX + 7, y + rowH, s.color);
                GuiRenderUtils.drawGradientRect(panelX + 7, y, panelX + 24, y + rowH,
                        (s.color & 0xFFFFFF) | 0x18000000, 0x00000000);
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
            int badgeBg = active ? (s.color & 0x00FFFFFF | 0x30000000) : 0x14FFFFFF;
            Gui.drawRect(cx, cy, cx + cw, cy + 10, badgeBg);
            fontRendererObj.drawString(count, cx + 3, cy + 1,
                    active ? (s.color & 0xFFFFFF | 0xDD000000) : 0xFF556070);

            y += rowH;
        }

        // Hint de navigation
        String hint = isSearchActive() ? "§7Recherche globale" : "§7ESC pour fermer";
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
        Gui.drawRect(ix, iy, ix + 5, iy + 1, ic);
        Gui.drawRect(ix, iy + 4, ix + 5, iy + 5, ic);
        Gui.drawRect(ix, iy, ix + 1, iy + 5, ic);
        Gui.drawRect(ix + 4, iy, ix + 5, iy + 5, ic);
        Gui.drawRect(ix + 3, iy + 4, ix + 5, iy + 7, ic); // handle

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

    private int getSectionColor(String name) {
        for (Section s : sections) if (s.name.equals(name)) return s.color;
        return UITheme.getPrimary();
    }

    @SuppressWarnings("unchecked")
    private void drawContent(int mx, int my) {
        // ── Search bar (drawn before scissor) ────────────────────────────────
        drawSearchBar(mx, my);

        int entryAreaTop = contentTop + SEARCH_BAR_H;
        int entryAreaH = contentH - SEARCH_BAR_H;

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

        int alpha = (int) (MathHelper.clamp_float(sectionFade, 0f, 1f) * 0xFF) << 24;
        int titleCol = (TEXT_PRIMARY & 0xFFFFFF) | alpha;
        int bodyCol = (TEXT_BODY & 0xFFFFFF) | alpha;

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
                    renderEntry(r.entry, y, titleCol, bodyCol, (idx % 2 == 0), getSectionColor(r.sectionName));
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
                    renderEntry(e, y, titleCol, bodyCol, (idx % 2 == 0), sections.get(currentSection).color);
                }
                y += eh + 8;
                idx++;
            }
        }

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);

        // ── Scrollbar ────────────────────────────────────────────────────────
        if (maxScroll > 0) {
            int sbW = 5;
            int sbX = panelX + panelW - sbW - 3;
            int sbTH = entryAreaH - 4;
            Gui.drawRect(sbX, entryAreaTop + 2, sbX + sbW, entryAreaTop + 2 + sbTH, 0x18FFFFFF);
            float ratio = scrollOffset / (float) maxScroll;
            float thumbR = (float) entryAreaH / (entryAreaH + maxScroll);
            int thumbH = Math.max(20, (int) (sbTH * thumbR));
            int thumbY = (int) ((sbTH - thumbH) * ratio);
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
    private void renderEntry(Entry e, int y, int titleCol, int bodyCol, boolean even, int accentColor) {
        int wrapW = contentW - 24;
        int eh = entryHeight(e);
        Gui.drawRect(contentX + 8, y + 1, panelX + panelW - 8, y + eh,
                even ? 0x06FFFFFF : 0x03FFFFFF);
        // Accent dot — couleur par section
        Gui.drawRect(contentX + 10, y + 5, contentX + 14, y + 11, accentColor);
        Gui.drawRect(contentX + 10, y + 11, contentX + 12, y + 13, (accentColor & 0xFFFFFF) | 0x44000000);
        // Titre : le "/" des commandes en couleur d'accent
        if (e.title.startsWith("/")) {
            int sw = fontRendererObj.getStringWidth("/");
            fontRendererObj.drawStringWithShadow("/", contentX + 20, y + 3, accentColor);
            fontRendererObj.drawStringWithShadow(e.title.substring(1), contentX + 20 + sw, y + 3, titleCol);
        } else {
            fontRendererObj.drawStringWithShadow(e.title, contentX + 20, y + 3, titleCol);
        }
        List<String> body = (List<String>) fontRendererObj.listFormattedStringToWidth(e.body, wrapW);
        int by = y + 15;
        for (int i2 = 0; i2 < body.size(); i2++) {
            fontRendererObj.drawString(body.get(i2), contentX + 20, by + i2 * 10, bodyCol);
        }
        int sepY = y + eh - 1;
        GuiRenderUtils.drawGradientRect(contentX + 14, sepY, contentX + contentW - 18, sepY + 1,
                (accentColor & 0xFFFFFF) | 0x22000000, 0x00000000);
        GuiRenderUtils.drawGradientRect(contentX + 14, sepY, contentX + contentW - 18, sepY + 1,
                0x00000000, (accentColor & 0xFFFFFF) | 0x22000000);
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
        int btnH = 24;
        int btnY = contentBottom + (footerH - btnH) / 2;
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
                        scrollTarget = 0;
                        scrollOffset = 0;
                        sectionFade = 0f;
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
        int entryAreaH = contentH - SEARCH_BAR_H;
        float ratio = (float) (my - entryAreaTop) / (float) entryAreaH;
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
                    currentSection--;
                    scrollTarget = 0;
                    scrollOffset = 0;
                    sectionFade = 0f;
                    recomputeMaxScroll();
                }
            } else if (keyCode == 208) { // down
                if (currentSection < sections.size() - 1) {
                    currentSection++;
                    scrollTarget = 0;
                    scrollOffset = 0;
                    sectionFade = 0f;
                    recomputeMaxScroll();
                }
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean inRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx < rx + rw && my < ry + rh;
    }
}
