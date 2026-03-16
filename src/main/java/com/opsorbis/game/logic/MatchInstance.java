package com.opsorbis.game.logic;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.GlobalConfig;
import com.opsorbis.config.LangManager;
import com.opsorbis.config.MapConfig;
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

/**
 * Représente une instance isolée d'une partie (Match).
 * Cette classe encapsule toute la logique, les managers et l'état d'un jeu spécifique.
 * Elle permet d'isoler les joueurs, les scores et les mécaniques par ID de match.
 */
public class MatchInstance {

    /** Identifiant unique du match utilisé pour filtrer les tics ECS et les commandes. */
    private final UUID matchId;
    
    /** Le monde Hytale dans lequel se déroule ce match. */
    private final World monde;
    
    /** Configuration spécifique à la carte (spawns, zones, etc.). */
    private final MapConfig mapConfig;
    
    /** État actuel de la partie (ATTENTE, EN_COURS, PAUSE, etc.). */
    private GameManager.GameState etatActuel;
    
    // Managers spécifiques à cette instance de match
    private final TeamManager teamManager;
    private final KitManager kitManager;
    private final RolesManager rolesManager;
    private NPCManager npcManager;
    private RelicManager relicManager;
    private final ScoreboardHUD scoreboardHUD;
    private final PlayerStateManager playerStateManager;
    private final StatsManager statsManager;
    
    /** Référence au plugin principal pour accéder aux services globaux. */
    private final OpsOrbis plugin;
    
    // Suivi des joueurs pour la gestion des déconnexions/reconnexions
    private final Map<UUID, Long> disconnectionTimes = new HashMap<>();
    private final Map<UUID, Long> reconnectionTimes = new HashMap<>();
    private final Map<UUID, Long> relicPickupCooldowns = new HashMap<>(); // Empêche de reprendre une relique immédiatement après reconnexion
    
    /** Variables de contrôle du temps et des manches. */
    private int compteAReboursDemarrage;
    private int roundActuel = 1;
    private long tempsRestantManche;
    private long tempsSansJoueursMillis = 0; // Utilisé pour auto-supprimer les matches vides

    /**
     * Initialise une nouvelle instance de match.
     * @param plugin Référence au plugin.
     * @param monde Monde de l'instance.
     * @param mapConfig Configuration de la carte.
     */
    public MatchInstance(OpsOrbis plugin, World monde, MapConfig mapConfig) {
        this.matchId = UUID.randomUUID();
        this.plugin = plugin;
        this.monde = monde;
        this.mapConfig = mapConfig;
        
        this.etatActuel = GameManager.GameState.ATTENTE;
        
        // Initialisation des managers avec 'this' pour qu'ils connaissent leur contexte de match
        this.playerStateManager = new PlayerStateManager();
        this.teamManager = new TeamManager(this);
        this.kitManager = new KitManager(this);
        this.rolesManager = new RolesManager(this);
        this.scoreboardHUD = new ScoreboardHUD(this);
        this.statsManager = new StatsManager();
        
        // Chargement des délais depuis la config globale
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        this.compteAReboursDemarrage = global.getAutoStartDelaySeconds();
        this.tempsRestantManche = global.getRoundDurationSeconds();
    }

    public UUID getMatchId() { return matchId; }
    public World getWorld() { return monde; }
    public MapConfig getMapConfig() { return mapConfig; }

    /**
     * Démarre la partie pour cette instance.
     */
    public void demarrerMatch() {
        if (monde == null) return;
        
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        HytaleLogger.getLogger().at(Level.INFO).log("[Match " + matchId + "] Démarrage (" + global.getMaxRounds() + " manches)...");
 
        if (etatActuel == GameManager.GameState.EN_COURS) return;
 
        this.etatActuel = GameManager.GameState.EN_COURS;
        teamManager.resetScoresEtRoles();
        statsManager.reset();
        this.roundActuel = 1;
        
        this.npcManager = new NPCManager(this);
        this.relicManager = new RelicManager(this);

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

        monde.execute(() -> {
            for (Player p : teamManager.getTousLesJoueurs()) {
                scoreboardHUD.afficher(p);
            }
        });

        demarrerRound();
    }

