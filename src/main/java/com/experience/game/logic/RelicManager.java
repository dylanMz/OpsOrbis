package com.experience.game.logic;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
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

/**
 * Gère les 2 reliques placées dans la base des défenseurs.
 * Les attaquants doivent les capturer et les déposer dans leur propre base.
 */
public class RelicManager {
    
    private final World monde;
    private final TeamManager teamManager;

    // Les 2 reliques sont dans la zone des défenseurs
    private Ref<EntityStore> relic1Ref;
    private Ref<EntityStore> relic2Ref;

    // Porteurs des reliques (uniquement des attaquants)
    private Player carrierRelic1;
    private Player carrierRelic2;

    // Compteur de reliques capturées par les attaquants (victoire à 2)
    private int relicsCapturees = 0;

    public RelicManager(World monde, TeamManager teamManager) {
        this.monde = monde;
        this.teamManager = teamManager;
    }

    /**
     * Initialise les 2 reliques dans la base des défenseurs.
     * Utilise les positions RedRelic1 et RedRelic2 de la config.
     */
    public void initRelics() {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Apparition des Reliques (base défenseurs)...");
        
        monde.execute(() -> {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            if (config.getRedRelic1() != null) spawnRelic(store, config.getRedRelic1(), 1);
            if (config.getRedRelic2() != null) spawnRelic(store, config.getRedRelic2(), 2);
        });
    }

    private void spawnRelic(Store<EntityStore> store, Vector3d position, int number) {
        String name = "Bench_Memories";
        Item item = Item.getAssetMap().getAsset(name);
        
        if (item == null) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[ExperienceMod] Relique introuvable: " + name);
            return;
        }
        
        Holder<EntityStore> relicHolder = ItemComponent.generateItemDrop(store, new ItemStack(name, 1), position, Vector3f.ZERO, 0f, 0f, 0f);
        relicHolder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
        relicHolder.ensureComponent(Interactable.getComponentType());
        relicHolder.addComponent(PropComponent.getComponentType(), new PropComponent());

        Ref<EntityStore> ref = store.addEntity(relicHolder, AddReason.SPAWN);
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Relique " + number + " apparue en " + position);
        
