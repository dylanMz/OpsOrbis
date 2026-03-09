package com.experience.game.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.experience.game.logic.GameManager;
import com.experience.game.logic.NPCManager;
import com.experience.game.logic.TeamAttitudeProvider;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Archetype;

public class FriendlyFireSystem extends DamageEventSystem {

    private final GameManager gameManager;
    private final Query<EntityStore> query;

    public FriendlyFireSystem(GameManager gameManager) {
        this.gameManager = gameManager;
        this.query = Archetype.of(NPCEntity.getComponentType());
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
        NPCEntity victimNPC = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (victimNPC == null) return;

        if (event.getSource() instanceof Damage.EntitySource) {
            Damage.EntitySource entitySource = (Damage.EntitySource) event.getSource();
            Ref<EntityStore> attackerRef = entitySource.getRef();
            
            Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
            if (attackerPlayer != null && gameManager.getNpcManager() != null) {
                boolean isVictimBlueNPC = gameManager.getNpcManager().estPnjBleu(victimRef);
                boolean isVictimRedNPC = gameManager.getNpcManager().estPnjRouge(victimRef);
                
                if (isVictimBlueNPC && gameManager.getTeamManager().estDansEquipe(attackerPlayer, "Bleue")) {
                    event.setCancelled(true);
                    event.setAmount(0);
                    event.putMetaObject(Damage.BLOCKED, true);
                } else if (isVictimRedNPC && gameManager.getTeamManager().estDansEquipe(attackerPlayer, "Rouge")) {
                    event.setCancelled(true);
                    event.setAmount(0);
                    event.putMetaObject(Damage.BLOCKED, true);
                }
            }
        }
    }
}
