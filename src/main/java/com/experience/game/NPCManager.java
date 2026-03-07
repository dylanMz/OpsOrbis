package com.experience.game;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Gère le spawn et la suppression des PNJ des deux équipes.
 */
public class NPCManager {

    private final World monde;
    private Entity pnjBleu;
    private Entity pnjRouge;

    public static final float PNJ_MAX_HP = 500.0f;
    private static final Vector3f ROTATION_DEFAUT = new Vector3f(0, 0, 0);

    public NPCManager(World monde) {
        this.monde = monde;
    }

    /**
     * Fait apparaître les PNJ des deux équipes en utilisant les spawns configurés.
     */
    public void faireApparaitrePNJ() {
        if (monde == null) return;

        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Apparition des PNJ aux positions configurées...");

        // Spawn PNJ Bleu
        monde.execute(() -> {
            NPCEntity entiteBleu = new NPCEntity(monde);
            entiteBleu.setRoleName("hytale:polar_bear");
            pnjBleu = monde.addEntity(entiteBleu, config.getBlueSpawn(), ROTATION_DEFAUT, AddReason.SPAWN);
        });

        // Spawn PNJ Rouge
        monde.execute(() -> {
            NPCEntity entiteRouge = new NPCEntity(monde);
            entiteRouge.setRoleName("hytale:polar_bear");
            pnjRouge = monde.addEntity(entiteRouge, config.getRedSpawn(), ROTATION_DEFAUT, AddReason.SPAWN);
        });
    }

    public boolean estPnjBleu(Entity entite) {
        return entite != null && entite == pnjBleu;
    }

    public boolean estPnjRouge(Entity entite) {
        return entite != null && entite == pnjRouge;
    }

    public void supprimerPNJ() {
        pnjBleu = null;
        pnjRouge = null;
    }
}
