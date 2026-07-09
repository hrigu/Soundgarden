# Soundgarden

Live-Coding-Rig in SuperCollider, um vorhandene elektronische Tracks während
einer Fusion-Tanzparty in Echtzeit zu verändern (Filter, Echo, Stutter/Glitch,
Bitcrush, Reverb) – ohne Aussetzer, per JITLib (`Ndef`).

## Setup (macOS)

1. SuperCollider installieren: https://supercollider.github.io/downloads
   (oder `brew install supercollider`)
2. Für `.m4a`-Dateien zusätzlich `ffmpeg` installieren (`brew install ffmpeg`).
   Hintergrund: `demos/reverb/reverb_song.scd` konvertiert `.m4a` bei Bedarf
   temporär nach `.wav`, weil SuperCollider diese Dateien nicht zuverlässig
   direkt lädt.
3. Repo öffnen, `boot/boot.scd` in der IDE öffnen.
4. Eigene Tracks (`.wav`/`.aiff`/`.flac`/`.m4a`) in den Ordner `sounds/` legen.
   Der Ordner ist per `.gitignore` von Git ausgeschlossen – deine Musik landet
   nicht im Repo.

## Workflow

Dateien der Reihe nach öffnen und blockweise ausführen (Cmd+Enter):

1. **Nur beim Erst-Setup:** **`boot/load_classes.scd`** – muss vor dem ersten Lauf von
   `boot/boot.scd` einmalig gelaufen sein (siehe Abschnitt unten): bindet `classes/`/`tests/`
   ein und kompiliert neu. Dabei trägt SuperCollider die Include-Pfade in
   `sclang_conf.yaml` unter `Application Support/SuperCollider` ein.
2. **`boot/boot.scd`** – bootet den Server und lädt alle Dateien aus `sounds/`
   automatisch in `~tracks` (z.B. `~tracks[\mytrack01]`). Die Soundgarden-Klassen sind dabei
   nach dem Erst-Setup automatisch verfügbar, weil SuperCollider den Klassenpfad aus der
   `sclang_conf.yaml` bereits kennt.
3. **`experiments/live_coding_rig/fx.scd`** – definiert die FX-Bausteine (`~fxChains`), die du live
   kombinierst.
4. **`experiments/live_coding_rig/set_template.scd`** – das eigentliche Live-Set. Jeder Block darin ist
   während der Party einzeln ausführbar: Track starten, FX-Kette
   umschalten, Parameter live verstellen, in den nächsten Track überblenden.

Kernidee: `Ndef(\track, { ... })` und `Ndef(\fx, { ... })` einfach neu
ausführen – JITLib überblendet automatisch (`fadeTime`), es gibt keine
Klicks oder Unterbrechungen.

Mehr Details zur Bedienung (verfügbare FX-Ketten, typische Abläufe wie Build-up/Drop/Break)
siehe `experiments/live_coding_rig/README.md`.

## classes/, tests/ und boot/load_classes.scd

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

Damit SuperCollider alles kennt, einmalig beim Erst-Setup **als allererstes** (vor
`boot/boot.scd`!) **`boot/load_classes.scd`** ausführen (bindet `classes/` und `tests/`
rekursiv ein) und im Post-Fenster auf `compile done` warten — danach stehen alle Klassen
überall zur Verfügung. `LanguageConfig.addIncludePath` persistiert die beiden Pfade dabei
standardmäßig in `sclang_conf.yaml`, weshalb dieser Schritt ab dann bei **jedem** weiteren
Start automatisch passiert und nicht wiederholt werden muss (außer nach einem Reset/Löschen
von `sclang_conf.yaml` oder auf einer neuen Maschine). Ohne diesen Schritt (bzw. vor dem
allerersten Mal) scheitert `boot/boot.scd` mit `ERROR: Class not defined.` (auf einer frisch
installierten SuperCollider-Instanz ist die Klassenbibliothek noch im Auslieferungszustand).

Eigene SCDoc-Hilfeseiten liegen unter `HelpSource/Classes/` und erscheinen im normalen
SuperCollider-HelpBrowser. Nach Änderungen an Klassen oder `.schelp`-Dateien in scide
**Help → Rebuild All Documentation** ausführen; danach öffnet z.B. `cmd-d` auf `Room` die
projektspezifische Hilfeseite mit den Hall-Parametern (`size`, `height`, `surface`, `mix`).

