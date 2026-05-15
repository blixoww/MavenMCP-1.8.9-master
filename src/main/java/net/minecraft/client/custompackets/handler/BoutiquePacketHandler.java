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

    /**
     * @param category 0=grade 1=commande 2=spawner 3=offre
     */
    // ── Packs par défaut (utilisés tant que le serveur n'en fournit pas) ──────

    private static void populateDemoPacks(BoutiqueData d) {
        d.packs.add(makePack("starter", "§b§lPack Starter",
                "iron_sword", 95, 130,
                "Grade VIP (Permanent)",
                "Kit Starter (Permanent)",
                "2 Spawners au choix"));
        d.packs.add(makePack("aventurier", "§a§lPack Aventurier",
                "diamond_chestplate", 140, 195,
                "Kit Combat (Permanent)",
                "3 Spawners au choix",
                "Commande /heal (Permanent)"));
        d.packs.add(makePack("elite", "§5§lPack Elite",
                "diamond", 220, 310,
                "Grade Elite (Permanent)",
                "Kit Combat + Kit PvP (Permanent)",
                "4 Spawners au choix",
                "Commandes /fly + /heal (Permanent)"));
        d.packs.add(makePack("mega", "§6§l§nMEGA PACK",
                "nether_star", 480, 720,
                "Grade Legend (Permanent)",
                "Tous les kits (Permanent)",
                "6 Spawners au choix",
                "Toutes les commandes (Permanent)",
                "Bonus : 500 PB offerts"));
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
