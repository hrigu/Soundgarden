// RhythmPatternNotation — interpretiert editierbare Text-Patterns als getaktete Gate-Events.
//
// Notation:
//   x  = Schlag
//   .  = Pause
//   |  = Segmentgrenze innerhalb eines Takts
//   || = Taktgrenze
//
// Jeder Takt dauert gleich lang (barDur). Innerhalb eines Takts sind alle Segmente gleich lang;
// die Zeichen innerhalb eines Segments teilen nur dieses Segment gleichmässig auf. Dadurch kann
// ein Segment feiner oder gröber notiert werden, ohne die Taktlänge zu verändern.
RhythmPatternNotation {

	*splitLogicalBars { |str|
		var bars = List.new;
		var current = List.new;
		var prevWasBar = false;

		str.do { |c|
			if(c == $|) {
				if(prevWasBar) {
					bars.add(current.asArray);
					current = List.new;
					prevWasBar = false;
				} {
					prevWasBar = true;
				};
			} {
				if(prevWasBar) {
					current.add($|);
					prevWasBar = false;
				};
				current.add(c);
			};
		};
		bars.add(current.asArray);
		^bars.asArray
	}

	*splitSegments { |bar|
		var segments = List.new;
		var current = List.new;

		bar.do { |c|
			if(c == $|) {
				segments.add(current.asArray);
				current = List.new;
			} {
				current.add(c);
			};
		};
		if(current.notEmpty or: { segments.isEmpty }) {
			segments.add(current.asArray);
		};
		^segments.asArray
	}

	*events { |str, barDur|
		^this.splitLogicalBars(str).collect { |bar|
			var segments = this.splitSegments(bar);
			var segmentDur = barDur / segments.size.max(1);
			segments.collect { |segment|
				if(segment.isEmpty) {
					[(gate: 0, dur: segmentDur)]
				} {
					var stepDur = segmentDur / segment.size;
					segment.collect { |c| (gate: if(c == $x) { 1 } { 0 }, dur: stepDur) }
				};
			}.flat;
		}.flat
	}

	*eventStream { |str, barDur|
		^Pseq(this.events(str, barDur), inf)
	}
}
