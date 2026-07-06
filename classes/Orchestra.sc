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

	// startet den zentralen Taktgeber: eine Routine statt einer pro SoundObject.
	play { |updateRate = 30|
		var dt = 1.0 / updateRate;

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
}
