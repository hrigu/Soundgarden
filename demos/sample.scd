// sample.scd — Prototyp: mehrere Klangobjekte, gleichmässig auf einem Kreis um den
// Hörer verteilt, spielen je ein Audiosample in festem Rhythmus. Eigenständiges Experiment
// (Intent 20) — unabhängig von insects.scd, nutzt aber dieselbe Spatial-Audio-
// Infrastruktur (Listener/Orchestra/SoundObject/Movable/Binauralizer).
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) boot/load_classes.scd — bindet SampleSound/SteadyMoveRule/AtkBinauralizer/Room/
//                          SoundObject/SpaceView in den Klassenpfad ein
//   2) boot/boot.scd   — Server muss laufen
//   3) ATK installiert (Quark + Kernel/Matrizen/Sounds, siehe README) — sonst schlägt
//      AtkBinauralizer.setup fehl
//
// Kopfhörer benutzen — binaurale Effekte (HRTF) funktionieren nicht über Lautsprecher richtig.
//
// Jedes Objekt bekommt dieselbe Trigger-Rate, aber einen über den Kreis verteilten
// Phasenversatz (0, 1/N, 2/N, ... einer Periode) — die Samples triggern dadurch nicht alle
// gleichzeitig, sondern wandern hörbar der Reihe nach um den Hörer herum.

// --- 1) Setup: SynthDefs registrieren + alle Soundobjekte anlegen ---
(
// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen (SuperCollider-Syntaxregel).
var soundsDir, sampleFiles, n, radius, rate;

SampleSound.addSynthDef;
RoomReverb.addSynthDef;
~room = Room.new(s);
// AtkBinauralizer lädt den HRTF-Kernel (asynchron) und registriert danach selbst die
// SynthDef (siehe AtkBinauralizer>>*setup) — etwas zeitlichen Abstand zu Block 2 lassen.
~room.addSynthDef(AtkBinauralizer, subjectID: 21);

// gültige Audiodateien aus sounds/ — rekursiv (filesDo statt entries.select), da die
// Samples in Unterordnern liegen (z.B. sounds/apple/, sounds/compmusic/); keine feste
// Dateiliste, damit das Demo mit den lokal vorhandenen Samples läuft.
soundsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "sounds");
sampleFiles = [];
soundsDir.filesDo({ |pe|
	if(BootTrackDetection.isValidExtension(pe.extension)) { sampleFiles = sampleFiles.add(pe) };
});
n = sampleFiles.size;
radius = 3;
rate = 0.1; // Hz — einmal alle 10s, bewusst langsamer als die Sample-Länge (~1s), da
          // SampleSound (Intent 20) noch kein Overlap-Handling hat

~sampleObjects = sampleFiles.collect({ |pe, i|
	var angle = (i / n) * 2pi;
	var pos = [radius * sin(angle), radius * cos(angle), 0];
	var phase = i / n;
	var movable = Movable.new(pos, SteadyMoveRule.new, roomRadius: radius + 2); // Marge, da die
		// Objekte exakt auf der Kreislinie sitzen (roomRadius nur fuer die organische
		// Rueckfuehrung bei bewegten Objekten relevant, siehe Movable>>step)
	var sound = SampleSound.new(pe.fullPath, rate, phase);
	sound.preload(s); // laedt den Buffer schon jetzt, statt erst bei room.play in
		// Block 2 — gibt der asynchronen Dateileseoperation Zeit (siehe SampleSound>>preload)
	// Binauralizer wird vom Room über dessen Listener gebaut (Room>>register,
	// Listener>>makeBinauralizer) — dieses Skript kennt AtkBinauralizer nicht mehr direkt.
	~room.register(movable, sound);
});

"% Sample(s) im Kreis: %\n".postf(n, sampleFiles.collect({ |pe| pe.fileNameWithoutExtension }));
)

// --- 2) Abspielen ---
// startet Orchestra vor RoomReverb in der richtigen Reihenfolge (siehe Room>>play).
~room.play;

// --- 3) Raum-Draufsicht öffnen (optional) ---
// Zeigt Listener im Zentrum und alle Sample-Objekte auf dem Kreis, von oben betrachtet.
~spaceView = SpaceView.new(~room.orchestra);
~spaceView.play;

~listenerControl = KeyboardListenerControl.new(~room.listener);
~listenerControl.play;

// --- 4) Stoppen ---
~room.stop;
AtkBinauralizer.teardown;
~spaceView.stop;
