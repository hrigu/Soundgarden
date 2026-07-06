// SteadyMoveRule — Bewegungsregel für stationäre Soundobjekte. Bleibt von selbst exakt an der
// aktuellen Position, ignoriert t und dt. Für Live-Coding-Umplatzierung siehe Movable>>moveTo,
// das die Position direkt setzt statt sich auf eine MoveRule zu verlassen.
SteadyMoveRule : MoveRule {
	next { |pos, t, dt|
		^pos
	}
}
