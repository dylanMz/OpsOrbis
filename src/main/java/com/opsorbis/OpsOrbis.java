package com.opsorbis;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.opsorbis.game.logic.GameManager;
import com.opsorbis.game.systems.FriendlyFireSystem;
import com.opsorbis.game.systems.PlayerFriendlyFireSystem;
import com.opsorbis.game.systems.RelicPickupSystem;
import com.opsorbis.game.systems.RelicDepositSystem;
import com.opsorbis.game.systems.RelicDeathSystem;
import com.opsorbis.game.systems.PlayerRespawnSystem;
import com.opsorbis.game.systems.KillTrackingSystem;
import com.opsorbis.utils.HytaleUtils;
import com.opsorbis.commands.GameCommand;
import com.opsorbis.commands.LobbyCommand;
import com.opsorbis.config.ConfigManager;
import com.opsorbis.config.LangManager;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.logging.Level;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Classe principale du mod Ops Orbis.
 */
public class OpsOrbis extends JavaPlugin {

    private static OpsOrbis instance;
    private GameManager gameManager;
    private ConfigManager configManager;
    private LangManager langManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public OpsOrbis(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static OpsOrbis get() {
        return instance;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("Setup du mod Ops Orbis...");
        
        // 1. Initialisation de la configuration
        this.configManager = new ConfigManager();
        this.langManager = new LangManager(configManager.getGlobalConfig().getLanguage());

        // 2. Initialisation du gestionnaire de jeu
        this.gameManager = new GameManager(this);

        // 3. Enregistrement des placeholders globaux
        registerPlaceholders();

        // 4. Enregistrement des commandes
        getCommandRegistry().registerCommand(new GameCommand(gameManager));
        getCommandRegistry().registerCommand(new LobbyCommand("lobby", "Ouvre la liste des parties"));

        // 4. Enregistrement des systèmes ECS (Dommages, Pickups, Dépôts)
        getEntityStoreRegistry().registerSystem(new FriendlyFireSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new PlayerFriendlyFireSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new RelicPickupSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new RelicDepositSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new RelicDeathSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new PlayerRespawnSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new KillTrackingSystem(gameManager));

        // 5. Enregistrement des évènements (Scoreboard auto-show/hide & Reconnexion)
        getEventRegistry().register(PlayerConnectEvent.class, event -> {
            PlayerRef ref = event.getPlayerRef();
            if (ref != null) {
                // --- NOUVEAU : On bloque le ramassage IMMEDIATEMENT au join ---
                gameManager.ajouterCooldownRamassageRelique(ref.getUuid(), 10000);
                // ------------------------------------------------------------

                long connectionTime = System.currentTimeMillis();
                
                // On tente une reconnexion immédiate ET une retardée pour pallier les délais de join d'Hytale
                scheduler.schedule(() -> {
                    for (World monde : com.hypixel.hytale.server.core.universe.Universe.get().getWorlds().values()) {
                        monde.execute(() -> {
                            Player joueur = HytaleUtils.getPlayerFromRef(ref, monde);
                            if (joueur != null) {
                                gameManager.gererReconnexion(joueur, connectionTime);
                            }
                        });
                    }
                }, 1, TimeUnit.SECONDS);

                scheduler.schedule(() -> {
                    for (World monde : com.hypixel.hytale.server.core.universe.Universe.get().getWorlds().values()) {
                        monde.execute(() -> {
                            Player joueur = HytaleUtils.getPlayerFromRef(ref, monde);
                            if (joueur != null) {
                                gameManager.gererReconnexion(joueur, connectionTime);
                            }
                        });
                    }
                }, 3, TimeUnit.SECONDS);
            }
        });

        getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            PlayerRef ref = event.getPlayerRef();
            if (ref != null) {
                // On essaie de récupérer le monde "default" pour l'exécution
                World monde = com.hypixel.hytale.server.core.universe.Universe.get().getWorld("default");
                
                if (monde != null) {
                    try {
                        monde.execute(() -> {
                            // ON CAPTURE LA POSITION ICI (SYNCHRONE SUR LE THREAD DU MONDE)
                            final Vector3d positionDeco;
                            Player joueur = gameManager.getTeamManager().getJoueurParRef(ref);
                            if (joueur != null && joueur.getWorld() != null) {
                                Store<EntityStore> store = joueur.getWorld().getEntityStore().getStore();
                                TransformComponent tc = store.getComponent(joueur.getReference(), TransformComponent.getComponentType());
                                positionDeco = (tc != null) ? tc.getPosition().clone() : null;
                            } else {
                                positionDeco = null;
                            }
                            
                            gameManager.retirerJoueurParRef(ref, positionDeco);
                        });
                    } catch (Exception e) {
                        // Ignored: world probably shutting down
                    }
                }
            }
        });
        
        // 6. Arret automatique si vide (Check toutes les 10 secondes)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                gameManager.verifierArretAutomatique();
            } catch (Exception e) {
                getLogger().at(Level.SEVERE).log("Erreur lors du check auto-stop: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
        
        // 7. Tick 1 seconde pour le démarrage automatique et le chrono de match
        scheduler.scheduleAtFixedRate(() -> {
            try {
                com.hypixel.hytale.server.core.universe.world.World monde = getPremierMonde();
                if (monde != null) {
                    monde.execute(() -> gameManager.tickSeconde(monde));
                }
            } catch (Exception e) {
                getLogger().at(Level.SEVERE).log("Erreur lors du tick 1s: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);

        getLogger().at(Level.INFO).log("Initialisation des systèmes et commandes terminée.");
    }

    /**
     * Tente de récupérer une instance de monde via les joueurs connectés.
     */
    private com.hypixel.hytale.server.core.universe.world.World getPremierMonde() {
        for (Player p : gameManager.getTeamManager().getEquipeAttaquants()) {
            if (p != null && p.getWorld() != null) return p.getWorld();
        }
        for (Player p : gameManager.getTeamManager().getEquipeDefenseurs()) {
            if (p != null && p.getWorld() != null) return p.getWorld();
        }
        return null;
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("Le mod Ops Orbis (Mini-Jeu 5v5) a bien été chargé !");
    }

    @Override
    protected void shutdown() {
        if (configManager != null) {
            configManager.save();
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }



    private void registerPlaceholders() {
        if (langManager == null || gameManager == null) return;

        langManager.registerGlobal("round", () -> gameManager.getRoundActuel());
        langManager.registerGlobal("max_round", () -> configManager.getGlobalConfig().getMaxRounds());
        langManager.registerGlobal("score1", () -> gameManager.getTeamManager().getScoreEquipe1());
        langManager.registerGlobal("score2", () -> gameManager.getTeamManager().getScoreEquipe2());
        langManager.registerGlobal("time", () -> {
            long t = gameManager.getTempsRestantManche();
            return String.format("%02d:%02d", t / 60, t % 60);
        });
    }

    public void runDelayed(long delayMs, Runnable task) {
        scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }
}
