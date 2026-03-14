package com.opsorbis.commands;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.LangManager;
import com.opsorbis.config.MapConfig;
import com.opsorbis.game.logic.GameManager;
import com.opsorbis.game.logic.PlayerRole;
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
 *   /oorbis reload                            — Recharger le plugin (Config & Lang)
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

        LangManager lang = OpsOrbis.get().getLangManager();
        if (args.length < 2) {
            joueur.sendMessage(lang.get(joueur, "cmd_usage_main"));
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
                        joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_not_joined"));
                        return;
                    }
                    handleRoleCommand(joueur, args);
                });
                break;
            case "kit":
                joueur.getWorld().execute(() -> {
                    if (!gameManager.getTeamManager().isJoueurDansMatch(joueur)) {
                        joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_not_joined"));
                        return;
                    }
                    handleKitCommand(joueur, args);
                });
                break;
            case "start":
                if (gameManager.getTeamManager().getNombreTotalJoueurs() == 0) {
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_match_empty"));
                    return;
                }
                joueur.getWorld().execute(() -> gameManager.demarrerMatch(joueur.getWorld()));
                break;
            case "stop":
                if (joueur.hasPermission("experience.admin")) {
                    gameManager.forcerArret(joueur.getWorld());
                } else {
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_no_permission"));
                }
                break;
            case "config":
                handleConfigCommand(joueur, args);
                break;
            case "reload":
                if (joueur.hasPermission("experience.admin")) {
                    OpsOrbis.get().getConfigManager().load();
                    OpsOrbis.get().getLangManager().load(OpsOrbis.get().getLangManager().getLanguage());
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_reload_success"));
                } else {
                    joueur.sendMessage(lang.get(joueur, "cmd_no_permission"));
                }
                break;
            default:
                joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_unknown"));
        }
    }

    private void handleRoleCommand(Player joueur, String[] args) {
        LangManager lang = OpsOrbis.get().getLangManager();
        if (args.length < 3) {
            joueur.sendMessage(lang.get(joueur, "cmd_role_invalid"));
            return;
        }
        try {
            RolesManager.RoleType role = RolesManager.RoleType.valueOf(args[2].toUpperCase());
            gameManager.getRolesManager().choisirRole(joueur, role);
        } catch (IllegalArgumentException e) {
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_role_invalid"));
        }
    }

    private void handleKitCommand(Player joueur, String[] args) {
        LangManager lang = OpsOrbis.get().getLangManager();
        if (args.length < 3) {
            joueur.sendMessage(lang.get(joueur, "cmd_kit_usage"));
            return;
        }
        try {
            KitManager.KitType kit = KitManager.KitType.valueOf(args[2].toUpperCase());

            // Vérifier que le kit est compatible avec le rôle du joueur
            if (!gameManager.getRolesManager().peutUtiliserKit(joueur, kit)) {
                RolesManager.RoleType roleActuel = gameManager.getRolesManager().getRole(joueur);
                joueur.sendMessage(lang.get(joueur, "cmd_kit_invalid", "kit", kit.getNom(), "role", roleActuel.getNom()));
                return;
            }

            gameManager.getKitManager().choisirKit(joueur, kit);
        } catch (IllegalArgumentException e) {
            joueur.sendMessage(lang.get(joueur, "cmd_kit_invalid"));
        }
    }

    private void handleConfigCommand(Player joueur, String[] args) {
        if (!joueur.hasPermission("experience.admin")) {
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_no_permission"));
            return;
        }

        if (args.length < 3) {
            envoyerAideConfig(joueur);
            return;
        }

        MapConfig config = OpsOrbis.get().getConfigManager().getMapConfig();
        switch (args[2].toLowerCase()) {

            // ── Zone de spawn ──────────────────────────────────────────────────
            case "setzone":
                if (args.length < 4) {
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_help_setzone"));
                    return;
                }
                setZoneDepuisSelection(joueur, args[3], config);
                break;

            // ── Position des reliques (2 reliques, côté défenseur) ─────────────
            case "setrelic":
                if (args.length < 4) {
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_help_setrelic"));
                    return;
                }
                setRelicDepuisPosition(joueur, args[3], config);
                break;

            // ── Spawn PNJ (1 ou 2) ──────────────────────────────────────────────
            case "setnpcspawn":
                if (args.length < 4) {
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_help_setnpcspawn"));
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
                joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_saved"));
                break;

            default:
                envoyerAideConfig(joueur);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void envoyerAideConfig(Player joueur) {
        LangManager lang = OpsOrbis.get().getLangManager();
        joueur.sendMessage(Message.join(
            lang.get("cmd_config_help_header"),
            lang.get("cmd_config_help_setzone"),
            lang.get("cmd_config_help_setrelic"),
            lang.get("cmd_config_help_setnpcspawn"),
            lang.get("cmd_config_help_setdeposit"),
            lang.get("cmd_config_help_save")
        ));
    }

    private void setZoneDepuisSelection(Player joueur, String equipe, MapConfig config) {
        SelectionProvider provider = SelectionManager.getSelectionProvider();
        if (provider == null) {
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_provider_missing"));
            return;
        }
        joueur.getWorld().execute(() ->
            provider.computeSelectionCopy(joueur.getReference(), joueur, (selection) -> {
                if (selection == null || !selection.hasSelectionBounds()) {
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_no_selection"));
                    return;
                }
                Box newBox = new Box(selection.getSelectionMin().toVector3d(), selection.getSelectionMax().toVector3d());
                String roleInput = equipe.toUpperCase();
                if (roleInput.equals(PlayerRole.ATTAQUANT.name())) {
                    config.setAttackerZone(newBox);
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_zone_attacker_success"));
                } else if (roleInput.equals(PlayerRole.DEFENSEUR.name())) {
                    config.setDefenderZone(newBox);
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_zone_defender_success"));
                } else {
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_team_invalid"));
                }
            }, joueur.getWorld().getEntityStore().getStore())
        );
    }

    private void setRelicDepuisPosition(Player joueur, String num, MapConfig config) {
        joueur.getWorld().execute(() -> {
            TransformComponent t = joueur.getWorld().getEntityStore().getStore()
                .getComponent(joueur.getReference(), TransformComponent.getComponentType());
            if (t == null) return;
            Vector3d pos = t.getPosition().clone();
            switch (num) {
                case "1":
                    config.setRelic1(pos);
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_relic_success", "number", 1));
                    break;
                case "2":
                    config.setRelic2(pos);
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_relic_success", "number", 2));
                    break;
                default:
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_number_invalid"));
            }
        });
    }

    private void setNpcSpawnDepuisPosition(Player joueur, String num, MapConfig config) {
        joueur.getWorld().execute(() -> {
            TransformComponent t = joueur.getWorld().getEntityStore().getStore()
                .getComponent(joueur.getReference(), TransformComponent.getComponentType());
            if (t == null) return;
            Vector3d pos = t.getPosition().clone();
            switch (num) {
                case "1":
                    config.setNpcSpawn1(pos);
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_npc_success", "number", 1));
                    break;
                case "2":
                    config.setNpcSpawn2(pos);
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_npc_success", "number", 2));
                    break;
                default:
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_number_invalid"));
            }
        });
    }

    private void setDepositDepuisSelection(Player joueur, MapConfig config) {
        SelectionProvider provider = SelectionManager.getSelectionProvider();
        if (provider == null) {
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_provider_missing"));
            return;
        }
        joueur.getWorld().execute(() ->
            provider.computeSelectionCopy(joueur.getReference(), joueur, (selection) -> {
                if (selection == null || !selection.hasSelectionBounds()) {
                    joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_no_selection"));
                    return;
                }
                config.setDepositZone(new Box(selection.getSelectionMin().toVector3d(), selection.getSelectionMax().toVector3d()));
                joueur.sendMessage(OpsOrbis.get().getLangManager().get("cmd_config_deposit_success"));
            }, joueur.getWorld().getEntityStore().getStore())
        );
    }
}
