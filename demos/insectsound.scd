// insectsound.scd — minimaler Testharness für InsectSound alleine: kein Room, kein
// Binauralizer, keine Spatial-Logik. Zum schnellen Herumspielen mit Klangparametern und
// CallingPattern-Konfigurationen, ohne die ganze Insekten-Population aus insects.scd neu
// starten zu müssen.
//
// Vorher (einmal pro Sitzung): boot/load_classes.scd, dann boot/boot.scd (Server muss laufen).
// Kein Kopfhörer-Zwang hier — InsectSound.play() ist mono, kein binaurales Rendering.

(
InsectSound.addSynthDef;
// monitorMono: liest den privaten Mono-Bus, den Sound>>play anlegt (siehe Sound.sc — normal
// liest der Binauralizer davon und schreibt binaural auf den Hardware-Output), direkt auf den
// Hardware-Output. addAfter im Aufruf unten, damit derselbe Audio-Block gelesen wird (gleiche
// Begründung wie bei SoundObject>>play für den echten Binauralizer).
SynthDef(\monitorMono, { |in = 0, out = 0, amp = 1|
	Out.ar(out, In.ar(in, 1).dup * amp);
}).add;
)

// --- Beispiel 1: Cricket-Pattern (Intent 30/31) ---
(
~insect = InsectSound.new(
	pattern: CallingPattern.new([
		[0.02, true], [0.03, false],
		[0.02, true], [0.03, false],
		[0.02, true], [0.03, false],
		[0.02, true], [0.03, false],
		[0.2, false]
	])
);
~insect.play(s);
~monitor = Synth(\monitorMono, [\in, ~insect.bus.index, \out, 0], target: ~insect.synth,
	addAction: \addAfter);
)

~insect.stop;
~monitor.free;

// --- Beispiel 2: kein Pattern — bisheriges, durchgehendes Zirpen ---
(
~insect = InsectSound.new;
~insect.play(s);
~monitor = Synth(\monitorMono, [\in, ~insect.bus.index, \out, 0], target: ~insect.synth,
	addAction: \addAfter);
)

~insect.stop;
~monitor.free;

// --- Beispiel 3: eigenes Pattern zusammenstellen ---
// pattern ist zwar <>-beschreibbar, play() liest pattern aber nur beim Start (siehe
// InsectSound>>play) — für ein neues Pattern also stop + neue Instanz + play, kein
// Hot-Swap auf derselben laufenden Instanz.
(
~insect = InsectSound.new(
	wingRate: 280, amp: 0.3,
	pattern: CallingPattern.new([[0.4, true], [1.5, false]]) // "kurz an, lang aus"
);
~insect.play(s);
~monitor = Synth(\monitorMono, [\in, ~insect.bus.index, \out, 0], target: ~insect.synth,
	addAction: \addAfter);
)

~insect.stop;
~monitor.free;
