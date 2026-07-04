// boot.scd — Server booten und alle Tracks aus sounds/ laden
// Als erstes ausführen (Cmd+Enter auf den Block).

(
s = Server.default;
s.options.numBuffers = 1024 * 16;
s.options.memSize = 8192 * 16;

s.waitForBoot({
	var soundsDir = PathName(thisProcess.nowExecutingPath.dirname +/+ "sounds");
	var validExt = [\wav, \aiff, \aif, \flac];

	~tracks = ();

	if(soundsDir.isFolder) {
		soundsDir.entries.select({ |pe|
			validExt.includes(pe.extension.asString.toLower.asSymbol)
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
// s.freeAll;

// Server neu starten (z.B. nach Absturz):
// s.reboot;
