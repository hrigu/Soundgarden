// Test-Double für SoundObject: zeichnet Aufrufe auf (step/updateSpatial/stop), ohne
// echten Movable/Sound/Binauralizer zu brauchen — Orchestra kennt SoundObject nur über
// diese drei Methoden plus pos.
FakeSoundObjectForOrchestraTest {
	var <pos;
	var <steppedDt;
	var <lastAzimuth;
	var <lastDistance;
	var <stopped;

	*new { |aPos|
		^super.new.init(aPos);
	}

	init { |aPos|
		pos = aPos;
		stopped = false;
	}

	step { |dt|
		steppedDt = dt;
	}

	updateSpatial { |azimuth, distance|
		lastAzimuth = azimuth;
		lastDistance = distance;
	}

	stop {
		stopped = true;
	}
}

// Test für Orchestra. Ausführen über run_tests.scd.
TestOrchestra : UnitTest {

	test_tickMovesAndUpdatesRegisteredSoundObjects {
		var listener = Listener.new;
		var soundObject = FakeSoundObjectForOrchestraTest.new(#[0, 2, 0]);
		var orchestra = Orchestra.new(listener);

		orchestra.register(soundObject);
		orchestra.tick(0.033);

		this.assertEquals(soundObject.steppedDt, 0.033, "step() wird mit dt aufgerufen");
		this.assertEquals(soundObject.lastAzimuth.round(0.0001), 0.0,
			"Objekt direkt vor dem Listener: Azimuth 0");
		this.assertEquals(soundObject.lastDistance.round(0.0001), 2.0,
			"Distanz entspricht der Fake-Position");
	}

	test_stopStopsAllRegisteredSoundObjectsAndClearsRegistry {
		var listener = Listener.new;
		var soundObject1 = FakeSoundObjectForOrchestraTest.new(#[0, 1, 0]);
		var soundObject2 = FakeSoundObjectForOrchestraTest.new(#[0, 1, 0]);
		var orchestra = Orchestra.new(listener);

		orchestra.register(soundObject1);
		orchestra.register(soundObject2);
		orchestra.stop;

		this.assert(soundObject1.stopped, "stop() wird auf jedem registrierten Objekt aufgerufen");
		this.assert(soundObject2.stopped, "stop() wird auf jedem registrierten Objekt aufgerufen");
		this.assertEquals(orchestra.soundObjects.size, 0, "Registry ist nach stop leer");
	}
}
