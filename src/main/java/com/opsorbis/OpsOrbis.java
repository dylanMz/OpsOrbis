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
import com.opsorbis.game.ui.LobbyUIManager;
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
    private LobbyUIManager lobbyUIManager;
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

        // 3b. Initialisation du gestionnaire de lobby UI
        this.lobbyUIManager = new LobbyUIManager();

        // 4. Enregistrement des commandes
        getCommandRegistry().registerCommand(new GameCommand(gameManager));
        getCommandRegistry().registerCommand(new LobbyCommand(lobbyUIManager));

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
                // On tente une reconnexion immédiate ET une retardée pour pallier les délais de join d'Hytale
                scheduler.schedule(() -> {
                    for (World monde : com.hypixel.hytale.server.core.universe.Universe.get().getWorlds().values()) {
                        monde.execute(() -> {
                            Player joueur = HytaleUtils.getPlayerFromRef(ref, monde);
                            if (joueur != null) {
                                gameManager.gererReconnexion(joueur);
                            }
                        });
                    }
                }, 1, TimeUnit.SECONDS);

                scheduler.schedule(() -> {
                    for (World monde : com.hypixel.hytale.server.core.universe.Universe.get().getWorlds().values()) {
                        monde.execute(() -> {
                            Player joueur = HytaleUtils.getPlayerFromRef(ref, monde);
                            if (joueur != null) {
                                gameManager.gererReconnexion(joueur);
                            }
                        });
                    }
                }, 3, TimeUnit.SECONDS);
            }
        });

        getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            PlayerRef ref = event.getPlayerRef();
            if (ref != null) {
                com.opsorbis.game.logic.MatchInstance match = gameManager.getMatchParJoueurRef(ref);
                if (match != null) {
                    World monde = match.getWorld();
                    if (monde != null) {
                        monde.execute(() -> {
                            final Vector3d positionDeco;
                            Player joueur = match.getTeamManager().getJoueurParRef(ref);
                            if (joueur != null && joueur.getWorld() != null) {
                                Store<EntityStore> store = joueur.getWorld().getEntityStore().getStore();
                                TransformComponent tc = store.getComponent(joueur.getReference(), TransformComponent.getComponentType());
                                positionDeco = (tc != null) ? tc.getPosition().clone() : null;
                            } else {
                                positionDeco = null;
                            }
                            match.retirerJoueurParRef(ref, positionDeco);
                        });
                    }
                }
            }
        });
        
        // 6. Arret automatique si vide (Check toutes les 10 secondes)
        // Note: Désactivé temporairement ou à déplacer dans MatchInstance si nécessaire
        /*scheduler.scheduleAtFixedRate(() -> {
            // ... logic to implement per match instance ...
        }, 10, 10, TimeUnit.SECONDS);*/
        
        // 7. Tick 1 seconde pour toutes les instances
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (World monde : com.hypixel.hytale.server.core.universe.Universe.get().getWorlds().values()) {
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
        for (com.opsorbis.game.logic.MatchInstance match : gameManager.getToutesLesInstances()) {
            for (Player p : match.getTeamManager().getTousLesJoueurs()) {
                if (p != null && p.getWorld() != null) return p.getWorld();
            }
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

    public LobbyUIManager getLobbyUIManager() {
        return lobbyUIManager;
    }

    private void registerPlaceholders() {
        if (langManager == null || gameManager == null) return;

        // Les placeholders globaux n'ont plus de sens direct sans contexte joueur
        // On pourrait utiliser le premier match pour les placeholders globaux ou les masquer
        langManager.registerGlobal("round", () -> 1);
    }

    public void runDelayed(long delayMs, Runnable task) {
        scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }
}
