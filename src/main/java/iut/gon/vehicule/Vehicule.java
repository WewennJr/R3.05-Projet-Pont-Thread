package iut.gon.vehicule;

import iut.gon.pont.Pont;

/**
 * Classe abstraite représentant un véhicule devant traverser le pont.
 * Un véhicule est un thread Java indépendant : il demande l'entrée au pont,
 * avance segment par segment (en attendant que le segment suivant soit libre) et
 * libère le pont à sa sortie.
 *
 * <p>Un véhicule occupe un bloc de segment(s) consécutifs : 1 pour une voiture,
 * 2 pour un camion, 4 pour un convoi exceptionnel. Il réserve toujours la totalité
 * de son bloc à l'entrée puis avance d'un cran à la fois, ne relâchant jamais
 * l'arrière de son bloc avant d'avoir réservé le segment suivant.</p>
 */
public abstract class Vehicule extends Thread {

    private static int numeroSuivant = 1;

    protected final Pont pont;
    protected final Sens sens;
    protected final char glyphe;
    protected final int nbSegments;
    protected final long dureeSegment;
    protected final long delaiArrivee;
    private int positionLocale = -1;

    protected Vehicule(char glyphe, Pont pont, Sens sens,
                       long delaiArrivee, int nbSegments, long dureeSegment) {
        super("Vehicule-" + numeroSuivant);
        this.numeroSuivant++;
        this.glyphe = glyphe;
        this.pont = pont;
        this.sens = sens;
        this.delaiArrivee = delaiArrivee;
        this.nbSegments = nbSegments;
        this.dureeSegment = dureeSegment;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(delaiArrivee);
            entrer();
            traverser();
            quitter();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void entrer() throws InterruptedException {
        pont.entrer(this);
    }

    private void traverser() throws InterruptedException {
        boolean prochaineEtapePossible = true;
        while (prochaineEtapePossible) {
            Thread.sleep(dureeSegment);
            prochaineEtapePossible = pont.avancer(this);
        }
    }

    protected void quitter() {
        pont.sortir(this);
    }

    public char getGlyphe() { return glyphe; }

    public int getNbSegments() { return nbSegments; }

    public Sens getSens() { return sens; }

    public boolean getBloqueSensOppose() { return false; }

    public String getNom() { return getName(); }

    public int getPositionLocale() { return positionLocale; }

    public void setPositionLocale(int positionLocale) { this.positionLocale = positionLocale; }

    @Override
    public String toString() {
        return String.format("%s (%d segment(s), %s)", getNom(), nbSegments, sens.libelle);
    }
}