    /**
     * Démarre une nouvelle manche (round).
     * Réinitialise les PNJ, les reliques et téléporte les joueurs à leurs spawns respectifs.
     */
    public void demarrerRound() {
        HytaleUtils.diffuserMessage(monde, plugin.getLangManager().get("match_start_chat", roundActuel));

        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            // Nettoyage et réapparition des entités de jeu
            if (npcManager != null) npcManager.supprimerPNJ(null);
            if (relicManager != null) relicManager.supprimerReliques(null);
            if (npcManager != null) npcManager.faireApparaitrePNJ_Direct(store);
            if (relicManager != null) relicManager.initRelics_Direct(store);
        });

        // Téléportation de sécurité et annonce
        for (Player p : teamManager.getTousLesJoueurs()) {
            playerStateManager.saveState(p);
            teamManager.teleporterAuSpawn(p);
        }

        HytaleUtils.diffuserMessage(monde, plugin.getLangManager().get("round_start_chat"));
        
        LangManager lang = plugin.getLangManager();
        plugin.getGameManager().diffuserAnnonceInstance(this,
            lang.get("match_start_title"),
            lang.get(teamManager.isEquipe1Attaquant() ? "team_1_attacking" : "team_1_defending").color(Color.WHITE)
        );
        
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        this.tempsRestantManche = global.getRoundDurationSeconds();
    }
 
    public void diffuserAnnonceJoueur(Player joueur, Message titre, Message sousTitre) {
        if (joueur != null && monde != null) {
            HytaleUtils.diffuserAnnonceFiltree(monde, p -> p.equals(joueur), titre, sousTitre);
        }
    }
 
    public void diffuserAnnonceEquipe(PlayerCamp camp, Message titre, Message sousTitre) {
        if (monde != null) {
            HytaleUtils.diffuserAnnonceFiltree(monde, p -> teamManager.isJoueurDansMatch(p) && teamManager.getCamp(p) == camp, titre, sousTitre);
        }
    }

    /**
     * Appelé chaque seconde par le GameManager pour cette instance.
     * Gère le compte à rebours, la zone de jeu et le timer de la manche.
     */
    public void tickSeconde() {
        if (monde == null) return;

        verifierDemarrageAutomatique();

        if (etatActuel == GameManager.GameState.EN_COURS) {
            this.tempsRestantManche--;
            monde.execute(scoreboardHUD::rafraichirTous);

            // Fin de la manche si le temps est écoulé (Victoire des défenseurs)
            if (tempsRestantManche <= 0) {
                HytaleUtils.diffuserMessage(monde, plugin.getLangManager().get("match_time_up"));
                terminerRound(PlayerCamp.DEFENSEUR, null);
            }

            // Vérification que les joueurs ne sortent pas de la zone autorisée
            verifierGameZone();
        }
    }

    private void verifierDemarrageAutomatique() {
        GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
        if (!global.isAutoStartEnabled()) return;

        int nbJoueurs = teamManager.getNombreTotalJoueurs();

        if (etatActuel == GameManager.GameState.ATTENTE) {
            if (nbJoueurs >= global.getMinPlayersToStart()) {
                this.etatActuel = GameManager.GameState.DEMARRAGE;
                this.compteAReboursDemarrage = global.getAutoStartDelaySeconds();
                HytaleUtils.diffuserMessage(monde, plugin.getLangManager().get("timer_auto_start", "seconds", compteAReboursDemarrage));
            }
        } else if (etatActuel == GameManager.GameState.DEMARRAGE) {
            if (nbJoueurs < global.getMinPlayersToStart()) {
                this.etatActuel = GameManager.GameState.ATTENTE;
                HytaleUtils.diffuserMessage(monde, plugin.getLangManager().get("match_start_cancelled"));
                return;
            }

            compteAReboursDemarrage--;

            if (compteAReboursDemarrage == 15 || compteAReboursDemarrage == 10 || (compteAReboursDemarrage <= 5 && compteAReboursDemarrage > 0)) {
                HytaleUtils.diffuserMessage(monde, plugin.getLangManager().get("timer_seconds_left", "seconds", compteAReboursDemarrage));
            }

            if (compteAReboursDemarrage <= 0) {
                demarrerMatch();
            }
        }
    }

    /**
     * Vérifie si les joueurs sont toujours dans la zone définie pour ce match.
     * Si un joueur sort, il est retéléporté à son spawn.
     */
    public void verifierGameZone() {
        com.hypixel.hytale.math.shape.Box zone = mapConfig.getGameZone();
        if (zone == null) return;

        for (Player p : teamManager.getTousLesJoueurs()) {
            if (p == null || p.getWorld() == null || p.getReference() == null) continue;
            
            Store<EntityStore> store = p.getWorld().getEntityStore().getStore();
            com.hypixel.hytale.server.core.modules.entity.component.TransformComponent tc = store.getComponent(p.getReference(), com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            
            if (tc != null) {
                Vector3d pos = tc.getPosition();
                Vector3d min = zone.getMin();
                Vector3d max = zone.getMax();

                // On vérifie si les coordonnées sont à l'intérieur de la boîte (Box)
                boolean isInside = pos.x >= min.x && pos.x <= max.x &&
                                 pos.y >= min.y && pos.y <= max.y &&
                                 pos.z >= min.z && pos.z <= max.z;

                if (!isInside) {
                    monde.execute(() -> {
                        p.sendMessage(plugin.getLangManager().get("cmd_out_of_bounds"));
                        teamManager.teleporterAuSpawn(p);
                    });
                }
            }
        }
    }

    /**
     * Termine une manche et attribue le point à l'équipe vainqueur.
     * @param campVainqueur L'équipe qui a gagné la manche.
     * @param buffer Optionnel: Buffer ECS pour la suppression synchrone d'entités.
     */
    public void terminerRound(PlayerCamp campVainqueur, com.hypixel.hytale.component.CommandBuffer<EntityStore> buffer) {
        teamManager.ajouterPointEquipe(campVainqueur);
        LangManager lang = plugin.getLangManager();
        String roleName = lang.getRaw(campVainqueur.getLangKey());
        
        // Annonces globales à l'instance
        HytaleUtils.diffuserMessage(monde, lang.get("round_winner_chat", "round", roundActuel, "camp", roleName));
        
        plugin.getGameManager().diffuserAnnonceInstance(this,
            lang.get("round_winner_title", "round", roundActuel, "camp", roleName),
            lang.get("round_winner_subtitle", "round", roundActuel, "role", roleName)
        );

        // Nettoyage des entités de la manche passée
        if (npcManager != null) npcManager.supprimerPNJ(buffer);
        if (relicManager != null) relicManager.supprimerReliques(buffer);
        
        this.etatActuel = GameManager.GameState.PAUSE;
        this.tempsRestantManche = 0;

        // Délai avant de passer à la manche suivante ou de finir le match
        plugin.runDelayed(10000, () -> {
            monde.execute(() -> {
                this.roundActuel++;
                GlobalConfig global = plugin.getConfigManager().getGlobalConfig();
                
                if (roundActuel > global.getMaxRounds()) {
                    terminerMatchGlobal();
                } else {
                    // Inversion des rôles à la mi-temps
                    if (roundActuel == (global.getMaxRounds() / 2) + 1) {
                        HytaleUtils.diffuserMessage(monde, plugin.getLangManager().get("half_time_swap"));
                        teamManager.inverserRoles();
                    }
                    this.etatActuel = GameManager.GameState.EN_COURS;
                    demarrerRound();
                }
            });
        });
    }

    public void terminerMatchGlobal() {
        this.etatActuel = GameManager.GameState.TERMINEE;
        
        String texteVainqueur;
        LangManager lang = plugin.getLangManager();
        if (teamManager.getScoreEquipe1() > teamManager.getScoreEquipe2()) {
            texteVainqueur = lang.getRaw("summary_victory_title", "camp", lang.getRaw("team_1_name"));
        } else if (teamManager.getScoreEquipe2() > teamManager.getScoreEquipe1()) {
            texteVainqueur = lang.getRaw("summary_victory_title", "camp", lang.getRaw("team_2_name"));
        } else {
            texteVainqueur = lang.getRaw("match_draw");
        }
        
        HytaleUtils.diffuserMessage(monde, Message.join(
            lang.get("match_over_header"),
            lang.get("match_over_scores"),
            lang.get("match_over_winner_label", "winner", texteVainqueur)
        ));

        List<Player> participants = new ArrayList<>(teamManager.getTousLesJoueurs());
        scoreboardHUD.masquerPourTousParticipants();

        plugin.runDelayed(10000, () -> {
            monde.execute(() -> {
                for (Player p : participants) {
                    retirerJoueurDuMatch(p); 
                }
                teamManager.viderEquipes();
                teamManager.resetScoresEtRoles();
                disconnectionTimes.clear();
                // Notifier le GameManager que ce match est fini
                plugin.getGameManager().supprimerInstance(this);
            });
        });
    }

    public void forcerArret() {
        this.etatActuel = GameManager.GameState.ATTENTE;
        HytaleUtils.diffuserMessage(monde, plugin.getLangManager().get("match_stopped_admin"));

        monde.execute(() -> {
            if (npcManager != null) npcManager.supprimerPNJ(null);
            if (relicManager != null) relicManager.supprimerReliques(null);
            
            List<Player> participants = new ArrayList<>(teamManager.getTousLesJoueurs());
            scoreboardHUD.masquerPourTousParticipants();

            for (Player p : participants) {
                retirerJoueurDuMatch(p); 
            }
            
            teamManager.viderEquipes();
            teamManager.resetScoresEtRoles();
            disconnectionTimes.clear();
            tempsSansJoueursMillis = 0;
            plugin.getGameManager().supprimerInstance(this);
        });
    }

    public void retirerJoueurDuMatch(Player joueur) {
        if (joueur == null) return;
        if (relicManager != null) relicManager.dropReliqueSiPorteur(joueur, null);
        if (playerStateManager.hasSavedState(joueur)) {
            playerStateManager.restoreState(joueur);
        }
        teamManager.retirerJoueur(joueur);
        scoreboardHUD.masquer(HytaleUtils.getPlayerRef(joueur), joueur);
    }

    public void gererReconnexion(Player joueur, long connectionTime) {
        if (joueur == null) return;
        UUID uuid = HytaleUtils.getPlayerUuid(joueur);
        if (uuid == null) return;

        if (reconnectionTimes.containsKey(uuid)) return;

        if (!playerStateManager.hasSavedState(uuid)) return;

        if (teamManager.isJoueurDansMatch(joueur)) {
            disconnectionTimes.remove(uuid);
            reconnectionTimes.put(uuid, connectionTime);
            ajouterCooldownRamassageRelique(uuid, 10000);
            teamManager.mettreAJourJoueur(joueur);
            
            joueur.sendMessage(plugin.getLangManager().get("player_reconnect_success"));
            scoreboardHUD.afficher(joueur);
            joueur.getInventory().clear();
            
            if (etatActuel == GameManager.GameState.EN_COURS && kitManager != null) {
                kitManager.donnerEquipement(joueur);
            }
        }
    }

    public void retirerJoueurParRef(PlayerRef ref, Vector3d pos) {
        UUID uuid = ref.getUuid();
        Player joueur = teamManager.getJoueurParRef(ref);
        reconnectionTimes.remove(uuid);
        relicPickupCooldowns.remove(uuid);
        if (etatActuel == GameManager.GameState.EN_COURS && relicManager != null) {
            relicManager.dropReliqueSiPorteurParUuid(uuid, joueur, pos);
        }
        if (playerStateManager.hasSavedState(uuid)) {
            disconnectionTimes.put(uuid, System.currentTimeMillis());
        }
    }

    public void ajouterCooldownRamassageRelique(UUID uuid, long duree) {
        relicPickupCooldowns.put(uuid, System.currentTimeMillis() + duree);
    }

    public boolean estBloqueRamassageRelique(UUID uuid) {
        Long expiration = relicPickupCooldowns.get(uuid);
        if (expiration == null) return false;
        if (System.currentTimeMillis() > expiration) {
            relicPickupCooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public TeamManager getTeamManager() { return teamManager; }
    public RelicManager getRelicManager() { return relicManager; }
    public NPCManager getNpcManager() { return npcManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public GameManager.GameState getEtatActuel() { return etatActuel; }
    public int getRoundActuel() { return roundActuel; }
    public long getTempsRestantManche() { return tempsRestantManche; }
    public ScoreboardHUD getScoreboardHUD() { return scoreboardHUD; }
    public RolesManager getRolesManager() { return rolesManager; }
    public KitManager getKitManager() { return kitManager; }
    public PlayerStateManager getPlayerStateManager() { return playerStateManager; }
}
