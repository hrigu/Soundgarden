// Test für CircularMoveRule. Ausführen über run_tests.scd.
TestCircularMoveRule : UnitTest {

	test_startsAtBaseRadiusOnPositiveYAxis {
		var rule = CircularMoveRule.new(2.5, 1.2, 0.15, 0.6);
		var pos = rule.next(#[0, 0, 0], 0, 0.033);

		this.assertEquals(pos[0].round(0.0001), 0.0, "bei t=0 kein Winkel, also x = 0");
		this.assertEquals(pos[1].round(0.0001), 2.5, "bei t=0 keine Atmung, also y = baseRadius");
		this.assertEquals(pos[2], 0.0, "Bewegung bleibt in der xy-Ebene (z = 0)");
	}

	test_radiusBreathesWithTime {
		var rule = CircularMoveRule.new(2.5, 1.2, 0.15, 0.6);
		var pos = rule.next(#[0, 0, 0], (0.5pi) / 0.15, 0.033);
		var dist = (pos.pow(2).sum).sqrt;

		this.assertEquals(dist.round(0.0001), 3.7, "bei sin(t * breathRate) = 1 ist der Radius baseRadius + breathAmount");
	}

	test_angleAdvancesWithAngularSpeed {
		var rule = CircularMoveRule.new(2.5, 0, 0.15, 0.6);
		var pos = rule.next(#[0, 0, 0], 0.5pi / 0.6, 0.033);

		this.assertEquals(pos[0].round(0.0001), 2.5, "bei angle = pi/2 liegt die Position auf der x-Achse");
		this.assertEquals(pos[1].round(0.0001), 0.0, "bei angle = pi/2 ist die y-Komponente 0");
	}
}
