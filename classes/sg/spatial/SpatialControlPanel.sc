// SpatialControlPanel — gemeinsames GUI für Spatial-Audio-Demos: ein Fenster mit
// editierbarer Raum-Draufsicht links und einem rechten Bedienbereich, aufgeteilt in einen
// statischen Bereich für Room-/Hall-Regler und einen dynamischen Bereich für das aktuell
// ausgewählte Soundobjekt. Listener kann per Tastatur und Maus bewegt werden; Soundobjekte
// lassen sich auswählen und — falls editable — ziehen. Optional (presetsDir) steht eine
// Preset-Bibliothek für Sound-Parameter zur Verfügung.
SpatialControlPanel {
	var <>room;
	var <>viewRadius;
	var <>moveSpeed;
	var <>rotateSpeed;
	var <>editable;
	var <>presetsDir;
	var <window;
	var view;
	var controlsView;
	var roomControlsView;
	var objectControlsView;
	var routine;
	var heldKeys;
	var draggedSoundObject;
	var isDraggingListener;
	var isRotatingListener;
	var <selectedSoundObject;

	*new { |room, viewRadius = 8, moveSpeed = 2, rotateSpeed = 90, editable = true, presetsDir|
		^super.new.init(room, viewRadius, moveSpeed, rotateSpeed, editable, presetsDir);
	}

	init { |aRoom, aViewRadius, aMoveSpeed, aRotateSpeed, anEditable, aPresetsDir|
		room = aRoom;
		viewRadius = aViewRadius;
		moveSpeed = aMoveSpeed;
		rotateSpeed = aRotateSpeed;
		editable = anEditable;
		presetsDir = aPresetsDir;
		heldKeys = Set.new;
		isDraggingListener = false;
		isRotatingListener = false;
	}

	play { |updateRate = 20|
		var dt = 1.0 / updateRate;
		var leftWidth = 460;
		var totalWidth = 860;
		var totalHeight = 650;

		window = Window.new("Spatial-Control-Panel", Rect(100, 100, totalWidth, totalHeight));
		view = UserView(window, Rect(0, 0, leftWidth, totalHeight));
		controlsView = CompositeView(window, Rect(leftWidth, 0, totalWidth - leftWidth, totalHeight));
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
					if(hit !== selectedSoundObject) {
						selectedSoundObject = hit;
						this.rebuildObjectControls;
					};
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

	installKeyActions {
		window.view.keyDownAction = { |aView, char|
			heldKeys.add(char.asString.toLower);
		};
		window.view.keyUpAction = { |aView, char|
			heldKeys.remove(char.asString.toLower);
		};
	}

	// rechter Bedienbereich: oben statische Room-/Hall-Regler, unten dynamische Regler fürs
	// ausgewählte Soundobjekt.
	installControls {
		var roomHeight = 360;

		roomControlsView = CompositeView(controlsView, Rect(0, 0, controlsView.bounds.width, roomHeight));
		roomControlsView.decorator = FlowLayout(roomControlsView.bounds.insetBy(16, 16));
		this.installRoomControls;

		this.rebuildObjectControls;
	}

	installRoomControls {
		var controlWidth = 340@26;
		var headerWidth = 340@22;
		var addGroupHeader = { |title|
			var label = StaticText(roomControlsView, headerWidth);
			label.string = title;
			label.font = Font.default.copy.size_(15).boldVariant;
			label.align = \left;
			roomControlsView.decorator.nextLine;
		};
		var makeSlider = { |label, initValue, spec, action|
			var slider = EZSlider(roomControlsView, controlWidth, label, spec,
				{ |ez| action.(ez.value) }, initValue);
			roomControlsView.decorator.nextLine;
			slider
		};

		addGroupHeader.("Room-Parameter");
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

		addGroupHeader.("Direkte Hall-Parameter");
		// Wertebereich grosszuegig (kein dokumentiertes Hard-Limit von GVerb) -- Experimentier-
		// Regler gegen metallisches Klingeln, siehe Room>>spread.
		makeSlider.("Spread", room.spread, ControlSpec(0, 60, \lin, 0.5), { |value|
			room.spread = value;
		});
		// inputBandwidth: Tiefpass vor dem Diffusions-Netzwerk, hell/dunkel des Hall-Eingangs.
		makeSlider.("InputBandwidth", room.inputBandwidth, ControlSpec(0, 1, \lin, 0.01), { |value|
			room.inputBandwidth = value;
		});
		// tailBalance: 0 = nur fruehe Reflexionen .. 1 = nur diffuser Nachhall-Schwanz.
		makeSlider.("TailBalance", room.tailBalance, ControlSpec(0, 1, \lin, 0.01), { |value|
			room.tailBalance = value;
		});

		addGroupHeader.("Soundobjekt-Parameter");
		makeSlider.("ReverbSend", this.currentReverbMix, ControlSpec(0, 1, \lin, 0.01), { |value|
			this.applyReverbMix(value);
		});
	}

	// baut den Objekt-Regler-Bereich komplett neu auf (bei jeder Selektionsänderung und nach
	// dem Laden eines Presets, damit die Regler den geladenen Werten folgen).
	rebuildObjectControls {
		var roomHeight = 360;
		var top = roomHeight + 10;
		var width = controlsView.bounds.width;
		var height = controlsView.bounds.height - top;

		objectControlsView !? { objectControlsView.remove };
		objectControlsView = CompositeView(controlsView, Rect(0, top, width, height));
		objectControlsView.decorator = FlowLayout(objectControlsView.bounds.insetBy(16, 16));

		if(selectedSoundObject.isNil) {
			StaticText(objectControlsView, 340@40).string_("Kein Soundobjekt ausgewählt");
		} {
			this.buildSoundParamControls;
			if(presetsDir.notNil) { this.buildPresetControls };
		};
	}

	// ein EZSlider pro editableParams-Eintrag des ausgewählten Sounds.
	buildSoundParamControls {
		var sound = selectedSoundObject.sound;
		var controlWidth = 340@26;
		var title = StaticText(objectControlsView, 340@22);

		title.string = sound.class.name.asString;
		title.font = Font.default.copy.size_(15).boldVariant;
		title.align = \left;
		objectControlsView.decorator.nextLine;

		sound.class.editableParams.do { |pair|
			var key = pair[0];
			var spec = pair[1];
			EZSlider(objectControlsView, controlWidth, key.asString, spec,
				{ |ez| sound.setParam(key, ez.value) }, sound.perform(key));
			objectControlsView.decorator.nextLine;
		};
	}

	// Preset-Bibliothek fürs ausgewählte Soundobjekt.
	buildPresetControls {
		var sound = selectedSoundObject.sound;
		var nameField = TextField(objectControlsView, 340@24);
		var presetMenu = PopUpMenu(objectControlsView, 340@24);
		var refreshPresetMenu = { presetMenu.items = SoundPresetLibrary.listNames(presetsDir) };

		refreshPresetMenu.value;
		objectControlsView.decorator.nextLine;

		Button(objectControlsView, 340@24).states_([["Preset speichern"]]).action_({
			if(nameField.string.size > 0) {
				SoundPresetLibrary.save(presetsDir, nameField.string, sound);
				refreshPresetMenu.value;
			};
		});
		objectControlsView.decorator.nextLine;

		Button(objectControlsView, 340@24).states_([["Preset laden"]]).action_({
			var name = presetMenu.item;
			var preset = if(name.notNil) { SoundPresetLibrary.load(presetsDir, name) } { nil };

			if(preset.notNil) {
				if(preset[\soundClass] == sound.class.name) {
					SoundPresetLibrary.applyTo(sound, preset);
					this.rebuildObjectControls;
				} {
					("Preset '" ++ name ++ "' passt nicht zu " ++ sound.class.name.asString
						++ " -- übersprungen").postln;
				};
			};
		});
		objectControlsView.decorator.nextLine;
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
		isDraggingListener = false;
		isRotatingListener = false;
		heldKeys = Set.new;
		window !? { window.close };
	}
}
