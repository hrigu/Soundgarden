// Test-Double: minimaler Sound mit editableParams, um SoundPresetLibrary isoliert von
// InsectSound/SampleSound zu testen -- analog FakeSoundForSoundTest (TestSound.sc).
FakeSoundForPresetLibraryTest : Sound {
	var <>freq, <>amp;

	*new { |freq = 440, amp = 0.5| ^super.new.init(freq, amp) }

	init { |aFreq, aAmp| freq = aFreq; amp = aAmp }

	*editableParams {
		^[[\freq, ControlSpec(20, 8000, \exp)], [\amp, ControlSpec(0, 1, \lin)]]
	}
}

// Test für SoundPresetLibrary. Ausführen über run_tests.scd. Nutzt ein temporäres
// Testverzeichnis (Platform.defaultTempDir) -- reine Datei-I/O, kein Server nötig.
TestSoundPresetLibrary : UnitTest {

	prTestDir {
		^Platform.defaultTempDir +/+ "soundgarden_preset_library_test"
	}

	test_saveThenLoadRoundTripsEditableParams {
		var dir = this.prTestDir;
		var sound = FakeSoundForPresetLibraryTest.new(freq: 660, amp: 0.8);
		var loaded;

		SoundPresetLibrary.save(dir, "roundtrip", sound);
		loaded = SoundPresetLibrary.load(dir, "roundtrip");

		this.assertEquals(loaded[\freq], 660);
		this.assertEquals(loaded[\amp], 0.8);
		this.assertEquals(loaded[\soundClass], \FakeSoundForPresetLibraryTest,
			"die Sound-Klasse wird mitgespeichert, um Presets später gegen Fehlanwendung zu prüfen");
	}

	test_loadMissingPresetReturnsNil {
		var dir = this.prTestDir;

		this.assertEquals(SoundPresetLibrary.load(dir, "does_not_exist_" ++ 123456789), nil);
	}

	test_applyToWritesParamsOntoSoundViaSetParam {
		var sound = FakeSoundForPresetLibraryTest.new(freq: 440, amp: 0.5);
		var preset = (soundClass: \FakeSoundForPresetLibraryTest, freq: 220, amp: 0.9);

		SoundPresetLibrary.applyTo(sound, preset);

		this.assertEquals(sound.freq, 220);
		this.assertEquals(sound.amp, 0.9);
	}

	test_applyToIgnoresSoundClassKey {
		var sound = FakeSoundForPresetLibraryTest.new;
		var preset = (soundClass: \FakeSoundForPresetLibraryTest, freq: 220, amp: 0.9);

		// darf nicht versuchen, einen Setter namens 'soundClass_' aufzurufen
		SoundPresetLibrary.applyTo(sound, preset);

		this.assert(true, "kein Error beim Anwenden -- soundClass wird übersprungen");
	}

	test_listNamesReturnsSavedPresetNames {
		var dir = this.prTestDir;
		var sound = FakeSoundForPresetLibraryTest.new;
		var names;

		SoundPresetLibrary.save(dir, "presetA", sound);
		SoundPresetLibrary.save(dir, "presetB", sound);
		names = SoundPresetLibrary.listNames(dir);

		// Array>>includes vergleicht per Identität, nicht per Inhalt (siehe CLAUDE.md
		// Gotchas) -- zwei inhaltsgleiche, aber unterschiedliche String-Objekte matchen dort
		// nicht, obwohl ein direkter ==-Vergleich true liefert. Deshalb hier .any statt
		// .includes.
		this.assert(names.any { |n| n == "presetA" });
		this.assert(names.any { |n| n == "presetB" });
	}

test_listNamesOnMissingDirReturnsEmptyArray {
		this.assertEquals(SoundPresetLibrary.listNames(this.prTestDir ++ "_does_not_exist"), []);
	}

	test_listNamesForSoundClassFiltersByStoredSoundClass {
		var dir = this.prTestDir;
		var names;

		SoundPresetLibrary.save(dir, "fakeA", FakeSoundForPresetLibraryTest.new);
		File.mkdir(dir);
		File.use(dir +/+ "other.scd", "w", { |f|
			f.write("(soundClass: \\OtherSoundClass, freq: 330, amp: 0.2)".asString)
		});

		names = SoundPresetLibrary.listNamesForSoundClass(dir, FakeSoundForPresetLibraryTest);

		this.assert(names.any { |n| n == "fakeA" });
		this.assert(names.any { |n| n == "other" }.not);
	}
}
