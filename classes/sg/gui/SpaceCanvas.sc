// SpaceCanvas — linke Draufsicht von SpatialControlPanel, extrahiert aus SpatialControlPanel
// (Intent 55): Zeichnen (Raumgrenze, Soundobjekte, Listener + Blickrichtung), Welt↔Bildschirm-
// Transform, Hit-Testing, Maus-Dragging von Listener/Soundobjekt und Tastatursteuerung
// (W/S/A/D/Q/E) des Listeners. Kennt SpatialControlPanel selbst nicht — meldet
// Selektionsänderungen per onSelect-Callback nach aussen (siehe SpatialControlPanel>>play).
//
// Tastatur-Events (keyDown/keyUp) und der Zeichen-/Bewegungs-Takt (tick) werden von aussen
// aufgerufen: SpatialControlPanel besitzt Window und die eine gemeinsame Routine (Timing ist
// Panel-Sache), SpaceCanvas kennt kein Window und keine eigene Routine.
SpaceCanvas {
	var <room;
	var <>viewRadius;
	var <>editable;
	var <>moveSpeed;
	var <>rotateSpeed;
	var <view;
	var heldKeys;
	var draggedSoundObject;
	var isDraggingListener;
	var isRotatingListener;
	var <selectedSoundObject;
	var <>onSelect;  // Callback({|soundObject| ...}), feuert bei jeder Selektionsänderung

	*new { |aRoom, aViewRadius = 8, aMoveSpeed = 2, aRotateSpeed = 90, anEditable = true|
		^super.new.init(aRoom, aViewRadius, aMoveSpeed, aRotateSpeed, anEditable);
	}

	init { |aRoom, aViewRadius, aMoveSpeed, aRotateSpeed, anEditable|
		room = aRoom;
		viewRadius = aViewRadius;
		moveSpeed = aMoveSpeed;
		rotateSpeed = aRotateSpeed;
		editable = anEditable;
		heldKeys = Set.new;
		isDraggingListener = false;
		isRotatingListener = false;
	}

	// baut die UserView in parentView/rect und verdrahtet die Maus-Actions.
	build { |parentView, rect|
		view = UserView(parentView, rect);
		view.drawFunc = { this.draw(view) };
		this.installMouseActions;
		^this
	}

	// Klick auf ein Soundobjekt wählt es aus (immer, auch bei editable == false — Klangparameter
	// ansehen/bearbeiten ist unabhängig vom Verschieben-Dürfen). Listener-Körper und -Nase
	// bekommen im editierbaren Modus Vorrang für Position/Rotation.
	installMouseActions {
		view.mouseDownAction = { |aView, x, y|
			var hit;

			draggedSoundObject = nil;
			isDraggingListener = false;
			isRotatingListener = false;

			if(editable and: { this.listenerHandleAtScreenPoint(aView, x, y) }) {
				isRotatingListener = true;
			} {
				if(editable and: { this.listenerAtScreenPoint(aView, x, y) }) {
					isDraggingListener = true;
				} {
					hit = this.soundObjectAtScreenPoint(aView, x, y);
					this.select(hit);
					if(editable) { draggedSoundObject = hit };
				};
			};

			aView.refresh;
		};

		if(editable) {
			view.mouseMoveAction = { |aView, x, y|
				if(isRotatingListener) {
					this.rotateListenerToScreenPoint(aView, x, y);
				} {
					if(isDraggingListener) {
						this.moveListenerToScreenPoint(aView, x, y);
					} {
						if(draggedSoundObject.notNil) {
							this.moveSoundObjectToScreenPoint(aView, draggedSoundObject, x, y);
						};
					};
				};
				aView.refresh;
			};
		};

		view.mouseUpAction = {
			draggedSoundObject = nil;
			isDraggingListener = false;
			isRotatingListener = false;
		};
	}

	select { |soundObject|
		if(soundObject !== selectedSoundObject) {
			selectedSoundObject = soundObject;
			onSelect.(soundObject);
		};
		^this
	}

	keyDown { |char|
		heldKeys.add(char.asString.toLower);
		^this
	}

	keyUp { |char|
		heldKeys.remove(char.asString.toLower);
		^this
	}

	// von der Panel-Routine pro Tick aufgerufen: Listener-Bewegung per gehaltener Taste,
	// danach Neuzeichnen.
	tick { |dt|
		this.applyHeldKeys(dt);
		view.refresh;
		^this
	}

	applyHeldKeys { |dt|
		var listener = room.listener;

		if(heldKeys.includes("w")) { listener.moveForward(moveSpeed * dt) };
		if(heldKeys.includes("s")) { listener.moveBackward(moveSpeed * dt) };
		if(heldKeys.includes("a")) { listener.strafeLeft(moveSpeed * dt) };
		if(heldKeys.includes("d")) { listener.strafeRight(moveSpeed * dt) };
		if(heldKeys.includes("q")) { listener.rotate(rotateSpeed.neg * dt) };
		if(heldKeys.includes("e")) { listener.rotate(rotateSpeed * dt) };
	}

	draw { |aView|
		var orchestra = room.orchestra;
		var bounds = aView.bounds;
		var cx = bounds.width * 0.5;
		var cy = bounds.height * 0.5;
		var scale = (min(bounds.width, bounds.height) * 0.5) / viewRadius;
		var listener = orchestra.listener;
		var listenerScreen = this.worldToScreen(bounds, listener.pos);
		var noseScreen = this.listenerHandleScreenPoint(bounds);
		var lx = listenerScreen[0];
		var ly = listenerScreen[1];

		Pen.fillColor = Color.white;
		Pen.fillRect(bounds);

		Pen.strokeColor = Color.gray(0.75);
		Pen.width = 1;
		Pen.addArc(cx@cy, viewRadius * scale, 0, 2pi);
		Pen.stroke;

		orchestra.soundObjects.do { |soundObject|
			var screenPos = this.worldToScreen(bounds, soundObject.pos);
			Pen.fillColor = if(soundObject === selectedSoundObject) { Color.yellow } { Color.red };
			Pen.fillOval(Rect(screenPos[0] - 5, screenPos[1] - 5, 10, 10));
		};

		Pen.strokeColor = Color.blue;
		Pen.width = 2;
		Pen.line(lx@ly, noseScreen[0]@noseScreen[1]);
		Pen.stroke;

		Pen.fillColor = Color.blue;
		Pen.fillOval(Rect(lx - 6, ly - 6, 12, 12));

		Pen.fillColor = Color.cyan;
		Pen.fillOval(Rect(noseScreen[0] - 4, noseScreen[1] - 4, 8, 8));
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
		^room.orchestra.soundObjects.detect { |soundObject|
			var screenPos = this.worldToScreen(aView.bounds, soundObject.pos);
			var dx = screenPos[0] - x;
			var dy = screenPos[1] - y;
			((dx * dx) + (dy * dy)).sqrt <= radius
		}
	}

	moveSoundObjectToScreenPoint { |aView, soundObject, x, y|
		var newPos = this.screenToWorld(aView.bounds, x, y, soundObject.pos[2]);
		soundObject.movable.moveTo(newPos);
	}

	listenerAtScreenPoint { |aView, x, y, radius = 10|
		var screenPos = this.worldToScreen(aView.bounds, room.listener.pos);
		var dx = screenPos[0] - x;
		var dy = screenPos[1] - y;
		^((dx * dx) + (dy * dy)).sqrt <= radius
	}

	listenerHandleScreenPoint { |bounds, handleDistance = 18|
		var listener = room.listener;
		var listenerScreen = this.worldToScreen(bounds, listener.pos);
		var forward = listener.forwardVector;
		^[listenerScreen[0] + (forward[0] * handleDistance),
			listenerScreen[1] - (forward[1] * handleDistance)]
	}

	listenerHandleAtScreenPoint { |aView, x, y, radius = 8|
		var handlePos = this.listenerHandleScreenPoint(aView.bounds);
		var dx = handlePos[0] - x;
		var dy = handlePos[1] - y;
		^((dx * dx) + (dy * dy)).sqrt <= radius
	}

	moveListenerToScreenPoint { |aView, x, y|
		var listener = room.listener;
		listener.pos = this.screenToWorld(aView.bounds, x, y, listener.pos[2]);
	}

	rotateListenerToScreenPoint { |aView, x, y|
		var listener = room.listener;
		var targetPos = this.screenToWorld(aView.bounds, x, y, listener.pos[2]);
		var rel = targetPos - listener.pos;

		if(rel[0].abs > 0.0001 or: { rel[1].abs > 0.0001 }) {
			listener.facing = atan2(rel[0], rel[1]).raddeg;
		};
	}

	stop {
		heldKeys = Set.new;
		draggedSoundObject = nil;
		isDraggingListener = false;
		isRotatingListener = false;
		^this
	}
}
