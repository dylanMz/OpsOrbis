package com.experience.game.logic;

import com.experience.ExperienceMod;
import com.experience.kits.KitManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.logger.HytaleLogger;
import com.experience.utils.HytaleUtils;
import java.util.logging.Level;
import java.awt.Color;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.experience.game.ui.ScoreboardHUD;

public class GameManager {

    /**
     * États possibles de la partie.
     */
    public enum GameState {
        ATTENTE,   // En attente de joueurs ou de démarrage
        EN_COURS,  // La partie est active
        TERMINEE   // Un gagnant a été désigné
    }

    private GameState etatActuel;
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private NPCManager npcManager;
    private RelicManager relicManager;
    private final ScoreboardHUD scoreboardHUD;
    private final ExperienceMod plugin;

    public GameManager(ExperienceMod plugin) {
        this.plugin = plugin;
        this.etatActuel = GameState.ATTENTE;
        this.teamManager = new TeamManager();
        this.kitManager = new KitManager();
        this.scoreboardHUD = new ScoreboardHUD(this);
    }

    /**
     * Démarre la partie, fait apparaître les PNJ et les reliques, et donne les kits.
     * @param monde Le monde où la partie se déroule.
     */
    public void demarrerPartie(World monde) {
        if (monde == null) return;
        
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Tentative de démarrage de la partie...");

        if (etatActuel == GameState.EN_COURS) {
            HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Partie déjà en cours.");
            return;
        }

        this.etatActuel = GameState.EN_COURS;
        this.npcManager = new NPCManager(monde, teamManager);
        this.relicManager = new RelicManager(monde, teamManager);

        npcManager.faireApparaitrePNJ();
        relicManager.initRelics();

        for (Player joueur : teamManager.getEquipeBleue()) {
            kitManager.donnerEquipement(joueur);
        }
        for (Player joueur : teamManager.getEquipeRouge()) {
            kitManager.donnerEquipement(joueur);
        }

        // Affichage du scoreboard pour les joueurs présents (via ECS Store)
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            Query<EntityStore> playerQuery = Archetype.of(Player.getComponentType());
            store.forEachChunk(playerQuery, (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Player p = chunk.getComponent(i, Player.getComponentType());
                    if (p != null) {
                        scoreboardHUD.afficher(p);
                    }
                }
            });
        });

        HytaleUtils.diffuserMessage(monde, Message.raw("La partie commence ! Défendez votre PNJ et capturez les reliques !").color(Color.GREEN));
    }

    /**
     * Termine la partie, annonce le vainqueur et nettoie le monde.
     * @param monde Le monde actuel.
     * @param equipeGagnante Nom de l'équipe victorieuse.
     * @param buffer CommandBuffer pour la suppression sécurisée d'entités.
     */
    public void terminerPartie(World monde, String equipeGagnante, com.hypixel.hytale.component.CommandBuffer<EntityStore> buffer) {
        this.etatActuel = GameState.TERMINEE;
        HytaleUtils.diffuserMessage(monde, Message.join(
            Message.raw("La partie est terminée ! L'équipe ").color(Color.ORANGE),
            Message.raw(equipeGagnante).color(equipeGagnante.equals("Bleue") ? Color.BLUE : Color.RED),
            Message.raw(" a gagné !").color(Color.ORANGE)
        ));

        // Nettoyage des entités de la partie avec Buffer safe
        if (npcManager != null) {
            npcManager.supprimerPNJ(buffer);
        }
        if (relicManager != null) {
            relicManager.supprimerReliques(buffer);
        }
    }

    public void diffuserMessage(World monde, Message message) {
        HytaleUtils.diffuserMessage(monde, message);
    }

    public GameState getEtatActuel() { return etatActuel; }
    public TeamManager getTeamManager() { return teamManager; }
    public KitManager getKitManager() { return kitManager; }
    public NPCManager getNpcManager() { return npcManager; }
    public RelicManager getRelicManager() { return relicManager; }
    public ScoreboardHUD getScoreboardHUD() { return scoreboardHUD; }
}
