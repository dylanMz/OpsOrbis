package com.opsorbis.game.logic;

import java.util.Map;

/**
 * Version sérialisable de PlayerState pour le stockage JSON.
 */
public class SerializablePlayerState {
    public double x, y, z;
    public float yaw, pitch, roll;
    public Map<Short, SerializableItemStack> hotbar;
    public Map<Short, SerializableItemStack> storage;
    public Map<Short, SerializableItemStack> armor;
    public Map<Short, SerializableItemStack> utility;

    public SerializablePlayerState() {}
}

class SerializableItemStack {
    public String assetId;
    public int count;
    public double durability;
    public double maxDurability;
    public boolean isBroken;
    public boolean isUnbreakable;
    public String blockKey;
    public String metadata; // Sérialisé en String si possible

    public SerializableItemStack() {}
    public SerializableItemStack(String assetId, int count, double durability, double maxDurability, boolean isBroken, boolean isUnbreakable, String blockKey, String metadata) {
        this.assetId = assetId;
        this.count = count;
        this.durability = durability;
        this.maxDurability = maxDurability;
        this.isBroken = isBroken;
        this.isUnbreakable = isUnbreakable;
        this.blockKey = blockKey;
        this.metadata = metadata;
    }
}
