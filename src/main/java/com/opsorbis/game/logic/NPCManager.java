package com.opsorbis.game.logic;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.MapConfig;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.blackboard.Blackboard;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.AttitudeView;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.IAttitudeProvider;
import it.unimi.dsi.fastutil.Pair;
import java.util.logging.Level;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le cycle de vie des PNJ (Skelettes Brûlés) pour un match spécifique.
 * Responsabilités :
 * - Apparition des PNJ protecteurs au début de chaque manche.
 * - Configuration de l'IA (Laisse/Leash et Alliances/Attitude).
 * - Nettoyage des PNJ à la fin de la manche.
 */
public class NPCManager {

    /** Monde Hytale de l'instance. */
    private final World monde;
    
    /** TeamManager de l'instance pour les Alliances (IA). */
    private final TeamManager teamManager;
    
    /** Références ECS des deux PNJ défenseurs. */
    private Ref<EntityStore> pnjBleuRef;
    private Ref<EntityStore> pnjRougeRef;
    
    /** Points de spawn initiaux (utilisés pour le retour à la base). */
    private Vector3d spawnBleu;
    private Vector3d spawnRouge;

    /** Liste des PNJ actuellement hors de leur zone et tentant de revenir (Leash). */
    private final Set<Ref<EntityStore>> returningNPCs = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    /** Points de vie maximum des PNJ défenseurs. */
    public static final float PNJ_MAX_HP = 500.0f;
    
    /** Rotation par défaut lors de l'apparition. */
    private static final Vector3f ROTATION_DEFAUT = new Vector3f(0, 0, 0);

    public boolean isReturningHome(Ref<EntityStore> ref) {
        return returningNPCs.contains(ref);
    }

    public void setReturningHome(Ref<EntityStore> ref, boolean returning) {
        if (returning) {
            returningNPCs.add(ref);
        } else {
            returningNPCs.remove(ref);
        }
    }

    /** Instance de match parente. */
    private final MatchInstance match;
 
    /**
     * @param match L'instance de match parente.
     */
    public NPCManager(MatchInstance match) {
        this.match = match;
        this.monde = match.getWorld();
        this.teamManager = match.getTeamManager();
    }

    public World getWorld() {
        return monde;
    }

    /**
     * Fait apparaître les PNJ Skelettes (Archers Brûlés) pour les deux équipes.
     * Les positions sont récupérées depuis la configuration du jeu.
     */
    public void faireApparaitrePNJ() {
        if (monde == null || monde.getEntityStore() == null) return;
        monde.execute(() -> faireApparaitrePNJ_Direct(monde.getEntityStore().getStore()));
    }

