// Test-Doubles für SoloMuteController: kein echtes GUI, kein Server, nur nachvollziehbare
// amp-Set-Aufrufe auf Fake-Synths.
FakeSynthForSoloMuteControllerTest {
	var <log;

	*new { |aLog| ^super.new.init(aLog) }

	init { |aLog| log = aLog }

	set { |key, value| log.add([key, value]) }
}

FakeSoundForSoloMuteControllerTest {
	var <>amp;
	var <synth;

	*new { |amp = 0.5, synth| ^super.new.init(amp, synth) }

	init { |anAmp, aSynth|
		amp = anAmp;
		synth = aSynth;
	}
}

FakeSoundObjectForSoloMuteControllerTest {
	var <>sound;

	*new { |aSound| ^super.new.init(aSound) }

	init { |aSound| sound = aSound }
}

FakeOrchestraForSoloMuteControllerTest {
	var <>soundObjects;

	*new { |someSoundObjects| ^super.new.init(someSoundObjects) }

	init { |someSoundObjects| soundObjects = someSoundObjects }
}

// Test für die GUI-lose Solo-Zustandslogik (extrahiert aus SpatialControlPanel, Intent 55).
// Ausführen über run_tests.scd.
TestSoloMuteController : UnitTest {

	test_selectionChangeInSoloRestoresOriginalAmpBeforeMutingPreviousSelection {
		var firstLog = List.new;
		var secondLog = List.new;
		var first = FakeSoundObjectForSoloMuteControllerTest.new(
			FakeSoundForSoloMuteControllerTest.new(0.2, FakeSynthForSoloMuteControllerTest.new(firstLog))
		);
		var second = FakeSoundObjectForSoloMuteControllerTest.new(
			FakeSoundForSoloMuteControllerTest.new(0.8, FakeSynthForSoloMuteControllerTest.new(secondLog))
		);
		var controller = SoloMuteController.new(
			FakeOrchestraForSoloMuteControllerTest.new([first, second])
		);
		var selected;

		selected = first;
		controller.applySoloState(selected); // entspricht selectSoundObject(first) ohne Solo
		controller.setSoloSelectedOnly(true, selected);
		controller.restoreIfActive; // entspricht selectSoundObject(second): vor Wechsel restaurieren
		selected = second;
		controller.applySoloState(selected);

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
		var first = FakeSoundObjectForSoloMuteControllerTest.new(
			FakeSoundForSoloMuteControllerTest.new(0.2, FakeSynthForSoloMuteControllerTest.new(firstLog))
		);
		var second = FakeSoundObjectForSoloMuteControllerTest.new(
			FakeSoundForSoloMuteControllerTest.new(0.8, FakeSynthForSoloMuteControllerTest.new(secondLog))
		);
		var controller = SoloMuteController.new(
			FakeOrchestraForSoloMuteControllerTest.new([first, second])
		);

		controller.applySoloState(first); // entspricht selectSoundObject(first)
		controller.setSoloSelectedOnly(true, first);
		controller.setSoloSelectedOnly(false, first);

		this.assertEquals(firstLog.last, [\amp, 0.2],
			"beim Ausschalten von Solo kehrt das selektierte Objekt zu seiner Instanz-amp zurück");
		this.assertEquals(secondLog.last, [\amp, 0.8],
			"beim Ausschalten von Solo kehrt auch ein zuvor stummes Objekt zu seiner " ++
			"Instanz-amp zurück");
	}

	test_enablingSoloWithoutSelectionKeepsConsistentState {
		var log = List.new;
		var soundObject = FakeSoundObjectForSoloMuteControllerTest.new(
			FakeSoundForSoloMuteControllerTest.new(0.4, FakeSynthForSoloMuteControllerTest.new(log))
		);
		var controller = SoloMuteController.new(
			FakeOrchestraForSoloMuteControllerTest.new([soundObject])
		);

		controller.setSoloSelectedOnly(true, nil);

		this.assertEquals(log.asArray, [[\amp, 0.4]],
			"ohne Auswahl wird nichts dauerhaft stummgeschaltet, sondern der Originalpegel " ++
			"wiederhergestellt");
	}
}
