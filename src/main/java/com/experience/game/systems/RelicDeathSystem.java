package com.experience.game.systems;

import com.experience.game.logic.GameManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Système ECS réagissant à la mort d'un joueur.
 * Permet de faire tomber la relique et de téléporter le joueur au spawn.
 */
public class RelicDeathSystem extends DeathSystems.OnDeathSystem {

    private final GameManager gameManager;
    private final Query<EntityStore> query;

    public RelicDeathSystem(GameManager gameManager) {
        this.gameManager = gameManager;
        this.query = Archetype.of(Player.getComponentType(), DeathComponent.getComponentType());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent death, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        Player joueur = store.getComponent(ref, Player.getComponentType());
        if (joueur != null && gameManager.getEtatActuel() == GameManager.GameState.EN_COURS) {
            
            // 1. Retour de la relique si le joueur en portait une
            if (gameManager.getRelicManager() != null) {
                gameManager.getRelicManager().gererMortDuPorteur(joueur);
            }
        }
    }
}
