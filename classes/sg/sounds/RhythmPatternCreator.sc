// RhythmPatternCreator — erzeugt CallingPattern-Instanzen mit konfigurierbarem Verhältnis von
// Sound- zu Stillezeit (Intent 33). Anders als CallingPattern.fromDurations (fixe 50/50-artige
// Zufallsgewichtung, immer "An" zuerst) lässt sich hier sowohl der Sound/Stille-Anteil
// (soundRatio) als auch die Startphase (startWithSound) steuern. Jeder createNewPattern-Aufruf
// erzeugt eine neue, unabhängige Zufallsgewichtung und CallingPattern-Instanz (kein geteilter
// Zustand zwischen Aufrufen, analog zum bisherigen ~makeRhythmPattern in insects.scd).
RhythmPatternCreator {
	var <totalDur, <numSegments, <soundRatio, <startWithSound;

	*new { |totalDur, numSegments, soundRatio = 0.5, startWithSound = true|
		^super.new.init(totalDur, numSegments, soundRatio, startWithSound);
	}

	init { |aTotalDur, aNumSegments, aSoundRatio, aStartWithSound|
		totalDur = aTotalDur;
		numSegments = aNumSegments;
		soundRatio = aSoundRatio;
		startWithSound = aStartWithSound;
	}

	// weist jedem Segment-Index einen zufälligen Rohgewicht zu, teilt sie nach ihrer Rolle
	// (An/Aus, abhängig von startWithSound) in zwei Gruppen und skaliert jede Gruppe so, dass
	// ihre Summe genau soundRatio bzw. (1 - soundRatio) von totalDur ergibt.
	createNewPattern {
		var weights = Array.fill(numSegments, { rrand(0.01, 2.0) });
		var isOn = numSegments.collect { |i| i.even == startWithSound };
		var onWeights = (0..numSegments - 1).select { |i| isOn[i] }.collect { |i| weights[i] };
		var offWeights = (0..numSegments - 1).select { |i| isOn[i].not }.collect { |i| weights[i] };
		var onScale = (soundRatio * totalDur) / onWeights.sum;
		var offScale = ((1 - soundRatio) * totalDur) / offWeights.sum;
		var segments = numSegments.collect { |i|
			var scale = if(isOn[i], onScale, offScale);
			[weights[i] * scale, isOn[i]]
		};

		^CallingPattern.new(segments);
	}
}
