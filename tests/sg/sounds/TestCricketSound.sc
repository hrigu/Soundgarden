// Test für CricketSound. Ausführen über run_tests.scd.
TestCricketSound : UnitTest {

	test_defaultsMatchResearchedValues {
		var cricket = CricketSound.new;

		this.assertEquals(cricket.wingRate, 27,
			"Silben-/Pulsrate ~25-30Hz (Intent 42-Recherche)");
		this.assertEquals(cricket.wingDuty, 0.5,
			"Silbendauer ~15-23ms bei ~33-40ms Periode -> Tastverhältnis ~0.5");
		this.assertEquals(cricket.ringFreq1, 4600,
			"dominante Trägerfrequenz ~4.6-4.8kHz (Intent 42-Recherche)");
		this.assertEquals(cricket.ringFreq2, 4700,
			"zweite Ringz-Frequenz bewusst nah bei ringFreq1 -- Grillen haben nur EIN "
			"Formant, kein zweites wie im generischen Insekt-Modell");
	}

	test_generatesPatternWhenNoneProvided {
		var cricket = CricketSound.new;

		this.assert(cricket.pattern.isKindOf(CallingPattern),
			"ohne explizites Pattern erzeugt CricketSound selbst eines über "
			"RhythmPatternCreator, statt InsectSounds nil-Default (durchgehendes Zirpen)");
	}

	test_acceptsExplicitPattern {
		var explicitPattern = CallingPattern.new([[0.1, true], [0.1, false]]);
		var cricket = CricketSound.new(pattern: explicitPattern);

		this.assertEquals(cricket.pattern, explicitPattern,
			"ein übergebenes Pattern wird nicht durch ein generiertes ersetzt");
	}

	test_makePatternRatioMatchesChirpDurationOverPeriod {
		var pattern = CricketSound.makePattern(chirpDur: 0.16, chirpPeriod: 0.32,
			numSegments: 8);
		var segments = 8.collect { pattern.next };
		var onDur = segments.select { |seg| seg[1] }.collect { |seg| seg[0] }.sum;

		this.assertEquals((onDur - 0.16).abs < 0.001, true,
			"Sound-Anteil des generierten Patterns entspricht chirpDur/chirpPeriod");
	}
}
