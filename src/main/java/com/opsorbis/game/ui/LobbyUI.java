package com.opsorbis.game.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Page UI du lobby : affiche la liste des parties disponibles.
 * Utilise InteractiveCustomUIPage (interface native Hytale, pas un HUD).
 * Pour l'instant, les données sont hardcodées (3 parties fictives).
 */
public class LobbyUI extends InteractiveCustomUIPage<LobbyUI.Data> {

    // ── Données hardcodées ────────────────────────────────────────────
    private static final String[] NOMS = { "Partie 1", "Partie 2", "Partie 3" };
    private static final int[] JOUEURS = { 3, 10, 0 };
    private static final int[] MAX = { 10, 10, 10 };
    private static final String[] STATUTS = { "En attente", "En cours", "En attente" };

    public LobbyUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
        super(playerRef, lifetime, Data.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder builder,
                      @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        builder.append("lobby.ui");

        // Injection des 3 parties hardcodées
        for (int i = 0; i < 3; i++) {
            int n = i + 1;

            builder.set("#NomPartie" + n + ".TextSpans", Message.raw(NOMS[i]));
            builder.set("#Joueurs" + n + ".TextSpans", Message.raw(JOUEURS[i] + "/" + MAX[i]));
            builder.set("#Statut" + n + ".TextSpans", Message.raw(STATUTS[i]));

            // "Rejoindre" visible uniquement si la partie est en attente
            boolean enAttente = "En attente".equals(STATUTS[i]);
            builder.set("#Rejoindre" + n + ".Visible", enAttente);
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, Data data) {
        super.handleDataEvent(ref, store, data);
    }

    // Codec minimal (pas d'événements interactifs pour l'instant)
    public static class Data {
        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .build();
    }
}
