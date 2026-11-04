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

    public static final int N_SEGMENTS = 8;
    public static final int SENS_GD = 0;
    public static final int SENS_DG = 1;

    private final Segment[][] segments = new Segment[2][N_SEGMENTS];

    private EtatPont etat = EtatPont.FERME;

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

    public void reserverSegment(int sens, int indice) throws InterruptedException {
        reserverSegments(sens, indice, 1);
    }

    public void reserverSegments(int sens, int depart, int nombre) throws InterruptedException {
        synchronized (this) {
            while (true) {
                if (etat.autoriseVehicule() && sontLibres(sens, depart, nombre)) {
                    for (int i = 0; i < nombre; i++) {
                        segments[sens][depart + i].tenterReservation();
                    }
                    return;
                }
                wait();
            }
        }
    }

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

    public void libererSegments(int sens, int depart, int nombre) {
        synchronized (this) {
            for (int i = 0; i < nombre; i++) {
                segments[sens][depart + i].liberer();
            }
            notifyAll();
        }
    }

    public boolean segmentEstOccupe(int sens, int indice) {
        synchronized (this) {
            return segments[sens][indice].estOccupe();
        }
    }

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
                notifyAll();
            }
            return transitionValide;
        }
    }

    public static int indicePourSENS_dG(int positionDepuisEntree) {
        return N_SEGMENTS - 1 - positionDepuisEntree;
    }

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
