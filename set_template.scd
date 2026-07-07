// set_template.scd — Live-Set-Vorlage
// Nach boot/boot.scd und fx.scd öffnen. Während des Sets Block für Block
// ausführen (Cmd+Enter). Ndef(\track) / Ndef(\fx) einfach neu ausführen —
// JITLib überblendet automatisch (fadeTime), kein Klicken, kein Stottern.

// --- 1) Track starten ---
(
Ndef(\track).fadeTime = 2;
Ndef(\track, {
	var buf = ~tracks[\Glass];
	PlayBuf.ar(2, buf, BufRateScale.kr(buf) * \rate.kr(1, 0.5), loop: 1)
});
)

// --- 2) durch die FX-Kette schicken und starten ---
(
Ndef(\fx).fadeTime = 0.3;
Ndef(\fx, { ~fxChains[\clean].(Ndef.ar(\track, 2)) });
Ndef(\fx).play;
)

// --- 3) Spannung aufbauen: Filter-Sweep ---
Ndef(\fx, { ~fxChains[\filterSweep].(Ndef.ar(\track, 2)) });
Ndef(\fx).set(\cutoff, 300);   // Filter zu...
Ndef(\fx).set(\cutoff, 12000); // ...dann für den Drop öffnen

// --- 4) Break: Stutter/Glitch ---
Ndef(\fx, { ~fxChains[\stutterGate].(Ndef.ar(\track, 2)) });
Ndef(\fx).set(\gateFreq, 16);
Ndef(\fx).set(\gateFreq, 4); // langsamer stottern

// --- 5) Echo-Ausklang in den nächsten Track ---
Ndef(\fx, { ~fxChains[\echo].(Ndef.ar(\track, 2)) });
Ndef(\fx).set(\mix, 0.6, \decay, 6);

// --- 6) Crossfade zum nächsten Track ---
// (Ndef(\track).fadeTime regelt die Überblendung automatisch)
(
Ndef(\track, {
	var buf = ~tracks[\mytrack02];
	PlayBuf.ar(2, buf, BufRateScale.kr(buf) * \rate.kr(1, 0.5), loop: 1)
});
)

// --- zurück zu clean / Reverb zum Ausklingen ---
Ndef(\fx, { ~fxChains[\reverb].(Ndef.ar(\track, 2)) });

// --- Panic / Reset ---
// Ndef(\track).clear; Ndef(\fx).clear;
