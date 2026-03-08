package net.minecraft.client.gui.inventory;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

/**
 * GuiCraftGuide — SIMPLE VERSION avec potions hardcodées
 */
public class GuiCraftGuide extends GuiScreen {

    private static final int C_BG = 0xDD0A0A14;
    private static final int C_PANEL = 0xFF15151F;
    private static final int C_PANEL2 = 0xFF1C1C2E;
    private static final int C_BORDER = 0xFF4A90E2;
    private static final int C_BORDER2 = 0xFF2A3A5A;
    private static final int C_SLOT = 0xFF1F1F30;
    private static final int C_SLOT_HOV = 0x55FF6B35;
    private static final int C_SLOT_SEL = 0x88FF8C00;
    private static final int C_TEXT_TTL = 0xFFFFB347;
    private static final int C_TEXT_DTL = 0xFF7A8BA5;
    private static final int C_HINT = 0xFF5A6A8A;
    private static final int C_SCR_BG = 0xFF1F2635;
    private static final int C_SCR_FG = 0xFF4A90E2;
    private static final int C_SCR_HL = 0xFF6CB4FF;
    private static final int C_BTN_BG = 0xFF2A3A5A;
    private static final int C_BTN_HV = 0xFF4A90E2;

    private static final int COLS = 9;
    private static final int ROWS = 5;
    private static final int SZ = 18;
    private static final int SBW = 8;
    private static final int SBP = 4;
    private static final int PAD = 10;
    private static final int GRID_W = COLS * SZ;
    private static final int PANEL_W = PAD + GRID_W + SBP + SBW + PAD;
    private static final int TITLE_H = 15;
    private static final int SEARCH_Y = TITLE_H + 4;
    private static final int SEARCH_H = 16;
    private static final int GRID_OFF = SEARCH_Y + SEARCH_H + 4;
    private static final int HINT_H = 9;
    private static final int PANEL_H = GRID_OFF + ROWS * SZ + 4 + HINT_H + 4;
    private static final int BTN_W = 55;
    private static final int BTN_H = 14;

    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "tile.litFurnace", "tile.farmland", "tile.doorWood", "tile.doorIron",
            "tile.litRedstoneOre", "tile.redstoneLamp.on", "tile.pistonHead",
            "tile.pistonMoving", "tile.cake", "tile.skull", "tile.carrots",
            "tile.potatoes", "tile.wheat", "tile.netherWart", "tile.cocoa",
            "tile.pumpkinStem", "tile.melonStem", "tile.tripWire",
            "tile.beetroots", "tile.portal", "tile.endPortal", "tile.endGateway",
            "tile.fire", "tile.water", "tile.lava", "tile.air"
    ));

    // Classe simple pour une recette de potion
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

    // Map : outputMeta → liste des recettes possibles
    private static final Map<Integer, List<PotionRecipe>> POTION_RECIPES = new HashMap<>();

    static {
        ItemStack glowstone = new ItemStack(Items.glowstone_dust);
        ItemStack redstone = new ItemStack(Items.redstone);
        ItemStack gunpowder = new ItemStack(Items.gunpowder);
        ItemStack fermented = new ItemStack(Items.fermented_spider_eye);
// AWKWARD
        POTION_RECIPES.computeIfAbsent(16, k -> new ArrayList<>()).add(new PotionRecipe(0, 16, new ItemStack(Items.nether_wart), "Awkward"));
// REGENERATION
        int base = 8193;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.ghast_tear), "Régénération"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Régénération II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Régénération Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Régénération Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Régénération II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Régénération Étendue Splash"));
// SWIFTNESS
        base = 8194;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.sugar), "Rapidité"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Rapidité II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Rapidité Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Rapidité Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Rapidité II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Rapidité Étendue Splash"));
// FIRE RESISTANCE
        base = 8195;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.magma_cream), "Résistance au Feu"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Résistance au Feu Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Résistance au Feu Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Résistance au Feu Étendue Splash"));
