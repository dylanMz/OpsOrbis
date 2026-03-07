package com.experience.commands;

import com.experience.ExperienceMod;
import com.experience.game.GameManager;
import com.experience.kits.KitManager;
import com.experience.config.GameConfig;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.prefab.selection.SelectionManager;
import com.hypixel.hytale.server.core.prefab.selection.SelectionProvider;
import com.hypixel.hytale.math.shape.Box;
import java.awt.Color;

import java.util.logging.Level;

/**
 * Commande principale du mini-jeu : /game
 */
public class GameCommand extends CommandBase {

    private final GameManager gameManager;

    public GameCommand(GameManager gameManager) {
        super("game", "Commandes du mini-jeu Experience 5v5");
        this.setAllowsExtraArguments(true);
        this.gameManager = gameManager;
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        if (!ctx.isPlayer()) return;

        Player joueur = (Player) ctx.sender();
        String input = ctx.getInputString();
        String[] args = input.trim().split("\\s+");

        if (args.length < 2) {
            joueur.sendMessage(Message.raw("Usage: /game <join|kit|start|config>").color(Color.RED));
            return;
        }

        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "join":
                gameManager.getTeamManager().ajouterJoueur(joueur);
                break;
            case "kit":
                handleKitCommand(joueur, args);
                break;
            case "start":
                gameManager.demarrerPartie(joueur.getWorld());
                break;
            case "config":
                handleConfigCommand(joueur, args);
                break;
            default:
                joueur.sendMessage(Message.raw("Commande inconnue.").color(Color.RED));
                break;
        }
    }

    private void handleKitCommand(Player joueur, String[] args) {
        if (args.length < 3) {
            joueur.sendMessage(Message.raw("Usage: /game kit <guerrier|archer|mage>").color(Color.RED));
            return;
        }
        try {
            KitManager.KitType kit = KitManager.KitType.valueOf(args[2].toUpperCase());
            gameManager.getKitManager().choisirKit(joueur, kit);
        } catch (IllegalArgumentException e) {
            joueur.sendMessage(Message.raw("Kit invalide.").color(Color.RED));
        }
    }

    private void handleConfigCommand(Player joueur, String[] args) {
        if (!joueur.hasPermission("experience.admin")) {
            joueur.sendMessage(Message.raw("Vous n'avez pas la permission.").color(Color.RED));
            return;
        }

        if (args.length < 3) {
            joueur.sendMessage(Message.raw("Usage: /game config <setzone|setspawn|save> ...").color(Color.RED));
            return;
        }

        String action = args[2].toLowerCase();
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();

        switch (action) {
            case "setzone":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /game config setzone <blue|red>").color(Color.RED));
                    return;
                }
                setZoneFromSelection(joueur, args[3].equalsIgnoreCase("blue"), config);
                break;

            case "setspawn":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /game config setspawn <blue|red>").color(Color.RED));
                    return;
                }
                if (args[3].equalsIgnoreCase("blue")) {
                    config.setBlueSpawn(joueur.getTransformComponent().getPosition().clone());
                } else {
                    config.setRedSpawn(joueur.getTransformComponent().getPosition().clone());
                }
                joueur.sendMessage(Message.raw("Spawn PNJ définit à votre position.").color(Color.GREEN));
                break;

            case "save":
                ExperienceMod.get().getConfigManager().save();
                joueur.sendMessage(Message.raw("Configuration sauvegardée !").color(Color.GREEN));
                break;
        }
    }

    /**
     * Récupère la sélection de l'éditeur Hytale du joueur et l'assigne à une équipe.
     */
    private void setZoneFromSelection(Player joueur, boolean isBlue, GameConfig config) {
        SelectionProvider provider = SelectionManager.getSelectionProvider();
        if (provider == null) {
            joueur.sendMessage(Message.raw("Erreur : SelectionProvider introuvable.").color(Color.RED));
            return;
        }

        joueur.getWorld().execute(() -> {
            provider.computeSelectionCopy(joueur.getReference(), joueur, (selection) -> {
                if (selection != null && selection.hasSelectionBounds()) {
                    Box newBox = new Box(selection.getSelectionMin().toVector3d(), selection.getSelectionMax().toVector3d());
                    if (isBlue) {
                        config.setBlueZone(newBox);
                    } else {
                        config.setRedZone(newBox);
                    }
                    joueur.sendMessage(Message.raw("Zone définie avec succès via l'outil d'édition !").color(Color.GREEN));
                } else {
                    joueur.sendMessage(Message.raw("Aucune zone sélectionnée dans l'éditeur.").color(Color.RED));
                }
            }, joueur.getWorld().getEntityStore().getStore());
        });
    }
}
