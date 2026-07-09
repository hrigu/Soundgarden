// Test für SampleSound. Ausführen über run_tests.scd.
TestSampleSound : UnitTest {

	test_editableParamsListsRatePhaseAmp {
		var keys = SampleSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\rate, \phase, \amp],
			"editableParams deckt alle live im GUI bearbeitbaren Klangparameter ab (Intent 43)");
	}

	test_editableParamsPairsKeyWithControlSpec {
		SampleSound.editableParams.do { |pair|
			this.assert(pair[1].isKindOf(ControlSpec),
				"jeder Eintrag ist [key, ControlSpec] -- " ++ pair[0]);
		};
	}
}
