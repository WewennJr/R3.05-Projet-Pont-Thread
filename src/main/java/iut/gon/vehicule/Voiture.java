package iut.gon.vehicule;

import iut.gon.pont.Pont;

/**
 * Voiture traversant le pont : elle occupe un seul segment à la fois
 * (parcouru dans son sens de circulation).
 */
public class Voiture extends Vehicule {

    private static final int TAILLE = 1;
    private static final long DUREE_SEGMENT = 400;

    public Voiture(Pont pont, Sens sens, long delaiArrivee) {
        super('V', pont, sens, delaiArrivee, TAILLE, DUREE_SEGMENT);
    }
}
