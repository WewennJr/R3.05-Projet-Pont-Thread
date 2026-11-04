package iut.gon.pont;

/**
 * Un segment du pont (S1, S2, ..., SN).
 * Constitue une ressource partagée dont l'occupation est contrôlée :
 * un seul véhicule peut l'occuper à la fois.
 */
public class Segment {

    private final int numero;
    private boolean occupe = false;

    public Segment(int numero) {
        this.numero = numero;
    }

    public synchronized boolean tenterReservation() {
        if (occupe) {
            return false;
        }
        occupe = true;
        return true;
    }

    public synchronized void liberer() {
        occupe = false;
        notifyAll();
    }

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
