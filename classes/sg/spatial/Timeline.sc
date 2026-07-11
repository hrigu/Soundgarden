// Timeline — allgemeiner Mechanismus, um Klang über die Zeit zu verändern (Intent 57).
// Zwei Grundbausteine: at (einmaliger Cue zu einem Zeitpunkt, z.B. "ein neuer Vogel setzt
// ein") und ramp (kontinuierlicher, linear interpolierter Übergang über einen Zeitraum, z.B.
// ein Master-Fade). Architektur bewusst analog zu Orchestra (classes/sg/spatial/Orchestra.sc):
// eine einzige zentrale Routine statt vieler Einzel-Routinen, reine tick(dt)-Logik ohne
// Server-Bezug, damit sie ohne Server/Audio testbar bleibt (siehe TestTimeline.sc) — play()
// ist nur ein dünner Wrapper, der tick in echter Zeit aufruft.
Timeline {
	var <totalDur;
	var cues;       // [[time, action], ...]
	var ramps;      // [[startTime, endTime, fromValue, toValue, setter], ...]
	var <elapsed;
	var firedCues;  // Set bereits ausgelöster Cues (Objekt-Identität der [time, action]-Paare)
	var routine;

	*new { |totalDur| ^super.new.init(totalDur) }

	init { |aTotalDur|
		totalDur = aTotalDur;
		cues = [];
		ramps = [];
		elapsed = 0;
		firedCues = Set.new;
	}

	// registriert einen einmaligen Cue: action feuert genau einmal, sobald elapsed >= time.
	at { |time, action|
		cues = cues.add([time, action]);
	}

	// registriert einen kontinuierlichen Übergang: setter.(currentValue) wird bei jedem tick
	// aufgerufen, solange elapsed zwischen startTime und endTime liegt, linear interpoliert.
	// Vor startTime passiert nichts, nach endTime bleibt der Wert bei toValue (kein Extrapolieren
	// über den definierten Bereich hinaus).
	ramp { |startTime, endTime, fromValue, toValue, setter|
		ramps = ramps.add([startTime, endTime, fromValue, toValue, setter]);
	}

	// reine Logik, ohne Server-Bezug — testbar wie Orchestra>>tick.
	tick { |dt|
		elapsed = elapsed + dt;

		cues.do { |cue|
			if((elapsed >= cue[0]) and: { firedCues.includes(cue).not }) {
				firedCues.add(cue);
				cue[1].value;
			};
		};

		ramps.do { |r|
			var startTime = r[0], endTime = r[1], fromValue = r[2], toValue = r[3], setter = r[4];

			if(elapsed >= startTime) {
				var frac = ((elapsed - startTime) / (endTime - startTime)).clip(0, 1);
				setter.value(fromValue + ((toValue - fromValue) * frac));
			};
		};
	}

	isDone {
		^elapsed >= totalDur
	}

	// startet eine zentrale Routine, die tick in echter Zeit aufruft, bis totalDur erreicht ist.
	play { |updateRate = 10|
		var dt = 1.0 / updateRate;

		routine = Routine({
			// block/break statt "^nil": ein Non-Local-Return würde versuchen, aus dieser Methode
			// (Timeline:play) zurückzukehren -- deren Frame ist aber längst weg, da play direkt
			// nach dem Routine-Start mit ^this zurückkehrt. Erreicht die Routine (asynchron, bei
			// einem späteren dt.wait-Resume) dann isDone, schlägt das mit "Out of context return"
			// fehl. block/break bricht stattdessen nur aus der Routine-Funktion selbst aus.
			block { |break|
				loop {
					this.tick(dt);
					if(this.isDone) { break.value(nil) };
					dt.wait;
				}
			}
		}).play;

		^this
	}

	stop {
		routine !? { routine.stop };
	}
}
