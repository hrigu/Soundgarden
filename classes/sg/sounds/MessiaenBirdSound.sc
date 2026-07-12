// MessiaenBirdSound — spielt ein BirdMotif (Tonhöhen-/Rhythmus-Sequenz) statt eines reinen
// An/Aus-Rufs ab (Intent 59, Messiaen-inspirierte Vogel-Komposition). Anders als BirdSound
// (feste baseFreq, reines pattern-gate) oder SampleSound (Sample-Wiedergabe): eine Routine
// treibt freq/noteDur/t_trig auf einem einzigen, durchgehend laufenden Synth -- eigenständige
// DSP-Struktur statt Ableitung, weil weder BirdSound noch SampleSound eine Tonhöhenfolge
// abbilden können (siehe deren Klassen-Kommentare).
MessiaenBirdSound : Sound {
	var <motif;       // BirdMotif, Pflichtargument -- siehe requiredConstructorArgs
	var <>amp;        // Grundlautstärke des Klangs selbst, unabhängig von Position
	var <>brightness; // 0..1, Anteil des perkussiven Rausch-"Chiff" am Attack (siehe addSynthDef)
	var <>loop;       // false = Motiv einmal spielen, true = endlos wiederholen
	var motifRoutine; // treibt die Noten des Motivs während play(), siehe play/stop

	*new { |motif, amp = 0.35, brightness = 0.5, loop = false|
		^super.new.init(motif, amp, brightness, loop);
	}

	init { |aMotif, aAmp, aBrightness, aLoop|
		motif = aMotif;
		amp = aAmp;
		brightness = aBrightness;
		loop = aLoop;
	}

	// startet Synth wie Sound>>play, zusätzlich eine Routine, die pro Note des Motivs freq/
	// noteDur auf den laufenden Synth setzt und t_trig feuert -- gleiches Grundmuster wie
	// BirdSound>>play/patternRoutine, nur mit variabler Tonhöhe statt reinem gate-Umschalten.
	play { |server|
		super.play(server);
		motifRoutine = Routine({
			block { |break|
				loop {
					motif.notes.do { |note|
						synth.set(\freq, note[0], \noteDur, note[1], \t_trig, 1);
						(note[1] * (note[2] ? 0.8)).wait;
						(note[1] * (1 - (note[2] ? 0.8))).wait;
					};
					if(loop.not) { break.value(nil) };
				};
			};
		}).play;
		^this
	}

	stop {
		motifRoutine !? { motifRoutine.stop };
		super.stop;
	}

	// registriert die \messiaenBird-SynthDef beim Server. Ein t_trig-Trigger pro Note löst eine
	// kurze perkussive Hüllkurve aus, Grundton + leiser 2. Harmonischer plus ein kurzer,
	// gefilterter Rausch-"Chiff" am Attack (brightness-gesteuert) sorgen für ein perkussives,
	// holzblas-/klavierartiges Timbre statt eines reinen Sinustons -- passend zu schnellen,
	// klar unterscheidbaren Tonfolgen (siehe BirdMotifExamples). Asynchron (/d_recv) -- vor
	// play() mit etwas zeitlichem Abstand aufrufen, wie BirdSound/InsectSound.
	*addSynthDef {
		SynthDef(\messiaenBird, { |out = 0, freq = 440, noteDur = 0.1, amp = 0.35,
				brightness = 0.5, t_trig = 0, gate = 1|
			var env = EnvGen.kr(Env.perc(0.003, noteDur * 0.7, curve: -4), t_trig);
			var tone = SinOsc.ar(freq) + (0.25 * SinOsc.ar(freq * 2));
			var chiff = Resonz.ar(WhiteNoise.ar(1), freq * 3, 0.05)
				* EnvGen.kr(Env.perc(0.001, 0.02), t_trig);
			var sig = (tone * (1 - (brightness * 0.4))) + (chiff * brightness);
			// amp/gate geglättet (Lag.kr) fürs klickfreie Live-Tunen im GUI UND fürs klickfreie
			// Verstummen (Sound-Konvention, siehe BirdSound/InsectSound).
			var ampCtrl = Lag.kr(amp, 0.15);
			var gateCtrl = Lag.kr(gate, 0.05);
			Out.ar(out, sig * env * ampCtrl * gateCtrl);
		}).add;
	}

	// editableParams — live im GUI bearbeitbare Klangparameter (SpatialControlPanel). motif
	// selbst ist kein Slider-Parameter -- siehe requiredConstructorArgs.
	*editableParams {
		^[
			[\amp, ControlSpec(0, 1, \lin)],
			[\brightness, ControlSpec(0, 1, \lin)]
		]
	}

	// requiredConstructorArgs — siehe Sound. motif ist kein editierbarer Slider-Parameter
	// (analog path bei SampleSound), aber Pflicht für *new.
	*requiredConstructorArgs {
		^[\motif]
	}

	makeSynth { |server, bus|
		^Synth(\messiaenBird, [
			\out, bus.index,
			\amp, amp,
			\brightness, brightness
		], server);
	}
}
