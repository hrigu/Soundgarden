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

	*new { |movable, sound, binauralizer|
		^super.new.init(movable, sound, binauralizer);
	}

	init { |aMovable, aSound, aBinauralizer|
		movable = aMovable;
		sound = aSound ? InsectSound.new;
		binauralizer = aBinauralizer ? Binauralizer.new;
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

	stop {
		binauralizer.stop;
		sound.stop;
	}
}
