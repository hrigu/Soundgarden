// Test für BirdSound. Ausführen über run_tests.scd.
TestBirdSound : UnitTest {

	test_editableParamsListsAllSoundParameters {
		var keys = BirdSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\chirpRate, \baseFreq, \glideRange, \chirpDur, \amp],
			"editableParams deckt alle live im GUI bearbeitbaren Klangparameter ab (Intent 57)");
	}

	test_editableParamsPairsKeyWithControlSpec {
		BirdSound.editableParams.do { |pair|
			this.assert(pair[1].isKindOf(ControlSpec),
				"jeder Eintrag ist [key, ControlSpec] -- " ++ pair[0]);
		};
	}

	test_defaultsMatchConstructorArguments {
		var bird = BirdSound.new(chirpRate: 8, baseFreq: 4000, glideRange: 900,
			chirpDur: 0.1, amp: 0.4);

		this.assertEquals(bird.chirpRate, 8);
		this.assertEquals(bird.baseFreq, 4000);
		this.assertEquals(bird.glideRange, 900);
		this.assertEquals(bird.chirpDur, 0.1);
		this.assertEquals(bird.amp, 0.4);
	}

	test_patternDefaultsToNilForContinuousCalling {
		var bird = BirdSound.new;

		this.assertEquals(bird.pattern, nil,
			"ohne explizites Pattern ruft der Vogel durchgehend, wie InsectSound ohne Pattern");
	}

	test_acceptsExplicitPattern {
		var pattern = CallingPattern.new([[0.5, true], [0.5, false]]);
		var bird = BirdSound.new(pattern: pattern);

		this.assertEquals(bird.pattern, pattern);
	}
}
