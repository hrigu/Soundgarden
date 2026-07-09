// crickets.scd — Grillenchor: mehrere CricketSound-Objekte (Intent 42), deren Klangfarbe auf
// recherchierter Gryllus-bimaculatus-Bioakustik beruht (siehe CricketSound.sc). Anders als
// insects.scd (grosser, generischer Insekten-Schwarm) hier bewusst wenige Objekte -- ein
// Grillenchor, kein Schwarm. Jede Instanz bekommt automatisch ein eigenes, per
// RhythmPatternCreator generiertes Pattern (CricketSound-Default, siehe CricketSound.makePattern)
// -- dadurch zirpen die Grillen nicht synchron im Unisono.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
var num_crickets;
var cricketMovableBase, cricketSoundBase;

CricketSound.addSynthDef;
RoomReverb.addSynthDef;
~room = Room.new(s, size: 8, height: 3, surface: 1, mix: 1);
~room.addSynthDef(AtkBinauralizer, subjectID: 21);


cricketMovableBase = (pos: [0, 2.5, 0], moveRule: CircularMoveRule.new, roomRadius: 5);
cricketSoundBase = (); // CricketSound.new-Defaults reichen als Basis (Bioakustik-Werte)

num_crickets = 8;

~crickets = num_crickets.collect { |i|
	var angle = (i / num_crickets) * 2pi;
	var radius = rrand(2.0, 4.0);
	var soundParams = cricketSoundBase ++ (
		// leichte Individualvarianz je Grille, wie insects.scd es für InsectSound tut --
		// Wertebereiche eng um die recherchierten Defaults (CricketSound.new), damit der Chor
		// erkennbar nach Grille klingt und nicht in generisches Insektenbrummen abdriftet.
		wingRate: 27 + rrand(-3, 3),
		ringFreq1: 4600 + rrand(-100, 150),
		amp: 0.35 * rrand(0.7, 1.2)
	);
	var movableParams = cricketMovableBase ++ (
		pos: [radius * cos(angle), radius * sin(angle), 0],
		moveRule: CircularMoveRule.new(baseRadius: radius, angularSpeed: rrand(0.1, 0.4)
			* [1, -1].choose)
	);
	~room.registerFromBuilder(soundParams, movableParams, soundClass: CricketSound)
};

// --- 2) Abspielen ---

)


~room.play;

(
~controlPanel = SpatialControlPanel.new(~room, viewRadius: 6, editable: true);
~controlPanel.play;
)


// --- 4) Stoppen ---
// room.stop stoppt das Ticken, ruft stop auf allen registrierten Soundobjekten auf
// (Synths/Busse werden freigegeben) und stoppt danach den Hall.
~room.stop;

// AtkBinauralizer.teardown gibt den geteilten HRTF-Decoder-Kernel frei (Server-Buffer) —
// separat, weil der Kernel der Klasse gehört, nicht einer einzelnen Instanz/room.stop.
AtkBinauralizer.teardown;

// Tastatursteuerung separat stoppen (schließt das Kontrollfenster) — nur nötig,
// falls Block 3 überhaupt ausgeführt wurde.
~controlPanel.stop;



//den Raum verändern
~room.size = 12;
~room.height = 3;
~room.surface = 1; // 0 = rau/absorbierend, 1 = glatt/hart
~room.mix = 0.2;
