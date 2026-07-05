# CLAUDE.md

Diese Datei gibt Claude Code Hinweise für die Arbeit in diesem Repository.

Zu Beginn jeder Sitzung `AGENTS.md` vollständig lesen, bevor Änderungen vorgenommen werden —
dort steht das Intent-Driven-Development-Protokoll (IDD), nach dem in diesem Repo gearbeitet wird.

## Was das hier ist

Soundgarden ist ein SuperCollider-Spielplatz mit aktuell zwei Arbeitssträngen:

1. **Live-Coding-Rig** (`boot.scd`, `fx.scd`, `set_template.scd`) — bestehende elektronische
   Tracks live umformen (Filter, Echo, Bitcrush, Stutter, Reverb) während einer
   Fusion-Tanzparty, über JITLib (`Ndef`), rein per Tastatur (kein MIDI-Controller).
2. **Spatial-Audio-Prototyp** (`classes/Movable.sc`, `classes/Listener.sc`,
   `classes/SoundInsect.sc`, `insect_demo.scd`) — ein virtuelles Klangobjekt ("Insekt"), das
   sich nach einer Bewegungsregel durch einen definierten Raum bewegt und dem Hörer über
   Kopfhörer binaural zugespielt wird.

Kommentare und Commit-Messages in diesem Repo sind auf Deutsch.

## Befehle

- SuperCollider-IDE (scide) öffnen (Spotlight oder `open -a SuperCollider`).
- `load_classes.scd` einmal pro Sitzung ausführen — bindet `classes/` in den Klassenpfad ein und
  kompiliert neu; danach im Post-Fenster auf `compile done` warten.
- `run_tests.scd` — automatisierte Tests (`UnitTest`) für die reine sclang-Logik.
- Live-Set-Workflow: `boot.scd` → `fx.scd` → `set_template.scd` (Block für Block, `Cmd+Enter`).
- Spatial-Audio-Prototyp: `boot.scd` → `load_classes.scd` → `insect_demo.scd` —
  **Kopfhörer benutzen**, binaurale Effekte funktionieren über Lautsprecher nicht richtig.

## Architektur

- sclang + scsynth (Client/Server), JITLib (`Ndef`) fürs klickfreie Live-Editing laufender Synths.
- `classes/` enthält eigene Klassen (`BootTrackDetection` + Test, `Movable`, `Listener`,
  `SoundInsect`) — projekt-lokal eingebunden via `LanguageConfig.addIncludePath` +
  `thisProcess.recompile` in `load_classes.scd` (kein globaler Extensions-Ordner).
- `sc3-plugins` ist bewusst (noch) nicht installiert — wo möglich mit Core-UGens gearbeitet
  (siehe Gotchas). Für Intent 5 (ATK/echtes HRTF) wird die Zusatzinstallation in Kauf genommen.

## Gotchas

- `PathName>>extension` kann ein **Symbol** statt einen String liefern. `Array:-includes`/
  `-indexOf` vergleichen intern per Identität — zwei inhaltsgleiche, aber unterschiedliche
  String-Objekte matchen NICHT, auch wenn ein direkter `==`-Vergleich `true` ergibt. Extensions
  deshalb immer als Symbol vergleichen (siehe `classes/BootTrackDetection.sc`).
- `Decimator` (Bitcrush-UGen) gehört zu `sc3-plugins`, nicht zum Core — auf einer frischen
  Installation `ERROR: Class not defined`. In `fx.scd` stattdessen mit `Latch` + `round`
  nachgebaut.
- Kein MIDI-Controller vorgesehen — Steuerung ist bewusst reine Tastatur (`Cmd+Enter` je Block).

Für das vollständige IDD-Protokoll (Intent-Format, Ordner-Workflow, Task-Bestätigungen,
Commit-Konventionen) siehe `AGENTS.md`.
