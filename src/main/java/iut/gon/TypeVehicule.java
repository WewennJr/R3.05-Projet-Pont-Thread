package iut.gon;

/**
 * Les différents types de véhicules qui peuvent circuler sur le pont.
 * Chaque type occupe un nombre de segments consécutifs différent.
 */
public enum TypeVehicule {
    /** Une voiture simple, occupe 1 seul segment à la fois. */
    VOITURE(1, 'v'),
    /** Un camion, occupe 2 segments consécutifs. */
    CAMION(2, 'c'),
    /** Un convoi exceptionnel, occupe 4 segments consécutifs et bloque le sens opposé. */
    CONVOI(4, 'C');

    /** Nombre de segments consécutifs occupés par ce type de véhicule. */
    public final int longueur;
    /** Symbole utilisé pour l'affichage console. */
    public final char symbole;

    TypeVehicule(int longueur, char symbole) {
        this.longueur = longueur;
        this.symbole = symbole;
    }
}
