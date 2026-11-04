package iut.gon.pont;

/**
 * Un segment du pont (S1, S2, ..., SN).
 * Constitue une ressource partagée dont l'occupation est contrôlée :
 * un seul véhicule peut l'occuper à la fois.
 */
public class Segment {

    /** Numéro du segment sur le pont (1..N). */
    private final int numero;

    /** Indique si le segment est actuellement occupé par un véhicule. */
    private boolean occupe = false;

    /**
     * Crée un segment libre numéroté.
     * @param numero le numéro du segment (1..N)
     */
    public Segment(int numero) {
        this.numero = numero;
    }

    /**
     * Tente de réserver le segment de façon atomique.
     * @return true si la réservation a réussi, false si le segment est déjà occupé
     */
    public synchronized boolean tenterReservation() {
        if (occupe) {
            return false;
        }
        occupe = true;
        return true;
    }

    /**
     * Libère le segment et réveille les véhicules en attente.
     */
    public synchronized void liberer() {
        occupe = false;
        notifyAll();
    }

    /**
     * @return true si le segment est occupé par un véhicule
     */
    public synchronized boolean estOccupe() {
        return occupe;
    }

    public int getNumero() {
        return numero;
    }

    @Override
    public String toString() {
        return occupe ? "[####]" : "[____]";
    }
}
