// run_tests.scd — automatisierte Tests für die reine sclang-Logik
// (kein Server, keine echten Audiodateien nötig).
// Vorher: boot/load_classes.scd nur beim Erst-Setup nötig (persistiert in sclang_conf.yaml,
// danach automatisch bei jedem Start eingebunden) — falls noch nie gelaufen, jetzt ausführen
// und im Post-Fenster auf "compile done" warten.

TestBootTrackDetection.run;
TestCircularMoveRule.run;
TestOrchestra.run;
TestSteadyMoveRule.run;
TestMovable.run;
TestListener.run;
TestRoom.run;
TestSoundObjectBuilder.run;
TestCallingPattern.run;
TestRhythmPatternCreator.run;
