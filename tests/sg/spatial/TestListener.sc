// Test-Double für Listener>>makeBinauralizer: zeichnet nur die übergebenen Konstruktor-
// Argumente auf, ohne echten Binauralizer (SynthDef/Server) zu brauchen.
FakeBinauralizerForListenerTest {
	var <reverbMix;

	*new { |reverbMix = 0.3| ^super.new.init(reverbMix) }

	init { |aReverbMix| reverbMix = aReverbMix }
}

// Test-Double für einen "normalen" Binauralizer-Typ (nur *addSynthDef, kein *setup) —
// analog FakeClassBinauralizerForRoomTest aus TestRoom.sc (Intent 51), jetzt für
// Listener>>setup/binauralizerClass_ (Intent 52).
FakeClassBinauralizerForListenerTest {
	classvar <lastAddSynthDefBus;

	*addSynthDef { |reverbBus|
		lastAddSynthDefBus = reverbBus;
	}

	*reset {
		lastAddSynthDefBus = nil;
	}
}

// Test-Double für einen ATK-artigen Binauralizer-Typ (nur *setup/*teardown) — Listener
// erkennt diesen Fall über respondsTo(\setup), nicht per Klassenvergleich (siehe
// Listener>>loadBinauralizerClass).
FakeAtkBinauralizerForListenerTest {
	classvar <lastSetupServer, <lastSetupSubjectID, <lastSetupBus, <teardownCalled;

	*setup { |server, subjectID = 21, reverbBus|
		lastSetupServer = server;
		lastSetupSubjectID = subjectID;
		lastSetupBus = reverbBus;
	}

	*teardown {
		teardownCalled = true;
	}

	*reset {
		lastSetupServer = nil;
		lastSetupSubjectID = nil;
		lastSetupBus = nil;
		teardownCalled = false;
	}
}

// Test-Double für directOutBinauralizerClass (Fallback bei binauralizerClass = nil) — hat
// bewusst nur *addSynthDef, kein *setup, analog zum echten DirectOutBinauralizer. *new muss
// reverbMix als Parameter akzeptieren, damit Listener>>makeBinauralizer(reverbMix: ...) keine
// Warnung "keyword arg 'reverbMix' not found" erzeugt.
FakeDirectOutBinauralizerForListenerTest {
	classvar <lastAddSynthDefBus;
	var <reverbMix;

	*new { |reverbMix = 0.2|
		^super.new.init(reverbMix);
	}

	init { |aReverbMix|
		reverbMix = aReverbMix;
	}

	*addSynthDef { |reverbBus|
		lastAddSynthDefBus = reverbBus;
	}

	*reset {
		lastAddSynthDefBus = nil;
	}
}

