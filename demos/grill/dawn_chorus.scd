// dawn_chorus.scd — Dämmerungs-Komposition (Intent 57): über ~3 Minuten hinweg beginnen
// vereinzelt Vögel zu rufen, es werden mit wachsender Dichte immer mehr, bis plötzlich die
// Sonne durchbricht — kurzer Akzent auf allen Vögeln (Sound>>call, Intent 17), sie verstummen
// (klickfrei dank Lag.kr auf amp, siehe BirdSound.sc), und die Grillen übernehmen -- ebenfalls
// nach und nach statt im Unisono (gleiche Konfiguration wie demos/grill/crickets_with_breaks.scd,
// aber gestaffelter Einsatz wie bei den Vögeln). Getrieben von Timeline (classes/sg/spatial/
// Timeline.sc) — dem allgemeinen Modell für "Klang über Zeit verändern": at() für einmalige
// Cues (ein neuer Vogel setzt ein, der Sonnenaufgang-Moment), analog zu Orchestras zentraler
// Tick-Routine.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
var totalDur, sunriseTime, numBirds, numCrickets, birdTimes, cricketFadeInDur, cricketTimes,
	birdFadeOutDur, cricketCycleDur, cricketSoundRatio, cricketStartDelay;

BirdSound.addSynthDef;
CricketSound.addSynthDef;
// Der Listener wird extern konfiguriert/instanziiert und dann dem Room übergeben (Intent 52).
~listener = Listener.new(binauralizerClass: Binauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 0.5, mix: 0.3);

totalDur = 60;
sunriseTime = 25;
numBirds = 44;
numCrickets = 50;
cricketFadeInDur = 10; // Grillen setzen nach dem Sonnenaufgang gestaffelt ein, siehe cricketTimes
cricketStartDelay = 5; // Pause nach dem Sonnenaufgang, bevor die erste Grille einsetzt -- der
// Sonnenaufgang-Moment (dreifacher Vogelruf) soll erst für sich stehen, bevor der nächste
// Abschnitt beginnt, statt direkt in die Grillen überzugehen.
birdFadeOutDur = 8; // Vögel verstummen nach dem Sonnenaufgang gestaffelt statt im Unisono, siehe unten
// Grillen-Rhythmus wie crickets_with_breaks.scd: lange Gesamtzykluslänge bei wenigen Segmenten
// (numSegments unten) lässt RhythmPatternCreator mal kurze, mal auffällig lange Einzel-Pausen
// erzeugen -- statt des permanenten Zirpens von vorher (dort fehlte ein pattern komplett).
cricketCycleDur = 40;
cricketSoundRatio = 1 / 30; // ~1:30 Sound zu Stille, wie crickets_with_breaks.scd

~activeBirds = List.new;
~crickets = List.new;
~timeline = Timeline.new(totalDur);

// Vogel-Einsatzplan: wachsende Dichte Richtung Sonnenaufgang -- Potenz > 1 staucht spätere
// Einsätze näher zusammen (wenige, weit auseinanderliegende Rufe am Anfang der Dämmerung,
// dichter Chor kurz davor). clip hält alle Einsätze sicher vor dem Sonnenaufgang-Cue.
birdTimes = (1..numBirds).collect { |i|
	(((i / numBirds) ** 1.8 * sunriseTime) + rrand(-2.0, 2.0)).clip(0, sunriseTime - 1)
};

