// Test für RhythmPatternCreator. Ausführen über run_tests.scd.
TestRhythmPatternCreator : UnitTest {

	test_defaultSoundRatioSplitsRoughlyEvenly {
		var creator = RhythmPatternCreator.new(totalDur: 3, numSegments: 12);
		var pattern = creator.createNewPattern;
		var segments = 12.collect { pattern.next };
		var onDur = segments.select { |seg| seg[1] }.collect { |seg| seg[0] }.sum;
		var offDur = segments.select { |seg| seg[1].not }.collect { |seg| seg[0] }.sum;

		this.assertEquals((onDur - offDur).abs < 0.001, true,
			"bei soundRatio 0.5 sollten An- und Aus-Zeit etwa gleich gross sein");
		this.assertEquals((onDur + offDur - 3).abs < 0.001, true,
			"Gesamtdauer aller Segmente muss totalDur ergeben");
	}

	test_lowSoundRatioProducesMostlySilence {
		var creator = RhythmPatternCreator.new(totalDur: 3, numSegments: 12, soundRatio: 0.1);
		var pattern = creator.createNewPattern;
		var segments = 12.collect { pattern.next };
		var onDur = segments.select { |seg| seg[1] }.collect { |seg| seg[0] }.sum;

		this.assertEquals((onDur - 0.3).abs < 0.001, true,
			"bei soundRatio 0.1 sollte die An-Zeit ca. 10% von totalDur sein");
	}

	test_startWithSoundTrueByDefault {
		var creator = RhythmPatternCreator.new(totalDur: 3, numSegments: 12);
		var pattern = creator.createNewPattern;

		this.assertEquals(pattern.next[1], true);
	}

	test_startWithSoundFalseStartsWithSilence {
		var creator = RhythmPatternCreator.new(
			totalDur: 3, numSegments: 12, startWithSound: false
		);
		var pattern = creator.createNewPattern;

		this.assertEquals(pattern.next[1], false);
		this.assertEquals(pattern.next[1], true,
			"danach wechselt es weiterhin strikt ab");
	}

	test_createNewPatternReturnsIndependentInstances {
		var creator = RhythmPatternCreator.new(totalDur: 3, numSegments: 12);
		var patternA = creator.createNewPattern;
		var patternB = creator.createNewPattern;

		patternA.next;

		this.assertEquals(patternA.index != patternB.index, true,
			"zwei createNewPattern-Aufrufe dürfen keinen Zustand teilen");
	}
}
