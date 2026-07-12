# CLAUDE.md

Diese Datei gibt Claude Code Hinweise für die Arbeit in diesem Repository.

Zu Beginn jeder Sitzung `AGENTS.md` vollständig lesen, bevor Änderungen vorgenommen werden —
dort steht das Intent-Driven-Development-Protokoll (IDD), nach dem in diesem Repo gearbeitet wird.

## Was das hier ist

Soundgarden ist ein SuperCollider-Spielplatz mit aktuell zwei Arbeitssträngen:

1. **Live-Coding-Rig** (`boot/boot.scd`, `experiments/live_coding_rig/fx.scd`,
   `experiments/live_coding_rig/set_template.scd`) — bestehende elektronische
   Tracks live umformen (Filter, Echo, Bitcrush, Stutter, Reverb) während einer
   Fusion-Tanzparty, über JITLib (`Ndef`), rein per Tastatur (kein MIDI-Controller).
2. **Spatial-Audio-Prototyp** (`classes/sg/`, `demos/grill/insects.scd`) — virtuelle
   Klangobjekte ("Insekten", Samples, Songs), die sich nach einer Bewegungsregel durch einen
   definierten Raum bewegen (oder stationär bleiben) und dem Hörer über Kopfhörer binaural
   zugespielt werden.

`demos/` enthält technische Beispiele/Vorführungen einzelner Features; `oeuvre/` (z.B.
`oeuvre/dawn/dawn_chorus.scd`, Intent 57/58) eigenständige Kompositionen, die über Timeline
(`classes/sg/spatial/Timeline.sc`) eine zeitliche Entwicklung durchlaufen — beide nutzen
dieselben `classes/sg/`-Bausteine, unterscheiden sich nur im Zweck (Beispiel vs. fertiges Stück).

Kommentare und Commit-Messages in diesem Repo sind auf Deutsch.

## Befehle

- SuperCollider-IDE (scide) öffnen (Spotlight oder `open -a SuperCollider`).
- `boot/load_classes.scd` einmalig beim Erst-Setup ausführen — bindet `classes/`/`tests/` über
  `LanguageConfig.addIncludePath` in den Klassenpfad ein und kompiliert neu; danach im
  Post-Fenster auf `compile done` warten. `addIncludePath` persistiert dabei standardmäßig in
  `sclang_conf.yaml`, weshalb SuperCollider die eigenen Klassen ab diesem Zeitpunkt bei **jedem**
  Start automatisch mitkompiliert — der Schritt ist danach nicht mehr nötig, außer nach einem
  Reset/Löschen von `sclang_conf.yaml` oder auf einer neuen Maschine. **Muss vor dem ersten Lauf
  von `boot/boot.scd`** erfolgt sein — `boot/boot.scd` ruft `BootTrackDetection` auf, das erst
  nach diesem Schritt existiert.
- `run_tests.scd` — automatisierte Tests (`UnitTest`) für die reine sclang-Logik.
- Live-Set-Workflow: `boot/boot.scd` → `experiments/live_coding_rig/fx.scd` →
  `experiments/live_coding_rig/set_template.scd` (Block für Block, `Cmd+Enter`).
- Spatial-Audio-Prototyp: `boot/boot.scd` → eine der Varianten in `demos/grill/` (bzw.
  `demos/sample.scd`, `demos/reverb.scd`, ...) oder eine Komposition unter `oeuvre/` (z.B.
  `oeuvre/dawn/dawn_chorus.scd`) — **Kopfhörer benutzen**, binaurale Effekte funktionieren über
  Lautsprecher nicht richtig.

## Architektur

- sclang + scsynth (Client/Server), JITLib (`Ndef`) fürs klickfreie Live-Editing laufender Synths.
- `classes/` enthält Produktiv-Klassen (`BootTrackDetection`), `tests/` die zugehörigen
  `UnitTest`-Klassen (`TestBootTrackDetection`) — beide projekt-lokal eingebunden via
  `LanguageConfig.addIncludePath` + `thisProcess.recompile` in `boot/load_classes.scd` (kein
  globaler Extensions-Ordner; beide Pfade werden rekursiv eingelesen).
