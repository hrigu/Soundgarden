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
// docs/ wird zusätzlich eingebunden, nicht wegen .sc-Dateien (docs/ enthält keine), sondern
// weil SCDoc jeden includePath-Root rekursiv nach einem Unterordner namens HelpSource
// durchsucht (Intent 34) — docs/HelpSource/Classes/*.schelp macht so die eigenen Klassen
// (z.B. Room) im normalen SuperCollider-HelpBrowser auffindbar (cmd-d auf den Klassennamen),
// nach einem einmaligen Help → Rebuild All Documentation.
(
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "classes").fullPath
);
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "tests").fullPath
);
LanguageConfig.addIncludePath(
	PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "docs").fullPath
);
thisProcess.recompile;
)
