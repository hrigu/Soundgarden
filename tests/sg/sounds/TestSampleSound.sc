// Test für SampleSound. Ausführen über run_tests.scd.
TestSampleSound : UnitTest {

	prTestSamplePath {
		^Platform.defaultTempDir +/+ "soundgarden_sample_sound_test.wav"
	}

	prWriteTestSample {
		SoundFile.writeArray([0.0, 0.5, -1.0, 0.25], this.prTestSamplePath,
			headerFormat: "WAV", sampleFormat: "float", sampleRate: 4);
	}

	test_editableParamsListsRatePhaseAmpStartFracDuration {
		var keys = SampleSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\rate, \phase, \amp, \startFrac, \duration],
			"editableParams deckt alle live im GUI bearbeitbaren Klangparameter ab, " ++
			"inklusive Ausschnitt-Konfiguration aus Intent 53 (Intent 43)");
	}

	test_editableParamsPairsKeyWithControlSpec {
		SampleSound.editableParams.do { |pair|
			this.assert(pair[1].isKindOf(ControlSpec),
				"jeder Eintrag ist [key, ControlSpec] -- " ++ pair[0]);
		};
	}

	// startFrac/duration (Intent 53) -- reine Konstruktor-/Accessor-Logik, kein Server nötig
	// (wie bei rate/phase/amp bereits der Fall).
	test_startFracAndDurationDefaultToZeroForOldBehaviour {
		var sound = SampleSound.new("irrelevant/path.wav");

		this.assertEquals(sound.startFrac, 0,
			"ohne Angabe beginnt die Wiedergabe wie bisher am Sample-Anfang");
		this.assertEquals(sound.duration, 0,
			"duration = 0 bedeutet 'kein Cutoff' -- Kompatibilität mit dem bisherigen Verhalten");
	}

	test_startFracAndDurationCanBeConfigured {
		var sound = SampleSound.new("irrelevant/path.wav", startFrac: 0.1, duration: 0.5);

		this.assertEquals(sound.startFrac, 0.1);
		this.assertEquals(sound.duration, 0.5);
	}

	test_sampleFileNameUsesOnlyLastPathComponent {
		var sound = SampleSound.new("/tmp/foo/bar/kick.wav");

		this.assertEquals(sound.sampleFileName, "kick.wav");
	}

	test_samplePreviewReadsFileNameDurationAndNormalizedPeaks {
		var sound;
		var preview;

		this.prWriteTestSample;
		sound = SampleSound.new(this.prTestSamplePath);
		preview = sound.samplePreview(4);

		this.assertEquals(preview[\fileName], "soundgarden_sample_sound_test.wav");
		this.assertEquals(preview[\bucketCount], 4);
		this.assertEquals(preview[\numFrames], 4);
		this.assertEquals(preview[\duration], 1.0);
		this.assertEquals(preview[\peaks], [0.0, 0.5, 1.0, 0.25],
			"ein Bucket pro Frame macht die kleine Testdatei direkt sichtbar");
		this.assertEquals(preview[\error], nil);
	}

	test_samplePreviewCachesPerBucketCount {
		var sound;
		var first;
		var second;
		var third;

		this.prWriteTestSample;
		sound = SampleSound.new(this.prTestSamplePath);
		first = sound.samplePreview(4);
		second = sound.samplePreview(4);
		third = sound.samplePreview(2);

		this.assert(first === second,
			"gleicher bucketCount soll dieselbe gecachte Vorschau zurueckgeben");
		this.assert((first === third).not,
			"anderer bucketCount soll einen eigenen Cache-Eintrag bekommen");
	}

	test_samplePreviewReturnsFallbackForMissingFile {
		var preview = SampleSound.new("/definitely/missing/sample.wav").samplePreview(3);

		this.assertEquals(preview[\fileName], "sample.wav");
		this.assertEquals(preview[\duration], 0.0);
		this.assertEquals(preview[\numFrames], 0);
		this.assertEquals(preview[\peaks], [0.0, 0.0, 0.0]);
		this.assert(preview[\error].notNil,
			"fehlende Datei soll eine lesbare Fallback-Info statt eines Fehlers liefern");
	}
}
