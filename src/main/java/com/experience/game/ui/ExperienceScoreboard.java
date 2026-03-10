package com.experience.game.ui;

import com.experience.game.logic.GameManager;
import com.experience.game.logic.RelicManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

/**
 * Scoreboard HUD pour le mode Attaquants vs Défenseurs.
 * Affiche les reliques capturées (X/2) et l'état de chaque relique.
 */
public class ExperienceScoreboard extends CustomUIHud {

    private final GameManager gameManager;

    public ExperienceScoreboard(PlayerRef playerRef, GameManager gameManager) {
        super(playerRef);
        this.gameManager = gameManager;
    }

    @Override
    protected void build(UICommandBuilder builder) {
        builder.append("scoreboard.ui");
    }

    /**
     * Met à jour le scoreboard avec les données actuelles.
     * Appelé depuis ScoreboardHUD.rafraichirTous().
     */
    public void rafraichir() {
        UICommandBuilder builder = new UICommandBuilder();
        injecterDonnees(builder);
        this.update(false, builder);
    }

    private void injecterDonnees(UICommandBuilder builder) {
        RelicManager rm = gameManager.getRelicManager();
        if (rm == null) return;

        int capturees = rm.getRelicsCapturees();

        // Score principal : reliques capturées
        builder.set("#ScoreBleu.TextSpans", Message.join(
            Message.raw("Reliques: ").color(Color.WHITE),
            Message.raw(capturees + "/2").color(capturees == 0 ? Color.LIGHT_GRAY : new Color(255, 160, 0))
        ));

        // On masque le score rouge (plus utilisé)
        builder.set("#ScoreRouge.TextSpans", Message.raw(""));

        // Statut Relique 1
        builder.set("#RelicB.TextSpans", Message.join(
            Message.raw("Relique 1: ").color(new Color(255, 160, 0)),
            Message.raw(rm.getRelicB1Status()).color(statusColor(rm.getRelicB1Status()))
        ));

        // Statut Relique 2
        builder.set("#RelicR.TextSpans", Message.join(
            Message.raw("Relique 2: ").color(new Color(255, 160, 0)),
            Message.raw(rm.getRelicB2Status()).color(statusColor(rm.getRelicB2Status()))
        ));

        // Compteur de joueurs
        builder.set("#PlayerCount.TextSpans", Message.raw("Joueurs: " + gameManager.getScoreboardHUD().getActiveCount()).color(Color.LIGHT_GRAY));
    }

    private Color statusColor(String status) {
        switch (status) {
            case "Base":     return new Color(0, 200, 100);  // Vert  = sécurisée
            case "Terrain":  return new Color(255, 220, 0);  // Jaune = lâchée au sol
            case "Volée":    return new Color(255, 80, 80);  // Rouge = portée par un attaquant
            case "Capturée": return new Color(255, 160, 0);  // Orange = perdue
            default:         return Color.LIGHT_GRAY;
        }
    }
}
