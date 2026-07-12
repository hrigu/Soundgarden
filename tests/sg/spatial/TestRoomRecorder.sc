// Test-Double für RoomRecorder: zeichnet nur record/stopRecording-Aufrufe auf, ohne echten Server.
FakeServerForRoomRecorderTest {
	var <recordCalls;
	var <stopRecordingCallCount;

	*new { ^super.new.init }

	init {
		recordCalls = List.new;
		stopRecordingCallCount = 0;
	}

	record { |path| recordCalls.add(path) }

	stopRecording { stopRecordingCallCount = stopRecordingCallCount + 1 }
}

// Test für RoomRecorder. Ausführen über run_tests.scd.
TestRoomRecorder : UnitTest {

	prTestDir {
		^Platform.defaultTempDir +/+ "soundgarden_recorder_test"
	}

	test_pathForBuildsWavPathWithGivenPrefixInsideDir {
		var recorder = RoomRecorder.new(this.prTestDir);
		var path = recorder.pathFor("myprefix");

		this.assert(path.beginsWith(this.prTestDir +/+ "myprefix_"),
			"Pfad liegt im gegebenen Ordner mit gegebenem Präfix");
		this.assert(path.endsWith(".wav"), "Aufnahmen werden als WAV gespeichert");
	}

	test_pathForDefaultsToRecordingPrefix {
		var recorder = RoomRecorder.new(this.prTestDir);
		var path = recorder.pathFor;

		this.assert(path.beginsWith(this.prTestDir +/+ "recording_"),
			"ohne explizites Präfix wird 'recording' verwendet");
	}

	test_startBeginsRecordingAndSetsLastPath {
		var server = FakeServerForRoomRecorderTest.new;
		var recorder = RoomRecorder.new(this.prTestDir);
		var path = recorder.start(server, "test");

		this.assertEquals(recorder.isRecording, true);
		this.assertEquals(server.recordCalls.asArray, [path]);
		this.assertEquals(recorder.lastPath, path);
	}

	test_startIsIdempotentWhileAlreadyRecording {
		var server = FakeServerForRoomRecorderTest.new;
		var recorder = RoomRecorder.new(this.prTestDir);
		var firstPath = recorder.start(server, "test");
		var secondPath = recorder.start(server, "test");

		this.assertEquals(secondPath, firstPath,
			"zweiter start() während laufender Aufnahme liefert denselben Pfad zurück");
		this.assertEquals(server.recordCalls.size, 1,
			"server.record darf während einer laufenden Aufnahme nicht erneut aufgerufen werden");
	}

	test_stopEndsRecording {
		var server = FakeServerForRoomRecorderTest.new;
		var recorder = RoomRecorder.new(this.prTestDir);
		recorder.start(server, "test");

		recorder.stop(server);

		this.assertEquals(recorder.isRecording, false);
		this.assertEquals(server.stopRecordingCallCount, 1);
	}

	test_stopWithoutRecordingIsSilentNoOp {
		var server = FakeServerForRoomRecorderTest.new;
		var recorder = RoomRecorder.new(this.prTestDir);

		recorder.stop(server);

		this.assertEquals(server.stopRecordingCallCount, 0,
			"stop() ohne laufende Aufnahme darf server.stopRecording nicht aufrufen");
	}

	test_defaultIsRecordingIsFalse {
		var recorder = RoomRecorder.new(this.prTestDir);

		this.assertEquals(recorder.isRecording, false);
	}
}
