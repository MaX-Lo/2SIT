## Merging Level Connections

indoorOSM:
- verticalpassage:floorange:x to y hat alle Level, welche über diese Verbindung erreichbar sind, das heißt aber nicht, dass
  auf anderen Ebenen das Layout genauso aussieht! Ebenso heißt das nicht, das Falls eine Level Connection Level 1, 2 und 3 besitzt
  eine Tür auf Level 2 existieren muss.

- Mapping auf 2 verschiedene Arten:
  1) eigener Weg auf jedem Level der dann einmalig von einer Floor-Relation referenziert wird
  2) Weg der das Outline für mehrere Level beschreibt -> gleicher Weg wird von mehreren Floor-Relation referenziert
  3) Ein Weg der als verticalpassage:floorange mehrere Level angegeben hat. Floor Relations für die jeweiligen Level enthalten
     diesen dann aber aus unbekannten Gründen nicht zwangsläufig als Member.

SIT
- ein Weg der durch den Level Tag angibt, in welchen Leveln er sich alles befindet
- Tag, welche anderen Level durch diese Verbindung erreichbar sind existiert eigentlich nicht

## Konvertieren
Annahme: Der Grundriss einer Level Connection Ändert sich nicht über Etagen

Schritt 1: Aufbauen der Level Connection Liste
- Alle Level Connections Objekte erstellen. Level ist zunächst nur das Level von dem Floor der die
  Level Connection referenziert.
- Referenzieren weitere Floors den gleichen Weg werden die zusätzlichen Level zu der Level Connection einfach hinzugefügt (2)

Schritt 2: Mergen von Level Connections
- Für Level Connections wird geschaut, ob es weitere LevelConnections gibt, die den gleichen/sehr ähnlichen Umriss besitzen
  dazu:
    1) für alle "einfachen" Nodes (keine Door/Window Nodes) des LevelConnection Weges schauen ob es in der anderen LevelConnection
       einen dazugehörigen Node in proximity gibt
    2) falls für alle solchen Nodes der Fall wird gemerged
- Mergen:
  - Level Tags der "einfachen" Nodes sind additiv zu behandeln, Level Tags spezieller Nodes (door/window) werden Teil des Weges, 
    Level Tags werden allerdings nicht verändert
    Außnahme: Door oder Window Tags haben ebenfalls ein Proximity gegenpart dann auch additiv
- Reihenfolgebestimmung der Nodes beim Mergen:
    einfach den Node nehmen der, von der Distanz am nächsten dran ist
- Dies wird für alle LevelConnections gemacht, Ergebnis sollte eine Liste nicht weiter verringerbarer LevelConnections sein