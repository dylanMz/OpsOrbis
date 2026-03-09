package com.experience.game;

import com.experience.ExperienceMod;
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
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

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

        NPCManager npcManager = gameManager.getNpcManager();
        TeamManager teamManager = gameManager.getTeamManager();
        if (npcManager == null || teamManager == null) return;

        // La victime est elle un de nos PNJ ?
        Ref<EntityStore> victimRef = chunk.getReferenceTo(entityId);
        boolean isBlueNPC = npcManager.estPnjBleu(victimRef);
        boolean isRedNPC = npcManager.estPnjRouge(victimRef);

        if (!isBlueNPC && !isRedNPC) {
            return;
        }

        // Récupérer l'attaquant
        if (event.getSource() instanceof Damage.EntitySource) {
            Damage.EntitySource entitySource = (Damage.EntitySource) event.getSource();
            Ref<EntityStore> attackerRef = entitySource.getRef();
            
            // Vérifier si l'attaquant a la composante Player
            Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
            if (attackerPlayer != null) {
                // Annuler les dégâts si l'attaquant est dans la même équipe
                if (isBlueNPC && teamManager.estDansEquipe(attackerPlayer, "Bleue")) {
                    event.setCancelled(true);
                    event.setAmount(0);
                    event.putMetaObject(Damage.BLOCKED, true);
                } else if (isRedNPC && teamManager.estDansEquipe(attackerPlayer, "Rouge")) {
                    event.setCancelled(true);
                    event.setAmount(0);
                    event.putMetaObject(Damage.BLOCKED, true);
                }
            }
        }
    }
}
