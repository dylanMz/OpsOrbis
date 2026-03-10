package com.experience.game.ui;

import com.experience.game.logic.GameManager;
import com.experience.game.logic.RelicManager;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

/**
 * Scoreboard dynamique utilisant le format .ui natif.
 * Utilise l'API Message d'Hytale pour les couleurs (pas les codes §).
 */
public class ExperienceScoreboard extends CustomUIHud {

    private final GameManager gameManager;

    public ExperienceScoreboard(PlayerRef playerRef, GameManager gameManager) {
        super(playerRef);
        this.gameManager = gameManager;
    }

    @Override
    protected void build(UICommandBuilder builder) {
        // On charge uniquement le template : les set() ne peuvent PAS être appelés ici
        builder.append("scoreboard.ui");
    }

    /**
     * Met à jour les données du scoreboard.
     * Appelé depuis ScoreboardHUD.rafraichirTous() après chaque événement de jeu.
     */
    public void rafraichir() {
        UICommandBuilder builder = new UICommandBuilder();
        injecterDonnees(builder);
        this.update(false, builder);
    }

    private void injecterDonnees(UICommandBuilder builder) {
        RelicManager rm = gameManager.getRelicManager();
        if (rm == null) return;

        // Scores avec couleurs via l'API Message
        builder.set("#ScoreBleu.TextSpans", Message.raw("Bleus: " + rm.getScoreBleu()).color(new Color(0, 200, 255)));
        builder.set("#ScoreRouge.TextSpans", Message.raw("Rouges: " + rm.getScoreRouge()).color(new Color(255, 80, 80)));

        // Statut des reliques — 2 lignes séparées pour éviter le débordement
        builder.set("#RelicB.TextSpans", Message.join(
            Message.raw("B1: ").color(new Color(0, 200, 255)),
            Message.raw(rm.getRelicB1Status()),
            Message.raw("   B2: ").color(new Color(0, 200, 255)),
            Message.raw(rm.getRelicB2Status())
        ));
        builder.set("#RelicR.TextSpans", Message.join(
            Message.raw("R1: ").color(new Color(255, 80, 80)),
            Message.raw(rm.getRelicR1Status()),
            Message.raw("   R2: ").color(new Color(255, 80, 80)),
            Message.raw(rm.getRelicR2Status())
        ));

        // Compteur joueurs
        builder.set("#PlayerCount.TextSpans", Message.raw("Joueurs: " + gameManager.getScoreboardHUD().getActiveCount()).color(Color.LIGHT_GRAY));
    }
}
