package net.minecraft.client.gui.inventory;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.client.gui.ui.UITheme;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.*;

/**
 * GuiCraftGuide — Ultimate Alchemist & Crafter Edition v3
 * - Fix scissor clipping (items no longer bleed into search bar)
 * - Clean layout, no overflowing text
 * - Brew recipe cycling arrows
 * - Robust custom potion matching
 */
public class GuiCraftGuide extends GuiScreen {

    // ── Palette — thème sombre rouge, cohérent avec GuiWiki / GuiUISettings ──
    private static final int C_OVERLAY     = 0xA0000000;  // overlay neutre
    private static final int C_PANEL       = 0xFF0A0808;  // fond panneau (rouge sombre)
    private static final int C_PANEL_HDR   = 0xFF0E0606;  // header
    private static final int C_PANEL_INNER = 0xFF0C0808;  // inner bg
    private static final int C_RECIPE_BG   = 0xFF080404;  // fond recette
    private static final int C_ACCENT      = 0xFFE02828;  // rouge vif (accent principal)
    private static final int C_BREW        = 0xFF00BBEE;  // cyan — alchimie
    private static final int C_FURNACE     = 0xFFFF7822;  // orange — cuisson
    private static final int C_GOLD        = 0xFFE8A030;  // or — résultat craft
    private static final int C_BORDER_DIM  = 0xFF100606;  // bord sombre
    private static final int C_BORDER_MID  = 0xFF2A0C0C;  // bord moyen
    private static final int C_SLOT        = 0xFF0A0808;  // fond slot
    private static final int C_SLOT_LT     = 0xFF1A1010;  // clair slot
    private static final int C_SLOT_DK     = 0xFF040202;  // sombre slot
    private static final int C_SCR_TRACK   = 0xFF060404;  // piste scrollbar
    private static final int C_SCR_THUMB   = 0xFF2A0A0A;  // thumb inactif
    private static final int C_SCR_ACTIVE  = 0xFFE02828;  // thumb actif
    private static final int C_BTN         = 0xFFEAEAEA;
    private static final int C_BTN_HOV     = 0xFFFFFFFF;
    private static final int C_BTN_BDR     = 0xFFAAAAAA;
    private static final int C_TXT_TITLE   = 0xFFFFFFFF;
    private static final int C_TXT_DIM     = 0xFF445060;
    private static final int C_TXT_HINT    = 0xFF334040;
    private static final int C_TXT_BREW    = 0xFF44CCFF;
    private static final int C_GLOW_BREW   = 0x2200BBEE;
    private static final int C_GLOW_CRAFT  = 0x22E02828;  // rouge glow
    private static final int C_GLOW_FURN   = 0x22FF7822;

    // ── Grid / scrollbar constants ─────────────────────────────────────────────
    private int COLS = 9;
    private int SZ   = 18;
    private static final int SBW = 6;
    private static final int SBP = 4;

    // ── Layout fields (computed in initGui) ────────────────────────────────────
    private int pad, panelW, panelH, panelX, panelY;
    private int gridX, gridY, sbX, sbY, sbH, GRID_W;
    private int titleH, searchY, searchH, gridOff;
    private int btnW, btnH, footerY;
    private int visibleRows;

    // ── Animations ─────────────────────────────────────────────────────────────
    private float smoothScroll   = 0f;
    private int   targetScroll   = 0;
    private float searchFocusAnim = 0f;
    private long  recipeOpenTime  = 0;

    // ══════════════════════════════════════════════════════════════════════════
    //  Potion recipe system
    // ══════════════════════════════════════════════════════════════════════════
    static class PotionRecipe {
        int inputMeta, outputMeta;
        ItemStack ingredient;
        String name;
        PotionRecipe(int in, int out, ItemStack ingr, String n) {
            inputMeta = in; outputMeta = out; ingredient = ingr; name = n;
        }
    }

    private static final Map<Integer, List<PotionRecipe>> POTION_RECIPES       = new HashMap<>();
    static final int META_HASTE1       = 9001;
    static final int META_HASTE2       = 9002;
    static final int META_HASTE1_SPLASH= 9003;
    static final int META_HASTE2_SPLASH= 9004;
    static final int META_FALL1        = 9005;
    static final int META_FALL2        = 9006;
    static final int META_FALL1_SPLASH = 9007;
    static final int META_FALL2_SPLASH = 9008;
    static final Map<Integer, ItemStack> CUSTOM_POTION_STACKS      = new HashMap<>();
    static final Map<String,  Integer>   CUSTOM_POTION_SIGNATURES  = new HashMap<>();

