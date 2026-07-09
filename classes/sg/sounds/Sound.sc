// Sound — Basisklasse für alle konkreten Klangerzeuger (z.B. InsectSound, künftig
// BirdSound, FountainSound, ...). Kapselt die gemeinsame Bus-/Synth-Lifecycle-Logik:
// play() legt einen privaten Mono-Bus an und delegiert die eigentliche Klangerzeugung
// an makeSynth() (von Subklassen zu implementieren); stop() gibt beides wieder frei.
// Kennt wie ihre Subklassen weder Position noch Pan/Lautstärke im Raum — das ist
// Aufgabe von Binauralizer.
Sound {
	var <bus;    // privater Mono-Bus, auf den der Klang geschrieben wird
	var <synth;  // laufender Synth, sobald play() aufgerufen wurde

	// legt einen privaten Mono-Bus an und lässt die Subklasse per makeSynth() den
	// eigentlichen Synth darauf starten. Setzt wie bisher voraus, dass die jeweilige
	// SynthDef vorher (mit etwas zeitlichem Abstand) registriert wurde.
	play { |server|
		bus = Bus.audio(server, 1); // 1: Anzahl Kanäle (hier Mono)
		synth = this.makeSynth(server, bus);
		^this
	}

	// von Subklassen zu implementieren: erzeugt und liefert den Synth, der auf
	// bus.index schreibt.
	makeSynth { |server, bus|
		^this.subclassResponsibility(thisMethod)
	}

	// gibt Synth und Bus wieder frei — Sound besitzt den Bus, ist also auch fürs
	// Aufräumen zuständig
	stop {
		synth.free;
		bus.free;
	}

	// call — löst einen kurzen, hörbaren Akzent auf dem laufenden Klang aus (z.B. für
	// Call-and-Response zwischen Soundobjekten, siehe Orchestra>>call). Generischer
	// Hook: setzt einen t_call-Trigger auf dem Synth. Subklassen, deren SynthDef
	// keinen t_call-Control besitzt, ignorieren das — .set auf einen nicht
	// existierenden Control-Namen ist ein stiller No-Op.
	call {
		synth.set(\t_call, 1);
	}

	// setParam — aktualisiert einen Klangparameter sowohl auf der Instanz (per <>-Setter,
	// z.B. für spätere Preset-Speicherung oder einen künftigen Neustart des Synths) als auch
	// live auf dem laufenden Synth, falls einer läuft. Funktioniert für jede Sound-Subklasse
	// einheitlich, weil deren Parameter 1:1 auf SynthDef-Controls gleichen Namens abbilden
	// (anders als bei Room, wo abgeleitete Werte eigene xyz_-Overrides brauchten, siehe
	// Room.sc) — kein Bedarf für individuelle Overrides pro Sound-Subklasse (Intent 43).
	setParam { |key, value|
		this.perform((key ++ "_").asSymbol, value);
		synth !? { synth.set(key, value) };
	}

	// editableParams — von Subklassen zu überschreiben: Liste von [key, ControlSpec] für alle
	// im GUI live bearbeitbaren Parameter (siehe SpatialControlPanel). Leer = keine
	// bearbeitbaren Parameter, generischer Default für Sound-Subklassen ohne GUI-Anbindung.
	*editableParams {
		^[]
	}
}
