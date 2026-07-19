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

	test_editableParamsOnlyExposeAudibleOneShotParams {
		var keys = OneShotSampleSound.editableParams.collect { |pair| pair[0] };

		this.assertEquals(keys, [\amp, \startFrac, \duration],
			"OneShotSampleSound zeigt nur Parameter, die der One-Shot-Synth wirklich nutzt");
	}

	test_buildFromSavedParamsUsesAudibleOneShotParams {
		var sound = OneShotSampleSound.buildFromSavedParams((path: "irrelevant/path.wav",
			amp: 0.6, startFrac: 0.2, duration: 0.5, rate: 7, phase: 0.4));

		this.assertEquals(sound.path, "irrelevant/path.wav");
		this.assertEquals(sound.amp, 0.6);
		this.assertEquals(sound.startFrac, 0.2);
		this.assertEquals(sound.duration, 0.5);
	}

	test_activeVoiceCountIsZeroBeforePlay {
		var sound = OneShotSampleSound.new("irrelevant/path.wav");

		this.assertEquals(sound.activeVoiceCount, 0,
			"vor dem ersten trigger gibt es keine aktiven One-Shot-Voices");
	}

	test_voiceCleanupDelayCoversAtLeastShortTail {
		var shortSound = OneShotSampleSound.new("irrelevant/path.wav", duration: 0.03);
		var longerSound = OneShotSampleSound.new("irrelevant/path.wav", duration: 0.4);

		this.assertFloatEquals(shortSound.voiceCleanupDelay, 0.2,
			"sehr kurze Samples bekommen genug Zeit fuer einen kleinen Ausklang");
		this.assertFloatEquals(longerSound.voiceCleanupDelay, 0.5,
			"laengere Samples bleiben bis nach ihrer Duration in der Active-Liste");
	}
}
