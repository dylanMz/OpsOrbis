package com.opsorbis.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.opsorbis.game.ui.LobbyUIManager;

/**
 * Commande /lobby — Ouvre l'interface de sélection de partie.
 */
public class LobbyCommand extends CommandBase {

    private final LobbyUIManager lobbyUIManager;

    public LobbyCommand(LobbyUIManager lobbyUIManager) {
        super("lobby", "Ouvre la liste des parties disponibles");
        this.lobbyUIManager = lobbyUIManager;
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        if (!ctx.isPlayer()) return;

        Player joueur = (Player) ctx.sender();
        joueur.getWorld().execute(() -> lobbyUIManager.ouvrir(joueur));
    }
}
