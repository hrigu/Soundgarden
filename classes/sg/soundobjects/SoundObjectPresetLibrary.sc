// SoundObjectPresetLibrary — speichert/lädt die komplette Konfiguration eines SoundObjects
// (Sound-Klangparameter UND Position/Bewegungsregel) als kleine .scd-Dateien: eine Bibliothek
// an Presets, die über die Sitzung hinaus erhalten bleibt (Intent 43, erweitert um
// Movable-Daten in Intent 46 -- vormals SoundPresetLibrary, reine Sound-Presets ohne
// Positions-/Bewegungs-Wiederherstellung). Zustandslos, kein Server/Synth nötig -- reine
// Datei-I/O + Event-Logik.
//
// Ein Preset ist ein Event mit:
//   - \soundClass (Symbol, der Sound-Klassenname)
//   - je einem Eintrag pro Sound-Klasse>>requiredConstructorArgs-Key (z.B. \path bei
//     SampleSound/SongSound -- nötig, um den Sound bei Bedarf komplett neu aufzubauen, siehe
//     Sound>>buildFromSavedParams; SoundObjectPresetLibrary>>applyTo selbst fasst diese Keys
//     nicht an, siehe dort)
//   - je einem Eintrag pro Sound-Klasse>>editableParams-Key
//   - \movable: ein verschachteltes Event mit \moveRuleClass, \pos, \roomRadius und je einem
//     Eintrag pro MoveRule-Klasse>>editableParams-Key
// \soundClass wird beim Laden mitgeliefert, damit applyTo (bzw. der Aufrufer) prüfen kann, ob
// ein Preset überhaupt zur Klasse des Ziel-Sounds passt, bevor es angewendet wird --
// SoundObjectPresetLibrary selbst erzwingt das nicht (das wäre Aufgabe der GUI, siehe
// SoundObjectControlsView). Presets ohne \movable (vor Intent 46 gespeichert) bleiben ladbar --
// applyTo überspringt die Positions-/Bewegungs-Wiederherstellung dann einfach.
SoundObjectPresetLibrary {

	// baut das Preset-Event für soundObject, ohne es zu schreiben -- gemeinsam genutzt von
	// *save (einzelnes Objekt-Preset) und RoomSceneLibrary (ganze Szene, Intent 46).
	*eventFor { |soundObject|
		var sound = soundObject.sound;
		var movable = soundObject.movable;
		var moveRule = movable.moveRule;
		var requiredValues = sound.class.requiredConstructorArgs.collect { |key|
			[key, sound.perform(key)]
		};
		var editableValues = sound.class.editableParams.collect { |pair|
			[pair[0], sound.perform(pair[0])]
		};
		var moveRuleValues = moveRule.class.editableParams.collect { |pair|
			[pair[0], moveRule.perform(pair[0])]
		};
		var movableEvent = (moveRuleClass: moveRule.class.name, pos: movable.pos,
			roomRadius: movable.roomRadius) ++ Event.newFrom(moveRuleValues.flatten);

		^(soundClass: sound.class.name) ++ Event.newFrom(requiredValues.flatten)
			++ Event.newFrom(editableValues.flatten) ++ (movable: movableEvent)
	}

	// schreibt das Preset-Event für soundObject nach dir/name.scd. File.mkdir erstellt dir
	// (inkl. fehlender Zwischenordner) und ist ein no-op, falls es schon existiert.
	*save { |dir, name, soundObject|
		var event = this.eventFor(soundObject);

		File.mkdir(dir);
		File.use(dir +/+ name ++ ".scd", "w", { |f| f.write(event.asCompileString) });
	}

	// liefert das gespeicherte Preset-Event, oder nil, falls dir/name.scd nicht existiert.
	// .load wertet die Datei aus und liefert deren letztes Statement -- hier die einzige
	// Event-Literal-Zeile, die *save geschrieben hat.
	*load { |dir, name|
		var path = dir +/+ name ++ ".scd";
		^if(File.exists(path)) { path.load } { nil };
	}

	// wendet ein geladenes Preset-Event auf soundObject an -- Sound-Parameter über
	// Sound>>setParam (live auf einem laufenden Synth UND auf der Instanz selbst), Position/
	// Bewegungsregel direkt auf movable. Nur editableParams-Keys werden auf den Sound
	// angewendet (nie \soundClass, nie requiredConstructorArgs-Keys wie \path -- die haben
	// keinen Setter, siehe Sound-Klassenkommentar, und sind hier nur für einen möglichen
	// Neuaufbau von anderer Stelle gedacht, siehe RoomSceneLibrary).
	*applyTo { |soundObject, presetEvent|
		var sound = soundObject.sound;
		var movablePreset = presetEvent[\movable];

		sound.class.editableParams.do { |pair|
			var key = pair[0];
			if(presetEvent[key].notNil) { sound.setParam(key, presetEvent[key]) };
		};

		if(movablePreset.notNil) {
			var moveRuleClass = movablePreset[\moveRuleClass] !? { |name| name.asClass };

			if(movablePreset[\pos].notNil) { soundObject.movable.moveTo(movablePreset[\pos]) };
			if(movablePreset[\roomRadius].notNil) {
				soundObject.movable.roomRadius = movablePreset[\roomRadius]
			};

			if(moveRuleClass.notNil) {
				var moveRule = moveRuleClass.new;
				moveRuleClass.editableParams.do { |pair|
					var key = pair[0];
					if(movablePreset[key].notNil) { moveRule.setParam(key, movablePreset[key]) };
				};
				soundObject.movable.moveRule = moveRule;
			};
		};
	}

	// listet alle gespeicherten Preset-Namen in dir (Dateiname ohne .scd-Endung), oder [], falls
	// dir nicht existiert. Extension als Symbol vergleichen, nicht als String (PathName>>
	// extension kann ein Symbol liefern, siehe CLAUDE.md Gotchas/BootTrackDetection).
	*listNames { |dir|
		^if(File.exists(dir)) {
			PathName(dir).files.select { |pn| pn.extension.asSymbol == \scd }
				.collect { |pn| pn.fileNameWithoutExtension }
		} { [] };
	}

	// listet nur Presets, deren gespeicherte soundClass zur gewünschten Sound-Klasse passt.
	// Defensiv: ungültige/kaputte Preset-Dateien werden still übersprungen, statt das GUI-Menü
	// zu sprengen.
	*listNamesForSoundClass { |dir, soundClass|
		^this.listNames(dir).select { |name|
			var preset = this.load(dir, name);
			preset.notNil and: { preset[\soundClass] == soundClass.name }
		}
	}
}
