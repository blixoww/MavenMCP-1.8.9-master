package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

/**
 * Widget Auto Armor — équipe automatiquement la meilleure armure disponible dans l'inventaire
 * dès qu'un slot d'armure est vide.
 *
 * Propriétés :
 *  - "autoActive"   (Boolean, défaut true)  : active/désactive la feature d'équipement automatique
 *  - "showDisplay"  (Boolean, défaut true)  : affiche ou cache le texte HUD (la feature tourne toujours si active)
 */
public class AutoArmorWidget extends BaseWidget {

    /** Cooldown en ticks entre deux vérifications d'équipement. */
    private int tickCooldown = 0;

    public AutoArmorWidget(String id, int x, int y) {
        super(id, x, y);
        this.width  = 94;
        this.height = 12;
        this.defaultWidth  = 94;
        this.defaultHeight = 12;
    }

    @Override
    public boolean supportsLabelColor() { return true; }

    // ---- Feature state ----

    /** Retourne true si la feature auto-armor est activée. */
    public boolean isAutoActive() {
        return !Boolean.FALSE.equals(getPropOrDefault("autoActive", Boolean.TRUE));
    }

    // ---- Render (logique + affichage) ----

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        // ── 1. Logique auto-armor — tourne indépendamment de showDisplay ──
        if (isAutoActive() && !UIManager.getInstance().isEditorActive()) {
            runAutoArmor(mc);
        }

        // ── 2. Affichage HUD (sauteable sans couper la feature) ──
        if (Boolean.FALSE.equals(getPropOrDefault("showDisplay", Boolean.TRUE))) return;

        FontRenderer fr = mc.fontRendererObj;
        String value = isAutoActive() ? "ON" : "OFF";
        drawLabelValue(fr, "Auto Armor: ", value, 0, 0);
    }

    // ---- Auto Armor Logic ----

    /**
     * Vérifie chaque slot d'armure : si vide, cherche la pièce correspondante
     * dans l'inventaire principal et la déplace via shift-clic simulé.
     *
     * Mapping armorInventory ↔ ContainerPlayer :
     *   armorInventory[0] = boots    → slot container 8
     *   armorInventory[1] = leggings → slot container 7
     *   armorInventory[2] = chest    → slot container 6
     *   armorInventory[3] = helmet   → slot container 5
     *
     * Mapping mainInventory[i] → slot container :
     *   i = 0-8  (hotbar)   → slot = i + 36
     *   i = 9-35 (main inv) → slot = i
     */
    private void runAutoArmor(Minecraft mc) {
        if (tickCooldown > 0) { tickCooldown--; return; }
        tickCooldown = 10; // vérifie toutes les 10 ticks (~0.5 s)

        try {
            net.minecraft.entity.player.EntityPlayer player = mc.thePlayer;
            InventoryPlayer inv = player.inventory;

            for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
                // Passer si le slot d'armure est déjà occupé
                if (inv.armorInventory[armorSlot] != null) continue;

                // armorType dans ItemArmor : 0=helmet, 1=chest, 2=leggings, 3=boots
                // armorInventory[3]=helmet(0), [2]=chest(1), [1]=legs(2), [0]=boots(3)
                int targetArmorType = 3 - armorSlot;

                // Chercher la meilleure armure de ce type dans l'inventaire
                ItemStack bestStack      = null;
                int       bestSlot       = -1;
                int       bestProtection = -1;

                for (int i = 0; i < inv.mainInventory.length; i++) {
                    ItemStack stack = inv.mainInventory[i];
                    if (stack == null) continue;
                    if (!(stack.getItem() instanceof ItemArmor)) continue;
                    ItemArmor armor = (ItemArmor) stack.getItem();
                    if (armor.armorType != targetArmorType) continue;
                    int protection = armor.damageReduceAmount;
                    if (protection > bestProtection) {
                        bestProtection = protection;
                        bestStack      = stack;
                        bestSlot       = i;
                    }
                }

                if (bestStack != null && bestSlot >= 0) {
                    // Calcul du slot container pour la source (mainInventory[bestSlot])
                    int containerSrc = (bestSlot < 9) ? (bestSlot + 36) : bestSlot;

                    // Shift-clic (mode=1) : équipe automatiquement l'armure
                    mc.playerController.windowClick(
                            player.inventoryContainer.windowId,
                            containerSrc, 0, 1, player
                    );

                    tickCooldown = 20; // pause plus longue après équipement
                    break; // une pièce à la fois
                }
            }
        } catch (Throwable ignored) {}
    }
}


