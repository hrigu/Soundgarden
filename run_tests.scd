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
TestTimeline.run;
TestCoupledOscillators.run;
TestGrooveNotation.run;
TestRoomRecorder.run;
TestRoomSceneLibrary.run;
TestSoloMuteController.run;
TestSoundObjectControlsView.run;
TestSoundObjectBuilder.run;
TestSoundObject.run;
TestCallingPattern.run;
TestRhythmPatternCreator.run;
TestRhythmPatternNotation.run;
TestBirdMotif.run;
TestBirdMotifExamples.run;
TestCricketSound.run;
TestSound.run;
TestInsectSound.run;
TestBirdSound.run;
TestMessiaenBirdSound.run;
TestSampleSound.run;
TestSampleSoundConfigurator.run;
TestSongSound.run;
TestSoundObjectPresetLibrary.run;
