package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntitySteelChest;

/**
 * Container du Coffre en Acier : 54 slots (6 rangées × 9) — équivalent double coffre.
 * Layout GUI : coffre en haut, inventaire joueur en bas.
 */
public class ContainerSteelChest extends Container
{
    private final TileEntitySteelChest steelChest;
    private static final int CHEST_ROWS   = 6;
    private static final int CHEST_SLOTS  = CHEST_ROWS * 9; // 54
    private static final int PLAYER_INV   = 27;
    private static final int PLAYER_HOTBAR = 9;

    public ContainerSteelChest(InventoryPlayer playerInventory, TileEntitySteelChest chest, EntityPlayer player)
    {
        this.steelChest = chest;
        chest.openInventory(player);

        // Slots du coffre (6 rangées × 9 colonnes)
        for (int row = 0; row < CHEST_ROWS; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlotToContainer(new Slot(chest, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Inventaire joueur (3 rangées)
        int yOffset = CHEST_ROWS * 18 + 14; // décalage vertical
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, yOffset + row * 18));
            }
        }

        // Barre d'accès rapide (hotbar)
        for (int col = 0; col < 9; col++)
        {
            this.addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, yOffset + 58));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        return this.steelChest.isUseableByPlayer(player);
    }

    @Override
    public void onContainerClosed(EntityPlayer player)
    {
        super.onContainerClosed(player);
        this.steelChest.closeInventory(player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index)
    {
        ItemStack copy = null;
        Slot slot = (Slot) this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack())
        {
            ItemStack stack = slot.getStack();
            copy = stack.copy();

            if (index < CHEST_SLOTS)
            {
                // Du coffre → inventaire joueur
                if (!this.mergeItemStack(stack, CHEST_SLOTS, this.inventorySlots.size(), true))
                    return null;
            }
            else
            {
                // De l'inventaire joueur → coffre
                if (!this.mergeItemStack(stack, 0, CHEST_SLOTS, false))
                    return null;
            }

            if (stack.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
        }
        return copy;
    }

    public TileEntitySteelChest getSteelChest()
    {
        return this.steelChest;
    }
}


