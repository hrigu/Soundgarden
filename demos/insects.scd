// insects.scd — Prototyp: zwei binaurale "Insekten" kreisen um den Hörer, dazu ein
// stationäres ("steady") Soundobjekt, dessen Position live per Code verändert werden kann,
// und ein per Tastatur beweglicher Listener.
//
// Vorher (einmal pro Sitzung, in dieser Reihenfolge):
//   1) boot/load_classes.scd — bindet Movable/Listener/CircularMoveRule/SteadyMoveRule/InsectSound/
//                          Binauralizer/AtkBinauralizer/Room/SoundObject/
//                          KeyboardListenerControl/SpaceView in den Klassenpfad ein
//   2) boot/boot.scd   — Server muss laufen
//   3) ATK installiert (Quark + Kernel/Matrizen/Sounds, siehe README) — sonst schlägt
//      AtkBinauralizer.setup fehl
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
//
// Ein Room hat eine Ohren-Strategie (siehe Room>>addSynthDef, Listener>>makeBinauralizer) —
// alle drei Objekte hier hören mit AtkBinauralizer (echtes HRTF-Rendering über das Ambisonic
// Toolkit). Für einen Vergleich mit der einfachen Binauralizer-Variante (Core-UGens, kein
// echtes HRTF): unten `~room.addSynthDef(Binauralizer)` statt `AtkBinauralizer` verwenden und
// neu ausführen.

