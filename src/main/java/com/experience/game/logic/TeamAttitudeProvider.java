package com.experience.game.logic;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.IAttitudeProvider;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Provider d'Attitude dynamique pour les PNJ.
 * Permet de définir si un joueur est allié (FRIENDLY) ou ennemi (HOSTILE)
 * en fonction de son équipe définie dans TeamManager.
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
        // 1. Déterminer l'équipe du PNJ qui effectue l'interrogation (l'observateur)
        boolean isBlue = npcManager.estPnjBleu(npcRef);
        boolean isRed = npcManager.estPnjRouge(npcRef);
        
        // Si c'est un PNJ inconnu, on ne gère pas son attitude ici
        if (!isBlue && !isRed) return null;
        
        String maTeam = isBlue ? "Bleue" : "Rouge";

        // 2. Vérifier si la cible de l'attention du PNJ est un joueur
        if (targetRef != null && accessor != null) {
            Player targetPlayerEntity = accessor.getComponent(targetRef, Player.getComponentType());
            if (targetPlayerEntity != null) {
                // Si le joueur est de la même équipe -> On est AMIS
                if (teamManager.estDansEquipe(targetPlayerEntity, maTeam)) {
                    return Attitude.FRIENDLY;
                } else {
                    // Joueur d'une équipe adverse -> On est HOSTILES
                    return Attitude.HOSTILE;
                }
            }
        }
        
        return null; // Attitude par défaut du moteur Hytale
    }
}
