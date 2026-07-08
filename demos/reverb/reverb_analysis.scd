// reverb_analysis.scd - kontrolliertes Analyse-Setup fuer den Room-Hall:
// vier stationaere Insekten an festen Positionen, gehoert ueber AtkBinauralizer.
//

// --- 1) Setup: SynthDefs registrieren + vier stationaere Insekten anlegen ---
(
var insectMovableBase, insectSoundBase, insectSoundVariants, positions, makeSequencePattern,
	chirpWindow;

InsectSound.addSynthDef;
RoomReverb.addSynthDef;

~room = Room.new(s, size: 8, height: 3, surface: 1, mix: 0.7);

// AtkBinauralizer laedt den HRTF-Kernel asynchron und registriert danach die SynthDef.
// Vor Block 2 etwas Zeit lassen, damit Kernel/SynthDef sicher bereit sind.
~room.addSynthDef(AtkBinauralizer, subjectID: 21);

// Gemeinsame Basis fuer die Insekten; pattern wird pro Objekt unten gesetzt,
// damit immer nur ein Insekt in seinem eigenen Zeitfenster zirpt.
insectSoundBase = (
	wingRate: 210,
	wingDuty: 0.25,
	ringFreq1: 3200,
	ringFreq2: 4600,
	ringDecay1: 0.02,
	ringDecay2: 0.015,
	amp: 0.22
);

// Kleine Klangunterschiede helfen, die vier festen Positionen beim Hoertest
// auseinanderzuhalten, ohne das Hall-Verhalten durch Bewegung zu vermischen.
insectSoundVariants = [
	(),
	(wingRate: 260, ringFreq1: 4200, ringFreq2: 6100, amp: 0.2),
	(wingRate: 170, wingDuty: 0.35, ringFreq1: 2600, ringFreq2: 3900, amp: 0.24),
	(wingRate: 320, wingDuty: 0.18, ringFreq1: 5600, ringFreq2: 7600, amp: 0.18)
];

// Listener facing=0 schaut entlang +y (siehe Listener). Die Quellen liegen
// vorne, rechts, hinten, links - alle mit SteadyMoveRule, also ohne Eigenbewegung.
positions = [
	[0, 3, 0],
	[3, 0, 0],
	[0, -3, 0],
	[-3, 0, 0]
];
insectMovableBase = (pos: [0, 3, 0], moveRule: SteadyMoveRule.new, roomRadius: 6);

// Ein voller Sequenzumlauf dauert 8s: jedes Insekt bekommt ein 2s-Fenster.
// Die eigentlichen Zirpsegmente kommen aus der Kurzform; nur Vor-/Nachlauf-
// Stille wird explizit als Aus-Segment davor/danach gesetzt.
chirpWindow = CallingPattern.fromDurations([0.02, 0.18, 0.02, 0.18, 0.02, 1.58]);
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
		reverbMix: 0.7
	)
};

"Raumhall-Analyse: 4 stationaere Insekten mit sequenziellen Zirp-Fenstern registriert.\n".post;
)

// --- 2) Abspielen ---
// Startet Insekten, Binauralizer, Orchestra-Tick und danach den gemeinsamen Hall.
~room.play;

// --- 3) Optional: Listener-Steuerung + Draufsicht ---
// W/S vor/zurueck, A/D seitlich, Q/E drehen. Fenster muss fokussiert sein.
~listenerControl = KeyboardListenerControl.new(~room.listener);
~listenerControl.play;

~spaceView = SpaceView.new(~room.orchestra);
~spaceView.play;

// --- 4) Stoppen ---
~room.stop;
~listenerControl.stop;
~spaceView.stop;
AtkBinauralizer.teardown;
