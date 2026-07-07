// reverb.scd — Testsetup: eine einzige, stationäre Klangquelle (trockenes Sample, fester
// Rhythmus), um die sich der Hörer per Tastatur frei bewegen und drehen kann — zum gezielten
// Einstellen der Raumeigenschaften (Größe/Höhe/Oberfläche) und des Direkt/Hall-Verhältnisses,
// ohne von mehreren gleichzeitigen Objekten (wie in sample.scd/sample_moving.scd) abgelenkt
// zu werden.
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) boot/load_classes.scd — bindet SampleSound/SteadyMoveRule/Binauralizer/Room/
//                               SoundObject/KeyboardListenerControl/SpaceView ein
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
~room = Room.new(s);
~room.addSynthDef(Binauralizer);

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
sound.preload(s); // lädt den Buffer schon jetzt, statt erst bei room.play in Block 2 — gibt
	// der asynchronen Dateileseoperation Zeit (siehe SampleSound>>preload)
~room.register(~testObject);

"Testklang: %\n".postf(sampleFile.fileNameWithoutExtension);
)

// --- 2) Abspielen ---
// startet Orchestra vor RoomReverb in der richtigen Reihenfolge (siehe Room>>play).
~room.play;

// --- 3) Tastatursteuerung + Raum-Draufsicht ---
// W/S vor/zurück, A/D seitlich, Q/E drehen — verändert Azimuth/Distanz zur Klangquelle live,
// hörbar an Pan/ITD und am Direkt/Hall-Verhältnis (weiter weg = mehr Hall-Anteil).
~listenerControl = KeyboardListenerControl.new(~room.listener);
~listenerControl.play;

// Zeigt Listener (Position + Blickrichtung) und die Klangquelle von oben betrachtet.
~spaceView = SpaceView.new(~room.orchestra);
~spaceView.play;

// --- 4) Live-Coding: Quelle umplatzieren (Alternative zum Herumlaufen) ---
~testObject.movable.moveTo([-2, 4, 0]);

// --- 5) Live-Tuning: Raumeigenschaften ---
// size/height/surface/mix per Zuweisung ändern — Room leitet daraus die Hall-Parameter ab
// und schreibt sie sofort auf den laufenden Reverb-Synth (siehe Room>>reverbParams).
~room.size = 12;
~room.height = 6;
~room.surface = 0.8; // 0 = rau/absorbierend, 1 = glatt/hart
~room.mix = 1;
~testObject.binauralizer.synth.set(\reverbMix, 0.8);

// --- 6) Stoppen ---
// ~room.stop / ~room.play sind beliebig abwechselbar (Intent 25, jetzt über Room>>stop/play) —
// zum Zwischendurch-Anhalten reicht die erste Zeile, das Setup (Block 1) bleibt gültig.
~room.stop;
~listenerControl.stop;
~spaceView.stop;