// Grillen-Einsatzplan: wie bei den Vögeln kein Unisono-Einsatz, sondern nach und nach --
// gleichmäßig über cricketFadeInDur nach dem Sonnenaufgang verteilt, mit Jitter. clip hält
// alle Einsätze innerhalb der Timeline-Gesamtdauer.
cricketTimes = (1..numCrickets).collect { |i|
	(sunriseTime + cricketStartDelay + ((i / numCrickets) * cricketFadeInDur) + rrand(-0.8, 0.8))
		.clip(sunriseTime + cricketStartDelay, totalDur - 1)
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

// Sonnenaufgang: heller Akzent auf allen aktiven Vögeln (Sound>>call, Intent 17), dreifach
// statt einmalig -- ein einzelner Call (Env.perc, ~0.35s Decay) ging im dichten Vogelchor
// akustisch fast unter, drei Rufe im Abstand machen den Moment klar erkennbar. Dieser Moment
// bleibt synchron, alle Vögel reagieren gemeinsam auf den Lichteinbruch. Das Verstummen danach
// ist aber gestaffelt statt im Unisono (jeder Vogel bekommt seine eigene Routine mit
// zufälligem Offset über birdFadeOutDur), damit die letzten Vögel noch singen, während schon
// die ersten Grillen einsetzen (cricketTimes oben) -- Vogelgesang und Grillenzirpen
// überlagern sich, statt hart aneinander zu grenzen.
~timeline.at(sunriseTime, {
	"Sonnenaufgang.".postln;

	Routine({
		3.do {
			~activeBirds.do { |so| so.sound.call };
			0.4.wait; // Akzent-Decay (Env.perc 0.35s) abwarten, bevor der nächste Ruf kommt
		};

		~activeBirds.do { |so|
			Routine({
				birdFadeOutDur.rand.wait; // gestaffelter Offset, Akzent-Decay ist oben schon abgewartet
				so.sound.setParam(\amp, 0);
				0.5.wait; // Lag.kr(amp, 0.15)-Ausklang abwarten, bevor der Synth ganz freigegeben wird
				so.stop;
				~room.orchestra.unregister(so);
				// aus der Liste entfernen -- sonst würde der Stück-Ende-Cue unten denselben,
				// längst gestoppten Vogel nochmal stoppen ("Node not found"/"Bus already freed").
				~activeBirds.remove(so);
			}).play;
		};
	}).play;
});

// Grillen (gleiche Konfiguration wie crickets_with_breaks.scd) setzen erst nach dem
// Sonnenaufgang ein, nicht von Anfang an -- der Tag beginnt. Nach und nach statt im Unisono
// (gleiches Muster wie birdTimes oben), je ein eigener Timeline-Cue pro Grille.
cricketTimes.do { |t, i|
	~timeline.at(t, {
		var angle = (i / numCrickets) * 2pi;
		var radius = rrand(2.0, 4.5);
		var ringFreq1 = rrand(1500, 7000);
		var numSegments = [6, 8, 10, 12].choose;
		var soundParams = (
			wingRate: rrand(15, 45),
			wingDuty: rrand(0.3, 0.7),
			ringFreq1: ringFreq1,
			ringFreq2: ringFreq1 + rrand(-400, 400),
			ringDecay1: rrand(0.005, 0.05),
			ringDecay2: rrand(0.005, 0.04),
			amp: 0.35 * rrand(0.5, 1.5),
			pattern: RhythmPatternCreator.new(
				totalDur: cricketCycleDur,
				numSegments: numSegments,
				soundRatio: cricketSoundRatio,
				startWithSound: 0.5.coin
			).createNewPattern
		);
		var movableParams = (
			pos: [radius * cos(angle), radius * sin(angle), 0],
			moveRule: SteadyMoveRule.new,
			roomRadius: radius + 2
		);
		var so = ~room.registerFromBuilder(soundParams, movableParams, soundClass: CricketSound);
		so.play(~room.server);
		~crickets.add(so);
		if(i == 0) { "Erste Grille.".postln };
	});
};

// Stück-Ende: eigener Cue statt Callback in Timeline (die Klasse kennt kein onDone), feuert
// dank Timeline>>tick zuverlässig im selben tick, der auch isDone auslöst (siehe Timeline.sc).
// Bislang zirpten die Grillen (und ggf. noch nicht ausgefadete Vögel) einfach weiter, weil
// nur die Timeline selbst stoppte -- hier jetzt zusätzlich alle verbliebenen Sound-Objekte
// klickfrei stoppen (erst amp -> 0, dann nach Lag.kr-Ausklang stop), gleiches Muster wie beim
// Sonnenaufgang-Fade-out der Vögel oben.
~timeline.at(totalDur, {
	"Stück ist fertig.".postln;
	(~crickets ++ ~activeBirds).do { |so| so.sound.setParam(\amp, 0) };

	Routine({
		0.3.wait; // Lag.kr(amp)-Ausklang abwarten (0.1s Grillen, 0.15s Vögel)
		(~crickets ++ ~activeBirds).do { |so|
			so.stop;
			~room.orchestra.unregister(so);
		};
		~crickets = List.new;
		~activeBirds = List.new;
	}).play;
});

// --- 2) Abspielen ---

)


"Beginn der Dämmerung.".postln;
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
