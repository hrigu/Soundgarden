// InsectSound — der reine Klang des Insekts (Flügelschlag + Brummen). Kennt
// weder Position noch Pan/Lautstärke im Raum — das ist Aufgabe von
// Binauralizer. Bus-/Synth-Lifecycle (privater Mono-Bus statt direkt auf den
// Hardware-Output) kommt von Sound; hier nur die eigentliche Klangerzeugung
// (makeSynth). Klangparameter mit Defaults aus dem ursprünglichen
// \insectVoice-Prototyp (Intent 3), aber pro Instanz überschreibbar.
// Unterstützt Sound>>call (t_call-Trigger): kurzzeitiger Aufblitz von Tonhöhe
// und Pegel, für Call-and-Response zwischen Soundobjekten (Intent 17).
// Optional ein CallingPattern (Intent 30): ohne pattern klingt der Synth wie
// bisher durchgehend; mit pattern schaltet eine Routine den gate-Control des
// Synths im Rhythmus der Pattern-Segmente an/aus (siehe play/stop, addSynthDef).
InsectSound : Sound {
	var <>wingRate;    // Flügelschlag-Rate in Hz (Puls-Gate für das Rauschen)
	var <>wingDuty;    // Tastverhältnis des Flügelschlag-Gates, 0..1
	var <>ringFreq1;   // erste Resonanzfrequenz des Brummens, in Hz
	var <>ringFreq2;   // zweite Resonanzfrequenz des Brummens, in Hz
	var <>ringDecay1;  // Ausklingzeit der ersten Resonanz, in Sekunden
	var <>ringDecay2;  // Ausklingzeit der zweiten Resonanz, in Sekunden
	var <>amp;         // Grundlautstärke des Klangs selbst, unabhängig von Position
	var <>pattern;     // optionales CallingPattern — nil = durchgehendes Zirpen wie bisher
	var patternRoutine; // treibt pattern.next während play(), siehe play/stop

	// erzeugt eine Instanz mit den gegebenen (oder Default-)Klangparametern
	*new { |wingRate = 210, wingDuty = 0.25, ringFreq1 = 3200, ringFreq2 = 4600,
			ringDecay1 = 0.02, ringDecay2 = 0.015, amp = 0.35, pattern|
		^super.new.init(wingRate, wingDuty, ringFreq1, ringFreq2, ringDecay1, ringDecay2, amp,
			pattern);
	}

	init { |aWingRate, aWingDuty, aRingFreq1, aRingFreq2, aRingDecay1, aRingDecay2, aAmp,
			aPattern|
		wingRate = aWingRate;
		wingDuty = aWingDuty;
		ringFreq1 = aRingFreq1;
		ringFreq2 = aRingFreq2;
		ringDecay1 = aRingDecay1;
		ringDecay2 = aRingDecay2;
		amp = aAmp;
		pattern = aPattern;
	}

	// startet Synth wie Sound>>play, und falls ein pattern gesetzt ist zusätzlich eine
	// Routine, die den gate-Control im Rhythmus der Pattern-Segmente umschaltet (an/aus,
	// siehe addSynthDef fürs klickfreie Lag.kr auf gate).
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

	// Routine vor dem Synth-Free stoppen (sonst .set auf einen bereits freigegebenen
	// Synth — harmlos, aber unnötig), dann wie Sound>>stop Synth+Bus freigeben.
	stop {
		patternRoutine !? { patternRoutine.stop };
		super.stop;
	}

	// registriert die \insectSound-SynthDef beim Server. Flügelschlag-Gate
	// (LFPulse) moduliert Rauschen, zwei Ringz erzeugen daraus den Brummton;
	// Ausgabe ist mono, ohne jeden Raumbezug. Asynchron (/d_recv) — vor play()
	// mit etwas zeitlichem Abstand aufrufen.
	//
	// Die Zahlen-Defaults hier dupliziert *new (bewusst, kein Versehen): play()
	// übergibt immer alle Instanzvariablen explizit als Synth-Args, die
	// SynthDef-eigenen Defaults greifen also nur, wenn man \insectSound direkt
	// am Server ohne die Klasse spielt (z.B. Synth(\insectSound) zum schnellen
	// Antesten in der IDE). SynthDef-Argument-Defaults müssen literale
	// Konstanten sein — sie können nicht auf *new verweisen, daher zwei Stellen.
	*addSynthDef {
		SynthDef(\insectSound, { |out = 0, wingRate = 210, wingDuty = 0.25,
				ringFreq1 = 3200, ringFreq2 = 4600, ringDecay1 = 0.02, ringDecay2 = 0.015,
				amp = 0.35, t_call = 0, gate = 1|
			// call-Akzent: kurzer, perkussiver Aufblitz von Tonhöhe (+30%) und
			// Pegel (+150%), ausgelöst durch Sound>>call (t_call-Trigger).
			var callEnv = EnvGen.kr(Env.perc(0.005, 0.35), t_call);
			var freqShift = 1 + (callEnv * 0.3);
			var ampBoost = 1 + (callEnv * 1.5);
			var wings = LFPulse.ar(wingRate, 0, wingDuty) * WhiteNoise.ar(1);
			var buzz = Ringz.ar(wings, ringFreq1 * freqShift, ringDecay1)
				+ Ringz.ar(wings, ringFreq2 * freqShift, ringDecay2);
			// gate schaltet das CallingPattern (Intent 30) An/Aus; Lag.kr glättet den
			// Sprung zu einer kurzen Rampe, damit das Umschalten nicht klickt. Default 1
			// (immer an) — ohne pattern bleibt gate unverändert, bisheriges durchgehendes
			// Verhalten.
			Out.ar(out, buzz * amp * ampBoost * Lag.kr(gate, 0.02));
		}).add;
	}

	// erzeugt den Synth auf dem von Sound angelegten Bus. Setzt voraus, dass
	// addSynthDef vorher (mit etwas zeitlichem Abstand) aufgerufen wurde —
	// SynthDef-Registrierung (/d_recv) ist asynchron und würde sonst mit dem
	// /s_new dieses Aufrufs um die Wette laufen.
	makeSynth { |server, bus|
		^Synth(\insectSound, [
			\out, bus.index,
			\wingRate, wingRate,
			\wingDuty, wingDuty,
			\ringFreq1, ringFreq1,
			\ringFreq2, ringFreq2,
			\ringDecay1, ringDecay1,
			\ringDecay2, ringDecay2,
			\amp, amp
		], server);
	}
}
