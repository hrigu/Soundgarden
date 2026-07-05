// CircularMoveRule — Kreisbahn um den Ursprung, deren Radius langsam "atmet"
// (baseRadius ± breathAmount). Defaults entsprechen der ursprünglichen
// ~circleRule aus insect_demo.scd (Intent 3).
CircularMoveRule : MoveRule {
	var <>baseRadius;     // Radius der Kreisbahn in Metern (Mittelwert)
	var <>breathAmount;   // Schwankung des Radius um baseRadius, in Metern
	var <>breathRate;     // Geschwindigkeit der Radius-Schwankung, in rad/s
	var <>angularSpeed;   // Winkelgeschwindigkeit auf der Kreisbahn, in rad/s

	// erzeugt eine Instanz mit den gegebenen (oder Default-)Parametern
	*new { |baseRadius = 2.5, breathAmount = 1.2, breathRate = 0.15, angularSpeed = 0.6|
		^super.new.init(baseRadius, breathAmount, breathRate, angularSpeed);
	}

	init { |aBaseRadius, aBreathAmount, aBreathRate, aAngularSpeed|
		baseRadius = aBaseRadius;
		breathAmount = aBreathAmount;
		breathRate = aBreathRate;
		angularSpeed = aAngularSpeed;
	}

	// next — siehe MoveRule. Radius atmet sinusförmig um baseRadius, der
	// Winkel wächst linear mit t; pos wird nicht gebraucht (rein zeitgesteuert).
	next { |pos, t, dt|
		var radius = baseRadius + (breathAmount * sin(t * breathRate));
		var angle = t * angularSpeed;
		^[radius * sin(angle), radius * cos(angle), 0]
	}
}
