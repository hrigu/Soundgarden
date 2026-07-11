// Test-Doubles für RoomSceneLibrary: reine sclang-Logik ohne Server, analog zu den Fakes in
// TestRoom.sc (FakeOrchestraForRoomTest, FakeReverbForRoomTest, FakeListenerForRoomTest) --
// hier eigenständig gehalten, da RoomSceneLibrary zusätzlich orchestra.soundObjects/clear und
// listener.pos_/facing_ braucht.
FakeOrchestraForRoomSceneLibraryTest {
	var <soundObjects;
	var <log;

	*new { |aLog| ^super.new.init(aLog) }

	init { |aLog|
		log = aLog;
		soundObjects = [];
	}

	register { |soundObject|
		soundObjects = soundObjects.add(soundObject);
		log.add([\register, soundObject]);
		^soundObject
	}

	clear {
		log.add(\clear);
		soundObjects = [];
	}

	play { |server| log.add(\play) }
}

FakeReverbForRoomSceneLibraryTest {
	var <bus, <log;

	*new { |aLog| ^super.new.init(aLog) }

	init { |aLog|
		log = aLog;
		bus = \fakeBus;
	}

	play { |server, roomSize, revTime, damping, mix, spread, inputBandwidth, tailBalance|
		log.add(\reverbPlay)
	}

	set { |roomSize, revTime, damping, mix, spread, inputBandwidth, tailBalance|
		log.add(\reverbSet)
	}

	stop { log.add(\reverbStop) }

	free { log.add(\reverbFree) }
}

FakeBinauralizerForRoomSceneLibraryTest {
	var <>reverbMix;
	var <playCount;

	*new { |reverbMix = 0.3| ^super.new.init(reverbMix) }

	init { |aReverbMix|
		reverbMix = aReverbMix;
		playCount = 0;
	}

	play { |server, inBus, outBus, target, addAction|
		playCount = playCount + 1;
	}

	set { |azimuth, distance| }

	stop { }
}

FakeListenerForRoomSceneLibraryTest {
	var <>pos, <>facing;

	*new { ^super.new.init }

	init {
		pos = [0, 0, 0];
		facing = 0;
	}

	makeBinauralizer { |reverbMix = 0.3| ^FakeBinauralizerForRoomSceneLibraryTest.new(reverbMix) }

	setup { |server, reverbBus| }

	teardown { }
}

// minimaler Sound mit editableParams, dessen play() keinen echten Bus/Server anfasst (anders
// als Sound>>play) -- headless-sicher, analog FakeSoundForSoundObjectPresetLibraryTest, aber
// mit überschriebenem play/stop für die playImmediately-Tests von *applyTo (verifiziert wird
// dort über soundObject.binauralizer.playCount, siehe FakeBinauralizerForRoomSceneLibraryTest).
FakeSoundForRoomSceneLibraryTest : Sound {
	var <>freq, <>amp;

	*new { |freq = 440, amp = 0.5| ^super.new.init(freq, amp) }

	init { |aFreq, aAmp| freq = aFreq; amp = aAmp }

	*editableParams {
		^[[\freq, ControlSpec(20, 8000, \exp)], [\amp, ControlSpec(0, 1, \lin)]]
	}

	play { |server| }

	stop { }
}

