// Binauralizer — formt, wie ein Klang für die Ohren des Hörers klingt,
// abhängig von Distanz und Ausrichtung zwischen Hörer und Klangquelle: Pan,
// winzige Laufzeitdifferenz zwischen den Ohren (ITD), distanzabhängiger
// Pegel/Tiefpass, "hinten"-Dämpfung. Liest einen Mono-Eingang (z.B. von
// InsectSound) und schreibt Stereo auf den Hardware-Output. Bewusst getrennt
// von der Klangerzeugung, damit sich die Binauralisierung (z.B. später
// ATK-HRTF, Intent 5) austauschen lässt, ohne den Klang selbst anzufassen.
//
// Gemeinsames Binauralizer-Interface (Duck-Typing, keine gemeinsame Basisklasse — siehe
// AtkBinauralizer/DirectOutBinauralizer, die dieselben Methoden implementieren, ohne von
// dieser Klasse zu erben): jede Binauralizer-Implementierung in diesem Projekt bietet
//   - *addSynthDef(reverbBus) bzw. *setup(server, subjectID, reverbBus) (AtkBinauralizer,
//     wegen asynchron zu ladendem HRTF-Kernel) — registriert die eigene SynthDef,
//   - play(server, inBus, outBus, target, addAction) — startet den Synth, muss NACH der
//     Klangquelle im Node-Baum laufen (siehe SoundObject>>play),
//   - set(azimuth, distance) — aktualisiert Ausrichtung/Distanz auf dem laufenden Synth,
//     pro Tick von Orchestra/SoundObject aufgerufen,
//   - stop — gibt den eigenen Synth frei (nicht den Eingangs-Bus, der gehört der Klangquelle).
// SoundObject/Room/Listener verwenden ausschließlich dieses Interface und kennen die
// konkrete Binauralizer-Klasse nicht (siehe Listener>>makeBinauralizer, Intent 27).
Binauralizer {
	var <>lagTime;        // Glättungszeit für Azimuth-/Distanzänderungen, in Sekunden
	var <>itdScale;       // max. Laufzeitdifferenz zwischen den Ohren, in Sekunden
	var <>cutoffMin;      // Tiefpass-Grenzfrequenz in maximaler Distanz, in Hz
	var <>cutoffMax;      // Tiefpass-Grenzfrequenz in Distanz 0, in Hz
	var <>ampRolloff;     // wie schnell die Lautstärke mit der Distanz abfällt
	var <>behindDampMin;  // Lautstärkefaktor, wenn die Quelle genau hinter dem Hörer ist
	var <>reverbMix;      // 0..1 — wie stark dieses Objekt insgesamt in RoomReverb einspeist
	var <synth;           // laufender Synth, sobald play() aufgerufen wurde

	// erzeugt eine Instanz mit den gegebenen (oder Default-)Parametern
	*new { |lagTime = 0.08, itdScale = 0.0006, cutoffMin = 600, cutoffMax = 9000,
			ampRolloff = 0.9, behindDampMin = 0.55, reverbMix = 0.2|
		^super.new.init(lagTime, itdScale, cutoffMin, cutoffMax, ampRolloff, behindDampMin,
			reverbMix);
	}

	init { |aLagTime, aItdScale, aCutoffMin, aCutoffMax, aAmpRolloff, aBehindDampMin,
			aReverbMix|
		lagTime = aLagTime;
		itdScale = aItdScale;
		cutoffMin = aCutoffMin;
		cutoffMax = aCutoffMax;
		ampRolloff = aAmpRolloff;
		behindDampMin = aBehindDampMin;
		reverbMix = aReverbMix;
	}

	// registriert die \binauralizer-SynthDef beim Server: Pan (sin(az)), ITD
	// (DelayL pro Kanal), distanzabhängiger Tiefpass/Pegel und Behind-Damping
	// (cos(az)) gegen das Vorne/Hinten-Cone-of-Confusion-Problem. Liest von
	// in (Mono), schreibt Stereo nach out. Asynchron (/d_recv) — vor play()
	// mit etwas zeitlichem Abstand aufrufen.
	//
	// reverbBus (optional): Bus einer RoomReverb-Instanz (siehe dort) — muss vor
	// addSynthDef existieren, wird per Closure fest in die SynthDef eingebacken (wie
	// decoder/encoder bei AtkBinauralizer), da Out.ar sein Bus-Ziel nicht zur Laufzeit
	// wechseln kann. Ohne reverbBus (nil) entsteht schlicht kein Send — kein Fehler.
	// Send-Betrag nutzt den ohnehin berechneten distAmp: nah (distAmp≈1) sendet kaum
	// etwas, weit (distAmp≈0) fast alles, skaliert mit reverbMix.
	//
	// Die Zahlen-Defaults hier dupliziert *new (bewusst, s. InsectSound für die
	// ausführliche Begründung): play() übergibt immer alle Instanzvariablen
	// explizit, die SynthDef-eigenen Defaults greifen also nur bei direktem
	// Server-Zugriff ohne die Klasse. SynthDef-Argument-Defaults müssen
	// literale Konstanten sein, können also nicht auf *new verweisen.
	*addSynthDef { |reverbBus|
		SynthDef(\binauralizer, { |in = 0, out = 0, azimuth = 0, distance = 1,
				lagTime = 0.08, itdScale = 0.0006, cutoffMin = 600, cutoffMax = 9000,
				ampRolloff = 0.9, behindDampMin = 0.55, reverbMix = 0.2|
			// azimuth kommt von Listener>>relativeAzimuth auf (-pi, pi] gewrappt an; beim
			// Vorbeiziehen hinter dem Hörer springt der Rohwert um ~2pi (+pi -> -pi). Lag.kr
			// direkt auf azimuth würde diesen Sprung linear durchfahren (hörbarer Klick, da
			// kurz alle Richtungen durchlaufen werden) — stattdessen sin/cos VOR dem Lag
			// bilden: die sind an der Wrap-Grenze stetig, kein Sprung.
			var dist = Lag.kr(distance, lagTime);
			var pan = Lag.kr(sin(azimuth), lagTime);
			var behindCos = Lag.kr(cos(azimuth), lagTime);
			var itd = itdScale * pan;
			var cutoff = (cutoffMax / (1 + dist)).clip(cutoffMin, cutoffMax);
			var behindDamp = behindCos.linlin(-1, 1, behindDampMin, 1);
			var distAmp = (1 / (1 + (dist * ampRolloff))).clip(0, 1);
			var sig = LPF.ar(In.ar(in, 1), cutoff) * behindDamp * distAmp;
			var stereo = Pan2.ar(sig, pan);
			var left = DelayL.ar(stereo[0], 0.01, 0.0015 + itd.max(0));
			var right = DelayL.ar(stereo[1], 0.01, 0.0015 + itd.neg.max(0));
			Out.ar(out, [left, right]);
			if(reverbBus.notNil) {
				Out.ar(reverbBus, sig * (1 - distAmp) * reverbMix);
			};
		}).add;
	}

	// target/addAction bestimmen die Position im Node-Baum — der Binauralizer
	// muss NACH der Klangquelle laufen, damit er ihren Bus im selben Audio-
	// Block noch lesen kann (siehe SoundObject, das dies verkabelt).
	// Setzt außerdem voraus, dass addSynthDef vorher (mit etwas zeitlichem
	// Abstand) aufgerufen wurde — siehe InsectSound>>play.
	play { |server, inBus, outBus = 0, target, addAction = \addToTail|
		synth = Synth.new(\binauralizer, [
			\in, inBus.index,
			\out, outBus,
			\lagTime, lagTime,
			\itdScale, itdScale,
			\cutoffMin, cutoffMin,
			\cutoffMax, cutoffMax,
			\ampRolloff, ampRolloff,
			\behindDampMin, behindDampMin,
			\reverbMix, reverbMix
		], target ? server, addAction);
		^this
	}

	// aktualisiert Azimuth/Distanz auf dem laufenden Synth — SoundObject ruft
	// dies bei jedem Tick der Bewegungs-Routine auf
	set { |azimuth, distance|
		synth.set(\azimuth, azimuth, \distance, distance);
	}

	// gibt nur den Synth frei — der Eingangs-Bus gehört der Klangquelle
	// (z.B. InsectSound), nicht dem Binauralizer
	stop {
		synth.free;
	}
}
