// SoundObject — die Klasse, die das konkrete Klangobjekt im Raum beschreibt. Sie
// verbindet ein Movable (das sich nach einer MoveRule bewegt) mit einem Sound (reiner
// Klang) und einem Binauralizer (formt den Klang für den Listener anhand Azimuth/
// Distanz). Kennt den Listener selbst nicht — das Ticken (Bewegung, Azimuth/Distanz-
// Berechnung) übernimmt eine Orchestra-Instanz, bei der sich das SoundObject
// registriert; sie ruft dafür pos/step/updateSpatial auf.
SoundObject {
	var <>movable;
	var <>insectSound;
	var <>binauralizer;

	*new { |movable, insectSound, binauralizer|
		^super.new.init(movable, insectSound, binauralizer);
	}

	init { |aMovable, aInsectSound, aBinauralizer|
		movable = aMovable;
		insectSound = aInsectSound ? InsectSound.new;
		binauralizer = aBinauralizer ? Binauralizer.new;
	}

	// startet Klang- und Binauralizer-Synth. Das Ticken (Bewegung, Azimuth/Distanz)
	// läuft nicht mehr hier, sondern über Orchestra.
	play { |server|
		insectSound.play(server);
		// addAfter: Binauralizer muss im Node-Baum nach InsectSound laufen,
		// um dessen Bus im selben Audio-Block lesen zu können.
		binauralizer.play(server, insectSound.bus, 0, insectSound.synth, \addAfter);
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

	stop {
		binauralizer.stop;
		insectSound.stop;
	}
}
