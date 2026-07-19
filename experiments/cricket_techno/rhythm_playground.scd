// rhythm_playground.scd — isolierter Testraum für Cricket-Techno-Rhythm-Patterns mit GUI.
// Ziel: einzelne Layer oder Kombinationen schnell an- und abschalten, Pattern-Strings direkt
// editieren und sofort neu starten, ohne durch den Code zu scrollen.
//
// Benutzung:
//   1. Block 1 (Setup + GUI) ausführen.
//   2. Im Fenster Layer an-/ausschalten, Pattern-Strings editieren, BPM wählen.
//   3. "Start" drücken.
//   4. "Stop" beendet alles; "Restart" übernimmt aktuelle Einstellungen neu.
//
// Kopfhörer benutzen — binaurale Effekte funktionieren nicht über Lautsprecher.

(
// --- Setup ----------------------------------------------------------------------------
CricketSound.addSynthDef;
~listener = Listener.new(binauralizerClass: Binauralizer);
~room = Room.new(s, listener: ~listener, size: 10, height: 3, surface: 0.5);

~bpm = 110;
~beatsPerBar = 4;

// Layer-Definitionen: Name, Standard-Pattern, Klangparameter, Chirp-Dauer.
// Das Standard-Pattern ist ein einzelner Takt, der endlos loopt.
~layerDefs = (
	kick: (
		pattern: "....|x...|x...|....",
		soundParams: (
			wingRate: 16, wingDuty: 0.60,
			ringFreq1: 900, ringFreq2: 950,
			ringDecay1: 0.10, ringDecay2: 0.08,
			amp: 0.08
		),
		chirpDur: 0.18
	),
	sub: (
		pattern: "x.....|......|......|......",
		soundParams: (
			wingRate: 10, wingDuty: 0.70,
			ringFreq1: 500, ringFreq2: 520,
			ringDecay1: 0.18, ringDecay2: 0.15,
			amp: 0.08
		),
		chirpDur: 0.30
	),
	snare: (
		pattern: "....|x...|....|x...",
		soundParams: (
			wingRate: 34, wingDuty: 0.32,
			ringFreq1: 2400, ringFreq2: 2500,
			ringDecay1: 0.035, ringDecay2: 0.025,
			amp: 0.20
		),
		chirpDur: 0.09
	),
	clap: (
		pattern: "....|x...|....|x...",
		soundParams: (
			wingRate: 45, wingDuty: 0.28,
			ringFreq1: 4500, ringFreq2: 4600,
			ringDecay1: 0.012, ringDecay2: 0.010,
			amp: 0.32
		),
		chirpDur: 0.05
	),
	chihat: (
		pattern: "x.x.|x.x.|x.x.|x.x.",
		soundParams: (
			wingRate: 60, wingDuty: 0.22,
			ringFreq1: 7500, ringFreq2: 7700,
			ringDecay1: 0.006, ringDecay2: 0.005,
			amp: 0.18
		),
		chirpDur: 0.03
	),
	ohihat: (
		pattern: "..x.|...x|...x|...x",
		soundParams: (
			wingRate: 48, wingDuty: 0.30,
			ringFreq1: 6000, ringFreq2: 6100,
			ringDecay1: 0.020, ringDecay2: 0.015,
			amp: 0.34
		),
		chirpDur: 0.08
	),
	perc: (
		pattern: "x...|x...|x...|x...",
		soundParams: (
			wingRate: 28, wingDuty: 0.45,
			ringFreq1: 1600, ringFreq2: 1700,
			ringDecay1: 0.05, ringDecay2: 0.04,
			amp: 0.22
		),
		chirpDur: 0.12
	),
	texture: (
		pattern: "..x.|x...|....|x.x.",
		soundParams: (
			wingRate: 36, wingDuty: 0.42,
			ringFreq1: 3200, ringFreq2: 3300,
			ringDecay1: 0.025, ringDecay2: 0.020,
			amp: 0.30
		),
		chirpDur: 0.06
	)
);

~layerNames = [\kick, \sub, \snare, \clap, \chihat, \ohihat, \perc, \texture];

// --- Pattern-Hilfsfunktionen ----------------------------------------------------------

~splitLogicalBars = { |str|
	var bars = List.new;
	var current = List.new;
	var prevWasBar = false;

	str.do { |c|
		if(c == $|) {
			if(prevWasBar) {
				bars.add(current.asArray);
				current = List.new;
				prevWasBar = false;
			} {
				prevWasBar = true;
			};
		} {
			if(prevWasBar) {
				current.add($|);
				prevWasBar = false;
			};
			current.add(c);
		};
	};
	if(prevWasBar) { current.add($|) };
	bars.add(current.asArray);
	bars.asArray;
};

~splitSegments = { |bar|
	var segments = List.new;
	var current = List.new;

	bar.do { |c|
		if(c == $|) {
			segments.add(current.asArray);
			current = List.new;
		} {
			current.add(c);
		};
	};
	segments.add(current.asArray);
	segments.asArray;
};

~patternToEvents = { |str, barDur|
	~splitLogicalBars.value(str).collect { |bar|
		var segments = ~splitSegments.value(bar);
		var segmentDur = barDur / segments.size.max(1);
		segments.collect { |segment|
			if(segment.isEmpty) {
				[(gate: 0, dur: segmentDur)]
			} {
				var stepDur = segmentDur / segment.size;
				segment.collect { |c| (gate: if(c == $x) { 1 } { 0 }, dur: stepDur) }
			};
		}.flat;
	}.flat;
};

~patternToEventStream = { |barString, barDur|
	Pseq(~patternToEvents.value(barString, barDur), inf);
};

// --- Laufzeit-Variablen ---------------------------------------------------------------

~playgroundRoutines = nil;
~playgroundSoundObjects = nil;
~playgroundIsRunning = false;
~playgroundRoomIsPlaying = false;
~playgroundStopToken = 0;

// --- GUI ------------------------------------------------------------------------------

~buildPlaygroundGUI = {
	var win, controls, startButton, stopButton, restartButton, bpmNumberBox;
	var header, label;

	win = Window("Rhythm Pattern Playground", Rect(100, 100, 720, 420));
	win.view.decorator = FlowLayout(win.view.bounds.insetBy(16, 16), Point(0, 4), Point(8, 8));
	controls = ();

	StaticText(win.view, 688@24)
		.string_("Rhythm Pattern Playground")
		.font_(Font.default.copy.size_(16).boldVariant)
		.align_(\left);
	win.view.decorator.nextLine;

	// Header: Start / Stop / Restart / BPM
	header = CompositeView(win.view, 688@36);
	header.decorator = FlowLayout(header.bounds, Point(0, 4), Point(8, 0));

	startButton = Button(header, 100@28).states_([["Start"]]).action_({ ~startPlayground.value(controls) });
	stopButton = Button(header, 100@28).states_([["Stop"]]).action_({ ~stopPlayground.value });
	restartButton = Button(header, 100@28).states_([["Restart"]]).action_({ ~stopPlayground.value; ~startPlayground.value(controls) });

	StaticText(header, 60@28).string_("BPM:").align_(\right);
	bpmNumberBox = NumberBox(header, 60@28)
		.value_(~bpm)
		.clipLo_(30)
		.clipHi_(240)
		.action_({ |nb| ~bpm = nb.value; });
	win.view.decorator.nextLine;

	label = StaticText(win.view, 688@20)
		.string_("Aktiv  Layer                Pattern (x = Schlag, . = Pause, | = Segment, || = Takt)")
		.font_(Font.default.copy.size_(11))
		.align_(\left);
	win.view.decorator.nextLine;

	// Pro Layer eine Zeile
	~layerNames.do { |layerName|
		var def = ~layerDefs[layerName];
		var row = CompositeView(win.view, 688@28);
		var checkbox, textField;

		row.decorator = FlowLayout(row.bounds, Point(0, 2), Point(8, 0));

		checkbox = CheckBox(row, 20@24).value_(true);
		StaticText(row, 70@24).string_(layerName.asString).align_(\left);
		textField = TextField(row, 560@24).string_(def[\pattern]);

		controls[layerName] = (active: checkbox, pattern: textField);
		win.view.decorator.nextLine;
	};

	win.front;
	~playgroundControls = controls;
	~playgroundBPMBox = bpmNumberBox;
};

// --- Start / Stop ----------------------------------------------------------------------

~startPlayground = { |controls|
	var activeLayers;

	if(~playgroundIsRunning) { ~stopPlayground.value };

	activeLayers = ~layerNames.select { |layerName|
		controls[layerName][\active].value
	};

	if(activeLayers.isEmpty) {
		"Kein Layer aktiv — Start abgebrochen.".postln;
		^nil;
	};

	~playgroundStopToken = ~playgroundStopToken + 1;
	~bpm = ~playgroundBPMBox.value;
	~barDur = 60 / ~bpm * ~beatsPerBar;

	~playgroundSoundObjects = activeLayers.collect { |layerName, i|
		var def = ~layerDefs[layerName];
		var patternString = controls[layerName][\pattern].value;
		var soundParams = def[\soundParams].copy;
		var angle = (i / activeLayers.size) * 2pi;
		var radius = 3.0;
		var movableParams = (
			pos: [radius * sin(angle), radius * cos(angle), 0],
			moveRule: SteadyMoveRule.new,
			roomRadius: radius + 2
		);
		// Pattern speichern wir temporär im Sound-Event mit, falls nützlich.
		~room.registerFromBuilder(soundParams ++ (pattern: nil), movableParams,
			soundClass: CricketSound)
	};

	~playgroundSoundObjects.do { |so| so.sound.pattern = nil };
	if(~playgroundRoomIsPlaying) {
		~playgroundSoundObjects.do { |so| so.play(s) };
	} {
		~room.play;
		~playgroundRoomIsPlaying = true;
	};
	~playgroundSoundObjects.do { |so| so.sound.synth.set(\gate, 0) };

	~playgroundRoutines = activeLayers.collect { |layerName, i|
		var def = ~layerDefs[layerName];
		var patternString = controls[layerName][\pattern].value;
		var stream;
		var synth = ~playgroundSoundObjects[i].sound.synth;
		var jitterStream = Pwhite(0.95, 1.05, inf).asStream;

		stream = ~patternToEventStream.value(patternString, ~barDur).asStream;

		Routine({
			loop {
				var event = stream.next;
				var dur = event[\dur] * jitterStream.next;
				if(event[\gate].notNil and: { event[\gate] > 0 }) {
					var chirpDur = min(def[\chirpDur], dur * 0.9);
					synth.set(\gate, 1);
					SystemClock.sched(chirpDur, { synth.set(\gate, 0) });
				};
				dur.wait;
			}
		});
	};

	~playgroundRoutines.do(_.play);
	~playgroundIsRunning = true;
	("Playground gestartet: " ++ activeLayers.collect(_.asString).join(", ")).postln;
};

~stopPlayground = {
	var soundObjects, remaining, stopToken;

	~playgroundStopToken = ~playgroundStopToken + 1;
	stopToken = ~playgroundStopToken;

	~playgroundRoutines !? { |routines|
		routines.do(_.stop);
		~playgroundRoutines = nil;
	};

	soundObjects = ~playgroundSoundObjects;
	if(soundObjects.notNil) {
		remaining = soundObjects.size;
		soundObjects.do { |so|
			~room.orchestra.unregister(so);
			so.fadeOutAndStop(0.5, {
				remaining = remaining - 1;
				if(remaining <= 0
					and: { ~playgroundStopToken == stopToken }
					and: { ~playgroundIsRunning.not }) {
					~room.stop;
					~playgroundRoomIsPlaying = false;
				};
			})
		};
		~playgroundSoundObjects = nil;
	} {
		if(~playgroundRoomIsPlaying and: { ~playgroundIsRunning.not }) {
			~room.stop;
			~playgroundRoomIsPlaying = false;
		};
	};
	~playgroundIsRunning = false;
	"Playground gestoppt.".postln;
};

~buildPlaygroundGUI.value;
)

// --- Hilfsblock: Fenster erneut öffnen (falls geschlossen) -----------------------------
(
~buildPlaygroundGUI.value;
)
