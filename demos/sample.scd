// sample.scd — Prototyp: mehrere Klangobjekte, gleichmässig auf einem Kreis um den
// Hörer verteilt, spielen je ein Audiosample in festem Rhythmus. Eigenständiges Experiment
// (Intent 20) — unabhängig von demos/grill/insects.scd, nutzt aber dieselbe Spatial-Audio-
// Infrastruktur (Listener/Orchestra/SoundObject/Movable/Binauralizer).
//
// Vorher (in dieser Reihenfolge):
//   1) boot/load_classes.scd — nur beim Erst-Setup nötig (danach automatisch bei jedem Start,
//                          siehe Kommentar dort) — bindet SampleSound/SteadyMoveRule/
//                          AtkBinauralizer/Room/SoundObject/SpatialControlPanel in den Klassenpfad ein
//   2) boot/boot.scd   — bei jedem Start: Server muss laufen
//   3) ATK installiert (Quark + Kernel/Matrizen/Sounds, siehe README) — sonst schlägt
//      AtkBinauralizer.setup fehl
//
// Kopfhörer benutzen — binaurale Effekte (HRTF) funktionieren nicht über Lautsprecher richtig.
//
// Jedes Objekt bekommt dieselbe Trigger-Rate, aber einen über den Kreis verteilten
// Phasenversatz (0, 1/N, 2/N, ... einer Periode) — die Samples triggern dadurch nicht alle
// gleichzeitig, sondern wandern hörbar der Reihe nach um den Hörer herum.
//
// Kurze Samples sollen vollstaendig bleiben, laengere automatisch auf einen relevanten
// Ausschnitt springen (Intent 53 Folgeausbau): gleiche Demo-Idee wie bisher, aber weniger
// "leere" Stellen in langen Phrasen/Rolls.

// --- 1) Setup: SynthDefs registrieren + alle Soundobjekte anlegen ---
(
// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen (SuperCollider-Syntaxregel).
var soundsDir, sampleFiles, n, radius, rate, maxSamples, shortThreshold, targetDuration,
	peakThreshold, previewBucketCount, baseAmp, targetRms, minGain, maxGain, silenceFloor,
	maxOutputPeak, configurator;

SampleSound.addSynthDef;
// Der Listener wird extern konfiguriert/instanziiert und dann dem Room übergeben (Intent 52)
// — Binauralisierung ist eine Eigenschaft seiner "Ohren", nicht des Room. Room verkabelt ihn
// bei der Konstruktion einmalig mit seinem Reverb-Bus (registriert dabei die passende
// SynthDef bzw. bei AtkBinauralizer den HRTF-Kernel, asynchron).
// Für echtes HRTF-Rendering stattdessen: ~listener.binauralizerClass = AtkBinauralizer;
~listener = Listener.new(binauralizerClass: Binauralizer);
~room = Room.new(s, listener: ~listener);

// gültige Audiodateien aus sounds/ — rekursiv (filesDo statt entries.select), da die
// Samples in Unterordnern liegen (z.B. sounds/apple/, sounds/compmusic/); keine feste
// Dateiliste, damit das Demo mit den lokal vorhandenen Samples läuft.
soundsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "sounds/instruments/Percussion");
sampleFiles = [];
soundsDir.filesDo({ |pe|
	if(BootTrackDetection.isValidExtension(pe.extension)) { sampleFiles = sampleFiles.add(pe) };
});
maxSamples = 30; // höchstens so viele Soundobjekte anlegen, auch wenn der Ordner mehr Dateien hat
sampleFiles = sampleFiles.keep(maxSamples);
n = sampleFiles.size;
radius = 3;
~viewRadius = radius + 1;
rate = 0.1; //0.1; // Hz — einmal alle 10s, bewusst langsamer als die Sample-Länge (~1s), da
          // SampleSound (Intent 20) noch kein Overlap-Handling hat
