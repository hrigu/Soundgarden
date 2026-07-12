// BirdMotif — eine feste Tonhöhen-/Rhythmus-Sequenz für MessiaenBirdSound (Intent 59): anders
// als CallingPattern (reines An/Aus-Timing ohne Tonhöhe) trägt jede Note ihre eigene Frequenz.
// Eine Note ist [freq, dur, gateRatio]: dur ist der Einsatzabstand zur nächsten Note, gateRatio
// der Anteil von dur, den die Note tatsächlich klingt, bevor die Hüllkurve für die nächste Note
// erneut triggert -- verhindert, dass sehr schnelle Läufe (z.B. wrenTrillCascade in
// BirdMotifExamples) zu einem ununterscheidbaren Legato-Brei verschmelzen.
BirdMotif {
	var <notes; // [[freq, dur, gateRatio], ...]

	*new { |notes|
		^super.new.init(notes);
	}

	// Autoring-Hilfe: Motiv aus Startfrequenz + Halbtonschritten + Dauern beschreiben, statt
	// jede absolute Hz-Zahl von Hand auszurechnen -- deutlich näher an "wie man ein Vogelmotiv
	// tatsächlich denkt" (Intervallfolge) als rohe Frequenzlisten. semitoneSteps ist pro Note
	// die Anzahl Halbtöne relativ zu startFreq (nicht relativ zur Vornote).
	*fromIntervals { |startFreq, semitoneSteps, durs, gateRatio = 0.8|
		var notes = semitoneSteps.collect { |steps, i|
			[startFreq * (2 ** (steps / 12)), durs[i], gateRatio]
		};
		^this.new(notes);
	}

	init { |aNotes|
		notes = aNotes;
	}

	// Summe aller Notendauern -- Gesamtlänge eines einmaligen Durchlaufs.
	totalDur {
		^notes.collect { |note| note[1] }.sum
	}

	// liefert [freq, einsatzzeitpunkt] pro Note -- einsatzzeitpunkt ist die Summe aller
	// vorherigen Notendauern, reine Zeitberechnung ohne Server-/Audio-Bezug.
	noteEvents {
		var onset = 0.0;
		^notes.collect { |note|
			var event = [note[0], onset];
			onset = onset + note[1];
			event
		}
	}
}
