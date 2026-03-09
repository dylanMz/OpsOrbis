package com.experience.game.systems;

import com.experience.game.logic.GameManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.RespawnSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.ItemStack;

/**
 * Système ECS gérant le respawn du joueur.
 */
public class PlayerRespawnSystem extends RespawnSystems.OnRespawnSystem {

    private final GameManager gameManager;
    private final Query<EntityStore> query;

    public PlayerRespawnSystem(GameManager gameManager) {
        this.gameManager = gameManager;
        this.query = Archetype.of(Player.getComponentType());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void onComponentAdded(Ref<EntityStore> ref, DeathComponent addedComponent, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        // Optionnel: on peut agir ici quand le joueur meurt (ajout du DeathComponent)
    }

    @Override
    public void onComponentRemoved(Ref<EntityStore> ref, DeathComponent removedComponent, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        Player joueur = store.getComponent(ref, Player.getComponentType());
        if (joueur != null && gameManager.getEtatActuel() == GameManager.GameState.EN_COURS) {
            // Téléportation au spawn de l'équipe après le respawn (quand DeathComponent est retiré)
            if (gameManager.getTeamManager() != null) {
                gameManager.getTeamManager().teleporterAuSpawn(joueur);
            }
            
            // Sécurité : Retirer la relique si elle a persisté malgré la mort
            joueur.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));

            // On peut aussi lui redonner son kit ici
            if (gameManager.getKitManager() != null) {
                gameManager.getKitManager().donnerEquipement(joueur);
            }
        }
    }
}
