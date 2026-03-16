package com.opsorbis.game.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.opsorbis.utils.HytaleUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Gère les instances de LobbyUI par joueur.
 * Même pattern que ScoreboardHUD : on crée l'instance une seule fois
 * et on bascule la visibilité ensuite.
 */
public class LobbyUIManager {

    private final Map<PlayerRef, LobbyUI> activeUis = new HashMap<>();

    /**
     * Ouvre (ou réaffiche) le lobby pour un joueur.
     */
    public void ouvrir(Player joueur) {
        if (joueur == null) return;

        PlayerRef ref = HytaleUtils.getPlayerRef(joueur);
        LobbyUI ui = activeUis.get(ref);

        if (ui == null) {
            ui = new LobbyUI(ref);
            if (joueur.getHudManager() != null) {
                joueur.getHudManager().setCustomHud(ref, ui);
                activeUis.put(ref, ui);
            }
        }

        if (ui != null) {
            ui.setVisible(true);
            ui.show();
            ui.rafraichir();
        }
    }

    /**
     * Ferme (masque) le lobby pour un joueur.
     */
    public void fermer(Player joueur) {
        if (joueur == null) return;

        PlayerRef ref = HytaleUtils.getPlayerRef(joueur);
        LobbyUI ui = activeUis.get(ref);
        if (ui != null) {
            ui.setVisible(false);
        }
    }
}
