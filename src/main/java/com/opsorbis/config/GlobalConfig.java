package com.opsorbis.config;

/**
 * Configuration globale du jeu (règles, délais, langue).
 */
public class GlobalConfig {

    private String language = "fr";
    private int minPlayersToStart = 2;
    private int maxRounds = 10;
    private int autoStartDelaySeconds = 30;
    private int roundDurationSeconds = 300;
    private int gracePeriodSeconds = 60;
    private boolean autoStartEnabled = true;

    public GlobalConfig() {
    }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getMinPlayersToStart() { return minPlayersToStart; }
    public void setMinPlayersToStart(int minPlayersToStart) { this.minPlayersToStart = minPlayersToStart; }

    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }

    public int getAutoStartDelaySeconds() { return autoStartDelaySeconds; }
    public void setAutoStartDelaySeconds(int autoStartDelaySeconds) { this.autoStartDelaySeconds = autoStartDelaySeconds; }

    public int getRoundDurationSeconds() { return roundDurationSeconds; }
    public void setRoundDurationSeconds(int roundDurationSeconds) { this.roundDurationSeconds = roundDurationSeconds; }

    public int getGracePeriodSeconds() { return gracePeriodSeconds; }
    public void setGracePeriodSeconds(int gracePeriodSeconds) { this.gracePeriodSeconds = gracePeriodSeconds; }
    
    public long getGracePeriodMs() { return gracePeriodSeconds * 1000L; }

    public boolean isAutoStartEnabled() { return autoStartEnabled; }
    public void setAutoStartEnabled(boolean autoStartEnabled) { this.autoStartEnabled = autoStartEnabled; }
}
