// insect_demo.scd — Prototyp: ein binaurales "Insekt" kreist um den Hörer.
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) boot.scd        — Server muss laufen
//   2) load_classes.scd — bindet Movable/Listener/CircularMoveRule/InsectSound/
//                          Binauralizer/SoundInsect in den Klassenpfad ein
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
// versucht, Synths dieser Defs zu instanzieren.
(
InsectSound.addSynthDef;
Binauralizer.addSynthDef;
)

// --- 2) Bewegungsregel, Klang, Binauralisierung und Listener anlegen ---
// Feste Bahn: Kreis um den Hörer, dessen Radius langsam "atmet"
// (2.5m ± 1.2m, CircularMoveRule-Defaults) — testet Azimuth (rundherum) UND
// Distanz (laut/leise) zugleich.
(
~moveRule = CircularMoveRule.new;                   // Defaults: 2.5m Radius, ±1.2m Atmung
~movable = Movable.new([0, 2.5, 0], ~moveRule, 5);  // Raumkugel: Radius 5m
~listener = Listener.new;                           // fix im Ursprung, Blick entlang +y
~insectSound = InsectSound.new;                     // reiner Klang, kennt keine Position
~binauralizer = Binauralizer.new;                   // formt Pan/ITD/Distanz für den Listener
~insect = SoundInsect.new(~movable, ~listener, ~insectSound, ~binauralizer);
)

// --- 3) Abspielen ---
~insect.play(s);

// --- 4) Stoppen ---
// ~insect.stop;
