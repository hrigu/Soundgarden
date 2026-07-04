// fx.scd — wiederverwendbare Live-FX-Bausteine (JITLib / Ndef)
// Nach boot.scd ausführen. Jeder Eintrag nimmt ein Stereo-Signal und
// gibt das bearbeitete Signal zurück — wird in set_template.scd in
// Ndef(\fx) eingesetzt.

(
~fxChains = ();

// Durchreichen, kein Effekt
~fxChains[\clean] = { |sig| sig };

// Tiefpass-Sweep — klassischer Build-up/Break-Effekt
~fxChains[\filterSweep] = { |sig|
	RLPF.ar(sig, \cutoff.kr(8000, 0.3), \rq.kr(0.3, 0.3))
};

// Echo/Delay — für Drops und Übergänge
~fxChains[\echo] = { |sig|
	var wet = CombN.ar(sig, 0.5, \delayTime.kr(0.375, 0.3), \decay.kr(2, 0.3));
	(sig * (1 - \mix.kr(0.4, 0.3))) + (wet * \mix.kr(0.4, 0.3))
};

// Bitcrush/Decimator — Lo-Fi/Glitch-Charakter
~fxChains[\bitcrush] = { |sig|
	Decimator.ar(sig, \rate.kr(44100, 0.3), \bits.kr(16, 0.3))
};

// Stutter/Gate — rhythmisches An-/Abschneiden für Breaks
~fxChains[\stutterGate] = { |sig|
	sig * LFPulse.ar(\gateFreq.kr(8, 0.3), 0, \gateWidth.kr(0.5, 0.3))
};

// Reverb — Raum/Ausklang
~fxChains[\reverb] = { |sig|
	FreeVerb.ar(sig, \mix.kr(0.3, 0.3), \room.kr(0.7, 0.3), \damp.kr(0.5, 0.3))
};

"FX-Ketten geladen: %\n".postf(~fxChains.keys.asArray.sort);
)
