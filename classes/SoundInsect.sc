// SoundInsect — Orchestrator: verbindet ein Movable (das sich nach einer
// MoveRule bewegt) mit einem InsectSound (reiner Klang) und einem
// Binauralizer (formt den Klang für den Listener anhand Azimuth/Distanz).
// Eine Routine tickt movable.step() regelmäßig, berechnet die Position
// relativ zum Listener und schiebt Azimuth/Distanz live in den Binauralizer.
SoundInsect {
	var <>movable;
	var <>listener;
	var <>insectSound;
	var <>binauralizer;
	var routine;

	*new { |movable, listener, insectSound, binauralizer|
		^super.new.init(movable, listener, insectSound, binauralizer);
	}

	init { |aMovable, aListener, aInsectSound, aBinauralizer|
		movable = aMovable;
		listener = aListener;
		insectSound = aInsectSound ? InsectSound.new;
		binauralizer = aBinauralizer ? Binauralizer.new;
	}

	play { |server, updateRate = 30|
		var dt = 1.0 / updateRate;

		insectSound.play(server);
		// addAfter: Binauralizer muss im Node-Baum nach InsectSound laufen,
		// um dessen Bus im selben Audio-Block lesen zu können.
		binauralizer.play(server, insectSound.bus, 0, insectSound.synth, \addAfter);

		routine = Routine({
			loop {
				var az, dist;
				movable.step(dt);
				az = listener.relativeAzimuth(movable.pos);
				dist = listener.distanceTo(movable.pos);
				binauralizer.set(az, dist);
				dt.wait;
			}
		}).play;

		^this
	}

	stop {
		routine.stop;
		binauralizer.stop;
		insectSound.stop;
	}
}
