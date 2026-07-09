// one_insect.scd — Minimalbeispiel: ein einzelnes binaurales "Insekt" (CircularMoveRule-
// Defaults, Standard-Klang) kreist um den Hörer, der per Tastatur beweglich ist. Zum
// Nachvollziehen der Grundstruktur (Movable/InsectSound/Room/SoundObject) ohne die
// Mehrfach-Insekten-Komplexität aus insects.scd/three_insects.scd.
//
// Vorher (in dieser Reihenfolge):
//   1) boot/load_classes.scd — nur beim Erst-Setup nötig (danach automatisch bei jedem Start,
//                          siehe Kommentar dort) — bindet Movable/CircularMoveRule/
//                          InsectSound/Binauralizer/AtkBinauralizer/Room/SoundObject/
//                          RoomControlPanel in den Klassenpfad ein
//   2) boot/boot.scd   — bei jedem Start: Server muss laufen
//   3) ATK installiert (Quark + Kernel/Matrizen/Sounds, siehe README) — sonst schlägt
//      AtkBinauralizer.setup fehl
//
// Kopfhörer benutzen — binaurale Effekte (ITD/Panning/HRTF) funktionieren nicht über
// Lautsprecher richtig.

(
// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen, vor jeder
// anderen Anweisung (SuperCollider-Syntaxregel) — deshalb hier gesammelt,
// auch wenn sie erst weiter unten mit Werten befüllt werden.
var moveRule, movable, insectSound;


InsectSound.addSynthDef;
RoomReverb.addSynthDef;

// Room übernimmt Setup/Instanziierung/Teardown für den konfigurierten Binauralizer-Typ selbst
// (Intent 51) — AtkBinauralizer.setup lädt dabei die geteilte Encoder-Matrix/den HRTF-Decoder-
// Kernel, braucht deshalb den bereits gebooteten Server (s).

~room = Room.new(s);
~room.binauralizerClass = AtkBinauralizer;

// Insekt 1: CircularMoveRule-Defaults (2.5m Radius, ±1.2m Atmung, 0.6 rad/s
// im Uhrzeigersinn), Standard-Klang.
moveRule = CircularMoveRule.new;
movable = Movable.new([0, 2.5, 0], moveRule, roomRadius: 5);
insectSound = InsectSound.new;

~insect = ~room.register(movable, insectSound);
)

// --- 2) Abspielen ---
// room.play startet den Klang-/Binauralizer-Synth für jedes registrierte
// Soundobjekt, den gemeinsamen Taktgeber (Bewegung + Azimuth/Distanz) und danach den Hall.
~room.play;


// --- 3) Gemeinsames Room-Control-Panel öffnen ---
// Bewusst separat und erst NACH Block 2 ausführen (Room muss schon spielen).
// W/S vor/zurück, A/D seitlich, Q/E drehen; dieselbe Oberfläche zeigt Listener,
// Insekt und Room-/Reverb-Regler.
~controlPanel = RoomControlPanel.new(~room, viewRadius: 6, editable: true);
~controlPanel.show;

// --- 3b) Live-Coding: Insekt zurufen lassen ---
// Löst bei ~insect einen kurzen Klang-Akzent aus (Tonhöhe/Pegel blitzen kurz auf). Da hier nur
// ein Objekt registriert ist, gibt es kein Call-and-Response (siehe Orchestra>>chooseResponder
// — kein Rückruf möglich, wenn kein anderes Objekt registriert ist).
~room.call(~insect);

// --- 4) Stoppen ---
// room.stop stoppt das Ticken, ruft stop auf allen registrierten
// Soundobjekten auf (Synths/Busse werden freigegeben) und stoppt danach den Hall.
// room.teardown gibt zusätzlich den Reverb-Bus frei und delegiert an AtkBinauralizer.teardown
// (Server-Buffer des geteilten HRTF-Decoder-Kernels, siehe Room>>teardown, Intent 51).
~room.teardown;

// Room-Control-Panel separat freigeben (schließt das Kontrollfenster) — nur nötig,
// falls Block 3 überhaupt ausgeführt wurde.
~controlPanel.free;
