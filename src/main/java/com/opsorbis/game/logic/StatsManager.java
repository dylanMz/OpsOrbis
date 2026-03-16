package com.opsorbis.game.logic;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.opsorbis.utils.HytaleUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.Optional;

/**
 * Gère la collection des statistiques pour tous les participants.
 */
public class StatsManager {
    private final Map<UUID, MatchStats> playerStats = new HashMap<>();

    public void reset() {
        playerStats.clear();
    }

    private MatchStats getOrCreateStats(Player joueur) {
        if (joueur == null) return null;
        UUID uuid = HytaleUtils.getPlayerUuid(joueur);
        return playerStats.computeIfAbsent(uuid, k -> new MatchStats(uuid, joueur.getDisplayName()));
    }

    public void incrementKill(Player tueur) {
        MatchStats s = getOrCreateStats(tueur);
        if (s != null) s.addKill();
    }

    public void incrementDeath(Player mort) {
        MatchStats s = getOrCreateStats(mort);
        if (s != null) s.addDeath();
    }

    public void incrementSteal(Player joueur) {
        MatchStats s = getOrCreateStats(joueur);
        if (s != null) s.addSteal();
    }

    public void incrementCapture(Player joueur) {
        MatchStats s = getOrCreateStats(joueur);
        if (s != null) s.addCapture();
    }

    public void incrementReturn(Player joueur) {
        MatchStats s = getOrCreateStats(joueur);
        if (s != null) s.addReturn();
    }

    public MatchStats getStats(Player joueur) {
        return playerStats.get(HytaleUtils.getPlayerUuid(joueur));
    }

    public Optional<MatchStats> getMVP() {
        return playerStats.values().stream()
                .max(Comparator.comparingInt(MatchStats::getPerformanceScore));
    }

    public Map<UUID, MatchStats> getAllStats() {
        return playerStats;
    }
}
