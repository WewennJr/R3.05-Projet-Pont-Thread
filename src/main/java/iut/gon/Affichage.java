package iut.gon;

import java.util.List;
import java.util.Objects;

/**
 * Thread d'affichage : imprime la scène complète du pont dans la console
 * (System.out.println uniquement), régulièrement, pour suivre la simulation.
 * La scène n'est réimprimée que si elle a réellement changé depuis la
 * dernière impression, pour garder la console lisible.
 */
public class Affichage extends Thread {

    /** Le pont partagé (source de toutes les informations affichées). */
    private final Pont pont;
    /** Scène de l'impression précédente (pour ne pas réimprimer pareil). */
    private List<String> precedent = List.of();
    /** Instant de départ de la simulation. */
    private static final long DEBUT = System.nanoTime();

    /** true : redessine la scène sur place (séquences ANSI) au lieu de faire défiler la console. */
    private final boolean ansi;

    /** Construit le thread d'affichage (défilement simple) pour le pont donné. */
    public Affichage(Pont pont) {
        this(pont, false);
    }

    /** Construit le thread d'affichage ; ansi = true pour animer sur place. */
    public Affichage(Pont pont, boolean ansi) {
        this.pont = pont;
        this.ansi = ansi;
        setDaemon(true);
        setName("Affichage");
    }

    /** Boucle d'animation : imprime la scène toutes les secondes si elle a changé. */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            List<String> lignes = pont.construireLignes();
            if (!Objects.equals(lignes, precedent)) {
                afficher(lignes);
                precedent = lignes;
            }
            try {
                // Rafraîchissement toutes les 200 ms : chaque véhicule/bateau
                // (vitesse ≥ 400 ms/segment) reste visible pendant au moins
                // 2 images : chaque case traversée est donc bien affichée.
                Thread.sleep(200);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /** Imprime une scène complète : titre horodaté, lignes du pont, ligne vide. */
    private void afficher(List<String> lignes) {
        double t = (System.nanoTime() - DEBUT) / 1e9;
        StringBuilder sb = new StringBuilder();
        if (ansi) sb.append("\033[H");                       // curseur en haut à gauche
        else sb.append('\n');
        sb.append("=============== SCÈNE DU PONT : ").append(String.format("%6.1f s", t))
                .append(" ===============").append(ansi ? "\033[K" : "").append('\n');
        for (String ligne : lignes) sb.append(ligne).append(ansi ? "\033[K" : "").append('\n');
        if (ansi) sb.append("\033[J");                       // efface le reste de l'écran
        else sb.append('\n');
        System.out.print(sb);
        System.out.flush();
    }
}