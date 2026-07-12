// Test für MessiaenBirdSound. Ausführen über run_tests.scd.
TestMessiaenBirdSound : UnitTest {

	test_editableParamsListsAmpBrightnessPitchShiftAndRepeatPause {
		var keys = MessiaenBirdSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\amp, \brightness, \pitchShift, \repeatPauseMin, \repeatPauseMax],
			"editableParams deckt alle live im GUI bearbeitbaren Klangparameter ab -- motif " ++
			"ist bewusst kein editierbarer Slider (siehe requiredConstructorArgs). pitchShift " ++
			"(Intent 59, Nutzer-Wunsch) transponiert die im Motiv fest hinterlegten Tonhöhen " ++
			"live, statt das Motiv selbst neu zu bauen (siehe transposedFreq). " ++
			"repeatPauseMin/Max (Intent 59-Folgewunsch: mehr Pausen-Varianz zwischen den " ++
			"Ruf-Wiederholungen bei loop: true)");
	}

	test_editableParamsPairsKeyWithControlSpec {
		MessiaenBirdSound.editableParams.do { |pair|
			this.assert(pair[1].isKindOf(ControlSpec),
				"jeder Eintrag ist [key, ControlSpec] -- " ++ pair[0]);
		};
	}

	test_requiredConstructorArgsListsMotif {
		this.assertEquals(MessiaenBirdSound.requiredConstructorArgs, [\motif],
			"motif ist Pflicht für *new, aber kein editierbarer Slider-Parameter " ++
			"(analog path bei SampleSound)");
	}

	test_defaultsMatchConstructorArguments {
		var motif = BirdMotif.new([[440, 0.1, 0.8]]);
		var bird = MessiaenBirdSound.new(motif, amp: 0.5, brightness: 0.7, loop: true,
			pitchShift: 3, repeatPauseMin: 1.0, repeatPauseMax: 4.0);

		this.assertEquals(bird.motif, motif);
		this.assertEquals(bird.amp, 0.5);
		this.assertEquals(bird.brightness, 0.7);
		this.assertEquals(bird.loop, true);
		this.assertEquals(bird.pitchShift, 3);
		this.assertEquals(bird.repeatPauseMin, 1.0);
		this.assertEquals(bird.repeatPauseMax, 4.0);
	}

	test_defaultAmpBrightnessLoopPitchShiftAndRepeatPause {
		var motif = BirdMotif.new([[440, 0.1, 0.8]]);
		var bird = MessiaenBirdSound.new(motif);

		this.assertEquals(bird.amp, 0.35);
		this.assertEquals(bird.brightness, 0.5);
		this.assertEquals(bird.loop, false);
		this.assertEquals(bird.pitchShift, 0);
		this.assertEquals(bird.repeatPauseMin, 0.8);
		this.assertEquals(bird.repeatPauseMax, 3.0);
	}

	test_transposedFreqAppliesPitchShiftInSemitones {
		var motif = BirdMotif.new([[440, 0.1, 0.8]]);
		var bird = MessiaenBirdSound.new(motif, pitchShift: 12);

		this.assertFloatEquals(bird.transposedFreq(440), 880.0,
			"12 Halbtöne (eine Oktave) müssen die Frequenz verdoppeln");
	}

	test_transposedFreqWithZeroPitchShiftIsUnchanged {
		var motif = BirdMotif.new([[440, 0.1, 0.8]]);
		var bird = MessiaenBirdSound.new(motif);

		this.assertFloatEquals(bird.transposedFreq(440), 440.0);
	}
}
