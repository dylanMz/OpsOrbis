package com.opsorbis.game.logic;

import com.opsorbis.OpsOrbis;
import com.opsorbis.kits.KitManager;
import com.opsorbis.roles.RolesManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.logger.HytaleLogger;
import com.opsorbis.utils.HytaleUtils;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.opsorbis.game.ui.ScoreboardHUD;

public class GameManager {

    /**
     * États possibles de la partie.
     */
    public enum GameState {
        ATTENTE,   // En attente de joueurs
        DEMARRAGE, // Compte à rebours avant le match
        EN_COURS,  // La partie est active
        PAUSE,     // Transition entre deux manches
        TERMINEE   // Un gagnant a été désigné
    }

    private GameState etatActuel;
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final RolesManager rolesManager;
    private NPCManager npcManager;
    private RelicManager relicManager;
    private final ScoreboardHUD scoreboardHUD;
    private final PlayerStateManager playerStateManager;
    private final OpsOrbis plugin;
    private final Map<UUID, Long> disconnectionTimes = new HashMap<>(); // UUID -> Heure de déconnexion
    private final Map<UUID, Long> reconnectionTimes = new HashMap<>(); // UUID -> Heure de reconnexion physique
    
    // Logique de démarrage automatique
    private int compteAReboursDemarrage = 30;
    private static final int DELAI_DEMARRAGE_SECONDES = 30;
    
    // Logique des manches
    private int roundActuel = 1;
    public static final int ROUNDS_MAX = 4;
    public static final int MOITIE_ROUNDS = ROUNDS_MAX / 2;
    public static final long GRACE_PERIOD_MS = 60000; // 1 minute
    
    // Constante de temps par manche (ex: 5 minutes = 300 secondes)
    public static final int TEMPS_MANCHE_SECONDES = 300;
    private long tempsRestantManche = TEMPS_MANCHE_SECONDES;
    private long lastTimeMillis = 0;
    private long tempsSansJoueursMillis = 0;

    public GameManager(OpsOrbis plugin) {
        this.plugin = plugin;
        this.etatActuel = GameState.ATTENTE;
        this.teamManager = new TeamManager();
        this.kitManager = new KitManager();
        this.rolesManager = new RolesManager();
        this.scoreboardHUD = new ScoreboardHUD(this);
        this.playerStateManager = new PlayerStateManager();
    }

