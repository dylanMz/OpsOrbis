package com.experience.game.ui;

import com.experience.game.logic.GameManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire du Scoreboard pour le mod Experience.
 * Cette classe fait le lien entre GameManager et les instances individuelles de ExperienceScoreboard.
 */
public class ScoreboardHUD {

    private final GameManager gameManager;
    private final Map<PlayerRef, ExperienceScoreboard> activeHuds = new HashMap<>();

    public ScoreboardHUD(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    /**
     * Affiche le scoreboard pour un joueur.
     * @param joueur Le joueur concerné.
     */
    public void afficher(Player joueur) {
        if (joueur == null || activeHuds.containsKey(joueur.getPlayerRef())) return;

        // Création de l'instance réelle du HUD
        ExperienceScoreboard scoreboard = new ExperienceScoreboard(joueur.getPlayerRef(), gameManager);
        
        // Enregistrement via le HudManager d'Hytale
        if (joueur.getHudManager() != null) {
            joueur.getHudManager().setCustomHud(joueur.getPlayerRef(), scoreboard);
            activeHuds.put(joueur.getPlayerRef(), scoreboard);
            
            // On force l'affichage puis on injecte immédiatement les données colorées
            scoreboard.show();
            scoreboard.rafraichir();
        }
    }

    /**
     * Met à jour le scoreboard pour un joueur spécifique.
     * @param joueur Le joueur dont le HUD doit être rafraîchi.
     */
    public void mettreAJour(Player joueur) {
        ExperienceScoreboard hud = activeHuds.get(joueur.getPlayerRef());
        if (hud != null) {
            hud.rafraichir();
        }
    }

    /**
     * Rafraîchit le scoreboard pour tous les joueurs actifs.
     */
    public void rafraichirTous() {
        for (ExperienceScoreboard hud : activeHuds.values()) {
            hud.rafraichir();
        }
    }

    /**
     * Retourne le nombre de scoreboards actuellement affichés.
     */
    public int getActiveCount() {
        return activeHuds.size();
    }

    /**
     * Masque le scoreboard pour un joueur via sa référence.
     * @param playerRef La référence du joueur.
     * @param joueur Le joueur (pour accéder au HudManager).
     */
    public void masquer(PlayerRef playerRef, Player joueur) {
        ExperienceScoreboard hud = activeHuds.remove(playerRef);
        if (hud != null && joueur != null && joueur.getHudManager() != null) {
            try {
                joueur.getHudManager().setCustomHud(playerRef, null);
            } catch (Exception e) {
                // Masquage échoué (joueur déjà déconnecté), on ignore
            }
        }
    }

    /**
     * Masque le scoreboard pour tous les joueurs actifs (fin de partie).
     */
    public void masquerTous(com.hypixel.hytale.server.core.universe.world.World monde) {
        if (monde == null) return;
        
        monde.execute(() -> {
            for (ExperienceScoreboard hud : activeHuds.values()) {
                if (hud != null) {
                    hud.setVisible(false); // Masque le contenu sans supprimer le HUD (évite les crashs)
                }
            }
            activeHuds.clear();
        });
    }

}
