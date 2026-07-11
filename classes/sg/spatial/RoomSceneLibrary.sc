// RoomSceneLibrary — speichert/lädt den kompletten Zustand eines Room als Datei: Raum-/Hall-
// Parameter, Listener-Position/-Blickrichtung und alle registrierten Soundobjekte (je über
// SoundObjectPresetLibrary>>eventFor, also inkl. Klangparametern, Position und Bewegungsregel)
// (Intent 46). Zwei Einstiegspunkte:
//   - *build — für den Code-Einstieg: konstruiert einen FRISCHEN Room aus einer Szenen-Datei,
//     inkl. Registrieren der benötigten SynthDefs. Gedacht für ein Demo-Skript, das die
//     Registrierung im selben Block macht und ~room.play danach in einem eigenen, separaten
//     Block aufruft (gleiches Zwei-Block-Muster wie alle anderen Demos -- das gibt den
//     asynchron registrierten SynthDefs genug zeitlichen Abstand, kein zusätzlicher
//     Sync-Mechanismus nötig).
//   - *applyTo — für die GUI: ersetzt die Soundobjekte eines BEREITS LAUFENDEN Room. Registriert
//     dafür bewusst KEINE SynthDefs (siehe Klassenkommentar-Einschränkung unten) und startet
//     jedes neue Soundobjekt sofort einzeln (im Gegensatz zu *build, wo das erst der separate
//     room.play-Block im Demo-Skript übernimmt).
//
// Bewusste Einschränkungen (Intent 46):
//   - *applyTo registriert keine SynthDefs -- die Sound-Klassen der geladenen Szene müssen in
//     der laufenden Session bereits bekannt sein (z.B. weil das Demo-Skript sie beim eigenen
//     Setup schon registriert hat). Ein sofortiges addSynthDef+play im selben GUI-Klick würde
//     wegen der asynchronen SynthDef-Registrierung riskieren, dass der erste Sound-Trigger auf
//     eine noch nicht fertige SynthDef trifft.
//   - reverbMix pro Soundobjekt wird nicht mitgespeichert (Room>>register-Default greift), da
//     das eine Binauralizer-Eigenschaft ist, keine Sound-/Movable-Eigenschaft (siehe Intent 27).
//   - Laufzeit-Zustände wie das exakte CallingPattern einer Grille werden nicht reproduziert,
//     sondern beim Neuaufbau frisch gewürfelt (siehe Sound>>buildFromSavedParams).
RoomSceneLibrary {

	// schreibt Room-Parameter, Listener und alle registrierten Soundobjekte nach dir/name.scd.
	*save { |dir, name, room|
		var soundObjectEvents = room.orchestra.soundObjects.collect { |soundObject|
			SoundObjectPresetLibrary.eventFor(soundObject)
		};
		var event = (
			roomParams: (size: room.size, height: room.height, surface: room.surface,
				mix: room.mix, spread: room.spread, inputBandwidth: room.inputBandwidth,
				tailBalance: room.tailBalance),
			listener: (pos: room.listener.pos, facing: room.listener.facing),
			soundObjects: soundObjectEvents
		);

		File.mkdir(dir);
		File.use(dir +/+ name ++ ".scd", "w", { |f| f.write(event.asCompileString) });
	}

	// liefert das gespeicherte Szenen-Event, oder nil, falls dir/name.scd nicht existiert.
	*load { |dir, name|
		var path = dir +/+ name ++ ".scd";
		^if(File.exists(path)) { path.load } { nil };
	}

	// listet alle gespeicherten Szenen-Namen in dir (Dateiname ohne .scd-Endung), oder [], falls
	// dir nicht existiert -- gleiche Logik wie SoundObjectPresetLibrary>>listNames.
	*listNames { |dir|
		^if(File.exists(dir)) {
			PathName(dir).files.select { |pn| pn.extension.asSymbol == \scd }
				.collect { |pn| pn.fileNameWithoutExtension }
		} { [] };
	}

	// baut aus einem verschachtelten movable-Event (siehe SoundObjectPresetLibrary) eine neue
	// Movable-Instanz inkl. frischer MoveRule -- gemeinsam genutzt von *applyTo und *build.
	*buildMovable { |movablePreset|
		var moveRuleClass = movablePreset[\moveRuleClass].asClass;
		var moveRule = moveRuleClass.new;

		moveRuleClass.editableParams.do { |pair|
			var key = pair[0];
			if(movablePreset[key].notNil) { moveRule.setParam(key, movablePreset[key]) };
		};
		^Movable.new(movablePreset[\pos], moveRule, movablePreset[\roomRadius])
	}

	// baut aus sceneEvent alle Soundobjekte neu auf und registriert sie an room (über
	// room.register, das den Binauralizer wie gehabt über listener.makeBinauralizer baut).
	// playImmediately=true (siehe *applyTo) startet jedes neue Soundobjekt sofort einzeln, da
	// orchestra.play in diesem Fall schon lange lief und neu registrierte Objekte sonst
	// registriert, aber niemals gestartet würden. playImmediately=false (siehe *build)
	// überlässt das Starten bewusst dem separaten room.play-Block des Demo-Skripts.
	*registerSoundObjects { |room, sceneEvent, playImmediately = false|
		sceneEvent[\soundObjects].do { |soundObjectEvent|
			var soundClass = soundObjectEvent[\soundClass].asClass;
			var sound = soundClass.buildFromSavedParams(soundObjectEvent);
			var movable = this.buildMovable(soundObjectEvent[\movable]);
			var soundObject;

			if(sound.respondsTo(\preload)) { sound.preload(room.server) };
			soundObject = room.register(movable, sound);
			if(playImmediately) { soundObject.play(room.server) };
		};
	}

	// ersetzt die Soundobjekte eines bereits laufenden Room durch die aus sceneEvent, und
	// übernimmt Room-/Listener-Parameter (siehe Klassenkommentar für Einschränkungen).
	*applyTo { |room, sceneEvent|
		var roomParams = sceneEvent[\roomParams];
		var listenerParams = sceneEvent[\listener];

		room.orchestra.clear;

		room.size = roomParams[\size];
		room.height = roomParams[\height];
		room.surface = roomParams[\surface];
		room.mix = roomParams[\mix];
		room.spread = roomParams[\spread];
		room.inputBandwidth = roomParams[\inputBandwidth];
		room.tailBalance = roomParams[\tailBalance];

		room.listener.pos = listenerParams[\pos];
		room.listener.facing = listenerParams[\facing];

		this.registerSoundObjects(room, sceneEvent, true);
		^room
	}

	// konstruiert einen frischen Room aus einer gespeicherten Szene -- für den Code-Einstieg
	// eines Demo-Skripts (siehe Klassenkommentar). listener wird wie bei Room.new üblich extern
	// konfiguriert/instanziiert und hier übergeben.
	*build { |dir, name, server, listener|
		var sceneEvent = this.load(dir, name);
		var roomParams = sceneEvent[\roomParams];
		var listenerParams = sceneEvent[\listener];
		var room;

		sceneEvent[\soundObjects].collect { |soundObjectEvent| soundObjectEvent[\soundClass] }
			.asSet.do { |soundClassName| soundClassName.asClass.addSynthDef };

		room = Room.new(server, listener, roomParams[\size], roomParams[\height],
			roomParams[\surface], roomParams[\mix], roomParams[\spread],
			roomParams[\inputBandwidth], roomParams[\tailBalance]);

		room.listener.pos = listenerParams[\pos];
		room.listener.facing = listenerParams[\facing];

		this.registerSoundObjects(room, sceneEvent, false);
		^room
	}
}
