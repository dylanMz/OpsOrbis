package com.experience.config;

import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Modèle de données pour la configuration du mini-jeu.
 * Contient les zones d'équipe et les points de spawn des PNJ.
 */
public class GameConfig {

    private Box blueZone;
    private Box redZone;
    private Vector3d blueSpawn;
    private Vector3d redSpawn;

    public GameConfig() {
        // Valeurs par défaut pour éviter les NullPointerException
        this.blueZone = new Box(90, 45, 90, 110, 55, 110);
        this.redZone = new Box(-110, 45, -110, -90, 55, -90);
        this.blueSpawn = new Vector3d(105, 50, 105);
        this.redSpawn = new Vector3d(-105, 50, -105);
    }

    // Getters et Setters
    public Box getBlueZone() { return blueZone; }
    public void setBlueZone(Box blueZone) { this.blueZone = blueZone; }

    public Box getRedZone() { return redZone; }
    public void setRedZone(Box redZone) { this.redZone = redZone; }

    public Vector3d getBlueSpawn() { return blueSpawn; }
    public void setBlueSpawn(Vector3d blueSpawn) { this.blueSpawn = blueSpawn; }

    public Vector3d getRedSpawn() { return redSpawn; }
    public void setRedSpawn(Vector3d redSpawn) { this.redSpawn = redSpawn; }

    /**
     * Calcule le centre d'une Box pour le spawn des joueurs.
     */
    public Vector3d getBoxCenter(Box box) {
        if (box == null) return new Vector3d(0, 50, 0);
        return new Vector3d(
            (box.getMin().x + box.getMax().x) / 2.0,
            box.getMin().y, // On spawn au sol de la zone
            (box.getMin().z + box.getMax().z) / 2.0
        );
    }
}
