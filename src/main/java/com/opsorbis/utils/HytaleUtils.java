package com.opsorbis.utils;

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
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import java.util.logging.Level;
import java.awt.Color;
import java.util.function.Predicate;

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
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            Query<EntityStore> playerQuery = Archetype.of(Player.getComponentType());
            
            store.forEachChunk(playerQuery, (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Player p = chunk.getComponent(i, Player.getComponentType());
                    if (p != null && p.getReference() != null && p.getReference().isValid()) {
                        try {
                            p.sendMessage(message);
                        } catch (Exception ignored) {}
                    }
                }
            });
        });
        
        String texte = message.getRawText();
        if (texte == null || texte.trim().isEmpty()) {
            texte = "Message composé (voir en jeu)";
        }
        HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] " + texte);
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
            monde.execute(() -> {
                monde.getEntityStore().getStore().addComponent(
                    joueur.getReference(), 
                    Teleport.getComponentType(), 
                    Teleport.createForPlayer(position, new Vector3f(0, 0, 0))
                );
            });
        }
    }

    /**
     * Diffuse une annonce visuelle au centre de l'écran à tous les joueurs.
     */
    public static void diffuserAnnonce(World monde, Message titre, Message sousTitre) {
        diffuserAnnonceFiltree(monde, p -> true, titre, sousTitre);
    }

    /**
     * Diffuse une annonce visuelle au centre de l'écran avec un filtre sur les joueurs.
     */
    /**
     * Diffuse une annonce visuelle au centre de l'écran avec des durées personnalisées.
     */
    public static void diffuserAnnonceFiltree(World monde, Predicate<Player> filtre, Message titre, Message sousTitre, float dureeMaintien, float dureeApparition, float dureeDisparition) {
        if (monde == null) return;
        
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            Query<EntityStore> playerQuery = Archetype.of(Player.getComponentType());
            
            store.forEachChunk(playerQuery, (chunk, buffer) -> {
                for (int i = 0; i < chunk.size(); i++) {
                    Player p = chunk.getComponent(i, Player.getComponentType());
                    if (p != null && p.getReference() != null && p.getReference().isValid() && filtre.test(p)) {
                        try {
                            EventTitleUtil.showEventTitleToPlayer(
                                p.getPlayerRef(),
                                titre, 
                                sousTitre != null ? sousTitre : Message.raw(""), 
                                true, // Major title
                                "title.show", // ID par défaut
                                dureeMaintien,
                                dureeApparition,
                                dureeDisparition
                            );
                        } catch (Exception ignored) {}
                    }
                }
            });
        });
    }

    /**
     * Diffuse une annonce visuelle au centre de l'écran avec un filtre sur les joueurs.
     */
    public static void diffuserAnnonceFiltree(World monde, Predicate<Player> filtre, Message titre, Message sousTitre) {
        // Durées par défaut (Stay=3s, In=1s, Out=1s)
        diffuserAnnonceFiltree(monde, filtre, titre, sousTitre, 3.0f, 1.0f, 1.0f);
    }

    /**
     * Vide l'inventaire d'un joueur (utile lors de l'arrêt forcé).
     * @param joueur Le joueur concerné.
     */
    public static void nettoyerInventaireJeu(Player joueur) {
        if (joueur == null) return;
        joueur.getWorld().execute(() -> {
            joueur.getInventory().clear();
            // On s'assure aussi de vider l'armure si clear() ne le fait pas sur ce conteneur spécifique
            if (joueur.getInventory().getArmor() != null) {
                joueur.getInventory().getArmor().clear();
            }
        });
    }
}
