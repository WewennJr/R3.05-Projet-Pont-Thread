package iut.gon.pont;


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
