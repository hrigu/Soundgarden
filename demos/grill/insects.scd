// insects.scd — Prototyp: mehrere zufällig generierte, binaurale "Insekten" (Anzahl über
// num_insects einstellbar), alle mit derselben Zirp-Zykluslänge (~totalCycleDur), aber
// unterschiedlicher innerer Rhythmus-Aufteilung und konfigurierbarem Sound/Stille-Verhältnis
// (RhythmPatternCreator, Intent 33) — kreisen auf zufälligen Kreisbahnen um den Hörer, der
// per Tastatur beweglich ist. Für 3 fest benannte, individuell konfigurierte Insekten mit
// Call-and-Response siehe stattdessen demos/grill/three_insects.scd.
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
(
var num_insects;
var insectMovableBase, insectSoundBase;

InsectSound.addSynthDef;
// Der Listener wird extern konfiguriert/instanziiert und dann dem Room übergeben (Intent 52).
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener, size: 8, height: 3, surface: 1, mix: 1);


insectMovableBase = (pos: [0, 2.5, 0], moveRule: CircularMoveRule.new, roomRadius: 5);
insectSoundBase = (wingRate: 210, wingDuty: 0.25, ringFreq1: 3200, ringFreq2: 4600,
	ringDecay1: 0.02, ringDecay2: 0.015, amp: 0.35);


// n weitere Insekten rund um den Hörer verteilt, jedes eine leichte Variante derselben
// Basis-Konfiguration (Intent 29) plus ein eigenes, zufälliges CallingPattern (Intent 30) —
// alle mit derselben Gesamt-Zykluslänge (~totalCycleDur), aber unterschiedlicher Anzahl/
// Gewichtung der Segmente. Dadurch bleibt der Chor als Ganzes in einem gemeinsamen Takt, ohne
// dass alle Insekten identisch klingen.
~totalCycleDur = 30;
// Stellschrauben für RhythmPatternCreator (Intent 33): soundRatio steuert den Anteil der
// Zirp-Zeit an ~totalCycleDur (0.5 = hälftig, 0.1 = viel mehr Stille als Sound),
// startWithSound legt fest, ob der Zyklus mit einem Zirp- oder einem Stille-Segment beginnt.
~insectSoundRatio = 0.01;
~insectStartWithSound = true;
num_insects = 30;

~insects = num_insects.collect { |i|
	var angle = (i / 20) * 2pi;
	var radius = rrand(2.0, 4.0);
	var numSegments = [12, 14, 16, 18, 20, 22, 24, 26].choose;
	var soundParams = insectSoundBase ++ (
		wingRate: insectSoundBase.wingRate + rrand(-80, 100),
		ringFreq1: insectSoundBase.ringFreq1 + rrand(-800, 1000),
		amp: insectSoundBase.amp * rrand(0.5, 1.3),
		pattern: RhythmPatternCreator.new(
			totalDur: ~totalCycleDur,
			numSegments: numSegments,
			soundRatio: ~insectSoundRatio,
			startWithSound: ~insectStartWithSound
		).createNewPattern
	);
	var movableParams = insectMovableBase ++ (
		pos: [radius * cos(angle), radius * sin(angle), 0],
		moveRule: CircularMoveRule.new(baseRadius: radius, angularSpeed: rrand(0.3, 1.4)
			* [1, -1].choose)
	);
	~room.registerFromBuilder(soundParams, movableParams)
};

// --- 2) Abspielen ---

)


~room.play;

(
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true);
~controlPanel.show;
)


// --- 4) Stoppen ---
// room.stop stoppt das Ticken, ruft stop auf allen registrierten Soundobjekten auf
// (Synths/Busse werden freigegeben) und stoppt danach den Hall.
// room.teardown gibt zusätzlich den Reverb-Bus frei und delegiert an listener.teardown, das
// bei AtkBinauralizer den HRTF-Kernel freigibt (siehe Room>>teardown/Listener>>teardown,
// Intent 52).
~room.teardown;

// Tastatursteuerung separat stoppen (schließt das Kontrollfenster) — nur nötig,
// falls Block 3 überhaupt ausgeführt wurde.
~controlPanel.free;



//den Raum verändern
~room.size = 12;
~room.height = 3;
~room.surface = 1; // 0 = rau/absorbierend, 1 = glatt/hart
~room.mix = 0.2;
