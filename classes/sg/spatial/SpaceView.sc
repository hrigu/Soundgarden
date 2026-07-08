// SpaceView — live aktualisierte 2D-Draufsicht auf den virtuellen Raum: Listener (Position +
// Blickrichtung) und alle bei einer Orchestra registrierten Soundobjekte. Hat keinen eigenen
// State — jedes Neuzeichnen liest orchestra.listener/.soundObjects frisch, SpaceView bewegt
// selbst nichts. Gleiches Fenster-Lifecycle-Muster wie KeyboardListenerControl (play öffnet,
// stop schließt).
SpaceView {
	var <>orchestra;
	var <>viewRadius;  // Meter — wie weit der sichtbare Ausschnitt um den Ursprung reicht
	var <>editable;
	var <window;
	var view;
	var routine;
	var draggedSoundObject;

	*new { |orchestra, viewRadius = 8|
		^super.new.init(orchestra, viewRadius);
	}

	init { |anOrchestra, aViewRadius|
		orchestra = anOrchestra;
		viewRadius = aViewRadius;
	}

	// öffnet das Fenster und startet die Refresh-Routine. updateRate: wie oft pro
	// Sekunde neu gezeichnet wird. editable=true erlaubt Drag-and-drop für die
	// registrierten Soundobjekte direkt in der Draufsicht.
	play { |updateRate = 20, editable = false|
		var dt = 1.0 / updateRate;

		this.editable = editable;
		window = Window.new("Raum-Draufsicht", Rect(450, 100, 360, 360));
		view = UserView(window, window.view.bounds);
		view.drawFunc = { this.draw(view) };
		if(editable) {
			view.mouseDownAction = { |aView, x, y|
				draggedSoundObject = this.soundObjectAtScreenPoint(aView, x, y);
			};
			view.mouseMoveAction = { |aView, x, y|
				if(draggedSoundObject.notNil) {
					this.moveSoundObjectToScreenPoint(aView, draggedSoundObject, x, y);
					aView.refresh;
				};
			};
			view.mouseUpAction = { draggedSoundObject = nil };
		};
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
		var lx = cx + (listener.pos[0] * scale);
		var ly = cy - (listener.pos[1] * scale);

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
			var screenPos = this.worldToScreen(bounds, pos);
			var x = screenPos[0];
			var y = screenPos[1];
			Pen.fillOval(Rect(x - 5, y - 5, 10, 10));
		};

		// Listener: Punkt + Blickrichtung als kurze Linie, an seiner tatsächlichen Position
		Pen.strokeColor = Color.blue;
		Pen.width = 2;
		Pen.line(lx@ly, (lx + (forward[0] * arrowLen))@(ly - (forward[1] * arrowLen)));
		Pen.stroke;

		Pen.fillColor = Color.blue;
		Pen.fillOval(Rect(lx - 6, ly - 6, 12, 12));
	}

	worldToScreen { |bounds, pos|
		var cx = bounds.width * 0.5;
		var cy = bounds.height * 0.5;
		var scale = (min(bounds.width, bounds.height) * 0.5) / viewRadius;
		^[cx + (pos[0] * scale), cy - (pos[1] * scale)]
	}

	screenToWorld { |bounds, x, y, z = 0|
		var cx = bounds.width * 0.5;
		var cy = bounds.height * 0.5;
		var scale = (min(bounds.width, bounds.height) * 0.5) / viewRadius;
		^[((x - cx) / scale).clip(viewRadius.neg, viewRadius),
			((cy - y) / scale).clip(viewRadius.neg, viewRadius), z]
	}

	soundObjectAtScreenPoint { |aView, x, y, radius = 10|
		var bounds = aView.bounds;
		^orchestra.soundObjects.detect { |soundObject|
			var screenPos = this.worldToScreen(bounds, soundObject.pos);
			var dx = screenPos[0] - x;
			var dy = screenPos[1] - y;
			((dx * dx) + (dy * dy)).sqrt <= radius
		}
	}

	moveSoundObjectToScreenPoint { |aView, soundObject, x, y|
		var newPos = this.screenToWorld(aView.bounds, x, y, soundObject.pos[2]);
		soundObject.movable.moveTo(newPos);
	}

	// beendet die Refresh-Routine und schließt das Fenster.
	stop {
		routine !? { routine.stop };
		draggedSoundObject = nil;
		window !? { window.close };
	}
}
