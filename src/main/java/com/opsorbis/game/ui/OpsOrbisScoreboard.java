package com.opsorbis.game.ui;

import com.opsorbis.OpsOrbis;
import com.opsorbis.game.logic.GameManager;
import com.opsorbis.game.logic.RelicManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;

/**
 * Scoreboard HUD simplifi&eacute;.
 * Utilise la visibilit&eacute; pour &eacute;viter les crashs de retrait CustomUI.
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
            if (p != null && p.getWorld() != null && p.getReference() != null && p.getReference().isValid()) {
                p.getWorld().execute(() -> this.update(false, builder));
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
        String tempsCouleur = (tempsRestantSec <= 30) ? "&c" : "&e";

        builder.set("#ScoreBleu.TextSpans", Message.join(
            OpsOrbis.get().getLangManager().get("scoreboard_round"),
            OpsOrbis.get().getLangManager().get("scoreboard_time", "time", tempsCouleur + tempsFormate)
        ));

        builder.set("#ScoreRouge.TextSpans", Message.join(
            OpsOrbis.get().getLangManager().get("scoreboard_team1"),
            OpsOrbis.get().getLangManager().get("scoreboard_team2")
        ));

        builder.set("#RelicB.TextSpans", OpsOrbis.get().getLangManager().get("scoreboard_relics", "count", capturees));

        builder.set("#RelicR.TextSpans", OpsOrbis.get().getLangManager().get("scoreboard_relic_status", "number", 1, "status", rm.getRelicB1Status()));

        builder.set("#PlayerCount.TextSpans", OpsOrbis.get().getLangManager().get("scoreboard_relic_status", "number", 2, "status", rm.getRelicB2Status()));

        // Injection du camp et de l'équipe
        Player p = gameManager.getTeamManager().getJoueurParRef(this.getPlayerRef());
        if (p != null) {
            String campTrad = OpsOrbis.get().getLangManager().getRaw(gameManager.getTeamManager().getCamp(p).getLangKey());
            String teamTrad = gameManager.getTeamManager().getEquipeAttaquants().contains(p) || gameManager.getTeamManager().getEquipeDefenseurs().contains(p) 
                ? (gameManager.getTeamManager().getEquipeAttaquants().contains(p) == gameManager.getTeamManager().isEquipe1Attaquant() ? OpsOrbis.get().getLangManager().getRaw("team_1_name") : OpsOrbis.get().getLangManager().getRaw("team_2_name"))
                : OpsOrbis.get().getLangManager().getRaw("camp_spectateur");
            
            builder.set("#PlayerCamp.TextSpans", OpsOrbis.get().getLangManager().get("scoreboard_player_role", "camp", campTrad));
            builder.set("#TeamName.TextSpans", OpsOrbis.get().getLangManager().get("scoreboard_team_label", "team", teamTrad));
        }
    }
}
