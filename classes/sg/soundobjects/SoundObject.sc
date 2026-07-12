// SoundObject — die Klasse, die das konkrete Klangobjekt im Raum beschreibt. Sie
// verbindet ein Movable (das sich nach einer MoveRule bewegt) mit einem Sound (reiner
// Klang) und einem Binauralizer (formt den Klang für den Listener anhand Azimuth/
// Distanz). Kennt den Listener selbst nicht — das Ticken (Bewegung, Azimuth/Distanz-
// Berechnung) übernimmt eine Orchestra-Instanz, bei der sich das SoundObject
// registriert; sie ruft dafür pos/step/updateSpatial auf.
SoundObject {
	var <>movable;
	var <>sound;
	var <>binauralizer;
	var hasStopped; // Guard gegen doppeltes Stoppen (siehe stop) -- Bug-Report zu Intent 59:
	                // in oeuvre/messiaen_birds/messiaen_birds.scd kann die individuelle
	                // Lebensdauer-Routine eines Vogels UND der Stück-Ende-Cue beide
	                // fadeOutAndStop auf demselben SoundObject auslösen (die Lebensdauer-Routine
	                // läuft unabhängig von der Timeline weiter), was ohne Guard zu einem
	                // zweiten n_free auf einen bereits freigegebenen Node führte.

	*new { |movable, sound, binauralizer|
		^super.new.init(movable, sound, binauralizer);
	}

	init { |aMovable, aSound, aBinauralizer|
		movable = aMovable;
		sound = aSound ? InsectSound.new;
		binauralizer = aBinauralizer ? Binauralizer.new;
		hasStopped = false;
	}

	// startet Klang- und Binauralizer-Synth. Das Ticken (Bewegung, Azimuth/Distanz)
	// läuft nicht mehr hier, sondern über Orchestra.
	play { |server|
		sound.play(server);
		// addAfter: Binauralizer muss im Node-Baum nach dem Sound laufen,
		// um dessen Bus im selben Audio-Block lesen zu können.
		binauralizer.play(server, sound.bus, outBus: 0, target: sound.synth, addAction: \addAfter);
		^this
	}

	// aktuelle Position — von Orchestra pro Tick abgefragt.
	pos {
		^movable.pos
	}

	// bewegt das Objekt einen Zeitschritt weiter — von Orchestra pro Tick aufgerufen.
	step { |dt|
		^movable.step(dt)
	}

	// nimmt die von Orchestra berechnete Azimuth/Distanz zum Listener entgegen und
	// schiebt sie in den Binauralizer.
	updateSpatial { |azimuth, distance|
		binauralizer.set(azimuth, distance);
	}

	// löst einen Zuruf aus (Call-and-Response, siehe Orchestra>>call) — delegiert
	// an den eigenen Sound.
	call {
		sound.call;
	}

	// idempotent (siehe hasStopped oben): ein zweiter Aufruf (z.B. weil zwei unabhängige
	// Routinen beide fadeOutAndStop auf demselben Objekt auslösen) ist ein stiller No-op statt
	// binauralizer/sound ein zweites Mal auf einen bereits freigegebenen Synth freizugeben.
	stop {
		if(hasStopped) { ^this };
		hasStopped = true;
		binauralizer.stop;
		sound.stop;
	}

	// fadeOutAndStop — klickfreies Stoppen (Intent 58): sound.amp erst auf 0 fahren (Lag.kr in
	// den SynthDefs glättet den Sprung), fadeTime abwarten (muss mindestens die Lag-Zeit der
	// jeweiligen Sound-Subklasse decken, siehe BirdSound/InsectSound/CricketSound), erst danach
	// stop -- ein sofortiges stop würde den Synth per n_free abrupt kappen und klickt.
	// Extrahiert aus oeuvre/dawn/dawn_chorus.scd, wo dasselbe Muster mehrfach dupliziert war.
	// onComplete läuft nach dem stop (z.B. für orchestra.unregister/Listen-Cleanup durch den
	// Aufrufer) -- SoundObject kennt die Orchestra selbst nicht (siehe Klassenkommentar oben).
	fadeOutAndStop { |fadeTime = 0.5, onComplete|
		sound.setParam(\amp, 0);
		Routine({
			fadeTime.wait;
			this.stop;
			onComplete.value(this);
		}).play;
	}
}
