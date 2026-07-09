// Test für InsectSound. Ausführen über run_tests.scd.
TestInsectSound : UnitTest {

	test_editableParamsListsAllSoundParameters {
		var keys = InsectSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys,
			[\wingRate, \wingDuty, \ringFreq1, \ringFreq2, \ringDecay1, \ringDecay2, \amp],
			"editableParams deckt alle live im GUI bearbeitbaren Klangparameter ab (Intent 43)");
	}

	test_editableParamsPairsKeyWithControlSpec {
		InsectSound.editableParams.do { |pair|
			this.assert(pair[1].isKindOf(ControlSpec),
				"jeder Eintrag ist [key, ControlSpec] -- " ++ pair[0]);
		};
	}

	test_cricketSoundInheritsEditableParams {
		this.assertEquals(CricketSound.editableParams, InsectSound.editableParams,
			"CricketSound nutzt dieselben Parameter/Ranges wie InsectSound, kein Override nötig");
	}
}
