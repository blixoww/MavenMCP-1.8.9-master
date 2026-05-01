package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.custompackets.data.FactionZoneTracker;
import net.minecraft.client.gui.FontRenderer;

/**
 * Widget HUD qui affiche le nom de la faction qui possède le chunk courant.
 *
 * <ul>
 *   <li>Wilderness (non réclamé)  → "Wilderness" vert foncé si option activée, sinon masqué</li>
 *   <li>Même faction (own)        → vert    #55FF55 (personnalisable)</li>
 *   <li>Allié  (ally)  / Trêve    → violet  #AA00AA (personnalisable)</li>
 *   <li>Ennemi (enemy)            → rouge   #FF5555 (personnalisable)</li>
 *   <li>Neutre (neutral)          → blanc   #FFFFFF (personnalisable)</li>
 *   <li>Mode rainbow              → couleur arc-en-ciel (surcharge toutes les couleurs ci-dessus)</li>
 * </ul>
 */
public class FactionZoneWidget extends BaseWidget {

    // ── Couleurs relation par défaut (utilisées si non personnalisées) ────────
    private static final int COLOR_OWN_DEF       = 0xFF55FF55; // vert
    private static final int COLOR_ALLY_DEF      = 0xFFAA00AA; // violet
    private static final int COLOR_ENEMY_DEF     = 0xFFFF5555; // rouge
    private static final int COLOR_NEUTRAL_DEF   = 0xFFFFFFFF; // blanc
    private static final int COLOR_WILDERNESS_DEF = 0xFF2E7D32; // vert foncé

    public FactionZoneWidget(String id, int x, int y) {
        super(id, x, y);
        this.width         = 100;
        this.height        = 10;
        this.defaultWidth  = 100;
        this.defaultHeight = 10;
    }

    @Override public boolean supportsLabelColor() { return true; }

    // ─────────────────────────────────────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        boolean editor         = UIManager.getInstance().isEditorActive();
        boolean claimed        = FactionZoneTracker.isClaimed();
        boolean showWilderness = Boolean.TRUE.equals(getPropOrDefault("showWilderness", Boolean.FALSE));
        // Masquer hors éditeur si pas de claim ET wilderness désactivé
        if (!claimed && !editor && !showWilderness) return;
        super.render(mouseX, mouseY, partialTicks);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        FontRenderer fr = mc.fontRendererObj;

        boolean editor         = UIManager.getInstance().isEditorActive();
        boolean showWilderness = Boolean.TRUE.equals(getPropOrDefault("showWilderness", Boolean.FALSE));
        String  name           = FactionZoneTracker.getFactionName();
        int     rel            = FactionZoneTracker.getRelation();
        String  ownFaction     = FactionZoneTracker.getOwnFactionName();

        boolean wilderness     = (name == null || name.isEmpty());

        if (wilderness) {
            if (editor) {
                // Prévisualisation en éditeur : faction propre ou fallback
                if (ownFaction != null && !ownFaction.isEmpty()) { name = ownFaction; rel = 0; }
                else if (showWilderness) { name = "Wilderness"; rel = 5; }
                else { name = "MaFaction"; rel = 0; }
            } else if (showWilderness) {
                name = "Wilderness"; rel = 5;
            } else {
                return; // masqué (normalement déjà bloqué dans render(), mais sécurité)
            }
        }

        if (name == null || name.isEmpty()) return;

        // ── Couleur : rainbow ou couleur de relation ───────────────────────
        int color;
        if (isRGBMode()) {
            color = getColor() | 0xFF000000;
        } else {
            switch (rel) {
                case 0:  color = getPropColor("colorOwn",        COLOR_OWN_DEF);       break; // même faction
                case 1:
                case 2:  color = getPropColor("colorAlly",       COLOR_ALLY_DEF);      break; // truce
                case 3:  color = getPropColor("colorEnemy",      COLOR_ENEMY_DEF);     break; // enemy
                case 5:  color = getPropColor("colorWilderness", COLOR_WILDERNESS_DEF);break; // wilderness
                default: color = getPropColor("colorNeutral",    COLOR_NEUTRAL_DEF);   break; // neutre / inconnu
            }
        }

        String text = "Zone: " + name;

        if (!Boolean.TRUE.equals(getPropOrDefault("customSize", false))) {
            this.width  = fr.getStringWidth(text) + 2;
            this.height = 9;
        }

        // "Zone: " en couleur label (ou couleur valeur si sync), nom en couleur relation
        if (isSyncColors()) {
            fr.drawStringWithShadow(text, 0, 0, color);
        } else {
            int lc = getLabelColor();
            fr.drawStringWithShadow("Zone: ", 0, 0, lc);
            fr.drawStringWithShadow(name, fr.getStringWidth("Zone: "), 0, color);
        }
    }

    /** Lit une couleur entière depuis les props, avec valeur par défaut. */
    private int getPropColor(String key, int def) {
        Object v = getProps().get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }
}
