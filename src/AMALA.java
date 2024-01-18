/*
  
	LABYRINTH OF AMALA / アマラ深界 est un jeu de rôle/RPG
	inspiré des premiers jeux de la licence japonaise
	"Shin Megami Tensei".
  
	Le joueur doit parcourir plusieurs étages aménagés
	comme des labyrinthes, pour espérer arriver au trône
	du tyran Amala dans le but de le vaincre.
  
	Dans les labyrinthes, il peut arriver au joueur de
	tomber sur des ennemis. Pour infliger des dégâts,
	le joueur doit répondre correctement à des questions
	sur diverses matières, telles que l'histoire,
	la géographie, etc.
  
	Le jeu possède deux modes:
	- Un mode "histoire", où le joueur doit parcourir une
	dizaine d'tages avant d'atteindre la salle du trône.
	- Un mode "sans fin", où le but est de parcourir le plus
	d'étages que possible.
*/

import extensions.*;
import java.io.IOException;

class AMALA extends Program {
    // important variables
    final int WINDOW_WIDTH = 1280;
    final int WINDOW_HEIGHT = 600;
    final int LABYRINTH_WIDTH = 10; // x axis VALUES ARE INVERTED
    final int LABYRINTH_HEIGHT = 25; // y axis
    final int PLAYER_LIVES = 10;
    final int MAX_ENCOUNTER_SEED = 100; // choix arbitraire
    final double CRITICAL_PERCENTAGE = 0.05;
    final double FONT_HEIGHT_VALUE = 1.3201; // trouvé après des calculs
    final double FONT_WIDTH_VALUE = 0.6001; // trouvé après des calculs
    final char NEWLINE = '\n';
    final char BACKSPACE = '\b';
    final String SPRITE_FOLDER = "resources/sprites/";
    final String TABLE_FOLDER = "resources/tables/";
    final String CLEAR_SCREEN = "\033\143";
    // gui/tui states and vars
    boolean GAMESTATE = false;
    boolean MAINMENU = false;
    boolean CONSULTATION_LDB = false;
    boolean CONSULTATION_PAGE = false;
    boolean ENDLESS = false;
    boolean STORYMODE = false;
    boolean NAME_ENTRY = false;
    boolean ENTRY = false;
    boolean ACTION = false;
    boolean COMBAT = false;
    boolean ACTIONCOMBAT = false;
    boolean PARTIEFINIE = false;
    boolean REPONSE = false;
    boolean CRITICAL = false;
    boolean MIDBOSS = false;
    boolean CUTSCENE = false;
    boolean BONUS = false;
    boolean AMALA = false;
    int PAGELDB = 1;
    int ETAGE_STORYMODE = 1;
    int ETAGE_ENDLESS = 1;
    int MODEVALUE = 0;
    int SCORE_ENDLESS = 0;
    String name = "";
    String[][] LDB = sortedLeaderboard(20);
    Image img;
    Joueur j = nouveauJoueur();
    Labyrinthe l = nouveauLabyrinthe(LABYRINTH_HEIGHT, LABYRINTH_WIDTH);
    Question question = nouvelleQuestion();
    Monstre monstre = nouveauMonstre(false);

    /*
      Création d'un nouveau labyrinthe.
    */
    Labyrinthe nouveauLabyrinthe(int longueur, int largeur) {
        Labyrinthe l = new Labyrinthe();
        l.longueur = longueur;
        l.largeur = largeur;
        l.labyrinthe = new int[l.longueur][l.largeur];
        l.decouvert = new boolean[l.longueur][l.largeur];
        genererLabyrinthe(l, 0, 0);
        casesDecouvertes(l);
        return l;
    }

    /*
      Création d'un nouveau monstre.
    */
    Monstre nouveauMonstre(boolean amala) {
        Monstre m = new Monstre();
        CSVFile enemytable = loadCSV(TABLE_FOLDER + "ENNEMIS.csv", ';');
        int index;
        if (amala) {
            index = 140;
            m.hpmax = stringToInt(getCell(enemytable, index, 1));
        }
        else if (MIDBOSS) {
            enemytable = loadCSV(TABLE_FOLDER + "MIDBOSS.csv", ';');
            index = ((ETAGE_STORYMODE / 5) - 1);
            m.hpmax = stringToInt(getCell(enemytable, index, 1));
        }
        else if (STORYMODE) {
            int min = (ETAGE_STORYMODE - 1) * 7;
            int max = (min + (6));
            index = random(min, max);
            m.hpmax = stringToInt(getCell(enemytable, index, 1));
        }
        else {
            index = random(0, (rowCount(enemytable) - 2));
            m.hpmax = (ETAGE_ENDLESS * 30);
        }
        m.nom = getCell(enemytable, index, 0);
        m.sprite = "";
        extensions.File ascii = newFile("resources/ascii/" + m.nom + ".txt");
        while (ready(ascii)) {
            m.sprite += correctASCII(readLine(ascii)) + NEWLINE;
        }
        return m;
    }

    /*
      Création d'une nouvelle question.
    */
    Question nouvelleQuestion() {
        Question q = new Question();
        CSVFile questiontable = loadCSV(TABLE_FOLDER + "QUESTIONS.csv", ';');
        int index = random(0, (rowCount(questiontable) - 1));
        q.enonce = getCell(questiontable, index, 0);
        q.propositions = new String[4];
        for (int idx = 0; idx < length(q.propositions); idx++) {
            q.propositions[idx] = getCell(questiontable, index, (idx + 1));
        }
        q.reponse = charAt(getCell(questiontable, index, 5), 0);
        q.idxreponse = stringToInt(getCell(questiontable, index, 5));
        return q;
    }

    /*
      Initialisation des cases découvertes d'un
      nouveau labyrinthe.
     */
    void casesDecouvertes(Labyrinthe l) {
        for (int idx = 0; idx < l.largeur; idx++) {
            for (int jdx = 0; jdx < l.longueur; jdx++ ){
                l.decouvert[jdx][idx] = false;
            }
        }
        l.decouvert[0][0] = true;
    }

    /*
      Création d'un nouveau joueur.
    */
    Joueur nouveauJoueur() {
        Joueur j = new Joueur();
        j.vies = PLAYER_LIVES;
        j.encounter_seed = 0;
        j.maxatk = 20;
        j.minatk = 5;
        return j;
    }

    /*
      Réinitialisation des statistiques
      d'attaque du joueur.
     */
    void resetATK() {
        j.maxatk = 20;
        j.minatk = 5;
    }

    /*
      Réinitialisation des coordonnées du joueur
      dans le labyrinthe.
    */
    void resetPositions() {
        j.posx = 0;
        j.posy = 0;
    }

    /*
      Génère un nombre aléatoire entre min (inclus)
      et max (inclus).
    */
    int random(int min, int max) {
        return (int) (random() * (max - min + 1)) + min;
    }

    /*
      Affichage graphique en topview du
      labyrinthe complet.
    */
    String affichage(Labyrinthe l) {
        String display = "";
        for (int idx = 0; idx < l.largeur; idx++) {
            for (int jdx = 0; jdx < l.longueur; jdx++) {
                if (idx == 0 && jdx == 0) {
                    display += "+   ";
                }
                else {
                    display += ((l.labyrinthe[jdx][idx] & 1) == 0 ? "+---" : "+   ");
                }
            }
            display += "+" + NEWLINE;
            for (int jdx = 0; jdx < l.longueur; jdx++) {
                if (idx == j.posy && jdx == j.posx) {
                    display += ((l.labyrinthe[jdx][idx] & 8) == 0 ? "| @ " : "  @ " );
                }
                else {
                    display += ((l.labyrinthe[jdx][idx] & 8) == 0 ? "|   " : "    " );
                }
            }
            display += "|" + NEWLINE;
        }
        for (int jdx = 0; jdx < l.longueur; jdx++) {
            if (jdx != (l.longueur - 1)) {
                display += "+---";
            }
            else {
                display += "+   ";
            }
        }
        return display + "+" + NEWLINE;
    }

    /*
      Affichage partiel du labyrinthe selon les
      cases découvertes par le joueur.
    */
    String affichageDecouvert(Labyrinthe l) {
        String display = "";
        for (int idx = 0; idx < l.largeur; idx++) {
            for (int jdx = 0; jdx < l.longueur; jdx++) {
                if (idx == 0 && jdx == 0) {
                    display += "+   ";
                }
                else {
                    display += (((l.labyrinthe[jdx][idx] & 1) == 0 && (l.decouvert[jdx][idx] == true))
                                || (idx == 0) ? "+---" : "+   ");
                }
            }
            display += "+" + NEWLINE;
            for (int jdx = 0; jdx < l.longueur; jdx++) {
                if (idx == j.posy && jdx == j.posx) {
                    display += ((l.labyrinthe[jdx][idx] & 8) == 0 ? "| @ " : "  @ " );
                }
                else {
                    display += (((l.labyrinthe[jdx][idx] & 8) == 0 && (l.decouvert[jdx][idx] == true))
                                || (jdx == 0) ? "|   " : "    " );
                }
            }
            display += "|" + NEWLINE;
        }
        for (int jdx = 0; jdx < l.longueur; jdx++) {
            if (jdx != (l.longueur - 1)) {
                display += "+---";
            }
            else {
                display += "+   ";
            }
        }
        return display + "+" + NEWLINE;
    }

    /*
      Rend découvertes toutes les cases adjacentes
      à celle où se trouve le joueur.
    */
    void decouverteAdjacentes(Labyrinthe l) {
        for (int idx = (j.posx > 0) ? -1 : 0; idx <= ((j.posx < (l.longueur - 1)) ? 1 : 0); ++idx) {
            for (int jdx = (j.posy > 0 ? -1 : 0); jdx <= (j.posy < (l.largeur - 1) ? 1 : 0); ++jdx) {
                if (idx != 0 || jdx != 0) {
                    l.decouvert[j.posx + idx][j.posy + jdx] = true;
                }
            }
        }
    }
    
