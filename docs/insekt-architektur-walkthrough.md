# Code-Walkthrough: Insekt-Architektur (nach Intent 9)

Notizen aus einem gemeinsamen, interaktiven Code-Review nach Intent 9
(`.intents/completed/9.enhancement_spatial-audio_insekt-architektur-aufteilen.md`).
Reihenfolge folgt dem tatsächlichen Datenfluss: `insect_demo.scd` → `MoveRule`/
`CircularMoveRule` → `Movable` → `InsectSound` → `Binauralizer` → `SoundObject` →
`Orchestra` (zentraler Taktgeber, seit Intent 12 — tickt beliebig viele registrierte
`SoundObject`s über eine gemeinsame Routine statt je einer eigenen, und hält dafür den
`Listener`, den `SoundObject` selbst nicht mehr kennt).

## insect_demo.scd — Übersicht

**Block 1 — SynthDefs registrieren**

```supercollider
InsectSound.addSynthDef;
Binauralizer.addSynthDef;
```

`.addSynthDef` ist eine Klassenmethode (`*addSynthDef`), die die eigentliche
`SynthDef` baut und per `.add` an den Server schickt (`/d_recv`, asynchron).
Steht bewusst als eigener, separat auszuführender Block da — genau wie im
Original-Prototyp —, damit zwischen "Server kennt die SynthDef" und "wir
instanzieren einen Synth davon" echte Zeit vergeht.

**Block 2 — Objekte anlegen (reines Zusammenstecken, keine Server-Kommunikation)**

```supercollider
~moveRule = CircularMoveRule.new;
~movable = Movable.new([0, 2.5, 0], ~moveRule, 5);
~listener = Listener.new;
~insectSound = InsectSound.new;
~binauralizer = Binauralizer.new;
~insect = SoundObject.new(~movable, ~insectSound, ~binauralizer);

~orchestra = Orchestra.new(~listener);
~orchestra.register(~insect);
```

Vier unabhängige Objekte für vier Verantwortlichkeiten: **wo** (Movable +
seine Regel), **wie es hört** (Listener), **wie es klingt** (InsectSound),
**wie es räumlich geformt wird** (Binauralizer). Seit Intent 12 bekommt
`SoundObject` aber nur noch drei davon injiziert — Movable, InsectSound,
Binauralizer — und erzeugt sie nicht selbst (außer als Default), sondern
verdrahtet sie nur. Den `Listener` kennt es nicht mehr; den hält stattdessen
`Orchestra`, bei der sich `SoundObject` per `register` anmeldet.

**Block 3 — Abspielen**

```supercollider
~orchestra.play(s);
```

Ein Aufruf reicht: `Orchestra` startet zuerst für jedes registrierte `SoundObject`
dessen Sound-/Binauralizer-Synth (Bus-Verkabelung, Node-Reihenfolge — Details dazu bei
`SoundObject`), danach die tickende Routine, die für jedes registrierte SoundObject
Bewegung und Azimuth/Distanz zum `Listener` berechnet — Details dazu bei `Orchestra`.

## Fragen & Antworten

### Was bedeutet das `~` vor jeder Variable?

`~name` ist eine **Environment-Variable** — im Gegensatz zu `var`-deklarierten
lokalen Variablen, die nur innerhalb ihres `(...)`-Blocks leben, lebt `~name`
im aktuellen `Environment`-Objekt (praktisch ein globales Dictionary für die
laufende sclang-Sitzung).

Warum das hier wichtig ist: Block 2 und Block 3 in `insect_demo.scd` sind
zwei **getrennte** Cmd+Enter-Ausführungen. Eine `var`-Deklaration in Block 2
(`var movable = ...`) würde beim Ende dieses Blocks aus dem Scope fallen —
Block 3 könnte gar nicht mehr darauf zugreifen. `~movable` dagegen bleibt im
Environment bestehen, bis die Sitzung endet oder es explizit überschrieben
wird.

