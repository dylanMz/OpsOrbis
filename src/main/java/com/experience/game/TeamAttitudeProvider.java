package com.experience.game;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.IAttitudeProvider;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

public class TeamAttitudeProvider implements IAttitudeProvider {

    private final TeamManager teamManager;
    private final NPCManager npcManager;
    public TeamAttitudeProvider(TeamManager teamManager, NPCManager npcManager) {
        this.teamManager = teamManager;
        this.npcManager = npcManager;
    }

    @Override
    public Attitude getAttitude(Ref<EntityStore> npcRef, Role role, Ref<EntityStore> targetRef, ComponentAccessor<EntityStore> accessor) {
        // Déterminer l'équipe du PNJ qui pose la question
        boolean isBlue = npcManager.estPnjBleu(npcRef);
        boolean isRed = npcManager.estPnjRouge(npcRef);
        
        if (!isBlue && !isRed) return null;
        
        String maTeam = isBlue ? "Bleue" : "Rouge";

        if (targetRef != null && accessor != null) {
            Player targetPlayerEntity = accessor.getComponent(targetRef, Player.getComponentType());
            if (targetPlayerEntity != null) {
                // Si le joueur est de la même équipe, on devient "AMICAL"
                if (teamManager.estDansEquipe(targetPlayerEntity, maTeam)) {
                    return Attitude.FRIENDLY;
                } else {
                    // Joueur ennemi - On est hostile seulement si on est dans la zone autorisée
                    return Attitude.HOSTILE;
                }
            }
        }
        
        return null;
    }
}
