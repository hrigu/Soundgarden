// reverb_analysis.scd - kontrolliertes Analyse-Setup fuer den Room-Hall:
// vier stationaere Insekten an festen Positionen, gehoert ueber AtkBinauralizer.
//

// --- 1) Setup + Start: alles an einer Stelle vorbereiten ---
(
var config, insectMovableBase, insectSoundBase, insectSoundVariants, positions,
	makeSequencePattern, chirpWindow, makeReverbSlider;

InsectSound.addSynthDef;
RoomReverb.addSynthDef;

// Zentrale Session-Konfiguration: diese Werte werden beim Hoertest am haeufigsten
// variiert, deshalb liegen sie gebuendelt an einer Stelle statt verteilt im Skript.
config = (
    roomSize: 10,
    roomHeight: 3,
    roomSurface: 0.35,
    roomMix: 0.16, //0
    reverbMix: 0.41, //0

//	roomSize: 8,
//	roomHeight: 3,
//	roomSurface: 1,
//	roomMix: 0.25,
//	reverbMix: 0.2,
	roomRadius: 6,
	positions: [
		[4, 3, 0],
		[2, 3, 0],
		[0, 3, 0],
		[-2, 0, 0]
	],
	chirpWindowDurations: [0.02, 0.18, 0.02, 0.18, 0.02, 1.58],
	insectSoundBase: (
		// Etwas tiefere Basis: weniger scharfer, glasiger Eindruck und zugleich
		// gut fuer Hallbeurteilung, weil die Ortung nicht nur ueber hohe Resonanzen kommt.
		wingRate: 185,
		wingDuty: 0.28,
		ringFreq1: 2400,
		ringFreq2: 3600,
		ringDecay1: 0.025,
		ringDecay2: 0.02,
		amp: 0.22
	),
	insectSoundVariants: [
		(),
		(wingRate: 225, ringFreq1: 3000, ringFreq2: 4500, amp: 0.2),
		(wingRate: 150, wingDuty: 0.38, ringFreq1: 1900, ringFreq2: 2900, amp: 0.24),
		(wingRate: 270, wingDuty: 0.2, ringFreq1: 3800, ringFreq2: 5600, amp: 0.18)
	]
);

~room = Room.new(s, size: config[\roomSize], height: config[\roomHeight],
	surface: config[\roomSurface], mix: config[\roomMix]);

// AtkBinauralizer laedt den HRTF-Kernel asynchron und registriert danach die SynthDef.
// Vor Block 2 etwas Zeit lassen, damit Kernel/SynthDef sicher bereit sind.
~room.addSynthDef(AtkBinauralizer, subjectID: 21);

// Gemeinsame Basis fuer die Insekten; pattern wird pro Objekt unten gesetzt,
// damit immer nur ein Insekt in seinem eigenen Zeitfenster zirpt.
insectSoundBase = config[\insectSoundBase];

// Kleine Klangunterschiede helfen, die vier festen Positionen beim Hoertest
// auseinanderzuhalten, ohne das Hall-Verhalten durch Bewegung zu vermischen.
insectSoundVariants = config[\insectSoundVariants];

// Listener facing=0 schaut entlang +y (siehe Listener). Die Quellen liegen
// wieder klar auf vorne/rechts/hinten/links, damit die Seitenortung nicht
// durch fast-frontale Winkel oder zu viel Hall verschmiert wird.
positions = config[\positions];
insectMovableBase = (pos: [0, 3, 0], moveRule: SteadyMoveRule.new,
	roomRadius: config[\roomRadius]);

// Ein voller Sequenzumlauf dauert 8s: jedes Insekt bekommt ein 2s-Fenster.
// Die eigentlichen Zirpsegmente kommen aus der Kurzform; nur Vor-/Nachlauf-
// Stille wird explizit als Aus-Segment davor/danach gesetzt.
chirpWindow = CallingPattern.fromDurations(config[\chirpWindowDurations]);
makeSequencePattern = { |prefixSilence, suffixSilence|
	var segments = Array.new;

	if(prefixSilence > 0) { segments = segments.add([prefixSilence, false]) };
	segments = segments ++ chirpWindow.segments;
	if(suffixSilence > 0) { segments = segments.add([suffixSilence, false]) };

	CallingPattern.new(segments)
};

~insects = positions.collect { |pos, index|
	~room.registerFromBuilder(
		insectSoundBase ++ insectSoundVariants[index]
			++ (pattern: makeSequencePattern.(index * 2, 6 - (index * 2))),
		insectMovableBase ++ (pos: pos),
		reverbMix: config[\reverbMix]
	)
};

"Raumhall-Analyse: Demo vorbereitet. Mit ~startDemo.() abspielen.\n".post;

~applyTuning = {
	~room.size = config[\roomSize];
	~room.height = config[\roomHeight];
	~room.surface = config[\roomSurface];
	~room.mix = config[\roomMix];
	~insects.do { |insect| insect.binauralizer.synth.set(\reverbMix, config[\reverbMix]) };
};

~startDemo = {
	~room.play;
	~applyTuning.();
	"Raumhall-Analyse: Demo laeuft.\n".post;
};

~showControls = {
	~listenerControl !? { ~listenerControl.stop };
	~spaceView !? { ~spaceView.stop };
	~listenerControl = KeyboardListenerControl.new(~room.listener);
	~listenerControl.play;
	~spaceView = SpaceView.new(~room.orchestra);
	~spaceView.play(editable: true);
	"Raumhall-Analyse: Steuerung und editierbare Draufsicht aktiv.\n".post;
};

~showReverbControls = {
	~reverbWindow !? { ~reverbWindow.close };
	~reverbWindow = Window.new("Room-Hall", Rect(820, 100, 360, 220));
	~reverbWindow.view.decorator = FlowLayout(~reverbWindow.view.bounds.insetBy(10, 10));

	makeReverbSlider = { |label, key, spec|
		EZSlider(~reverbWindow, 330@24, label, spec, { |ez|
			config[key] = ez.value;
			~applyTuning.();
		}, config[key]);
	};

	makeReverbSlider.("Size", \roomSize, ControlSpec(2, 30, \lin, 0.1));
	makeReverbSlider.("Height", \roomHeight, ControlSpec(1, 20, \lin, 0.1));
	makeReverbSlider.("Surface", \roomSurface, ControlSpec(0, 1, \lin, 0.01));
	makeReverbSlider.("Mix", \roomMix, ControlSpec(0, 1, \lin, 0.01));
	makeReverbSlider.("ReverbSend", \reverbMix, ControlSpec(0, 1, \lin, 0.01));

	~reverbWindow.onClose = { ~reverbWindow = nil };
	~reverbWindow.front;
	"Raumhall-Analyse: Reverb-GUI aktiv.\n".post;
};

~stopDemo = {
	~room.stop;
	~listenerControl !? { ~listenerControl.stop };
	~spaceView !? { ~spaceView.stop };
	~reverbWindow !? { ~reverbWindow.close };
	"Raumhall-Analyse: Demo gestoppt.\n".post;
};

~freeDemo = {
	~stopDemo.();
	~room.teardown;
	AtkBinauralizer.teardown;
	"Raumhall-Analyse: ATK-Kernel freigegeben.\n".post;
};

"Raumhall-Analyse: 4 stationaere Insekten mit sequenziellen Zirp-Fenstern registriert.\n".post;
)

// --- 2) Abspielen ---
~startDemo.();

// --- 3) Optional: Listener-Steuerung + Draufsicht ---
// W/S vor/zurueck, A/D seitlich, Q/E drehen. Fenster muss fokussiert sein.
~showControls.();

// --- 4) Live-Tuning ---
// Raum- und Send-Werte fuer diese Session gesammelt anpassen.
~applyTuning.();

// --- 5) Reverb-GUI ---
~showReverbControls.();

// --- 6) Stoppen ---
~stopDemo.();

// --- 7) Vollstaendig freigeben ---
// Nur noetig, wenn die Session wirklich beendet wird.
~freeDemo.();
