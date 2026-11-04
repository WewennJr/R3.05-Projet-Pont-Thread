# R3.05 Project to learn thread in Java

## Compilation et exécution (Maven)

```bash
mvn compile
mvn exec:java -Dexec.mainClass=iut.gon.Main
# ou après compilation :
java -cp target/classes iut.gon.Main
```

> L'animation utilise des séquences ANSI : utilisez Windows Terminal ou un terminal
> compatible (toutes les consoles modernes sous Windows 10+).

## Structure du projet

- `iut.gon.pont` — le contrôleur du pont :
  - `Pont` : verrou unique `ReentrantLock` + `Condition` signalée globalement ;
    gestion des segments S1..SN pour chacune des deux chaussées (G→D et D→G),
    réservation/libération des segments, blocage d'entrée, anti-collision.
  - `Sens` / `EtatPont` : sens de circulation et machine à états du pont (prête
    pour la gestion des bateaux, étape suivante).
  - `Affichage` : animation console avec les séquences ANSI de l'annexe du sujet.
- `iut.gon.vehicule` — les véhicules (threads) :
  - `Vehicule` (abstraite, étend `Thread`) : entrée bloquante, traversée segment
    par segment (attend si le segment suivant est occupé), sortie.
  - `Voiture` (1 segment), `Camion` (2 segments consécutifs), `ConvoiExceptionnel`
    (4 segments consécutifs + blocage de l'entrée du sens opposé).
- `iut.gon.Main` : simulation de test avec déplacements simultanés dans les deux sens.

## Étapes réalisées

- [x] Classe abstraite `Vehicule` (Thread)
- [x] `Voiture` (1 segment)
- [x] `Camion` (2 segments consécutifs)
- [x] `ConvoiExceptionnel` (4 segments consécutifs)
- [x] Déplacements dans les deux sens (deux chaussées indépendantes)
- [x] Attente lorsqu'un segment est occupé (`Condition.await` / `signalAll`)
- [x] Test des déplacements simultanés (`Main`, aucune collision : invariant vérifié
      dans la réservation des segments)

## Reste à faire (étapes du sujet)

- Gestion des bateaux (file FIFO, états OUVERTURE/OUVERT/FERMETURE — l'enum `EtatPont`
  est déjà en place), délai minimal de 1 minute entre deux ouvertures (Timer/TimerTask),
  véhicules prioritaires (ambulances/pompiers, bande d'urgence), fichier .jar, UML.
