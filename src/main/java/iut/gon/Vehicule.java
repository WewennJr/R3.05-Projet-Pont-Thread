package iut.gon;

/**
 * Thread représentant un véhicule : voiture (1 segment), camion (2 segments),
 * convoi exceptionnel (4 segments), ou véhicule prioritaire (ambulance /
 * pompiers) qui bénéficie de la bande d'urgence.
 */
public class Vehicule extends Thread {

    /** Le pont partagé. */
    public final Pont pont;
    /** Identifiant unique du véhicule. */
    public final int id;
    /** Type du véhicule (voiture, camion, convoi). */
    public final TypeVehicule type;
    /** Sens de circulation du véhicule. */
    public final SensVehicule sens;
    /** true si c'est un véhicule prioritaire. */
    public final boolean prioritaire;
    /** Symbole d'affichage sur le pont. */
    public final char symbole;
    /** Longueur en segments (1 pour les prioritaires). */
    public final int longueur;

    /** Indice du segment de tête du véhicule (-1 = pas encore entré). */
    public volatile int positionTete = -1;
    /** true tant que le véhicule est sur le pont. */
    public volatile boolean surPont = false;
    /** Numéro d'arrivée à l'entrée du pont (attribué par le pont, sert à départager les convois). */
    public int ordreArrivee;

    /** Construit un véhicule. */
    public Vehicule(Pont pont, int id, TypeVehicule type, SensVehicule sens, boolean prioritaire) {
        this.pont = pont;
        this.id = id;
        this.type = type;
        this.sens = sens;
        this.prioritaire = prioritaire;
        this.symbole = prioritaire ? 'P' : type.symbole;
        this.longueur = prioritaire ? 1 : type.longueur;
        setName("Vehicule-" + id + "(" + type + ")");
    }

    /** Cycle de vie du véhicule : entrée, traversée segment par segment, sortie. */
    @Override
    public void run() {
        try {
            pont.arriverVehicule(this);
            pont.entrerVehicule(this);
            traverser();
            pont.quitterVehicule(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Traversée du pont. Un prioritaire glisse sur la bande d'urgence (pas de
     * réservation de segments), les autres avancent un segment à la fois.
     */
    private void traverser() throws InterruptedException {
        if (prioritaire) {
            int pas = (sens == SensVehicule.GAUCHE_DROITE) ? 1 : -1;
            int depart = (pas == 1) ? 0 : Pont.NB_SEGMENTS - 1;
            for (int i = 0; i < Pont.NB_SEGMENTS; i++) {
                positionTete = depart + pas * i;
                Thread.sleep(250);
            }
            return;
        }
        while (surPont) {
            Thread.sleep(vitesseSegment());
            pont.avancer(this);
        }
    }

    /** @return le temps de parcours d'un segment (ms) selon le type de véhicule. */
    private int vitesseSegment() {
        return switch (type) {
            case VOITURE -> 400;
            case CAMION -> 500;
            case CONVOI -> 800;
        };
    }
}