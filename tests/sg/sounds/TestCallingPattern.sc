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
}
