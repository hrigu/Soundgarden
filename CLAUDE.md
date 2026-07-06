# CLAUDE.md

Diese Datei gibt Claude Code Hinweise für die Arbeit in diesem Repository.

Zu Beginn jeder Sitzung `AGENTS.md` vollständig lesen, bevor Änderungen vorgenommen werden —
dort steht das Intent-Driven-Development-Protokoll (IDD), nach dem in diesem Repo gearbeitet wird.

## Was das hier ist

Soundgarden ist ein SuperCollider-Spielplatz mit aktuell zwei Arbeitssträngen:

1. **Live-Coding-Rig** (`boot.scd`, `fx.scd`, `set_template.scd`) — bestehende elektronische
   Tracks live umformen (Filter, Echo, Bitcrush, Stutter, Reverb) während einer
   Fusion-Tanzparty, über JITLib (`Ndef`), rein per Tastatur (kein MIDI-Controller).
2. **Spatial-Audio-Prototyp** (`classes/sg/`, `insect_demo.scd`) — virtuelle Klangobjekte
   ("Insekten"), die sich nach einer Bewegungsregel durch einen definierten Raum bewegen und
   dem Hörer über Kopfhörer binaural zugespielt werden.

Kommentare und Commit-Messages in diesem Repo sind auf Deutsch.

## Befehle

- SuperCollider-IDE (scide) öffnen (Spotlight oder `open -a SuperCollider`).
- `load_classes.scd` einmal pro Sitzung ausführen — bindet `classes/` in den Klassenpfad ein und
  kompiliert neu; danach im Post-Fenster auf `compile done` warten. **Muss vor `boot.scd`
  laufen** — `boot.scd` ruft `BootTrackDetection` auf, das erst nach diesem Schritt existiert.
- `run_tests.scd` — automatisierte Tests (`UnitTest`) für die reine sclang-Logik.
- Live-Set-Workflow: `boot.scd` → `fx.scd` → `set_template.scd` (Block für Block, `Cmd+Enter`).
- Spatial-Audio-Prototyp: `boot.scd` → `insect_demo.scd` —
  **Kopfhörer benutzen**, binaurale Effekte funktionieren über Lautsprecher nicht richtig.

## Architektur

- sclang + scsynth (Client/Server), JITLib (`Ndef`) fürs klickfreie Live-Editing laufender Synths.
- `classes/` enthält Produktiv-Klassen (`BootTrackDetection`), `tests/` die zugehörigen
  `UnitTest`-Klassen (`TestBootTrackDetection`) — beide projekt-lokal eingebunden via
  `LanguageConfig.addIncludePath` + `thisProcess.recompile` in `load_classes.scd` (kein
  globaler Extensions-Ordner; beide Pfade werden rekursiv eingelesen).
- Spatial-Audio-Klassen liegen unter `classes/sg/` (kein echter SC-Namespace — SuperCollider
  hat nur einen globalen, flachen Klassen-Namensraum — sondern reine Ordnerkonvention, drei
  Unter-Ordner nach Zuständigkeit):
  - `classes/sg/sounds/` — `Sound`, `InsectSound` (reine Klangerzeugung)
  - `classes/sg/soundobjects/` — `SoundObject`, `Movable`, `MoveRule`, `CircularMoveRule`,
    `SteadyMoveRule` (Bewegung + Klangobjekt)
  - `classes/sg/spatial/` — `Listener`, `Binauralizer`, `Orchestra` (Raumwahrnehmung/-steuerung)
  - zugehörige Tests spiegelbildlich unter `tests/sg/soundobjects/`, `tests/sg/spatial/`
  - `BootTrackDetection` gehört bewusst nicht zu `sg` (andere Domäne: Live-Set statt
    Spatial-Audio).
- `sc3-plugins` ist bewusst (noch) nicht installiert — wo möglich mit Core-UGens gearbeitet
  (siehe Gotchas). Für Intent 5 (ATK/echtes HRTF) wird die Zusatzinstallation in Kauf genommen.

## Gotchas

- `PathName>>extension` kann ein **Symbol** statt einen String liefern. `Array:-includes`/
  `-indexOf` vergleichen intern per Identität — zwei inhaltsgleiche, aber unterschiedliche
  String-Objekte matchen NICHT, auch wenn ein direkter `==`-Vergleich `true` ergibt. Extensions
  deshalb immer als Symbol vergleichen (siehe `classes/BootTrackDetection.sc`, getestet in
  `tests/TestBootTrackDetection.sc`).
- `Decimator` (Bitcrush-UGen) gehört zu `sc3-plugins`, nicht zum Core — auf einer frischen
  Installation `ERROR: Class not defined`. In `fx.scd` stattdessen mit `Latch` + `round`
  nachgebaut.
- Kein MIDI-Controller vorgesehen — Steuerung ist bewusst reine Tastatur (`Cmd+Enter` je Block).
- Bluetooth-Headsets (z.B. AirPods, Bose) liefern für ihr Mikrofon oft nur 16kHz, während der
  Output mit 44100Hz läuft — das lässt den Server-Boot mit einem Samplerate-Konflikt scheitern,
  UND hält macOS die Verbindung systemweit im HFP-Modus (mono, verrauscht, kein Stereo-Panning
  hörbar), solange das Headset in den **macOS-Systemeinstellungen** (Ton → Eingabe) noch als
  Standard-Mikrofon eingestellt ist. Der eigentliche Fix ist dort: Eingabegerät auf ein
  Nicht-Bluetooth-Gerät umstellen, dann handelt macOS A2DP (sauberes Stereo) neu aus.
  `s.options.numInputBusChannels = 0` in `boot.scd` ist zusätzlich vorbereitet (aktuell
  auskommentiert), falls der Server trotzdem mit aktivem Bluetooth-Mikrofon gebootet wird.

Für das vollständige IDD-Protokoll (Intent-Format, Ordner-Workflow, Task-Bestätigungen,
Commit-Konventionen) siehe `AGENTS.md`.
