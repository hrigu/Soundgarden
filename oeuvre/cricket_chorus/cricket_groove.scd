// cricket_groove.scd — Fünfstimmiger Grillen-Groove mit emergenter Synchronisation (Intent 62),
// Rollen abgeleitet aus einem echten Referenz-Drumgroove (~/Downloads/Download.midi: 5 Stimmen —
// Kick/Snare/Hihat/Floor-Tom/Mid-Tom, 16tel-Raster, 80 BPM) statt aus vier frei erfundenen
// Layern wie in cricket_chorus.scd (Intent 61). Gleiches Grundprinzip wie dort: zu Beginn ruft
// jede Stimme unkoordiniert mit ihrer eigenen naturalFreq, ein "Dirigent" (CoupledOscillators,
// siehe classes/sg/spatial/CoupledOscillators.sc) zieht sie über die Stückdauer zu ihrer
// jeweiligen Rollen-Position — echte Selbstorganisation (Kuramoto-Modell), kein geskripteter
// Step-Sequencer.
//
// ~groove: der Referenz-Drumgroove als editierbare, sichtbare Notation (Drum-Tab-Stil, ein
// "x"/"."-String pro Stimme, Länge = Schritte pro Takt) statt einer Handrechnung von
// roleOffset/roleMultiplier im Code. GrooveNotation.deriveRoles (classes/sg/spatial/
// GrooveNotation.sc, Intent 62 Task 4) leitet daraus automatisch roleMultiplier (Anzahl Schläge)
// und roleOffset (zirkulärer Mittelwert der Schlag-Winkel, siehe Klassenkommentar) ab. Wer den
// Groove ändern will, editiert nur die Strings unten — keine Winkel-Handrechnung mehr nötig.
// Die im MIDI zusätzlich vorhandene Verdichtung im zweiten Takt (mehr Snare-/Tom-Schläge gegen
// Taktende) wird NICHT 1:1 nachgebaut, sondern bewusst als Näherung über die abgeleiteten
// Rollen-Positionen abgebildet (kein Step-Sequencer, siehe Intent-62-Entscheid).
//
// Wie cricket_chorus.scd: erster Block registriert die Stimmen stationär im Raum und setzt
// pattern = nil PRO SOUND-INSTANZ (nicht nur im soundParams-Event) — siehe CLAUDE.md-Gotcha zu
// CricketSound, sonst zirpt jede Grille zusätzlich per eigenem CallingPattern gegen das externe
// Gate der Tick-Routine an. Zweiter Block: Dirigent (CoupledOscillators), Tick-Routine, Timeline.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über Lautsprecher
// richtig.
(
CricketSound.addSynthDef;
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 0.5);

// ~conductorFreq: gemeinsamer Grundpuls in Hz (wie in cricket_chorus.scd, Intent 61) — Startwert
// übernommen, Feintuning für die 5-stimmige Besetzung folgt per Hörtest (Task 5).
~conductorFreq = 0.35;

// Reihenfolge: Kick, Snare, Hihat, Floor-Tom, Mid-Tom (aus Download.midi transkribiert, ein
// Zeichen pro 16tel-Schritt eines Takts) — hier editieren, um den Groove zu verändern.
~groove = [
	"x.......x.......",  // Kick: Schritt 0+8 (Grundschlag)
	"....x...........",  // Snare: Schritt 4 (Backbeat)
	".xxx.xxx.xxx.xxx",  // Hihat: 12 von 16 Schritten (treibender Puls)
	"...x.x.x.....x.x",  // Floor-Tom: synkopierte Fill-Akzente
	"...........xx...",  // Mid-Tom: Fill-Doppelschlag gg. Taktende
];

~roles = GrooveNotation.deriveRoles(~groove);

// naturalFreq-Streufaktoren: bewusst weit von der jeweiligen Rollen-Zielfrequenz
// (roleMultiplier * conductorFreq) entfernt gewählt, damit das Stück hörbar unkoordiniert
// beginnt (gleiches Muster wie cricket_chorus.scd) — ein Faktor pro Stimme, gleiche Reihenfolge
// wie ~groove.
~naturalFreqScatter = [0.6, 1.7, 0.7, 1.3, 1.6];

