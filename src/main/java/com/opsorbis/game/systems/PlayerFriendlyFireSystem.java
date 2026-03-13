package com.opsorbis.game.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.opsorbis.game.logic.GameManager;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Archetype;

/**
 * Système chargé d'empêcher les joueurs d'une même équipe de s'infliger des dégâts.
 * Ce système s'enregistre dans le groupe de filtrage des dégâts d'Hytale.
 */
public class PlayerFriendlyFireSystem extends DamageEventSystem {

    private final GameManager gameManager;
    private final Query<EntityStore> query;

    public PlayerFriendlyFireSystem(GameManager gameManager) {
        this.gameManager = gameManager;
        this.query = Archetype.of(Player.getComponentType());
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public void handle(int entityId, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer, Damage event) {
        // Si l'évènement est déjà annulé, on ne fait rien
        if (event.isCancelled()) return;

        // Récupération de la victime
        Ref<EntityStore> victimRef = chunk.getReferenceTo(entityId);
        Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
        if (victimPlayer == null) return;

        // Vérification si la source des dégâts est une entité (joueur, projectile, etc.)
        if (event.getSource() instanceof Damage.EntitySource) {
            Damage.EntitySource entitySource = (Damage.EntitySource) event.getSource();
            Ref<EntityStore> attackerRef = entitySource.getRef();
            
            // Si l'attaquant est un joueur
            Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
            if (attackerPlayer != null) {
                // Si l'attaquant et la victime sont dans la même équipe, on annule les dégâts
                if (gameManager.getTeamManager().sontDansLaMemeEquipe(attackerPlayer, victimPlayer)) {
                    event.setCancelled(true);
                    event.setAmount(0);
                    event.putMetaObject(Damage.BLOCKED, true); // Informe Hytale que les dégâts ont été bloqués
                }
            }
        }
    }
}
