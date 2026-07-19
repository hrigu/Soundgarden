// rhythm_playground.scd — isolierter Testraum für die Cricket-Techno-Rhythm-Patterns.
// Ziel: einzelne Layer oder Layer-Kombinationen schnell hören, ohne den vollen 36-Stimmen-Raum.
// Kein GUI: Layer werden über ~activeLayers im Code ausgewählt.
//
// Kopfhörer benutzen — binaurale Effekte funktionieren nicht über Lautsprecher.

// --- 1) Setup -------------------------------------------------------------------------
(
CricketSound.addSynthDef;
~listener = Listener.new(binauralizerClass: Binauralizer);
~room = Room.new(s, listener: ~listener, size: 10, height: 3, surface: 0.5);

// Tempo und Raster
~bpm = 110;
~beatsPerBar = 4;
~numBars = 16;
~stepsPerBeat = 4;              // 16tel-Noten
~stepDur = 60 / ~bpm / ~stepsPerBeat;
~stepsPerBar = ~beatsPerBar * ~stepsPerBeat;

// Layer-Auswahl: Namen einfach in das Array schreiben, die man hören will.
// Verfügbare Layer: \kick, \sub, \snare, \clap, \chihat, \ohihat, \perc, \texture
~activeLayers = [\kick, \snare];

// Pro Layer: Pattern-Bausteine, Sequenz, Klangparameter und Chirp-Dauer.
~layerDefs = (
	kick: (
		bars: [
			"....|x...|x...|....",   // 0: Kick auf 1 und 3 (half-time-feel)
			"x...|x...|x...|x...",   // 1: vier auf dem Floor
			"x...|..x.|....|x...",   // 2: Kick auf 1, 16tel-Flur, 3
			"x...|x...|x.....|x.",   // 3: vier auf dem Floor + Vorgriff
			"x...|x...|x.x.|x...",   // 4: Kick auf 1, 3 und Nachschlag
			"..x.|x...|x...|...."    // 5: synkopiert
		],
		seq: [0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 4],
		soundParams: (
			wingRate: 16, wingDuty: 0.60,
			ringFreq1: 900, ringFreq2: 950,
			ringDecay1: 0.10, ringDecay2: 0.08,
			amp: 0.08
		),
		chirpDur: 0.18
	),
	sub: (
		bars: [
			"x.....|......|......|......",   // 0: ein Sub-Schlag pro Takt (24tel)
			"......|......|......|......",   // 1: Pause
			"x.....|..x...|......|......"    // 2: zwei Sub-Schläge
		],
		seq: [0, 1, 0, 1, 2, 1, 0, 1, 0, 1, 0, 1, 2, 1, 0, 1],
		soundParams: (
			wingRate: 10, wingDuty: 0.70,
			ringFreq1: 500, ringFreq2: 520,
			ringDecay1: 0.18, ringDecay2: 0.15,
			amp: 0.08
		),
		chirpDur: 0.30,
		subdivision: 24
	),
	snare: (
		bars: [
			"....|x...|....|x...",   // 0: klassischer Backbeat
			"....|x...|x...|x...",   // 1: Backbeat + Offbeat
			"....|x...|....|x.x.",   // 2: Backbeat + Vorgriff
			"x...|x...|x...|x...",   // 3: Roll
			"....|....|....|...."    // 4: Pause
		],
		seq: [0, 0, 1, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 2, 3, 4],
		soundParams: (
			wingRate: 34, wingDuty: 0.32,
			ringFreq1: 2400, ringFreq2: 2500,
			ringDecay1: 0.035, ringDecay2: 0.025,
			amp: 0.20
		),
		chirpDur: 0.09
	),
	clap: (
		bars: [
			"....|x...|....|x...",   // 0: Backbeat-Clap
			"x...|....|x...|....",   // 1: halbzeit-Clap
			"....|x...|x...|.x..",   // 2: synkopiert
			"....|....|....|...."    // 3: Pause
		],
		seq: [0, 3, 0, 2, 0, 3, 1, 0, 0, 3, 0, 2, 0, 3, 0, 0],
		soundParams: (
			wingRate: 45, wingDuty: 0.28,
			ringFreq1: 4500, ringFreq2: 4600,
			ringDecay1: 0.012, ringDecay2: 0.010,
			amp: 0.32
		),
		chirpDur: 0.05
	),
	chihat: (
		bars: [
			"x.x.|x.x.|x.x.|x.x.",   // 0: geschlossene 16tel
			".x.x|.x.x|.x.x|.x.x",   // 1: versetzt
			"x...|x...|x...|x...",   // 2: Achtel
			"x.x.|x.x.|...x|.x.x.",   // 3: mit Lücken
			"....|....|....|...."    // 4: Pause
		],
		seq: [0, 0, 1, 0, 0, 3, 0, 0, 0, 1, 0, 0, 0, 2, 0, 4],
		soundParams: (
			wingRate: 60, wingDuty: 0.22,
			ringFreq1: 7500, ringFreq2: 7700,
			ringDecay1: 0.006, ringDecay2: 0.005,
			amp: 0.18
		),
		chirpDur: 0.03
	),
	ohihat: (
		bars: [
			"..x.|...x|...x|...x",   // 0: offene Hihat auf den Offbeats
			"x...|x...|x...|x...",   // 1: jeder Beat
			"....|x...|....|x...",   // 2: Backbeat
			"....|....|....|...."    // 3: Pause
		],
		seq: [0, 3, 0, 0, 1, 3, 0, 0, 0, 3, 0, 1, 0, 3, 2, 0],
		soundParams: (
			wingRate: 48, wingDuty: 0.30,
			ringFreq1: 6000, ringFreq2: 6100,
			ringDecay1: 0.020, ringDecay2: 0.015,
			amp: 0.34
		),
		chirpDur: 0.08
	),
	perc: (
		bars: [
			"x...|x...|x...|x...",   // 0: gleichmässig
			"x.x.|x.x.|x.x.|x.x.",   // 1: 16tel
			"....|....|....|x...",   // 2: Taktende-Akzent
			"....|....|x.x.|x...",   // 3: Triolen-Fill
			"....|....|....|....",   // 4: Pause
			"x...|x...|x...|x.x."    // 5: Vorgriff
		],
		seq: [4, 4, 0, 4, 4, 2, 4, 4, 4, 0, 4, 3, 4, 4, 1, 5],
		soundParams: (
			wingRate: 28, wingDuty: 0.45,
			ringFreq1: 1600, ringFreq2: 1700,
			ringDecay1: 0.05, ringDecay2: 0.04,
			amp: 0.22
		),
		chirpDur: 0.12
	),
	texture: (
		bars: nil,   // wird zufällig generiert
		seq: nil,
		soundParams: (
			wingRate: 36, wingDuty: 0.42,
			ringFreq1: 3200, ringFreq2: 3300,
			ringDecay1: 0.025, ringDecay2: 0.020,
			amp: 0.30
		),
		chirpDur: 0.06,
		textureDensity: 0.20
	)
);

// --- Pattern-Hilfsfunktionen ----------------------------------------------------------

// Entfernt grafische Taktstriche ('|') aus einem Pattern-String.
~cleanPattern = { |str| str.select { |c| c != $| } };

// Wandelt einen String wie "x...x..." in einen Array von Gates (1/0) um.
~stringToGates = { |str|
	var cleaned = ~cleanPattern.value(str);
	Array.fill(cleaned.size, { |i| if(cleaned[i] == $x) { 1 } { 0 } })
};

// Wandelt ein Array von Takt-Strings in einen endlosen Gate-Stream um.
~barsToGateStream = { |barStrings|
	var gates = barStrings.collect(~stringToGates).flat;
	Pseq(gates, inf);
};

// Spezialfall Sub: 24tel-Auflösung (längere Strings) auf 16tel herunterrechnen.
~subBarsToGateStream = { |barStrings|
	var gates = barStrings.collect { |str|
		var cleaned = ~cleanPattern.value(str);
		~stepsPerBar.collect { |step|
			var idx = (step * cleaned.size / ~stepsPerBar).floor.asInteger;
			if(cleaned[idx] == $x) { 1 } { 0 }
		}
	}.flat;
	Pseq(gates, inf);
};

// Baut einen zufälligen Texture-Stream aus 16 Takten.
~buildTextureStream = { |density|
	var bars = ~numBars.collect {
		String.fill(~stepsPerBar, {
			if(density.coin) { $x } { $. }
		})
	};
	~barsToGateStream.value(bars);
};

// Pro aktivem Layer ein CricketSound im Raum registrieren.
// Alle stehen an derselben Position, damit der Fokus auf dem Rhythmus liegt.
~soundObjects = ~activeLayers.collect { |layerName, i|
	var def = ~layerDefs[layerName];
	var movableParams = (
		pos: [0, 0, 0],
		moveRule: SteadyMoveRule.new,
		roomRadius: 5
	);
	~room.registerFromBuilder(def[\soundParams] ++ (pattern: nil), movableParams,
		soundClass: CricketSound)
};

// Gotcha: eingebautes CallingPattern ausschalten.
~soundObjects.do { |so| so.sound.pattern = nil };
)