    /*
      Affichage du labyrinthe en mode DEBUG des
      nombres selon les directions cardinales.
    */
    String affichageDEBUG(Labyrinthe l) {
        String debug = "";
        for (int idx = 0; idx < l.largeur; idx++) {
            debug += ("| ");
            for (int jdx = 0; jdx < l.longueur; jdx++) {
                debug += (l.labyrinthe[jdx][idx] + " | ");
            }
            debug += NEWLINE;
        }
        return debug;
    }

    /*
      Retourne les directions cardinales ouvertes
      selon une case du labyrinthe donnée.
    */
    String getDirections(int valeur, boolean keys) {
        String cardinals = "";
        DIRS[] directions = DIRS.values();
        for (DIRS direction : directions) {
            if ((valeur & direction.bit) != 0) {
                if (keys) {
                    cardinals += direction.key;
                }
                else {
                    cardinals += direction.name();
                }
            }
        }
        return cardinals;
    }

    /*
      Sauvegarde un labyrinthe donné dans le fichier
      LABYRINTHES.csv.
    */
    void sauvegarderLabyrinthe(Labyrinthe l) {
        CSVFile table = loadCSV("resources/tables/LABYRINTHES.csv");
        String[][] content = new String[rowCount(table) + 1][(l.longueur * l.largeur) + 2];
        for (int idx = 0; idx < length(content, 1) - 1; idx++) {
            for (int jdx = 0; jdx < length(content, 2); jdx++) {
                content[idx][jdx] = getCell(table, idx, jdx);
            }
        }
        content[length(content) - 1][0] = String.valueOf(l.longueur);
        content[length(content) - 1][1] = String.valueOf(l.largeur);
        int index = 2;
        for (int idx = 0; idx < l.longueur; idx++) {
            for (int jdx = 0; jdx < l.largeur; jdx++) {
                content[length(content) - 1][index] = String.valueOf(l.labyrinthe[idx][jdx]);
                index++;
            }
        }
        saveCSV(content, "resources/tables/LABYRINTHES.csv");
    }

    /*
      Sauvegarde un score donné dans le fichier
      SCORES.csv.
    */
    void sauvegarderScore(int score) {
        CSVFile table = loadCSV("resources/tables/SCORES.csv");
        String[][] content = new String[rowCount(table) + 1][2];
        for (int idx = 0; idx < length(content, 1) - 1; idx++) {
            for (int jdx = 0; jdx < length(content, 2); jdx++) {
                content[idx][jdx] = getCell(table, idx, jdx);
            }
        }
        content[length(content) - 1][0] = j.nom;
        content[length(content) - 1][1] = String.valueOf(score);
        saveCSV(content, "resources/tables/SCORES.csv");
    }

    /*
      Génération procédurale du tracé du labyrinthe
      à l'aide de l'algorithme récursif RDFS
      (Randomized Depth-First Search).
    */
    void genererLabyrinthe(Labyrinthe l, int cx, int cy) {
        DIRS[] directions = DIRS.values();
        shuffleArray(directions);
        for (DIRS direction : directions) {
            int nx = cx + direction.dx;
            int ny = cy + direction.dy;
            if (entre(nx, l.longueur) && entre(ny, l.largeur) && (l.labyrinthe[nx][ny] == 0)) {
                l.labyrinthe[cx][cy] |= direction.bit;
                l.labyrinthe[nx][ny] |= direction.inverse.bit;
                genererLabyrinthe(l, nx, ny);
            }
        }
    }

    /*
      Mélange le tableau des directions pour la
      fonction genererLabyrinthe().
      Remplace java.util.Collections.shuffle().
    */
    void shuffleArray(DIRS[] directions) {
        for (int idx = length(directions) - 1; idx > 0; idx--) {
            int rand = random(0, idx);
            DIRS temp = directions[idx];
            directions[idx] = directions[rand];
            directions[rand] = temp;
        }
    }

    /*
      Fonction pour vérifier si on ne
      sort pas des limites du labyrinthe.
    */
    boolean entre(int pos, int limite) {
        return (pos >= 0) && (pos < limite);
    }

    /*
      Affichage graphique du menu principal du jeu.
    */
    void mainMenuGUI() {
        MODEVALUE = 0;
        MAINMENU = true;
        while (MAINMENU) {
            fill(img, rgbColor(0, 0, 0));
            Image logo = newImage("logo", "./resources/images/logo.png");
            double scaleFactor = 0.5;
            int newLogoWidth = (int) (getWidth(logo) * scaleFactor);
            int newLogoHeight = (int) (getHeight(logo) * scaleFactor);
            drawImage(img, copyAndResize(logo, "logo", newLogoWidth, newLogoHeight), ((WINDOW_WIDTH - newLogoWidth) / 2), ((WINDOW_HEIGHT / newLogoHeight) / 2) + (WINDOW_HEIGHT / 12));
            setNewFont(img, "JetBrains Mono", "PLAIN", 50);
            int[] dimensionsHIST = tailleTexte("1) MODE HISTOIRE", 50); // HIST et ENDL ont les mêmes dimensions
            int[] dimensionsENDL = tailleTexte("2) MODE SANS FIN", 50); // mais on les sépare pour la clarté du code
            int[] dimensionsLEAD = tailleTexte("3) CLASSEMENT", 50);
            int[] dimensionsEXIT = tailleTexte("4) QUITTER", 50);
            // l'ajout de 70 dans les dimensions y est arbitraire, c'est un espace requis pour séparer les éléments correctement
            drawString(img, "1) MODE HISTOIRE", ((WINDOW_WIDTH - dimensionsHIST[1]) / 2), ((WINDOW_HEIGHT - dimensionsHIST[0]) / 2) + 70);
            drawString(img, "2) MODE SANS FIN", ((WINDOW_WIDTH - dimensionsENDL[1]) / 2), ((WINDOW_HEIGHT - dimensionsENDL[0]) / 2) + (70 + dimensionsHIST[0]));
            drawString(img, "3) CLASSEMENT", ((WINDOW_WIDTH - dimensionsLEAD[1]) / 2), ((WINDOW_HEIGHT - dimensionsLEAD[0]) / 2) + (70 + (dimensionsHIST[0] * 2)));
            drawString(img, "4) QUITTER", ((WINDOW_WIDTH - dimensionsEXIT[1]) / 2), ((WINDOW_HEIGHT - dimensionsEXIT[0]) / 2) + (70 + (dimensionsHIST[0] * 3)));
            show(img);
            while (MAINMENU) {
                delay(1);
            }
        }
    }

