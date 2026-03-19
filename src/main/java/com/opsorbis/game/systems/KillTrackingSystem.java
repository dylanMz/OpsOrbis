package com.opsorbis.game.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.opsorbis.game.logic.GameManager;

import javax.annotation.Nonnull;

/**
 * Système ECS qui écoute les événements de kill pour mettre à jour les statistiques.
 * KillFeedEvent.KillerMessage est envoyé à l'entité qui a tué.
 */
public class KillTrackingSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {

    private final GameManager gameManager;

    public KillTrackingSystem(GameManager gameManager) {
        super(KillFeedEvent.KillerMessage.class);
        this.gameManager = gameManager;
    }

    @Override
    public com.hypixel.hytale.component.query.Query<EntityStore> getQuery() {
        return com.hypixel.hytale.component.Archetype.of(Player.getComponentType());
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer, @Nonnull KillFeedEvent.KillerMessage event) {
        if (gameManager.getEtatActuel() != GameManager.GameState.EN_COURS) return;

        // On vérifie si le tueur est un joueur
        Player tueur = chunk.getComponent(index, Player.getComponentType());
        if (tueur != null) {
            gameManager.getStatsManager().incrementKill(tueur);
        }
    }
}
