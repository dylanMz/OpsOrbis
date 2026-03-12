# Ops Orbis - Hytale Minigame Mod

[English](#english) | [Français](#français)

---

<a name="english"></a>
## 🇺🇸 English

### 🏁 Game Objective
**Ops Orbis** is a Hytale minigame featuring two teams: **Attackers** and **Defenders**.

*   **Attacker Goal:** Break into the enemy base, retrieve the **2 relics**, and bring them back to your deposit zone.
*   **Defender Goal:** Protect the relics and prevent attackers from capturing them until the timer runs out.
*   **Relics:** When a carrier dies, the relic is dropped on the spot.
    *   An **Attacker** can pick it up to continue the objective.
    *   A **Defender** can instantly return it to base by simply walking over it.
*   **NPCs:** Two NPC guards assist the defenders in protecting their territory.

### 🎮 Game Commands
*   `/oorbis join`: Join the game (auto-assigns a team).
*   `/oorbis role <melee|distance>`: Choose your combat archetype.
*   `/oorbis kit <guerrier|assassin|archer|arbaletrier>`: Choose specific equipment (depends on selected role).
*   `/oorbis start`: Manually start the match (requires players).
*   `/oorbis stop`: (Admin) Force stop the game and reset players/world.

### ⚙️ Configuration (Admin)
Configuration relies on player position or HyUI selections.
*   `/oorbis config setzone <attaquant|defenseur>`: Defines a team's spawn zone (via selection).
*   `/oorbis config setrelic <1|2>`: Sets a relic's spawn position (at your location).
*   `/oorbis config setnpcspawn <1|2>`: Sets an NPC guard's spawn point (at your location).
*   `/oorbis config setdeposit`: Defines the zone where attackers must bring relics (via selection).
*   `/oorbis config save`: **Essential** to save all changes to the configuration file.

---

<a name="français"></a>
## 🇫🇷 Français

### 🏁 Principe du Jeu
**Ops Orbis** est un mini-jeu Hytale opposant deux équipes : les **Attaquants** et les **Défenseurs**.

*   **Objectif des Attaquants :** Pénétrer dans la base ennemie, récupérer les **2 reliques** et les ramener dans leur zone de dépôt.
*   **Objectif des Défenseurs :** Protéger les reliques et empêcher les attaquants de les capturer jusqu'à la fin du temps imparti.
*   **Les Reliques :** Lorsqu'un porteur de relique meurt, la relique tombe au sol. 
    *   Un **Attquant** peut la ramasser pour continuer la progression.
    *   Un **Défenseur** peut la renvoyer instantanément à sa base en passant simplement dessus.
*   **PNJs :** Deux gardes PNJs aident les défenseurs à protéger leur territoire.

### 🎮 Commandes de Jeu
*   `/oorbis join` : Rejoindre la partie (assigne une équipe automatiquement).
*   `/oorbis role <melee|distance>` : Choisir votre archétype de combat.
*   `/oorbis kit <guerrier|assassin|archer|arbaletrier>` : Choisir un équipement spécifique (dépend du rôle).
*   `/oorbis start` : Lancer manuellement le match (nécessite des joueurs).
*   `/oorbis stop` : (Admin) Arrêter la partie et réinitialiser le monde/joueurs.

### ⚙️ Configuration (Admin)
La configuration utilise les sélections HyUI ou la position du joueur.
*   `/oorbis config setzone <attaquant|defenseur>` : Définit la zone de spawn d'une équipe (via sélection).
*   `/oorbis config setrelic <1|2>` : Définit la position d'apparition d'une relique (votre position).
*   `/oorbis config setnpcspawn <1|2>` : Définit le point de spawn des gardes PNJs (votre position).
*   `/oorbis config setdeposit` : Définit la zone où les attaquants doivent ramener les reliques (via sélection).
*   `/oorbis config save` : **Indispensable** pour sauvegarder les changements dans le fichier de config.
