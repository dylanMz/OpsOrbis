package com.opsorbis.game.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;

/**
 * HUD du lobby : affiche la liste des parties disponibles.
 * Pour l'instant, les données sont hardcodées (3 parties fictives).
 */
public class LobbyUI extends CustomUIHud {

    private boolean visible = false;

    // Plus de données hardcodées statiques
    private static final int MAX_JOUEURS = 10;

    public LobbyUI(PlayerRef playerRef) {
        super(playerRef);
    }

    // ── Build : charge le fichier .ui ─────────────────────────────────
    @Override
    protected void build(UICommandBuilder builder) {
        builder.append("lobby.ui");
    }

    // ── Visibilité ────────────────────────────────────────────────────
    public void setVisible(boolean visible) {
        if (this.visible == visible)
            return;
        this.visible = visible;
        rafraichir();
    }

    public boolean isVisible() {
        return visible;
    }

    // ── Rafraîchir les données ────────────────────────────────────────
    public void rafraichir() {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#MainGroup.Visible", visible);

        if (visible) {
            injecterDonnees(builder);
        }

        try {
            this.update(false, builder);
        } catch (Exception ignored) {
        }
    }

    // ── Injection des parties réelles ────────────────────────────
    private void injecterDonnees(UICommandBuilder builder) {
        java.util.List<com.opsorbis.game.logic.MatchInstance> matches = new java.util.ArrayList<>(com.opsorbis.OpsOrbis.get().getGameManager().getToutesLesInstances());
        
        for (int i = 0; i < 3; i++) {
            int n = i + 1; // IDs dans le .ui commencent à 1

            if (i < matches.size()) {
                com.opsorbis.game.logic.MatchInstance inst = matches.get(i);
                String shortId = inst.getMatchId().toString().substring(0, 4);
                
                builder.set("#NomPartie" + n + ".TextSpans", Message.raw("Match " + shortId));
                builder.set("#Joueurs" + n + ".TextSpans", Message.raw(inst.getTeamManager().getNombreTotalJoueurs() + "/" + MAX_JOUEURS));
                builder.set("#Statut" + n + ".TextSpans", Message.raw(traduireEtat(inst.getEtatActuel())));

                // Bouton "Rejoindre" visible uniquement si la partie est en attente
                boolean rejoindreOk = inst.getEtatActuel() == com.opsorbis.game.logic.GameManager.GameState.ATTENTE;
                builder.set("#Rejoindre" + n + ".Visible", rejoindreOk);
            } else {
                builder.set("#NomPartie" + n + ".TextSpans", Message.raw("---"));
                builder.set("#Joueurs" + n + ".TextSpans", Message.raw("0/" + MAX_JOUEURS));
                builder.set("#Statut" + n + ".TextSpans", Message.raw("Disponible"));
                builder.set("#Rejoindre" + n + ".Visible", false);
            }
        }
    }

    private String traduireEtat(com.opsorbis.game.logic.GameManager.GameState etat) {
        switch (etat) {
            case ATTENTE: return "En attente";
            case EN_COURS: return "En cours";
            case TERMINEE: return "Terminé";
            default: return "Inconnu";
        }
    }
}
