package com.opsorbis.game.systems;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.GameConfig;
import com.opsorbis.game.logic.GameManager;
import com.opsorbis.game.logic.RelicManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.system.tick.ArchetypeTickingSystem;

/**
 * Système ECS qui vérifie périodiquement si un porteur de relique est 
 * entré dans sa propre zone de dépôt.
 */
public class RelicDepositSystem extends ArchetypeTickingSystem<EntityStore> {

    private final Query<EntityStore> query;
    private final GameManager gameManager;

    public RelicDepositSystem(GameManager gameManager) {
        this.gameManager = gameManager;
        this.query = Archetype.of(Player.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public boolean test(com.hypixel.hytale.component.ComponentRegistry<EntityStore> registry, Archetype<EntityStore> archetype) {
        return archetype.contains(Player.getComponentType()) && archetype.contains(TransformComponent.getComponentType());
    }

    @Override
    public void tick(float delta, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        if (gameManager == null || gameManager.getEtatActuel() != GameManager.GameState.EN_COURS || gameManager.getRelicManager() == null) return;
        RelicManager relicManager = gameManager.getRelicManager();

        for (int i = 0; i < chunk.size(); i++) {
            Player joueur = chunk.getComponent(i, Player.getComponentType());
            if (joueur == null) continue;

            // Appel de la vérification de dépôt dans le manager
            relicManager.verifierDepot(joueur, buffer);
        }
    }
}
