package com.opsorbis.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.opsorbis.game.ui.LobbyUI;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Commande /lobby — Ouvre l'interface de sélection de partie (UI Page).
 */
public class LobbyCommand extends AbstractPlayerCommand {

    public LobbyCommand(@Nonnull String name, @Nonnull String description) {
        super(name, description);
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = commandContext.senderAs(Player.class);

        CompletableFuture.runAsync(() -> {
            player.getPageManager().openCustomPage(ref, store, new LobbyUI(playerRef, CustomPageLifetime.CanDismiss));
            playerRef.sendMessage(Message.raw("Lobby ouvert !"));
        }, world);
    }
}
