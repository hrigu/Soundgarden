// boot.scd — Server booten und alle Tracks aus sounds/ laden
// Als erstes ausführen (Cmd+Enter auf den Block).

(
s = Server.default;
s.options.numBuffers = 1024 * 16;
// hoch angesetzt: dawn_chorus.scd hält zeitweise >30 AtkBinauralizer-Synths gleichzeitig
// aktiv, jeder mit HRTF-Faltung (Convolution2 in FoaDecode) -- bei zu knappem memSize
// schlägt das mit "Convolution2_Ctor: alloc failed" fehl.
s.options.memSize = 8192 * 64;
// nicht nötig, wenn Mikrofon-Input des Bluetooth-Headsets in den Systemeinstellungen deaktiviert ist
//s.options.numInputBusChannels = 0; // kein Mikrofon-Input nötig — vermeidet Samplerate-Konflikte
                                    // mit Bluetooth-Headsets (deren Mikrofon oft nur 16kHz liefert)

s.waitForBoot({
	var soundsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "sounds");

	~tracks = ();

	if(soundsDir.isFolder) {
		soundsDir.entries.select({ |pe|
			BootTrackDetection.isValidExtension(pe.extension)
		}).do({ |pe|
			var key = pe.fileNameWithoutExtension.asSymbol;
			~tracks[key] = Buffer.read(s, pe.fullPath);
		});

		s.sync;
		"Loaded % track(s): %\n".postf(~tracks.size, ~tracks.keys.asArray.sort);
	} {
		"Kein 'sounds'-Ordner gefunden — Tracks dort ablegen und boot.scd neu ausführen.\n".postln;
	};

	"Server bereit.\n".postln;
});
)

// Panic / alles stoppen:
s.freeAll;

// Server neu starten (z.B. nach Absturz):
// s.reboot;