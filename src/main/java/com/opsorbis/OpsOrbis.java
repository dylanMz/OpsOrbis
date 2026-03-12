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
import com.opsorbis.commands.GameCommand;
import com.opsorbis.config.ConfigManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.logging.Level;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.lang.reflect.Method;

/**
 * Classe principale du mod Ops Orbis.
 */
public class OpsOrbis extends JavaPlugin {

    private static OpsOrbis instance;
    private GameManager gameManager;
    private ConfigManager configManager;
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

        // 2. Initialisation du gestionnaire de jeu
        this.gameManager = new GameManager(this);

        // 3. Enregistrement des commandes
        getCommandRegistry().registerCommand(new GameCommand(gameManager));

        // 4. Enregistrement des systèmes ECS (Dommages, Pickups, Dépôts)
        getEntityStoreRegistry().registerSystem(new FriendlyFireSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new PlayerFriendlyFireSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new RelicPickupSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new RelicDepositSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new RelicDeathSystem(gameManager));
        getEntityStoreRegistry().registerSystem(new PlayerRespawnSystem(gameManager));

        // 5. Enregistrement des évènements (Scoreboard auto-show/hide & Reconnexion)
        getEventRegistry().register(PlayerConnectEvent.class, event -> {
            Player joueur = event.getPlayer();
            if (joueur != null) {
                long connectionTime = System.currentTimeMillis();
                // On attend 2 secondes que le joueur soit bien ajouté au monde pour éviter les NPE
                scheduler.schedule(() -> {
                    gameManager.gererReconnexion(joueur, connectionTime);
                }, 2, TimeUnit.SECONDS);
            }
        });

        getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            gameManager.retirerJoueurParRef(event.getPlayerRef());
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

    public void runDelayed(long delayMs, Runnable task) {
        scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }
}
