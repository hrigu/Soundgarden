// Test-Double für SoundObject: zeichnet Aufrufe auf (play/step/updateSpatial/stop),
// ohne echten Movable/Sound/Binauralizer zu brauchen — Orchestra kennt SoundObject nur
// über diese Methoden plus pos.
FakeSoundObjectForOrchestraTest {
	var <pos;
	var <playedServer;
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

	play { |server|
		playedServer = server;
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

	test_playStartsAllRegisteredSoundObjects {
		var listener = Listener.new;
		var soundObject1 = FakeSoundObjectForOrchestraTest.new(#[0, 1, 0]);
		var soundObject2 = FakeSoundObjectForOrchestraTest.new(#[0, 1, 0]);
		var orchestra = Orchestra.new(listener);

		orchestra.register(soundObject1);
		orchestra.register(soundObject2);
		orchestra.play(\fakeServer);

		this.assertEquals(soundObject1.playedServer, \fakeServer,
			"play() wird mit dem Server auf jedem registrierten Objekt aufgerufen");
		this.assertEquals(soundObject2.playedServer, \fakeServer,
			"play() wird mit dem Server auf jedem registrierten Objekt aufgerufen");

		orchestra.stop;
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

	test_chooseResponderNeverReturnsCallerItself {
		var listener = Listener.new;
		var caller = FakeSoundObjectForOrchestraTest.new(#[0, 1, 0]);
		var other1 = FakeSoundObjectForOrchestraTest.new(#[0, 1, 0]);
		var other2 = FakeSoundObjectForOrchestraTest.new(#[0, 1, 0]);
		var orchestra = Orchestra.new(listener);

		orchestra.register(caller);
		orchestra.register(other1);
		orchestra.register(other2);

		// mehrfach ziehen, um sicherzugehen, dass der Zufall nie caller trifft
		20.do {
			var responder = orchestra.chooseResponder(caller);
			this.assert(responder != caller, "chooseResponder wählt nie den Aufrufer selbst");
			this.assert([other1, other2].includes(responder),
				"chooseResponder wählt aus den übrigen registrierten Objekten");
		};
	}

	test_chooseResponderReturnsNilWhenOnlyCallerRegistered {
		var listener = Listener.new;
		var caller = FakeSoundObjectForOrchestraTest.new(#[0, 1, 0]);
		var orchestra = Orchestra.new(listener);

		orchestra.register(caller);

		this.assertEquals(orchestra.chooseResponder(caller), nil,
			"kein Rückruf möglich, wenn kein anderes Objekt registriert ist");
	}
}
