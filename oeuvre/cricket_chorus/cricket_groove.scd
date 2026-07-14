// cricket_groove.scd — Fünfstimmiger Grillen-Groove mit emergenter Synchronisation (Intent 62),
// Rollen abgeleitet aus einem echten Referenz-Drumgroove (~/Downloads/Download.midi: 5 Stimmen —
// Kick/Snare/Hihat/Floor-Tom/Mid-Tom, 16tel-Raster, 80 BPM) statt aus vier frei erfundenen
// Layern wie in cricket_chorus.scd (Intent 61). Gleiches Grundprinzip wie dort: zu Beginn ruft
// jede Stimme unkoordiniert mit ihrer eigenen naturalFreq, ein "Dirigent" (CoupledOscillators,
// siehe classes/sg/spatial/CoupledOscillators.sc) zieht sie über die Stückdauer zu ihrer
// jeweiligen Rollen-Position — echte Selbstorganisation (Kuramoto-Modell), kein geskripteter
// Step-Sequencer.
//
// Rollen-Ableitung aus dem MIDI-Groove (Zyklus = 1 Takt = 16 Schritte, Winkel = Schritt * pi/8,
// roleAngle = roleMultiplier * conductorPhase + roleOffset):
//   - Kick   (MIDI-Note 35, Schritte 0+8/Takt):  roleMultiplier 2, roleOffset 0
//   - Snare  (MIDI-Note 38, Schritt 4/Takt, Backbeat): roleMultiplier 1, roleOffset pi/2
//   - Hihat  (MIDI-Note 42, 12 von 16 Schritten/Takt): roleMultiplier 12, roleOffset 0 —
//     bewusste Näherung als gleichmässiger dichter Puls statt Step-für-Step-Nachbau der exakten
//     (ungleichmässigen) 16tel-Lücken; kein Sequencer, siehe Intent-62-Entscheid.
//   - Floor-Tom (MIDI-Note 43, 5 synkopierte Akzente/Takt an Schritt 3/5/7/13/15): roleMultiplier
//     5, roleOffset 3*pi/8 (zirkulärer Mittelwert der 5 Schritt-Winkel).
//   - Mid-Tom (MIDI-Note 48, 2 eng benachbarte Fill-Schläge an Schritt 11/12): roleMultiplier 2,
//     roleOffset 11.5*pi/8 (Mittelwert der beiden Schritt-Winkel).
// Die im MIDI zusätzlich vorhandene Verdichtung im zweiten Takt (mehr Snare-/Tom-Schläge gegen
// Taktende) wird NICHT 1:1 nachgebaut, sondern bewusst als Näherung über die genannten
// Rollen-Positionen abgebildet.
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
// übernommen, Feintuning für die 5-stimmige Besetzung folgt per Hörtest (Task 4).
~conductorFreq = 0.35;

// 5 Stimmen, je einer MIDI-Rolle zugeordnet. naturalFreq ist die eigene, unbeeinflusste
// Ausgangsfrequenz vor dem Einrasten — bewusst weit von der jeweiligen Rollen-Zielfrequenz
// (roleMultiplier * conductorFreq) entfernt, damit das Stück hörbar unkoordiniert beginnt
// (gleiches Muster wie cricket_chorus.scd).
~voices = [
	// --- Kick: tief, tonal, trägt den Grundschlag (Taktanfang + Taktmitte) ---
	(soundParams: (ringFreq1: 900, wingRate: 14, wingDuty: 0.6, ringDecay1: 0.05,
		ringDecay2: 0.04, amp: 0.5, pattern: nil),
		chirpDur: 0.18, naturalFreq: ~conductorFreq * 2 * 0.6, roleOffset: 0, roleMultiplier: 2),

	// --- Snare: hell, scharfer Einsatz, Backbeat auf Zählzeit 2 ---
	(soundParams: (ringFreq1: 3800, wingRate: 34, wingDuty: 0.3, ringDecay1: 0.015,
		ringDecay2: 0.01, amp: 0.42, pattern: nil),
		chirpDur: 0.09, naturalFreq: ~conductorFreq * 1 * 1.7, roleOffset: pi / 2, roleMultiplier: 1),

	// --- Hihat: sehr hell, kurz, dicht — treibender 16tel-artiger Puls ---
	(soundParams: (ringFreq1: 6000, wingRate: 48, wingDuty: 0.25, ringDecay1: 0.006,
		ringDecay2: 0.005, amp: 0.2, pattern: nil),
		chirpDur: 0.04, naturalFreq: ~conductorFreq * 12 * 0.7, roleOffset: 0, roleMultiplier: 12),

	// --- Floor-Tom: dunkler, synkopierte Fill-Akzente ---
	(soundParams: (ringFreq1: 1400, wingRate: 20, wingDuty: 0.5, ringDecay1: 0.035,
		ringDecay2: 0.025, amp: 0.35, pattern: nil),
		chirpDur: 0.14, naturalFreq: ~conductorFreq * 5 * 1.3, roleOffset: 3 * pi / 8,
		roleMultiplier: 5),

	// --- Mid-Tom: etwas heller als Floor-Tom, Fill-Doppelschlag gg. Taktende ---
	(soundParams: (ringFreq1: 2000, wingRate: 22, wingDuty: 0.45, ringDecay1: 0.03,
		ringDecay2: 0.02, amp: 0.32, pattern: nil),
		chirpDur: 0.12, naturalFreq: ~conductorFreq * 2 * 1.6, roleOffset: 11.5 * pi / 8,
		roleMultiplier: 2),
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
// finale Justierung folgt per Hörtest (Task 4).
~targetCoupling = 60;

~oscillators = CoupledOscillators.new(
	naturalFreqs: ~naturalFreqs,
	coupling: 0,                   // keine Mutual-Kopplung -- nur der Dirigent zieht die Rollen
	initialPhases: ~numVoices.collect { 2pi.rand },
	conductorFreq: ~conductorFreq,
	conductorCoupling: 0,          // startet bei 0, Timeline rampt es hoch (siehe unten)
	roleOffsets: ~roleOffsets,
	roleMultipliers: ~roleMultipliers
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
