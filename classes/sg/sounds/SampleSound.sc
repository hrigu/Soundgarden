// SampleSound — spielt statt eines synthetisierten Klangs (wie InsectSound) ein
// Audiosample rhythmisch ab: Impulse.kr(rate, phase) triggert PlayBuf.ar, das das
// Sample bei jedem Trigger von vorne startet (kein Loop, kein Überlappen-Management).
// Lädt die Datei als Mono-Buffer (channels: [0] — auch bei stereo Quelldateien nur der
// erste Kanal), passend zum Mono-Bus-Vertrag von Sound.
SampleSound : Sound {
	var <path;      // Pfad zur Audiodatei
	var <>rate;     // Trigger-Rate in Hz — wie oft das Sample pro Sekunde neu startet
	var <>phase;    // Phasenversatz des Triggers, 0..1 (Anteil einer Periode)
	var <>amp;      // Grundlautstärke des Klangs selbst, unabhängig von Position
	var <buffer;    // geladener Mono-Buffer, ab play() gesetzt

	// erzeugt eine Instanz mit gegebener Audiodatei und (oder Default-)Klangparametern
	*new { |path, rate = 2, phase = 0, amp = 0.5|
		^super.new.init(path, rate, phase, amp);
	}

	init { |aPath, aRate, aPhase, aAmp|
		path = aPath;
		rate = aRate;
		phase = aPhase;
		amp = aAmp;
	}

	// registriert die \sampleSound-SynthDef beim Server. buf wird als Instanz-Argument
	// gesetzt (siehe makeSynth) — eine SynthDef für alle SampleSound-Instanzen, jede mit
	// ihrem eigenen Buffer. Asynchron (/d_recv) — vor play() mit etwas zeitlichem
	// Abstand aufrufen, wie InsectSound/Binauralizer.
	*addSynthDef {
		SynthDef(\sampleSound, { |out = 0, buf = 0, rate = 2, phase = 0, amp = 0.5|
			var trig = Impulse.kr(rate, phase);
			var sig = PlayBuf.ar(1, buf, BufRateScale.kr(buf), trig, 0, doneAction: 0);
			Out.ar(out, sig * amp);
		}).add;
	}

	// lädt den Buffer (asynchron, aber bufnum steht sofort fest — PlayBuf braucht nur
	// die Nummer, keine Buffer-Metadaten zum SynthDef-Bauzeitpunkt wie z.B.
	// FoaDecoderKernel, siehe AtkBinauralizer) und erzeugt den Synth darauf.
	makeSynth { |server, bus|
		buffer = Buffer.read(server, path, channels: [0]);
		^Synth(\sampleSound, [
			\out, bus.index,
			\buf, buffer.bufnum,
			\rate, rate,
			\phase, phase,
			\amp, amp
		], server);
	}

	// gibt zusätzlich zu Synth/Bus (Sound>>stop) auch den geladenen Buffer frei.
	stop {
		super.stop;
		buffer !? { buffer.free };
	}
}
