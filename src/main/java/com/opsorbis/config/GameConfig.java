package com.opsorbis.config;

import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Configuration du mode Attaquants vs Défenseurs.
 *
 * Zones :
 * - attackerZone    : spawn des attaquants
 * - defenderZone    : spawn des défenseurs (+ base des reliques)
 * - relic1 / relic2 : positions des 2 reliques dans la base défenseurs
 * - depositZone     : zone de dépôt des attaquants (pour capturer les reliques)
 * - npcSpawn1/2     : spawns des 2 PNJs défenseurs
 */
public class GameConfig {

    // Zones de spawn des équipes
    private Box attackerZone;      // Spawn attaquants
    private Box defenderZone;      // Spawn défenseurs

    // Positions des 2 reliques (côté défenseurs)
    private Vector3d relic1;
    private Vector3d relic2;

    // Spawns des 2 PNJs (protègent les défenseurs)
    private Vector3d npcSpawn1;
    private Vector3d npcSpawn2;

    // Zone de dépôt des attaquants (pour capter les reliques)
    private Box depositZone;

    public GameConfig() {
        // Valeurs par défaut
        this.attackerZone  = new Box(90, 45, 90, 110, 55, 110);
        this.defenderZone  = new Box(-110, 45, -110, -90, 55, -90);
        this.relic1        = new Vector3d(-100, 50, -105);
        this.relic2        = new Vector3d(-110, 50, -105);
        this.npcSpawn1     = new Vector3d(-100, 50, -100);
        this.npcSpawn2     = new Vector3d(-100, 50, -100);
        this.depositZone   = new Box(95, 45, 95, 105, 55, 105);
    }

    // ── Zones de spawn ──────────────────────────────────────────────────────

    public Box getAttackerZone() { return attackerZone; }
    public void setAttackerZone(Box attackerZone) { this.attackerZone = attackerZone; }

    public Box getDefenderZone() { return defenderZone; }
    public void setDefenderZone(Box defenderZone) { this.defenderZone = defenderZone; }

    // ── Reliques ─────────────────────────────────────────────────────────────

    public Vector3d getRelic1() { return relic1; }
    public void setRelic1(Vector3d relic1) { this.relic1 = relic1; }

    public Vector3d getRelic2() { return relic2; }
    public void setRelic2(Vector3d relic2) { this.relic2 = relic2; }

    // ── Spawns PNJ ───────────────────────────────────────────────────────────

    public Vector3d getNpcSpawn1() { return npcSpawn1; }
    public void setNpcSpawn1(Vector3d npcSpawn1) { this.npcSpawn1 = npcSpawn1; }

    public Vector3d getNpcSpawn2() { return npcSpawn2; }
    public void setNpcSpawn2(Vector3d npcSpawn2) { this.npcSpawn2 = npcSpawn2; }

    // ── Zone de dépôt attaquants ─────────────────────────────────────────────

    public Box getDepositZone() { return depositZone; }
    public void setDepositZone(Box depositZone) { this.depositZone = depositZone; }

    // ── Alias pour la compatibilité avec l'ancien code ───────────────────────

    public Box getBlueZone() { return attackerZone; }
    public Box getRedZone() { return defenderZone; }
    public Vector3d getRedRelic1() { return relic1; }
    public Vector3d getRedRelic2() { return relic2; }
    public Vector3d getBlueRelic1() { return null; } // Supprimé
    public Vector3d getBlueRelic2() { return null; } // Supprimé
    public Vector3d getBlueNpcSpawn() { return npcSpawn1; }
    public Vector3d getRedNpcSpawn() { return npcSpawn2; }
    public Box getBlueDepositZone() { return depositZone; }
    public Box getRedDepositZone() { return null; } // Supprimé

    /**
     * Calcule le centre d'une Box pour le spawn des joueurs.
     */
    public Vector3d getBoxCenter(Box box) {
        if (box == null) return new Vector3d(0, 50, 0);
        return new Vector3d(
            (box.getMin().x + box.getMax().x) / 2.0,
            box.getMin().y,
            (box.getMin().z + box.getMax().z) / 2.0
        );
    }
}
