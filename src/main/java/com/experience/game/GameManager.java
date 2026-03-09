package com.experience.game;

import com.experience.ExperienceMod;
import com.experience.kits.KitManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.logger.HytaleLogger;
import java.awt.Color;
import java.util.logging.Level;

public class GameManager {

    public enum GameState {
        ATTENTE,
        EN_COURS,
        TERMINEE
    }

    private GameState etatActuel;
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private NPCManager npcManager;
    private final ExperienceMod plugin;

    public GameManager(ExperienceMod plugin) {
        this.plugin = plugin;
        this.etatActuel = GameState.ATTENTE;
        this.teamManager = new TeamManager();
        this.kitManager = new KitManager();
    }

    public void demarrerPartie(World monde) {
        if (monde == null) return;
        
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Tentative de démarrage de la partie...");

        if (etatActuel == GameState.EN_COURS) {
            HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Partie déjà en cours.");
            return;
        }

        this.etatActuel = GameState.EN_COURS;
        this.npcManager = new NPCManager(monde, teamManager);

        npcManager.faireApparaitrePNJ();

        for (Player joueur : teamManager.getEquipeBleue()) {
            kitManager.donnerEquipement(joueur);
        }
        for (Player joueur : teamManager.getEquipeRouge()) {
            kitManager.donnerEquipement(joueur);
        }

        diffuserMessage(monde, Message.raw("La partie commence ! Défendez votre PNJ !").color(Color.GREEN));
    }

    public void terminerPartie(World monde, String equipeGagnante) {
        this.etatActuel = GameState.TERMINEE;
        diffuserMessage(monde, Message.join(
            Message.raw("La partie est terminée ! L'équipe ").color(Color.ORANGE),
            Message.raw(equipeGagnante).color(equipeGagnante.equals("Bleue") ? Color.BLUE : Color.RED),
            Message.raw(" a gagné !").color(Color.ORANGE)
        ));

        if (npcManager != null) {
            npcManager.supprimerPNJ();
        }
    }

    public void diffuserMessage(World monde, Message message) {
        // Envoyer à TOUS les joueurs du monde pour plus de visibilité
        for (Player p : monde.getPlayers()) {
            p.sendMessage(message);
        }
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] " + message.getRawText());
    }

    public GameState getEtatActuel() { return etatActuel; }
    public TeamManager getTeamManager() { return teamManager; }
    public KitManager getKitManager() { return kitManager; }
    public NPCManager getNpcManager() { return npcManager; }
}