Technisch ist `~foo = x` ungefähr Zucker für `currentEnvironment.put(\foo,
x)`, und `~foo` liest es wieder aus. Keine Deklaration nötig — die erste
Zuweisung legt den Eintrag an.

Kontrast: `s` (in `s = Server.default` aus `boot.scd`) ist keine
Environment-Variable, sondern eine der 26 reservierten
Interpreter-Variablen (`a`–`z`) — anderer Mechanismus, aber gleicher Zweck:
über Blöcke hinweg persistent, im Gegensatz zu `var`.

Innerhalb der Klassen selbst (`Movable`, `SoundObject`, ...) stehen dagegen
echte Instanzvariablen (`var <>pos` etc.) — kein Environment-Bezug.

## MoveRule — die Basisklasse

```supercollider
MoveRule {
	next { |pos, t, dt|
		^this.subclassResponsibility(thisMethod)
	}
}
```

Reine Schnittstelle, keine Funktionalität. `subclassResponsibility` ist
SuperCollider-Standardidiom für "abstrakte Methode": Wird `next` direkt auf
einer `MoveRule`-Instanz (statt auf einer Subklasse) aufgerufen, wirft es
einen klaren Fehler statt still falsch zu funktionieren. Vorher war die
Bewegungsregel eine rohe `Function`, die `Movable` per `.value(pos, t, dt)`
aufgerufen hat — jetzt ist es ein Objekt mit derselben Signatur, aber als
Methode `next(pos, t, dt)`. Vorteil: eine Regel kann jetzt eigenen
Zustand/Parameter tragen (siehe `CircularMoveRule`), was eine anonyme
Function nicht so sauber könnte.

Auf Wunsch direkt im Code dokumentiert (`classes/sg/soundobjects/MoveRule.sc`):

```supercollider
// next — von Subklassen zu implementieren.
// pos: aktuelle Position [x, y, z]
// t:   seit Bewegungsbeginn verstrichene Zeit in Sekunden (kumulativ)
// dt:  Zeitschritt seit dem letzten Aufruf in Sekunden
// -> neue Position [x, y, z]
```

## CircularMoveRule — die erste konkrete Regel

```supercollider
CircularMoveRule : MoveRule {
	var <>baseRadius;
	var <>breathAmount;
	var <>breathRate;
	var <>angularSpeed;

	*new { |baseRadius = 2.5, breathAmount = 1.2, breathRate = 0.15, angularSpeed = 0.6|
		^super.new.init(baseRadius, breathAmount, breathRate, angularSpeed);
	}
	...
	next { |pos, t, dt|
		var radius = baseRadius + (breathAmount * sin(t * breathRate));
		var angle = t * angularSpeed;
		^[radius * sin(angle), radius * cos(angle), 0]
	}
}
```

`: MoveRule` ist die Vererbung — `CircularMoveRule` erbt die (leere)
Schnittstelle und überschreibt `next` konkret. Die vier Parameter
(`baseRadius`, `breathAmount`, `breathRate`, `angularSpeed`) sind exakt das,
was vorher als Zahlen fest in der `~circleRule`-Function aus
`insect_demo.scd` stand (2.5 / 1.2 / 0.15 / 0.6) — jetzt benannte,
überschreibbare Defaults. `pos` wird hier gar nicht benutzt — die Kreisbahn
ist rein zeitgesteuert (`t`), nicht positionsabhängig. Das ist okay, weil die
Schnittstelle `next(pos, t, dt)` generisch genug für Regeln sein muss, die
*doch* von der aktuellen Position abhängen (z.B. eine Fluchtregel, die von
einer anderen Position weg steuert).

`<>` bei den Vars erzeugt automatisch Getter *und* Setter (`aRule.baseRadius
= 3.0` funktioniert also live).

## Movable — Position + Bewegungsregel + sanfte Raumgrenze

