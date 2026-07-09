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
}
