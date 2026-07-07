// RoomReverb — ein einziger, geteilter Raumklang für den ganzen virtuellen Raum, statt eines
// Halls pro Soundobjekt (akustisch richtig: ein Raum hat ein Nachklangfeld, nicht eines pro
// Quelle; ausserdem N-mal günstiger). SoundObjects speisen über Binauralizer/AtkBinauralizer
// (siehe dort, reverbBus/reverbMix) einen distanzabhängigen Anteil ihres trockenen Signals in
// den geteilten Bus — mehrere gleichzeitige Out.ar auf denselben Bus summieren sich
// automatisch, kein manuelles Mischen nötig. GVerb selbst ist Core-UGen (keine sc3-plugins).
RoomReverb {
	var <bus;     // geteilter Mono-Bus, in den die Binauralizer ihren Hall-Anteil schicken
	var <synth;

	// registriert die \roomReverb-SynthDef beim Server. drylevel=0 fest verdrahtet — der
	// Direktschall kommt schon über die Binauralizer, hier nur der Nachklang-Anteil.
	*addSynthDef {
		SynthDef(\roomReverb, { |in = 0, out = 0, roomSize = 8, revTime = 3, damping = 0.5,
				mix = 1|
			var sig = In.ar(in, 1);
			var wet = GVerb.ar(sig, roomSize, revTime, damping, 0.5, 15, 0, mix, mix,
				roomSize + 1);
			Out.ar(out, wet);
		}).add;
	}

	// legt nur den geteilten Bus an, ohne schon den Verarbeitungs-Synth zu starten — bewusst
	// getrennt von play(), damit Binauralizer/AtkBinauralizer ihren reverbBus (die Bus-Nummer
	// wird bei addSynthDef per Closure eingebacken) schon referenzieren können, bevor der
	// Reverb-Synth selbst im Node-Baum existiert.
	allocBus { |server|
		bus = Bus.audio(server, 1);
		^bus
	}

	// startet den Reverb-Synth. Bewusst zuletzt aufrufen, nachdem alle SoundObjects schon
	// laufen (addToTail) — der Synth liest dann erst den bereits vollständig summierten
	// Bus-Inhalt jedes Blocks. Bei später live dazu registrierten Objekten kann es dadurch
	// einen Block Verzögerung geben (akzeptierte Einschränkung dieser ersten Version).
	play { |server, roomSize = 8, revTime = 3, damping = 0.5, mix = 1|
		synth = Synth(\roomReverb, [
			\in, bus.index,
			\out, 0,
			\roomSize, roomSize,
			\revTime, revTime,
			\damping, damping,
			\mix, mix
		], server, \addToTail);
		^this
	}

	// gibt Synth und Bus wieder frei.
	stop {
		synth.free;
		bus.free;
	}
}
