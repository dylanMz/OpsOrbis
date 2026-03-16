package com.opsorbis.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.opsorbis.utils.TranslationUtils;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Gère le chargement des fichiers de langue et la traduction des messages.
 */
public class LangManager {

    private static final String LANG_DIR = "mods/OpsOrbis/lang";
    private final Map<String, String> translations = new HashMap<>();
    private final Map<String, java.util.function.Supplier<Object>> globalPlaceholders = new HashMap<>();
    private final Gson gson = new Gson();
    private String currentLang;

    public LangManager(String language) {
        this.currentLang = language;
        ensureDirectoryAndFiles();
        load(language);
    }

    /**
     * S'assure que le répertoire lang existe et crée les fichiers par défaut s'ils manquent.
     */
    private void ensureDirectoryAndFiles() {
        try {
            Path dir = Path.of(LANG_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Répertoire lang créé : " + dir.toAbsolutePath());
            }

            // Création des fichiers par défaut s'ils n'existent pas
            ensureFile("fr.json", getDefaultFr());
            ensureFile("en.json", getDefaultEn());

        } catch (IOException e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur lors de l'initialisation du répertoire lang : " + e.getMessage());
        }
    }

    private void ensureFile(String fileName, String content) throws IOException {
        Path path = Path.of(LANG_DIR, fileName);
        if (!Files.exists(path)) {
            Files.writeString(path, content);
            HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Fichier de langue par défaut créé : " + path.toAbsolutePath());
        } else {
            // Fusionner les nouvelles clés avec le fichier existant
            mergeWithDefaults(path, content);
        }
    }

