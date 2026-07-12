// messiaen_birds.scd — Messiaen-inspirierte Vogel-Komposition (Intent 59): drei charakteristische
// Motiv-Typen (BirdMotifExamples, siehe demos/sounds/messiaenbird.scd für den Einzel-Hörtest)
// setzen über die Stückdauer zeitversetzt und überlappend ein, räumlich verteilt im Raum, jeder
// eigenständig für eine zufällige Lebensdauer laufend, bevor er klickfrei verstummt.
//
// Getrieben von Timeline (classes/sg/spatial/Timeline.sc), Strukturvorbild 1:1
// oeuvre/dawn/dawn_chorus.scd: Timeline.staggeredTimes für die Einsatzpläne, SoundObject>>
// fadeOutAndStop fürs klickfreie Verstummen. Anders als dawn_chorus.scd: MessiaenBirdSound hat
// mit motif einen Pflicht-Konstruktorparameter ohne Setter (siehe MessiaenBirdSound.sc) --
// SoundObjectBuilder/registerFromBuilder (setter-basiert) passt hier nicht, deshalb Sound-
// Instanzen von Hand bauen und über Room>>register (fertige Instanzen) anmelden, wie
// Room>>register es für genau diesen Fall vorsieht.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
var totalDur, numBlackbirds, numWrens, numCallBirds, birdLifespanMin, birdLifespanMax,
	blackbirdStartFreqRange, wrenStartFreqRange, callBirdStartFreqRange,
	blackbirdTimes, wrenTimes, callBirdTimes;

// motifFunc statt eines fertigen Motivs: pro Vogel-Einsatz neu aufgerufen, damit jeder Vogel
// (Intent 59, Nutzer-Wunsch nach mehr Varianz) eine eigene, zufällig gewählte startFreq bekommt
// -- BirdMotifExamples baut daraus ein frisches Motiv, statt dass alle Vögel eines Typs
// dieselbe feste Tonlage teilen.
var spawnBird = { |motifFunc, ampRange, brightnessRange|
	var motif = motifFunc.value;
	var angle = 2pi.rand;
	var radius = rrand(2.0, 4.5);
	var movable = Movable.new(
		pos: [radius * cos(angle), radius * sin(angle), 0],
		moveRule: SteadyMoveRule.new,
		roomRadius: radius + 2
	);
	var sound = MessiaenBirdSound.new(motif,
		amp: rrand(ampRange[0], ampRange[1]), brightness: rrand(brightnessRange[0], brightnessRange[1]),
		loop: true);
	var so = ~room.register(movable, sound);
	so.play(~room.server);
	~activeBirds.add(so);

	Routine({
		rrand(birdLifespanMin, birdLifespanMax).wait;
		so.fadeOutAndStop(0.5, { |stopped|
			~room.orchestra.unregister(stopped);
			~activeBirds.remove(stopped);
		});
	}).play;
};

MessiaenBirdSound.addSynthDef;
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 0.5);

// --- Parameter zum Experimentieren -----------------------------------------------------
totalDur = 90;            // Gesamtdauer des Stücks in Sekunden
numBlackbirds = 10;       // blackbirdRun: perlender Lauf, gleichmäßig über das Stück verteilt
numWrens = 6;             // wrenTrillCascade: sehr schnelle Kaskade, seltener (auffälliger)
numCallBirds = 8;         // callBirdLeaps: großer Sprung, ebenfalls über das Stück verteilt
birdLifespanMin = 8;      // jeder Vogel läuft für eine zufällige Dauer in diesem Bereich,
birdLifespanMax = 18;     // bevor er klickfrei verstummt (siehe spawnBird oben)
// startFreq-Bereiche pro Motiv-Typ (Intent 59) -- jeder Vogel bekommt beim Einsatz eine eigene,
// zufällig aus diesem Bereich gewählte Grundtonlage (siehe spawnBird/motifFunc oben), statt dass
// alle Vögel eines Typs identisch klingen. Bereiche orientieren sich an den bisherigen festen
// BirdMotifExamples-Defaults (3200/5500/2200 Hz), aber mit Spielraum nach oben/unten.
blackbirdStartFreqRange = [2600, 4200];
wrenStartFreqRange = [4500, 6800];
callBirdStartFreqRange = [1800, 2800];
// ----------------------------------------------------------------------------------------

~activeBirds = List.new;
~timeline = Timeline.new(totalDur);

// gestaffelte Einsatzzeitpunkte pro Motiv-Typ, mit Jitter (power = 1: gleichmäßig über die
// Stückdauer verteilt) -- unterschiedliche jitter-Werte je nach charakteristischer "Dichte" des
// Typs, siehe Timeline>>staggeredTimes.
blackbirdTimes = Timeline.staggeredTimes(count: numBlackbirds, spanDur: totalDur - 5, jitter: 3.0);
wrenTimes = Timeline.staggeredTimes(count: numWrens, spanDur: totalDur - 5, jitter: 4.0);
callBirdTimes = Timeline.staggeredTimes(count: numCallBirds, spanDur: totalDur - 5, jitter: 3.0);

blackbirdTimes.do { |t|
	~timeline.at(t, {
		spawnBird.(
			{ BirdMotifExamples.blackbirdRun(rrand(blackbirdStartFreqRange[0], blackbirdStartFreqRange[1])) },
			[0.25, 0.45], [0.3, 0.6])
	});
};
wrenTimes.do { |t|
	~timeline.at(t, {
		spawnBird.(
			{ BirdMotifExamples.wrenTrillCascade(rrand(wrenStartFreqRange[0], wrenStartFreqRange[1])) },
			[0.2, 0.35], [0.6, 0.9])
	});
};
callBirdTimes.do { |t|
	~timeline.at(t, {
		spawnBird.(
			{ BirdMotifExamples.callBirdLeaps(rrand(callBirdStartFreqRange[0], callBirdStartFreqRange[1])) },
			[0.3, 0.5], [0.3, 0.5])
	});
};

// Stück-Ende: alle verbliebenen Vögel klickfrei stoppen (fadeOutAndStop), statt sie einfach
// weiterlaufen zu lassen -- gleiches Muster wie dawn_chorus.scd.
~timeline.at(totalDur, {
	"Stück ist fertig.".postln;
	~activeBirds.do { |so| so.fadeOutAndStop(0.3, { |stopped| ~room.orchestra.unregister(stopped) }) };
	~activeBirds = List.new;
});

// --- 2) Abspielen ---

)

"Beginn der Messiaen-Vogel-Komposition.".postln;
~room.play;
~timeline.play;

(
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
	+/+ "messiaen_birds").fullPath;
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true,
	presetsDir: ~presetsDir);
~controlPanel.show;
)

// --- 4) Stoppen ---
~timeline.stop;
~room.teardown;
~controlPanel.free;
