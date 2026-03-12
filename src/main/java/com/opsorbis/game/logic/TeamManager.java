package com.opsorbis.game.logic;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.GameConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import com.opsorbis.utils.HytaleUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Gère la répartition des joueurs dans deux équipes persistantes (Equipe 1 et Equipe 2).
 * Le rôle (Attaquant/Défenseur) de chaque équipe change dynamiquement à la mi-temps.
 */
public class TeamManager {

    private final List<Player> equipe1;
    private final List<Player> equipe2;
    private boolean equipe1EstAttaquant; // true = Eq1 est Attaquant, false = Eq1 est Défenseur
    private int scoreEquipe1;
    private int scoreEquipe2;

    public int getNombreTotalJoueurs() {
        return equipe1.size() + equipe2.size();
    }
    
    public int countConnectedPlayers() {
        int count = 0;
        for (Player p : equipe1) {
            if (p != null && p.getWorld() != null) count++;
        }
        for (Player p : equipe2) {
            if (p != null && p.getWorld() != null) count++;
        }
        return count;
    }

    public TeamManager() {
        this.equipe1 = new ArrayList<>();
        this.equipe2 = new ArrayList<>();
        this.equipe1EstAttaquant = true; // Par défaut, l'équipe 1 commence en attaque
        this.scoreEquipe1 = 0;
        this.scoreEquipe2 = 0;
    }

    /**
     * Ajoute un joueur à une équipe (équilibrage automatique) s'il n'en a pas déjà une.
     * @param joueur Le joueur à intégrer.
     */
    public void ajouterJoueur(Player joueur) {
        if (contientJoueur(equipe1, joueur) || contientJoueur(equipe2, joueur)) return;

        // Équilibrage : equipe 1 d'abord si égaux
        if (equipe1.size() <= equipe2.size()) {
            equipe1.add(joueur);
        } else {
            equipe2.add(joueur);
        }
        
        // Annonce du rôle actuel au joueur et téléportation
        informerEtTeleporterSpawner(joueur);
    }

