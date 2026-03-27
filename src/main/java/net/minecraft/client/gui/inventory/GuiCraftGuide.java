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
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

/**
 * GuiCraftGuide — Ultimate Alchemist & Crafter Edition
 *
 * Optimized for MCP 1.8.9 with:
 * · Robust Potion Recipe Navigation (Fixes Splash -> Normal transition)
 * · Enhanced Visuals (Glow, Alchemic Flows, Smooth Scroll)
 * · Improved Ergonomics (Right-click to go back, Tooltip hints, Keyboard support)
 */
public class GuiCraftGuide extends GuiScreen {

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final int C_OVERLAY = 0xCC030511;
    private static final int C_PANEL = 0xFF0B0C17;
    private static final int C_PANEL_HDR = 0xFF080A14;
    private static final int C_PANEL_INNER = 0xFF0E0F1C;
    private static final int C_RECIPE_BG = 0xFF09090F;
    private static final int C_ACCENT = 0xFF3D8EFF;
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

    // ── Grid constants ─────────────────────────────────────────────────────────
    private int COLS = 9;
    private int SZ = 18;
    private static final int SBW = 6;
    private static final int SBP = 4;

    // ── Layout ───────────────────────────────────────────────────────────
    private int pad, panelW, panelH, panelX, panelY;
    private int gridX, gridY, sbX, sbY, sbH, GRID_W;
    private int titleH, searchY, searchH, gridOff, btnW, btnH;
    private int visibleRows;
    private int footerY;

    // ── Animations ─────────────────────────────────────────────────────────────
    private float smoothScroll = 0f;
    private int targetScroll = 0;
    private float searchFocusAnim = 0f;

    // ── Recipe system ──────────────────────────────────────────────────────────
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
    static final int META_HASTE1 = 9001;
    static final int META_HASTE2 = 9002;
    static final int META_HASTE1_SPLASH = 9003;
    static final int META_HASTE2_SPLASH = 9004;
    static final int META_FALL1 = 9005;
    static final int META_FALL2 = 9006;
    static final int META_FALL1_SPLASH = 9007;
    static final int META_FALL2_SPLASH = 9008;
    static final Map<Integer, ItemStack> CUSTOM_POTION_STACKS = new HashMap<>();
    static final Map<String, Integer> CUSTOM_POTION_SIGNATURES = new HashMap<>();

