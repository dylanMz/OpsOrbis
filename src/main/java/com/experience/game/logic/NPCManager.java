package com.experience.game.logic;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
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
import com.hypixel.hytale.logger.HytaleLogger;
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
 * Gère le spawn et la suppression des PNJ des deux équipes.
 */
public class NPCManager {

    private final World monde;
    private final TeamManager teamManager;
    private Ref<EntityStore> pnjBleuRef;
    private Ref<EntityStore> pnjRougeRef;
    private Vector3d spawnBleu;
    private Vector3d spawnRouge;

    // Liste des PNJ en cours de retour au bercail (leash break)
    private final Set<Ref<EntityStore>> returningNPCs = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Points de vie maximum des PNJ défenseurs
    public static final float PNJ_MAX_HP = 500.0f;
    
    // Rotation par défaut lors de l'apparition
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

    public NPCManager(World monde, TeamManager teamManager) {
        this.monde = monde;
        this.teamManager = teamManager;
    }

    /**
     * Fait apparaître les PNJ Skelettes (Archers Brûlés) pour les deux équipes.
     * Les positions sont récupérées depuis la configuration du jeu.
     */
    public void faireApparaitrePNJ() {
        if (monde == null) return;

        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Apparition des PNJ aux positions configurées via NPCPlugin...");

        // Exécution différée pour respecter le thread de simulation
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            String npcType = "SKELETON_BURNT_ARCHER";

            // ==== SPAWN PNJ BLEU ====
            Vector3d posBleu = config.getBlueNpcSpawn();
            if (posBleu == null) {
                HytaleLogger.getLogger().at(Level.SEVERE).log("[ExperienceMod] ERREUR CRITIQUE: La position du spawn Bleu est NULL !");
            } else {
                Pair<Ref<EntityStore>, INonPlayerCharacter> resultBleu = NPCPlugin.get().spawnNPC(store, npcType, null, posBleu, ROTATION_DEFAUT);
                
                if (resultBleu == null || resultBleu.first() == null) {
                    HytaleLogger.getLogger().at(Level.SEVERE).log("[ExperienceMod] Échec du spawn du PNJ Bleu: " + npcType + " introuvable ou erreur interne !");
                } else {
                    pnjBleuRef = resultBleu.first();
                    spawnBleu = posBleu;
                    configurerIA(pnjBleuRef, store, posBleu, true);
                    HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] PNJ Bleu a spawn avec succès !");
                }
            }

            // ==== SPAWN PNJ ROUGE ====
            Vector3d posRouge = config.getRedNpcSpawn();
            if (posRouge == null) {
                HytaleLogger.getLogger().at(Level.SEVERE).log("[ExperienceMod] ERREUR CRITIQUE: La position du spawn Rouge est NULL !");
            } else {
                Pair<Ref<EntityStore>, INonPlayerCharacter> resultRouge = NPCPlugin.get().spawnNPC(store, npcType, null, posRouge, ROTATION_DEFAUT);
                
                if (resultRouge == null || resultRouge.first() == null) {
                    HytaleLogger.getLogger().at(Level.SEVERE).log("[ExperienceMod] Échec du spawn du PNJ Rouge: " + npcType + " introuvable ou erreur interne !");
                } else {
                    pnjRougeRef = resultRouge.first();
                    spawnRouge = posRouge;
                    configurerIA(pnjRougeRef, store, posRouge, false);
                    HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] PNJ Rouge a spawn avec succès !");
                }
            }
        });
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
     * Supprime proprement les PNJ du monde (fin de partie).
     * @param buffer Optionnel: Buffer ECS pour suppression différée.
     */
    public void supprimerPNJ(CommandBuffer<EntityStore> buffer) {
        if (monde != null) {
            Ref<EntityStore> bRef = pnjBleuRef;
            Ref<EntityStore> rRef = pnjRougeRef;
            
            // Suppression via Buffer (système ECS tick)
            if (buffer != null) {
                try { if (bRef != null) buffer.removeEntity(bRef, RemoveReason.REMOVE); } catch (Exception ignored) {}
                try { if (rRef != null) buffer.removeEntity(rRef, RemoveReason.REMOVE); } catch (Exception ignored) {}
            } 
            // Suppression directe (thread simulation)
            else {
                monde.execute(() -> {
                    Store<EntityStore> store = monde.getEntityStore().getStore();
                    try { if (bRef != null) store.removeEntity(bRef, RemoveReason.REMOVE); } catch (Exception ignored) {}
                    try { if (rRef != null) store.removeEntity(rRef, RemoveReason.REMOVE); } catch (Exception ignored) {}
                });
            }
        }
        pnjBleuRef = null;
        pnjRougeRef = null;
    }
}
