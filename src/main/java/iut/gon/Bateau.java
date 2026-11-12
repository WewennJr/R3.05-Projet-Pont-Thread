package iut.gon;

/**
 * Thread représentant un bateau. Tous les bateaux partagent une file d'attente
 * FIFO gérée par le pont : le premier arrivé est le premier à passer.
 * L'ouverture du pont est déclenchée par le pont lui-même (arrivée d'un bateau
 * ou Timer du délai minimal) ; le bateau traverse seul (un seul bateau à la fois).
 */
public class Bateau extends Thread {

    /** Le pont partagé. */
    public final Pont pont;
    /** Identifiant unique du bateau. */
    public final int id;
    /** Direction de circulation (nord -> sud ou sud -> nord). */
    public final Direction direction;
    /** Symbole d'affichage. */
    public final char symbole = 'B';

    /** Position du bateau : -1 = en approche ou sorti côté gauche, 0..7 = sous le pont, 8 = en approche ou sorti côté droit. */
    public volatile int position;

    /**
     * Construit un bateau. Il se met en approche du pont du côté de son sens
     * de circulation, à gauche pour N→S, à droite pour S→N.
     */
    public Bateau(Pont pont, int id, Direction direction) {
        this.pont = pont;
        this.id = id;
        this.direction = direction;
        this.position = (direction == Direction.NORD_SUD) ? -1 : Pont.NB_SEGMENTS;
        setName("Bateau-" + id);
    }

    /** Cycle de vie du bateau : file FIFO, attente de l'ouverture, traversée, sortie. */
    @Override
    public void run() {
        try {
            pont.arriverBateau(this);
            pont.ouvrirLePont(this);
            pont.monTour(this);
            traverser();
            pont.quitterBateau(this); // si c'est le dernier, le pont passe à FERMETURE (Timer)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Traversée sous le pont, segment par segment, puis complète sortie de l'espace du pont. */
    private void traverser() throws InterruptedException {
        int pas = (direction == Direction.NORD_SUD) ? 1 : -1;
        int depart = (pas == 1) ? 0 : Pont.NB_SEGMENTS - 1;
        for (int i = 0; i < Pont.NB_SEGMENTS; i++) {
            position = depart + pas * i;
            Thread.sleep(400);
        }
        position = (direction == Direction.NORD_SUD) ? Pont.NB_SEGMENTS : -1;
    }
}