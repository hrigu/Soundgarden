# Soundgarden

Live-Coding-Rig in SuperCollider, um vorhandene elektronische Tracks während
einer Fusion-Tanzparty in Echtzeit zu verändern (Filter, Echo, Stutter/Glitch,
Bitcrush, Reverb) – ohne Aussetzer, per JITLib (`Ndef`).

## Setup (macOS)

1. SuperCollider installieren: https://supercollider.github.io/downloads
   (oder `brew install supercollider`)
2. Repo öffnen, `boot.scd` in der IDE öffnen.
3. Eigene Tracks (`.wav`/`.aiff`/`.flac`) in den Ordner `sounds/` legen.
   Der Ordner ist per `.gitignore` von Git ausgeschlossen – deine Musik landet
   nicht im Repo.

## Workflow

Dateien der Reihe nach öffnen und blockweise ausführen (Cmd+Enter):

1. **`load_classes.scd`** – muss zuerst laufen (einmal pro Sitzung): bindet `classes/`/`tests/`
   ein und kompiliert neu. `boot.scd` braucht die Klasse `BootTrackDetection` daraus.
2. **`boot.scd`** – bootet den Server und lädt alle Dateien aus `sounds/`
   automatisch in `~tracks` (z.B. `~tracks[\mytrack01]`).
3. **`fx.scd`** – definiert die FX-Bausteine (`~fxChains`), die du live
   kombinierst.
4. **`set_template.scd`** – das eigentliche Live-Set. Jeder Block darin ist
   während der Party einzeln ausführbar: Track starten, FX-Kette
   umschalten, Parameter live verstellen, in den nächsten Track überblenden.

Kernidee: `Ndef(\track, { ... })` und `Ndef(\fx, { ... })` einfach neu
ausführen – JITLib überblendet automatisch (`fadeTime`), es gibt keine
Klicks oder Unterbrechungen.

## classes/, tests/ und load_classes.scd

Eigene Produktiv-Klassen liegen in `classes/`, die zugehörigen Test-Klassen
(`UnitTest`-Subklassen) getrennt davon in `tests/`. `BootTrackDetection` (Live-Set-Domäne)
liegt direkt in `classes/`; die Spatial-Audio-Klassen sind unter `classes/sg/` nach
Zuständigkeit sortiert (reine Ordnerkonvention — SuperCollider selbst kennt keine echten
Namespaces):
- `classes/sg/sounds/` — `Sound`, `InsectSound`
- `classes/sg/soundobjects/` — `SoundObject`, `Movable`, `MoveRule`, `CircularMoveRule`
- `classes/sg/spatial/` — `Listener`, `Binauralizer`, `AtkBinauralizer`, `Orchestra`,
  `KeyboardListenerControl`, `SpaceView`
- passende Tests spiegelbildlich unter `tests/sg/soundobjects/`, `tests/sg/spatial/`

Damit SuperCollider alles kennt, einmal pro Sitzung **als allererstes** (vor `boot.scd`!)
**`load_classes.scd`** ausführen (bindet `classes/` und `tests/` rekursiv ein) und im
Post-Fenster auf `compile done` warten — danach stehen alle Klassen überall zur Verfügung.
Ohne diesen Schritt scheitert `boot.scd` mit `ERROR: Class not defined.` (auf einer frisch
gestarteten SuperCollider-Sitzung ist die Klassenbibliothek noch im Auslieferungszustand).

## Tests

Die reine sclang-Logik (aktuell: Extension-Erkennung für Tracks) wird über
SuperCollders eingebautes `UnitTest`-Framework getestet — ohne Server, ohne
echte Audiodateien. Nach `load_classes.scd` einfach **`run_tests.scd`**
ausführen; Ergebnis (grün/rot pro Testfall) erscheint im Post-Fenster.

## Zweiter Prototyp: binaurales Insekt (`insect_demo.scd`)

Ein virtuelles Klangobjekt ("Insekt"), das nach einer Bewegungsregel durch
einen definierten Raum kurvt; der Hörer bekommt es über Kopfhörer binaural
zugespielt — mal vorne, mal hinten, mal laut (nah), mal leise (fern). Der
Hörer selbst kann sich per Tastatur durch den Raum bewegen und drehen.

Objektmodell:
- **`Movable`** — hat eine Position, die sich nach einer Bewegungsregel
  verändert; steuert sanft zurück, falls sie eine Raumkugel verlässt.
  `moveTo(newPos)` setzt die Position stattdessen direkt (Live-Coding-Platzierung,
  ohne Rückführung).
- **`MoveRule`/`CircularMoveRule`/`SteadyMoveRule`** — Bewegungsregeln: Kreisbahn
  mit atmendem Radius, oder `SteadyMoveRule` für stationäre Soundobjekte (bewegt
  sich nicht von selbst, per `moveTo` trotzdem umplatzierbar).
- **`Listener`** — Position und Blickrichtung, beweglich: `moveForward`/
  `moveBackward`/`strafeLeft`/`strafeRight`/`rotate`, alle relativ zur aktuellen
  Blickrichtung. Berechnet aus einer Weltposition Azimuth (relativ zur eigenen
  Blickrichtung) und Distanz.
