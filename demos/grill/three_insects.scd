// three_insects.scd — Prototyp: 3 fest benannte, binaurale "Insekten" (Insekt 2/3 als
// individuell konfigurierte Varianten von Insekt 1 per Event-Merge, Intent 29) mit eigenem
// CallingPattern-Zirp-Rhythmus (Intent 30/31) kreisen um den Hörer, der per Tastatur beweglich
// ist. Für viele zufällig generierte Insekten mit konfigurierbarem Sound/Stille-Verhältnis
// siehe stattdessen demos/grill/insects.scd.
//
// Vorher (in dieser Reihenfolge):
//   1) boot/load_classes.scd — nur beim Erst-Setup nötig (danach automatisch bei jedem Start,
//                          siehe Kommentar dort) — bindet Movable/Listener/CircularMoveRule/
//                          InsectSound/AtkBinauralizer/Room/SoundObject/CallingPattern/
//                          SpatialControlPanel in den Klassenpfad ein
//   2) boot/boot.scd   — bei jedem Start: Server muss laufen
//   3) ATK installiert (Quark + Kernel/Matrizen/Sounds, siehe README) — sonst schlägt
//      AtkBinauralizer.setup fehl
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.
//
// Der Listener hat eine Ohren-Strategie (siehe Listener>>binauralizerClass/makeBinauralizer,
// Intent 52) — alle drei Objekte hier hören mit AtkBinauralizer (echtes HRTF-Rendering über
// das Ambisonic Toolkit). Für einen Vergleich mit der einfachen Binauralizer-Variante
// (Core-UGens, kein echtes HRTF): unten `binauralizerClass: Binauralizer` statt
// `AtkBinauralizer` verwenden und neu ausführen.

