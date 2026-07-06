// Listener — der Hörer. Position aktuell fix, Blickrichtung (facing, in Grad,
// 0 = entlang +y) aktuell fix. Rechnet Weltkoordinaten in "was hört er" um:
//bAzimuth relativ zur eigenen Blickrichtung (0 = vorne, positiv = rechts,
// ±pi = hinten) und Distanz.
Listener {
	var <>pos;     // [x, y, z]
	var <>facing;  // Grad, 0 = entlang +y

	*new { |pos = #[0, 0, 0], facing = 0|
		^super.new.setup(pos, facing);
	}

	setup { |aPos, aFacing|
		pos = aPos;
		facing = aFacing;
	}

	// Azimuth zur anderen Position relativ zur Blickrichtung
	relativeAzimuth { |targetPos|
		var rel = targetPos - pos;
		var worldAngle = atan2(rel[0], rel[1]);
		^(worldAngle - facing.degrad).wrap(pi.neg, pi)
	}

	distanceTo { |targetPos|
		var rel = targetPos - pos;
		^(rel.pow(2).sum).sqrt
	}
}
