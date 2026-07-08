// sample_moving.scd — Variante von sample.scd (Intent 20): dieselben rhythmischen
// Sample-Objekte, aber statt fest auf dem Kreis zu stehen, bewegen sie sich auf einer
// gemeinsamen Kreisbahn um den Hörer — gleicher Radius, gleiche Drehrichtung, aber
// unterschiedliches Tempo pro Objekt. Nutzt nur Samples aus sounds/compmusic/ (nicht
// sounds/apple/, siehe Intent 21).
//
// Vorher (in dieser Reihenfolge):
//   1) boot/load_classes.scd — nur beim Erst-Setup nötig (danach automatisch bei jedem Start,
//                          siehe Kommentar dort) — bindet SampleSound/CircularMoveRule/
//                          AtkBinauralizer/Room/SoundObject/SpaceView in den Klassenpfad ein
//   2) boot/boot.scd   — bei jedem Start: Server muss laufen
//   3) ATK installiert (Quark + Kernel/Matrizen/Sounds, siehe README) — sonst schlägt
//      AtkBinauralizer.setup fehl
//
// Kopfhörer benutzen — binaurale Effekte (HRTF) funktionieren nicht über Lautsprecher richtig.
//
// Alle Objekte starten gleichmässig verteilt auf demselben Kreis (startAngle) und drehen sich
// in dieselbe Richtung (angularSpeed hat für alle dasselbe Vorzeichen), aber mit
// unterschiedlichem Tempo — ihre Abstände zueinander verändern sich dadurch hörbar über die
// Zeit, statt dass die Formation starr bleibt. breathAmount = 0: der Radius bleibt für alle
// exakt gleich, kein "Atmen".

// --- 1) Setup: SynthDefs registrieren + alle Soundobjekte anlegen ---
(
// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen (SuperCollider-Syntaxregel).
var soundsDir, sampleFiles, n, radius, rate, minSpeed, maxSpeed;

SampleSound.addSynthDef;
RoomReverb.addSynthDef;
~room = Room.new(s);
// AtkBinauralizer lädt den HRTF-Kernel (asynchron) und registriert danach selbst die
// SynthDef (siehe AtkBinauralizer>>*setup) — etwas zeitlichen Abstand zu Block 2 lassen.
~room.addSynthDef(AtkBinauralizer, subjectID: 21);

// nur sounds/compmusic/ (nicht der ganze sounds/-Baum — auf Wunsch, apple/ bleibt aussen vor).
soundsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "sounds" +/+ "compmusic");
sampleFiles = [];
soundsDir.filesDo({ |pe|
	if(BootTrackDetection.isValidExtension(pe.extension)) { sampleFiles = sampleFiles.add(pe) };
});
n = sampleFiles.size;
radius = 3;
rate = 0.1; // Hz — einmal alle 10s, bewusst langsamer als die Sample-Länge, da SampleSound
            // (Intent 20) noch kein Overlap-Handling hat
minSpeed = 0.05; // rad/s — langsamstes Objekt: eine Umdrehung in ca. 126s
maxSpeed = 0.3;  // rad/s — schnellstes Objekt: eine Umdrehung in ca. 21s

~sampleObjects = sampleFiles.collect({ |pe, i|
	var startAngle = (i / n) * 2pi;
	// gleichmässig zwischen minSpeed und maxSpeed gestaffelt, alle positiv — gleiche
	// Drehrichtung für alle, nur das Tempo unterscheidet sich.
	var angularSpeed = minSpeed + (((n - 1).max(1).reciprocal * i) * (maxSpeed - minSpeed));
	var startPos = [radius * sin(startAngle), radius * cos(startAngle), 0];
	var moveRule = CircularMoveRule.new(baseRadius: radius, breathAmount: 0, breathRate: 0,
		angularSpeed: angularSpeed, startAngle: startAngle);
	var movable = Movable.new(startPos, moveRule, roomRadius: radius + 2); // Marge, s. sample.scd
	var phase = i / n;
	var sound = SampleSound.new(pe.fullPath, rate, phase);
	sound.preload(s); // laedt den Buffer schon jetzt, statt erst bei room.play in
		// Block 2 — gibt der asynchronen Dateileseoperation Zeit (siehe SampleSound>>preload)
	// Binauralizer wird vom Room über dessen Listener gebaut (Room>>register,
	// Listener>>makeBinauralizer) — dieses Skript kennt AtkBinauralizer nicht mehr direkt.
	~room.register(movable, sound);
});

"% Sample(s) auf gemeinsamer Kreisbahn: %\n".postf(n, sampleFiles.collect({ |pe| pe.fileNameWithoutExtension }));
)

// --- 2) Abspielen ---
// startet Orchestra vor RoomReverb in der richtigen Reihenfolge (siehe Room>>play).
~room.play;

// --- 3) Raum-Draufsicht öffnen (optional) ---
// Zeigt Listener im Zentrum und alle Sample-Objekte, die sich auf der Kreisbahn bewegen.
~spaceView = SpaceView.new(~room.orchestra);
~spaceView.play;

~listenerControl = KeyboardListenerControl.new(~room.listener);
~listenerControl.play;

// --- 4) Stoppen ---
~room.stop;
AtkBinauralizer.teardown;
~spaceView.stop;
