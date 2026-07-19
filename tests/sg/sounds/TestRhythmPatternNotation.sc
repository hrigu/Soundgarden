// Test für RhythmPatternNotation. Ausführen über run_tests.scd.
TestRhythmPatternNotation : UnitTest {

	assertEvent { |event, gate, dur, message|
		this.assertEquals(event[\gate], gate, message);
		this.assertFloatEquals(event[\dur], dur, message);
	}

	test_eventsSplitsDoubleBarIntoEqualLengthBars {
		var events = RhythmPatternNotation.events("x...|....||....|x...", barDur: 4);
		var totalDur = events.collect { |event| event[\dur] }.sum;
		var hits = events.select { |event| event[\gate] == 1 };

		this.assertEquals(events.size, 16,
			"zwei Takte mit je zwei 4er-Segmenten ergeben 16 Events");
		this.assertFloatEquals(totalDur, 8,
			"zwei logische Takte dauern zusammen 2 * barDur");
		this.assertFloatEquals(hits[0][\dur], 0.5,
			"jedes 4-Zeichen-Segment teilt seinen halben Takt in 4 Schritte");
		this.assertFloatEquals(hits[1][\dur], 0.5,
			"der zweite Takt wird nicht auf den ersten Takt zusammengerechnet");
	}

	test_eventsMakesSegmentsWithinBarEqualLength {
		var events = RhythmPatternNotation.events("x...|x.", barDur: 4);
		var firstSegmentDur = events[0..3].collect { |event| event[\dur] }.sum;
		var secondSegmentDur = events[4..5].collect { |event| event[\dur] }.sum;

		this.assertFloatEquals(firstSegmentDur, 2,
			"erstes Segment bekommt die halbe Taktdauer");
		this.assertFloatEquals(secondSegmentDur, 2,
			"zweites Segment bekommt trotz weniger Zeichen dieselbe Segmentdauer");
		this.assertEvent(events[0], 1, 0.5,
			"vier Zeichen im ersten Segment ergeben feinere Schritte");
		this.assertEvent(events[4], 1, 1.0,
			"zwei Zeichen im zweiten Segment ergeben gröbere Schritte");
	}

	test_eventsKeepsBarLengthWithVariableSegmentLengths {
		var events = RhythmPatternNotation.events("x...|x.|....|x.....", barDur: 4);
		var totalDur = events.collect { |event| event[\dur] }.sum;
		var hitDurs = events.select { |event| event[\gate] == 1 }.collect { |event| event[\dur] };

		this.assertFloatEquals(totalDur, 4,
			"variable Segmentlängen verändern die Taktdauer nicht");
		this.assertFloatEquals(hitDurs[0], 0.25,
			"4 Zeichen in einem Beat ergeben 16tel-artige Dauer");
		this.assertFloatEquals(hitDurs[1], 0.5,
			"2 Zeichen in einem Beat ergeben gröbere Dauer");
		this.assertFloatEquals(hitDurs[2], 1 / 6,
			"6 Zeichen in einem Beat ergeben feinere Dauer");
	}

	test_eventStreamRepeatsEvents {
		var stream = RhythmPatternNotation.eventStream("x.", barDur: 2).asStream;

		this.assertEvent(stream.next, 1, 1,
			"erstes Event ist der Schlag");
		this.assertEvent(stream.next, 0, 1,
			"zweites Event ist die Pause");
		this.assertEvent(stream.next, 1, 1,
			"der Stream wiederholt das Pattern endlos");
	}

	test_eventsIgnoresTrailingSegmentSeparator {
		this.assertEquals(
			RhythmPatternNotation.events("xxxx|x.x.x.", barDur: 4),
			RhythmPatternNotation.events("xxxx|x.x.x.|", barDur: 4),
			"ein abschließendes | erzeugt kein zusätzliches leeres Segment"
		);
	}

	test_eventsIgnoresTrailingBarSeparator {
		this.assertEquals(
			RhythmPatternNotation.events("xxxx||x.x.", barDur: 4),
			RhythmPatternNotation.events("xxxx||x.x.||", barDur: 4),
			"ein abschließendes || erzeugt keinen zusätzlichen leeren Takt"
		);
		this.assertEquals(
			RhythmPatternNotation.events("xxxx", barDur: 4),
			RhythmPatternNotation.events("xxxx||", barDur: 4),
			"ein einzelner Takt mit abschließendem || bleibt derselbe Takt"
		);
	}

	test_eventsIgnoresLeadingSegmentSeparator {
		this.assertEquals(
			RhythmPatternNotation.events("xxxx|x.x.x.", barDur: 4),
			RhythmPatternNotation.events("|xxxx|x.x.x.", barDur: 4),
			"ein führendes | erzeugt kein leeres Anfangssegment"
		);
	}

	test_eventsIgnoresLeadingBarSeparator {
		this.assertEquals(
			RhythmPatternNotation.events("xxxx||x.x.", barDur: 4),
			RhythmPatternNotation.events("||xxxx||x.x.", barDur: 4),
			"ein führendes || erzeugt keinen leeren Anfangstakt"
		);
		this.assertEquals(
			RhythmPatternNotation.events("xxxx", barDur: 4),
			RhythmPatternNotation.events("||xxxx", barDur: 4),
			"ein einzelner Takt mit führendem || bleibt derselbe Takt"
		);
	}
}
