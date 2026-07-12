// Test für BirdMotifExamples. Ausführen über run_tests.scd.
TestBirdMotifExamples : UnitTest {

	test_blackbirdRunReturnsPlausibleMotif {
		var motif = BirdMotifExamples.blackbirdRun;

		this.assert(motif.isKindOf(BirdMotif));
		this.assert(motif.notes.size >= 6 && { motif.notes.size <= 12 },
			"perlender Lauf hat 6-12 Noten");
		motif.notes.do { |note|
			this.assert(note[1] >= 0.08 && { note[1] <= 0.15 },
				"Notendauern liegen im schnellen, gleichmäßigen Lauf-Bereich");
		};
	}

	test_wrenTrillCascadeReturnsPlausibleMotif {
		var motif = BirdMotifExamples.wrenTrillCascade;

		this.assert(motif.isKindOf(BirdMotif));
		this.assert(motif.notes.size >= 12 && { motif.notes.size <= 20 },
			"Triller-Kaskade hat viele Noten");
		motif.notes.do { |note|
			this.assert(note[1] >= 0.02 && { note[1] <= 0.05 },
				"Notendauern liegen im sehr schnellen Triller-Bereich");
		};
	}

	test_callBirdLeapsReturnsPlausibleMotif {
		var motif = BirdMotifExamples.callBirdLeaps;

		this.assert(motif.isKindOf(BirdMotif));
		this.assert(motif.notes.size >= 2 && { motif.notes.size <= 4 },
			"Rufvogel-Phrase hat wenige Noten");
		motif.notes.do { |note|
			this.assert(note[1] >= 0.15 && { note[1] <= 0.4 },
				"Notendauern liegen im längeren Rufvogel-Bereich");
		};
	}

	test_startFreqDefaultsMatchPreviousHardcodedValues {
		// Default-Werte müssen erhalten bleiben, damit bestehende Aufrufer (z.B.
		// demos/sounds/messiaenbird.scd) unverändert klingen.
		this.assertEquals(BirdMotifExamples.blackbirdRun.notes[0][0], 3200.0);
		this.assertEquals(BirdMotifExamples.wrenTrillCascade.notes[0][0], 5500.0);
		this.assertEquals(BirdMotifExamples.callBirdLeaps.notes[0][0], 2200.0);
	}

	test_startFreqIsConfigurablePerFactory {
		this.assertEquals(BirdMotifExamples.blackbirdRun(4000).notes[0][0], 4000.0);
		this.assertEquals(BirdMotifExamples.wrenTrillCascade(6000).notes[0][0], 6000.0);
		this.assertEquals(BirdMotifExamples.callBirdLeaps(1800).notes[0][0], 1800.0);
	}

	test_callBirdLeapsHasLargeIntervalJumps {
		var motif = BirdMotifExamples.callBirdLeaps;
		var freqs = motif.notes.collect { |note| note[0] };
		var hasOctaveLeap = freqs.size > 1 && {
			(1..freqs.size - 1).any { |i| (freqs[i] / freqs[i - 1]) >= 1.8
				or: { (freqs[i - 1] / freqs[i]) >= 1.8 } }
		};

		this.assert(hasOctaveLeap, "mindestens ein Sprung von ~Oktave oder mehr");
	}
}
