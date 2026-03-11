package com.experience.game.systems;

import com.experience.game.logic.GameManager;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.system.tick.ArchetypeTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;

/**
 * Système ECS autonome qui gère le chrono de la manche en cours.
 */
public class MatchTimerSystem extends ArchetypeTickingSystem<EntityStore> {

    private final GameManager gameManager;
    private final Query<EntityStore> query;

    public MatchTimerSystem(GameManager gameManager) {
        this.gameManager = gameManager;
        // La query ici est arbitraire car on a juste besoin que le tick s'exécute, 
        // interroger les joueurs est un bon point d'ancrage.
        this.query = Archetype.of(Player.getComponentType());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public boolean test(com.hypixel.hytale.component.ComponentRegistry<EntityStore> registry, Archetype<EntityStore> archetype) {
        return archetype.contains(Player.getComponentType());
    }

    @Override
    public void tick(float delta, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Appeler tickChrono seulement s'il y a du monde pour ne pas boucler inutilement
        if (chunk.size() > 0) {
            Player p = chunk.getComponent(0, Player.getComponentType());
            if (p != null && p.getWorld() != null) {
                gameManager.tickChrono(p.getWorld(), buffer);
            }
        }
    }
}
