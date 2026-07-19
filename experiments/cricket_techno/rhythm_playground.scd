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
OneShotSampleSound.addSynthDef;
~listener = Listener.new(binauralizerClass: Binauralizer);
~room = Room.new(s, listener: ~listener, size: 10, height: 3, surface: 0.5);
~repoRoot = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname).fullPath;
~patternPresetsDir = ~repoRoot +/+ "presets" +/+ "rhythm_playground";

~bpm = 110;
~beatsPerBar = 4;
~soundMode = \cricket;

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

// --- Laufzeit-Variablen ---------------------------------------------------------------

~playgroundRoutines = nil;
~playgroundSoundObjects = nil;
~playgroundIsRunning = false;
~playgroundRoomIsPlaying = false;
~playgroundStopToken = 0;

~playgroundDefaultSampleParams = { |layerName|
	var def = ~layerDefs[layerName];
	RhythmPlaygroundPercussionSamples.soundParamsFor(layerName, ~repoRoot,
		amp: def[\soundParams][\amp], duration: def[\chirpDur].max(0.12));
};

~playgroundSampleParamsFromControls = { |layerName, controls|
	var sampleControls = controls[layerName][\sample];
	(
		path: RhythmPlaygroundPercussionSamples.pathFor(layerName, ~repoRoot),
		amp: sampleControls[\amp].value.clip(0, 1),
		startFrac: sampleControls[\startFrac].value.clip(0, 1),
		duration: sampleControls[\duration].value.max(0.01)
	)
};

~playgroundPresetEvent = { |controls|
	var layerStates = ();
	~layerNames.do { |layerName|
		layerStates[layerName] = (
			active: controls[layerName][\active].value,
			pattern: controls[layerName][\pattern].value,
			sample: (
				amp: controls[layerName][\sample][\amp].value,
				startFrac: controls[layerName][\sample][\startFrac].value,
				duration: controls[layerName][\sample][\duration].value
			)
		);
	};
	RhythmPlaygroundPatternPresetLibrary.eventFor(~playgroundBPMBox.value, ~soundMode, layerStates);
};

~applyPlaygroundPreset = { |controls, preset|
	var layers = preset[\layers];

	if(preset[\bpm].notNil) {
		~bpm = preset[\bpm];
		~playgroundBPMBox.value = ~bpm;
	};
	if(preset[\soundMode].notNil) {
		~soundMode = preset[\soundMode];
		~playgroundSoundModeMenu.value = if(~soundMode == \percussion) { 1 } { 0 };
	};
	if(layers.notNil) {
		~layerNames.do { |layerName|
			var layerPreset = layers[layerName];
			if(layerPreset.notNil) {
				if(layerPreset[\active].notNil) {
					controls[layerName][\active].value = layerPreset[\active];
				};
				if(layerPreset[\pattern].notNil) {
					controls[layerName][\pattern].value = layerPreset[\pattern];
				};
				if(layerPreset[\sample].notNil) {
					if(layerPreset[\sample][\amp].notNil) {
						controls[layerName][\sample][\amp].value = layerPreset[\sample][\amp];
					};
					if(layerPreset[\sample][\startFrac].notNil) {
						controls[layerName][\sample][\startFrac].value = layerPreset[\sample][\startFrac];
					};
					if(layerPreset[\sample][\duration].notNil) {
						controls[layerName][\sample][\duration].value = layerPreset[\sample][\duration];
					};
				};
			};
		};
	};
};

// --- GUI ------------------------------------------------------------------------------

