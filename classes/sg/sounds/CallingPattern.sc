// CallingPattern — ein einfaches, festes An/Aus-Zeitmuster für Sounds wie InsectSound
// (Intent 30, Phase 1): eine Serie von [dauer, istAn]-Segmenten, die next zyklisch
// durchläuft und nach dem letzten Segment wieder beim ersten weitermacht. Bewusst ein
// eigenes, minimales next-Interface statt SC-Stream-Protokoll — spätere Phasen können
// stattdessen z.B. Pseq(...).asStream an dieselbe Aufrufstelle übergeben, solange diese
// nur next erwartet (siehe Intent 30, 3.1).
CallingPattern {
	var <segments;
	var <>index;

	*new { |segments|
		^super.new.init(segments);
	}

	// Kurzschreibweise: reine Dauern statt [dauer, istAn]-Paaren — per Konvention beginnt das
	// erste Segment "an", danach wechselt es strikt ab (an/aus/an/aus/...). Deckt den
	// häufigsten Fall ab (siehe Intent 30, Nutzer-Feedback zur Verbosität von *new).
	*fromDurations { |durations|
		^this.new(durations.collect { |dur, i| [dur, i.even] });
	}

	init { |aSegments|
		segments = aSegments;
		index = 0;
	}

	// liefert das aktuelle Segment ([dauer, istAn]) und rückt eine Position weiter,
	// wrappt nach dem letzten Segment zurück auf das erste.
	next {
		var segment = segments[index];
		index = (index + 1) % segments.size;
		^segment
	}
}
