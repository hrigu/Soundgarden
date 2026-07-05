// Binauralizer — formt, wie ein Klang für die Ohren des Hörers klingt,
// abhängig von Distanz und Ausrichtung zwischen Hörer und Klangquelle: Pan,
// winzige Laufzeitdifferenz zwischen den Ohren (ITD), distanzabhängiger
// Pegel/Tiefpass, "hinten"-Dämpfung. Liest einen Mono-Eingang (z.B. von
// InsectSound) und schreibt Stereo auf den Hardware-Output. Bewusst getrennt
// von der Klangerzeugung, damit sich die Binauralisierung (z.B. später
// ATK-HRTF, Intent 5) austauschen lässt, ohne den Klang selbst anzufassen.
Binauralizer {
	var <>lagTime;
	var <>itdScale;
	var <>cutoffMin;
	var <>cutoffMax;
	var <>ampRolloff;
	var <>behindDampMin;
	var <synth;

	*new { |lagTime = 0.08, itdScale = 0.0006, cutoffMin = 600, cutoffMax = 9000,
			ampRolloff = 0.9, behindDampMin = 0.55|
		^super.new.init(lagTime, itdScale, cutoffMin, cutoffMax, ampRolloff, behindDampMin);
	}

	init { |aLagTime, aItdScale, aCutoffMin, aCutoffMax, aAmpRolloff, aBehindDampMin|
		lagTime = aLagTime;
		itdScale = aItdScale;
		cutoffMin = aCutoffMin;
		cutoffMax = aCutoffMax;
		ampRolloff = aAmpRolloff;
		behindDampMin = aBehindDampMin;
	}

	*addSynthDef {
		SynthDef(\binauralizer, { |in = 0, out = 0, azimuth = 0, distance = 1,
				lagTime = 0.08, itdScale = 0.0006, cutoffMin = 600, cutoffMax = 9000,
				ampRolloff = 0.9, behindDampMin = 0.55|
			var az = Lag.kr(azimuth, lagTime);
			var dist = Lag.kr(distance, lagTime);
			var pan = sin(az);
			var itd = itdScale * pan;
			var cutoff = (cutoffMax / (1 + dist)).clip(cutoffMin, cutoffMax);
			var behindDamp = cos(az).linlin(-1, 1, behindDampMin, 1);
			var distAmp = (1 / (1 + (dist * ampRolloff))).clip(0, 1);
			var sig = LPF.ar(In.ar(in, 1), cutoff) * behindDamp * distAmp;
			var stereo = Pan2.ar(sig, pan);
			var left = DelayL.ar(stereo[0], 0.01, 0.0015 + itd.max(0));
			var right = DelayL.ar(stereo[1], 0.01, 0.0015 + itd.neg.max(0));
			Out.ar(out, [left, right]);
		}).add;
	}

	// target/addAction bestimmen die Position im Node-Baum — der Binauralizer
	// muss NACH der Klangquelle laufen, damit er ihren Bus im selben Audio-
	// Block noch lesen kann (siehe SoundInsect, das dies verkabelt).
	play { |server, inBus, outBus = 0, target, addAction = \addToTail|
		Binauralizer.addSynthDef;
		synth = Synth.new(\binauralizer, [
			\in, inBus.index,
			\out, outBus,
			\lagTime, lagTime,
			\itdScale, itdScale,
			\cutoffMin, cutoffMin,
			\cutoffMax, cutoffMax,
			\ampRolloff, ampRolloff,
			\behindDampMin, behindDampMin
		], target ? server, addAction);
		^this
	}

	set { |azimuth, distance|
		synth.set(\azimuth, azimuth, \distance, distance);
	}

	stop {
		synth.free;
	}
}