    /**
     * Démarre globalement la compétition de 10 manches.
     * @param monde Le monde où la partie se déroule.
     */
    public void demarrerMatch(World monde) {
        if (monde == null) return;
        
        HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Tentative de démarrage du match (10 manches)...");

        if (etatActuel == GameState.EN_COURS) {
            HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Match déjà en cours.");
            return;
        }

        this.etatActuel = GameState.EN_COURS;
        teamManager.resetScoresEtRoles();
        this.roundActuel = 1;
        
        this.npcManager = new NPCManager(monde, teamManager);
        this.relicManager = new RelicManager(monde, teamManager);

        for (Player joueur : teamManager.getEquipeAttaquants()) {
            if (joueur != null && joueur.getWorld() != null) {
                playerStateManager.saveState(joueur);
                kitManager.donnerEquipement(joueur);
            }
        }
        for (Player joueur : teamManager.getEquipeDefenseurs()) {
            if (joueur != null && joueur.getWorld() != null) {
                playerStateManager.saveState(joueur);
                kitManager.donnerEquipement(joueur);
            }
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
        HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("match_start_chat", roundActuel));

        // Nettoyage et respawn groupés sur le même tick/tâche monde pour éviter les race conditions
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            
            // 1. Suppression (Sync)
            if (npcManager != null) npcManager.supprimerPNJ(null);
            if (relicManager != null) relicManager.supprimerReliques(null);
            
            // 2. Apparition (Sync)
            if (npcManager != null) npcManager.faireApparaitrePNJ_Direct(store);
            if (relicManager != null) relicManager.initRelics_Direct(store);
        });

        // On téléporte tout le monde aux spawns correspondant à leurs rôles
        for (Player p : teamManager.getEquipeAttaquants()) {
            playerStateManager.saveState(p);
            teamManager.teleporterAuSpawn(p);
        }
        for (Player p : teamManager.getEquipeDefenseurs()) {
            playerStateManager.saveState(p);
            teamManager.teleporterAuSpawn(p);
        }

        HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("round_start_chat"));
        
        // Annonce visuelle au centre de l'écran (On utilise des titres traduits)
        diffuserAnnonce(monde, 
            OpsOrbis.get().getLangManager().get("match_start_title", roundActuel),
            Message.raw(teamManager.isEquipe1Attaquant() ? "ÉQUIPE 1 : ATTAQUE" : "ÉQUIPE 1 : DÉFENSE").color(Color.WHITE)
        );
        
        // Initialisation du chrono
        this.tempsRestantManche = TEMPS_MANCHE_SECONDES;
        this.lastTimeMillis = System.currentTimeMillis();
    }

    /**
     * Appelé chaque seconde par le plugin pour gérer le démarrage auto et les chronos.
     */
    public void tickSeconde(World monde) {
        if (monde == null) return;

        // 1. Gestion du démarrage automatique
        verifierDemarrageAutomatique(monde);

        // 2. Si un match est en cours, on peut déléguer au tickChrono ou le faire ici
        // Pour la cohérence, on gère le chrono de manche ici aussi
        if (etatActuel == GameState.EN_COURS) {
            this.tempsRestantManche--;
            monde.execute(scoreboardHUD::rafraichirTous);

            if (tempsRestantManche <= 0) {
                HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("match_time_up"));
                // On délègera la fin de round à un petit délai pour laisser l'info apparaître
                terminerRound(monde, "Defenseur", null);
            }
        }
    }

    private void verifierDemarrageAutomatique(World monde) {
        int nbJoueurs = teamManager.getNombreTotalJoueurs();

        if (etatActuel == GameState.ATTENTE) {
            if (nbJoueurs > 0) {
                this.etatActuel = GameState.DEMARRAGE;
                this.compteAReboursDemarrage = DELAI_DEMARRAGE_SECONDES;
                HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("timer_auto_start", compteAReboursDemarrage));
            }
        } else if (etatActuel == GameState.DEMARRAGE) {
            if (nbJoueurs == 0) {
                this.etatActuel = GameState.ATTENTE;
                HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("match_start_cancelled"));
                return;
            }

            compteAReboursDemarrage--;

            // Annonces
            if (compteAReboursDemarrage == 15 || compteAReboursDemarrage == 10 || (compteAReboursDemarrage <= 5 && compteAReboursDemarrage > 0)) {
                HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("timer_seconds_left", compteAReboursDemarrage));
                // Annonce rapide : Stay=0.6s, In=0.1s, Out=0.3s pour coller au rythme de 1s
                diffuserAnnonceRapide(monde, 
                    Message.raw(String.valueOf(compteAReboursDemarrage)).color(Color.YELLOW),
                    OpsOrbis.get().getLangManager().get("timer_seconds_left", compteAReboursDemarrage),
                    0.6f, 0.1f, 0.3f
                );
            }

            if (compteAReboursDemarrage <= 0) {
                demarrerMatch(monde);
            }
        }
    }

    /**
     * Appelé périodiquement pour arrêter le match s'il est vide.
     */
    public void verifierArretAutomatique() {
        if (etatActuel != GameState.EN_COURS) return;

        int connectes = teamManager.countConnectedPlayers();
        long now = System.currentTimeMillis();

        if (connectes == 0) {
            if (tempsSansJoueursMillis == 0) tempsSansJoueursMillis = now;
            
            // Si le match est vide de joueurs PHYSIQUEMENT présents (même en grace period)
            // depuis plus de la période de grâce + 5s de marge, on arrête.
            if ((now - tempsSansJoueursMillis) > (GRACE_PERIOD_MS + 5000)) {
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Match vide depuis trop longtemps. Arret automatique.");
                
                // On nettoie les listes de joueurs (pour vider la teamManager et les états si besoin)
                List<Player> temp = new ArrayList<>(teamManager.getEquipeAttaquants());
                temp.addAll(teamManager.getEquipeDefenseurs());
                for (Player p : temp) {
                    retirerJoueurDuMatch(p); 
                }

                if (npcManager != null && npcManager.getWorld() != null) {
                    forcerArret(npcManager.getWorld());
                }
            }
            
            // Si les listes sont littéralement vides (tout le monde a fait /game leave)
            if (teamManager.getNombreTotalJoueurs() == 0) {
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Plus aucun joueur dans les equipes. Arret immediat.");
                if (npcManager != null && npcManager.getWorld() != null) {
                    forcerArret(npcManager.getWorld());
                }
            }
        } else {
            tempsSansJoueursMillis = 0;
            
            // Nettoyage périodique des joueurs déconnectés ayant dépassé le délai
            // même si certains sont encore connectés
            verifierDelaisGrace();
        }
    }

    private void verifierDelaisGrace() {
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : disconnectionTimes.entrySet()) {
            if (now - entry.getValue() > GRACE_PERIOD_MS) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            Player p = teamManager.getJoueurParUUID(uuid);
            if (p != null) {
                retirerJoueurDuMatch(p);
            }
            disconnectionTimes.remove(uuid);
        }
    }

    /**
     * Termine une seule manche et avance selon le déroulement (Mi-temps ou Fin).
     * @param roleVainqueur "Attaquant" ou "Defenseur"
     */
    public void terminerRound(World monde, String roleVainqueur, com.hypixel.hytale.component.CommandBuffer<EntityStore> buffer) {
        teamManager.ajouterPointEquipe(roleVainqueur);
        
        HytaleUtils.diffuserMessage(monde, Message.join(
            OpsOrbis.get().getLangManager().get("prefix"),
            Message.raw(roleVainqueur).color("Attaquant".equals(roleVainqueur) ? new Color(255,160,0) : new Color(0,200,100))
        ));

        // 1. Nettoyage immédiat des entités via le buffer ECS
        if (npcManager != null) npcManager.supprimerPNJ(buffer);
        if (relicManager != null) relicManager.supprimerReliques(buffer);
        
        // 2. On passe en état PAUSE pour arrêter les systèmes ECS pendant la transition
        this.etatActuel = GameState.PAUSE;
        this.tempsRestantManche = 0;

        // 3. On décale la logique de changement de round/match pour sortir du Tick ECS (Évite IllegalStateException)
        OpsOrbis.get().runDelayed(3000, () -> {
            monde.execute(() -> {
                this.roundActuel++;
                
                if (roundActuel > ROUNDS_MAX) {
                    terminerMatchGlobal(monde);
                } else {
                    if (roundActuel == MOITIE_ROUNDS + 1) {
                        HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("half_time_swap"));
                        teamManager.inverserRoles();
                    }
                    this.etatActuel = GameState.EN_COURS;
                    demarrerRound(monde);
                }
            });
        });
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
            OpsOrbis.get().getLangManager().get("match_over_header"),
            OpsOrbis.get().getLangManager().get("match_over_scores", teamManager.getScoreEquipe1(), teamManager.getScoreEquipe2()),
            OpsOrbis.get().getLangManager().get("match_over_winner_label", texteVainqueur)
        ));
        
        // Nettoyer les listes de joueurs et restaurer leur état
        List<Player> participants = new ArrayList<>(teamManager.getEquipeAttaquants());
        participants.addAll(teamManager.getEquipeDefenseurs());

        // On masque le scoreboard proprement pour tout le monde (participants et spectateurs)
        scoreboardHUD.masquerPourTousInscrits(monde);

        for (Player p : participants) {
            retirerJoueurDuMatch(p); 
        }
        
        teamManager.viderEquipes();
        teamManager.resetScoresEtRoles();
        disconnectionTimes.clear();
    }

    /**
     * Force l'arrêt de la partie et nettoie l'environnement.
     * @param monde Le monde où la partie se déroule.
     */
    public void forcerArret(World monde) {
        if (monde == null) return;

        this.etatActuel = GameState.ATTENTE;
        HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("match_stopped_admin"));

        monde.execute(() -> {
            // 1. Supprimer PNJ et Reliques (buffer null pour suppression directe)
            if (npcManager != null) npcManager.supprimerPNJ(null);
            if (relicManager != null) relicManager.supprimerReliques(null);
            
            // 2. Nettoyer les listes de joueurs et restaurer leur état
            List<Player> participants = new ArrayList<>(teamManager.getEquipeAttaquants());
            participants.addAll(teamManager.getEquipeDefenseurs());
            
            // On masque le scoreboard proprement pour tout le monde (participants et spectateurs)
            scoreboardHUD.masquerPourTousInscrits(monde);

            for (Player p : participants) {
                retirerJoueurDuMatch(p); 
            }
            
            teamManager.viderEquipes();
            teamManager.resetScoresEtRoles();
            
            // 4. Nettoyer les délais de déconnexion
            disconnectionTimes.clear();
            tempsSansJoueursMillis = 0;
        });
    }

    /**
     * Gère la reconnexion d'un joueur.
     */
    public void gererReconnexion(Player joueur, long connectionTime) {
        if (joueur == null) return;
        UUID uuid = HytaleUtils.getPlayerUuid(joueur);
        if (uuid == null || joueur.getWorld() == null) return;

        // 1. Est-ce qu'on a un état sauvegardé pour lui ?
        if (!playerStateManager.hasSavedState(uuid)) {
            return;
        }

        // 2. Si la partie est finie, on restaure direct
        if (etatActuel != GameState.EN_COURS) {
            playerStateManager.restoreState(joueur);
            disconnectionTimes.remove(uuid);
            reconnectionTimes.put(uuid, connectionTime);
            
            // Sécurité : clean du terrain quand un joueur revient si pas de match
            if (npcManager != null) npcManager.supprimerPNJ(null);
            if (relicManager != null) relicManager.supprimerReliques(null);
            return;
        }

        // 3. Si la partie est en cours, on check le délai
        Long decoTime = disconnectionTimes.get(uuid);

        if (decoTime != null && (connectionTime - decoTime) <= GRACE_PERIOD_MS) {
            
            // Reconnexion rapide : il reste dans le match
            disconnectionTimes.remove(uuid);
            reconnectionTimes.put(uuid, connectionTime);
            
            // IMPORTANT : Mettre à jour l'instance Player dans le TeamManager !
            teamManager.mettreAJourJoueur(joueur);
            
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("player_reconnect_success"));
            scoreboardHUD.afficher(joueur);
            
            // On lui redonne son équipement de kit car il revient de déconnexion
            if (kitManager != null) {
                kitManager.donnerEquipement(joueur);
            }
        } else {
            // Reconnexion tardive : on lui rend son inventaire de base
            playerStateManager.restoreState(joueur);
            disconnectionTimes.remove(uuid);
            reconnectionTimes.put(uuid, connectionTime);
            teamManager.retirerJoueur(joueur);
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("player_grace_expired"));
        }
    }

    /**
     * Retire un joueur de la partie à partir de sa référence (utile pour la déconnexion).
     */
    public void retirerJoueurParRef(PlayerRef ref) {
        if (ref == null) return;
        
        UUID uuid = ref.getUuid();
        Player joueur = teamManager.getJoueurParRef(ref);
        
        // Faire tomber la relique si le joueur en porte une
        if (etatActuel == GameState.EN_COURS && relicManager != null) {
            relicManager.dropReliqueSiPorteur(joueur);
        }

        // Au lieu de restaurer immédiatement, on enregistre l'heure de déconnexion
        if (playerStateManager.hasSavedState(uuid)) {
            disconnectionTimes.put(uuid, System.currentTimeMillis());
        } else {
            // Si pas d'état sauvegardé, on nettoie juste le HUD
            // Ici joueur n'est pas forcément nul, on le récupère si possible
            scoreboardHUD.masquer(ref, teamManager.getJoueurParRef(ref));
        }
    }

    /**
     * Retire un joueur de la partie (abandon ou déconnexion).
     */
    public void retirerJoueurDuMatch(Player joueur) {
        if (joueur == null) return;
        
        // 1. Restaurer l'état (si sauvegardé)
        if (playerStateManager.hasSavedState(joueur)) {
            playerStateManager.restoreState(joueur);
        }
        
        // 2. Retirer de l'équipe
        teamManager.retirerJoueur(joueur);
        
        // 3. Masquer le HUD
        scoreboardHUD.masquer(HytaleUtils.getPlayerRef(joueur), joueur);
    }

    /**
     * Restaure l'état d'origine de tous les joueurs ayant rejoint la partie.
     */
    private void restaurerTousLesJoueurs(World monde) {
        Store<EntityStore> store = monde.getEntityStore().getStore();
        Query<EntityStore> playerQuery = Archetype.of(Player.getComponentType());
        store.forEachChunk(playerQuery, (chunk, b) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Player p = chunk.getComponent(i, Player.getComponentType());
                if (p != null && playerStateManager.hasSavedState(p)) {
                    playerStateManager.restoreState(p);
                }
            }
        });
    }

    public void diffuserMessage(World monde, Message message) {
        HytaleUtils.diffuserMessage(monde, message);
    }

    /**
     * Diffuse une annonce visuelle au centre de l'écran à tous les joueurs.
     */
    public void diffuserAnnonce(World monde, Message titre, Message sousTitre) {
        HytaleUtils.diffuserAnnonce(monde, titre, sousTitre);
    }

    /**
     * Diffuse une annonce visuelle avec des durées de fondu personnalisées.
     */
    public void diffuserAnnonceRapide(World monde, Message titre, Message sousTitre, float dureeMaintien, float dureeApparition, float dureeDisparition) {
        HytaleUtils.diffuserAnnonceFiltree(monde, p -> true, titre, sousTitre, dureeMaintien, dureeApparition, dureeDisparition);
    }

    /**
     * Diffuse une annonce visuelle uniquement à une équipe spécifique.
     */
    public void diffuserAnnonceEquipe(World monde, String role, Message titre, Message sousTitre) {
        HytaleUtils.diffuserAnnonceFiltree(monde, p -> teamManager.getRole(p).equals(role), titre, sousTitre);
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
    public PlayerStateManager getPlayerStateManager() { return playerStateManager; }

    /**
     * Retourne le moment de la dernière reconnexion physique du joueur.
     */
    public long getReconnectionTime(UUID uuid) {
        return reconnectionTimes.getOrDefault(uuid, 0L);
    }
}
