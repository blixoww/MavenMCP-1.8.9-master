package net.minecraft.client.visuals.ping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

/**
 * Singleton gérant le pool de pings actifs.
 *
 * <p>Design : pool fixe de {@value MAX_PINGS} objets {@link Ping} pré-alloués —
 * aucune allocation sur le hot-path (tick / render). Le tableau {@code renderSnapshot}
 * est mis à jour une fois par tick et lu directement par {@link PingRenderer}.
 *
 * <p>Le rendu visuel (couleur, taille, style) est entièrement déterminé par les
 * {@link PingSettings} du <b>viewer</b> local, pas de l'expéditeur.
 */
public final class PingManager {

    public static final PingManager INSTANCE = new PingManager();

    public  static final int  MAX_PINGS         = 16;
    public  static final long NOTIF_DURATION_MS = 4000L;
    private static final int  MAX_NOTIFS        = 5;

    private final Ping[] pool = new Ping[MAX_PINGS];

    final Ping[] renderSnapshot = new Ping[MAX_PINGS];
    int renderCount = 0;

    private PingSettings settings;
    private long         lastPingTime = 0L;

    private final String[] notifText = new String[MAX_NOTIFS];
    private final long[]   notifTime = new long[MAX_NOTIFS];
    private int notifCount = 0;

    // ─────────────────────────────────────────────────────────────────────────

    private PingManager() {
        for (int i = 0; i < MAX_PINGS; i++) pool[i] = new Ping();
        settings = PingSettings.load();
        applyStoredKeyBinding();
    }

    /**
     * Applique le keyCode persisté dans PingSettings au KeyBinding de GameSettings.
     * Appelé au démarrage et après chaque changement de touche via la GUI.
     */
    public void applyStoredKeyBinding() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.gameSettings == null) return;
            mc.gameSettings.keyBindPing.setKeyCode(settings.keyCode);
            KeyBinding.resetKeyBindingArrayAndHash();
        } catch (Exception ignored) {}
    }

    // ── Accesseurs ───────────────────────────────────────────────────────────

    public PingSettings getSettings()               { return settings; }
    public void         setSettings(PingSettings s) { this.settings = s; applyStoredKeyBinding(); }

    // ── Création locale ───────────────────────────────────────────────────────

    /** @return false si le ping est désactivé ou si le cooldown est actif */
    public boolean createLocalPing(double x, double y, double z) {
        if (!settings.enabled) return false;
        if (System.currentTimeMillis() - lastPingTime < settings.cooldownMs) return false;
        lastPingTime = System.currentTimeMillis();
        acquire(x, y, z, localName(), -1);
        return true;
    }

    // ── Création distante (packet S2C reçu du serveur) ────────────────────────

    /**
     * Ajoute un ping distant reçu du serveur.
     * @param relation type envoyé par le serveur : 0=faction,1=ally,2=friend
     */
    public void addRemotePing(double x, double y, double z, String sender, int relation) {
        // Filtrage selon les préférences du viewer
        if (relation == 0 && !settings.showTeamPings) return;
        if (relation == 1 && !settings.showAllyPings) return;
        if (relation == 2 && !settings.showFriendPings) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            double dx = x - mc.thePlayer.posX;
            double dy = y - mc.thePlayer.posY;
            double dz = z - mc.thePlayer.posZ;
            if (dx * dx + dy * dy + dz * dz > settings.maxRange * settings.maxRange) return;
        }
        acquire(x, y, z, sender, relation);
        pushNotif("\u00A7b" + sender + " \u00BB Ping");
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    public void tick() {
        long now = System.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < MAX_PINGS; i++) {
            Ping p = pool[i];
            if (!p.inUse) continue;
            if (p.isExpired()) { p.recycle(); continue; }
            renderSnapshot[count++] = p;
        }
        for (int i = count; i < renderSnapshot.length; i++) renderSnapshot[i] = null;
        renderCount = count;

        for (int i = notifCount - 1; i >= 0; i--) {
            if (now - notifTime[i] > NOTIF_DURATION_MS) removeNotif(i);
        }
    }

    // ── Cooldown ─────────────────────────────────────────────────────────────

    public boolean canPing() {
        return settings.enabled && !isOnCooldown();
    }

    public long cooldownRemaining() {
        return Math.max(0L, settings.cooldownMs - (System.currentTimeMillis() - lastPingTime));
    }

    public boolean isOnCooldown() {
        return System.currentTimeMillis() - lastPingTime < settings.cooldownMs;
    }

    /** 0.0 = début de cooldown, 1.0 = prêt. */
    public float getCooldownProgress() {
        long elapsed = System.currentTimeMillis() - lastPingTime;
        return elapsed >= settings.cooldownMs ? 1.0f : (float) elapsed / settings.cooldownMs;
    }

    // ── Notifications HUD ─────────────────────────────────────────────────────

    public int    getNotifCount()     { return notifCount; }
    public String getNotifText(int i) { return notifText[i]; }
    public long   getNotifTime(int i) { return notifTime[i]; }

    // ── Internes ─────────────────────────────────────────────────────────────

    private void acquire(double x, double y, double z, String sender, int relation) {
        for (int i = 0; i < MAX_PINGS; i++) {
            if (!pool[i].inUse) {
                pool[i].init(x, y, z, sender, settings.durationMs);
                pool[i].relation = relation;
                return;
            }
        }
        // Pool plein : recycler le plus ancien
        int oldest = 0;
        for (int i = 1; i < MAX_PINGS; i++) {
            if (pool[i].createdAt < pool[oldest].createdAt) oldest = i;
        }
        pool[oldest].init(x, y, z, sender, settings.durationMs);
        pool[oldest].relation = relation;
    }

    private void pushNotif(String text) {
        if (notifCount >= MAX_NOTIFS) removeNotif(0);
        notifText[notifCount] = text;
        notifTime[notifCount] = System.currentTimeMillis();
        notifCount++;
    }

    private void removeNotif(int idx) {
        for (int i = idx; i < notifCount - 1; i++) {
            notifText[i] = notifText[i + 1];
            notifTime[i] = notifTime[i + 1];
        }
        notifCount--;
    }

    private static String localName() {
        Minecraft mc = Minecraft.getMinecraft();
        return (mc.thePlayer != null) ? mc.thePlayer.getName() : "?";
    }
}
