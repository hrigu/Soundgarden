// Test für RhythmPlaygroundPercussionSamples. Ausführen über run_tests.scd.
TestRhythmPlaygroundPercussionSamples : UnitTest {

	test_relativePathsCoverPlaygroundLayers {
		var mapping = RhythmPlaygroundPercussionSamples.relativePaths;

		[\kick, \sub, \snare, \clap, \chihat, \ohihat, \perc, \texture].do { |layerName|
			this.assert(mapping[layerName].notNil,
				"Percussion-Mapping muss Layer % abdecken".format(layerName));
		};
	}

	test_mappedFilesExist {
		RhythmPlaygroundPercussionSamples.relativePaths.keysValuesDo { |layerName, relativePath|
			this.assert(File.exists(relativePath),
				"Sample-Datei für % existiert: %".format(layerName, relativePath));
		};
	}

	test_soundParamsForBuildsAbsolutePathAndDefaults {
		var params = RhythmPlaygroundPercussionSamples.soundParamsFor(\kick, "/repo/root",
			amp: 0.25, duration: 0.12);

		this.assert(params[\path].beginsWith("/repo/root/sounds/instruments/Percussion/"),
			"Pfad wird relativ zur Repo-Wurzel aufgebaut");
		this.assertEquals(params[\amp], 1.0,
			"Percussion-Samples werden gegenueber den leisen Cricket-Amps angehoben");
		this.assertEquals(params[\duration], 0.12);
		this.assertEquals(params[\startFrac], 0);
	}

	test_soundParamsForAcceptsStartFrac {
		var params = RhythmPlaygroundPercussionSamples.soundParamsFor(\kick, "/repo/root",
			startFrac: 0.35);

		this.assertEquals(params[\startFrac], 0.35);
	}

	test_unknownLayerReturnsNil {
		this.assertEquals(RhythmPlaygroundPercussionSamples.pathFor(\missing, "/repo/root"), nil);
		this.assertEquals(RhythmPlaygroundPercussionSamples.soundParamsFor(\missing, "/repo/root"), nil);
	}

	test_soundParamsForScalesAmpButClipsAtOne {
		var quietParams = RhythmPlaygroundPercussionSamples.soundParamsFor(\kick, "/repo/root",
			amp: 0.1);
		var loudParams = RhythmPlaygroundPercussionSamples.soundParamsFor(\kick, "/repo/root",
			amp: 0.5);

		this.assertEquals(quietParams[\amp], 0.4);
		this.assertEquals(loudParams[\amp], 1.0,
			"Sample-Amp bleibt im gueltigen Synth-Parameterbereich");
	}
}