## Tests

Die reine sclang-Logik (aktuell: Extension-Erkennung für Tracks) wird über
SuperCollders eingebautes `UnitTest`-Framework getestet — ohne Server, ohne
echte Audiodateien. Sobald `boot/load_classes.scd` einmal gelaufen ist (siehe oben), einfach
**`run_tests.scd`** ausführen; Ergebnis (grün/rot pro Testfall) erscheint im Post-Fenster.

## Zweiter Prototyp: binaurales Insekt (`demos/grill/`)

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

Ausführen: `boot/load_classes.scd` → `boot/boot.scd` → eine der Varianten in `demos/grill/`
(Block für Block), mit Kopfhörern. Für die Tastatursteuerung muss das
`KeyboardListenerControl`-Fenster fokussiert sein.

- `demos/grill/one_insect.scd` — Minimalbeispiel, ein einzelnes Insekt.
- `demos/grill/three_insects.scd` — 3 fest benannte, individuell konfigurierte Insekten mit
  Call-and-Response.
- `demos/grill/insects.scd` — mehrere zufällig generierte Insekten mit konfigurierbarem
  Sound/Stille-Verhältnis (`RhythmPatternCreator`).

### Reverb-Testsetup (`demos/reverb.scd`)

Zum gezielten Einstellen der Raumeigenschaften: eine einzige, stationäre Klangquelle
(trockenes Sample, fester Rhythmus), um die man sich per Tastatur frei bewegen und drehen kann
(W/S/A/D/Q/E) — anders als bei `demos/sample.scd`/`sample_moving.scd` mit mehreren
gleichzeitigen Objekten lässt sich der Nachklang hier klar heraushören. Enthält einen
Live-Tuning-Block (`room.size`/`.height`/`.surface`/`.mix` per Zuweisung, `reverbMix` weiterhin
per `synth.set(...)`, siehe unten) sowie eine Möglichkeit, die Quelle selbst umzuplatzieren
(`moveTo(...)`).

Ein `Room` hat eine einzige Binauralisierungs-"Ohren"-Strategie (`Binauralizer` oder
`AtkBinauralizer`, festgelegt über `room.addSynthDef(...)`, siehe `Listener>>makeBinauralizer`)
— Demo-Skripte konstruieren keinen Binauralizer mehr selbst:

Für `demos/reverb/reverb_song.scd` gilt zusätzlich: wenn das Quellmaterial als `.m4a`
vorliegt, muss `ffmpeg` installiert sein (`brew install ffmpeg`), damit das Skript die Datei
vor dem Laden temporär nach `.wav` konvertieren kann.

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
- Weitere FX-Bausteine in `experiments/live_coding_rig/fx.scd` ergänzen (z.B. Reverse, Granular).
- Pattern-basierte Automation (`Pbind`) für sich selbst entwickelnde
  Übergänge zwischen Tracks.
- `Movable` mit einem `UnitTest` absichern: `step()` ist trotz Seiteneffekt gut
  testbar (`MoveRule` injizierbar per Fake, Rückführungslogik bei
  `roomRadius`-Überschreitung ist reine Mathematik ohne Server-Bezug).
- `SampleSound`/`AtkBinauralizer` ressourcenschonender machen: aktuell läuft pro
  Sample-Objekt ein dauerhafter Synth (inkl. teurer HRTF-Faltung), auch wenn es
  die meiste Zeit nur Stille verarbeitet (z.B. `demos/sample.scd` mit vielen
  Objekten im Kreis, die sich den Rhythmus per Phasenversatz weitergeben — real
  hörbar ist meist nur eines gleichzeitig). Idee: On-Demand-Synths statt eines
  durchgehend laufenden pro Objekt — bei jedem Trigger neu erzeugen, nach
  Abklingen von Sample+HRTF-Tail per `doneAction` automatisch freigeben. Bricht
  mit dem bisher einheitlichen "ein Synth pro SoundObject läuft von play() bis
  stop()"-Muster (passt zu `InsectSound`, das kontinuierlich tönt, aber nicht
  zu rhythmischen One-Shots) — eigener Intent, kein Quick-Fix.
