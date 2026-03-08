package net.minecraft.item.crafting;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class RecipesWeapons
{
    private String[][] recipePatterns = new String[][] {{"X", "X", "#"}};
    private Object[][] recipeItems = new Object[][] {{Blocks.planks, Blocks.cobblestone, Items.iron_ingot, Items.diamond, Items.gold_ingot}, {Items.wooden_sword, Items.stone_sword, Items.iron_sword, Items.diamond_sword, Items.golden_sword}};

    /**
     * Adds the weapon recipes to the CraftingManager.
     */
    public void addRecipes(CraftingManager mgr)
    {
        // Épées vanilla
        for (int i = 0; i < this.recipeItems[0].length; ++i)
        {
            Object object = this.recipeItems[0][i];
            for (int j = 0; j < this.recipeItems.length - 1; ++j)
            {
                Item item = (Item)this.recipeItems[j + 1][i];
                mgr.addRecipe(new ItemStack(item), new Object[] {this.recipePatterns[j], '#', Items.stick, 'X', object});
            }
        }
        mgr.addRecipe(new ItemStack(Items.bow, 1), new Object[] {" #X", "# X", " #X", 'X', Items.string, '#', Items.stick});
        mgr.addRecipe(new ItemStack(Items.arrow, 4), new Object[] {"X", "#", "Y", 'Y', Items.feather, 'X', Items.flint, '#', Items.stick});

        // ── Épées custom ──────────────────────────────────────────────────────────
        mgr.addRecipe(new ItemStack(Items.steel_sword),   new Object[]{"X", "X", "#", '#', Items.stick, 'X', Items.steel_ingot});
        mgr.addRecipe(new ItemStack(Items.emerald_sword), new Object[]{"X", "X", "#", '#', Items.stick, 'X', Items.emerald});
        mgr.addRecipe(new ItemStack(Items.ruby_sword),    new Object[]{"X", "X", "#", '#', Items.stick, 'X', Items.ruby});
        mgr.addRecipe(new ItemStack(Items.cobalt_sword),  new Object[]{"X", "X", "#", '#', Items.stick, 'X', Items.cobalt_ingot});
    }
}


