// SampleSound — spielt statt eines synthetisierten Klangs (wie InsectSound) ein
// Audiosample rhythmisch ab: Impulse.kr(rate, phase) triggert PlayBuf.ar, das das
// Sample bei jedem Trigger von vorne startet (kein Loop, kein Überlappen-Management).
// Lädt die Datei als Mono-Buffer (Buffer.readChannel mit channels: [0] — auch bei stereo
// Quelldateien nur der erste Kanal), passend zum Mono-Bus-Vertrag von Sound.
SampleSound : Sound {
	var <path;      // Pfad zur Audiodatei
	var <>rate;     // Trigger-Rate in Hz — wie oft das Sample pro Sekunde neu startet
	var <>phase;    // Phasenversatz des Triggers, 0..1 (Anteil einer Periode)
	var <>amp;      // Grundlautstärke des Klangs selbst, unabhängig von Position
	var <>startFrac;  // 0..1 — Anteil der Sample-Länge, ab dem die Wiedergabe beginnt
	                  // (Intent 53), analog zum Stil von phase
	var <>duration;   // Sekunden, feste Hördauer des Ausschnitts (Intent 53) — 0 bedeutet
	                  // "kein Cutoff": komplette Restlänge ab startFrac wie bisher
	var <buffer;    // geladener Mono-Buffer, ab play() gesetzt
	var <previewCache; // gecachte Sprachseiten-Metadaten/Vorschauen für GUI-Zwecke

	// erzeugt eine Instanz mit gegebener Audiodatei und (oder Default-)Klangparametern
	*new { |path, rate = 2, phase = 0, amp = 0.5, startFrac = 0, duration = 0|
		^super.new.init(path, rate, phase, amp, startFrac, duration);
	}

	init { |aPath, aRate, aPhase, aAmp, aStartFrac, aDuration|
		path = aPath;
		rate = aRate;
		phase = aPhase;
		amp = aAmp;
		startFrac = aStartFrac;
		duration = aDuration;
		previewCache = Dictionary.new;
	}

	// registriert die \sampleSound-SynthDef beim Server. buf wird als Instanz-Argument
	// gesetzt (siehe makeSynth) — eine SynthDef für alle SampleSound-Instanzen, jede mit
	// ihrem eigenen Buffer. Asynchron (/d_recv) — vor play() mit etwas zeitlichem
	// Abstand aufrufen, wie InsectSound/Binauralizer.
	*addSynthDef {
		SynthDef(\sampleSound, { |out = 0, buf = 0, rate = 2, phase = 0, amp = 0.5,
				startFrac = 0, duration = 0|
			// rate/amp geglättet (Lag.kr) fürs klickfreie Live-Tunen im GUI (Intent 43,
			// gleiches Muster wie InsectSound/RoomReverb) -- phase bleibt ungeglättet, reiner
			// Zeitversatz des Triggers ohne die Klick-Problematik von Frequenz-/Pegelsprüngen.
			var rateCtrl = Lag.kr(rate, 0.1);
			var ampCtrl = Lag.kr(amp, 0.1);
			var trig = Impulse.kr(rateCtrl, phase);
			// startFrac wählt den Startpunkt im Buffer (Intent 53) — 0 = wie bisher am Anfang.
			var startPos = startFrac * BufFrames.kr(buf);
			var sig = PlayBuf.ar(1, buf, BufRateScale.kr(buf), trig, startPos, doneAction: 0);
			// duration > 0: fester Ausschnitt statt kompletter Restlänge -- kurzer Fade-in/-out
			// (Env.linen) verhindert einen hörbaren Klick am Cutoff-Punkt, unabhängig davon,
			// wie lang das Original-Sample ist (Intent 53). duration <= 0 (Default) bedeutet
			// "kein Cutoff" -- Select.kr wählt dann Gain 1, das Sample klingt wie bisher
			// vollständig aus (kein zusätzlicher Envelope-Multiply).
			var attack = 0.01;
			var release = 0.05;
			var sustain = (duration - attack - release).max(0);
			var cutoffEnv = EnvGen.kr(Env.linen(attack, sustain, release), trig, doneAction: 0);
			var gain = Select.kr(duration > 0, [1, cutoffEnv]);
			Out.ar(out, sig * ampCtrl * gain);
		}).add;
	}

	// editableParams — live im GUI bearbeitbare Klangparameter (SpatialControlPanel,
	// Intent 43). startFrac/duration seit Intent 53.
	*editableParams {
		^[
			[\rate, ControlSpec(0.2, 8, \exp)],
			[\phase, ControlSpec(0, 1, \lin)],
			[\amp, ControlSpec(0, 1, \lin)],
			[\startFrac, ControlSpec(0, 1, \lin)],
			[\duration, ControlSpec(0, 5, \lin)]
		]
	}

	// lädt den Buffer vorab (asynchron) — optional, aber empfohlen: ohne preload lädt
	// makeSynth den Buffer erst bei play(), dann kann der allererste Trigger auf einen
	// noch leeren Buffer treffen ("Buffer UGen: no buffer data", harmlos, aber unschön).
	// Mit etwas zeitlichem Abstand zu play() aufrufen, wie InsectSound/Binauralizer
	// addSynthDef — z.B. schon in Block 1 des Demo-Skripts, während Block 2 noch nicht
	// lief.
	preload { |server|
		^buffer = Buffer.readChannel(server, path, channels: [0]);
	}

	sampleFileName {
		^PathName(path).fileName
	}

	// liefert Dateiname, Dauer und eine kleine normalisierte Peak-Huelle fuer die GUI.
	// Liest direkt aus der Audiodatei (nicht aus dem Server-Buffer), damit die Daten auch
	// ohne laufenden Synth verfuegbar bleiben. Ergebnisse pro bucketCount cachen.
	samplePreview { |bucketCount = 96|
		var normalizedBucketCount = bucketCount.asInteger.max(1);

		if(previewCache[normalizedBucketCount].isNil) {
			previewCache[normalizedBucketCount] = this.prBuildSamplePreview(normalizedBucketCount);
		};

		^previewCache[normalizedBucketCount]
	}

	// heuristischer Vorschlag fuer sample.scd: kurze Samples ganz, laengere ab dem ersten
	// ausreichend starken Peak mit festem Fenster. Gibt nur einen Vorschlag zurueck, schreibt
	// die Instanz noch nicht um.
	suggestExcerpt { |shortThreshold = 0.5, targetDuration = 0.3, peakThreshold = 0.25,
			bucketCount = 96|
		var preview = this.samplePreview(bucketCount);
		var durationSeconds = preview[\duration] ? 0.0;
		var peaks = preview[\peaks] ? [];
		var hitIndex;
		var startFracSuggestion = 0.0;
		var durationSuggestion;

		if(durationSeconds <= shortThreshold) {
			^(startFrac: 0.0, duration: 0.0, strategy: \fullFile)
		};

		hitIndex = peaks.detectIndex { |peak| peak >= peakThreshold };
		if(hitIndex.notNil and: { peaks.size > 0 }) {
			startFracSuggestion = hitIndex / peaks.size;
		};

		durationSuggestion = targetDuration.min(durationSeconds).max(0.0);
		^(startFrac: startFracSuggestion, duration: durationSuggestion,
			strategy: if(hitIndex.notNil) { \firstPeakWindow } { \fallbackWindow })
	}

	// Pegelschaetzung fuer den tatsaechlich gespielten Ausschnitt. Fuer duration = 0 wird
	// der komplette Rest des Files ab startFrac analysiert. Dient dem pragmatischen
	// Lautheitsausgleich in demos/sample.scd, nicht als allgemeine Loudness-Normierung.
	excerptLevelEstimate {
		var soundFile = SoundFile.openRead(path.standardizePath);
		var fileName = this.sampleFileName;
		var result;

		if(soundFile.isNil) {
			^(fileName: fileName, path: path, startFrame: 0, frameCount: 0, duration: 0.0,
				rms: 0.0, peak: 0.0, error: "Sample-Datei nicht lesbar")
		};

		protect {
			var totalFrames = soundFile.numFrames.asInteger.max(0);
			var numChannels = soundFile.numChannels.asInteger.max(1);
			var sampleRate = soundFile.sampleRate.asFloat.max(1.0);
			var boundedStartFrac = startFrac.clip(0, 1);
			var startFrame = (boundedStartFrac * totalFrames).floor.asInteger.clip(0, totalFrames);
			var availableFrames = (totalFrames - startFrame).max(0);
			var targetFrames = if(duration > 0) {
				(duration * sampleRate).ceil.asInteger.max(1)
			} {
				availableFrames
			};
			var frameCount = targetFrames.min(availableFrames).max(0);
			var rawData = FloatArray.newClear(frameCount * numChannels);
			var sumSquares = 0.0;
			var peak = 0.0;
			var sampleCount = 0;

			if(frameCount > 0) {
				soundFile.seek(startFrame, 0);
				soundFile.readData(rawData);
				rawData.do { |sample|
					var abs = sample.abs;
					sumSquares = sumSquares + (sample * sample);
					if(abs > peak) { peak = abs };
					sampleCount = sampleCount + 1;
				};
			};

			result = (
				fileName: fileName,
				path: path,
				startFrame: startFrame,
				frameCount: frameCount,
				duration: frameCount / sampleRate,
				rms: if(sampleCount > 0) { (sumSquares / sampleCount).sqrt } { 0.0 },
				peak: peak,
				error: nil
			);
		} {
			soundFile.close;
		};

		^result
	}

	prBuildSamplePreview { |bucketCount|
		var soundFile = SoundFile.openRead(path.standardizePath);
		var fileName = this.sampleFileName;
		var result;

		if(soundFile.isNil) {
			^(fileName: fileName, path: path, duration: 0.0, numFrames: 0, bucketCount: bucketCount,
				peaks: Array.fill(bucketCount, 0.0), error: "Sample-Datei nicht lesbar")
		};

		protect {
			var totalFrames = soundFile.numFrames.asInteger;
			var numChannels = soundFile.numChannels.asInteger.max(1);
			var peaks = Array.fill(bucketCount, 0.0);
			var framesPerBucket = (totalFrames / bucketCount).ceil.asInteger.max(1);
			var maxPeak = 0.0;

			bucketCount.do { |bucketIndex|
				var startFrame = bucketIndex * framesPerBucket;

				if(startFrame < totalFrames) {
					var framesToRead = min(framesPerBucket, totalFrames - startFrame).asInteger;
					var rawData = FloatArray.newClear(framesToRead * numChannels);
					var bucketPeak = 0.0;

					soundFile.seek(startFrame, 0);
					soundFile.readData(rawData);
					rawData.do { |sample|
						var abs = sample.abs;
						if(abs > bucketPeak) { bucketPeak = abs };
					};
					peaks[bucketIndex] = bucketPeak;
					if(bucketPeak > maxPeak) { maxPeak = bucketPeak };
				};
			};

			if(maxPeak > 0) {
				peaks = peaks.collect { |peak| peak / maxPeak };
			};

			result = (fileName: fileName, path: path, duration: soundFile.duration,
				numFrames: totalFrames, bucketCount: bucketCount, peaks: peaks, error: nil);
		} {
			soundFile.close;
		};

		^result
	}

	// erzeugt den Synth; lädt den Buffer selbst nach, falls preload nicht vorher
	// aufgerufen wurde. bufnum steht so oder so sofort fest — PlayBuf braucht nur die
	// Nummer, keine Buffer-Metadaten zum SynthDef-Bauzeitpunkt wie z.B. FoaDecoderKernel
	// (siehe AtkBinauralizer).
	makeSynth { |server, bus|
		buffer = buffer ?? { Buffer.readChannel(server, path, channels: [0]) };
		^Synth(\sampleSound, [
			\out, bus.index,
			\buf, buffer.bufnum,
			\rate, rate,
			\phase, phase,
			\amp, amp,
			\startFrac, startFrac,
			\duration, duration
		], server);
	}

	// gibt zusätzlich zu Synth/Bus (Sound>>stop) auch den geladenen Buffer frei.
	// buffer danach auf nil setzen, sonst würde makeSynth beim nächsten play die
	// tote Buffer-Nummer weiterverwenden (buffer ?? lädt nur bei nil nach) — Stille.
	stop {
		super.stop;
		buffer !? { buffer.free };
		buffer = nil;
	}
}
