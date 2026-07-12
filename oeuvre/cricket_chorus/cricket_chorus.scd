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
// Task 2.1 (dieser Block): die 12 Stimmen + ihre Rollen definieren und stationär im Raum
// registrieren (pattern: nil -- kein internes CallingPattern, gate wird extern per
// CoupledOscillators-Feuerungs-Event getriggert). Tick-Routine/Timeline/Kontrollpanel folgen in
// Task 2.2.
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

// für Task 2.2 (CoupledOscillators-Tick-Routine) parallel zu ~soundObjects extrahiert.
~roleOffsets = ~voices.collect { |v| v[\roleOffset] };
~roleMultipliers = ~voices.collect { |v| v[\roleMultiplier] };
~naturalFreqs = ~voices.collect { |v| v[\naturalFreq] };
~chirpDurs = ~voices.collect { |v| v[\chirpDur] };
)