        if (number == 1) relic1Ref = ref;
        if (number == 2) relic2Ref = ref;
    }

    /**
     * Tente le ramassage d'une relique par un joueur.
     * Seuls les attaquants peuvent ramasser les reliques.
     */
    public void ramasserRelique(Player joueur, int number, CommandBuffer<EntityStore> buffer) {
        // Seuls les attaquants peuvent voler les reliques
        if (!teamManager.estDansEquipe(joueur, "Attaquant")) {
            joueur.sendMessage(Message.raw("Les défenseurs ne peuvent pas ramasser leurs propres reliques !").color(Color.RED));
            return;
        }

        // Un attaquant ne peut pas porter 2 reliques à la fois
        if (joueur == carrierRelic1 || joueur == carrierRelic2) {
            joueur.sendMessage(Message.raw("Vous portez déjà une relique !").color(Color.RED));
            return;
        }

        // Vérifier si la relique est disponible (pas déjà en cours de portage)
        if (number == 1 && carrierRelic1 != null) {
            joueur.sendMessage(Message.raw("Cette relique est déjà prise !").color(Color.RED));
            return;
        }
        if (number == 2 && carrierRelic2 != null) {
            joueur.sendMessage(Message.raw("Cette relique est déjà prise !").color(Color.RED));
            return;
        }

        // Assigner le porteur
        if (number == 1) carrierRelic1 = joueur;
        if (number == 2) carrierRelic2 = joueur;

        ExperienceMod.get().getGameManager().diffuserMessage(monde,
            Message.join(
                Message.raw("⚠ L'attaquant ").color(Color.ORANGE),
                Message.raw(joueur.getDisplayName()).color(Color.YELLOW),
                Message.raw(" a volé la relique " + number + " !").color(Color.ORANGE)
            )
        );
        
        joueur.sendMessage(Message.raw("Courez à votre base pour capturer la relique !").color(Color.YELLOW));

        // Retirer l'entité du monde
        if (buffer != null) {
            if (number == 1 && relic1Ref != null) { buffer.removeEntity(relic1Ref, RemoveReason.REMOVE); relic1Ref = null; }
            if (number == 2 && relic2Ref != null) { buffer.removeEntity(relic2Ref, RemoveReason.REMOVE); relic2Ref = null; }
        } else {
            Store<EntityStore> store = monde.getEntityStore().getStore();
            if (number == 1 && relic1Ref != null) { store.removeEntity(relic1Ref, RemoveReason.REMOVE); relic1Ref = null; }
            if (number == 2 && relic2Ref != null) { store.removeEntity(relic2Ref, RemoveReason.REMOVE); relic2Ref = null; }
        }

        // Donner la relique à l'inventaire
        Store<EntityStore> sStore = monde.getEntityStore().getStore();
        joueur.giveItem(new ItemStack("Bench_Memories", 1), joueur.getReference(), sStore);

        ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
    }

    /**
     * Vérifie si un attaquant portant une relique est dans la zone de dépôt atacquants.
     * Si oui, incrémente le score et vérifie la victoire.
     */
    public void verifierDepot(Player joueur, CommandBuffer<EntityStore> buffer) {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        
        if (joueur != carrierRelic1 && joueur != carrierRelic2) return;

        TransformComponent transform = joueur.getWorld().getEntityStore().getStore()
            .getComponent(joueur.getReference(), TransformComponent.getComponentType());

        // Zone de dépôt = BlueDepositZone (base attaquants)
        if (config.getBlueDepositZone() != null && config.getBlueDepositZone().containsPosition(
                transform.getPosition().getX(),
                transform.getPosition().getY(),
                transform.getPosition().getZ())) {

            int relicNum = (joueur == carrierRelic1) ? 1 : 2;
            relicsCapturees++;
            joueur.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));

            if (joueur == carrierRelic1) carrierRelic1 = null;
            else carrierRelic2 = null;

            ExperienceMod.get().getGameManager().diffuserMessage(monde,
                Message.join(
                    Message.raw("✓ Relique " + relicNum + " capturée ! ").color(new Color(255, 160, 0)),
                    Message.raw(relicsCapturees + "/2 reliques capturées.").color(Color.WHITE)
                )
            );

            ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
            verifierVictoire(monde, buffer);
        }
    }

    private void verifierVictoire(World monde, CommandBuffer<EntityStore> buffer) {
        if (relicsCapturees >= 2) {
            ExperienceMod.get().getGameManager().terminerPartie(monde, "Attaquants", buffer);
        }
    }

    /**
     * Remet la relique à sa position d'origine si le porteur est tué.
     */
    public void gererMortDuPorteur(Player mort) {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();

        if (mort == carrierRelic1) {
            ExperienceMod.get().getGameManager().diffuserMessage(monde,
                Message.raw("La Relique 1 retourne à la base des défenseurs !").color(Color.YELLOW));
            carrierRelic1 = null;
            monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), config.getRedRelic1(), 1));
        } else if (mort == carrierRelic2) {
            ExperienceMod.get().getGameManager().diffuserMessage(monde,
                Message.raw("La Relique 2 retourne à la base des défenseurs !").color(Color.YELLOW));
            carrierRelic2 = null;
            monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), config.getRedRelic2(), 2));
        }

        mort.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));
        ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
    }

    public void supprimerReliques(CommandBuffer<EntityStore> buffer) {
        Store<EntityStore> store = monde.getEntityStore().getStore();
        if (buffer != null) {
            if (relic1Ref != null) { buffer.removeEntity(relic1Ref, RemoveReason.REMOVE); relic1Ref = null; }
            if (relic2Ref != null) { buffer.removeEntity(relic2Ref, RemoveReason.REMOVE); relic2Ref = null; }
        } else {
            if (relic1Ref != null) { store.removeEntity(relic1Ref, RemoveReason.REMOVE); relic1Ref = null; }
            if (relic2Ref != null) { store.removeEntity(relic2Ref, RemoveReason.REMOVE); relic2Ref = null; }
        }
        carrierRelic1 = null;
        carrierRelic2 = null;
    }

    // Getters pour le scoreboard et les systèmes
    public int getRelicsCapturees() { return relicsCapturees; }
    public Player getCarrierRelic1() { return carrierRelic1; }
    public Player getCarrierRelic2() { return carrierRelic2; }

    // Anciennes API conservées pour compatibilité avec les systèmes existants
    public int getScoreBleu() { return relicsCapturees; }
    public int getScoreRouge() { return 0; }

    public String getRelicB1Status() { return formatStatus(relic1Ref, carrierRelic1); }
    public String getRelicB2Status() { return formatStatus(relic2Ref, carrierRelic2); }
    public String getRelicR1Status() { return "—"; }
    public String getRelicR2Status() { return "—"; }

    private String formatStatus(Ref<EntityStore> ref, Player carrier) {
        if (carrier != null) return "Volée";
        if (ref != null) return "Base";
        return "Capturée";
    }

    public boolean estReliqueDisponible(boolean isBlue, int number) {
        if (number == 1) return relic1Ref != null;
        return relic2Ref != null;
    }
}