    private void mergeWithDefaults(Path path, String defaultContent) {
        try {
            String existingJson = Files.readString(path);
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> existingMap = gson.fromJson(existingJson, type);
            Map<String, String> defaultMap = gson.fromJson(defaultContent, type);

            if (existingMap == null) existingMap = new HashMap<>();
            
            boolean changed = false;
            for (Map.Entry<String, String> entry : defaultMap.entrySet()) {
                if (!existingMap.containsKey(entry.getKey())) {
                    existingMap.put(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }

            if (changed) {
                String updatedJson = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(existingMap);
                Files.writeString(path, updatedJson);
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Fichier de langue mis à jour avec de nouvelles clés : " + path.getFileName());
            }
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur lors de la fusion du fichier de langue " + path.getFileName() + " : " + e.getMessage());
        }
    }

    private String getDefaultFr() {
        return "{\n" +
                "  \"prefix\": \"&8[&6Ops Orbis&8] \",\n" +
                "  \"match_start_title\": \"&eDémarrage de la Manche {round} !\",\n" +
                "  \"match_start_chat\": \"&e[Phase] Démarrage de la Manche {round} !\",\n" +
                "  \"round_start_chat\": \"&aLa manche commence ! Attaquants, volez les reliques !\",\n" +
                "  \"team_assign_attacker\": \"&fVous rejoignez le camp des &6Attaquants &f! Volez les 2 reliques des défenseurs.\",\n" +
                "  \"team_assign_defender\": \"&fVous rejoignez le camp des &2Défenseurs &f! Protégez vos reliques.\",\n" +
                "  \"kit_selected\": \"&fVous avez sélectionné le kit &6{kit}\",\n" +
                "  \"kit_distributed\": \"&aVotre équipement a été distribué !\",\n" +
                "  \"kit_help_available\": \"&7Kits disponibles : &6{kits}\",\n" +
                "  \"kit_help_command\": \" &7— Utilisez &e/oorbis kit <nom> &7pour choisir.\",\n" +
                "  \"role_selected\": \"&fVous avez choisi le rôle {role}\",\n" +
                "  \"relic_stolen_title\": \"&aRELIQUE RÉCUPÉRÉE\",\n" +
                "  \"relic_stolen_subtitle\": \"&fRapportez-la vite au dépôt !\",\n" +
                "  \"relic_alert_title\": \"&cALERTE : RELIQUE VOLÉE\",\n" +
                "  \"relic_alert_subtitle\": \"&fInterceptez le porteur immédiatement !\",\n" +
                "  \"relic_dropped_chat\": \"&c[Mort] &e{player} &6a été éliminé ! La Relique {number} est au sol — les défenseurs peuvent la récupérer !\",\n" +
                "  \"relic_returned_chat\": \"&2[Défense] Un défenseur a remis la Relique {number} à la base !\",\n" +
                "  \"relic_captured_chat\": \"&6[Attaque] Relique {number} capturée ! &f{count}/2 reliques.\",\n" +
                "  \"relic_carrier_already\": \"&cVous portez déjà une relique !\",\n" +
                "  \"relic_carrier_teammate\": \"&cCette relique est déjà portée par un coéquipier !\",\n" +
                "  \"relic_in_hand\": \"&eRelique {number} en main ! Courez à votre base !\",\n" +
                "  \"relic_picked_up_global\": \"&6[!] &e{player} &6a récupéré la Relique {number} !\",\n" +
                "  \"timer_seconds_left\": \"&eDémarrage dans {seconds} secondes...\",\n" +
                "  \"timer_auto_start\": \"&b[Ops Orbis] Un joueur a rejoint ! Démarrage automatique dans {seconds} s...\",\n" +
                "  \"match_start_cancelled\": \"&c[Ops Orbis] Plus aucun joueur, démarrage annulé.\",\n" +
                "  \"match_stopped_admin\": \"&c=== LA PARTIE A ÉTÉ ARRÊTÉE PAR UN ADMIN ===\",\n" +
                "  \"match_time_up\": \"&c[Temps] LE TEMPS EST ÉCOULÉ !\",\n" +
                "  \"half_time_swap\": \"&b[Changement] MI-TEMPS ! Changement de camps !\",\n" +
                "  \"match_over_winner\": \"&eMatch terminé ! Les &l{team} &agagnent !\",\n" +
                "  \"match_over_header\": \"&6=== LE MATCH EST TERMINÉ ===\\n\",\n" +
                "  \"match_over_scores\": \"&fScores - Eq1: {score1} | Eq2: {score2}\\n\",\n" +
                "  \"match_over_winner_label\": \"&eGagnant : {winner}\",\n" +
                "  \"player_reconnect_success\": \"&aBon retour ! Vous avez rejoint la partie en cours.\",\n" +
                "  \"player_grace_expired\": \"&cDélai de grâce expiré. Vous avez été retiré de la partie.\",\n" +
                "  \"cmd_not_joined\": \"&cVous n'avez pas encore rejoint le match ! Utilisez &e/oorbis join\",\n" +
                "  \"cmd_match_empty\": \"&cImpossible de démarrer : le match est vide.\",\n" +
                "  \"cmd_role_invalid\": \"&cRôle invalide. Utilisation : &e/oorbis role <melee/distance>\",\n" +
                "  \"cmd_kit_invalid\": \"&cKit invalide ou non autorisé pour votre rôle.\",\n" +
                "  \"cmd_kit_usage\": \"&cUtilisation : &e/oorbis kit <nom>\",\n" +
                "  \"cmd_only_player\": \"Cette commande est réservée aux joueurs.\",\n" +
                "  \"cmd_no_permission\": \"&cVous n'avez pas la permission.\",\n" +
                "  \"cmd_unknown\": \"&cCommande inconnue.\",\n" +
                "  \"cmd_usage_main\": \"&cUsage: /oorbis <join|role|kit|start|config>\",\n" +
                "  \"cmd_out_of_bounds\": \"&c[Limite] Vous êtes sorti de la zone de jeu !\",\n" +
                "  \"cmd_config_saved\": \"&aConfiguration sauvegardée !\",\n" +
                "  \"cmd_kit_not_compatible\": \"&cLe kit &6{kit} &cn'est pas disponible pour le rôle &6{role}&c. Changez de rôle avec &e/oorbis role <melee|distance>\",\n" +
                "  \"scoreboard_round\": \"&7Manche: &e{round}/{max_round}\",\n" +
                "  \"scoreboard_time\": \" &7| Tps: {time}\",\n" +
                "  \"scoreboard_team1\": \"&bÉquipe 1: &f{score1}\",\n" +
                "  \"scoreboard_team2\": \" &7| &cÉquipe 2: &f{score2}\",\n" +
                "  \"scoreboard_player_role\": \"&7Camp: &e{camp}\",\n  \"scoreboard_team_label\": \"&5({team})\",\n" +
                "  \"team_1_name\": \"Équipe 1\",\n" +
                "  \"team_2_name\": \"Équipe 2\",\n" +
                "  \"scoreboard_relics\": \"&7Reliques: &6{count}/2 capturées\",\n" +
                "  \"scoreboard_relic_status\": \"&7Relique {number}: {status}\",\n" +
                "  \"relic_status_at_base\": \"Base\",\n" +
                "  \"relic_status_carried\": \"Portée\",\n" +
                "  \"relic_status_dropped\": \"Au sol\",\n" +
                "  \"relic_status_captured\": \"Capturée\",\n" +
                "  \"round_winner_chat\": \"&6[Manche] La manche {round} a été remportée par les &l{camp}&r !\",\n" +
                "  \"round_winner_title\": \"&6&lVICTOIRE DES {camp}\",\n" +
                "  \"round_winner_subtitle\": \"&fManche {round} terminée\",\n" +
                "  \"match_draw\": \"Égalité !\",\n" +
                "  \"camp_attaquant\": \"Attaquants\",\n" +
                "  \"camp_defenseur\": \"Défenseurs\",\n" +
                "  \"camp_spectateur\": \"Spectateur\",\n" +
                "  \"camp_none\": \"Spectateur\",\n" +
                "  \"team_1_attacking\": \"ÉQUIPE 1 : ATTAQUE\",\n" +
                "  \"team_1_defending\": \"ÉQUIPE 1 : DÉFENSE\",\n" +
                "  \"cmd_config_help_header\": \"&6Config — sous-commandes :\",\n" +
                "  \"cmd_config_help_setzone\": \"\\n &f• &esetzone <attaquant|defenseur> &7— Zone de spawn\",\n" +
                "  \"cmd_config_help_setrelic\": \"\\n &f• &esetrelic <1|2> &7— Position relique\",\n" +
                "  \"cmd_config_help_setnpcspawn\": \"\\n &f• &esetnpcspawn <1|2> &7— Spawn PNJ\",\n" +
                "  \"cmd_config_help_setdeposit\": \"\\n &f• &esetdeposit &7— Zone dépôt attaquants\",\n" +
                "  \"cmd_config_help_setgamezone\": \"\\n &f• &esetgamezone &7— Zone de jeu globale\",\n" +
                "  \"cmd_config_help_save\": \"\\n &f• &esave &7— Sauvegarder\",\n" +
                "  \"cmd_config_provider_missing\": \"&cSelectionProvider introuvable.\",\n" +
                "  \"cmd_config_no_selection\": \"&cAucune zone sélectionnée.\",\n" +
                "  \"cmd_config_zone_attacker_success\": \"&a✓ Zone Attaquants définie.\",\n" +
                "  \"cmd_config_zone_defender_success\": \"&a✓ Zone Défenseurs définie.\",\n" +
                "  \"cmd_config_team_invalid\": \"&cÉquipe invalide (attaquant ou defenseur).\",\n" +
                "  \"cmd_config_relic_success\": \"&a✓ Relique {number} positionnée.\",\n" +
                "  \"cmd_config_number_invalid\": \"&cNuméro invalide (1 ou 2).\",\n" +
                "  \"cmd_config_npc_success\": \"&a✓ Spawn PNJ {number} défini.\",\n" +
                "  \"cmd_config_deposit_success\": \"&a✓ Zone de dépôt Attaquants définie.\",\n" +
                "  \"cmd_config_gamezone_success\": \"&a✓ Zone de jeu globale définie.\",\n" +
                "  \"cmd_role_required\": \"&cVous devez d'abord rejoindre la partie avec /oorbis join\",\n" +
                "  \"cmd_reload_success\": \"&a✓ Plugin rechargé avec succès !\",\n" +
                "  \"summary_victory_title\": \"&6&lVICTOIRE DES {camp}\",\n" +
                "  \"summary_mvp_label\": \"&e--- MVP DU MATCH ---\",\n" +
                "  \"summary_mvp_score\": \"&7Score de performance : &b{score}\",\n" +
                "  \"summary_personal_title\": \"&f&nTES STATISTIQUES&r\",\n" +
                "  \"summary_stat_kills_label\": \"Éliminations\",\n" +
                "  \"summary_stat_deaths_label\": \"Morts\",\n" +
                "  \"summary_stat_steals_label\": \"Vols de Reliques\",\n" +
                "  \"summary_stat_captures_label\": \"Captures\",\n" +
                "  \"summary_stat_returns_label\": \"Retours Base\",\n" +
                "  \"summary_performance_label\": \"SCORE TOTAL\",\n" +
                "  \"summary_close\": \"&7[Appuyez sur Échap pour quitter]\"\n" +
                "}";
    }

    private String getDefaultEn() {
        return "{\n" +
                "  \"prefix\": \"&8[&6Ops Orbis&8] \",\n" +
                "  \"match_start_title\": \"&eStarting Round {round}!\",\n" +
                "  \"match_start_chat\": \"&e[Phase] Starting Round {round}!\",\n" +
                "  \"round_start_chat\": \"&aThe round begins! Attackers, steal the relics!\",\n" +
                "  \"team_assign_attacker\": \"&fYou are an &6Attacker&f! Steal the 2 enemy relics.\",\n" +
                "  \"team_assign_defender\": \"&fYou are a &2Defender&f! Protect your relics.\",\n" +
                "  \"kit_selected\": \"&fYou have selected the &6{kit} &fkit.\",\n" +
                "  \"kit_distributed\": \"&aYour equipment has been distributed!\",\n" +
                "  \"kit_help_available\": \"&7Available kits: &6{kits}\",\n" +
                "  \"kit_help_command\": \" &7— Use &e/oorbis kit <name> &7to choose.\",\n" +
                "  \"role_selected\": \"&fYou have chosen the {role} role.\",\n" +
                "  \"relic_stolen_title\": \"&aRELIC RETRIEVED\",\n" +
                "  \"relic_stolen_subtitle\": \"&fBring it back to the deposit zone!\",\n" +
                "  \"relic_alert_title\": \"&cALERT: RELIC STOLEN\",\n" +
                "  \"relic_alert_subtitle\": \"&fIntercept the carrier immediately!\",\n" +
                "  \"relic_dropped_chat\": \"&c[Death] &e{player} &6has been eliminated! Relic {number} is on the ground — defenders can retrieve it!\",\n" +
                "  \"relic_returned_chat\": \"&2[Defense] A defender returned Relic {number} to base!\",\n" +
                "  \"relic_captured_chat\": \"&6[Attack] Relic {number} captured! &f{count}/2 relics.\",\n" +
                "  \"relic_carrier_already\": \"&cYou are already carrying a relic!\",\n" +
                "  \"relic_carrier_teammate\": \"&cThis relic is already being carried by a teammate!\",\n" +
                "  \"relic_in_hand\": \"&eRelic {number} in hand! Run to your base!\",\n" +
                "  \"relic_picked_up_global\": \"&6[!] &e{player} &6picked up Relic {number}!\",\n" +
                "  \"timer_seconds_left\": \"&eStarting in {seconds} seconds...\",\n" +
                "  \"timer_auto_start\": \"&b[Ops Orbis] A player has joined! Automatic start in {seconds} s...\",\n" +
                "  \"match_start_cancelled\": \"&c[Ops Orbis] No more players, start cancelled.\",\n" +
                "  \"match_stopped_admin\": \"&c=== THE GAME HAS BEEN STOPPED BY AN ADMIN ===\",\n" +
                "  \"match_time_up\": \"&c[Time] TIME IS UP!\",\n" +
                "  \"half_time_swap\": \"&b[Change] HALF-TIME! Swapping camps!\",\n" +
                "  \"match_over_winner\": \"&eMatch over! The &l{team} &awin!\",\n" +
                "  \"match_over_header\": \"&6=== GAME OVER ===\\n\",\n" +
                "  \"match_over_scores\": \"&fScores - Eq1: {score1} | Eq2: {score2}\\n\",\n" +
                "  \"match_over_winner_label\": \"&eWinner: {winner}\",\n" +
                "  \"player_reconnect_success\": \"&aWelcome back! You have rejoined the ongoing game.\",\n" +
                "  \"player_grace_expired\": \"&cGrace period expired. You have been removed from the game.\",\n" +
                "  \"cmd_not_joined\": \"&cYou haven't joined the match yet! Use &e/oorbis join\",\n" +
                "  \"cmd_match_empty\": \"&cCannot start: the match is empty.\",\n" +
                "  \"cmd_role_invalid\": \"&cInvalid role. Usage: &e/oorbis role <melee/distance>\",\n" +
                "  \"cmd_kit_invalid\": \"&cInvalid kit or not allowed for your role.\",\n" +
                "  \"cmd_kit_usage\": \"&cUsage: &e/oorbis kit <name>\",\n" +
                "  \"cmd_only_player\": \"This command is for players only.\",\n" +
                "  \"cmd_no_permission\": \"&cYou don't have permission.\",\n" +
                "  \"cmd_unknown\": \"&cUnknown command.\",\n" +
                "  \"cmd_usage_main\": \"&cUsage: /oorbis <join|role|kit|start|config>\",\n" +
                "  \"cmd_out_of_bounds\": \"&c[Boundary] You left the game zone!\",\n" +
                "  \"cmd_config_saved\": \"&aConfiguration saved!\",\n" +
                "  \"cmd_kit_not_compatible\": \"&cThe &6{kit} &ckit is not available for the &6{role} &crole. Change roles with &e/oorbis role <melee/distance>\",\n" +
                "  \"scoreboard_round\": \"&7Round: &e{round}/{max_round}\",\n" +
                "  \"scoreboard_time\": \" &7| Time: {time}\",\n" +
                "  \"scoreboard_team1\": \"&bTeam 1: {score1}\",\n" +
                "  \"scoreboard_team2\": \" &7| &cTeam 2: {score2}\",\n" +
                "  \"scoreboard_relics\": \"&7Relics: &6{count}/2 captured\",\n" +
                "  \"scoreboard_relic_status\": \"&7Relic {number}: {status}\",\n" +
                "  \"relic_status_at_base\": \"At Base\",\n" +
                "  \"relic_status_carried\": \"Carried\",\n" +
                "  \"relic_status_dropped\": \"Dropped\",\n" +
                "  \"relic_status_captured\": \"Captured\",\n" +
                "  \"team_1_name\": \"Team 1\",\n" +
                "  \"team_2_name\": \"Team 2\",\n" +
                "  \"round_winner_chat\": \"&6[Round] Round {round} won by &l{camp}&r!\",\n" +
                "  \"round_winner_title\": \"&6ROUND {round} OVER\",\n" +
                "  \"round_winner_subtitle\": \"&fVictory for &l{camp}\",\n" +
                "  \"match_draw\": \"Draw!\",\n" +
                "  \"scoreboard_player_role\": \"&7Camp: &e{camp}\",\n" +
                "  \"scoreboard_team_label\": \"&5({team})\",\n" +
                "  \"camp_attaquant\": \"Attacker\",\n" +
                "  \"camp_defenseur\": \"Defender\",\n" +
                "  \"camp_spectateur\": \"Spectator\",\n" +
                "  \"camp_none\": \"Spectator\",\n" +
                "  \"team_1_attacking\": \"TEAM 1: ATTACK\",\n" +
                "  \"team_1_defending\": \"TEAM 1: DEFENSE\",\n" +
                "  \"cmd_config_help_header\": \"&6Config — sub-commands:\",\n" +
                "  \"cmd_config_help_setzone\": \"\\n &f• &esetzone <attaquant|defenseur> &7— Spawn Zone\",\n" +
                "  \"cmd_config_help_setrelic\": \"\\n &f• &esetrelic <1|2> &7— Relic Position\",\n" +
                "  \"cmd_config_help_setnpcspawn\": \"\\n &f• &esetnpcspawn <1|2> &7— NPC Spawn\",\n" +
                "  \"cmd_config_help_setdeposit\": \"\\n &f• &esetdeposit &7— Attacker Deposit Zone\",\n" +
                "  \"cmd_config_help_setgamezone\": \"\\n &f• &esetgamezone &7— Global Play Zone\",\n" +
                "  \"cmd_config_help_save\": \"\\n &f• &esave &7— Save Config\",\n" +
                "  \"cmd_config_provider_missing\": \"&cSelectionProvider not found.\",\n" +
                "  \"cmd_config_no_selection\": \"&cNo zone selected.\",\n" +
                "  \"cmd_config_zone_attacker_success\": \"&a✓ Attacker Zone defined.\",\n" +
                "  \"cmd_config_zone_defender_success\": \"&a✓ Defender Zone defined.\",\n" +
                "  \"cmd_config_team_invalid\": \"&cInvalid team (attaquant or defenseur).\",\n" +
                "  \"cmd_config_relic_success\": \"&a✓ Relic {number} positioned.\",\n" +
                "  \"cmd_config_number_invalid\": \"&cInvalid number (1 or 2).\",\n" +
                "  \"cmd_config_npc_success\": \"&a✓ NPC Spawn {number} defined.\",\n" +
                "  \"cmd_config_deposit_success\": \"&a✓ Attacker Deposit Zone defined.\",\n" +
                "  \"cmd_config_gamezone_success\": \"&a✓ Global Play Zone defined.\",\n" +
                "  \"cmd_role_required\": \"&cYou must join the match first with /oorbis join\",\n" +
                "  \"cmd_reload_success\": \"&a✓ Plugin reloaded successfully!\",\n" +
                "  \"summary_victory_title\": \"&6&l{camp} WIN\",\n" +
                "  \"summary_mvp_label\": \"&e--- MATCH MVP ---\",\n" +
                "  \"summary_mvp_score\": \"&7Performance Score: &b{score}\",\n" +
                "  \"summary_personal_title\": \"&f&nYOUR STATISTICS&r\",\n" +
                "  \"summary_stat_kills_label\": \"Kills\",\n" +
                "  \"summary_stat_deaths_label\": \"Deaths\",\n" +
                "  \"summary_stat_steals_label\": \"Relic Steals\",\n" +
                "  \"summary_stat_captures_label\": \"Captures\",\n" +
                "  \"summary_stat_returns_label\": \"Base Returns\",\n" +
                "  \"summary_performance_label\": \"TOTAL SCORE\",\n" +
                "  \"summary_close\": \"&7[Press Escape to quit]\"\n" +
                "}";
    }

    /**
     * Enregistre un placeholder global (ex: {round}) qui sera résolu dynamiquement.
     */
    public void registerGlobal(String key, java.util.function.Supplier<Object> supplier) {
        globalPlaceholders.put(key, supplier);
    }

    /**
     * Charge un fichier de langue (ex: fr.json).
     */
    public void load(String lang) {
        translations.clear();
        Path path = Path.of(LANG_DIR, lang.toLowerCase() + ".json");
        
        try {
            if (!Files.exists(path)) {
                HytaleLogger.getLogger().at(Level.WARNING).log("[Ops Orbis] Fichier de langue introuvable : " + path);
                return;
            }

            String content = Files.readString(path);
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> map = gson.fromJson(content, type);
            if (map != null) {
                translations.putAll(map);
            }
            HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Langue chargée : " + lang + " (" + translations.size() + " clés)");
        } catch (IOException e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur chargement langue : " + e.getMessage());
        }
    }

    /**
     * Retourne la chaîne brute formatée (sans conversion en Message).
     */
    /**
     * Retourne la chaîne brute formatée avec des placeholders nommés {key}.
     * Args doit être une alternance de clé et valeur : getRaw("key", "player", "Dylan", "count", 5)
     */
    public String getRaw(String key, Object... args) {
        String raw = translations.getOrDefault(key, key);
        
        // 1. Appliquer les arguments locaux
        if (args.length > 0) {
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    String placeholder = "{" + args[i].toString() + "}";
                    String value = args[i + 1] == null ? "null" : args[i + 1].toString();
                    raw = raw.replace(placeholder, value);
                }
            }
        }

        // 2. Appliquer les placeholders globaux
        if (!globalPlaceholders.isEmpty()) {
            for (Map.Entry<String, java.util.function.Supplier<Object>> entry : globalPlaceholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                if (raw.contains(placeholder)) {
                    Object val = entry.getValue().get();
                    raw = raw.replace(placeholder, val != null ? val.toString() : "");
                }
            }
        }

        return raw;
    }

    /**
     * Retourne un objet Message traduit avec support des couleurs et placeholders globaux.
     */
    public Message get(String key, Object... args) {
        return TranslationUtils.parse(getRaw(key, args));
    }

    /**
     * Version spécifique à un joueur (placeholer {player} auto-résolu).
     */
    public Message get(com.hypixel.hytale.server.core.entity.entities.Player player, String key, Object... args) {
        String raw = getRaw(key, args);
        if (player != null && raw.contains("{player}")) {
            raw = raw.replace("{player}", player.getDisplayName());
        }
        return TranslationUtils.parse(raw);
    }

    /**
     * Change la langue actuelle.
     */
    public void setLanguage(String lang) {
        if (!lang.equals(currentLang)) {
            this.currentLang = lang;
            load(lang);
        }
    }

    public String getLanguage() {
        return currentLang;
    }
}
