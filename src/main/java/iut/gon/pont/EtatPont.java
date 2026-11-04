package iut.gon.pont;

/**
 * Les quatre états du pont mobile.
 * Les transitions autorisées sont :
 * FERMÉ → OUVERTURE → OUVERT → FERMETURE → FERMÉ.
 */
public enum EtatPont {

    FERME,
    OUVERTURE,
    OUVERT,
    FERMETURE;

    public boolean autoriseVehicule() {
        return this == FERME;
    }
}
