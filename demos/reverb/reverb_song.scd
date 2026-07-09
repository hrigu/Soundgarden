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
var config, songsDir, songFiles, normalizedSongName, exactMatch, partialMatch;
var originalPath, tempWavPath;

SongSound.addSynthDef;
RoomReverb.addSynthDef;

// Zentrale Session-Konfiguration fuer den Hoertest.
	config = (
		songsDir: "/Users/chrigu/code/my/musicnet/downloads/tracks",
	roomSize: 10,
	roomHeight: 3,
	roomSurface: 0.35,
	roomMix: 0.2,
	reverbMix: 0.35,
	viewRadius: 8,
	songPosition: [0, 3, 0],
	songAmp: 0.75,
	songRate: 1,
	songLoop: true
);

// Kann vor dem Setup gesetzt werden, z.B.:
 ~songName = "Anonyme, Anne Delafosse, Clara Coutouly, Paulin Bundgen - Stella maris nuncuparis";

~songConfig = config;
~room = Room.new(s, size: config[\roomSize], height: config[\roomHeight],
	surface: config[\roomSurface], mix: config[\roomMix]);
~room.binauralizerClass = AtkBinauralizer;

// Song-Auswahl:
// - exakter Match auf fileNameWithoutExtension gewinnt
// - sonst erstes Namensfragment-Match
// - ohne ~songName oder ohne Treffer: erste gueltige Audiodatei
songsDir = PathName(config[\songsDir]);
songFiles = [];
songsDir.filesDo({ |pe|
	if(
		pe.isFile
		and: { pe.fileName.beginsWith(".").not }
		and: { BootTrackDetection.isValidExtension(pe.extension) }
	) {
		songFiles = songFiles.add(pe);
	};
});

// FEHLERBEHEBUNG: .sort statt .sortBy nutzen, um den Absturz bei PathName zu verhindern
songFiles = songFiles.sort({ |a, b| a.fullPath < b.fullPath });

if(songFiles.isEmpty) {
	Error("Reverb-Song-Demo: Keine gueltigen Audiodateien unter % gefunden."
		.format(config[\songsDir])).throw;
};

normalizedSongName = ~songName.asString.toLower;
exactMatch = if(~songName.notNil) {
	songFiles.detect({ |pe| pe.fileNameWithoutExtension.asString.toLower == normalizedSongName })
};
partialMatch = if(~songName.notNil) {
	songFiles.detect({ |pe|
		pe.fileNameWithoutExtension.asString.toLower.contains(normalizedSongName)
	})
};

~songFile = exactMatch ?? partialMatch ?? songFiles.first;


// ERWEITERUNG (Methode 2): Automatische M4A zu WAV Konvertierung via absolutem ffmpeg-Pfad
originalPath = ~songFile.fullPath;
if(originalPath.endsWith(".m4a")) {
	tempWavPath = originalPath.copyRange(0, originalPath.size - 5) ++ ".wav";

	if(File.exists(tempWavPath).not) {
		var ffmpegPath, cmd;
		"Reverb-Song-Demo: M4A erkannt. Konvertiere temporär in WAV via ffmpeg...".postln;

		// Prüfen, ob ffmpeg im Homebrew-Pfad für Apple Silicon liegt, sonst Standard-Fallback
		ffmpegPath = if(File.exists("/opt/homebrew/bin/ffmpeg")) {
			"/opt/homebrew/bin/ffmpeg"
		} {
			"ffmpeg" // Fallback für Intel-Macs (/usr/local/bin) oder globale PATHs
		};

		// Befehl mit absolutem Programmpfad und sicheren Anführungszeichen zusammenbauen
		cmd = ffmpegPath + "-i " + originalPath.asString.quote + " " + tempWavPath.asString.quote;
		("Führe Befehl aus: " + cmd).postln;

		cmd.systemCmd;
	};

	if(File.exists(tempWavPath)) {
		~songFile = PathName(tempWavPath);
		"Reverb-Song-Demo: Konvertierung erfolgreich. Nutze WAV-Datei.".postln;
	} {
		"Reverb-Song-Demo: Warnung! ffmpeg-Konvertierung trotz absolutem Pfad fehlgeschlagen.".postln;
	};
};


~songSound = SongSound.new(~songFile.fullPath, amp: config[\songAmp], loop: config[\songLoop],
	rate: config[\songRate]);
~songSound.preload(s);
~songObject = ~room.register(
	Movable.new(config[\songPosition], SteadyMoveRule.new, roomRadius: config[\viewRadius]),
	~songSound,
	reverbMix: config[\reverbMix]
);

"Reverb-Song-Demo: Gewaehlter Song: %\n".postf(~songFile.fileNameWithoutExtension);

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

~stopDemo = {
	~room.stop;
	~controlPanel !? { ~controlPanel.hide };
	"Reverb-Song-Demo: Demo gestoppt.\n".post;
};

~freeDemo = {
	~stopDemo.();
	~controlPanel !? {
		~controlPanel.free;
		~controlPanel = nil;
	};
	// room.teardown delegiert bereits an AtkBinauralizer.teardown (siehe Room>>teardown,
	// Intent 51) — kein separater Aufruf mehr nötig.
	~room.teardown;
	"Reverb-Song-Demo: ATK-Kernel freigegeben.\n".post;
	};

	"Reverb-Song-Demo: Grundgeruest vorbereitet.\n".post;
	"Reverb-Song-Demo: Mit ~songName kann vor Block 1 ein bestimmter Titel gewaehlt werden.\n".post;
	~songPresetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
		+/+ "reverb_song").fullPath;
)

// --- 2) Abspielen ---
~startDemo.();

// --- 3) Optional: gemeinsames GUI ---
~controlPanel = RoomControlPanel.new(~room, viewRadius: ~songConfig[\viewRadius], editable: true,
	presetsDir: ~songPresetsDir);
~controlPanel.show;

// --- 4) Stoppen ---
~stopDemo.();

// --- 5) Vollstaendig freigeben ---
~freeDemo.();
