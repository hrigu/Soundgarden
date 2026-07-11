// BirdSound — synthetischer Vogelklang für die Dämmerungs-Komposition (Intent 57): kurze,
// tonhöhengleitende Zwitscher-Bursts statt InsectSounds Flügelschlag-Brummen, daher eine
// eigenständige Sound-Subklasse mit eigener DSP-Struktur (keine InsectSound-Ableitung).
// Übernimmt aber deren pattern/gate-Muster (Intent 30) unverändert: ohne pattern ruft der Vogel
// durchgehend, mit pattern schaltet eine Routine den gate-Control im Rhythmus der Pattern-
// Segmente (Ruf-Phrase/Pause) — siehe InsectSound.sc für dasselbe Muster. Klangdetails
// (Hüllkurvenform, Vibrato) sind ein erster Entwurf, im Hörtest nachjustierbar wie bei
// CricketSound.
BirdSound : Sound {
	var <>chirpRate;    // Zwitscher pro Sekunde innerhalb einer Ruf-Phrase, in Hz
	var <>baseFreq;     // Grundtonhöhe der Zwitscher, in Hz
	var <>glideRange;   // Tonhöhensprung pro Zwitscher (Gleiten nach oben und zurück), in Hz
	var <>chirpDur;     // Dauer eines einzelnen Zwitschers, in Sekunden
	var <>amp;          // Grundlautstärke des Klangs selbst, unabhängig von Position
	var <>pattern;      // optionales CallingPattern — nil = durchgehendes Rufen
	var patternRoutine; // treibt pattern.next während play(), siehe play/stop

	*new { |chirpRate = 6, baseFreq = 3500, glideRange = 800, chirpDur = 0.08, amp = 0.3,
			pattern|
		^super.new.init(chirpRate, baseFreq, glideRange, chirpDur, amp, pattern);
	}

	init { |aChirpRate, aBaseFreq, aGlideRange, aChirpDur, aAmp, aPattern|
		chirpRate = aChirpRate;
		baseFreq = aBaseFreq;
		glideRange = aGlideRange;
		chirpDur = aChirpDur;
		amp = aAmp;
		pattern = aPattern;
	}

	// startet Synth wie Sound>>play, und falls ein pattern gesetzt ist zusätzlich eine Routine,
	// die den gate-Control im Rhythmus der Pattern-Segmente umschaltet — identisches Muster zu
	// InsectSound>>play (Intent 30).
	play { |server|
		super.play(server);
		if(pattern.notNil) {
			patternRoutine = Routine({
				loop {
					var segment = pattern.next;
					synth.set(\gate, segment[1].asInteger);
					segment[0].wait;
				}
			}).play;
		};
		^this
	}

	stop {
		patternRoutine !? { patternRoutine.stop };
		super.stop;
	}

	// registriert die \birdSound-SynthDef beim Server. Ein Impulse.kr triggert pro Zwitscher
	// eine kurze Perc-Hüllkurve UND eine Tonhöhengleit-Hüllkurve (schnell hoch, wieder zurück),
	// ein leichtes Vibrato sorgt für den charakteristisch "lebendigen" Ton statt eines reinen
	// Sinus-Pieps. Asynchron (/d_recv) — vor play() mit etwas zeitlichem Abstand aufrufen.
	*addSynthDef {
		SynthDef(\birdSound, { |out = 0, chirpRate = 6, baseFreq = 3500, glideRange = 800,
				chirpDur = 0.08, amp = 0.3, t_call = 0, gate = 1|
			var trig = Impulse.kr(chirpRate);
			var chirpEnv = EnvGen.kr(Env.perc(0.005, chirpDur), trig);
			var pitchGlide = EnvGen.kr(
				Env([0, 1, 0], [chirpDur * 0.4, chirpDur * 0.6], \sin), trig);
			var vibrato = SinOsc.kr(18, 0, 20);
			var freq = baseFreq + (pitchGlide * glideRange) + vibrato;
			// call-Akzent: kurzer, perkussiver Pegel-Aufblitz, ausgelöst durch Sound>>call
			// (t_call-Trigger) — gleiches Muster wie InsectSound.
			var callEnv = EnvGen.kr(Env.perc(0.005, 0.35), t_call);
			// amp geglättet (Lag.kr) fürs klickfreie Live-Tunen im GUI UND fürs klickfreie
			// Verstummen beim Sonnenaufgang-Cue der Dämmerungs-Komposition (Intent 57, gleiches
			// Muster wie InsectSound/SampleSound, Intent 43).
			var ampCtrl = Lag.kr(amp, 0.15);
			var sig = SinOsc.ar(freq) * chirpEnv;
			// gate schaltet das CallingPattern (Intent 30) An/Aus, kurze Lag-Zeit wie bei
			// InsectSound, damit auch kurze Ruf-Phrasen hörbar bleiben.
			Out.ar(out, sig * ampCtrl * (1 + (callEnv * 1.5)) * Lag.kr(gate, 0.05));
		}).add;
	}

	// editableParams — live im GUI bearbeitbare Klangparameter (RoomControlPanel, Intent 43).
	*editableParams {
		^[
			[\chirpRate, ControlSpec(1, 15, \lin)],
			[\baseFreq, ControlSpec(1500, 7000, \exp)],
			[\glideRange, ControlSpec(0, 2000, \lin)],
			[\chirpDur, ControlSpec(0.02, 0.2, \lin)],
			[\amp, ControlSpec(0, 1, \lin)]
		]
	}

	makeSynth { |server, bus|
		^Synth(\birdSound, [
			\out, bus.index,
			\chirpRate, chirpRate,
			\baseFreq, baseFreq,
			\glideRange, glideRange,
			\chirpDur, chirpDur,
			\amp, amp
		], server);
	}
}
