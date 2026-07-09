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
	var <spread;      // GVerb-Diffusion (Streuung der internen Verzögerungsleitungen) — kein
	                  // abgeleitetes Raum-Merkmal wie roomSize/revTime/damping, sondern ein
	                  // direkter Experimentier-Knopf gegen metallisches Klingeln (Intent 41)
	var <inputBandwidth;  // Tiefpass vor dem Diffusions-Netzwerk, 0..1 — hell/dunkel des in
	                      // den Hall eingespeisten Signals, kein Raum-Merkmal (Intent 41)
	var <tailBalance;     // 0 = nur frühe Reflexionen .. 1 = nur diffuser Nachhall-Schwanz,
	                      // 0.5 = ausbalanciert (bisheriges Verhalten) — verschiebt nur das
	                      // Verhältnis, mix bleibt der Gesamtpegel (Intent 41)
	var <binauralizerClass;            // aktuell konfigurierter Binauralizer-Typ, siehe
	                                    // binauralizerClass_ — nil bedeutet "kein binaurales
	                                    // Processing" (Intent 51), intern auf
	                                    // directOutBinauralizerClass abgebildet
	var <>directOutBinauralizerClass;  // Fallback-Klasse für binauralizerClass = nil
	var <>subjectID;                   // ATK-Kunstkopf-ID, nur für AtkBinauralizer relevant

	// listener optional: normalerweise legt Room seinen eigenen Listener an, aber ein
	// künftiges Multi-Room-Setup (ein Listener wandert zwischen mehreren Rooms, siehe
	// Intent 27) kann hier einen bestehenden übergeben — keine eigene Mechanik dafür heute,
	// nur der Konstruktor-Spielraum.
	*new { |server, listener, size = 8, height = 3, surface = 0.5, mix = 1, spread = 15,
			inputBandwidth = 0.5, tailBalance = 0.5, subjectID = 21|
		var aListener = listener ?? { Listener.new };
		^super.new.init(server, aListener, Orchestra.new(aListener), RoomReverb.new(server),
			size, height, surface, mix, spread, inputBandwidth, tailBalance, subjectID);
	}

	// Test-Konstruktor: orchestra/reverb kommen fertig (Fakes) rein, statt dass Room selbst
	// einen echten Server für RoomReverb.new anfasst — siehe TestRoom. listener ist ein
	// echter Listener (reine sclang-Logik, kein Server nötig), damit register() über
	// listener.makeBinauralizer testbar bleibt.
	*forTest { |orchestra, reverb, size = 8, height = 3, surface = 0.5, mix = 1, spread = 15,
			inputBandwidth = 0.5, tailBalance = 0.5, subjectID = 21|
		^super.new.init(nil, Listener.new, orchestra, reverb, size, height, surface, mix,
			spread, inputBandwidth, tailBalance, subjectID);
	}

	init { |aServer, aListener, anOrchestra, aReverb, aSize, aHeight, aSurface, aMix, aSpread,
			aInputBandwidth, aTailBalance, aSubjectID|
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
		subjectID = aSubjectID;
		// Default bleibt Binauralizer (Kompatibilität mit bisherigem Verhalten) — reine
		// Buchhaltung, löst noch KEIN addSynthDef/setup aus (siehe binauralizerClass_); der
		// Listener selbst defaultet ohnehin auf Binauralizer (siehe Listener>>setup).
		binauralizerClass = Binauralizer;
		directOutBinauralizerClass = DirectOutBinauralizer;
	}

	// zentrale Stelle für den Binauralizer-Typ dieses Room (Intent 51): lädt die passende
	// SynthDef bzw. initialisiert ATK, verkabelt mit dem geteilten Reverb-Bus, UND legt fest,
	// dass der Listener dieses Room ab jetzt mit dieser Klasse "hört" (siehe
	// Listener>>makeBinauralizer, register unten) — ein Room hat eine Ohren-Strategie, kein
	// Nebeneinander mehrerer Typen (Intent 27). Skripte kennen reverbBus/bus gar nicht mehr.
	// AtkBinauralizer erkennt sich per respondsTo(\setup) statt Klassenvergleich, damit
	// Test-Doubles ohne echte Vererbung dieselbe Weiche durchlaufen (siehe TestRoom).
	// nil wird NICHT als Fehlen behandelt, sondern auf directOutBinauralizerClass
	// abgebildet — SoundObject/Listener bleiben dadurch immer mit einer echten Klasse
	// unterwegs (siehe Klassenkommentar).
	binauralizerClass_ { |aBinauralizerClass|
		binauralizerClass = aBinauralizerClass;
		if(aBinauralizerClass.isNil) {
			if(directOutBinauralizerClass.respondsTo(\addSynthDef)) {
				directOutBinauralizerClass.addSynthDef(reverb.bus);
			};
			listener.binauralizerClass = directOutBinauralizerClass;
		} {
			if(aBinauralizerClass.respondsTo(\setup)) {
				aBinauralizerClass.setup(server, subjectID, reverb.bus);
			} {
				aBinauralizerClass.addSynthDef(reverb.bus);
			};
			listener.binauralizerClass = aBinauralizerClass;
		};
	}

	// Migrations-Shim für den bisherigen Einstieg — delegiert vollständig auf
	// binauralizerClass_ (siehe Challenges & Solutions).
	addSynthDef { |aBinauralizerClass, aSubjectID = 21|
		subjectID = aSubjectID;
		this.binauralizerClass = aBinauralizerClass;
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

	// finaler Ressourcenabbau für das Ende einer Session: stop/play nutzt weiter stop(),
	// teardown gibt zusätzlich den Reverb-Bus frei UND delegiert an binauralizerClass.teardown,
	// falls der konfigurierte Typ selbst geteilte Ressourcen freigibt (z.B. AtkBinauralizer/
	// dessen HRTF-Kernel) — respondsTo(\teardown) statt Klassenvergleich, siehe
	// binauralizerClass_.
	teardown {
		this.stop;
		reverb.free;
		if(binauralizerClass.notNil and: { binauralizerClass.respondsTo(\teardown) }) {
			binauralizerClass.teardown;
		};
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
