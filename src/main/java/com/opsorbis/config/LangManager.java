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
        }
    }

    private String getDefaultFr() {
        return "{\n" +
                "  \"prefix\": \"&8[&6Ops Orbis&8] \",\n" +
                "  \"match_start_title\": \"&eDémarrage de la Manche %d !\",\n" +
                "  \"match_start_chat\": \"&e[Phase] Démarrage de la Manche %d !\",\n" +
                "  \"round_start_chat\": \"&aLa manche commence ! Attaquants, volez les reliques !\",\n" +
                "  \"team_assign_attacker\": \"&fVous êtes &6Attaquant &f! Volez les 2 reliques des défenseurs.\",\n" +
                "  \"team_assign_defender\": \"&fVous êtes &2Défenseur &f! Protégez vos reliques.\",\n" +
                "  \"kit_selected\": \"&fVous avez sélectionné le kit &6%s\",\n" +
                "  \"kit_distributed\": \"&aVotre équipement a été distribué !\",\n" +
                "  \"kit_help_available\": \"&7Kits disponibles : &6%s\",\n" +
                "  \"kit_help_command\": \" &7— Utilisez &e/oorbis kit <nom> &7pour choisir.\",\n" +
                "  \"role_selected\": \"&fVous avez choisi le rôle %s\",\n" +
                "  \"relic_stolen_title\": \"&aRELIQUE RÉCUPÉRÉE\",\n" +
                "  \"relic_stolen_subtitle\": \"&fRapportez-la vite au dépôt !\",\n" +
                "  \"relic_alert_title\": \"&cALERTE : RELIQUE VOLÉE\",\n" +
                "  \"relic_alert_subtitle\": \"&fInterceptez le porteur immédiatement !\",\n" +
                "  \"relic_dropped_chat\": \"&c[Mort] &e%s &6a été éliminé ! La Relique %d est au sol — les défenseurs peuvent la récupérer !\",\n" +
                "  \"relic_returned_chat\": \"&2[Défense] Un défenseur a remis la Relique %d à la base !\",\n" +
                "  \"relic_captured_chat\": \"&6[Attaque] Relique %d capturée ! &f%d/2 reliques.\",\n" +
                "  \"relic_carrier_already\": \"&cVous portez déjà une relique !\",\n" +
                "  \"relic_carrier_teammate\": \"&cCette relique est déjà portée par un coéquipier !\",\n" +
                "  \"relic_in_hand\": \"&eRelique %d en main ! Courez à votre base !\",\n" +
                "  \"relic_picked_up_global\": \"&6[!] &e%s &6a récupéré la Relique %d !\",\n" +
                "  \"timer_seconds_left\": \"&eDémarrage dans %d secondes...\",\n" +
                "  \"timer_auto_start\": \"&b[Ops Orbis] Un joueur a rejoint ! Démarrage automatique dans %d s...\",\n" +
                "  \"match_start_cancelled\": \"&c[Ops Orbis] Plus aucun joueur, démarrage annulé.\",\n" +
                "  \"match_stopped_admin\": \"&c=== LA PARTIE A ÉTÉ ARRÊTÉE PAR UN ADMIN ===\",\n" +
                "  \"match_time_up\": \"&c[Temps] LE TEMPS EST ÉCOULÉ !\",\n" +
                "  \"half_time_swap\": \"&b[Changement] MI-TEMPS ! Changement de rôles !\",\n" +
                "  \"match_over_winner\": \"&eMatch terminé ! Les &l%s &agagnent !\",\n" +
                "  \"match_over_header\": \"&6=== LE MATCH EST TERMINÉ ===\\n\",\n" +
                "  \"match_over_scores\": \"&fScores - Eq1: %d | Eq2: %d\\n\",\n" +
                "  \"match_over_winner_label\": \"&eGagnant : %s\",\n" +
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
                "  \"cmd_config_saved\": \"&aConfiguration sauvegardée !\",\n" +
                "  \"cmd_kit_not_compatible\": \"&cLe kit &6%s &cn'est pas disponible pour le rôle &6%s&c. Changez de rôle avec &e/oorbis role <melee|distance>\",\n" +
                "  \"scoreboard_round\": \"&7Manche: &e%d/%d\",\n" +
                "  \"scoreboard_time\": \" &7| Tps: %s\",\n" +
                "  \"scoreboard_team1\": \"&bÉquipe 1: &f%d\",\n" +
                "  \"scoreboard_team2\": \" &7| &cÉquipe 2: &f%d\",\n" +
                "  \"scoreboard_relics\": \"&7Reliques: &6%d/2 capturées\",\n" +
                "  \"scoreboard_relic_status\": \"&7Relique %d: %s\",\n" +
                "  \"relic_status_at_base\": \"&2Base\",\n" +
                "  \"relic_status_carried\": \"&6Portée\",\n" +
                "  \"relic_status_dropped\": \"&eAu sol\",\n" +
                "  \"relic_status_captured\": \"&7Capturée\"\n" +
                "}";
    }

    private String getDefaultEn() {
        return "{\n" +
                "  \"prefix\": \"&8[&6Ops Orbis&8] \",\n" +
                "  \"match_start_title\": \"&eStarting Round %d!\",\n" +
                "  \"match_start_chat\": \"&e[Phase] Starting Round %d!\",\n" +
                "  \"round_start_chat\": \"&aThe round begins! Attackers, steal the relics!\",\n" +
                "  \"team_assign_attacker\": \"&fYou are an &6Attacker&f! Steal the 2 enemy relics.\",\n" +
                "  \"team_assign_defender\": \"&fYou are a &2Defender&f! Protect your relics.\",\n" +
                "  \"kit_selected\": \"&fYou have selected the &6%s &fkit.\",\n" +
                "  \"kit_distributed\": \"&aYour equipment has been distributed!\",\n" +
                "  \"kit_help_available\": \"&7Available kits: &6%s\",\n" +
                "  \"kit_help_command\": \" &7— Use &e/oorbis kit <name> &7to choose.\",\n" +
                "  \"role_selected\": \"&fYou have chosen the %s role.\",\n" +
                "  \"relic_stolen_title\": \"&aRELIC RETRIEVED\",\n" +
                "  \"relic_stolen_subtitle\": \"&fBring it back to the deposit zone!\",\n" +
                "  \"relic_alert_title\": \"&cALERT: RELIC STOLEN\",\n" +
                "  \"relic_alert_subtitle\": \"&fIntercept the carrier immediately!\",\n" +
                "  \"relic_dropped_chat\": \"&c[Death] &e%s &6has been eliminated! Relic %d is on the ground — defenders can retrieve it!\",\n" +
                "  \"relic_returned_chat\": \"&2[Defense] A defender returned Relic %d to base!\",\n" +
                "  \"relic_captured_chat\": \"&6[Attack] Relic %d captured! &f%d/2 relics.\",\n" +
                "  \"relic_carrier_already\": \"&cYou are already carrying a relic!\",\n" +
                "  \"relic_carrier_teammate\": \"&cThis relic is already being carried by a teammate!\",\n" +
                "  \"relic_in_hand\": \"&eRelic %d in hand! Run to your base!\",\n" +
                "  \"relic_picked_up_global\": \"&6[!] &e%s &6picked up Relic %d!\",\n" +
                "  \"timer_seconds_left\": \"&eStarting in %d seconds...\",\n" +
                "  \"timer_auto_start\": \"&b[Ops Orbis] A player has joined! Automatic start in %d s...\",\n" +
                "  \"match_start_cancelled\": \"&c[Ops Orbis] No more players, start cancelled.\",\n" +
                "  \"match_stopped_admin\": \"&c=== THE GAME HAS BEEN STOPPED BY AN ADMIN ===\",\n" +
                "  \"match_time_up\": \"&c[Time] TIME IS UP!\",\n" +
                "  \"half_time_swap\": \"&b[Change] HALF-TIME! Swapping roles!\",\n" +
                "  \"match_over_winner\": \"&eMatch over! The &l%s &awin!\",\n" +
                "  \"match_over_header\": \"&6=== GAME OVER ===\\n\",\n" +
                "  \"match_over_scores\": \"&fScores - Eq1: %d | Eq2: %d\\n\",\n" +
                "  \"match_over_winner_label\": \"&eWinner: %s\",\n" +
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
                "  \"cmd_config_saved\": \"&aConfiguration saved!\",\n" +
                "  \"cmd_kit_not_compatible\": \"&cThe &6%s &ckit is not available for the &6%s &crole. Change roles with &e/oorbis role <melee/distance>\",\n" +
                "  \"scoreboard_round\": \"&7Round: &e%d/%d\",\n" +
                "  \"scoreboard_time\": \" &7| Time: %s\",\n" +
                "  \"scoreboard_team1\": \"&bTeam 1: &f%d\",\n" +
                "  \"scoreboard_team2\": \" &7| &cTeam 2: &f%d\",\n" +
                "  \"scoreboard_relics\": \"&7Relics: &6%d/2 captured\",\n" +
                "  \"scoreboard_relic_status\": \"&7Relic %d: %s\",\n" +
                "  \"relic_status_at_base\": \"&2At Base\",\n" +
                "  \"relic_status_carried\": \"&6Carried\",\n" +
                "  \"relic_status_dropped\": \"&eDropped\",\n" +
                "  \"relic_status_captured\": \"&7Captured\"\n" +
                "}";
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
    public String getRaw(String key, Object... args) {
        String raw = translations.getOrDefault(key, key);
        try {
            if (args.length > 0) {
                raw = String.format(raw, args);
            }
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.WARNING).log("[Ops Orbis] Erreur formatage message brut (" + key + ") : " + e.getMessage());
        }
        return raw;
    }

    /**
     * Retourne un objet Message traduit avec support des couleurs.
     */
    public Message get(String key, Object... args) {
        return TranslationUtils.parse(getRaw(key, args));
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
}
