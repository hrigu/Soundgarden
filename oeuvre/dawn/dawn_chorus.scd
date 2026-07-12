// dawn_chorus.scd — Dämmerungs-Komposition (Intent 57, modularisiert in Intent 58): über
// ~1 Minute hinweg beginnen vereinzelt Vögel zu rufen, es werden mit wachsender Dichte immer
// mehr, bis plötzlich die Sonne durchbricht — fünffacher Akzent auf allen Vögeln (Sound>>call,
// Intent 17), sie verstummen gestaffelt (klickfrei via SoundObject>>fadeOutAndStop), und nach
// einer kurzen Pause übernehmen die Grillen -- ebenfalls nach und nach statt im Unisono.
// Getrieben von Timeline (classes/sg/spatial/Timeline.sc) — dem allgemeinen Modell für "Klang
// über Zeit verändern": at() für einmalige Cues, Timeline.staggeredTimes für die
// Einsatzpläne (gestaffelte Zeitpunkte mit wachsender Dichte + Jitter, siehe dort).
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
var totalDur, sunriseTime, numBirds, numCrickets, birdFadeOutDur, cricketStartDelay,
	cricketFadeInDur, cricketCycleDur, cricketSoundRatio, cricketVolumeRampDur, birdTimes,
	cricketTimes;

BirdSound.addSynthDef;
CricketSound.addSynthDef;
// Der Listener wird extern konfiguriert/instanziiert und dann dem Room übergeben (Intent 52).
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 0.5);

// --- Parameter zum Experimentieren -----------------------------------------------------
totalDur = 100;           // Gesamtdauer des Stücks in Sekunden
sunriseTime = 30;        // Zeitpunkt des Sonnenaufgang-Moments
numBirds = 44;           // Anzahl Vögel, die bis zum Sonnenaufgang gestaffelt einsetzen
numCrickets = 50;        // Anzahl Grillen, die nach dem Sonnenaufgang gestaffelt einsetzen
birdFadeOutDur = 16;      // Zeitraum nach dem Sonnenaufgang, über den die Vögel gestaffelt
                         // verstummen (jede zu einem zufälligen Zeitpunkt darin)
cricketStartDelay = 8;   // Pause nach dem Sonnenaufgang, bevor die erste Grille einsetzt --
                         // der Sonnenaufgang-Moment soll erst für sich stehen
cricketFadeInDur = 20;   // Zeitraum, über den die Grillen nach cricketStartDelay einsetzen
cricketCycleDur = 50;    // Grillen-Rhythmus wie crickets_with_breaks.scd: lange
                         // Gesamtzykluslänge bei wenigen Segmenten (numSegments unten) lässt
                         // RhythmPatternCreator mal kurze, mal auffällig lange Pausen erzeugen
cricketSoundRatio = 1 / 40; // ~1:40 Sound zu Stille (etwas sparsamer als crickets_with_breaks.scd)
cricketVolumeRampDur = cricketFadeInDur; // Zeitraum, über den die Grillen-Lautstärke insgesamt
                         // von ganz leise auf die volle, jeweils konfigurierte Lautstärke
                         // ansteigt (siehe ~timeline.ramp weiter unten) -- unabhängig von den
                         // einzelnen Grillen-Rufrhythmen (RhythmPatternCreator/pattern oben)
// ----------------------------------------------------------------------------------------

// totalDur ist eine lokale var dieses Blocks -- für die Aufnahmedauer in Block 2 (eigener
// Ausführungskontext) als ~-Variable verfügbar machen.
~totalDur = totalDur;

~activeBirds = List.new;
~crickets = List.new;
// merkt sich pro Grille die eigentliche Ziel-Lautstärke (soundParams[\amp] unten), damit der
// Lautstärke-Ramp sie mit dem aktuellen Ramp-Faktor multiplizieren kann, ohne den Zielwert
// selbst durch wiederholtes Lesen von sound.amp zu verfälschen (siehe ~timeline.ramp unten).
~cricketTargetAmp = IdentityDictionary.new;
~timeline = Timeline.new(totalDur);

// Vogel-Einsatzplan: wachsende Dichte Richtung Sonnenaufgang (power > 1 staucht spätere
// Einsätze näher zusammen). Grillen-Einsatzplan: gleichmäßig über cricketFadeInDur nach
// cricketStartDelay verteilt, mit Jitter (power = 1, Default). Siehe Timeline>>staggeredTimes.
birdTimes = Timeline.staggeredTimes(count: numBirds, spanDur: sunriseTime, jitter: 2.0,
	power: 1.8, clipMax: sunriseTime - 1);
cricketTimes = Timeline.staggeredTimes(count: numCrickets, spanDur: cricketFadeInDur,
	startTime: sunriseTime + cricketStartDelay, jitter: 0.8, clipMax: totalDur - 1);

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

