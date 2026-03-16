package net.minecraft.item.crafting;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class RecipesTools
{
    private String[][] recipePatterns = new String[][] {{"XXX", " # ", " # "}, {"X", "#", "#"}, {"XX", "X#", " #"}, {"XX", " #", " #"}};
    private Object[][] recipeItems = new Object[][] {{Blocks.planks, Blocks.cobblestone, Items.iron_ingot, Items.diamond, Items.gold_ingot}, {Items.wooden_pickaxe, Items.stone_pickaxe, Items.iron_pickaxe, Items.diamond_pickaxe, Items.golden_pickaxe}, {Items.wooden_shovel, Items.stone_shovel, Items.iron_shovel, Items.diamond_shovel, Items.golden_shovel}, {Items.wooden_axe, Items.stone_axe, Items.iron_axe, Items.diamond_axe, Items.golden_axe}, {Items.wooden_hoe, Items.stone_hoe, Items.iron_hoe, Items.diamond_hoe, Items.golden_hoe}};

    /**
     * Adds the tool recipes to the CraftingManager.
     */
    public void addRecipes(CraftingManager mgr)
    {
        // Recettes vanilla
        for (int i = 0; i < this.recipeItems[0].length; ++i)
        {
            Object object = this.recipeItems[0][i];
            for (int j = 0; j < this.recipeItems.length - 1; ++j)
            {
                Item item = (Item)this.recipeItems[j + 1][i];
                mgr.addRecipe(new ItemStack(item), new Object[] {this.recipePatterns[j], '#', Items.stick, 'X', object});
            }
        }
        mgr.addRecipe(new ItemStack(Items.shears), new Object[] {" #", "# ", '#', Items.iron_ingot});

        // ── Outils Steel ──────────────────────────────────────────────────────────
        mgr.addRecipe(new ItemStack(Items.steel_pickaxe), new Object[]{"XXX", " # ", " # ", '#', Items.stick, 'X', Items.steel_ingot});
        mgr.addRecipe(new ItemStack(Items.steel_shovel),  new Object[]{"X",   "#",   "#",   '#', Items.stick, 'X', Items.steel_ingot});
        mgr.addRecipe(new ItemStack(Items.steel_axe),     new Object[]{"XX",  "X#",  " #",  '#', Items.stick, 'X', Items.steel_ingot});
        mgr.addRecipe(new ItemStack(Items.steel_hoe),     new Object[]{"XX",  " #",  " #",  '#', Items.stick, 'X', Items.steel_ingot});

        // ── Outils Émeraude ───────────────────────────────────────────────────────
        mgr.addRecipe(new ItemStack(Items.emerald_pickaxe), new Object[]{"XXX", " # ", " # ", '#', Items.stick, 'X', Items.emerald});
        mgr.addRecipe(new ItemStack(Items.emerald_shovel),  new Object[]{"X",   "#",   "#",   '#', Items.stick, 'X', Items.emerald});
        mgr.addRecipe(new ItemStack(Items.emerald_axe),     new Object[]{"XX",  "X#",  " #",  '#', Items.stick, 'X', Items.emerald});
        mgr.addRecipe(new ItemStack(Items.emerald_hoe),     new Object[]{"XX",  " #",  " #",  '#', Items.stick, 'X', Items.emerald});

        // ── Outils Ruby ────────────────────────────────────────────────────────────
        mgr.addRecipe(new ItemStack(Items.ruby_pickaxe), new Object[]{"XXX", " # ", " # ", '#', Items.stick, 'X', Items.ruby});
        mgr.addRecipe(new ItemStack(Items.ruby_shovel),  new Object[]{"X",   "#",   "#",   '#', Items.stick, 'X', Items.ruby});
        mgr.addRecipe(new ItemStack(Items.ruby_axe),     new Object[]{"XX",  "X#",  " #",  '#', Items.stick, 'X', Items.ruby});
        mgr.addRecipe(new ItemStack(Items.ruby_hoe),     new Object[]{"XX",  " #",  " #",  '#', Items.stick, 'X', Items.ruby});

        // ── Outils Cobalt ──────────────────────────────────────────────────────────
        mgr.addRecipe(new ItemStack(Items.cobalt_pickaxe), new Object[]{"XXX", " # ", " # ", '#', Items.stick, 'X', Items.cobalt_ingot});
        mgr.addRecipe(new ItemStack(Items.cobalt_shovel),  new Object[]{"X",   "#",   "#",   '#', Items.stick, 'X', Items.cobalt_ingot});
        mgr.addRecipe(new ItemStack(Items.cobalt_axe),     new Object[]{"XX",  "X#",  " #",  '#', Items.stick, 'X', Items.cobalt_ingot});
        mgr.addRecipe(new ItemStack(Items.cobalt_hoe),     new Object[]{"XX",  " #",  " #",  '#', Items.stick, 'X', Items.cobalt_ingot});

        // ── Cobalt Hammer (3x3 mining) ───────────────────────────────────────────
        mgr.addRecipe(new ItemStack(Items.cobalt_hammer), new Object[]{"CCC", "C#C", " # ", '#', Items.stick, 'C', Items.cobalt_ingot});
    }
}


