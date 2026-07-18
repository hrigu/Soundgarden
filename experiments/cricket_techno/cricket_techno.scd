// cricket_techno.scd — 36-stimmiges Grillen-Experiment im Stil eines low-beat-art-Techno.
// Kein Chaos-zu-Groove-Übergang (anders als cricket_groove.scd, Intent 62): der Groove ist
// sofort da und direkt editierbar. SuperCollider-Patterns (Pseq, Pwhite, Pwrand) bilden das
// rhythmische Geflecht; jede Grille wird als CricketSound im Raum positioniert und von einer
// eigenen Pattern-Routine über das gate-Control getriggert.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über Lautsprecher.

// --- 1) Setup: 36 CricketSounds im Raum ---------------------------------------------
(
CricketSound.addSynthDef;
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 10, height: 3, surface: 0.5);

// Tempo und Raster
~bpm = 110;
~beatsPerBar = 4;
~numBars = 16;
~stepsPerBeat = 4;              // 16tel-Noten
~stepDur = 60 / ~bpm / ~stepsPerBeat;
~stepsPerBar = ~beatsPerBar * ~stepsPerBeat;
~totalSteps = ~numBars * ~stepsPerBar;

// --- Layer-Definitionen --------------------------------------------------------------
// Jede Schicht hat eine eigene Klangfarbe und räumliche Positionierung. Die Patterns sind
// weiter unten als lesbare Strings definiert und lassen sich dort unkompliziert ändern.
//
// kickLayer:    4 Stimmen, tief und lang — die "Bassdrum"
// snareLayer:   6 Stimmen, mittel, scharf — "Snare/Clap"
// hihatLayer:  10 Stimmen, hell und kurz — "Hihat"
// textureLayer: 16 Stimmen, dichte, probabilistische Füllung — atmosphärisches Rauschen

~layers = [
	(name: \kick,    count: 4,  baseAmp: 0.55, chirpDur: 0.18),
	(name: \snare,   count: 6,  baseAmp: 0.42, chirpDur: 0.10),
	(name: \hihat,   count: 10, baseAmp: 0.22, chirpDur: 0.04),
	(name: \texture, count: 16, baseAmp: 0.12, chirpDur: 0.06)
];

// Pro Stimme: Klangparameter abhängig von der Schicht. Diese Werte sind ein erster
// Ausgangspunkt — Feintuning passiert nach dem Hörtest.
~voiceSpecs = [];
~layers.do { |layer|
	layer[\count].do { |i|
		var spec = switch(layer[\name],
			\kick,    [ (wingRate: 16, wingDuty: 0.60, ringFreq1: 900,  ringFreq2: 950,
						ringDecay1: 0.08, ringDecay2: 0.06, amp: layer[\baseAmp]) ],
			\snare,   [ (wingRate: 32, wingDuty: 0.35, ringFreq1: 2500, ringFreq2: 2600,
						ringDecay1: 0.03, ringDecay2: 0.02, amp: layer[\baseAmp]) ],
			\hihat,   [ (wingRate: 55, wingDuty: 0.25, ringFreq1: 7000, ringFreq2: 7200,
						ringDecay1: 0.008, ringDecay2: 0.006, amp: layer[\baseAmp]) ],
			\texture, [ (wingRate: 38, wingDuty: 0.40, ringFreq1: 3500, ringFreq2: 3600,
						ringDecay1: 0.02, ringDecay2: 0.015, amp: layer[\baseAmp]) ]
		).first;
		~voiceSpecs = ~voiceSpecs.add((
			layer: layer[\name],
			layerIndex: i,
			soundParams: spec ++ (pattern: nil),
			chirpDur: layer[\chirpDur],
			ampScale: 1.0
		));
	};
};

// Räumliche Verteilung: Kick nah und zentriert vor dem Hörer, Snare/Hihat seitlich,
// Textur weit gestreut. Jede Stimme bekommt leichte Zufallsstreuung, damit sich 36 Stimmen
// nicht auf einen Punkt zusammenziehen.
~soundObjects = ~voiceSpecs.collect { |spec, i|
	var layer = spec[\layer];
	var angle, radius, z;
	var movableParams;

	switch(layer,
		\kick, {
			angle = rrand(-0.3, 0.3);           // leicht vorne
			radius = rrand(2.0, 3.0);           // nah
			z = rrand(-0.2, 0.2);
		},
		\snare, {
			angle = (pi * 0.5) + rrand(-0.4, 0.4);  // rechts/links
			radius = rrand(3.0, 4.5);
			z = rrand(-0.3, 0.3);
		},
		\hihat, {
			angle = (pi * 1.5) + rrand(-0.6, 0.6);  // um den Hörer herum
			radius = rrand(3.5, 5.5);
			z = rrand(-0.2, 0.5);
		},
		\texture, {
			angle = 2pi.rand;
			radius = rrand(5.0, 8.0);           // weit
			z = rrand(-0.5, 0.5);
		}
	);

	movableParams = (
		pos: [radius * cos(angle), radius * sin(angle), z],
		moveRule: SteadyMoveRule.new,
		roomRadius: radius + 2
	);

	~room.registerFromBuilder(spec[\soundParams], movableParams, soundClass: CricketSound)
};

// Gotcha: CricketSound.new(pattern: nil) schaltet das eingebaute CallingPattern NICHT ab
// (siehe CLAUDE.md). Muss pro Sound-Instanz explizit auf nil setzen, BEVOR ~room.play läuft.
~soundObjects.do { |so| so.sound.pattern = nil };

