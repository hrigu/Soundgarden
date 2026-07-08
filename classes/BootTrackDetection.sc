// Extension-Erkennung für boot.scd — als Klasse, damit sie testbar ist,
// ohne einen Server oder echte Dateien zu brauchen.
BootTrackDetection {
	classvar <validExtensions;

	*initClass {
		validExtensions = [\wav, \aiff, \aif, \flac, \m4a];
	}

	*isValidExtension { |ext|
		^validExtensions.includes(ext.asString.toLower.asSymbol)
	}
}
