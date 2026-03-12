package com.opsorbis.game.ui;

import com.opsorbis.game.logic.GameManager;
import com.opsorbis.game.logic.RelicManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

/**
 * Scoreboard HUD pour le mode Attaquants vs Défenseurs.
 * Affiche les reliques capturées (X/2) et l'état de chaque relique.
 */
public class OpsOrbisScoreboard extends CustomUIHud {

    private final GameManager gameManager;
    private boolean visible = true;

    public OpsOrbisScoreboard(PlayerRef playerRef, GameManager gameManager) {
        super(playerRef);
        this.gameManager = gameManager;
    }

    @Override
    protected void build(UICommandBuilder builder) {
        builder.append("scoreboard.ui");
    }

    /**
     * Définit si le scoreboard doit être affiché ou masqué.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        rafraichir();
    }

    /**
     * Met à jour le scoreboard avec les données actuelles.
     * Appelé depuis ScoreboardHUD.rafraichirTous().
     */
    public void rafraichir() {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#MainGroup.Visible", visible);
        if (visible) {
            injecterDonnees(builder);
        }
        
        // Protection contre les erreurs Netty lors de la déconnexion
        try {
            Player p = gameManager.getTeamManager().getJoueurParRef(this.getPlayerRef());
            if (p != null && p.getReference() != null && p.getReference().isValid()) {
                this.update(false, builder);
            }
        } catch (Exception ignored) {}
    }

    private void injecterDonnees(UICommandBuilder builder) {
        RelicManager rm = gameManager.getRelicManager();
        if (rm == null) return;

        int capturees = rm.getRelicsCapturees();

        int round = gameManager.getRoundActuel();
        int eq1Score = gameManager.getTeamManager().getScoreEquipe1();
        int eq2Score = gameManager.getTeamManager().getScoreEquipe2();

        long tempsRestantSec = gameManager.getTempsRestantManche();
        String tempsFormate = String.format("%02d:%02d", tempsRestantSec / 60, tempsRestantSec % 60);

        builder.set("#ScoreBleu.TextSpans", Message.join(
            Message.raw("Manche: ").color(Color.LIGHT_GRAY),
            Message.raw(round + "/" + GameManager.ROUNDS_MAX).color(Color.YELLOW),
            Message.raw(" | Tps: ").color(Color.LIGHT_GRAY),
            Message.raw(tempsFormate).color(tempsRestantSec <= 60 ? Color.RED : Color.YELLOW)
        ));

        builder.set("#ScoreRouge.TextSpans", Message.join(
            Message.raw("Équipe 1: ").color(Color.CYAN),
            Message.raw(String.valueOf(eq1Score)).color(Color.WHITE),
            Message.raw(" | Équipe 2: ").color(Color.RED),
            Message.raw(String.valueOf(eq2Score)).color(Color.WHITE)
        ));

        builder.set("#RelicB.TextSpans", Message.join(
            Message.raw("Reliques: ").color(Color.LIGHT_GRAY),
            Message.raw(capturees + "/2 capturées").color(new Color(255, 160, 0))
        ));

        builder.set("#RelicR.TextSpans", Message.join(
            Message.raw("Relique 1: ").color(Color.LIGHT_GRAY),
            Message.raw(rm.getRelicB1Status()).color(statusColor(rm.getRelicB1Status()))
        ));

        // On remplace l'affichage du nombre de joueurs par la relique 2 pour éviter le dépassement UI
        builder.set("#PlayerCount.TextSpans", Message.join(
            Message.raw("Relique 2: ").color(Color.LIGHT_GRAY),
            Message.raw(rm.getRelicB2Status()).color(statusColor(rm.getRelicB2Status()))
        ));
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
