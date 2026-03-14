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
 * Gère le chargement et la sauvegarde des configurations JSON.
 */
public class ConfigManager {

    private static final String CONFIG_DIR = "mods/OpsOrbis";
    private static final String GLOBAL_CONFIG_FILE = "GlobalConfig.json";
    private static final String MAP_CONFIG_FILE = "MapConfig.json";
    
    private final Gson gson;
    private GlobalConfig globalConfig;
    private MapConfig mapConfig;

    public ConfigManager() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        load();
    }

    /**
     * Charge toutes les configurations depuis les fichiers JSON.
     */
    public void load() {
        this.globalConfig = loadConfig(GLOBAL_CONFIG_FILE, GlobalConfig.class);
        this.mapConfig = loadConfig(MAP_CONFIG_FILE, MapConfig.class);
    }

    private <T> T loadConfig(String fileName, Class<T> clazz) {
        try {
            Path path = Path.of(CONFIG_DIR, fileName);
            if (Files.exists(path)) {
                String json = Files.readString(path);
                T config = gson.fromJson(json, clazz);
                if (config != null) {
                    HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Config chargée : " + fileName);
                    // Sauvegarder immédiatement pour ajouter les nouveaux champs par défaut
                    saveConfig(fileName, config);
                    return config;
                }
            }
        } catch (IOException e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur chargement " + fileName + " : " + e.getMessage());
        }
        
        try {
            HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Utilisation des valeurs par défaut pour " + fileName);
            T defaultConfig = clazz.getDeclaredConstructor().newInstance();
            saveConfig(fileName, defaultConfig);
            return defaultConfig;
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur création config par défaut : " + e.getMessage());
            return null;
        }
    }

    /**
     * Sauvegarde toutes les configurations.
     */
    public void save() {
        saveConfig(GLOBAL_CONFIG_FILE, globalConfig);
        saveConfig(MAP_CONFIG_FILE, mapConfig);
    }

    private void saveConfig(String fileName, Object config) {
        if (config == null) return;
        try {
            Path dir = Path.of(CONFIG_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path path = dir.resolve(fileName);
            String json = gson.toJson(config);
            Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur sauvegarde " + fileName + " : " + e.getMessage());
        }
    }

    public GlobalConfig getGlobalConfig() { return globalConfig; }
    public MapConfig getMapConfig() { return mapConfig; }
    
    // Pour la compatibilité temporaire si nécessaire
    @Deprecated
    public MapConfig getConfig() { return mapConfig; }
}
