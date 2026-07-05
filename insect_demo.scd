// insect_demo.scd — Prototyp: ein binaurales "Insekt" kreist um den Hörer.
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) boot.scd        — Server muss laufen
//   2) load_classes.scd — bindet Movable/Listener/SoundInsect in den Klassenpfad ein
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning) funktionieren nicht über
// Lautsprecher richtig.
//
// Hinweis zur Räumlichkeit: Das ist die "einfache" Variante (Core-UGens, kein
// echtes HRTF) — Panning + winzige Laufzeitdifferenz zwischen den Ohren (ITD) +
// entfernungsabhängiger Pegel/Tiefpass. Vorne/hinten bleibt dadurch etwas
// mehrdeutig (das bekannte "Cone of Confusion"-Problem echter Ohren lässt sich
// ohne Kunstkopf-Impulsantworten nicht ganz auflösen) — ein leichtes zusätzliches
// Dämpfen "hinter" dem Hörer gibt zumindest einen groben Anhaltspunkt.

// --- 1) Insekt-Klang definieren ---
(
SynthDef(\insectVoice, { |azimuth = 0, distance = 1, amp = 0.35, out = 0|
	var az = Lag.kr(azimuth, 0.08);
	var dist = Lag.kr(distance, 0.08);
	var pan = sin(az);
	var itd = 0.0006 * pan; // Sekunden — realistische Größenordnung für Kopfbreite
	var cutoff = (9000 / (1 + dist)).clip(600, 9000); // ferner = dumpfer
	var behindDamp = cos(az).linlin(-1, 1, 0.55, 1);   // hinten etwas leiser/dumpfer
	var distAmp = (amp / (1 + (dist * 0.9))).clip(0, 1);
	var wings = LFPulse.ar(210, 0, 0.25) * WhiteNoise.ar(1);
	var buzz = Ringz.ar(wings, 3200, 0.02) + Ringz.ar(wings, 4600, 0.015);
	var sig = LPF.ar(buzz, cutoff) * behindDamp * distAmp;
	var stereo = Pan2.ar(sig, pan);
	var left = DelayL.ar(stereo[0], 0.01, 0.0015 + itd.max(0));
	var right = DelayL.ar(stereo[1], 0.01, 0.0015 + itd.neg.max(0));
	Out.ar(out, [left, right]);
}).add;
)

// --- 2) Bewegungsregel + Movable/Listener/SoundInsect anlegen ---
// Feste Bahn: Kreis um den Hörer, dessen Radius langsam "atmet"
// (2.5m ± 1.2m) — testet Azimuth (rundherum) UND Distanz (laut/leise) zugleich.
(
~circleRule = { |pos, t, dt|
	var radius = 2.5 + (1.2 * sin(t * 0.15));
	var angle = t * 0.6;
	[radius * sin(angle), radius * cos(angle), 0]
};

~listener = Listener.new;                          // fix im Ursprung, Blick entlang +y
~movable = Movable.new([0, 2.5, 0], ~circleRule, 5); // Raumkugel: Radius 5m
~insect = SoundInsect.new(~movable, ~listener);
)

// --- 3) Abspielen ---
~insect.play(s);

// --- 4) Stoppen ---
// ~insect.stop;
