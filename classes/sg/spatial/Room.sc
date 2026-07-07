// Room — kapselt die akustischen Eigenschaften eines virtuellen Raums (Größe, Deckenhöhe,
// Oberflächenbeschaffenheit) und leitet daraus ab, wie er klingt (aktuell: RoomReverb;
// updateAcoustics ist der Seam für künftige, noch zu definierende raumabhängige Effekte wie
// Echo/Slap-Delay — nicht Teil dieses Intents). Bündelt zusätzlich Listener + Orchestra, die
// bisher jedes Demo-Skript identisch von Hand verkabelte.
//
// Bewusst kein physikalisch exaktes Modell (keine Sabine-Formel o.ä.) — ein plausibles, per
// Ohr nachjustierbares Mapping in reverbParams. Rohe Reverb-Parameter sind deshalb nicht mehr
// von aussen erreichbar, nur size/height/surface/mix.
Room {
	var <listener;
	var <orchestra;
	var reverb;       // RoomReverb-Instanz — Implementierungsdetail, siehe reverbParams
	var <server;
	var <size;        // Grundfläche (Kantenlänge, m) — size = ... aktualisiert den Hall
	var <height;      // Deckenhöhe (m)
	var <surface;     // 0 (rau/absorbierend) .. 1 (glatt/hart) — siehe reverbParams
	var <mix;         // Hall-Gesamtpegel, kein Raum-Merkmal, reiner Mix-Knopf

	*new { |server, size = 8, height = 3, surface = 0.5, mix = 1|
		var aListener = Listener.new;
		^super.new.init(server, aListener, Orchestra.new(aListener), RoomReverb.new(server),
			size, height, surface, mix);
	}

	// Test-Konstruktor: orchestra/reverb kommen fertig (Fakes) rein, statt dass Room selbst
	// einen echten Server für RoomReverb.new anfasst — siehe TestRoom.
	*forTest { |orchestra, reverb, size = 8, height = 3, surface = 0.5, mix = 1|
		^super.new.init(nil, nil, orchestra, reverb, size, height, surface, mix);
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
	// Bus — Skripte kennen reverbBus/bus gar nicht mehr. Nur 2 bekannte Subtypen mit
	// unterschiedlicher Signatur (AtkBinauralizer lädt zusätzlich asynchron den HRTF-Kernel,
	// braucht den Server) — einfacher Klassenvergleich reicht.
	addSynthDef { |binauralizerClass, subjectID = 21|
		if(binauralizerClass === AtkBinauralizer) {
			AtkBinauralizer.setup(server, subjectID, reverb.bus);
		} {
			binauralizerClass.addSynthDef(reverb.bus);
		};
	}

	register { |soundObject| orchestra.register(soundObject) }

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