// 5 Stimmen, je einer MIDI-Rolle zugeordnet; roleOffset/roleMultiplier kommen aus ~roles (oben),
// nicht mehr aus Handrechnung.
~voices = [
	// --- Kick: tief, tonal, trägt den Grundschlag (Taktanfang + Taktmitte) ---
	(soundParams: (ringFreq1: 900, wingRate: 14, wingDuty: 0.6, ringDecay1: 0.05,
		ringDecay2: 0.04, amp: 0.5, pattern: nil),
		chirpDur: 0.18, naturalFreq: ~conductorFreq * ~roles[0].roleMultiplier * ~naturalFreqScatter[0],
		roleOffset: ~roles[0].roleOffset, roleMultiplier: ~roles[0].roleMultiplier),

	// --- Snare: hell, scharfer Einsatz, Backbeat auf Zählzeit 2 ---
	(soundParams: (ringFreq1: 3800, wingRate: 34, wingDuty: 0.3, ringDecay1: 0.015,
		ringDecay2: 0.01, amp: 0.42, pattern: nil),
		chirpDur: 0.09, naturalFreq: ~conductorFreq * ~roles[1].roleMultiplier * ~naturalFreqScatter[1],
		roleOffset: ~roles[1].roleOffset, roleMultiplier: ~roles[1].roleMultiplier),

	// --- Hihat: sehr hell, kurz, dicht — treibender 16tel-artiger Puls ---
	(soundParams: (ringFreq1: 6000, wingRate: 48, wingDuty: 0.25, ringDecay1: 0.006,
		ringDecay2: 0.005, amp: 0.2, pattern: nil),
		chirpDur: 0.04, naturalFreq: ~conductorFreq * ~roles[2].roleMultiplier * ~naturalFreqScatter[2],
		roleOffset: ~roles[2].roleOffset, roleMultiplier: ~roles[2].roleMultiplier),

	// --- Floor-Tom: dunkler, synkopierte Fill-Akzente ---
	(soundParams: (ringFreq1: 1400, wingRate: 20, wingDuty: 0.5, ringDecay1: 0.035,
		ringDecay2: 0.025, amp: 0.35, pattern: nil),
		chirpDur: 0.14, naturalFreq: ~conductorFreq * ~roles[3].roleMultiplier * ~naturalFreqScatter[3],
		roleOffset: ~roles[3].roleOffset, roleMultiplier: ~roles[3].roleMultiplier),

	// --- Mid-Tom: etwas heller als Floor-Tom, Fill-Doppelschlag gg. Taktende ---
	(soundParams: (ringFreq1: 2000, wingRate: 22, wingDuty: 0.45, ringDecay1: 0.03,
		ringDecay2: 0.02, amp: 0.32, pattern: nil),
		chirpDur: 0.12, naturalFreq: ~conductorFreq * ~roles[4].roleMultiplier * ~naturalFreqScatter[4],
		roleOffset: ~roles[4].roleOffset, roleMultiplier: ~roles[4].roleMultiplier),
];
~numVoices = ~voices.size;

// stationär im Kreis positioniert (SteadyMoveRule, wie cricket_chorus.scd), Radius leicht
// gestreut, damit die 5 Stimmen nicht alle exakt gleich weit vom Hörer entfernt sind.
~soundObjects = ~voices.collect { |voice, i|
	var angle = (i / ~numVoices) * 2pi;
	var radius = rrand(2.5, 4.0);
	var movableParams = (
		pos: [radius * cos(angle), radius * sin(angle), 0],
		moveRule: SteadyMoveRule.new,
		roomRadius: radius + 2
	);
	~room.registerFromBuilder(voice[\soundParams], movableParams, soundClass: CricketSound)
};

// pattern: nil im soundParams-Event (oben) reicht NICHT, um CricketSounds eingebautes
// CallingPattern abzuschalten (siehe CLAUDE.md-Gotcha, gleiches Vorgehen wie cricket_chorus.scd,
// Intent 61 Task 2.3) — muss vor ~room.play (Block 3) gesetzt werden.
~soundObjects.do { |so| so.sound.pattern = nil };

// parallel zu ~soundObjects extrahiert, für den Dirigenten unten.
~roleOffsets = ~voices.collect { |v| v[\roleOffset] };
~roleMultipliers = ~voices.collect { |v| v[\roleMultiplier] };
~naturalFreqs = ~voices.collect { |v| v[\naturalFreq] };
~chirpDurs = ~voices.collect { |v| v[\chirpDur] };
)

