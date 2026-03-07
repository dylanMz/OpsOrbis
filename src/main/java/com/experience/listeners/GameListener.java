package com.experience.listeners;

import com.experience.game.GameManager;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.entities.Player;
// import com.hypixel.hytale.event.Subscribe;
// import com.hypixel.hytale.server.core.modules.entity.damage.DamageEvent; 
// import com.hypixel.hytale.server.core.modules.entity.damage.DeathEvent;
// TODO: Utiliser les imports exacts de Hytale Server 2026.02.19 pour les dégâts.

public class GameListener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    // --- INTERCEPTION DES DÉGÂTS ---
    /*
    @Subscribe
    public void onEntityDamage(DamageEvent event) {
        if (gameManager.getEtatActuel() != GameManager.GameState.EN_COURS) {
            return;
        }

        Entity target = event.getTarget();
        Entity attacker = event.getAttacker(); // (Peut être nul si dégâts de chute etc)

        if (attacker instanceof Player) {
            Player pAttacker = (Player) attacker;

            // 1) Désactiver le tir ami (Friendly fire)
            if (target instanceof Player) {
                Player pTarget = (Player) target;
                if (gameManager.getTeamManager().sontDansLaMemeEquipe(pAttacker, pTarget)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // 2) Gérer les dégâts sur les PNJ
            if (gameManager.getNpcManager() != null) {
                if (gameManager.getNpcManager().estPnjBleu(target)) {
                    // Empêcher l'équipe bleue de taper son propre PNJ
                    if (gameManager.getTeamManager().getEquipeBleue().contains(pAttacker)) {
                        event.setCancelled(true);
                        pAttacker.sendMessage("§cNe frappez pas le PNJ de votre équipe !");
                    } else {
                        // Alerter que le PNJ se fait attaquer
                        gameManager.diffuserMessage("§c[Alerte] Le PNJ Bleu se fait attaquer !");
                    }
                }
                else if (gameManager.getNpcManager().estPnjRouge(target)) {
                    // Empêcher l'équipe rouge de taper son propre PNJ
                    if (gameManager.getTeamManager().getEquipeRouge().contains(pAttacker)) {
                        event.setCancelled(true);
                        pAttacker.sendMessage("§cNe frappez pas le PNJ de votre équipe !");
                    } else {
                        // Alerter que le PNJ se fait attaquer
                        gameManager.diffuserMessage("§c[Alerte] Le PNJ Rouge se fait attaquer !");
                    }
                }
            }
        }
    }
    */

    // --- INTERCEPTION DE LA MORT ---
    /*
    @Subscribe
    public void onEntityDeath(DeathEvent event) {
        if (gameManager.getEtatActuel() != GameManager.GameState.EN_COURS) {
            return;
        }

        Entity mort = event.getEntity();

        // 1) Vérification de la mort des PNJ (Condition de victoire)
        if (gameManager.getNpcManager() != null) {
            if (gameManager.getNpcManager().estPnjBleu(mort)) {
                // Le PNJ Bleu est mort, l'équipe Rouge gagne
                gameManager.terminerPartie("Rouge");
            } 
            else if (gameManager.getNpcManager().estPnjRouge(mort)) {
                // Le PNJ Rouge est mort, l'équipe Bleue gagne
                gameManager.terminerPartie("Bleue");
            }
        }

        // 2) Respawn du joueur avec son kit
        if (mort instanceof Player) {
            Player pMort = (Player) mort;
            // TODO: Programmer le respawn du joueur avec un délai
            // pMort.respawn(gameManager.getTeamManager().getSpawnRouge / Bleu);
            // gameManager.getKitManager().donnerEquipement(pMort);
        }
    }
    */
}
