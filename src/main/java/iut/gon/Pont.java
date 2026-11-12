package iut.gon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Le pont mobile, ressource partagée entre les véhicules et les bateaux.
 * <p>
 * Cette classe contient tout l'état partagé du système et joue le rôle de
 * moniteur : toutes les méthodes publiques sont synchronized.
 * <p>
 * Elle gère :
 * <ul>
 *   <li>la machine à états : FERME -> OUVERTURE -> OUVERT -> FERMETURE -> FERME ;</li>
 *   <li>les segments des deux voies (un tableau par sens de circulation) ;</li>
 *   <li>les convois exceptionnels (blocage du sens opposé) ;</li>
 *   <li>les véhicules prioritaires (bande d'urgence) ;</li>
 *   <li>la file d'attente FIFO des bateaux ;</li>
 *   <li>le délai minimal entre deux ouvertures (Timer/TimerTask).</li>
 * </ul>
 */
public class Pont {

    /** Nombre de segments du pont (S1...SN), identique pour les deux voies. */
    public static final int NB_SEGMENTS = 8;
    /** Délai minimal entre deux ouvertures : 1 h en réalité, 1 min pour la simulation (consigne §5). */
    public static final long DELAI_ENTRE_OUVERTURES = 60_000;
    /** Durée visible de l'état OUVERTURE (ms). */
    public static final long DUREE_OUVERTURE = 1500;
    /** Durée visible de l'état FERMETURE (ms). */
    public static final long DUREE_FERMETURE = 1500;

    /** Les quatre états possibles du pont. */
    public enum Etat { FERME, OUVERTURE, OUVERT, FERMETURE }

    /** État courant du pont. */
    private Etat etat = Etat.FERME;

    /** Occupation des segments dans chaque sens : référence du véhicule, null = segment libre. */
    private final Vehicule[] voieGD = new Vehicule[NB_SEGMENTS];
    private final Vehicule[] voieDG = new Vehicule[NB_SEGMENTS];

    /**
     * true dès qu'une ouverture est prise en charge (entrées bloquées pour les
     * nouveaux véhicules) jusqu'au retour complet à l'état FERME.
     * Tant que des véhicules sont encore sur le pont, l'état reste FERME
     * (ils terminent leur traversée) ; l'état OUVERTURE ne démarre qu'une fois le pont vide.
     */
    private boolean ouvertureEnCours = false;

    /** Convois exceptionnels arrivés à l'entrée et pas encore sortis, par sens. */
    private final List<Vehicule> convoisGD = new ArrayList<>();
    private final List<Vehicule> convoisDG = new ArrayList<>();
    /** Compteur d'ordre d'arrivée des véhicules (sert à départager les convois opposés). */
    private int prochainOrdre = 0;

    /** File d'attente FIFO commune à tous les bateaux. */
    private final Deque<Bateau> fileBateaux = new ArrayDeque<>();
    /** Nombre de bateaux en train de traverser sous le pont (0 ou 1). */
    private int bateauxSurPont = 0;

    /** Nombre de véhicules prioritaires actuellement sur le pont. */
    private int prioritairesSurPont = 0;
    /** Nombre de véhicules prioritaires arrivés à l'entrée mais pas encore engagés. */
    private int prioritairesEnAttente = 0;
    /** Liste des véhicules prioritaires présents (pour la scène). */
    private final List<Vehicule> prioritairesListe = new ArrayList<>();

    /** Véhicules en attente devant chaque entrée, dans l'ordre d'arrivée. */
    private final List<Vehicule> attenteGD = new ArrayList<>();
    private final List<Vehicule> attenteDG = new ArrayList<>();

    /** Heure (en ms) de la prochaine ouverture autorisée ; 0 = autorisée dès maintenant. */
    private long heureProchaineOuverture = 0;
    /** Timer : transitions d'états et autorisation de la prochaine ouverture. */
    private final Timer timer = new Timer("TimerPont", true);

    /** @return l'état courant du pont. */
    public synchronized Etat getEtat() { return etat; }

    /* =====================  côté véhicules  ===================== */

    /**
     * Un véhicule arrive à l'entrée du pont. Un convoi exceptionnel bloque
     * aussitôt l'entrée du sens opposé pour les nouveaux véhicules (les véhicules
     * déjà engagés terminent leur traversée). Un prioritaire est détecté dès son arrivée.
     */
    public synchronized void arriverVehicule(Vehicule v) {
        v.ordreArrivee = prochainOrdre++;
        attenteDe(v).add(v);
        if (estConvoi(v)) convoisDe(v).add(v);
        if (v.prioritaire) prioritairesEnAttente++;
        notifyAll();
    }

    /**
     * Le véhicule attend son tour puis entre sur le pont en réservant
     * tous les segments de sa partie avant (1, 2 ou 4 selon le type) simultanément.
     */
    public synchronized void entrerVehicule(Vehicule v) throws InterruptedException {
        int pas = pasDe(v);
        int entree = (v.sens == SensVehicule.GAUCHE_DROITE) ? 0 : NB_SEGMENTS - 1;
        int teteEntree = entree + (v.longueur - 1) * pas;
        List<Integer> blocEntree = blocDe(v, teteEntree);

        while (!peutEntrer(v, blocEntree)) wait();

        attenteDe(v).remove(v);

        if (v.prioritaire) {
            // Bande d'urgence : le prioritaire ne réserve pas de segments, il traverse
            // pendant que les autres véhicules (qui gardent leurs segments) s'arrêtent.
            prioritairesEnAttente--;
            prioritairesSurPont++;
            prioritairesListe.add(v);
            v.positionTete = (pas == 1) ? -1 : NB_SEGMENTS;
        } else {
            Vehicule[] voie = voieDe(v);
            for (int idx : blocEntree) voie[idx] = v;
            v.positionTete = teteEntree;
        }
        v.surPont = true;
        notifyAll();
    }

    /**
     * Fait avancer le véhicule d'un segment : attend que le nouveau segment de
     * tête soit libre (et qu'aucun prioritaire ne passe, sinon il reste arrêté
     * sur la bande d'urgence avec ses segments), puis réserve le nouveau
     * segment et libère celui qu'il quitte.
     */
    public synchronized void avancer(Vehicule v) throws InterruptedException {
        int pas = pasDe(v);
        int ancienneTete = v.positionTete;
        int nouvelleTete = ancienneTete + pas;
        List<Integer> ancien = blocDe(v, ancienneTete);
        List<Integer> nouveau = blocDe(v, nouvelleTete);

        while (!peutAvancer(v, nouveau)) wait();

        Vehicule[] voie = voieDe(v);
        for (int idx : nouveau) voie[idx] = v;
        for (int idx : ancien) if (!nouveau.contains(idx)) voie[idx] = null;

        v.positionTete = nouvelleTete;
        v.surPont = !nouveau.isEmpty();
        progresserOuverture();
        notifyAll();
    }

    /**
     * Le véhicule a complètement quitté le pont.
     * Un convoi exceptionnel débloque alors l'entrée du sens opposé.
     */
    public synchronized void quitterVehicule(Vehicule v) {
        Vehicule[] voie = voieDe(v);
        if (!v.prioritaire) {
            for (int i = 0; i < NB_SEGMENTS; i++) if (voie[i] == v) voie[i] = null;
        } else {
            prioritairesSurPont--;
            prioritairesListe.remove(v);
        }
        v.surPont = false;
        v.positionTete = -1;
        if (estConvoi(v)) convoisDe(v).remove(v);
        progresserOuverture();
        notifyAll();
    }

    /** @return true si le véhicule peut entrer sur le pont maintenant. */
    private boolean peutEntrer(Vehicule v, List<Integer> blocEntree) {
        if (ouvertureEnCours || etat != Etat.FERME) return false;
        if (v.prioritaire) return true;
        if (prioritaireActif()) return false;
        if (!estPremierEnAttente(v)) return false;          // pas de dépassement à l'entrée
        if (!segmentsLibres(v, blocEntree)) return false;
        if (bloqueParConvoiOppose(v)) return false;
        if (estConvoi(v)) {
            SensVehicule oppose = opposeDe(v);
            if (!voieVide(voieDe(oppose))) return false;
            if (!aucunPrioritaireSens(oppose)) return false;
            // un véhicule opposé arrivé avant le convoi passe d'abord
            for (Vehicule w : attenteDe(oppose)) {
                if (!w.prioritaire && w.ordreArrivee < v.ordreArrivee) return false;
            }
        }
        return true;
    }

    /** @return true si le véhicule peut avancer (pas de prioritaire actif + segments libres). */
    private boolean peutAvancer(Vehicule v, List<Integer> nouveau) {
        if (prioritaireActif()) return false;
        return segmentsLibres(v, nouveau);
    }

    /** @return true si un véhicule prioritaire est sur le pont ou prêt à s'engager (les autres s'arrêtent). */
    private boolean prioritaireActif() {
        return prioritairesSurPont > 0 || (prioritairesEnAttente > 0 && !ouvertureEnCours);
    }

    /**
     * @return true si un convoi du sens opposé interdit l'entrée à v : il est déjà
     * engagé sur le pont, ou il est arrivé avant v (v est alors un "nouveau véhicule").
     */
    private boolean bloqueParConvoiOppose(Vehicule v) {
        for (Vehicule c : convoisDe(opposeDe(v))) {
            if (c.surPont || c.ordreArrivee < v.ordreArrivee) return true;
        }
        return false;
    }

    /** @return true si v est le premier véhicule non prioritaire de la file d'attente de son sens. */
    private boolean estPremierEnAttente(Vehicule v) {
        for (Vehicule w : attenteDe(v)) {
            if (!w.prioritaire) return w == v;
        }
        return true;
    }

    /** @return true si tous les segments de la liste sont libres ou déjà occupés par ce véhicule. */
    private boolean segmentsLibres(Vehicule v, List<Integer> segments) {
        if (v.prioritaire) return true; // bande d'urgence : aucun segment réservé
        Vehicule[] voie = voieDe(v);
        for (int idx : segments) {
            if (voie[idx] != null && voie[idx] != v) return false;
        }
        return true;
    }

    /** @return true si tous les segments de la voie donnée sont libres. */
    private boolean voieVide(Vehicule[] voie) {
        for (Vehicule v : voie) if (v != null) return false;
        return true;
    }

    /** @return true s'il n'y a pas de véhicule prioritaire traversant dans le sens donné. */
    private boolean aucunPrioritaireSens(SensVehicule sens) {
        for (Vehicule v : prioritairesListe) {
            if (v.sens == sens) return false;
        }
        return true;
    }

    /** @return true si aucun véhicule (des 2 voies + prioritaires) n'est sur le pont. */
    private boolean pontVehiculesVide() {
        return voieVide(voieGD) && voieVide(voieDG) && prioritairesListe.isEmpty();
    }

    /** @return true si ce véhicule est un convoi exceptionnel (un prioritaire n'en est jamais un). */
    private boolean estConvoi(Vehicule v) {
        return v.type == TypeVehicule.CONVOI && !v.prioritaire;
    }

    private SensVehicule opposeDe(Vehicule v) { return opposeDe(v.sens); }

    private SensVehicule opposeDe(SensVehicule s) {
        return s == SensVehicule.GAUCHE_DROITE ? SensVehicule.DROITE_GAUCHE : SensVehicule.GAUCHE_DROITE;
    }

    private Vehicule[] voieDe(SensVehicule s) {
        return s == SensVehicule.GAUCHE_DROITE ? voieGD : voieDG;
    }

    private List<Vehicule> attenteDe(Vehicule v) { return attenteDe(v.sens); }

    private List<Vehicule> attenteDe(SensVehicule s) {
        return s == SensVehicule.GAUCHE_DROITE ? attenteGD : attenteDG;
    }

    private List<Vehicule> convoisDe(Vehicule v) { return convoisDe(v.sens); }

    private List<Vehicule> convoisDe(SensVehicule s) {
        return s == SensVehicule.GAUCHE_DROITE ? convoisGD : convoisDG;
    }

    /** @return le pas d'avancement du véhicule selon son sens de circulation (+1 ou -1). */
    private int pasDe(Vehicule v) {
        return (v.sens == SensVehicule.GAUCHE_DROITE) ? 1 : -1;
    }

    /** @return la voie (tableau de segments) du sens du véhicule. */
    private Vehicule[] voieDe(Vehicule v) {
        return voieDe(v.sens);
    }

    /**
     * Indices des segments occupés par le véhicule si sa tête est à l'indice "tete"
     * (seuls les indices dans [0 ; NB_SEGMENTS-1] sont retenus : vide = hors du pont).
     */
    private List<Integer> blocDe(Vehicule v, int tete) {
        int pas = pasDe(v);
        List<Integer> bloc = new ArrayList<>();
        for (int k = 0; k < v.longueur; k++) {
            int idx = tete - pas * k;
            if (idx >= 0 && idx < NB_SEGMENTS) bloc.add(idx);
        }
        return bloc;
    }

    /* =====================  côté bateaux  ===================== */

    /**
     * Un bateau arrive : sa demande est enregistrée en fin de file FIFO.
     * Si l'ouverture est autorisée, elle est déclenchée aussitôt.
     */
    public synchronized void arriverBateau(Bateau b) {
        fileBateaux.addLast(b);
        declencherOuvertureSiPossible();
        notifyAll();
    }

    /**
     * Le bateau attend d'être en tête de file ET que le pont soit complètement
     * OUVERT (l'ouverture est pilotée par declencherOuvertureSiPossible, par le
     * Timer et par les véhicules qui libèrent le pont).
     */
    public synchronized void ouvrirLePont(Bateau b) throws InterruptedException {
        while (fileBateaux.peekFirst() != b || etat != Etat.OUVERT) wait();
    }

    /**
     * Attend le tour du bateau : être en tête de file, pont à l'état OUVERT
     * et aucun autre bateau en traversée (un seul bateau passe à la fois).
     */
    public synchronized void monTour(Bateau b) throws InterruptedException {
        while (!(etat == Etat.OUVERT && fileBateaux.peekFirst() == b && bateauxSurPont == 0)) {
            wait();
        }
        bateauxSurPont = 1;
        notifyAll();
    }

    /**
     * Fin de la traversée du bateau : la file défile d'un cran.
     * Si c'était le dernier bateau en attente, le pont passe à FERMETURE
     * puis revient à FERME grâce au Timer.
     */
    public synchronized void quitterBateau(Bateau b) {
        fileBateaux.pollFirst();
        bateauxSurPont--;
        if (etat == Etat.OUVERT && fileBateaux.isEmpty() && bateauxSurPont == 0) {
            etat = Etat.FERMETURE;
            planifier(DUREE_FERMETURE, this::confirmerFermeture);
        }
        notifyAll();
    }

    /**
     * Prend en charge une ouverture si : pont FERME, aucune ouverture déjà en cours,
     * au moins un bateau attend et le délai minimal depuis la dernière fermeture est écoulé.
     * Les entrées sont alors bloquées pour les nouveaux véhicules.
     */
    private void declencherOuvertureSiPossible() {
        if (etat == Etat.FERME && !ouvertureEnCours && !fileBateaux.isEmpty() && ouvertureAutorisee()) {
            ouvertureEnCours = true;
            progresserOuverture();
            notifyAll();
        }
    }

    /**
     * Si une ouverture est en cours et que tous les véhicules ont quitté le pont,
     * démarre l'état OUVERTURE (puis OUVERT après DUREE_OUVERTURE).
     */
    private void progresserOuverture() {
        if (ouvertureEnCours && etat == Etat.FERME && pontVehiculesVide()) {
            etat = Etat.OUVERTURE;
            planifier(DUREE_OUVERTURE, this::passerOuvert);
            notifyAll();
        }
    }

    /** Tâche du Timer : OUVERTURE -> OUVERT, les bateaux peuvent traverser. */
    private synchronized void passerOuvert() {
        if (etat == Etat.OUVERTURE) {
            etat = Etat.OUVERT;
            notifyAll();
        }
    }

    /**
     * Tâche du Timer : FERMETURE -> FERME. Les entrées sont rouvertes, on mémorise
     * l'heure de fermeture complète et on planifie la prochaine ouverture autorisée
     * (fermeture + DELAI_ENTRE_OUVERTURES).
     */
    private synchronized void confirmerFermeture() {
        if (etat != Etat.FERMETURE) return;
        etat = Etat.FERME;
        ouvertureEnCours = false;
        heureProchaineOuverture = System.currentTimeMillis() + DELAI_ENTRE_OUVERTURES;
        planifierOuverture();
        notifyAll();
    }

    /** @return true si le délai minimal entre deux ouvertures est écoulé. */
    private boolean ouvertureAutorisee() {
        return heureProchaineOuverture == 0 || System.currentTimeMillis() >= heureProchaineOuverture;
    }

    /** @return le temps restant (en s) avant la prochaine ouverture possible. */
    public synchronized long secondesAvantOuverture() {
        long reste = heureProchaineOuverture - System.currentTimeMillis();
        return Math.max(0, (reste + 999) / 1000);
    }

    /**
     * Planifie un TimerTask à l'heure de la prochaine ouverture autorisée :
     * à ce moment, si des bateaux attendent, l'ouverture est déclenchée.
     */
    private void planifierOuverture() {
        long retard = Math.max(1, heureProchaineOuverture - System.currentTimeMillis());
        planifier(retard, this::heureOuvertureAtteinte);
    }

    /** Tâche du Timer : l'heure autorisée est atteinte, on vérifie les bateaux en attente. */
    private synchronized void heureOuvertureAtteinte() {
        if (!ouvertureAutorisee()) {      // réveil en avance de quelques ms : on replanifie
            planifierOuverture();
            return;
        }
        declencherOuvertureSiPossible();
    }

    /** Programme une action dans le Timer. */
    private void planifier(long delaiMs, Runnable action) {
        timer.schedule(new TimerTask() {
            @Override public void run() { action.run(); }
        }, delaiMs);
    }

    /* =====================  scène console  ===================== */

    /** @return la scène du pont, sous forme de lignes de texte. */
    public synchronized List<String> construireLignes() {
        List<String> lignes = new ArrayList<>();

        String info = "";
        if (etat == Etat.FERME && ouvertureEnCours) {
            info = "   (ouverture demandée : entrées bloquées, le pont se vide)";
        } else if (etat == Etat.FERME && !ouvertureAutorisee()) {
            info = "   (prochaine ouverture possible dans " + secondesAvantOuverture() + " s)";
        }
        lignes.add(" État : " + nomEtat() + info);

        String fifo = "";
        if (!fileBateaux.isEmpty()) {
            StringBuilder ls = new StringBuilder();
            for (Bateau b : fileBateaux) ls.append("B").append(b.id).append(", ");
            fifo = " (FIFO : " + ls.substring(0, ls.length() - 2) + ", en tête : B" + fileBateaux.peekFirst().id + ")";
        }
        lignes.add(" Entrées véhicules : G→D " + entreeTxt(SensVehicule.GAUCHE_DROITE)
                + " | D→G " + entreeTxt(SensVehicule.DROITE_GAUCHE)
                + " | Bateaux en attente : " + fileBateaux.size() + fifo);

        lignes.add(SEP);
        lignes.add("  G→D  " + ligneAttente(attenteGD) + ligneVoie(voieGD));
        lignes.add("  ──── " + ligneBateaux() + "  (eau sous le pont)");
        lignes.add("  D←G  " + ligneVoie(voieDG) + ligneAttente(attenteDG));
        lignes.add(SEP);

        if (prioritaireActif()) {
            lignes.add(" URGENCE : " + ligneUrgence());
            lignes.add(" Arrêtés sur la bande d'urgence : " + ligneArretes());
        }
        lignes.add(" Légende : v voiture  c camion  C convoi  P prioritaire  B bateau");

        return lignes;
    }

    /** @return l'état de l'entrée véhicules pour le sens donné. */
    private String entreeTxt(SensVehicule sens) {
        if (ouvertureEnCours) return "fermée (pont mobile)";
        if (!convoisDe(opposeDe(sens)).isEmpty()) return "fermée (convoi en face)";
        return "ouverte";
    }

    /** Ligne de séparation de la scène. */
    private static final String SEP = " --------------------------------------";

    /** @return le nom de l'état courant, tel qu'affiché à l'écran. */
    private String nomEtat() {
        return switch (etat) {
            case FERME -> "FERMÉ";
            case OUVERTURE -> "OUVERTURE";
            case OUVERT -> "OUVERT";
            case FERMETURE -> "FERMETURE";
        };
    }

    /** Construit la ligne des segments d'une voie : [x] pour occupé, [ ] pour libre. */
    private String ligneVoie(Vehicule[] voie) {
        StringBuilder sb = new StringBuilder();
        for (Vehicule v : voie) {
            sb.append('[').append(v == null ? ' ' : v.symbole).append(']');
        }
        return sb.toString();
    }

    /**
     * Construit la ligne des bateaux sur 10 positions : approche à gauche (x=-1),
     * les 8 segments du pont (x=0..7), approche à droite (x=8). Si plusieurs
     * bateaux attendent au même endroit, le nombre est affiché.
     */
    private String ligneBateaux() {
        StringBuilder sb = new StringBuilder();
        for (int x = -1; x <= NB_SEGMENTS; x++) {
            int n = 0;
            for (Bateau b : fileBateaux) if (b.position == x) n++;
            char c = n == 0 ? ' ' : (n == 1 ? 'B' : (char) ('0' + Math.min(n, 9)));
            sb.append(' ').append(c).append(' ');
        }
        return sb.toString();
    }

    /** Construit la liste des véhicules qui attendent devant l'entrée du sens donné. */
    private String ligneAttente(List<Vehicule> attente) {
        StringBuilder sb = new StringBuilder();
        for (Vehicule v : attente) sb.append(v.symbole).append(' ');
        return sb.toString();
    }

    /** Construit la description des véhicules prioritaires en cours de passage. */
    private String ligneUrgence() {
        if (prioritairesListe.isEmpty()) return "prioritaire à l'entrée, les véhicules s'arrêtent";
        StringBuilder sb = new StringBuilder();
        for (Vehicule v : prioritairesListe) {
            sb.append("P").append(v.id).append(" (")
                    .append(v.sens == SensVehicule.GAUCHE_DROITE ? "G→D" : "D→G")
                    .append(", seg. ").append(Math.max(1, Math.min(NB_SEGMENTS, v.positionTete + 1))).append(") ");
        }
        return sb.toString();
    }

    /** Liste les véhicules arrêtés avec les segments qu'ils retrouveront à la reprise. */
    private String ligneArretes() {
        Map<Vehicule, int[]> plages = new LinkedHashMap<>();
        for (Vehicule[] voie : new Vehicule[][] { voieGD, voieDG }) {
            for (int i = 0; i < NB_SEGMENTS; i++) {
                if (voie[i] == null) continue;
                final int idx = i;
                int[] p = plages.computeIfAbsent(voie[i], k -> new int[] { idx, idx });
                p[0] = Math.min(p[0], i);
                p[1] = Math.max(p[1], i);
            }
        }
        if (plages.isEmpty()) return "aucun";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Vehicule, int[]> e : plages.entrySet()) {
            Vehicule v = e.getKey();
            int[] p = e.getValue();
            sb.append(v.symbole).append(v.id).append("@S").append(p[0] + 1);
            if (p[1] != p[0]) sb.append("-S").append(p[1] + 1);
            sb.append(' ');
        }
        return sb.toString();
    }
}