package iut.gon.pont;

/**
 * Les quatre états du pont mobile.
 * Les transitions autorisées sont :
 * FERMÉ → OUVERTURE → OUVERT → FERMETURE → FERMÉ.
 */
public enum EtatPont {

    /** Le pont est fermé à la circulation des bateaux et peut accueillir les véhicules. */
    FERME,

    /** Le pont est en cours d'ouverture : aucun nouveau véhicule terrestre ne peut entrer. */
    OUVERTURE,

    /** Le pont est ouvert pour permettre le passage des bateaux. */
    OUVERT,

    /** Le pont est en cours de fermeture : aucun véhicule terrestre ne peut entrer avant le retour à FERME. */
    FERMETURE;

    /**
     * Indique si l'état autorise un nouveau véhicule terrestre à entrer sur le pont.
     * @return true uniquement quand le pont est FERME
     */
    public boolean autoriseVehicule() {
        return this == FERME;
    }
}
