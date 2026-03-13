package com.opsorbis.game.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.opsorbis.utils.HytaleUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gestionnaire de l'état des joueurs permettant la sauvegarde et la restauration 
 * de l'inventaire et de la position. Inclut un système de persistance sur disque 
 * pour prévenir la perte de données en cas de crash serveur.
 */
public class PlayerStateManager {
    
    private final Map<UUID, PlayerState> states = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path recoveryDir = Paths.get("mods", "OpsOrbis", "recovery");

    public PlayerStateManager() {
        try {
            Files.createDirectories(recoveryDir);
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Impossible de créer le dossier de récupération : " + e.getMessage());
        }
    }

    /**
     * Sauvegarde l'état complet d'un joueur (Position et Inventaires).
     * Les données sont stockées en mémoire et sur le disque pour la récupération après crash.
     * 
     * @param joueur Le joueur dont on veut sauvegarder l'état.
     */
    public void saveState(Player joueur) {
        if (joueur == null || joueur.getWorld() == null) return;
        
        TransformComponent transform = joueur.getWorld().getEntityStore().getStore()
            .getComponent(joueur.getReference(), TransformComponent.getComponentType());
        
        if (transform == null || hasSavedState(joueur)) return;

        // Extraction des différents conteneurs d'inventaire
        Map<Short, ItemStack> hotbarItems = extraireConteneur(joueur.getInventory().getHotbar(), 9);
        Map<Short, ItemStack> storageItems = extraireConteneur(joueur.getInventory().getStorage(), 27);
        Map<Short, ItemStack> armorItems = extraireConteneur(joueur.getInventory().getArmor(), 4);
        Map<Short, ItemStack> utilityItems = extraireConteneur(joueur.getInventory().getUtility(), 3);

        PlayerState state = new PlayerState(
            transform.getPosition().clone(),
            transform.getRotation().clone(),
            hotbarItems,
            storageItems,
            armorItems,
            utilityItems
        );

        // Sauvegarde double : Mémoire (rapide) + Disque (sécurité)
        states.put(joueur.getUuid(), state);
        saveToDisk(joueur.getUuid(), state);
    }

    /**
     * Extrait les objets d'un ItemContainer dans une Map.
     */
    private Map<Short, ItemStack> extraireConteneur(ItemContainer container, int taille) {
        Map<Short, ItemStack> items = new HashMap<>();
        if (container == null) return items;
        
        for (short i = 0; i < taille; i++) {
            try {
                ItemStack stack = container.getItemStack(i);
                if (stack != null) items.put(i, stack);
            } catch (Exception ignored) {}
        }
        return items;
    }

    /**
     * Écrit l'état du joueur dans un fichier JSON.
     */
    private void saveToDisk(UUID uuid, PlayerState state) {
        try {
            SerializablePlayerState sState = new SerializablePlayerState();
            sState.x = state.getPosition().getX();
            sState.y = state.getPosition().getY();
            sState.z = state.getPosition().getZ();
            sState.yaw = state.getRotation().getX();
            sState.pitch = state.getRotation().getY();
            sState.roll = state.getRotation().getZ();
            
            sState.hotbar = convertToSerializable(state.getHotbar());
            sState.storage = convertToSerializable(state.getStorage());
            sState.armor = convertToSerializable(state.getArmor());
            sState.utility = convertToSerializable(state.getUtility());

            String json = gson.toJson(sState);
            Files.writeString(recoveryDir.resolve(uuid.toString() + ".json"), json);
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Échec de la sauvegarde sur disque pour " + uuid + " : " + e.getMessage());
        }
    }

    private Map<Short, SerializableItemStack> convertToSerializable(Map<Short, ItemStack> items) {
        Map<Short, SerializableItemStack> result = new HashMap<>();
        for (Map.Entry<Short, ItemStack> entry : items.entrySet()) {
            ItemStack stack = entry.getValue();
            
            result.put(entry.getKey(), new SerializableItemStack(
                stack.getItemId(), 
                stack.getQuantity(),
                stack.getDurability(),
                stack.getMaxDurability(),
                stack.isBroken(),
                stack.isUnbreakable(),
                stack.getBlockKey(),
                stack.getMetadata() != null ? stack.getMetadata().toString() : null
            ));
        }
        return result;
    }

