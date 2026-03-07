package com.experience.kits;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.Inventory;
import java.awt.Color;

import java.util.HashMap;
import java.util.Map;

public class KitManager {

    public enum KitType {
        GUERRIER("Guerrier"),
        ARCHER("Archer"),
        MAGE("Mage");

        private final String nom;

        KitType(String nom) {
            this.nom = nom;
        }

        public String getNom() {
            return nom;
        }
    }

    private final Map<Player, KitType> joueursKits;

    public KitManager() {
        this.joueursKits = new HashMap<>();
    }

    public void choisirKit(Player joueur, KitType kit) {
        joueursKits.put(joueur, kit);
        joueur.sendMessage(Message.join(
            Message.raw("Vous avez sélectionné le kit ").color(Color.WHITE),
            Message.raw(kit.getNom()).color(Color.ORANGE)
        ));
    }

    public KitType getKit(Player joueur) {
        return joueursKits.getOrDefault(joueur, KitType.GUERRIER);
    }

    public void donnerEquipement(Player joueur) {
        KitType kit = getKit(joueur);
        Inventory inventaire = joueur.getInventory();
        
        inventaire.clear();

        switch (kit) {
            case GUERRIER:
                donnerKitGuerrier(inventaire);
                break;
            case ARCHER:
                donnerKitArcher(inventaire);
                break;
            case MAGE:
                donnerKitMage(inventaire);
                break;
        }
        joueur.sendMessage(Message.raw("Votre équipement a été distribué !").color(Color.GREEN));
    }

    private void donnerKitGuerrier(Inventory inventaire) {
        // TODO: Ajouter équipement réel
    }

    private void donnerKitArcher(Inventory inventaire) {
        // TODO: Ajouter équipement réel
    }

    private void donnerKitMage(Inventory inventaire) {
        // TODO: Ajouter équipement réel
    }
}
