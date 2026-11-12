package iut.gon;

/**
 * Les deux sens de circulation terrestre sur le pont.
 * Chaque sens possède sa propre voie (ses propres segments).
 */
public enum SensVehicule {
    /** Circulation de gauche vers la droite (segments parcourus de S1 vers SN). */
    GAUCHE_DROITE,
    /** Circulation de droite vers la gauche (segments parcourus de SN vers S1). */
    DROITE_GAUCHE
}
