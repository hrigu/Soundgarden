// insect_demo.scd — Prototyp: zwei binaurale "Insekten" kreisen um den Hörer.
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) boot.scd        — Server muss laufen
//   2) load_classes.scd — bindet Movable/Listener/CircularMoveRule/InsectSound/
//                          Binauralizer/SoundObject in den Klassenpfad ein
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
// braucht seinen eigenen Bus/State.
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

~insect1 = SoundObject.new(movable1, listener, insectSound1, binauralizer1);
~insect2 = SoundObject.new(movable2, listener, insectSound2, binauralizer2);
)

// --- 3) Abspielen ---
~insect1.play(s);
~insect2.play(s);

// --- 4) Stoppen ---
 ~insect1.stop;
 ~insect2.stop;
