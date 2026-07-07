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
		[0.02, true], [0.3, false],
		[0.02, true], [0.3, false],
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

// --- Beispiel 3: eigenes Pattern zusammenstellen, per fromDurations ---
// fromDurations statt new([[dauer, istAn], ...]): reine Dauern, per Konvention beginnt das
// erste Segment "an", dann wechselt es strikt ab — kürzer zu schreiben für den häufigsten
// Fall (regelmäßiges An/Aus). new([[dauer, istAn], ...]) bleibt nötig, sobald die Abfolge NICHT
// strikt abwechselt (siehe Beispiel 1: zwei "aus"-Segmente in Folge für die längere
// Interchirp-Pause). pattern ist zwar <>-beschreibbar, play() liest pattern aber nur beim
// Start (siehe InsectSound>>play) — für ein neues Pattern also stop + neue Instanz + play,
// kein Hot-Swap auf derselben laufenden Instanz.
(
~insect = InsectSound.new(
	wingRate: 280, amp: 0.3,
	pattern: CallingPattern.fromDurations([0.4, 1.5, 0.02, 0.9, 0.02, 0.8, 0.02, 0.7]) // "kurz an, lang aus"
);
~insect.play(s);
~monitor = Synth(\monitorMono, [\in, ~insect.bus.index, \out, 0], target: ~insect.synth,
	addAction: \addAfter);
)

~insect.stop;
~monitor.free;

// --- Beispiel 4: fromDurations mit mehr als zwei Segmenten ---
(
~insect = InsectSound.new(
	pattern: CallingPattern.fromDurations([0.02, 0.03, 0.02, 0.2]) // an, aus, an, aus
);
~insect.play(s);
~monitor = Synth(\monitorMono, [\in, ~insect.bus.index, \out, 0], target: ~insect.synth,
	addAction: \addAfter);
)

~insect.stop;
~monitor.free;
