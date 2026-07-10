// Listener — der Hörer. Position und Blickrichtung (facing, in Grad, 0 = entlang +y) können
// sich bewegen/drehen (siehe moveForward/strafeLeft/rotate etc., z.B. von
// SpatialControlPanel genutzt). Rechnet Weltkoordinaten in "was hört er" um: Azimuth
// relativ zur eigenen Blickrichtung (0 = vorne, positiv = rechts, ±pi = hinten) und Distanz.
// Kennt zusätzlich seine "Ohren" (binauralizerClass, siehe makeBinauralizer) — Binauralisierung
// ist eine Eigenschaft des Hörers, nicht der Klangquelle (Intent 27).
Listener {
	var <>pos;     // [x, y, z]
	var <>facing;  // Grad, 0 = entlang +y
	var <binauralizerClass;            // welche "Ohren" dieser Listener hat, siehe
	                                    // makeBinauralizer/binauralizerClass_
	var <>subjectID;                    // ATK-Kunstkopf-ID, nur für AtkBinauralizer relevant
	var <>directOutBinauralizerClass;  // Fallback-Klasse für binauralizerClass = nil
	var server, reverbBus;              // gemerkt seit setup() — siehe dort

	*new { |pos = #[0, 0, 0], facing = 0, binauralizerClass, subjectID = 21|
		^super.new.init(pos, facing, binauralizerClass, subjectID);
	}

	// binauralizerClass-Default (Binauralizer) hier statt in der Parameterliste aufgelöst —
	// Klassenreferenzen sind dort keine gültigen literalen Default-Werte (SuperCollider-
	// Syntaxregel, gleiches Muster wie bei Sound/SoundObject). Reine Buchhaltung, löst noch
	// KEIN addSynthDef/setup aus (siehe binauralizerClass_/setup) — das passiert erst, wenn
	// dieser Listener einem Room mit echtem Server/Reverb-Bus zugewiesen wird.
	init { |aPos, aFacing, aBinauralizerClass, aSubjectID|
		pos = aPos;
		facing = aFacing;
		binauralizerClass = aBinauralizerClass ?? { Binauralizer };
		subjectID = aSubjectID;
		directOutBinauralizerClass = DirectOutBinauralizer;
	}

	// verkabelt diesen Listener mit einem konkreten Server/Reverb-Bus (Intent 52) — von Room
	// aufgerufen, wenn ihm dieser Listener zugewiesen wird (Room besitzt den Reverb-Bus, siehe
	// RoomReverb). Lädt dabei sofort die aktuell konfigurierte binauralizerClass. Muss erneut
	// aufgerufen werden, sobald derselbe Listener einem anderen Room zugewiesen wird (anderer
	// Reverb-Bus) — relevant für ein künftiges Multi-Room-Setup, nicht Teil dieses Intents.
	setup { |aServer, aReverbBus|
		server = aServer;
		reverbBus = aReverbBus;
		this.loadBinauralizerClass(binauralizerClass);
	}

	// zentrale Stelle für den Binauralizer-Typ dieses Listeners (Intent 52, vormals in Room —
	// siehe Intent 27/51): lädt bei bereits verkabeltem Listener (server.notNil, siehe setup)
	// sofort die passende SynthDef bzw. initialisiert ATK; vor dem ersten setup nur
	// Buchhaltung. nil wird NICHT als Fehlen behandelt, sondern auf directOutBinauralizerClass
	// abgebildet — makeBinauralizer bleibt dadurch immer mit einer echten Klasse unterwegs.
	binauralizerClass_ { |aBinauralizerClass|
		if(aBinauralizerClass.isNil) {
			binauralizerClass = directOutBinauralizerClass;
		} {
			binauralizerClass = aBinauralizerClass;
		};
		if(server.notNil) {
			this.loadBinauralizerClass(binauralizerClass);
		};
	}

	// registriert die SynthDef eines Binauralizer-Typs bzw. initialisiert ATK — AtkBinauralizer
	// erkennt sich per respondsTo(\setup) statt Klassenvergleich, damit Test-Doubles ohne echte
	// Vererbung dieselbe Weiche durchlaufen (siehe TestListener).
	loadBinauralizerClass { |aClass|
		if(aClass.respondsTo(\setup)) {
			aClass.setup(server, subjectID, reverbBus);
		} {
			aClass.addSynthDef(reverbBus);
		};
	}

	// finaler Ressourcenabbau, wenn dieser Listener einen Room verlässt (siehe Room>>teardown)
	// — delegiert an binauralizerClass.teardown, falls die Klasse geteilte Ressourcen freigibt
	// (z.B. AtkBinauralizer/dessen HRTF-Kernel); respondsTo(\teardown) statt Klassenvergleich,
	// siehe binauralizerClass_.
	teardown {
		if(binauralizerClass.notNil and: { binauralizerClass.respondsTo(\teardown) }) {
			binauralizerClass.teardown;
		};
	}

	// erzeugt einen neuen Binauralizer passend zu den "Ohren" dieses Listeners — SoundObjects
	// bekommen ihren Binauralizer nicht mehr direkt zugewiesen, sondern über Room>>register,
	// das diese Methode aufruft. Interna: welche Klasse/Strategie verwendet wird, ist Sache
	// des Listeners, nicht des aufrufenden Skripts (siehe Intent 27).
	makeBinauralizer { |reverbMix = 0.3|
		^binauralizerClass.new(reverbMix: reverbMix)
	}

	// Azimuth zur anderen Position relativ zur Blickrichtung
	relativeAzimuth { |targetPos|
		var rel = targetPos - pos;
		var worldAngle = atan2(rel[0], rel[1]);
		^(worldAngle - facing.degrad).wrap(pi.neg, pi)
	}

	distanceTo { |targetPos|
		var rel = targetPos - pos;
		^(rel.pow(2).sum).sqrt
	}

	// Vorwärtsvektor der aktuellen Blickrichtung (Einheitsvektor, xy-Ebene).
	// Bei facing=0 zeigt er entlang +y, bei facing=90 entlang +x — konsistent
	// mit der "positiv = rechts"-Konvention aus relativeAzimuth.
	forwardVector {
		^[sin(facing.degrad), cos(facing.degrad), 0]
	}

	// Rechtsvektor der aktuellen Blickrichtung — 90° im Uhrzeigersinn von forwardVector.
	rightVector {
		^[cos(facing.degrad), sin(facing.degrad).neg, 0]
	}

	// moveForward/moveBackward — Bewegung entlang der Blickrichtung (Distanz d in Metern).
	moveForward { |d|
		pos = pos + (this.forwardVector * d);
		^pos
	}

	moveBackward { |d|
		^this.moveForward(d.neg)
	}

	// strafeLeft/strafeRight — seitliche Bewegung, senkrecht zur Blickrichtung.
	strafeRight { |d|
		pos = pos + (this.rightVector * d);
		^pos
	}

	strafeLeft { |d|
		^this.strafeRight(d.neg)
	}

	// rotate — ändert die Blickrichtung um deltaDegrees (positiv = im Uhrzeigersinn,
	// konsistent mit der "positiv = rechts"-Konvention).
	rotate { |deltaDegrees|
		facing = facing + deltaDegrees;
		^facing
	}
}