```supercollider
Movable {
	var <>pos;        // [x, y, z]
	var <>moveRule;   // MoveRule-Objekt: next(pos, t, dt) -> neue [x, y, z]
	var <>roomRadius;
	var <>time;

	step { |dt = 0.033|
		var newPos, dist, targetScale, scale, pullback = 0.2;

		time = time + dt;
		newPos = moveRule.next(pos, time, dt);

		dist = (newPos.pow(2).sum).sqrt;
		if(dist > roomRadius) {
			targetScale = roomRadius / dist;
			scale = 1 + ((targetScale - 1) * pullback);
			newPos = newPos * scale;
		};

		pos = newPos;
		^pos
	}
}
```

Die "physikalische" Seite: ein kumulativer `time`-Zähler (startet bei 0), die
eigentliche Bewegungsberechnung ist komplett an `moveRule.next(pos, time,
dt)` delegiert. Zusätzlich übernimmt `Movable` etwas, das `MoveRule` bewusst
*nicht* wissen muss: die Raumgrenze.

Die "sanfte Rückführung":
- `dist = (newPos.pow(2).sum).sqrt` — euklidische Distanz vom Ursprung.
- Bei `dist > roomRadius` würde hartes Clamping (`newPos * (roomRadius/dist)`)
  einen Sprung/Knick in der Bahn erzeugen — hörbar als Klick.
- Stattdessen: `pullback = 0.2` heißt "nur 20% des Wegs zurück zur
  Kugeloberfläche, nicht 100%". `targetScale` wäre der volle Rückzugsfaktor
  (`roomRadius/dist`, <1), `scale` interpoliert nur 20% dorthin. Bei
  `CircularMoveRule`s Defaults (Radius bis 3.7, `roomRadius=5`) greift das im
  Demo aktuell nie — reine Absicherung für Regeln, die weiter ausschlagen.

Kein Bezug zu Klang oder Listener — reine Geometrie/Kinematik. Auf Wunsch im
Code dokumentiert (`classes/sg/soundobjects/Movable.sc`): `step()` hat jetzt einen
Doc-Kommentar zu `dt`, Rückgabewert/Seiteneffekt, und `pullback` ist erklärt.

**Notiz für später** (in README → "Nächste Schritte / Ideen" eingetragen):
`Movable` ist trotz Seiteneffekt gut per `UnitTest` testbar — `MoveRule` ist
injizierbar (Fake-Regel mit fester Position weit außerhalb `roomRadius`), und
die Rückführungslogik ist reine Mathematik ohne Server-Bezug. Bisher gibt es
dafür noch keinen Test (nur indirekt über `CircularMoveRule`-Tests + Hörtest).

## InsectSound — der reine Klang

```supercollider
InsectSound {
	var <>wingRate;
	var <>wingDuty;
	var <>ringFreq1;
	var <>ringFreq2;
	var <>ringDecay1;
	var <>ringDecay2;
	var <>amp;
	var <bus;
	var <synth;
	...
}
```

Gleiches Muster wie bei `CircularMoveRule`: `*new` mit Defaults, `init` setzt
die Instanzvars. `var <bus` und `var <synth` haben nur `<` (Getter, kein
Setter) — von außen soll niemand einfach `insectSound.bus = irgendwas`
setzen, das ist internes, vom Objekt selbst verwaltetes Zeug. Nur *lesbar*,
weil `SoundObject` gleich `insectSound.bus`/`insectSound.synth` braucht, um
den `Binauralizer` zu verkabeln.

**Die SynthDef (`*addSynthDef`):**

```supercollider
*addSynthDef {
	SynthDef(\insectSound, { |out = 0, wingRate = 210, wingDuty = 0.25,
			ringFreq1 = 3200, ringFreq2 = 4600, ringDecay1 = 0.02, ringDecay2 = 0.015,
			amp = 0.35|
		var wings = LFPulse.ar(wingRate, 0, wingDuty) * WhiteNoise.ar(1);
		var buzz = Ringz.ar(wings, ringFreq1, ringDecay1)
			+ Ringz.ar(wings, ringFreq2, ringDecay2);
		Out.ar(out, buzz * amp);
	}).add;
}
```

