// Room — kapselt die akustischen Eigenschaften eines virtuellen Raums (Größe, Deckenhöhe,
// Oberflächenbeschaffenheit) und leitet daraus ab, wie er klingt (aktuell: RoomReverb;
// updateAcoustics ist der Seam für künftige, noch zu definierende raumabhängige Effekte wie
// Echo/Slap-Delay — nicht Teil dieses Intents). Bündelt zusätzlich Orchestra, die bisher jedes
// Demo-Skript identisch von Hand verkabelte.
//
// Bewusst kein physikalisch exaktes Modell (keine Sabine-Formel o.ä.) — ein plausibles, per
// Ohr nachjustierbares Mapping in reverbParams. Rohe Reverb-Parameter sind deshalb nicht mehr
// von aussen erreichbar, nur size/height/surface/mix.
//
// register() baut auch den Binauralizer für jedes SoundObject — über den Listener (siehe
// Listener>>makeBinauralizer), nicht durch das aufrufende Skript. Binauralisierung ist eine
// Eigenschaft der Ohren des Hörers, nicht der Klangquelle (Intent 27) — Room kennt seit
// Intent 52 keine Binauralizer-Details mehr (weder Konfiguration noch Setup/Teardown), nur
// noch den Hall. Ein Listener wird deshalb typischerweise VOR dem Room extern konfiguriert
// und instanziiert, dann dem Room übergeben (Room.new(s, listener: ~listener)) — Room
// verkabelt ihn bei der Konstruktion einmalig mit seinem eigenen Reverb-Bus (siehe
// Listener>>setup) und delegiert bei teardown symmetrisch an listener.teardown.
Room {
	var <listener;
	var <orchestra;
	var reverb;       // RoomReverb-Instanz — Implementierungsdetail, siehe reverbParams
	var <server;
	var <size;        // Grundfläche (Kantenlänge, m) — size = ... aktualisiert den Hall
	var <height;      // Deckenhöhe (m)
	var <surface;     // 0 (rau/absorbierend) .. 1 (glatt/hart) — siehe reverbParams
	var <mix;         // Hall-Gesamtpegel, kein Raum-Merkmal, reiner Mix-Knopf
	var <spread;      // GVerb-Diffusion (Streuung der internen Verzögerungsleitungen) — kein
	                  // abgeleitetes Raum-Merkmal wie roomSize/revTime/damping, sondern ein
	                  // direkter Experimentier-Knopf gegen metallisches Klingeln (Intent 41)
	var <inputBandwidth;  // Tiefpass vor dem Diffusions-Netzwerk, 0..1 — hell/dunkel des in
	                      // den Hall eingespeisten Signals, kein Raum-Merkmal (Intent 41)
	var <tailBalance;     // 0 = nur frühe Reflexionen .. 1 = nur diffuser Nachhall-Schwanz,
	                      // 0.5 = ausbalanciert (bisheriges Verhalten) — verschiebt nur das
	                      // Verhältnis, mix bleibt der Gesamtpegel (Intent 41)

	// listener optional: typischerweise extern konfiguriert/instanziiert und hier übergeben
	// (siehe Klassenkommentar); ohne Angabe legt Room einen Default-Listener an. Ein künftiges
	// Multi-Room-Setup (ein Listener wandert zwischen mehreren Rooms, siehe Intent 27) baut auf
	// listener.setup/teardown auf — keine eigene Mechanik dafür heute.
	*new { |server, listener, size = 8, height = 3, surface = 0.5, mix = 0.25, spread = 15,
			inputBandwidth = 0.5, tailBalance = 0.5|
		var aListener = listener ?? { Listener.new };
		// RoomReverb ist ein Interna des Room (siehe Klassenkommentar) — Room registriert
		// dessen SynthDef deshalb selbst, statt das jedem Demo-Skript zu überlassen.
		RoomReverb.addSynthDef;
		^super.new.init(server, aListener, Orchestra.new(aListener), RoomReverb.new(server),
			size, height, surface, mix, spread, inputBandwidth, tailBalance);
	}

	// Test-Konstruktor: orchestra/reverb/listener kommen fertig (Fakes) rein, statt dass Room
	// selbst einen echten Server für RoomReverb.new anfasst — siehe TestRoom. listener MUSS
	// hier immer explizit übergeben werden (wie orchestra/reverb): ein echter Listener.new()
	// würde bei der Konstruktion (siehe init unten) mit der echten Binauralizer-Klasse und
	// einem Fake-Bus-Symbol einen Server-seitigen Fehler werfen (siehe Intent 52, Challenges &
	// Solutions zu Task 1.0) — Tests verwenden stattdessen FakeListenerForRoomTest.
	*forTest { |orchestra, reverb, listener, size = 8, height = 3, surface = 0.5, mix = 1,
			spread = 15, inputBandwidth = 0.5, tailBalance = 0.5|
		^super.new.init(nil, listener, orchestra, reverb, size, height, surface, mix,
			spread, inputBandwidth, tailBalance);
	}

	init { |aServer, aListener, anOrchestra, aReverb, aSize, aHeight, aSurface, aMix, aSpread,
			aInputBandwidth, aTailBalance|
		server = aServer;
		listener = aListener;
		orchestra = anOrchestra;
		reverb = aReverb;
		size = aSize;
		height = aHeight;
		surface = aSurface;
		mix = aMix;
		spread = aSpread;
		inputBandwidth = aInputBandwidth;
		tailBalance = aTailBalance;
		// verkabelt den Listener einmalig mit server/reverb.bus (Intent 52) — lädt dabei
		// sofort dessen aktuell konfigurierte binauralizerClass (siehe Listener>>setup).
		listener.setup(server, reverb.bus);
	}

	// baut ein SoundObject aus movable+sound, mit einem Binauralizer passend zu den "Ohren"
	// des eigenen Listeners (siehe Listener>>makeBinauralizer) — Skripte kennen Binauralizer/
	// AtkBinauralizer ab hier nicht mehr direkt (Intent 27).
	register { |movable, sound, reverbMix = 0.2|
		var binauralizer = listener.makeBinauralizer(reverbMix);
		^this.finishRegistering(SoundObject.new(movable, sound, binauralizer))
	}

	// wie register, baut movable+sound aber über SoundObjectBuilder aus zwei Params-Events
	// statt fertiger Instanzen — für Varianten, die per Event-Merge (baseParams ++ (...))
	// aus einer Basis-Konfiguration abgeleitet werden (Intent 29). SoundObjectBuilder kennt
	// den Binauralizer nicht selbst (Intent 27) — der kommt wie bei register aus
	// listener.makeBinauralizer.
	registerFromBuilder { |soundParams, movableParams, reverbMix = 0.2, soundClass, movableClass|
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

	// finaler Ressourcenabbau für das Ende einer Session: stop/play nutzt weiter stop(),
	// teardown gibt zusätzlich den Reverb-Bus frei UND delegiert symmetrisch zu init()
	// (siehe dort) an listener.teardown — Room kennt Binauralizer-Details dafür nicht mehr
	// (Intent 52), der Listener entscheidet selbst, ob er z.B. einen HRTF-Kernel freigibt.
	teardown {
		this.stop;
		reverb.free;
		listener.teardown;
	}

	// size/height/surface/mix/spread/inputBandwidth/tailBalance per Zuweisung änderbar
	// (~room.size = 12;) — jede Änderung schreibt die neu abgeleiteten Parameter sofort auf
	// den laufenden Hall.
	size_ { |aSize| size = aSize; this.updateAcoustics; }
	height_ { |aHeight| height = aHeight; this.updateAcoustics; }
	surface_ { |aSurface| surface = aSurface; this.updateAcoustics; }
	mix_ { |aMix| mix = aMix; this.updateAcoustics; }
	spread_ { |aSpread| spread = aSpread; this.updateAcoustics; }
	inputBandwidth_ { |aInputBandwidth| inputBandwidth = aInputBandwidth; this.updateAcoustics; }
	tailBalance_ { |aTailBalance| tailBalance = aTailBalance; this.updateAcoustics; }

	// leitet aus size/height/surface die RoomReverb-Parameter ab — einziger Ort für dieses
	// Mapping, damit play() und die Live-Setter (size_ etc.) immer konsistent bleiben.
	// Faustregel, keine akustische Simulation: volume = size² × height (Raum als Würfel mit
	// Kantenlänge size gedacht); revTime wächst mit der Kubikwurzel des Volumens und mit
	// glatterer Oberfläche (surface näher 1 → länger, heller); damping fällt mit surface
	// (glatt/hart dämpft hohe Frequenzen weniger als rau/absorbierend). spread/mix/
	// inputBandwidth/tailBalance fliessen unverändert durch — keine abgeleiteten
	// Raum-Merkmale, siehe Instanzvariablen oben.
	reverbParams {
		var volume = size.squared * height;
		var revTime = (volume.pow(1/3) * 0.3 * (0.5 + surface)).clip(0.3, 15);
		var damping = (1 - surface).clip(0.05, 0.95);
		^[size, revTime, damping, mix, spread, inputBandwidth, tailBalance]
	}

	// schreibt die aktuell abgeleiteten Parameter live auf den laufenden Hall (no-op vor dem
	// ersten play, siehe RoomReverb>>set).
	updateAcoustics {
		reverb.set(*this.reverbParams);
	}
}
