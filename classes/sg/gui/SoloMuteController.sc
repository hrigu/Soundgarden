// SoloMuteController — Solo/Mute-Zustandsmaschine fürs Spatial-Control-Panel, extrahiert aus
// SpatialControlPanel (Intent 55). Reine Logik, kein GUI-Bezug: kennt nur eine Orchestra
// (bzw. ein Fake mit orchestra.soundObjects, siehe Tests) und schaltet bei aktivem Solo alle
// Soundobjekte ausser dem selektierten stumm (amp = 0 auf dem laufenden Synth), ohne die
// Instanz-amp selbst zu verändern (Restore liest immer sound.amp zurück).
//
// Die aktuell selektierte SoundObject-Instanz bleibt bewusst Sache des Aufrufers (siehe
// SpatialControlPanel>>selectSoundObject) — SoundObjectControlsView braucht dieselbe
// Selektion für ihren eigenen Zweck, dieser Controller soll dafür keine zweite Quelle der
// Wahrheit werden.
//
// Invariante bei Selektionswechsel während aktivem Solo: IMMER zuerst der komplette
// Originalzustand aller Objekte restaurieren (restoreIfActive, noch mit der ALTEN
// Selektion), DANACH erst applySoloState mit der NEUEN Selektion aufrufen — sonst könnte ein
// Objekt, das gerade die Selektion verliert, fälschlich auf amp=0 hängen bleiben.
SoloMuteController {
	var <orchestra;
	var <soloSelectedOnly;

	*new { |anOrchestra|
		^super.new.init(anOrchestra);
	}

	init { |anOrchestra|
		orchestra = anOrchestra;
		soloSelectedOnly = false;
	}

	// vom Aufrufer bei jedem Selektionswechsel VOR dem Aktualisieren der eigenen Selektion
	// aufzurufen (siehe Klassenkommentar) — no-op, falls Solo gerade nicht aktiv ist.
	restoreIfActive {
		if(soloSelectedOnly) { this.restoreSoloAmps };
		^this
	}

	setSoloSelectedOnly { |aBool, selectedSoundObject|
		soloSelectedOnly = aBool.asBoolean;
		this.applySoloState(selectedSoundObject);
		^this
	}

	restoreSoloAmps {
		orchestra.soundObjects.do { |soundObject|
			if(soundObject.sound.respondsTo(\amp)) {
				soundObject.sound.synth !? {
					soundObject.sound.synth.set(\amp, soundObject.sound.amp)
				};
			};
		};
	}

	applySoloState { |selectedSoundObject|
		// DEBUG (Intent 61, Bug-Diagnose "durchgehendes Zirpen trotz Solo") -- temporär.
		("[debug] applySoloState aufgerufen -- soloSelectedOnly=" ++ soloSelectedOnly
			++ " selectedSoundObject=" ++ selectedSoundObject
			++ " registrierte SoundObjects=" ++ orchestra.soundObjects.size).postln;

		if(soloSelectedOnly.not) {
			this.restoreSoloAmps;
			^this
		};

		if(selectedSoundObject.isNil) {
			soloSelectedOnly = false;
			this.restoreSoloAmps;
			^this
		};

		orchestra.soundObjects.do { |soundObject|
			if(soundObject.sound.respondsTo(\amp)) {
				var amp = if(soundObject === selectedSoundObject) {
					soundObject.sound.amp
				} {
					0
				};
				soundObject.sound.synth !? { soundObject.sound.synth.set(\amp, amp) };
				// DEBUG -- temporär.
				("[debug]   soundObject " ++ soundObject.identityHash
					++ (if(soundObject === selectedSoundObject) { " (SELEKTIERT)" } { " -> amp=0" })
					++ "  synth=" ++ soundObject.sound.synth).postln;
			};
		};
		^this
	}

	// finaler Rücksetz-Zustand, z.B. wenn SpatialControlPanel geschlossen wird — restauriert
	// erst alle Originalpegel, setzt danach Solo aus.
	reset {
		this.restoreSoloAmps;
		soloSelectedOnly = false;
		^this
	}
}
