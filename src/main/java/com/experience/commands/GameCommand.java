package com.experience.commands;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
import com.experience.game.logic.GameManager;
import com.experience.kits.KitManager;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.selection.SelectionManager;
import com.hypixel.hytale.server.core.prefab.selection.SelectionProvider;

import java.awt.*;

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
            joueur.sendMessage(Message.raw("Usage: /game config <setzone|setrelic|setdeposit|setnpcspawn|save> ...").color(Color.RED));
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

            case "setrelic":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /game config setrelic <b1|b2|r1|r2>").color(Color.RED));
                    return;
                }
                assert joueur.getWorld() != null;
                joueur.getWorld().execute(() -> {
                    assert joueur.getReference() != null;
                    TransformComponent transform = joueur.getWorld().getEntityStore().getStore().getComponent(joueur.getReference(), TransformComponent.getComponentType());
                    if (transform == null) return;
                    Vector3d currentPos = transform.getPosition().clone();
                    switch (args[3].toLowerCase()) {
                        case "b1":
                            config.setBlueRelic1(currentPos);
                            joueur.sendMessage(Message.raw("Relique Bleue 1 définie.").color(Color.GREEN));
                            break;
                        case "b2":
                            config.setBlueRelic2(currentPos);
                            joueur.sendMessage(Message.raw("Relique Bleue 2 définie.").color(Color.GREEN));
                            break;
                        case "r1":
                            config.setRedRelic1(currentPos);
                            joueur.sendMessage(Message.raw("Relique Rouge 1 définie.").color(Color.GREEN));
                            break;
                        case "r2":
                            config.setRedRelic2(currentPos);
                            joueur.sendMessage(Message.raw("Relique Rouge 2 définie.").color(Color.GREEN));
                            break;
                        default:
                            joueur.sendMessage(Message.raw("Relique invalide (choisir b1, b2, r1, r2).").color(Color.RED));
                            break;
                    }
                });
                break;

            case "setdeposit":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /game config setdeposit <blue|red>").color(Color.RED));
                    return;
                }
                setDepositZoneFromSelection(joueur, args[3].equalsIgnoreCase("blue"), config);
                break;

            case "setnpcspawn":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /game config setnpcspawn <blue|red>").color(Color.RED));
                    return;
                }
                assert joueur.getWorld() != null;
                joueur.getWorld().execute(() -> {
                    assert joueur.getReference() != null;
                    TransformComponent transform = joueur.getWorld().getEntityStore().getStore().getComponent(joueur.getReference(), TransformComponent.getComponentType());
                    if (transform == null) return;
                    Vector3d currentPos = transform.getPosition().clone();
                    if (args[3].equalsIgnoreCase("blue")) {
                        config.setBlueNpcSpawn(currentPos);
                        joueur.sendMessage(Message.raw("Spawn PNJ Bleu défini.").color(Color.GREEN));
                    } else if (args[3].equalsIgnoreCase("red")) {
                        config.setRedNpcSpawn(currentPos);
                        joueur.sendMessage(Message.raw("Spawn PNJ Rouge défini.").color(Color.GREEN));
                    } else {
                        joueur.sendMessage(Message.raw("Équipe invalide.").color(Color.RED));
                    }
                });
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

        assert joueur.getWorld() != null;
        joueur.getWorld().execute(() -> {
            assert joueur.getReference() != null;
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

    private void setDepositZoneFromSelection(Player joueur, boolean isBlue, GameConfig config) {
        SelectionProvider provider = SelectionManager.getSelectionProvider();
        if (provider == null) {
            joueur.sendMessage(Message.raw("Erreur : SelectionProvider introuvable.").color(Color.RED));
            return;
        }

        assert joueur.getWorld() != null;
        joueur.getWorld().execute(() -> {
            assert joueur.getReference() != null;
            provider.computeSelectionCopy(joueur.getReference(), joueur, (selection) -> {
                if (selection != null && selection.hasSelectionBounds()) {
                    Box newBox = new Box(selection.getSelectionMin().toVector3d(), selection.getSelectionMax().toVector3d());
                    if (isBlue) {
                        config.setBlueDepositZone(newBox);
                    } else {
                        config.setRedDepositZone(newBox);
                    }
                    joueur.sendMessage(Message.raw("Zone de dépôt définie avec succès !").color(Color.GREEN));
                } else {
                    joueur.sendMessage(Message.raw("Aucune zone sélectionnée dans l'éditeur.").color(Color.RED));
                }
            }, joueur.getWorld().getEntityStore().getStore());
        });
    }
}
