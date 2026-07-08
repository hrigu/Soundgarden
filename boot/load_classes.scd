// load_classes.scd — nur einmalig beim Erst-Setup ausführen (bzw. nach einem Reset/Löschen
// von sclang_conf.yaml oder auf einer neuen Maschine).
// Bindet classes/ (Produktivcode: BootTrackDetection, Movable, Listener, SoundObject, ...)
// und tests/ (Test-Klassen: TestBootTrackDetection, ...) in den Klassenpfad ein und
// kompiliert neu. LanguageConfig.addIncludePath persistiert dabei standardmäßig in
// sclang_conf.yaml — ab jetzt bindet SuperCollider beide Ordner bei jedem weiteren Start
// automatisch ein, ohne dass dieses Skript erneut laufen muss. Nach dem ersten Lauf im
// Post-Fenster auf "compile done" warten, bevor run_tests.scd oder eine der Demos in
// demos/grill/ benutzt wird.
//
// Eigene SCDoc-Hilfeseiten liegen direkt unter HelpSource/ im Projektverzeichnis (Intent 34).
// Nach Änderungen daran in scide Help → Rebuild All Documentation ausführen; danach sind
// z.B. Room/RoomReverb per cmd-d auf den Klassennamen im HelpBrowser auffindbar.
(
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "classes").fullPath
);
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "tests").fullPath
);
thisProcess.recompile;
)
