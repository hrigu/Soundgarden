// insect_demo.scd — Prototyp: zwei binaurale "Insekten" kreisen um den Hörer, dazu ein
// stationäres ("steady") Soundobjekt, dessen Position live per Code verändert werden kann,
// und ein per Tastatur beweglicher Listener.
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) load_classes.scd — bindet Movable/Listener/CircularMoveRule/SteadyMoveRule/InsectSound/
//                          Binauralizer/SoundObject/Orchestra/KeyboardListenerControl in den
//                          Klassenpfad ein
//   2) boot.scd        — Server muss laufen
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning) funktionieren nicht über
// Lautsprecher richtig.
//
// Hinweis zur Räumlichkeit: Das ist die "einfache" Variante (Core-UGens, kein
// echtes HRTF) — Panning + winzige Laufzeitdifferenz zwischen den Ohren (ITD) +
// entfernungsabhängiger Pegel/Tiefpass, siehe Binauralizer. Vorne/hinten bleibt
// dadurch etwas mehrdeutig (das bekannte "Cone of Confusion"-Problem echter
// Ohren lässt sich ohne Kunstkopf-Impulsantworten nicht ganz auflösen) — ein
// leichtes zusätzliches Dämpfen "hinter" dem Hörer gibt zumindest einen groben
// Anhaltspunkt.

// --- 1) Setup & Start ---
// SynthDefs registrieren, alle Soundobjekte anlegen, Orchester und Tastatursteuerung
// starten — alles in einem Block. Umschlossen von fork{}, damit s.sync nach der
// (asynchronen) SynthDef-Registrierung innerhalb des Blocks korrekt wartet, statt den
// Hauptthread zu blockieren (gleiches Muster wie s.waitForBoot({...}) in boot.scd, das
// intern ebenfalls forkt).
(
fork {
	// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen, vor jeder
	// anderen Anweisung (SuperCollider-Syntaxregel) — deshalb hier gesammelt,
	// auch wenn sie erst weiter unten mit Werten befüllt werden.
	var moveRule1, movable1, insectSound1, binauralizer1;
	var moveRule2, movable2, insectSound2, binauralizer2;
	var insect1, insect2;
	var fountainMoveRule, fountainSound, fountainBinauralizer, fountain;

	InsectSound.addSynthDef;
	Binauralizer.addSynthDef;
	s.sync;

	// Ein gemeinsamer Listener (Position/Blickrichtung per Tastatur beweglich,
	// siehe unten) hört alle Soundobjekte. Jedes Soundobjekt braucht aber sein
	// eigenes Movable/Sound/Binauralizer — die Binauralisierung hängt von der
	// Position ab, die pro Objekt verschieden ist, und jeder Synth braucht
	// seinen eigenen Bus/State. Ein gemeinsames Orchestra hält den Listener und
	// tickt alle Objekte über eine einzige Routine statt je einer eigenen — die
	// Soundobjekte selbst kennen den Listener nicht.
	~listener = Listener.new; // Environment-Variable statt var: Tastatursteuerung
	                          // unten braucht eine Referenz darauf über den Block hinaus.

	// Insekt 1: CircularMoveRule-Defaults (2.5m Radius, ±1.2m Atmung, 0.6 rad/s
	// im Uhrzeigersinn), Standard-Klang.
	moveRule1 = CircularMoveRule.new;
	movable1 = Movable.new([0, 2.5, 0], moveRule1, 5);
	insectSound1 = InsectSound.new;
	binauralizer1 = Binauralizer.new;

	// Insekt 2: etwas schneller (1.0 statt 0.6 rad/s) und in Gegenrichtung
	// (negatives angularSpeed); heller/schneller Klang (höhere Flügelschlag- und
	// Resonanzfrequenzen, kürzeres Ausklingen) statt Standard-Brummen.
	moveRule2 = CircularMoveRule.new(3.0, 1.0, 0.15, -1.0);
	movable2 = Movable.new([0, -3.0, 0], moveRule2, 5);
	insectSound2 = InsectSound.new(280, 0.2, 5200, 7000, 0.015, 0.01, 0.3);
	binauralizer2 = Binauralizer.new;

	insect1 = SoundObject.new(movable1, insectSound1, binauralizer1);
	insect2 = SoundObject.new(movable2, insectSound2, binauralizer2);

	// "Brunnen": steady Soundobjekt — SteadyMoveRule bewegt sich nicht von selbst
	// (next() gibt die Position unverändert zurück), tiefer/langsamer Klang zur
	// Unterscheidung von den Insekten. Position per Live-Coding änderbar, siehe
	// Block 2 weiter unten (~fountainMovable.moveTo(...)).
	fountainMoveRule = SteadyMoveRule.new;
	~fountainMovable = Movable.new([2, 1, 0], fountainMoveRule, 5);
	fountainSound = InsectSound.new(60, 0.6, 800, 1400, 0.08, 0.06, 0.25);
	fountainBinauralizer = Binauralizer.new;
	fountain = SoundObject.new(~fountainMovable, fountainSound, fountainBinauralizer);

	~orchestra = Orchestra.new(~listener);
	~orchestra.register(insect1);
	~orchestra.register(insect2);
	~orchestra.register(fountain);

	// orchestra.play startet den Klang-/Binauralizer-Synth für jedes registrierte
	// Soundobjekt und danach den gemeinsamen Taktgeber (Bewegung + Azimuth/Distanz).
	~orchestra.play(s);

	// Tastatursteuerung für den Listener: öffnet ein kleines Fenster, das während
	// der Steuerung fokussiert sein muss (SuperCollider-Standardidiom für
	// Tastatur-Input). Tasten halten: W/S vor/zurück, A/D seitlich, Q/E drehen.
	~listenerControl = KeyboardListenerControl.new(~listener);
	~listenerControl.play;
};
)

// --- 2) Live-Coding: Brunnen umplatzieren ---
// Position jederzeit während des Sets änderbar — moveTo setzt sie direkt,
// ohne die roomRadius-Rückführung, die für die Insekten-Eigenbewegung gedacht ist.
~fountainMovable.moveTo([-3, 2, 0]);

// --- 3) Stoppen ---
// Ein Block: Orchester stoppen (Ticken + alle registrierten Soundobjekte) und
// Tastatursteuerung stoppen (schließt das Kontrollfenster).
(
~orchestra.stop;
~listenerControl.stop;
)