// Synths initial stumm setzen, damit nichts dauerzirpt, bevor die Patterns starten.
~soundObjects.do { |so| so.sound.synth.set(\gate, 0) };
)

// --- 2) Groove-Patterns -------------------------------------------------------------
// Hier wird der Groove definiert. Jede Schicht hat ein Array von 16 Strings — einer pro
// Takt. Jedes Zeichen ist ein 16tel-Schritt: 'x' = Grille zirpt, '.' = Pause.
// So lässt sich der Groove direkt editieren, ohne den Rest des Skripts anzufassen.
//
// Tipp: Um Variationen über die 16 Takte zu bauen, einfach die 16 Strings pro Schicht
// abwandeln. Um die Dichte zu erhöhen, mehr 'x' einsetzen. Um polyrhythmische Layer zu
// erhalten, unterschiedliche Perioden pro Schicht wählen.

(
~kickPattern = Array.fill(16, "x...x...x.......");   // Bassdrum auf 1 und 3
~snarePattern = Array.fill(16, "....x.......x...");  // Snare auf 2 und 4
~hihatPattern = Array.fill(16, "x.x.x.x.x.x.x.x.");  // geschlossene Hihat auf 16tel

// Texture-Layer: weichere, nicht metrische Dichte — pro Takt ein zufällig gefüllter String,
// der bei jedem Start neu berechnet wird (siehe ~buildTexturePattern unten).
~textureDensity = 0.25;   // Anteil 'x' pro Takt (0..1)

// --- Pattern-Hilfsfunktionen ---------------------------------------------------------

// Wandelt einen String wie "x...x..." in einen Array von Gates (1/0) um.
~stringToGates = { |str|
	Array.fill(str.size, { |i| if(str[i] == $x) { 1 } { 0 } })
};

// Wandelt ein Array von 16 Takt-Strings in einen langen Gate-Stream für die gesamte Länge.
~barsToGateStream = { |barStrings|
	var gates = barStrings.collect(~stringToGates).flat;
	Pseq(gates, 1);
};

// Baut den Texture-Pattern-Stream aus 16 zufällig gefüllten Takten.
~buildTexturePattern = {
	var bars = ~numBars.collect {
		String.fill(~stepsPerBar, {
			if(~textureDensity.coin) { $x } { $. }
		})
	};
	~barsToGateStream.value(bars);
};

// Jede Stimme bekommt ihren eigenen leicht variierten Stream:
// - timing jitter (Pwhite auf den stepDur)
// - Amplituden-Skaling pro Stimme
// - Texture-Stream pro Stimme neu zufällig
~voicePatterns = ~voiceSpecs.collect { |spec, i|
	var layer = spec[\layer];
	var baseStream;
	var jitter;
	var probability;

	switch(layer,
		\kick,    { baseStream = ~barsToGateStream.value(~kickPattern) },
		\snare,   { baseStream = ~barsToGateStream.value(~snarePattern) },
		\hihat,   { baseStream = ~barsToGateStream.value(~hihatPattern) },
		\texture, { baseStream = ~buildTexturePattern.value }
	);

	// Timing-Jitter: Stimmen in derselben Schicht nicht exakt synchron, damit es organisch
	// bleibt. Kick bekommt kaum Jitter (präzise), Texture viel.
	jitter = switch(layer,
		\kick,    { 0.02 },
		\snare,   { 0.04 },
		\hihat,   { 0.06 },
		\texture, { 0.10 }
	);

	// Zufällige Auslassungen in der Hihat- und Texture-Schicht für menschlichere Dichte.
	probability = switch(layer,
		\kick,    { 1.0 },
		\snare,   { 1.0 },
		\hihat,   { 0.85 },
		\texture, { 0.70 }
	);

	(
		stream: Pseed(i * 12345,
			Pwhite(probability * 0.9, probability, inf) * baseStream
		),
		stepDur: ~stepDur * Pwhite(1 - jitter, 1 + jitter, inf),
		chirpDur: spec[\chirpDur],
		ampScale: spec[\ampScale]
	)
};
)

// --- 3) Trigger-Routinen und Abspielen ---------------------------------------------
(
~routines = ~voicePatterns.collect { |pat, i|
	var synth = ~soundObjects[i].sound.synth;
	var stream = pat[\stream].asStream;
	var durStream = pat[\stepDur].asStream;
	Routine({
		~totalSteps.do {
			var gate = stream.next;
			var dur = durStream.next;
			if(gate.notNil and: { gate > 0 }) {
				var chirpDur = min(pat[\chirpDur], dur * 0.9);
				synth.set(\amp, ~voiceSpecs[i][\soundParams][\amp] * pat[\ampScale]);
				synth.set(\gate, 1);
				SystemClock.sched(chirpDur, { synth.set(\gate, 0) });
			};
			dur.wait;
		}
	});
};

"Beginn des Cricket-Techno-Experiments.".postln;
~room.play;
~routines.do(_.play);
)

// --- 4) Control-Panel (optional) -----------------------------------------------------
(
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
	+/+ "cricket_techno").fullPath;
~recordingsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname
	+/+ "recordings").fullPath;
~controlPanel = RoomControlPanel.new(~room, viewRadius: 10, editable: true,
	presetsDir: ~presetsDir, recordingsDir: ~recordingsDir);
~controlPanel.show;
)

// --- 5) Stoppen ---------------------------------------------------------------------
(
~routines.do(_.stop);
~soundObjects.do { |so| so.fadeOutAndStop(0.5, { |stopped| ~room.orchestra.unregister(stopped) }) };
~controlPanel.free;
)
