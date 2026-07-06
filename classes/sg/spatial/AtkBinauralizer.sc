// AtkBinauralizer — Binauralisierung über echtes HRTF-Rendering (Ambisonic Toolkit, ATK),
// als Ersatz für die einfache Pan/ITD-Näherung aus Binauralizer (Intent 5). Gleiche
// öffentliche Schnittstelle wie Binauralizer (play/set/stop), damit SoundObject beide
// austauschbar verwenden kann. Scope bewusst auf die horizontale Ebene beschränkt (siehe
// Intent 5, 1.4) — Elevation (phi) ist fest 0.
//
// Signalweg pro Instanz: Mono-Eingang → distanzabhängiger Tiefpass/Pegel (wie Binauralizer)
// → FoaEncode (Omni) → FoaPush (auf Azimuth "geschoben", volle Direktionalität) → FoaDecode
// (HRTF-Kernel) → Stereo. Encoder-Matrix und Decoder-Kernel sind teure, geteilte Ressourcen
// (Server-Buffer) — genau EINE Instanz pro Session, siehe *setup/*teardown, nicht pro
// AtkBinauralizer-Objekt.
//
// Azimuth-Vorzeichen: Listener>>relativeAzimuth liefert positiv = rechts (siehe Listener.sc).
// ATK verwendet die umgekehrte Konvention (positiv = links, siehe Guides/Encoding-FOA:
// "azimuth -> hard left = back, centre = centre, hard right = back", d.h. positiv dreht nach
// links) — deshalb wird azimuth beim Senden an den Synth negiert.
AtkBinauralizer {
	classvar <encoderMatrix;  // FoaEncoderMatrix.newOmni — eine geteilte Instanz für alle
	classvar <decoderKernel;  // FoaDecoderKernel (HRTF) — eine geteilte Instanz für alle

	var <>lagTime;        // Glättungszeit für Azimuth-/Distanzänderungen, in Sekunden
	var <>cutoffMin;       // Tiefpass-Grenzfrequenz in maximaler Distanz, in Hz
	var <>cutoffMax;       // Tiefpass-Grenzfrequenz in Distanz 0, in Hz
	var <>ampRolloff;      // wie schnell die Lautstärke mit der Distanz abfällt
	var <synth;            // laufender Synth, sobald play() aufgerufen wurde

	// erzeugt eine Instanz mit den gegebenen (oder Default-)Parametern
	*new { |lagTime = 0.08, cutoffMin = 600, cutoffMax = 9000, ampRolloff = 0.9|
		^super.new.init(lagTime, cutoffMin, cutoffMax, ampRolloff);
	}

	init { |aLagTime, aCutoffMin, aCutoffMax, aAmpRolloff|
		lagTime = aLagTime;
		cutoffMin = aCutoffMin;
		cutoffMax = aCutoffMax;
		ampRolloff = aAmpRolloff;
	}

	// legt die geteilte Encoder-Matrix und den HRTF-Decoder-Kernel an — MUSS vor addSynthDef
	// abgeschlossen sein, da die SynthDef beide Objekte direkt referenziert (ihre
	// Server-Buffernummern werden beim Definieren in den UGen-Graph eingebacken). Asynchron
	// (der Kernel lädt HRTF-Daten als Server-Buffer) — etwas zeitlichen Abstand zu
	// addSynthDef lassen, genau wie bei InsectSound/Binauralizer.
	// subjectID: 21 = KEMAR-Kunstkopf (generische Messung, kein bestimmtes Individuum) —
	// siehe FoaDecoderKernel-Hilfe für Alternativen zum Ausprobieren.
	*setup { |server, subjectID = 21|
		encoderMatrix = FoaEncoderMatrix.newOmni;
		decoderKernel = FoaDecoderKernel.newCIPIC(subjectID, server);
	}

	// gibt den geteilten HRTF-Kernel frei (Server-Buffer) — einmal am Ende der Session,
	// nicht pro Instanz (siehe stop).
	*teardown {
		decoderKernel !? { decoderKernel.free };
	}

	// registriert die \atkBinauralizer-SynthDef beim Server. Setzt voraus, dass *setup
	// bereits abgeschlossen ist (siehe oben).
	*addSynthDef {
		var encoder = encoderMatrix;
		var decoder = decoderKernel;

		SynthDef(\atkBinauralizer, { |in = 0, out = 0, azimuth = 0, distance = 1,
				lagTime = 0.08, cutoffMin = 600, cutoffMax = 9000, ampRolloff = 0.9|
			var az = Lag.kr(azimuth, lagTime);
			var dist = Lag.kr(distance, lagTime);
			var cutoff = (cutoffMax / (1 + dist)).clip(cutoffMin, cutoffMax);
			var distAmp = (1 / (1 + (dist * ampRolloff))).clip(0, 1);
			var mono = LPF.ar(In.ar(in, 1), cutoff) * distAmp;
			var encoded = FoaEncode.ar(mono, encoder);
			// angle = pi/2: voll auf eine ebene Welle "geschoben" (Punktquelle), nicht
			// diffus — nur die Richtung (theta) ändert sich live.
			var pushed = FoaTransform.ar(encoded, 'push', pi / 2, az.neg);
			var binaural = FoaDecode.ar(pushed, decoder);
			Out.ar(out, binaural);
		}).add;
	}

	// target/addAction bestimmen die Position im Node-Baum — muss NACH der Klangquelle
	// laufen, gleiches Muster wie Binauralizer.
	play { |server, inBus, outBus = 0, target, addAction = \addToTail|
		synth = Synth.new(\atkBinauralizer, [
			\in, inBus.index,
			\out, outBus,
			\lagTime, lagTime,
			\cutoffMin, cutoffMin,
			\cutoffMax, cutoffMax,
			\ampRolloff, ampRolloff
		], target ? server, addAction);
		^this
	}

	// aktualisiert Azimuth/Distanz auf dem laufenden Synth — SoundObject ruft dies bei
	// jedem Tick der Bewegungs-Routine auf.
	set { |azimuth, distance|
		synth.set(\azimuth, azimuth, \distance, distance);
	}

	// gibt nur den Synth frei — der geteilte Decoder-Kernel gehört der Klasse (*teardown),
	// nicht der Instanz.
	stop {
		synth.free;
	}
}