- **`KeyboardListenerControl`** — bewegt/dreht einen `Listener` kontinuierlich
  per Tastatur, solange eine Taste gehalten wird (**W**/**S** vor/zurück,
  **A**/**D** seitlich, **Q**/**E** drehen). Braucht ein fokussiertes Fenster
  (SuperCollider-Standardidiom für Tastatur-Input).
- **`SoundObject`** — verbindet ein `Movable` mit einem klingenden Synth
  (`Sound`-Subklasse) und einem Binauralisierer (`Binauralizer` oder
  `AtkBinauralizer`, austauschbar, gleiche Schnittstelle); kennt den `Listener`
  selbst nicht. Startet nur den Synth, tickt aber nicht selbst.
- **`Orchestra`** — hält den `Listener` und eine Registry registrierter
  `SoundObject`s. `play(server)` startet jedes registrierte SoundObject und
  danach eine einzige gemeinsame Tick-Routine (Bewegung + Azimuth/Distanz
  statt Pan, winzige Laufzeitdifferenz zwischen den Ohren, entfernungs-
  abhängiger Pegel/Tiefpass pro Objekt). `stop` beendet das Ticken und
  stoppt alle registrierten SoundObjects. `call(caller)` löst bei `caller`
  einen kurzen Klang-Akzent aus (Call-and-Response, siehe `Sound>>call`) —
  ein zufälliges anderes registriertes Objekt ruft nach kurzer Verzögerung
  zurück.
- **`SpaceView`** — live aktualisierte 2D-Draufsicht: Listener (Position +
  Blickrichtung als Pfeil) und alle registrierten Soundobjekte, von oben
  betrachtet. Bewegt selbst nichts, liest bei jedem Neuzeichnen einfach den
  aktuellen Zustand der `Orchestra`.

Ausführen: `load_classes.scd` → `boot.scd` → `insect_demo.scd` (Block für
Block), mit Kopfhörern. Für die Tastatursteuerung muss das
`KeyboardListenerControl`-Fenster fokussiert sein.

Zwei Binauralisierungs-Varianten stehen zum direkten Vergleich zur Verfügung
(in `insect_demo.scd`: Insekt 1 nutzt `AtkBinauralizer`, Insekt 2 und der
Brunnen `Binauralizer`):

- **`Binauralizer`** — einfache Näherung mit reinen Core-UGens (Pan +
  winzige Laufzeitdifferenz zwischen den Ohren + entfernungsabhängiger
  Pegel/Tiefpass, kein Zusatzpaket nötig). Vorne/hinten bleibt dadurch etwas
  mehrdeutig (Cone-of-Confusion-Problem echter Ohren).
- **`AtkBinauralizer`** — echtes HRTF-Rendering über das [Ambisonic Toolkit
  (ATK)](https://github.com/ambisonictoolkit/atk-sc3): `PanB2` codiert
  Azimuth/Distanz horizontal ins B-Format, `FoaDecoderKernel` (gemessene
  HRTF, z.B. CIPIC) decodiert binaural. Vorne/hinten ist dadurch deutlich
  eindeutiger hörbar. Elevation ist bewusst außen vor (Datenmodell ist
  aktuell rein 2D, siehe Intent 5).

### ATK-Setup (für `AtkBinauralizer`)

Einmal pro Installation, in scide ausführen:

```supercollider
// 1) Quark installieren
Quarks.install("https://github.com/ambisonictoolkit/atk-sc3.git");
// Klassenbibliothek danach neu kompilieren (Language → Recompile Class Library)

// 2) HRTF-Kernel/Matrizen/Soundfiles laden
Atk.downloadKernels;
Atk.downloadMatrices;
Atk.downloadSounds;
```

`sc3-plugins` wird für `AtkBinauralizer` **nicht** gebraucht — `FoaTransform`/`FoaRotate`
(kompilierte UGens aus `sc3-plugins`) werden bewusst umgangen, da `sc3-plugins` in diesem
Projekt (noch) nicht installiert ist (siehe Gotchas in `CLAUDE.md`); `PanB2` (Core-UGen) deckt
den hier ohnehin rein horizontalen Fall gleichwertig ab. `FoaDecoderKernel`/`FoaDecode`
brauchen intern nur `Convolution2` (ebenfalls Core).

In `AtkBinauralizer.setup(server, subjectID)` lässt sich `subjectID` (Default 21 = KEMAR-
Kunstkopf) auf ein anderes CIPIC-Subjekt ändern, um mit verschiedenen Kopfformen zu
experimentieren (siehe `FoaDecoderKernel`-Hilfe in scide für die Liste gültiger IDs).

Zwei Dokumente mit mehr Hintergrund (Stand nach Intent 9, `MoveRule`/
`InsectSound`/`Binauralizer` als eigene Klassen):
- `docs/insekt-architektur-walkthrough.md` — Code-Walkthrough, wer macht was
- `docs/insekt-architektur-zeitablauf.html` — Grafik: Signalfluss und wie
  Kontrollrate (sclang) und Audiorate (scsynth) zusammenspielen (im Browser
  öffnen)

## Nächste Schritte / Ideen

- MIDI-Controller oder OSC-Fader anbinden, um Parameter (Cutoff, Mix,
  Gate-Rate) live mit den Händen statt per Tastatur zu steuern.
- Weitere FX-Bausteine in `fx.scd` ergänzen (z.B. Reverse, Granular).
- Pattern-basierte Automation (`Pbind`) für sich selbst entwickelnde
  Übergänge zwischen Tracks.
- `Movable` mit einem `UnitTest` absichern: `step()` ist trotz Seiteneffekt gut
  testbar (`MoveRule` injizierbar per Fake, Rückführungslogik bei
  `roomRadius`-Überschreitung ist reine Mathematik ohne Server-Bezug).