    /**
     * Restaure l'état sauvegardé d'un joueur.
     * Tente d'abord via la mémoire vive, sinon via le fichier de récupération (cas de crash).
     * 
     * @param joueur Le joueur à restaurer.
     */
    public void restoreState(Player joueur) {
        if (joueur == null || joueur.getWorld() == null) return;

        PlayerState state = states.remove(joueur.getUuid());
        
        // Si pas en mémoire (ex: reboot après crash), chargement depuis le disque
        if (state == null) {
            state = loadFromDisk(joueur.getUuid());
        }

        if (state == null) return;

        // Nettoyage immédiat du fichier pour éviter les duplications lors de reconnexions futures
        removeStateFile(joueur.getUuid());

        final PlayerState finalState = state;
        joueur.getWorld().execute(() -> {
            // 1. Restauration Position
            HytaleUtils.teleporterJoueur(joueur, finalState.getPosition());
            
            // 2. Clear inventaire actuel
            joueur.getInventory().clear();
            
            // 3. Restauration des conteneurs
            peuplerConteneur(joueur.getInventory().getHotbar(), finalState.getHotbar());
            peuplerConteneur(joueur.getInventory().getStorage(), finalState.getStorage());
            peuplerConteneur(joueur.getInventory().getArmor(), finalState.getArmor());
            peuplerConteneur(joueur.getInventory().getUtility(), finalState.getUtility());
        });
    }

    /**
     * Remplit un conteneur d'inventaire à partir d'une map d'objets.
     */
    private void peuplerConteneur(ItemContainer container, Map<Short, ItemStack> items) {
        if (container == null || items == null) return;
        for (Map.Entry<Short, ItemStack> entry : items.entrySet()) {
            try {
                container.addItemStackToSlot(entry.getKey(), entry.getValue());
            } catch (Exception ignored) {}
        }
    }

    public boolean hasSavedState(Player joueur) {
        return joueur != null && (states.containsKey(joueur.getUuid()) || Files.exists(recoveryDir.resolve(joueur.getUuid().toString() + ".json")));
    }

    public boolean hasSavedState(UUID uuid) {
        return states.containsKey(uuid) || Files.exists(recoveryDir.resolve(uuid.toString() + ".json"));
    }

    public void removeState(UUID uuid) {
        states.remove(uuid);
        removeStateFile(uuid);
    }

    private void removeStateFile(UUID uuid) {
        try {
            Files.deleteIfExists(recoveryDir.resolve(uuid.toString() + ".json"));
        } catch (Exception ignored) {}
    }

    /**
     * Charge l'état depuis le fichier JSON.
     */
    private PlayerState loadFromDisk(UUID uuid) {
        try {
            Path path = recoveryDir.resolve(uuid.toString() + ".json");
            if (!Files.exists(path)) return null;

            String json = Files.readString(path);
            SerializablePlayerState sState = gson.fromJson(json, SerializablePlayerState.class);
            if (sState == null) return null;

            return new PlayerState(
                new Vector3d(sState.x, sState.y, sState.z),
                new Vector3f(sState.yaw, sState.pitch, sState.roll),
                convertToItems(sState.hotbar),
                convertToItems(sState.storage),
                convertToItems(sState.armor),
                convertToItems(sState.utility)
            );
        } catch (Exception e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Erreur lors du chargement de la récupération : " + e.getMessage());
            return null;
        }
    }

    private Map<Short, ItemStack> convertToItems(Map<Short, SerializableItemStack> sItems) {
        Map<Short, ItemStack> result = new HashMap<>();
        if (sItems == null) return result;
        for (Map.Entry<Short, SerializableItemStack> entry : sItems.entrySet()) {
            SerializableItemStack sStack = entry.getValue();
            
            // Création de la base
            ItemStack stack = new ItemStack(sStack.assetId, sStack.count);
            
            // Restauration via l'API "with..." car l'ItemStack est quasi-immuable
            try {
                // On chaîne les modifications
                stack = stack.withQuantity(sStack.count);
                stack = stack.withDurability(sStack.durability);
                stack = stack.withMaxDurability(sStack.maxDurability);
                
                // Note: isBroken et isUnbreakable semblent être dérivés de la durabilité 
                // ou de la config de l'item, on ne peut pas forcer le setter s'il n'existe pas.
                
                // Pour le metadata, c'est plus complexe (BsonDocument), 
                // on restaure déjà l'essentiel pour le moment.
            } catch (Exception e) {
                HytaleLogger.getLogger().at(Level.WARNING).log("[Ops Orbis] Erreur lors de la restauration d'un item: " + sStack.assetId, e);
            }
            
            result.put(entry.getKey(), stack);
        }
        return result;
    }

    /**
     * Vérifie s'il existe une sauvegarde de récupération pour ce joueur et la restaure.
     */
    public void checkRecovery(Player joueur) {
        if (joueur == null) return;
        Path path = recoveryDir.resolve(joueur.getUuid().toString() + ".json");
        if (Files.exists(path)) {
            HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Recovery trouvé pour " + joueur.getDisplayName() + ". Restauration en cours...");
            restoreState(joueur);
        }
    }
}
