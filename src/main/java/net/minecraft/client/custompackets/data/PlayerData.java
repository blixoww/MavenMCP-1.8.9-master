package net.minecraft.client.custompackets.data;

import net.minecraft.network.PacketBuffer;

/**
 * Données joueur reçues depuis le serveur (monnaie, rang, stats, etc.).
 */
public class PlayerData {

    private String rank;
    private long balance;
    private int kills;
    private int deaths;
    private int playTimeMinutes;

    public PlayerData() {}

    // ── Sérialisation ────────────────────────────────────────────────────────

    public static PlayerData readFrom(PacketBuffer buf) {
        PlayerData d = new PlayerData();
        d.rank            = buf.readStringFromBuffer(32);
        d.balance         = buf.readLong();
        d.kills           = buf.readVarIntFromBuffer();
        d.deaths          = buf.readVarIntFromBuffer();
        d.playTimeMinutes = buf.readVarIntFromBuffer();
        return d;
    }

    public void writeTo(PacketBuffer buf) {
        buf.writeString(rank);
        buf.writeLong(balance);
        buf.writeVarIntToBuffer(kills);
        buf.writeVarIntToBuffer(deaths);
        buf.writeVarIntToBuffer(playTimeMinutes);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getRank()           { return rank; }
    public void   setRank(String r)   { this.rank = r; }

    public long   getBalance()        { return balance; }
    public void   setBalance(long b)  { this.balance = b; }

    public int    getKills()              { return kills; }
    public void   setKills(int k)         { this.kills = k; }
    public int    getDeaths()             { return deaths; }
    public void   setDeaths(int d)        { this.deaths = d; }
    public int    getPlayTimeMinutes()    { return playTimeMinutes; }
    public void   setPlayTimeMinutes(int m) { this.playTimeMinutes = m; }

    /** KD ratio – retourne 0 si aucune mort */
    public float  getKDRatio() {
        return deaths == 0 ? kills : (float) kills / deaths;
    }

    @Override
    public String toString() {
        return "PlayerData{rank='" + rank + "', balance=" + balance
                + ", kills=" + kills + ", deaths=" + deaths + '}';
    }
}

