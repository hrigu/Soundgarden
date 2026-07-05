// Movable — irgendetwas, das eine Position im Raum hat und sich nach einer
// Bewegungsregel verändert. roomRadius definiert eine Kugel um den Ursprung;
// wird sie überschritten, steuert step() sanft zurück (kein hartes Abprallen).
Movable {
	var <>pos;        // [x, y, z]
	var <>moveRule;   // MoveRule-Objekt: next(pos, t, dt) -> neue [x, y, z]
	var <>roomRadius;
	var <>time;

	*new { |pos = #[0, 0, 0], moveRule, roomRadius = 5|
		^super.new.init(pos, moveRule, roomRadius);
	}

	init { |aPos, aMoveRule, aRadius|
		pos = aPos;
		moveRule = aMoveRule;
		roomRadius = aRadius;
		time = 0;
	}

	step { |dt = 0.033|
		var newPos, dist, targetScale, scale, pullback = 0.2;

		time = time + dt;
		newPos = moveRule.next(pos, time, dt);

		dist = (newPos.pow(2).sum).sqrt;
		if(dist > roomRadius) {
			targetScale = roomRadius / dist;
			scale = 1 + ((targetScale - 1) * pullback);
			newPos = newPos * scale;
		};

		pos = newPos;
		^pos
	}
}
