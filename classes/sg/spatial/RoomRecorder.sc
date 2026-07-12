// RoomRecorder — kapselt Server-Aufnahme (Start/Stop) + Dateiname-Erzeugung (Intent 60: Start/
// Stop-Recording-Button im GUI statt manuellem s.record-Aufruf am Ende jedes Kompositions-
// skripts, siehe RoomParamsView). "Recorder" ist bereits eine SuperCollider-Kernklasse, daher
// der Room-Präfix. pathFor ist reine Datei-/Zeitstempel-Logik ohne Server-Bezug, daher ohne
// Server testbar (siehe TestRoomRecorder); start/stop selbst brauchen einen echten Server, sind
// aber idempotent wie SoundObject>>stop (Intent 59-Folgefix) -- ein zweiter Button-Klick
// während einer laufenden/gestoppten Aufnahme darf nicht erneut auf den Server wirken.
RoomRecorder {
	var <dir;         // Zielordner für Aufnahmen, z.B. .../recordings
	var <isRecording;
	var <lastPath;

	*new { |aDir|
		^super.new.init(aDir);
	}

	init { |aDir|
		dir = aDir;
		isRecording = false;
	}

	// reine Pfad-Logik, ohne Server-Bezug -- testbar (siehe TestRoomRecorder).
	pathFor { |prefix = "recording"|
		^dir +/+ (prefix ++ "_" ++ Date.getDate.format("%Y-%m-%d_%H-%M-%S") ++ ".wav")
	}

	// idempotent: ein zweiter start() während einer laufenden Aufnahme ist ein stiller No-op
	// (liefert den bereits laufenden Pfad zurück) statt server.record ein zweites Mal mit
	// einem neuen Pfad aufzurufen.
	start { |server, prefix = "recording"|
		if(isRecording) { ^lastPath };
		if(File.exists(dir).not) { File.mkdir(dir) };
		lastPath = this.pathFor(prefix);
		server.record(path: lastPath);
		isRecording = true;
		^lastPath
	}

	// ebenfalls idempotent: stop() ohne laufende Aufnahme ist ein stiller No-op.
	stop { |server|
		if(isRecording.not) { ^this };
		server.stopRecording;
		isRecording = false;
		^this
	}
}
