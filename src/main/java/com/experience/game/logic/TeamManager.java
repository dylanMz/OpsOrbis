package com.experience.game.logic;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
import com.experience.utils.HytaleUtils;
import java.awt.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Gère la répartition des joueurs dans les équipes et leurs téléportations aux spawns.
 * Cette classe assure l'équilibrage automatique des équipes (Bleue vs Rouge).
 */
public class TeamManager {

    private final List<Player> equipeBleue;
    private final List<Player> equipeRouge;

    public TeamManager() {
        this.equipeBleue = new ArrayList<>();
        this.equipeRouge = new ArrayList<>();
    }

    /**
     * Ajoute un joueur à une équipe (équilibrage automatique) et le téléporte.
     * @param joueur Le joueur à intégrer.
     */
    /**
     * Ajoute un joueur à une équipe en privilégiant l'équipe la moins nombreuse (équilibrage).
     * @param joueur Le joueur à intégrer.
     */
    public void ajouterJoueur(Player joueur) {
        // Éviter d'ajouter deux fois le même joueur
        if (equipeBleue.contains(joueur) || equipeRouge.contains(joueur)) {
            return;
        }

        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();

        // Logique d'équilibrage : si l'équipe bleue est plus petite ou égale à la rouge
        if (equipeBleue.size() <= equipeRouge.size()) {
            equipeBleue.add(joueur);
            joueur.sendMessage(Message.join(
                Message.raw("Vous avez rejoint l'équipe ").color(Color.WHITE),
                Message.raw("Bleue").color(Color.BLUE),
                Message.raw(" !").color(Color.WHITE)
            ));
            // Téléportation au centre de la zone de spawn bleue
            teleporterJoueur(joueur, config.getBoxCenter(config.getBlueZone()));
        } else {
            // Sinon, ajout dans l'équipe rouge
            equipeRouge.add(joueur);
            joueur.sendMessage(Message.join(
                Message.raw("Vous avez rejoint l'équipe ").color(Color.WHITE),
                Message.raw("Rouge").color(Color.RED),
                Message.raw(" !").color(Color.WHITE)
            ));
            // Téléportation au centre de la zone de spawn rouge
            teleporterJoueur(joueur, config.getBoxCenter(config.getRedZone()));
        }
    }

    /**
     * Retire un joueur de son équipe actuelle (lors d'une déconnexion par exemple).
     * @param joueur Le joueur à retirer.
     */
    public void retirerJoueur(Player joueur) {
        equipeBleue.remove(joueur);
        equipeRouge.remove(joueur);
    }

    /**
     * Téléporte le joueur au point de spawn correspondant à son équipe actuelle.
     * @param joueur Le joueur à téléporter.
     */
    public void teleporterAuSpawn(Player joueur) {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        // Vérification de l'appartenance à l'équipe bleue
        if (equipeBleue.contains(joueur)) {
            teleporterJoueur(joueur, config.getBoxCenter(config.getBlueZone()));
        } 
        // Vérification pour l'équipe rouge
        else if (equipeRouge.contains(joueur)) {
            teleporterJoueur(joueur, config.getBoxCenter(config.getRedZone()));
        }
    }

    private void teleporterJoueur(Player joueur, Vector3d position) {
        HytaleUtils.teleporterJoueur(joueur, position);
    }

    /**
     * Vérifie si un joueur appartient à une équipe spécifique.
     * @param joueur Le joueur à tester.
     * @param equipe Le nom de l'équipe ("Bleue" ou "Rouge").
     * @return vrai si le joueur est dans cette équipe.
     */
    public boolean estDansEquipe(Player joueur, String equipe) {
        if ("Bleue".equalsIgnoreCase(equipe)) {
            return equipeBleue.contains(joueur);
        } else if ("Rouge".equalsIgnoreCase(equipe)) {
            return equipeRouge.contains(joueur);
        }
        return false;
    }

    /**
     * Vérifie si deux joueurs sont partenaires (dans la même équipe).
     * @param joueur1 Premier joueur.
     * @param joueur2 Second joueur.
     * @return vrai s'ils sont dans la même équipe.
     */
    public boolean sontDansLaMemeEquipe(Player joueur1, Player joueur2) {
        if (equipeBleue.contains(joueur1) && equipeBleue.contains(joueur2)) return true;
        if (equipeRouge.contains(joueur1) && equipeRouge.contains(joueur2)) return true;
        return false;
    }

    public List<Player> getEquipeBleue() { return equipeBleue; }
    public List<Player> getEquipeRouge() { return equipeRouge; }
}
