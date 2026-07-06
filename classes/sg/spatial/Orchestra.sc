// Orchestra — zentraler Taktgeber für alle SoundObjects. Hält den (einen) Listener
// und eine Registry registrierter SoundObjects; tickt sie über eine einzige Routine
// statt dass jedes SoundObject seine eigene Routine mitbringt. Ein SoundObject muss
// dafür nur pos, step(dt) und updateSpatial(azimuth, distance) implementieren — es
// muss den Listener selbst nicht kennen.
Orchestra {
	var <listener;
	var <soundObjects;
	var routine;

	*new { |listener|
		^super.new.init(listener);
	}

	init { |aListener|
		listener = aListener;
		soundObjects = [];
	}

	register { |soundObject|
		soundObjects = soundObjects.add(soundObject);
	}

	unregister { |soundObject|
		soundObjects.remove(soundObject);
	}

	// tickt einmal alle registrierten SoundObjects: bewegt sie weiter und schiebt
	// ihre aktuelle Azimuth/Distanz zum Listener in updateSpatial. Reine sclang-Logik
	// ohne Server-Bezug, daher ohne laufende Routine direkt testbar (siehe
	// tests/TestOrchestra.sc).
	tick { |dt|
		soundObjects.do { |soundObject|
			var az, dist;
			soundObject.step(dt);
			az = listener.relativeAzimuth(soundObject.pos);
			dist = listener.distanceTo(soundObject.pos);
			soundObject.updateSpatial(az, dist);
		};
	}

	// startet jedes registrierte SoundObject (Sound-/Binauralizer-Synth) und danach
	// den zentralen Taktgeber: eine Routine statt einer pro SoundObject.
	play { |server, updateRate = 30|
		var dt = 1.0 / updateRate;

		soundObjects.do { |soundObject| soundObject.play(server) };

		routine = Routine({
			loop {
				this.tick(dt);
				dt.wait;
			}
		}).play;

		^this
	}

	// beendet das Ticken und stoppt alle registrierten SoundObjects (Synths/Busse
	// werden dabei freigegeben); danach ist die Registry leer. routine kann hier
	// noch nil sein, falls play() nie aufgerufen wurde.
	stop {
		routine !? { routine.stop };
		soundObjects.do { |soundObject| soundObject.stop };
		soundObjects = [];
	}

	// wählt ein zufälliges anderes registriertes SoundObject (nie caller selbst) für
	// Call-and-Response; nil, falls keine anderen registriert sind. Reine sclang-Logik,
	// ohne Server-Bezug, daher separat von call() testbar (siehe tests/TestOrchestra.sc).
	chooseResponder { |caller|
		var others = soundObjects.reject { |soundObject| soundObject == caller };
		^if(others.notEmpty) { others.choose } { nil };
	}

	// löst bei caller einen Zuruf aus (siehe Sound>>call) und lässt, falls ein anderes
	// Objekt registriert ist, dieses nach kurzer, leicht zufälliger Verzögerung
	// zurückrufen (Call-and-Response).
	call { |caller|
		var responder = this.chooseResponder(caller);
		caller.call;
		if(responder.notNil) {
			Routine({
				(0.3 + 0.4.rand).wait;
				responder.call;
			}).play;
		};
	}
}