shortThreshold = 1.0;  // Sekunden — kurze One-Shots komplett spielen
targetDuration = 1.5;  // Sekunden — Zielausschnitt für längere Samples
peakThreshold = 0.25;  // 0..1 relativ zur normalisierten Peak-Huelle
previewBucketCount = 96;
baseAmp = 0.5;        // bisheriger SampleSound-Default
targetRms = 0.12;     // konservativerer Zielwert, damit dichte Samples feine Klaenge weniger uebertoenen
minGain = 0.5;        // keine drastische Absenkung einzelner Samples
maxGain = 1.4;        // weniger pauschales Hochziehen vieler Samples
silenceFloor = 0.0001; // fast stille Ausschnitte nicht kuenstlich aufblasen
maxOutputPeak = 0.6;   // Peak-Schutz gegen einzelne dominante Transienten
configurator = SampleSoundConfigurator.new(shortThreshold: shortThreshold,
	targetDuration: targetDuration, peakThreshold: peakThreshold,
	previewBucketCount: previewBucketCount, baseAmp: baseAmp, targetRms: targetRms,
	minGain: minGain, maxGain: maxGain, silenceFloor: silenceFloor,
	maxOutputPeak: maxOutputPeak);

~sampleObjects = sampleFiles.collect({ |pe, i|
	var angle = (i / n) * 2pi;
	var pos = [radius * sin(angle), radius * cos(angle), 0];
	var phase = i / n;
	var configured = configurator.prepare(pe.fullPath, rate, phase);
	var sound = configured[\sound];
	var suggestion = configured[\suggestion];
	var excerptLevel = configured[\excerptLevel];
	var movable = Movable.new(pos, SteadyMoveRule.new, roomRadius: radius + 2); // Marge, da die
		// Objekte exakt auf der Kreislinie sitzen (roomRadius nur fuer die organische
		// Rueckfuehrung bei bewegten Objekten relevant, siehe Movable>>step)
	sound.preload(s); // laedt den Buffer schon jetzt, statt erst bei room.play in
		// Block 2 — gibt der asynchronen Dateileseoperation Zeit (siehe SampleSound>>preload)
	// Binauralizer wird vom Room über dessen Listener gebaut (Room>>register,
	// Listener>>makeBinauralizer) — dieses Skript kennt AtkBinauralizer nicht mehr direkt.
	"% -> startFrac=%, duration=%, rms=%, peak=%, rmsGain=%, peakGain=%, gain=%, amp=%\n".postf(
		pe.fileName, sound.startFrac.round(0.001), sound.duration.round(0.001),
		(excerptLevel[\rms] ? 0.0).round(0.0001), (excerptLevel[\peak] ? 0.0).round(0.0001),
		configured[\rmsGain].round(0.001), configured[\peakSafetyGain].round(0.001),
		configured[\gainFactor].round(0.001),
		sound.amp.round(0.001));
	~room.register(movable, sound);
});

"% Sample(s) im Kreis (max %): %\n".postf(n, maxSamples,
	sampleFiles.collect({ |pe| pe.fileNameWithoutExtension }));
"Ausschnitt-Heuristik: shortThreshold=%, targetDuration=%, peakThreshold=%\n".postf(
	shortThreshold, targetDuration, peakThreshold);
"Lautheits-Heuristik: baseAmp=%, targetRms=%, minGain=%, maxGain=%, silenceFloor=%, maxOutputPeak=%\n".postf(
	baseAmp, targetRms, minGain, maxGain, silenceFloor, maxOutputPeak);
)

// --- 2) Abspielen ---
// startet Orchestra vor RoomReverb in der richtigen Reihenfolge (siehe Room>>play).
~room.play;

// --- 3) Gemeinsames Room-Control-Panel öffnen (optional) ---
// Zeigt Listener und alle Sample-Objekte in einem Fenster; W/S/A/D/Q/E steuern den Listener.
~presetsDir = PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "presets"
	+/+ "sample").fullPath;
// scenesDir: Ordner für ganze Raum-Szenen (Intent 46) -- gleiches Muster, eigener Unterordner.
~scenesDir = PathName(thisProcess.nowExecutingPath.dirname.dirname +/+ "presets"
	+/+ "sample" +/+ "rooms").fullPath;
~controlPanel = RoomControlPanel.new(~room, viewRadius: ~viewRadius, editable: true,
	presetsDir: ~presetsDir, scenesDir: ~scenesDir);
~controlPanel.show;

// --- 4) Stoppen ---
// room.teardown stoppt, gibt den Reverb-Bus frei UND delegiert an listener.teardown, das bei
// AtkBinauralizer den HRTF-Kernel freigibt (siehe Room>>teardown/Listener>>teardown, Intent 52).
~room.teardown;
~controlPanel.free;
