package com.opsorbis.kits;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.npc.util.InventoryHelper;
import com.hypixel.hytale.component.Store;
import java.awt.Color;

import java.util.HashMap;
import java.util.Map;

public class KitManager {

    public enum KitType {
        GUERRIER("Guerrier"),
        ASSASSIN("Assassin"),
        ARCHER("Archer"),
        ARBALETRIER("Arbalétrier");

        private final String nom;

        KitType(String nom) {
            this.nom = nom;
        }

        public String getNom() {
            return nom;
        }
    }

    private final Map<Ref<EntityStore>, KitType> joueursKits;

    public KitManager() {
        this.joueursKits = new HashMap<>();
    }

    public void choisirKit(Player joueur, KitType kit) {
        joueursKits.put(joueur.getReference(), kit);
        joueur.sendMessage(Message.join(
            Message.raw("Vous avez sélectionné le kit ").color(Color.WHITE),
            Message.raw(kit.getNom()).color(Color.ORANGE)
        ));
    }

    public KitType getKit(Player joueur) {
        return joueursKits.getOrDefault(joueur.getReference(), KitType.GUERRIER);
    }

    public void donnerEquipement(Player joueur) {
        joueur.getWorld().execute(() -> {
            KitType kit = getKit(joueur);
            Inventory inventaire = joueur.getInventory();
            
            inventaire.clear();

            switch (kit) {
                case GUERRIER:
                    donnerKitGuerrier(joueur, inventaire);
                    break;
                case ASSASSIN:
                    donnerKitAssassin(joueur, inventaire);
                    break;
                case ARCHER:
                    donnerKitArcher(joueur, inventaire);
                    break;
                case ARBALETRIER:
                    donnerKitArbaletrier(joueur, inventaire);
                    break;
            }
            joueur.sendMessage(Message.raw("Votre équipement a été distribué !").color(Color.GREEN));
        });
    }

    private void donnerKitGuerrier(Player joueur, Inventory inventaire) {
        Store<EntityStore> stockage = joueur.getWorld().getEntityStore().getStore();
        ItemContainer armor = inventaire.getArmor();

        // Armes et outils (Inventaire/Hotbar)
        joueur.giveItem(new ItemStack("Weapon_Battleaxe_Cobalt", 1), joueur.getReference(), stockage);
        joueur.giveItem(new ItemStack("Weapon_Shield_Iron", 1), joueur.getReference(), stockage);

        // Armure (Équipement automatique via InventoryHelper)
        InventoryHelper.useArmor(armor, "Armor_Cobalt_Head");
        InventoryHelper.useArmor(armor, "Armor_Cobalt_Chest");
        InventoryHelper.useArmor(armor, "Armor_Cobalt_Hands");
        InventoryHelper.useArmor(armor, "Armor_Cobalt_Legs");
    }

    private void donnerKitAssassin(Player joueur, Inventory inventaire) {
        Store<EntityStore> stockage = joueur.getWorld().getEntityStore().getStore();
        ItemContainer armor = inventaire.getArmor();

        // Armes
        joueur.giveItem(new ItemStack("Weapon_Daggers_Adamantite_Saurian", 1), joueur.getReference(), stockage);

        // Armure (Fer)
        InventoryHelper.useArmor(armor, "Armor_Iron_Head");
        InventoryHelper.useArmor(armor, "Armor_Iron_Chest");
        InventoryHelper.useArmor(armor, "Armor_Iron_Hands");
        InventoryHelper.useArmor(armor, "Armor_Iron_Legs");
    }

    private void donnerKitArcher(Player joueur, Inventory inventaire) {
        Store<EntityStore> stockage = joueur.getWorld().getEntityStore().getStore();
        ItemContainer armor = inventaire.getArmor();

        // Armes
        joueur.giveItem(new ItemStack("Weapon_Shortbow_Copper", 1), joueur.getReference(), stockage);
        joueur.giveItem(new ItemStack("Weapon_Arrow_Crude", 64), joueur.getReference(), stockage);

        // Armure (Fer par défaut)
        InventoryHelper.useArmor(armor, "Armor_Iron_Head");
        InventoryHelper.useArmor(armor, "Armor_Iron_Chest");
        InventoryHelper.useArmor(armor, "Armor_Iron_Hands");
        InventoryHelper.useArmor(armor, "Armor_Iron_Legs");
    }

    private void donnerKitArbaletrier(Player joueur, Inventory inventaire) {
        Store<EntityStore> stockage = joueur.getWorld().getEntityStore().getStore();
        ItemContainer armor = inventaire.getArmor();

        // Armes
        joueur.giveItem(new ItemStack("Weapon_Crossbow_Ancient_Steel", 1), joueur.getReference(), stockage);
        joueur.giveItem(new ItemStack("Weapon_Arrow_Crude", 64), joueur.getReference(), stockage);

        // Armure
        InventoryHelper.useArmor(armor, "Armor_Iron_Head");
        InventoryHelper.useArmor(armor, "Armor_Iron_Chest");
        InventoryHelper.useArmor(armor, "Armor_Iron_Hands");
        InventoryHelper.useArmor(armor, "Armor_Iron_Legs");
    }
}
