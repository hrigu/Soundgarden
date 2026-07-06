// insect_demo.scd — Prototyp: zwei binaurale "Insekten" kreisen um den Hörer, dazu ein
// stationäres ("steady") Soundobjekt, dessen Position live per Code verändert werden kann.
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) load_classes.scd — bindet Movable/Listener/CircularMoveRule/SteadyMoveRule/InsectSound/
//                          Binauralizer/SoundObject/Orchestra in den Klassenpfad ein
//   2) boot.scd        — Server muss laufen
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning) funktionieren nicht über
// Lautsprecher richtig.
//
// Hinweis zur Räumlichkeit: Das ist die "einfache" Variante (Core-UGens, kein
// echtes HRTF) — Panning + winzige Laufzeitdifferenz zwischen den Ohren (ITD) +
// entfernungsabhängiger Pegel/Tiefpass, siehe Binauralizer. Vorne/hinten bleibt
// dadurch etwas mehrdeutig (das bekannte "Cone of Confusion"-Problem echter
// Ohren lässt sich ohne Kunstkopf-Impulsantworten nicht ganz auflösen) — ein
// leichtes zusätzliches Dämpfen "hinter" dem Hörer gibt zumindest einen groben
// Anhaltspunkt.

// --- 1) SynthDefs registrieren (einmal pro Sitzung, vor dem Abspielen) ---
// /d_recv ist asynchron — dieser Schritt muss abgeschlossen sein, bevor Block 3
// versucht, Synths dieser Defs zu instanzieren. Beide Insekten teilen sich
// dieselben SynthDefs (\insectSound, \binauralizer), nur mit unterschiedlichen
// Synth-Args.
(
InsectSound.addSynthDef;
Binauralizer.addSynthDef;
)

// --- 2) Zwei Insekten anlegen: unterschiedliches Tempo/Richtung, unterschiedlicher Klang ---
// Ein gemeinsamer Listener (fix im Ursprung) hört beide. Jedes Insekt braucht
// aber sein eigenes Movable/InsectSound/Binauralizer — die Binauralisierung
// hängt von der Position ab, die pro Insekt verschieden ist, und jeder Synth
// braucht seinen eigenen Bus/State. Ein gemeinsames Orchestra hält den Listener
// und tickt beide Insekten über eine einzige Routine statt je einer eigenen —
// die Insekten selbst kennen den Listener nicht mehr.
(
var listener = Listener.new;

// Insekt 1: CircularMoveRule-Defaults (2.5m Radius, ±1.2m Atmung, 0.6 rad/s
// im Uhrzeigersinn), Standard-Klang.
var moveRule1 = CircularMoveRule.new;
var movable1 = Movable.new([0, 2.5, 0], moveRule1, 5);
var insectSound1 = InsectSound.new;
var binauralizer1 = Binauralizer.new;

// Insekt 2: etwas schneller (1.0 statt 0.6 rad/s) und in Gegenrichtung
// (negatives angularSpeed); heller/schneller Klang (höhere Flügelschlag- und
// Resonanzfrequenzen, kürzeres Ausklingen) statt Standard-Brummen.
var moveRule2 = CircularMoveRule.new(3.0, 1.0, 0.15, -1.0);
var movable2 = Movable.new([0, -3.0, 0], moveRule2, 5);
var insectSound2 = InsectSound.new(280, 0.2, 5200, 7000, 0.015, 0.01, 0.3);
var binauralizer2 = Binauralizer.new;

var insect1 = SoundObject.new(movable1, insectSound1, binauralizer1);
var insect2 = SoundObject.new(movable2, insectSound2, binauralizer2);

// "Brunnen": steady Soundobjekt — SteadyMoveRule bewegt sich nicht von selbst
// (next() gibt die Position unverändert zurück), tiefer/langsamer Klang zur
// Unterscheidung von den Insekten. Position per Live-Coding änderbar, siehe
// Block 3b weiter unten (~fountainMovable.moveTo(...)).
var fountainMoveRule = SteadyMoveRule.new;
~fountainMovable = Movable.new([2, 1, 0], fountainMoveRule, 5);
var fountainSound = InsectSound.new(60, 0.6, 800, 1400, 0.08, 0.06, 0.25);
var fountainBinauralizer = Binauralizer.new;
var fountain = SoundObject.new(~fountainMovable, fountainSound, fountainBinauralizer);

~orchestra = Orchestra.new(listener);
~orchestra.register(insect1);
~orchestra.register(insect2);
~orchestra.register(fountain);
)

// --- 3) Abspielen ---
// orchestra.play startet den Klang-/Binauralizer-Synth für jedes registrierte
// Insekt und danach den gemeinsamen Taktgeber (Bewegung + Azimuth/Distanz
// für beide).
~orchestra.play(s);

// --- 3b) Live-Coding: Brunnen umplatzieren ---
// Position jederzeit während des Sets änderbar — moveTo setzt sie direkt,
// ohne die roomRadius-Rückführung, die für die Insekten-Eigenbewegung gedacht ist.
~fountainMovable.moveTo([-3, 2, 0]);

// --- 4) Stoppen ---
// Ein Aufruf stoppt das Ticken UND ruft stop auf beiden registrierten
// Insekten auf (Synths/Busse werden freigegeben).
~orchestra.stop;
