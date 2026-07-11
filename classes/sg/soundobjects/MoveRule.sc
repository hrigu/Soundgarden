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

	// editableParams — von Subklassen zu überschreiben: Liste von [key, ControlSpec] für alle
	// serialisierbaren Bewegungsparameter (Intent 46, analog zu Sound>>editableParams — dient
	// hier der Szenen-/Objekt-Preset-Serialisierung, noch nicht dem Live-GUI-Editieren). Leer
	// = keine Parameter, generischer Default für parameterlose Regeln wie SteadyMoveRule.
	*editableParams {
		^[]
	}

	// setParam — aktualisiert einen Bewegungsparameter per <>-Setter (z.B. beim Anwenden eines
	// SoundObject-Presets, Intent 46). Anders als Sound>>setParam kein Synth-Bezug — eine
	// MoveRule hat keinen laufenden Server-Zustand, der live nachgezogen werden müsste.
	setParam { |key, value|
		this.perform((key ++ "_").asSymbol, value);
	}
}
