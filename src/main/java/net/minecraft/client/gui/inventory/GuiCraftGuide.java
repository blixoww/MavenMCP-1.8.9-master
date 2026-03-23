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
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

/**
 * GuiCraftGuide — Lunar Client-inspired professional redesign
 * PvP Faction | MCP 1.8.9
 * <p>
 * Visual style:
 * · Ultra-dark panels with multi-layer shadow & gradient headers
 * · Electric-blue accent system (#3D8EFF) with mode-tinted variants
 * · 3-D inset slots with smooth hover pulse
 * · Smooth scrolling with momentum
 * · Search bar with icon & focus animation
 * · Animated chevron arrows in recipe views
 * · Fully screen-adaptive sizing (works at any resolution)
 */
public class GuiCraftGuide extends GuiScreen {

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final int C_OVERLAY = 0xCC030511;
    private static final int C_PANEL = 0xFF0B0C17;
    private static final int C_PANEL_HDR = 0xFF080A14;
    private static final int C_PANEL_INNER = 0xFF0E0F1C;
    private static final int C_RECIPE_BG = 0xFF09090F;
    private static final int C_ACCENT = 0xFF3D8EFF;
    private static final int C_ACCENT_DIM = 0xFF0C1A38;
    private static final int C_BREW = 0xFF00BBEE;
    private static final int C_FURNACE = 0xFFFF7822;
    private static final int C_GOLD = 0xFFE8A030;
    private static final int C_BORDER_MID = 0xFF182040;
    private static final int C_BORDER_DIM = 0xFF080C18;
    private static final int C_SLOT = 0xFF0B0C18;
    private static final int C_SLOT_LT = 0xFF1C1E30;
    private static final int C_SLOT_DK = 0xFF040408;
    private static final int C_SCR_TRACK = 0xFF060710;
    private static final int C_SCR_THUMB = 0xFF1A2C50;
    private static final int C_SCR_ACTIVE = 0xFF3D8EFF;
    private static final int C_BTN = 0xFF0B1022;
    private static final int C_BTN_HOV = 0xFF162038;
    private static final int C_BTN_BDR = 0xFF162040;
    private static final int C_TXT_TITLE = 0xFFFFFFFF;
    private static final int C_TXT_DIM = 0xFF445870;
    private static final int C_TXT_HINT = 0xFF2A3648;
    private static final int C_TXT_GOLD = 0xFFE8A030;
    private static final int C_TXT_BREW = 0xFF44CCFF;

    // ── Grid constants (base values, scaled in initGui) ────────────────────────
    private int COLS = 9;
    private int SZ = 18;
    private static final int SBW = 6;
    private static final int SBP = 4;

    // ── Dynamic layout ─────────────────────────────────────────────────────────
    private int pad, panelW, panelH, panelX, panelY;
    private int gridX, gridY, sbX, sbY, sbH, GRID_W;
    private int titleH, searchY, searchH, gridOff, btnW, btnH;
    private int visibleRows;
    private int footerY;

    // ── Smooth scroll ──────────────────────────────────────────────────────────
    private float smoothScroll = 0f;
    private int targetScroll = 0;

    // ── Search focus animation ─────────────────────────────────────────────────
    private float searchFocusAnim = 0f;

    // ── Potion recipe system ───────────────────────────────────────────────────
    static class PotionRecipe {
        int inputMeta, outputMeta;
        ItemStack ingredient;
        String name;

        PotionRecipe(int in, int out, ItemStack ingr, String n) {
            inputMeta = in;
            outputMeta = out;
            ingredient = ingr;
            name = n;
        }
    }

    private static final Map<Integer, List<PotionRecipe>> POTION_RECIPES = new HashMap<>();
    // Clés fictives pour les potions custom (non encodables en meta vanilla)
    static final int META_HASTE1 = 9001;
    static final int META_HASTE2 = 9002;
    static final int META_HASTE1_SPLASH = 9003;
    static final int META_HASTE2_SPLASH = 9004;
    static final int META_FALL1 = 9005;
    static final int META_FALL2 = 9006;
    static final int META_FALL1_SPLASH = 9007;
    static final int META_FALL2_SPLASH = 9008;
    // ItemStacks NBT réels associés aux meta fictifs
    static final Map<Integer, ItemStack> CUSTOM_POTION_STACKS = new HashMap<>();
    // Map signatures des potions -> meta fictif pour résolution rapide
    static final Map<String, Integer> CUSTOM_POTION_SIGNATURES = new HashMap<>();

