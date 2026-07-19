// RhythmPlaygroundPatternPresetLibrary — speichert/lädt GUI-Zustände des Rhythm-Playgrounds.
// Ein Preset enthält nur Pattern-Editor-Zustand: BPM, Klangmodus und pro Layer aktiv/pattern.
// Raum-/SoundObject-Zustand bleibt bewusst bei RoomSceneLibrary/SoundObjectPresetLibrary.
RhythmPlaygroundPatternPresetLibrary {

	*eventFor { |bpm, soundMode, layerStates|
		^(bpm: bpm, soundMode: soundMode, layers: layerStates)
	}

	*save { |dir, name, presetEvent|
		File.mkdir(dir);
		File.use(dir +/+ name ++ ".scd", "w", { |f| f.write(presetEvent.asCompileString) });
	}

	*load { |dir, name|
		var path = dir +/+ name ++ ".scd";
		^if(File.exists(path)) { path.load } { nil }
	}

	*listNames { |dir|
		^if(File.exists(dir)) {
			PathName(dir).files.select { |pn| pn.fileName.endsWith(".scd") }
				.collect { |pn| pn.fileNameWithoutExtension.asString }
		} { [] };
	}
}