    /*
      Affichage graphique d'un combat.
    */
    void combatGUI() {
        COMBAT = true;
        monstre = nouveauMonstre(false);
        monstre.hpact = monstre.hpmax;
        Image sprite = newImage(monstre.nom, (SPRITE_FOLDER + monstre.nom + ".png"));
        double scaleFactor = (400.0 / getHeight(sprite));
        int newSpriteWidth = (int) (getWidth(sprite) * scaleFactor);
        int newSpriteHeight = (int) (getHeight(sprite) * scaleFactor);
        boolean combatfini = false;
        while (COMBAT) {
            REPONSE = false;
            fill(img, rgbColor(0, 0, 0));
            drawImage(img, copyAndResize(sprite, monstre.nom, newSpriteWidth, newSpriteHeight), ((WINDOW_WIDTH - newSpriteWidth) / 2), ((WINDOW_HEIGHT / newSpriteHeight) / 2) + (WINDOW_HEIGHT / 11));
            setNewFont(img, "JetBrains Mono", "BOLD", 25);
            String texteENNEMI = (MIDBOSS ? "GARDIEN DE L'ÉTAGE: " : "") + monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)";
            int[] dimensionsENNEMI = tailleTexte(texteENNEMI, 25);
            drawString(img, texteENNEMI, ((WINDOW_WIDTH - dimensionsENNEMI[1]) / 2), 35); // 35 est un choix arbitraire
            setNewFont(img, "JetBrains Mono", "PLAIN", 20);
            if (j.vies == 0) {
                String mort1 = "Vous n'avez plus de vies.";
                String mort2 = "Votre aventure se termine ici, héros.";
                String mort3 = "Votre corps sans vie tombe, dans les limbes du labyrinthe d'Amala.";
                int[] dimensionsMORT1 = tailleTexte(mort1, 20);
                int[] dimensionsMORT2 = tailleTexte(mort2, 20);
                int[] dimensionsMORT3 = tailleTexte(mort3, 20);
                drawString(img, mort1, ((WINDOW_WIDTH - dimensionsMORT1[1]) / 2), (100 + newSpriteHeight));
                delay(2000);
                drawString(img, mort2, ((WINDOW_WIDTH - dimensionsMORT2[1]) / 2), (100 + newSpriteHeight + dimensionsMORT1[0]));
                delay(2000);
                drawString(img, mort3, ((WINDOW_WIDTH - dimensionsMORT3[1]) / 2), (100 + newSpriteHeight + dimensionsMORT1[0] + dimensionsMORT2[0]));
                delay(5000);
                COMBAT = false;
                MAINMENU = true;
            }
            else {
                ACTIONCOMBAT = true;
                CRITICAL = false;
                question = nouvelleQuestion();
                int[] dimensionsENONCE = tailleTexte(question.enonce, 20);
                String texteREPONSES = "";
                for (int idx = 0; idx < length(question.propositions); idx++) {
                    texteREPONSES += ((idx + 1) + ") " + question.propositions[idx]);
                    if (idx != (length(question.propositions) - 1)) {
                        texteREPONSES += " | ";
                    }
                }
                int[] dimensionsREPONSES = tailleTexte(texteREPONSES, 20);
                drawString(img, question.enonce, ((WINDOW_WIDTH - dimensionsENONCE[1]) / 2), (100 + newSpriteHeight));
                drawString(img, texteREPONSES, ((WINDOW_WIDTH - dimensionsREPONSES[1]) / 2), (100 + newSpriteHeight + dimensionsENONCE[0]));
                show(img);
                while (ACTIONCOMBAT) {
                    delay(1);
                }
                fill(img, rgbColor(0, 0, 0));
                drawImage(img, copyAndResize(sprite, monstre.nom, newSpriteWidth, newSpriteHeight), ((WINDOW_WIDTH - newSpriteWidth) / 2), ((WINDOW_HEIGHT / newSpriteHeight) / 2) + (WINDOW_HEIGHT / 11));
                if (REPONSE) {
                    int degats = random(10, 15);
                    if (random() < CRITICAL_PERCENTAGE) {
                        CRITICAL = true;
                        degats = (int) (degats * 1.5);
                    }
                    monstre.hpact = monstre.hpact - degats;
                    if (monstre.hpact < 1) {
                        monstre.hpact = 0;
                        COMBAT = false;
                        if (MODEVALUE == 1) {
                            STORYMODE = true;
                        }
                        else {
                            ENDLESS = true;
                        }
                        SCORE_ENDLESS += 30;
                    }
                    texteENNEMI = monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)";
                    dimensionsENNEMI = tailleTexte(texteENNEMI, 25);
                    setNewFont(img, "JetBrains Mono", "BOLD", 25);
                    drawString(img, texteENNEMI, ((WINDOW_WIDTH - dimensionsENNEMI[1]) / 2), 35);
                    setNewFont(img, "JetBrains Mono", "PLAIN", 20);
                    String resultat1 = "Réponse correcte! " + (CRITICAL ? "COUP CRITIQUE!" : "");
                    String resultat2 = "Vous " + (monstre.hpact == 0 ? "délivrez le coup fatal en infligeant " : "infligez ") + degats + " points de dégâts à l'ennemi!";
                    int[] dimensionsREP1 = tailleTexte(resultat1, 20);
                    int[] dimensionsREP2 = tailleTexte(resultat2, 20);
                    drawString(img, resultat1, ((WINDOW_WIDTH - dimensionsREP1[1]) / 2), (100 + newSpriteHeight));
                    drawString(img, resultat2, ((WINDOW_WIDTH - dimensionsREP2[1]) / 2), (100 + newSpriteHeight + dimensionsREP1[0]));
                    delay(3000);
                }
                else {
                    j.vies--;
                    setNewFont(img, "JetBrains Mono", "BOLD", 25);
                    drawString(img, texteENNEMI, ((WINDOW_WIDTH - dimensionsENNEMI[1]) / 2), 35);
                    setNewFont(img, "JetBrains Mono", "PLAIN", 20);
                    String echec1 = "Réponse incorrecte! La bonne réponse était " + question.propositions[question.idxreponse - 1] + ".";
                    String echec2 = "L'ennemi vous enlève une vie.";
                    int[] dimensionsECH1 = tailleTexte(echec1, 20);
                    int[] dimensionsECH2 = tailleTexte(echec2, 20);
                    drawString(img, echec1, ((WINDOW_WIDTH - dimensionsECH1[1]) / 2), (100 + newSpriteHeight));
                    drawString(img, echec2, ((WINDOW_WIDTH - dimensionsECH2[1]) / 2), (100 + newSpriteHeight + dimensionsECH1[0]));
                    delay(3000);
                }
            }
        }
    }

    /*
      Retire le format de fichier d'une chaîne
      de caractères. Utilisé pour les sprites.
    */
    String nameFormat(String filename) {
        filename = substring(filename, 0, (length(filename) - 4));
        return filename;
    }

    /*
      Mode histoire du jeu.
      Le joueur est confronté face à 20 étages,
      tous plus durs que le précédent, avant d'arriver
      au trône du tyran Amala, dans le but de le vaincre.
      AFFICHAGE TEXTUEL.
    */
    void modeHistoireTUI() {
        clearTerminal();
        //boolean partiefinie = true;
        boolean etagefini = true;
        if (getFiles("txt", "resources/ascii") != 141) {
            println("Le jeu a détecté l'absence de fichiers dans le dossier des sprites.");
            println("Veuillez vérifier à ne pas avoir modifié de fichiers dans le dossier resources/ascii.");
            println("Sinon, retéléchargez le jeu dans son entièreté via le lien suivant:");
            println("https://github.com/nachtreiher/AMALA");
            println("Vous serez ramené au menu principal dans quelques secondes.");
            delay(10000);
        }
        else {
            extensions.File scenario = newFile("resources/text/SCENARIO.txt");
            while (ready(scenario)) {
                String line = readLine(scenario);
                if (equals(line, "---")) {
                    clearTerminal();
                }
                else {
                    println(line);
                    delay(3500);
                }
            }
            resetATK();
            STORYMODE = true;
            etagefini = false;
        }
        clearTerminal();
        ETAGE_STORYMODE = 1;
        while (STORYMODE) {
            if (ETAGE_STORYMODE > 20) {
                amalaFightTUI();
                STORYMODE = false;
            }
            else {
                MIDBOSS = false;
                etagefini = false;
                l = nouveauLabyrinthe(LABYRINTH_HEIGHT, LABYRINTH_WIDTH);
                resetPositions();
                decouverteAdjacentes(l);
                while (STORYMODE && !etagefini) {
                    if (j.vies != 0) {
                        clearTerminal();
                        println("Mode histoire, étage " + ETAGE_STORYMODE + NEWLINE
                                + "-----------------------" + NEWLINE
                                + "Joueur: " + j.nom + " | " + j.vies + " vies restantes" + NEWLINE);
                        print(affichageDecouvert(l));
                        if (j.posx == (l.longueur - 1) && j.posy == (l.largeur - 1) && ETAGE_STORYMODE % 5 == 0 && !MIDBOSS) {
                            MIDBOSS = true;
                            println("Vous sentez une aura émaner des alentours...");
                            combatTUI();
                            j.encounter_seed = 0;
                        }
                        else if (j.encounter_seed >= MAX_ENCOUNTER_SEED) {
                            combatTUI();
                            j.encounter_seed = 0;
                        }
                        else if (j.posx != (l.longueur - 1) || j.posy != (l.largeur - 1)) {
                            println("DIRECTIONS: ⭡ Z | ⭠ Q | ⭣ S | ⭢ D");
                            print("Sélectionnez une direction: ");
                            String movement = toUpperCase(readString());
                            if (length(movement) != 0 && peutDeplacer(charAt(movement, 0), getDirections(l.labyrinthe[j.posx][j.posy], true))) {
                                deplacer(l, getCardinal(charAt(movement, 0)));
                                l.decouvert[j.posx][j.posy] = true;
                                decouverteAdjacentes(l);
                                j.encounter_seed += random(1, 5);
                            }
                        }
                        else {
                            println("Étage " + ETAGE_STORYMODE + " fini !" + NEWLINE
                                    + (ETAGE_STORYMODE == 20 ? "\"Ton voyage s'arrête ici.\"" : "Un nouvel étage vous attend..."));
                            delay(3000);
                            ETAGE_STORYMODE++;
                            j.maxatk += 5;
                            j.minatk += 3;
                            etagefini = true;
                        }
                    }
                    else {
                        STORYMODE = false;
                    }
                }
            }
        }
    }

    /*
      Combat contre le boss final du mode
      histoire, Amala.
      AFFICHAGE TEXTUEL.
    */
    void amalaFightTUI() {
        extensions.File monologue = newFile("resources/text/AMALA.txt");
        while (ready(monologue)) {
            String line = readLine(monologue);
            if (equals(line, "---")) {
                clearTerminal();
            }
            else {
                println(line);
                delay(3500);
            }
        }
        Monstre monstre = nouveauMonstre(true);
        monstre.hpact = monstre.hpmax;
        boolean combatfini = false;
        while (combatfini == false) {
            clearTerminal();
            if (j.vies == 0) {
                println(monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)" + NEWLINE);
                println(monstre.sprite + NEWLINE);
                println("Eh bien.");
                delay(2000);
                println("Tu as osé affronter les ténèbres.");
                delay(2000);
                println("Mais même la plus grande bravoure ne peut égaler ma puissance.");
                delay(2000);
                println("Autant de parcours pour tomber entre mes mains.");
                delay(2000);
                println("Pathétique.");
                combatfini = true;
                delay(3000);
            }
            else {
                Question question = nouvelleQuestion();
                String reponse = "";
                char reponsec = ' ';
                do {
                    clearTerminal();
                    println(monstre.nom + ", Empereur du chaos" + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)" + NEWLINE);
                    println(monstre.sprite);
                    println("Vos vies: " + j.vies);
                    if (monstre.hpact < (monstre.hpmax / 2) && !BONUS) {
                        println("Pour affronter la tâche qui vous semble ardue, vous priez auprès des cieux pour une aide.");
                        delay(2000);
                        println("VOTRE ATTAQUE EST DOUBLÉE ET TOUTES VOS VIES RÉGÉNÉRÉES!");
                        j.vies = 10;
                        j.minatk = j.minatk * 2;
                        j.maxatk = j.maxatk * 2;
                        BONUS = true;
                        delay(2000);
                    }
                    else {
                        println(question.enonce);
                        for (int idx = 0; idx < length(question.propositions); idx++) {
                            print((idx + 1) + ") " + question.propositions[idx]);
                            if (idx != (length(question.propositions) - 1)) {
                                print(" | ");
                            }
                        }
                        println();
                        print("Réponse (donner le numéro): ");
                        reponse = readString();
                        if (length(reponse) > 0) {
                            reponsec = charAt(reponse, 0);
                        }
                    }
                } while (reponsec > '4' || reponsec < '0');
                if (reponsec == question.reponse) {
                    clearTerminal();
                    int degats = random(j.minatk, j.maxatk);
                    boolean critique = false;
                    if (random() < CRITICAL_PERCENTAGE) {
                        degats = (int) (degats * 1.5);
                        critique = true;
                    }
                    monstre.hpact = monstre.hpact - degats;
                    if (monstre.hpact < 1) {
                        monstre.hpact = 0;
                        combatfini = true;
                        SCORE_ENDLESS += 30;
                    }
                    println(monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)" + NEWLINE);
                    println(monstre.sprite + NEWLINE);
                    println("Réponse correcte! " + (critique == true ? "COUP CRITIQUE! " : "") + "Vous " + (monstre.hpact == 0 ? "délivrez le coup fatal en infligeant " : "infligez ") + degats + " points de dégâts à l'ennemi!");
                    delay(3000);
                }
                else {
                    clearTerminal();
                    println(monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)" + NEWLINE);
                    println(monstre.sprite + NEWLINE);
                    println("Réponse incorrecte! La bonne réponse était " + question.propositions[question.idxreponse - 1] + ". L'ennemi vous enlève une vie.");
                    j.vies--;
                    delay(3000);
                }
            }
        }
        if (combatfini && monstre.hpact == 0) {
            clearTerminal();
            extensions.File ending = newFile("resources/text/ENDING.txt");
            while (ready(ending)) {
                String line = readLine(ending);
                if (equals(line, "---")) {
                    clearTerminal();
                }
                else {
                    println(line);
                    delay(3500);
                }
            }
            clearTerminal();
            println("FIN.");
            delay(10000);
        }
    }

    /*
      Combat contre le boss final du mode
      histoire, Amala.
      AFFICHAGE GRAPHIQUE.
    */
    void amalaFightGUI() {
        STORYMODE = false;
        setNewFont(img, "JetBrains Mono", "PLAIN", 25);
        fill(img, rgbColor(255, 255, 255));
        setColor(img, rgbColor(0, 0, 0));
        CUTSCENE = true;
        extensions.File monologue = newFile("resources/text/AMALA.txt");
        while (ready(monologue) && CUTSCENE) {
            String line = readLine(monologue);
            if (!equals(line, "---")) {
                int[] dimensionsLINE = tailleTexte(line, 25);
                drawString(img, line, ((WINDOW_WIDTH - dimensionsLINE[1]) / 2), ((WINDOW_HEIGHT - dimensionsLINE[0]) / 2));
                delay(3500);
                fill(img, rgbColor(255, 255, 255));
            }
        }
        CUTSCENE = false;
        COMBAT = true;
        monstre = nouveauMonstre(true);
        monstre.hpact = monstre.hpmax;
        Image sprite = newImage(monstre.nom, (SPRITE_FOLDER + monstre.nom + "W" + ".png"));
        double scaleFactor = (400.0 / getHeight(sprite));
        int newSpriteWidth = (int) (getWidth(sprite) * scaleFactor);
        int newSpriteHeight = (int) (getHeight(sprite) * scaleFactor);
        boolean combatfini = false;
        while (COMBAT) {
            REPONSE = false;
            fill(img, rgbColor(255, 255, 255));
            drawImage(img, copyAndResize(sprite, monstre.nom, newSpriteWidth, newSpriteHeight), ((WINDOW_WIDTH - newSpriteWidth) / 2), ((WINDOW_HEIGHT / newSpriteHeight) / 2) + (WINDOW_HEIGHT / 11));
            setNewFont(img, "JetBrains Mono", "BOLD", 25);
            String texteENNEMI = monstre.nom + ", Empereur du chaos" + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)";
            int[] dimensionsENNEMI = tailleTexte(texteENNEMI, 25);
            drawString(img, texteENNEMI, ((WINDOW_WIDTH - dimensionsENNEMI[1]) / 2), 35);
            setNewFont(img, "JetBrains Mono", "PLAIN", 20);
            if (j.vies == 0) {
                String mort1 = "Eh bien.";
                String mort2 = "Tu as osé affronter les ténèbres.";
                String mort3 = "Mais même la plus grande bravoure ne peut égaler ma puissance.";
                String mort4 = "Autant de parcours pour tomber entre mes mains.";
                String mort5 = "Pathétique.";
                int[] dimensionsMORT1 = tailleTexte(mort1, 20);
                int[] dimensionsMORT2 = tailleTexte(mort2, 20);
                int[] dimensionsMORT3 = tailleTexte(mort3, 20);
                int[] dimensionsMORT4 = tailleTexte(mort4, 20);
                int[] dimensionsMORT5 = tailleTexte(mort5, 20);
                fill(img, rgbColor(255, 255, 255));
                drawString(img, mort1, ((WINDOW_WIDTH - dimensionsMORT1[1]) / 2), ((WINDOW_HEIGHT - dimensionsMORT1[0]) / 2));
                delay(2000);
                fill(img, rgbColor(255, 255, 255));
                drawString(img, mort2, ((WINDOW_WIDTH - dimensionsMORT2[1]) / 2), ((WINDOW_HEIGHT - dimensionsMORT2[0]) / 2));
                delay(2000);
                fill(img, rgbColor(255, 255, 255));
                drawString(img, mort3, ((WINDOW_WIDTH - dimensionsMORT3[1]) / 2), ((WINDOW_HEIGHT - dimensionsMORT3[0]) / 2));
                delay(2000);
                fill(img, rgbColor(255, 255, 255));
                drawString(img, mort4, ((WINDOW_WIDTH - dimensionsMORT4[1]) / 2), ((WINDOW_HEIGHT - dimensionsMORT4[0]) / 2));
                delay(2000);
                fill(img, rgbColor(255, 255, 255));
                drawString(img, mort5, ((WINDOW_WIDTH - dimensionsMORT5[1]) / 2), ((WINDOW_HEIGHT - dimensionsMORT5[0]) / 2));
                delay(5000);
                COMBAT = false;
                MAINMENU = true;
            }
            else {
                if (monstre.hpact < (monstre.hpmax / 2) && !BONUS) {
                    String bonus1 = "Pour affronter la tâche qui vous semble ardue, vous priez auprès des cieux pour une aide.";
                    String bonus2 = "VOTRE ATTAQUE EST DOUBLÉE ET TOUTES VOS VIES RÉGÉNÉRÉES!";
                    int[] dimensionsBON1 = tailleTexte(bonus1, 20);
                    int[] dimensionsBON2 = tailleTexte(bonus2, 20);
                    fill(img, rgbColor(255, 255, 255));
                    drawString(img, bonus1, ((WINDOW_WIDTH - dimensionsBON1[1]) / 2), ((WINDOW_HEIGHT - dimensionsBON1[0]) / 2));
                    delay(3000);
                    fill(img, rgbColor(255, 255, 255));
                    drawString(img, bonus2, ((WINDOW_WIDTH - dimensionsBON2[1]) / 2), ((WINDOW_HEIGHT - dimensionsBON2[0]) / 2));
                    j.vies = 10;
                    j.minatk = j.minatk * 2;
                    j.maxatk = j.maxatk * 2;
                    BONUS = true;
                    delay(5000);
                }
                else {
                    ACTIONCOMBAT = true;
                    CRITICAL = false;
                    question = nouvelleQuestion();
                    int[] dimensionsENONCE = tailleTexte(question.enonce, 20);
                    String texteREPONSES = "";
                    for (int idx = 0; idx < length(question.propositions); idx++) {
                        texteREPONSES += ((idx + 1) + ") " + question.propositions[idx]);
                        if (idx != (length(question.propositions) - 1)) {
                            texteREPONSES += " | ";
                        }
                    }
                    int[] dimensionsREPONSES = tailleTexte(texteREPONSES, 20);
                    drawString(img, question.enonce, ((WINDOW_WIDTH - dimensionsENONCE[1]) / 2), (100 + newSpriteHeight));
                    drawString(img, texteREPONSES, ((WINDOW_WIDTH - dimensionsREPONSES[1]) / 2), (100 + newSpriteHeight + dimensionsENONCE[0]));
                    show(img);
                    while (ACTIONCOMBAT) {
                        delay(1);
                    }
                    fill(img, rgbColor(255, 255, 255));
                    drawImage(img, copyAndResize(sprite, monstre.nom, newSpriteWidth, newSpriteHeight), ((WINDOW_WIDTH - newSpriteWidth) / 2), ((WINDOW_HEIGHT / newSpriteHeight) / 2) + (WINDOW_HEIGHT / 11));
                    if (REPONSE) {
                        int degats = random(j.minatk, j.maxatk);
                        if (random() < CRITICAL_PERCENTAGE) {
                            CRITICAL = true;
                            degats = (int) (degats * 1.5);
                        }
                        monstre.hpact = monstre.hpact - degats;
                        if (monstre.hpact < 1) {
                            monstre.hpact = 0;
                            COMBAT = false;
                        }
                        texteENNEMI = monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)";
                        dimensionsENNEMI = tailleTexte(texteENNEMI, 25);
                        setNewFont(img, "JetBrains Mono", "BOLD", 25);
                        drawString(img, texteENNEMI, ((WINDOW_WIDTH - dimensionsENNEMI[1]) / 2), 35);
                        setNewFont(img, "JetBrains Mono", "PLAIN", 20);
                        String resultat1 = "Réponse correcte! " + (CRITICAL ? "COUP CRITIQUE!" : "");
                        String resultat2 = "Vous " + (monstre.hpact == 0 ? "délivrez le coup fatal en infligeant " : "infligez ") + degats + " points de dégâts à l'ennemi!";
                        int[] dimensionsREP1 = tailleTexte(resultat1, 20);
                        int[] dimensionsREP2 = tailleTexte(resultat2, 20);
                        drawString(img, resultat1, ((WINDOW_WIDTH - dimensionsREP1[1]) / 2), (100 + newSpriteHeight));
                        drawString(img, resultat2, ((WINDOW_WIDTH - dimensionsREP2[1]) / 2), (100 + newSpriteHeight + dimensionsREP1[0]));
                        delay(3000);
                    }
                    else {
                        j.vies--;
                        setNewFont(img, "JetBrains Mono", "BOLD", 25);
                        drawString(img, texteENNEMI, ((WINDOW_WIDTH - dimensionsENNEMI[1]) / 2), 35);
                        setNewFont(img, "JetBrains Mono", "PLAIN", 20);
                        String echec1 = "Réponse incorrecte! La bonne réponse était " + question.propositions[question.idxreponse - 1] + ".";
                        String echec2 = "L'ennemi vous enlève une vie.";
                        int[] dimensionsECH1 = tailleTexte(echec1, 20);
                        int[] dimensionsECH2 = tailleTexte(echec2, 20);
                        drawString(img, echec1, ((WINDOW_WIDTH - dimensionsECH1[1]) / 2), (100 + newSpriteHeight));
                        drawString(img, echec2, ((WINDOW_WIDTH - dimensionsECH2[1]) / 2), (100 + newSpriteHeight + dimensionsECH1[0]));
                        delay(3000);
                    }
                }
            }
        }
        if (!COMBAT && monstre.hpact == 0) {
            CUTSCENE = true;
            setNewFont(img, "JetBrains Mono", "PLAIN", 25);
            fill(img, rgbColor(255, 255, 255));
            extensions.File ending = newFile("resources/text/ENDING.txt");
            while (ready(ending) && CUTSCENE) {
                String line = readLine(ending);
                if (!equals(line, "---")) {
                    int[] dimensionsLINE = tailleTexte(line, 25);
                    drawString(img, line, ((WINDOW_WIDTH - dimensionsLINE[1]) / 2), ((WINDOW_HEIGHT - dimensionsLINE[0]) / 2));
                    delay(3500);
                    fill(img, rgbColor(255, 255, 255));
                }
            }
            fill(img, rgbColor(255, 255, 255));
            setNewFont(img, "JetBrains Mono", "PLAIN", 50);
            String fin = "FIN.";
            int[] dimensionsFIN = tailleTexte(fin, 50);
            drawString(img, fin, ((WINDOW_WIDTH - dimensionsFIN[1]) / 2), ((WINDOW_HEIGHT - dimensionsFIN[0]) / 2));
            delay(5000);
            fill(img, rgbColor(0, 0, 0));
            setColor(img, rgbColor(255, 255, 255));
            CUTSCENE = false;
            MAINMENU = true;
        }
    }

    /*
      Mode histoire du jeu.
      Le joueur est confronté face à 20 étages,
      tous plus durs que le précédent, avant d'arriver
      au trône du tyran Amala, dans le but de le vaincre.
      AFFICHAGE GRAPHIQUE.
    */
    void modeHistoireGUI() {
        fill(img, rgbColor(0, 0, 0));
        if (getFiles("png", "resources/sprites") != 142) {
            setNewFont(img, "JetBrains Mono", "PLAIN", 20);
            setColor(img, rgbColor(196, 24, 12));
            String erreur1 = "Le jeu a détecté l'absence de fichiers dans le dossier des sprites.";
            String erreur2 = "Veuillez vérifier à ne pas avoir modifié de fichiers dans le dossier resources/sprites.";
            String erreur3 = "Sinon, retéléchargez le jeu dans son entièreté via le lien suivant:";
            String erreur4 = "https://github.com/nachtreiher/AMALA";
            String erreur5 = "Vous serez ramené au menu principal dans quelques secondes.";
            int[] dimensionsERR1 = tailleTexte(erreur1, 20);
            int[] dimensionsERR2 = tailleTexte(erreur2, 20);
            int[] dimensionsERR3 = tailleTexte(erreur3, 20);
            int[] dimensionsERR4 = tailleTexte(erreur4, 20);
            int[] dimensionsERR5 = tailleTexte(erreur5, 20);
            drawString(img, erreur1, ((WINDOW_WIDTH - dimensionsERR1[1]) / 2), (((WINDOW_HEIGHT - dimensionsERR1[0]) / 2) - (dimensionsERR1[0] * 2)));
            drawString(img, erreur2, ((WINDOW_WIDTH - dimensionsERR2[1]) / 2), (((WINDOW_HEIGHT - dimensionsERR2[0]) / 2) - (dimensionsERR2[0])));
            drawString(img, erreur3, ((WINDOW_WIDTH - dimensionsERR3[1]) / 2), ((WINDOW_HEIGHT - dimensionsERR3[0]) / 2));
            drawString(img, erreur4, ((WINDOW_WIDTH - dimensionsERR4[1]) / 2), (((WINDOW_HEIGHT - dimensionsERR4[0]) / 2) + (dimensionsERR4[0])));
            drawString(img, erreur5, ((WINDOW_WIDTH - dimensionsERR5[1]) / 2), (((WINDOW_HEIGHT - dimensionsERR5[0]) / 2) + (dimensionsERR5[0] * 2)));
            delay(10000);
            setColor(img, rgbColor(255, 255, 255));
        }
        else {
            setNewFont(img, "JetBrains Mono", "PLAIN", 30);
            fill(img, rgbColor(255, 255, 255));
            setColor(img, rgbColor(0, 0, 0));
            CUTSCENE = true;
            extensions.File scenario = newFile("resources/text/SCENARIO.txt");
            while (ready(scenario) && CUTSCENE) {
                String line = readLine(scenario);
                if (!equals(line, "---")) {
                    int[] dimensionsLINE = tailleTexte(line, 30);
                    drawString(img, line, ((WINDOW_WIDTH - dimensionsLINE[1]) / 2), ((WINDOW_HEIGHT - dimensionsLINE[0]) / 2));
                    delay(3500);
                    fill(img, rgbColor(255, 255, 255));
                }
            }
            CUTSCENE = false;
            fill(img, rgbColor(0, 0, 0));
            setColor(img, rgbColor(255, 255, 255));
            STORYMODE = true;
        }
        MODEVALUE = 1;
        resetATK();
        int dy = 20;
        String layer = "";
        ETAGE_STORYMODE = 1;
        while (STORYMODE) {
            if (ETAGE_STORYMODE > 20) {
                amalaFightGUI();
            }
            else {
                MIDBOSS = false;
                PARTIEFINIE = false;
                l = nouveauLabyrinthe(LABYRINTH_HEIGHT, LABYRINTH_WIDTH);
                resetPositions();
                decouverteAdjacentes(l);
                while (STORYMODE && !PARTIEFINIE) {
                    fill(img, rgbColor(0, 0, 0));
                    if (j.vies != 0) {
                        dy = 0;
                        ACTION = true;
                        setNewFont(img, "JetBrains Mono", "PLAIN", 30);
                        String titre = "Mode histoire, étage " + ETAGE_STORYMODE + " | Joueur: " + j.nom + ", " + j.vies + " vies restantes";
                        int[] dimensionsTITRE = tailleTexte(titre, 30);
                        dy = dimensionsTITRE[0];
                        drawString(img, titre, ((WINDOW_WIDTH - dimensionsTITRE[1]) / 2), dy);
                        if (j.posx == (l.longueur - 1) && j.posy == (l.largeur - 1) && ETAGE_STORYMODE % 5 == 0 && !MIDBOSS) {
                            String midboss = "Vous sentez une aura émaner des alentours...";
                            int[] dimensionsMIDB = tailleTexte(midboss, 30);
                            drawString(img, midboss, ((WINDOW_WIDTH - dimensionsMIDB[1]) / 2), ((WINDOW_HEIGHT - dimensionsMIDB[0]) / 2) + dy);
                            delay(3000);
                            MIDBOSS = true;
                            STORYMODE = false;
                            combatGUI();
                            j.encounter_seed = 0;
                        }
                        else if (j.encounter_seed >= MAX_ENCOUNTER_SEED) {
                            String ennemi = "Un ennemi approche... Prenez-garde !";
                            int[] dimensionsENNEMI = tailleTexte(ennemi, 30);
                            drawString(img, ennemi, ((WINDOW_WIDTH - dimensionsENNEMI[1]) / 2), ((WINDOW_HEIGHT - dimensionsENNEMI[0]) / 2) + dy);
                            delay(3000);
                            STORYMODE = false;
                            combatGUI();
                            j.encounter_seed = 0;
                        }
                        else if (j.posx != (l.longueur - 1) || j.posy != (l.largeur - 1)) {
                            setNewFont(img, "JetBrains Mono", "PLAIN", 18);
                            String labyrinthe = affichageDecouvert(l);
                            for (int idx = 0; idx < length(labyrinthe); idx++) {
                                if (charAt(labyrinthe, idx) != '\n') {
                                    layer += charAt(labyrinthe, idx);
                                }
                                else {
                                    int[] dimensionsLAYER = tailleTexte(layer, 18);
                                    dy += dimensionsLAYER[0];
                                    drawString(img, layer, ((WINDOW_WIDTH - dimensionsLAYER[1]) / 2), dy);
                                    layer = "";
                                }
                            }
                            while (ACTION) {
                                delay(1);
                            }
                        }
                        else {
                            String etagefini1 = "Étage " + ETAGE_STORYMODE + " fini!";
                            String etagefini2 = (ETAGE_STORYMODE == 20 ? "\"Ton voyage s'arrête ici.\"" : "Un nouvel étage vous attend...");
                            int[] dimensionsETF1 = tailleTexte(etagefini1, 30);
                            int[] dimensionsETF2 = tailleTexte(etagefini2, 30);
                            drawString(img, etagefini1, ((WINDOW_WIDTH - dimensionsETF1[1]) / 2), (((WINDOW_HEIGHT - dimensionsETF1[0]) / 2) - dimensionsTITRE[0]));
                            delay(3000);
                            if (ETAGE_STORYMODE == 20) {
                                fill(img, rgbColor(255, 255, 255));
                                setColor(img, rgbColor(0, 0, 0));
                            }
                            drawString(img, etagefini2, ((WINDOW_WIDTH - dimensionsETF2[1]) / 2), (((WINDOW_HEIGHT - dimensionsETF2[0]) / 2)) - (ETAGE_STORYMODE != 20 ? dimensionsTITRE[0] : 0) + (ETAGE_STORYMODE != 20 ?  + dimensionsETF1[0] : 0));
                            delay(3000);
                            PARTIEFINIE = true;
                        }
                    }
                }
            }
        }
    }

    /*
      Mode sans fin/endless du jeu.
      Le joueur doit finir autant de labyrinthes
      que possible. Son score sera sauvegardé
      dans le leaderboard.
      AFFICHAGE TEXTUEL.
    */
    void modeEndlessTUI() {
        SCORE_ENDLESS = 0;
        resetATK();
        ETAGE_ENDLESS = 1;
        boolean fini = true;
        boolean partiefinie = true;
        if (getFiles("txt", "resources/ascii") != 141) {
            clearTerminal();
            println("Le jeu a détecté l'absence de fichiers dans le dossier des sprites.");
            println("Veuillez vérifier à ne pas avoir modifié de fichiers dans le dossier resources/ascii.");
            println("Sinon, retéléchargez le jeu dans son entièreté via le lien suivant:");
            println("https://github.com/nachtreiher/AMALA");
            println("Vous serez ramené au menu principal dans quelques secondes.");
            delay(10000);
        }
        else {
            fini = false;
            partiefinie = false;
        }
        clearTerminal();
        while (fini == false) {
            partiefinie = false;
            Labyrinthe l = nouveauLabyrinthe(LABYRINTH_HEIGHT, LABYRINTH_WIDTH);
            resetPositions();
            decouverteAdjacentes(l);
            while (partiefinie == false) {
                clearTerminal();
                if (j.vies != 0) {
                    println("Labyrinthes sans fin, étage " + ETAGE_ENDLESS + NEWLINE
                            + "-----------------------------" + NEWLINE
                            + "Joueur: " + j.nom + " | " + j.vies + " vies restantes" + NEWLINE);
                    println(affichageDecouvert(l));
                    if (j.encounter_seed >= MAX_ENCOUNTER_SEED) {
                        combatTUI();
                        j.encounter_seed = 0;
                    }
                    else if (j.posx != (l.longueur - 1) || j.posy != (l.largeur - 1)) {
                        println("DIRECTIONS: ⭡ Z | ⭠ Q | ⭣ S | ⭢ D");
                        print("Sélectionnez une direction: ");
                        String movement = toUpperCase(readString());
                        if (length(movement) != 0 && peutDeplacer(charAt(movement, 0), getDirections(l.labyrinthe[j.posx][j.posy], true))) {
                            deplacer(l, getCardinal(charAt(movement, 0)));
                            l.decouvert[j.posx][j.posy] = true;
                            decouverteAdjacentes(l);
                            j.encounter_seed += random(1, 5);
                        }
                    }
                    else {
                        println("Étage " + ETAGE_ENDLESS + " fini !" + NEWLINE
                                + "Un nouvel étage vous attend...");
                        delay(3000);
                        partiefinie = true;
                        j.minatk += 3;
                        j.maxatk += 5;
                        ETAGE_ENDLESS++;
                        SCORE_ENDLESS += 50;
                    }
                }
                else {
                    fini = true;
                    sauvegarderScore(SCORE_ENDLESS);
                }
            }
        }
    }

    /*
      Retourne le cardinal selon la touche
      de déplacement donnée.
    */
    char getCardinal(char key) {
        char res = ' ';
        DIRS[] directions = DIRS.values();
        for (DIRS direction : directions) {
            if (direction.key == key) {
                res = charAt(direction.name(), 0);
            }
        }
        return res;
    }

    int getFiles(String format, String directory) {
        int number = 0;
        String[] files = getAllFilesFromDirectory(directory);
        for (int idx = 0; idx < length(files); idx++) {
            if (equals(format, substring(files[idx], length(files[idx]) - length(format), length(files[idx])))) {
                number++;
            }
        }
        return number;
    }

    /*
      Mode sans fin/endless du jeu.a
      Le joueur doit finir autant de labyrinthes
      que possible. Son score sera sauvegardé
      dans le leaderboard.
      AFFICHAGE GRAPHIQUE.
    */
    void modeEndlessGUI() {
        fill(img, rgbColor(0, 0, 0));
        if (getFiles("png", "resources/sprites") != 142) {
            setNewFont(img, "JetBrains Mono", "PLAIN", 20);
            setColor(img, rgbColor(196, 24, 12));
            String erreur1 = "Le jeu a détecté l'absence de fichiers dans le dossier des sprites.";
            String erreur2 = "Veuillez vérifier à ne pas avoir modifié de fichiers dans le dossier resources/sprites.";
            String erreur3 = "Sinon, retéléchargez le jeu dans son entièreté via le lien suivant:";
            String erreur4 = "https://github.com/nachtreiher/AMALA";
            String erreur5 = "Vous serez ramené au menu principal dans quelques secondes.";
            int[] dimensionsERR1 = tailleTexte(erreur1, 20);
            int[] dimensionsERR2 = tailleTexte(erreur2, 20);
            int[] dimensionsERR3 = tailleTexte(erreur3, 20);
            int[] dimensionsERR4 = tailleTexte(erreur4, 20);
            int[] dimensionsERR5 = tailleTexte(erreur5, 20);
            drawString(img, erreur1, ((WINDOW_WIDTH - dimensionsERR1[1]) / 2), (((WINDOW_HEIGHT - dimensionsERR1[0]) / 2) - (dimensionsERR1[0] * 2)));
            drawString(img, erreur2, ((WINDOW_WIDTH - dimensionsERR2[1]) / 2), (((WINDOW_HEIGHT - dimensionsERR2[0]) / 2) - (dimensionsERR2[0])));
            drawString(img, erreur3, ((WINDOW_WIDTH - dimensionsERR3[1]) / 2), ((WINDOW_HEIGHT - dimensionsERR3[0]) / 2));
            drawString(img, erreur4, ((WINDOW_WIDTH - dimensionsERR4[1]) / 2), (((WINDOW_HEIGHT - dimensionsERR4[0]) / 2) + (dimensionsERR4[0])));
            drawString(img, erreur5, ((WINDOW_WIDTH - dimensionsERR5[1]) / 2), (((WINDOW_HEIGHT - dimensionsERR5[0]) / 2) + (dimensionsERR5[0] * 2)));
            delay(10000);
            setColor(img, rgbColor(255, 255, 255));
            fill(img, rgbColor(0, 0, 0));
        }
        else {
            ENDLESS = true;
        }
        MODEVALUE = 2;
        SCORE_ENDLESS = 0;
        resetATK();
        ETAGE_ENDLESS = 1;
        ENDLESS = true;
        String layer = "";
        int dy = 20;
        while (ENDLESS) {
            PARTIEFINIE = false;
            l = nouveauLabyrinthe(LABYRINTH_HEIGHT, LABYRINTH_WIDTH);
            resetPositions();
            decouverteAdjacentes(l);
            while (ENDLESS && !PARTIEFINIE) {
                fill(img, rgbColor(0, 0, 0));
                if (j.vies != 0) {
                    ACTION = true;
                    setNewFont(img, "JetBrains Mono", "PLAIN", 30);
                    String titre = "Labyrinthes sans fin, étage " + ETAGE_ENDLESS + " | Joueur: " + j.nom + ", " + j.vies + " vies restantes";
                    int[] dimensionsTITRE = tailleTexte(titre, 30);
                    dy = dimensionsTITRE[0];
                    drawString(img, titre, ((WINDOW_WIDTH - dimensionsTITRE[1]) / 2), dy);
                    if (j.encounter_seed > MAX_ENCOUNTER_SEED) {
                        String ennemi = "Un ennemi approche... Prenez-garde !";
                        int[] dimensionsENNEMI = tailleTexte(ennemi, 30);
                        drawString(img, ennemi, ((WINDOW_WIDTH - dimensionsENNEMI[1]) / 2), ((WINDOW_HEIGHT - dimensionsENNEMI[0]) / 2) + dy);
                        delay(3000);
                        ENDLESS = false;
                        combatGUI();
                        j.encounter_seed = 0;
                    }
                    else if (j.posx != (l.longueur - 1) || j.posy != (l.largeur - 1)) {
                        setNewFont(img, "JetBrains Mono", "PLAIN", 18);
                        String labyrinthe = affichageDecouvert(l);
                        for (int idx = 0; idx < length(labyrinthe); idx++) {
                            if (charAt(labyrinthe, idx) != '\n') {
                                layer += charAt(labyrinthe, idx);
                            }
                            else {
                                int[] dimensionsLAYER = tailleTexte(layer, 18);
                                dy += dimensionsLAYER[0];
                                drawString(img, layer, ((WINDOW_WIDTH - dimensionsLAYER[1]) / 2), dy);
                                layer = "";
                            }
                        }
                        while (ACTION) {
                            delay(1);
                        }
                    }
                    else {
                        String etagefini1 = "Étage " + ETAGE_ENDLESS + " fini!";
                        String etagefini2 = "Un nouvel étage vous attend...";
                        int[] dimensionsETF1 = tailleTexte(etagefini1, 30);
                        int[] dimensionsETF2 = tailleTexte(etagefini2, 30);
                        drawString(img, etagefini1, ((WINDOW_WIDTH - dimensionsETF1[1]) / 2), (((WINDOW_HEIGHT - dimensionsETF1[0]) / 2) - dimensionsTITRE[0]));
                        delay(3000);
                        drawString(img, etagefini2, ((WINDOW_WIDTH - dimensionsETF2[1]) / 2), (((WINDOW_HEIGHT - dimensionsETF2[0]) / 2) - dimensionsTITRE[0]) + dimensionsETF1[0]);
                        delay(3000);
                        ETAGE_ENDLESS++;
                        SCORE_ENDLESS += 50;
                        PARTIEFINIE = true;
                    }
                }
                else {
                    PARTIEFINIE = true;
                    ENDLESS = false;
                    MAINMENU = true;
                    sauvegarderScore(SCORE_ENDLESS);
                }
            }
        }
    }

    /*
      Fonction de combat.
      AFFICHAGE TEXTUEL.
    */
    void combatTUI() {
        if (!MIDBOSS) {
            println("Un ennemi approche...");
        }
        delay(3000);
        Monstre monstre = nouveauMonstre(false);
        monstre.hpact = monstre.hpmax;
        boolean combatfini = false;
        while (combatfini == false) {
            clearTerminal();
            if (j.vies == 0) {
                println(monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)" + NEWLINE);
                println(monstre.sprite + NEWLINE);
                println("Vous n'avez plus de vies.");
                delay(2000);
                println("Votre aventure se termine ici, héros.");
                delay(2000);
                println("Votre corps sans vie tombe, dans les limbes du labyrinthe d'Amala.");
                combatfini = true;
                delay(3000);
            }
            else {
                Question question = nouvelleQuestion();
                String reponse = "";
                char reponsec = ' ';
                do {
                    clearTerminal();
                    println((MIDBOSS ? "GARDIEN DE L'ÉTAGE: " : "") + monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)" + NEWLINE);
                    println(monstre.sprite);
                    println("Vos vies: " + j.vies);
                    println(question.enonce);
                    for (int idx = 0; idx < length(question.propositions); idx++) {
                        print((idx + 1) + ") " + question.propositions[idx]);
                        if (idx != (length(question.propositions) - 1)) {
                            print(" | ");
                        }
                    }
                    println();
                    print("Réponse (donner le numéro): ");
                    reponse = readString();
                    if (length(reponse) > 0) {
                        reponsec = charAt(reponse, 0);
                    }
                } while (reponsec > '4' || reponsec < '0');
                if (reponsec == question.reponse) {
                    clearTerminal();
                    int degats = random(j.minatk, j.maxatk);
                    boolean critique = false;
                    if (random() < CRITICAL_PERCENTAGE) {
                        degats = (int) (degats * 1.5);
                        critique = true;
                    }
                    monstre.hpact = monstre.hpact - degats;
                    if (monstre.hpact < 1) {
                        monstre.hpact = 0;
                        combatfini = true;
                        SCORE_ENDLESS += 30;
                    }
                    println(monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)" + NEWLINE);
                    println(monstre.sprite + NEWLINE);
                    println("Réponse correcte! " + (critique == true ? "COUP CRITIQUE! " : "") + "Vous " + (monstre.hpact == 0 ? "délivrez le coup fatal en infligeant " : "infligez ") + degats + " points de dégâts à l'ennemi!");
                    delay(3000);
                }
                else {
                    clearTerminal();
                    println(monstre.nom + " (" + monstre.hpact + "/" + monstre.hpmax + " PV)" + NEWLINE);
                    println(monstre.sprite + NEWLINE);
                    println("Réponse incorrecte! La bonne réponse était " + question.propositions[question.idxreponse - 1] + ". L'ennemi vous enlève une vie.");
                    j.vies--;
                    delay(3000);
                }
            }
        }
    }

    /*
      Renvoie si oui ou non le personnage peut
      se déplacer selon une zone donnée.
    */
    boolean peutDeplacer(char direction, String cardinals) {
        boolean res = false;
        for (int idx = 0; idx < length(cardinals); idx++) {
            if (charAt(cardinals, idx) == direction) {
                res = true;
            } 
        }
        return res;
    }

    /*
      Déplace le personnage dans le labyrinthe
      selon la direction donnée.
    */
    void deplacer(Labyrinthe l, char movement) {
        DIRS[] directions = DIRS.values();
        for (DIRS direction : directions) {
            if (movement == charAt(direction.name(), 0)) {
                j.posx += direction.dx;
                j.posy += direction.dy;
            }
        }
    }

    /*
      Clear le terminal.
    */
    void clearTerminal() {
        print(CLEAR_SCREEN);
    }

    /*
      Affichage terminal/textuel du jeu.
     */
    void amalaTUI() {
        char choix;
        clearTerminal();
        print("Entrez un nom: ");
        j.nom = readString();
        while (GAMESTATE) {
            do {
                clearTerminal();
                println(ANSI_RED + "アマラ深界" + ANSI_RESET + NEWLINE
                        + "LABYRINTH OF AMALA");
                print(NEWLINE
                      + "Choisir un mode:" + NEWLINE
                      + "1) Mode histoire" + NEWLINE
                      + "2) Mode sans fin" + NEWLINE
                      + "3) Classement" + NEWLINE
                      + "4) Quitter" + NEWLINE
                      + "> ");
                choix = readChar();
            } while (choix < '1' && choix > '4');
            if (choix == '1') {
                modeHistoireTUI();
            }
            else if (choix == '2') {
                modeEndlessTUI();
            }
            else if (choix == '3') {
                leaderboardTUI();
            }
            else {
                GAMESTATE = false;
                clearTerminal();
            }
        }
    }

    /*
      Affichage textuel du classement.
    */
    void leaderboardTUI() {
        CONSULTATION_LDB = true;
        boolean fin = false;
        PAGELDB = 1;
        LDB = sortedLeaderboard(20);
        while (CONSULTATION_LDB) {
            clearTerminal();
            fin = false;
            println("CLASSEMENT DES SCORES | MODE SANS FIN");
            println("Page n°" + PAGELDB + (PAGELDB == length(LDB, 1) ? " (dernière)" : ""));
            println("-------------------------------------" + NEWLINE);
            if (leaderboardVide(LDB) == true) {
                println("Aucune donnée n'est présente dans le leaderboard." + NEWLINE);
            }
            else {
                for (int idx = 0; idx < length(LDB, 2); idx++) {
                    if (LDB[PAGELDB - 1][idx] != null) {
                        println((((PAGELDB - 1) * 20) + idx + 1) + ") | " + LDB[PAGELDB - 1][idx]);
                    }
                    else {
                        fin = true;
                    }
                }
            }
            if (fin == true) {
                println("Fin du classement.");
            }
            println();
            println("n: page suivante | p: page précédente | q: quitter");
            print("> ");
            char c = readChar();
            if (c == 'q') {
                CONSULTATION_LDB = false;
            }
            else if (c == 'n') {
                if (PAGELDB != length(LDB, 1)) {
                    PAGELDB++;
                }
            }
            else if (c == 'p') {
                if (PAGELDB > 1) {
                    PAGELDB--;
                }
            }
        }
    }

    /*
      Vérifie si un classement donné est vide.
    */
    boolean leaderboardVide(String[][] ldb) {
        boolean vide = true;
        for (int idx = 0; idx < length(ldb, 1); idx++) {
            for (int jdx = 0; jdx < length(ldb, 2); jdx++) {
                if (ldb[idx][jdx] != null) {
                    vide = false;
                }
            }
        }
        return vide;
    }

    /*
      Affichage graphique du classement.
    */
    void leaderboardGUI() {
        LDB = sortedLeaderboard(10);
        CONSULTATION_LDB = true;
        boolean FINPAGE = false;
        int dy = 50;
        while (CONSULTATION_LDB) {
            dy = 50;
            FINPAGE = false;
            CONSULTATION_PAGE = true;
            fill(img, rgbColor(0, 0, 0));
            setNewFont(img, "JetBrains Mono", "PLAIN", 40);
            String titre = "CLASSEMENT DES SCORES | MODE SANS FIN | PAGE " + PAGELDB;
            int[] dimensionsTITRE = tailleTexte(titre, 40);
            drawString(img, titre, ((WINDOW_WIDTH - dimensionsTITRE[1]) / 2), dy);
            dy += 70;
            setNewFont(img, "JetBrains Mono", "PLAIN", 30);
            if (leaderboardVide(LDB) == true) {
                int[] dimensionsEMPTY = tailleTexte("Aucune donnée n'est présente.", 30);
                drawString(img, "Aucune donnée n'est présente.", ((WINDOW_WIDTH - dimensionsEMPTY[1]) / 2), ((WINDOW_HEIGHT - dimensionsEMPTY[0]) / 2) + 70);
            }
            else {
                for (int idx = 0; idx < length(LDB, 2); idx++) {
                    if (LDB[PAGELDB - 1][idx] != null) {
                        String texte = ((((PAGELDB - 1) * 10) + idx + 1) + ") | " + LDB[PAGELDB - 1][idx]);
                        int[] dimensionsLDB = tailleTexte(texte, 30);
                        drawString(img, texte, 20, dy);
                        dy += dimensionsLDB[0];
                    }
                    else {
                        FINPAGE = true;
                    }
                }
            }
            if (FINPAGE == true) {
                drawString(img, "Fin du classement.", 20, dy);
            }
            String options = "n: page suivante | p: page précédente | q: quitter";
            int[] dimensionsOPTIONS = tailleTexte(options, 30);
            drawString(img, options, ((WINDOW_WIDTH - dimensionsOPTIONS[1]) / 2), (WINDOW_HEIGHT - dimensionsOPTIONS[0]));
            show(img);
            while (CONSULTATION_PAGE) {
                delay(1);
            }
        }
    }

  /*
      Fonction pour interagir avec les différents
      menus graphiques du jeu.
    */
    void keyChanged(char c, String event) {
        if (equals(event, "TYPED")) {
            if (CONSULTATION_LDB) {
                if (c == 'n') {
                    if (PAGELDB != length(LDB, 1)) {
                        PAGELDB++;
                        CONSULTATION_PAGE = false;
                    }
                }
                else if (c == 'p') {
                    if (PAGELDB > 1) {
                        PAGELDB--;
                        CONSULTATION_PAGE = false;
                    }
                }
                else if (c == 'q') {
                    CONSULTATION_PAGE = false;
                    CONSULTATION_LDB = false;
                    MAINMENU = true;
                }
            }
            else if (MAINMENU) {
                if (c == '1') {
                    MAINMENU = false;
                    STORYMODE = true;
                }
                else if (c == '2') {
                    MAINMENU = false;
                    ENDLESS = true;
                }
                else if (c == '3') {
                    MAINMENU = false;
                    CONSULTATION_LDB = true;
                }
                else if (c == '4') {
                    MAINMENU = false;
                    GAMESTATE = false;
                    clearTerminal();
                }
            }
            else if (CUTSCENE) {
                c = Character.toUpperCase(c);
                if (c == 'P') {
                    CUTSCENE = false;
                }
            }
            else if (NAME_ENTRY) {
                if (alphanumeric(c)) {
                    name += c;
                    ENTRY = false;
                }
                else if (c == BACKSPACE && length(name) != 0) {
                    name = substring(name, 0, length(name) - 1);
                    ENTRY = false;
                }
                else if (c == NEWLINE) {
                    j.nom = name;
                    ENTRY = false;
                    NAME_ENTRY = false;
                    MAINMENU = true;
                }
            }
            else if (ENDLESS || STORYMODE) {
                if (ACTION) {
                    c = Character.toUpperCase(c);
                    if (peutDeplacer(c, getDirections(l.labyrinthe[j.posx][j.posy], true))) {
                        deplacer(l, getCardinal(c));
                        l.decouvert[j.posx][j.posy] = true;
                        decouverteAdjacentes(l);
                        j.encounter_seed += random(1, 5);
                        ACTION = false;
                    }
                }
            }
            else if (ACTIONCOMBAT) {
                if (c >= '1' && c <= '4') {
                    if (c == question.reponse) {
                        REPONSE = true;
                        ACTIONCOMBAT = false;
                    }
                    else {
                        ACTIONCOMBAT = false;
                    }
                }
            }
        }
    }

    /*
      Vérification de si un caractère est alphanumérique.
    */
    boolean alphanumeric(char c) {
        return ((c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9'));
    }

    /*
      Renvoie le nombre de pages nécessaires pour le
      classement des scores selon un nombre de scores
      par page donné.
     */
    int leaderboardPages(int scores, int pageSize) {
        return (scores + pageSize - 1) / pageSize;
    }

    /*
      Renvoie le classement du mode sans fin,
      trié, avec le nombre de pages donné.
    */
    String[][] sortedLeaderboard(int pageSize) {
        CSVFile ldb = loadCSV("resources/tables/SCORES.csv");
        String[][] fullLeaderboard = new String[rowCount(ldb)][2];
        String[][] pagedLeaderboard = new String[leaderboardPages(rowCount(ldb), pageSize)][pageSize];
        for (int idx = 0; idx < rowCount(ldb); idx++) {
            fullLeaderboard[idx][0] = getCell(ldb, idx, 0);
            fullLeaderboard[idx][1] = getCell(ldb, idx, 1);
        }
        int borneMax = length(fullLeaderboard, 1);
        boolean permutation = true;
        while (permutation) {
            permutation = false;
            for (int idx = 0; idx < borneMax - 1; idx++) {
                if (Integer.valueOf(fullLeaderboard[idx][1]) < Integer.valueOf(fullLeaderboard[idx + 1][1])) {
                    permutation = true;
                    String[] swap = fullLeaderboard[idx];
                    fullLeaderboard[idx] = fullLeaderboard[idx + 1];
                    fullLeaderboard[idx + 1] = swap;
                }
            }
        }
        int idxPage = 0;
        for (int idx = 0; idx < length(fullLeaderboard, 1); idx++) {
            if (idx % pageSize == 0 && idx > 0) {
                idxPage++;
            }
            pagedLeaderboard[idxPage][idx - (idxPage * pageSize)] = fullLeaderboard[idx][0] + ": " + fullLeaderboard[idx][1] + " points";
        }
        return pagedLeaderboard;
    }

    /*
      Menu de l'entrée du nom du joueur.
      AFFICHAGE GRAPHIQUE.
    */
    void entreeNom() {
        while (NAME_ENTRY) {
            ENTRY = true;
            fill(img, rgbColor(0, 0, 0));
            setNewFont(img, "JetBrains Mono", "PLAIN", 50);
            int[] dimensionsQUESTION = tailleTexte("Entrez votre nom ci-dessous:", 50);
            drawString(img, "Entrez votre nom ci-dessous:", ((WINDOW_WIDTH - dimensionsQUESTION[1]) / 2), ((WINDOW_HEIGHT - dimensionsQUESTION[0]) / 2));
            int[] dimensionsENTRY = tailleTexte((name + "_"), 50);
            drawString(img, (name + "_"), ((WINDOW_WIDTH - dimensionsENTRY[1]) / 2), ((WINDOW_HEIGHT - dimensionsENTRY[0]) / 2) + dimensionsENTRY[0]);
            show(img);
            while (ENTRY) {
                delay(1);
            }
        }
    }

    /*
      Affichage graphique du jeu.
     */
    void amalaGUI() {
        img = newImage(WINDOW_WIDTH, WINDOW_HEIGHT);
        while (GAMESTATE) {
            if (MAINMENU) {
                mainMenuGUI();
            }
            else if (CONSULTATION_LDB) {
                PAGELDB = 1;
                leaderboardGUI();
            }
            else if (ENDLESS) {
                modeEndlessGUI();
            }
            else if (NAME_ENTRY) {
                entreeNom();
            }
            else if (STORYMODE) {
                modeHistoireGUI();
            }
        }
    }

    /*
      Renvoie les dimensions en pixels d'un texte
      dans une taille donnée.
      POLICE JETBRAINS MONO SEULEMENT.
    */
    int[] tailleTexte(String texte, int fontsize) {
        int[] dimensions = new int[2];
        double height = (FONT_HEIGHT_VALUE * fontsize);
        double width = (FONT_WIDTH_VALUE * length(texte)) * fontsize;
        if (height - ((int) height) >= 0.1) {
            height++;
        }
        if (width - ((int) width) >= 0.1) {
            width++;
        }
        dimensions[0] = (int) height + 1;
        dimensions[1] = (int) width + 1;
        return dimensions;
    }

    /*
      Remplace les occurences de \x1b par
      \033 pour les sprites ASCII
      en couleur.
     */
    String correctASCII(String baseASCII) {
        String ASCII = "";
        for (int idx = 0; idx < length(baseASCII); idx++) {
            if (charAt(baseASCII, idx) != '\\') {
                ASCII += charAt(baseASCII, idx);
            }
            else {
                ASCII += "\033";
                idx += 3;
            }
        }
        return ASCII;
    }

    /*
      Fonction principale.
    */
    void algorithm() {
        String choix;
        char cchoix = ' ';
        GAMESTATE = true;
        while (GAMESTATE) {
            do {
                clearTerminal();
                print(ANSI_RED + "アマラ深界" + ANSI_RESET + NEWLINE + "LABYRINTH OF AMALA" + NEWLINE + NEWLINE
                      + "Sélectionner l'affichage:" + NEWLINE
                      + "1) Affichage terminal" + NEWLINE
                      + "2) Affichage graphique" + NEWLINE
                      + "> ");
                choix = readString();
                if (length(choix) != 0) {
                    cchoix = charAt(choix, 0);
                }
            } while (cchoix != '1' && cchoix != '2');
            if (cchoix == '1') {
                amalaTUI();
            }

            else {
                NAME_ENTRY = true;
                amalaGUI();
            }
        }
    }

    // FONCTION TEST
    void testGetDirections() {
        assertEquals("NSEW", getDirections(15, false));
        assertEquals("SW", getDirections(10, false));
        assertEquals("NE", getDirections(5, false));
        assertEquals("", getDirections(0, false));
    }

    // FONCTION TEST
    void testPeutDeplacer() {
        assertEquals(true, peutDeplacer('N', "NSW"));
        assertEquals(true, peutDeplacer('E', "SE"));
        assertEquals(true, peutDeplacer('S', "NSEW"));
        assertEquals(false, peutDeplacer('W', "NE"));
        assertEquals(false, peutDeplacer('S', ""));
    }
}
