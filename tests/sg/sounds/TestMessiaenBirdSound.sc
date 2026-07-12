// Test für MessiaenBirdSound. Ausführen über run_tests.scd.
TestMessiaenBirdSound : UnitTest {

	test_editableParamsListsAmpAndBrightness {
		var keys = MessiaenBirdSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\amp, \brightness],
			"editableParams deckt alle live im GUI bearbeitbaren Klangparameter ab -- motif " ++
			"ist bewusst kein editierbarer Slider (siehe requiredConstructorArgs)");
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
		var bird = MessiaenBirdSound.new(motif, amp: 0.5, brightness: 0.7, loop: true);

		this.assertEquals(bird.motif, motif);
		this.assertEquals(bird.amp, 0.5);
		this.assertEquals(bird.brightness, 0.7);
		this.assertEquals(bird.loop, true);
	}

	test_defaultAmpBrightnessAndLoop {
		var motif = BirdMotif.new([[440, 0.1, 0.8]]);
		var bird = MessiaenBirdSound.new(motif);

		this.assertEquals(bird.amp, 0.35);
		this.assertEquals(bird.brightness, 0.5);
		this.assertEquals(bird.loop, false);
	}
}