// POISON
        base = 8196;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.spider_eye), "Poison"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Poison II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Poison Étendu"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Poison Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Poison II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Poison Étendu Splash"));
// HEALING
        base = 8197;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.speckled_melon), "Soin"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Soin II"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Soin Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Soin II Splash"));
// NIGHT VISION
        base = 8198;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.golden_carrot), "Vision Nocturne"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Vision Nocturne Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Vision Nocturne Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Vision Nocturne Étendue Splash"));
// STRENGTH
        base = 8201;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.blaze_powder), "Force"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Force II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Force Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Force Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Force II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Force Étendue Splash"));
// LEAPING
        base = 8203;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.rabbit_foot), "Saut"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glowstone, "Saut II"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Saut Étendu"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Saut Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Saut II Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Saut Étendu Splash"));
// WATER BREATHING
        base = 8205;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, new ItemStack(Items.fish, 1, 3), "Respiration Aquatique"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Respiration Aquatique Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Respiration Aquatique Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Respiration Aquatique Étendue Splash"));
// WEAKNESS
        base = 8200;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(0, base, fermented, "Faiblesse"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, redstone, "Faiblesse Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Faiblesse Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Faiblesse Étendue Splash"));
// SLOWNESS
        base = 8202;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8194, base, fermented, "Lenteur"));
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8203, base, fermented, "Lenteur"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(8258, base + 64, fermented, "Lenteur Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(8267, base + 64, fermented, "Lenteur Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Lenteur Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Lenteur Étendue Splash"));
// HARMING
        base = 8204;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8197, base, fermented, "Dommages"));
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8196, base, fermented, "Dommages"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(8229, base + 32, fermented, "Dommages II"));
        POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(8228, base + 32, fermented, "Dommages II"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Dommages Splash"));
        POTION_RECIPES.computeIfAbsent((base + 32) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, (base + 32) + 8192, gunpowder, "Dommages II Splash"));