1:1 der "Flügel/Brumm"-Teil aus der alten monolithischen `\insectVoice`-
SynthDef — nur ohne die Pan/ITD/Distanz-Logik, die jetzt bei `Binauralizer`
liegt. Klanglich:
- `LFPulse.ar(wingRate, 0, wingDuty)` — Pulswellen-Gate bei `wingRate` Hz
  (210), zu `wingDuty` (25%) "offen".
- `* WhiteNoise.ar(1)` — gattertes weißes Rauschen: Rauschimpulse im Rhythmus
  des Flügelschlags.
- `Ringz.ar(wings, freq, decay)` — resonanter Filter ("klingelt" bei `freq`
  nach, Ausklingzeit `decay`), macht aus den Rauschimpulsen einen tonalen
  "Brumm". Zwei parallel (3200Hz/4600Hz) für einen reicheren Klang.
- `Out.ar(out, buzz * amp)` — **mono**, kein Pan. `amp` ist die
  Grundlautstärke des Insekts an sich, nicht die entfernungsabhängige.

**`play`/`stop`:**

```supercollider
play { |server|
	bus = Bus.audio(server, 1);
	synth = Synth(\insectSound, [...], server);
	^this
}

stop {
	synth.free;
	bus.free;
}
```

`Bus.audio(server, 1)` reserviert einen **privaten** Audio-Bus mit einem
Kanal — ein unsichtbares Kabel zwischen Synths, geht nicht auf Lautsprecher/
Kopfhörer. `bus.index` (als `\out` im `Synth`-Aufruf) ist die Nummer dieses
Kabels. Dieses Bus-Objekt gibt `InsectSound` über den Getter nach außen,
damit `Binauralizer` davon lesen kann (`In.ar(in, 1)`).

Kurz: Diese Klasse weiß buchstäblich nichts von Raum, Hörer oder Pan.

## Binauralizer — Pan/ITD/Distanz für den Listener

Konstruktor-Parameter, jeder einer Formel unten zugeordnet:

```supercollider
*new { |lagTime = 0.08, itdScale = 0.0006, cutoffMin = 600, cutoffMax = 9000,
		ampRolloff = 0.9, behindDampMin = 0.55|
```

Die SynthDef, Formel für Formel:

- `az/dist = Lag.kr(azimuth/distance, lagTime)` — glättet die 30x/Sekunde von
  `SoundObject` reingeschobenen Werte exponentiell (Zeitkonstante `lagTime`,
  80ms), sonst gäbe es stufige Sprünge.
- `pan = sin(az)` — `az` kommt von `Listener.relativeAzimuth` (0 = vorne,
  positiv = rechts, ±π = hinten). `sin(az)` mappt auf den `Pan2`-Bereich
  [-1..1]: vorne (`az=0`) → Mitte, rechts (`az=π/2`) → voll rechts — **aber**
  hinten (`az=π`) → auch Mitte! Das ist das "Cone of Confusion"-Problem: rein
  über Pan sind vorne/hinten nicht unterscheidbar (beide gleich weit von
  beiden Ohren).
- `itd = itdScale * pan` — Interaural Time Difference, der Laufzeitunterschied
  zwischen den Ohren. `itdScale = 0.0006` (0.6ms) entspricht ungefähr der
  realen Größenordnung für einen menschlichen Kopf.
- `cutoff = (cutoffMax / (1 + dist)).clip(cutoffMin, cutoffMax)` —
  distanzabhängiger Tiefpass: nah → hell (`cutoffMax`, 9000Hz), fern → dumpf
  (Richtung `cutoffMin`, 600Hz) — hohe Frequenzen werden über Distanz stärker
  verschluckt.
