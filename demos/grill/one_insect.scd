//simple demo


(
// Alle var-Deklarationen müssen ganz am Anfang des Blocks stehen, vor jeder
// anderen Anweisung (SuperCollider-Syntaxregel) — deshalb hier gesammelt,
// auch wenn sie erst weiter unten mit Werten befüllt werden.
var moveRule, movable, insectSound, atk_binauralizer, simple_binauralizer;


InsectSound.addSynthDef;
Binauralizer.addSynthDef;

// AtkBinauralizer.setup MUSS vor addSynthDef abgeschlossen sein (legt die geteilte
// Encoder-Matrix/den HRTF-Decoder-Kernel an, die die SynthDef referenziert) — braucht
// deshalb den bereits gebooteten Server (s), anders als die übrigen addSynthDef-Aufrufe hier.

AtkBinauralizer.setup(s);

// Ein gemeinsamer Listener (Position/Blickrichtung per Tastatur beweglich,
// siehe Block 3) hört alle Soundobjekte. Jedes Soundobjekt braucht aber sein
// eigenes Movable/Sound/Binauralizer — die Binauralisierung hängt von der
// Position ab, die pro Objekt verschieden ist, und jeder Synth braucht seinen
// eigenen Bus/State. Ein gemeinsames Orchestra hält den Listener und tickt
// alle Objekte über eine einzige Routine statt je einer eigenen — die
// Soundobjekte selbst kennen den Listener nicht.
~listener = Listener.new; // Environment-Variable statt var: Block 3 (Tastatursteuerung)
                          // braucht eine Referenz darauf über den Block hinaus.

// Insekt 1: CircularMoveRule-Defaults (2.5m Radius, ±1.2m Atmung, 0.6 rad/s
// im Uhrzeigersinn), Standard-Klang. AtkBinauralizer statt Binauralizer — echtes
// HRTF, zum direkten Vergleich mit Insekt 2/Brunnen (siehe Kopfkommentar).
moveRule = CircularMoveRule.new;
movable = Movable.new([0, 2.5, 0], moveRule, roomRadius: 5);
insectSound = InsectSound.new;

atk_binauralizer = AtkBinauralizer.new;
simple_binauralizer = Binauralizer.new;

~insect = SoundObject.new(movable, insectSound, atk_binauralizer);

~orchestra = Orchestra.new(~listener);
~orchestra.register(~insect);
)

// --- 2) Abspielen ---
// orchestra.play startet den Klang-/Binauralizer-Synth für jedes registrierte
// Soundobjekt und danach den gemeinsamen Taktgeber (Bewegung + Azimuth/Distanz).
~orchestra.play(s);


// --- 3) Tastatursteuerung für den Listener starten ---
// Bewusst separat und erst NACH Block 2 ausführen (Orchester muss schon spielen).
// Öffnet ein kleines Fenster, das während der Steuerung fokussiert sein muss
// (SuperCollider-Standardidiom für Tastatur-Input). Tasten halten:
// W/S vor/zurück, A/D seitlich, Q/E drehen.
~listenerControl = KeyboardListenerControl.new(~listener);
~listenerControl.play;

// --- 3b) Live-Coding: Insekt 1 zurufen lassen ---
// Löst bei insect1 einen kurzen Klang-Akzent aus (Tonhöhe/Pegel blitzen kurz
// auf); nach kurzer, leicht zufälliger Verzögerung ruft ein zufälliges anderes
// registriertes Objekt (insect2 oder fountain) zurück (Call-and-Response).
~orchestra.call(~insect);

// --- 3c) Raum-Draufsicht öffnen ---
// Live aktualisiertes Fenster: Listener (Position + Blickrichtung als Pfeil) und
// alle registrierten Soundobjekte, von oben betrachtet. Bewegt selbst nichts —
// liest bei jedem Neuzeichnen einfach den aktuellen Zustand von ~orchestra.
~spaceView = SpaceView.new(~orchestra);
~spaceView.play;

// --- 4) Stoppen ---
// orchestra.stop stoppt das Ticken UND ruft stop auf allen registrierten
// Soundobjekten auf (Synths/Busse werden freigegeben).
~orchestra.stop;

// AtkBinauralizer.teardown gibt den geteilten HRTF-Decoder-Kernel frei (Server-Buffer) —
// separat, weil der Kernel der Klasse gehört, nicht einer einzelnen Instanz/orchestra.stop.
AtkBinauralizer.teardown;

// Tastatursteuerung separat stoppen (schließt das Kontrollfenster) — nur nötig,
// falls Block 3 überhaupt ausgeführt wurde.
~listenerControl.stop;

// Raum-Draufsicht separat stoppen (schließt das Fenster) — nur nötig, falls
// Block 3c überhaupt ausgeführt wurde.
~spaceView.stop;
