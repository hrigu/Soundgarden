// RoomReverb — ein einziger, geteilter Raumklang für den ganzen virtuellen Raum, statt eines
// Halls pro Soundobjekt (akustisch richtig: ein Raum hat ein Nachklangfeld, nicht eines pro
// Quelle; ausserdem N-mal günstiger). SoundObjects speisen über Binauralizer/AtkBinauralizer
// (siehe dort, reverbBus/reverbMix) einen distanzabhängigen Anteil ihres trockenen Signals in
// den geteilten Bus — mehrere gleichzeitige Out.ar auf denselben Bus summieren sich
// automatisch, kein manuelles Mischen nötig. GVerb selbst ist Core-UGen (keine sc3-plugins).
RoomReverb {
	var server;
	var <bus;     // geteilter Mono-Bus, in den die Binauralizer ihren Hall-Anteil schicken
	var <synth;

	// legt den geteilten Bus schon hier an, nicht in einem separaten Schritt — Bus.audio ist
	// rein client-seitige Buchhaltung (keine Server-Kommunikation, kein server.sync nötig),
	// es gibt also keinen Grund, das dem Aufrufer zu überlassen (vorher: allocBus — vergessen
	// führte still zu bus == nil, kein Fehler, einfach kein Hall).
	*new { |server|
		^super.new.init(server);
	}

	init { |aServer|
		server = aServer;
		bus = Bus.audio(server, 1);
	}

	// registriert die \roomReverb-SynthDef beim Server. drylevel=0 fest verdrahtet — der
	// Direktschall kommt schon über die Binauralizer, hier nur der Nachklang-Anteil.
	*addSynthDef {
		SynthDef(\roomReverb, { |in = 0, out = 0, roomSize = 8, revTime = 3, damping = 0.5,
				mix = 1, spread = 15|
			var sig = In.ar(in, 1);
			var roomSizeCtrl = Lag.kr(roomSize, 0.25);
			var revTimeCtrl = Lag.kr(revTime, 0.25);
			var dampingCtrl = Lag.kr(damping, 0.2);
			var mixCtrl = Lag.kr(mix, 0.12);
			// spread: Streuung/Diffusion der internen Verzoegerungsleitungen (siehe
			// GVerb.schelp) -- vorher Literal (GVerbs eigener Default), jetzt experimentierbar
			// (Intent 41), da zu wenig Diffusion die klassische Ursache fuer hoerbares
			// Kammfilter-/"metallisches" Klingeln ist.
			var spreadCtrl = Lag.kr(spread, 0.25);
			// GVerb reagiert auf harte Parameterspruenge oft mit kurzen Artefakten.
			// Darum die Room-Controls vor dem UGen leicht glaetten.
			var wet = GVerb.ar(sig, roomSizeCtrl, revTimeCtrl, dampingCtrl, 0.5, spreadCtrl, 0,
				mixCtrl, mixCtrl, roomSizeCtrl + 1);
			Out.ar(out, wet);
		}).add;
	}

	// startet den Reverb-Synth. Bewusst zuletzt aufrufen, nachdem alle SoundObjects schon
	// laufen (addToTail) — der Synth liest dann erst den bereits vollständig summierten
	// Bus-Inhalt jedes Blocks. Bei später live dazu registrierten Objekten kann es dadurch
	// einen Block Verzögerung geben (akzeptierte Einschränkung dieser ersten Version).
	play { |server, roomSize = 8, revTime = 3, damping = 0.5, mix = 1, spread = 15|
		synth = Synth(\roomReverb, [
			\in, bus.index,
			\out, 0,
			\roomSize, roomSize,
			\revTime, revTime,
			\damping, damping,
			\mix, mix,
			\spread, spread
		], server, \addToTail);
		^this
	}

	// aktualisiert die Parameter auf dem laufenden Synth live (no-op vor dem ersten play,
	// gleiches Muster wie Binauralizer>>set).
	set { |roomSize, revTime, damping, mix, spread|
		synth !? {
			synth.set(\roomSize, roomSize, \revTime, revTime, \damping, damping, \mix, mix,
				\spread, spread);
		};
	}

	// stop ist restart-sicher: beendet nur den laufenden Reverb-Synth, der geteilte Bus
	// bleibt erhalten, damit Room.play denselben Room später erneut starten kann.
	stop {
		synth !? { synth.free };
		synth = nil;
	}

	// finaler Ressourcenabbau: zusätzlich zum laufenden Synth auch den geteilten Bus
	// freigeben. Für echte Session-Enden gedacht, nicht für stop/play-Zyklen.
	free {
		this.stop;
		bus !? { bus.free };
		bus = nil;
	}
}
