package com.experience.game.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.experience.game.logic.GameManager;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Archetype;

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
        if (event.isCancelled()) return;
        Ref<EntityStore> victimRef = chunk.getReferenceTo(entityId);
        Player victimPlayer = store.getComponent(victimRef, Player.getComponentType());
        if (victimPlayer == null) return;

        if (event.getSource() instanceof Damage.EntitySource) {
            Damage.EntitySource entitySource = (Damage.EntitySource) event.getSource();
            Ref<EntityStore> attackerRef = entitySource.getRef();
            
            Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
            if (attackerPlayer != null) {
                if (gameManager.getTeamManager().sontDansLaMemeEquipe(attackerPlayer, victimPlayer)) {
                    event.setCancelled(true);
                    event.setAmount(0);
                    event.putMetaObject(Damage.BLOCKED, true);
                }
            }
        }
    }
}
