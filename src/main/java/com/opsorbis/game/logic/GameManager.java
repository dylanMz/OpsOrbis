package com.opsorbis.game.logic;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.MapConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import java.util.Optional;

/**
 * Gestionnaire global des parties (Matchs).
 * C'est l'orchestrateur central : il crée, stocke et gère le cycle de vie 
 * de plusieurs MatchInstance s'exécutant simultanément.
 */
public class GameManager {

    /** États possibles d'une partie. */
    public enum GameState {
        ATTENTE, DEMARRAGE, EN_COURS, PAUSE, TERMINEE
    }

    private final OpsOrbis plugin;
    
    /** Map liant un UUID de match à son instance. */
    private final Map<UUID, MatchInstance> instances = new HashMap<>();
    
    /** Map liant un UUID de joueur à l'ID du match qu'il a rejoint. */
    private final Map<UUID, UUID> playerToMatch = new HashMap<>();

    public GameManager(OpsOrbis plugin) {
        this.plugin = plugin;
    }

    /**
     * Crée une nouvelle instance de match.
     * @param monde Le monde Hytale où le match aura lieu.
     * @param config La configuration de carte à utiliser.
     * @return L'instance MatchInstance créée.
     */
    public MatchInstance creerMatch(World monde, MapConfig config) {
        MatchInstance match = new MatchInstance(plugin, monde, config);
        instances.put(match.getMatchId(), match);
        HytaleLogger.getLogger().at(Level.INFO).log("[GameManager] Nouveau match créé : " + match.getMatchId());
        return match;
    }

    /**
     * Supprime une instance de match proprement.
     * @param match L'instance à supprimer.
     */
    public void supprimerInstance(MatchInstance match) {
        if (match == null) return;
        instances.remove(match.getMatchId());
        // On retire l'association joueur-match pour tous les participants
        playerToMatch.values().removeIf(id -> id.equals(match.getMatchId()));
        HytaleLogger.getLogger().at(Level.INFO).log("[GameManager] Match supprimé : " + match.getMatchId());
    }

    public MatchInstance getMatch(UUID matchId) {
        return instances.get(matchId);
    }

    public MatchInstance getMatchParJoueur(Player joueur) {
        if (joueur == null) return null;
        UUID uuid = com.opsorbis.utils.HytaleUtils.getPlayerUuid(joueur);
        UUID matchId = playerToMatch.get(uuid);
        return matchId != null ? instances.get(matchId) : null;
    }
    
    public MatchInstance getMatchParJoueurRef(PlayerRef ref) {
        if (ref == null) return null;
        UUID matchId = playerToMatch.get(ref.getUuid());
        return matchId != null ? instances.get(matchId) : null;
    }

    public void assignerJoueurAMatch(Player joueur, MatchInstance match) {
        UUID uuid = com.opsorbis.utils.HytaleUtils.getPlayerUuid(joueur);
        playerToMatch.put(uuid, match.getMatchId());
    }
 
    public void retirerJoueurDeMatch(Player joueur) {
        UUID uuid = com.opsorbis.utils.HytaleUtils.getPlayerUuid(joueur);
        playerToMatch.remove(uuid);
    }

    public void tickSeconde(World monde) {
        // Seuls les matches dans ce monde reçoivent le tick
        for (MatchInstance match : instances.values()) {
            if (match.getWorld().equals(monde)) {
                match.tickSeconde();
            }
        }
    }

    /**
     * Utile pour les annonces globales à une instance.
     */
    public void diffuserAnnonceInstance(MatchInstance match, Message titre, Message sousTitre) {
        if (match == null) return;
        for (Player p : match.getTeamManager().getTousLesJoueurs()) {
            match.diffuserAnnonceJoueur(p, titre, sousTitre);
        }
    }

    public Collection<MatchInstance> getToutesLesInstances() {
        return instances.values();
    }

    /**
     * Tente de trouver un match pour un joueur qui se reconnecte.
     */
    public void gererReconnexion(Player joueur) {
        MatchInstance match = getMatchParJoueur(joueur);
        if (match != null) {
            match.gererReconnexion(joueur, System.currentTimeMillis());
        }
    }

    // Méthodes de compatibilité temporaire si nécessaire
    public TeamManager getTeamManager() {
        // Retourne le premier match trouvé ou null (pour éviter les crashs pendant la transition)
        return instances.values().stream()
                .map(MatchInstance::getTeamManager)
                .findFirst()
                .orElse(null);
    }
}