    /**
     * Fait apparaître les PNJ directement (doit être appelé sur le thread monde).
     */
    public void faireApparaitrePNJ_Direct(Store<EntityStore> store) {
        MapConfig config = OpsOrbis.get().getConfigManager().getMapConfig();
        String npcType = "SKELETON_BURNT_ARCHER";

            // ==== SPAWN PNJ BLEU ====
            Vector3d posBleu = config.getBlueNpcSpawn();
            if (posBleu == null) {
                HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] ERREUR CRITIQUE: La position du spawn Bleu est NULL !");
            } else {
                Pair<Ref<EntityStore>, INonPlayerCharacter> resultBleu = NPCPlugin.get().spawnNPC(store, npcType, null, posBleu, ROTATION_DEFAUT);
                
                if (resultBleu == null || resultBleu.first() == null) {
                    HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Échec du spawn du PNJ Bleu: " + npcType + " introuvable ou erreur interne !");
                } else {
                    pnjBleuRef = resultBleu.first();
                    spawnBleu = posBleu;
                    configurerIA(pnjBleuRef, store, posBleu, true);
                    HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] PNJ Bleu a spawn avec succès !");
                }
            }

            // ==== SPAWN PNJ ROUGE ====
            Vector3d posRouge = config.getRedNpcSpawn();
            if (posRouge == null) {
                HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] ERREUR CRITIQUE: La position du spawn Rouge est NULL !");
            } else {
                Pair<Ref<EntityStore>, INonPlayerCharacter> resultRouge = NPCPlugin.get().spawnNPC(store, npcType, null, posRouge, ROTATION_DEFAUT);
                
                if (resultRouge == null || resultRouge.first() == null) {
                    HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Échec du spawn du PNJ Rouge: " + npcType + " introuvable ou erreur interne !");
                } else {
                    pnjRougeRef = resultRouge.first();
                    spawnRouge = posRouge;
                    configurerIA(pnjRougeRef, store, posRouge, false);
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] PNJ Rouge a spawn avec succès !");
            }
        }
    }

    /**
     * Configure la Laisse (Leash) pour qu'il reste sur son point de spawn
     * et l'Attitude (Alliances) pour qu'il ne s'en prenne pas à ses alliés.
     */
    private void configurerIA(Ref<EntityStore> npcRef, Store<EntityStore> store, Vector3d pointAttache, boolean isBlueTeam) {
        NPCEntity npcEntity = store.getComponent(npcRef, NPCEntity.getComponentType());
        if (npcEntity != null) {
            // Empêcher de s'éloigner du point de spawn (Laisse Hytale native)
            npcEntity.saveLeashInformation(pointAttache, ROTATION_DEFAUT);
        }

        // Configuration de l'Attitude (Alliances)
        Blackboard blackboard = store.getResource(Blackboard.getResourceType());
        if (blackboard != null) {
            AttitudeView attitudeView = blackboard.getView(AttitudeView.class, npcRef, store);
            if (attitudeView != null) {
                // On enregistre le provider qui gère dynamiquement l'équipe selon le npcRef
                attitudeView.registerProvider(IAttitudeProvider.OVERRIDE_PRIORITY, new TeamAttitudeProvider(teamManager, this));
            }
        }
    }

    public boolean estPnjBleu(Ref<EntityStore> entityRef) {
        return entityRef != null && entityRef.equals(pnjBleuRef);
    }

    public boolean estPnjRouge(Ref<EntityStore> entityRef) {
        return entityRef != null && entityRef.equals(pnjRougeRef);
    }

    public Vector3d getHomePosition(Ref<EntityStore> npcRef) {
        if (estPnjBleu(npcRef)) return spawnBleu;
        if (estPnjRouge(npcRef)) return spawnRouge;
        return null;
    }

    public boolean estPnjBleu(Entity entite) {
        return entite != null && estPnjBleu(entite.getReference());
    }

    public boolean estPnjRouge(Entity entite) {
        return entite != null && estPnjRouge(entite.getReference());
    }

    /**
     * Nettoie les PNJ de ce match en cherchant les entités proches des points de spawn.
     * C'est une sécurité si les Refs sont perdues.
     */
    private void nettoyerPNJCible(Store<EntityStore> store) {
        if (store == null) return;
        MapConfig config = OpsOrbis.get().getConfigManager().getMapConfig();
        Vector3d sB = config.getBlueNpcSpawn();
        Vector3d sR = config.getRedNpcSpawn();
        
        Query<EntityStore> query = Archetype.of(NPCEntity.getComponentType(), TransformComponent.getComponentType());
        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                if (tc != null) {
                    Vector3d pos = tc.getPosition();
                    // Si le PNJ est à moins de 25 blocs d'un de nos spawns, c'est probablement le nôtre
                    if ((sB != null && pos.distanceTo(sB) < 25) || (sR != null && pos.distanceTo(sR) < 25)) {
                        buffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                    }
                }
            }
        });
    }

    public void supprimerPNJ(CommandBuffer<EntityStore> buffer) {
        if (monde != null) {
            Ref<EntityStore> bRef = pnjBleuRef;
            Ref<EntityStore> rRef = pnjRougeRef;
            
            // Suppression via Buffer (système ECS tick)
            if (buffer != null) {
                try { if (bRef != null && bRef.isValid()) buffer.removeEntity(bRef, RemoveReason.REMOVE); } catch (Exception ignored) {}
                try { if (rRef != null && rRef.isValid()) buffer.removeEntity(rRef, RemoveReason.REMOVE); } catch (Exception ignored) {}
                // On ne fait PAS de nettoyerPNJCible(store) ici car store.forEachChunk 
                // peut provoquer un crash s'il est appelé pendant un tick ECS (processing).
            } 
            // Suppression directe (thread simulation)
            else if (monde.getEntityStore() != null) {
                Store<EntityStore> store = monde.getEntityStore().getStore();
                try { if (bRef != null && bRef.isValid()) store.removeEntity(bRef, RemoveReason.REMOVE); } catch (Exception ignored) {}
                try { if (rRef != null && rRef.isValid()) store.removeEntity(rRef, RemoveReason.REMOVE); } catch (Exception ignored) {}
                nettoyerPNJCible(store);
            }
        }
        pnjBleuRef = null;
        pnjRougeRef = null;
    }
}
