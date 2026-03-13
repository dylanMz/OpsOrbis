package com.opsorbis.game.logic;

import com.opsorbis.OpsOrbis;
import com.opsorbis.config.LangManager;
import com.opsorbis.config.MapConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.math.vector.Vector3d;
import com.opsorbis.utils.HytaleUtils;
import java.awt.Color;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère la répartition des joueurs dans deux équipes persistantes (Equipe 1 et Equipe 2).
 * Le rôle (Attaquant/Défenseur) de chaque équipe change dynamiquement à la mi-temps.
 */
public class TeamManager {

    private final List<Player> equipe1 = new CopyOnWriteArrayList<>();
    private final List<Player> equipe2 = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<UUID, Player> uuidMap = new ConcurrentHashMap<>();
    private boolean equipe1EstAttaquant; // true = Eq1 est Attaquant, false = Eq1 est Défenseur
    private int scoreEquipe1;
    private int scoreEquipe2;

    /**
     * Retourne le nombre total de joueurs inscrits dans les deux équipes.
     * @return Le nombre total de joueurs.
     */
    public int getNombreTotalJoueurs() {
        return equipe1.size() + equipe2.size();
    }
    
    /**
     * Compte le nombre de joueurs actuellement connectés au monde.
     * @return Le nombre de joueurs actifs.
     */
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
        
        // Mise à jour du cache (doit être fait sur le thread du monde)
        UUID uuid = HytaleUtils.getPlayerUuid(joueur);
        if (uuid != null) uuidMap.put(uuid, joueur);

