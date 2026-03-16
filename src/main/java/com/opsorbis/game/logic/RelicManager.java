package com.opsorbis.game.logic;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.MapConfig;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.opsorbis.utils.HytaleUtils;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.awt.*;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gère la logique des reliques pour un match spécifique.
 * Responsabilités :
 * - Apparition des reliques au début de la manche.
 * - Gestion du ramassage par les attaquants (vol) ou les défenseurs (retour).
 * - Vérification du dépôt dans la zone de capture.
 * - Gestion de la mort des porteurs (drop au sol).
 */
public class RelicManager {
 
    /** Instance du match à laquelle appartient ce manager. */
    private final MatchInstance match;
    
    /** Monde Hytale de l'instance. */
    private final World monde;
    
    /** Manager d'équipe de l'instance pour vérifier les camps (attaquant/défenseur). */
    private final TeamManager teamManager;
 
    // Références ECS des entités reliques représentées dans le monde
    private Ref<EntityStore> relic1Ref;
    private Ref<EntityStore> relic2Ref;
 
    /** Positions de spawn initiales (bases défenseurs). */
    private Vector3d relic1OriginalPos;
    private Vector3d relic2OriginalPos;
 
    /** Positions actuelles des reliques si elles sont lâchées au sol. */
    private Vector3d relic1DroppedPos;
    private Vector3d relic2DroppedPos;
 
    /** Indicateurs de capture finale (déposées en base attaquants). */
    private boolean relic1Capturee;
    private boolean relic2Capturee;
 
    /** UUIDs des joueurs portant actuellement les reliques (null si au sol ou à la base). */
    private java.util.UUID carrier1Uuid;
    private java.util.UUID carrier2Uuid;
 
    /** Nombre total de reliques capturées dans cette manche. */
    private int relicsCapturees = 0;
 
    /**
     * @param match L'instance de match parente.
     */
    public RelicManager(MatchInstance match) {
        this.match = match;
        this.monde = match.getWorld();
        this.teamManager = match.getTeamManager();
    }

    public void initRelics() {
        monde.execute(() -> {
            initRelics_Direct(monde.getEntityStore().getStore());
        });
    }

    /**
     * Initialise les reliques directement (doit être appelé sur le thread monde).
     */
    public void initRelics_Direct(Store<EntityStore> store) {
        MapConfig config = OpsOrbis.get().getConfigManager().getMapConfig();
        relic1OriginalPos = config.getRelic1();
        relic2OriginalPos = config.getRelic2();
        
        spawnRelics(store);
    }

    public void spawnRelics(Store<EntityStore> store) {
        if (monde == null) return;
        HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Apparition des Reliques...");
        
        // Reset des porteurs
        carrier1Uuid = null;
        carrier2Uuid = null;
        relic1DroppedPos = null;
        relic2DroppedPos = null;
        relic1Capturee = false;
        relic2Capturee = false;
        relicsCapturees = 0;
        HytaleLogger.getLogger().at(Level.INFO).log("[Ops Orbis] Apparition des Reliques...");
        
        if (relic1OriginalPos != null) spawnRelic(store, relic1OriginalPos, 1);
        if (relic2OriginalPos != null) spawnRelic(store, relic2OriginalPos, 2);
    }

