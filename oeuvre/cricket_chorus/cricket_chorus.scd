// cricket_chorus.scd — Grillenchor mit emergenter Synchronisation (Intent 61): 12 kuratierte,
// stationäre Grillen-Stimmen, jede mit einer festen rhythmischen Rolle in einem gemeinsamen
// Pattern (nicht bloß einem gemeinsamen Puls) — Grundschlag, Off-Beat, Doppelzeit, Halbzeit.
// Zu Beginn rufen alle unkoordiniert mit ihrer eigenen naturalFreq; über die Stückdauer zieht
// ein "Dirigent" (CoupledOscillators, siehe classes/sg/spatial/CoupledOscillators.sc) sie zu
// ihrer jeweiligen Rollen-Position im Pattern, ohne den Zeitpunkt des Einrastens zu skripten —
// echte Selbstorganisation (Kuramoto-Modell) statt geskriptetem Übergang.
//
// Vier rhythmische Schichten (roleMultiplier/roleOffset relativ zum gemeinsamen Puls
// ~conductorFreq):
//   - Puls (roleMultiplier 1, roleOffset 0): 3 Grillen, tragen den Grundschlag
//   - Off-Beat (roleMultiplier 1, roleOffset != 0): 3 Grillen, je eigene Position abseits des
//     Grundschlags
//   - Doppelzeit (roleMultiplier 2): 3 schnelle, helle Grillen im doppelten Tempo
//   - Halbzeit (roleMultiplier 0.5): 3 tiefe, seltene Grillen im halben Tempo
//
// Erster Block: die 12 Stimmen + ihre Rollen definieren und stationär im Raum registrieren
// (pattern: nil -- kein internes CallingPattern, gate wird extern per CoupledOscillators-
// Feuerungs-Event getriggert). Zweiter Block: Dirigent (CoupledOscillators), Tick-Routine,
// Timeline (rampt conductorCoupling hoch, beendet das Stück).
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
CricketSound.addSynthDef;
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 0.5);

// ~conductorFreq: gemeinsamer Grundpuls in Hz, auf den der Chor letztlich konvergiert (Intent
// 61, "Dirigent") -- 1.3Hz entspricht ~78 Anschlägen pro Minute.
~conductorFreq = 1.3;

