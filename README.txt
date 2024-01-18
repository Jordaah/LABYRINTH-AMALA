--------------------------
/!\ À LIRE EN PRIORITÉ /!\
--------------------------

-- LABYRINTH OF AMALA/アマラ深界 --

=== SCÉNARIO ===

LABYRINTH OF AMALA est un jeu de rôle/RPG inspiré
des premiers jeux de la licence "Shin Megami Tensei".

Le joueur doit parcourir de nombreux labyrinthes,
ainsi qu'affronter divers ennemis avant d'arriver au
trône du tyran Amala.

Pour infliger des dégâts, le joueur doit répondre
correctement à des questions de culture générale.

=== MODES ===

Le jeu possède deux modes disponibles:

- MODE HISTOIRE: Le joueur est plongé dans le château 
d'Amala, et doit parcourir 20 étages avant d'arriver à la 
confrontation finale.

- MODE SANS FIN: Le joueur doit parcourir le plus
d'étages que possible avant qu'il n'aie plus de vies. 

=== INTERFACES ===

Le jeu est disponible via interface textuelle,
ainsi que graphique.

Voici les recommendations pour une
bonne expérience de jeu:
(* = OBLIGATOIRE)

GÉNÉRAL:
- Système d'exploitation Linux

MODE TEXTUEL:
- * Terminal en plein écran
- * Au moins 166 colonnes et 35 lignes (voir commande tput)
- * Police mono (recommandé: JetBrains Mono), taille 10 (peut varier selon la taille de l'écran)
- * Terminal avec support de couleurs ANSI et RGB

MODE GRAPHIQUE:
- * Écran de résolution minimum 1280*600
- * Police JetBrains Mono installée

Le jeu a été testé sur:

== Lenovo ThinkPad X220 // AUCUN PROBLÈME ==
- écran 12.5"
- résolution 1366*768
- système Linux: openSUSE Tumbleweed
- terminal Xfce
- police JetBrains Mono Regular, taille 10
- 166 colonnes, 35 lignes
- OpenJDK 21

=== CONTRÔLES ===

Pour le mode textuel, la saisie est demandée par le jeu avec le caractère ">".
Entrez la touche désirée puis appuyez sur entrée.

Pour le mode graphique, cliquez sur la partie principale, puis utilisez vos touches normalement.
La partie d'entrée du bas n'est pas nécessaire.

Répondre aux questions: 1, 2, 3, 4 (propositions)
Se déplacer dans le labyrinthe: Z (haut), Q (gauche), S (bas), D (droite)
Passer une cinématique (UNIQUEMENT MODE GRAPHIQUE): p

=== SPRITES ENNEMIS ===

À cause du poids conséquent des images des ennemis pour le mode graphique (~500MB),
les sprites n'ont pas été inclus dans le .zip présent sur Moodle.

Les modes de jeux graphiques ne démarreront pas tant que les sprites ne sont pas présents dans le dossier
resources/sprites.

Pour télécharger les sprites, veuillez cloner la repo GitHub du projet avec git:
=> https://github.com/nachtreiher/LABYRINTHAMALA
