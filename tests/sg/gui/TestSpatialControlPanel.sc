// Test-Doubles für SpatialControlPanel-Solo-Logik: kein echtes GUI, kein Server, nur
// nachvollziehbare amp-Set-Aufrufe auf Fake-Synths.
FakeSynthForSpatialControlPanelTest {
	var <log;

	*new { |aLog| ^super.new.init(aLog) }

	init { |aLog| log = aLog }

	set { |key, value| log.add([key, value]) }
}

FakeSoundForSpatialControlPanelTest {
	var <>amp;
	var <synth;

	*new { |amp = 0.5, synth| ^super.new.init(amp, synth) }

	init { |anAmp, aSynth|
		amp = anAmp;
		synth = aSynth;
	}
}

FakeSoundObjectForSpatialControlPanelTest {
	var <>sound;

	*new { |aSound| ^super.new.init(aSound) }

	init { |aSound| sound = aSound }
}

FakeOrchestraForSpatialControlPanelTest {
	var <>soundObjects;

	*new { |someSoundObjects| ^super.new.init(someSoundObjects) }

	init { |someSoundObjects| soundObjects = someSoundObjects }
}

FakeRoomForSpatialControlPanelTest {
	var <>orchestra;

	*new { |anOrchestra| ^super.new.init(anOrchestra) }

	init { |anOrchestra| orchestra = anOrchestra }
}

// Test für die GUI-nahe Solo-Zustandslogik. Ausführen über run_tests.scd.
TestSpatialControlPanel : UnitTest {

	test_selectionChangeInSoloRestoresOriginalAmpBeforeMutingPreviousSelection {
		var firstLog = List.new;
		var secondLog = List.new;
		var first = FakeSoundObjectForSpatialControlPanelTest.new(
			FakeSoundForSpatialControlPanelTest.new(0.2, FakeSynthForSpatialControlPanelTest.new(firstLog))
		);
		var second = FakeSoundObjectForSpatialControlPanelTest.new(
			FakeSoundForSpatialControlPanelTest.new(0.8, FakeSynthForSpatialControlPanelTest.new(secondLog))
		);
		var panel = SpatialControlPanel.new(
			FakeRoomForSpatialControlPanelTest.new(
				FakeOrchestraForSpatialControlPanelTest.new([first, second])
			)
		);

		panel.selectSoundObject(first);
		panel.setSoloSelectedOnly(true);
		panel.selectSoundObject(second);

		this.assertEquals(firstLog.asArray, [[\amp, 0.2], [\amp, 0.2], [\amp, 0.2], [\amp, 0]],
			"beim Wechsel wird zuerst der komplette Originalzustand restauriert; erst danach " ++
			"wird das zuvor selektierte Objekt als Nicht-Selektion stummgeschaltet");
		this.assertEquals(secondLog.asArray, [[\amp, 0.8], [\amp, 0], [\amp, 0.8], [\amp, 0.8]],
			"auch ein zuvor stummes Objekt wird beim Wechsel erst restauriert und bleibt danach " ++
			"als neue Selektion hörbar");
	}

	test_disablingSoloRestoresAllObjectAmps {
		var firstLog = List.new;
		var secondLog = List.new;
		var first = FakeSoundObjectForSpatialControlPanelTest.new(
			FakeSoundForSpatialControlPanelTest.new(0.2, FakeSynthForSpatialControlPanelTest.new(firstLog))
		);
		var second = FakeSoundObjectForSpatialControlPanelTest.new(
			FakeSoundForSpatialControlPanelTest.new(0.8, FakeSynthForSpatialControlPanelTest.new(secondLog))
		);
		var panel = SpatialControlPanel.new(
			FakeRoomForSpatialControlPanelTest.new(
				FakeOrchestraForSpatialControlPanelTest.new([first, second])
			)
		);

		panel.selectSoundObject(first);
		panel.setSoloSelectedOnly(true);
		panel.setSoloSelectedOnly(false);

		this.assertEquals(firstLog.last, [\amp, 0.2],
			"beim Ausschalten von Solo kehrt das selektierte Objekt zu seiner Instanz-amp zurück");
		this.assertEquals(secondLog.last, [\amp, 0.8],
			"beim Ausschalten von Solo kehrt auch ein zuvor stummes Objekt zu seiner " ++
			"Instanz-amp zurück");
	}

	test_enablingSoloWithoutSelectionKeepsConsistentState {
		var log = List.new;
		var soundObject = FakeSoundObjectForSpatialControlPanelTest.new(
			FakeSoundForSpatialControlPanelTest.new(0.4, FakeSynthForSpatialControlPanelTest.new(log))
		);
		var panel = SpatialControlPanel.new(
			FakeRoomForSpatialControlPanelTest.new(
				FakeOrchestraForSpatialControlPanelTest.new([soundObject])
			)
		);

		panel.setSoloSelectedOnly(true);

		this.assertEquals(log.asArray, [[\amp, 0.4]],
			"ohne Auswahl wird nichts dauerhaft stummgeschaltet, sondern der Originalpegel " ++
			"wiederhergestellt");
	}

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
