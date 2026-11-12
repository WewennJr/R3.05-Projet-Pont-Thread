
PROJET R3.05 - Simulation d'un pont mobile bidirectionnel (threads Java)
Binôme : Legros Ewenn / Faveroult Hugo

1. PARTIES NON FAITES
---------------------

Tout le principal est fait, juste les bateaux ne connaissent pas l'heure de la prochaine ouverture, mais c'est affiché.

2. LANCEMENT
------------

```bash
java -jar pont.jar [durée_ms] [ansi]
```

  - durée_ms : durée en ms.
  - ansi : meilleur affichage dans un terminal ANSI.

Recommandé :
```bash
java -jar pont.jar 120000 ansi
```

Légende : v voiture, c camion, C convoi exceptionnel, P prioritaire, B bateau.

3. CHOIX DE CONCEPTION
----------------------

- Délai entre deux ouvertures : 1 minute (au lieu de 60), géré par Timer/TimerTask.
- Ouverture : dès qu'elle est prise en charge, les entrées sont bloquées ; l'état
  reste FERMÉ tant que des véhicules sont sur le pont ; OUVERTURE ne démarre
  qu'une fois le pont vide.
- Convoi : bloque l'entrée du sens opposé dès son arrivée (pour les véhicules arrivés
  après lui). Si deux convois de sens opposés arrivent, le premier arrivé passe d'abord.
- Prioritaire : détecté dès son arrivée à l'entrée ; les autres véhicules s'arrêtent
  (bande d'urgence) en gardant leurs segments, puis reprennent après sa sortie.
  Il peut passer même si un convoi est engagé en sens inverse (bande d'urgence).
- File d'attente à chaque entrée : premier arrivé, premier engagé (pas de dépassement).
- Un seul moniteur (la classe Pont, méthodes synchronized) protège tout l'état partagé.

4. VIDEO
--------

Dans la vidéo de démonstration, on peux voir tout les cas d'utilisation du pont avec :
- les différents états du pont (ouvert, fermé, ouverture, fermeture)
- les différents types de véhicules (voiture, camion, convoi exceptionnel, prioritaire)
- les bateaux qui passent un par un dans l'ordre d'arrivée
- les convois qui passent un par un dans l'ordre d'arrivée et bloquent les autres véhicules
- les véhicules qui se mettent en bande d'urgence pour laisser passer un véhicule prioritaire (même si il y a un convoi engagé en sens inverse)
- quand le pont est ouvert, les véhicules ne peuvent pas passer et doivent attendre la fermeture du pont pour pouvoir passer
- quand le pont veux s'ouvrir, il attend que les véhicules sur le pont soient passés pour pouvoir s'ouvrir

5. COMMENTAIRE DE LA VIDÉO (demo_pont.mp4)
------------------------------------------
 

6. CONTENU DU DOSSIER
---------------------
src/ (package iut.gon), pont.jar, schema-UML.png, demo_pont.mp4, README.txt