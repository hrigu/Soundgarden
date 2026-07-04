// Test für BootTrackDetection. Ausführen über run_tests.scd.
TestBootTrackDetection : UnitTest {

	test_acceptsSymbolExtensions {
		[\wav, \aiff, \aif, \flac].do { |ext|
			this.assert(
				BootTrackDetection.isValidExtension(ext),
				"% (Symbol) sollte erkannt werden".format(ext)
			);
		};
	}

	test_acceptsStringExtensions {
		this.assert(
			BootTrackDetection.isValidExtension("aiff"),
			"'aiff' als String sollte erkannt werden — genau der Bug, der boot.scd zuerst lahmgelegt hat"
		);
	}

	test_isCaseInsensitive {
		this.assert(
			BootTrackDetection.isValidExtension("AIFF"),
			"Großschreibung darf keine Rolle spielen"
		);
	}

	test_rejectsUnsupportedExtensions {
		this.assert(
			BootTrackDetection.isValidExtension("mp3").not,
			"mp3 steht nicht auf der Liste und muss abgelehnt werden"
		);
	}
}
