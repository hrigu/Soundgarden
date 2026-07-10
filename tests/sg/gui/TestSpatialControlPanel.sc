// Test-Double für SpatialControlPanel: kein echtes GUI, kein Server. Solo/Mute-Logik ist
// nach SoloMuteController ausgelagert (siehe tests/sg/gui/TestSoloMuteController.sc, Intent
// 55) — hier bleiben nur noch die Tests für sampleSelectionRangeFor, die keine Orchestra
// brauchen.
FakeRoomForSpatialControlPanelTest {
	var <>orchestra;

	*new { |anOrchestra| ^super.new.init(anOrchestra) }

	init { |anOrchestra| orchestra = anOrchestra }
}

// Test für die verbleibende GUI-nahe Logik von SpatialControlPanel. Ausführen über
// run_tests.scd.
TestSpatialControlPanel : UnitTest {

	test_sampleSelectionRangeUsesDurationRelativeToPreview {
		var panel = SpatialControlPanel.new(FakeRoomForSpatialControlPanelTest.new);
		var sound = SampleSound.new("irrelevant.wav", startFrac: 0.25, duration: 1.0);
		var range = panel.sampleSelectionRangeFor(sound, (duration: 4.0));

		this.assertEquals(range, [0.25, 0.5],
			"die Ausschnittsmarkierung endet bei startFrac + duration / sampleDuration");
	}

	test_sampleSelectionRangeWithoutCutoffRunsToEnd {
		var panel = SpatialControlPanel.new(FakeRoomForSpatialControlPanelTest.new);
		var sound = SampleSound.new("irrelevant.wav", startFrac: 0.4, duration: 0);
		var range = panel.sampleSelectionRangeFor(sound, (duration: 4.0));

		this.assertEquals(range, [0.4, 1.0],
			"duration = 0 bedeutet im GUI: Markierung ab Startpunkt bis Sample-Ende");
	}

	test_sampleSelectionRangeClipsAtSampleEnd {
		var panel = SpatialControlPanel.new(FakeRoomForSpatialControlPanelTest.new);
		var sound = SampleSound.new("irrelevant.wav", startFrac: 0.8, duration: 2.0);
		var range = panel.sampleSelectionRangeFor(sound, (duration: 4.0));

		this.assertEquals(range, [0.8, 1.0],
			"reicht die konfigurierte Dauer ueber das Sample-Ende hinaus, wird bei 1.0 gekappt");
	}
}