    /**
     * Envoie un message au joueur pour lui rappeler son rôle et le téléporte à son spawn.
     * Utilisé lors de la connexion ou à chaque début de manche.
     */
    public void informerEtTeleporterSpawner(Player joueur) {
        GameConfig config = OpsOrbis.get().getConfigManager().getConfig();

        if (estDansEquipe(joueur, "Attaquant")) {
            joueur.sendMessage(Message.join(
                Message.raw("Vous êtes ").color(Color.WHITE),
                Message.raw("Attaquant").color(new Color(255, 160, 0)),
                Message.raw(" ! Volez les 2 reliques des défenseurs.").color(Color.WHITE)
            ));
            teleporterJoueur(joueur, config.getBoxCenter(config.getBlueZone()));
        } else if (estDansEquipe(joueur, "Defenseur")) {
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
        equipe1.removeIf(p -> p != null && java.util.Objects.equals(p.getReference(), joueur.getReference()));
        equipe2.removeIf(p -> p != null && java.util.Objects.equals(p.getReference(), joueur.getReference()));
    }

    /**
     * Téléporte le joueur au spawn de son équipe ACTUELLE (Attaquant ou Défenseur).
     * @param joueur Le joueur à téléporter.
     */
    public void teleporterAuSpawn(Player joueur) {
        informerEtTeleporterSpawner(joueur);
    }

    private void teleporterJoueur(Player joueur, Vector3d position) {
        HytaleUtils.teleporterJoueur(joueur, position);
    }

    /**
     * Vérifie si un joueur a le rôle spécifié sur LA MANCHE EN COURS.
     * @param joueur Le joueur.
     * @param role "Attaquant" ou "Defenseur"
     */
    public boolean estDansEquipe(Player joueur, String role) {
        if ("Attaquant".equalsIgnoreCase(role)) return contientJoueur(getEquipeAttaquants(), joueur);
        if ("Defenseur".equalsIgnoreCase(role)) return contientJoueur(getEquipeDefenseurs(), joueur);
        return false;
    }

    /**
     * Vérifie si deux joueurs sont dans la même équipe permanente.
     */
    public boolean sontDansLaMemeEquipe(Player joueur1, Player joueur2) {
        if (contientJoueur(equipe1, joueur1) && contientJoueur(equipe1, joueur2)) return true;
        if (contientJoueur(equipe2, joueur1) && contientJoueur(equipe2, joueur2)) return true;
        return false;
    }

    /**
     * Renvoie dynamiquement la liste des joueurs jouant l'Attaque en ce moment.
     */
    public List<Player> getEquipeAttaquants() {
        return equipe1EstAttaquant ? equipe1 : equipe2;
    }

    /**
     * Renvoie dynamiquement la liste des joueurs jouant la Défense en ce moment.
     */
    public List<Player> getEquipeDefenseurs() {
        return equipe1EstAttaquant ? equipe2 : equipe1;
    }

    // Gestion du match global (Changement de camp & Scores)
    
    public void inverserRoles() {
        this.equipe1EstAttaquant = !this.equipe1EstAttaquant;
    }

    public void ajouterPointEquipe(String roleVainqueurRound) {
        if ("Attaquant".equalsIgnoreCase(roleVainqueurRound)) {
            if (equipe1EstAttaquant) scoreEquipe1++; else scoreEquipe2++;
        } else {
            if (equipe1EstAttaquant) scoreEquipe2++; else scoreEquipe1++;
        }
    }

    public int getScoreEquipe1() { return scoreEquipe1; }
    public int getScoreEquipe2() { return scoreEquipe2; }
    
    public void resetScoresEtRoles() {
        this.scoreEquipe1 = 0;
        this.scoreEquipe2 = 0;
        this.equipe1EstAttaquant = true;
    }

    // Compatibilité avec l'ancien code existant pour préserver les signatures si besoin
    public List<Player> getEquipeBleue() { return getEquipeAttaquants(); }
    public List<Player> getEquipeRouge() { return getEquipeDefenseurs(); }

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

    public String getRole(Player player) {
        return estDansEquipe(player, "Attaquant") ? "Attaquant" : "Defenseur";
    }

    /**
     * Recherche un joueur dans les équipes à partir de sa référence.
     */
    public Player getJoueurParUUID(UUID uuid) {
        if (uuid == null) return null;
        for (Player p : equipe1) {
            if (p != null && p.getUuid().equals(uuid)) return p;
        }
        for (Player p : equipe2) {
            if (p != null && p.getUuid().equals(uuid)) return p;
        }
        return null;
    }

    public Player getJoueurParRef(PlayerRef ref) {
        if (ref == null) return null;
        for (Player p : equipe1) {
            if (p != null && p.getPlayerRef().equals(ref)) return p;
        }
        for (Player p : equipe2) {
            if (p != null && p.getPlayerRef().equals(ref)) return p;
        }
        return null;
    }

    /**
     * Remplace l'ancienne instance d'un joueur par la nouvelle après reconnexion.
     */
    public void mettreAJourJoueur(Player nouveauJoueur) {
        if (nouveauJoueur == null) return;
        UUID uuid = nouveauJoueur.getUuid();
        
        for (int i = 0; i < equipe1.size(); i++) {
            Player p = equipe1.get(i);
            if (p != null && p.getUuid().equals(uuid)) {
                equipe1.set(i, nouveauJoueur);
                return;
            }
        }
        for (int i = 0; i < equipe2.size(); i++) {
            Player p = equipe2.get(i);
            if (p != null && p.getUuid().equals(uuid)) {
                equipe2.set(i, nouveauJoueur);
                return;
            }
        }
    }

    public boolean isEquipe1Attaquant() {
        return equipe1EstAttaquant;
    }
}
