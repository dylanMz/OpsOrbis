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

    // ── Données hardcodées ────────────────────────────────────────────
    private static final String[] NOMS = { "Partie 1", "Partie 2", "Partie 3" };
    private static final int[] JOUEURS = { 3, 10, 0 };
    private static final int[] MAX = { 10, 10, 10 };
    private static final String[] STATUTS = { "En attente", "En cours", "En attente" };

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

    // ── Injection des 3 parties hardcodées ────────────────────────────
    private void injecterDonnees(UICommandBuilder builder) {
        for (int i = 0; i < 3; i++) {
            int n = i + 1; // IDs dans le .ui commencent à 1

            builder.set("#NomPartie" + n + ".TextSpans", Message.plain(NOMS[i]));
            builder.set("#Joueurs" + n + ".TextSpans", Message.plain(JOUEURS[i] + "/" + MAX[i]));
            builder.set("#Statut" + n + ".TextSpans", Message.plain(STATUTS[i]));

            // Bouton "Rejoindre" visible uniquement si la partie est en attente
            boolean enAttente = "En attente".equals(STATUTS[i]);
            builder.set("#Rejoindre" + n + ".Visible", enAttente);
        }
    }
}
