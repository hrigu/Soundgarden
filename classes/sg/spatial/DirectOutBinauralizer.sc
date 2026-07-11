// DirectOutBinauralizer — der "kein Binauralizer"-Fall (Room>>binauralizerClass = nil, siehe
// Intent 51): gibt den Mono-Eingang unbearbeitet auf beide Kanäle aus, ohne Pan/ITD/Distanz-
// Verarbeitung wie Binauralizer/AtkBinauralizer. Implementiert dasselbe gemeinsame
// Binauralizer-Interface (play/set/stop, siehe Binauralizer.sc für die vollständige
// Beschreibung), damit SoundObject ihn austauschbar verwenden kann, ohne selbst zu wissen,
// dass gar keine Binauralisierung passiert. set() ist bewusst ein No-op — Azimuth/Distanz
// haben hier keine hörbare Wirkung.
DirectOutBinauralizer {
	var <>reverbMix;  // 0..1 — wie stark dieses Objekt insgesamt in RoomReverb einspeist
	var <synth;       // laufender Synth, sobald play() aufgerufen wurde

	*new { |reverbMix = 0.2|
		^super.new.init(reverbMix);
	}

	init { |aReverbMix|
		reverbMix = aReverbMix;
	}

	// registriert die \directOutBinauralizer-SynthDef beim Server — reine Durchleitung, kein
	// Pan/ITD/Tiefpass. reverbBus siehe Binauralizer>>addSynthDef (gleiches Muster).
	*addSynthDef { |reverbBus|
		SynthDef(\directOutBinauralizer, { |in = 0, out = 0, reverbMix = 0.2|
			var sig = In.ar(in, 1);
			Out.ar(out, sig.dup);
			if(reverbBus.notNil) {
				Out.ar(reverbBus, sig * reverbMix);
			};
		}).add;
	}

	// target/addAction wie bei Binauralizer — muss NACH der Klangquelle laufen.
	play { |server, inBus, outBus = 0, target, addAction = \addToTail|
		synth = Synth.new(\directOutBinauralizer, [
			\in, inBus.index,
			\out, outBus,
			\reverbMix, reverbMix
		], target ? server, addAction);
		^this
	}

	// No-op: ohne binaurale Nachbearbeitung haben Azimuth/Distanz keine Wirkung.
	set { |azimuth, distance| }

	stop {
		synth.free;
	}
}