    static {
        ItemStack glowstone = new ItemStack(Items.glowstone_dust);
        ItemStack redstone = new ItemStack(Items.redstone);
        ItemStack gunpowder = new ItemStack(Items.gunpowder);
        ItemStack fermented = new ItemStack(Items.fermented_spider_eye);
        ItemStack feather = new ItemStack(Items.feather);
        ItemStack diamBlock = new ItemStack(net.minecraft.init.Blocks.diamond_block);

        POTION_RECIPES.computeIfAbsent(16, k -> new ArrayList<>()).add(new PotionRecipe(0, 16, new ItemStack(Items.nether_wart), "Potion Étrange"));

        addPotionSet(8193, new ItemStack(Items.ghast_tear), "Régénération", glowstone, redstone, gunpowder);
        addPotionSet(8194, new ItemStack(Items.sugar), "Rapidité", glowstone, redstone, gunpowder);
        addPotionSet(8195, new ItemStack(Items.magma_cream), "Résistance au Feu", null, redstone, gunpowder);
        addPotionSet(8196, new ItemStack(Items.spider_eye), "Poison", glowstone, redstone, gunpowder);
        addPotionSet(8197, new ItemStack(Items.speckled_melon), "Soin", glowstone, null, gunpowder);
        addPotionSet(8198, new ItemStack(Items.golden_carrot), "Vision Nocturne", null, redstone, gunpowder);
        addPotionSet(8201, new ItemStack(Items.blaze_powder), "Force", glowstone, redstone, gunpowder);
        addPotionSet(8203, new ItemStack(Items.rabbit_foot), "Saut", glowstone, redstone, gunpowder);
        addPotionSet(8205, new ItemStack(Items.fish, 1, 3), "Respiration Aquatique", null, redstone, gunpowder);

        POTION_RECIPES.computeIfAbsent(8200, k -> new ArrayList<>()).add(new PotionRecipe(0, 8200, fermented, "Faiblesse"));
        POTION_RECIPES.computeIfAbsent(8264, k -> new ArrayList<>()).add(new PotionRecipe(8200, 8264, redstone, "Faiblesse Étendue"));
        POTION_RECIPES.computeIfAbsent(16392, k -> new ArrayList<>()).add(new PotionRecipe(8200, 16392, gunpowder, "Faiblesse Jetable"));
        POTION_RECIPES.computeIfAbsent(16456, k -> new ArrayList<>()).add(new PotionRecipe(8264, 16456, gunpowder, "Faiblesse Étendue Jetable"));

        POTION_RECIPES.computeIfAbsent(8202, k -> new ArrayList<>()).add(new PotionRecipe(8194, 8202, fermented, "Lenteur"));
        POTION_RECIPES.computeIfAbsent(8202, k -> new ArrayList<>()).add(new PotionRecipe(8203, 8202, fermented, "Lenteur"));
        POTION_RECIPES.computeIfAbsent(8204, k -> new ArrayList<>()).add(new PotionRecipe(8197, 8204, fermented, "Dégâts Instantanés"));
        POTION_RECIPES.computeIfAbsent(8206, k -> new ArrayList<>()).add(new PotionRecipe(8198, 8206, fermented, "Invisibilité"));

        // Custom Potions
        POTION_RECIPES.computeIfAbsent(META_HASTE1, k -> new ArrayList<>()).add(new PotionRecipe(16, META_HASTE1, diamBlock, "Célérité"));
        POTION_RECIPES.computeIfAbsent(META_HASTE2, k -> new ArrayList<>()).add(new PotionRecipe(META_HASTE1, META_HASTE2, glowstone, "Célérité II"));
        POTION_RECIPES.computeIfAbsent(META_HASTE1_SPLASH, k -> new ArrayList<>()).add(new PotionRecipe(META_HASTE1, META_HASTE1_SPLASH, gunpowder, "Célérité Jetable"));
        POTION_RECIPES.computeIfAbsent(META_HASTE2_SPLASH, k -> new ArrayList<>()).add(new PotionRecipe(META_HASTE2, META_HASTE2_SPLASH, gunpowder, "Célérité II Jetable"));
        POTION_RECIPES.computeIfAbsent(META_FALL1, k -> new ArrayList<>()).add(new PotionRecipe(16, META_FALL1, feather, "Fall Protection"));
        POTION_RECIPES.computeIfAbsent(META_FALL2, k -> new ArrayList<>()).add(new PotionRecipe(META_FALL1, META_FALL2, glowstone, "Fall Protection II"));
        POTION_RECIPES.computeIfAbsent(META_FALL1_SPLASH, k -> new ArrayList<>()).add(new PotionRecipe(META_FALL1, META_FALL1_SPLASH, gunpowder, "Fall Protection Jetable"));
        POTION_RECIPES.computeIfAbsent(META_FALL2_SPLASH, k -> new ArrayList<>()).add(new PotionRecipe(META_FALL2, META_FALL2_SPLASH, gunpowder, "Fall Protection II Jetable"));

        CUSTOM_POTION_STACKS.put(META_HASTE1, ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id, 0, 3600, "Potion de Célérité"));
        CUSTOM_POTION_STACKS.put(META_HASTE2, ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id, 1, 1800, "Potion de Célérité II"));
        CUSTOM_POTION_STACKS.put(META_HASTE1_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id, 0, 3600, "Potion de Célérité Jetable", true));
        CUSTOM_POTION_STACKS.put(META_HASTE2_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.digSpeed.id, 1, 1800, "Potion de Célérité II Jetable", true));
        CUSTOM_POTION_STACKS.put(META_FALL1, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id, 0, 3600, "Potion de Fall Protection"));
        CUSTOM_POTION_STACKS.put(META_FALL2, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id, 1, 1800, "Potion de Fall Protection II"));
        CUSTOM_POTION_STACKS.put(META_FALL1_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id, 0, 3600, "Potion de Fall Protection Jetable", true));
        CUSTOM_POTION_STACKS.put(META_FALL2_SPLASH, ItemPotion.createCustomPotion(Items.potionitem, Potion.fallProtection.id, 1, 1800, "Potion de Fall Protection II Jetable", true));

        for (Map.Entry<Integer, ItemStack> e : CUSTOM_POTION_STACKS.entrySet()) {
            String sig = buildPotionSignature(e.getValue());
            if (!sig.isEmpty()) CUSTOM_POTION_SIGNATURES.put(sig, e.getKey());
        }
    }

    private static void addPotionSet(int base, ItemStack ing, String name, ItemStack glow, ItemStack red, ItemStack gun) {
        POTION_RECIPES.computeIfAbsent(base, k -> new ArrayList<>()).add(new PotionRecipe(16, base, ing, name));
        if (glow != null) POTION_RECIPES.computeIfAbsent(base + 32, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 32, glow, name + " II"));
        if (red != null) POTION_RECIPES.computeIfAbsent(base + 64, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 64, red, name + " Étendue"));
        if (gun != null) {
            POTION_RECIPES.computeIfAbsent(base + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base, base + 8192, gun, name + " Jetable"));
            if (glow != null) POTION_RECIPES.computeIfAbsent(base + 32 + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 32, base + 32 + 8192, gun, name + " II Jetable"));
            if (red != null) POTION_RECIPES.computeIfAbsent(base + 64 + 8192, k -> new ArrayList<>()).add(new PotionRecipe(base + 64, base + 64 + 8192, gun, name + " Étendue Jetable"));
        }
    }

    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
            "tile.litFurnace", "tile.farmland", "tile.doorWood", "tile.doorIron", "tile.litRedstoneOre", "tile.redstoneLamp.on", "tile.pistonHead",
            "tile.pistonMoving", "tile.cake", "tile.skull", "tile.carrots", "tile.potatoes", "tile.wheat", "tile.netherWart", "tile.cocoa",
            "tile.pumpkinStem", "tile.melonStem", "tile.tripWire", "tile.beetroots", "tile.portal", "tile.endPortal", "tile.endGateway",
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
    private ItemStack initialStack = null;
    private IRecipe craftRecipe;
    private ItemStack furnaceInput;
    private List<PotionRecipe> brewRecipesForItem = new ArrayList<>();
    private int brewRecipeIndex = 0;
    private PotionRecipe currentBrewRecipe = null;
    private GuiButton btnBack, btnClose;

    public GuiCraftGuide(GuiScreen parent) {}
    public GuiCraftGuide(GuiScreen parent, ItemStack initial) { this(parent); this.initialStack = initial; }

    @Override
    public void initGui() {
        buttonList.clear();
        mode = Mode.LIST;
        selected = null;

        SZ = GuiRenderUtils.clamp(height / 18, 16, 22);
        COLS = GuiRenderUtils.clamp((width - 60) / SZ, 7, 10);
        visibleRows = GuiRenderUtils.clamp((height - 150) / SZ, 4, 7);
        GRID_W = COLS * SZ;
        pad = Math.max(12, width / 60);
        titleH = 24;
        searchH = 18;
        searchY = titleH + 6;
        gridOff = searchY + searchH + 8;
        btnH = 18;
        btnW = Math.max(60, width / 10);

        panelW = pad + GRID_W + SBP + SBW + pad;
        panelH = gridOff + visibleRows * SZ + 12 + btnH + 20;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        gridX = panelX + pad;
        gridY = panelY + gridOff;
        sbX = gridX + GRID_W + SBP;
        sbY = gridY;
        sbH = visibleRows * SZ;
        footerY = gridY + sbH + 4;

        btnBack = new GuiButton(1, panelX + pad, panelY + panelH - btnH - 10, btnW, btnH, "Retour");
        btnClose = new GuiButton(2, panelX + panelW - pad - btnW, panelY + panelH - btnH - 10, btnW, btnH, "Fermer");

        searchBox = new GuiTextField(0, fontRendererObj, panelX + pad + 18, panelY + searchY, panelW - pad * 2 - SBP - SBW - 18, searchH);
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
            for (int meta : POTION_RECIPES.keySet()) {
                if (added.add(meta)) {
                    allItems.add(getPotionStack(meta));
                }
            }
        }
        filter("");
        if (initialStack != null) { loadRecipe(initialStack); initialStack = null; }
    }

    // ── Drawing Helpers ────────────────────────────────────────────────────────

    private void drawSlot3D(int x, int y) {
        drawRect(x, y, x + SZ, y + SZ, C_SLOT);
        drawRect(x, y, x + SZ, y + 1, C_SLOT_LT);
        drawRect(x, y, x + 1, y + SZ, C_SLOT_LT);
        drawRect(x + SZ - 1, y, x + SZ, y + SZ, C_SLOT_DK);
        drawRect(x, y + SZ - 1, x + SZ, y + SZ, C_SLOT_DK);
    }

    private void drawSlotOutput(int x, int y, int borderColor) {
        drawSlot3D(x, y);
        int base = borderColor & 0x00FFFFFF;
        drawRect(x - 2, y - 2, x + SZ + 2, y + SZ + 2, 0x15000000 | base);
        drawRect(x, y, x + SZ, y + 1, borderColor);
        drawRect(x, y, x + 1, y + SZ, borderColor);
        drawRect(x + SZ - 1, y, x + SZ, y + SZ, C_BORDER_DIM);
        drawRect(x, y + SZ - 1, x + SZ, y + SZ, C_BORDER_DIM);
    }

    private void drawSlotHover(int x, int y) {
        long t = System.currentTimeMillis() % 1200;
        float p = t < 600 ? t / 600f : (1200f - t) / 600f;
        drawRect(x, y, x + SZ, y + SZ, ((int)(0x30 + 0x30 * p) << 24) | 0x3D8EFF);
        drawRect(x, y, x + SZ, y + 1, ((int)(0x60 + 0x40 * p) << 24) | 0x3D8EFF);
    }

    private void drawNavButton(int x, int y, int w, int h, String text, boolean hover, int accentCol) {
        GuiRenderUtils.drawStyledButton(x, y, w, h, hover ? C_BTN_HOV : C_BTN, hover ? accentCol : C_BTN_BDR, hover);
        drawCenteredString(fontRendererObj, text, x + w / 2, y + (h - 8) / 2, hover ? 0xFFFFFFFF : 0xFFAABBCC);
    }

    private void drawModeBadge(int x, int y, String label, int accent) {
        int tw = fontRendererObj.getStringWidth(label);
        drawRect(x, y, x + tw + 8, y + 12, 0x30000000 | (accent & 0xFFFFFF));
        drawRect(x, y, x + tw + 8, y + 1, accent);
        drawRect(x, y, x + 1, y + 12, accent);
        fontRendererObj.drawStringWithShadow(label, x + 4, y + 2, accent);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        smoothScroll = GuiRenderUtils.lerp(smoothScroll, targetScroll, 0.25f);
        searchFocusAnim = GuiRenderUtils.lerp(searchFocusAnim, (searchBox.isFocused() ? 1f : 0f), 0.15f);

        drawRect(0, 0, width, height, C_OVERLAY);
        if (mode == Mode.LIST) {
            GuiRenderUtils.drawRoundedPanel(panelX, panelY, panelW, panelH, C_PANEL, C_PANEL_HDR, titleH, modeAccent());
            drawRect(panelX + 1, panelY + titleH + 1, panelX + panelW - 1, panelY + panelH - 1, C_PANEL_INNER);
            fontRendererObj.drawStringWithShadow("\u00a7l\u2726 Guide de Craft", panelX + pad, panelY + (titleH - 8) / 2, C_TXT_TITLE);
            String countStr = filtered.size() + " items";
            fontRendererObj.drawStringWithShadow("\u00a77" + countStr, panelX + panelW - pad - fontRendererObj.getStringWidth(countStr), panelY + (titleH - 8) / 2, C_TXT_DIM);
            drawSearchBox(mx, my);
            drawItemGrid(mx, my);
        } else {
            drawRecipeMode(mx, my);
        }
        super.drawScreen(mx, my, pt);
    }

    private void drawSearchBox(int mx, int my) {
        int sx = panelX + pad, sy = panelY + searchY, sw = panelW - pad * 2 - SBP - SBW;
        drawRect(sx, sy, sx + sw, sy + searchH, 0xFF080A14);
        int focusColor = GuiRenderUtils.colorLerp(C_BORDER_MID, C_ACCENT, searchFocusAnim);
        drawRect(sx, sy + searchH - 1, sx + sw, sy + searchH, focusColor);
        GuiRenderUtils.drawSearchIcon(sx + 4, sy + (searchH - 10) / 2 + 1, GuiRenderUtils.colorLerp(C_TXT_HINT, C_ACCENT, searchFocusAnim));
        if (searchBox.getText().isEmpty()) fontRendererObj.drawStringWithShadow("\u00a7o Rechercher...", sx + 18, sy + (searchH - 8) / 2, C_TXT_HINT);
        searchBox.drawTextBox();
    }

    private void drawItemGrid(int mx, int my) {
        drawRect(sbX, sbY, sbX + SBW, sbY + sbH, C_SCR_TRACK);
        int ms = maxScroll();
        if (ms > 0) {
            int th = thumbH();
            int ty = sbY + (int) ((smoothScroll / ms) * (sbH - th));
            drawRect(sbX + 1, ty, sbX + SBW - 1, ty + th, (inside(mx, my, sbX, sbY, SBW, sbH) || draggingSB) ? C_SCR_ACTIVE : C_SCR_THUMB);
        }
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableDepth();
        int scrollRow = (int) smoothScroll;
        int pixelOffset = (int) ((smoothScroll - scrollRow) * SZ);
        ItemStack hov = null;
        for (int row = 0; row <= visibleRows; row++) {
            for (int col = 0; col < COLS; col++) {
                int i = scrollRow * COLS + row * COLS + col;
                if (i >= filtered.size()) break;
                int x = gridX + col * SZ, y = gridY + row * SZ - pixelOffset;
                if (y + SZ <= gridY || y >= gridY + sbH) continue;
                drawSlot3D(x, y);
                itemRender.renderItemAndEffectIntoGUI(filtered.get(i), x + 1, y + 1);
                // Correction : Highlight pour les slots partiellement visibles
                if (inside(mx, my, x, y, SZ, SZ) && y + SZ > gridY && y < gridY + sbH) {
                    drawSlotHover(x, y);
                    hov = filtered.get(i);
                }
            }
        }
        GlStateManager.disableDepth(); RenderHelper.disableStandardItemLighting();
        if (hov != null) renderToolTip(hov, mx, my);
    }

    private void drawRecipeMode(int mx, int my) {
        if (selected == null) return;
        int accent = modeAccent();
        int recipeAreaH = (mode == Mode.BREWING) ? (BREW_H + 30) : (mode == Mode.CRAFT ? 3 * SZ + 30 : SZ + 30);
        int recPanelH = Math.max(panelH, titleH + 10 + 15 + recipeAreaH + 40);
        int recPanelY = (height - recPanelH) / 2;

        GuiRenderUtils.drawRoundedPanel(panelX, recPanelY, panelW, recPanelH, C_PANEL, C_PANEL_HDR, titleH, accent);
        drawRect(panelX + 1, recPanelY + titleH + 1, panelX + panelW - 1, recPanelY + recPanelH - 1, C_PANEL_INNER);

        // Header Item & Title
        int iconX = panelX + pad, iconY = recPanelY + (titleH - SZ) / 2;
        RenderHelper.enableGUIStandardItemLighting(); GlStateManager.enableDepth();
        itemRender.renderItemAndEffectIntoGUI(selected, iconX, iconY);
        GlStateManager.disableDepth(); RenderHelper.disableStandardItemLighting();

        String name = selected.getDisplayName();
        int maxNameW = panelW - pad * 2 - SZ - 90;
        if (fontRendererObj.getStringWidth(name) > maxNameW) name = fontRendererObj.trimStringToWidth(name, maxNameW) + "..";
        fontRendererObj.drawStringWithShadow("\u00a7f\u00a7l" + name, iconX + SZ + 6, recPanelY + (titleH - 8) / 2, 0xFFFFFFFF);

        String badge = mode == Mode.BREWING ? " ALCHIMIE " : mode == Mode.FURNACE ? " CUISSON " : " ATELIER ";
        drawModeBadge(panelX + panelW - pad - fontRendererObj.getStringWidth(badge) - 8, recPanelY + (titleH - 12) / 2, badge, accent);

        int curY = recPanelY + titleH + 12;
        GuiRenderUtils.drawGradientRect(panelX + pad, curY, panelX + panelW - pad, curY + 1, 0x00000000, (accent & 0x60FFFFFF));
        curY += 14;

        int cx = panelX + panelW / 2;
        ItemStack hovItem = null;
        RenderHelper.enableGUIStandardItemLighting(); GlStateManager.enableDepth();
        if (mode == Mode.BREWING) hovItem = drawBrewingRecipe(mx, my, cx, curY);
        else if (mode == Mode.CRAFT) hovItem = drawCraftGrid(mx, my, cx, curY);
        else if (mode == Mode.FURNACE) hovItem = drawFurnaceRecipe(mx, my, cx, curY);
        GlStateManager.disableDepth(); RenderHelper.disableStandardItemLighting();

        if (hovItem != null) renderToolTip(hovItem, mx, my);

        int btnY = recPanelY + recPanelH - btnH - 10;
        btnBack.yPosition = btnY; btnBack.xPosition = panelX + pad;
        btnClose.yPosition = btnY; btnClose.xPosition = panelX + panelW - pad - btnW;
        drawNavButton(btnBack.xPosition, btnBack.yPosition, btnW, btnH, "\u25C4 Retour", inside(mx, my, btnBack.xPosition, btnBack.yPosition, btnW, btnH), accent);
        drawNavButton(btnClose.xPosition, btnClose.yPosition, btnW, btnH, "Fermer", inside(mx, my, btnClose.xPosition, btnClose.yPosition, btnW, btnH), C_ACCENT);

        if (!historyStack.isEmpty()) {
            drawCenteredString(fontRendererObj, "\u00a78" + historyStack.size() + " en historique", cx, btnY + 5, C_TXT_DIM);
        }
    }

    private ItemStack drawCraftGrid(int mx, int my, int cx, int top) {
        ItemStack[] grid = getGrid(craftRecipe);
        int gw = 3 * SZ, sx0 = cx - (gw + 30 + SZ) / 2;
        ItemStack hov = null;
        drawRect(sx0 - 6, top - 6, sx0 + gw + 10, top + 3 * SZ + 6, C_RECIPE_BG);
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            int sx = sx0 + c * SZ, sy = top + r * SZ, i = r * 3 + c;
            drawRect(sx, sy, sx + SZ, sy + SZ, C_SLOT);
            drawRect(sx, sy, sx + SZ, sy + 1, C_SLOT_LT);
            drawRect(sx, sy, sx + 1, sy + SZ, C_SLOT_LT);
            drawRect(sx + SZ - 1, sy, sx + SZ, sy + SZ, C_SLOT_DK);
            drawRect(sx, sy + SZ - 1, sx + SZ, sy + SZ, C_SLOT_DK);

            if (grid[i] != null) {
                itemRender.renderItemAndEffectIntoGUI(grid[i], sx + 1, sy + 1);
                if (inside(mx, my, sx, sy, SZ, SZ)) { drawSlotHover(sx, sy); hov = grid[i]; }
            }
        }
        GuiRenderUtils.drawChevronArrow(sx0 + gw + 8, top + SZ + SZ / 2, 6, 0xE8A030, (System.currentTimeMillis() % 1000) / 1000f);
        int rx = sx0 + gw + 26, ry = top + SZ;
        drawSlotOutput(rx, ry, C_GOLD);
        ItemStack out = craftRecipe.getRecipeOutput();
        itemRender.renderItemAndEffectIntoGUI(out, rx + 1, ry + 1);
        if (inside(mx, my, rx, ry, SZ, SZ)) { drawSlotHover(rx, ry); hov = out; }
        return hov;
    }

    private ItemStack drawFurnaceRecipe(int mx, int my, int cx, int top) {
        int lx = cx - (SZ + 32 + SZ) / 2;
        ItemStack hov = null;
        drawRect(lx - 6, top - 6, lx + SZ + 32 + SZ + 6, top + SZ + 6, C_RECIPE_BG);
        ItemStack input = fixWildcard(furnaceInput);
        drawSlot3D(lx, top);
        itemRender.renderItemAndEffectIntoGUI(input, lx + 1, top + 1);
        if (inside(mx, my, lx, top, SZ, SZ)) { drawSlotHover(lx, top); hov = input; }
        GuiRenderUtils.drawChevronArrow(lx + SZ + 6, top + SZ / 2, 6, 0xFF7822, (System.currentTimeMillis() % 1000) / 1000f);
        int rx = lx + SZ + 32;
        drawSlotOutput(rx, top, C_FURNACE);
        itemRender.renderItemAndEffectIntoGUI(selected, rx + 1, top + 1);
        if (inside(mx, my, rx, top, SZ, SZ)) { drawSlotHover(rx, top); hov = selected; }
        return hov;
    }

    private static final int BREW_W = 96, BREW_H = 70;
    private ItemStack drawBrewingRecipe(int mx, int my, int cx, int top) {
        if (currentBrewRecipe == null) return null;
        int bx = cx - BREW_W / 2; ItemStack hov = null;
        drawRect(bx - 4, top - 4, bx + BREW_W + 4, top + BREW_H + 4, C_RECIPE_BG);

        // Visual connecting lines
        int ingX = bx + BREW_W / 2 - SZ / 2, ingY = top + 6;
        int inX = bx + 10, inY = top + BREW_H - SZ - 6;
        int outX = bx + BREW_W - SZ - 10, outY = inY;

        drawRect(cx, ingY + SZ, cx + 1, inY - 4, C_BREW & 0x44FFFFFF);
        drawRect(bx + 16, inY - 4, bx + BREW_W - 16, inY - 3, C_BREW & 0x44FFFFFF);

        drawSlot3D(ingX, ingY);
        itemRender.renderItemAndEffectIntoGUI(currentBrewRecipe.ingredient, ingX + 1, ingY + 1);
        if (inside(mx, my, ingX, ingY, SZ, SZ)) { hov = currentBrewRecipe.ingredient; drawSlotHover(ingX, ingY); }

        drawSlot3D(inX, inY);
        ItemStack inS = getPotionStack(currentBrewRecipe.inputMeta);
        itemRender.renderItemAndEffectIntoGUI(inS, inX + 1, inY + 1);
        if (inside(mx, my, inX, inY, SZ, SZ)) { hov = inS; drawSlotHover(inX, inY); }

        drawSlotOutput(outX, outY, C_BREW);
        ItemStack outS = getPotionStack(currentBrewRecipe.outputMeta);
        itemRender.renderItemAndEffectIntoGUI(outS, outX + 1, outY + 1);
        if (inside(mx, my, outX, outY, SZ, SZ)) { hov = outS; drawSlotHover(outX, outY); }

        String recipeInfo = currentBrewRecipe.name + (brewRecipesForItem.size() > 1 ? " (" + (brewRecipeIndex + 1) + "/" + brewRecipesForItem.size() + ")" : "");
        drawCenteredString(fontRendererObj, "\u00a7b" + recipeInfo, cx, top + BREW_H + 6, C_TXT_BREW);
        return hov;
    }

    private ItemStack getPotionStack(int meta) {
        if (CUSTOM_POTION_STACKS.containsKey(meta)) return CUSTOM_POTION_STACKS.get(meta).copy();
        return new ItemStack(Items.potionitem, 1, meta);
    }

    // ── Logic ──────────────────────────────────────────────────────────────────

    private int modeAccent() { return mode == Mode.BREWING ? C_BREW : (mode == Mode.FURNACE ? C_FURNACE : C_ACCENT); }
    private boolean isBlocked(ItemStack s) {
        if (s == null || s.getItem() == null) return true;
        String u = s.getUnlocalizedName();
        for (String b : BLOCKED) if (u != null && u.startsWith(b)) return true;
        return false;
    }
    private void filter(String q) {
        filtered.clear(); String lq = q.trim().toLowerCase();
        for (ItemStack s : allItems) if (lq.isEmpty() || s.getDisplayName().toLowerCase().contains(lq)) filtered.add(s);
        targetScroll = 0; smoothScroll = 0;
    }
    private int maxScroll() { return Math.max(0, (filtered.size() + COLS - 1) / COLS - visibleRows); }
    private int thumbH() { int total = (filtered.size() + COLS - 1) / COLS; return total <= visibleRows ? sbH : Math.max(16, sbH * visibleRows / total); }
    private void applyScrollFromMouse(int my) {
        int th = thumbH(), ms = maxScroll(); if (ms == 0) return;
        targetScroll = Math.max(0, Math.min(ms, Math.round((float) (my - sbY - th / 2) * ms / (sbH - th))));
    }

    private boolean isSameStack(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getItem() != b.getItem()) return false;
        if (a.getItem() == Items.potionitem) {
            int ma = resolveCustomPotionMeta(a), mb = resolveCustomPotionMeta(b);
            if (ma != -1 || mb != -1) return ma == mb;

            // Vanilla potion comparison: meta bits represent the effect
            int da = a.getItemDamage(), db = b.getItemDamage();
            // Normaliser en ignorant les bits inutiles pour la comparaison de type
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
            if (prev != null) {
                if (historyStack.isEmpty() || !isSameStack(historyStack.get(historyStack.size() - 1), prev)) historyStack.add(prev);
            }
        }
    }

    private boolean loadRecipe(ItemStack stack) {
        if (stack == null) return false;
        Mode oldMode = mode; ItemStack oldSel = selected; IRecipe oldCraft = craftRecipe; ItemStack oldFurnace = furnaceInput;
        List<PotionRecipe> oldBrew = brewRecipesForItem; PotionRecipe oldCurBrew = currentBrewRecipe; int oldBrewIdx = brewRecipeIndex;

        selected = stack; craftRecipe = null; furnaceInput = null; brewRecipesForItem = new ArrayList<>(); mode = Mode.LIST;

        if (stack.getItem() == Items.potionitem) {
            int meta = resolveCustomPotionMeta(stack);
            if (meta == -1) meta = stack.getItemDamage();

            if (POTION_RECIPES.containsKey(meta)) brewRecipesForItem.addAll(POTION_RECIPES.get(meta));
            else {
                for (Map.Entry<Integer, List<PotionRecipe>> e : POTION_RECIPES.entrySet()) {
                    if (isSameStack(getPotionStack(e.getKey()), stack)) { brewRecipesForItem.addAll(e.getValue()); break; }
                }
            }
            if (!brewRecipesForItem.isEmpty()) {
                brewRecipesForItem.sort(Comparator.comparingInt(a -> a.inputMeta));
                currentBrewRecipe = brewRecipesForItem.get(0); mode = Mode.BREWING;
            }
        }
        if (mode == Mode.LIST) {
            int targetMeta = stack.getItemDamage();
            for (IRecipe r : CraftingManager.getInstance().getRecipeList()) {
                ItemStack out = r.getRecipeOutput();
                if (out != null && out.getItem() == stack.getItem() && (out.getItemDamage() == 32767 || out.getItemDamage() == targetMeta)) {
                    craftRecipe = r; mode = Mode.CRAFT; break;
                }
            }
            if (mode == Mode.LIST) {
                for (Map.Entry<ItemStack, ItemStack> e : FurnaceRecipes.instance().getSmeltingList().entrySet()) {
                    ItemStack out = e.getValue();
                    if (out != null && out.getItem() == stack.getItem() && (out.getItemDamage() == 32767 || out.getItemDamage() == targetMeta)) {
                        furnaceInput = e.getKey(); mode = Mode.FURNACE; break;
                    }
                }
            }
        }
        if (mode != Mode.LIST) return true;

        mode = oldMode; selected = oldSel; craftRecipe = oldCraft; furnaceInput = oldFurnace; brewRecipesForItem = oldBrew; currentBrewRecipe = oldCurBrew; brewRecipeIndex = oldBrewIdx;
        return false;
    }

    private boolean hasRecipe(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() == Items.potionitem) {
            int meta = resolveCustomPotionMeta(stack); if (meta == -1) meta = stack.getItemDamage();
            if (POTION_RECIPES.containsKey(meta)) return true;
            for (int k : POTION_RECIPES.keySet()) if (isSameStack(getPotionStack(k), stack)) return true;
            return false;
        }
        int targetMeta = stack.getItemDamage();
        for (IRecipe r : CraftingManager.getInstance().getRecipeList()) {
            ItemStack out = r.getRecipeOutput();
            if (out != null && out.getItem() == stack.getItem() && (out.getItemDamage() == 32767 || out.getItemDamage() == targetMeta)) return true;
        }
        for (ItemStack out : FurnaceRecipes.instance().getSmeltingList().values()) {
            if (out != null && out.getItem() == stack.getItem() && (out.getItemDamage() == 32767 || out.getItemDamage() == targetMeta)) return true;
        }
        return false;
    }

    private void goBack() { if (!historyStack.isEmpty()) loadRecipe(historyStack.remove(historyStack.size() - 1)); else goMenu(); }
    private void goMenu() { mode = Mode.LIST; selected = null; historyStack.clear(); }

    private int resolveCustomPotionMeta(ItemStack stack) {
        if (stack == null || stack.getItem() != Items.potionitem) return -1;
        String sig = buildPotionSignature(stack);
        if (CUSTOM_POTION_SIGNATURES.containsKey(sig)) return CUSTOM_POTION_SIGNATURES.get(sig);
        return -1;
    }

    private static String buildPotionSignature(ItemStack stack) {
        if (stack == null) return "";
        try {
            ItemPotion ip = (ItemPotion) Items.potionitem;
            List<PotionEffect> eff = ip.getEffects(stack);
            List<String> parts = new ArrayList<>();
            if (eff != null) { for (PotionEffect p : eff) parts.add(p.getPotionID() + ":" + p.getAmplifier()); Collections.sort(parts); }
            return String.join(",", parts) + "|" + stack.getDisplayName().replaceAll("\u00A7.", "").toLowerCase(Locale.ROOT);
        } catch (Exception ex) { return ""; }
    }

    private ItemStack fixWildcard(ItemStack s) { if (s == null) return null; ItemStack c = s.copy(); if (c.getItemDamage() == 32767) c.setItemDamage(0); return c; }

    private ItemStack[] getGrid(IRecipe r) {
        ItemStack[] g = new ItemStack[9];
        if (r instanceof ShapedRecipes) {
            ShapedRecipes sr = (ShapedRecipes) r;
            int rw = sr.getRecipeWidth();
            int rh = sr.getRecipeHeight();
            ItemStack[] items = sr.getRecipeItems();
            for (int row = 0; row < rh; row++) {
                for (int col = 0; col < rw; col++) {
                    g[row * 3 + col] = fixWildcard(items[row * rw + col]);
                }
            }
        } else if (r instanceof ShapelessRecipes) {
            List<ItemStack> items = ((ShapelessRecipes) r).getRecipeItems();
            for (int i = 0; i < items.size() && i < 9; i++) g[i] = fixWildcard(items.get(i));
        }
        return g;
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn == 1) { goBack(); return; } // Right-click = Back

        searchBox.mouseClicked(mx, my, btn);
        if (mode != Mode.LIST) {
            if (inside(mx, my, btnBack.xPosition, btnBack.yPosition, btnW, btnH)) { goBack(); return; }
            if (inside(mx, my, btnClose.xPosition, btnClose.yPosition, btnW, btnH)) { mc.displayGuiScreen(null); return; }

            int recipeAreaH = (mode == Mode.BREWING) ? (BREW_H + 30) : (mode == Mode.CRAFT ? 3 * SZ + 30 : SZ + 30);
            int recPanelY = (height - Math.max(panelH, titleH + 10 + 15 + recipeAreaH + 40)) / 2;
            int contentY = recPanelY + titleH + 12 + 14;
            int cx = panelX + panelW / 2;

            if (btn == 0) {
                if (mode == Mode.BREWING && currentBrewRecipe != null) {
                    int bx = cx - BREW_W / 2;
                    if (inside(mx, my, bx + BREW_W / 2 - SZ / 2, contentY + 6, SZ, SZ)) openRecipe(currentBrewRecipe.ingredient);
                    else if (inside(mx, my, bx + 10, contentY + BREW_H - SZ - 6, SZ, SZ)) openRecipe(getPotionStack(currentBrewRecipe.inputMeta));
                    else if (inside(mx, my, bx + BREW_W - SZ - 10, contentY + BREW_H - SZ - 6, SZ, SZ)) openRecipe(getPotionStack(currentBrewRecipe.outputMeta));
                } else if (mode == Mode.CRAFT && craftRecipe != null) {
                    ItemStack[] grid = getGrid(craftRecipe); int sx0 = cx - (3 * SZ + 30 + SZ) / 2;
                    for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) if (grid[r * 3 + c] != null && inside(mx, my, sx0 + c * SZ, contentY + r * SZ, SZ, SZ)) openRecipe(grid[r * 3 + c]);
                    if (inside(mx, my, sx0 + 3 * SZ + 26, contentY + SZ, SZ, SZ)) openRecipe(craftRecipe.getRecipeOutput());
                } else if (mode == Mode.FURNACE && furnaceInput != null) {
                    int lx = cx - (SZ + 32 + SZ) / 2;
                    if (inside(mx, my, lx, contentY, SZ, SZ)) openRecipe(furnaceInput);
                    else if (inside(mx, my, lx + SZ + 32, contentY, SZ, SZ)) openRecipe(selected);
                }
            }
            return;
        }
        if (btn == 0) {
            if (inside(mx, my, sbX, sbY, SBW, sbH)) { draggingSB = true; applyScrollFromMouse(my); return; }
            int scrollRow = (int) smoothScroll; int pixelOffset = (int) ((smoothScroll - scrollRow) * SZ);
            for (int r = 0; r <= visibleRows; r++) for (int c = 0; c < COLS; c++) {
                int i = scrollRow * COLS + r * COLS + c; if (i >= filtered.size()) return;
                int x = gridX + c * SZ, y = gridY + r * SZ - pixelOffset;
                if (y + SZ > gridY && y < gridY + sbH && inside(mx, my, x, y, SZ, SZ)) { openRecipe(filtered.get(i)); return; }
            }
        }
    }

    @Override protected void mouseReleased(int mx, int my, int state) { super.mouseReleased(mx, my, state); draggingSB = false; }
    @Override protected void mouseClickMove(int mx, int my, int btn, long t) { super.mouseClickMove(mx, my, btn, t); if (draggingSB && mode == Mode.LIST) applyScrollFromMouse(my); }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int w = Mouse.getEventDWheel(), ms = maxScroll();
        if (w < 0 && targetScroll < ms) targetScroll++; if (w > 0 && targetScroll > 0) targetScroll--;
    }
    @Override protected void keyTyped(char ch, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE || (mode != Mode.LIST && (key == Keyboard.KEY_BACK || key == Keyboard.KEY_LEFT))) {
            if (mode != Mode.LIST) goBack(); else mc.displayGuiScreen(null);
        }
        else if (searchBox.textboxKeyTyped(ch, key)) filter(searchBox.getText());
        else super.keyTyped(ch, key);
    }
    @Override protected void actionPerformed(GuiButton btn) throws IOException { if (btn.id == 1) goBack(); else if (btn.id == 2) mc.displayGuiScreen(null); }
}