~buildPlaygroundGUI = {
	var win, controls, startButton, stopButton, restartButton, bpmNumberBox, soundModeMenu;
	var header, presetRow, label, presetNameField, presetMenu, refreshPresetMenu;

	win = Window("Rhythm Pattern Playground", Rect(100, 100, 760, 700));
	win.view.decorator = FlowLayout(win.view.bounds.insetBy(16, 16), Point(0, 4), Point(8, 8));
	controls = ();

	StaticText(win.view, 728@24)
		.string_("Rhythm Pattern Playground")
		.font_(Font.default.copy.size_(16).boldVariant)
		.align_(\left);
	win.view.decorator.nextLine;

	// Header: Start / Stop / Restart / BPM
	header = CompositeView(win.view, 728@36);
	header.decorator = FlowLayout(header.bounds, Point(0, 4), Point(8, 0));

	startButton = Button(header, 100@28).states_([["Start"]]).action_({ ~startPlayground.value(controls) });
	stopButton = Button(header, 100@28).states_([["Stop"]]).action_({ ~stopPlayground.value });
	restartButton = Button(header, 100@28).states_([["Restart"]]).action_({ ~stopPlayground.value; ~startPlayground.value(controls) });

	StaticText(header, 60@28).string_("Klang:").align_(\right);
	soundModeMenu = PopUpMenu(header, 120@28)
		.items_(["Grillen", "Percussion"])
		.value_(if(~soundMode == \percussion) { 1 } { 0 })
		.action_({ |menu| ~soundMode = [\cricket, \percussion][menu.value] });

	StaticText(header, 45@28).string_("BPM:").align_(\right);
	bpmNumberBox = NumberBox(header, 60@28)
		.value_(~bpm)
		.clipLo_(30)
		.clipHi_(240)
		.action_({ |nb| ~bpm = nb.value; });
	win.view.decorator.nextLine;

	presetRow = CompositeView(win.view, 728@36);
	presetRow.decorator = FlowLayout(presetRow.bounds, Point(0, 4), Point(8, 0));
	StaticText(presetRow, 60@28).string_("Preset:").align_(\right);
	presetNameField = TextField(presetRow, 150@24).string_("pattern");
	presetMenu = PopUpMenu(presetRow, 190@24);
	refreshPresetMenu = {
		var names = RhythmPlaygroundPatternPresetLibrary.listNames(~patternPresetsDir);
		presetMenu.items = names;
		if(names.notEmpty) { presetMenu.value = 0 };
	};
	refreshPresetMenu.value;
	Button(presetRow, 100@24).states_([["Speichern"]]).action_({
		var name = presetNameField.string;
		if(name.notEmpty) {
			RhythmPlaygroundPatternPresetLibrary.save(~patternPresetsDir, name,
				~playgroundPresetEvent.value(controls));
			refreshPresetMenu.value;
		};
	});
	Button(presetRow, 80@24).states_([["Laden"]]).action_({
		var name = presetMenu.item;
		var preset = if(name.notNil) {
			RhythmPlaygroundPatternPresetLibrary.load(~patternPresetsDir, name)
		} { nil };
		if(preset.notNil) {
			~applyPlaygroundPreset.value(controls, preset);
			presetNameField.string = name;
		};
	});
	win.view.decorator.nextLine;

	label = StaticText(win.view, 728@20)
		.string_("Aktiv  Layer                Pattern (x = Schlag, . = Pause, | = Segment, || = Takt)")
		.font_(Font.default.copy.size_(11))
		.align_(\left);
	win.view.decorator.nextLine;

	// Pro Layer eine Zeile
	~layerNames.do { |layerName|
		var def = ~layerDefs[layerName];
		var row = CompositeView(win.view, 728@28);
		var sampleRow = CompositeView(win.view, 728@30);
		var checkbox, textField, sampleParams, ampSlider, startSlider, durationSlider;

		row.decorator = FlowLayout(row.bounds, Point(0, 2), Point(8, 0));
		sampleRow.decorator = FlowLayout(sampleRow.bounds, Point(0, 2), Point(8, 0));

		checkbox = CheckBox(row, 20@24).value_(true);
		StaticText(row, 70@24).string_(layerName.asString).align_(\left);
		textField = TextField(row, 600@24).string_(def[\pattern]);

		sampleParams = ~playgroundDefaultSampleParams.value(layerName);
		StaticText(sampleRow, 98@24).string_("Sample").align_(\right);
		ampSlider = EZSlider(sampleRow, 200@24, "Amp", ControlSpec(0, 1, \lin, 0.01),
			nil, sampleParams[\amp], false, 54, 46);
		startSlider = EZSlider(sampleRow, 200@24, "Start", ControlSpec(0, 1, \lin, 0.001),
			nil, sampleParams[\startFrac], false, 54, 46);
		durationSlider = EZSlider(sampleRow, 200@24, "Dauer", ControlSpec(0.01, 2, \lin, 0.01),
			nil, sampleParams[\duration], false, 54, 46);

		controls[layerName] = (
			active: checkbox,
			pattern: textField,
			sample: (amp: ampSlider, startFrac: startSlider, duration: durationSlider)
		);
		win.view.decorator.nextLine;
	};

	win.front;
	~playgroundControls = controls;
	~playgroundBPMBox = bpmNumberBox;
	~playgroundSoundModeMenu = soundModeMenu;
};