- `behindDamp = cos(az).linlin(-1, 1, behindDampMin, 1)` — der Trick gegen das
  Cone-of-Confusion-Problem: vorne (`cos=1`) volle Lautstärke, hinten
  (`cos=-1`) spürbar leiser (`behindDampMin`, 0.55) — kein Ersatz für echtes
  HRTF, aber ein grober Hinweis.
- `distAmp = (1 / (1 + (dist * ampRolloff))).clip(0, 1)` — Lautstärke fällt
  mit der Distanz.
- `sig = LPF.ar(In.ar(in, 1), cutoff) * behindDamp * distAmp` — liest den
  Mono-Bus von `InsectSound`; **hier** passiert die eigentliche
  distanz-/richtungsabhängige Formung, komplett getrennt von der
  Klangerzeugung.
- `stereo = Pan2.ar(sig, pan)`, dann pro Kanal eine Zusatzverzögerung fürs
  ITD: ist `itd` positiv (Klang rechts), wird nur der **linke** Kanal extra
  verzögert (`itd.max(0)`) — das linke Ohr hört später, wie in echt.
  Spiegelverkehrt für `itd` negativ am rechten Kanal (`itd.neg.max(0)`).

`play`/`set`/`stop`:

```supercollider
play { |server, inBus, outBus = 0, target, addAction = \addToTail|
	synth = Synth.new(\binauralizer, [...], target ? server, addAction);
	^this
}

set { |azimuth, distance|
	synth.set(\azimuth, azimuth, \distance, distance);
}

stop {
	synth.free;
}
```

`target ? server` ist SuperCollider-Kurzform für "nimm `target`, falls nicht
`nil`, sonst `server`" — Fallback, falls niemand einen Ziel-Node für die
Node-Reihenfolge angibt. `set` ist die schlanke Schnittstelle, die
`SoundObject` jeden Tick aufruft. `stop` gibt **nur den Synth** frei, keinen
Bus — `Binauralizer` besitzt den Bus nicht, er liest nur davon; wer eine
Ressource anlegt (`InsectSound`), ist auch fürs Aufräumen zuständig.

## Orchestra — zentraler Taktgeber (seit Intent 12)

Vor Intent 12 hatte jedes `SoundObject` seine eigene `Routine`, kannte den `Listener` direkt und
schob Azimuth/Distanz selbst in seinen `Binauralizer`. Bei mehreren Klangobjekten hiess das: so
viele unabhängige Routinen wie Objekte, jede mit eigenem Timing — und jedes `SoundObject` musste
den `Listener` kennen, obwohl das eigentlich reine Geometrie ist, die nichts mit dem Klangobjekt
selbst zu tun hat.

```supercollider
Orchestra {
	var <listener;
	var <soundObjects;
	var routine;

	register { |soundObject|
		soundObjects = soundObjects.add(soundObject);
	}

	tick { |dt|
		soundObjects.do { |soundObject|
			var az, dist;
			soundObject.step(dt);
			az = listener.relativeAzimuth(soundObject.pos);
			dist = listener.distanceTo(soundObject.pos);
			soundObject.updateSpatial(az, dist);
		};
	}

	play { |server, updateRate = 30|
		var dt = 1.0 / updateRate;
		soundObjects.do { |soundObject| soundObject.play(server) };
		routine = Routine({ loop { this.tick(dt); dt.wait } }).play;
		^this
	}

	stop {
		routine !? { routine.stop };
		soundObjects.do { |soundObject| soundObject.stop };
		soundObjects = [];
	}
}
```

`Orchestra` hält jetzt den einen `Listener` und eine Registry beliebig vieler `SoundObject`s.
`play(server)` startet zuerst jedes registrierte `SoundObject` (dessen Sound-/Binauralizer-Synth)
und danach eine **einzige** Routine statt einer pro Objekt — dadurch reicht ein Aufruf
(`orchestra.play(s)`), statt jedes Insekt einzeln zu starten und danach noch das Orchester.
`tick` ist bewusst als eigene Methode ausgelagert (statt inline in der Routine), weil sie dadurch
ganz ohne laufende Routine und ohne Server direkt per `UnitTest` testbar ist
(`tests/sg/spatial/TestOrchestra.sc`, mit Fake-Objekten, die nur `play`/`pos`/`step`/`updateSpatial`
beantworten müssen).

