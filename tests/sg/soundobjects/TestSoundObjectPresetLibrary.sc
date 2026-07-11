// Test-Doubles für SoundObjectPresetLibrary: minimaler Sound mit editableParams (analog
// FakeSoundForSoundTest, TestSound.sc) bzw. mit zusätzlichem requiredConstructorArgs-Feld
// (analog FakeConfigurableSoundForSoundTest). Movable/MoveRule sind die echten Klassen --
// reine sclang-Logik, kein Server nötig.
FakeSoundForSoundObjectPresetLibraryTest : Sound {
	var <>freq, <>amp;

	*new { |freq = 440, amp = 0.5| ^super.new.init(freq, amp) }

	init { |aFreq, aAmp| freq = aFreq; amp = aAmp }

	*editableParams {
		^[[\freq, ControlSpec(20, 8000, \exp)], [\amp, ControlSpec(0, 1, \lin)]]
	}
}

FakeSoundWithPathForSoundObjectPresetLibraryTest : Sound {
	var <path;
	var <>amp;

	*new { |path, amp = 0.5| ^super.new.init(path, amp) }

	init { |aPath, aAmp| path = aPath; amp = aAmp }

	*editableParams {
		^[[\amp, ControlSpec(0, 1, \lin)]]
	}

	*requiredConstructorArgs {
		^[\path]
	}
}

