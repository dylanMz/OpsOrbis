package com.experience.listeners;

import com.experience.game.logic.GameManager;

/**
 * Ancien écoutéur d'évènements. 
 * Désormais remplacé par les systèmes ECS (RelicPickupSystem, RelicDepositSystem, RelicDeathSystem).
 */
public class GameListener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }
}
