// OneShotSampleSound — extern triggerbares Sample fuer Pattern-Sequencer.
//
// Anders als SampleSound erzeugt diese Klasse keine eigene Impulse.kr-Rate. Der Synth bleibt
// still, bis trigger() ein t_trig setzt; dann spielt PlayBuf den gewaehlten Ausschnitt einmal ab.
OneShotSampleSound : SampleSound {

	*addSynthDef {
		SynthDef(\oneShotSampleSound, { |out = 0, buf = 0, amp = 0.5, startFrac = 0,
				duration = 0, t_trig = 0|
			var ampCtrl = Lag.kr(amp, 0.1);
			var startPos = startFrac * BufFrames.kr(buf);
			var sig = PlayBuf.ar(1, buf, BufRateScale.kr(buf), t_trig, startPos, doneAction: 0);
			var attack = 0.005;
			var release = 0.03;
			var sustain = (duration - attack - release).max(0);
			var cutoffEnv = EnvGen.kr(Env.linen(attack, sustain, release), t_trig,
				doneAction: 0);
			var gain = Select.kr(duration > 0, [1, cutoffEnv]);
			Out.ar(out, sig * ampCtrl * gain);
		}).add;
	}

	makeSynth { |server, bus|
		buffer = buffer ?? { Buffer.readChannel(server, path, channels: [0]) };
		^Synth(\oneShotSampleSound, [
			\out, bus.index,
			\buf, buffer.bufnum,
			\amp, amp,
			\startFrac, startFrac,
			\duration, duration
		], server);
	}

	trigger {
		synth.set(\t_trig, 1);
	}
}
