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
 * Système ECS réagissant à la mort d'un joueur (ajout du DeathComponent).
 * Gère principalement le retour des reliques portées vers leur base d'origine.
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
        // Récupération de l'entité joueur décédée
        Player joueur = store.getComponent(ref, Player.getComponentType());
        
        // On n'agit que si le joueur est valide et que la partie est active
        if (joueur != null && gameManager.getEtatActuel() == GameManager.GameState.EN_COURS) {
            if (gameManager.getRelicManager() != null) {
                gameManager.getRelicManager().gererMortDuPorteur(joueur, store, buffer);
            }
        }
    }
}
