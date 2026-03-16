package com.opsorbis.game.ui;

import com.opsorbis.game.logic.MatchInstance;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.opsorbis.utils.HytaleUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire du Scoreboard utilisant la visibilité pour le nettoyage.
 * Évite les crashs de retrait CustomUI en ne faisant que masquer l'élément.
 */
public class ScoreboardHUD {

    private final MatchInstance matchInstance;
    private final Map<PlayerRef, OpsOrbisScoreboard> activeHuds = new HashMap<>();

    public ScoreboardHUD(MatchInstance matchInstance) {
        this.matchInstance = matchInstance;
    }

    public void afficher(Player joueur) {
        if (joueur == null) return;
        
        PlayerRef ref = HytaleUtils.getPlayerRef(joueur);
        OpsOrbisScoreboard scoreboard = activeHuds.get(ref);
        
        if (scoreboard == null) {
            scoreboard = new OpsOrbisScoreboard(ref, matchInstance);
            if (joueur.getHudManager() != null) {
                com.buuz135.mhud.MultipleHUD.getInstance().setCustomHud(joueur, ref, "OpsOrbisScoreboard", scoreboard);
                activeHuds.put(ref, scoreboard);
            }
        }

        scoreboard.setVisible(true);
        scoreboard.show();
        scoreboard.rafraichir();
    }

    public void rafraichirTous() {
        for (OpsOrbisScoreboard hud : activeHuds.values()) {
            if (hud.isVisible()) {
                hud.rafraichir();
            }
        }
    }

    public void masquer(PlayerRef playerRef, Player joueur) {
        OpsOrbisScoreboard hud = activeHuds.get(playerRef);
        if (hud != null) {
            hud.setVisible(false);
            // On le garde dans la map pour pouvoir le réafficher facilement
            // mais on le considère "inactif" pour la logique de rafraîchissement
        }
    }

    /**
     * Masque le scoreboard pour tous les participants.
     */
    public void masquerPourTousParticipants() {
        for (OpsOrbisScoreboard hud : activeHuds.values()) {
            hud.setVisible(false);
        }
    }
}
