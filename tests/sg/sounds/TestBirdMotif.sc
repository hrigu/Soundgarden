// Test für BirdMotif. Ausführen über run_tests.scd.
TestBirdMotif : UnitTest {

	test_notesReturnsConstructorArgument {
		var notes = [[440, 0.1, 0.8], [880, 0.2, 0.8]];
		var motif = BirdMotif.new(notes);

		this.assertEquals(motif.notes, notes);
	}

	test_totalDurSumsNoteDurations {
		var motif = BirdMotif.new([[440, 0.1, 0.8], [880, 0.2, 0.8], [660, 0.05, 0.8]]);

		this.assertFloatEquals(motif.totalDur, 0.35);
	}

	test_noteEventsReturnsCumulativeOnsetTimes {
		var motif = BirdMotif.new([[440, 0.1, 0.8], [880, 0.2, 0.8], [660, 0.05, 0.8]]);
		var events = motif.noteEvents;

		this.assertEquals(events.collect { |e| e[0] }, [440, 880, 660],
			"Tonhöhen bleiben in Reihenfolge erhalten");
		events.collect { |e| e[1] }.do { |onset, i|
			this.assertFloatEquals(onset, [0.0, 0.1, 0.3][i],
				"Einsatzzeitpunkt jeder Note ist die Summe aller vorherigen Dauern");
		};
	}

	test_fromIntervalsConvertsSemitonesToFrequency {
		// 12 Halbtöne = eine Oktave, muss die Frequenz exakt verdoppeln.
		var motif = BirdMotif.fromIntervals(440, [0, 12, -12], [0.1, 0.1, 0.1]);

		this.assertEquals(motif.notes[0][0], 440.0);
		this.assertEquals(motif.notes[1][0], 880.0);
		this.assertEquals(motif.notes[2][0], 220.0);
	}

	test_fromIntervalsUsesGivenDursAndDefaultGateRatio {
		var motif = BirdMotif.fromIntervals(440, [0, 7], [0.1, 0.2]);

		this.assertEquals(motif.notes[0][1], 0.1);
		this.assertEquals(motif.notes[1][1], 0.2);
		this.assertEquals(motif.notes[0][2], 0.8, "Default gateRatio ist 0.8");
		this.assertEquals(motif.notes[1][2], 0.8);
	}

	test_fromIntervalsAcceptsExplicitGateRatio {
		var motif = BirdMotif.fromIntervals(440, [0], [0.1], 0.5);

		this.assertEquals(motif.notes[0][2], 0.5);
	}
}
