package com.opsorbis.roles;

import com.opsorbis.OpsOrbis;
import com.opsorbis.kits.KitManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gère le choix des rôles (Mêlée / Distance) et la correspondance rôle → kits disponibles.
 */
public class RolesManager {

    /**
     * Les deux rôles disponibles.
     */
    public enum RoleType {
        MELEE("Mêlée"),
        DISTANCE("Distance");

        private final String nom;

        RoleType(String nom) {
            this.nom = nom;
        }

        public String getNom() {
            return nom;
        }
    }

    /**
     * Mapping des kits disponibles par rôle.
     */
    private static final Map<RoleType, List<KitManager.KitType>> KITS_PAR_ROLE = new HashMap<>();

    static {
        KITS_PAR_ROLE.put(RoleType.MELEE, Arrays.asList(
            KitManager.KitType.GUERRIER,
            KitManager.KitType.ASSASSIN
        ));
        KITS_PAR_ROLE.put(RoleType.DISTANCE, Arrays.asList(
            KitManager.KitType.ARCHER,
            KitManager.KitType.ARBALETRIER
        ));
    }

    private final Map<Ref<EntityStore>, RoleType> joueursRoles;

    public RolesManager() {
        this.joueursRoles = new HashMap<>();
    }

    /**
     * Assigne un rôle à un joueur et affiche les kits disponibles.
     * @param joueur Le joueur qui choisit un rôle.
     * @param role Le rôle sélectionné.
     */
    public void choisirRole(Player joueur, RoleType role) {
        joueursRoles.put(joueur.getReference(), role);

        // Message de confirmation
        joueur.sendMessage(OpsOrbis.get().getLangManager().get("role_selected", role.getNom()));

        // Afficher les kits disponibles pour ce rôle
        List<KitManager.KitType> kitsDisponibles = getKitsDisponibles(role);
        StringBuilder kitsNoms = new StringBuilder();
        for (int i = 0; i < kitsDisponibles.size(); i++) {
            if (i > 0) kitsNoms.append(", ");
            kitsNoms.append(kitsDisponibles.get(i).getNom());
        }

        joueur.sendMessage(Message.join(
            OpsOrbis.get().getLangManager().get("kit_help_available", kitsNoms.toString()),
            OpsOrbis.get().getLangManager().get("kit_help_command")
        ));
    }

    /**
     * Retourne le rôle d'un joueur. Par défaut : MELEE.
     * @param joueur Le joueur dont on veut le rôle.
     * @return Le rôle du joueur.
     */
    public RoleType getRole(Player joueur) {
        return joueursRoles.getOrDefault(joueur.getReference(), RoleType.MELEE);
    }

    /**
     * Retourne la liste des kits accessibles pour un rôle donné.
     * @param role Le rôle.
     * @return Liste des KitType disponibles.
     */
    public List<KitManager.KitType> getKitsDisponibles(RoleType role) {
        return KITS_PAR_ROLE.get(role);
    }

    /**
     * Vérifie si un joueur peut utiliser un kit donné en fonction de son rôle.
     * @param joueur Le joueur.
     * @param kit Le kit à vérifier.
     * @return true si le kit est compatible avec le rôle du joueur.
     */
    public boolean peutUtiliserKit(Player joueur, KitManager.KitType kit) {
        RoleType role = getRole(joueur);
        List<KitManager.KitType> kitsDisponibles = KITS_PAR_ROLE.get(role);
        return kitsDisponibles != null && kitsDisponibles.contains(kit);
    }

    /**
     * Vérifie si un joueur a déjà choisi un rôle.
     * @param joueur Le joueur.
     * @return true si le joueur a un rôle assigné.
     */
    public boolean aChoisiRole(Player joueur) {
        return joueursRoles.containsKey(joueur.getReference());
    }
}
