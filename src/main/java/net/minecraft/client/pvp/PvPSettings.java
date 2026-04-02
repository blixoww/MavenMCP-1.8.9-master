package net.minecraft.client.pvp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Configuration centralisée pour l'optimisation PvP.
 * Tous les paramètres sont sauvegardés dans config/pvp.json
 * et peuvent être modifiés en jeu.
 */
public class PvPSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File SETTINGS_FILE = new File(Minecraft.getMinecraft().mcDataDir, "config/pvp.json");

    private static PvPSettings instance;

    // ── Knockback - Base ─────────────────────────────────────────────────
    // Facteur de réduction de la vélocité existante avant d'appliquer le KB.
    // Vanilla = 0.5 (divise par 2). Plus haut = KB plus consistant car moins
    // dépendant du mouvement actuel de la cible.
    public double kbFriction = 0.6;

    // Force horizontale du knockback de base.
    // Vanilla = 0.4. Augmenter = plus de recul horizontal = meilleurs combos.
    public double kbHorizontal = 0.4;

    // Force verticale du knockback de base.
    // Vanilla = 0.4. Réduire légèrement = moins "floaty", combat plus ancré au sol.
    public double kbVertical = 0.36;

    // Cap maximum de la vélocité verticale lors d'un KB.
    // Vanilla = 0.4. Empêche les joueurs de s'envoler trop haut.
    public double kbVerticalCap = 0.4;

    // ── Knockback - Sprint (W-tap) ───────────────────────────────────────
    // Multiplicateur horizontal par niveau de KB lors d'un sprint hit.
    // Vanilla = 0.5. Augmenter = récompense davantage le W-tap.
    public double sprintKbHorizontal = 0.6;

    // Force verticale additionnelle lors d'un sprint hit.
    // Vanilla = 0.1. Augmenter légèrement aide à maintenir les combos.
    public double sprintKbVertical = 0.13;

    // Facteur de ralentissement de l'attaquant après un sprint hit.
    // Vanilla = 0.6. Plus bas = l'attaquant garde plus de vitesse après le hit.
    public double sprintAttackerSlowdown = 0.6;

    // ── Hit Registration / I-Frames ──────────────────────────────────────
    // Durée d'invulnérabilité en ticks après un hit (i-frames).
    // Vanilla = 20 (la cible ne peut être re-hit que quand hurtResistantTime <= 10).
    // 18 = hit toutes les 9 ticks au lieu de 10 → récompense un CPS légèrement plus élevé.
    // 16 = hit toutes les 8 ticks → encore plus agressif.
    public int maxHurtResistantTime = 19;

    // ── Click Processing ─────────────────────────────────────────────────
    // Cooldown en ticks quand le joueur miss dans le vide (pas de bloc ni d'entité).
    // Vanilla = 10 (0.5s de cooldown). 0 = aucun cooldown = plus réactif.
    public int missCooldownTicks = 0;

    // ── Hit Detection ────────────────────────────────────────────────────
    // Reach distance pour toucher des entités en survival.
    // Vanilla = 3.0 (blocs). La plupart des serveurs PvP gardent 3.0.
    public double entityReachDistance = 3.0;

    // Expansion de la hitbox pour le ray tracing de détection.
    // Vanilla = 0.1. Plus bas = hitbox plus précise, récompense l'aim.
    // Plus haut = plus facile de toucher (mais moins skill-based).
    public float collisionBorderSize = 0.1F;

    // ── Avancé ───────────────────────────────────────────────────────────
    // Appliquer le knockback vertical même si la cible est au sol.
    // Cela garantit que la cible décolle toujours légèrement, rendant les combos
    // plus viables et le timing du W-tap plus important.
    public boolean alwaysApplyVerticalKb = true;

    // Multiplicateur de dégâts des coups critiques.
    // Vanilla = 1.5 (50% de bonus). Ajuster pour récompenser les crits.
    public float critDamageMultiplier = 1.5F;

    public void save() {
        try {
            if (!SETTINGS_FILE.getParentFile().exists()) SETTINGS_FILE.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(SETTINGS_FILE);
            GSON.toJson(this, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PvPSettings load() {
        try {
            if (SETTINGS_FILE.exists()) {
                FileReader reader = new FileReader(SETTINGS_FILE);
                PvPSettings s = GSON.fromJson(reader, PvPSettings.class);
                reader.close();
                if (s != null) {
                    instance = s;
                    return s;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        instance = new PvPSettings();
        return instance;
    }

    public static PvPSettings get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
}
