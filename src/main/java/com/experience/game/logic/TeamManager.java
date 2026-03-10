package com.experience.game.logic;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.experience.utils.HytaleUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère la répartition des joueurs dans les deux équipes asymétriques :
 * - Équipe Attaquants : volent les reliques des défenseurs
 * - Équipe Défenseurs : protègent leurs reliques, assistés de PNJs
 */
public class TeamManager {

    private final List<Player> equipeAttaquants;
    private final List<Player> equipeDefenseurs;

    public TeamManager() {
        this.equipeAttaquants = new ArrayList<>();
        this.equipeDefenseurs = new ArrayList<>();
    }

    /**
     * Ajoute un joueur à une équipe (équilibrage automatique) et le téléporte.
     * @param joueur Le joueur à intégrer.
     */
    public void ajouterJoueur(Player joueur) {
        if (contientJoueur(equipeAttaquants, joueur) || contientJoueur(equipeDefenseurs, joueur)) return;

        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();

        // Équilibrage : attaquants d'abord si égaux
        if (equipeAttaquants.size() <= equipeDefenseurs.size()) {
            equipeAttaquants.add(joueur);
            joueur.sendMessage(Message.join(
                Message.raw("Vous êtes ").color(Color.WHITE),
                Message.raw("Attaquant").color(new Color(255, 160, 0)),
                Message.raw(" ! Volez les 2 reliques des défenseurs.").color(Color.WHITE)
            ));
            teleporterJoueur(joueur, config.getBoxCenter(config.getBlueZone()));
        } else {
            equipeDefenseurs.add(joueur);
            joueur.sendMessage(Message.join(
                Message.raw("Vous êtes ").color(Color.WHITE),
                Message.raw("Défenseur").color(new Color(0, 200, 100)),
                Message.raw(" ! Protégez vos reliques.").color(Color.WHITE)
            ));
            teleporterJoueur(joueur, config.getBoxCenter(config.getRedZone()));
        }
    }

    /**
     * Retire un joueur de son équipe actuelle.
     * @param joueur Le joueur à retirer.
     */
    public void retirerJoueur(Player joueur) {
        if (joueur == null || joueur.getReference() == null) return;
        equipeAttaquants.removeIf(p -> p != null && java.util.Objects.equals(p.getReference(), joueur.getReference()));
        equipeDefenseurs.removeIf(p -> p != null && java.util.Objects.equals(p.getReference(), joueur.getReference()));
    }

    /**
     * Téléporte le joueur au spawn de son équipe.
     * @param joueur Le joueur à téléporter.
     */
    public void teleporterAuSpawn(Player joueur) {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        if (contientJoueur(equipeAttaquants, joueur)) {
            teleporterJoueur(joueur, config.getBoxCenter(config.getBlueZone()));
        } else if (contientJoueur(equipeDefenseurs, joueur)) {
            teleporterJoueur(joueur, config.getBoxCenter(config.getRedZone()));
        }
    }

    private void teleporterJoueur(Player joueur, Vector3d position) {
        HytaleUtils.teleporterJoueur(joueur, position);
    }

    /**
     * Vérifie si un joueur est dans l'équipe spécifiée.
     * @param joueur Le joueur.
     * @param equipe "Attaquant" ou "Defenseur"
     */
    public boolean estDansEquipe(Player joueur, String equipe) {
        if ("Attaquant".equalsIgnoreCase(equipe)) return contientJoueur(equipeAttaquants, joueur);
        if ("Defenseur".equalsIgnoreCase(equipe)) return contientJoueur(equipeDefenseurs, joueur);
        return false;
    }

    /**
     * Vérifie si deux joueurs sont dans la même équipe.
     */
    public boolean sontDansLaMemeEquipe(Player joueur1, Player joueur2) {
        if (contientJoueur(equipeAttaquants, joueur1) && contientJoueur(equipeAttaquants, joueur2)) return true;
        if (contientJoueur(equipeDefenseurs, joueur1) && contientJoueur(equipeDefenseurs, joueur2)) return true;
        return false;
    }

    public List<Player> getEquipeAttaquants() { return equipeAttaquants; }
    public List<Player> getEquipeDefenseurs() { return equipeDefenseurs; }

    // Compatibilité avec l'ancien code (pour éviter les erreurs de compilation)
    public List<Player> getEquipeBleue() { return equipeAttaquants; }
    public List<Player> getEquipeRouge() { return equipeDefenseurs; }

    /**
     * Vérifie de manière robuste si un joueur est dans une liste.
     */
    private boolean contientJoueur(List<Player> equipe, Player joueur) {
        if (joueur == null) return false;
        for (Player p : equipe) {
            if (java.util.Objects.equals(p.getReference(), joueur.getReference())) return true;
        }
        return false;
    }
}
