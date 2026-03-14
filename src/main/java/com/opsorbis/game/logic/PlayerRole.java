package com.opsorbis.game.logic;

import java.awt.Color;

/**
 * Enumération des rôles de joueurs pour type-safe management.
 */
public enum PlayerRole {
    ATTAQUANT("role_attaquant", new Color(255, 160, 0)),
    DEFENSEUR("role_defenseur", new Color(0, 200, 100)),
    AUCUN("role_none", Color.GRAY);

    private final String translationKey;
    private final Color color;

    PlayerRole(String translationKey, Color color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Color getColor() {
        return color;
    }
}
