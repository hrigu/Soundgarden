// CircularMoveRule — Kreisbahn um den Ursprung, deren Radius langsam "atmet"
// (baseRadius ± breathAmount). Defaults entsprechen der ursprünglichen
// ~circleRule aus insect_demo.scd (Intent 3).
CircularMoveRule : MoveRule {
	var <>baseRadius;
	var <>breathAmount;
	var <>breathRate;
	var <>angularSpeed;

	*new { |baseRadius = 2.5, breathAmount = 1.2, breathRate = 0.15, angularSpeed = 0.6|
		^super.new.init(baseRadius, breathAmount, breathRate, angularSpeed);
	}

	init { |aBaseRadius, aBreathAmount, aBreathRate, aAngularSpeed|
		baseRadius = aBaseRadius;
		breathAmount = aBreathAmount;
		breathRate = aBreathRate;
		angularSpeed = aAngularSpeed;
	}

	next { |pos, t, dt|
		var radius = baseRadius + (breathAmount * sin(t * breathRate));
		var angle = t * angularSpeed;
		^[radius * sin(angle), radius * cos(angle), 0]
	}
}
