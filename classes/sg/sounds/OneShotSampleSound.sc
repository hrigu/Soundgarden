// OneShotSampleSound — extern triggerbares Sample fuer Pattern-Sequencer.
//
// Anders als SampleSound erzeugt diese Klasse keine eigene Impulse.kr-Rate. makeSynth erzeugt
// nur einen stillen Owner-Synth als stabiles Target fuer den Binauralizer; trigger() startet pro
// Hit einen eigenen kurzlebigen Playback-Synth auf denselben Bus. Dichte Hits schneiden sich so
// nicht gegenseitig ab.
OneShotSampleSound : SampleSound {
	var activeSynths;

	*addSynthDef {
		SynthDef(\oneShotSampleOwner, { |out = 0|
			Out.ar(out, Silent.ar(1));
		}).add;

		SynthDef(\oneShotSampleVoice, { |out = 0, buf = 0, amp = 0.5, startFrac = 0,
				duration = 0|
			var ampCtrl = Lag.kr(amp, 0.1);
			var startPos = startFrac * BufFrames.kr(buf);
			var sig = PlayBuf.ar(1, buf, BufRateScale.kr(buf), 1, startPos, doneAction: 0);
			var attack = 0.005;
			var release = 0.03;
			var sustain = (duration - attack - release).max(0);
			var cutoffEnv = EnvGen.kr(Env.linen(attack, sustain, release), 1, doneAction: 2);
			var gain = Select.kr(duration > 0, [1, cutoffEnv]);
			Out.ar(out, sig * ampCtrl * gain);
		}).add;
	}

	makeSynth { |server, bus|
		buffer = buffer ?? { Buffer.readChannel(server, path, channels: [0]) };
		activeSynths = List.new;
		^Synth(\oneShotSampleOwner, [\out, bus.index], server);
	}

	trigger {
		var voice;
		if(synth.isNil or: { buffer.isNil }) { ^this };
		voice = Synth(\oneShotSampleVoice, [
			\out, bus.index,
			\buf, buffer.bufnum,
			\amp, amp,
			\startFrac, startFrac,
			\duration, duration
		], synth, \addBefore);
		activeSynths.add(voice);
		SystemClock.sched(this.voiceCleanupDelay, {
			activeSynths.remove(voice);
			nil
		});
		^this
	}

	voiceCleanupDelay {
		^duration.max(0.1) + 0.1
	}

	activeVoiceCount {
		^activeSynths !? { activeSynths.size } ?? { 0 }
	}

	stop {
		activeSynths !? { activeSynths.do(_.free); activeSynths = List.new };
		super.stop;
	}
}
