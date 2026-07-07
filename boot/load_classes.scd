// load_classes.scd — einmal pro SuperCollider-Sitzung ausführen.
// Bindet classes/ (Produktivcode: BootTrackDetection, Movable, Listener, SoundObject, ...)
// und tests/ (Test-Klassen: TestBootTrackDetection, ...) in den Klassenpfad ein und
// kompiliert neu. Danach im Post-Fenster auf "compile done" warten, bevor run_tests.scd
// oder demos/insects.scd benutzt wird.
(
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "classes").fullPath
);
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "tests").fullPath
);
thisProcess.recompile;
)
