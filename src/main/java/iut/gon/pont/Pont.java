package iut.gon.pont;

/**
 * Représente le pont mobile partagé entre les véhicules et les bateaux.
 * <p>
 * Le pont est divisé en N segments consécutifs (S1, S2, ..., SN).
 * Il y a deux sens de circulation : les véhicules de gauche à droite (G→D)
 * parcourent les segments S1 → SN, ceux de droite à gauche (D→G) les parcourent
 * dans l'ordre inverse SN → S1. Un véhicule du sens (D→G) utilise l'index
 * (N - 1 - position) du tableau du sens opposé.
 * <p>
 * Deux véhicules de sens opposés ne peuvent jamais occuper le même segment
 * (pas de dépassement, ni de demi-tour sur le pont).
 */
public class Pont {

    /** Nombre de segments du pont (N). */
    public static final int N_SEGMENTS = 8;

    /** Sens de circulation gauche → droite : les indices progressent de 0 vers N-1. */
    public static final int SENS_GD = 0;

    /** Sens de circulation droite → gauche : les indices régressent de N-1 vers 0. */
    public static final int SENS_DG = 1;

    /** Les segments du pont, un par sens de circulation. */
    private final Segment[][] segments = new Segment[2][N_SEGMENTS];

    /** État courant du pont. */
    private EtatPont etat = EtatPont.FERME;

    /**
     * Crée le pont : N segments libres dans chaque sens, pont FERMÉ.
     */
    public Pont() {
        for (int sens = 0; sens < 2; sens++) {
            for (int i = 0; i < N_SEGMENTS; i++) {
                segments[sens][i] = new Segment(i + 1);
            }
        }
    }

    public EtatPont getEtat() {
        synchronized (this) {
            return etat;
        }
    }

    /**
     * Réserve un seul segment (cas d'une voiture) : bloque le thread appelant
     * jusqu'à ce que le segment soit libre et que le pont soit FERME.
     *
     * @param sens   SENS_GD ou SENS_DG
     * @param indice indice du segment à réserver dans ce sens
     * @throws InterruptedException si le thread est interrompu pendant l'attente
     */
    public void reserverSegment(int sens, int indice) throws InterruptedException {
        reserverSegments(sens, indice, 1);
    }

    /**
     * Réserve plusieurs segments consécutifs de façon atomique (2 pour un camion,
     * 4 pour un convoi exceptionnel). Un camion ne peut pas occupper un segment
     * et attendre le second : la réservation complète est faite d'un coup.
     * Le thread appelant est bloqué jusqu'à ce que ce soit possible.
     *
     * @param sens   SENS_GD ou SENS_DG
     * @param depart indice du premier segment à réserver
     * @param nombre nombre de segments consécutifs à réserver (1, 2 ou 4)
     * @throws InterruptedException si le thread est interrompu pendant l'attente
     */
    public void reserverSegments(int sens, int depart, int nombre) throws InterruptedException {
        synchronized (this) {
            while (true) {
                // Exclusivité : le pont doit être FERME et tous les segments libres.
                // Tout est fait dans un seul bloc synchronisé : deux véhicules ne
                // peuvent donc pas réserver les mêmes segments en même temps.
                if (etat.autoriseVehicule() && sontLibres(sens, depart, nombre)) {
                    for (int i = 0; i < nombre; i++) {
                        segments[sens][depart + i].tenterReservation();
                    }
                    return;
                }
                wait(); // libère le moniteur et attend une libération / changement d'état
            }
        }
    }

    /**
     * Tente une réservation immédiate, sans bloquer (utile pour la bande d'urgence
     * et les tests).
     *
     * @param sens   SENS_GD ou SENS_DG
     * @param depart indice du premier segment à réserver
     * @param nombre nombre de segments consécutifs
     * @return true si tous les segments demandés ont été réservés
     */
    public boolean tenterReservationSegments(int sens, int depart, int nombre) {
        synchronized (this) {
            if (!etat.autoriseVehicule() || !sontLibres(sens, depart, nombre)) {
                return false;
            }
            for (int i = 0; i < nombre; i++) {
                segments[sens][depart + i].tenterReservation();
            }
            return true;
        }
    }

    /**
     * Libère un ou plusieurs segments consécutifs précédemment réservés
     * et réveille les véhicules qui attendaient ces segments.
     *
     * @param sens   SENS_GD ou SENS_DG
     * @param depart indice du premier segment à libérer
     * @param nombre nombre de segments consécutifs à libérer
     */
    public void libererSegments(int sens, int depart, int nombre) {
        synchronized (this) {
            for (int i = 0; i < nombre; i++) {
                segments[sens][depart + i].liberer();
            }
            notifyAll();
        }
    }

    /**
     * @param sens   SENS_GD ou SENS_DG
     * @param indice indice du segment
     * @return true si le segment est occupé
     */
    public boolean segmentEstOccupe(int sens, int indice) {
        synchronized (this) {
            return segments[sens][indice].estOccupe();
        }
    }

    /**
     * @return true si tous les segments des deux sens sont libres
     *         (condition nécessaire pour démarrer une OUVERTURE)
     */
    public boolean estVide() {
        synchronized (this) {
            for (int sens = 0; sens < 2; sens++) {
                for (Segment s : segments[sens]) {
                    if (s.estOccupe()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * Change l'état du pont en respectant les transitions autorisées :
     * FERME → OUVERTURE → OUVERT → FERMETURE → FERME.
     * L'ouverture ne peut démarrer que si le pont est complètement vide.
     *
     * @param nouvelEtat le nouvel état demandé
     * @return true si la transition a été effectuée
     */
    public boolean changerEtat(EtatPont nouvelEtat) {
        synchronized (this) {
            boolean transitionValide = switch (etat) {
                case FERME -> nouvelEtat == EtatPont.OUVERTURE && estVide();
                case OUVERTURE -> nouvelEtat == EtatPont.OUVERT;
                case OUVERT -> nouvelEtat == EtatPont.FERMETURE;
                case FERMETURE -> nouvelEtat == EtatPont.FERME;
            };
            if (transitionValide) {
                etat = nouvelEtat;
                notifyAll(); // les véhicules peuvent réessayer de réserver
            }
            return transitionValide;
        }
    }

    /**
     * Renvoie l'indice du tableau pour un véhicule circulant dans le sens (D→G),
     * qui parcourt les segments dans l'ordre inverse : il entre au segment SN.
     *
     * @param positionDepuisEntree position du véhicule depuis son entrée (0, 1, ...)
     * @return l'indice du segment dans le tableau du pont
     */
    public static int indicePourSENS_dG(int positionDepuisEntree) {
        return N_SEGMENTS - 1 - positionDepuisEntree;
    }

    /**
     * Vérifie qu'un intervalle consécutif de segments est entièrement libre.
     * Doit être appelé en tenant le moniteur du pont.
     */
    private boolean sontLibres(int sens, int depart, int nombre) {
        if (depart < 0 || depart + nombre > N_SEGMENTS) {
            return false;
        }
        for (int i = 0; i < nombre; i++) {
            if (segments[sens][depart + i].estOccupe()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        synchronized (this) {
            StringBuilder sb = new StringBuilder("Pont [").append(etat).append("]\n");
            sb.append("  :←    ");
            for (int i = N_SEGMENTS - 1; i >= 0; i--) sb.append(segments[SENS_DG][i]);
            sb.append('\n');
            sb.append("  →:    ");
            for (Segment s : segments[SENS_GD]) sb.append(s);
            sb.append('\n');
            return sb.toString();
        }
    }
}
