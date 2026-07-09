// Test-Double für Sound>>setParam: zeichnet nur gesetzte Controls auf, ohne echten
// Server/Synth zu brauchen — analog FakeReverbForRoomTest (TestRoom.sc).
FakeSynthForSoundTest {
	var <log;

	*new { |aLog| ^super.new.init(aLog) }

	init { |aLog| log = aLog }

	set { |key, value| log.add([key, value]) }
}

// Minimale Sound-Subklasse mit einem einzigen <>-Parameter, um setParam isoliert von
// InsectSound/SampleSound zu testen.
FakeSoundForSoundTest : Sound {
	var <>freq;

	*new { |freq = 440| ^super.new.init(freq) }

	init { |aFreq| freq = aFreq }

	// synth hat in Sound nur einen Getter (<synth) -- innerhalb der Subklasse ist die
	// Instanzvariable aber direkt zuweisbar, kein echter Server/Synth für den Test nötig.
	setSynth { |aSynth| synth = aSynth }
}

// Test für Sound. Ausführen über run_tests.scd.
TestSound : UnitTest {

	test_setParamUpdatesInstanceVariable {
		var sound = FakeSoundForSoundTest.new;

		sound.setParam(\freq, 880);

		this.assertEquals(sound.freq, 880,
			"setParam ruft den <>-Setter des Parameters auf");
	}

	test_setParamPushesToRunningSynth {
		var sound = FakeSoundForSoundTest.new;
		var log = List.new;
		sound.setSynth(FakeSynthForSoundTest.new(log));

		sound.setParam(\freq, 880);

		this.assertEquals(log.asArray, [[\freq, 880]],
			"setParam schreibt denselben Wert live auf den laufenden Synth");
	}

	test_setParamWithoutRunningSynthDoesNotFail {
		var sound = FakeSoundForSoundTest.new;

		this.assert(sound.synth.isNil, "Vorbedingung: kein Synth gesetzt");
		sound.setParam(\freq, 880); // darf keinen Error werfen (synth !? {...})

		this.assertEquals(sound.freq, 880,
			"die Instanzvariable wird auch ohne laufenden Synth aktualisiert");
	}

	test_defaultEditableParamsIsEmpty {
		this.assertEquals(Sound.editableParams, [],
			"generischer Default: keine bearbeitbaren Parameter ohne Override");
	}
}
