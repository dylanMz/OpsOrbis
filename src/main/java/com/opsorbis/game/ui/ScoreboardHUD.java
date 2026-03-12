package com.opsorbis.game.ui;

import com.opsorbis.game.logic.GameManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire du Scoreboard utilisant la visibilité pour le nettoyage.
 * Évite les crashs de retrait CustomUI en ne faisant que masquer l'élément.
 */
public class ScoreboardHUD {

    private final GameManager gameManager;
    private final Map<PlayerRef, OpsOrbisScoreboard> activeHuds = new HashMap<>();

    public ScoreboardHUD(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void afficher(Player joueur) {
        if (joueur == null) return;
        
        PlayerRef ref = joueur.getPlayerRef();
        OpsOrbisScoreboard scoreboard = activeHuds.get(ref);
        
        if (scoreboard == null) {
            scoreboard = new OpsOrbisScoreboard(ref, gameManager);
            if (joueur.getHudManager() != null) {
                joueur.getHudManager().setCustomHud(ref, scoreboard);
                activeHuds.put(ref, scoreboard);
            }
        }
        
        if (scoreboard != null) {
            scoreboard.setVisible(true);
            scoreboard.show();
            scoreboard.rafraichir();
        }
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
     * Masque le scoreboard pour tout le monde via visibilité.
     */
    public void masquerPourTousInscrits(com.hypixel.hytale.server.core.universe.world.World monde) {
        for (OpsOrbisScoreboard hud : activeHuds.values()) {
            hud.setVisible(false);
        }
        // On ne vide pas forcément la map, on laisse les instances prêtes
    }
}