// INVISIBILITY
        base = 8206;
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(8198, base, fermented, "Invisibilité"));
        POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(8262, base + 64, fermented, "Invisibilité Étendue"));
        POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gunpowder, "Invisibilité Splash"));
        POTION_RECIPES.computeIfAbsent((base + 64) + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, (base + 64) + 8192, gunpowder, "Invisibilité Étendue Splash"));
    }

    private GuiTextField searchBox;
    private final List<ItemStack> allItems = new ArrayList<>();
    private final List<ItemStack> filtered = new ArrayList<>();
    private final List<ItemStack> historyStack = new ArrayList<>();
    private int scroll = 0;

    private enum Mode {LIST, CRAFT, BREWING, FURNACE}

    private Mode mode = Mode.LIST;
    private ItemStack selected;
    private IRecipe craftRecipe;
    private ItemStack furnaceInput;
    private List<PotionRecipe> brewRecipesForItem = new ArrayList<>();
    private int brewRecipeIndex = 0;
    private PotionRecipe currentBrewRecipe = null;

    private int panelX, panelY, gridX, gridY, sbX, sbY, sbH;
    private boolean draggingSB = false;
    private GuiButton btnBack, btnMenu;

    public GuiCraftGuide(GuiScreen parent) {
    }

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

        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
        gridX = panelX + PAD;
        gridY = panelY + GRID_OFF;
        sbX = gridX + GRID_W + SBP;
        sbY = gridY;
        sbH = ROWS * SZ;

        searchBox = new GuiTextField(0, fontRendererObj, panelX + PAD, panelY + SEARCH_Y, PANEL_W - PAD * 2, SEARCH_H);
        searchBox.setMaxStringLength(40);
        searchBox.setFocused(true);

        btnBack = new GuiButton(1, panelX + PAD + 2, panelY + PANEL_H - BTN_H - 2, BTN_W, BTN_H, "« Retour");
        btnMenu = new GuiButton(2, panelX + PANEL_W - PAD - BTN_W - 2, panelY + PANEL_H - BTN_H - 2, BTN_W, BTN_H, "Menu");

        if (allItems.isEmpty()) {
            for (Item item : Item.itemRegistry) {
                if (item == null) continue;
                // Les potions sont ajoutées séparément à partir de POTION_RECIPES
                if (item == Items.potionitem) continue;
                List<ItemStack> sub = new ArrayList<>();
                item.getSubItems(item, null, sub);
                for (ItemStack s : sub) if (s != null && !isBlocked(s)) allItems.add(s);
            }
            // Ajouter les potions depuis POTION_RECIPES pour garantir la correspondance des metas
            Set<Integer> addedPotionMetas = new HashSet<>();
            for (Map.Entry<Integer, List<PotionRecipe>> entry : POTION_RECIPES.entrySet()) {
                int meta = entry.getKey();
                if (addedPotionMetas.add(meta)) {
                    allItems.add(new ItemStack(Items.potionitem, 1, meta));
                }
            }
        }
        filter("");
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
        scroll = 0;
    }

    private int maxScroll() {
        return Math.max(0, (filtered.size() + COLS - 1) / COLS - ROWS);
    }

    private int thumbH() {
        int total = (filtered.size() + COLS - 1) / COLS;
        if (total <= ROWS) return sbH;
        return Math.max(18, sbH * ROWS / total);
    }

    private void applyScrollFromMouse(int my) {
        int th = thumbH(), ms = maxScroll();
        if (ms == 0) return;
        scroll = Math.round((float) (my - sbY - th / 2) * ms / (sbH - th));
        scroll = Math.max(0, Math.min(scroll, ms));
    }

    private boolean isSameStack(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;
        return a.getItemDamage() == b.getItemDamage();
    }

    private void openRecipe(ItemStack stack) {
        if (stack == null) return;
        // Si on clique sur l'item déjà sélectionné, ne rien faire
        if (selected != null && isSameStack(selected, stack)) return;
        ItemStack previousSelected = selected;
        loadRecipe(stack);
        if ((mode == Mode.CRAFT && craftRecipe != null) || (mode == Mode.BREWING && !brewRecipesForItem.isEmpty())) {
            if (previousSelected != null) {
                // Vérifier que l'item précédent n'est pas déjà au sommet de l'historique
                boolean alreadyTop = !historyStack.isEmpty() && isSameStack(historyStack.get(historyStack.size() - 1), previousSelected);
                if (!alreadyTop) {
                    historyStack.add(previousSelected);
                }
            }
        }
    }

    private void goBack() {
        if (!historyStack.isEmpty()) {
            loadRecipe(historyStack.remove(historyStack.size() - 1));
        } else {
            mode = Mode.LIST;
            selected = null;
        }
    }

    private void goMenu() {
        mode = Mode.LIST;
        selected = null;
        historyStack.clear();
    }

    private void loadRecipe(ItemStack stack) {
        selected = stack;
        craftRecipe = null;
        currentBrewRecipe = null;
        brewRecipesForItem.clear();
        brewRecipeIndex = 0;
        if (stack.getItem() == Items.potionitem) {
            int meta = stack.getItemDamage();
            if (POTION_RECIPES.containsKey(meta)) {
                brewRecipesForItem = new ArrayList<>(POTION_RECIPES.get(meta));
                brewRecipesForItem.sort(Comparator.comparingInt(a -> a.inputMeta));
                if (!brewRecipesForItem.isEmpty()) {
                    currentBrewRecipe = brewRecipesForItem.get(0);
                    mode = Mode.BREWING;
                    return;
                }
            }
        }
        for (IRecipe r : CraftingManager.getInstance().getRecipeList()) {
            ItemStack out = r.getRecipeOutput();
            if (out != null && out.getItem() == stack.getItem() && out.getItemDamage() == stack.getItemDamage()) {
                craftRecipe = r;
                break;
            }
        }

        if (craftRecipe == null) {
            Map<ItemStack, ItemStack> furnaceRecipes = FurnaceRecipes.instance().getSmeltingList();
            for (Map.Entry<ItemStack, ItemStack> entry : furnaceRecipes.entrySet()) {
                ItemStack out = entry.getValue();
                if (out != null && out.getItem() == stack.getItem() && out.getItemDamage() == stack.getItemDamage()) {
                    furnaceInput = entry.getKey();
                    break;
                }
            }
        }

        if (craftRecipe != null) mode = Mode.CRAFT;
        else if (furnaceInput != null) mode = Mode.FURNACE;
        else if (!brewRecipesForItem.isEmpty()) mode = Mode.BREWING;
        else mode = Mode.LIST;
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
                    if (src < sr.getRecipeItems().length) g[row * 3 + col] = fixWildcard(sr.getRecipeItems()[src]);
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

    private void drawButtonCustom(int x, int y, int w, int h, String text, boolean hover) {
        int bgCol = hover ? C_BTN_HV : C_BTN_BG;
        drawRect(x, y, x + w, y + h, bgCol);
        drawRect(x, y, x + w, y + 1, C_BORDER);
        drawRect(x, y + h - 1, x + w, y + h, C_BORDER2);
        drawCenteredString(fontRendererObj, text, x + w / 2, y + 3, 0xFFFFFFFF);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, C_BG);
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, C_PANEL);
        drawBorder(panelX, panelY, PANEL_W, PANEL_H, C_BORDER);
        drawRect(panelX + 4, panelY + TITLE_H - 1, panelX + PANEL_W - 4, panelY + TITLE_H, C_BORDER2);
        drawCenteredString(fontRendererObj, "§e§lGuide de Craft", panelX + PANEL_W / 2, panelY + 3, C_TEXT_TTL);

        if (mode == Mode.LIST) {
            searchBox.drawTextBox();
            drawList(mx, my);
        } else {
            drawRecipeMode(mx, my);
        }
        super.drawScreen(mx, my, pt);
    }

    private void drawBorder(int x, int y, int w, int h, int col) {
        drawRect(x, y, x + w, y + 1, col);
        drawRect(x, y + h - 1, x + w, y + h, col);
        drawRect(x, y, x + 1, y + h, col);
        drawRect(x + w - 1, y, x + w, y + h, col);
    }

    private void drawSlot(int x, int y) {
        drawRect(x, y, x + SZ, y + SZ, C_SLOT);
        drawRect(x, y, x + SZ, y + 1, C_BORDER2);
        drawRect(x, y, x + 1, y + SZ, C_BORDER2);
    }

    private void drawList(int mx, int my) {
        int ms = maxScroll();
        drawRect(sbX, sbY, sbX + SBW, sbY + sbH, C_SCR_BG);
        drawBorder(sbX, sbY, SBW, sbH, C_BORDER2);
        if (ms > 0) {
            int th = thumbH();
            int ty = sbY + scroll * (sbH - th) / ms;
            drawRect(sbX + 1, ty, sbX + SBW - 1, ty + th,
                    (inside(mx, my, sbX, sbY, SBW, sbH) || draggingSB) ? C_SCR_HL : C_SCR_FG);
        }

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();

        int base = scroll * COLS;
        ItemStack hov = null;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int i = base + row * COLS + col;
                if (i >= filtered.size()) break;
                ItemStack s = filtered.get(i);
                int x = gridX + col * SZ, y = gridY + row * SZ;
                drawSlot(x, y);
                itemRender.renderItemAndEffectIntoGUI(s, x + 1, y + 1);
                itemRender.renderItemOverlayIntoGUI(fontRendererObj, s, x + 1, y + 1, null);
                if (inside(mx, my, x, y, SZ, SZ)) {
                    drawRect(x, y, x + SZ, y + SZ, C_SLOT_HOV);
                    hov = s;
                }
            }
        }

        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();

        if (hov != null) renderToolTip(hov, mx, my);
        if (filtered.isEmpty())
            drawCenteredString(fontRendererObj, "Aucun item", panelX + PANEL_W / 2, gridY + ROWS * SZ / 2, C_HINT);
    }

    private void drawRecipeMode(int mx, int my) {
        if (selected == null) return;
        int cx = panelX + PANEL_W / 2;

        drawRect(panelX + 4, panelY + TITLE_H - 1, panelX + PANEL_W - 4, panelY + TITLE_H, C_BORDER2);

        int rpY = panelY + TITLE_H + 2;
        int rpH = PANEL_H - TITLE_H - 2;
        drawRect(panelX + 4, rpY, panelX + PANEL_W - 4, rpY + rpH - 2, C_PANEL2);

        int iconX = panelX + PAD + 4, iconY = rpY + 4;
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        itemRender.renderItemAndEffectIntoGUI(selected, iconX, iconY);
        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();

        String name = selected.getDisplayName();
        int maxNW = panelX + PANEL_W - PAD - (iconX + SZ + 4);
        if (fontRendererObj.getStringWidth(name) > maxNW)
            name = fontRendererObj.trimStringToWidth(name, maxNW) + "..";
        drawString(fontRendererObj, "§e" + name, iconX + SZ + 4, iconY + 1, C_TEXT_TTL);

        String badge = mode == Mode.BREWING ? "[ Alambic ]" : mode == Mode.FURNACE ? " [ Four ] " : "[ Craft ]";
        drawString(fontRendererObj, badge, iconX + SZ + 4, iconY + SZ - 2, C_TEXT_DTL);

        int sep2 = rpY + 4 + SZ + 6;
        drawRect(panelX + 8, sep2, panelX + PANEL_W - 8, sep2 + 1, C_BORDER2);

        int contentY = sep2 + 8;
        ItemStack hovItem = null;

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();

        if (mode == Mode.BREWING && currentBrewRecipe != null) {
            hovItem = drawBrewingRecipe(mx, my, cx, contentY);
        } else if (craftRecipe != null) {
            hovItem = drawCraftGrid(mx, my, cx, contentY);
        } else if (furnaceInput != null) {
            hovItem = drawFurnaceRecipe(mx, my, cx, contentY);
        }

        GlStateManager.disableDepth();
        RenderHelper.disableStandardItemLighting();

        if (hovItem != null) renderToolTip(hovItem, mx, my);

        boolean backHov = inside(mx, my, btnBack.xPosition, btnBack.yPosition, btnBack.getButtonWidth(), btnBack.getButtonHeight());
        boolean menuHov = inside(mx, my, btnMenu.xPosition, btnMenu.yPosition, btnMenu.getButtonWidth(), btnMenu.getButtonHeight());
        drawButtonCustom(btnBack.xPosition, btnBack.yPosition, btnBack.getButtonWidth(), btnBack.getButtonHeight(), btnBack.displayString, backHov);
        drawButtonCustom(btnMenu.xPosition, btnMenu.yPosition, btnMenu.getButtonWidth(), btnMenu.getButtonHeight(), btnMenu.displayString, menuHov);
    }

    private ItemStack drawFurnaceRecipe(int mx, int my, int cx, int top) {
        int lx = cx - (SZ + 20 + SZ) / 2;
        ItemStack hov = null;

        ItemStack input = fixWildcard(furnaceInput);
        drawSlot(lx, top);
        itemRender.renderItemAndEffectIntoGUI(input, lx + 1, top + 1);
        if (inside(mx, my, lx, top, SZ, SZ)) {
            drawRect(lx, top, lx + SZ, top + SZ, C_SLOT_SEL);
            hov = input;
        }

        drawCenteredString(fontRendererObj, "§6▶", lx + SZ + 10, top + 5, 0xFFFFFFFF);

        int rx = lx + SZ + 20;
        drawSlot(rx, top);
        drawBorder(rx, top, SZ, SZ, C_BORDER);
        itemRender.renderItemAndEffectIntoGUI(selected, rx + 1, top + 1);
        if (inside(mx, my, rx, top, SZ, SZ)) hov = selected;

        return hov;
    }

    private ItemStack drawCraftGrid(int mx, int my, int cx, int top) {
        ItemStack[] grid = getGrid(craftRecipe);
        int gw = 3 * SZ;
        int totalW = gw + 16 + SZ;
        int sx0 = cx - totalW / 2;
        ItemStack hov = null;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                int sx = sx0 + col * SZ, sy = top + row * SZ;
                drawSlot(sx, sy);
                if (grid[idx] != null) {
                    itemRender.renderItemAndEffectIntoGUI(grid[idx], sx + 1, sy + 1);
                    if (inside(mx, my, sx, sy, SZ, SZ)) {
                        drawRect(sx, sy, sx + SZ, sy + SZ, C_SLOT_SEL);
                        hov = grid[idx];
                    }
                }
            }
        }

        drawString(fontRendererObj, "§6»", sx0 + gw + 3, top + SZ + 4, 0xFFFFFFFF);

        int rx = sx0 + gw + 16, ry = top + SZ;
        drawSlot(rx, ry);
        drawBorder(rx, ry, SZ, SZ, C_BORDER);
        ItemStack out = craftRecipe.getRecipeOutput();
        itemRender.renderItemAndEffectIntoGUI(out, rx + 1, ry + 1);
        itemRender.renderItemOverlayIntoGUI(fontRendererObj, out, rx + 1, ry + 1, null);

        return hov;
    }

    private static final int BREW_GUI_W = 75;
    private static final int BREW_GUI_H = 55;

    private int[] brewSlotPositions(int bx, int by) {
        int ingX = bx + BREW_GUI_W / 2 - SZ / 2;
        int ingY = by + 8;
        int inX = bx + 6;
        int inY = by + BREW_GUI_H - SZ - 8;
        int outX = bx + BREW_GUI_W - SZ - 6;
        return new int[]{ingX, ingY, inX, inY, outX, inY};
    }

    private ItemStack drawBrewingRecipe(int mx, int my, int cx, int top) {
        if (currentBrewRecipe == null) return null;

        int bx = cx - BREW_GUI_W / 2;
        int[] s = brewSlotPositions(bx, top);
        int ingX = s[0], ingY = s[1], inX = s[2], inY = s[3], outX = s[4], outY = s[5];
        ItemStack hov = null;

        drawRect(bx, top, bx + BREW_GUI_W, top + BREW_GUI_H, C_PANEL2);
        drawBorder(bx, top, BREW_GUI_W, BREW_GUI_H, C_BORDER2);

        // Ingrédient
        drawSlot(ingX, ingY);
        itemRender.renderItemAndEffectIntoGUI(currentBrewRecipe.ingredient, ingX + 1, ingY + 1);
        if (inside(mx, my, ingX, ingY, SZ, SZ)) {
            drawRect(ingX, ingY, ingX + SZ, ingY + SZ, C_SLOT_SEL);
            hov = currentBrewRecipe.ingredient;
        }

        drawCenteredString(fontRendererObj, "§6↓", ingX + SZ / 2, ingY + SZ + 1, 0xFF7A8BA5);

        // Input potion
        ItemStack inputPotion = new ItemStack(Items.potionitem, 1, currentBrewRecipe.inputMeta);
        drawSlot(inX, inY);
        itemRender.renderItemAndEffectIntoGUI(inputPotion, inX + 1, inY + 1);
        if (inside(mx, my, inX, inY, SZ, SZ)) {
            drawRect(inX, inY, inX + SZ, inY + SZ, C_SLOT_SEL);
            hov = inputPotion;
        }

        drawString(fontRendererObj, "§6→", inX + SZ + 2, inY + SZ / 2 - 2, 0xFFFFB347);

        // Output potion
        ItemStack outputPotion = new ItemStack(Items.potionitem, 1, currentBrewRecipe.outputMeta);
        drawSlot(outX, outY);
        drawBorder(outX, outY, SZ, SZ, C_BORDER);
        itemRender.renderItemAndEffectIntoGUI(outputPotion, outX + 1, outY + 1);
        if (inside(mx, my, outX, outY, SZ, SZ)) hov = outputPotion;

        if (brewRecipesForItem.size() > 1) {
            String recipeIndicator = (brewRecipeIndex + 1) + "/" + brewRecipesForItem.size();
            drawCenteredString(fontRendererObj, "§7" + recipeIndicator, bx + BREW_GUI_W / 2, top - 8, C_TEXT_DTL);
        }

        return hov;
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        searchBox.mouseClicked(mx, my, btn);

        if (mode != Mode.LIST) {
            if (inside(mx, my, btnBack.xPosition, btnBack.yPosition, btnBack.getButtonWidth(), btnBack.getButtonHeight())) {
                goBack();
                return;
            }
            if (inside(mx, my, btnMenu.xPosition, btnMenu.yPosition, btnMenu.getButtonWidth(), btnMenu.getButtonHeight())) {
                goMenu();
                return;
            }

            int rpY = panelY + TITLE_H + 2;
            int sep2 = rpY + 4 + SZ + 6;
            int contentY = sep2 + 8;
            int cx = panelX + PANEL_W / 2;

            if (btn == 0) {
                if (mode == Mode.CRAFT && craftRecipe != null) {
                    ItemStack[] grid = getGrid(craftRecipe);
                    int sx0 = cx - (3 * SZ + 16 + SZ) / 2;
                    for (int row = 0; row < 3; row++)
                        for (int col = 0; col < 3; col++) {
                            int idx = row * 3 + col;
                            if (idx >= grid.length || grid[idx] == null) continue;
                            if (inside(mx, my, sx0 + col * SZ, contentY + row * SZ, SZ, SZ)) {
                                openRecipe(grid[idx]);
                                return;
                            }
                        }
                }

                if (mode == Mode.FURNACE && furnaceInput != null) {
                    int lx = cx - (SZ + 20 + SZ) / 2;
                    if (inside(mx, my, lx, contentY, SZ, SZ)) {
                        openRecipe(furnaceInput);
                        return;
                    }
                }

                if (mode == Mode.BREWING && currentBrewRecipe != null) {
                    int bx = cx - BREW_GUI_W / 2;
                    int[] sp = brewSlotPositions(bx, contentY);
                    int ingX = sp[0], ingY = sp[1], inX = sp[2], inY = sp[3];

                    if (inside(mx, my, ingX, ingY, SZ, SZ)) {
                        openRecipe(currentBrewRecipe.ingredient);
                        return;
                    }
                    if (inside(mx, my, inX, inY, SZ, SZ)) {
                        openRecipe(new ItemStack(Items.potionitem, 1, currentBrewRecipe.inputMeta));
                        return;
                    }
                }

            } else if (btn == 1 && mode == Mode.BREWING && brewRecipesForItem.size() > 1) {
                brewRecipeIndex = (brewRecipeIndex + 1) % brewRecipesForItem.size();
                currentBrewRecipe = brewRecipesForItem.get(brewRecipeIndex);
                return;
            }
            return;
        }

        if (btn == 0) {
            if (inside(mx, my, sbX, sbY, SBW, sbH)) {
                draggingSB = true;
                applyScrollFromMouse(my);
                return;
            }
            int base = scroll * COLS;
            for (int row = 0; row < ROWS; row++)
                for (int col = 0; col < COLS; col++) {
                    int i = base + row * COLS + col;
                    if (i >= filtered.size()) return;
                    if (inside(mx, my, gridX + col * SZ, gridY + row * SZ, SZ, SZ)) {
                        openRecipe(filtered.get(i));
                        return;
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
        if (mode != Mode.LIST) return;
        int w = Mouse.getEventDWheel(), ms = maxScroll();
        if (w < 0 && scroll < ms) scroll++;
        if (w > 0 && scroll > 0) scroll--;
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
