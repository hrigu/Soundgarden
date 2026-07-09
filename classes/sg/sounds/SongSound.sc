// SongSound - spielt eine Audiodatei als fortlaufenden, nicht rhythmisch getriggerten Klang.
// Gedacht fuer echte Songs oder laengere Atmosphaeren, die im Raum als kontinuierliche Quelle
// gehoert und mit Hall-/Positionsparametern untersucht werden sollen.
SongSound : Sound {
	var <path;       // Pfad zur Audiodatei
	var <>amp;       // Grundlautstaerke des Songs selbst
	var <>loop;      // true = Song nach Ende loopen, false = einmalig abspielen
	var <>rate;      // Abspielgeschwindigkeit (1 = original)
	var <buffer;     // geladener Mono-Buffer

	*new { |path, amp = 0.7, loop = true, rate = 1|
		^super.new.init(path, amp, loop, rate);
	}

	init { |aPath, aAmp, aLoop, aRate|
		path = aPath;
		amp = aAmp;
		loop = aLoop;
		rate = aRate;
	}

	*addSynthDef {
		SynthDef(\songSound, { |out = 0, buf = 0, amp = 0.7, loop = 1, rate = 1|
			var rateCtrl = Lag.kr(rate, 0.1);
			var ampCtrl = Lag.kr(amp, 0.1);
			var sig = PlayBuf.ar(1, buf, BufRateScale.kr(buf) * rateCtrl, 1, 0, loop,
				doneAction: 0);
			Out.ar(out, sig * ampCtrl);
		}).add;
	}

	// editableParams — im GUI sinnvoll live bearbeitbare Song-Parameter. path ist kein
	// Live-Parameter, loop vorerst ebenfalls nicht: Bool-Umschaltung braucht einen anderen
	// Widget-Typ als die bisherigen Slider.
	*editableParams {
		^[
			[\rate, ControlSpec(0.25, 2, \exp)],
			[\amp, ControlSpec(0, 1, \lin)]
		]
	}

	preload { |server|
		^buffer = Buffer.readChannel(server, path, channels: [0]);
	}

	makeSynth { |server, bus|
		buffer = buffer ?? { Buffer.readChannel(server, path, channels: [0]) };
		^Synth(\songSound, [
			\out, bus.index,
			\buf, buffer.bufnum,
			\amp, amp,
			\loop, loop.asInteger,
			\rate, rate
		], server);
	}

	stop {
		super.stop;
		buffer !? { buffer.free };
		buffer = nil;
	}
}
