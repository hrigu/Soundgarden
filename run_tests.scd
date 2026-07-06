// run_tests.scd — automatisierte Tests für die reine sclang-Logik
// (kein Server, keine echten Audiodateien nötig).
// Vorher (einmal pro Sitzung): load_classes.scd ausführen, falls noch
// nicht geschehen, und im Post-Fenster auf "compile done" warten.

TestBootTrackDetection.run;
TestCircularMoveRule.run;
TestOrchestra.run;
TestSteadyMoveRule.run;
TestMovable.run;
TestListener.run;
