package com.experience.game;

import com.experience.ExperienceMod;
import com.experience.config.GameConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
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

    private void teleporterJoueur(Player joueur, Vector3d position) {
        World monde = joueur.getWorld();
        if (monde != null) {
            monde.execute(() -> {
                monde.getEntityStore().getStore().addComponent(
                    joueur.getReference(), 
                    Teleport.getComponentType(), 
                    Teleport.createForPlayer(position, new Vector3f(0, 0, 0))
                );
            });
        }
    }

    public boolean estDansEquipe(Player joueur, String equipe) {
        if ("Bleue".equalsIgnoreCase(equipe)) {
            return equipeBleue.contains(joueur);
        } else if ("Rouge".equalsIgnoreCase(equipe)) {
            return equipeRouge.contains(joueur);
        }
        return false;
    }

    public List<Player> getEquipeBleue() { return equipeBleue; }
    public List<Player> getEquipeRouge() { return equipeRouge; }
}
