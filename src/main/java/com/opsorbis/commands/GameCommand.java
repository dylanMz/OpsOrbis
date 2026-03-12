package com.opsorbis.commands;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.GameConfig;
import com.opsorbis.game.logic.GameManager;
import com.opsorbis.kits.KitManager;
import com.opsorbis.roles.RolesManager;
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
 * Commande principale : /oorbis
 *
 * Sous-commandes disponibles :
 *   /oorbis join                              — Rejoindre une équipe
 *   /oorbis kit <guerrier|archer|mage>        — Choisir un kit
 *   /oorbis start                             — Démarrer la partie
 *   /oorbis config setzone <attaquant|defenseur>
 *   /oorbis config setrelic <1|2>
 *   /oorbis config setnpcspawn <1|2>
 *   /oorbis config setdeposit
 *   /oorbis config save
 */
public class GameCommand extends CommandBase {

    private final GameManager gameManager;

    public GameCommand(GameManager gameManager) {
        super("oorbis", "Commandes du mini-jeu Experience 5v5");
        this.setAllowsExtraArguments(true);
        this.gameManager = gameManager;
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        if (!ctx.isPlayer()) return;

        Player joueur = (Player) ctx.sender();
        String[] args = ctx.getInputString().trim().split("\\s+");

        if (args.length < 2) {
            joueur.sendMessage(Message.raw("Usage: /oorbis <join|role|kit|start|config>").color(Color.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "join":
                joueur.getWorld().execute(() -> {
                    // Sauvegarder l'état du joueur s'il n'est pas déjà dans une équipe
                    if (!gameManager.getPlayerStateManager().hasSavedState(joueur)) {
                        gameManager.getPlayerStateManager().saveState(joueur);
                    }
                    // On vide l'inventaire immédiatement après la sauvegarde
                    joueur.getInventory().clear();
                    
                    gameManager.getTeamManager().ajouterJoueur(joueur);
                });
                break;
            case "role":
                joueur.getWorld().execute(() -> {
                    if (!gameManager.getTeamManager().isJoueurDansMatch(joueur)) {
                        joueur.sendMessage(Message.raw("Vous devez d'abord rejoindre la partie avec /oorbis join").color(Color.RED));
                        return;
                    }
                    handleRoleCommand(joueur, args);
                });
                break;
            case "kit":
                joueur.getWorld().execute(() -> {
                    if (!gameManager.getTeamManager().isJoueurDansMatch(joueur)) {
                        joueur.sendMessage(Message.raw("Vous devez d'abord rejoindre la partie avec /oorbis join").color(Color.RED));
                        return;
                    }
                    handleKitCommand(joueur, args);
                });
                break;
            case "start":
                if (gameManager.getTeamManager().getNombreTotalJoueurs() == 0) {
                    joueur.sendMessage(Message.raw("Impossible de démarrer : il n'y a aucun joueur dans la partie !").color(Color.RED));
                    return;
                }
                joueur.getWorld().execute(() -> gameManager.demarrerMatch(joueur.getWorld()));
                break;
            case "stop":
                if (joueur.hasPermission("experience.admin")) {
                    gameManager.forcerArret(joueur.getWorld());
                } else {
                    joueur.sendMessage(Message.raw("Vous n'avez pas la permission.").color(Color.RED));
                }
                break;
            case "config":
                handleConfigCommand(joueur, args);
                break;
            default:
                joueur.sendMessage(Message.raw("Commande inconnue.").color(Color.RED));
        }
    }

    private void handleRoleCommand(Player joueur, String[] args) {
        if (args.length < 3) {
            joueur.sendMessage(Message.raw("Usage: /oorbis role <melee|distance>").color(Color.RED));
            return;
        }
        try {
            RolesManager.RoleType role = RolesManager.RoleType.valueOf(args[2].toUpperCase());
            gameManager.getRolesManager().choisirRole(joueur, role);
        } catch (IllegalArgumentException e) {
            joueur.sendMessage(Message.raw("Rôle invalide. Choisissez : melee ou distance").color(Color.RED));
        }
    }

    private void handleKitCommand(Player joueur, String[] args) {
        if (args.length < 3) {
            joueur.sendMessage(Message.raw("Usage: /oorbis kit <guerrier|assassin|archer|arbaletrier>").color(Color.RED));
            return;
        }
        try {
            KitManager.KitType kit = KitManager.KitType.valueOf(args[2].toUpperCase());

            // Vérifier que le kit est compatible avec le rôle du joueur
            if (!gameManager.getRolesManager().peutUtiliserKit(joueur, kit)) {
                RolesManager.RoleType roleActuel = gameManager.getRolesManager().getRole(joueur);
                joueur.sendMessage(Message.join(
                    Message.raw("Le kit ").color(Color.RED),
                    Message.raw(kit.getNom()).color(Color.ORANGE),
                    Message.raw(" n'est pas disponible pour le rôle ").color(Color.RED),
                    Message.raw(roleActuel.getNom()).color(Color.ORANGE),
                    Message.raw(". Changez de rôle avec ").color(Color.RED),
                    Message.raw("/oorbis role <melee|distance>").color(Color.YELLOW)
                ));
                return;
            }

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

        GameConfig config = OpsOrbis.get().getConfigManager().getConfig();
        switch (args[2].toLowerCase()) {

            // ── Zone de spawn ──────────────────────────────────────────────────
            case "setzone":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /oorbis config setzone <attaquant|defenseur>").color(Color.RED));
                    return;
                }
                setZoneDepuisSelection(joueur, args[3], config);
                break;

            // ── Position des reliques (2 reliques, côté défenseur) ─────────────
            case "setrelic":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /oorbis config setrelic <1|2>").color(Color.RED));
                    return;
                }
                setRelicDepuisPosition(joueur, args[3], config);
                break;

            // ── Spawn PNJ (1 ou 2) ──────────────────────────────────────────────
            case "setnpcspawn":
                if (args.length < 4) {
                    joueur.sendMessage(Message.raw("Usage: /oorbis config setnpcspawn <1|2>").color(Color.RED));
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
                OpsOrbis.get().getConfigManager().save();
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