// Test für SoundObjectPresetLibrary (vormals SoundPresetLibrary, um Movable-Daten erweitert
// in Intent 46). Ausführen über run_tests.scd. Nutzt ein temporäres Testverzeichnis
// (Platform.defaultTempDir) -- reine Datei-I/O, kein Server nötig.
TestSoundObjectPresetLibrary : UnitTest {

	prTestDir {
		^Platform.defaultTempDir +/+ "soundgarden_preset_library_test"
	}

	prSoundObject { |sound, moveRule, pos = #[0, 0, 0], roomRadius = 5|
		^SoundObject.new(Movable.new(pos, moveRule ? SteadyMoveRule.new, roomRadius), sound)
	}

	test_saveThenLoadRoundTripsEditableParamsAndMovable {
		var dir = this.prTestDir;
		var sound = FakeSoundForSoundObjectPresetLibraryTest.new(freq: 660, amp: 0.8);
		var moveRule = CircularMoveRule.new(baseRadius: 3, breathAmount: 1, breathRate: 0.2,
			angularSpeed: 0.5, startAngle: 1.2);
		var soundObject = this.prSoundObject(sound, moveRule, pos: [1, 2, 0], roomRadius: 6);
		var loaded;

		SoundObjectPresetLibrary.save(dir, "roundtrip", soundObject);
		loaded = SoundObjectPresetLibrary.load(dir, "roundtrip");

		this.assertEquals(loaded[\freq], 660);
		this.assertEquals(loaded[\amp], 0.8);
		this.assertEquals(loaded[\soundClass], \FakeSoundForSoundObjectPresetLibraryTest,
			"die Sound-Klasse wird mitgespeichert, um Presets später gegen Fehlanwendung zu prüfen");
		this.assertEquals(loaded[\movable][\moveRuleClass], \CircularMoveRule);
		this.assertEquals(loaded[\movable][\pos], [1, 2, 0]);
		this.assertEquals(loaded[\movable][\roomRadius], 6);
		this.assertEquals(loaded[\movable][\baseRadius], 3,
			"MoveRule-editableParams (Intent 46) werden im verschachtelten movable-Event mitgespeichert");
	}

	test_saveIncludesRequiredConstructorArgsForReconstruction {
		var dir = this.prTestDir;
		var sound = FakeSoundWithPathForSoundObjectPresetLibraryTest.new("/some/path.wav", 0.4);
		var soundObject = this.prSoundObject(sound);
		var loaded;

		SoundObjectPresetLibrary.save(dir, "withpath", soundObject);
		loaded = SoundObjectPresetLibrary.load(dir, "withpath");

		this.assertEquals(loaded[\path], "/some/path.wav",
			"requiredConstructorArgs-Werte (z.B. path) werden mitgespeichert, für einen " ++
			"möglichen Neuaufbau von anderer Stelle (siehe RoomSceneLibrary)");
	}

	test_loadMissingPresetReturnsNil {
		var dir = this.prTestDir;

		this.assertEquals(SoundObjectPresetLibrary.load(dir, "does_not_exist_" ++ 123456789), nil);
	}

	test_applyToWritesSoundParamsAndRestoresPositionAndMoveRule {
		var sound = FakeSoundForSoundObjectPresetLibraryTest.new(freq: 440, amp: 0.5);
		var soundObject = this.prSoundObject(sound, SteadyMoveRule.new, pos: [0, 0, 0],
			roomRadius: 5);
		var preset = (soundClass: \FakeSoundForSoundObjectPresetLibraryTest, freq: 220,
			amp: 0.9, movable: (moveRuleClass: \CircularMoveRule, pos: [7, 8, 0], roomRadius: 9,
				baseRadius: 4, breathAmount: 0, breathRate: 0.1, angularSpeed: 0.2,
				startAngle: 0));

		SoundObjectPresetLibrary.applyTo(soundObject, preset);

		this.assertEquals(sound.freq, 220);
		this.assertEquals(sound.amp, 0.9);
		this.assertEquals(soundObject.movable.pos, [7, 8, 0]);
		this.assertEquals(soundObject.movable.roomRadius, 9);
		this.assert(soundObject.movable.moveRule.isKindOf(CircularMoveRule),
			"moveRuleClass aus dem Preset ersetzt die bisherige MoveRule-Instanz");
		this.assertEquals(soundObject.movable.moveRule.baseRadius, 4);
	}

	test_applyToDoesNotTouchRequiredConstructorArgs {
		var sound = FakeSoundWithPathForSoundObjectPresetLibraryTest.new("/original/path.wav",
			0.3);
		var soundObject = this.prSoundObject(sound);
		var preset = (soundClass: \FakeSoundWithPathForSoundObjectPresetLibraryTest,
			path: "/other/path.wav", amp: 0.9);

		SoundObjectPresetLibrary.applyTo(soundObject, preset);

		this.assertEquals(sound.path, "/original/path.wav",
			"path hat keinen Setter -- applyTo darf ihn nicht anfassen, nur editableParams " ++
			"(Intent 46)");
		this.assertEquals(sound.amp, 0.9);
	}

	test_applyToWithoutMovableKeySkipsPositionRestoration {
		var sound = FakeSoundForSoundObjectPresetLibraryTest.new;
		var soundObject = this.prSoundObject(sound, SteadyMoveRule.new, pos: [5, 5, 0],
			roomRadius: 9);
		// altes Format (vor Intent 46) -- kein movable-Key
		var preset = (soundClass: \FakeSoundForSoundObjectPresetLibraryTest, freq: 220,
			amp: 0.9);

		SoundObjectPresetLibrary.applyTo(soundObject, preset);

		this.assertEquals(sound.freq, 220);
		this.assertEquals(soundObject.movable.pos, [5, 5, 0],
			"kein movable-Key im Preset -- Position bleibt unverändert " ++
			"(Rückwärtskompatibilität, Intent 46)");
	}

	test_listNamesReturnsSavedPresetNames {
		var dir = this.prTestDir;
		var soundObject = this.prSoundObject(FakeSoundForSoundObjectPresetLibraryTest.new);
		var names;

		SoundObjectPresetLibrary.save(dir, "presetA", soundObject);
		SoundObjectPresetLibrary.save(dir, "presetB", soundObject);
		names = SoundObjectPresetLibrary.listNames(dir);

		// Array>>includes vergleicht per Identität, nicht per Inhalt (siehe CLAUDE.md
		// Gotchas) -- zwei inhaltsgleiche, aber unterschiedliche String-Objekte matchen dort
		// nicht, obwohl ein direkter ==-Vergleich true liefert. Deshalb hier .any statt
		// .includes.
		this.assert(names.any { |n| n == "presetA" });
		this.assert(names.any { |n| n == "presetB" });
	}

	test_listNamesOnMissingDirReturnsEmptyArray {
		this.assertEquals(SoundObjectPresetLibrary.listNames(this.prTestDir ++ "_does_not_exist"), []);
	}

	test_listNamesForSoundClassFiltersByStoredSoundClass {
		var dir = this.prTestDir;
		var names;

		SoundObjectPresetLibrary.save(dir, "fakeA",
			this.prSoundObject(FakeSoundForSoundObjectPresetLibraryTest.new));
		File.mkdir(dir);
		File.use(dir +/+ "other.scd", "w", { |f|
			f.write("(soundClass: \\OtherSoundClass, freq: 330, amp: 0.2)".asString)
		});

		names = SoundObjectPresetLibrary.listNamesForSoundClass(dir,
			FakeSoundForSoundObjectPresetLibraryTest);

		this.assert(names.any { |n| n == "fakeA" });
		this.assert(names.any { |n| n == "other" }.not);
	}
}