    static {
        ItemStack glowstone = new ItemStack(Items.glowstone_dust);
        ItemStack redstone = new ItemStack(Items.redstone);
        ItemStack gunpowder = new ItemStack(Items.gunpowder);
        ItemStack fermented = new ItemStack(Items.fermented_spider_eye);
        ItemStack feather = new ItemStack(Items.feather);
        ItemStack diamBlock = new ItemStack(net.minecraft.init.Blocks.diamond_block);
        POTION_RECIPES.computeIfAbsent(16, k -> new ArrayList<>()).add(new PotionRecipe(0, 16, new ItemStack(Items.nether_wart), "Awkward"));
        int base = 8193;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.ghast_tear), "Régénération"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Régénération II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Régénération Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Régénération Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Régénération II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Régénération Étendue Splash"));
        base = 8194;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.sugar), "Rapidité"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Rapidité II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Rapidité Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Rapidité Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Rapidité II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Rapidité Étendue Splash"));
        base = 8195;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.magma_cream), "Résistance au Feu"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Résistance au Feu Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Résistance au Feu Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Résistance au Feu Étendue Splash"));
        base = 8196;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.spider_eye), "Poison"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Poison II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Poison Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Poison Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Poison II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Poison Étendue Splash"));
        base = 8197;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.speckled_melon), "Soin"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Soin II"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Soin Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Soin II Splash"));
        base = 8198;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.golden_carrot), "Vision Nocturne"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Vision Nocturne Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Vision Nocturne Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Vision Nocturne Étendue Splash"));
        base = 8201;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.blaze_powder), "Force"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Force II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Force Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Force Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Force II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Force Étendue Splash"));
        base = 8203;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.rabbit_foot), "Saut"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Saut II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Saut Étendu"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Saut Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Saut II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Saut Étendu Splash"));
        base = 8205;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.fish, 1, 3), "Respiration Aquatique"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Respiration Aquatique Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Respiration Aquatique Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Respiration Aquatique Étendue Splash"));
        base = 8200;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(0, base, fermented, "Faiblesse"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Faiblesse Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Faiblesse Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Faiblesse Étendue Splash"));
        base = 8202;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8194, base, fermented, "Lenteur"));
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8203, base, fermented, "Lenteur"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(8258, base + 64, fermented, "Lenteur Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(8267, base + 64, fermented, "Lenteur Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Lenteur Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Lenteur Étendue Splash"));
        base = 8204;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8197, base, fermented, "Dommages"));
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8196, base, fermented, "Dommages"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(8229, base + 32, fermented, "Dommages II"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(8228, base + 32, fermented, "Dommages II"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Dommages Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Dommages II Splash"));
        base = 8206;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8198, base, fermented, "Invisibilité"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(8262, base + 64, fermented, "Invisibilité Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Invisibilité Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Invisibilité Étendue Splash"));

        // ── Potions custom : Haste ─────────────────────────────────────────────
        // Haste I  : Potion bancale + Bloc de diamant
        POTION_RECIPES.computeIfAbsent(META_HASTE1, k -> new ArrayList<>())
                .add(new PotionRecipe(16, META_HASTE1, diamBlock, "Haste"));
        // Haste II : Haste I + Glowstone
        POTION_RECIPES.computeIfAbsent(META_HASTE2, k -> new ArrayList<>())
                .add(new PotionRecipe(META_HASTE1, META_HASTE2, glowstone, "Haste II"));
        // Haste I Splash : Haste I + Gunpowder
        POTION_RECIPES.computeIfAbsent(META_HASTE1_SPLASH, k -> new ArrayList<>())
                .add(new PotionRecipe(META_HASTE1, META_HASTE1_SPLASH, gunpowder, "Haste Splash"));
        // Haste II Splash : Haste II + Gunpowder
        POTION_RECIPES.computeIfAbsent(META_HASTE2_SPLASH, k -> new ArrayList<>())
                .add(new PotionRecipe(META_HASTE2, META_HASTE2_SPLASH, gunpowder, "Haste II Splash"));

        // ── Potions custom : Fall Protection ──────────────────────────────────
        // Fall I  : Potion bancale + Plume
        POTION_RECIPES.computeIfAbsent(META_FALL1, k -> new ArrayList<>())
                .add(new PotionRecipe(16, META_FALL1, feather, "Protection contre la Chute"));
        // Fall II : Fall I + Glowstone
        POTION_RECIPES.computeIfAbsent(META_FALL2, k -> new ArrayList<>())
                .add(new PotionRecipe(META_FALL1, META_FALL2, glowstone, "Protection contre la Chute II"));
        // Fall I Splash : Fall I + Gunpowder
        POTION_RECIPES.computeIfAbsent(META_FALL1_SPLASH, k -> new ArrayList<>())
                .add(new PotionRecipe(META_FALL1, META_FALL1_SPLASH, gunpowder, "Protection contre la Chute Splash"));
        // Fall II Splash : Fall II + Gunpowder
        POTION_RECIPES.computeIfAbsent(META_FALL2_SPLASH, k -> new ArrayList<>())
                .add(new PotionRecipe(META_FALL2, META_FALL2_SPLASH, gunpowder, "Protection contre la Chute II Splash"));

        // ── ItemStacks NBT réels pour les potions custom ───────────────────────
        CUSTOM_POTION_STACKS.put(META_HASTE1, ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id, 0, 3600, "Potion of Haste"));
        CUSTOM_POTION_STACKS.put(META_HASTE2, ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id, 1, 1800, "Potion of Haste II"));
        CUSTOM_POTION_STACKS.put(META_HASTE1_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id, 0, 3600, "Splash Potion of Haste", true));
        CUSTOM_POTION_STACKS.put(META_HASTE2_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id, 1, 1800, "Splash Potion of Haste II", true));
        CUSTOM_POTION_STACKS.put(META_FALL1, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id, 0, 3600, "Potion de Fall Protection"));
        CUSTOM_POTION_STACKS.put(META_FALL2, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id, 1, 1800, "Potion de Fall Protection II"));
        CUSTOM_POTION_STACKS.put(META_FALL1_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id, 0, 3600, "Splash Potion of Fall Protection", true));
        CUSTOM_POTION_STACKS.put(META_FALL2_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id, 1, 1800, "Splash Potion of Fall Protection II", true));

        // Construire des signatures pour résolution fiable (effets triés + nom sans formatage)
        for (Map.Entry<Integer, ItemStack> e : CUSTOM_POTION_STACKS.entrySet()) {
            Integer key = e.getKey();
            ItemStack stack = e.getValue();
            if (stack == null) continue;
            try {
                String sig = buildPotionSignature(stack);
                if (sig != null && !sig.isEmpty()) CUSTOM_POTION_SIGNATURES.put(sig, key);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "tile.litFurnace", "tile.farmland", "tile.doorWood", "tile.doorIron",
            "tile.litRedstoneOre", "tile.redstoneLamp.on", "tile.pistonHead",
            "tile.pistonMoving", "tile.cake", "tile.skull", "tile.carrots",
            "tile.potatoes", "tile.wheat", "tile.netherWart", "tile.cocoa",
            "tile.pumpkinStem", "tile.melonStem", "tile.tripWire",
            "tile.beetroots", "tile.portal", "tile.endPortal", "tile.endGateway",
            "tile.fire", "tile.water", "tile.lava", "tile.air"
    ));

    // ── State ──────────────────────────────────────────────────────────────────
    private GuiTextField searchBox;
    private final List<ItemStack> allItems = new ArrayList<>();
    private final List<ItemStack> filtered = new ArrayList<>();
    private final List<ItemStack> historyStack = new ArrayList<>();
    private boolean draggingSB = false;

    private enum Mode {LIST, CRAFT, BREWING, FURNACE}

    private Mode mode = Mode.LIST;
    private ItemStack selected;
    // Si non-null, sera utilisé dans initGui() pour pré-sélectionner une recette
    private ItemStack initialStack = null;
    private IRecipe craftRecipe;
    private ItemStack furnaceInput;
    private List<PotionRecipe> brewRecipesForItem = new ArrayList<>();
    private int brewRecipeIndex = 0;
    private PotionRecipe currentBrewRecipe = null;
    private GuiButton btnBack, btnMenu;

    public GuiCraftGuide(GuiScreen parent) {
    }

    // Constructeur pratique : ouvre directement le guide sur un ItemStack donné
    public GuiCraftGuide(GuiScreen parent, ItemStack initial) {
        this(parent);
        if (initial != null) {
            // Conserver l'item initial et l'appliquer dans initGui()
            this.initialStack = initial;
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        buttonList.clear();
        historyStack.clear();
        mode = Mode.LIST;
        selected = null;
        craftRecipe = null;
        furnaceInput = null;
        currentBrewRecipe = null;
        brewRecipesForItem = new ArrayList<>();
        brewRecipeIndex = 0;

        // Si un item initial a été fourni via le constructeur, l'ouvrir maintenant
        if (initialStack != null) {
            loadRecipe(initialStack);
            // clear initialStack pour éviter réouverture si initGui est rappelé
            initialStack = null;
            // leave init flow so layout will reflect selected recipe
        }

        // Adaptive scaling based on screen resolution
        SZ = GuiRenderUtils.clamp(height / 18, 16, 24);
        COLS = GuiRenderUtils.clamp((width - 60) / SZ, 7, 11);
        visibleRows = GuiRenderUtils.clamp((height - 150) / SZ, 3, 8);
        GRID_W = COLS * SZ;
        pad = Math.max(10, width / 50);
        titleH = Math.max(24, height / 18);
        searchH = 18;
        searchY = titleH + 6;
        gridOff = searchY + searchH + 6;
        btnH = 18;
        btnW = Math.max(60, width / 8);

        panelW = GuiRenderUtils.clamp(pad + GRID_W + SBP + SBW + pad, 200, width - 20);
        panelH = GuiRenderUtils.clamp(gridOff + visibleRows * SZ + 12 + btnH + 30, 180, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        gridX = panelX + pad;
        gridY = panelY + gridOff;
        sbX = gridX + GRID_W + SBP;
        sbY = gridY;
        sbH = visibleRows * SZ;
        footerY = gridY + sbH + 4;

        btnBack = new GuiButton(1, panelX + pad + 2, panelY + panelH - btnH - 6, btnW, btnH, "Retour");
        btnMenu = new GuiButton(2, panelX + panelW - pad - btnW - 2, panelY + panelH - btnH - 6, btnW, btnH, "Menu");

        searchBox = new GuiTextField(0, fontRendererObj,
                panelX + pad + 16, panelY + searchY,
                panelW - pad * 2 - SBP - SBW - 16, searchH);
        searchBox.setMaxStringLength(40);
        searchBox.setFocused(true);

        if (allItems.isEmpty()) {
            for (Item item : Item.itemRegistry) {
                if (item == null || item == Items.potionitem) continue;
                List<ItemStack> sub = new ArrayList<>();
                item.getSubItems(item, null, sub);
                for (ItemStack s : sub) if (s != null && !isBlocked(s)) allItems.add(s);
            }
            Set<Integer> added = new HashSet<>();
            for (Map.Entry<Integer, List<PotionRecipe>> e : POTION_RECIPES.entrySet()) {
                int meta = e.getKey();
                if (added.add(meta)) {
                    // Utiliser le vrai ItemStack NBT pour les potions custom, sinon le meta brut
                    if (CUSTOM_POTION_STACKS.containsKey(meta)) {
                        allItems.add(CUSTOM_POTION_STACKS.get(meta));
                    } else {
                        allItems.add(new ItemStack(Items.potionitem, 1, meta));
                    }
                }
            }
        }
        filter("");
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private void drawSlot3D(int x, int y) {
        drawRect(x, y, x + SZ, y + SZ, C_SLOT);
        drawRect(x, y, x + SZ, y + 1, C_SLOT_LT);
        drawRect(x, y, x + 1, y + SZ, C_SLOT_LT);
        drawRect(x, y + SZ - 1, x + SZ, y + SZ, C_SLOT_DK);
        drawRect(x + SZ - 1, y, x + SZ, y + SZ, C_SLOT_DK);
    }

    private void drawSlotOutput(int x, int y, int borderColor) {
        drawSlot3D(x, y);
        int base = borderColor & 0x00FFFFFF;
        drawRect(x - 2, y - 2, x + SZ + 2, y + SZ + 2, 0x10000000 | base);
        drawRect(x - 1, y - 1, x + SZ + 1, y + SZ + 1, 0x20000000 | base);
        drawRect(x, y, x + SZ, y + 1, borderColor);
        drawRect(x, y, x + 1, y + SZ, borderColor);
        drawRect(x, y + SZ - 1, x + SZ, y + SZ, C_BORDER_DIM);
        drawRect(x + SZ - 1, y, x + SZ, y + SZ, C_BORDER_DIM);
    }

    private void drawSlotHover(int x, int y) {
        long t = System.currentTimeMillis() % 1200;
        float p = t < 600 ? t / 600f : (1200f - t) / 600f;
        int a = (int) (0x30 + 0x30 * p);
        drawRect(x, y, x + SZ, y + SZ, (a << 24) | 0x3D8EFF);
        drawRect(x, y, x + SZ, y + 1, ((int) (0x80 + 0x40 * p) << 24) | 0x3D8EFF);
    }

    private void drawNavButton(int x, int y, int w, int h, String text, boolean hover, int accentCol) {
        GuiRenderUtils.drawStyledButton(x, y, w, h, hover ? C_BTN_HOV : C_BTN, hover ? accentCol : C_BTN_BDR, hover);
        int txtColor = hover ? 0xFFFFFFFF : 0xFFAABBCC;
        drawCenteredString(fontRendererObj, text, x + w / 2, y + (h - 8) / 2, txtColor);
    }

    private void drawSeparator(int x, int y, int w, int accent) {
        GuiRenderUtils.drawGradientRect(x, y, x + w / 3, y + 1, 0x00000000, accent);
        drawRect(x + w / 3, y, x + w * 2 / 3, y + 1, accent);
        GuiRenderUtils.drawGradientRect(x + w * 2 / 3, y, x + w, y + 1, accent, 0x00000000);
    }

    private void drawModeBadge(int x, int y, String label, int bg, int border) {
        int tw = fontRendererObj.getStringWidth(label);
        drawRect(x, y, x + tw + 8, y + 12, bg);
        drawRect(x, y, x + tw + 8, y + 1, border);
        drawRect(x, y, x + 1, y + 12, border);
        fontRendererObj.drawStringWithShadow(label, x + 4, y + 2, border);
    }

    // ── Main draw ─────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mx, int my, float pt) {
        // Update animations
        smoothScroll = GuiRenderUtils.lerp(smoothScroll, targetScroll, 0.25f);
        if (Math.abs(smoothScroll - targetScroll) < 0.05f) smoothScroll = targetScroll;

        float searchTarget = (searchBox != null && searchBox.isFocused()) ? 1.0f : 0.0f;
        searchFocusAnim = GuiRenderUtils.lerp(searchFocusAnim, searchTarget, 0.15f);

        // Background overlay
        drawRect(0, 0, width, height, C_OVERLAY);
        int accent = modeAccent();

        if (mode == Mode.LIST) {
            // Panel with shadow + rounded corners (only for list mode)
            GuiRenderUtils.drawRoundedPanel(panelX, panelY, panelW, panelH, C_PANEL, C_PANEL_HDR, titleH, accent);
            // Inner area
            drawRect(panelX + 1, panelY + titleH + 1, panelX + panelW - 1, panelY + panelH - 1, C_PANEL_INNER);

            // Title
            fontRendererObj.drawStringWithShadow("\u00a7l\u2726 Guide de Craft", panelX + pad, panelY + (titleH - 8) / 2, C_TXT_TITLE);

            // Item count
            String countStr = filtered.size() + " items";
            int countX = panelX + panelW - pad - fontRendererObj.getStringWidth(countStr);
            fontRendererObj.drawStringWithShadow("\u00a77" + countStr, countX, panelY + (titleH - 8) / 2, C_TXT_DIM);

            drawSearchBox(mx, my);
            drawItemGrid(mx, my);
            drawFooter();
        } else {
            // Recipe mode draws its own panel with dynamic height
            drawRecipeMode(mx, my);
        }
        super.drawScreen(mx, my, pt);
    }

    private void drawSearchBox(int mx, int my) {
        int sx = panelX + pad, sy = panelY + searchY;
        int sw = panelW - pad * 2 - SBP - SBW;

        // Outer border
        drawRect(sx - 1, sy - 1, sx + sw + 1, sy + searchH + 1, C_BORDER_DIM);
        // Background
        drawRect(sx, sy, sx + sw, sy + searchH, 0xFF080A14);
        // Top inset shadow
        drawRect(sx, sy, sx + sw, sy + 1, 0xFF020305);
        drawRect(sx, sy, sx + 1, sy + searchH, 0xFF020305);

        // Animated focus underline
        int focusColor = GuiRenderUtils.colorLerp(C_BORDER_MID, C_ACCENT, searchFocusAnim);
        drawRect(sx, sy + searchH - 1, sx + sw, sy + searchH, focusColor);
        if (searchFocusAnim > 0.1f) {
            int glowAlpha = (int) (0x18 * searchFocusAnim);
            drawRect(sx, sy + searchH - 2, sx + sw, sy + searchH - 1, (glowAlpha << 24) | (C_ACCENT & 0x00FFFFFF));
        }

        // Search icon
        int iconColor = GuiRenderUtils.colorLerp(C_TXT_HINT, C_ACCENT, searchFocusAnim);
        GuiRenderUtils.drawSearchIcon(sx + 3, sy + (searchH - 10) / 2 + 1, iconColor);

        // Placeholder text
        if (searchBox.getText().isEmpty()) {
            fontRendererObj.drawStringWithShadow("\u00a7o Rechercher...", sx + 16, sy + (searchH - 8) / 2, C_TXT_HINT);
        }
        searchBox.drawTextBox();
    }

    private void drawItemGrid(int mx, int my) {
        // Scrollbar track
        drawRect(sbX, sbY, sbX + SBW, sbY + sbH, C_SCR_TRACK);
        drawRect(sbX, sbY, sbX + SBW, sbY + 1, C_BORDER_DIM);
        drawRect(sbX, sbY + sbH - 1, sbX + SBW, sbY + sbH, C_BORDER_DIM);

        int ms = maxScroll();
        if (ms > 0) {
            int th = thumbH();
            float scrollFrac = smoothScroll / ms;
            int ty = sbY + (int) (scrollFrac * (sbH - th));
            boolean h = inside(mx, my, sbX, sbY, SBW, sbH) || draggingSB;
            int thumbColor = h ? C_SCR_ACTIVE : C_SCR_THUMB;
            drawRect(sbX + 1, ty, sbX + SBW - 1, ty + th, thumbColor);
            if (h) {
                drawRect(sbX, ty, sbX + SBW, ty + 1, C_ACCENT);
                drawRect(sbX, ty + th - 1, sbX + SBW, ty + th, C_ACCENT);
            }
        }

        // Items
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();

        int scrollRow = (int) smoothScroll;
        float scrollFract = smoothScroll - scrollRow;
        int pixelOffset = (int) (scrollFract * SZ);
        int base = scrollRow * COLS;
        ItemStack hov = null;

        for (int row = 0; row <= visibleRows; row++) {
            for (int col = 0; col < COLS; col++) {
                int i = base + row * COLS + col;
                if (i >= filtered.size()) break;
                ItemStack s = filtered.get(i);
                int x = gridX + col * SZ;
                int y = gridY + row * SZ - pixelOffset;

                // Skip if outside visible area
                if (y + SZ <= gridY || y >= gridY + sbH) continue;

                drawSlot3D(x, y);
                itemRender.renderItemAndEffectIntoGUI(s, x + 1, y + 1);
                itemRender.renderItemOverlayIntoGUI(fontRendererObj, s, x + 1, y + 1, null);
                if (inside(mx, my, x, y, SZ, SZ) && y >= gridY && y + SZ <= gridY + sbH) {
                    drawSlotHover(x, y);
                    hov = s;
                }
            }
        }

        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        if (hov != null) renderToolTip(hov, mx, my);

        // Empty state
        if (filtered.isEmpty()) {
            int emptyY = gridY + sbH / 2 - 16;
            drawRect(panelX + pad, emptyY - 4, panelX + panelW - pad, emptyY + 28, 0x22FFFFFF);
            drawCenteredString(fontRendererObj, "\u00a77Aucun r\u00e9sultat trouv\u00e9", panelX + panelW / 2, emptyY + 2, C_TXT_DIM);
            drawCenteredString(fontRendererObj, "\u00a78Essayez un autre terme de recherche", panelX + panelW / 2, emptyY + 14, 0xFF2A3040);
        }
    }

    private void drawFooter() {
        GuiRenderUtils.drawGradientRect(panelX + 1, footerY, panelX + panelW - 1, footerY + 14, 0x00000000, 0x22000000);
        drawRect(panelX + 1, footerY + 14, panelX + panelW - 1, footerY + 15, 0x11FFFFFF);
        String hint = "\u00a77Clic\u00a78: recette  \u00a77\u00b7  Molette\u00a78: d\u00e9filer";
        drawCenteredString(fontRendererObj, hint, panelX + panelW / 2, footerY + 3, C_TXT_DIM);
    }

    private void drawRecipeMode(int mx, int my) {
        if (selected == null) return;
        int accent = modeAccent();

        // ── Calculate content height to properly size the panel ──
        // Breadcrumb area
        int breadcrumbH = historyStack.isEmpty() ? 0 : 14;
        // Recipe area height depends on mode
        int recipeAreaH;
        if (mode == Mode.BREWING) {
            recipeAreaH = BREW_H + 12 + 12; // brew box + recipe name
            if (brewRecipesForItem != null && brewRecipesForItem.size() > 1) recipeAreaH += 14; // indicator line
        } else if (mode == Mode.CRAFT) {
            recipeAreaH = 3 * SZ + 24; // grid + output text + margin
        } else {
            recipeAreaH = SZ + 20; // furnace: single row + margin
        }
        // Total needed: header + breadcrumb + separator + title + recipe + buttons + margins
        int neededH = titleH + 8 + breadcrumbH + 10 + 14 + recipeAreaH + 16 + btnH + 12;
        int recPanelH = Math.max(panelH, neededH);
        int recPanelY = (height - recPanelH) / 2;

        // Redraw panel background for recipe mode with correct height
        GuiRenderUtils.drawRoundedPanel(panelX, recPanelY, panelW, recPanelH, C_PANEL, C_PANEL_HDR, titleH, accent);
        drawRect(panelX + 1, recPanelY + titleH + 1, panelX + panelW - 1, recPanelY + recPanelH - 1, C_PANEL_INNER);

        // Item icon in header
        int iconX = panelX + pad + 2, iconY = recPanelY + (titleH - SZ) / 2;
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        itemRender.renderItemAndEffectIntoGUI(selected, iconX, iconY);
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();

        // Item name (truncated to fit)
        String name = selected.getDisplayName();
        int maxNameW = panelW - pad * 2 - SZ - 90;
        if (fontRendererObj.getStringWidth(name) > maxNameW)
            name = fontRendererObj.trimStringToWidth(name, maxNameW) + "..";
        fontRendererObj.drawStringWithShadow("\u00a7l" + name, iconX + SZ + 4, recPanelY + (titleH - 8) / 2, C_TXT_TITLE);

        // Mode badge (right side of header)
        String badge = mode == Mode.BREWING ? " ALAMBIC " : mode == Mode.FURNACE ? "  FOUR  " : "  CRAFT  ";
        int badgeX = panelX + panelW - pad - fontRendererObj.getStringWidth(badge) - 10;
        drawModeBadge(badgeX, recPanelY + (titleH - 12) / 2, badge, C_ACCENT_DIM, accent);

        // Breadcrumb navigation
        int curY = recPanelY + titleH + 6;
        if (!historyStack.isEmpty()) {
            StringBuilder crumb = new StringBuilder();
            for (int i = Math.max(0, historyStack.size() - 2); i < historyStack.size(); i++) {
                if (crumb.length() > 0) crumb.append(" \u00a77\u203A ");
                crumb.append("\u00a78").append(historyStack.get(i).getDisplayName());
            }
            crumb.append(" \u00a77\u203A \u00a7f").append(selected.getDisplayName());
            String crumbStr = crumb.toString();
            int crumbMaxW = panelW - pad * 2;
            if (fontRendererObj.getStringWidth(crumbStr) > crumbMaxW)
                crumbStr = fontRendererObj.trimStringToWidth(crumbStr, crumbMaxW);
            fontRendererObj.drawStringWithShadow(crumbStr, panelX + pad, curY, C_TXT_DIM);
            curY += 14;
        }
        drawSeparator(panelX + pad, curY, panelW - pad * 2, accent);
        curY += 10;
        int cx = panelX + panelW / 2;
        ItemStack hovItem = null;

        // Recipe title
        String recipeTitle = mode == Mode.CRAFT ? "Crafting" : mode == Mode.BREWING ? "Alchimie" : "Fourneau";
        // Pour le mode CRAFT on augmente légèrement l'espacement vertical afin que le titre soit
        // clairement au-dessus de la zone de craft (évite le chevauchement avec la bordure de la boîte).
        drawCenteredString(fontRendererObj, "\u00a77" + recipeTitle, cx, curY, C_TXT_DIM);
        curY += (mode == Mode.CRAFT ? 20 : 14);

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        if (mode == Mode.BREWING && currentBrewRecipe != null)
            hovItem = drawBrewingRecipe(mx, my, cx, curY);
        else if (mode == Mode.CRAFT && craftRecipe != null)
            hovItem = drawCraftGrid(mx, my, cx, curY);
        else if (mode == Mode.FURNACE && furnaceInput != null)
            hovItem = drawFurnaceRecipe(mx, my, cx, curY);
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();
        if (hovItem != null) renderToolTip(hovItem, mx, my);

        // Navigation buttons — positioned at bottom of recipe panel, not overlapping content
        int btnAreaY = recPanelY + recPanelH - btnH - 8;
        btnBack.yPosition = btnAreaY;
        btnBack.xPosition = panelX + pad + 2;
        btnMenu.yPosition = btnAreaY;
        btnMenu.xPosition = panelX + panelW - pad - btnW - 2;
        boolean backHov = inside(mx, my, btnBack.xPosition, btnBack.yPosition, btnW, btnH);
        boolean menuHov = inside(mx, my, btnMenu.xPosition, btnMenu.yPosition, btnW, btnH);
        drawNavButton(btnBack.xPosition, btnBack.yPosition, btnW, btnH, "\u25C4 Retour", backHov, accent);
        drawNavButton(btnMenu.xPosition, btnMenu.yPosition, btnW, btnH, "Menu", menuHov, C_ACCENT);
        if (!historyStack.isEmpty()) {
            String depth = historyStack.size() + " niv.";
            drawCenteredString(fontRendererObj, "\u00a77" + depth, cx, btnBack.yPosition + 5, C_TXT_DIM);
        }
    }

    private ItemStack drawCraftGrid(int mx, int my, int cx, int top) {
        ItemStack[] grid = getGrid(craftRecipe);
        int gw = 3 * SZ;
        int totalW = gw + 30 + SZ;
        int sx0 = cx - totalW / 2;
        ItemStack hov = null;

        // Recipe background
        drawRect(sx0 - 6, top - 6, sx0 + gw + 10, top + 3 * SZ + 6, C_RECIPE_BG);
        drawRect(sx0 - 6, top - 6, sx0 + gw + 10, top - 5, C_BORDER_MID);
        drawRect(sx0 - 6, top - 6, sx0 - 5, top + 3 * SZ + 6, C_BORDER_MID);
        drawRect(sx0 - 6, top + 3 * SZ + 5, sx0 + gw + 10, top + 3 * SZ + 6, C_BORDER_DIM);
        drawRect(sx0 + gw + 9, top - 6, sx0 + gw + 10, top + 3 * SZ + 6, C_BORDER_DIM);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                int sx = sx0 + col * SZ, sy = top + row * SZ;
                drawSlot3D(sx, sy);
                if (grid[idx] != null) {
                    itemRender.renderItemAndEffectIntoGUI(grid[idx], sx + 1, sy + 1);
                    if (inside(mx, my, sx, sy, SZ, SZ)) {
                        drawSlotHover(sx, sy);
                        hov = grid[idx];
                    }
                }
            }
        }

        // Animated chevron arrow
        long t = System.currentTimeMillis() % 1000;
        float arrowAnim = t / 1000f;
        int arrowX = sx0 + gw + 8;
        int arrowY = top + SZ + SZ / 2;
        GuiRenderUtils.drawChevronArrow(arrowX, arrowY, 6, 0xE8A030, arrowAnim);

        // Output slot
        int rx = sx0 + gw + 26, ry = top + SZ;
        drawSlotOutput(rx, ry, C_GOLD);
        ItemStack out = craftRecipe.getRecipeOutput();
        itemRender.renderItemAndEffectIntoGUI(out, rx + 1, ry + 1);
        itemRender.renderItemOverlayIntoGUI(fontRendererObj, out, rx + 1, ry + 1, null);
        if (inside(mx, my, rx, ry, SZ, SZ)) {
            drawSlotHover(rx, ry);
            hov = out;
        }
        if (out.stackSize > 1)
            fontRendererObj.drawStringWithShadow("\u00a76x" + out.stackSize, rx, ry + SZ + 4, C_TXT_GOLD);
        return hov;
    }

    private ItemStack drawFurnaceRecipe(int mx, int my, int cx, int top) {
        int lx = cx - (SZ + 32 + SZ) / 2;
        ItemStack hov = null;

        // Recipe background
        drawRect(lx - 6, top - 6, lx + SZ + 32 + SZ + 6, top + SZ + 6, C_RECIPE_BG);
        drawRect(lx - 6, top - 6, lx + SZ + 32 + SZ + 6, top - 5, C_BORDER_MID);
        drawRect(lx - 6, top - 6, lx - 5, top + SZ + 6, C_BORDER_MID);
        drawRect(lx - 6, top + SZ + 5, lx + SZ + 32 + SZ + 6, top + SZ + 6, C_BORDER_DIM);
        drawRect(lx + SZ + 32 + SZ + 5, top - 6, lx + SZ + 32 + SZ + 6, top + SZ + 6, C_BORDER_DIM);

        ItemStack input = fixWildcard(furnaceInput);
        drawSlot3D(lx, top);
        itemRender.renderItemAndEffectIntoGUI(input, lx + 1, top + 1);
        if (inside(mx, my, lx, top, SZ, SZ)) {
            drawSlotHover(lx, top);
            hov = input;
        }

        // Animated arrow
        long t = System.currentTimeMillis() % 1000;
        GuiRenderUtils.drawChevronArrow(lx + SZ + 6, top + SZ / 2, 6, 0xFF7822, t / 1000f);

        int rx = lx + SZ + 32;
        drawSlotOutput(rx, top, C_FURNACE);
        itemRender.renderItemAndEffectIntoGUI(selected, rx + 1, top + 1);
        if (inside(mx, my, rx, top, SZ, SZ)) {
            drawSlotHover(rx, top);
            hov = selected;
        }
        return hov;
    }

    private static final int BREW_W = 96, BREW_H = 70;

    private ItemStack drawBrewingRecipe(int mx, int my, int cx, int top) {
        if (currentBrewRecipe == null) {
            return null; // Return early if no brewing recipe is available
        }

        int bx = cx - BREW_W / 2;
        ItemStack hov = null;

        // Background
        drawRect(bx - 4, top - 4, bx + BREW_W + 4, top + BREW_H + 4, C_RECIPE_BG);
        drawRect(bx - 4, top - 4, bx + BREW_W + 4, top - 3, C_BREW);
        drawRect(bx - 4, top - 4, bx - 3, top + BREW_H + 4, C_BREW);
        drawRect(bx - 4, top + BREW_H + 3, bx + BREW_W + 4, top + BREW_H + 4, C_BORDER_DIM);
        drawRect(bx + BREW_W + 3, top - 4, bx + BREW_W + 4, top + BREW_H + 4, C_BORDER_DIM);

        // Glow
        int glowBase = C_BREW & 0x00FFFFFF;
        drawRect(bx - 5, top - 5, bx + BREW_W + 5, top + BREW_H + 5, 0x08000000 | glowBase);

        // Ingredient (top center)
        int ingX = bx + BREW_W / 2 - SZ / 2, ingY = top + 4;
        drawSlot3D(ingX, ingY);
        itemRender.renderItemAndEffectIntoGUI(currentBrewRecipe.ingredient, ingX + 1, ingY + 1);
        if (inside(mx, my, ingX, ingY, SZ, SZ)) {
            drawSlotHover(ingX, ingY);
            hov = currentBrewRecipe.ingredient;
        }

        // Animated drip
        long t = System.currentTimeMillis() % 1000;
        int dropY = ingY + SZ + (int) (8 * (t / 1000f));
        int dropAlpha = (int) (0xCC * (1f - t / 1000f));
        drawRect(ingX + SZ / 2 - 1, dropY, ingX + SZ / 2 + 1, dropY + 3, (dropAlpha << 24) | 0x00BBEE);

        // Input potion (bottom left)
        int inX = bx + 8, inY = top + BREW_H - SZ - 6;
        drawSlot3D(inX, inY);
        ItemStack inputPotion = CUSTOM_POTION_STACKS.containsKey(currentBrewRecipe.inputMeta)
                ? CUSTOM_POTION_STACKS.get(currentBrewRecipe.inputMeta)
                : new ItemStack(Items.potionitem, 1, currentBrewRecipe.inputMeta);
        itemRender.renderItemAndEffectIntoGUI(inputPotion, inX + 1, inY + 1);
        if (inside(mx, my, inX, inY, SZ, SZ)) {
            drawSlotHover(inX, inY);
            hov = inputPotion;
        }

        // Output potion (bottom right)
        int outX = bx + BREW_W - SZ - 8, outY = inY;
        drawSlotOutput(outX, outY, C_BREW);
        ItemStack outputPotion = CUSTOM_POTION_STACKS.containsKey(currentBrewRecipe.outputMeta)
                ? CUSTOM_POTION_STACKS.get(currentBrewRecipe.outputMeta)
                : new ItemStack(Items.potionitem, 1, currentBrewRecipe.outputMeta);
        itemRender.renderItemAndEffectIntoGUI(outputPotion, outX + 1, outY + 1);
        if (inside(mx, my, outX, outY, SZ, SZ)) {
            drawSlotHover(outX, outY);
            hov = outputPotion;
        }

        // Multiple recipe indicator + Recipe name — stacked without overlap
        int textY = top + BREW_H + 8;
        if (brewRecipesForItem.size() > 1) {
            String ind = (brewRecipeIndex + 1) + "/" + brewRecipesForItem.size();
            fontRendererObj.drawStringWithShadow("\u00a77" + ind, bx + BREW_W - fontRendererObj.getStringWidth(ind), textY, C_TXT_DIM);
            fontRendererObj.drawStringWithShadow("\u00a77Clic-droit: autre voie", bx, textY, C_TXT_DIM);
            textY += 12;
        }

        // Recipe name
        drawCenteredString(fontRendererObj, "\u00a7b" + currentBrewRecipe.name, cx, textY, C_TXT_BREW);
        return hov;
    }

    // ── Logic ──────────────────────────────────────────────────────────────────

    private int modeAccent() {
        if (mode == Mode.BREWING) return C_BREW;
        if (mode == Mode.FURNACE) return C_FURNACE;
        return C_ACCENT;
    }

    private boolean isBlocked(ItemStack s) {
        if (s == null || s.getItem() == null) return true;
        String u = s.getUnlocalizedName();
        if (u == null) return true;
        for (String b : BLOCKED) if (u.startsWith(b)) return true;
        return false;
    }

    private void filter(String q) {
        filtered.clear();
        String lq = q.trim().toLowerCase();
        for (ItemStack s : allItems)
            if (lq.isEmpty() || s.getDisplayName().toLowerCase().contains(lq)) filtered.add(s);
        targetScroll = 0;
        smoothScroll = 0;
    }

    private int maxScroll() {
        return Math.max(0, (filtered.size() + COLS - 1) / COLS - visibleRows);
    }

    private int thumbH() {
        int total = (filtered.size() + COLS - 1) / COLS;
        if (total <= visibleRows) return sbH;
        return Math.max(16, sbH * visibleRows / total);
    }

    private void applyScrollFromMouse(int my) {
        int th = thumbH(), ms = maxScroll();
        if (ms == 0) return;
        targetScroll = Math.round((float) (my - sbY - th / 2) * ms / (sbH - th));
        targetScroll = Math.max(0, Math.min(targetScroll, ms));
    }

    /**
     * Résout le meta fictif d'une potion custom en se basant prioritairement sur NBT/effets, puis nom.
     * Retourne -1 si aucune correspondance trouvée.
     */
    private int resolveCustomPotionMeta(ItemStack stack) {
        if (stack == null) return -1;
        try {
            // Première tentative : signature basée sur effets triés + nom (construction déterministe)
            String sig = buildPotionSignature(stack);
            if (sig != null && !sig.isEmpty() && CUSTOM_POTION_SIGNATURES.containsKey(sig)) {
                return CUSTOM_POTION_SIGNATURES.get(sig);
            }
            // Ensuite, comparaison NBT exacte
            for (Map.Entry<Integer, ItemStack> e : CUSTOM_POTION_STACKS.entrySet()) {
                int key = e.getKey();
                ItemStack candidate = e.getValue();
                if (candidate == null) continue;
                if (candidate.hasTagCompound() && stack.hasTagCompound()) {
                    if (candidate.getTagCompound().equals(stack.getTagCompound())) return key;
                }
            }
            // Enfin, heuristiques : comparaison par effets puis par nom
            for (Map.Entry<Integer, ItemStack> e : CUSTOM_POTION_STACKS.entrySet()) {
                int key = e.getKey();
                ItemStack candidate = e.getValue();
                if (candidate == null) continue;
                try {
                    ItemPotion ip = (ItemPotion) Items.potionitem;
                    List<PotionEffect> ce = ip.getEffects(candidate);
                    List<PotionEffect> te = ip.getEffects(stack);
                    if (ce != null && te != null && comparePotionEffects(ce, te)) return key;
                } catch (Exception ex) {
                    // ignore
                }
                String candName = stripFormatting(candidate.getDisplayName()).toLowerCase(Locale.ROOT);
                String targetName = stripFormatting(stack.getDisplayName()).toLowerCase(Locale.ROOT);
                if (!candName.isEmpty() && candName.equals(targetName)) return key;
            }
        } catch (Exception ex) {
            // fallback silencieux
        }
        return -1;
    }

    // Construit une signature stable pour une potion : ensemble trié d'effets (id:amp) + nom nettoyée
    private static String buildPotionSignature(ItemStack stack) {
        if (stack == null) return "";
        try {
            ItemPotion ip = (ItemPotion) Items.potionitem;
            List<PotionEffect> eff = ip.getEffects(stack);
            List<String> parts = new ArrayList<>();
            if (eff != null) {
                for (PotionEffect p : eff) parts.add(p.getPotionID() + ":" + p.getAmplifier());
                Collections.sort(parts);
            }
            String name = stripFormattingStatic(stack.getDisplayName()).toLowerCase(Locale.ROOT);
            String joined = String.join(",", parts);
            return joined + "|" + name;
        } catch (Exception ex) {
            return stripFormattingStatic(stack.getDisplayName()).toLowerCase(Locale.ROOT);
        }
    }

    // static helper pour stripFormatting dans un contexte static
    private static String stripFormattingStatic(String s) {
        if (s == null) return "";
        return s.replaceAll("\u00A7.", "");
    }

    private boolean isSameStack(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;
        // Cas spécial : potions (vanilla ou custom)
        if (a.getItem() == Items.potionitem) {
            // Tenter résolution via signature/NBT/effets
            int ma = resolveCustomPotionMeta(a);
            int mb = resolveCustomPotionMeta(b);
            if (ma != -1 && mb != -1) return ma == mb;
            if (ma != -1 || mb != -1) return false;
            // Sinon comparer les effets (robuste) puis le nom sans formatage, puis meta brut
            try {
                ItemPotion ip = (ItemPotion) Items.potionitem;
                List<PotionEffect> ea = ip.getEffects(a);
                List<PotionEffect> eb = ip.getEffects(b);
                if (ea != null && eb != null) {
                    if (comparePotionEffects(ea, eb)) return true;
                }
            } catch (Exception ex) {
                // ignore
            }
            String na = stripFormatting(a.getDisplayName()).toLowerCase(Locale.ROOT);
            String nb = stripFormatting(b.getDisplayName()).toLowerCase(Locale.ROOT);
            if (!na.isEmpty() && !nb.isEmpty() && na.equals(nb)) return true;
            return a.getItemDamage() == b.getItemDamage();
        }
        // Cas général : comparer le metadata
        return a.getItemDamage() == b.getItemDamage();
    }

    private void openRecipe(ItemStack stack) {
        if (stack == null) return;
        if (selected != null && isSameStack(selected, stack)) return;
        ItemStack prev = selected;
        if (loadRecipe(stack)) {
            if (prev != null) {
                boolean top = !historyStack.isEmpty() && isSameStack(historyStack.get(historyStack.size() - 1), prev);
                if (!top) historyStack.add(prev);
            }
        }
    }

    private void goBack() {
        if (!historyStack.isEmpty()) loadRecipe(historyStack.remove(historyStack.size() - 1));
        else {
            mode = Mode.LIST;
            selected = null;
        }
    }

    private void goMenu() {
        mode = Mode.LIST;
        selected = null;
        historyStack.clear();
    }

    private boolean comparePotionEffects(List<PotionEffect> a, List<PotionEffect> b) {
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        // Compare as multisets of (potionId, amplifier) so ordering doesn't matter
        Map<String, Integer> cntA = new HashMap<>();
        Map<String, Integer> cntB = new HashMap<>();
        for (PotionEffect p : a) {
            String k = p.getPotionID() + ":" + p.getAmplifier();
            cntA.put(k, cntA.getOrDefault(k, 0) + 1);
        }
        for (PotionEffect p : b) {
            String k = p.getPotionID() + ":" + p.getAmplifier();
            cntB.put(k, cntB.getOrDefault(k, 0) + 1);
        }
        return cntA.equals(cntB);
    }

    // Enlève les codes de formatage Minecraft (§x) pour faciliter la comparaison de noms
    private String stripFormatting(String s) {
        if (s == null) return "";
        return s.replaceAll("\u00A7.", "");
    }

    private boolean loadRecipe(ItemStack stack) {
        // Sauvegarde de l'état actuel pour restauration si aucune recette n'est trouvée
        Mode oldMode = mode;
        ItemStack oldSel = selected;
        IRecipe oldCraft = craftRecipe;
        ItemStack oldFurnace = furnaceInput;
        List<PotionRecipe> oldBrew = brewRecipesForItem;
        PotionRecipe oldCurBrew = currentBrewRecipe;
        int oldBrewIdx = brewRecipeIndex;

        selected = stack;
        craftRecipe = null;
        currentBrewRecipe = null;
        brewRecipesForItem = new ArrayList<>();
        brewRecipeIndex = 0;
        furnaceInput = null;
        
        // Initialiser temporairement à LIST ; si rien n'est trouvé, cela restera ainsi
        mode = Mode.LIST;

        if (stack.getItem() == Items.potionitem) {
            // Détection des potions custom par leur nom NBT
            int customMeta = resolveCustomPotionMeta(stack);
            int meta = (customMeta != -1) ? customMeta : stack.getItemDamage();
            if (POTION_RECIPES.containsKey(meta)) {
                brewRecipesForItem = new ArrayList<>(POTION_RECIPES.get(meta));
                brewRecipesForItem.sort(Comparator.comparingInt(a -> a.inputMeta));
                if (!brewRecipesForItem.isEmpty()) {
                    currentBrewRecipe = brewRecipesForItem.get(0);
                    mode = Mode.BREWING;
                }
            }
            // Passe de secours : essayer de trouver une clé POTION_RECIPES dont le ItemStack candidat correspond
            if (brewRecipesForItem.isEmpty()) {
                for (Map.Entry<Integer, List<PotionRecipe>> e : POTION_RECIPES.entrySet()) {
                    int key = e.getKey();
                    ItemStack candidate = CUSTOM_POTION_STACKS.containsKey(key)
                            ? CUSTOM_POTION_STACKS.get(key)
                            : new ItemStack(Items.potionitem, 1, key);
                    if (isSameStack(candidate, stack)) {
                        brewRecipesForItem = new ArrayList<>(e.getValue());
                        brewRecipesForItem.sort(Comparator.comparingInt(a -> a.inputMeta));
                        if (!brewRecipesForItem.isEmpty()) {
                            currentBrewRecipe = brewRecipesForItem.get(0);
                            mode = Mode.BREWING;
                        }
                    }
                }
            }
        }
        int targetMeta = stack.getItemDamage();
        if (targetMeta == 32767) targetMeta = 0;
        boolean isDamageable = stack.getItem().isDamageable();
        // Première passe : recherche stricte (wildcard ou meta exact)
        for (IRecipe r : CraftingManager.getInstance().getRecipeList()) {
            ItemStack out = r.getRecipeOutput();
            if (out == null) continue;
            if (out.getItem() != stack.getItem()) continue;
            int outMeta = out.getItemDamage();
            if (outMeta == 32767 || outMeta == targetMeta) {
                craftRecipe = r;
                break;
            }
        }
        // Seconde passe : si aucun résultat et item damageable, accepter premier recipe du même item
        if (craftRecipe == null && isDamageable) {
            for (IRecipe r : CraftingManager.getInstance().getRecipeList()) {
                ItemStack out = r.getRecipeOutput();
                if (out == null) continue;
                if (out.getItem() == stack.getItem()) {
                    craftRecipe = r;
                    break;
                }
            }
        }
        if (craftRecipe == null) {
            // Furnace: même logique en deux passes
            for (Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
                ItemStack out = e.getValue();
                if (out == null) continue;
                if (out.getItem() != stack.getItem()) continue;
                int outMeta = out.getItemDamage();
                if (outMeta == 32767 || outMeta == targetMeta) {
                    furnaceInput = e.getKey();
                    break;
                }
            }
            if (furnaceInput == null && isDamageable) {
                for (Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
                    ItemStack out = e.getValue();
                    if (out == null) continue;
                    if (out.getItem() == stack.getItem()) {
                        furnaceInput = e.getKey();
                        break;
                    }
                }
            }
        }

        // Finaliser le mode
        if (craftRecipe != null) mode = Mode.CRAFT;
        else if (furnaceInput != null) mode = Mode.FURNACE;
        else if (!brewRecipesForItem.isEmpty()) mode = Mode.BREWING;
        
        // Si un mode a été trouvé, c'est un succès
        if (mode != Mode.LIST) {
            return true;
        }

        // Sinon, on restaure l'état précédent et on retourne false
        // (Sauf si on était déjà en mode LIST, auquel cas on reste en LIST mais sans recette)
        // Mais ici on veut éviter de quitter une recette pour aller vers LIST si on clique un ingrédient sans recette.
        mode = oldMode;
        selected = oldSel;
        craftRecipe = oldCraft;
        furnaceInput = oldFurnace;
        brewRecipesForItem = oldBrew;
        currentBrewRecipe = oldCurBrew;
        brewRecipeIndex = oldBrewIdx;
        return false;
    }

    private ItemStack fixWildcard(ItemStack s) {
        if (s == null) return null;
        ItemStack c = s.copy();
        if (c.getItemDamage() == 32767) c.setItemDamage(0);
        return c;
    }

    private ItemStack[] getGrid(IRecipe recipe) {
        ItemStack[] g = new ItemStack[9];
        if (recipe instanceof ShapedRecipes) {
            ShapedRecipes sr = (ShapedRecipes) recipe;
            for (int row = 0; row < sr.getRecipeHeight(); row++)
                for (int col = 0; col < sr.getRecipeWidth(); col++) {
                    int src = row * sr.getRecipeWidth() + col;
                    if (src < sr.getRecipeItems().length)
                        g[row * 3 + col] = fixWildcard(sr.getRecipeItems()[src]);
                }
        } else if (recipe instanceof ShapelessRecipes) {
            List<ItemStack> items = ((ShapelessRecipes) recipe).getRecipeItems();
            for (int i = 0; i < items.size() && i < 9; i++) g[i] = fixWildcard(items.get(i));
        }
        return g;
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ── Input ──────────────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        searchBox.mouseClicked(mx, my, btn);
        if (mode != Mode.LIST) {
            if (inside(mx, my, btnBack.xPosition, btnBack.yPosition, btnW, btnH)) {
                goBack();
                return;
            }
            if (inside(mx, my, btnMenu.xPosition, btnMenu.yPosition, btnW, btnH)) {
                goMenu();
                return;
            }
            // Calculate contentY to match drawRecipeMode exactly
            int breadcrumbH = historyStack.isEmpty() ? 0 : 14;
            int recipeAreaH;
            if (mode == Mode.BREWING) {
                recipeAreaH = BREW_H + 12 + 12;
                if (brewRecipesForItem != null && brewRecipesForItem.size() > 1) recipeAreaH += 14;
            } else if (mode == Mode.CRAFT) {
                recipeAreaH = 3 * SZ + 24;
            } else {
                recipeAreaH = SZ + 20;
            }
            int neededH = titleH + 8 + breadcrumbH + 10 + 14 + recipeAreaH + 16 + btnH + 12;
            int recPanelH = Math.max(panelH, neededH);
            int recPanelY = (height - recPanelH) / 2;
            int curY = recPanelY + titleH + 6 + breadcrumbH;
            curY += 10; // after separator
            curY += (mode == Mode.CRAFT ? 20 : 14);
            int contentY = curY;
            int cx = panelX + panelW / 2;

            if (btn == 0) {
                if (mode == Mode.BREWING && currentBrewRecipe != null) {
                    int bx = cx - BREW_W / 2;
                    // Ingredient
                    int ingX = bx + BREW_W / 2 - SZ / 2, ingY = contentY + 4;
                    if (inside(mx, my, ingX, ingY, SZ, SZ)) {
                        openRecipe(currentBrewRecipe.ingredient);
                        return;
                    }
                    // Input Potion
                    int inX = bx + 8, inY = contentY + BREW_H - SZ - 6;
                    if (inside(mx, my, inX, inY, SZ, SZ)) {
                        ItemStack inputStack = CUSTOM_POTION_STACKS.containsKey(currentBrewRecipe.inputMeta)
                                ? CUSTOM_POTION_STACKS.get(currentBrewRecipe.inputMeta)
                                : new ItemStack(Items.potionitem, 1, currentBrewRecipe.inputMeta);
                        openRecipe(inputStack);
                        return;
                    }
                    // Output Potion
                    int outX = bx + BREW_W - SZ - 8, outY = inY;
                    if (inside(mx, my, outX, outY, SZ, SZ)) {
                        ItemStack outputStack = CUSTOM_POTION_STACKS.containsKey(currentBrewRecipe.outputMeta)
                                ? CUSTOM_POTION_STACKS.get(currentBrewRecipe.outputMeta)
                                : new ItemStack(Items.potionitem, 1, currentBrewRecipe.outputMeta);
                        openRecipe(outputStack);
                        return;
                    }
                } else if (mode == Mode.CRAFT && craftRecipe != null) {
                    ItemStack[] grid = getGrid(craftRecipe);
                    int gw = 3 * SZ;
                    int totalW = gw + 30 + SZ;
                    int sx0 = cx - totalW / 2;
                    // Grid inputs
                    for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 3; col++) {
                            int idx = row * 3 + col;
                            if (grid[idx] == null) continue;
                            int sx = sx0 + col * SZ, sy = contentY + row * SZ;
                            if (inside(mx, my, sx, sy, SZ, SZ)) {
                                openRecipe(grid[idx]);
                                return;
                            }
                        }
                    }
                    // Output
                    int rx = sx0 + gw + 26, ry = contentY + SZ;
                    if (inside(mx, my, rx, ry, SZ, SZ)) {
                        openRecipe(craftRecipe.getRecipeOutput());
                        return;
                    }
                } else if (mode == Mode.FURNACE && furnaceInput != null) {
                    int lx = cx - (SZ + 32 + SZ) / 2;
                    // Input
                    if (inside(mx, my, lx, contentY, SZ, SZ)) {
                        openRecipe(furnaceInput);
                        return;
                    }
                    // Output
                    int rx = lx + SZ + 32;
                    if (inside(mx, my, rx, contentY, SZ, SZ)) {
                        openRecipe(selected);
                        return;
                    }
                }
            } else if (btn == 1 && mode == Mode.BREWING && brewRecipesForItem.size() > 1) {
                brewRecipeIndex = (brewRecipeIndex + 1) % brewRecipesForItem.size();
                currentBrewRecipe = brewRecipesForItem.get(brewRecipeIndex);
            }
            return;
        }
        if (btn == 0) {
            if (inside(mx, my, sbX, sbY, SBW, sbH)) {
                draggingSB = true;
                applyScrollFromMouse(my);
                return;
            }
            int scrollRow = (int) smoothScroll;
            float scrollFract = smoothScroll - scrollRow;
            int pixelOffset = (int) (scrollFract * SZ);
            int base = scrollRow * COLS;
            for (int row = 0; row <= visibleRows; row++) {
                for (int col = 0; col < COLS; col++) {
                    int i = base + row * COLS + col;
                    if (i >= filtered.size()) return;
                    int x = gridX + col * SZ;
                    int y = gridY + row * SZ - pixelOffset;
                    if (y + SZ <= gridY || y >= gridY + sbH) continue;
                    if (inside(mx, my, x, y, SZ, SZ)) {
                        openRecipe(filtered.get(i));
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        draggingSB = false;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        super.mouseClickMove(mx, my, btn, time);
        if (draggingSB && mode == Mode.LIST) applyScrollFromMouse(my);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int w = Mouse.getEventDWheel(), ms = maxScroll();
        if (w < 0 && targetScroll < ms) targetScroll++;
        if (w > 0 && targetScroll > 0) targetScroll--;
    }

    @Override
    protected void keyTyped(char ch, int key) throws IOException {
        if (mode != Mode.LIST && key == 1) {
            goMenu();
            return;
        }
        if (searchBox.textboxKeyTyped(ch, key)) filter(searchBox.getText());
        else super.keyTyped(ch, key);
    }

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        if (btn.id == 1) goBack();
        else if (btn.id == 2) goMenu();
    }
}
