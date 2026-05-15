package net.minecraft.client.custompackets.data;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot de la boutique PB reçue depuis le serveur.
 * Voir {@code BoutiquePacketSender.sendData} côté serveur pour le format.
 */
public class BoutiqueData {

    public String title = "";
    public long balance;
    public int  pb;

    public final List<Entry> grades    = new ArrayList<>();
    public final List<Entry> kits      = new ArrayList<>();
    public final List<Entry> commandes = new ArrayList<>();
    public final List<Entry> spawners  = new ArrayList<>();
    public final List<Entry> packs     = new ArrayList<>();

    public OffreData offre;   // null si absente
    public long receivedAtMs;

    public static class Entry {
        public String id;
        public String nom;
        public String icone;
        public List<String> description = new ArrayList<>();
        public long  prixMonnaie;
        public int   prixPB;
        public int   duree;
        public String mob;
        // Prix pour achat permanent (0 = option permanente non disponible)
        public long  prixMonnaieP;
        public int   prixPBP;
    }

    public static class OffreData {
        public String id;
        public ItemStack item;
        public String nom;
        public List<String> lore = new ArrayList<>();
        public long  prixMonnaie;
        public int   prixPB;
        public int   stock;
        public int   stockInitial;
        public long  expiresAt;  // ms (System.currentTimeMillis sur le serveur)
        public long  expiresAtLocal; // ms ajusté localement
    }

    public static BoutiqueData readFrom(PacketBuffer buf) throws Exception {
        BoutiqueData d = new BoutiqueData();
        d.title   = buf.readStringFromBuffer(64);
        d.balance = buf.readLong();
        d.pb      = buf.readVarIntFromBuffer();
        readCategory(buf, d.grades);
        readCategory(buf, d.kits);
        readCategory(buf, d.commandes);
        readCategory(buf, d.spawners);
        boolean has = buf.readBoolean();
        if (has) {
            OffreData of = new OffreData();
            of.id = buf.readStringFromBuffer(64);
            of.item = buf.readItemStackFromBuffer();
            of.nom = buf.readStringFromBuffer(128);
            int n = buf.readVarIntFromBuffer();
            for (int i = 0; i < n; i++) of.lore.add(buf.readStringFromBuffer(128));
            of.prixMonnaie = buf.readLong();
            of.prixPB = buf.readVarIntFromBuffer();
            of.stock = buf.readVarIntFromBuffer();
            of.stockInitial = buf.readVarIntFromBuffer();
            of.expiresAt = buf.readLong();
            of.expiresAtLocal = of.expiresAt;
            d.offre = of;
        }
        // Packs — extension optionnelle en fin de paquet (backward compatible)
        try {
            if (buf.readableBytes() > 0) readCategory(buf, d.packs);
        } catch (Exception ignored) {}
        d.receivedAtMs = System.currentTimeMillis();
        return d;
    }

    private static void readCategory(PacketBuffer buf, List<Entry> target) {
        int n = buf.readVarIntFromBuffer();
        for (int i = 0; i < n; i++) {
            Entry e = new Entry();
            e.id    = buf.readStringFromBuffer(64);
            e.nom   = buf.readStringFromBuffer(64);
            e.icone = buf.readStringFromBuffer(48);
            int dl  = buf.readVarIntFromBuffer();
            for (int k = 0; k < dl; k++) e.description.add(buf.readStringFromBuffer(128));
            e.prixMonnaie = buf.readLong();
            e.prixPB      = buf.readVarIntFromBuffer();
            e.duree       = buf.readVarIntFromBuffer();
            e.mob         = buf.readStringFromBuffer(48);
            e.prixMonnaieP = buf.readLong();
            e.prixPBP      = buf.readVarIntFromBuffer();
            target.add(e);
        }
    }
}
