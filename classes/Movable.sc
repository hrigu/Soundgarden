// Movable — irgendetwas, das eine Position im Raum hat und sich nach einer
// Bewegungsregel verändert. roomRadius definiert eine Kugel um den Ursprung;
// wird sie überschritten, steuert step() sanft zurück (kein hartes Abprallen).
Movable {
	var <>pos;        // [x, y, z]
	var <>moveRule;   // MoveRule-Objekt: next(pos, t, dt) -> neue [x, y, z]
	var <>roomRadius;
	var <>time;

	// erzeugt eine Instanz an gegebener Startposition mit gegebener MoveRule;
	// time startet bei 0
	*new { |pos = #[0, 0, 0], moveRule, roomRadius = 5|
		^super.new.init(pos, moveRule, roomRadius);
	}

	init { |aPos, aMoveRule, aRadius|
		pos = aPos;
		moveRule = aMoveRule;
		roomRadius = aRadius;
		time = 0;
	}

	// step — bewegt das Objekt einen Zeitschritt weiter.
	// dt: Zeitschritt in Sekunden seit dem letzten step()-Aufruf
	// -> neue Position [x, y, z]; mutiert dabei auch pos und time (kumulative
	//    Gesamtzeit) als Seiteneffekt
	step { |dt = 0.033|
		// pullback: Anteil des Wegs zurück zur Kugeloberfläche pro step()
		// (0 = kein Zurücksteuern, 1 = hartes Clamping auf roomRadius).
		// Klein gehalten, damit die Rückführung sanft statt als Sprung/Klick
		// hörbar wird.
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
