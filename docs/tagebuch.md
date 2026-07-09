# Tagebuch

Kurzes Projekt-Tagebuch: ein Eintrag pro Arbeitstag, was gemacht wurde und welche Probleme
dabei aufgetaucht sind. Bezieht sich auf Intents in `.intents/` (siehe dort für Details).

## 08.07.2026

- `reverb_song.scd` als neue Reverb-Demo mit echten Songs aus `musicnet/downloads` angelegt.
- Song-Suche ergänzt: rekursiver Scan, exakter Name vor Teilstring, sonst Fallback auf erste Datei.
- Song-Wiedergabe auf kontinuierliches `SongSound`-Playback als stationäre Raumquelle umgestellt.
- Für `demos/reverb/reverb_song.scd` geklärt, dass `.m4a`-Quelldateien vor dem Laden per
  `ffmpeg` nach `.wav` konvertiert werden müssen; dafür lokal `ffmpeg` installiert.
- GUI für den Reverb-Song-Workflow ergänzt.
- Setup in `Application Support/SuperCollider` ergänzt (`startup.scd`/Klassenpfade), damit
  Soundgarden-Klassen automatisch geladen werden.

## 07.07.2026

- Freesound-Samples (CompMusic-Pack, Mridangam-Schläge) besorgt und `sounds/` in
  Unterordner (`apple/`, `compmusic/`) reorganisiert — Bug: `sample_demo.scd`s Scan fand nur
  die oberste Ebene, `entries.select` durch rekursives `filesDo` ersetzt.
- Intent 21: `CircularMoveRule` um `startAngle` erweitert (ohne das würden alle Instanzen bei
  `t=0` auf demselben Winkel starten, unabhängig von der Movable-Startposition) — neues
  `sample_demo_moving.scd`: Sample-Objekte aus `sounds/compmusic/` bewegen sich auf einer
  gemeinsamen Kreisbahn (gleicher Radius, gleiche Richtung, gestaffeltes Tempo 0.05–0.3 rad/s).
  Hörtest bestätigt: Bewegung und Tempo-Unterschiede gut hörbar.
- Nebenbei: `scsynth`-Boot-Fehler ("address in use") durch verwaisten Server-Prozess behoben.
- Entdeckt: `sclang` lässt sich headless direkt ausführen (`/Applications/SuperCollider.app/
  Contents/MacOS/sclang`, `cocoa`- statt `offscreen`-Qt-Plugin) — Tests künftig selbst
  verifizierbar statt den Nutzer jedes Mal zu fragen.
- Idee notiert (README): `SampleSound`/`AtkBinauralizer` ressourcenschonender machen
  (On-Demand-Synths statt dauerhaft laufender pro Objekt).

## 06.07.2026

- Intents 10–14: `SoundObject`-Umbenennung, `Sound`-Superklasse, `Orchestra`-Klasse,
  `sg/`-Namespace-Ordnerstruktur.
- Intents 15–17: steady Soundobjekte (`SteadyMoveRule`/`moveTo`), Tastatursteuerung für den
  Listener (`KeyboardListenerControl`), Call-and-Response zwischen Soundobjekten.
- Intent 18: `insect_demo.scd`-Setup zu einem Block zusammengefasst.
- Intent 19: `SpaceView` (Live-Draufsicht). Zwei Bugs beim Testen gefunden und gefixt:
  Refresh-Routine lief auf `SystemClock` statt `AppClock` (Fenster blieb Standbild);
  Listener wurde immer im Fensterzentrum gezeichnet statt an seiner echten Position
  (Rotation sichtbar, Bewegung nicht).
- Intent 5: ATK-basiertes HRTF für Insekt 1 (`AtkBinauralizer`). Mehrere Probleme
  unterwegs: Scope nachträglich auf die horizontale Ebene reduziert (Datenmodell kennt
  keine Elevation); `PanB2` statt `FoaEncode`/`FoaTransform` verwendet, um eine
  sc3-plugins-Abhängigkeit (`FoaRotate not installed`) zu vermeiden; Azimuth-Wrap-Klick
  behoben (Lag.kr auf gewrapptem Winkel); Kernel-Lade-Race behoben (`AtkBinauralizer.setup`
  wartet jetzt intern per `server.sync`). Hörtest bestätigt: kein Klick mehr, deutlich
  besseres Vorne/Hinten-Gefühl als die alte Pan/ITD-Näherung.
- Intent 20: rhythmische Sample-Wiedergabe im Kreis (`SampleSound`, `sample_demo.scd`) —
  alle Samples aus `sounds/` dynamisch im Kreis verteilt, Listener im Zentrum. Zwei kleine
  Bugs: `Buffer.read(..., channels:)` existiert nicht (richtig: `Buffer.readChannel`);
  `Buffer UGen: no buffer data`-Warnung durch `SampleSound>>preload` behoben (Buffer lädt
  vor statt erst bei `orchestra.play`). Hörtest bestätigt: guter Rhythmus, gute
  Räumlichkeit.

## 05.07.2026

- Intent-Driven-Development-Protokoll (CLAUDE.md/AGENTS.md) eingeführt, Intents 1–4
  rückwirkend dokumentiert.
- Intent 4: HRTF-Optionen für SuperCollider recherchiert, Entscheidung für ATK.
- Bug: Server-Boot scheiterte mit Bluetooth-Headset (Samplerate-Konflikt) — Fix über
  macOS-Eingabegerät, `numInputBusChannels` als Fallback vorbereitet.
- Bug: `load_classes.scd` muss vor `boot.scd` laufen, Doku war falsch herum.
- Intent 9: grosses Refactoring — Insekt-Architektur aufgeteilt in `MoveRule`/
  `CircularMoveRule`, `InsectSound`, `Binauralizer`.
- Zweites Insekt in `insect_demo.scd` ergänzt, kleinere Bugs unterwegs (Tippfehler).


## 04.07.2026

- Live-Coding-Rig Grundgerüst angelegt (`boot.scd`, `fx.scd`, `set_template.scd`).
- Track-Extension-Erkennung: Bug gefunden (`PathName>>extension` liefert ein Symbol,
  `includes`/`indexOf` vergleichen aber per Identität) — mit UnitTest abgesichert.
- `Decimator` (sc3-plugins) durch Core-UGen-Bitcrush (`Latch`+`round`) ersetzt, da
  sc3-plugins bewusst nicht installiert ist.