// --- Start / Stop ----------------------------------------------------------------------

~startPlayground = { |controls|
	var activeLayers;
	var soundMode;
	var startToken;

	if(~playgroundIsRunning) { ~stopPlayground.value };

	activeLayers = ~layerNames.select { |layerName|
		controls[layerName][\active].value
	};

	if(activeLayers.isEmpty) {
		"Kein Layer aktiv — Start abgebrochen.".postln;
		^nil;
	};

	~playgroundStopToken = ~playgroundStopToken + 1;
	startToken = ~playgroundStopToken;
	~bpm = ~playgroundBPMBox.value;
	~barDur = 60 / ~bpm * ~beatsPerBar;
	soundMode = ~soundMode;

	~playgroundSoundObjects = activeLayers.collect { |layerName, i|
		var def = ~layerDefs[layerName];
		var patternString = controls[layerName][\pattern].value;
		var soundParams;
		var angle = (i / activeLayers.size) * 2pi;
		var radius = 3.0;
		var movableParams = (
			pos: [radius * sin(angle), radius * cos(angle), 0],
			moveRule: SteadyMoveRule.new,
			roomRadius: radius + 2
		);

		if(soundMode == \percussion) {
			soundParams = ~playgroundSampleParamsFromControls.value(layerName, controls);
			~room.register(
				Movable.new(
					pos: movableParams[\pos],
					moveRule: movableParams[\moveRule],
					roomRadius: movableParams[\roomRadius]
				),
				OneShotSampleSound.new(
					soundParams[\path],
					amp: soundParams[\amp],
					startFrac: soundParams[\startFrac],
					duration: soundParams[\duration]
				)
			)
		} {
			soundParams = def[\soundParams].copy;
			// Pattern speichern wir temporär im Sound-Event mit, falls nützlich.
			~room.registerFromBuilder(soundParams ++ (pattern: nil), movableParams,
				soundClass: CricketSound)
		};
	};

	if(soundMode == \cricket) {
		~playgroundSoundObjects.do { |so| so.sound.pattern = nil };
	} {
		~playgroundSoundObjects.do { |so| so.sound.preload(s) };
	};
	if(~playgroundRoomIsPlaying) {
		~playgroundSoundObjects.do { |so| so.play(s) };
	} {
		~room.play;
		~playgroundRoomIsPlaying = true;
	};
	if(soundMode == \cricket) {
		~playgroundSoundObjects.do { |so| so.sound.synth.set(\gate, 0) };
	};

	~playgroundRoutines = activeLayers.collect { |layerName, i|
		var def = ~layerDefs[layerName];
		var patternString = controls[layerName][\pattern].value;
		var stream;
		var sound = ~playgroundSoundObjects[i].sound;
		var synth = sound.synth;
		var jitterStream = Pwhite(0.95, 1.05, inf).asStream;

		stream = RhythmPatternNotation.eventStream(patternString, ~barDur).asStream;

		Routine({
			loop {
				var event = stream.next;
				var dur = event[\dur] * jitterStream.next;
				if(event[\gate].notNil and: { event[\gate] > 0 }) {
					var chirpDur = min(def[\chirpDur], dur * 0.9);
					if(soundMode == \percussion) {
						sound.trigger;
					} {
						synth.set(\gate, 1);
						SystemClock.sched(chirpDur, { synth.set(\gate, 0) });
					};
				};
				dur.wait;
			}
		});
	};

	if(soundMode == \percussion) {
		SystemClock.sched(0.2, {
			if(~playgroundStopToken == startToken and: { ~playgroundIsRunning }) {
				~playgroundRoutines.do(_.play);
			};
			nil
		});
	} {
		~playgroundRoutines.do(_.play);
	};
	~playgroundIsRunning = true;
	("Playground gestartet (" ++ soundMode.asString ++ "): "
		++ activeLayers.collect(_.asString).join(", ")).postln;
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
