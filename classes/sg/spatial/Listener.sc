// Listener — der Hörer. Position und Blickrichtung (facing, in Grad, 0 = entlang +y) können
// sich bewegen/drehen (siehe moveForward/strafeLeft/rotate etc., z.B. von
// KeyboardListenerControl genutzt). Rechnet Weltkoordinaten in "was hört er" um: Azimuth
// relativ zur eigenen Blickrichtung (0 = vorne, positiv = rechts, ±pi = hinten) und Distanz.
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

	// Vorwärtsvektor der aktuellen Blickrichtung (Einheitsvektor, xy-Ebene).
	// Bei facing=0 zeigt er entlang +y, bei facing=90 entlang +x — konsistent
	// mit der "positiv = rechts"-Konvention aus relativeAzimuth.
	forwardVector {
		^[sin(facing.degrad), cos(facing.degrad), 0]
	}

	// Rechtsvektor der aktuellen Blickrichtung — 90° im Uhrzeigersinn von forwardVector.
	rightVector {
		^[cos(facing.degrad), sin(facing.degrad).neg, 0]
	}

	// moveForward/moveBackward — Bewegung entlang der Blickrichtung (Distanz d in Metern).
	moveForward { |d|
		pos = pos + (this.forwardVector * d);
		^pos
	}

	moveBackward { |d|
		^this.moveForward(d.neg)
	}

	// strafeLeft/strafeRight — seitliche Bewegung, senkrecht zur Blickrichtung.
	strafeRight { |d|
		pos = pos + (this.rightVector * d);
		^pos
	}

	strafeLeft { |d|
		^this.strafeRight(d.neg)
	}

	// rotate — ändert die Blickrichtung um deltaDegrees (positiv = im Uhrzeigersinn,
	// konsistent mit der "positiv = rechts"-Konvention).
	rotate { |deltaDegrees|
		facing = facing + deltaDegrees;
		^facing
	}
}
