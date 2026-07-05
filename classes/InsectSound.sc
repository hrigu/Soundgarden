// InsectSound — der reine Klang des Insekts (Flügelschlag + Brummen). Kennt
// weder Position noch Pan/Lautstärke im Raum — das ist Aufgabe von
// Binauralizer. Spielt auf einen privaten Mono-Bus statt direkt auf den
// Hardware-Output. Klangparameter mit Defaults aus dem ursprünglichen
// \insectVoice-Prototyp (Intent 3), aber pro Instanz überschreibbar.
InsectSound {
	var <>wingRate;
	var <>wingDuty;
	var <>ringFreq1;
	var <>ringFreq2;
	var <>ringDecay1;
	var <>ringDecay2;
	var <>amp;
	var <bus;
	var <synth;

	*new { |wingRate = 210, wingDuty = 0.25, ringFreq1 = 3200, ringFreq2 = 4600,
			ringDecay1 = 0.02, ringDecay2 = 0.015, amp = 0.35|
		^super.new.init(wingRate, wingDuty, ringFreq1, ringFreq2, ringDecay1, ringDecay2, amp);
	}

	init { |aWingRate, aWingDuty, aRingFreq1, aRingFreq2, aRingDecay1, aRingDecay2, aAmp|
		wingRate = aWingRate;
		wingDuty = aWingDuty;
		ringFreq1 = aRingFreq1;
		ringFreq2 = aRingFreq2;
		ringDecay1 = aRingDecay1;
		ringDecay2 = aRingDecay2;
		amp = aAmp;
	}

	*addSynthDef {
		SynthDef(\insectSound, { |out = 0, wingRate = 210, wingDuty = 0.25,
				ringFreq1 = 3200, ringFreq2 = 4600, ringDecay1 = 0.02, ringDecay2 = 0.015,
				amp = 0.35|
			var wings = LFPulse.ar(wingRate, 0, wingDuty) * WhiteNoise.ar(1);
			var buzz = Ringz.ar(wings, ringFreq1, ringDecay1)
				+ Ringz.ar(wings, ringFreq2, ringDecay2);
			Out.ar(out, buzz * amp);
		}).add;
	}

	play { |server|
		InsectSound.addSynthDef;
		bus = Bus.audio(server, 1);
		synth = Synth(\insectSound, [
			\out, bus.index,
			\wingRate, wingRate,
			\wingDuty, wingDuty,
			\ringFreq1, ringFreq1,
			\ringFreq2, ringFreq2,
			\ringDecay1, ringDecay1,
			\ringDecay2, ringDecay2,
			\amp, amp
		], server);
		^this
	}

	stop {
		synth.free;
		bus.free;
	}
}
