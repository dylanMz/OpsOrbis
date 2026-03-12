package com.opsorbis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;

/**
 * Gère le chargement et la sauvegarde de la configuration GameConfig en JSON.
 */
public class ConfigManager {

    private static final String CONFIG_DIR = "mods/OpsOrbis";
    private static final String CONFIG_FILE = "GameConfig.json";
    
    private final Gson gson;
    private GameConfig config;

    public ConfigManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        load();
    }

    /**
     * Charge la configuration depuis le fichier JSON.
     * Si le fichier n'existe pas, crée une configuration par défaut.
     */
    public void load() {
        try {
            Path path = Path.of(CONFIG_DIR, CONFIG_FILE);
            if (Files.exists(path)) {
                String json = Files.readString(path);
                this.config = gson.fromJson(json, GameConfig.class);
                if (this.config == null) {
                    this.config = new GameConfig();
                }
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Configuration chargée avec succès.");
            } else {
                HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Aucune config trouvée, utilisation des valeurs par défaut.");
                this.config = new GameConfig();
                save(); // Créer le fichier par défaut
            }
        } catch (IOException e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur lors du chargement de la config : " + e.getMessage());
            this.config = new GameConfig();
        }
    }

    /**
     * Sauvegarde la configuration actuelle dans le fichier JSON.
     */
    public void save() {
        try {
            Path dir = Path.of(CONFIG_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path path = dir.resolve(CONFIG_FILE);
            String json = gson.toJson(this.config);
            Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Configuration sauvegardée dans " + path.toAbsolutePath());
        } catch (IOException e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur lors de la sauvegarde de la config : " + e.getMessage());
        }
    }

    public GameConfig getConfig() {
        return config;
    }
}
