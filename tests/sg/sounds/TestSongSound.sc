// Test für SongSound. Ausführen über run_tests.scd.
TestSongSound : UnitTest {

	test_editableParamsListsRateAndAmp {
		var keys = SongSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\rate, \amp],
			"editableParams deckt die sinnvollen live bearbeitbaren Song-Parameter ab");
	}

	test_editableParamsPairsKeyWithControlSpec {
		SongSound.editableParams.do { |pair|
			this.assert(pair[1].isKindOf(ControlSpec),
				"jeder Eintrag ist [key, ControlSpec] -- " ++ pair[0]);
		};
	}

	test_requiredConstructorArgsListsPath {
		this.assertEquals(SongSound.requiredConstructorArgs, [\path],
			"path ist Pflicht für *new, aber kein editierbarer Slider-Parameter (Intent 46)");
	}

	test_buildFromSavedParamsReconstructsPathAndEditableParams {
		var sound = SongSound.buildFromSavedParams((path: "irrelevant/song.wav", rate: 1.2,
			amp: 0.5));

		this.assertEquals(sound.path, "irrelevant/song.wav");
		this.assertEquals(sound.rate, 1.2);
		this.assertEquals(sound.amp, 0.5);
	}
}
