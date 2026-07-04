// run_tests.scd — automatisierte Tests für die reine sclang-Logik
// (kein Server, keine echten Audiodateien nötig).

// Block 1: einmal pro SuperCollider-Sitzung ausführen.
// Bindet den classes/-Ordner in den Klassenpfad ein und kompiliert neu.
(
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname +/+ "classes").fullPath
);
thisProcess.recompile;
)

// Block 2: erst ausführen, NACHDEM im Post-Fenster "compile done" steht.
// (Nach einem Neustart von SuperCollider reicht danach direkt dieser Block.)
TestBootTrackDetection.run;
