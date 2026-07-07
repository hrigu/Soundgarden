// reverb.scd — Testsetup: eine einzige, stationäre Klangquelle (trockenes Sample, fester
// Rhythmus), um die sich der Hörer per Tastatur frei bewegen und drehen kann — zum gezielten
// Einstellen von RoomReverb-Parametern und Direkt/Hall-Verhältnis, ohne von mehreren
// gleichzeitigen Objekten (wie in sample.scd/sample_moving.scd) abgelenkt zu werden.
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) boot/load_classes.scd — bindet SampleSound/SteadyMoveRule/Binauralizer/RoomReverb/
//                               SoundObject/Orchestra/KeyboardListenerControl/SpaceView ein
//   2) boot/boot.scd        — Server muss laufen
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning) funktionieren nicht über Lautsprecher
// richtig.

// --- 1) Setup: SynthDefs registrieren + Soundobjekt anlegen ---
(
// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen (SuperCollider-Syntaxregel).
var soundsDir, sampleFiles, sampleFile, pos, movable, sound, binauralizer;

SampleSound.addSynthDef;
RoomReverb.addSynthDef;
// allocBus (nicht play!) — der geteilte Bus muss existieren, bevor Binauralizer.addSynthDef
// ihn unten als reverbBus referenziert; der eigentliche Reverb-Synth startet erst in Block 2,
// nachdem der Sound-Synth schon läuft (siehe RoomReverb>>play).
~roomReverb = RoomReverb.new;
~roomReverb.allocBus(s);
Binauralizer.addSynthDef(~roomReverb.bus);

~listener = Listener.new;
~orchestra = Orchestra.new(~listener);

// gültige Audiodateien aus sounds/ — rekursiv (filesDo statt entries.select), wie in
// sample.scd; nimmt einfach die erste gefundene Datei als Testklang.
soundsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "sounds");
sampleFiles = [];
soundsDir.filesDo({ |pe|
	if(BootTrackDetection.isValidExtension(pe.extension)) { sampleFiles = sampleFiles.add(pe) };
});
sampleFile = sampleFiles.first;

pos = [0, 3, 0]; // 3m direkt vor dem Listener (facing=0 zeigt entlang +y, siehe Listener)
movable = Movable.new(pos, SteadyMoveRule.new, 5); // roomRadius ohne Wirkung, da
	// SteadyMoveRule sich nicht von selbst bewegt (nur für bewegte Objekte relevant,
	// siehe Movable>>step)
sound = SampleSound.new(sampleFile.fullPath, 1, 0); // 1 Hz — einmal pro Sekunde, damit der
	// Nachklang zwischen den Wiederholungen gut hörbar ausklingen kann
binauralizer = Binauralizer.new(reverbMix: 0.6); // höher als der Default (0.3) — der Hall
	// soll bei diesem Testsetup bewusst gut hörbar sein

~testObject = SoundObject.new(movable, sound, binauralizer);
sound.preload(s); // lädt den Buffer schon jetzt, statt erst bei orchestra.play(s) in Block 2
	// — gibt der asynchronen Dateileseoperation Zeit (siehe SampleSound>>preload)
~orchestra.register(~testObject);

"Testklang: %\n".postf(sampleFile.fileNameWithoutExtension);
)

// --- 2) Abspielen ---
~orchestra.play(s);
// RoomReverb erst jetzt starten (addToTail), nachdem der Sound-Synth schon läuft —
// siehe RoomReverb>>play.
~roomReverb.play(s);

// --- 3) Tastatursteuerung + Raum-Draufsicht ---
// W/S vor/zurück, A/D seitlich, Q/E drehen — verändert Azimuth/Distanz zur Klangquelle live,
// hörbar an Pan/ITD und am Direkt/Hall-Verhältnis (weiter weg = mehr Hall-Anteil).
~listenerControl = KeyboardListenerControl.new(~listener);
~listenerControl.play;

// Zeigt Listener (Position + Blickrichtung) und die Klangquelle von oben betrachtet.
~spaceView = SpaceView.new(~orchestra);
~spaceView.play;

// --- 4) Live-Coding: Quelle umplatzieren (Alternative zum Herumlaufen) ---
~testObject.movable.moveTo([-2, 4, 0]);

// --- 5) Live-Tuning: Hall-Parameter ---
// RoomReverb/Binauralizer exponieren ihren laufenden Synth öffentlich (<synth) — direktes
// .set() reicht, keine eigene Methode nötig. Werte nach Belieben verändern und neu ausführen.
~roomReverb.synth.set(\roomSize, 12);
~roomReverb.synth.set(\revTime, 5);
~roomReverb.synth.set(\damping, 0.3);
~roomReverb.synth.set(\mix, 1);
~testObject.binauralizer.synth.set(\reverbMix, 0.8);

// --- 6) Stoppen ---
// ~orchestra.stop / ~orchestra.play(s) sind beliebig abwechselbar (Intent 25) — zum
// Zwischendurch-Anhalten reicht die erste Zeile, das Setup (Block 1) bleibt gültig.
~orchestra.stop;
~roomReverb.stop;
~listenerControl.stop;
~spaceView.stop;
