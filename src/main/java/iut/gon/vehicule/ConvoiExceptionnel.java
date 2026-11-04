package iut.gon.vehicule;

import iut.gon.pont.Pont;


public class ConvoiExceptionnel extends Vehicule {

    private static final int TAILLE = 4;
    private static final long DUREE_SEGMENT = 1200;

    public ConvoiExceptionnel(Pont pont, Sens sens, long delaiArrivee) {
        super('X', pont, sens, delaiArrivee, TAILLE, DUREE_SEGMENT);
    }

    @Override
    public boolean getBloqueSensOppose() { return true; }
}