    static {
        ItemStack glow     = new ItemStack(Items.glowstone_dust);
        ItemStack red      = new ItemStack(Items.redstone);
        ItemStack gun      = new ItemStack(Items.gunpowder);
        ItemStack ferm     = new ItemStack(Items.fermented_spider_eye);
        ItemStack feather  = new ItemStack(Items.feather);
        ItemStack diam     = new ItemStack(net.minecraft.init.Blocks.diamond_block);

        POTION_RECIPES.computeIfAbsent(16, k -> new ArrayList<>())
                .add(new PotionRecipe(0, 16, new ItemStack(Items.nether_wart), "Potion Etrange"));

        addPotionSet(8193, new ItemStack(Items.ghast_tear),    "Regeneration",        glow, red, gun);
        addPotionSet(8194, new ItemStack(Items.sugar),         "Rapidite",            glow, red, gun);
        addPotionSet(8195, new ItemStack(Items.magma_cream),   "Resistance au Feu",   null, red, gun);
        addPotionSet(8196, new ItemStack(Items.spider_eye),    "Poison",              glow, red, gun);
        addPotionSet(8197, new ItemStack(Items.speckled_melon),"Soin",                glow, null,gun);
        addPotionSet(8198, new ItemStack(Items.golden_carrot), "Vision Nocturne",     null, red, gun);
        addPotionSet(8201, new ItemStack(Items.blaze_powder),  "Force",               glow, red, gun);
        addPotionSet(8203, new ItemStack(Items.rabbit_foot),   "Saut",                glow, red, gun);
        addPotionSet(8205, new ItemStack(Items.fish,1,3),      "Respiration Aq.",     null, red, gun);

        POTION_RECIPES.computeIfAbsent(8200,  k->new ArrayList<>()).add(new PotionRecipe(0,    8200,  ferm,"Faiblesse"));
        POTION_RECIPES.computeIfAbsent(8264,  k->new ArrayList<>()).add(new PotionRecipe(8200, 8264,  red, "Faiblesse Etendue"));
        POTION_RECIPES.computeIfAbsent(16392, k->new ArrayList<>()).add(new PotionRecipe(8200, 16392, gun, "Faiblesse Jetable"));
        POTION_RECIPES.computeIfAbsent(16456, k->new ArrayList<>()).add(new PotionRecipe(8264, 16456, gun, "Faiblesse Etendue Jetable"));
        POTION_RECIPES.computeIfAbsent(8202,  k->new ArrayList<>()).add(new PotionRecipe(8194, 8202,  ferm,"Lenteur"));
        POTION_RECIPES.computeIfAbsent(8202,  k->new ArrayList<>()).add(new PotionRecipe(8203, 8202,  ferm,"Lenteur"));
        POTION_RECIPES.computeIfAbsent(8204,  k->new ArrayList<>()).add(new PotionRecipe(8197, 8204,  ferm,"Degats Instantanes"));
        POTION_RECIPES.computeIfAbsent(8206,  k->new ArrayList<>()).add(new PotionRecipe(8198, 8206,  ferm,"Invisibilite"));

        // Custom potions
        POTION_RECIPES.computeIfAbsent(META_HASTE1,       k->new ArrayList<>()).add(new PotionRecipe(16,          META_HASTE1,       diam,"Celerite"));
        POTION_RECIPES.computeIfAbsent(META_HASTE2,       k->new ArrayList<>()).add(new PotionRecipe(META_HASTE1, META_HASTE2,       glow,"Celerite II"));
        POTION_RECIPES.computeIfAbsent(META_HASTE1_SPLASH,k->new ArrayList<>()).add(new PotionRecipe(META_HASTE1, META_HASTE1_SPLASH,gun, "Celerite Jetable"));
        POTION_RECIPES.computeIfAbsent(META_HASTE2_SPLASH,k->new ArrayList<>()).add(new PotionRecipe(META_HASTE2, META_HASTE2_SPLASH,gun, "Celerite II Jetable"));
        POTION_RECIPES.computeIfAbsent(META_FALL1,        k->new ArrayList<>()).add(new PotionRecipe(16,          META_FALL1,        feather,"Fall Protection"));
        POTION_RECIPES.computeIfAbsent(META_FALL2,        k->new ArrayList<>()).add(new PotionRecipe(META_FALL1,  META_FALL2,        glow,   "Fall Protection II"));
        POTION_RECIPES.computeIfAbsent(META_FALL1_SPLASH, k->new ArrayList<>()).add(new PotionRecipe(META_FALL1,  META_FALL1_SPLASH, gun,    "Fall Protection Jetable"));
        POTION_RECIPES.computeIfAbsent(META_FALL2_SPLASH, k->new ArrayList<>()).add(new PotionRecipe(META_FALL2,  META_FALL2_SPLASH, gun,    "Fall Protection II Jetable"));

        CUSTOM_POTION_STACKS.put(META_HASTE1,       ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id,     0,3600,"Potion de Celerite"));
        CUSTOM_POTION_STACKS.put(META_HASTE2,       ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id,     1,1800,"Potion de Celerite II"));
        CUSTOM_POTION_STACKS.put(META_HASTE1_SPLASH,ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id,     0,3600,"Potion de Celerite Jetable",true));
        CUSTOM_POTION_STACKS.put(META_HASTE2_SPLASH,ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id,     1,1800,"Potion de Celerite II Jetable",true));
        CUSTOM_POTION_STACKS.put(META_FALL1,        ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id,0,3600,"Potion de Fall Protection"));
        CUSTOM_POTION_STACKS.put(META_FALL2,        ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id,1,1800,"Potion de Fall Protection II"));
        CUSTOM_POTION_STACKS.put(META_FALL1_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id,0,3600,"Potion de Fall Protection Jetable",true));
        CUSTOM_POTION_STACKS.put(META_FALL2_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id,1,1800,"Potion de Fall Protection II Jetable",true));

        for (Map.Entry<Integer, ItemStack> e : CUSTOM_POTION_STACKS.entrySet()) {
            String sig    = buildPotionSignature(e.getValue());
            String effSig = buildEffectSignature(e.getValue());
            if (!sig.isEmpty())    CUSTOM_POTION_SIGNATURES.put(sig, e.getKey());
            if (!effSig.isEmpty() && !CUSTOM_POTION_SIGNATURES.containsKey(effSig))
                CUSTOM_POTION_SIGNATURES.put(effSig, e.getKey());
        }
    }

    private static void addPotionSet(int base, ItemStack ing, String name,
                                     ItemStack glow, ItemStack redst, ItemStack gun) {
        POTION_RECIPES.computeIfAbsent(base,             k->new ArrayList<>()).add(new PotionRecipe(16,        base,             ing,  name));
        if (glow  != null) POTION_RECIPES.computeIfAbsent(base+32,           k->new ArrayList<>()).add(new PotionRecipe(base,   base+32,          glow, name+" II"));
        if (redst != null) POTION_RECIPES.computeIfAbsent(base+64,           k->new ArrayList<>()).add(new PotionRecipe(base,   base+64,          redst,name+" Etendue"));
        if (gun   != null) {
            POTION_RECIPES.computeIfAbsent(base+8192,    k->new ArrayList<>()).add(new PotionRecipe(base,      base+8192,        gun,  name+" Jetable"));
            if (glow  != null) POTION_RECIPES.computeIfAbsent(base+32+8192,  k->new ArrayList<>()).add(new PotionRecipe(base+32,base+32+8192,     gun,  name+" II Jetable"));
            if (redst != null) POTION_RECIPES.computeIfAbsent(base+64+8192,  k->new ArrayList<>()).add(new PotionRecipe(base+64,base+64+8192,    gun,  name+" Etendue Jetable"));
        }
    }

    // ── Blocked tile names ─────────────────────────────────────────────────────
    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "tile.litFurnace","tile.farmland","tile.doorWood","tile.doorIron",
            "tile.litRedstoneOre","tile.redstoneLamp.on","tile.pistonHead","tile.pistonMoving",
            "tile.cake","tile.skull","tile.carrots","tile.potatoes","tile.wheat",
            "tile.netherWart","tile.cocoa","tile.pumpkinStem","tile.melonStem",
            "tile.tripWire","tile.beetroots","tile.portal","tile.endPortal",
            "tile.endGateway","tile.fire","tile.water","tile.lava","tile.air"
    ));

    // ── GUI state ──────────────────────────────────────────────────────────────
    private GuiTextField searchBox;
    private final List<ItemStack> allItems     = new ArrayList<>();
    private final List<ItemStack> filtered     = new ArrayList<>();
    private final List<ItemStack> historyStack = new ArrayList<>();
    private boolean draggingSB = false;

    private enum Mode { LIST, CRAFT, BREWING, FURNACE }
    private Mode mode = Mode.LIST;
    private ItemStack selected;
    private ItemStack initialStack = null;
    private IRecipe craftRecipe;
    private ItemStack furnaceInput;
    private List<PotionRecipe> brewRecipesForItem = new ArrayList<>();
    private int brewRecipeIndex = 0;
    private PotionRecipe currentBrewRecipe = null;
    private GuiButton btnBack, btnClose;

    // Brewing nav button hit areas (set during draw, used in click)
    private int brewPrevBtnX, brewPrevBtnY, brewNextBtnX, brewNextBtnY;
    private static final int BREW_NAV_W = 18, BREW_NAV_H = 14;
    // Brewing slot geometry — kept in sync with drawBrewingRecipe
    // row0: ingredient at (cx-SZ/2, curY)
    // row1: input at (cx - row1W/2, curY+SZ+24), output at (input+SZ+32, same Y)
    private static final int BREW_ROW1_GAP = 24; // pixels between row0 bottom and row1 top
    private static final int BREW_SLOT_GAP = 32; // horizontal gap between input and output

    // ══════════════════════════════════════════════════════════════════════════
    //  Init
    // ══════════════════════════════════════════════════════════════════════════
    private final GuiScreen parent;
    public GuiCraftGuide(GuiScreen parent) { this.parent = parent; }
    public GuiCraftGuide(GuiScreen parent, ItemStack initial) { this(parent); this.initialStack = initial; }

    @Override
    public void initGui() {
        buttonList.clear();
        mode = Mode.LIST;
        selected = null;

        // Reset scroll state — after a resize the old scroll position is invalid
        // because COLS and visibleRows will change.
        targetScroll = 0;
        smoothScroll = 0f;

        SZ          = GuiRenderUtils.clamp(height / 18, 16, 22);
        COLS        = GuiRenderUtils.clamp((width - 80) / SZ, 6, 10);
        visibleRows = GuiRenderUtils.clamp((height - 160) / SZ, 4, 7);
        GRID_W      = COLS * SZ;
        pad         = Math.max(12, width / 60);
        titleH      = 26;
        searchH     = 18;
        searchY     = titleH + 6;
        gridOff     = searchY + searchH + 10;
        btnH        = 18;
        btnW        = Math.max(64, width / 10);

        panelW = pad + GRID_W + SBP + SBW + pad;
        panelH = gridOff + visibleRows * SZ + 14 + btnH + 22;
        panelX = (width  - panelW) / 2;
        panelY = (height - panelH) / 2;
        gridX  = panelX + pad;
        gridY  = panelY + gridOff;
        sbX    = gridX + GRID_W + SBP;
        sbY    = gridY;
        sbH    = visibleRows * SZ;
        footerY = gridY + sbH + 4;

        btnBack  = new GuiButton(1, panelX + pad,                      panelY + panelH - btnH - 10, btnW, btnH, "< Retour");
        btnClose = new GuiButton(2, panelX + panelW - pad - btnW,      panelY + panelH - btnH - 10, btnW, btnH, "Fermer");

        searchBox = new GuiTextField(0, fontRendererObj,
                panelX + pad + 18, panelY + searchY,
                panelW - pad * 2 - SBP - SBW - 18, searchH);
        searchBox.setMaxStringLength(30);
        searchBox.setFocused(true);

        if (allItems.isEmpty()) {
            for (Item item : Item.itemRegistry) {
                if (item == null || item == Items.potionitem) continue;
                List<ItemStack> sub = new ArrayList<>();
                item.getSubItems(item, null, sub);
                for (ItemStack s : sub) if (s != null && !isBlocked(s)) allItems.add(s);
            }
            Set<Integer> added = new HashSet<>();
            for (int meta : POTION_RECIPES.keySet())
                if (added.add(meta)) allItems.add(getPotionStack(meta));
        }
        filter("");
        if (initialStack != null) { loadRecipe(initialStack); initialStack = null; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Draw helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void drawSlot3D(int x, int y) {
        drawRect(x,       y,       x + SZ,     y + SZ,     C_SLOT);
        drawRect(x,       y,       x + SZ,     y + 1,      C_SLOT_LT);
        drawRect(x,       y,       x + 1,      y + SZ,     C_SLOT_LT);
        drawRect(x+SZ-1,  y,       x + SZ,     y + SZ,     C_SLOT_DK);
        drawRect(x,       y+SZ-1,  x + SZ,     y + SZ,     C_SLOT_DK);
    }

    private void drawSlotOutput(int x, int y, int borderColor) {
        drawSlot3D(x, y);
        int base = borderColor & 0x00FFFFFF;
        drawRect(x-2, y-2, x+SZ+2, y+SZ+2, 0x15000000 | base);
        drawRect(x,   y,   x+SZ,   y+1,    borderColor);
        drawRect(x,   y,   x+1,    y+SZ,   borderColor);
        drawRect(x+SZ-1, y, x+SZ,  y+SZ,   C_BORDER_DIM);
        drawRect(x, y+SZ-1, x+SZ,  y+SZ,   C_BORDER_DIM);
    }

    private void drawSlotHover(int x, int y) {
        long t = System.currentTimeMillis() % 1200;
        float p = t < 600 ? t / 600f : (1200f - t) / 600f;
        drawRect(x, y, x+SZ, y+SZ, ((int)(0x30 + 0x30 * p) << 24) | 0xE02828);
        drawRect(x, y, x+SZ, y+1,  ((int)(0x60 + 0x40 * p) << 24) | 0xE02828);
    }

    private void drawNavButton(int x, int y, int w, int h, String text, boolean hover, int accentCol) {
        float pulse = hover ? (float)(Math.sin(System.currentTimeMillis() / 350.0) * 0.5 + 0.5) : 0f;

        // Fond sombre rouge-teinté
        int bgTop    = hover ? 0xFF1A1010 : 0xFF110808;
        int bgBottom = hover ? 0xFF100808 : 0xFF0C0606;
        GuiRenderUtils.drawGradientRect(x, y, x + w, y + h, bgTop, bgBottom);

        // Reflet subtil en haut
        drawRect(x + 1, y + 1, x + w - 1, y + 2, hover ? 0x22FFFFFF : 0x10FFFFFF);

        // Contour coloré avec animation hover
        int borderAlpha = hover ? (int)(0x99 + 0x50 * pulse) : 0x44;
        int borderColor = (borderAlpha << 24) | (accentCol & 0x00FFFFFF);
        GuiRenderUtils.drawRectOutline(x, y, w, h, borderColor);

        // Ligne d'accent pleine en bas (soulignement — toujours visible, plus vif au survol)
        int lineAlpha = hover ? (int)(0xBB + 0x44 * pulse) : 0x55;
        int lineColor = (lineAlpha << 24) | (accentCol & 0x00FFFFFF);
        drawRect(x + 1, y + h - 1, x + w - 1, y + h, lineColor);

        // Halo externe léger au survol
        if (hover) {
            int glowAlpha = (int)(0x14 + 0x14 * pulse);
            GuiRenderUtils.drawRectOutline(x - 1, y - 1, w + 2, h + 2,
                    (glowAlpha << 24) | (accentCol & 0x00FFFFFF));
        }

        // Texte blanc, légèrement teinté de la couleur d'accent au survol
        int textColor = hover
                ? GuiRenderUtils.colorLerp(0xFFDDDDDD, 0xFFFFFFFF, 0.5f + 0.4f * pulse)
                : 0xFFAAAAAA;
        drawCenteredString(fontRendererObj, text, x + w / 2, y + (h - 8) / 2, textColor);
    }

    /** Compact coloured badge — no Unicode symbols that may not render */
    private void drawModeBadge(int x, int y, String label, int accent) {
        int tw = fontRendererObj.getStringWidth(label);
        int bw = tw + 8, bh = 13;
        drawRect(x,    y,    x+bw,   y+bh,   0x55000000 | (accent & 0xFFFFFF));
        drawRect(x,    y,    x+bw,   y+1,    accent);
        drawRect(x,    y,    x+1,    y+bh,   accent);
        fontRendererObj.drawStringWithShadow(label, x+4, y+3, accent);
    }

    /**
     * Enable OpenGL scissor test so nothing is drawn outside [x,y,w,h].
     * Must call {@link #disableScissor()} afterwards.
     * Uses ScaledResolution (same class Minecraft uses) so the scale factor
     * always matches the current GUI coords, even after a window resize.
     */
    private void enableScissor(int x, int y, int w, int h) {
        net.minecraft.client.Minecraft mc2 = net.minecraft.client.Minecraft.getMinecraft();
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc2);
        int s  = sr.getScaleFactor();
        int dh = mc2.displayHeight;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * s, dh - (y + h) * s, w * s, h * s);
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  drawScreen
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void drawScreen(int mx, int my, float pt) {
        smoothScroll    = GuiRenderUtils.lerp(smoothScroll, targetScroll, 0.25f);
        searchFocusAnim = GuiRenderUtils.lerp(searchFocusAnim, searchBox.isFocused() ? 1f : 0f, 0.15f);

        drawRect(0, 0, width, height, C_OVERLAY);

        if (mode == Mode.LIST) {
            GuiRenderUtils.drawRoundedPanel(panelX, panelY, panelW, panelH, C_PANEL, C_PANEL_HDR, titleH, C_ACCENT);
            drawRect(panelX+1, panelY+titleH+1, panelX+panelW-1, panelY+panelH-1, C_PANEL_INNER);

            // Title
            fontRendererObj.drawStringWithShadow("\u00a7lGuide de Craft",
                    panelX + pad, panelY + (titleH-8)/2, C_TXT_TITLE);
            String countStr = filtered.size() + " items";
            fontRendererObj.drawStringWithShadow("\u00a77" + countStr,
                    panelX + panelW - pad - fontRendererObj.getStringWidth(countStr),
                    panelY + (float) (titleH - 8) /2, C_TXT_DIM);

            drawSearchBox();
            drawItemGrid(mx, my);

            // ── Bouton "Retour" en bas à droite ──────────────────────────────
            int listBtnY = panelY + panelH - btnH - 10;
            boolean hRetour = inside(mx, my, panelX + panelW - pad - btnW, listBtnY, btnW, btnH);
            // Séparateur fin au-dessus du bouton
            drawRect(panelX + pad, listBtnY - 5, panelX + panelW - pad, listBtnY - 4, 0xFF0E0606);
            drawNavButton(panelX + panelW - pad - btnW, listBtnY, btnW, btnH, "Retour", hRetour, C_ACCENT);
        } else {
            drawRecipeMode(mx, my);
        }
        super.drawScreen(mx, my, pt);
    }

    // ── Search bar ─────────────────────────────────────────────────────────────
    private void drawSearchBox() {
        int sx = panelX + pad, sy = panelY + searchY, sw = panelW - pad*2 - SBP - SBW;
        drawRect(sx, sy, sx+sw, sy+searchH, 0xFF0A0808);
        int focusColor = GuiRenderUtils.colorLerp(C_BORDER_MID, C_ACCENT, searchFocusAnim);
        drawRect(sx,     sy+searchH-1, sx+sw, sy+searchH, focusColor);
        drawRect(sx,     sy,           sx+sw, sy+1,        0x10FFFFFF);
        GuiRenderUtils.drawSearchIcon(sx+4, sy+(searchH-10)/2+1,
                GuiRenderUtils.colorLerp(C_TXT_HINT, C_ACCENT, searchFocusAnim));
        if (searchBox.getText().isEmpty())
            fontRendererObj.drawStringWithShadow("\u00a7o Rechercher un item...",
                    sx+18, sy+(searchH-8)/2, C_TXT_HINT);
        searchBox.drawTextBox();
    }

    // ── Item grid ──────────────────────────────────────────────────────────────
    private void drawItemGrid(int mx, int my) {
        // Scrollbar track + thumb
        drawRect(sbX, sbY, sbX+SBW, sbY+sbH, C_SCR_TRACK);
        int ms = maxScroll();
        if (ms > 0) {
            int th = thumbH();
            int ty = sbY + (int)((smoothScroll / ms) * (sbH - th));
            drawRect(sbX+1, ty, sbX+SBW-1, ty+th,
                    (inside(mx,my,sbX,sbY,SBW,sbH) || draggingSB) ? C_SCR_ACTIVE : C_SCR_THUMB);
        }

        // ── SCISSOR: clip item rendering strictly inside the grid zone ──────
        // Extend by 1 GUI-pixel on each side to avoid sub-pixel rounding artifacts
        // at the edges (items appearing as 1-pixel slivers).
        enableScissor(gridX - 1, gridY - 1, GRID_W + 2, sbH + 2);

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();

        // Clamp smoothScroll to [0, maxScroll] to prevent negative pixelOffset
        // which would shift all items downward and leave a blank strip at the top.
        float clampedScroll = Math.max(0f, Math.min(smoothScroll, maxScroll()));
        int scrollRow   = (int) clampedScroll;
        int pixelOffset = (int)((clampedScroll - scrollRow) * SZ);
        ItemStack hov   = null;

        for (int row = 0; row <= visibleRows + 1; row++) {
            for (int col = 0; col < COLS; col++) {
                int i = scrollRow * COLS + row * COLS + col;
                if (i >= filtered.size()) break;
                int x = gridX + col * SZ;
                int y = gridY + row * SZ - pixelOffset;
                // Skip rows fully outside visible window (scissor handles the rest)
                if (y + SZ <= gridY || y >= gridY + sbH) continue;
                drawSlot3D(x, y);
                itemRender.renderItemAndEffectIntoGUI(filtered.get(i), x+1, y+1);
                if (inside(mx, my, x, y, SZ, SZ)) {
                    drawSlotHover(x, y);
                    hov = filtered.get(i);
                }
            }
        }

        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();

        disableScissor();
        // ── END SCISSOR ─────────────────────────────────────────────────────

        if (hov != null) renderToolTip(hov, mx, my);

    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Recipe view
    // ══════════════════════════════════════════════════════════════════════════
    private void drawRecipeMode(int mx, int my) {
        if (selected == null) return;
        int accent = modeAccent();

        // Panel height — give enough room for all content
        // Labels are BELOW slots (+10), so add that to content height
        // brewing: row0(SZ) + gap(BREW_ROW1_GAP) + row1(SZ) + name(12) + nav(14) = ~60
        int recipeContentH = (mode == Mode.BREWING) ? (SZ + BREW_ROW1_GAP + SZ + 26 + 14)
                           : (mode == Mode.CRAFT)   ? (3*SZ + 20)   // grid only, labels below
                           :                          (SZ   + 20);   // slot only, labels below
        // Sections: header(titleH) + sep+label(26) + recipe content + slot labels(10)
        //           + history hint(14) + separator(6) + buttons(btnH) + margins(14)
        int minH = titleH + 26 + recipeContentH + 10 + 14 + 6 + btnH + 14;
        int recPanelH = Math.max(panelH, minH);
        int recPanelY = (height - recPanelH) / 2;

        // Glow halo behind panel
        int glowCol = mode==Mode.BREWING ? C_GLOW_BREW : mode==Mode.FURNACE ? C_GLOW_FURN : C_GLOW_CRAFT;
        drawRect(panelX-6, recPanelY-6, panelX+panelW+6, recPanelY+recPanelH+6, glowCol);

        GuiRenderUtils.drawRoundedPanel(panelX, recPanelY, panelW, recPanelH, C_PANEL, C_PANEL_HDR, titleH, accent);
        drawRect(panelX+1, recPanelY+titleH+1, panelX+panelW-1, recPanelY+recPanelH-1, C_PANEL_INNER);

        // ── Header: item icon + name + mode badge ────────────────────────────
        int iconX = panelX + pad;
        int iconY = recPanelY + (titleH - 16) / 2;   // always 16px item
        RenderHelper.enableGUIStandardItemLighting(); GlStateManager.enableDepth();
        itemRender.renderItemAndEffectIntoGUI(selected, iconX, iconY);
        GlStateManager.disableDepth(); RenderHelper.disableStandardItemLighting();

        // Badge label (simple, no problematic Unicode)
        String badgeLabel = mode==Mode.BREWING ? "ALCHIMIE" : mode==Mode.FURNACE ? "CUISSON" : "CRAFT";
        int badgeW = fontRendererObj.getStringWidth(badgeLabel) + 8;
        int badgeX = panelX + panelW - pad - badgeW;
        int badgeY = recPanelY + (titleH - 13) / 2;

        // Item name — clamp so it never overlaps badge (leave 4px safety margin)
        int nameX = iconX + 18;
        // Available width: from name start to the left edge of the badge, with a small margin
        int availW = badgeX - nameX - 4;
        if (availW < 8) availW = 8;
        String rawName = selected.getDisplayName();
        String name = rawName;
        if (fontRendererObj.getStringWidth(name) > availW) {
            int ellWidth = fontRendererObj.getStringWidth("...");
            int trimW = Math.max(0, availW - ellWidth);
            name = fontRendererObj.trimStringToWidth(name, trimW) + "...";
        }
        // Draw name first, then badge on top — so the badge always covers any overflow
        fontRendererObj.drawStringWithShadow("\u00a7f\u00a7l" + name,
                nameX, recPanelY + (titleH - 8) / 2, 0xFFFFFFFF);

        // Draw badge AFTER the name so it renders on top and hides any text that bleeds in
        drawModeBadge(badgeX, badgeY, badgeLabel, accent);

        // ── Separator + section label ────────────────────────────────────────
        int sepY = recPanelY + titleH + 5;
        GuiRenderUtils.drawGradientRect(panelX+pad,      sepY, panelX+panelW/2,   sepY+1, 0x00000000, accent & 0x55FFFFFF);
        GuiRenderUtils.drawGradientRect(panelX+panelW/2, sepY, panelX+panelW-pad, sepY+1, accent & 0x55FFFFFF, 0x00000000);

        String typeLabel = mode==Mode.BREWING ? "Recette d'Alchimie"
                         : mode==Mode.FURNACE ? "Recette de Cuisson"
                         : "Recette de Craft";
        int cx   = panelX + panelW / 2;
        int curY = recPanelY + titleH + 16;
        drawCenteredString(fontRendererObj, "\u00a78" + typeLabel, cx, curY, C_TXT_DIM);
        curY += 14;

        // ── Recipe content ───────────────────────────────────────────────────
        ItemStack hovItem = null;
        RenderHelper.enableGUIStandardItemLighting(); GlStateManager.enableDepth();
        if      (mode == Mode.BREWING) hovItem = drawBrewingRecipe(mx, my, cx, curY + 8);
        else if (mode == Mode.CRAFT)   hovItem = drawCraftGrid(mx, my, cx, curY);
        else if (mode == Mode.FURNACE) hovItem = drawFurnaceRecipe(mx, my, cx, curY);
        GlStateManager.disableDepth(); RenderHelper.disableStandardItemLighting();

        if (hovItem != null) renderToolTip(hovItem, mx, my);

        // ── Bottom bar: back / close buttons + history hint ──────────────────
        int btnY = recPanelY + recPanelH - btnH - 10;

        // History hint — shown ABOVE the separator, never overlapping buttons
        if (!historyStack.isEmpty()) {
            String histTxt = "[ " + historyStack.size() + " precedent(s) — clic droit ]";
            int htw = fontRendererObj.getStringWidth(histTxt);
            // Clamp to panel width
            if (htw > panelW - pad*2)
                histTxt = fontRendererObj.trimStringToWidth(histTxt, panelW - pad*2 - 4) + "..";
            drawCenteredString(fontRendererObj, "\u00a78" + histTxt, cx, btnY - 14, C_TXT_DIM);
        }

        // Thin separator above buttons
        drawRect(panelX+pad, btnY-5, panelX+panelW-pad, btnY-4, 0xFF111320);

        btnBack.yPosition  = btnY; btnBack.xPosition  = panelX + pad;
        btnClose.yPosition = btnY; btnClose.xPosition = panelX + panelW - pad - btnW;
        drawNavButton(btnBack.xPosition,  btnBack.yPosition,  btnW, btnH, "< Retour",
                inside(mx,my,btnBack.xPosition,btnBack.yPosition,btnW,btnH), accent);
        drawNavButton(btnClose.xPosition, btnClose.yPosition, btnW, btnH, "Fermer",
                inside(mx,my,btnClose.xPosition,btnClose.yPosition,btnW,btnH), 0xFFE03040);
    }

    // ── Craft grid ─────────────────────────────────────────────────────────────
    private ItemStack drawCraftGrid(int mx, int my, int cx, int top) {
        ItemStack[] grid = getGrid(craftRecipe);
        int gw = 3 * SZ;
        // arrow zone = 26px, output slot = SZ
        int totalW = gw + 26 + SZ;
        int sx0    = cx - totalW / 2;
        ItemStack hov = null;

        int bgBottom = top + 3*SZ + 4;
        drawRect(sx0-4, top-2, sx0+totalW+4, bgBottom, C_RECIPE_BG);
        GuiRenderUtils.drawRectOutline(sx0-4, top-2, totalW+8, 3*SZ+6, 0x15FFFFFF);

        // 3x3 grid
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int i  = r*3 + c;
                int sx = sx0 + c*SZ, sy = top + r*SZ;
                drawSlot3D(sx, sy);
                if (grid[i] != null) {
                    itemRender.renderItemAndEffectIntoGUI(grid[i], sx+1, sy+1);
                    if (inside(mx,my,sx,sy,SZ,SZ)) { drawSlotHover(sx, sy); hov = grid[i]; }
                }
            }
        }

        // Animated arrow centred vertically on the grid
        int arrowCX  = sx0 + gw + 13;
        int arrowCY  = top + SZ + SZ/2;
        GuiRenderUtils.drawChevronArrow(arrowCX - 6, arrowCY, 6, 0xE8A030,
                (System.currentTimeMillis() % 1000) / 1000f);

        // Output slot — vertically centred on row 1 (middle row)
        int rx = sx0 + gw + 26, ry = top + SZ;
        drawSlotOutput(rx, ry, C_GOLD);
        ItemStack out = craftRecipe.getRecipeOutput();
        itemRender.renderItemAndEffectIntoGUI(out, rx+1, ry+1);
        if (inside(mx,my,rx,ry,SZ,SZ)) { drawSlotHover(rx,ry); hov = out; }

        // Quantity badge: moved below the output slot (centered)
        if (out.stackSize > 1) {
            String qty = "x" + out.stackSize;
            int qx = rx + SZ / 2 - fontRendererObj.getStringWidth(qty) / 2;
            int qy = ry + SZ + 2; // a bit below the slot
            fontRendererObj.drawStringWithShadow("\u00a7e" + qty, qx, qy, C_GOLD);
        }

        // Labels BELOW the recipe area — safe, cannot overlap anything above
        int labelY = bgBottom + 3;
        drawCenteredString(fontRendererObj, "\u00a78Ingredients", sx0 + gw/2,       labelY, C_TXT_DIM);
        drawCenteredString(fontRendererObj, "\u00a78Resultat",    rx + SZ/2,        labelY, C_TXT_DIM);

        return hov;
    }

    // ── Furnace recipe ─────────────────────────────────────────────────────────
    private ItemStack drawFurnaceRecipe(int mx, int my, int cx, int top) {
        int arrowGap = 28;
        int totalW = SZ + arrowGap + SZ;
        int lx     = cx - totalW / 2;
        ItemStack hov = null;

        int bgBottom = top + SZ + 4;
        drawRect(lx-4, top-2, lx+totalW+4, bgBottom, C_RECIPE_BG);
        GuiRenderUtils.drawRectOutline(lx-4, top-2, totalW+8, SZ+6, 0x15FFFFFF);

        // Input slot
        ItemStack input = fixWildcard(furnaceInput);
        drawSlot3D(lx, top);
        itemRender.renderItemAndEffectIntoGUI(input, lx+1, top+1);
        if (inside(mx,my,lx,top,SZ,SZ)) { drawSlotHover(lx,top); hov = input; }

        // Animated arrow in the gap, vertically centred
        float anim = (System.currentTimeMillis() % 1000) / 1000f;
        GuiRenderUtils.drawChevronArrow(lx + SZ + arrowGap/2 - 6, top + SZ/2, 6, 0xFF7822, anim);

        // Output slot
        int rx = lx + SZ + arrowGap;
        drawSlotOutput(rx, top, C_FURNACE);
        itemRender.renderItemAndEffectIntoGUI(selected, rx+1, top+1);
        if (inside(mx,my,rx,top,SZ,SZ)) { drawSlotHover(rx,top); hov = selected; }

        // Labels BELOW the slots — no overlap risk
        int labelY = bgBottom + 3;
        drawCenteredString(fontRendererObj, "\u00a78Entree",  lx + SZ/2,  labelY, C_TXT_DIM);
        drawCenteredString(fontRendererObj, "\u00a78Sortie",  rx + SZ/2,  labelY, C_TXT_DIM);

        return hov;
    }

    // ── Brewing recipe ─────────────────────────────────────────────────────────
    private ItemStack drawBrewingRecipe(int mx, int my, int cx, int top) {
        if (currentBrewRecipe == null) return null;
        ItemStack hov = null;

        // Layout (all Y relative to top):
        //   row0: ingredient slot   (top + 0)
        //   label "Ingredient"      (top + SZ + 2)
        //   gap / line              (top + SZ + 12 .. top + SZ + 22)
        //   row1: input + output    (top + SZ + 24)
        //   label "Base" / "Resultat"  (top + 2*SZ + 26)
        //   recipe name             (top + 2*SZ + 36)
        //   nav buttons (if >1)     (top + 2*SZ + 48)

        int row1Y = top + SZ + 24;   // Y of input/output slots
        int slotGap = 32;             // horizontal gap between input and output slots

        // Total width of row1: SZ + slotGap + SZ
        int row1W = SZ + slotGap + SZ;
        int ingX = cx - SZ/2;         // ingredient centred
        int inX  = cx - row1W/2;      // input left
        int outX = inX + SZ + slotGap;// output right

        // Background
        int bgTop    = top - 2;
        int bgBottom = row1Y + SZ + 4;
        int bgLeft   = Math.min(ingX, inX) - 6;
        int bgRight  = Math.max(ingX + SZ, outX + SZ) + 6;
        drawRect(bgLeft, bgTop, bgRight, bgBottom, C_RECIPE_BG);
        GuiRenderUtils.drawRectOutline(bgLeft, bgTop, bgRight - bgLeft, bgBottom - bgTop, 0x15FFFFFF);

        // Animated connecting lines
        long time  = System.currentTimeMillis();
        float wave = (float)(Math.sin(time / 400.0) * 0.4 + 0.6);
        int lineCol = GuiRenderUtils.colorLerp(0x22FFFFFF, C_BREW, wave * 0.35f);
        int juncY   = row1Y - 4;   // junction just above input/output slots

        // vertical from ingredient down to junction
        drawRect(cx - 1, top + SZ + 2, cx + 1, juncY, lineCol);
        // horizontal junction line
        drawRect(inX + SZ/2, juncY - 1, outX + SZ/2, juncY + 1, lineCol);
        // drops to each slot
        drawRect(inX  + SZ/2 - 1, juncY, inX  + SZ/2 + 1, row1Y, lineCol);
        drawRect(outX + SZ/2 - 1, juncY, outX + SZ/2 + 1, row1Y, lineCol);

        // Animated particle flowing down
        int maxDrop = Math.max(1, juncY - (top + SZ + 2));
        // Use two staggered particles with a longer period for a smooth, slow flow
        int period = 1800; // ms for a full descent (increase -> slower)
        float prog1 = (time % period) / (float) period; // 0..1
        float prog2 = ((time + period / 2) % period) / (float) period; // offset by half period

        // easing (soft start/stop) - simple ease-in-out
        float ease1 = (float) (0.5f - 0.5f * Math.cos(prog1 * Math.PI));
        float ease2 = (float) (0.5f - 0.5f * Math.cos(prog2 * Math.PI));

        int pY1 = top + SZ + 2 + (int) (ease1 * maxDrop);
        int pY2 = top + SZ + 2 + (int) (ease2 * maxDrop);

        // particle sizes and alpha vary with progress (fade in/out)
        int a1 = 0x60 + (int) (0x60 * (1f - Math.abs(0.5f - prog1) * 2f));
        int a2 = 0x40 + (int) (0x50 * (1f - Math.abs(0.5f - prog2) * 2f));
        int colBase = C_BREW & 0x00FFFFFF;
        int col1 = (a1 << 24) | colBase;
        int col2 = (a2 << 24) | colBase;

        // draw a small square + subtle trail for each particle
        int size1 = 3 + (int) (1f * (ease1));
        int size2 = 2 + (int) (1f * (ease2));
        int half1 = size1 / 2, half2 = size2 / 2;

        drawRect(cx - half1, pY1, cx + half1 + 1, pY1 + size1, col1);
        // faint trailing line beneath
        drawRect(cx - 1, pY1 + size1, cx + 1, pY1 + size1 + 2, (a1/2 << 24) | colBase);

        drawRect(cx - half2, pY2, cx + half2 + 1, pY2 + size2, col2);
        drawRect(cx - 1, pY2 + size2, cx + 1, pY2 + size2 + 1, (a2/2 << 24) | colBase);

        // ── Ingredient slot (top, centred) with label above
        // Label above the ingredient (so it appears under the global title and separator)
        drawCenteredString(fontRendererObj, "\u00a7bIngredient", ingX + SZ/2, top - 10, C_TXT_BREW);
        drawSlot3D(ingX, top);
        itemRender.renderItemAndEffectIntoGUI(currentBrewRecipe.ingredient, ingX+1, top+1);
        if (inside(mx,my,ingX,top,SZ,SZ)) { hov = currentBrewRecipe.ingredient; drawSlotHover(ingX,top); }

        // ── Arrow "->" between input and output ──────────────────────────────
        int arrTextX = inX + SZ + (slotGap - fontRendererObj.getStringWidth("->")) / 2;
        fontRendererObj.drawStringWithShadow("->", arrTextX, row1Y + SZ/2 - 4, C_BREW);

        // ── Input (base) slot ────────────────────────────────────────────────
        drawSlot3D(inX, row1Y);
        ItemStack inS = getPotionStack(currentBrewRecipe.inputMeta);
        itemRender.renderItemAndEffectIntoGUI(inS, inX+1, row1Y+1);
        if (inside(mx,my,inX,row1Y,SZ,SZ)) { hov = inS; drawSlotHover(inX,row1Y); }
        // label below
        drawCenteredString(fontRendererObj, "\u00a78Base", inX + SZ/2, row1Y + SZ + 2, C_TXT_DIM);

        // ── Output slot ──────────────────────────────────────────────────────
        drawSlotOutput(outX, row1Y, C_BREW);
        ItemStack outS = getPotionStack(currentBrewRecipe.outputMeta);
        itemRender.renderItemAndEffectIntoGUI(outS, outX+1, row1Y+1);
        if (inside(mx,my,outX,row1Y,SZ,SZ)) { hov = outS; drawSlotHover(outX,row1Y); }
        // label below
        drawCenteredString(fontRendererObj, "\u00a7bResultat", outX + SZ/2, row1Y + SZ + 2, C_BREW);

        // ── Multi-recipe navigation ──────────────────────────────────────────
        if (brewRecipesForItem.size() > 1) {
            String navInfo = "(" + (brewRecipeIndex+1) + "/" + brewRecipesForItem.size() + ")";
            int navInfoW = fontRendererObj.getStringWidth(navInfo);
            int navY     = row1Y + 12;

            brewPrevBtnX = cx - navInfoW/2 - BREW_NAV_W - 4;
            brewPrevBtnY = navY;
            boolean hPrev = inside(mx, my, brewPrevBtnX, brewPrevBtnY, BREW_NAV_W, BREW_NAV_H);
            drawNavButton(brewPrevBtnX, brewPrevBtnY, BREW_NAV_W, BREW_NAV_H, "<", hPrev, C_BREW);

            fontRendererObj.drawStringWithShadow("\u00a77" + navInfo,
                    cx - navInfoW/2, navY + (BREW_NAV_H - 8)/2, C_TXT_DIM);

            brewNextBtnX = cx + navInfoW/2 + 4;
            brewNextBtnY = navY;
            boolean hNext = inside(mx, my, brewNextBtnX, brewNextBtnY, BREW_NAV_W, BREW_NAV_H);
            drawNavButton(brewNextBtnX, brewNextBtnY, BREW_NAV_W, BREW_NAV_H, ">", hNext, C_BREW);
        }

        return hov;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Logic helpers
    // ══════════════════════════════════════════════════════════════════════════

    private ItemStack getPotionStack(int meta) {
        if (CUSTOM_POTION_STACKS.containsKey(meta)) return CUSTOM_POTION_STACKS.get(meta).copy();
        return new ItemStack(Items.potionitem, 1, meta);
    }

    private int modeAccent() {
        return mode == Mode.BREWING ? C_BREW : mode == Mode.FURNACE ? C_FURNACE : C_ACCENT;
    }

    private boolean isBlocked(ItemStack s) {
        if (s == null || s.getItem() == null) return true;
        String u = s.getUnlocalizedName();
        for (String b : BLOCKED) if (u != null && u.startsWith(b)) return true;
        return false;
    }

    private void filter(String q) {
        filtered.clear();
        String lq = q.trim().toLowerCase();
        for (ItemStack s : allItems
        ) if (lq.isEmpty() || s.getDisplayName().toLowerCase().contains(lq)) filtered.add(s);
        targetScroll = 0;
        smoothScroll = 0;
    }

    private int maxScroll() {
        return Math.max(0, (filtered.size() + COLS - 1) / COLS - visibleRows);
    }

    private int thumbH() {
        int total = (filtered.size() + COLS - 1) / COLS;
        return total <= visibleRows ? sbH : Math.max(16, sbH * visibleRows / total);
    }

    private void applyScrollFromMouse(int my) {
        int th = thumbH(), ms = maxScroll();
        if (ms == 0) return;
        targetScroll = Math.max(0, Math.min(ms, Math.round((float) (my - sbY - th / 2) * ms / (sbH - th))));
    }

    private boolean isSameStack(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;
        if (a.getItem() == Items.potionitem) {
            int ma = resolveCustomPotionMeta(a), mb = resolveCustomPotionMeta(b);
            if (ma != -1 || mb != -1) return ma == mb;
            int da = a.getItemDamage(), db = b.getItemDamage();
            int mask = 15 | 32 | 64 | 16384;
            return (da & mask) == (db & mask);
        }
        return a.getItemDamage() == b.getItemDamage();
    }

    private void openRecipe(ItemStack stack) {
        if (stack == null) return;
        if (selected != null && isSameStack(selected, stack)) return;
        ItemStack prev = selected;
        if (loadRecipe(stack)) {
            if (prev != null && (historyStack.isEmpty() || !isSameStack(historyStack.get(historyStack.size()-1), prev)))
                historyStack.add(prev);
        }
    }

    private boolean loadRecipe(ItemStack stack) {
        if (stack == null) return false;
        Mode       oldMode  = mode;           ItemStack      oldSel    = selected;
        IRecipe    oldCraft = craftRecipe;    ItemStack      oldFurn   = furnaceInput;
        List<PotionRecipe> oldBrew = brewRecipesForItem;
        PotionRecipe oldCurBrew = currentBrewRecipe;
        int        oldIdx   = brewRecipeIndex;

        selected = stack; craftRecipe = null; furnaceInput = null;
        brewRecipesForItem = new ArrayList<>(); mode = Mode.LIST;

        if (stack.getItem() == Items.potionitem) {
            int meta = resolveCustomPotionMeta(stack);
            if (meta == -1) meta = stack.getItemDamage();
            if (POTION_RECIPES.containsKey(meta)) {
                brewRecipesForItem.addAll(POTION_RECIPES.get(meta));
            } else {
                for (Map.Entry<Integer, List<PotionRecipe>> e : POTION_RECIPES.entrySet()) {
                    if (isSameStack(getPotionStack(e.getKey()), stack)) {
                        brewRecipesForItem.addAll(e.getValue()); break;
                    }
                }
            }
            if (!brewRecipesForItem.isEmpty()) {
                brewRecipesForItem.sort(Comparator.comparingInt(a -> a.inputMeta));
                currentBrewRecipe = brewRecipesForItem.get(0);
                mode = Mode.BREWING;
            }
        }
        if (mode == Mode.LIST) {
            int tm = stack.getItemDamage();
            for (IRecipe r : CraftingManager.getInstance().getRecipeList()) {
                ItemStack out = r.getRecipeOutput();
                if (out != null && out.getItem() == stack.getItem()
                        && (out.getItemDamage() == 32767 || out.getItemDamage() == tm)) {
                    craftRecipe = r; mode = Mode.CRAFT; break;
                }
            }
        }
        if (mode == Mode.LIST) {
            int tm = stack.getItemDamage();
            for (Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
                ItemStack out = e.getValue();
                if (out != null && out.getItem() == stack.getItem()
                        && (out.getItemDamage() == 32767 || out.getItemDamage() == tm)) {
                    furnaceInput = e.getKey(); mode = Mode.FURNACE; break;
                }
            }
        }
        if (mode != Mode.LIST) { recipeOpenTime = System.currentTimeMillis(); brewRecipeIndex = 0; return true; }
        // Rollback
        mode = oldMode; selected = oldSel; craftRecipe = oldCraft; furnaceInput = oldFurn;
        brewRecipesForItem = oldBrew; currentBrewRecipe = oldCurBrew; brewRecipeIndex = oldIdx;
        return false;
    }

    private void goBack() {
        if (!historyStack.isEmpty()) loadRecipe(historyStack.remove(historyStack.size()-1));
        else goMenu();
    }
    private void goMenu() { mode = Mode.LIST; selected = null; historyStack.clear(); }

    // ── Potion resolution ──────────────────────────────────────────────────────

    private int resolveCustomPotionMeta(ItemStack stack) {
        if (stack == null || stack.getItem() != Items.potionitem) return -1;
        String sig    = buildPotionSignature(stack);
        if (CUSTOM_POTION_SIGNATURES.containsKey(sig))    return CUSTOM_POTION_SIGNATURES.get(sig);
        String effSig = buildEffectSignature(stack);
        if (CUSTOM_POTION_SIGNATURES.containsKey(effSig)) return CUSTOM_POTION_SIGNATURES.get(effSig);
        try {
            ItemPotion ip      = (ItemPotion) Items.potionitem;
            List<PotionEffect> eff = ip.getEffects(stack);
            if (eff == null || eff.isEmpty()) return -1;
            boolean splash = (stack.getItemDamage() & 16384) != 0 || ItemPotion.isSplash(stack.getItemDamage());
            if (!splash && stack.hasTagCompound()) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag.hasKey("CustomPotionEffects"))
                    splash = (stack.getItemDamage() & 16384) != 0;
            }
            for (Map.Entry<Integer, ItemStack> e : CUSTOM_POTION_STACKS.entrySet()) {
                ItemStack c = e.getValue();
                boolean cs  = ItemPotion.isSplash(c.getItemDamage()) || (c.getItemDamage() & 16384) != 0;
                if (cs != splash) continue;
                List<PotionEffect> ce = ip.getEffects(c);
                if (ce != null && effectsMatch(eff, ce)) return e.getKey();
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static boolean effectsMatch(List<PotionEffect> a, List<PotionEffect> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).getPotionID() != b.get(i).getPotionID()
             || a.get(i).getAmplifier() != b.get(i).getAmplifier()) return false;
        }
        return true;
    }

    private static String buildPotionSignature(ItemStack stack) {
        if (stack == null) return "";
        try {
            ItemPotion ip = (ItemPotion) Items.potionitem;
            List<PotionEffect> eff = ip.getEffects(stack);
            List<String> parts = new ArrayList<>();
            if (eff != null) { for (PotionEffect p : eff) parts.add(p.getPotionID()+":"+p.getAmplifier()); Collections.sort(parts); }
            boolean splash = (stack.getItemDamage() & 16384) != 0 || ItemPotion.isSplash(stack.getItemDamage());
            return String.join(",", parts) + "|" + (splash?"splash":"normal") + "|"
                    + stack.getDisplayName().replaceAll("\u00A7.", "").toLowerCase(Locale.ROOT);
        } catch (Exception ex) { return ""; }
    }

    private static String buildEffectSignature(ItemStack stack) {
        if (stack == null) return "";
        try {
            ItemPotion ip = (ItemPotion) Items.potionitem;
            List<PotionEffect> eff = ip.getEffects(stack);
            List<String> parts = new ArrayList<>();
            if (eff != null) { for (PotionEffect p : eff) parts.add(p.getPotionID()+":"+p.getAmplifier()); Collections.sort(parts); }
            boolean splash = (stack.getItemDamage() & 16384) != 0 || ItemPotion.isSplash(stack.getItemDamage());
            return String.join(",", parts) + "|" + (splash?"splash":"normal");
        } catch (Exception ex) { return ""; }
    }

    // ── Misc ───────────────────────────────────────────────────────────────────

    private ItemStack fixWildcard(ItemStack s) {
        if (s == null) return null; ItemStack c = s.copy(); if (c.getItemDamage()==32767) c.setItemDamage(0); return c;
    }

    private ItemStack[] getGrid(IRecipe r) {
        ItemStack[] g = new ItemStack[9];
        if (r instanceof ShapedRecipes) {
            ShapedRecipes sr = (ShapedRecipes) r;
            int rw = sr.getRecipeWidth(), rh = sr.getRecipeHeight();
            ItemStack[] items = sr.getRecipeItems();
            for (int row = 0; row < rh; row++)
                for (int col = 0; col < rw; col++)
                    g[row*3+col] = fixWildcard(items[row*rw+col]);
        } else if (r instanceof ShapelessRecipes) {
            List<ItemStack> items = ((ShapelessRecipes) r).getRecipeItems();
            for (int i = 0; i < items.size() && i < 9; i++) g[i] = fixWildcard(items.get(i));
        }
        return g;
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x+w && my >= y && my < y+h;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Input handlers
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn == 1) { goBack(); return; }

        searchBox.mouseClicked(mx, my, btn);

        if (mode != Mode.LIST) {
            if (inside(mx,my,btnBack.xPosition, btnBack.yPosition, btnW,btnH)) { goBack(); return; }
            if (inside(mx,my,btnClose.xPosition,btnClose.yPosition,btnW,btnH)) { mc.displayGuiScreen(parent); return; }

            if (btn == 0) {
                // Brew nav arrows
                if (mode == Mode.BREWING && brewRecipesForItem.size() > 1) {
                    if (inside(mx,my,brewPrevBtnX,brewPrevBtnY,BREW_NAV_W,BREW_NAV_H)) {
                        brewRecipeIndex = (brewRecipeIndex - 1 + brewRecipesForItem.size()) % brewRecipesForItem.size();
                        currentBrewRecipe = brewRecipesForItem.get(brewRecipeIndex); return;
                    }
                    if (inside(mx,my,brewNextBtnX,brewNextBtnY,BREW_NAV_W,BREW_NAV_H)) {
                        brewRecipeIndex = (brewRecipeIndex + 1) % brewRecipesForItem.size();
                        currentBrewRecipe = brewRecipesForItem.get(brewRecipeIndex); return;
                    }
                }

                // Compute curY exactly as drawRecipeMode does
                int recipeContentH = mode==Mode.BREWING ? (SZ + BREW_ROW1_GAP + SZ + 26 + 14)
                                   : mode==Mode.CRAFT   ? (3*SZ + 20)
                                   :                      (SZ   + 20);
                int minH      = titleH + 26 + recipeContentH + 10 + 14 + 6 + btnH + 14;
                int recPanelH = Math.max(panelH, minH);
                int recPanelY = (height - recPanelH) / 2;
                int cx        = panelX + panelW / 2;
                // curY after "Recette d'Alchimie" label (same as drawRecipeMode)
                int curY      = recPanelY + titleH + 16 + 14;

                if (mode == Mode.BREWING && currentBrewRecipe != null) {
                    // drawBrewingRecipe is called with top = curY + 8
                    int brewTop = curY + 8;
                    int ingX    = cx - SZ/2;
                    int row1W   = SZ + BREW_SLOT_GAP + SZ;
                    int row1Y   = brewTop + SZ + BREW_ROW1_GAP;
                    int inX     = cx - row1W/2;
                    int outX    = inX + SZ + BREW_SLOT_GAP;
                    if (inside(mx,my,ingX,brewTop,SZ,SZ))  openRecipe(currentBrewRecipe.ingredient);
                    else if (inside(mx,my,inX,row1Y,SZ,SZ))  openRecipe(getPotionStack(currentBrewRecipe.inputMeta));
                    else if (inside(mx,my,outX,row1Y,SZ,SZ)) openRecipe(getPotionStack(currentBrewRecipe.outputMeta));
                } else if (mode == Mode.CRAFT && craftRecipe != null) {
                    int gw    = 3*SZ;
                    int totalW= gw + 26 + SZ;
                    int sx0   = cx - totalW/2;
                    ItemStack[] grid = getGrid(craftRecipe);
                    for (int r = 0; r < 3; r++)
                        for (int c = 0; c < 3; c++)
                            if (grid[r*3+c] != null && inside(mx,my,sx0+c*SZ,curY+r*SZ,SZ,SZ))
                                openRecipe(grid[r*3+c]);
                    // output slot at middle row
                    if (inside(mx,my, sx0+gw+26, curY+SZ, SZ,SZ)) openRecipe(craftRecipe.getRecipeOutput());
                } else if (mode == Mode.FURNACE && furnaceInput != null) {
                    int arrowGap = 28;
                    int totalW   = SZ + arrowGap + SZ;
                    int lx       = cx - totalW/2;
                    int rx       = lx + SZ + arrowGap;
                    if (inside(mx,my,lx,curY,SZ,SZ)) openRecipe(furnaceInput);
                    if (inside(mx,my,rx,curY,SZ,SZ)) openRecipe(selected);
                }
            }
            return;
        }

        if (btn == 0) {
            // Bouton "Retour" en mode liste
            int listBtnY = panelY + panelH - btnH - 10;
            if (inside(mx, my, panelX + panelW - pad - btnW, listBtnY, btnW, btnH)) {
                mc.displayGuiScreen(parent);
                return;
            }
            if (inside(mx,my,sbX,sbY,SBW,sbH)) { draggingSB = true; applyScrollFromMouse(my); return; }
            int scrollRow   = (int) smoothScroll;
            int pixelOffset = (int)((smoothScroll - scrollRow) * SZ);
            for (int r = 0; r <= visibleRows; r++) {
                for (int c = 0; c < COLS; c++) {
                    int i = scrollRow*COLS + r*COLS + c;
                    if (i >= filtered.size()) return;
                    int x = gridX + c*SZ, y = gridY + r*SZ - pixelOffset;
                    if (y+SZ > gridY && y < gridY+sbH && inside(mx,my,x,y,SZ,SZ)) { openRecipe(filtered.get(i)); return; }
                }
            }
        }
    }

    @Override protected void mouseReleased(int mx, int my, int state) { super.mouseReleased(mx,my,state); draggingSB = false; }
    @Override protected void mouseClickMove(int mx, int my, int btn, long t) { super.mouseClickMove(mx,my,btn,t); if (draggingSB && mode==Mode.LIST) applyScrollFromMouse(my); }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int w = Mouse.getEventDWheel(), ms = maxScroll();
        if (w < 0 && targetScroll < ms) targetScroll++;
        if (w > 0 && targetScroll > 0)  targetScroll--;
    }

    @Override
    protected void keyTyped(char ch, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            if (mode != Mode.LIST) goBack(); else mc.displayGuiScreen(parent);
        } else if (key == Keyboard.KEY_BACK) {
            if (mode != Mode.LIST) goBack();
            else if (searchBox.textboxKeyTyped(ch, key)) filter(searchBox.getText());
        } else if (key == Keyboard.KEY_LEFT) {
            if (mode == Mode.BREWING && brewRecipesForItem.size() > 1) {
                brewRecipeIndex = (brewRecipeIndex-1+brewRecipesForItem.size()) % brewRecipesForItem.size();
                currentBrewRecipe = brewRecipesForItem.get(brewRecipeIndex);
            } else if (mode != Mode.LIST) goBack();
        } else if (key == Keyboard.KEY_RIGHT) {
            if (mode == Mode.BREWING && brewRecipesForItem.size() > 1) {
                brewRecipeIndex = (brewRecipeIndex+1) % brewRecipesForItem.size();
                currentBrewRecipe = brewRecipesForItem.get(brewRecipeIndex);
            }
        } else if (searchBox.textboxKeyTyped(ch, key)) {
            filter(searchBox.getText());
        } else {
            super.keyTyped(ch, key);
        }
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        if (btn.id == 1) goBack();
        else if (btn.id == 2) mc.displayGuiScreen(parent);
    }
}
