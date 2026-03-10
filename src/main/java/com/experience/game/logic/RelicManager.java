package com.experience.game.logic;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
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

import java.awt.*;
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

    // Porteurs
    private Player carrierRelic1;
    private Player carrierRelic2;

    // Score
    private int relicsCapturees = 0;

    public RelicManager(World monde, TeamManager teamManager) {
        this.monde = monde;
        this.teamManager = teamManager;
    }

    public void initRelics() {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        relic1OriginalPos = config.getRelic1();
        relic2OriginalPos = config.getRelic2();
        // Réinitialiser l'état au début de chaque partie
        relic1DroppedPos = null;
        relic2DroppedPos = null;
        HytaleLogger.getLogger().at(Level.INFO).log("[ExperienceMod] Apparition des Reliques...");
        monde.execute(() -> {
            Store<EntityStore> stockageInit = monde.getEntityStore().getStore();
            if (relic1OriginalPos != null) spawnRelic(stockageInit, relic1OriginalPos, 1);
            if (relic2OriginalPos != null) spawnRelic(stockageInit, relic2OriginalPos, 2);
        });
    }

    private void spawnRelic(Store<EntityStore> stockage, Vector3d position, int numero) {
        String nom = "Bench_Memories";
        if (Item.getAssetMap().getAsset(nom) == null) {
            HytaleLogger.getLogger().at(Level.SEVERE).log("[ExperienceMod] Relique introuvable: " + nom);
            return;
        }
        Holder<EntityStore> entite = ItemComponent.generateItemDrop(stockage, new ItemStack(nom, 1), position, Vector3f.ZERO, 0f, 0f, 0f);
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
            if (buffer != null) buffer.removeEntity(relic1Ref, RemoveReason.REMOVE);
            else stockage.removeEntity(relic1Ref, RemoveReason.REMOVE);
            relic1Ref = null;
        }
        if (numero == 2 && relic2Ref != null) {
            if (buffer != null) buffer.removeEntity(relic2Ref, RemoveReason.REMOVE);
            else stockage.removeEntity(relic2Ref, RemoveReason.REMOVE);
            relic2Ref = null;
        }
    }

    // ─── Ramassage ────────────────────────────────────────────────────────────

    /**
     * Un joueur tente de ramasser une relique (à sa position d'origine ou lâchée).
     * Seuls les attaquants peuvent la voler ; les défenseurs la retournent à la base.
     */
    public void ramasserRelique(Player joueur, int numero, CommandBuffer<EntityStore> buffer) {
        boolean estAttaquant = teamManager.estDansEquipe(joueur, "Attaquant");
        boolean estDefenseur = teamManager.estDansEquipe(joueur, "Defenseur");

        if (estDefenseur) {
            // Un défenseur passe sur la relique lâchée → elle retourne à la base
            retournerRelique(numero, buffer);
            return;
        }

        if (!estAttaquant) return;

        // Vérifications attaquant
        if (estMemeJoueur(joueur, carrierRelic1) || estMemeJoueur(joueur, carrierRelic2)) {
            joueur.sendMessage(Message.raw("Vous portez déjà une relique !").color(Color.RED));
            return;
        }
        if (numero == 1 && carrierRelic1 != null) {
            joueur.sendMessage(Message.raw("Cette relique est déjà portée par un coéquipier !").color(Color.RED));
            return;
        }
        if (numero == 2 && carrierRelic2 != null) {
            joueur.sendMessage(Message.raw("Cette relique est déjà portée par un coéquipier !").color(Color.RED));
            return;
        }

        boolean etaitAuSol = (numero == 1) ? relic1DroppedPos != null : relic2DroppedPos != null;
        if (numero == 1) { carrierRelic1 = joueur; relic1DroppedPos = null; }
        if (numero == 2) { carrierRelic2 = joueur; relic2DroppedPos = null; }

        ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.join(
            Message.raw("⚔ ").color(Color.ORANGE),
            Message.raw(joueur.getDisplayName()).color(Color.YELLOW),
            Message.raw(etaitAuSol
                ? " récupère la Relique " + numero + " au sol !"
                : " vole la Relique " + numero + " !")
                .color(Color.ORANGE)
        ));

        joueur.sendMessage(Message.raw("Relique " + numero + " en main ! Courez à votre base !").color(Color.YELLOW));
        supprimerEntiteRelique(numero, buffer);

        Store<EntityStore> stockage = monde.getEntityStore().getStore();
        joueur.giveItem(new ItemStack("Bench_Memories", 1), joueur.getReference(), stockage);
        ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
    }

    // ─── Retour défenseur ─────────────────────────────────────────────────────

    /**
     * Un défenseur passe sur une relique lâchée → retour au spawn d'origine.
     */
    private void retournerRelique(int numero, CommandBuffer<EntityStore> buffer) {
        Vector3d positionOrigine = (numero == 1) ? relic1OriginalPos : relic2OriginalPos;
        if (positionOrigine == null) return;

        supprimerEntiteRelique(numero, buffer);

        // La relique retourne à la base : effacer sa position de terrain
        if (numero == 1) relic1DroppedPos = null;
        if (numero == 2) relic2DroppedPos = null;

        final int numeroFinal = numero;
        final Vector3d positionFinale = positionOrigine;
        monde.execute(() -> spawnRelic(monde.getEntityStore().getStore(), positionFinale, numeroFinal));

        ExperienceMod.get().getGameManager().diffuserMessage(monde,
            Message.raw("🛡 Un défenseur a remis la Relique " + numero + " à la base !").color(new Color(0, 200, 100)));

        ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
    }

    // ─── Dépôt ────────────────────────────────────────────────────────────────

    public void verifierDepot(Player joueur, CommandBuffer<EntityStore> buffer) {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        if (!estMemeJoueur(joueur, carrierRelic1) && !estMemeJoueur(joueur, carrierRelic2)) return;

        TransformComponent transform = joueur.getWorld().getEntityStore().getStore()
            .getComponent(joueur.getReference(), TransformComponent.getComponentType());

        if (config.getDepositZone() != null && config.getDepositZone().containsPosition(
                transform.getPosition().getX(), transform.getPosition().getY(), transform.getPosition().getZ())) {

            int numeroRelique = estMemeJoueur(joueur, carrierRelic1) ? 1 : 2;
            relicsCapturees++;
            joueur.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));
            if (estMemeJoueur(joueur, carrierRelic1)) carrierRelic1 = null; else carrierRelic2 = null;

            ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.join(
                Message.raw("✓ Relique " + numeroRelique + " capturée ! ").color(new Color(255, 160, 0)),
                Message.raw(relicsCapturees + "/2 reliques capturées.").color(Color.WHITE)
            ));

            ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
            verifierVictoire(monde, buffer);
        }
    }

    private void verifierVictoire(World monde, CommandBuffer<EntityStore> buffer) {
        if (relicsCapturees >= 2) {
            ExperienceMod.get().getGameManager().terminerPartie(monde, "Attaquants", buffer);
        }
    }

    // ─── Mort du porteur ──────────────────────────────────────────────────────

    /**
     * Un attaquant porteur meurt → drop la relique à sa position de mort.
     * La relique n'est PAS renvoyée à la base : elle reste au sol jusqu'à qu'un
     * défenseur passe dessus.
     */
    public void gererMortDuPorteur(Player mort, Store<EntityStore> stockage, CommandBuffer<EntityStore> buffer) {
        int numeroRelique = 0;
        if (estMemeJoueur(mort, carrierRelic1)) numeroRelique = 1;
        else if (estMemeJoueur(mort, carrierRelic2)) numeroRelique = 2;
        if (numeroRelique == 0) return;

        // Obtenir la position de mort
        TransformComponent transform = stockage.getComponent(mort.getReference(), TransformComponent.getComponentType());
        Vector3d positionMort = (transform != null) ? transform.getPosition().clone() :
            ((numeroRelique == 1) ? relic1OriginalPos : relic2OriginalPos);

        if (numeroRelique == 1) { carrierRelic1 = null; relic1DroppedPos = positionMort; }
        else                    { carrierRelic2 = null; relic2DroppedPos = positionMort; }

        // Retirer la relique de l'inventaire
        mort.getInventory().getCombinedEverything().removeItemStack(new ItemStack("Bench_Memories", 1));

        // Faire apparaître la relique à la position de mort et mettre à jour le scoreboard
        final int numeroFinal = numeroRelique;
        final Vector3d positionSpawn = positionMort;
        monde.execute(() -> {
            spawnRelic(monde.getEntityStore().getStore(), positionSpawn, numeroFinal);
            // Rafraîchissement scoreboard hors contexte ECS pour éviter les blocages
            ExperienceMod.get().getGameManager().getScoreboardHUD().rafraichirTous();
        });

        ExperienceMod.get().getGameManager().diffuserMessage(monde, Message.join(
            Message.raw("💀 ").color(Color.RED),
            Message.raw(mort.getDisplayName()).color(Color.YELLOW),
            Message.raw(" a été éliminé ! La Relique " + numeroRelique + " est au sol — les défenseurs peuvent la récupérer !").color(Color.ORANGE)
        ));
    }

    // ─── Nettoyage ────────────────────────────────────────────────────────────

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
        relic1DroppedPos = null;
        relic2DroppedPos = null;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    private boolean estMemeJoueur(Player p1, Player p2) {
        if (p1 == null || p2 == null) return false;
        return java.util.Objects.equals(p1.getReference(), p2.getReference());
    }

    public int getRelicsCapturees() { return relicsCapturees; }
    public Player getCarrierRelic1() { return carrierRelic1; }
    public Player getCarrierRelic2() { return carrierRelic2; }
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
        if (carrierRelic1 != null) return "Volée";
        if (relic1DroppedPos != null) return "Terrain";
        if (relic1Ref != null) return "Base";
        return "Capturée";
    }

    public String getRelicB2Status() {
        if (carrierRelic2 != null) return "Volée";
        if (relic2DroppedPos != null) return "Terrain";
        if (relic2Ref != null) return "Base";
        return "Capturée";
    }

    public String getRelicR1Status() { return "—"; }
    public String getRelicR2Status() { return "—"; }

    public boolean estReliqueDisponible(boolean isBlue, int number) {
        if (number == 1) return relic1Ref != null;
        return relic2Ref != null;
    }
}
