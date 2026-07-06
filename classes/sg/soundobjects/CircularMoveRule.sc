// CircularMoveRule — Kreisbahn um den Ursprung, deren Radius langsam "atmet"
// (baseRadius ± breathAmount).
CircularMoveRule : MoveRule {
	var <>baseRadius;     // Radius der Kreisbahn in Metern (Mittelwert)
	var <>breathAmount;   // Schwankung des Radius um baseRadius, in Metern
	var <>breathRate;     // Geschwindigkeit der Radius-Schwankung, in rad/s
	var <>angularSpeed;   // Winkelgeschwindigkeit auf der Kreisbahn, in rad/s
	var <>startAngle;     // Winkel bei t=0, in Radiant — verschiedene Instanzen mit
	                      // gleichem angularSpeed starten sonst alle am selben Punkt,
	                      // da next() den Winkel rein aus t berechnet (pos wird ignoriert)

	// erzeugt eine Instanz mit den gegebenen (oder Default-)Parametern
	*new { |baseRadius = 2.5, breathAmount = 1.2, breathRate = 0.15, angularSpeed = 0.6,
			startAngle = 0|
		^super.new.init(baseRadius, breathAmount, breathRate, angularSpeed, startAngle);
	}

	init { |aBaseRadius, aBreathAmount, aBreathRate, aAngularSpeed, aStartAngle|
		baseRadius = aBaseRadius;
		breathAmount = aBreathAmount;
		breathRate = aBreathRate;
		angularSpeed = aAngularSpeed;
		startAngle = aStartAngle;
	}

	// next — siehe MoveRule. Radius atmet sinusförmig um baseRadius, der
	// Winkel wächst linear mit t ab startAngle; pos wird nicht gebraucht
	// (rein zeitgesteuert).
	next { |pos, t, dt|
		var radius = baseRadius + (breathAmount * sin(t * breathRate));
		var angle = (t * angularSpeed) + startAngle;
		^[radius * sin(angle), radius * cos(angle), 0]
	}
}
