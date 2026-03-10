package com.experience.utils;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.logger.HytaleLogger;
import java.util.logging.Level;

/**
 * Utilitaires globaux pour simplifier les interactions avec l'API Hytale.
 */
public class HytaleUtils {

    /**
     * Diffuse un message à tous les joueurs du monde.
     * @param monde Le monde actuel.
     * @param message Le message à diffuser.
     */
    public static void diffuserMessage(World monde, Message message) {
        if (monde == null) return;
        // Utilisation de monde.execute pour garantir la sécurité des threads Hytale
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            // Création d'un archetype pour cibler uniquement les joueurs
            Query<EntityStore> playerQuery = Archetype.of(Player.getComponentType());
            
            // Parcours de chaque "chunk" d'entités chargés
            store.forEachChunk(playerQuery, (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Player p = chunk.getComponent(i, Player.getComponentType());
                    if (p != null) {
                        p.sendMessage(message);
                    }
                }
            });
        });
        
        String texte = message.getRawText();
        if (texte == null || texte.trim().isEmpty()) {
            texte = "Message composé (voir en jeu)";
        }
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] " + texte);
    }

    /**
     * Téléporte un joueur à une position précise avec une rotation par défaut.
     * @param joueur Le joueur à téléporter.
     * @param position La destination.
     */
    public static void teleporterJoueur(Player joueur, Vector3d position) {
        if (joueur == null || position == null) return;
        World monde = joueur.getWorld();
        if (monde != null) {
            // Ajout du composant Teleport via le thread de simulation
            monde.execute(() -> {
                monde.getEntityStore().getStore().addComponent(
                    joueur.getReference(), 
                    Teleport.getComponentType(), 
                    // Création de l'objet Teleport (destination + rotation nulle)
                    Teleport.createForPlayer(position, new Vector3f(0, 0, 0))
                );
            });
        }
    }
}