// --- 1) Setup: SynthDefs registrieren + alle Soundobjekte anlegen ---
// Ein Block statt zwei — SynthDef-Registrierung (/d_recv, asynchron) muss nicht
// abgeschlossen sein, bevor die Objekte hier unten angelegt werden (reines
// Zusammenstecken, keine Server-Kommunikation); sie muss erst abgeschlossen sein,
// bevor Block 2 (orchestra.play) Synths instanziert. Da Block 2 ein eigener,
// manuell ausgeführter Schritt ist, vergeht bis dahin ohnehin reale Zeit.
(
// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen, vor jeder
// anderen Anweisung (SuperCollider-Syntaxregel) — deshalb hier gesammelt,
// auch wenn sie erst weiter unten mit Werten befüllt werden.
var moveRule1, movable1, insectSound1;
var moveRule2, movable2, insectSound2;
var fountainMoveRule, fountainMovable, fountainSound;

// ~insect1 als Environment-Variable statt var: Block 3b (Call-and-Response) braucht eine
// Referenz darauf über den Block hinaus. ~movable2/~insectSound2/~fountainMovable/
// ~fountainSound ebenso — Block 2b/später live nachträglich registrierbar (siehe unten).

InsectSound.addSynthDef;
RoomReverb.addSynthDef;
~room = Room.new(s);
// AtkBinauralizer lädt den HRTF-Kernel (asynchron) und registriert danach selbst die SynthDef
// (siehe AtkBinauralizer>>*setup); legt zugleich fest, dass dieser Room ab jetzt mit
// AtkBinauralizer "hört" (siehe Room>>addSynthDef, Listener>>makeBinauralizer) — braucht
// deshalb den bereits gebooteten Server (s). Etwas mehr zeitlichen Abstand zu Block 2 lassen
// als bei einer normalen SynthDef-Registrierung: der Kernel lädt spürbar länger.
~room.addSynthDef(AtkBinauralizer, subjectID: 21);

// Ein gemeinsamer Listener (Position/Blickrichtung per Tastatur beweglich, siehe Block 3)
// hört alle Soundobjekte — Teil von ~room, siehe Room>>listener. Jedes Soundobjekt braucht
// aber sein eigenes Movable/Sound — die Binauralisierung hängt von der Position ab, die pro
// Objekt verschieden ist, und jeder Synth braucht seinen eigenen Bus/State. Room tickt alle
// registrierten Objekte über eine einzige Routine statt je einer eigenen — die Soundobjekte
// selbst kennen den Listener nicht, den Binauralizer bekommen sie von Room>>register über
// den Listener (siehe Kopfkommentar).

// Insekt 1: CircularMoveRule-Defaults (2.5m Radius, ±1.2m Atmung, 0.6 rad/s im Uhrzeigersinn),
// Standard-Klang.
moveRule1 = CircularMoveRule.new;
movable1 = Movable.new([0, 2.5, 0], moveRule1, roomRadius: 5);
insectSound1 = InsectSound.new;

// Insekt 2: etwas schneller (1.0 statt 0.6 rad/s) und in Gegenrichtung
// (negatives angularSpeed); heller/schneller Klang (höhere Flügelschlag- und
// Resonanzfrequenzen, kürzeres Ausklingen) statt Standard-Brummen.
moveRule2 = CircularMoveRule.new(baseRadius: 3.0, breathAmount: 1.0, breathRate: 0.15,
	angularSpeed: -1.0);
movable2 = Movable.new([0, -3.0, 0], moveRule2, roomRadius: 5);
insectSound2 = InsectSound.new(wingRate: 280, wingDuty: 0.2, ringFreq1: 5200, ringFreq2: 7000,
	ringDecay1: 0.015, ringDecay2: 0.01, amp: 0.3);

~insect1 = ~room.register(movable1, insectSound1);

// "Brunnen": steady Soundobjekt — SteadyMoveRule bewegt sich nicht von selbst
// (next() gibt die Position unverändert zurück), tiefer/langsamer Klang zur
// Unterscheidung von den Insekten. Position per Live-Coding änderbar, siehe
// Block 2b weiter unten (~fountainMovable.moveTo(...)).
fountainMoveRule = SteadyMoveRule.new;
fountainMovable = Movable.new([2, 1, 0], fountainMoveRule, roomRadius: 5);
fountainSound = InsectSound.new(wingRate: 60, wingDuty: 0.6, ringFreq1: 800, ringFreq2: 1400,
	ringDecay1: 0.08, ringDecay2: 0.06, amp: 0.05);

~insect2 = ~room.register(movable2, insectSound2);
~fountain = ~room.register(fountainMovable, fountainSound);
)
(
// --- 3) Tastatursteuerung für den Listener starten ---
// Bewusst separat und erst NACH Block 2 ausführen (Orchester muss schon spielen).
// Öffnet ein kleines Fenster, das während der Steuerung fokussiert sein muss
// (SuperCollider-Standardidiom für Tastatur-Input). Tasten halten:
// W/S vor/zurück, A/D seitlich, Q/E drehen.
~listenerControl = KeyboardListenerControl.new(~room.listener);
~listenerControl.play;

// --- 3c) Raum-Draufsicht öffnen ---
// Live aktualisiertes Fenster: Listener (Position + Blickrichtung als Pfeil) und
// alle registrierten Soundobjekte, von oben betrachtet. Bewegt selbst nichts —
// liest bei jedem Neuzeichnen einfach den aktuellen Zustand von ~room.orchestra.
~spaceView = SpaceView.new(~room.orchestra);
~spaceView.play;
)

// --- 2) Abspielen ---
// room.play startet den Klang-/Binauralizer-Synth für jedes registrierte Soundobjekt, den
// gemeinsamen Taktgeber (Bewegung + Azimuth/Distanz) und danach den Hall — in der richtigen
// Reihenfolge (siehe Room>>play).
~room.play;

// --- 2b) Live-Coding: Brunnen umplatzieren ---
// Position jederzeit während des Sets änderbar — moveTo setzt sie direkt,
// ohne die roomRadius-Rückführung, die für die Insekten-Eigenbewegung gedacht ist.
~fountainMovable.moveTo([-3, 2, 0]);

// --- 3b) Live-Coding: Insekt 1 zurufen lassen ---
// Löst bei insect1 einen kurzen Klang-Akzent aus (Tonhöhe/Pegel blitzen kurz
// auf); nach kurzer, leicht zufälliger Verzögerung ruft ein zufälliges anderes
// registriertes Objekt (insect2 oder fountain) zurück (Call-and-Response).
~room.call(~insect1);

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
~room.surface = 0.4; // 0 = rau/absorbierend, 1 = glatt/hart
~room.mix = 1;