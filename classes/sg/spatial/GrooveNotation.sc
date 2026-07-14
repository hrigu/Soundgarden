// GrooveNotation — leitet CoupledOscillators-Rollenparameter (roleOffset/roleMultiplier) aus
// einer editierbaren, textuellen Groove-Notation ab (Intent 62): pro Stimme ein String aus "x"
// (Schlag) und "." (Pause), Länge = Anzahl Schritte pro Zyklus (z.B. 16 für einen 16tel-Takt).
// Ersetzt die manuelle Handrechnung solcher Werte (siehe cricket_groove.scd) durch eine
// nachvollziehbare, testbare Ableitung.
//
// Herleitung (siehe CoupledOscillators>>tick): ein Oszillator feuert, wenn roleAngle =
// roleMultiplier * conductorPhase + roleOffset über ein Vielfaches von 2pi läuft. Damit ein
// Schlag auf Schritt-Winkel phi liegt, muss roleMultiplier * phi + roleOffset ≡ 0 (mod 2pi)
// gelten, also roleOffset ≡ -roleMultiplier * phi (mod 2pi). roleMultiplier wird als Anzahl der
// Schläge im Pattern gewählt (rationale Unterteilung des Zyklus); roleOffset ist der zirkuläre
// Mittelwert der so skalierten Winkel aller Schläge -- bei exakt gleichmässig verteilten Schlägen
// (wie Kick/Snare in cricket_groove.scd) ergeben alle Schläge denselben Wert, bei unregelmässigen
// Fill-Patterns (wie Floor-/Mid-Tom) liefert der Mittelwert eine sinnvolle Näherung, kein
// Step-Sequencer-Nachbau (siehe Intent-62-Entscheid gegen exakte Step-für-Step-Wiedergabe).
GrooveNotation {

	*deriveRole { |pattern|
		var steps = pattern.size;
		var hitSteps = (0..steps - 1).select { |i| pattern[i] == $x };
		var multiplier = hitSteps.size;
		var scaledAngles = hitSteps.collect { |step|
			(multiplier.neg * (step * 2pi / steps)) % 2pi
		};
		var sumCos = scaledAngles.collect { |a| cos(a) }.sum;
		var sumSin = scaledAngles.collect { |a| sin(a) }.sum;
		var offset = atan2(sumSin, sumCos) % 2pi;

		^(roleMultiplier: multiplier, roleOffset: offset)
	}

	*deriveRoles { |patterns|
		^patterns.collect { |pattern| this.deriveRole(pattern) }
	}
}
