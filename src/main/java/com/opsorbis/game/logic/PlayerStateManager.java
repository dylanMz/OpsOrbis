package com.opsorbis.game.logic;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.opsorbis.utils.HytaleUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gère la sauvegarde et la restauration de l'état des joueurs (position, inventaire).
 */
public class PlayerStateManager {
    private final Map<UUID, PlayerState> states = new HashMap<>();

    /**
     * Sauvegarde l'état actuel d'un joueur.
     */
    public void saveState(Player joueur) {
        TransformComponent transform = joueur.getWorld().getEntityStore().getStore()
            .getComponent(joueur.getReference(), TransformComponent.getComponentType());
        
        if (transform == null || hasSavedState(joueur)) return;

        Map<Short, ItemStack> hotbarItems = new HashMap<>();
        Map<Short, ItemStack> storageItems = new HashMap<>();
        Map<Short, ItemStack> armorItems = new HashMap<>();
        Map<Short, ItemStack> utilityItems = new HashMap<>();
        
        ItemContainer hotbar = joueur.getInventory().getHotbar();
        ItemContainer storage = joueur.getInventory().getStorage();
        ItemContainer armor = joueur.getInventory().getArmor();
        ItemContainer utility = joueur.getInventory().getUtility();

        // Hotbar (9 slots)
        for (short i = 0; i < 9; i++) {
            try {
                ItemStack stack = hotbar.getItemStack(i);
                if (stack != null) hotbarItems.put(i, stack);
            } catch (Exception ignored) {}
        }

        // Storage (27 slots)
        for (short i = 0; i < 27; i++) {
            try {
                ItemStack stack = storage.getItemStack(i);
                if (stack != null) storageItems.put(i, stack);
            } catch (Exception ignored) {}
        }

        // Armor (4 slots)
        for (short i = 0; i < 4; i++) {
            try {
                ItemStack stack = armor.getItemStack(i);
                if (stack != null) armorItems.put(i, stack);
            } catch (Exception ignored) {}
        }

        // Utility (3 slots typically: Offhand, etc.)
        for (short i = 0; i < 3; i++) {
            try {
                ItemStack stack = utility.getItemStack(i);
                if (stack != null) utilityItems.put(i, stack);
            } catch (Exception ignored) {}
        }

        states.put(joueur.getUuid(), new PlayerState(
            transform.getPosition().clone(),
            transform.getRotation().clone(),
            hotbarItems,
            storageItems,
            armorItems,
            utilityItems
        ));
    }

    /**
     * Restaure l'état sauvegardé d'un joueur et supprime la sauvegarde.
     */
    public void restoreState(Player joueur) {
        if (joueur == null || joueur.getWorld() == null) return;

        final PlayerState state = states.remove(joueur.getUuid());
        if (state == null) return;

        joueur.getWorld().execute(() -> {
            // 1. Restaurer la Position
            HytaleUtils.teleporterJoueur(joueur, state.getPosition());
            
            // 2. Vider complètement l'inventaire
            joueur.getInventory().clear();
            
            // 3. Restaurer chaque container précisément
            ItemContainer hotbar = joueur.getInventory().getHotbar();
            ItemContainer storage = joueur.getInventory().getStorage();
            ItemContainer armor = joueur.getInventory().getArmor();
            ItemContainer utility = joueur.getInventory().getUtility();

            // Restauration Hotbar
            for (Map.Entry<Short, ItemStack> entry : state.getHotbar().entrySet()) {
                try {
                    hotbar.addItemStackToSlot(entry.getKey(), entry.getValue());
                } catch (Exception ignored) {}
            }

            // Restauration Storage
            for (Map.Entry<Short, ItemStack> entry : state.getStorage().entrySet()) {
                try {
                    storage.addItemStackToSlot(entry.getKey(), entry.getValue());
                } catch (Exception ignored) {}
            }
            
            // Restauration Armor
            for (Map.Entry<Short, ItemStack> entry : state.getArmor().entrySet()) {
                try {
                    armor.addItemStackToSlot(entry.getKey(), entry.getValue());
                } catch (Exception ignored) {}
            }

            // Restauration Utility
            for (Map.Entry<Short, ItemStack> entry : state.getUtility().entrySet()) {
                try {
                    utility.addItemStackToSlot(entry.getKey(), entry.getValue());
                } catch (Exception ignored) {}
            }
        });
    }

    public boolean hasSavedState(Player joueur) {
        if (joueur == null) return false;
        return states.containsKey(joueur.getUuid());
    }

    public boolean hasSavedState(UUID uuid) {
        return states.containsKey(uuid);
    }

    public void removeState(UUID uuid) {
        states.remove(uuid);
    }
}
