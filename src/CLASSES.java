class Labyrinthe {
    int longueur;
    int largeur;
    int[][] labyrinthe;
    boolean[][] decouvert;
}

class Joueur {
    String nom;
    int vies;
    int posx;
    int posy;
    int encounter_seed;
    int maxatk;
    int minatk;
    int score;
}

class Monstre {
    String nom;
    int hpmax;
    int hpact;
    String sprite;
}

class Question {
    String enonce;
    String[] propositions;
    char reponse;
    int idxreponse;
}

enum DIRS {
    N(1, 0, -1, 'Z'), S(2, 0, 1, 'S'), E(4, 1, 0, 'D'), W(8, -1, 0, 'Q');
    final int bit;
    final int dx;
    final int dy;
    final char key;
    DIRS inverse;

    static {
        N.inverse = S;
        S.inverse = N;
        E.inverse = W;
        W.inverse = E;
    }

    DIRS(int bit, int dx, int dy, char key) {
        this.bit = bit;
        this.dx = dx;
        this.dy = dy;
        this.key = key;
    }
}
