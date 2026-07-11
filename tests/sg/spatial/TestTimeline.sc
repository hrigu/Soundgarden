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

	// staggeredTimes (Intent 58): allgemeine Utility für "gestaffelte Einsatzzeitpunkte mit
	// wachsender Dichte + Jitter", extrahiert aus dawn_chorus.scd (dort für Vogel- und
	// Grillen-Einsatzplan dupliziert). jitter: 0 macht die Tests deterministisch.
	test_staggeredTimesWithoutJitterIsLinearlySpaced {
		var times = Timeline.staggeredTimes(count: 4, spanDur: 8, startTime: 0, jitter: 0,
			power: 1);

		this.assertEquals(times, [2.0, 4.0, 6.0, 8.0]);
	}

	test_staggeredTimesShiftsByStartTime {
		var times = Timeline.staggeredTimes(count: 2, spanDur: 4, startTime: 10, jitter: 0);

		this.assertEquals(times, [12.0, 14.0]);
	}

	test_staggeredTimesAppliesPowerCurveToStaggerLaterEventsCloser {
		var times = Timeline.staggeredTimes(count: 2, spanDur: 10, startTime: 0, jitter: 0,
			power: 2);

		this.assertEquals(times, [2.5, 10.0]);
	}

	test_staggeredTimesClipsToClipMax {
		var times = Timeline.staggeredTimes(count: 3, spanDur: 10, startTime: 0, jitter: 0,
			clipMax: 5);

		this.assertFloatEquals(times[0], 10.0 / 3);
		this.assertEquals(times[1], 5.0);
		this.assertEquals(times[2], 5.0);
	}

	test_staggeredTimesStaysWithinClipBoundsDespiteJitter {
		var times = Timeline.staggeredTimes(count: 20, spanDur: 10, startTime: 5, jitter: 50,
			clipMax: 12);

		times.do { |t|
			this.assert(t >= 5 and: { t <= 12 }, "Zeitpunkt % ausserhalb [5, 12]".format(t));
		};
	}
}
