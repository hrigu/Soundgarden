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

1. **`boot.scd`** – bootet den Server und lädt alle Dateien aus `sounds/`
   automatisch in `~tracks` (z.B. `~tracks[\mytrack01]`).
2. **`fx.scd`** – definiert die FX-Bausteine (`~fxChains`), die du live
   kombinierst.
3. **`set_template.scd`** – das eigentliche Live-Set. Jeder Block darin ist
   während der Party einzeln ausführbar: Track starten, FX-Kette
   umschalten, Parameter live verstellen, in den nächsten Track überblenden.

Kernidee: `Ndef(\track, { ... })` und `Ndef(\fx, { ... })` einfach neu
ausführen – JITLib überblendet automatisch (`fadeTime`), es gibt keine
Klicks oder Unterbrechungen.

## Nächste Schritte / Ideen

- MIDI-Controller oder OSC-Fader anbinden, um Parameter (Cutoff, Mix,
  Gate-Rate) live mit den Händen statt per Tastatur zu steuern.
- Weitere FX-Bausteine in `fx.scd` ergänzen (z.B. Reverse, Granular).
- Pattern-basierte Automation (`Pbind`) für sich selbst entwickelnde
  Übergänge zwischen Tracks.
