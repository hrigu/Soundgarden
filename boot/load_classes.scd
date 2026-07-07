// load_classes.scd — einmal pro SuperCollider-Sitzung ausführen.
// Bindet classes/ (Produktivcode: BootTrackDetection, Movable, Listener, SoundObject, ...)
// und tests/ (Test-Klassen: TestBootTrackDetection, ...) in den Klassenpfad ein und
// kompiliert neu. Danach im Post-Fenster auf "compile done" warten, bevor run_tests.scd
// oder insect_demo.scd benutzt wird.
(
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname +/+ ".." +/+ "classes").fullPath
);
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname +/+ ".." +/+ "tests").fullPath
);
thisProcess.recompile;
)
