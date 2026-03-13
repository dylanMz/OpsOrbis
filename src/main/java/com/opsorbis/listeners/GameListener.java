package com.opsorbis.listeners;

import com.opsorbis.game.logic.GameManager;

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
