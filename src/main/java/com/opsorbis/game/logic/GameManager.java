package com.opsorbis.game.logic;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.GlobalConfig;
import com.opsorbis.config.LangManager;
import com.opsorbis.kits.KitManager;
import com.opsorbis.roles.RolesManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Vector3d;
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
    private final StatsManager statsManager;
    private final OpsOrbis plugin;
    private final Map<UUID, Long> disconnectionTimes = new HashMap<>(); // UUID -> Heure de déconnexion
    private final Map<UUID, Long> reconnectionTimes = new HashMap<>(); // Anti-doublon pour le traitement de reco
    private final Map<UUID, Long> relicPickupCooldowns = new HashMap<>(); // UUID -> Heure de fin du blocage ramassage
    
    // Logique de démarrage automatique
    private int compteAReboursDemarrage;
    
    // Logique des manches
    private int roundActuel = 1;
    private long tempsRestantManche;
    private long tempsSansJoueursMillis = 0;

    public GameManager(OpsOrbis plugin) {
        this.plugin = plugin;
        this.etatActuel = GameState.ATTENTE;
        this.teamManager = new TeamManager();
        this.kitManager = new KitManager();
        this.rolesManager = new RolesManager();
        this.scoreboardHUD = new ScoreboardHUD(this);
        this.playerStateManager = new PlayerStateManager();
        this.statsManager = new StatsManager();
        
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        this.compteAReboursDemarrage = global.getAutoStartDelaySeconds();
        this.tempsRestantManche = global.getRoundDurationSeconds();
    }

    /**
     * Démarre globalement la compétition de X manches.
     * @param monde Le monde où la partie se déroule.
     */
    public void demarrerMatch(World monde) {
        if (monde == null) return;
        
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Tentative de démarrage du match (" + global.getMaxRounds() + " manches)...");

        if (etatActuel == GameState.EN_COURS) {
            HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Match déjà en cours.");
            return;
        }

        this.etatActuel = GameState.EN_COURS;
        teamManager.resetScoresEtRoles();
        statsManager.reset();
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
        
        LangManager lang = OpsOrbis.get().getLangManager();
        diffuserAnnonce(monde, 
            lang.get("match_start_title"),
            lang.get(teamManager.isEquipe1Attaquant() ? "team_1_attacking" : "team_1_defending").color(Color.WHITE)
        );
        
        // Initialisation du chrono
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        this.tempsRestantManche = global.getRoundDurationSeconds();
    }

    /**
     * Appelé chaque seconde par le plugin pour gérer le démarrage auto et les chronos.
     */
    public void tickSeconde(World monde) {
        if (monde == null) return;

        // 1. Gestion du démarrage automatique
        verifierDemarrageAutomatique(monde);

        // 2. Si un match est en cours, gère le chrono
        if (etatActuel == GameState.EN_COURS) {
            this.tempsRestantManche--;
            monde.execute(scoreboardHUD::rafraichirTous);

            if (tempsRestantManche <= 0) {
                HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("match_time_up"));
                terminerRound(monde, PlayerCamp.DEFENSEUR, null);
            }
        }
    }

    private void verifierDemarrageAutomatique(World monde) {
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        if (!global.isAutoStartEnabled()) return;

        int nbJoueurs = teamManager.getNombreTotalJoueurs();

        if (etatActuel == GameState.ATTENTE) {
            if (nbJoueurs >= global.getMinPlayersToStart()) {
                this.etatActuel = GameState.DEMARRAGE;
                this.compteAReboursDemarrage = global.getAutoStartDelaySeconds();
                HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("timer_auto_start", "seconds", compteAReboursDemarrage));
            }
        } else if (etatActuel == GameState.DEMARRAGE) {
            if (nbJoueurs < global.getMinPlayersToStart()) {
                this.etatActuel = GameState.ATTENTE;
                HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("match_start_cancelled"));
                return;
            }

            compteAReboursDemarrage--;

            // Annonces
            if (compteAReboursDemarrage == 15 || compteAReboursDemarrage == 10 || (compteAReboursDemarrage <= 5 && compteAReboursDemarrage > 0)) {
                HytaleUtils.diffuserMessage(monde, OpsOrbis.get().getLangManager().get("timer_seconds_left", "seconds", compteAReboursDemarrage));
                diffuserAnnonceRapide(monde, 
                    Message.raw(String.valueOf(compteAReboursDemarrage)).color(Color.YELLOW),
                    OpsOrbis.get().getLangManager().get("timer_seconds_left", "seconds", compteAReboursDemarrage),
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

        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        int connectes = teamManager.countConnectedPlayers();
        long now = System.currentTimeMillis();

        if (connectes == 0) {
            if (tempsSansJoueursMillis == 0) tempsSansJoueursMillis = now;
            
            // On arrête si vide depuis plus de la période de grâce + 5s
            if ((now - tempsSansJoueursMillis) > (global.getGracePeriodMs() + 5000)) {
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Match vide depuis trop longtemps. Arret automatique.");
                
                List<Player> temp = new ArrayList<>(teamManager.getEquipeAttaquants());
                temp.addAll(teamManager.getEquipeDefenseurs());
                for (Player p : temp) {
                    retirerJoueurDuMatch(p); 
                }

                if (npcManager != null && npcManager.getWorld() != null) {
                    forcerArret(npcManager.getWorld());
                }
            }
            
            if (teamManager.getNombreTotalJoueurs() == 0) {
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Plus aucun joueur dans les equipes. Arret immediat.");
                if (npcManager != null && npcManager.getWorld() != null) {
                    forcerArret(npcManager.getWorld());
                }
            }
        } else {
            tempsSansJoueursMillis = 0;
            verifierDelaisGrace();
        }
    }

    private void verifierDelaisGrace() {
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        long now = System.currentTimeMillis();
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : disconnectionTimes.entrySet()) {
            if (now - entry.getValue() > global.getGracePeriodMs()) {
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
     * @param campVainqueur Le camp vainqueur.
     */
    public void terminerRound(World monde, PlayerCamp campVainqueur, com.hypixel.hytale.component.CommandBuffer<EntityStore> buffer) {
        teamManager.ajouterPointEquipe(campVainqueur);
        
        LangManager lang = OpsOrbis.get().getLangManager();
        String roleName = lang.getRaw(campVainqueur.getLangKey());
        
        // 1. Message de chat détaillé
        HytaleUtils.diffuserMessage(monde, lang.get("round_winner_chat", "round", roundActuel, "camp", roleName));
        
        // 2. Annonce visuelle au centre de l'écran (TitleEvent)
        diffuserAnnonce(monde, 
            lang.get("round_winner_title", "round", roundActuel, "camp", roleName),
            lang.get("round_winner_subtitle", "round", roundActuel, "role", roleName)
        );

        // 3. Nettoyage immédiat des entités via le buffer ECS
        if (npcManager != null) npcManager.supprimerPNJ(buffer);
        if (relicManager != null) relicManager.supprimerReliques(buffer);
        
        // 4. On passe en état PAUSE pour arrêter les systèmes ECS pendant la transition
        this.etatActuel = GameState.PAUSE;
        this.tempsRestantManche = 0;

        // 5. On décale la logique de changement de round de 10 secondes (au lieu de 3)
        OpsOrbis.get().runDelayed(10000, () -> {
            monde.execute(() -> {
                this.roundActuel++;
                
                GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
                if (roundActuel > global.getMaxRounds()) {
                    terminerMatchGlobal(monde);
                } else {
                    if (roundActuel == (global.getMaxRounds() / 2) + 1) {
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
        LangManager lang = OpsOrbis.get().getLangManager();
        if (teamManager.getScoreEquipe1() > teamManager.getScoreEquipe2()) {
            texteVainqueur = lang.getRaw("summary_victory_title", "camp", lang.getRaw("team_1_name"));
        } else if (teamManager.getScoreEquipe2() > teamManager.getScoreEquipe1()) {
            texteVainqueur = lang.getRaw("summary_victory_title", "camp", lang.getRaw("team_2_name"));
        } else {
            texteVainqueur = lang.getRaw("match_draw");
        }
        
        HytaleUtils.diffuserMessage(monde, Message.join(
            OpsOrbis.get().getLangManager().get("match_over_header"),
            OpsOrbis.get().getLangManager().get("match_over_scores"),
            OpsOrbis.get().getLangManager().get("match_over_winner_label", "winner", texteVainqueur)
        ));

        // Nettoyer les listes de joueurs
        List<Player> participants = new ArrayList<>(teamManager.getEquipeAttaquants());
        participants.addAll(teamManager.getEquipeDefenseurs());

        // 1. Masquer le scoreboard proprement
        scoreboardHUD.masquerPourTousInscrits(monde);
        // On retarde le nettoyage pour laisser le temps de lire le résumé
        OpsOrbis.get().runDelayed(10000, () -> {
            monde.execute(() -> {
                for (Player p : participants) {
                    retirerJoueurDuMatch(p); 
                }
                teamManager.viderEquipes();
                teamManager.resetScoresEtRoles();
                disconnectionTimes.clear();
            });
        });
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

        // Anti-doublon assoupli : on accepte une nouvelle connexion si l'ancienne n'est pas "active"
        // (On le nettoie maintenant dans retirerJoueurParRef pour permettre les re-décos/re-recos)
        if (reconnectionTimes.containsKey(uuid)) {
            HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Reconnexion deja traitee pour " + uuid);
            return;
        }

        HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Tentative de reconnexion pour " + uuid);

        // 1. Est-ce qu'on a un état sauvegardé pour lui ?
        if (!playerStateManager.hasSavedState(uuid)) {
            return;
        }

        // 2. Si le joueur est toujours dans une équipe (période de grâce active),
        // on ne restaure SURTOUT PAS son inventaire d'origine car il est toujours "en jeu"
        if (teamManager.isJoueurDansMatch(joueur)) {
            disconnectionTimes.remove(uuid);
            reconnectionTimes.put(uuid, connectionTime);
            // On rafraîchit le cooldown de ramassage (déjà mis au join, mais par sécurité)
            ajouterCooldownRamassageRelique(uuid, 10000);
            teamManager.mettreAJourJoueur(joueur);
            
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("player_reconnect_success"));
            scoreboardHUD.afficher(joueur);
            
            // Nettoyage de l'inventaire avant de redonner le kit (évite les reliques fantômes)
            joueur.getInventory().clear();
            
            // Si la partie est active, on lui redonne son kit
            if (etatActuel == GameState.EN_COURS && kitManager != null) {
                kitManager.donnerEquipement(joueur);
            }
            return;
        }

        // 3. Si le joueur n'est plus dans une équipe (délais expiré) ou si la partie est terminée,
        // on lui rend ses affaires.
        if (etatActuel != GameState.EN_COURS || !disconnectionTimes.containsKey(uuid)) {
            playerStateManager.restoreState(joueur);
            disconnectionTimes.remove(uuid);
            ajouterCooldownRamassageRelique(uuid, 10000);
            
            // Sécurité : clean du terrain quand un joueur revient si pas de match
            if (etatActuel == GameState.ATTENTE) {
                if (npcManager != null) npcManager.supprimerPNJ(null);
                if (relicManager != null) relicManager.supprimerReliques(null);
            }
            return;
        }

        // 4. Si la partie est en cours et qu'il a dépassé le délai de grâce (cas théorique car isJoueurDansMatch aurait dû être faux)
        playerStateManager.restoreState(joueur);
        disconnectionTimes.remove(uuid);
        relicPickupCooldowns.put(uuid, System.currentTimeMillis() + 10000);
        teamManager.retirerJoueur(joueur);
        joueur.sendMessage(OpsOrbis.get().getLangManager().get("player_grace_expired"));
    }

    /**
     * Retire un joueur de la partie à partir de sa référence (utile pour la déconnexion).
     */
    public void retirerJoueurParRef(PlayerRef ref, Vector3d positionDerniereConnaissance) {
        if (ref == null) return;
        
        UUID uuid = ref.getUuid();
        Player joueur = teamManager.getJoueurParRef(ref);
        
        // Faire tomber la relique si le joueur en porte une (avec position explicite capturée à la déco)
        reconnectionTimes.remove(uuid); 
        relicPickupCooldowns.remove(uuid); // Permet de se reconnecter une N-ième fois
        if (etatActuel == GameState.EN_COURS && relicManager != null) {
            relicManager.dropReliqueSiPorteurParUuid(uuid, joueur, positionDerniereConnaissance);
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
        
        // Drop de relique immédiat
        if (relicManager != null) relicManager.dropReliqueSiPorteur(joueur, null);
        
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
        HytaleUtils.diffuserMessageFiltre(monde, p -> teamManager.getCamp(p) != PlayerCamp.AUCUN, message);
    }

    /**
     * Diffuse une annonce visuelle au centre de l'écran à tous les joueurs.
     */
    public void diffuserAnnonce(World monde, Message titre, Message sousTitre) {
        HytaleUtils.diffuserAnnonceFiltree(monde, p -> teamManager.getCamp(p) != PlayerCamp.AUCUN, titre, sousTitre);
    }

    /**
     * Diffuse une annonce visuelle avec des durées de fondu personnalisées.
     */
    public void diffuserAnnonceRapide(World monde, Message titre, Message sousTitre, float dureeMaintien, float dureeApparition, float dureeDisparition) {
        HytaleUtils.diffuserAnnonceFiltree(monde, p -> teamManager.getCamp(p) != PlayerCamp.AUCUN, titre, sousTitre, dureeMaintien, dureeApparition, dureeDisparition);
    }

    /**
     * Diffuse une annonce visuelle uniquement à un camp spécifique.
     */
    public void diffuserAnnonceEquipe(World monde, PlayerCamp camp, Message titre, Message sousTitre) {
        HytaleUtils.diffuserAnnonceFiltree(monde, p -> teamManager.getCamp(p) == camp, titre, sousTitre);
    }

    public long getTempsRestantManche() { return tempsRestantManche; }
    public StatsManager getStatsManager() { return statsManager; }
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
     * Ajoute ou rafraîchit un cooldown de ramassage de relique pour un joueur.
     */
    public void ajouterCooldownRamassageRelique(UUID uuid, long dureeMillis) {
        if (uuid == null) return;
        relicPickupCooldowns.put(uuid, System.currentTimeMillis() + dureeMillis);
    }

    /**
     * Vérifie si le joueur est actuellement sous cooldown de ramassage de relique.
     */
    public boolean estBloqueRamassageRelique(UUID uuid) {
        Long expiration = relicPickupCooldowns.get(uuid);
        if (expiration == null) return false;
        
        if (System.currentTimeMillis() > expiration) {
            relicPickupCooldowns.remove(uuid);
            return false;
        }
        return true;
    }
}
