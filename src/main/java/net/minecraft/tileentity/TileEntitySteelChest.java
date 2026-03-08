package net.minecraft.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerSteelChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;

/**
 * Coffre en Acier : hérite de TileEntityChest pour bénéficier du renderer vanilla
 * (animation du couvercle, texture). Surcharge le tableau de contenu à 54 slots.
 */
public class TileEntitySteelChest extends TileEntityChest {
    private ItemStack[] steelContents = new ItemStack[54];
    private String customName;
    private int ticksSinceSync = 0; // Compteur local pour la synchronisation

    public TileEntitySteelChest() {
        super(-1); // -1 = type custom, pas un coffre piégé
    }

    @Override
    public int getSizeInventory() {
        return 54;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.steelContents[index];
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (this.steelContents[index] != null) {
            if (this.steelContents[index].stackSize <= count) {
                ItemStack stack = this.steelContents[index];
                this.steelContents[index] = null;
                this.markDirty();
                return stack;
            }
            ItemStack split = this.steelContents[index].splitStack(count);
            if (this.steelContents[index].stackSize == 0) this.steelContents[index] = null;
            this.markDirty();
            return split;
        }
        return null;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (this.steelContents[index] != null) {
            ItemStack stack = this.steelContents[index];
            this.steelContents[index] = null;
            return stack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.steelContents[index] = stack;
        if (stack != null && stack.stackSize > this.getInventoryStackLimit())
            stack.stackSize = this.getInventoryStackLimit();
        this.markDirty();
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : "container.steelChest";
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null && !this.customName.isEmpty();
    }

    public void setCustomName(String name) {
        this.customName = name;
    }

    @Override
    public void openInventory(EntityPlayer player) {
        if (!player.isSpectator()) {
            if (this.numPlayersUsing < 0) this.numPlayersUsing = 0;
            ++this.numPlayersUsing;
            this.worldObj.addBlockEvent(this.pos, this.getBlockType(), 1, this.numPlayersUsing);
        }
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        if (!player.isSpectator()) {
            --this.numPlayersUsing;
            this.worldObj.addBlockEvent(this.pos, this.getBlockType(), 1, this.numPlayersUsing);
        }
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new net.minecraft.inventory.ContainerSteelChest(playerInventory, this, playerIn);
    }

    @Override
    public String getGuiID() {
        return "minecraft:steel_chest";
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        // On lit manuellement sans appeler super (qui utilise chestContents[27])
        this.steelContents = new ItemStack[54];
        NBTTagList list = compound.getTagList("SteelItems", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 255;
            if (slot < this.steelContents.length)
                this.steelContents[slot] = ItemStack.loadItemStackFromNBT(tag);
        }
        if (compound.hasKey("CustomName", 8))
            this.customName = compound.getString("CustomName");
        // Lire les données de base de TileEntity (pos, world, etc.)
        super.readFromNBT(compound);
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < this.steelContents.length; i++) {
            if (this.steelContents[i] != null) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("Slot", (byte) i);
                this.steelContents[i].writeToNBT(tag);
                list.appendTag(tag);
            }
        }
        compound.setTag("SteelItems", list);
        if (this.hasCustomName())
            compound.setString("CustomName", this.customName);
    }

    @Override
    public void clear() {
        java.util.Arrays.fill(this.steelContents, null);
    }

    /**
     * Surcharge update() pour vérifier ContainerSteelChest (et non ContainerChest)
     * afin que numPlayersUsing soit correct et que le couvercle s'ouvre/ferme bien.
     */
    @Override
    public void update() {
        // Incrémentation continue
        this.ticksSinceSync++;

        // Resynchronisation toutes les 200 ticks côté serveur
        if (!this.worldObj.isRemote && this.ticksSinceSync % 200 == 0) {
            this.numPlayersUsing = 0;
            float range = 5.0F;
            for (EntityPlayer player : this.worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    new AxisAlignedBB(this.pos.getX() - range, this.pos.getY() - range, this.pos.getZ() - range,
                            this.pos.getX() + 1 + range, this.pos.getY() + 1 + range, this.pos.getZ() + 1 + range))) {
                if (player.openContainer instanceof ContainerSteelChest) {
                    ++this.numPlayersUsing;
                }
            }
        }

        // Animation du couvercle
        this.prevLidAngle = this.lidAngle;
        float speed = 0.1F;

        if (this.numPlayersUsing > 0 && this.lidAngle == 0.0F) {
            this.worldObj.playSoundEffect(
                    this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5,
                    "random.chestopen", 0.5F,
                    this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }

        if (this.numPlayersUsing == 0 && this.lidAngle > 0.0F || this.numPlayersUsing > 0 && this.lidAngle < 1.0F) {
            float prev = this.lidAngle;
            if (this.numPlayersUsing > 0)
                this.lidAngle += speed;
            else
                this.lidAngle -= speed;

            if (this.lidAngle > 1.0F) this.lidAngle = 1.0F;
            if (this.lidAngle < 0.0F) this.lidAngle = 0.0F;

            if (this.lidAngle < 0.5F && prev >= 0.5F) {
                this.worldObj.playSoundEffect(
                        this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5,
                        "random.chestclosed", 0.5F,
                        this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
            }
        }
    }
}
