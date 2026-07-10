// crickets_with_breaks.scd — Grillenchor wie eine echte Sommernacht (Intent 56): 30
// stationäre CricketSound-Objekte mit deutlich mehr Klangvarianz als demos/grill/crickets.scd
// und einem Sound/Stille-Verhältnis von etwa 1:30 -- die meiste Zeit Stille, mit kurzen
// Zirp-Ausbrüchen und gelegentlich auffällig langen Pausen. "Gelegentlich lange Pausen"
// entsteht dabei ohne eigene Mechanik: RhythmPatternCreator gewichtet Segmente zufällig (siehe
// dort), bei wenigen Segmenten auf eine lange Gesamtzykluslänge verteilt ergeben sich von
// selbst mal kurze, mal auffällig lange Einzel-Pausen (gleiches Muster wie
// demos/grill/insects.scd mit soundRatio: 0.01).
//
// Anders als demos/grill/crickets.scd bewegen sich die Grillen hier nicht auf Kreisbahnen
// (SteadyMoveRule statt CircularMoveRule) -- jede sitzt fest an ihrem Platz, lässt sich aber
// im Control-Panel per Klick verschieben (wie die Sample-Objekte in demos/sample.scd).
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
var num_crickets;
var totalCycleDur, soundRatio;

CricketSound.addSynthDef;
// Der Listener wird extern konfiguriert/instanziiert und dann dem Room übergeben (Intent 52).
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 1, mix: 1);

num_crickets = 30;
// totalCycleDur bewusst lang: bei wenigen Segmenten (numSegments unten) verteilt sich die
// Gesamtdauer auf so wenige Stücke, dass die Zufallsgewichtung (RhythmPatternCreator) mal
// sehr kurze, mal auffällig lange Einzel-Pausen erzeugt -- genau das gewünschte "ab und zu
// ganz lange Pausen" (Intent 56), ohne eigene Mechanik dafür.
totalCycleDur = 40;
soundRatio = 1 / 30; // ~1:30 Sound zu Stille

~crickets = num_crickets.collect { |i|
	var angle = (i / num_crickets) * 2pi;
	var radius = rrand(2.0, 4.5);
	var ringFreq1 = rrand(1500, 7000);
	var numSegments = [6, 8, 10, 12].choose;
	var soundParams = (
		// deutlich mehr Streuung als crickets.scd (dort nur wingRate/ringFreq1/amp leicht
		// variiert) -- jede Grille soll hörbar anders klingen (Intent 56).
		wingRate: rrand(15, 45),
		wingDuty: rrand(0.3, 0.7),
		ringFreq1: ringFreq1,
		// ringFreq2 bewusst nah an ringFreq1 (siehe CricketSound-Klassenkommentar: eine
		// dominante Trägerfrequenz statt eines zweiten Formanten) -- nur die Streuung selbst
		// ist hier weiter als in crickets.scd.
		ringFreq2: ringFreq1 + rrand(-400, 400),
		ringDecay1: rrand(0.005, 0.05),
		ringDecay2: rrand(0.005, 0.04),
		amp: 0.35 * rrand(0.5, 1.5),
		pattern: RhythmPatternCreator.new(
			totalDur: totalCycleDur,
			numSegments: numSegments,
			soundRatio: soundRatio,
			startWithSound: 0.5.coin
		).createNewPattern
	);
	// SteadyMoveRule statt CircularMoveRule -- Grille bleibt an ihrem Platz (Intent 56),
	// Rückführung bei roomRadius-Überschreitung bleibt trotzdem vorbereitet, falls sie später
	// per Klick nach draussen gezogen wird (siehe Movable>>step).
	var movableParams = (
		pos: [radius * cos(angle), radius * sin(angle), 0],
		moveRule: SteadyMoveRule.new,
		roomRadius: radius + 2
	);
	~room.registerFromBuilder(soundParams, movableParams, soundClass: CricketSound)
};

// --- 2) Abspielen ---

)


~room.play;

(
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
	+/+ "crickets_with_breaks").fullPath;
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true,
	presetsDir: ~presetsDir);
~controlPanel.show;
)


// --- 4) Stoppen ---
// room.teardown stoppt, gibt den Reverb-Bus frei UND delegiert an listener.teardown, das bei
// AtkBinauralizer den HRTF-Kernel freigibt (siehe Room>>teardown/Listener>>teardown, Intent
// 52).
~room.teardown;

// Tastatursteuerung separat stoppen (schließt das Kontrollfenster) — nur nötig,
// falls Block 3 überhaupt ausgeführt wurde.
~controlPanel.free;
