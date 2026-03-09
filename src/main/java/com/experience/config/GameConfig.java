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
    private Vector3d blueRelic1;
    private Vector3d blueRelic2;
    private Vector3d redRelic1;
    private Vector3d redRelic2;

    private Vector3d blueNpcSpawn;
    private Vector3d redNpcSpawn;

    private Box blueDepositZone;
    private Box redDepositZone;

    public GameConfig() {
        // Valeurs par défaut pour éviter les NullPointerException
        this.blueZone = new Box(90, 45, 90, 110, 55, 110);
        this.redZone = new Box(-110, 45, -110, -90, 55, -90);
        this.blueRelic1 = new Vector3d(100, 50, 105);
        this.blueRelic2 = new Vector3d(110, 50, 105);
        this.redRelic1 = new Vector3d(-100, 50, -105);
        this.redRelic2 = new Vector3d(-110, 50, -105);
        
        this.blueNpcSpawn = new Vector3d(100, 50, 100);
        this.redNpcSpawn = new Vector3d(-100, 50, -100);
        
        this.blueDepositZone = new Box(95, 45, 95, 105, 55, 105);
        this.redDepositZone = new Box(-105, 45, -105, -95, 55, -95);
    }

    // Getters et Setters
    public Box getBlueZone() { return blueZone; }
    public void setBlueZone(Box blueZone) { this.blueZone = blueZone; }

    public Box getRedZone() { return redZone; }
    public void setRedZone(Box redZone) { this.redZone = redZone; }

    public Vector3d getBlueRelic1() { return blueRelic1; }
    public void setBlueRelic1(Vector3d blueRelic1) { this.blueRelic1 = blueRelic1; }

    public Vector3d getBlueRelic2() { return blueRelic2; }
    public void setBlueRelic2(Vector3d blueRelic2) { this.blueRelic2 = blueRelic2; }

    public Vector3d getRedRelic1() { return redRelic1; }
    public void setRedRelic1(Vector3d redRelic1) { this.redRelic1 = redRelic1; }

    public Vector3d getRedRelic2() { return redRelic2; }
    public void setRedRelic2(Vector3d redRelic2) { this.redRelic2 = redRelic2; }

    public Box getBlueDepositZone() { return blueDepositZone; }
    public void setBlueDepositZone(Box blueDepositZone) { this.blueDepositZone = blueDepositZone; }

    public Box getRedDepositZone() { return redDepositZone; }
    public void setRedDepositZone(Box redDepositZone) { this.redDepositZone = redDepositZone; }

    public Vector3d getBlueNpcSpawn() { return blueNpcSpawn; }
    public void setBlueNpcSpawn(Vector3d blueNpcSpawn) { this.blueNpcSpawn = blueNpcSpawn; }

    public Vector3d getRedNpcSpawn() { return redNpcSpawn; }
    public void setRedNpcSpawn(Vector3d redNpcSpawn) { this.redNpcSpawn = redNpcSpawn; }

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
