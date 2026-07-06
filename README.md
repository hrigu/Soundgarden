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

Eigene Produktiv-Klassen (`BootTrackDetection`, `Movable`, `Listener`,
`SoundObject`, ...) liegen in `classes/`, die zugehörigen Test-Klassen
(`UnitTest`-Subklassen wie `TestBootTrackDetection`) getrennt davon in
`tests/`. Damit SuperCollider beide kennt, einmal pro Sitzung **als
allererstes** (vor `boot.scd`!) **`load_classes.scd`** ausführen und im
Post-Fenster auf `compile done` warten — danach stehen sie überall zur
Verfügung. Ohne diesen Schritt scheitert `boot.scd` mit
`ERROR: Class not defined.` (auf einer frisch gestarteten
SuperCollider-Sitzung ist die Klassenbibliothek noch im
Auslieferungszustand).

## Tests

Die reine sclang-Logik (aktuell: Extension-Erkennung für Tracks) wird über
SuperCollders eingebautes `UnitTest`-Framework getestet — ohne Server, ohne
echte Audiodateien. Nach `load_classes.scd` einfach **`run_tests.scd`**
ausführen; Ergebnis (grün/rot pro Testfall) erscheint im Post-Fenster.

## Zweiter Prototyp: binaurales Insekt (`insect_demo.scd`)

Ein virtuelles Klangobjekt ("Insekt"), das nach einer Bewegungsregel durch
einen definierten Raum kurvt; der Hörer (aktuell fix in der Mitte, mit zwei
Ohren) bekommt es über Kopfhörer binaural zugespielt — mal vorne, mal
hinten, mal laut (nah), mal leise (fern).

Objektmodell:
- **`Movable`** — hat eine Position, die sich nach einer Bewegungsregel
  verändert; steuert sanft zurück, falls sie eine Raumkugel verlässt.
- **`Listener`** — Position und Blickrichtung aktuell fix; berechnet aus
  einer Weltposition Azimuth (relativ zur eigenen Blickrichtung) und Distanz.
- **`SoundObject`** — verbindet ein `Movable` mit einem klingenden Synth;
  ein `Routine` tickt die Bewegung und schiebt Azimuth/Distanz live in den
  Synth (Pan, winzige Laufzeitdifferenz zwischen den Ohren, entfernungs-
  abhängiger Pegel/Tiefpass).

Ausführen: `load_classes.scd` → `boot.scd` → `insect_demo.scd` (Block für
Block), mit Kopfhörern.

Aktuell eine einfache Binaural-Näherung mit reinen Core-UGens (kein
HRTF/Kunstkopf) — vorne/hinten bleibt dadurch etwas mehrdeutig. Echtes HRTF
(überzeugenderes Vorne/Hinten, braucht ein Zusatzpaket) ist eine mögliche
spätere Ausbaustufe.

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
