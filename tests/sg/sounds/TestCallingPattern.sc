// Test für CallingPattern. Ausführen über run_tests.scd.
TestCallingPattern : UnitTest {

	test_nextReturnsFirstSegmentFirst {
		var pattern = CallingPattern.new([[0.15, true], [0.05, false]]);

		this.assertEquals(pattern.next, [0.15, true]);
	}

	test_nextCyclesThroughSegmentsInOrder {
		var pattern = CallingPattern.new([[0.15, true], [0.05, false]]);

		pattern.next;
		this.assertEquals(pattern.next, [0.05, false]);
	}

	test_nextWrapsAroundAfterLastSegment {
		var pattern = CallingPattern.new([[0.15, true], [0.05, false]]);

		pattern.next;
		pattern.next;

		this.assertEquals(pattern.next, [0.15, true],
			"nach dem letzten Segment geht es wieder beim ersten weiter");
	}

	test_nextWithSingleSegmentAlwaysReturnsIt {
		var pattern = CallingPattern.new([[0.3, true]]);

		this.assertEquals(pattern.next, [0.3, true]);
		this.assertEquals(pattern.next, [0.3, true]);
	}

	test_nextWithManySegmentsWrapsAfterFullCycle {
		var pattern = CallingPattern.new([[0.1, true], [0.1, false], [0.2, true], [0.4, false]]);
		var collected = 5.collect { pattern.next };

		this.assertEquals(collected, [
			[0.1, true], [0.1, false], [0.2, true], [0.4, false], [0.1, true]
		]);
	}

	// fromDurations — Kurzschreibweise: reine Dauern, per Konvention beginnt das erste
	// Segment "an", dann wechselt es ab.
	test_fromDurationsAlternatesStartingWithOn {
		var pattern = CallingPattern.fromDurations([0.02, 0.03, 0.02, 0.2]);

		this.assertEquals(pattern.next, [0.02, true]);
		this.assertEquals(pattern.next, [0.03, false]);
		this.assertEquals(pattern.next, [0.02, true]);
		this.assertEquals(pattern.next, [0.2, false]);
	}

	test_fromDurationsWrapsAroundLikeNew {
		var pattern = CallingPattern.fromDurations([0.02, 0.03]);

		pattern.next;
		pattern.next;

		this.assertEquals(pattern.next, [0.02, true],
			"nach dem letzten Segment geht es wieder beim ersten (an) weiter");
	}
}
