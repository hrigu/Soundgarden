// Test für SampleSound. Ausführen über run_tests.scd.
TestSampleSound : UnitTest {

	test_editableParamsListsRatePhaseAmpStartFracDuration {
		var keys = SampleSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\rate, \phase, \amp, \startFrac, \duration],
			"editableParams deckt alle live im GUI bearbeitbaren Klangparameter ab, " ++
			"inklusive Ausschnitt-Konfiguration aus Intent 53 (Intent 43)");
	}

	test_editableParamsPairsKeyWithControlSpec {
		SampleSound.editableParams.do { |pair|
			this.assert(pair[1].isKindOf(ControlSpec),
				"jeder Eintrag ist [key, ControlSpec] -- " ++ pair[0]);
		};
	}

	// startFrac/duration (Intent 53) -- reine Konstruktor-/Accessor-Logik, kein Server nötig
	// (wie bei rate/phase/amp bereits der Fall).
	test_startFracAndDurationDefaultToZeroForOldBehaviour {
		var sound = SampleSound.new("irrelevant/path.wav");

		this.assertEquals(sound.startFrac, 0,
			"ohne Angabe beginnt die Wiedergabe wie bisher am Sample-Anfang");
		this.assertEquals(sound.duration, 0,
			"duration = 0 bedeutet 'kein Cutoff' -- Kompatibilität mit dem bisherigen Verhalten");
	}

	test_startFracAndDurationCanBeConfigured {
		var sound = SampleSound.new("irrelevant/path.wav", startFrac: 0.1, duration: 0.5);

		this.assertEquals(sound.startFrac, 0.1);
		this.assertEquals(sound.duration, 0.5);
	}
}
