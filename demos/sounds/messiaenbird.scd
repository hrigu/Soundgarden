// messiaenbird.scd — minimaler Testharness für MessiaenBirdSound alleine: kein Room, kein
// Binauralizer, keine Spatial-Logik. Zum schnellen Hörtest der Messiaen-inspirierten
// Beispiel-Motive (BirdMotifExamples, Intent 59) und zum musikalischen Nachjustieren, bevor sie
// in der vollen räumlichen Komposition (oeuvre/messiaen_birds/messiaen_birds.scd) laufen.
//
// Vorher: boot/load_classes.scd (nur beim Erst-Setup nötig, siehe Kommentar dort), dann
// boot/boot.scd (bei jedem Start, Server muss laufen).
// Kein Kopfhörer-Zwang hier — MessiaenBirdSound.play() ist mono, kein binaurales Rendering.

(
MessiaenBirdSound.addSynthDef;
// monitorMono: liest den privaten Mono-Bus, den Sound>>play anlegt (siehe Sound.sc — normal
// liest der Binauralizer davon und schreibt binaural auf den Hardware-Output), direkt auf den
// Hardware-Output. addAfter im Aufruf unten, damit derselbe Audio-Block gelesen wird (gleiche
// Begründung wie bei SoundObject>>play für den echten Binauralizer).
SynthDef(\monitorMono, { |in = 0, out = 0, amp = 1|
	Out.ar(out, In.ar(in, 1).dup * amp);
}).add;
)

// --- Beispiel 1: blackbirdRun — perlender, schneller Lauf ---
(
~bird = MessiaenBirdSound.new(BirdMotifExamples.blackbirdRun, loop: true);
~bird.play(s);
~monitor = Synth(\monitorMono, [\in, ~bird.bus.index, \out, 0], target: ~bird.synth,
	addAction: \addAfter);
)

~bird.stop;
~monitor.free;

// --- Beispiel 2: wrenTrillCascade — sehr schnelle Triller-Kaskade ---
(
~bird = MessiaenBirdSound.new(BirdMotifExamples.wrenTrillCascade, brightness: 0.7, loop: true);
~bird.play(s);
~monitor = Synth(\monitorMono, [\in, ~bird.bus.index, \out, 0], target: ~bird.synth,
	addAction: \addAfter);
)

~bird.stop;
~monitor.free;

// --- Beispiel 3: callBirdLeaps — großer Intervallsprung, einmaliger Durchlauf (kein loop) ---
(
~bird = MessiaenBirdSound.new(BirdMotifExamples.callBirdLeaps, amp: 0.5);
~bird.play(s);
~monitor = Synth(\monitorMono, [\in, ~bird.bus.index, \out, 0], target: ~bird.synth,
	addAction: \addAfter);
)

~bird.stop;
~monitor.free;

// --- Beispiel 4: eigenes Motiv per BirdMotif.fromIntervals zusammenstellen ---
// Halbtonschritte relativ zur Startfrequenz, nicht relativ zur Vornote (siehe BirdMotif.sc).
(
~bird = MessiaenBirdSound.new(
	BirdMotif.fromIntervals(4000, [0, 3, 7, 10, 7, 3], [0.1, 0.09, 0.08, 0.09, 0.08, 0.1]),
	loop: true
);
~bird.play(s);
~monitor = Synth(\monitorMono, [\in, ~bird.bus.index, \out, 0], target: ~bird.synth,
	addAction: \addAfter);
)

~bird.stop;
~monitor.free;
