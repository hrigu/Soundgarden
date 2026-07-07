// Test-Doubles für Room: zeichnen nur Aufrufreihenfolge/-argumente auf, ohne echten Server/
// Bus zu brauchen — analog zu FakeSoundObjectForOrchestraTest (TestOrchestra.sc). log ist ein
// List statt eines rohen Array: Array>>add mutiert nur in-place, solange genug Kapazität
// übrig ist, und reallokiert sonst still (Rückgabewert verworfen, da wir ihn hier nicht
// auffangen) — bei mehreren Aufrufen über zwei Fake-Instanzen hinweg blieb dadurch nur der
// erste Log-Eintrag erhalten. List>>add mutiert dagegen immer dasselbe Objekt, egal wie oft.
FakeOrchestraForRoomTest {
	var <log;

	*new { |aLog| ^super.new.init(aLog) }

	init { |aLog| log = aLog }

	play { |server| log.add(\orchestraPlay) }

	stop { log.add(\orchestraStop) }

	register { |soundObject| log.add(soundObject) }

	call { |caller| log.add(caller) }
}

FakeReverbForRoomTest {
	var <bus, <log;

	*new { |aLog| ^super.new.init(aLog) }

	init { |aLog|
		log = aLog;
		bus = \fakeBus;
	}

	play { |server, roomSize, revTime, damping, mix| log.add(\reverbPlay) }

	set { |roomSize, revTime, damping, mix| log.add(\reverbSet) }

	stop { log.add(\reverbStop) }
}

// Test-Double für Listener>>makeBinauralizer: zeichnet nur den übergebenen reverbMix auf,
// ohne echten Binauralizer (SynthDef/Server) zu brauchen — analog
// FakeBinauralizerForListenerTest (TestListener.sc).
FakeBinauralizerForRoomTest {
	var <reverbMix;

	*new { |reverbMix = 0.3| ^super.new.init(reverbMix) }

	init { |aReverbMix| reverbMix = aReverbMix }
}

// Test-Doubles für registerFromBuilder: reine Marker-Klassen mit denselben <>-Setter-Namen wie
// InsectSound/Movable, damit sichtbar wird, dass SoundObjectBuilder (und nicht Room selbst) die
// Params-Events anwendet — analog Fake*ForSoundObjectBuilderTest (TestSoundObjectBuilder.sc).
FakeSoundForRoomRegisterFromBuilderTest {
	var <>wingRate;
}

FakeMovableForRoomRegisterFromBuilderTest {
	var <>roomRadius;
}

// Test für Room. Ausführen über run_tests.scd.
TestRoom : UnitTest {

	test_playStartsOrchestraBeforeReverb {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));

		room.play;

		this.assertEquals(log.asArray, [\orchestraPlay, \reverbPlay],
			"orchestra.play muss vor reverb.play laufen (RoomReverb braucht addToTail)");
	}

	test_stopStopsBoth {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));

		room.stop;

		this.assertEquals(log.asArray, [\orchestraStop, \reverbStop]);
	}

	test_registerBuildsSoundObjectUsingListenersBinauralizer {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));
		var movable = \someMovable;
		var sound = \someSound;
		var registered;

		room.listener.binauralizerClass = FakeBinauralizerForRoomTest;
		registered = room.register(movable, sound, reverbMix: 0.7);

		this.assertEquals(registered.movable, movable,
			"das registrierte SoundObject bekommt das übergebene movable");
		this.assertEquals(registered.sound, sound,
			"das registrierte SoundObject bekommt den übergebenen sound");
		this.assert(registered.binauralizer.isKindOf(FakeBinauralizerForRoomTest),
			"der Binauralizer stammt aus listener.makeBinauralizer, nicht vom Skript konstruiert");
		this.assertEquals(registered.binauralizer.reverbMix, 0.7,
			"reverbMix wird bis zum Binauralizer durchgereicht");
		this.assertEquals(log.asArray, [registered],
			"orchestra.register bekommt genau dieses SoundObject");
	}

	test_registerFromBuilderBuildsSoundObjectViaBuilder {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));
		var registered;

		room.listener.binauralizerClass = FakeBinauralizerForRoomTest;
		registered = room.registerFromBuilder((wingRate: 280), (roomRadius: 4), reverbMix: 0.7,
			soundClass: FakeSoundForRoomRegisterFromBuilderTest,
			movableClass: FakeMovableForRoomRegisterFromBuilderTest);

		this.assertEquals(registered.sound.wingRate, 280,
			"soundParams werden über den Builder auf soundClass angewendet");
		this.assertEquals(registered.movable.roomRadius, 4,
			"movableParams werden über den Builder auf movableClass angewendet");
		this.assert(registered.binauralizer.isKindOf(FakeBinauralizerForRoomTest),
			"der Binauralizer stammt aus listener.makeBinauralizer, nicht vom Skript konstruiert");
		this.assertEquals(registered.binauralizer.reverbMix, 0.7,
			"reverbMix wird bis zum Binauralizer durchgereicht");
		this.assertEquals(log.asArray, [registered],
			"orchestra.register bekommt genau dieses SoundObject");
	}

	test_callDelegatesToOrchestra {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));

		room.call(\someCaller);

		this.assertEquals(log.asArray, [\someCaller]);
	}

	test_sizeChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));

		room.size = 12;

		this.assertEquals(log.last, \reverbSet, "size = ... aktualisiert den Hall sofort");
	}

	test_heightChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));

		room.height = 6;

		this.assertEquals(log.last, \reverbSet, "height = ... aktualisiert den Hall sofort");
	}

	test_surfaceChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));

		room.surface = 0.9;

		this.assertEquals(log.last, \reverbSet, "surface = ... aktualisiert den Hall sofort");
	}

	test_mixChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log));

		room.mix = 0.7;

		this.assertEquals(log.last, \reverbSet, "mix = ... aktualisiert den Hall sofort");
	}

	// reverbParams-Mapping — reine sclang-Logik, direkt testbar ohne Server (wie
	// TestCircularMoveRule/TestMovable).
	test_reverbParamsUsesSizeDirectlyAsRoomSize {
		var room = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), size: 12);

		this.assertEquals(room.reverbParams[0], 12);
	}

	test_reverbParamsSmootherSurfaceMeansLessDamping {
		var rough = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), surface: 0);
		var smooth = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), surface: 1);

		this.assert(smooth.reverbParams[2] < rough.reverbParams[2],
			"glattere Oberfläche dämpft weniger (kleinerer damping-Wert)");
	}

	test_reverbParamsLargerVolumeMeansLongerRevTime {
		var small = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), size: 4, height: 2);
		var big = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), size: 20, height: 10);

		this.assert(big.reverbParams[1] > small.reverbParams[1],
			"größeres Raumvolumen führt zu längerer Nachhallzeit");
	}
}
