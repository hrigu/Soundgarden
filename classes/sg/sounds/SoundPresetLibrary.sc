// SoundPresetLibrary — speichert/lädt Klangparameter-Kombinationen eines Sound-Objekts (z.B.
// eine besonders gute CricketSound-Einstellung) als kleine .scd-Dateien: eine Bibliothek an
// Presets, die über die Sitzung hinaus erhalten bleibt (Intent 43). Zustandslos, kein
// Server/Synth nötig -- reine Datei-I/O + Event-Logik.
//
// Ein Preset ist ein Event mit einem \soundClass-Eintrag (Symbol, der Klassenname) plus je
// einem Eintrag pro Sound>>editableParams-Key. \soundClass wird beim Laden mitgeliefert, damit
// applyTo (bzw. der Aufrufer) prüfen kann, ob ein Preset überhaupt zur Klasse des Ziel-Sounds
// passt, bevor es angewendet wird -- SoundPresetLibrary selbst erzwingt das nicht (das wäre
// Aufgabe der GUI, siehe SpatialControlPanel).
SoundPresetLibrary {

	// schreibt die aktuellen Werte aller editableParams von sound als Event-Literal nach
	// dir/name.scd. File.mkdir erstellt dir (inkl. fehlender Zwischenordner) und ist ein
	// no-op, falls es schon existiert.
	*save { |dir, name, sound|
		var values = sound.class.editableParams.collect { |pair| [pair[0], sound.perform(pair[0])] };
		var event = (soundClass: sound.class.name) ++ Event.newFrom(values.flatten);

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

	// wendet ein geladenes Preset-Event auf sound an -- über Sound>>setParam, also live auf
	// einem laufenden Synth UND auf der Instanz selbst. \soundClass ist reine Metadaten für den
	// Aufrufer (siehe Klassenkommentar), wird hier übersprungen statt als Parameter behandelt.
	*applyTo { |sound, presetEvent|
		presetEvent.keysValuesDo { |k, v|
			if(k != \soundClass) { sound.setParam(k, v) };
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
