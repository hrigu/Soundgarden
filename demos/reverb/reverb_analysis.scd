// reverb_analysis.scd - kontrolliertes Analyse-Setup fuer den Room-Hall:
// vier stationaere Insekten an festen Positionen, gehoert ueber AtkBinauralizer.
//
// Vorher (in dieser Reihenfolge):
//   1) boot/load_classes.scd - nur beim Erst-Setup noetig (danach automatisch)
//   2) boot/boot.scd         - bei jedem Start: Server muss laufen
//   3) ATK installiert (Quark + Kernel/Matrizen/Sounds, siehe README)
//
// Kopfhoerer benutzen - HRTF/Binauralisierung ist ueber Lautsprecher nicht sinnvoll
// beurteilbar.

// --- 1) Setup: SynthDefs registrieren + vier stationaere Insekten anlegen ---
(
var insectBase, insectVariants, positions;

InsectSound.addSynthDef;
RoomReverb.addSynthDef;

~room = Room.new(s, size: 8, height: 3, surface: 1, mix: 0.7);

// AtkBinauralizer laedt den HRTF-Kernel asynchron und registriert danach die SynthDef.
// Vor Block 2 etwas Zeit lassen, damit Kernel/SynthDef sicher bereit sind.
~room.addSynthDef(AtkBinauralizer, subjectID: 21);

// Gemeinsame Basis: bewusst ohne pattern. Task 2 setzt daraus ein sequenzielles
// Zirp-Muster pro Insekt; Task 1 etabliert nur die stationaeren Quellen.
insectBase = (
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
insectVariants = [
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

~insects = positions.collect { |pos, index|
	~room.registerFromBuilder(
		insectBase ++ insectVariants[index],
		(pos: pos, moveRule: SteadyMoveRule.new, roomRadius: 6),
		reverbMix: 0.7
	)
};

"Raumhall-Analyse: 4 stationaere Insekten registriert.\n".post;
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
