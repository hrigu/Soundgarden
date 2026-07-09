// SpatialControlPanel — gemeinsames GUI für Spatial-Audio-Demos: ein Fenster mit
// editierbarer Raum-Draufsicht links und einem rechten Bedienbereich. Task 2 von
// Intent 39 deckt hier zunächst Tastatursteuerung des Listeners plus Drag-and-drop
// der Klangquellen ab; Room-/Reverb-Regler folgen in Task 3.
SpatialControlPanel {
	var <>room;
	var <>viewRadius;
	var <>moveSpeed;
	var <>rotateSpeed;
	var <>editable;
	var <window;
	var view;
	var controlsView;
	var routine;
	var heldKeys;
	var draggedSoundObject;

	*new { |room, viewRadius = 8, moveSpeed = 2, rotateSpeed = 90, editable = true|
		^super.new.init(room, viewRadius, moveSpeed, rotateSpeed, editable);
	}

	init { |aRoom, aViewRadius, aMoveSpeed, aRotateSpeed, anEditable|
		room = aRoom;
		viewRadius = aViewRadius;
		moveSpeed = aMoveSpeed;
		rotateSpeed = aRotateSpeed;
		editable = anEditable;
		heldKeys = Set.new;
	}

	play { |updateRate = 20|
		var dt = 1.0 / updateRate;
		var leftWidth = 420;
		var totalWidth = 620;
		var totalHeight = 420;

		window = Window.new("Spatial-Control-Panel", Rect(100, 100, totalWidth, totalHeight));
		view = UserView(window, Rect(0, 0, leftWidth, totalHeight));
		controlsView = CompositeView(window, Rect(leftWidth, 0, totalWidth - leftWidth, totalHeight));
		controlsView.decorator = FlowLayout(controlsView.bounds.insetBy(10, 10));
		view.drawFunc = { this.draw(view) };
		this.installMouseActions;
		this.installKeyActions;
		this.installControls;
		window.front;

		routine = Routine({
			loop {
				this.applyHeldKeys(dt);
				view.refresh;
				dt.wait;
			}
		}).play(AppClock);

		^this
	}

	installMouseActions {
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
	}

	installKeyActions {
		window.view.keyDownAction = { |aView, char|
			heldKeys.add(char.asString.toLower);
		};
		window.view.keyUpAction = { |aView, char|
			heldKeys.remove(char.asString.toLower);
		};
	}

	installControls {
		var makeSlider = { |label, initValue, spec, action|
			EZSlider(controlsView, 170@24, label, spec, { |ez| action.(ez.value) }, initValue);
		};

		makeSlider.("Size", room.size, ControlSpec(2, 30, \lin, 0.1), { |value|
			room.size = value;
		});
		makeSlider.("Height", room.height, ControlSpec(1, 20, \lin, 0.1), { |value|
			room.height = value;
		});
		makeSlider.("Surface", room.surface, ControlSpec(0, 1, \lin, 0.01), { |value|
			room.surface = value;
		});
		makeSlider.("Mix", room.mix, ControlSpec(0, 1, \lin, 0.01), { |value|
			room.mix = value;
		});
		// Wertebereich grosszuegig (kein dokumentiertes Hard-Limit von GVerb) -- Experimentier-
		// Regler gegen metallisches Klingeln, siehe Room>>spread (Intent 41).
		makeSlider.("Spread", room.spread, ControlSpec(0, 60, \lin, 0.5), { |value|
			room.spread = value;
		});
		// inputBandwidth: Tiefpass vor dem Diffusions-Netzwerk, hell/dunkel des Hall-Eingangs.
		makeSlider.("InputBandwidth", room.inputBandwidth, ControlSpec(0, 1, \lin, 0.01), { |value|
			room.inputBandwidth = value;
		});
		// tailBalance: 0 = nur fruehe Reflexionen .. 1 = nur diffuser Nachhall-Schwanz,
		// 0.5 = ausbalanciert (siehe Room>>tailBalance, Intent 41).
		makeSlider.("TailBalance", room.tailBalance, ControlSpec(0, 1, \lin, 0.01), { |value|
			room.tailBalance = value;
		});
		makeSlider.("ReverbSend", this.currentReverbMix, ControlSpec(0, 1, \lin, 0.01), { |value|
			this.applyReverbMix(value);
		});
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
		var forward = listener.forwardVector;
		var arrowLen = 18;
		var listenerScreen = this.worldToScreen(bounds, listener.pos);
		var lx = listenerScreen[0];
		var ly = listenerScreen[1];

		Pen.fillColor = Color.white;
		Pen.fillRect(bounds);

		Pen.strokeColor = Color.gray(0.75);
		Pen.width = 1;
		Pen.addArc(cx@cy, viewRadius * scale, 0, 2pi);
		Pen.stroke;

		Pen.fillColor = Color.red;
		orchestra.soundObjects.do { |soundObject|
			var screenPos = this.worldToScreen(bounds, soundObject.pos);
			Pen.fillOval(Rect(screenPos[0] - 5, screenPos[1] - 5, 10, 10));
		};

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

	currentReverbMix {
		var first = room.orchestra.soundObjects.detect({ |soundObject|
			soundObject.binauralizer.notNil
		});
		^if(first.notNil) { first.binauralizer.reverbMix } { 0.3 }
	}

	applyReverbMix { |value|
		room.orchestra.soundObjects.do { |soundObject|
			soundObject.binauralizer.reverbMix = value;
			soundObject.binauralizer.synth !? {
				soundObject.binauralizer.synth.set(\reverbMix, value);
			};
		};
	}

	stop {
		routine !? { routine.stop };
		draggedSoundObject = nil;
		heldKeys = Set.new;
		window !? { window.close };
	}
}