// --- 2) Dirigent, Tick-Routine, Timeline ------------------------------------------------
(
~totalDur = 100;       // Gesamtdauer des Stücks in Sekunden
~chaosDur = 8;         // reine Chaos-Phase am Anfang, bevor die Kopplung zu wirken beginnt
~coupleRampDur = 40;   // Dauer, über die conductorCoupling von 0 auf ihren Zielwert steigt
// ~targetCoupling: wie in cricket_chorus.scd (Intent 61, Task 2.3) muss conductorCoupling weit
// über der reinen Frequenz-Einrast-Schwelle liegen, um auch PHASEN-seitig hörbar synchron zu
// werden (sin(dev) = Frequenzabstand*2pi / conductorCoupling) statt nur mit gleicher Periode,
// aber hörbar versetzt zu feuern. Hihat hat hier mit roleMultiplier 12 den bei weitem grössten
// Frequenzabstand zur naturalFreq aller 5 Stimmen -- Startwert daher deutlich höher als der in
// cricket_chorus.scd für die dortige max. roleMultiplier 2 (Doppelzeit) verifizierte Wert;
// finale Justierung folgt per Hörtest (Task 5).
~targetCoupling = 60;

// ~phaseNoise (Intent 62 Task 5, Hörtest-Feedback): ohne Rauschen ist jede Stimme vor dem
// Einrasten ein exakt periodischer Takt -- unkoordiniert zueinander, aber pro Stimme klingt das
// schon mechanisch statt stochastisch. phaseNoise bleibt über das ganze Stück konstant (kein
// eigenes Ramp nötig): bei kleiner conductorCoupling (Chaos-Phase) dominiert das Rauschen relativ
// stark, sobald conductorCoupling gegen ~targetCoupling rampt, wird die Rückstellkraft zur
// Rollen-Zielphase viel stärker als das konstante Rauschen -- der Groove rastet trotzdem hörbar
// ein, bleibt aber mit einem leichten organischen Wobble statt komplett mechanisch.
//
// Wertebereich: sinnvoll nur >= 0 (0 = kein Rauschen, exakt das alte deterministische Verhalten).
// Grössenordnung ist ein Diffusionskoeffizient (rad/sqrt(s)), keine Frequenz/kein Prozentwert.
// Die verbleibende Phasenstreuung im EINGERASTETEN Zustand (nachdem conductorCoupling bis
// ~targetCoupling hochgerampt ist) skaliert grob wie phaseNoise / sqrt(2 * conductorCoupling) --
// zur Einordnung bei ~targetCoupling: 60 (aktueller Wert oben):
//   phaseNoise   Streuung im Lock (rad)   Charakter
//   0.6          ~0.055                   sehr eng, kaum hörbarer Wobble im fertigen Groove
//   2-3          ~0.2-0.27                hörbar organischer, "lebendiger" Lock
//   ~5           ~0.46                    deutlich lockerer, Groove "schwimmt" spürbar
//   >= ~11 (= sqrt(2*60))                 Lock destabilisiert sich, Rauschen überwiegt die
//                                         Rückstellkraft -- rastet praktisch nie sauber ein
// Während der Chaos-Phase (conductorCoupling = 0) wirkt phaseNoise dagegen ungebremst -- je
// höher, desto stochastischer jede einzelne Stimme dort. Live veränderbar (<>-Attribut), z.B.
// ~oscillators.phaseNoise = 2.0 während des Abspielens zum direkten Vergleich.
~phaseNoise = 3;

~oscillators = CoupledOscillators.new(
	naturalFreqs: ~naturalFreqs,
	coupling: 0,                   // keine Mutual-Kopplung -- nur der Dirigent zieht die Rollen
	initialPhases: ~numVoices.collect { 2pi.rand },
	conductorFreq: ~conductorFreq,
	conductorCoupling: 0,          // startet bei 0, Timeline rampt es hoch (siehe unten)
	roleOffsets: ~roleOffsets,
	roleMultipliers: ~roleMultipliers,
	phaseNoise: ~phaseNoise
);

~timeline = Timeline.new(~totalDur);
~timeline.ramp(~chaosDur, ~chaosDur + ~coupleRampDur, 0, ~targetCoupling,
	{ |v| ~oscillators.conductorCoupling = v });

// zentrale Tick-Routine (identisches Muster wie cricket_chorus.scd) -- treibt CoupledOscillators
// in Echtzeit und löst bei jedem Feuerungs-Event einen kurzen Gate-An/Aus-Impuls (Ruflänge = die
// kuratierte chirpDur dieser Stimme) auf dem jeweiligen laufenden Synth aus.
~oscDt = 0.02;
~oscRoutine = Routine({
	loop {
		var fired = ~oscillators.tick(~oscDt);
		fired.do { |i|
			Routine({
				var synth = ~soundObjects[i].sound.synth;
				synth.set(\gate, 1);
				~chirpDurs[i].wait;
				synth.set(\gate, 0);
			}).play;
		};
		~oscDt.wait;
	}
});

// Stück-Ende: Tick-Routine stoppen, alle Grillen klickfrei verstummen (fadeOutAndStop, gleiches
// Muster wie cricket_chorus.scd/messiaen_birds.scd/dawn_chorus.scd).
~timeline.at(~totalDur, {
	"Grillen-Groove ist fertig.".postln;
	~oscRoutine.stop;
	~soundObjects.do { |so|
		so.fadeOutAndStop(0.5, { |stopped| ~room.orchestra.unregister(stopped) })
	};
});
)

// --- 3) Abspielen ---

(
"Beginn des Grillen-Grooves -- unkoordiniert.".postln;
~room.play;
// Start: keine Grille dauerzirpt (SynthDef-Default gate=1) -- nur die vom Dirigenten
// getriggerten Kurz-Rufe sollen hörbar sein.
~soundObjects.do { |so| so.sound.synth.set(\gate, 0) };
~oscRoutine.play;
~timeline.play;
)

(
// eigenes presets-/recordings-Verzeichnis (nicht dasselbe wie cricket_chorus), damit sich die
// Presets der beiden Stücke nicht überschreiben.
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
	+/+ "cricket_groove").fullPath;
~recordingsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname
	+/+ "recordings").fullPath;
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true,
	presetsDir: ~presetsDir, recordingsDir: ~recordingsDir);
~controlPanel.show;
)

// --- 4) Stoppen ---
~timeline.stop;
~oscRoutine.stop;
~room.teardown;
~controlPanel.free;
