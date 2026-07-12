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
}
