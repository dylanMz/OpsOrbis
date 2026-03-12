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
 * Scoreboard HUD simplifié.
 * Utilise la visibilité pour éviter les crashs de retrait CustomUI.
 */
public class OpsOrbisScoreboard extends CustomUIHud {

    private final GameManager gameManager;
    private boolean visible = true;
    private boolean aEteMasque = false;

    public OpsOrbisScoreboard(PlayerRef playerRef, GameManager gameManager) {
        super(playerRef);
        this.gameManager = gameManager;
    }

    @Override
    protected void build(UICommandBuilder builder) {
        builder.append("scoreboard.ui");
    }

    public void setVisible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        if (!visible) this.aEteMasque = false; // Reset pour renvoyer l'ordre de masquage
        rafraichir();
        if (!visible) this.aEteMasque = true;
    }

    public boolean isVisible() {
        return visible;
    }

    public void rafraichir() {
        // Si déjà masqué et qu'on veut rester masqué, on ne fait plus rien pour économiser le réseau
        if (!visible && aEteMasque) return;

        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#MainGroup.Visible", visible);
        
        if (visible) {
            injecterDonnees(builder);
        }
        
        try {
            Player p = gameManager.getTeamManager().getJoueurParRef(this.getPlayerRef());
            if (p != null && p.getReference() != null && p.getReference().isValid()) {
                this.update(false, builder);
            }
        } catch (Exception ignored) {}
        
        if (!visible) aEteMasque = true;
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

        builder.set("#PlayerCount.TextSpans", Message.join(
            Message.raw("Relique 2: ").color(Color.LIGHT_GRAY),
            Message.raw(rm.getRelicB2Status()).color(statusColor(rm.getRelicB2Status()))
        ));
    }

    private Color statusColor(String status) {
        switch (status) {
            case "Base":     return new Color(0, 200, 100);
            case "Terrain":  return new Color(255, 220, 0);
            case "Volée":    return new Color(255, 80, 80);
            case "Capturée": return new Color(255, 160, 0);
            default:         return Color.LIGHT_GRAY;
        }
    }
}