// Sonnenaufgang: heller Akzent auf allen aktiven Vögeln (Sound>>call, Intent 17), 5-fach
// statt einmalig -- ein einzelner Call (Env.perc, ~0.35s Decay) ging im dichten Vogelchor
// akustisch fast unter, mehrere Rufe im Abstand machen den Moment klar erkennbar. Dieser Moment
// bleibt synchron, alle Vögel reagieren gemeinsam auf den Lichteinbruch. Das Verstummen danach
// ist aber gestaffelt statt im Unisono (jeder Vogel bekommt seine eigene Routine mit
// zufälligem Offset über birdFadeOutDur), damit die letzten Vögel noch singen, während schon
// die ersten Grillen einsetzen (cricketTimes oben) -- Vogelgesang und Grillenzirpen
// überlagern sich, statt hart aneinander zu grenzen.
~timeline.at(sunriseTime, {
	"Sonnenaufgang.".postln;

	Routine({
		5.do {
			~activeBirds.do { |so| so.sound.call };
			0.4.wait; // Akzent-Decay (Env.perc 0.35s) abwarten, bevor der nächste Ruf kommt
		};

		~activeBirds.do { |so|
			Routine({
				birdFadeOutDur.rand.wait; // gestaffelter Offset, Akzent-Decay ist oben schon abgewartet
				so.fadeOutAndStop(0.5, { |stopped|
					~room.orchestra.unregister(stopped);
					// aus der Liste entfernen -- sonst würde der Stück-Ende-Cue unten
					// denselben, längst gestoppten Vogel nochmal stoppen.
					~activeBirds.remove(stopped);
				});
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
		// targetAmp ist die eigentlich konfigurierte Lautstärke dieser Grille -- die tatsächlich
		// initial gesetzte amp (unten) ist bereits mit dem an dieser Stelle (Zeitpunkt t) fälligen
		// Ramp-Faktor skaliert, damit eine Grille, die mitten im Ramp einsetzt, nicht kurz voll
		// laut aufblitzt, bevor der nächste Ramp-Tick sie wieder auf den passenden Wert bringt.
		var targetAmp = 0.35 * rrand(0.5, 1.5);
		var rampFrac = ((t - (sunriseTime + cricketStartDelay)) / cricketVolumeRampDur).clip(0, 1);
		var soundParams = (
			wingRate: rrand(15, 45),
			wingDuty: rrand(0.3, 0.7),
			ringFreq1: ringFreq1,
			ringFreq2: ringFreq1 + rrand(-400, 400),
			ringDecay1: rrand(0.005, 0.05),
			ringDecay2: rrand(0.005, 0.04),
			amp: targetAmp * rampFrac,
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
		~cricketTargetAmp[so] = targetAmp;
		if(i == 0) { "Erste Grille.".postln };
	});
};

// Grillen-Lautstärke-Ramp: steigt gemeinsam für alle Grillen von ganz leise (0) auf die volle,
// jeweils konfigurierte Lautstärke (~cricketTargetAmp), über cricketVolumeRampDur ab dem
// Einsatz der ersten Grille. Wirkt auf ~crickets statt auf eine feste Liste, weil neue Grillen
// während des Ramps laufend dazukommen (siehe cricketTimes.do oben) -- jeder ramp-Tick erfasst
// automatisch alle bis dahin registrierten. Nach dem Stück-Ende-Cue (unten) ist ~crickets
// bereits geleert, bevor dieser ramp im selben tick erneut aufgerufen wird (siehe
// Timeline>>tick: cues laufen vor ramps), überschreibt also nicht den dortigen Fadeout.
~timeline.ramp(sunriseTime + cricketStartDelay, sunriseTime + cricketStartDelay + cricketVolumeRampDur,
	0.0, 1.0, { |factor|
		~crickets.do { |so| so.sound.setParam(\amp, ~cricketTargetAmp[so] * factor) };
	});

// Stück-Ende: eigener Cue statt Callback in Timeline (die Klasse kennt kein onDone), feuert
// dank Timeline>>tick zuverlässig im selben tick, der auch isDone auslöst (siehe Timeline.sc).
// Alle verbliebenen Sound-Objekte klickfrei stoppen (fadeOutAndStop), statt sie einfach
// weiterlaufen zu lassen -- nur die Timeline selbst stoppte bisher.
~timeline.at(totalDur, {
	"Stück ist fertig.".postln;
	(~crickets ++ ~activeBirds).do { |so|
		so.fadeOutAndStop(0.3, { |stopped| ~room.orchestra.unregister(stopped) });
	};
	~crickets = List.new;
	~activeBirds = List.new;
});

// --- 2) Abspielen ---

)


"Beginn der Dämmerung.".postln;
~room.play;
~timeline.play;

(
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
	+/+ "dawn_chorus").fullPath;
// recordingsDir (Intent 60): schaltet den Start/Stop-Recording-Bereich im Kontrollpanel frei --
// ersetzt den früheren manuellen s.record(duration:)-Aufruf mit vorab geschätzter Stückdauer.
~recordingsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname
	+/+ "recordings").fullPath;
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true,
	presetsDir: ~presetsDir, recordingsDir: ~recordingsDir);
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
