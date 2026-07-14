// Test für GrooveNotation (Intent 62). Ausführen über run_tests.scd. Reine sclang-Logik, kein
// Server nötig.
TestGrooveNotation : UnitTest {

	// ein einzelner Schlag auf Schritt 0 -- roleMultiplier ist die Anzahl Schläge (1),
	// roleOffset 0, weil der Schlag exakt auf den Zyklusanfang fällt.
	test_deriveRoleWithSingleHitAtStepZeroYieldsOffsetZero {
		var role = GrooveNotation.deriveRole("x...");

		this.assertEquals(role.roleMultiplier, 1);
		this.assertFloatEquals(role.roleOffset, 0);
	}

	// ein einzelner Schlag auf Schritt 1 von 4 (Winkel pi/2 im Zyklus) -- damit
	// roleMultiplier * conductorPhase + roleOffset bei conductorPhase = pi/2 auf ein Vielfaches
	// von 2pi trifft, muss roleOffset = -1 * pi/2 (mod 2pi) = 3pi/2 sein (siehe
	// CoupledOscillators>>tick: gefeuert wird, wenn roleAngle über 2pi läuft).
	test_deriveRoleWithSingleHitOffCenterYieldsNegatedScaledOffset {
		var role = GrooveNotation.deriveRole(".x..");

		this.assertEquals(role.roleMultiplier, 1);
		this.assertFloatEquals(role.roleOffset, 3 * pi / 2);
	}

	// zwei exakt gleichmässig verteilte Schläge (Schritt 0 und 4 von 8, wie die Kick-Rolle in
	// cricket_groove.scd) -- roleMultiplier 2, roleOffset 0 (beide Schläge liegen exakt auf
	// Vielfachen von 2pi/roleMultiplier ab Zyklusanfang).
	test_deriveRoleWithTwoEvenlySpacedHitsYieldsOffsetZero {
		var role = GrooveNotation.deriveRole("x...x...");

		this.assertEquals(role.roleMultiplier, 2);
		this.assertFloatEquals(role.roleOffset, 0);
	}

	// vier exakt gleichmässig verteilte, aber versetzte Schläge (Schritt 1/3/5/7 von 8) --
	// roleMultiplier 4, roleOffset konsistent pi für alle vier Schläge (Wert per Handrechnung
	// vorab geprüft: -4 * (schritt * 2pi/8) mod 2pi ergibt für alle vier Schritte denselben Wert).
	test_deriveRoleWithFourEvenlySpacedOffsetHitsYieldsConsistentOffset {
		var role = GrooveNotation.deriveRole(".x.x.x.x");

		this.assertEquals(role.roleMultiplier, 4);
		this.assertFloatEquals(role.roleOffset, pi);
	}

	// unregelmässiges Fill-Pattern (Floor-Tom-Rolle aus cricket_groove.scd, Intent 62): 5 Schläge
	// auf Schritt 3/5/7/13/15 von 16, nicht exakt gleichmässig verteilt. roleOffset ist hier der
	// zirkuläre Mittelwert der einzelnen -roleMultiplier*Winkel-Werte -- Wert per
	// Python-Referenzrechnung vorab geprüft (siehe Intent-62-Planung).
	test_deriveRoleWithIrregularPatternYieldsCircularMeanOffset {
		var role = GrooveNotation.deriveRole("...x.x.x.....x.x");

		this.assertEquals(role.roleMultiplier, 5);
		this.assertFloatEquals(role.roleOffset, pi / 8, 0.001);
	}

	// deriveRoles wendet deriveRole auf mehrere Patterns an, in derselben Reihenfolge --
	// Grundlage für die 5-stimmige Rollen-Ableitung in cricket_groove.scd.
	test_deriveRolesAppliesDeriveRoleToEachPatternInOrder {
		var roles = GrooveNotation.deriveRoles(["x...", ".x.."]);

		this.assertEquals(roles.size, 2);
		this.assertEquals(roles[0].roleMultiplier, 1);
		this.assertFloatEquals(roles[0].roleOffset, 0);
		this.assertEquals(roles[1].roleMultiplier, 1);
		this.assertFloatEquals(roles[1].roleOffset, 3 * pi / 2);
	}
}
