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
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.component.system.tick.ArchetypeTickingSystem;

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
    public boolean test(com.hypixel.hytale.component.ComponentRegistry<EntityStore> registry, Archetype<EntityStore> archetype) {
        return archetype.contains(Player.getComponentType()) && archetype.contains(TransformComponent.getComponentType());
    }

    @Override
    public void tick(float delta, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        if (gameManager == null || gameManager.getEtatActuel() != GameManager.GameState.EN_COURS) return;
        RelicManager rm = gameManager.getRelicManager();
        if (rm == null) return;

        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();

        for (int i = 0; i < chunk.size(); i++) {
            // Si le joueur est mort, il ne peut pas ramasser d'objet
            if (chunk.getComponent(i, DeathComponent.getComponentType()) != null) continue;

            Player joueur = chunk.getComponent(i, Player.getComponentType());
            TransformComponent transform = chunk.getComponent(i, TransformComponent.getComponentType());
            if (joueur == null || transform == null) continue;
            
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
                // Relique lâchée sur le terrain — quiconque passe dessus interagit
                if (pos.distanceTo(positionLachee1) <= PICKUP_RANGE) {
                    rm.ramasserRelique(joueur, 1, buffer);
                    continue;
                }
            } else if (rm.estReliqueDisponible(false, 1) && positionOrigine1 != null && pos.distanceTo(positionOrigine1) <= PICKUP_RANGE) {
                // Relique à sa base — seuls les attaquants peuvent la voler
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
