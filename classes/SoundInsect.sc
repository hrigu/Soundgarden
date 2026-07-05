// SoundInsect — verbindet ein Movable mit einem klingenden Synth (\insectVoice,
// siehe insect_demo.scd). Ein Routine tickt movable.step() regelmäßig, berechnet
// die Position relativ zum Listener und schiebt Azimuth/Distanz live in den Synth.
SoundInsect {
	var <>movable;
	var <>listener;
	var <synth;
	var routine;

	*new { |movable, listener|
		^super.new.init(movable, listener);
	}

	init { |aMovable, aListener|
		movable = aMovable;
		listener = aListener;
	}

	play { |server, updateRate = 30|
		var dt = 1.0 / updateRate;

		synth = Synth(\insectVoice, [\azimuth, 0, \distance, 1], server);

		routine = Routine({
			loop {
				var az, dist;
				movable.step(dt);
				az = listener.relativeAzimuth(movable.pos);
				dist = listener.distanceTo(movable.pos);
				synth.set(\azimuth, az, \distance, dist);
				dt.wait;
			}
		}).play;

		^this
	}

	stop {
		routine.stop;
		synth.free;
	}
}
