package com.opsorbis.game.systems;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.MapConfig;
import com.opsorbis.game.logic.GameManager;
import com.opsorbis.game.logic.RelicManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.opsorbis.utils.HytaleUtils;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.component.system.tick.ArchetypeTickingSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.UUID;

/**
 * Détecte et gère la proximité entre les joueurs et les reliques.
 * <p>
 * Deux cas :
 *  1. Relique à sa position d'origine  → les attaquants la ramassent.
 *  2. Relique lâchée sur le terrain     → les attaquants la récupèrent,
 *                                          les défenseurs la renvoient à la base.
 */
public class RelicPickupSystem extends ArchetypeTickingSystem<EntityStore> {

    private static final double PICKUP_RANGE = 1.5;

    private final Query<EntityStore> query;
    private final GameManager gameManager;

    public RelicPickupSystem(GameManager gameManager) {
        this.gameManager = gameManager;
        this.query = Archetype.of(Player.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public Query<EntityStore> getQuery() { return query; }

    @Override
    public boolean test(@NonNullDecl com.hypixel.hytale.component.ComponentRegistry<EntityStore> registry, Archetype<EntityStore> archetype) {
        return archetype.contains(Player.getComponentType()) && archetype.contains(TransformComponent.getComponentType());
    }

    @Override
    public void tick(float delta, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> buffer) {
        for (int i = 0; i < chunk.size(); i++) {
            Player joueur = chunk.getComponent(i, Player.getComponentType());
            TransformComponent transform = chunk.getComponent(i, TransformComponent.getComponentType());
            if (joueur == null || transform == null) continue;

            // Trouver le match du joueur
            com.opsorbis.game.logic.MatchInstance match = gameManager.getMatchParJoueur(joueur);
            if (match == null || match.getEtatActuel() != com.opsorbis.game.logic.GameManager.GameState.EN_COURS) continue;

            RelicManager rm = match.getRelicManager();
            if (rm == null) continue;
            MapConfig config = match.getMapConfig();

            // Si le joueur est mort, il ne peut pas ramasser d'objet
            if (chunk.getComponent(i, DeathComponent.getComponentType()) != null) continue;

            // --- NOUVEAU : Cooldown de 10s après reconnexion ---
            UUID uuid = HytaleUtils.getPlayerUuid(joueur);
            if (match.estBloqueRamassageRelique(uuid)) {
                continue;
            }
            
            Player c1 = rm.getCarrierRelic1();
            Player c2 = rm.getCarrierRelic2();
            if ((c1 != null && java.util.Objects.equals(joueur.getReference(), c1.getReference())) || 
                (c2 != null && java.util.Objects.equals(joueur.getReference(), c2.getReference()))) {
                continue;
            }

            Vector3d pos = transform.getPosition();

            // ── Relique 1 ──────────────────────────────────────────────────────
            Vector3d positionLachee1 = rm.getRelic1DroppedPos();
            Vector3d positionOrigine1 = config.getRelic1();

            if (positionLachee1 != null) {
                if (pos.distanceTo(positionLachee1) <= PICKUP_RANGE) {
                    rm.ramasserRelique(joueur, 1, buffer);
                    continue;
                }
            } else if (rm.estReliqueDisponible(false, 1) && positionOrigine1 != null && pos.distanceTo(positionOrigine1) <= PICKUP_RANGE) {
                rm.ramasserRelique(joueur, 1, buffer);
                continue;
            }

            // ── Relique 2 ──────────────────────────────────────────────────────
            Vector3d positionLachee2 = rm.getRelic2DroppedPos();
            Vector3d positionOrigine2 = config.getRelic2();

            if (positionLachee2 != null) {
                if (pos.distanceTo(positionLachee2) <= PICKUP_RANGE) {
                    rm.ramasserRelique(joueur, 2, buffer);
                }
            } else if (rm.estReliqueDisponible(false, 2) && positionOrigine2 != null && pos.distanceTo(positionOrigine2) <= PICKUP_RANGE) {
                rm.ramasserRelique(joueur, 2, buffer);
            }
        }
    }
}
