// CoupledOscillators — gekoppelte Phasenoszillatoren nach dem Kuramoto-Modell (Intent 61):
// reine, server-freie tick(dt)-Logik (analog Timeline/Orchestra), kein Server-Bezug, testbar.
// Jeder Oszillator hat eine eigene naturalFreq (Hz) und läuft unabhängig um, solange coupling
// (Mutual-Kopplung untereinander) und conductorCoupling (Kopplung an einen externen "Dirigenten"
// fester conductorFreq) beide 0 sind -- das erzeugt das anfängliche, unkoordinierte Rufen. Über
// Zeit (ausreichend starke Kopplung) synchronisieren sich die Phasen von selbst (orderParameter
// -> 1), ohne dass der Zeitpunkt der Synchronisation geskriptet wird.
//
// roleOffsets/roleMultipliers (Default: überall 0 bzw. 1, reines Unisono-Verhalten) geben jedem
// Oszillator eine feste rhythmische Rolle statt reiner Gleichschaltung: die Ziel-Phase
// (roleAngle) eines Oszillators ist roleMultiplier * conductorPhase + roleOffset. Sowohl die
// Mutual- als auch die Dirigenten-Kopplung wirken auf die Abweichung von dieser Ziel-Phase
// (deviation = phase - roleAngle) statt auf die rohe Phase -- der ganze Chor einigt sich auf
// denselben Puls, aber jeder Oszillator bleibt an seiner zugewiesenen Position/seinem
// zugewiesenen Tempo-Verhältnis im Pattern (z.B. Grundschlag/Doppelzeit/Off-Beat), statt ins
// Unisono gezogen zu werden. Bei roleOffset=0/roleMultiplier=1 für alle reduziert sich das exakt
// auf das einfache Kuramoto-Modell.
CoupledOscillators {
	var <phases;         // aktuelle Phasen, 0..2pi, ein Wert pro Oszillator
	var <>coupling;       // K, Stärke der Mutual-Kopplung untereinander (live veränderbar)
	var naturalFreqs;     // eigene, unbeeinflusste Frequenz jedes Oszillators, in Hz
	var <>conductorFreq;      // feste Zielfrequenz des Dirigenten, in Hz
	var <>conductorCoupling;  // Kc, Stärke der Kopplung an den Dirigenten (live veränderbar)
	var <conductorPhase;      // Phase des Dirigenten, läuft unbeeinflusst von den Oszillatoren --
	                          // bewusst UNwrapped (kein mod 2pi), siehe tick-Kommentar unten
	var roleOffsets;      // fester Phasenversatz pro Oszillator (Position im Pattern)
	var roleMultipliers;  // rationales Tempo-Verhältnis pro Oszillator zur conductorFreq

	*new { |naturalFreqs, coupling = 0, initialPhases, conductorFreq = 0, conductorCoupling = 0,
			roleOffsets, roleMultipliers|
		^super.new.init(naturalFreqs, coupling, initialPhases, conductorFreq, conductorCoupling,
			roleOffsets, roleMultipliers);
	}

	init { |aNaturalFreqs, aCoupling, aInitialPhases, aConductorFreq, aConductorCoupling,
			aRoleOffsets, aRoleMultipliers|
		naturalFreqs = aNaturalFreqs;
		coupling = aCoupling;
		phases = aInitialPhases.copy;
		conductorFreq = aConductorFreq;
		conductorCoupling = aConductorCoupling;
		conductorPhase = 0;
		roleOffsets = aRoleOffsets ?? { naturalFreqs.collect { 0 } };
		roleMultipliers = aRoleMultipliers ?? { naturalFreqs.collect { 1 } };
	}

	// reine Logik, ohne Server-Bezug -- testbar wie Timeline>>tick/Orchestra>>tick. Liefert die
	// Indizes aller Oszillatoren zurück, deren Phase in diesem Tick über 2pi gelaufen ist
	// (Feuerungs-Event, z.B. für einen Ruf-Trigger). Alle neuen Phasen basieren auf einem
	// Snapshot der alten Phasen/Abweichungen -- keine benutzt bereits aktualisierte Werte
	// anderer Oszillatoren, sonst würde die Update-Reihenfolge das Ergebnis verzerren.
	tick { |dt|
		var n = phases.size;
		var oldPhases = phases.copy;
		var roleAngles = n.collect { |i| (roleMultipliers[i] * conductorPhase) + roleOffsets[i] };
		var deviations = n.collect { |i| oldPhases[i] - roleAngles[i] };
		var fired = [];

		n.do { |i|
			var couplingSum = n.collect { |j| sin(deviations[j] - deviations[i]) }.sum;
			var conductorTerm = conductorCoupling * sin(deviations[i].neg);
			var dtheta = (naturalFreqs[i] * 2pi + (coupling * couplingSum / n) + conductorTerm) * dt;
			var newPhase = oldPhases[i] + dtheta;

			if(newPhase >= 2pi) { fired = fired.add(i) };
			phases[i] = newPhase % 2pi;
		};

		// conductorPhase bewusst NICHT mod 2pi zurückgewickelt (Bug-Fix, Intent 61 Task 2.3):
		// roleAngle = roleMultiplier * conductorPhase wird nur innerhalb von sin() verwendet,
		// wo ein Sprung von genau 2pi unsichtbar ist -- das gilt aber nur, wenn roleMultiplier
		// eine ganze Zahl ist (roleMultiplier * 2pi bleibt dann ein Vielfaches von 2pi). Bei
		// einem gebrochenen roleMultiplier (z.B. 0.5 für eine "Halbzeit"-Rolle) würde ein
		// gewrapptes conductorPhase bei jedem Dirigenten-Umlauf einen echten Phasensprung in
		// roleAngle erzeugen und die Kopplung destabilisieren (siehe
		// TestCoupledOscillators>>test_tickWithFractionalRoleMultiplierLocksToRationalMultipleOfConductorFreq).
		// Unwrapped bleibt für die Dauer eines Stücks (Minuten) unproblematisch für Float-Präzision.
		conductorPhase = conductorPhase + (conductorFreq * 2pi * dt);

		^fired
	}

	// Kuramoto-Ordnungsparameter: Betrag des gemittelten Phasenvektors aller Oszillatoren,
	// 0 = völlig unkoordiniert (gleichmässig über den Kreis verteilt), 1 = perfekt synchron.
	orderParameter {
		var n = phases.size;
		var sumCos = phases.collect { |p| cos(p) }.sum;
		var sumSin = phases.collect { |p| sin(p) }.sum;
		^((sumCos / n).squared + (sumSin / n).squared).sqrt
	}
}