- Spatial-Audio-Klassen liegen unter `classes/sg/` (kein echter SC-Namespace — SuperCollider
  hat nur einen globalen, flachen Klassen-Namensraum — sondern reine Ordnerkonvention, vier
  Unter-Ordner nach Zuständigkeit):
  - `classes/sg/sounds/` — `Sound` (Basisklasse: Bus-/Synth-Lifecycle, `editableParams`/
    `setParam`-Hook fürs GUI, `call` löst einen kurzen Klang-Akzent für Call-and-Response
    aus), `InsectSound` (Flügelschlag+Brummen, optional per `CallingPattern` rhythmisiert),
    `CricketSound` (`InsectSound`-Ableitung mit bioakustisch recherchierten Defaults),
    `SampleSound` (rhythmisch getriggertes Sample, mit `startFrac`/`duration`-Ausschnitt-Wahl
    und Waveform-Preview fürs GUI), `SampleSoundConfigurator` (Ausschnitts-/Lautheits-
    Heuristik für `SampleSound`), `SongSound` (durchlaufender Song/Atmo als Klangquelle),
    `CallingPattern`/`RhythmPatternCreator` (An/Aus-Zeitmuster für Ruf-Rhythmen),
    `SoundPresetLibrary` (Klangparameter-Presets als `.scd`-Dateien speichern/laden),
    `BirdSound` (fester `baseFreq`-Zwitscher-Akzent für die Dämmerungs-Komposition, Intent 57),
    `BirdMotif`/`BirdMotifExamples` (Tonhöhen-/Rhythmus-Sequenz statt reinem An/Aus-Timing —
    `fromIntervals` beschreibt ein Motiv als Halbtonschritte relativ zur `startFreq`;
    `BirdMotifExamples` liefert Messiaen-inspirierte Platzhalter-Motive, keine Zitate aus dem
    urheberrechtlich geschützten Catalogue d'oiseaux, Intent 59), `MessiaenBirdSound`
    (spielt ein `BirdMotif` über eine Routine ab, die pro Note `freq`/`noteDur`/`t_trig` auf
    einen einzigen laufenden Synth setzt statt bei jeder Note neu zu starten; `pitchShift`
    transponiert die Tonhöhen live übers GUI, ohne `motif` selbst neu zu bauen)
  - `classes/sg/soundobjects/` — `SoundObject` (Bewegung + Klang + Binauralizer;
    `fadeOutAndStop` stoppt klickfrei: amp → 0, Lag-Ausklang abwarten, dann stop, mit
    optionalem `onComplete`-Callback fürs Aufräumen durch den Aufrufer, z.B. Orchestra-
    Unregister), `Movable`, `MoveRule`, `CircularMoveRule`, `SteadyMoveRule`,
    `SoundObjectBuilder` (baut ein `SoundObject` aus zwei Params-Events statt Sound/Movable
    von Hand zu instanzieren)
  - `classes/sg/spatial/` — `Timeline` (allgemeines Modell für "Klang über Zeit verändern":
    `at`/`ramp`-Cues, `staggeredTimes`-Utility für gestaffelte Einsatzzeitpunkte mit
    wachsender Dichte + Jitter, siehe Intent 57/58), `Listener` (Position/Blickrichtung, kennt
    seine "Ohren"-Strategie, siehe `binauralizerClass`/`makeBinauralizer`), `Orchestra`
    (zentraler Taktgeber, tickt alle registrierten `SoundObject`s), `Room` (kapselt Raumakustik +
    `Orchestra` + gemeinsamen `RoomReverb`, einziger Ort fürs Größe/Höhe/Oberfläche →
    Hallparameter-Mapping), `RoomReverb` (ein geteilter GVerb-Hall für den ganzen Raum),
    `Binauralizer` (einfache Pan/ITD/Distanz-Näherung, reine Core-UGens), `AtkBinauralizer`
    (echtes HRTF-Rendering über ATK/`FoaDecoderKernel`, geteilter Decoder-Kernel pro Session),
    `DirectOutBinauralizer` (Durchleitung ohne Binauralisierung, Fallback für
    `binauralizerClass = nil`) — alle drei Binauralizer-Klassen teilen sich informell
    (Duck-Typing) das Interface `play`/`set`/`stop`, kanonisch dokumentiert in
    `Binauralizer.sc`; `RoomRecorder` (Start/Stop-Aufnahme + Dateiname-/Zeitstempel-Erzeugung,
    idempotent wie `SoundObject>>stop` — Name bewusst nicht `Recorder`, das kollidiert mit einer
    gleichnamigen SuperCollider-Kernklasse, Intent 60)
  - `classes/sg/gui/` — `SpatialControlPanel` (ein Fenster: editierbare Raum-Draufsicht +
    Regler für Room-/Hall-/Soundobjekt-Parameter, Tastatursteuerung des Listeners W/S/A/D/Q/E,
    Solo/Mute, Preset-Load/Save, optionaler Start/Stop-Recording-Bereich über `recordingsDir`,
    siehe `RoomRecorder`), `RoomControlPanel` (dünner Lifecycle-Wrapper um
    `SpatialControlPanel` fürs Demo-Skript, hält genau eine Panel-Instanz)
  - zugehörige Tests spiegelbildlich unter `tests/sg/sounds/`, `tests/sg/soundobjects/`,
    `tests/sg/spatial/`, `tests/sg/gui/` (nur rein logische, servertfreie GUI-Tests)
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
  Installation `ERROR: Class not defined`. In `experiments/live_coding_rig/fx.scd` stattdessen
  mit `Latch` + `round`
  nachgebaut.
- Kein MIDI-Controller vorgesehen — Steuerung ist bewusst reine Tastatur (`Cmd+Enter` je Block).
- `rrand(*someArray)` funktioniert **nicht** wie ein normaler Funktionsaufruf mit Array-Splatting:
  `rrand(lo, hi)` wird vom Compiler wie ein Binäroperator (`lo.rrand(hi)`) übersetzt, nicht wie
  eine generische Methode — das Splatting landet dann als einzelnes Args-Array beim (fehlenden)
  Empfänger `nil` statt auf zwei echte Argumente verteilt zu werden. Fehlerbild: `ERROR: binary
  operator 'rrand' failed. RECEIVER: nil` erst zur Laufzeit, kein Compile-Fehler. Immer explizit
  `rrand(range[0], range[1])` schreiben, nie `rrand(*range)` (zweimal in Intent 59 aufgetreten,
  siehe `oeuvre/messiaen_birds/messiaen_birds.scd`).
- Bluetooth-Headsets (z.B. AirPods, Bose) liefern für ihr Mikrofon oft nur 16kHz, während der
  Output mit 44100Hz läuft — das lässt den Server-Boot mit einem Samplerate-Konflikt scheitern,
  UND hält macOS die Verbindung systemweit im HFP-Modus (mono, verrauscht, kein Stereo-Panning
  hörbar), solange das Headset in den **macOS-Systemeinstellungen** (Ton → Eingabe) noch als
  Standard-Mikrofon eingestellt ist. Der eigentliche Fix ist dort: Eingabegerät auf ein
  Nicht-Bluetooth-Gerät umstellen, dann handelt macOS A2DP (sauberes Stereo) neu aus.
  `s.options.numInputBusChannels = 0` in `boot/boot.scd` ist zusätzlich vorbereitet (aktuell
  auskommentiert), falls der Server trotzdem mit aktivem Bluetooth-Mikrofon gebootet wird.

Für das vollständige IDD-Protokoll (Intent-Format, Ordner-Workflow, Task-Bestätigungen,
Commit-Konventionen) siehe `AGENTS.md`.
