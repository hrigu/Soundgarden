// SoundObjectBuilder — baut ein SoundObject aus zwei reinen Params-Events (sound/movable),
// statt Movable/Sound von Hand zu instanzieren. Kein Zustand, keine Instanz: Merge (für
// Varianten) und Klon-Unabhängigkeit übernimmt bereits Event selbst (++/copy, SC-Core), dafür
// braucht dieses Projekt keine eigene Logik (Intent 29).
//
// Kennt keine Binauralizer-Klasse — der wird als fertige Instanz entgegengenommen (Intent 27:
// Binauralizer-Auswahl bleibt bei Room/Listener), siehe Room>>registerFromBuilder.
SoundObjectBuilder {

	// baut ein SoundObject: pro Params-Event eine Instanz von soundClass/movableClass, deren
	// <>-Setter (params.key ++ "_") mit den Event-Werten befüllt werden — alle nicht in params
	// genannten Parameter bleiben bei den Defaults der Zielklasse. soundClass/movableClass
	// defaulten auf nil statt direkt auf InsectSound/Movable — Klassennamen sind in sclang
	// keine gültigen Default-Argumentwerte (nur literale Konstanten), daher der Fallback per ??
	// im Methodenkörper.
	*build { |soundParams, movableParams, binauralizer, soundClass, movableClass|
		var sound = this.prApply((soundClass ?? { InsectSound }).new, soundParams);
		var movable = this.prApply((movableClass ?? { Movable }).new, movableParams);
		^SoundObject.new(movable, sound, binauralizer)
	}

	*prApply { |instance, params|
		params.keysValuesDo { |k, v| instance.perform((k ++ "_").asSymbol, v) };
		^instance
	}
}
