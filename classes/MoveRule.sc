// MoveRule — Basisklasse für Bewegungsregeln. Subklassen implementieren next(),
// um aus aktueller Position, verstrichener Zeit und Zeitschritt die neue
// Position zu berechnen. Movable ruft next() bei jedem step() auf.
MoveRule {
	next { |pos, t, dt|
		^this.subclassResponsibility(thisMethod)
	}
}
