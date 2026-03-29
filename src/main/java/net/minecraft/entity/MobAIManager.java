package net.minecraft.entity;

import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.pathfinding.PathNavigate;

/**
 * Utilitaire pour activer/désactiver l'IA des entités côté serveur (MCP 1.8.9).
 */
public class MobAIManager {

    /**
     * Désactive totalement l'IA d'une entité EntityLiving (ou sous-classe).
     * Après cet appel, les tâches AI, la navigation et les contrôleurs sont stoppés.
     */
    public static void disableAI(EntityLiving entity) {
        if (entity == null) return;
        entity.aiDisabled = true;

        // Vide les goals existants
        if (entity.tasks != null) entity.tasks.clearTasks();
        if (entity.targetTasks != null) entity.targetTasks.clearTasks();

        // Stoppe la navigation en cours
        PathNavigate nav = entity.getNavigator();
        if (nav != null) nav.clearPathEntity();

        // Stoppe la vélocité horizontale
        entity.motionX = 0.0D;
        entity.motionZ = 0.0D;
    }

    /**
     * Réactive l'IA d'une entité (ne restaure pas les tasks précédentes automatiquement).
     */
    public static void enableAI(EntityLiving entity) {
        if (entity == null) return;
        entity.aiDisabled = false;
    }
}

