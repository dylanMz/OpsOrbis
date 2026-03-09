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
    public void ajouterJoueur(Player joueur) {
        if (equipeBleue.contains(joueur) || equipeRouge.contains(joueur)) {
            return;
        }

        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();

        if (equipeBleue.size() <= equipeRouge.size()) {
            equipeBleue.add(joueur);
            joueur.sendMessage(Message.join(
                Message.raw("Vous avez rejoint l'équipe ").color(Color.WHITE),
                Message.raw("Bleue").color(Color.BLUE),
                Message.raw(" !").color(Color.WHITE)
            ));
            teleporterJoueur(joueur, config.getBoxCenter(config.getBlueZone()));
        } else {
            equipeRouge.add(joueur);
            joueur.sendMessage(Message.join(
                Message.raw("Vous avez rejoint l'équipe ").color(Color.WHITE),
                Message.raw("Rouge").color(Color.RED),
                Message.raw(" !").color(Color.WHITE)
            ));
            teleporterJoueur(joueur, config.getBoxCenter(config.getRedZone()));
        }
    }

    public void retirerJoueur(Player joueur) {
        equipeBleue.remove(joueur);
        equipeRouge.remove(joueur);
    }

    /**
     * Téléporte le joueur à la base de son équipe actuelle.
     * @param joueur Le joueur à téléporter.
     */
    public void teleporterAuSpawn(Player joueur) {
        GameConfig config = ExperienceMod.get().getConfigManager().getConfig();
        if (equipeBleue.contains(joueur)) {
            teleporterJoueur(joueur, config.getBoxCenter(config.getBlueZone()));
        } else if (equipeRouge.contains(joueur)) {
            teleporterJoueur(joueur, config.getBoxCenter(config.getRedZone()));
        }
    }

    private void teleporterJoueur(Player joueur, Vector3d position) {
        HytaleUtils.teleporterJoueur(joueur, position);
    }

    public boolean estDansEquipe(Player joueur, String equipe) {
        if ("Bleue".equalsIgnoreCase(equipe)) {
            return equipeBleue.contains(joueur);
        } else if ("Rouge".equalsIgnoreCase(equipe)) {
            return equipeRouge.contains(joueur);
        }
        return false;
    }

    public boolean sontDansLaMemeEquipe(Player joueur1, Player joueur2) {
        if (equipeBleue.contains(joueur1) && equipeBleue.contains(joueur2)) return true;
        if (equipeRouge.contains(joueur1) && equipeRouge.contains(joueur2)) return true;
        return false;
    }

    public List<Player> getEquipeBleue() { return equipeBleue; }
    public List<Player> getEquipeRouge() { return equipeRouge; }
}
