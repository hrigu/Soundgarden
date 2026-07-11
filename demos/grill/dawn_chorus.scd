// dawn_chorus.scd — Dämmerungs-Komposition (Intent 57): über ~3 Minuten hinweg beginnen
// vereinzelt Vögel zu rufen, es werden mit wachsender Dichte immer mehr, bis plötzlich die
// Sonne durchbricht — kurzer Akzent auf allen Vögeln (Sound>>call, Intent 17), sie verstummen
// (klickfrei dank Lag.kr auf amp, siehe BirdSound.sc), und die Grillen übernehmen (wie
// demos/grill/crickets_with_breaks.scd). Getrieben von Timeline (classes/sg/spatial/
// Timeline.sc) — dem allgemeinen Modell für "Klang über Zeit verändern": at() für einmalige
// Cues (ein neuer Vogel setzt ein, der Sonnenaufgang-Moment), analog zu Orchestras zentraler
// Tick-Routine.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
var totalDur, sunriseTime, numBirds, numCrickets, birdTimes;

BirdSound.addSynthDef;
CricketSound.addSynthDef;
// Der Listener wird extern konfiguriert/instanziiert und dann dem Room übergeben (Intent 52).
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 1, mix: 1);

totalDur = 180;
sunriseTime = 150;
numBirds = 24;
numCrickets = 30;

~activeBirds = List.new;
~timeline = Timeline.new(totalDur);

// Vogel-Einsatzplan: wachsende Dichte Richtung Sonnenaufgang -- Potenz > 1 staucht spätere
// Einsätze näher zusammen (wenige, weit auseinanderliegende Rufe am Anfang der Dämmerung,
// dichter Chor kurz davor). clip hält alle Einsätze sicher vor dem Sonnenaufgang-Cue.
birdTimes = (1..numBirds).collect { |i|
	(((i / numBirds) ** 1.8 * sunriseTime) + rrand(-2.0, 2.0)).clip(0, sunriseTime - 1)
};

birdTimes.do { |t|
	~timeline.at(t, {
		var angle = 2pi.rand;
		var radius = rrand(2.0, 4.5);
		var soundParams = (
			chirpRate: rrand(3, 10),
			baseFreq: rrand(2000, 6000),
			glideRange: rrand(300, 1500),
			chirpDur: rrand(0.04, 0.12),
			amp: 0.3 * rrand(0.6, 1.3),
			// eigene Ruf-Phrase/Pause pro Vogel (Intent 33), damit der Chor nicht synchron
			// im Unisono ruft -- gleiches Muster wie bei CricketSound/insects.scd.
			pattern: RhythmPatternCreator.new(
				totalDur: rrand(3.0, 6.0),
				numSegments: [4, 6, 8].choose,
				soundRatio: rrand(0.15, 0.35),
				startWithSound: true
			).createNewPattern
		);
		var movableParams = (
			pos: [radius * cos(angle), radius * sin(angle), 0],
			moveRule: SteadyMoveRule.new,
			roomRadius: radius + 2
		);
		// Objekte, die nach orchestra.play (siehe unten, ~room.play) registriert werden, laufen
		// nicht automatisch mit -- deshalb hier explizit play(server) auf dem zurückgegebenen
		// SoundObject (Room>>server ist dafür öffentlich zugänglich, siehe Timeline.sc-Kontext).
		var so = ~room.registerFromBuilder(soundParams, movableParams, soundClass: BirdSound);
		so.play(~room.server);
		~activeBirds.add(so);
	});
};

// Sonnenaufgang: kurzer, heller Akzent auf allen aktiven Vögeln (Sound>>call, Intent 17),
// danach verstummen sie (amp -> 0, dank Lag.kr im SynthDef klickfrei) und werden gestoppt --
// erst NACH dem Akzent-Decay, damit der Moment noch hörbar ist, bevor Stille einkehrt. Die
// Grillen (gleiche Konfiguration wie crickets_with_breaks.scd) werden erst jetzt registriert
// und gestartet, nicht von Anfang an -- der Tag beginnt.
~timeline.at(sunriseTime, {
	~activeBirds.do { |so| so.sound.call };

	Routine({
		0.4.wait; // Akzent-Decay (Env.perc 0.35s) erst ausklingen lassen
		~activeBirds.do { |so| so.sound.setParam(\amp, 0) };
		0.5.wait; // Lag.kr(amp, 0.15)-Ausklang abwarten, bevor der Synth ganz freigegeben wird
		~activeBirds.do { |so|
			so.stop;
			~room.orchestra.unregister(so);
		};
	}).play;

	~crickets = numCrickets.collect { |i|
		var angle = (i / numCrickets) * 2pi;
		var radius = rrand(2.0, 4.5);
		var ringFreq1 = rrand(4200, 5200);
		var soundParams = (
			wingRate: rrand(24, 30),
			wingDuty: rrand(0.4, 0.6),
			ringFreq1: ringFreq1,
			ringFreq2: ringFreq1 + rrand(-100, 100),
			amp: 0.35 * rrand(0.7, 1.2)
		);
		var movableParams = (
			pos: [radius * cos(angle), radius * sin(angle), 0],
			moveRule: SteadyMoveRule.new,
			roomRadius: radius + 2
		);
		var so = ~room.registerFromBuilder(soundParams, movableParams, soundClass: CricketSound);
		so.play(~room.server);
		so
	};
});

// --- 2) Abspielen ---

)


~room.play;
~timeline.play;

(
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
	+/+ "dawn_chorus").fullPath;
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true,
	presetsDir: ~presetsDir);
~controlPanel.show;
)


// --- 4) Stoppen ---
~timeline.stop;

// room.teardown stoppt, gibt den Reverb-Bus frei UND delegiert an listener.teardown, das bei
// AtkBinauralizer den HRTF-Kernel freigibt (siehe Room>>teardown/Listener>>teardown, Intent
// 52).
~room.teardown;

// Tastatursteuerung separat stoppen (schließt das Kontrollfenster) — nur nötig,
// falls Block 3 überhaupt ausgeführt wurde.
~controlPanel.free;
