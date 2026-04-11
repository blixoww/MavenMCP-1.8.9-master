package net.minecraft.client.visuals.ping;

/**
 * Données d'un marqueur de ping actif.
 */
public class PingEntry {

    public final int id;
    public final double x, y, z;
    public final String senderName;
    public final int color;   // 0xAARRGGBB
    public final long createdAt;

    public PingEntry(int id, double x, double y, double z, String senderName, int color) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.senderName = senderName;
        this.color = color;
        this.createdAt = System.currentTimeMillis();
    }
}
