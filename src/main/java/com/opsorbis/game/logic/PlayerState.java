package com.opsorbis.game.logic;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import java.util.Map;

/**
 * Stocke l'état d'un joueur avant qu'il ne rejoigne une partie.
 */
public class PlayerState {
    private final Vector3d position;
    private final Vector3f rotation;
    private final Map<Short, ItemStack> hotbar;
    private final Map<Short, ItemStack> storage;
    private final Map<Short, ItemStack> armor;
    private final Map<Short, ItemStack> utility;

    public PlayerState(Vector3d position, Vector3f rotation, 
                      Map<Short, ItemStack> hotbar, 
                      Map<Short, ItemStack> storage, 
                      Map<Short, ItemStack> armor,
                      Map<Short, ItemStack> utility) {
        this.position = position;
        this.rotation = rotation;
        this.hotbar = hotbar;
        this.storage = storage;
        this.armor = armor;
        this.utility = utility;
    }

    public Vector3d getPosition() { return position; }
    public Vector3f getRotation() { return rotation; }
    public Map<Short, ItemStack> getHotbar() { return hotbar; }
    public Map<Short, ItemStack> getStorage() { return storage; }
    public Map<Short, ItemStack> getArmor() { return armor; }
    public Map<Short, ItemStack> getUtility() { return utility; }
}
