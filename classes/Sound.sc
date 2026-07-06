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
}
