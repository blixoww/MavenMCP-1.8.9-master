package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.entity.Entity;

public class ReachWidget extends BaseWidget {
    private double lastReach = -1.0D;
    private long lastAttackTime = 0L;

    public ReachWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 60;
        this.height = 12;
    }

    @Override public boolean supportsLabelColor() { return true; }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        // On vérifie si le joueur est en train de cliquer ou si l'animation de coup est en cours
        boolean isAttacking = mc.gameSettings.keyBindAttack.isKeyDown() || mc.thePlayer.isSwingInProgress;

        if (isAttacking) {
            Entity target = null;
            Vec3 hitVec = null;

            // 1. Tentative via objectMouseOver (contient le point d'impact exact sur la hitbox)
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                target = mc.objectMouseOver.entityHit;
                hitVec = mc.objectMouseOver.hitVec;
            }

            
            // 2. Tentative via pointedEntity (fallback si l'autre échoue)
            if (target == null) {
                target = mc.pointedEntity;
            }

            if (target != null) {
                double dist;
                if (hitVec != null) {
                    // Distance précise Yeux -> Point d'impact
                    dist = mc.thePlayer.getPositionEyes(1.0F).distanceTo(hitVec);
                } else {
                    // Distance brute entre les entités
                    dist = mc.thePlayer.getDistanceToEntity(target);
                }
                
                // On ne met à jour que si on a une valeur cohérente (> 0)
                if (dist > 0) {
                    this.lastReach = dist;
                    this.lastAttackTime = System.currentTimeMillis();
                }
            }
        }

        // Garder la valeur affichée pendant 4 secondes après le dernier coup
        if (System.currentTimeMillis() - lastAttackTime > 4000L && !UIManager.getInstance().isEditorActive()) {
            lastReach = -1.0D;
        }

        String value;
        if (lastReach < 0) {
            value = UIManager.getInstance().isEditorActive() ? "3.00" : "0.00";
        } else {
            value = String.format("%.2f", lastReach);
        }

        FontRenderer fr = mc.fontRendererObj;
        if (!Boolean.TRUE.equals(getPropOrDefault("customSize", false))) {
            this.width = fr.getStringWidth("Reach: " + value) + 4;
        }
        
        int col = getColor();
        if ((col & 0xFF000000) == 0) col |= 0xFF000000;
        drawLabelValue(fr, "Reach: ", value, 0, 0, col);
    }
}
