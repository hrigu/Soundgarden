// SampleSoundConfigurator — kapselt die Demo-Heuristik fuer SampleSound in einer eigenen
// Klasse, statt Ausschnitts- und Lautheitswahl direkt in demos/sample.scd zu verteilen.
// Reine sclang-Logik: waehlt den Ausschnitt, bewertet dessen Pegel und setzt startFrac,
// duration und amp konsistent auf einem SampleSound.
SampleSoundConfigurator {
	var <>shortThreshold;
	var <>targetDuration;
	var <>peakThreshold;
	var <>previewBucketCount;
	var <>baseAmp;
	var <>targetRms;
	var <>minGain;
	var <>maxGain;
	var <>silenceFloor;
	var <>maxOutputPeak;

	*new { |shortThreshold = 1.0, targetDuration = 1.5, peakThreshold = 0.25,
			previewBucketCount = 96, baseAmp = 0.5, targetRms = 0.12, minGain = 0.5,
			maxGain = 1.4, silenceFloor = 0.0001, maxOutputPeak = 0.6|
		^super.new.init(shortThreshold, targetDuration, peakThreshold, previewBucketCount,
			baseAmp, targetRms, minGain, maxGain, silenceFloor, maxOutputPeak);
	}

	init { |aShortThreshold, aTargetDuration, aPeakThreshold, aPreviewBucketCount, aBaseAmp,
			aTargetRms, aMinGain, aMaxGain, aSilenceFloor, aMaxOutputPeak|
		shortThreshold = aShortThreshold;
		targetDuration = aTargetDuration;
		peakThreshold = aPeakThreshold;
		previewBucketCount = aPreviewBucketCount;
		baseAmp = aBaseAmp;
		targetRms = aTargetRms;
		minGain = aMinGain;
		maxGain = aMaxGain;
		silenceFloor = aSilenceFloor;
		maxOutputPeak = aMaxOutputPeak;
		^this
	}

	// Baut ein SampleSound mit den uebergebenen Grundparametern und konfiguriert es direkt
	// mit der aktuell hinterlegten Demo-Heuristik.
	prepare { |path, rate = 2, phase = 0|
		^this.configure(SampleSound.new(path, rate, phase))
	}

	// Wendet die Heuristik auf ein vorhandenes SampleSound an und gibt neben dem mutierten
	// Sound auch Diagnosewerte fuer Post-Ausgaben oder spaetere Tests zurueck.
	configure { |sound|
		var suggestion = sound.suggestExcerpt(shortThreshold: shortThreshold,
			targetDuration: targetDuration, peakThreshold: peakThreshold,
			bucketCount: previewBucketCount);
		var excerptLevel;
		var rmsGain;
		var peakSafetyGain;
		var gainFactor;

		sound.startFrac = suggestion[\startFrac];
		sound.duration = suggestion[\duration];
		excerptLevel = sound.excerptLevelEstimate;

		rmsGain = if((excerptLevel[\rms] ? 0.0) < silenceFloor) {
			1.0
		} {
			targetRms / excerptLevel[\rms]
		};

		peakSafetyGain = if((excerptLevel[\peak] ? 0.0) <= 0.0) {
			maxGain
		} {
			maxOutputPeak / excerptLevel[\peak]
		};

		gainFactor = min(rmsGain, peakSafetyGain).clip(minGain, maxGain);
		sound.amp = baseAmp * gainFactor;

		^(sound: sound, suggestion: suggestion, excerptLevel: excerptLevel, rmsGain: rmsGain,
			peakSafetyGain: peakSafetyGain, gainFactor: gainFactor)
	}
}
