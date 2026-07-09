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

	play { |server, roomSize, revTime, damping, mix, spread, inputBandwidth, tailBalance|
		log.add(\reverbPlay)
	}

	set { |roomSize, revTime, damping, mix, spread, inputBandwidth, tailBalance|
		log.add(\reverbSet)
	}

	stop { log.add(\reverbStop) }

	free { log.add(\reverbFree) }
}

// Test-Double für Listener>>makeBinauralizer: zeichnet nur den übergebenen reverbMix auf,
// ohne echten Binauralizer (SynthDef/Server) zu brauchen — analog
// FakeBinauralizerForListenerTest (TestListener.sc).
FakeBinauralizerForRoomTest {
	var <reverbMix;

	*new { |reverbMix = 0.3| ^super.new.init(reverbMix) }

	init { |aReverbMix| reverbMix = aReverbMix }
}

// Test-Double für Listener selbst (Intent 52): Room kennt Binauralizer-Setup/-Teardown-Details
// seit Intent 52 nicht mehr, delegiert das vollständig an den Listener (siehe TestListener.sc
// für die echte Binauralizer-Mechanik). Dieses Fake macht nur sichtbar, DASS/WIE Room
// delegiert (setup(server, reverbBus) bei der Konstruktion, teardown bei Room>>teardown,
// makeBinauralizer bei register/registerFromBuilder) — ohne echten Server/SynthDef zu
// brauchen. Wichtig: Room.forTest verlangt IMMER einen expliziten listener-Parameter (wie
// orchestra/reverb) — ein echter Listener.new() würde bei setup() mit der echten
// Binauralizer-Klasse und dem Fake-Bus-Symbol (\fakeBus) einen Server-seitigen Fehler werfen
// (siehe Intent 52, Challenges & Solutions zu Task 1.0).
FakeListenerForRoomTest {
	var <>binauralizerClass;
	var <log;

	*new { |aLog, aBinauralizerClass| ^super.new.init(aLog, aBinauralizerClass) }

	init { |aLog, aBinauralizerClass|
		log = aLog ?? { List.new };
		binauralizerClass = aBinauralizerClass ?? { FakeBinauralizerForRoomTest };
	}

	makeBinauralizer { |reverbMix = 0.3|
		^binauralizerClass.new(reverbMix: reverbMix)
	}

	setup { |server, reverbBus|
		log.add([\listenerSetup, server, reverbBus]);
	}

	teardown {
		log.add(\listenerTeardown);
	}
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

	test_constructionWiresListenerWithServerAndReverbBus {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), listener: FakeListenerForRoomTest.new(log));

		this.assertEquals(log.asArray, [[\listenerSetup, nil, \fakeBus]],
			"Room verkabelt den Listener bei der Konstruktion mit server (hier nil, siehe " ++
			"forTest) und reverb.bus (Intent 52) — unabhängig davon, ob je eine " ++
			"binauralizerClass gesetzt wird");
	}

	test_playStartsOrchestraBeforeReverb {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.play;

		this.assertEquals(log.asArray, [\orchestraPlay, \reverbPlay],
			"orchestra.play muss vor reverb.play laufen (RoomReverb braucht addToTail)");
	}

	test_stopStopsBoth {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.stop;

		this.assertEquals(log.asArray, [\orchestraStop, \reverbStop]);
	}

	test_teardownStopsFreesReverbAndDelegatesToListener {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new(log));

		room.teardown;

		this.assertEquals(log.asArray,
			[[\listenerSetup, nil, \fakeBus], \orchestraStop, \reverbStop, \reverbFree,
				\listenerTeardown],
			"listenerSetup passiert bereits bei der Konstruktion (siehe " ++
			"test_constructionWiresListenerWithServerAndReverbBus); teardown stoppt danach, " ++
			"gibt die Reverb-Ressourcen frei und delegiert zusätzlich an listener.teardown " ++
			"(Intent 52) — Room kennt Binauralizer-Details dafür nicht mehr");
	}

	test_registerBuildsSoundObjectUsingListenersBinauralizer {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new(List.new, FakeBinauralizerForRoomTest));
		var movable = \someMovable;
		var sound = \someSound;
		var registered;

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
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new(List.new, FakeBinauralizerForRoomTest));
		var registered;

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
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.call(\someCaller);

		this.assertEquals(log.asArray, [\someCaller]);
	}

	test_sizeChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.size = 12;

		this.assertEquals(log.last, \reverbSet, "size = ... aktualisiert den Hall sofort");
	}

	test_heightChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.height = 6;

		this.assertEquals(log.last, \reverbSet, "height = ... aktualisiert den Hall sofort");
	}

	test_surfaceChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.surface = 0.9;

		this.assertEquals(log.last, \reverbSet, "surface = ... aktualisiert den Hall sofort");
	}

	test_mixChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.mix = 0.7;

		this.assertEquals(log.last, \reverbSet, "mix = ... aktualisiert den Hall sofort");
	}

	test_spreadChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.spread = 30;

		this.assertEquals(log.last, \reverbSet, "spread = ... aktualisiert den Hall sofort");
	}

	test_inputBandwidthChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.inputBandwidth = 0.8;

		this.assertEquals(log.last, \reverbSet,
			"inputBandwidth = ... aktualisiert den Hall sofort");
	}

	test_tailBalanceChangeUpdatesReverbLive {
		var log = List.new;
		var room = Room.forTest(FakeOrchestraForRoomTest.new(log), FakeReverbForRoomTest.new(log),
			listener: FakeListenerForRoomTest.new());

		room.tailBalance = 0.2;

		this.assertEquals(log.last, \reverbSet, "tailBalance = ... aktualisiert den Hall sofort");
	}

	// reverbParams-Mapping — reine sclang-Logik, direkt testbar ohne Server (wie
	// TestCircularMoveRule/TestMovable).
	test_reverbParamsUsesSizeDirectlyAsRoomSize {
		var room = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), size: 12,
			listener: FakeListenerForRoomTest.new());

		this.assertEquals(room.reverbParams[0], 12);
	}

	test_reverbParamsSmootherSurfaceMeansLessDamping {
		var rough = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), surface: 0,
			listener: FakeListenerForRoomTest.new());
		var smooth = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), surface: 1,
			listener: FakeListenerForRoomTest.new());

		this.assert(smooth.reverbParams[2] < rough.reverbParams[2],
			"glattere Oberfläche dämpft weniger (kleinerer damping-Wert)");
	}

	test_reverbParamsLargerVolumeMeansLongerRevTime {
		var small = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), size: 4, height: 2,
			listener: FakeListenerForRoomTest.new());
		var big = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), size: 20, height: 10,
			listener: FakeListenerForRoomTest.new());

		this.assert(big.reverbParams[1] > small.reverbParams[1],
			"größeres Raumvolumen führt zu längerer Nachhallzeit");
	}

	test_reverbParamsIncludesSpread {
		var room = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), listener: FakeListenerForRoomTest.new());

		this.assertEquals(room.reverbParams[4], 15,
			"spread-Default entspricht dem bisherigen Literal (GVerbs eigener Default)");
	}

	test_reverbParamsIncludesInputBandwidth {
		var room = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), listener: FakeListenerForRoomTest.new());

		this.assertEquals(room.reverbParams[5], 0.5,
			"inputBandwidth-Default entspricht dem bisherigen Literal");
	}

	test_reverbParamsIncludesTailBalance {
		var room = Room.forTest(FakeOrchestraForRoomTest.new(List.new),
			FakeReverbForRoomTest.new(List.new), listener: FakeListenerForRoomTest.new());

		this.assertEquals(room.reverbParams[6], 0.5,
			"tailBalance-Default (0.5) entspricht der bisherigen mix=mix-Kopplung");
	}
}