// --- 1) Setup: SynthDefs registrieren + alle Soundobjekte anlegen ---
// Ein Block statt zwei — SynthDef-Registrierung (/d_recv, asynchron) muss nicht
// abgeschlossen sein, bevor die Objekte hier unten angelegt werden (reines
// Zusammenstecken, keine Server-Kommunikation); sie muss erst abgeschlossen sein,
// bevor Block 2 (room.play) Synths instanziert. Da Block 2 ein eigener,
// manuell ausgeführter Schritt ist, vergeht bis dahin ohnehin reale Zeit.
(
// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen, vor jeder
// anderen Anweisung (SuperCollider-Syntaxregel) — deshalb hier gesammelt,
// auch wenn sie erst weiter unten mit Werten befüllt werden.
var insectMovableBase, insectSoundBase;
var rhythmPatternBase, rhythmPatternBase2, rhythmPatternBase3;

// ~insect1 als Environment-Variable statt var: Block 3b (Call-and-Response) braucht eine
// Referenz darauf über den Block hinaus. ~insect2/~insect3 ebenso, für spätere Referenz/
// Live-Coding-Zugriff.

InsectSound.addSynthDef;
// Der Listener wird extern konfiguriert/instanziiert und dann dem Room übergeben (Intent 52)
// — Binauralisierung ist eine Eigenschaft seiner "Ohren", nicht des Room. Hier lädt das den
// HRTF-Kernel asynchron und registriert danach selbst die SynthDef (siehe
// AtkBinauralizer>>*setup); Room verkabelt den Listener bei der Konstruktion mit seinem
// Reverb-Bus (siehe Room>>init, Listener>>setup) — braucht deshalb den bereits gebooteten
// Server (s). Etwas mehr zeitlichen Abstand zu Block 2 lassen als bei einer normalen
// SynthDef-Registrierung: der Kernel lädt spürbar länger.
~listener = Listener.new(binauralizerClass: AtkBinauralizer);
~room = Room.new(s, listener: ~listener);

// Ein gemeinsamer Listener (Position/Blickrichtung per Tastatur beweglich, siehe Block 3)
// hört alle Soundobjekte — Teil von ~room, siehe Room>>listener. Jedes Soundobjekt braucht
// aber sein eigenes Movable/Sound — die Binauralisierung hängt von der Position ab, die pro
// Objekt verschieden ist, und jeder Synth braucht seinen eigenen Bus/State. Room tickt alle
// registrierten Objekte über eine einzige Routine statt je einer eigenen — die Soundobjekte
// selbst kennen den Listener nicht, den Binauralizer bekommen sie von Room>>register über
// den Listener (siehe Kopfkommentar).

// Insekt 1: CircularMoveRule-Defaults (2.5m Radius, ±1.2m Atmung, 0.6 rad/s im Uhrzeigersinn),
// Standard-Klang. Als Basis-Konfiguration explizit als Event formuliert (Werte entsprechen
// InsectSound/Movable-Defaults), damit Insekt 2 unten per Event-Merge (++) nur die
// tatsächlichen Abweichungen angeben muss, statt alle Parameter erneut vollständig
// aufzuzählen (Intent 29).
insectMovableBase = (pos: [0, 2.5, 0], moveRule: CircularMoveRule.new, roomRadius: 5);
insectSoundBase = (wingRate: 210, wingDuty: 0.25, ringFreq1: 3200, ringFreq2: 4600,
	ringDecay1: 0.02, ringDecay2: 0.015, amp: 0.35);

rhythmPatternBase = CallingPattern.fromDurations([0.02, 0.3, 0.02, 1.7, 0.02, 0.3, 0.2, 0.9]);
rhythmPatternBase2 = CallingPattern.fromDurations([1.9, 0.02, 0.5, 0.02, 0.3, 0.02, 0.3, 0.2]);
rhythmPatternBase3 = CallingPattern.fromDurations([0.02, 0.3, 0.02, 0.9, 0.02, 1.8, 0.2, 0.3]);

// Insekt 1 bekommt zusätzlich ein CallingPattern (Intent 30) statt durchgehend zu zirpen:
// Zahlensatz aus Intent 31 (Bioakustik-Recherche zu Gryllus bimaculatus) — 4 Pulse à 20ms an/
// 30ms aus, die letzte Pulspause verlängert zur Interchirp-Pause (0.2s) statt eines
// zusätzlichen Segments — dadurch wechselt die Liste strikt an/aus/an/aus/... ab und
// fromDurations reicht (kein new([[dauer, istAn], ...]) nötig). Chirpdauer (4×0.02 + 3×0.03 =
// 0.17s) trifft den sourced-Bereich ~160-171ms fast exakt. Eigene CallingPattern-Instanz statt
// in insectSoundBase — ein CallingPattern führt intern Zustand (aktuelle Segment-Position),
// eine gemeinsame Instanz über mehrere Insekten hinweg (z.B. via insectSoundBase ++ ...) würde
// von mehreren Routinen gleichzeitig verändert und liefe aus dem Takt.
~insect1 = ~room.registerFromBuilder(
	insectSoundBase ++ (pattern: rhythmPatternBase),
	insectMovableBase);

// Insekt 2: Variante von Insekt 1 — etwas schneller (1.0 statt 0.6 rad/s) und in
// Gegenrichtung (negatives angularSpeed) unterwegs, mit hellerem/schnellerem Klang (höhere
// Flügelschlag- und Resonanzfrequenzen, kürzeres Ausklingen) statt Standard-Brummen. Nur die
// Abweichungen von der Basis werden angegeben, ++ übernimmt den Rest unverändert (z.B.
// roomRadius: 5).
~insect2 = ~room.registerFromBuilder(
	insectSoundBase ++ (wingRate: 280, wingDuty: 0.2, ringFreq1: 5200, ringFreq2: 7000,
		ringDecay1: 0.015, ringDecay2: 0.01, amp: 0.3) ++ (pattern: rhythmPatternBase2),
	insectMovableBase ++ (pos: [0, -3.0, 0], moveRule: CircularMoveRule.new(baseRadius: 3.0,
		breathAmount: 1.0, breathRate: 0.15, angularSpeed: -1.0)));


~insect3 = ~room.registerFromBuilder(
	insectSoundBase ++ (wingRate: 100, wingDuty: 0.6) ++ (pattern: rhythmPatternBase3),
	insectMovableBase ++ (pos: [0, 3.0, 0], moveRule: CircularMoveRule.new(baseRadius: 3.0,
		breathAmount: 1.0, breathRate: 0.35, angularSpeed: -1.0)));

)
(
// --- 3) Gemeinsames Room-Control-Panel ---
// Bewusst separat und erst NACH Block 2 ausführen (Room muss schon spielen).
// W/S vor/zurück, A/D seitlich, Q/E drehen; dazu dieselbe Oberfläche für
// Draufsicht und Room-/Reverb-Regler.
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true);
~controlPanel.show;
)

// --- 2) Abspielen ---
// room.play startet den Klang-/Binauralizer-Synth für jedes registrierte Soundobjekt, den
// gemeinsamen Taktgeber (Bewegung + Azimuth/Distanz) und danach den Hall — in der richtigen
// Reihenfolge (siehe Room>>play).
~room.play;

// --- 3b) Live-Coding: Insekt 1 zurufen lassen ---
// Löst bei insect1 einen kurzen Klang-Akzent aus (Tonhöhe/Pegel blitzen kurz
// auf); nach kurzer, leicht zufälliger Verzögerung ruft ein zufälliges anderes
// registriertes Objekt (insect2 oder insect3) zurück (Call-and-Response).
~room.call(~insect1);

// --- 4) Stoppen ---
// room.stop stoppt das Ticken, ruft stop auf allen registrierten Soundobjekten auf
// (Synths/Busse werden freigegeben) und stoppt danach den Hall.
// room.teardown gibt zusätzlich den Reverb-Bus frei und delegiert an listener.teardown, das
// bei AtkBinauralizer den HRTF-Kernel freigibt (siehe Room>>teardown/Listener>>teardown,
// Intent 52).
~room.teardown;

~controlPanel.free;



//den Raum verändern
~room.size = 12;
~room.height = 3;
~room.surface = 1; // 0 = rau/absorbierend, 1 = glatt/hart
~room.mix = 0.2;
