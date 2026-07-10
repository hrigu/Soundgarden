// Test für SampleSoundConfigurator. Ausführen über run_tests.scd.
TestSampleSoundConfigurator : UnitTest {

	prTestSamplePath {
		^Platform.defaultTempDir +/+ "soundgarden_sample_sound_configurator_test.wav"
	}

	prWriteTestSample {
		SoundFile.writeArray([0.0, 0.5, -1.0, 0.25], this.prTestSamplePath,
			headerFormat: "WAV", sampleFormat: "float", sampleRate: 4);
	}

	test_prepareSetsExcerptAndAmpFromConfiguredHeuristics {
		var configurator;
		var configured;
		var sound;

		this.prWriteTestSample;
		configurator = SampleSoundConfigurator.new(shortThreshold: 0.5, targetDuration: 0.3,
			peakThreshold: 0.75, previewBucketCount: 4, baseAmp: 0.5, targetRms: 0.12,
			minGain: 0.5, maxGain: 1.4, silenceFloor: 0.0001, maxOutputPeak: 0.6);
		configured = configurator.prepare(this.prTestSamplePath, 3, 0.25);
		sound = configured[\sound];

		this.assert(sound.isKindOf(SampleSound));
		this.assertEquals(sound.rate, 3);
		this.assertEquals(sound.phase, 0.25);
		this.assertEquals(sound.startFrac, 0.5);
		this.assertEquals(sound.duration, 0.3);
		this.assertFloatEquals(configured[\excerptLevel][\rms], 0.72886898685566,
			"bei 0.3s und startFrac 0.5 landen wegen ceil() die Frames [-1.0, 0.25] im Ausschnitt");
		this.assertFloatEquals(configured[\gainFactor], 0.5,
			"das RMS-Ziel waere noch kleiner, wird aber auf minGain 0.5 geklemmt");
		this.assertFloatEquals(sound.amp, 0.25, "baseAmp 0.5 * gain 0.5");
	}

	test_configureUsesSilenceFloorInsteadOfAggressiveBoost {
		var silencePath = Platform.defaultTempDir +/+ "soundgarden_sample_sound_configurator_silence.wav";
		var configurator;
		var configured;
		var sound;

		SoundFile.writeArray([0.0, 0.0, 0.0, 0.0], silencePath,
			headerFormat: "WAV", sampleFormat: "float", sampleRate: 4);
		sound = SampleSound.new(silencePath);
		configurator = SampleSoundConfigurator.new(shortThreshold: 0.5, targetDuration: 0.3,
			peakThreshold: 0.25, previewBucketCount: 4, baseAmp: 0.5, targetRms: 0.12,
			minGain: 0.5, maxGain: 1.4, silenceFloor: 0.0001, maxOutputPeak: 0.6);
		configured = configurator.configure(sound);

		this.assertFloatEquals(configured[\rmsGain], 1.0,
			"unterhalb silenceFloor wird kein aggressives Hochziehen erzwungen");
		this.assertFloatEquals(configured[\gainFactor], 1.0);
		this.assertFloatEquals(sound.amp, 0.5);
	}
}
