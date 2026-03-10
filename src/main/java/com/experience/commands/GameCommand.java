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
 * Commande principale : /game
 *
 * Sous-commandes disponibles :
 *   /game join                              — Rejoindre une équipe
 *   /game kit <guerrier|archer|mage>        — Choisir un kit
 *   /game start                             — Démarrer la partie
 *   /game config setzone <attaquant|defenseur>
 *   /game config setrelic <1|2>
 *   /game config setnpcspawn <1|2>
 *   /game config setdeposit
 *   /game config save
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
        String[] args = ctx.getInputString().trim().split("\\s+");

        if (args.length < 2) {
            joueur.sendMessage(Message.raw("Usage: /game <join|kit|start|config>").color(Color.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
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
            envoyerAideConfig(joueur);
            return;
        }

        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        switch (args[2].toLowerCase()) {

            // ── Zone de spawn ──────────────────────────────────────────────────
            case "setzone":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /game config setzone <attaquant|defenseur>").color(Color.RED));
                    return;
                }
                setZoneDepuisSelection(joueur, args[3], config);
                break;

            // ── Position des reliques (2 reliques, côté défenseur) ─────────────
            case "setrelic":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /game config setrelic <1|2>").color(Color.RED));
                    return;
                }
                setRelicDepuisPosition(joueur, args[3], config);
                break;

            // ── Spawn PNJ (1 ou 2) ──────────────────────────────────────────────
            case "setnpcspawn":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /game config setnpcspawn <1|2>").color(Color.RED));
                    return;
                }
                setNpcSpawnDepuisPosition(joueur, args[3], config);
                break;

            // ── Zone de dépôt attaquants ────────────────────────────────────────
            case "setdeposit":
                setDepositDepuisSelection(joueur, config);
                break;

            // ── Sauvegarde ──────────────────────────────────────────────────────
            case "save":
                ExperienceMod.get().getConfigManager().save();
                joueur.sendMessage(Message.raw("Configuration sauvegardée !").color(Color.GREEN));
                break;

            default:
                envoyerAideConfig(joueur);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void envoyerAideConfig(Player joueur) {
        joueur.sendMessage(Message.join(
            Message.raw("Config — sous-commandes :").color(Color.YELLOW),
            Message.raw("\n • setzone <attaquant|defenseur>  — Zone de spawn").color(Color.WHITE),
            Message.raw("\n • setrelic <1|2>                — Position relique").color(Color.WHITE),
            Message.raw("\n • setnpcspawn <1|2>             — Spawn PNJ").color(Color.WHITE),
            Message.raw("\n • setdeposit                    — Zone dépôt attaquants").color(Color.WHITE),
            Message.raw("\n • save                           — Sauvegarder").color(Color.WHITE)
        ));
    }

    private void setZoneDepuisSelection(Player joueur, String equipe, GameConfig config) {
        SelectionProvider provider = SelectionManager.getSelectionProvider();
        if (provider == null) {
            joueur.sendMessage(Message.raw("SelectionProvider introuvable.").color(Color.RED));
            return;
        }
        joueur.getWorld().execute(() ->
            provider.computeSelectionCopy(joueur.getReference(), joueur, (selection) -> {
                if (selection == null || !selection.hasSelectionBounds()) {
                    joueur.sendMessage(Message.raw("Aucune zone sélectionnée.").color(Color.RED));
                    return;
                }
                Box newBox = new Box(selection.getSelectionMin().toVector3d(), selection.getSelectionMax().toVector3d());
                switch (equipe.toLowerCase()) {
                    case "attaquant":
                        config.setAttackerZone(newBox);
                        joueur.sendMessage(Message.raw("✓ Zone Attaquants définie.").color(Color.GREEN));
                        break;
                    case "defenseur":
                        config.setDefenderZone(newBox);
                        joueur.sendMessage(Message.raw("✓ Zone Défenseurs définie.").color(Color.GREEN));
                        break;
                    default:
                        joueur.sendMessage(Message.raw("Équipe invalide (attaquant ou defenseur).").color(Color.RED));
                }
            }, joueur.getWorld().getEntityStore().getStore())
        );
    }

    private void setRelicDepuisPosition(Player joueur, String num, GameConfig config) {
        joueur.getWorld().execute(() -> {
            TransformComponent t = joueur.getWorld().getEntityStore().getStore()
                .getComponent(joueur.getReference(), TransformComponent.getComponentType());
            if (t == null) return;
            Vector3d pos = t.getPosition().clone();
            switch (num) {
                case "1":
                    config.setRelic1(pos);
                    joueur.sendMessage(Message.raw("✓ Relique 1 positionnée.").color(Color.GREEN));
                    break;
                case "2":
                    config.setRelic2(pos);
                    joueur.sendMessage(Message.raw("✓ Relique 2 positionnée.").color(Color.GREEN));
                    break;
                default:
                    joueur.sendMessage(Message.raw("Numéro invalide (1 ou 2).").color(Color.RED));
            }
        });
    }

    private void setNpcSpawnDepuisPosition(Player joueur, String num, GameConfig config) {
        joueur.getWorld().execute(() -> {
            TransformComponent t = joueur.getWorld().getEntityStore().getStore()
                .getComponent(joueur.getReference(), TransformComponent.getComponentType());
            if (t == null) return;
            Vector3d pos = t.getPosition().clone();
            switch (num) {
                case "1":
                    config.setNpcSpawn1(pos);
                    joueur.sendMessage(Message.raw("✓ Spawn PNJ 1 défini.").color(Color.GREEN));
                    break;
                case "2":
                    config.setNpcSpawn2(pos);
                    joueur.sendMessage(Message.raw("✓ Spawn PNJ 2 défini.").color(Color.GREEN));
                    break;
                default:
                    joueur.sendMessage(Message.raw("Numéro invalide (1 ou 2).").color(Color.RED));
            }
        });
    }

    private void setDepositDepuisSelection(Player joueur, GameConfig config) {
        SelectionProvider provider = SelectionManager.getSelectionProvider();
        if (provider == null) {
            joueur.sendMessage(Message.raw("SelectionProvider introuvable.").color(Color.RED));
            return;
        }
        joueur.getWorld().execute(() ->
            provider.computeSelectionCopy(joueur.getReference(), joueur, (selection) -> {
                if (selection == null || !selection.hasSelectionBounds()) {
                    joueur.sendMessage(Message.raw("Aucune zone sélectionnée.").color(Color.RED));
                    return;
                }
                config.setDepositZone(new Box(selection.getSelectionMin().toVector3d(), selection.getSelectionMax().toVector3d()));
                joueur.sendMessage(Message.raw("✓ Zone de dépôt Attaquants définie.").color(Color.GREEN));
            }, joueur.getWorld().getEntityStore().getStore())
        );
    }
}
