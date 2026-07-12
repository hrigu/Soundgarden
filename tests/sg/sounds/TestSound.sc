// Test-Double für Sound>>setParam: zeichnet nur gesetzte Controls auf, ohne echten
// Server/Synth zu brauchen — analog FakeReverbForRoomTest (TestRoom.sc). free ist ein
// No-op wie beim echten Synth (Sound>>stop ruft es bedingungslos auf).
FakeSynthForSoundTest {
	var <log;

	*new { |aLog| ^super.new.init(aLog) }

	init { |aLog| log = aLog }

	set { |key, value| log.add([key, value]) }

	free { }
}

// Test-Double für Sound>>stop: nur free() als No-op, kein echter Server-Bus nötig.
FakeBusForSoundTest {
	free { }
}

// Minimale Sound-Subklasse mit einem einzigen <>-Parameter, um setParam isoliert von
// InsectSound/SampleSound zu testen.
FakeSoundForSoundTest : Sound {
	var <>freq;

	*new { |freq = 440| ^super.new.init(freq) }

	init { |aFreq| freq = aFreq }

	// synth/bus haben in Sound nur Getter (<synth/<bus) -- innerhalb der Subklasse sind die
	// Instanzvariablen aber direkt zuweisbar, kein echter Server/Synth/Bus für den Test nötig.
	setSynth { |aSynth| synth = aSynth }
	setBus { |aBus| bus = aBus }
}

// Minimale Sound-Subklasse mit einem Pflicht-Konstruktorargument (id) und einem editierbaren
// Parameter (freq), um buildFromSavedParams (Intent 46) isoliert zu testen.
FakeConfigurableSoundForSoundTest : Sound {
	var <id;
	var <>freq;

	*new { |id, freq = 440| ^super.new.init(id, freq) }

	init { |anId, aFreq|
		id = anId;
		freq = aFreq;
	}

	*editableParams {
		^[[\freq, ControlSpec(20, 2000, \exp)]]
	}

	*requiredConstructorArgs {
		^[\id]
	}
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

	// Bug-Report (Intent 59-Folgearbeit): GUI-Regler eines längst gestoppten SoundObject
	// schickten wiederholt "/n_set Node ... not found", weil stop() synth/bus zwar freigab,
	// die Instanzvariablen selbst aber nicht auf nil zurücksetzte -- setParam prüft nur
	// "synth !? {...}" und feuerte deshalb weiter auf den toten Synth.
	test_stopClearsSynthAndBusReferences {
		var sound = FakeSoundForSoundTest.new;
		sound.setSynth(FakeSynthForSoundTest.new(List.new));
		sound.setBus(FakeBusForSoundTest.new);

		sound.stop;

		this.assert(sound.synth.isNil,
			"stop setzt synth auf nil, damit setParam danach keinen toten Node anspricht");
		this.assert(sound.bus.isNil, "stop setzt bus ebenfalls auf nil");
	}

	test_setParamAfterStopDoesNotTouchDeadSynth {
		var sound = FakeSoundForSoundTest.new;
		var log = List.new;
		sound.setSynth(FakeSynthForSoundTest.new(log));
		sound.setBus(FakeBusForSoundTest.new);
		sound.stop;

		sound.setParam(\freq, 880);

		this.assertEquals(log.asArray, [],
			"nach stop darf setParam keinen .set mehr auf den (jetzt toten) Synth schicken");
		this.assertEquals(sound.freq, 880,
			"die Instanzvariable selbst wird trotzdem weiter aktualisiert");
	}

	test_defaultEditableParamsIsEmpty {
		this.assertEquals(Sound.editableParams, [],
			"generischer Default: keine bearbeitbaren Parameter ohne Override");
	}

	test_defaultRequiredConstructorArgsIsEmpty {
		this.assertEquals(Sound.requiredConstructorArgs, [],
			"generischer Default (Intent 46): kein *new einer Subklasse braucht zwingend " ++
			"weitere Konstruktor-Argumente ausser den editableParams");
	}

	test_buildFromSavedParamsUsesRequiredConstructorArgsThenEditableParams {
		var sound = FakeConfigurableSoundForSoundTest.buildFromSavedParams((id: \abc, freq: 880));

		this.assertEquals(sound.id, \abc,
			"buildFromSavedParams (Intent 46) übergibt requiredConstructorArgs an *new");
		this.assertEquals(sound.freq, 880,
			"buildFromSavedParams wendet danach editableParams per setParam an");
	}
}
