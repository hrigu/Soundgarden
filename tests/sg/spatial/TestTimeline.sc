// Test für Timeline. Ausführen über run_tests.scd. Reine sclang-Logik, kein Server nötig —
// tick(dt) wird hier direkt aufgerufen statt über eine laufende Routine (play), analog zu
// Orchestra>>tick (TestOrchestra.sc).
TestTimeline : UnitTest {

	test_cueFiresExactlyOnceWhenElapsedReachesItsTime {
		var timeline = Timeline.new(10);
		var log = List.new;

		timeline.at(1.0, { log.add(\fired) });

		timeline.tick(0.5); // elapsed = 0.5, noch nicht erreicht
		this.assertEquals(log.asArray, []);

		timeline.tick(0.5); // elapsed = 1.0, genau erreicht
		this.assertEquals(log.asArray, [\fired]);

		timeline.tick(0.5); // elapsed = 1.5, darf nicht nochmal feuern
		this.assertEquals(log.asArray, [\fired]);
	}

	test_multipleCuesFireIndependentlyAtTheirOwnTime {
		var timeline = Timeline.new(10);
		var log = List.new;

		timeline.at(1.0, { log.add(\early) });
		timeline.at(2.0, { log.add(\late) });

		timeline.tick(1.0); // elapsed = 1.0
		this.assertEquals(log.asArray, [\early]);

		timeline.tick(1.0); // elapsed = 2.0
		this.assertEquals(log.asArray, [\early, \late]);
	}

	test_cueScheduledAtTimeZeroFiresOnFirstTick {
		var timeline = Timeline.new(10);
		var log = List.new;

		timeline.at(0, { log.add(\fired) });
		timeline.tick(0.1);

		this.assertEquals(log.asArray, [\fired]);
	}

	test_rampInterpolatesLinearlyBetweenStartAndEndTime {
		var timeline = Timeline.new(10);
		var log = List.new;

		timeline.ramp(1.0, 3.0, 0.0, 1.0, { |v| log.add(v) });

		timeline.tick(1.0); // elapsed = 1.0 -> frac 0
		timeline.tick(1.0); // elapsed = 2.0 -> frac 0.5
		timeline.tick(1.0); // elapsed = 3.0 -> frac 1

		this.assertEquals(log.asArray, [0.0, 0.5, 1.0]);
	}

	test_rampDoesNotCallSetterBeforeStartTime {
		var timeline = Timeline.new(10);
		var log = List.new;

		timeline.ramp(2.0, 4.0, 0.0, 1.0, { |v| log.add(v) });
		timeline.tick(1.0); // elapsed = 1.0, vor startTime

		this.assertEquals(log.asArray, []);
	}

	test_rampClipsToToValueAfterEndTime {
		var timeline = Timeline.new(10);
		var log = List.new;

		timeline.ramp(0.0, 1.0, 0.0, 10.0, { |v| log.add(v) });
		timeline.tick(1.0); // elapsed = 1.0 -> frac 1, Wert 10
		timeline.tick(1.0); // elapsed = 2.0, weit über endTime hinaus

		this.assertEquals(log.asArray, [10.0, 10.0],
			"nach endTime bleibt der Wert bei toValue, statt weiter zu extrapolieren");
	}

	test_isDoneBecomesTrueOnceElapsedReachesTotalDur {
		var timeline = Timeline.new(2);

		this.assertEquals(timeline.isDone, false);
		timeline.tick(1.0);
		this.assertEquals(timeline.isDone, false);
		timeline.tick(1.0);
		this.assertEquals(timeline.isDone, true);
	}

	test_elapsedAccumulatesAcrossTicks {
		var timeline = Timeline.new(10);

		timeline.tick(0.3);
		timeline.tick(0.4);

		this.assertFloatEquals(timeline.elapsed, 0.7);
	}
}
