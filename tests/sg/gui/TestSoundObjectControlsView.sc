// Test für sampleSelectionRangeFor, extrahiert aus TestSpatialControlPanel.sc (Intent 55).
// Reine Berechnung ohne GUI-Bezug -- parentView/presetsDir/soloMuteController bleiben nil,
// sampleSelectionRangeFor greift nicht darauf zu. Ausführen über run_tests.scd.
TestSoundObjectControlsView : UnitTest {

	test_sampleSelectionRangeUsesDurationRelativeToPreview {
		var view = SoundObjectControlsView.new(nil, 0, nil, nil);
		var sound = SampleSound.new("irrelevant.wav", startFrac: 0.25, duration: 1.0);
		var range = view.sampleSelectionRangeFor(sound, (duration: 4.0));

		this.assertEquals(range, [0.25, 0.5],
			"die Ausschnittsmarkierung endet bei startFrac + duration / sampleDuration");
	}

	test_sampleSelectionRangeWithoutCutoffRunsToEnd {
		var view = SoundObjectControlsView.new(nil, 0, nil, nil);
		var sound = SampleSound.new("irrelevant.wav", startFrac: 0.4, duration: 0);
		var range = view.sampleSelectionRangeFor(sound, (duration: 4.0));

		this.assertEquals(range, [0.4, 1.0],
			"duration = 0 bedeutet im GUI: Markierung ab Startpunkt bis Sample-Ende");
	}

	test_sampleSelectionRangeClipsAtSampleEnd {
		var view = SoundObjectControlsView.new(nil, 0, nil, nil);
		var sound = SampleSound.new("irrelevant.wav", startFrac: 0.8, duration: 2.0);
		var range = view.sampleSelectionRangeFor(sound, (duration: 4.0));

		this.assertEquals(range, [0.8, 1.0],
			"reicht die konfigurierte Dauer ueber das Sample-Ende hinaus, wird bei 1.0 gekappt");
	}
}
