// MoveRule — Basisklasse für Bewegungsregeln. Subklassen implementieren next(),
// um aus aktueller Position, verstrichener Zeit und Zeitschritt die neue
// Position zu berechnen. Movable ruft next() bei jedem step() auf.
MoveRule {
	// next — von Subklassen zu implementieren.
	// pos: aktuelle Position [x, y, z]
	// t:   seit Bewegungsbeginn verstrichene Zeit in Sekunden (kumulativ)
	// dt:  Zeitschritt seit dem letzten Aufruf in Sekunden
	// -> neue Position [x, y, z]
	next { |pos, t, dt|
		^this.subclassResponsibility(thisMethod)
	}
}
