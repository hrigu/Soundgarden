// Test-Double für Listener>>makeBinauralizer: zeichnet nur die übergebenen Konstruktor-
// Argumente auf, ohne echten Binauralizer (SynthDef/Server) zu brauchen.
FakeBinauralizerForListenerTest {
	var <reverbMix;

	*new { |reverbMix = 0.3| ^super.new.init(reverbMix) }

	init { |aReverbMix| reverbMix = aReverbMix }
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
}