// 12 kuratierte Stimmen: jede mit eigenem Klangcharakter (ringFreq1/wingRate/wingDuty/
// ringDecay/amp), eigener, kurzer Ruflänge (chirpDur) und einer festen rhythmischen Rolle
// (roleOffset/roleMultiplier, siehe Kommentar oben). naturalFreq ist die eigene, unbeeinflusste
// Ausgangsfrequenz vor dem Einrasten -- bewusst weit von ihrer jeweiligen Rollen-Zielfrequenz
// (roleMultiplier * conductorFreq) entfernt gewählt, damit das Stück hörbar unkoordiniert
// beginnt.
~voices = [
	// --- Puls: 3 Grillen auf dem Grundschlag ---
	(soundParams: (ringFreq1: 2200, wingRate: 22, wingDuty: 0.45, ringDecay1: 0.03,
		ringDecay2: 0.02, amp: 0.4, pattern: nil),
		chirpDur: 0.14, naturalFreq: ~conductorFreq * 0.55, roleOffset: 0, roleMultiplier: 1),
	(soundParams: (ringFreq1: 2600, wingRate: 26, wingDuty: 0.4, ringDecay1: 0.025,
		ringDecay2: 0.018, amp: 0.38, pattern: nil),
		chirpDur: 0.12, naturalFreq: ~conductorFreq * 1.6, roleOffset: 0, roleMultiplier: 1),
	(soundParams: (ringFreq1: 3000, wingRate: 24, wingDuty: 0.5, ringDecay1: 0.02,
		ringDecay2: 0.015, amp: 0.42, pattern: nil),
		chirpDur: 0.15, naturalFreq: ~conductorFreq * 0.8, roleOffset: 0, roleMultiplier: 1),

	// --- Off-Beat: 3 Grillen auf unterschiedlichen Positionen abseits des Grundschlags ---
	(soundParams: (ringFreq1: 3600, wingRate: 30, wingDuty: 0.35, ringDecay1: 0.02,
		ringDecay2: 0.012, amp: 0.32, pattern: nil),
		chirpDur: 0.10, naturalFreq: ~conductorFreq * 1.2, roleOffset: 2pi / 3, roleMultiplier: 1),
	(soundParams: (ringFreq1: 4100, wingRate: 28, wingDuty: 0.4, ringDecay1: 0.018,
		ringDecay2: 0.01, amp: 0.3, pattern: nil),
		chirpDur: 0.09, naturalFreq: ~conductorFreq * 0.65, roleOffset: pi, roleMultiplier: 1),
	(soundParams: (ringFreq1: 4600, wingRate: 32, wingDuty: 0.45, ringDecay1: 0.015,
		ringDecay2: 0.01, amp: 0.34, pattern: nil),
		chirpDur: 0.11, naturalFreq: ~conductorFreq * 1.45, roleOffset: 4pi / 3, roleMultiplier: 1),

	// --- Doppelzeit: 3 schnelle, helle Grillen im doppelten Tempo ---
	(soundParams: (ringFreq1: 5200, wingRate: 40, wingDuty: 0.3, ringDecay1: 0.01,
		ringDecay2: 0.008, amp: 0.28, pattern: nil),
		chirpDur: 0.07, naturalFreq: ~conductorFreq * 2 * 0.6, roleOffset: 0, roleMultiplier: 2),
	(soundParams: (ringFreq1: 5800, wingRate: 45, wingDuty: 0.28, ringDecay1: 0.009,
		ringDecay2: 0.007, amp: 0.26, pattern: nil),
		chirpDur: 0.06, naturalFreq: ~conductorFreq * 2 * 1.4, roleOffset: 2pi / 3, roleMultiplier: 2),
	(soundParams: (ringFreq1: 6400, wingRate: 42, wingDuty: 0.32, ringDecay1: 0.008,
		ringDecay2: 0.006, amp: 0.27, pattern: nil),
		chirpDur: 0.08, naturalFreq: ~conductorFreq * 2 * 0.85, roleOffset: 4pi / 3, roleMultiplier: 2),

	// --- Halbzeit: 3 tiefe, seltene Grillen im halben Tempo ---
	(soundParams: (ringFreq1: 1600, wingRate: 16, wingDuty: 0.55, ringDecay1: 0.04,
		ringDecay2: 0.03, amp: 0.45, pattern: nil),
		chirpDur: 0.2, naturalFreq: ~conductorFreq * 0.5 * 0.7, roleOffset: 0, roleMultiplier: 0.5),
	(soundParams: (ringFreq1: 1900, wingRate: 18, wingDuty: 0.5, ringDecay1: 0.035,
		ringDecay2: 0.025, amp: 0.43, pattern: nil),
		chirpDur: 0.22, naturalFreq: ~conductorFreq * 0.5 * 1.5, roleOffset: pi, roleMultiplier: 0.5),
	(soundParams: (ringFreq1: 2000, wingRate: 20, wingDuty: 0.5, ringDecay1: 0.03,
		ringDecay2: 0.022, amp: 0.4, pattern: nil),
		chirpDur: 0.19, naturalFreq: ~conductorFreq * 0.5 * 1.1, roleOffset: 2pi / 3, roleMultiplier: 0.5),
];
~numVoices = ~voices.size;

// stationär im Kreis positioniert (SteadyMoveRule, wie crickets_with_breaks.scd), Radius leicht
// gestreut, damit die 12 Stimmen nicht alle exakt gleich weit vom Hörer entfernt sind.
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
// ~targetCoupling: die am weitesten von ihrer Rollen-Zielfrequenz gestreute Stimme (Doppelzeit-
// Schicht, naturalFreq bis zu 1.04Hz über ihrer Zielfrequenz 2*conductorFreq) braucht
// conductorCoupling > ca. 6.5, um überhaupt einzurasten (Schwelle ~ |naturalFreq - Zielfreq| *
// 2pi, siehe CoupledOscillators-Tests). 12 gibt genug Sicherheitsmarge für zuverlässiges,
// nicht bloss knapp-kritisches (und damit sehr langsames) Einrasten. Feintuning per Hörtest
// (Task 2.3).
~targetCoupling = 12;

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

// zentrale Tick-Routine (Muster wie Timeline>>play/Orchestra: eine Routine statt viele pro
// Stimme) -- treibt CoupledOscillators in Echtzeit und löst bei jedem Feuerungs-Event einen
// kurzen Gate-An/Aus-Impuls (Ruflänge = die kuratierte chirpDur dieser Stimme) auf dem
// jeweiligen laufenden Synth aus.
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
// Muster wie messiaen_birds.scd/dawn_chorus.scd).
~timeline.at(~totalDur, {
	"Grillenchor ist fertig.".postln;
	~oscRoutine.stop;
	~soundObjects.do { |so|
		so.fadeOutAndStop(0.5, { |stopped| ~room.orchestra.unregister(stopped) })
	};
});
)

// --- 3) Abspielen ---

(
"Beginn des Grillenchors -- unkoordiniert.".postln;
~room.play;
// Start: keine Grille dauerzirpt (SynthDef-Default gate=1) -- nur die vom Dirigenten
// getriggerten Kurz-Rufe sollen hörbar sein.
~soundObjects.do { |so| so.sound.synth.set(\gate, 0) };
~oscRoutine.play;
~timeline.play;
)

(
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname.dirname +/+ "presets"
	+/+ "cricket_chorus").fullPath;
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
