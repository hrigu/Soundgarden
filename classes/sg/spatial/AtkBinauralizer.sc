// AtkBinauralizer — Binauralisierung über echtes HRTF-Rendering (Ambisonic Toolkit, ATK),
// als Ersatz für die einfache Pan/ITD-Näherung aus Binauralizer (Intent 5). Gleiche
// öffentliche Schnittstelle wie Binauralizer (play/set/stop), damit SoundObject beide
// austauschbar verwenden kann. Scope bewusst auf die horizontale Ebene beschränkt (siehe
// Intent 5, 1.4) — Elevation (phi) ist fest 0.
//
// Signalweg pro Instanz: Mono-Eingang → distanzabhängiger Tiefpass/Pegel (wie Binauralizer)
// → PanB2 (horizontales B-Format, SuperCollider-Core) → FoaDecode (HRTF-Kernel) → Stereo.
// Encoding bewusst über PanB2 statt FoaEncode/FoaTransform('push'): letztere kompilieren
// (für den generischen, nicht-achsengebundenen Fall) zu FoaRotate, einem UGen aus
// sc3-plugins — das ist laut CLAUDE.md (Gotchas) bewusst nicht installiert. PanB2 ist
// Core-UGen, erzeugt aber dieselbe (FuMa-)B-Format-Konvention, die FoaDecode erwartet, und
// deckt den hier ohnehin auf die horizontale Ebene beschränkten Fall (siehe Intent 5, 1.4)
// vollständig ab. Der Decoder-Kernel selbst (FoaDecoderKernel/FoaDecode) braucht keine
// sc3-plugins — intern nur Convolution2 & Co., beides SuperCollider-Core.
//
// Decoder-Kernel ist eine teure, geteilte Ressource (Server-Buffer) — genau EINE Instanz
// pro Session, siehe *setup/*teardown, nicht pro AtkBinauralizer-Objekt.
//
// Azimuth-Einheit/Vorzeichen: Listener>>relativeAzimuth liefert Radiant, positiv = rechts
// (siehe Listener.sc). PanB2 erwartet -1..1 (nicht Radiant; siehe PanB2-Hilfe: -1/+1 =
// hinten, -0.5 = links, 0 = vorne, +0.5 = rechts) — Konvention "positiv = rechts" stimmt
// direkt überein, daher nur Skalierung (/pi), kein Vorzeichenwechsel nötig.
AtkBinauralizer {
	classvar <decoderKernel;  // FoaDecoderKernel (HRTF) — eine geteilte Instanz für alle

	var <>lagTime;        // Glättungszeit für Azimuth-/Distanzänderungen, in Sekunden
	var <>cutoffMin;       // Tiefpass-Grenzfrequenz in maximaler Distanz, in Hz
	var <>cutoffMax;       // Tiefpass-Grenzfrequenz in Distanz 0, in Hz
	var <>ampRolloff;      // wie schnell die Lautstärke mit der Distanz abfällt
	var <>reverbMix;       // 0..1 — wie stark dieses Objekt insgesamt in RoomReverb einspeist
	var <synth;            // laufender Synth, sobald play() aufgerufen wurde

	// erzeugt eine Instanz mit den gegebenen (oder Default-)Parametern
	*new { |lagTime = 0.08, cutoffMin = 600, cutoffMax = 9000, ampRolloff = 0.9,
			reverbMix = 0.3|
		^super.new.init(lagTime, cutoffMin, cutoffMax, ampRolloff, reverbMix);
	}

	init { |aLagTime, aCutoffMin, aCutoffMax, aAmpRolloff, aReverbMix|
		lagTime = aLagTime;
		cutoffMin = aCutoffMin;
		cutoffMax = aCutoffMax;
		ampRolloff = aAmpRolloff;
		reverbMix = aReverbMix;
	}

	// legt den geteilten HRTF-Decoder-Kernel an und registriert danach die SynthDef — ANDERS
	// als bei InsectSound/Binauralizer reicht hier kein bloßer zeitlicher Abstand zwischen
	// den Schritten: der Kernel lädt HRTF-Daten asynchron als
	// Server-Buffer, und addSynthDef braucht dessen tatsächliche Buffergröße (Convolution2
	// intern), nicht nur die Buffernummer. Ohne Sync schlägt addSynthDef mit
	// "Convolution2 arg: 'framesize' has bad input: nil" fehl (Buffer-Info noch nicht da).
	// server.sync (in einer Routine, da .sync nur innerhalb eines Threads yielden kann)
	// wartet zuverlässig, bevor addSynthDef intern aufgerufen wird.
	// subjectID: 21 = KEMAR-Kunstkopf (generische Messung, kein bestimmtes Individuum) —
	// siehe FoaDecoderKernel-Hilfe für Alternativen zum Ausprobieren.
	// reverbBus (optional): siehe addSynthDef.
	*setup { |server, subjectID = 21, reverbBus|
		decoderKernel = FoaDecoderKernel.newCIPIC(subjectID, server);
		Routine({
			server.sync;
			this.addSynthDef(reverbBus);
		}).play;
	}

	// gibt den geteilten HRTF-Kernel frei (Server-Buffer) — einmal am Ende der Session,
	// nicht pro Instanz (siehe stop).
	*teardown {
		decoderKernel !? { decoderKernel.free };
	}

	// registriert die \atkBinauralizer-SynthDef beim Server. Wird von *setup selbst
	// aufgerufen, sobald der Kernel geladen ist — nicht separat von außen aufrufen.
	//
	// reverbBus (optional): Bus einer RoomReverb-Instanz (siehe dort) — per Closure fest
	// eingebacken (wie decoder), da Out.ar sein Bus-Ziel nicht zur Laufzeit wechseln kann.
	// Ohne reverbBus (nil) entsteht kein Send. Send-Betrag nutzt den ohnehin berechneten
	// distAmp: nah (distAmp≈1) sendet kaum etwas, weit (distAmp≈0) fast alles, skaliert mit
	// reverbMix.
	*addSynthDef { |reverbBus|
		var decoder = decoderKernel;

		SynthDef(\atkBinauralizer, { |in = 0, out = 0, azimuth = 0, distance = 1,
				lagTime = 0.08, cutoffMin = 600, cutoffMax = 9000, ampRolloff = 0.9,
				reverbMix = 0.3|
			// azimuth kommt von Listener>>relativeAzimuth auf (-pi, pi] gewrappt an; beim
			// Vorbeiziehen hinter dem Hörer springt der Rohwert um ~2pi (+pi -> -pi). Lag.kr
			// direkt auf azimuth würde diesen Sprung linear durchfahren (hörbarer Klick, da
			// PanB2 kurz alle Richtungen durchläuft) — stattdessen sin/cos VOR dem Lag bilden
			// (an der Wrap-Grenze stetig) und den Winkel danach per atan2 rekonstruieren.
			var sinAz = Lag.kr(sin(azimuth), lagTime);
			var cosAz = Lag.kr(cos(azimuth), lagTime);
			var az = atan2(sinAz, cosAz);
			var dist = Lag.kr(distance, lagTime);
			var cutoff = (cutoffMax / (1 + dist)).clip(cutoffMin, cutoffMax);
			var distAmp = (1 / (1 + (dist * ampRolloff))).clip(0, 1);
			var mono = LPF.ar(In.ar(in, 1), cutoff) * distAmp;
			var bFormat = PanB2.ar(mono, az / pi, 1);
			var binaural = FoaDecode.ar(bFormat, decoder);
			Out.ar(out, binaural);
			if(reverbBus.notNil) {
				Out.ar(reverbBus, mono * (1 - distAmp) * reverbMix);
			};
		}).add;
	}

	// target/addAction bestimmen die Position im Node-Baum — muss NACH der Klangquelle
	// laufen, gleiches Muster wie Binauralizer.
	play { |server, inBus, outBus = 0, target, addAction = \addToTail|
		synth = Synth.new(\atkBinauralizer, [
			\in, inBus.index,
			\out, outBus,
			\lagTime, lagTime,
			\cutoffMin, cutoffMin,
			\cutoffMax, cutoffMax,
			\ampRolloff, ampRolloff,
			\reverbMix, reverbMix
		], target ? server, addAction);
		^this
	}

	// aktualisiert Azimuth/Distanz auf dem laufenden Synth — SoundObject ruft dies bei
	// jedem Tick der Bewegungs-Routine auf.
	set { |azimuth, distance|
		synth.set(\azimuth, azimuth, \distance, distance);
	}

	// gibt nur den Synth frei — der geteilte Decoder-Kernel gehört der Klasse (*teardown),
	// nicht der Instanz.
	stop {
		synth.free;
	}
}
