package com.opsorbis.game.logic;

import java.util.UUID;

/**
 * Stocke les statistiques individuelles d'un joueur pour un match.
 */
public class MatchStats {
    private final UUID playerUuid;
    private final String playerName;
    
    private int kills = 0;
    private int deaths = 0;
    private int steals = 0;   // Reliques ramassées
    private int captures = 0; // Reliques déposées en base
    private int returns = 0;  // Reliques ramenées à la base (défenseurs)

    public MatchStats(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
    }

    public void addKill() { kills++; }
    public void addDeath() { deaths++; }
    public void addSteal() { steals++; }
    public void addCapture() { captures++; }
    public void addReturn() { returns++; }

    // Getters
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getSteals() { return steals; }
    public int getCaptures() { return captures; }
    public int getReturns() { return returns; }

    /**
     * Calcule un score de performance pour déterminer le MVP.
     * Formule simple : (Captures * 100) + (Steals * 30) + (Kills * 10) + (Returns * 20) - (Deaths * 5)
     */
    public int getPerformanceScore() {
        return (captures * 100) + (steals * 30) + (kills * 10) + (returns * 20) - (deaths * 5);
    }
}
