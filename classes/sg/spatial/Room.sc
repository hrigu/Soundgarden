// Room — kapselt die akustischen Eigenschaften eines virtuellen Raums (Größe, Deckenhöhe,
// Oberflächenbeschaffenheit) und leitet daraus ab, wie er klingt (aktuell: RoomReverb;
// updateAcoustics ist der Seam für künftige, noch zu definierende raumabhängige Effekte wie
// Echo/Slap-Delay — nicht Teil dieses Intents). Bündelt zusätzlich Listener + Orchestra, die
// bisher jedes Demo-Skript identisch von Hand verkabelte.
//
// Bewusst kein physikalisch exaktes Modell (keine Sabine-Formel o.ä.) — ein plausibles, per
// Ohr nachjustierbares Mapping in reverbParams. Rohe Reverb-Parameter sind deshalb nicht mehr
// von aussen erreichbar, nur size/height/surface/mix.
//
// register() baut auch den Binauralizer für jedes SoundObject — über den Listener (siehe
// Listener>>makeBinauralizer), nicht durch das aufrufende Skript. Binauralisierung ist eine
// Eigenschaft der Ohren des Hörers, nicht der Klangquelle (Intent 27).
Room {
	var <listener;
	var <orchestra;
	var reverb;       // RoomReverb-Instanz — Implementierungsdetail, siehe reverbParams
	var <server;
	var <size;        // Grundfläche (Kantenlänge, m) — size = ... aktualisiert den Hall
	var <height;      // Deckenhöhe (m)
	var <surface;     // 0 (rau/absorbierend) .. 1 (glatt/hart) — siehe reverbParams
	var <mix;         // Hall-Gesamtpegel, kein Raum-Merkmal, reiner Mix-Knopf

	// listener optional: normalerweise legt Room seinen eigenen Listener an, aber ein
	// künftiges Multi-Room-Setup (ein Listener wandert zwischen mehreren Rooms, siehe
	// Intent 27) kann hier einen bestehenden übergeben — keine eigene Mechanik dafür heute,
	// nur der Konstruktor-Spielraum.
	*new { |server, listener, size = 8, height = 3, surface = 0.5, mix = 1|
		var aListener = listener ?? { Listener.new };
		^super.new.init(server, aListener, Orchestra.new(aListener), RoomReverb.new(server),
			size, height, surface, mix);
	}

	// Test-Konstruktor: orchestra/reverb kommen fertig (Fakes) rein, statt dass Room selbst
	// einen echten Server für RoomReverb.new anfasst — siehe TestRoom. listener ist ein
	// echter Listener (reine sclang-Logik, kein Server nötig), damit register() über
	// listener.makeBinauralizer testbar bleibt.
	*forTest { |orchestra, reverb, size = 8, height = 3, surface = 0.5, mix = 1|
		^super.new.init(nil, Listener.new, orchestra, reverb, size, height, surface, mix);
	}

	init { |aServer, aListener, anOrchestra, aReverb, aSize, aHeight, aSurface, aMix|
		server = aServer;
		listener = aListener;
		orchestra = anOrchestra;
		reverb = aReverb;
		size = aSize;
		height = aHeight;
		surface = aSurface;
		mix = aMix;
	}

	// registriert die SynthDef eines Binauralizer-Typs, verkabelt mit dem geteilten Reverb-
	// Bus, UND legt fest, dass der Listener dieses Room ab jetzt mit dieser Klasse "hört"
	// (siehe Listener>>makeBinauralizer, register unten) — ein Room hat eine Ohren-Strategie,
	// kein Nebeneinander mehrerer Typen (Intent 27). Skripte kennen reverbBus/bus gar nicht
	// mehr. Nur 2 bekannte Subtypen mit unterschiedlicher Signatur (AtkBinauralizer lädt
	// zusätzlich asynchron den HRTF-Kernel, braucht den Server) — einfacher Klassenvergleich
	// reicht.
	addSynthDef { |binauralizerClass, subjectID = 21|
		if(binauralizerClass === AtkBinauralizer) {
			AtkBinauralizer.setup(server, subjectID, reverb.bus);
		} {
			binauralizerClass.addSynthDef(reverb.bus);
		};
		listener.binauralizerClass = binauralizerClass;
	}

	// baut ein SoundObject aus movable+sound, mit einem Binauralizer passend zu den "Ohren"
	// des eigenen Listeners (siehe Listener>>makeBinauralizer) — Skripte kennen Binauralizer/
	// AtkBinauralizer ab hier nicht mehr direkt (Intent 27).
	register { |movable, sound, reverbMix = 0.3|
		var binauralizer = listener.makeBinauralizer(reverbMix);
		^this.finishRegistering(SoundObject.new(movable, sound, binauralizer))
	}

	// wie register, baut movable+sound aber über SoundObjectBuilder aus zwei Params-Events
	// statt fertiger Instanzen — für Varianten, die per Event-Merge (baseParams ++ (...))
	// aus einer Basis-Konfiguration abgeleitet werden (Intent 29). SoundObjectBuilder kennt
	// den Binauralizer nicht selbst (Intent 27) — der kommt wie bei register aus
	// listener.makeBinauralizer.
	registerFromBuilder { |soundParams, movableParams, reverbMix = 0.3, soundClass, movableClass|
		var binauralizer = listener.makeBinauralizer(reverbMix);
		^this.finishRegistering(
			SoundObjectBuilder.build(soundParams, movableParams, binauralizer, soundClass,
				movableClass));
	}

	// gemeinsamer Abschluss von register/registerFromBuilder: bei Orchestra anmelden, damit es
	// pro Tick bewegt/räumlich aktualisiert wird.
	finishRegistering { |soundObject|
		orchestra.register(soundObject);
		^soundObject
	}

	call { |caller| orchestra.call(caller) }

	// startet Orchestra vor RoomReverb (siehe RoomReverb>>play: addToTail braucht schon
	// laufende SoundObjects), dann den Hall mit den aktuell abgeleiteten Parametern.
	play {
		orchestra.play(server);
		reverb.play(server, *this.reverbParams);
		^this
	}

	stop {
		orchestra.stop;
		reverb.stop;
	}

	// size/height/surface/mix per Zuweisung änderbar (~room.size = 12;) — jede Änderung
	// schreibt die neu abgeleiteten Parameter sofort auf den laufenden Hall.
	size_ { |aSize| size = aSize; this.updateAcoustics; }
	height_ { |aHeight| height = aHeight; this.updateAcoustics; }
	surface_ { |aSurface| surface = aSurface; this.updateAcoustics; }
	mix_ { |aMix| mix = aMix; this.updateAcoustics; }

	// leitet aus size/height/surface die RoomReverb-Parameter ab — einziger Ort für dieses
	// Mapping, damit play() und die Live-Setter (size_ etc.) immer konsistent bleiben.
	// Faustregel, keine akustische Simulation: volume = size² × height (Raum als Würfel mit
	// Kantenlänge size gedacht); revTime wächst mit der Kubikwurzel des Volumens und mit
	// glatterer Oberfläche (surface näher 1 → länger, heller); damping fällt mit surface
	// (glatt/hart dämpft hohe Frequenzen weniger als rau/absorbierend).
	reverbParams {
		var volume = size.squared * height;
		var revTime = (volume.pow(1/3) * 0.3 * (0.5 + surface)).clip(0.3, 15);
		var damping = (1 - surface).clip(0.05, 0.95);
		^[size, revTime, damping, mix]
	}

	// schreibt die aktuell abgeleiteten Parameter live auf den laufenden Hall (no-op vor dem
	// ersten play, siehe RoomReverb>>set).
	updateAcoustics {
		reverb.set(*this.reverbParams);
	}
}
