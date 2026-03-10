package com.experience.game.logic;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.IAttitudeProvider;

/**
 * Provider d'Attitude pour les PNJ.
 * Dans le mode Attaquants vs Défenseurs :
 * - Les PNJ sont ALLIÉS des défenseurs (FRIENDLY)
 * - Les PNJ sont ENNEMIS des attaquants (HOSTILE)
 */
public class TeamAttitudeProvider implements IAttitudeProvider {

    private final TeamManager teamManager;
    private final NPCManager npcManager;

    public TeamAttitudeProvider(TeamManager teamManager, NPCManager npcManager) {
        this.teamManager = teamManager;
        this.npcManager = npcManager;
    }

    @Override
    public Attitude getAttitude(Ref<EntityStore> npcRef, Role role, Ref<EntityStore> targetRef, ComponentAccessor<EntityStore> accessor) {
        // Vérifier que c'est bien un de nos PNJs
        boolean estPnjConnu = npcManager.estPnjBleu(npcRef) || npcManager.estPnjRouge(npcRef);
        if (!estPnjConnu) return null;

        // Vérifier si la cible est un joueur
        if (targetRef != null && accessor != null) {
            Player joueurCible = accessor.getComponent(targetRef, Player.getComponentType());
            if (joueurCible != null) {
                // Les PNJs protègent les défenseurs → amicaux
                if (teamManager.estDansEquipe(joueurCible, "Defenseur")) {
                    return Attitude.FRIENDLY;
                }
                // Les PNJs attaquent les attaquants → hostiles
                if (teamManager.estDansEquipe(joueurCible, "Attaquant")) {
                    return Attitude.HOSTILE;
                }
            }
        }

        return null; // Attitude par défaut du moteur
    }
}
