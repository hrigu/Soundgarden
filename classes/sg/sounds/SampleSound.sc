// SampleSound — spielt statt eines synthetisierten Klangs (wie InsectSound) ein
// Audiosample rhythmisch ab: Impulse.kr(rate, phase) triggert PlayBuf.ar, das das
// Sample bei jedem Trigger von vorne startet (kein Loop, kein Überlappen-Management).
// Lädt die Datei als Mono-Buffer (Buffer.readChannel mit channels: [0] — auch bei stereo
// Quelldateien nur der erste Kanal), passend zum Mono-Bus-Vertrag von Sound.
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
			// rate/amp geglättet (Lag.kr) fürs klickfreie Live-Tunen im GUI (Intent 43,
			// gleiches Muster wie InsectSound/RoomReverb) -- phase bleibt ungeglättet, reiner
			// Zeitversatz des Triggers ohne die Klick-Problematik von Frequenz-/Pegelsprüngen.
			var rateCtrl = Lag.kr(rate, 0.1);
			var ampCtrl = Lag.kr(amp, 0.1);
			var trig = Impulse.kr(rateCtrl, phase);
			var sig = PlayBuf.ar(1, buf, BufRateScale.kr(buf), trig, 0, doneAction: 0);
			Out.ar(out, sig * ampCtrl);
		}).add;
	}

	// editableParams — live im GUI bearbeitbare Klangparameter (SpatialControlPanel,
	// Intent 43).
	*editableParams {
		^[
			[\rate, ControlSpec(0.2, 8, \exp)],
			[\phase, ControlSpec(0, 1, \lin)],
			[\amp, ControlSpec(0, 1, \lin)]
		]
	}

	// lädt den Buffer vorab (asynchron) — optional, aber empfohlen: ohne preload lädt
	// makeSynth den Buffer erst bei play(), dann kann der allererste Trigger auf einen
	// noch leeren Buffer treffen ("Buffer UGen: no buffer data", harmlos, aber unschön).
	// Mit etwas zeitlichem Abstand zu play() aufrufen, wie InsectSound/Binauralizer
	// addSynthDef — z.B. schon in Block 1 des Demo-Skripts, während Block 2 noch nicht
	// lief.
	preload { |server|
		^buffer = Buffer.readChannel(server, path, channels: [0]);
	}

	// erzeugt den Synth; lädt den Buffer selbst nach, falls preload nicht vorher
	// aufgerufen wurde. bufnum steht so oder so sofort fest — PlayBuf braucht nur die
	// Nummer, keine Buffer-Metadaten zum SynthDef-Bauzeitpunkt wie z.B. FoaDecoderKernel
	// (siehe AtkBinauralizer).
	makeSynth { |server, bus|
		buffer = buffer ?? { Buffer.readChannel(server, path, channels: [0]) };
		^Synth(\sampleSound, [
			\out, bus.index,
			\buf, buffer.bufnum,
			\rate, rate,
			\phase, phase,
			\amp, amp
		], server);
	}

	// gibt zusätzlich zu Synth/Bus (Sound>>stop) auch den geladenen Buffer frei.
	// buffer danach auf nil setzen, sonst würde makeSynth beim nächsten play die
	// tote Buffer-Nummer weiterverwenden (buffer ?? lädt nur bei nil nach) — Stille.
	stop {
		super.stop;
		buffer !? { buffer.free };
		buffer = nil;
	}
}
