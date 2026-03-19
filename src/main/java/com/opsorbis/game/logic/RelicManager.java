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
 * Gère les 2 reliques dans la base des défenseurs.
 * <p>
 * Nouveau comportement de mort d'un porteur :
 *  - La relique est lâchée à la position du joueur mort (pas respawned à l'origine).
 *  - Un ATTAQUANT qui passe dessus la récupère.
 *  - Un DÉFENSEUR qui passe dessus la renvoie à son spawn d'origine.
 */
public class RelicManager {

    private final World monde;
    private final TeamManager teamManager;

    // Refs des entités reliques dans le monde
    private Ref<EntityStore> relic1Ref;
    private Ref<EntityStore> relic2Ref;

    // Position originale dans la base des défenseurs
    private Vector3d relic1OriginalPos;
    private Vector3d relic2OriginalPos;

    // Position actuelle de la relique lâchée (null si portée ou à la base)
    private Vector3d relic1DroppedPos;
    private Vector3d relic2DroppedPos;

    // État strict de capture (pour éviter le faux-positif si l'entité despawn dans le vide)
    private boolean relic1Capturee;
    private boolean relic2Capturee;

    // UUIDs des porteurs (plus robuste pour la déconnexion/reconnexion)
    private java.util.UUID carrier1Uuid;
    private java.util.UUID carrier2Uuid;

    // Score
    private int relicsCapturees = 0;

    public RelicManager(World monde, TeamManager teamManager) {
        this.monde = monde;
        this.teamManager = teamManager;
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
     * Un joueur tente de ramasser une relique (à sa position d'origine ou lâchée).
     * Seuls les attaquants peuvent la voler ; les défenseurs la retournent à la base.
     */
    public void ramasserRelique(Player joueur, int numero, CommandBuffer<EntityStore> buffer) {
        PlayerCamp camp = teamManager.getCamp(joueur);
        boolean estAttaquant = (camp == PlayerCamp.ATTAQUANT);
        boolean estDefenseur = (camp == PlayerCamp.DEFENSEUR);

        boolean estAuSol = (numero == 1) ? relic1DroppedPos != null : relic2DroppedPos != null;

        if (estDefenseur) {
            // Seulement si la relique a été lâchée au sol (elle n'est pas à la base)
            if (estAuSol) {
                retournerRelique(numero, buffer);
                OpsOrbis.get().getGameManager().getStatsManager().incrementReturn(joueur);
            }
            return;
        }

        if (!estAttaquant) return;

        // Vérifications attaquant
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
  OpsOrbis.get().getGameManager().diffuserMessage(monde, 
            OpsOrbis.get().getLangManager().get("relic_picked_up_global", "player", joueur.getDisplayName(), "number", numero)
        );
        
        OpsOrbis.get().getGameManager().getStatsManager().incrementSteal(joueur);

        // Annonce visuelle au centre de l'écran
        OpsOrbis.get().getGameManager().diffuserAnnonceEquipe(monde, PlayerCamp.ATTAQUANT,
            OpsOrbis.get().getLangManager().get("relic_stolen_title", "player", joueur.getDisplayName()),
            OpsOrbis.get().getLangManager().get("relic_stolen_subtitle", "player", joueur.getDisplayName())
        );
        
        OpsOrbis.get().getGameManager().diffuserAnnonceEquipe(monde, PlayerCamp.DEFENSEUR,
            OpsOrbis.get().getLangManager().get("relic_alert_title", "player", joueur.getDisplayName()),
            OpsOrbis.get().getLangManager().get("relic_alert_subtitle", "player", joueur.getDisplayName())
        );


        joueur.sendMessage(OpsOrbis.get().getLangManager().get("relic_in_hand", "number", numero));
        supprimerEntiteRelique(numero, buffer);

        Store<EntityStore> stockage = monde.getEntityStore().getStore();
        assert joueur.getReference() != null;
        joueur.giveItem(new ItemStack("Bench_Memories", 1), joueur.getReference(), stockage);
        OpsOrbis.get().getGameManager().getScoreboardHUD().rafraichirTous();
    }

    // ─── Retour défenseur ─────────────────────────────────────────────────────

    /**
     * Un défenseur passe sur une relique lâchée → retour au spawn d'origine.
     */
    private void retournerRelique(int numero, CommandBuffer<EntityStore> buffer) {
        Vector3d positionOrigine = (numero == 1) ? relic1OriginalPos : relic2OriginalPos;
        if (positionOrigine == null) return;

        // --- CORRECTION : Supprimer l'entité AVANT de nullifier (déjà géré par supprimerEntiteRelique mais sécurité) ---
        supprimerEntiteRelique(numero, buffer);

        // La relique retourne à la base : effacer sa position de terrain
        if (numero == 1) relic1DroppedPos = null;
        if (numero == 2) relic2DroppedPos = null;

        final int numeroFinal = numero;
        final Vector3d positionFinale = positionOrigine;
        monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), positionFinale, numeroFinal));

        OpsOrbis.get().getGameManager().diffuserMessage(monde,
            OpsOrbis.get().getLangManager().get("relic_returned_chat", "number", numero));
            
        // Trouver un défenseur proche pour lui donner le crédit du retour ? 
        // Pour l'instant on se base sur l'appelant direct si disponible
        // Mais retournerRelique est interne. On va devoir passer le joueur.

        OpsOrbis.get().getGameManager().getScoreboardHUD().rafraichirTous();
    }

    // ─── Dépôt ────────────────────────────────────────────────────────────────

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
            if (config.getDepositZone().containsPosition(
                    transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ())) {

                // --- NOUVEAU : Vérification de possession réelle de l'ITEM ---
                if (!possedeReliqueItem(joueur)) {
                    // Le joueur est marqué comme porteur mais n'a pas l'objet (ou l'a jeté/perdu)
                    assert uuidJoueur != null;
                    if (uuidJoueur.equals(carrier1Uuid)) carrier1Uuid = null;
                    if (uuidJoueur.equals(carrier2Uuid)) carrier2Uuid = null;
                    OpsOrbis.get().getGameManager().getScoreboardHUD().rafraichirTous();
                    return;
                }
                // -------------------------------------------------------------

                int numeroRelique = java.util.Objects.equals(uuidJoueur, carrier1Uuid) ? 1 : 2;
                relicsCapturees++;

                if (numeroRelique == 1) relic1Capturee = true;
                else relic2Capturee = true;

                joueur.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));
                if (numeroRelique == 1) carrier1Uuid = null;
                else carrier2Uuid = null;

                OpsOrbis.get().getGameManager().diffuserMessage(monde,
                        OpsOrbis.get().getLangManager().get("relic_captured_chat", "number", numeroRelique, "count", relicsCapturees)
                );

                OpsOrbis.get().getGameManager().getStatsManager().incrementCapture(joueur);

                OpsOrbis.get().getGameManager().getScoreboardHUD().rafraichirTous();
                verifierVictoire(monde, buffer);
            }
        }
    }

    private void verifierVictoire(World monde, CommandBuffer<EntityStore> buffer) {
        if (relicsCapturees >= 2) {
            OpsOrbis.get().getGameManager().terminerRound(monde, PlayerCamp.ATTAQUANT, buffer);
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
            OpsOrbis.get().getGameManager().getStatsManager().incrementDeath(mort);
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
            OpsOrbis.get().getGameManager().getScoreboardHUD().rafraichirTous();
        });

        String nomJoueur = (mort != null) ? mort.getDisplayName() : "Un porteur";
        OpsOrbis.get().getGameManager().diffuserMessage(monde, 
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
