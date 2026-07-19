// Test für RhythmPlaygroundPatternPresetLibrary. Ausführen über run_tests.scd.
TestRhythmPlaygroundPatternPresetLibrary : UnitTest {

	prTestDir {
		^Platform.defaultTempDir +/+ "soundgarden_rhythm_playground_pattern_presets_test"
	}

	prExamplePreset {
		^RhythmPlaygroundPatternPresetLibrary.eventFor(
			123,
			\percussion,
			(
				kick: (active: true, pattern: "x...|....",
					sample: (amp: 0.7, startFrac: 0.1, duration: 0.25)),
				snare: (active: false, pattern: "....|x...")
			)
		)
	}

	test_eventForStoresBpmSoundModeAndLayers {
		var preset = this.prExamplePreset;

		this.assertEquals(preset[\bpm], 123);
		this.assertEquals(preset[\soundMode], \percussion);
		this.assertEquals(preset[\layers][\kick][\active], true);
		this.assertEquals(preset[\layers][\kick][\pattern], "x...|....");
		this.assertEquals(preset[\layers][\kick][\sample][\amp], 0.7);
		this.assertEquals(preset[\layers][\kick][\sample][\startFrac], 0.1);
		this.assertEquals(preset[\layers][\kick][\sample][\duration], 0.25);
	}

	test_saveThenLoadRoundTripsPresetEvent {
		var dir = this.prTestDir;
		var preset = this.prExamplePreset;
		var loaded;

		RhythmPlaygroundPatternPresetLibrary.save(dir, "roundtrip", preset);
		loaded = RhythmPlaygroundPatternPresetLibrary.load(dir, "roundtrip");

		this.assertEquals(loaded, preset);
	}

	test_loadMissingPresetReturnsNil {
		this.assertEquals(
			RhythmPlaygroundPatternPresetLibrary.load(this.prTestDir, "missing_" ++ 123456789),
			nil
		);
	}

	test_listNamesReturnsSavedPresetNames {
		var dir = this.prTestDir;
		var preset = this.prExamplePreset;
		var names;

		RhythmPlaygroundPatternPresetLibrary.save(dir, "presetA", preset);
		RhythmPlaygroundPatternPresetLibrary.save(dir, "presetB", preset);
		names = RhythmPlaygroundPatternPresetLibrary.listNames(dir);

		// Array>>includes vergleicht Strings per Identität, nicht zuverlässig per Inhalt.
		this.assert(names.any { |n| n == "presetA" });
		this.assert(names.any { |n| n == "presetB" });
	}

	test_listNamesOnMissingDirReturnsEmptyArray {
		this.assertEquals(
			RhythmPlaygroundPatternPresetLibrary.listNames(this.prTestDir ++ "_does_not_exist"),
			[]
		);
	}
}