        // Annonce du rôle actuel au joueur et téléportation
        informerEtTeleporterSpawner(joueur);
    }

    /**
     * Envoie un message au joueur pour lui rappeler son rôle et le téléporte à son spawn.
     * Utilisé lors de la connexion ou à chaque début de manche.
     */
    public void informerEtTeleporterSpawner(Player joueur) {
        MapConfig config = OpsOrbis.get().getConfigManager().getMapConfig();
        PlayerRole role = getRole(joueur);

        LangManager lang = OpsOrbis.get().getLangManager();
        if (role == PlayerRole.ATTAQUANT) {
            joueur.sendMessage(lang.get(joueur, "prefix"));
            joueur.sendMessage(lang.get(joueur, "team_assign_attacker"));
            teleporterJoueur(joueur, config.getBoxCenter(config.getBlueZone()));
        } else if (role == PlayerRole.DEFENSEUR) {
            joueur.sendMessage(lang.get(joueur, "prefix"));
            joueur.sendMessage(lang.get(joueur, "team_assign_defender"));
            teleporterJoueur(joueur, config.getBoxCenter(config.getRedZone()));
        }
    }

    /**
     * Retire un joueur de son équipe actuelle.
     * @param joueur Le joueur à retirer.
     */
    public void retirerJoueur(Player joueur) {
        if (joueur == null) return;
        
        UUID uuid = HytaleUtils.getPlayerUuid(joueur);
        if (uuid != null) {
            Player oldInstance = uuidMap.remove(uuid);
            equipe1.removeIf(p -> p == oldInstance || p == joueur);
            equipe2.removeIf(p -> p == oldInstance || p == joueur);
        } else {
            // Fallback sur l'instance si l'UUID est introuvable
            equipe1.remove(joueur);
            equipe2.remove(joueur);
        }
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
     * @param role Le rôle à vérifier.
     */
    public boolean estDansEquipe(Player joueur, PlayerRole role) {
        if (role == PlayerRole.ATTAQUANT) return contientJoueur(getEquipeAttaquants(), joueur);
        if (role == PlayerRole.DEFENSEUR) return contientJoueur(getEquipeDefenseurs(), joueur);
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
    
    /**
     * Inverse les rôles (Attaquant/Défenseur) des deux équipes.
     * Utilisé généralement à la mi-temps.
     */
    public void inverserRoles() {
        this.equipe1EstAttaquant = !this.equipe1EstAttaquant;
    }

    /**
     * Ajoute un point au score de l'équipe qui a remporté la manche.
     * @param roleVainqueurRound Le rôle vainqueur.
     */
    public void ajouterPointEquipe(PlayerRole roleVainqueurRound) {
        if (PlayerRole.ATTAQUANT == roleVainqueurRound) {
            if (equipe1EstAttaquant) scoreEquipe1++; else scoreEquipe2++;
        } else if (PlayerRole.DEFENSEUR == roleVainqueurRound) {
            if (equipe1EstAttaquant) scoreEquipe2++; else scoreEquipe1++;
        }
    }

    /** @return Le score actuel de l'Équipe 1. */
    public int getScoreEquipe1() { return scoreEquipe1; }
    
    /** @return Le score actuel de l'Équipe 2. */
    public int getScoreEquipe2() { return scoreEquipe2; }
    
    /**
     * Réinitialise les scores à zéro et remet l'Équipe 1 en position d'Attaquant.
     */
    public void resetScoresEtRoles() {
        this.scoreEquipe1 = 0;
        this.scoreEquipe2 = 0;
        this.equipe1EstAttaquant = true;
    }

    /**
     * Retire tous les joueurs des deux équipes.
     */
    public void viderEquipes() {
        this.equipe1.clear();
        this.equipe2.clear();
        this.uuidMap.clear();
    }

    // Compatibilité avec l'ancien code existant pour préserver les signatures si besoin
    /** @return Liste des joueurs de l'équipe Attaquante actuelle. */
    public List<Player> getEquipeBleue() { return getEquipeAttaquants(); }
    
    /** @return Liste des joueurs de l'équipe Défenseuse actuelle. */
    public List<Player> getEquipeRouge() { return getEquipeDefenseurs(); }

    /**
     * Vérifie de manière robuste si un joueur est dans une liste.
     */
    private boolean contientJoueur(List<Player> equipe, Player joueur) {
        if (joueur == null) return false;
        UUID uuid = HytaleUtils.getPlayerUuid(joueur);
        if (uuid == null) return equipe.contains(joueur);
        
        Player mappedInstance = uuidMap.get(uuid);
        for (Player p : equipe) {
            if (p == joueur || (mappedInstance != null && p == mappedInstance)) return true;
        }
        return false;
    }

    /**
     * Vérifie si un joueur est inscrit dans l'une des deux équipes.
     * @param joueur Le joueur à vérifier.
     * @return true si le joueur est dans une équipe.
     */
    public boolean isJoueurDansMatch(Player joueur) {
        return contientJoueur(equipe1, joueur) || contientJoueur(equipe2, joueur);
    }

    /**
     * Retourne le rôle actuel (Attaquant/Defenseur) d'un joueur.
     * @param player Le joueur.
     * @return Le rôle.
     */
    public PlayerRole getRole(Player player) {
        if (estDansEquipe(player, PlayerRole.ATTAQUANT)) return PlayerRole.ATTAQUANT;
        if (estDansEquipe(player, PlayerRole.DEFENSEUR)) return PlayerRole.DEFENSEUR;
        return PlayerRole.AUCUN;
    }

    /**
     * Recherche un joueur dans les équipes à partir de sa référence.
     */
    public Player getJoueurParUUID(UUID uuid) {
        if (uuid == null) return null;
        return uuidMap.get(uuid);
    }

    public Player getJoueurParRef(PlayerRef ref) {
        if (ref == null) return null;
        return getJoueurParUUID(ref.getUuid());
    }

    /**
     * Remplace l'ancienne instance d'un joueur par la nouvelle après reconnexion.
     */
    public void mettreAJourJoueur(Player nouveauJoueur) {
        if (nouveauJoueur == null) return;
        UUID uuid = HytaleUtils.getPlayerUuid(nouveauJoueur);
        if (uuid == null) return;
        
        // On récupère l'ancienne instance avant de mettre à jour la map
        Player oldJoueur = uuidMap.get(uuid);
        
        // Mise à jour du cache
        uuidMap.put(uuid, nouveauJoueur);

        // Si on a trouvé une ancienne instance, on la remplace chirurgicalement dans les listes
        if (oldJoueur != null) {
            for (int i = 0; i < equipe1.size(); i++) {
                if (equipe1.get(i) == oldJoueur) {
                    equipe1.set(i, nouveauJoueur);
                }
            }
            for (int i = 0; i < equipe2.size(); i++) {
                if (equipe2.get(i) == oldJoueur) {
                    equipe2.set(i, nouveauJoueur);
                }
            }
        }
    }

    /**
     * Vérifie si l'Équipe 1 est actuellement l'équipe attaquante.
     * @return true si Eq1 attaque.
     */
    public boolean isEquipe1Attaquant() {
        return equipe1EstAttaquant;
    }
}
