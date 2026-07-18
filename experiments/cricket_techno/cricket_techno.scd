// cricket_techno.scd — 36-stimmiges Grillen-Experiment im Stil eines low-beat-art-Techno.
// Kein Chaos-zu-Groove-Übergang und kein bio-mathematisches Modell (anders als
// cricket_groove.scd/cricket_chorus.scd, Intents 61/62): der Groove ist sofort da, direkt
// editierbar und wird ausschliesslich von SuperCollider-Patterns (Pseq, Pwhite, Pwrand)
// gesteuert. Jede Grille ist ein CricketSound im Raum, der von einer Pattern-Routine über
// das gate-Control getriggert wird.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über Lautsprecher.

// --- 1) Setup: 36 CricketSounds im Raum ---------------------------------------------
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
~totalSteps = ~numBars * ~stepsPerBar;

// --- Layer-Definitionen --------------------------------------------------------------
// Acht Schichten mit unterschiedlichen Klangfarben und räumlichen Positionen. Die Patterns
// sind weiter unten aus lesbaren Bausteinen zusammengesetzt und lassen sich dort direkt
// editieren.
//
// kick:     4 Stimmen, tief/lang — Bassdrum
// sub:      2 Stimmen, sehr tief, spärlich — Sub-Bass-Akzente
// snare:    5 Stimmen, mittel/scharf — Snare + Ghost Notes
// clap:     4 Stimmen, hell/schnell — Clap/Offbeat-Akzente
// chihat:   8 Stimmen, hell/kurz — geschlossene Hihat, treibend
// ohihat:   4 Stimmen, hell/länger — offene Hihat/Ride
// perc:     5 Stimmen, variabel — Tom/Percussion-Fills
// texture:  4 Stimmen, weit gestreut — atmosphärische Füllung

~layers = [
	(name: \kick,    count: 4, baseAmp: 0.55, chirpDur: 0.18),
	(name: \sub,     count: 2, baseAmp: 0.45, chirpDur: 0.30),
	(name: \snare,   count: 5, baseAmp: 0.40, chirpDur: 0.09),
	(name: \clap,    count: 4, baseAmp: 0.32, chirpDur: 0.05),
	(name: \chihat,  count: 8, baseAmp: 0.18, chirpDur: 0.03),
	(name: \ohihat,  count: 4, baseAmp: 0.14, chirpDur: 0.08),
	(name: \perc,    count: 5, baseAmp: 0.22, chirpDur: 0.12),
	(name: \texture, count: 4, baseAmp: 0.10, chirpDur: 0.06)
];

