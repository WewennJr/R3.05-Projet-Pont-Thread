package iut.gon;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Programme principal de la simulation.
 * <p>
 * Crée le pont, lance l'animation console, puis génère pendant la durée
 * de la simulation des flux de véhicules (voitures, camions, convois,
 * véhicules prioritaires) et de bateaux dans les 4 sens de circulation.
 * Arguments facultatifs (dans n'importe quel ordre) : la durée en ms
 * (100 000 par défaut) et le mot "ansi" pour animer la console sur place.
 */
public class Main {

    /** Durée totale de la simulation en ms (100 s). */
    private static final long DUREE_SIMULATION = 100_000;

    /** Point d'entrée de la simulation. */
    public static void main(String[] args) throws InterruptedException {
        long duree = DUREE_SIMULATION;
        boolean ansi = false;
        for (String a : args) {
            if (a.equalsIgnoreCase("ansi")) ansi = true;
            else duree = Long.parseLong(a);
        }

        Pont pont = new Pont();
        if (ansi) System.out.print("\033[2J");
        new Affichage(pont, ansi).start();

        List<Thread> threadsVehicules = new ArrayList<>();
        List<Thread> threadsBateaux = new ArrayList<>();

        Thread generateurVehicules = new Thread(() -> genererVehicules(pont, threadsVehicules), "Gen-Vehicules");
        Thread generateurBateaux = new Thread(() -> genererBateaux(pont, threadsBateaux), "Gen-Bateaux");
        generateurVehicules.start();
        generateurBateaux.start();

        // Fin de la simulation : plus de nouveaux véhicules ni bateaux,
        // et interruption des parties encore en attente (pont en délai, files...).
        Thread.sleep(duree);
        generateurVehicules.interrupt();
        generateurBateaux.interrupt();
        generateurVehicules.join(2000);
        generateurBateaux.join(2000);
        for (Thread t : threadsVehicules) t.interrupt();
        for (Thread t : threadsBateaux) t.interrupt();
        for (Thread t : threadsVehicules) t.join(2000);
        for (Thread t : threadsBateaux) t.join(2000);

        System.out.println("\n=== Simulation terminée ===");
    }

    /**
     * Génère régulièrement de nouveaux véhicules du type choisi au hasard
     * (majorité de voitures, quelques camions, rares convois exceptionnels,
     * ~6% de véhicules prioritaires) dans les deux sens de circulation.
     */
    private static void genererVehicules(Pont pont, List<Thread> threads) {
        Random rand = new Random();
        int id = 1;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                double tirage = rand.nextDouble();
                TypeVehicule type = tirage < 0.50 ? TypeVehicule.VOITURE
                        : tirage < 0.75 ? TypeVehicule.CAMION
                        : tirage < 0.82 ? TypeVehicule.CONVOI
                        : TypeVehicule.VOITURE;
                boolean prioritaire = rand.nextInt(100) < 6;
                SensVehicule sens = rand.nextBoolean()
                        ? SensVehicule.GAUCHE_DROITE : SensVehicule.DROITE_GAUCHE;
                Vehicule v = new Vehicule(pont, id++, type, sens, prioritaire);
                threads.add(v);
                v.start();
                Thread.sleep(1500 + rand.nextInt(2500));
            }
        } catch (InterruptedException e) {
            // fin de la génération
        }
    }

    /**
     * Génère régulièrement de nouveaux bateaux dans les deux directions,
     * tous enregistrés dans la même file FIFO du pont.
     */
    private static void genererBateaux(Pont pont, List<Thread> threads) {
        Random rand = new Random();
        int id = 1;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Direction direction = rand.nextBoolean() ? Direction.NORD_SUD : Direction.SUD_NORD;
                Bateau b = new Bateau(pont, id++, direction);
                threads.add(b);
                b.start();
                Thread.sleep(6000 + rand.nextInt(5000));
            }
        } catch (InterruptedException e) {
            // fin de la génération
        }
    }
}