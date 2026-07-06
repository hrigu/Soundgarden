// SpaceView — live aktualisierte 2D-Draufsicht auf den virtuellen Raum: Listener (Position +
// Blickrichtung) und alle bei einer Orchestra registrierten Soundobjekte. Hat keinen eigenen
// State — jedes Neuzeichnen liest orchestra.listener/.soundObjects frisch, SpaceView bewegt
// selbst nichts. Gleiches Fenster-Lifecycle-Muster wie KeyboardListenerControl (play öffnet,
// stop schließt).
SpaceView {
	var <>orchestra;
	var <>viewRadius;  // Meter — wie weit der sichtbare Ausschnitt um den Ursprung reicht
	var <window;
	var view;
	var routine;

	*new { |orchestra, viewRadius = 8|
		^super.new.init(orchestra, viewRadius);
	}

	init { |anOrchestra, aViewRadius|
		orchestra = anOrchestra;
		viewRadius = aViewRadius;
	}

	// öffnet das Fenster und startet die Refresh-Routine. updateRate: wie oft pro
	// Sekunde neu gezeichnet wird.
	play { |updateRate = 20|
		var dt = 1.0 / updateRate;

		window = Window.new("Raum-Draufsicht", Rect(450, 100, 360, 360));
		view = UserView(window, window.view.bounds);
		view.drawFunc = { this.draw(view) };
		window.front;

		// AppClock statt SystemClock (Default): view.refresh ist ein GUI-Aufruf und muss
		// dort laufen, damit Qt ihn zuverlässig verarbeitet — sonst "läuft" die Routine,
		// aber das Fenster bleibt ein Standbild.
		routine = Routine({
			loop {
				view.refresh;
				dt.wait;
			}
		}).play(AppClock);

		^this
	}

	// zeichnet den aktuellen Zustand: Raumgrenze, Soundobjekte, Listener + Blickrichtung.
	// Weltkoordinaten (x=rechts, y=vorne) auf Bildschirmkoordinaten (y wächst nach unten)
	// abgebildet — +y (vorne) zeigt deshalb im Bild nach oben.
	draw { |aView|
		var bounds = aView.bounds;
		var cx = bounds.width * 0.5;
		var cy = bounds.height * 0.5;
		var scale = (min(bounds.width, bounds.height) * 0.5) / viewRadius;
		var listener = orchestra.listener;
		var forward = listener.forwardVector;
		var arrowLen = 18;

		Pen.fillColor = Color.white;
		Pen.fillRect(bounds);

		// Raumgrenze (Sichtausschnitt)
		Pen.strokeColor = Color.gray(0.75);
		Pen.width = 1;
		Pen.addArc(cx@cy, viewRadius * scale, 0, 2pi);
		Pen.stroke;

		// Soundobjekte
		Pen.fillColor = Color.red;
		orchestra.soundObjects.do { |soundObject|
			var pos = soundObject.pos;
			var x = cx + (pos[0] * scale);
			var y = cy - (pos[1] * scale);
			Pen.fillOval(Rect(x - 5, y - 5, 10, 10));
		};

		// Listener: Punkt + Blickrichtung als kurze Linie
		Pen.strokeColor = Color.blue;
		Pen.width = 2;
		Pen.line(cx@cy, (cx + (forward[0] * arrowLen))@(cy - (forward[1] * arrowLen)));
		Pen.stroke;

		Pen.fillColor = Color.blue;
		Pen.fillOval(Rect(cx - 6, cy - 6, 12, 12));
	}

	// beendet die Refresh-Routine und schließt das Fenster.
	stop {
		routine !? { routine.stop };
		window !? { window.close };
	}
}
