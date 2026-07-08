// reverb_song.scd - Reverb-Demo mit echtem Songmaterial aus musicnet/downloads.
// Ziel: Hallcharakter mit vollstaendigen Musikstuecken statt nur mit Testsignalen beurteilen.
//
// Vorher:
//   1) boot/load_classes.scd - nur beim Erst-Setup noetig
//   2) boot/boot.scd - bei jedem Start: Server muss laufen
//   3) ATK installiert - sonst schlaegt AtkBinauralizer fehl
//
// Kopfhoerer benutzen - binaurale Effekte funktionieren ueber Lautsprecher nicht sauber.

// --- 1) Setup + Start: Grundgeruest vorbereiten ---
(
var config;

SampleSound.addSynthDef;
RoomReverb.addSynthDef;

// Zentrale Session-Konfiguration fuer den Hoertest.
config = (
	songsDir: "/Users/chrigu/code/my/musicnet/downloads",
	roomSize: 10,
	roomHeight: 3,
	roomSurface: 0.35,
	roomMix: 0.2,
	reverbMix: 0.35,
	viewRadius: 8,
	songPosition: [0, 3, 0],
	songRate: 1,
	songPhase: 0
);

// Kann vor dem Setup gesetzt werden, z.B.:
// ~songName = "Billie Bossa Nova";

~songConfig = config;
~room = Room.new(s, size: config[\roomSize], height: config[\roomHeight],
	surface: config[\roomSurface], mix: config[\roomMix]);
~room.addSynthDef(AtkBinauralizer, subjectID: 21);

// Task 2 baut hier die rekursive Song-Suche und Dateiauswahl ein.
~songFile = nil;
~songObject = nil;

~applyTuning = {
	~room.size = ~songConfig[\roomSize];
	~room.height = ~songConfig[\roomHeight];
	~room.surface = ~songConfig[\roomSurface];
	~room.mix = ~songConfig[\roomMix];
	~songObject !? {
		~songObject.binauralizer.synth !? {
			~songObject.binauralizer.synth.set(\reverbMix, ~songConfig[\reverbMix]);
		};
	};
};

~startDemo = {
	~room.play;
	~applyTuning.();
	"Reverb-Song-Demo: Demo laeuft.\n".post;
};

~showControls = {
	~controlPanel !? { ~controlPanel.stop };
	~controlPanel = SpatialControlPanel.new(~room, viewRadius: ~songConfig[\viewRadius],
		editable: true);
	~controlPanel.play;
	"Reverb-Song-Demo: gemeinsames Steuerungsfenster aktiv.\n".post;
};

~stopDemo = {
	~room.stop;
	~controlPanel !? { ~controlPanel.stop };
	"Reverb-Song-Demo: Demo gestoppt.\n".post;
};

~freeDemo = {
	~stopDemo.();
	~room.teardown;
	AtkBinauralizer.teardown;
	"Reverb-Song-Demo: ATK-Kernel freigegeben.\n".post;
};

"Reverb-Song-Demo: Grundgeruest vorbereitet.\n".post;
)

// --- 2) Abspielen ---
~startDemo.();

// --- 3) Optional: gemeinsames GUI ---
~showControls.();

// --- 4) Stoppen ---
~stopDemo.();

// --- 5) Vollstaendig freigeben ---
~freeDemo.();
