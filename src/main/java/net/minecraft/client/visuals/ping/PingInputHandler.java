package net.minecraft.client.visuals.ping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.custompackets.PacketChannel;
import net.minecraft.client.custompackets.PacketId;
import net.minecraft.client.custompackets.PacketSender;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gère l'input du système de ping.
 * Appelé depuis la boucle d'input de {@code Minecraft.runTick()}.
 *
 * <p>Un seul ping, pas de type. La couleur/style/taille sont réglés localement
 * dans les {@link PingSettings} de chaque joueur.
 */
public final class PingInputHandler {

    private static final Logger LOGGER = LogManager.getLogger("PingSystem");
    private static final double PING_RANGE = 64.0;

    private PingInputHandler() {}

    /**
     * Appelé quand la touche ping est pressée.
     * Effectue un raytrace, crée le ping localement et envoie le packet au serveur.
     */
    public static void onPingKey(Minecraft mc) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        PingManager  pm = PingManager.INSTANCE;
        PingSettings s  = pm.getSettings();
        if (!s.enabled) return;

        if (!pm.canPing()) {
            // Cooldown actif : on n'affiche rien de superflu
            return;
        }

        // ── Raytrace ──────────────────────────────────────────────────────────
        Vec3 eye  = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 look = mc.thePlayer.getLook(1.0f);
        Vec3 end  = eye.addVector(
            look.xCoord * PING_RANGE,
            look.yCoord * PING_RANGE,
            look.zCoord * PING_RANGE
        );

        double x, y, z;
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(eye, end, false);

        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            net.minecraft.util.BlockPos bp = mop.getBlockPos();
            x = bp.getX() + 0.5;
            y = bp.getY() + 1.0;
            z = bp.getZ() + 0.5;
        } else if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY
                   && mop.entityHit != null) {
            x = mop.entityHit.posX;
            y = mop.entityHit.posY + mop.entityHit.height;
            z = mop.entityHit.posZ;
        } else {
            x = end.xCoord;
            y = end.yCoord;
            z = end.zCoord;
        }

        // ── Création locale + son ─────────────────────────────────────────────
        if (pm.createLocalPing(x, y, z)) {
            if (s.soundEnabled) {
                mc.thePlayer.playSound(
                    PingType.PING.sound,
                    PingType.PING.soundVol,
                    PingType.PING.soundPitch
                );
            }
            sendPingPacket(x, y, z);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie le packet PING_PLACE au serveur.
     * Format : VarInt packetId | double x | double y | double z
     */
    private static void sendPingPacket(double x, double y, double z) {
        try {
            PacketBuffer buf = PacketSender.newBuffer();
            buf.writeVarIntToBuffer(PacketId.PING_PLACE);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            PacketSender.send(PacketChannel.PING_C2S, buf);
        } catch (Exception e) {
            LOGGER.warn("[PingSystem] Erreur envoi PING_PLACE", e);
        }
    }
}
