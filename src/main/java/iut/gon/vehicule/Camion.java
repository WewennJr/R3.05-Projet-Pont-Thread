package iut.gon.vehicule;

import iut.gon.pont.Pont;

/**
 * Camion traversant le pont : il occupe deux segments consécutifs.
 * Il doit réserver ses deux segments ensemble à l'entrée du pont et ne peut
 * pas avancer tant que les deux segments dont il a besoin ne sont pas libres.
 * Il n'occupe jamais un segment seul en attendant le second.
 */
public class Camion extends Vehicule {

    private static final int TAILLE = 2;
    private static final long DUREE_SEGMENT = 700;

    public Camion(Pont pont, Sens sens, long delaiArrivee) {
        super('C', pont, sens, delaiArrivee, TAILLE, DUREE_SEGMENT);
    }
}
