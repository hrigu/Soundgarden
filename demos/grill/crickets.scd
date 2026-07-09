// crickets.scd — Grillenchor: CricketSound-Objekte (Intent 42, synthetisch, Klangfarbe auf
// recherchierter Gryllus-bimaculatus-Bioakustik) UND optional echte Grillen-Samples
// (SampleSound) im selben Raum, jedes einzeln per Klick in der Raum-Draufsicht auswählbar und
// live editierbar (Intent 43) -- inkl. Preset-Bibliothek zum Speichern/Laden guter
// Klangeinstellungen (siehe SoundPresetLibrary, presets/crickets/). Anders als insects.scd
// (grosser, generischer Insekten-Schwarm) hier bewusst wenige Objekte -- ein Grillenchor, kein
// Schwarm. Jede CricketSound-Instanz bekommt automatisch ein eigenes, per RhythmPatternCreator
// generiertes Pattern (CricketSound-Default) -- dadurch zirpen die Grillen nicht synchron im
// Unisono.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
var num_crickets, num_sample_crickets;
var cricketMovableBase, cricketSoundBase;

// Optional: Pfad zu einem echten Grillen-Chirp-Sample -- ein KURZER Einzel-Chirp, keine lange
// Ambient-Nachtaufnahme mit vielen überlagerten Tieren (passend zum Retrigger-Ansatz von
// SampleSound: das Sample wird bei jedem Trigger von vorne gestartet). Quellen (Intent 43):
//   https://freesound.org/people/kmckinney7/sounds/353073/ (CC0, ein einzelner Chirp)
//   https://freesound.org/people/Otterbahn/sounds/253447/ (eine einzelne Grille)
//   https://pixabay.com/sound-effects/search/cricket/ (kein Account nötig)
//   https://samplefocus.com/samples/cricket-chirp
// Datei herunterladen, Pfad hier eintragen -- ohne gesetzten Pfad werden die Sample-Grillen
// einfach übersprungen (Post-Hinweis), das Demo bleibt ohne Download lauffähig.
~cricketSamplePath = "/Users/chrigu/code/my/Soundgarden/sounds/crickets/cricket-sound_81bpm_G_minor.wav";

CricketSound.addSynthDef;
SampleSound.addSynthDef;
RoomReverb.addSynthDef;
// Der Listener wird extern konfiguriert/instanziiert und dann dem Room übergeben (Intent 52).
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 1, mix: 1);


cricketMovableBase = (pos: [0, 2.5, 0], moveRule: CircularMoveRule.new, roomRadius: 5);
cricketSoundBase = (); // CricketSound.new-Defaults reichen als Basis (Bioakustik-Werte)

num_crickets = 10;

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

// echte Sample-Grillen (Intent 43) -- SampleSound braucht path im Konstruktor (kein <>-Setter
// dafür, siehe SampleSound.sc), deshalb direkt instanziert statt über registerFromBuilder (das
// baut Instanzen immer ohne Argumente und füllt sie erst danach über Setter -- passt für path
// nicht).
num_sample_crickets = 0;

~sampleCrickets = if(~cricketSamplePath.notNil) {
	num_sample_crickets.collect { |i|
		var angle = ((i + 0.5) / num_sample_crickets) * 2pi;
		var radius = rrand(2.0, 4.0);
		var sound = SampleSound.new(~cricketSamplePath, rate: rrand(2.0, 3.0),
			phase: 1.0.rand, amp: rrand(0.3, 0.5));
		var movable = Movable.new(pos: [radius * cos(angle), radius * sin(angle), 0],
			moveRule: CircularMoveRule.new(baseRadius: radius, angularSpeed: rrand(0.1, 0.4)
				* [1, -1].choose));
		~room.register(movable, sound)
	}
} {
	"~cricketSamplePath ist nil -- Sample-Grillen übersprungen (siehe Kommentar oben für Quellen).".postln;
	[]
};

// --- 2) Abspielen ---

)


~room.play;

(
// presetsDir: Ordner für die Sound-Preset-Bibliothek (Intent 43) -- gleiches Pfad-Muster wie
// das bestehende soundsDir in anderen Demos (thisProcess.nowExecutingPath, projektrelativ).
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
	+/+ "crickets").fullPath;
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true,
	presetsDir: ~presetsDir);
~controlPanel.show;
)


// --- 4) Stoppen ---
// room.stop stoppt das Ticken, ruft stop auf allen registrierten Soundobjekten auf
// (Synths/Busse werden freigegeben) und stoppt danach den Hall.
// room.teardown gibt zusätzlich den Reverb-Bus frei und delegiert an AtkBinauralizer.teardown
// (Server-Buffer des geteilten HRTF-Decoder-Kernels, siehe Room>>teardown, Intent 51).
~room.teardown;

// Tastatursteuerung separat stoppen (schließt das Kontrollfenster) — nur nötig,
// falls Block 3 überhaupt ausgeführt wurde.
~controlPanel.free;



//den Raum verändern
~room.size = 12;
~room.height = 3;
~room.surface = 1; // 0 = rau/absorbierend, 1 = glatt/hart
~room.mix = 0.2;