`SoundObject` selbst schrumpft dadurch auf drei schlanke Methoden, die `Orchestra` pro Tick
aufruft: `pos` (liest `movable.pos`), `step(dt)` (delegiert an `movable.step(dt)`) und
`updateSpatial(azimuth, distance)` (delegiert an `binauralizer.set(azimuth, distance)`) — den
`Listener` braucht es dafür an keiner Stelle mehr.

`stop` räumt konsequent zu Ende auf: Routine stoppen, `stop` auf jedem registrierten
`SoundObject` aufrufen (das gibt wiederum dessen Synth/Bus frei), Registry leeren. Ein einziger
Aufruf (`orchestra.stop`) ersetzt damit das bisherige Stoppen jedes Insekts einzeln.

## Frage: Warum liegen die Defaults in InsectSound/Binauralizer doppelt vor?

Auffällig: dieselben Zahlen (z.B. `wingRate = 210`) stehen sowohl in `*new`
als auch in `*addSynthDef`:

```supercollider
*new { |wingRate = 210, wingDuty = 0.25, ...|          // Objekt-Defaults (sclang, Client-Seite)
	^super.new.init(...);
}

*addSynthDef {
	SynthDef(\insectSound, { |out = 0, wingRate = 210, wingDuty = 0.25, ...|  // SynthDef-Defaults (Server-Seite)
		...
	}).add;
}
```

Zwei verschiedene Ebenen: `*new`s Defaults füllen die Instanzvariablen, wenn
`InsectSound.new` ohne Argumente aufgerufen wird. Die SynthDef-Argument-
Defaults greifen dagegen nur, wenn ein `Synth(\insectSound, ...)` **ohne**
dieses Argument erzeugt wird — z.B. direkt in der IDE zum schnellen Antesten
(`Synth(\insectSound)`), unabhängig von der Klasse.

Der Haken: `play()` übergibt immer *alle* Instanzvariablen explizit als
Synth-Args, die SynthDef-eigenen Defaults kommen über den normalen
Klassen-Pfad also nie zum Einsatz — praktisch tot, außer man ruft
`Synth(\insectSound)` komplett an der Klasse vorbei auf. Eine echte
Ableitung "aus einer Quelle" ist nicht möglich, weil SynthDef-Argument-
Defaults literale Konstanten sein müssen (sie werden Teil der kompilierten
SynthDef) — man kann dort nicht auf `*new` verweisen.

Entscheidung: so gelassen (nicht neutralisiert) — die SynthDef bleibt dadurch
auch direkt am Server spielbar/testbar, unabhängig von der Klasse. Beide
Stellen jetzt im Code kurz kommentiert (`classes/sg/sounds/InsectSound.sc`,
`classes/sg/spatial/Binauralizer.sc`), damit die Duplikation beim nächsten Lesen nicht
wie ein Versehen wirkt.

## Fund beim Review: Tippfehler in insect_demo.scd

Beim gemeinsamen Durchgehen wurde `insect_demo.scd` zwischenzeitlich
umgebaut: die Zwischenobjekte (`moveRule`, `movable`, `listener`,
`insectSound`, `binauralizer`) wurden von `~`-Environment-Variablen auf
lokale `var`s umgestellt — passend, weil sie nur innerhalb von Block 2
gebraucht werden (nur die fertige `SoundInsect`-Instanz muss über den Block
hinaus als `~insect` erreichbar bleiben, für Block 3). Dabei aber ein
Tippfehler eingeschlichen: `~insectt = SoundInsect.new(...)` (doppeltes "t"),
während Block 3 weiterhin `~insect.play(s)` aufrief — das wäre `nil.play(s)`
gewesen. Korrigiert auf `~insect`.