// Test für die Bewegungsmethoden von Listener. Ausführen über run_tests.scd.
TestListener : UnitTest {

	test_makeBinauralizerUsesConfiguredClass {
		var listener = Listener.new(#[0, 0, 0], facing: 0,
			binauralizerClass: FakeBinauralizerForListenerTest);
		var binauralizer = listener.makeBinauralizer(0.7);

		this.assert(binauralizer.isKindOf(FakeBinauralizerForListenerTest),
			"makeBinauralizer erzeugt eine Instanz der konfigurierten binauralizerClass");
		this.assertEquals(binauralizer.reverbMix, 0.7,
			"reverbMix wird an den Konstruktor der binauralizerClass weitergereicht");
	}

	test_moveForwardAtFacingZeroMovesAlongY {
		var listener = Listener.new(#[0, 0, 0], facing: 0);
		listener.moveForward(1);

		this.assertEquals(
			[listener.pos[0].round(0.0001), listener.pos[1].round(0.0001), listener.pos[2]],
			[0.0, 1.0, 0.0],
			"bei facing=0 bewegt moveForward entlang +y"
		);
	}

	test_moveBackwardAtFacingZeroMovesAlongMinusY {
		var listener = Listener.new(#[0, 0, 0], facing: 0);
		listener.moveBackward(1);

		this.assertEquals(
			[listener.pos[0].round(0.0001), listener.pos[1].round(0.0001), listener.pos[2]],
			[0.0, -1.0, 0.0],
			"bei facing=0 bewegt moveBackward entlang -y"
		);
	}

	test_strafeRightAtFacingZeroMovesAlongX {
		var listener = Listener.new(#[0, 0, 0], facing: 0);
		listener.strafeRight(1);

		this.assertEquals(
			[listener.pos[0].round(0.0001), listener.pos[1].round(0.0001), listener.pos[2]],
			[1.0, 0.0, 0.0],
			"bei facing=0 bewegt strafeRight entlang +x"
		);
	}

	test_strafeLeftAtFacingZeroMovesAlongMinusX {
		var listener = Listener.new(#[0, 0, 0], facing: 0);
		listener.strafeLeft(1);

		this.assertEquals(
			[listener.pos[0].round(0.0001), listener.pos[1].round(0.0001), listener.pos[2]],
			[-1.0, 0.0, 0.0],
			"bei facing=0 bewegt strafeLeft entlang -x"
		);
	}

	test_rotateChangesFacing {
		var listener = Listener.new(#[0, 0, 0], facing: 0);
		listener.rotate(90);

		this.assertEquals(listener.facing, 90, "rotate addiert deltaDegrees auf facing");
	}

	test_rotationChangesMoveForwardDirection {
		var listener = Listener.new(#[0, 0, 0], facing: 90);
		listener.moveForward(1);

		this.assertEquals(
			[listener.pos[0].round(0.0001), listener.pos[1].round(0.0001), listener.pos[2]],
			[1.0, 0.0, 0.0],
			"bei facing=90 bewegt moveForward entlang +x statt +y"
		);
	}

	// setup/binauralizerClass_/teardown (Intent 52) — Listener übernimmt die Verantwortung,
	// die bisher im Room lag (siehe Room.sc vor Intent 52).

	test_setupRegistersPlainBinauralizerOnGivenReverbBus {
		var listener = Listener.new(binauralizerClass: FakeClassBinauralizerForListenerTest);

		FakeClassBinauralizerForListenerTest.reset;
		listener.setup(\fakeServer, \fakeBus);

		this.assertEquals(FakeClassBinauralizerForListenerTest.lastAddSynthDefBus, \fakeBus,
			"ein normaler Binauralizer-Typ wird bei setup mit dem übergebenen Reverb-Bus registriert");
	}

	test_setupUsesSubjectIDForAtkStyleSetup {
		var listener = Listener.new(binauralizerClass: FakeAtkBinauralizerForListenerTest,
			subjectID: 42);

		FakeAtkBinauralizerForListenerTest.reset;
		listener.setup(\fakeServer, \fakeBus);

		this.assertEquals(FakeAtkBinauralizerForListenerTest.lastSetupServer, \fakeServer,
			"setup reicht den übergebenen Server an einen ATK-artigen Binauralizer-Typ weiter");
		this.assertEquals(FakeAtkBinauralizerForListenerTest.lastSetupSubjectID, 42,
			"setup reicht die konfigurierte subjectID weiter");
		this.assertEquals(FakeAtkBinauralizerForListenerTest.lastSetupBus, \fakeBus,
			"setup reicht den übergebenen Reverb-Bus weiter");
	}

	test_binauralizerClassChangeAfterSetupLoadsImmediately {
		var listener = Listener.new(binauralizerClass: FakeClassBinauralizerForListenerTest);
		listener.setup(\fakeServer, \fakeBus);

		FakeAtkBinauralizerForListenerTest.reset;
		listener.binauralizerClass = FakeAtkBinauralizerForListenerTest;

		this.assertEquals(FakeAtkBinauralizerForListenerTest.lastSetupServer, \fakeServer,
			"binauralizerClass_ lädt nach setup sofort mit dem gemerkten Server nach");
		this.assertEquals(FakeAtkBinauralizerForListenerTest.lastSetupBus, \fakeBus,
			"binauralizerClass_ lädt nach setup sofort mit dem gemerkten Reverb-Bus nach");
	}

	test_binauralizerClassChangeBeforeSetupDoesNotTriggerLoading {
		var listener = Listener.new;

		FakeClassBinauralizerForListenerTest.reset;
		listener.binauralizerClass = FakeClassBinauralizerForListenerTest;

		this.assertEquals(FakeClassBinauralizerForListenerTest.lastAddSynthDefBus, nil,
			"vor dem ersten setup wird noch keine SynthDef registriert (kein Server/Bus bekannt)");
	}

	test_setBinauralizerClassNilUsesDirectOutFallback {
		var listener = Listener.new(binauralizerClass: FakeClassBinauralizerForListenerTest);
		listener.directOutBinauralizerClass = FakeDirectOutBinauralizerForListenerTest;
		listener.binauralizerClass = nil;

		FakeDirectOutBinauralizerForListenerTest.reset;
		listener.setup(\fakeServer, \fakeBus);

		this.assertEquals(FakeDirectOutBinauralizerForListenerTest.lastAddSynthDefBus, \fakeBus,
			"binauralizerClass = nil registriert bei setup stattdessen directOutBinauralizerClass");
		this.assert(listener.makeBinauralizer.isKindOf(FakeDirectOutBinauralizerForListenerTest),
			"makeBinauralizer baut nach binauralizerClass = nil eine directOutBinauralizerClass-Instanz");
	}

	test_teardownDelegatesToBinauralizerClassWhenItRespondsToTeardown {
		var listener = Listener.new(binauralizerClass: FakeAtkBinauralizerForListenerTest);
		listener.setup(\fakeServer, \fakeBus);

		FakeAtkBinauralizerForListenerTest.reset;
		listener.teardown;

		this.assert(FakeAtkBinauralizerForListenerTest.teardownCalled,
			"teardown delegiert an binauralizerClass.teardown, falls die Klasse das anbietet");
	}

	test_teardownIgnoresBinauralizerClassWithoutTeardown {
		var listener = Listener.new(binauralizerClass: FakeClassBinauralizerForListenerTest);
		listener.setup(\fakeServer, \fakeBus);

		// darf keinen Error werfen, obwohl FakeClassBinauralizerForListenerTest kein *teardown hat
		listener.teardown;
		this.assert(true, "teardown bleibt ohne Error, wenn binauralizerClass kein *teardown anbietet");
	}
}
