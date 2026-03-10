package com.experience.game.logic;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;
import java.util.logging.Level;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.system.tick.ArchetypeTickingSystem;
import it.unimi.dsi.fastutil.Pair;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.entity.UUIDComponent;

public class RelicManager {
    
    private final World monde;
    private final TeamManager teamManager;
    
    private Ref<EntityStore> blueRelic1Ref;
    private Ref<EntityStore> blueRelic2Ref;
    private Ref<EntityStore> redRelic1Ref;
    private Ref<EntityStore> redRelic2Ref;
    
    // Suivi de qui porte quelle relique
    private Player carrierBlueRelic1;
    private Player carrierBlueRelic2;
    private Player carrierRedRelic1;
    private Player carrierRedRelic2;

    // Scores
    private int scoreBleu = 0;
    private int scoreRouge = 0;

    public RelicManager(World monde, TeamManager teamManager) {
        this.monde = monde;
        this.teamManager = teamManager;
    }

    /**
     * Initialise les 4 reliques (2 par équipe) aux positions configurées.
     */
    public void initRelics() {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Apparition des Reliques...");
        
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            
            if (config.getBlueRelic1() != null) spawnRelic(store, config.getBlueRelic1(), true, 1);
            if (config.getBlueRelic2() != null) spawnRelic(store, config.getBlueRelic2(), true, 2);
            if (config.getRedRelic1() != null) spawnRelic(store, config.getRedRelic1(), false, 1);
            if (config.getRedRelic2() != null) spawnRelic(store, config.getRedRelic2(), false, 2);
        });
    }

    private void spawnRelic(Store<EntityStore> store, Vector3d position, boolean isBlue, int number) {
        String name = "Bench_Memories";
        Item item = Item.getAssetMap().getAsset(name);
        
        if (item == null) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[ExperienceMod] Impossible de faire spawner la relique. L'ID 'Bench_Memories' est introuvable.");
            return;
        }
        
        // Spawn via ItemComponent natif d'Hytale
        Holder<EntityStore> relicHolder = ItemComponent.generateItemDrop(store, new ItemStack(name, 1), position, Vector3f.ZERO, 0f, 0f, 0f);

        // On empêche les joueurs de le ramasser dans leur inventaire
        relicHolder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);

        // Interaction (Clic gauche/droite pour le mode de jeu)
        relicHolder.ensureComponent(Interactable.getComponentType());
        
        // PropComponent pour affichage Client et persistance
        relicHolder.addComponent(PropComponent.getComponentType(), new PropComponent());

        Ref<EntityStore> ref = store.addEntity(relicHolder, AddReason.SPAWN);
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Entité relique (" + name + ") apparue en " + position.toString());
        
        // Stocker la référence pour pouvoir la supprimer au ramassage
        if (isBlue && number == 1) blueRelic1Ref = ref;
        if (isBlue && number == 2) blueRelic2Ref = ref;
        if (!isBlue && number == 1) redRelic1Ref = ref;
        if (!isBlue && number == 2) redRelic2Ref = ref;
    }

    /**
     * Gère le ramassage d'une relique par un joueur.
     * Vérifie les règles (vol ennemi uniquement, max 1 par équipe).
     * @param joueur Le joueur qui tente le ramassage.
     * @param isBlue Si la relique visée est bleue.
     * @param number Numéro de la relique (1 ou 2).
     * @param buffer Buffer ECS pour la modification d'entités.
     */
    public void ramasserRelique(Player joueur, boolean isBlue, int number, CommandBuffer<EntityStore> buffer) {
        boolean teamDuJoueurEstBleue = teamManager.estDansEquipe(joueur, "Bleue");

        // Règle: On ne ramasse que la relique adverse
        if (teamDuJoueurEstBleue && isBlue) {
            joueur.sendMessage(Message.raw("Vous ne pouvez pas voler votre propre relique !").color(Color.RED));
            return;
        }
        if (!teamDuJoueurEstBleue && !isBlue) {
            joueur.sendMessage(Message.raw("Vous ne pouvez pas voler votre propre relique !").color(Color.RED));
            return;
        }

        // Règle: Une seule relique portée par équipe
        if (teamDuJoueurEstBleue) {
            if (carrierRedRelic1 != null || carrierRedRelic2 != null) {
                joueur.sendMessage(Message.raw("Votre équipe porte déjà une relique !").color(Color.RED));
                return;
            }
            if (!isBlue && number == 1) carrierRedRelic1 = joueur;
            if (!isBlue && number == 2) carrierRedRelic2 = joueur;
        } else {
            if (carrierBlueRelic1 != null || carrierBlueRelic2 != null) {
                joueur.sendMessage(Message.raw("Votre équipe porte déjà une relique !").color(Color.RED));
                return;
            }
            if (isBlue && number == 1) carrierBlueRelic1 = joueur;
            if (isBlue && number == 2) carrierBlueRelic2 = joueur;
        }

        ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.raw("Le joueur " + joueur.getDisplayName() + " a volé une relique !").color(Color.ORANGE));
        
        joueur.sendMessage(Message.raw("Vous avez la relique ! Courez à la base pour marquer le point !").color(Color.YELLOW));

        // Faire disparaître l'entité du monde
        if (buffer != null) {
            if (isBlue && number == 1 && blueRelic1Ref != null) { buffer.removeEntity(blueRelic1Ref, RemoveReason.REMOVE); blueRelic1Ref = null; }
            if (isBlue && number == 2 && blueRelic2Ref != null) { buffer.removeEntity(blueRelic2Ref, RemoveReason.REMOVE); blueRelic2Ref = null; }
            if (!isBlue && number == 1 && redRelic1Ref != null) { buffer.removeEntity(redRelic1Ref, RemoveReason.REMOVE); redRelic1Ref = null; }
            if (!isBlue && number == 2 && redRelic2Ref != null) { buffer.removeEntity(redRelic2Ref, RemoveReason.REMOVE); redRelic2Ref = null; }
        } else {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            if (isBlue && number == 1 && blueRelic1Ref != null) { store.removeEntity(blueRelic1Ref, RemoveReason.REMOVE); blueRelic1Ref = null; }
            if (isBlue && number == 2 && blueRelic2Ref != null) { store.removeEntity(blueRelic2Ref, RemoveReason.REMOVE); blueRelic2Ref = null; }
            if (!isBlue && number == 1 && redRelic1Ref != null) { store.removeEntity(redRelic1Ref, RemoveReason.REMOVE); redRelic1Ref = null; }
            if (!isBlue && number == 2 && redRelic2Ref != null) { store.removeEntity(redRelic2Ref, RemoveReason.REMOVE); redRelic2Ref = null; }
        }

        // Mettre la relique dans l'inventaire du joueur
        Store<EntityStore> sStore = monde.getEntityStore().getStore();
        joueur.giveItem(new ItemStack("Bench_Memories", 1), joueur.getReference(), sStore);

        // Mise à jour du scoreboard pour tout le monde
        ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
    }

    /**
     * Vérifie si le joueur se trouve dans sa zone de dépôt avec une relique adverse.
     * @param joueur Le joueur à vérifier.
     * @param buffer Buffer ECS.
     */
    public void verifierDepot(Player joueur, CommandBuffer<EntityStore> buffer) {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();

        if (joueur == carrierRedRelic1 || joueur == carrierRedRelic2) {
            // L'équipe bleue a ramené une relique rouge
            TransformComponent transform = joueur.getWorld().getEntityStore().getStore().getComponent(joueur.getReference(), TransformComponent.getComponentType());
            if (config.getBlueDepositZone() != null && config.getBlueDepositZone().containsPosition(transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ())) {
                scoreBleu++;
                joueur.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));
                
                ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.raw("L'équipe Bleue a marqué un point ! Score: " + scoreBleu + " - " + scoreRouge).color(Color.CYAN));
                
                // Ne pas faire réapparaître la relique capturée (elle est définitivement volée)

                if (joueur == carrierRedRelic1) carrierRedRelic1 = null;
                else carrierRedRelic2 = null;

                // Mise à jour du scoreboard suite au point marqué
                ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();

                verifierVictoire(monde, buffer);
            }
        }
        else if (joueur == carrierBlueRelic1 || joueur == carrierBlueRelic2) {
            // L'équipe rouge a ramené une relique bleue
            TransformComponent transform = joueur.getWorld().getEntityStore().getStore().getComponent(joueur.getReference(), TransformComponent.getComponentType());
            if (config.getRedDepositZone() != null && config.getRedDepositZone().containsPosition(transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ())) {
                scoreRouge++;
                
                // Retirer de l'inventaire
                joueur.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));

                ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.raw("L'équipe Rouge a marqué un point ! Score: " + scoreBleu + " - " + scoreRouge).color(Color.RED));
                
                // Ne pas faire réapparaître la relique capturée

                if (joueur == carrierBlueRelic1) carrierBlueRelic1 = null;
                else carrierBlueRelic2 = null;

                // Mise à jour du scoreboard suite au point marqué
                ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();

                verifierVictoire(monde, buffer);
            }
        }
    }

    private void verifierVictoire(World monde, CommandBuffer<EntityStore> buffer) {
        if (scoreBleu >= 2) {
            ExperienceMod.get().getGameManager().terminerPartie(monde, "Bleue", buffer);
        } else if (scoreRouge >= 2) {
            ExperienceMod.get().getGameManager().terminerPartie(monde, "Rouge", buffer);
        }
    }

    /**
     * Gère le retour d'une relique à sa base lorsqu'un porteur meurt.
     * @param mort Le joueur décédé.
     */
    public void gererMortDuPorteur(Player mort) {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();

        if (mort == carrierBlueRelic1) {
            ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.raw("La Relique Bleue 1 retourne à sa base !").color(Color.YELLOW));
            carrierBlueRelic1 = null;
            monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), config.getBlueRelic1(), true, 1));
        }
        else if (mort == carrierBlueRelic2) {
            ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.raw("La Relique Bleue 2 retourne à sa base !").color(Color.YELLOW));
            carrierBlueRelic2 = null;
            monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), config.getBlueRelic2(), true, 2));
        }
        else if (mort == carrierRedRelic1) {
            ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.raw("La Relique Rouge 1 retourne à sa base !").color(Color.YELLOW));
            carrierRedRelic1 = null;
            monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), config.getRedRelic1(), false, 1));
        }
        else if (mort == carrierRedRelic2) {
            ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.raw("La Relique Rouge 2 retourne à sa base !").color(Color.YELLOW));
            carrierRedRelic2 = null;
            monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), config.getRedRelic2(), false, 2));
        }
        
        // Mise à jour du scoreboard car une relique est retournée à la base
        ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
        
        // Retirer la relique de l'inventaire du joueur mort
        mort.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));
    }

    public void supprimerReliques(CommandBuffer<EntityStore> buffer) {
        Store<EntityStore> store = monde.getEntityStore().getStore();
        if (buffer != null) {
            if (blueRelic1Ref != null) { buffer.removeEntity(blueRelic1Ref, RemoveReason.REMOVE); blueRelic1Ref = null; }
            if (blueRelic2Ref != null) { buffer.removeEntity(blueRelic2Ref, RemoveReason.REMOVE); blueRelic2Ref = null; }
            if (redRelic1Ref != null) { buffer.removeEntity(redRelic1Ref, RemoveReason.REMOVE); redRelic1Ref = null; }
            if (redRelic2Ref != null) { buffer.removeEntity(redRelic2Ref, RemoveReason.REMOVE); redRelic2Ref = null; }
        } else {
            if (blueRelic1Ref != null) { store.removeEntity(blueRelic1Ref, RemoveReason.REMOVE); blueRelic1Ref = null; }
            if (blueRelic2Ref != null) { store.removeEntity(blueRelic2Ref, RemoveReason.REMOVE); blueRelic2Ref = null; }
            if (redRelic1Ref != null) { store.removeEntity(redRelic1Ref, RemoveReason.REMOVE); redRelic1Ref = null; }
            if (redRelic2Ref != null) { store.removeEntity(redRelic2Ref, RemoveReason.REMOVE); redRelic2Ref = null; }
        }

        // Réinitialiser les porteurs
        carrierBlueRelic1 = null;
        carrierBlueRelic2 = null;
        carrierRedRelic1 = null;
        carrierRedRelic2 = null;
    }

    public Player getCarrierBlueRelic1() { return carrierBlueRelic1; }
    public Player getCarrierBlueRelic2() { return carrierBlueRelic2; }
    public Player getCarrierRedRelic1() { return carrierRedRelic1; }
    public Player getCarrierRedRelic2() { return carrierRedRelic2; }

    public int getScoreBleu() { return scoreBleu; }
    public int getScoreRouge() { return scoreRouge; }

    /**
     * Retourne une chaîne formatée représentant l'état des 4 reliques pour le scoreboard.
     * @return État formaté avec des codes couleurs Hytale.
     */
    public String getRelicB1Status() { return formatStatus(blueRelic1Ref, carrierBlueRelic1); }
    public String getRelicB2Status() { return formatStatus(blueRelic2Ref, carrierBlueRelic2); }
    public String getRelicR1Status() { return formatStatus(redRelic1Ref, carrierRedRelic1); }
    public String getRelicR2Status() { return formatStatus(redRelic2Ref, carrierRedRelic2); }

    private String formatStatus(Ref<EntityStore> ref, Player carrier) {
        if (carrier != null) return "Portée";
        if (ref != null) return "Base";
        return "Prise";
    }

    public boolean estReliqueDisponible(boolean isBlue, int number) {
        if (isBlue) {
            return (number == 1) ? blueRelic1Ref != null : blueRelic2Ref != null;
        } else {
            return (number == 1) ? redRelic1Ref != null : redRelic2Ref != null;
        }
    }
}
