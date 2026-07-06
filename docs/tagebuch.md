# Tagebuch

Kurzes Projekt-Tagebuch: ein Eintrag pro Arbeitstag, was gemacht wurde und welche Probleme
dabei aufgetaucht sind. Bezieht sich auf Intents in `.intents/` (siehe dort für Details).

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
