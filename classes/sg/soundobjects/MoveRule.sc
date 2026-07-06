// MoveRule — Basisklasse für Bewegungsregeln. Subklassen implementieren next(),
// um aus aktueller Position, verstrichener Zeit und Zeitschritt die neue
// Position zu berechnen. Movable ruft next() bei jedem step() auf.
MoveRule {

	// Diese Methode muss von Subklassen implementiert werden.
	// Berechnet die neue Position nach der eigenen Bewegungsregeln.
	// arg pos: aktuelle Position [x, y, z]
	// arg t:   seit Bewegungsbeginn verstrichene Zeit in Sekunden (kumulativ)
	// arg dt:  Zeitschritt seit dem letzten Aufruf in Sekunden
	// -> neue Position [x, y, z]
	next { |pos, t, dt|
		^this.subclassResponsibility(thisMethod)
	}
}
