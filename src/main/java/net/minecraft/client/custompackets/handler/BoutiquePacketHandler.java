package net.minecraft.client.custompackets.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.custompackets.PacketChannel;
import net.minecraft.client.custompackets.PacketDispatcher;
import net.minecraft.client.custompackets.PacketId;
import net.minecraft.client.custompackets.PacketSender;
import net.minecraft.client.custompackets.data.BoutiqueData;
import net.minecraft.client.gui.GuiBoutique;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gère les packets BOUTIQUE_S2C : ouvre/rafraîchit {@link GuiBoutique} sur DATA,
 * affiche le résultat d'un achat sur RESULT.
 */
public final class BoutiquePacketHandler {

    private static final Logger LOGGER = LogManager.getLogger("Boutique");

    private static BoutiqueData cached;
    private static String lastResultMessage = "";
    private static boolean lastResultSuccess = true;
    private static long lastResultAt = 0L;

    private BoutiquePacketHandler() {}

    public static BoutiqueData getCached() { return cached; }
    public static String  getLastResultMessage() { return lastResultMessage; }
    public static boolean isLastResultSuccess()  { return lastResultSuccess; }
    public static long    getLastResultAt()      { return lastResultAt; }

    public static void registerHandlers() {
        PacketDispatcher.register(PacketChannel.BOUTIQUE_S2C, PacketId.BOUTIQUE_DATA, buf -> {
            try {
                cached = BoutiqueData.readFrom(buf);
                // Si le serveur n'a pas (encore) envoyé de packs, on injecte une
                // sélection par défaut pour que la catégorie soit visible côté client.
                if (cached.packs.isEmpty()) populateDemoPacks(cached);
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    Minecraft mc = Minecraft.getMinecraft();
                    if (mc.currentScreen instanceof GuiBoutique) {
                        ((GuiBoutique) mc.currentScreen).onDataRefreshed();
                    } else {
                        mc.displayGuiScreen(new GuiBoutique());
                    }
                });
            } catch (Exception e) {
                LOGGER.error("[Boutique] Erreur parsing BOUTIQUE_DATA", e);
            }
        });

        PacketDispatcher.register(PacketChannel.BOUTIQUE_S2C, PacketId.BOUTIQUE_RESULT, buf -> {
            lastResultSuccess = buf.readBoolean();
            lastResultMessage = buf.readStringFromBuffer(256);
            lastResultAt      = System.currentTimeMillis();
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen instanceof GuiBoutique) {
                    ((GuiBoutique) mc.currentScreen).onResult();
                }
            });
        });
    }

    // ── C2S ─────────────────────────────────────────────────────────────────

    public static void requestData() {
        PacketSender.sendSimple(PacketChannel.BOUTIQUE_C2S, PacketId.BOUTIQUE_REQUEST);
    }

    // ── Packs par défaut (utilisés tant que le serveur n'en fournit pas) ──────

    private static void populateDemoPacks(BoutiqueData d) {
        // ── Pack Starter ─────────────────────────────────────────────────────
        // Valeur individuelle : Elite perm (1500) + 4 spawners (3500) + custom items (~600) + kit (~300) + cles (~150) ≈ 6050 PB
        d.packs.add(makePack("starter", "§b§lPack Starter",
                "iron_chestplate", 2800, 6050,
                "§bGrade Elite §7(Permanent)",
                "§74 Spawners : 2 Zombie + 2 Squelette",
                "§7Armure complete Prot.3 en Emeraude",
                "§7Epee Ruby Tranchant IV",
                "§7Pioche Ruby Efficacite IV Incassable III",
                "§74 stacks d'Obsidienne",
                "§aKit Starter §7(Permanent)",
                "§73 Cles en Acier §8(steel_key)"));

        // ── Pack Avancé ──────────────────────────────────────────────────────
        // Valeur individuelle : Elite perm (1500) + 6 spawners (6300) + custom ruby stuff (~900) + kit (~500) + cles (~350) ≈ 9550 PB
        d.packs.add(makePack("avance", "§6§lPack Avance",
                "diamond_chestplate", 4200, 9550,
                "§bGrade Elite §7(Permanent)",
                "§76 Spawners : 2 Zombie + 2 Pig Zombie + 2 Squelette",
                "§7Stuff complet Prot.3 en Ruby",
                "§7Epee Cobalt Tranchant IV",
                "§7Pioche Cobalt Efficacite IV Incassable III",
                "§76 stacks d'Obsidienne",
                "§aKit Bonus §7(Permanent)",
                "§73 Cles en Ruby"));

        // ── Mega Pack ────────────────────────────────────────────────────────
        // Valeur individuelle : Immortel perm (2500) + 8 spawners (9100) + custom ruby P4 stuff (~1400) + kit (~700) + cles (~500) ≈ 14200 PB
        d.packs.add(makePack("mega", "§c§l§nMEGA PACK",
                "nether_star", 6500, 14200,
                "§6Grade Immortel §7(Permanent)",
                "§78 Spawners : 2 Zombie + 4 Pig Zombie + 2 Squelette",
                "§7Stuff complet Prot.4 Ruby Incassable III",
                "§7Epee Cobalt Tranchant V",
                "§7Pioche Cobalt Efficacite V Incassable III",
                "§710 stacks d'Obsidienne",
                "§dKit Potion §7(Permanent)",
                "§73 Cles en Cobalt"));
    }

    private static BoutiqueData.Entry makePack(String id, String nom, String icone,
                                                int prixPack, int valeurOriginale,
                                                String... contenu) {
        BoutiqueData.Entry e = new BoutiqueData.Entry();
        e.id = id;
        e.nom = nom;
        e.icone = icone;
        e.prixMonnaie = 0L;
        e.prixPB      = prixPack;
        e.prixMonnaieP = 0L;
        e.prixPBP     = valeurOriginale; // repurposé : valeur d'origine
        e.duree       = 0;
        e.mob         = "";
        for (String c : contenu) e.description.add(c);
        return e;
    }

    public static void sendBuy(int category, String id, boolean payPB, boolean temporary) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.BOUTIQUE_BUY);
        buf.writeByte((byte) (category & 0xFF));
        buf.writeString(id == null ? "" : id);
        buf.writeBoolean(payPB);
        buf.writeBoolean(temporary);
        PacketSender.send(PacketChannel.BOUTIQUE_C2S, buf);
    }
}
