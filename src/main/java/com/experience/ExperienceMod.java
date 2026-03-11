package com.experience;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.experience.game.logic.GameManager;
import com.experience.game.systems.FriendlyFireSystem;
import com.experience.game.systems.PlayerFriendlyFireSystem;
import com.experience.game.systems.RelicPickupSystem;
import com.experience.game.systems.RelicDepositSystem;
import com.experience.game.systems.RelicDeathSystem;
import com.experience.game.systems.PlayerRespawnSystem;
import com.experience.game.systems.MatchTimerSystem;
import com.experience.commands.GameCommand;
import com.experience.config.ConfigManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import java.util.logging.Level;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Classe principale du mod ExperienceMod.
 */
public class ExperienceMod extends JavaPlugin {

    private static ExperienceMod instance;
    private GameManager gameManager;
    private ConfigManager configManager;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ExperienceMod(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static ExperienceMod get() {
        return instance;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("Setup du mod ExperienceMod (Mini-Jeu 5v5)...");
        
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
        getEntityStoreRegistry().registerSystem(new MatchTimerSystem(gameManager));

        // 5. Enregistrement des évènements (Scoreboard auto-show/hide)
        getEventRegistry().register(PlayerConnectEvent.class, event -> {
            if (gameManager.getEtatActuel() == GameManager.GameState.EN_COURS && event.getPlayer() != null) {
                gameManager.getScoreboardHUD().afficher(event.getPlayer());
            }
        });

        getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            // Pas besoin du HUD manager ici car le joueur quitte, 
            // on retire juste de notre map interne pour éviter les fuites.
            gameManager.getScoreboardHUD().masquer(event.getPlayerRef(), null);
        });
        
        getLogger().at(Level.INFO).log("Initialisation des systèmes et commandes terminée.");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("Le mod ExperienceMod (Mini-Jeu 5v5) a bien été chargé !");
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