// Pro Stimme: Klangparameter abhängig von der Schicht.
~voiceSpecs = [];
~layers.do { |layer|
	layer[\count].do { |i|
		var spec = switch(layer[\name],
			\kick,    [ (wingRate: 16, wingDuty: 0.60, ringFreq1: 900,  ringFreq2: 950,
						ringDecay1: 0.10, ringDecay2: 0.08, amp: layer[\baseAmp]) ],
			\sub,     [ (wingRate: 10, wingDuty: 0.70, ringFreq1: 500,  ringFreq2: 520,
						ringDecay1: 0.18, ringDecay2: 0.15, amp: layer[\baseAmp]) ],
			\snare,   [ (wingRate: 34, wingDuty: 0.32, ringFreq1: 2400, ringFreq2: 2500,
						ringDecay1: 0.035, ringDecay2: 0.025, amp: layer[\baseAmp]) ],
			\clap,    [ (wingRate: 45, wingDuty: 0.28, ringFreq1: 4500, ringFreq2: 4600,
						ringDecay1: 0.012, ringDecay2: 0.010, amp: layer[\baseAmp]) ],
			\chihat,  [ (wingRate: 60, wingDuty: 0.22, ringFreq1: 7500, ringFreq2: 7700,
						ringDecay1: 0.006, ringDecay2: 0.005, amp: layer[\baseAmp]) ],
			\ohihat,  [ (wingRate: 48, wingDuty: 0.30, ringFreq1: 6000, ringFreq2: 6100,
						ringDecay1: 0.020, ringDecay2: 0.015, amp: layer[\baseAmp]) ],
			\perc,    [ (wingRate: 28, wingDuty: 0.45, ringFreq1: 1600, ringFreq2: 1700,
						ringDecay1: 0.05, ringDecay2: 0.04, amp: layer[\baseAmp]) ],
			\texture, [ (wingRate: 36, wingDuty: 0.42, ringFreq1: 3200, ringFreq2: 3300,
						ringDecay1: 0.025, ringDecay2: 0.020, amp: layer[\baseAmp]) ]
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

// Räumliche Verteilung: tiefe Schichten nah und zentriert, helle/perkussive weiter und
// gestreut. Jede Stimme bekommt leichte Zufallsstreuung.
~soundObjects = ~voiceSpecs.collect { |spec, i|
	var layer = spec[\layer];
	var angle, radius, z;
	var movableParams;

	switch(layer,
		\kick, {
			angle = rrand(-0.3, 0.3);
			radius = rrand(2.0, 3.0);
			z = rrand(-0.2, 0.2);
		},
		\sub, {
			angle = rrand(-0.2, 0.2);
			radius = rrand(2.5, 3.5);
			z = rrand(-0.3, 0.0);
		},
		\snare, {
			angle = (pi * 0.5) + rrand(-0.4, 0.4);
			radius = rrand(3.0, 4.5);
			z = rrand(-0.3, 0.3);
		},
		\clap, {
			angle = (pi * 1.5) + rrand(-0.5, 0.5);
			radius = rrand(3.5, 5.0);
			z = rrand(0.0, 0.4);
		},
		\chihat, {
			angle = 2pi.rand;
			radius = rrand(3.5, 5.5);
			z = rrand(-0.2, 0.5);
		},
		\ohihat, {
			angle = 2pi.rand;
			radius = rrand(4.0, 6.0);
			z = rrand(0.1, 0.6);
		},
		\perc, {
			angle = 2pi.rand;
			radius = rrand(4.5, 6.5);
			z = rrand(-0.4, 0.4);
		},
		\texture, {
			angle = 2pi.rand;
			radius = rrand(6.0, 9.0);
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
)

// --- 2) Groove-Patterns -------------------------------------------------------------
// Der Groove wird aus Bausteinen (kurzen Takt-Strings) pro Schicht zusammengesetzt. So
// bleibt er übersichtlich editierbar, aber über 16 Takte variiert. Jede Schicht hat ihre
// eigene Baustein-Sammlung (~xxxBars) und eine 16-elementige Sequenz (~xxxSeq), die angibt,
// welcher Baustein an welcher Takt-Position gespielt wird.
//
// 'x' = Grille zirpt auf diesem 16tel-Schritt, '.' = Pause.

(
// ---- KICK: abwechslungsreicher, nicht nur bum-bum-buuuum ----
~kickBars = [
	"x...x...x.......",   // 0: Kick auf 1 und 3 (half-time-feel)
	"x...x...x...x...",   // 1: vier auf dem Floor
	"x...x.x.x...x...",   // 2: Kick auf 1, 16tel-Flur, 3
	"x...x...x...x.x.",   // 3: vier auf dem Floor + Vorgriff
	"x...x...x.x.x...",   // 4: Kick auf 1, 3 und Nachschlag
	"x.x.x...x......."    // 5: synkopiert
];
~kickSeq = [0, 1, 0, 2, 0, 1, 0, 3, 0, 1, 0, 2, 0, 1, 0, 4];
~kickPattern = ~kickSeq.collect { |i| ~kickBars[i] };

// ---- SUB: sehr spärliche, tiefe Akzente ----
~subBars = [
	"x........................",   // 0: ein Sub-Schlag pro Takt (24tel)
	"........................",   // 1: Pause
	"x.......x................"    // 2: zwei Sub-Schläge
];
// Sub-Schicht arbeitet mit 8tel-Auflösung, damit die langen Töne nicht zu dicht werden.
~subSeq = [0, 1, 0, 1, 2, 1, 0, 1, 0, 1, 0, 1, 2, 1, 0, 1];

// ---- SNARE: Backbeat + Ghost Notes ----
~snareBars = [
	"....x.......x...",   // 0: klassischer Backbeat
	"....x...x...x...",   // 1: Backbeat + Offbeat
	"....x.......x.x.",   // 2: Backbeat + Vorgriff
	"x...x...x...x...",   // 3: Roll
	"................"    // 4: Pause
];
~snareSeq = [0, 0, 1, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 2, 3, 4];
~snarePattern = ~snareSeq.collect { |i| ~snareBars[i] };

// ---- CLAP: scharfe, spärliche Akzente ----
~clapBars = [
	"....x.......x...",   // 0: Backbeat-Clap
	"x.......x.......",   // 1: halbzeit-Clap
	"....x...x....x..",   // 2: synkopiert
	"................"    // 3: Pause
];
~clapSeq = [0, 3, 0, 2, 0, 3, 1, 0, 0, 3, 0, 2, 0, 3, 0, 0];
~clapPattern = ~clapSeq.collect { |i| ~clapBars[i] };

// ---- CHIHAT: treibende 16tel mit Dichte-Variation ----
~chihatBars = [
	"x.x.x.x.x.x.x.x.",   // 0: geschlossene 16tel
	".x.x.x.x.x.x.x.x",   // 1: versetzt
	"x...x...x...x...",   // 2: Achtel
	"x.x.x.x...x.x.x.",   // 3: mit Lücken
	"................"    // 4: Pause
];
~chihatSeq = [0, 0, 1, 0, 0, 3, 0, 0, 0, 1, 0, 0, 0, 2, 0, 4];
~chihatPattern = ~chihatSeq.collect { |i| ~chihatBars[i] };

// ---- OHIHAT: offene Akzente ----
~ohihatBars = [
	"..x...x...x...x.",   // 0: offene Hihat auf den Offbeats
	"x...x...x...x...",   // 1: jeder Beat
	"....x.......x...",   // 2: Backbeat
	"................"    // 3: Pause
];
~ohihatSeq = [0, 3, 0, 0, 1, 3, 0, 0, 0, 3, 0, 1, 0, 3, 2, 0];
~ohihatPattern = ~ohihatSeq.collect { |i| ~ohihatBars[i] };

// ---- PERC: Fills und Polyrhythmen ----
~percBars = [
	"x...x...x...x...",   // 0: gleichmässig
	"x.x.x.x.x.x.x.x.",   // 1: 16tel
	"............x...",   // 2: Taktende-Akzent
	"........x.x.x...",   // 3: Triolen-Fill
	"................",   // 4: Pause
	"x...x...x...x.x."    // 5: Vorgriff
];
~percSeq = [4, 4, 0, 4, 4, 2, 4, 4, 4, 0, 4, 3, 4, 4, 1, 5];
~percPattern = ~percSeq.collect { |i| ~percBars[i] };

// ---- TEXTURE: zufällige, weiche Dichte ----
~textureDensity = 0.20;

// --- Pattern-Hilfsfunktionen ---------------------------------------------------------

// Wandelt einen String wie "x...x..." in einen Array von Gates (1/0) um.
~stringToGates = { |str|
	Array.fill(str.size, { |i| if(str[i] == $x) { 1 } { 0 } })
};

// Wandelt ein Array von Takt-Strings in einen langen Gate-Stream um.
~barsToGateStream = { |barStrings|
	var gates = barStrings.collect(~stringToGates).flat;
	Pseq(gates, 1);
};

// Spezialfall Sub: 24tel-Auflösung (längere Strings) auf 16tel herunterrechnen.
// Jeder 24tel-Schritt wird auf den nächsten 16tel-Schritt gemappt.
~subBarsToGateStream = { |barStrings|
	var gates = barStrings.collect { |str|
		~stepsPerBar.collect { |step|
			var idx = (step * str.size / ~stepsPerBar).floor.asInteger;
			if(str[idx] == $x) { 1 } { 0 }
		}
	}.flat;
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

// Jede Stimme bekommt ihren eigenen leicht variierten Stream.
~voicePatterns = ~voiceSpecs.collect { |spec, i|
	var layer = spec[\layer];
	var baseStream;
	var jitter;
	var probability;

	switch(layer,
		\kick,    { baseStream = ~barsToGateStream.value(~kickPattern) },
		\sub,     { baseStream = ~subBarsToGateStream.value(~subSeq.collect { |j| ~subBars[j] }) },
		\snare,   { baseStream = ~barsToGateStream.value(~snarePattern) },
		\clap,    { baseStream = ~barsToGateStream.value(~clapPattern) },
		\chihat,  { baseStream = ~barsToGateStream.value(~chihatPattern) },
		\ohihat,  { baseStream = ~barsToGateStream.value(~ohihatPattern) },
		\perc,    { baseStream = ~barsToGateStream.value(~percPattern) },
		\texture, { baseStream = ~buildTexturePattern.value }
	);

	// Timing-Jitter: Kick präzise, Texture organisch.
	jitter = switch(layer,
		\kick,    { 0.02 },
		\sub,     { 0.01 },
		\snare,   { 0.04 },
		\clap,    { 0.05 },
		\chihat,  { 0.06 },
		\ohihat,  { 0.05 },
		\perc,    { 0.07 },
		\texture, { 0.12 }
	);

	// Wahrscheinlichkeit, mit der ein geplanter Schlag tatsächlich ausgelöst wird.
	probability = switch(layer,
		\kick,    { 1.00 },
		\sub,     { 1.00 },
		\snare,   { 0.95 },
		\clap,    { 0.90 },
		\chihat,  { 0.85 },
		\ohihat,  { 0.80 },
		\perc,    { 0.75 },
		\texture, { 0.65 }
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
"Beginn des Cricket-Techno-Experiments.".postln;

// Zuerst Room starten — erst dadurch existieren die Synths auf den Sound-Instanzen.
~room.play;

// Alle Grillen-Synths initial stumm setzen.
~soundObjects.do { |so| so.sound.synth.set(\gate, 0) };

// Trigger-Routinen bauen (jetzt existieren die Synths).
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
