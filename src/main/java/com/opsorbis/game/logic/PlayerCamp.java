package com.opsorbis.game.logic;

import java.awt.Color;

/**
 * Enumération des camps de joueurs (Attaquants, Défenseurs, Spectateurs).
 * Distinct des "Rôles" de kits (Mêlée, Distance).
 */
public enum PlayerCamp {
    ATTAQUANT("camp_attaquant", new Color(255, 160, 0)),
    DEFENSEUR("camp_defenseur", new Color(0, 200, 100)),
    AUCUN("camp_none", Color.GRAY);

    private final String langKey;
    private final Color color;

    PlayerCamp(String langKey, Color color) {
        this.langKey = langKey;
        this.color = color;
    }

    public String getLangKey() {
        return langKey;
    }

}
