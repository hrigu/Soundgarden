// Test für CoupledOscillators (Intent 61). Ausführen über run_tests.scd. Reine sclang-Logik,
// kein Server nötig — tick(dt) wird hier direkt aufgerufen, analog zu Timeline/Orchestra
// (TestTimeline.sc/TestOrchestra.sc).
TestCoupledOscillators : UnitTest {

	// coupling: 0 -- jede Phase läuft unabhängig mit ihrer eigenen naturalFreq um, ohne
	// gegenseitige Beeinflussung.
	test_tickWithoutCouplingAdvancesEachPhaseIndependently {
		var osc = CoupledOscillators.new(naturalFreqs: [1.0, 2.0], coupling: 0,
			initialPhases: [0, 0]);

		osc.tick(0.1); // 1Hz -> +0.1*2pi ; 2Hz -> +0.2*2pi

		this.assertFloatEquals(osc.phases[0], 0.1 * 2pi);
		this.assertFloatEquals(osc.phases[1], 0.2 * 2pi);
	}

	// ein Phasenumlauf über 2pi hinaus wird als Feuerungs-Event (Index) gemeldet, die Phase
	// wird dabei auf den Rest nach dem Umlauf zurückgesetzt (mod 2pi), nicht einfach weiter
	// akkumuliert.
	test_tickReportsFiringWhenPhaseWrapsPast2pi {
		var osc = CoupledOscillators.new(naturalFreqs: [1.0], coupling: 0,
			initialPhases: [2pi * 0.9]);
		var fired;

		fired = osc.tick(0.2); // +0.2*2pi -> insgesamt 1.1*2pi, wrap

		this.assertEquals(fired, [0]);
		this.assertFloatEquals(osc.phases[0], 0.1 * 2pi);
	}

	// kein Umlauf in diesem Tick -> leere Feuerungs-Liste, kein falsches Feuern.
	test_tickReportsNoFiringWhenPhaseStaysBelow2pi {
		var osc = CoupledOscillators.new(naturalFreqs: [1.0], coupling: 0,
			initialPhases: [0]);
		var fired;

		fired = osc.tick(0.1);

		this.assertEquals(fired, []);
	}

	// mehrere Oszillatoren, nur einer wrapt in diesem Tick -> nur dessen Index wird gemeldet.
	test_tickReportsOnlyIndicesThatWrapInThisTick {
		var osc = CoupledOscillators.new(naturalFreqs: [1.0, 5.0], coupling: 0,
			initialPhases: [0, 2pi * 0.95]);
		var fired;

		fired = osc.tick(0.1); // Oszillator 0: +0.1*2pi, kein Wrap; Oszillator 1: +0.5*2pi, Wrap

		this.assertEquals(fired, [1]);
	}

	// Mutual-Kopplung (Intent 61, Kuramoto-Modell): vier eng beieinanderliegende natürliche
	// Frequenzen, weit gestreute Startphasen (Ordnungsparameter exakt 0 -- Phasen gleichmässig
	// über den Kreis verteilt), ausreichend starke coupling. Über eine simulierte Dauer von 5s
	// (500 Ticks à 10ms) richten sich die Phasen von selbst aneinander aus -- orderParameter
	// nähert sich 1 an. Werte per Python-Referenzsimulation der geplanten tick-Formel
	// vorab geprüft (siehe Intent-Planung), nicht willkürlich geraten.
	test_tickWithMutualCouplingConvergesOrderParameterTowardOne {
		var osc = CoupledOscillators.new(naturalFreqs: [1.0, 1.05, 0.95, 1.02], coupling: 4.0,
			initialPhases: [0, pi / 2, pi, 3 * pi / 2]);

		this.assertFloatEquals(osc.orderParameter, 0.0,
			"Startphasen gleichmässig über den Kreis verteilt -- komplett unkoordiniert");

		500.do { osc.tick(0.01) };

		this.assert(osc.orderParameter > 0.95,
			"nach 5s simulierter Zeit mit ausreichender Kopplung sind die Phasen synchronisiert (orderParameter %)".format(osc.orderParameter));
	}

	// Dirigent (Intent 61): vier natürliche Frequenzen deutlich unter der vorgegebenen
	// conductorFreq (Mittelwert 1.275, conductorFreq 1.6) -- ohne Dirigent würde ein reines
	// Mutual-Modell zum Mittelwert konvergieren. Mit ausreichend starker conductorCoupling und
	// coupling: 0 (keine Mutual-Kopplung, damit nur der Dirigenten-Effekt gemessen wird) lockt
	// jeder Oszillator stattdessen auf conductorFreq: der Phasenversatz zum Dirigenten wird nach
	// dem Einschwingen (5s) über eine weitere simulierte Sekunde hinweg praktisch konstant --
	// beweist Frequenzgleichheit mit dem Dirigenten, nicht mit dem Mittelwert der 12 (hier: 4)
	// Einzelfrequenzen. Zahlen per Python-Referenzsimulation der geplanten tick-Formel
	// vorab geprüft (siehe Intent-Planung).
	test_tickWithConductorLocksPhasesToConductorFreqNotToMeanNaturalFreq {
		var osc = CoupledOscillators.new(naturalFreqs: [1.2, 1.25, 1.3, 1.35], coupling: 0,
			initialPhases: [0, pi / 2, pi, 3 * pi / 2],
			conductorFreq: 1.6, conductorCoupling: 4.0);
		var diffsBefore, diffsAfter;

		500.do { osc.tick(0.01) }; // 5s Einschwingzeit

		diffsBefore = osc.phases.collect { |p|
			((p - osc.conductorPhase + pi) % 2pi) - pi
		};

		100.do { osc.tick(0.01) }; // weitere simulierte Sekunde

		diffsAfter = osc.phases.collect { |p|
			((p - osc.conductorPhase + pi) % 2pi) - pi
		};

		diffsBefore.do { |d, i|
			this.assertFloatEquals(diffsAfter[i], d,
				"Phasenversatz zum Dirigenten bleibt über Zeit konstant -- Frequenz ist auf conductorFreq gelockt, nicht auf den Mittelwert der Einzelfrequenzen",
				0.01);
		};
	}

	// roleOffsets (Intent 61, "Pattern statt Unisono"): vier Oszillatoren mit gleichem
	// roleMultiplier (1), aber vier verschiedenen roleOffset-Werten, gleichmässig über den
	// Kreis verteilt (0, pi/2, pi, 3pi/2) -- wie vier feste rhythmische Positionen in einem
	// gemeinsamen Takt. Nach dem Einschwingen (5s) stabilisiert sich jeder Oszillator nahe
	// seiner EIGENEN Rollen-Zielphase (conductorPhase * roleMultiplier + roleOffset), nicht bei
	// einer gemeinsamen Phase wie im Unisono-Fall -- das ist der Unterschied zwischen "Puls" und
	// "Pattern". Toleranz (0.5 rad) grosszügig, weil die Streuung der natürlichen Frequenzen
	// (wie im Dirigenten-Test) einen kleinen, aber stabilen Rest-Versatz zur Zielphase erzeugt,
	// siehe Python-Referenzsimulation der geplanten tick-Formel (Diffs dort < 0.32 rad).
	test_tickWithDifferentRoleOffsetsLocksEachOscillatorToItsOwnPatternPosition {
		var roleOffsets = [0, pi / 2, pi, 3 * pi / 2];
		var osc = CoupledOscillators.new(naturalFreqs: [1.3, 1.4, 1.6, 1.7], coupling: 0,
			initialPhases: [0, 0, 0, 0],
			conductorFreq: 1.5, conductorCoupling: 4.0,
			roleOffsets: roleOffsets, roleMultipliers: [1, 1, 1, 1]);

		500.do { osc.tick(0.01) }; // 5s Einschwingzeit

		osc.phases.do { |p, i|
			var roleAngle = (osc.conductorPhase + roleOffsets[i]) % 2pi;
			var diff = ((p - roleAngle + pi) % 2pi) - pi;

			this.assert(diff.abs < 0.5,
				"Oszillator % stabilisiert sich nahe seiner eigenen Rollen-Zielphase (Abweichung %), nicht bei einer gemeinsamen Unisono-Phase".format(i, diff));
		};
	}
}
