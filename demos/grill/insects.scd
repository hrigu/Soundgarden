// insects.scd — Prototyp: mehrere binaurale "Insekten" (3 fest benannte + 20 generierte, alle
// mit demselben Zirp-Zykluslänge, aber unterschiedlicher innerer Rhythmus-Aufteilung) kreisen
// um den Hörer, dazu ein stationäres ("steady") Soundobjekt, dessen Position live per Code
// verändert werden kann, und ein per Tastatur beweglicher Listener.
(
var num_insects;
var insectMovableBase, insectSoundBase;

InsectSound.addSynthDef;
RoomReverb.addSynthDef;
~room = Room.new(s);
~room.addSynthDef(AtkBinauralizer, subjectID: 21);


insectMovableBase = (pos: [0, 2.5, 0], moveRule: CircularMoveRule.new, roomRadius: 5);
insectSoundBase = (wingRate: 210, wingDuty: 0.25, ringFreq1: 3200, ringFreq2: 4600,
	ringDecay1: 0.02, ringDecay2: 0.015, amp: 0.35);


// n weitere Insekten rund um den Hörer verteilt, jedes eine leichte Variante derselben
// Basis-Konfiguration (Intent 29) plus ein eigenes, zufälliges CallingPattern (Intent 30) —
// alle mit derselben Gesamt-Zykluslänge (~totalCycleDur), aber unterschiedlicher Anzahl/
// Gewichtung der Segmente. Dadurch bleibt der Chor als Ganzes in einem gemeinsamen Takt, ohne
// dass alle Insekten identisch klingen.
~totalCycleDur = 3;
// Stellschrauben für RhythmPatternCreator (Intent 33): soundRatio steuert den Anteil der
// Zirp-Zeit an ~totalCycleDur (0.5 = hälftig, 0.1 = viel mehr Stille als Sound),
// startWithSound legt fest, ob der Zyklus mit einem Zirp- oder einem Stille-Segment beginnt.
~insectSoundRatio = 0.5;
~insectStartWithSound = true;
num_insects = 3;

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
~room.play;

)

(
~listenerControl = KeyboardListenerControl.new(~room.listener);
~listenerControl.play;

~spaceView = SpaceView.new(~room.orchestra);
~spaceView.play;
)


// --- 4) Stoppen ---
// room.stop stoppt das Ticken, ruft stop auf allen registrierten Soundobjekten auf
// (Synths/Busse werden freigegeben) und stoppt danach den Hall.
~room.stop;

// AtkBinauralizer.teardown gibt den geteilten HRTF-Decoder-Kernel frei (Server-Buffer) —
// separat, weil der Kernel der Klasse gehört, nicht einer einzelnen Instanz/room.stop.
AtkBinauralizer.teardown;

// Tastatursteuerung separat stoppen (schließt das Kontrollfenster) — nur nötig,
// falls Block 3 überhaupt ausgeführt wurde.
~listenerControl.stop;

// Raum-Draufsicht separat stoppen (schließt das Fenster) — nur nötig, falls
// Block 3c überhaupt ausgeführt wurde.
~spaceView.stop;



//den Raum verändern
~room.size = 12;
~room.height = 3;
~room.surface = 1; // 0 = rau/absorbierend, 1 = glatt/hart
~room.mix = 0.2;