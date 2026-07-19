// Test für OneShotSampleSound. Ausführen über run_tests.scd.
TestOneShotSampleSound : UnitTest {

	test_inheritsSampleSoundConstructorAndRequiredPath {
		var sound = OneShotSampleSound.new("irrelevant/path.wav", amp: 0.3, startFrac: 0.2,
			duration: 0.4);

		this.assertEquals(sound.path, "irrelevant/path.wav");
		this.assertEquals(sound.amp, 0.3);
		this.assertEquals(sound.startFrac, 0.2);
		this.assertEquals(sound.duration, 0.4);
		this.assertEquals(OneShotSampleSound.requiredConstructorArgs, [\path],
			"OneShotSampleSound bleibt preset-/builder-kompatibel mit SampleSound");
	}

	test_editableParamsRemainSampleParams {
		var keys = OneShotSampleSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\rate, \phase, \amp, \startFrac, \duration],
			"OneShotSampleSound erbt die Sample-Parameter; rate/phase werden nur nicht im " ++
			"eigenen Synth benutzt");
	}
}