// Test für RoomSceneLibrary (Intent 46). *build (konstruiert einen echten Room über Room.new,
// braucht einen echten Server) wird manuell verifiziert (siehe Intent 46, Task 6.2) -- hier
// wird die server-unabhängige Logik getestet: *save/*load (reine Datei-I/O) und *applyTo/
// *buildMovable/*registerSoundObjects über Room.forTest mit Fakes, analog TestRoom.sc.
TestRoomSceneLibrary : UnitTest {

	prTestDir {
		^Platform.defaultTempDir +/+ "soundgarden_room_scene_library_test"
	}

	prForTestRoom { |log|
		^Room.forTest(FakeOrchestraForRoomSceneLibraryTest.new(log),
			FakeReverbForRoomSceneLibraryTest.new(log), listener: FakeListenerForRoomSceneLibraryTest.new(),
			size: 10, height: 4, surface: 0.6, mix: 0.8, spread: 20, inputBandwidth: 0.4,
			tailBalance: 0.3)
	}

	test_saveThenLoadRoundTripsRoomListenerAndSoundObjects {
		var dir = this.prTestDir;
		var log = List.new;
		var room = this.prForTestRoom(log);
		var sound = FakeSoundForRoomSceneLibraryTest.new(freq: 660, amp: 0.8);
		var movable = Movable.new([1, 2, 0], SteadyMoveRule.new, 6);
		var loaded;

		room.listener.pos = [3, 4, 0];
		room.listener.facing = 45;
		room.register(movable, sound);

		RoomSceneLibrary.save(dir, "roundtrip", room);
		loaded = RoomSceneLibrary.load(dir, "roundtrip");

		this.assertEquals(loaded[\roomParams][\size], 10);
		this.assertEquals(loaded[\roomParams][\height], 4);
		this.assertEquals(loaded[\roomParams][\surface], 0.6);
		this.assertEquals(loaded[\roomParams][\mix], 0.8);
		this.assertEquals(loaded[\roomParams][\spread], 20);
		this.assertEquals(loaded[\roomParams][\inputBandwidth], 0.4);
		this.assertEquals(loaded[\roomParams][\tailBalance], 0.3);
		this.assertEquals(loaded[\listener][\pos], [3, 4, 0]);
		this.assertEquals(loaded[\listener][\facing], 45);
		this.assertEquals(loaded[\soundObjects].size, 1);
		this.assertEquals(loaded[\soundObjects][0][\soundClass], \FakeSoundForRoomSceneLibraryTest);
		this.assertEquals(loaded[\soundObjects][0][\freq], 660);
		this.assertEquals(loaded[\soundObjects][0][\movable][\pos], [1, 2, 0]);
		this.assertEquals(loaded[\soundObjects][0][\movable][\moveRuleClass], \SteadyMoveRule);
	}

	test_loadMissingSceneReturnsNil {
		this.assertEquals(RoomSceneLibrary.load(this.prTestDir, "does_not_exist_" ++ 123456789),
			nil);
	}

	test_listNamesReturnsSavedSceneNames {
		var dir = this.prTestDir;
		var log = List.new;
		var room = this.prForTestRoom(log);
		var names;

		RoomSceneLibrary.save(dir, "sceneA", room);
		RoomSceneLibrary.save(dir, "sceneB", room);
		names = RoomSceneLibrary.listNames(dir);

		this.assert(names.any { |n| n == "sceneA" });
		this.assert(names.any { |n| n == "sceneB" });
	}

	test_buildMovableReconstructsPositionAndMoveRule {
		var movablePreset = (moveRuleClass: \CircularMoveRule, pos: [2, 3, 0], roomRadius: 7,
			baseRadius: 4, breathAmount: 0.5, breathRate: 0.1, angularSpeed: 0.3, startAngle: 0);
		var movable = RoomSceneLibrary.buildMovable(movablePreset);

		this.assertEquals(movable.pos, [2, 3, 0]);
		this.assertEquals(movable.roomRadius, 7);
		this.assert(movable.moveRule.isKindOf(CircularMoveRule));
		this.assertEquals(movable.moveRule.baseRadius, 4);
	}

	test_applyToClearsOldSoundObjectsUpdatesRoomAndPlaysNewOnesImmediately {
		var log = List.new;
		var room = this.prForTestRoom(log);
		var oldSound = FakeSoundForRoomSceneLibraryTest.new;
		var sceneEvent = (
			roomParams: (size: 15, height: 5, surface: 0.9, mix: 0.5, spread: 25,
				inputBandwidth: 0.7, tailBalance: 0.6),
			listener: (pos: [9, 9, 0], facing: 90),
			soundObjects: [
				(soundClass: \FakeSoundForRoomSceneLibraryTest, freq: 550, amp: 0.6,
					movable: (moveRuleClass: \SteadyMoveRule, pos: [4, 4, 0], roomRadius: 8))
			]
		);

		room.register(Movable.new(moveRule: SteadyMoveRule.new), oldSound);
		log.clear;

		RoomSceneLibrary.applyTo(room, sceneEvent);

		this.assert(log.includes(\clear), "applyTo räumt zuerst die alten Soundobjekte ab");
		this.assertEquals(room.size, 15);
		this.assertEquals(room.height, 5);
		this.assertEquals(room.surface, 0.9);
		this.assertEquals(room.mix, 0.5);
		this.assertEquals(room.spread, 25);
		this.assertEquals(room.inputBandwidth, 0.7);
		this.assertEquals(room.tailBalance, 0.6);
		this.assertEquals(room.listener.pos, [9, 9, 0]);
		this.assertEquals(room.listener.facing, 90);
		this.assertEquals(room.orchestra.soundObjects.size, 1);
		this.assertEquals(room.orchestra.soundObjects[0].sound.freq, 550);
		this.assertEquals(room.orchestra.soundObjects[0].movable.pos, [4, 4, 0]);
		this.assertEquals(room.orchestra.soundObjects[0].binauralizer.playCount, 1,
			"applyTo startet jedes neu aufgebaute Soundobjekt sofort (playImmediately), " ++
			"da orchestra.play in diesem Szenario schon lange lief");
	}

	test_registerSoundObjectsWithoutPlayImmediatelyLeavesSoundObjectsUnplayed {
		var log = List.new;
		var room = this.prForTestRoom(log);
		var sceneEvent = (
			soundObjects: [
				(soundClass: \FakeSoundForRoomSceneLibraryTest, freq: 440, amp: 0.5,
					movable: (moveRuleClass: \SteadyMoveRule, pos: [0, 0, 0], roomRadius: 5))
			]
		);

		RoomSceneLibrary.registerSoundObjects(room, sceneEvent, false);

		this.assertEquals(room.orchestra.soundObjects.size, 1);
		this.assertEquals(room.orchestra.soundObjects[0].binauralizer.playCount, 0,
			"playImmediately=false (siehe *build) startet Soundobjekte nicht selbst -- das " ++
			"überlässt build bewusst dem separaten room.play-Block im Demo-Skript");
	}
}
