package com.opsorbis.game.systems;

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
import com.opsorbis.game.logic.GameManager;
import com.opsorbis.game.logic.PlayerCamp;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Archetype;

/**
 * Système chargé d'empêcher les joueurs d'infliger des dégâts à leurs propres PNJ défenseurs.
 */
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
        // Ignorer si les dégâts sont déjà annulés par un autre système
        if (event.isCancelled()) return;

        // Récupération de la référence et du composant de la victime (le PNJ)
        Ref<EntityStore> victimRef = chunk.getReferenceTo(entityId);
        NPCEntity victimNPC = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (victimNPC == null) return;

        // On vérifie si la source de l'attaque est une entité
        if (event.getSource() instanceof Damage.EntitySource) {
            Damage.EntitySource entitySource = (Damage.EntitySource) event.getSource();
            Ref<EntityStore> attackerRef = entitySource.getRef();
            
            // On vérifie si l'attaquant est un joueur
            Player attackerPlayer = store.getComponent(attackerRef, Player.getComponentType());
            if (attackerPlayer != null && gameManager.getNpcManager() != null) {
                // Identification de l'équipe du PNJ victime
                boolean isVictimNPC = gameManager.getNpcManager().estPnjBleu(victimRef) || 
                                     gameManager.getNpcManager().estPnjRouge(victimRef);
                
                // Si la victime est un de nos PNJ et l'attaquant est un défenseur, on bloque
                if (isVictimNPC && gameManager.getTeamManager().getCamp(attackerPlayer) == PlayerCamp.DEFENSEUR) {
                    event.setCancelled(true);
                    event.setAmount(0);
                    event.putMetaObject(Damage.BLOCKED, true);
                }
            }
        }
    }
}
