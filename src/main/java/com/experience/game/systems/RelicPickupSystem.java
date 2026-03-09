package com.experience.game.systems;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
import com.experience.game.logic.GameManager;
import com.experience.game.logic.RelicManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.system.tick.ArchetypeTickingSystem;

/**
 * Système ECS de type ArchetypeTickingSystem qui vérifie périodiquement 
 * la proximité entre les joueurs et les points de spawn des reliques.
 */
public class RelicPickupSystem extends ArchetypeTickingSystem<EntityStore> {

    private final Query<EntityStore> query;
    private final GameManager gameManager;

    public RelicPickupSystem(GameManager gameManager) {
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
        // On ne vérifie que si la partie est en cours
        if (gameManager == null || gameManager.getEtatActuel() != GameManager.GameState.EN_COURS || gameManager.getRelicManager() == null) return;
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        RelicManager relicManager = gameManager.getRelicManager();

        for (int i = 0; i < chunk.size(); i++) {
            Player joueur = chunk.getComponent(i, Player.getComponentType());
            TransformComponent transform = chunk.getComponent(i, TransformComponent.getComponentType());
            if (joueur == null || transform == null) continue;

            Vector3d pos = transform.getPosition();
            double pRange = 1.5; // Distance de ramassage

            // Vérification pour chaque relique (disponibilité, porteur actuel, distance)
            if (config.getBlueRelic1() != null && relicManager.estReliqueDisponible(true, 1) && relicManager.getCarrierBlueRelic1() == null && pos.distanceTo(config.getBlueRelic1()) <= pRange) {
                relicManager.ramasserRelique(joueur, true, 1, buffer);
            }
            else if (config.getBlueRelic2() != null && relicManager.estReliqueDisponible(true, 2) && relicManager.getCarrierBlueRelic2() == null && pos.distanceTo(config.getBlueRelic2()) <= pRange) {
                relicManager.ramasserRelique(joueur, true, 2, buffer);
            }
            else if (config.getRedRelic1() != null && relicManager.estReliqueDisponible(false, 1) && relicManager.getCarrierRedRelic1() == null && pos.distanceTo(config.getRedRelic1()) <= pRange) {
                relicManager.ramasserRelique(joueur, false, 1, buffer);
            }
            else if (config.getRedRelic2() != null && relicManager.estReliqueDisponible(false, 2) && relicManager.getCarrierRedRelic2() == null && pos.distanceTo(config.getRedRelic2()) <= pRange) {
                relicManager.ramasserRelique(joueur, false, 2, buffer);
            }
        }
    }
}
