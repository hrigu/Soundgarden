// RhythmPlaygroundPercussionSamples — zentrale Sample-Auswahl fuer den Rhythm-Playground.
// Die Pfade sind relativ zur Repository-Wurzel, damit Skripte und Tests dieselbe Zuordnung
// verwenden koennen.
RhythmPlaygroundPercussionSamples {

	*ampScale {
		^4.0
	}

	*relativePaths {
		^(
			kick: "sounds/instruments/Percussion/bass drum/bass-drum__025_forte_bass-drum-mallet.mp3",
			sub: "sounds/instruments/Percussion/surdo/surdo__05_forte_damped.mp3",
			snare: "sounds/instruments/Percussion/snare drum/snare-drum__025_forte_with-snares.mp3",
			clap: "sounds/instruments/Percussion/whip/whip__025_forte_struck-together.mp3",
			chihat: "sounds/instruments/Percussion/woodblock/woodblock__025_mezzo-forte_struck-singly.mp3",
			ohihat: "sounds/instruments/Percussion/suspended cymbal/suspended-cymbal__05_mezzo-forte_damped.mp3",
			perc: "sounds/instruments/Percussion/tambourine/tambourine__025_forte_hand.mp3",
			texture: "sounds/instruments/Percussion/spring coil/spring-coil__05_mezzo-forte_struck-singly.mp3"
		)
	}

	*pathFor { |layerName, repoRoot|
		var relativePath = this.relativePaths[layerName];
		if(relativePath.isNil) { ^nil };
		^repoRoot +/+ relativePath
	}

	*soundParamsFor { |layerName, repoRoot, amp = 0.5, duration = 0.35, startFrac = 0|
		var path = this.pathFor(layerName, repoRoot);
		if(path.isNil) { ^nil };
		^(path: path, amp: (amp * this.ampScale).clip(0, 1), duration: duration,
			startFrac: startFrac.clip(0, 1))
	}
}