    private void spawnRelic(Store<EntityStore> stockage, Vector3d position, int numero) {
        String nom = "Bench_Memories";
        if (Item.getAssetMap().getAsset(nom) == null) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[Ops Orbis] Relique introuvable: " + nom);
            return;
        }
        Holder<EntityStore> entite = ItemComponent.generateItemDrop(stockage, new ItemStack(nom, 1), position, Vector3f.ZERO, 0f, 0f, 0f);
        assert entite != null;
        entite.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
        entite.ensureComponent(Interactable.getComponentType());
        entite.addComponent(PropComponent.getComponentType(), new PropComponent());
        Ref<EntityStore> reference = stockage.addEntity(entite, AddReason.SPAWN);
        // Note : on ne touche PAS à relic1DroppedPos ici.
        // C'est la responsabilité de chaque appelant de gérer l'état de la relique.
        if (numero == 1) relic1Ref = reference;
        if (numero == 2) relic2Ref = reference;
    }

    private void supprimerEntiteRelique(int numero, CommandBuffer<EntityStore> buffer) {
        Store<EntityStore> stockage = monde.getEntityStore().getStore();
        if (numero == 1 && relic1Ref != null) {
            if (relic1Ref.isValid()) {
                if (buffer != null) buffer.removeEntity(relic1Ref, RemoveReason.REMOVE);
                else stockage.removeEntity(relic1Ref, RemoveReason.REMOVE);
            }
            relic1Ref = null;
        }
        if (numero == 2 && relic2Ref != null) {
            if (relic2Ref.isValid()) {
                if (buffer != null) buffer.removeEntity(relic2Ref, RemoveReason.REMOVE);
                else stockage.removeEntity(relic2Ref, RemoveReason.REMOVE);
            }
            relic2Ref = null;
        }
    }

    // ─── Ramassage ────────────────────────────────────────────────────────────

    /**
     * Tente le ramassage d'une relique par un joueur.
     * Logique :
     * 1. Si DÉFENSEUR et relique au sol -> Elle est renvoyée à la base.
     * 2. Si ATTAQUANT et relique non portée -> Il la vole et devient porteur.
     */
    public void ramasserRelique(Player joueur, int numero, CommandBuffer<EntityStore> buffer) {
        PlayerCamp camp = teamManager.getCamp(joueur);
        boolean estAttaquant = (camp == PlayerCamp.ATTAQUANT);
        boolean estDefenseur = (camp == PlayerCamp.DEFENSEUR);

        boolean estAuSol = (numero == 1) ? relic1DroppedPos != null : relic2DroppedPos != null;

        if (estDefenseur) {
            // Un défenseur "sauve" la relique lâchée
            if (estAuSol) {
                retournerRelique(numero, buffer);
                match.getStatsManager().incrementReturn(joueur);
            }
            return;
        }

        if (!estAttaquant) return;

        // Empêcher de ramasser les deux reliques en même temps ou si déjà porté par un allié
        java.util.UUID uuidJoueur = HytaleUtils.getPlayerUuid(joueur);
        if (java.util.Objects.equals(uuidJoueur, carrier1Uuid) || java.util.Objects.equals(uuidJoueur, carrier2Uuid)) {
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("relic_carrier_already"));
            return;
        }
        if (numero == 1 && carrier1Uuid != null) {
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("relic_carrier_teammate"));
            return;
        }
        if (numero == 2 && carrier2Uuid != null) {
            joueur.sendMessage(OpsOrbis.get().getLangManager().get("relic_carrier_teammate"));
            return;
        }

        boolean etaitAuSol = (numero == 1) ? relic1DroppedPos != null : relic2DroppedPos != null;
        
        // --- CORRECTION : Supprimer l'entité AVANT de nullifier la référence ---
        supprimerEntiteRelique(numero, buffer);

        if (numero == 1) {
            carrier1Uuid = HytaleUtils.getPlayerUuid(joueur);
            relic1DroppedPos = null;
        } else {
            carrier2Uuid = HytaleUtils.getPlayerUuid(joueur);
            relic2DroppedPos = null;
        }
        HytaleUtils.diffuserMessage(monde, 
            OpsOrbis.get().getLangManager().get("relic_picked_up_global", "player", joueur.getDisplayName(), "number", numero)
        );
        
        match.getStatsManager().incrementSteal(joueur);
 
        // Annonce visuelle au centre de l'écran
        match.diffuserAnnonceEquipe(PlayerCamp.ATTAQUANT,
            OpsOrbis.get().getLangManager().get("relic_stolen_title", "player", joueur.getDisplayName()),
            OpsOrbis.get().getLangManager().get("relic_stolen_subtitle", "player", joueur.getDisplayName())
        );
        
        match.diffuserAnnonceEquipe(PlayerCamp.DEFENSEUR,
            OpsOrbis.get().getLangManager().get("relic_alert_title", "player", joueur.getDisplayName()),
            OpsOrbis.get().getLangManager().get("relic_alert_subtitle", "player", joueur.getDisplayName())
        );

        joueur.sendMessage(OpsOrbis.get().getLangManager().get("relic_in_hand", "number", numero));
        supprimerEntiteRelique(numero, buffer);
 
        Store<EntityStore> stockage = monde.getEntityStore().getStore();
        assert joueur.getReference() != null;
        joueur.giveItem(new ItemStack("Bench_Memories", 1), joueur.getReference(), stockage);
        match.getScoreboardHUD().rafraichirTous();
    }

    // ─── Retour défenseur ─────────────────────────────────────────────────────

    /**
     * Renvoie la relique à son point de spawn initial (base défenseurs).
     * Appelé quand un défenseur "touche" une relique lâchée par un attaquant mort.
     */
    private void retournerRelique(int numero, CommandBuffer<EntityStore> buffer) {
        Vector3d positionOrigine = (numero == 1) ? relic1OriginalPos : relic2OriginalPos;
        if (positionOrigine == null) return;

        supprimerEntiteRelique(numero, buffer);

        // La relique retourne à la base : effacer sa position de "drop" au sol
        if (numero == 1) relic1DroppedPos = null;
        if (numero == 2) relic2DroppedPos = null;

        final int numeroFinal = numero;
        final Vector3d positionFinale = positionOrigine;
        monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), positionFinale, numeroFinal));

        HytaleUtils.diffuserMessage(monde,
            OpsOrbis.get().getLangManager().get("relic_returned_chat", "number", numero));
             
        match.getScoreboardHUD().rafraichirTous();
    }

    // ─── Dépôt ────────────────────────────────────────────────────────────────

    /**
     * Vérifie si un porteur est dans la zone de dépôt (base attaquants).
     * Si oui, la relique est considérée comme capturée.
     */
    public void verifierDepot(Player joueur, CommandBuffer<EntityStore> buffer) {
        MapConfig config = OpsOrbis.get().getConfigManager().getMapConfig();
        java.util.UUID uuidJoueur = HytaleUtils.getPlayerUuid(joueur);
        if (!java.util.Objects.equals(uuidJoueur, carrier1Uuid) && !java.util.Objects.equals(uuidJoueur, carrier2Uuid)) return;

        assert joueur.getReference() != null;
        assert joueur.getWorld() != null;
        TransformComponent transform = joueur.getWorld().getEntityStore().getStore()
            .getComponent(joueur.getReference(), TransformComponent.getComponentType());

        if (config.getDepositZone() != null) {
            assert transform != null;
            // On vérifie les coordonnées du joueur par rapport à la zone de dépôt
            if (config.getDepositZone().containsPosition(
                    transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ())) {

                // Sécurité : on vérifie que le joueur a bien l'item physique dans son inventaire
                if (!possedeReliqueItem(joueur)) {
                    assert uuidJoueur != null;
                    if (uuidJoueur.equals(carrier1Uuid)) carrier1Uuid = null;
                    if (uuidJoueur.equals(carrier2Uuid)) carrier2Uuid = null;
                    match.getScoreboardHUD().rafraichirTous();
                    return;
                }

                int numeroRelique = java.util.Objects.equals(uuidJoueur, carrier1Uuid) ? 1 : 2;
                relicsCapturees++;

                if (numeroRelique == 1) relic1Capturee = true;
                else relic2Capturee = true;

                // Suppression de l'item d'inventaire
                joueur.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));
                if (numeroRelique == 1) carrier1Uuid = null;
                else carrier2Uuid = null;

                HytaleUtils.diffuserMessage(monde,
                        OpsOrbis.get().getLangManager().get("relic_captured_chat", "number", numeroRelique, "count", relicsCapturees)
                );

                match.getStatsManager().incrementCapture(joueur);
  
                match.getScoreboardHUD().rafraichirTous();
                verifierVictoire(monde, buffer);
            }
        }
    }

    private void verifierVictoire(World monde, CommandBuffer<EntityStore> buffer) {
        if (relicsCapturees >= 2) {
            match.terminerRound(PlayerCamp.ATTAQUANT, buffer);
        }
    }

    // ─── Mort du porteur ──────────────────────────────────────────────────────

    /**
     * Un attaquant porteur meurt ou se déconnecte → drop la relique à sa position.
     * @param uuidPorteur L'UUID du porteur (obligatoire)
     * @param mort Le joueur (optionnel, peut être nul lors d'une déconnexion)
     */
    public void gererMortDuPorteur(java.util.UUID uuidPorteur, Player mort, Store<EntityStore> stockage, CommandBuffer<EntityStore> buffer, Vector3d positionExplicite) {
        if (uuidPorteur == null) return;

        int numeroRelique = 0;
        if (uuidPorteur.equals(carrier1Uuid)) numeroRelique = 1;
        else if (uuidPorteur.equals(carrier2Uuid)) numeroRelique = 2;
        
        if (numeroRelique == 0) return;
 
        if (mort != null) {
            match.getStatsManager().incrementDeath(mort);
        }

        // Obtenir la position de drop de manière sécurisée
        Vector3d positionDrop = positionExplicite;
        if (positionDrop == null && mort != null) {
            Ref<EntityStore> ref = mort.getReference();
            TransformComponent transform = (ref != null) ? stockage.getComponent(ref, TransformComponent.getComponentType()) : null;
            if (transform != null) positionDrop = transform.getPosition().clone();
        }
        
        // Fallback ultime : position d'origine
        if (positionDrop == null) {
            positionDrop = (numeroRelique == 1) ? relic1OriginalPos : relic2OriginalPos;
        }

        if (numeroRelique == 1) { carrier1Uuid = null; relic1DroppedPos = positionDrop; }
        else                    { carrier2Uuid = null; relic2DroppedPos = positionDrop; }

        // Retirer la relique de l'inventaire si possible
        if (mort != null && mort.getWorld() != null) {
            try {
                mort.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));
            } catch (Exception ignored) {}
        }

        // Faire apparaître la relique et rafraîchir le scoreboard
        final int numeroFinal = numeroRelique;
        final Vector3d positionSpawn = positionDrop;
        monde.execute(() -> {
            spawnRelic(monde.getEntityStore().getStore(), positionSpawn, numeroFinal);
            match.getScoreboardHUD().rafraichirTous();
        });

        String nomJoueur = (mort != null) ? mort.getDisplayName() : "Un porteur";
        HytaleUtils.diffuserMessage(monde, 
            OpsOrbis.get().getLangManager().get("relic_dropped_chat", "player", nomJoueur, "number", numeroRelique)
        );
    }
    
    public void dropReliqueSiPorteur(Player joueur, Vector3d positionExplicite) {
        if (joueur == null) return;
        dropReliqueSiPorteurParUuid(HytaleUtils.getPlayerUuid(joueur), joueur, positionExplicite);
    }

    /**
     * Version robuste utilisant l'UUID directement pour la déconnexion.
     */
    public void dropReliqueSiPorteurParUuid(UUID uuid, Player joueur, Vector3d positionExplicite) {
        int numero = 0;
        if (uuid != null) {
            if (uuid.equals(carrier1Uuid)) numero = 1;
            else if (uuid.equals(carrier2Uuid)) numero = 2;
        }
        
        if (numero == 0) return;
        
        final int numeroRelique = numero;
        final java.util.UUID uuidFinal = uuid;
        final Vector3d positionFinale = positionExplicite;
        monde.execute(() -> {
            gererMortDuPorteur(uuidFinal, joueur, monde.getEntityStore().getStore(), null, positionFinale);
        });
    }

    // ─── Nettoyage ────────────────────────────────────────────────────────────

    /**
     * Nettoie les reliques (ItemComponent) proches de leurs points de spawn.
     * Sécurité si les Refs sont perdues.
     */
    private void nettoyerReliquesCible(Store<EntityStore> store) {
        if (store == null) return;
        MapConfig config = OpsOrbis.get().getConfigManager().getMapConfig();
        Vector3d s1 = config.getRelic1();
        Vector3d s2 = config.getRelic2();
        
        // On cherche les drops d'items (ItemComponent) AVEC leur position (TransformComponent)
        Query<EntityStore> query = Archetype.of(ItemComponent.getComponentType(), TransformComponent.getComponentType());
        store.forEachChunk(query, (chunk, buffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                ItemComponent ic = chunk.getComponent(i, ItemComponent.getComponentType());
                TransformComponent tc = chunk.getComponent(i, TransformComponent.getComponentType());
                
                if (ic != null && tc != null && ic.getItemStack() != null) {
                    String assetId = ic.getItemStack().toString(); // Fallback string check
                    if (assetId.contains("Bench_Memories")) {
                        Vector3d pos = tc.getPosition();
                        // Si l'objet est proche d'un spawn ou d'une position lâchée connue
                        if ((s1 != null && pos.distanceTo(s1) < 10) || 
                            (s2 != null && pos.distanceTo(s2) < 10) ||
                            (relic1DroppedPos != null && pos.distanceTo(relic1DroppedPos) < 5) ||
                            (relic2DroppedPos != null && pos.distanceTo(relic2DroppedPos) < 5)) {
                            buffer.removeEntity(chunk.getReferenceTo(i), RemoveReason.REMOVE);
                        }
                    }
                }
            }
        });
    }

    public void supprimerReliques(CommandBuffer<EntityStore> buffer) {
        Store<EntityStore> store = monde.getEntityStore().getStore();
        if (buffer != null) {
            try { if (relic1Ref != null) buffer.removeEntity(relic1Ref, RemoveReason.REMOVE); } catch (Exception ignored) {}
            try { if (relic2Ref != null) buffer.removeEntity(relic2Ref, RemoveReason.REMOVE); } catch (Exception ignored) {}
            // On ne fait PAS de nettoyerReliquesCible ici pour éviter le crash en plein tick
        } else {
            try { if (relic1Ref != null) store.removeEntity(relic1Ref, RemoveReason.REMOVE); } catch (Exception ignored) {}
            try { if (relic2Ref != null) store.removeEntity(relic2Ref, RemoveReason.REMOVE); } catch (Exception ignored) {}
            nettoyerReliquesCible(store);
        }
        relic1Ref = null;
        relic2Ref = null;
        carrier1Uuid = null;
        carrier2Uuid = null;
        relic1DroppedPos = null;
        relic2DroppedPos = null;
        relic1Capturee = false;
        relic2Capturee = false;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    private boolean estMemeJoueur(Player p1, Player p2) {
        if (p1 == null || p2 == null) return false;
        return java.util.Objects.equals(p1.getReference(), p2.getReference());
    }

    public int getRelicsCapturees() { return relicsCapturees; }
    public java.util.UUID getCarrier1Uuid() { return carrier1Uuid; }
    public java.util.UUID getCarrier2Uuid() { return carrier2Uuid; }
    
    public Player getCarrierRelic1() { 
        return carrier1Uuid != null ? OpsOrbis.get().getGameManager().getTeamManager().getJoueurParUUID(carrier1Uuid) : null; 
    }
    public Player getCarrierRelic2() { 
        return carrier2Uuid != null ? OpsOrbis.get().getGameManager().getTeamManager().getJoueurParUUID(carrier2Uuid) : null; 
    }
    public Vector3d getRelic1DroppedPos() { return relic1DroppedPos; }
    public Vector3d getRelic2DroppedPos() { return relic2DroppedPos; }

    // Alias pour compatibilité
    public int getScoreBleu() { return relicsCapturees; }
    public int getScoreRouge() { return 0; }

    /**
     * État de la relique pour le scoreboard.
     * Base → dans sa position d'origine
     * Terrain → lâchée sur le sol (récupérable)
     * Volée → portée par un attaquant
     * Capturée → déposée en base attaquants
     */
    public String getRelicB1Status() {
        if (relic1Capturee) return OpsOrbis.get().getLangManager().getRaw("relic_status_captured");
        if (carrier1Uuid != null) return OpsOrbis.get().getLangManager().getRaw("relic_status_carried");
        if (relic1DroppedPos != null) return OpsOrbis.get().getLangManager().getRaw("relic_status_dropped");
        return OpsOrbis.get().getLangManager().getRaw("relic_status_at_base");
    }

    public String getRelicB2Status() {
        if (relic2Capturee) return OpsOrbis.get().getLangManager().getRaw("relic_status_captured");
        if (carrier2Uuid != null) return OpsOrbis.get().getLangManager().getRaw("relic_status_carried");
        if (relic2DroppedPos != null) return OpsOrbis.get().getLangManager().getRaw("relic_status_dropped");
        return OpsOrbis.get().getLangManager().getRaw("relic_status_at_base");
    }

    public String getRelicR1Status() { return "—"; }
    public String getRelicR2Status() { return "—"; }

    public boolean estReliqueDisponible(boolean isBlue, int number) {
        if (number == 1) return relic1Ref != null && relic1Ref.isValid();
        return relic2Ref != null && relic2Ref.isValid();
    }

    private boolean possedeReliqueItem(Player joueur) {
        if (joueur == null) return false;
        return checkConteneur(joueur.getInventory().getHotbar(), 9) ||
               checkConteneur(joueur.getInventory().getStorage(), 27) ||
               checkConteneur(joueur.getInventory().getUtility(), 3);
    }

    private boolean checkConteneur(ItemContainer container, int taille) {
        if (container == null) return false;
        for (short i = 0; i < (short)taille; i++) {
            try {
                ItemStack stack = container.getItemStack(i);
                if (stack != null && stack.toString().contains("Bench_Memories")) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
}