// --- 2) Start -------------------------------------------------------------------------
(
"Rhythm-Pattern-Playground startet: ".post;
~activeLayers.do { |n| (n.asString ++ " ").post };
"".postln;

~room.play;
~soundObjects.do { |so| so.sound.synth.set(\gate, 0) };

~routines = ~activeLayers.collect { |layerName, i|
	var def = ~layerDefs[layerName];
	var stream;
	var synth = ~soundObjects[i].sound.synth;
	var durStream = (~stepDur * Pwhite(0.95, 1.05, inf)).asStream;

	if(layerName == \texture) {
		stream = ~buildTextureStream.value(def[\textureDensity]).asStream;
	} {
		if(def[\subdivision] == 24) {
			stream = ~subBarsToGateStream.value(def[\seq].collect { |idx| def[\bars][idx] }).asStream;
		} {
			stream = ~barsToGateStream.value(def[\seq].collect { |idx| def[\bars][idx] }).asStream;
		};
	};

	Routine({
		loop {
			var gate = stream.next;
			var dur = durStream.next;
			if(gate.notNil and: { gate > 0 }) {
				var chirpDur = min(def[\chirpDur], dur * 0.9);
				synth.set(\gate, 1);
				SystemClock.sched(chirpDur, { synth.set(\gate, 0) });
			};
			dur.wait;
		}
	});
};

~routines.do(_.play);
)

// --- 3) Stop --------------------------------------------------------------------------
(
~routines.do(_.stop);
~soundObjects.do { |so| so.fadeOutAndStop(0.5, { |stopped| ~room.orchestra.unregister(stopped) }) };
)
