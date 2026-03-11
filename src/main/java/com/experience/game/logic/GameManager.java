package com.experience.game.logic;

import com.experience.ExperienceMod;
import com.experience.kits.KitManager;
import com.experience.roles.RolesManager;
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
    private final RolesManager rolesManager;
    private NPCManager npcManager;
    private RelicManager relicManager;
    private final ScoreboardHUD scoreboardHUD;
    private final ExperienceMod plugin;
    
    // Logique des manches
    private int roundActuel = 1;
    public static final int ROUNDS_MAX = 4;
    public static final int MOITIE_ROUNDS = ROUNDS_MAX / 2;
    
    // Constante de temps par manche (ex: 5 minutes = 300 secondes)
    public static final int TEMPS_MANCHE_SECONDES = 300;
    private long tempsRestantManche = TEMPS_MANCHE_SECONDES;
    private long lastTimeMillis = 0;

    public GameManager(ExperienceMod plugin) {
        this.plugin = plugin;
        this.etatActuel = GameState.ATTENTE;
        this.teamManager = new TeamManager();
        this.kitManager = new KitManager();
        this.rolesManager = new RolesManager();
        this.scoreboardHUD = new ScoreboardHUD(this);
    }

    /**
     * Démarre globalement la compétition de 10 manches.
     * @param monde Le monde où la partie se déroule.
     */
    public void demarrerMatch(World monde) {
        if (monde == null) return;
        
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Tentative de démarrage du match (10 manches)...");

        if (etatActuel == GameState.EN_COURS) {
            HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Match déjà en cours.");
            return;
        }

        this.etatActuel = GameState.EN_COURS;
        teamManager.resetScoresEtRoles();
        this.roundActuel = 1;
        
        this.npcManager = new NPCManager(monde, teamManager);
        this.relicManager = new RelicManager(monde, teamManager);

        for (Player joueur : teamManager.getEquipeAttaquants()) {
            kitManager.donnerEquipement(joueur);
        }
        for (Player joueur : teamManager.getEquipeDefenseurs()) {
            kitManager.donnerEquipement(joueur);
        }

        // Affichage initial du scoreboard
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            Query<EntityStore> playerQuery = Archetype.of(Player.getComponentType());
            store.forEachChunk(playerQuery, (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Player p = chunk.getComponent(i, Player.getComponentType());
                    if (p != null) scoreboardHUD.afficher(p);
                }
            });
        });

        demarrerRound(monde);
    }

    /**
     * Prépare les entités et téléporte les joueurs pour une manche individuelle.
     */
    public void demarrerRound(World monde) {
        HytaleUtils.diffuserMessage(monde, Message.raw("[Phase] Démarrage de la Manche " + roundActuel + " !").color(Color.YELLOW));

        // Nettoyage avant respawn de round
        monde.execute(() -> {
            if (npcManager != null) npcManager.supprimerPNJ(null);
            if (relicManager != null) relicManager.supprimerReliques(null);
        });
        
        npcManager.faireApparaitrePNJ();
        relicManager.initRelics();

        // On téléporte tout le monde aux spawns correspondant à leurs rôles
        for (Player p : teamManager.getEquipeAttaquants()) {
            teamManager.teleporterAuSpawn(p);
        }
        for (Player p : teamManager.getEquipeDefenseurs()) {
            teamManager.teleporterAuSpawn(p);
        }

        HytaleUtils.diffuserMessage(monde, Message.raw("La manche commence ! Attaquants, volez les reliques !").color(Color.GREEN));
        
        // Initialisation du chrono
        this.tempsRestantManche = TEMPS_MANCHE_SECONDES;
        this.lastTimeMillis = System.currentTimeMillis();
    }

    /**
     * Appelé en boucle par le système ECS (ex: MatchTimerSystem)
     */
    public void tickChrono(World monde, com.hypixel.hytale.component.CommandBuffer<EntityStore> buffer) {
        if (etatActuel != GameState.EN_COURS || monde == null) return;
        
        long now = System.currentTimeMillis();
        if (now - lastTimeMillis >= 1000) { // On décrémente chaque seconde
            tempsRestantManche--;
            lastTimeMillis = now;
            
            // Rafraîchir l'UI des joueurs
            scoreboardHUD.rafraichirTous();

            // Condition Victoire Défenseurs : temps écoulé
            if (tempsRestantManche <= 0) {
                HytaleUtils.diffuserMessage(monde, Message.raw("[Temps] LE TEMPS EST ÉCOULÉ !").color(Color.RED));
                terminerRound(monde, "Defenseur", buffer);
            }
        }
    }

    /**
     * Termine une seule manche et avance selon le déroulement (Mi-temps ou Fin).
     * @param roleVainqueur "Attaquant" ou "Defenseur"
     */
    public void terminerRound(World monde, String roleVainqueur, com.hypixel.hytale.component.CommandBuffer<EntityStore> buffer) {
        teamManager.ajouterPointEquipe(roleVainqueur);
        
        HytaleUtils.diffuserMessage(monde, Message.join(
            Message.raw("[Victoire] Manche remportée par l'équipe : ").color(Color.ORANGE),
            Message.raw(roleVainqueur).color("Attaquant".equals(roleVainqueur) ? new Color(255,160,0) : new Color(0,200,100))
        ));

        // Nettoyage sécurité asynchrone pour la transition
        if (npcManager != null) npcManager.supprimerPNJ(buffer);
        if (relicManager != null) relicManager.supprimerReliques(buffer);
        
        // Progression globale
        this.roundActuel++;
        
        if (roundActuel > ROUNDS_MAX) {
            terminerMatchGlobal(monde);
        } else {
            // Check mi-temps
            if (roundActuel == MOITIE_ROUNDS + 1) {
                HytaleUtils.diffuserMessage(monde, Message.raw("[Changement] MI-TEMPS ! Changement de rôles !").color(Color.CYAN));
                teamManager.inverserRoles();
            }
            // Enchaîner au round suivant (idealement avec délai, ici instantané pour le POC)
            demarrerRound(monde);
        }
    }

    /**
     * Termine la compétition globale, annonce le vainqueur et arrête le jeu.
     */
    public void terminerMatchGlobal(World monde) {
        this.etatActuel = GameState.TERMINEE;
        
        String texteVainqueur;
        if (teamManager.getScoreEquipe1() > teamManager.getScoreEquipe2()) {
            texteVainqueur = "Équipe 1";
        } else if (teamManager.getScoreEquipe2() > teamManager.getScoreEquipe1()) {
            texteVainqueur = "Équipe 2";
        } else {
            texteVainqueur = "Égalité !";
        }
        
        HytaleUtils.diffuserMessage(monde, Message.join(
            Message.raw("=== LE MATCH EST TERMINÉ ===\n").color(Color.ORANGE),
            Message.raw("Scores - Eq1: " + teamManager.getScoreEquipe1() + " | Eq2: " + teamManager.getScoreEquipe2() + "\n").color(Color.WHITE),
            Message.raw("Gagnant : " + texteVainqueur).color(Color.YELLOW)
        ));
    }

    public void diffuserMessage(World monde, Message message) {
        HytaleUtils.diffuserMessage(monde, message);
    }

    public long getTempsRestantManche() { return tempsRestantManche; }
    public int getRoundActuel() { return roundActuel; }
    public GameState getEtatActuel() { return etatActuel; }
    public TeamManager getTeamManager() { return teamManager; }
    public KitManager getKitManager() { return kitManager; }
    public RolesManager getRolesManager() { return rolesManager; }
    public NPCManager getNpcManager() { return npcManager; }
    public RelicManager getRelicManager() { return relicManager; }
    public ScoreboardHUD getScoreboardHUD() { return scoreboardHUD; }
}
