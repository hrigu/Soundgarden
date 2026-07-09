// SpatialControlPanel — gemeinsames GUI für Spatial-Audio-Demos: ein Fenster mit
// editierbarer Raum-Draufsicht links und einem rechten Bedienbereich, aufgeteilt in
// Raum-Regler (oben, statisch) und Regler für ein per Klick ausgewähltes Soundobjekt
// (unten, dynamisch neu aufgebaut — siehe rebuildObjectControls). Klick auf ein
// Soundobjekt in der Draufsicht wählt es aus, unabhängig von editable (Auswahl/Ansehen ist
// kein Verschieben); die bekannten <>-Setter jedes Sound-Subklasse (siehe
// Sound>>editableParams/setParam) treiben dessen Regler. Optional (presetsDir) eine
// Preset-Bibliothek zum Speichern/Laden guter Klangeinstellungen (SoundPresetLibrary,
// Intent 43).
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
	}

	play { |updateRate = 20|
		var dt = 1.0 / updateRate;
		var leftWidth = 420;
		var totalWidth = 620;
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

	// Klick auf ein Soundobjekt wählt es aus (immer, auch bei editable == false — Ansehen/
	// Bearbeiten der Klangparameter ist unabhängig vom Verschieben-Dürfen). Dragging (Bewegen
	// im Raum) bleibt wie bisher an editable gekoppelt.
	installMouseActions {
		view.mouseDownAction = { |aView, x, y|
			var hit = this.soundObjectAtScreenPoint(aView, x, y);

			if(hit !== selectedSoundObject) {
				selectedSoundObject = hit;
				this.rebuildObjectControls;
			};
			if(editable) { draggedSoundObject = hit };
			aView.refresh;
		};
		if(editable) {
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

	// baut den statischen Raum-Regler-Bereich (oben) und den anfangs leeren (Platzhalter)
	// Objekt-Regler-Bereich (unten, siehe rebuildObjectControls) — zwei fest positionierte
	// Unter-Views statt eines gemeinsamen FlowLayout auf controlsView selbst, damit
	// rebuildObjectControls den unteren Bereich beliebig oft neu aufbauen kann, ohne die
	// Raum-Regler zu verschieben.
	installControls {
		var roomHeight = 260;

		roomControlsView = CompositeView(controlsView, Rect(0, 0, controlsView.bounds.width, roomHeight));
		roomControlsView.decorator = FlowLayout(roomControlsView.bounds.insetBy(10, 10));
		this.installRoomControls;

		this.rebuildObjectControls;
	}

	installRoomControls {
		var makeSlider = { |label, initValue, spec, action|
			EZSlider(roomControlsView, 170@24, label, spec, { |ez| action.(ez.value) }, initValue);
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

	// baut den Objekt-Regler-Bereich komplett neu auf (bei jeder Selektionsänderung und nach
	// dem Laden eines Presets, damit die Regler den geladenen Werten folgen). View>>remove
	// entfernt die alte Unter-View sauber aus controlsView, bevor eine neue an derselben
	// Rect-Position entsteht -- kein FlowLayout-Reordering-Risiko, da objectControlsView fest
	// positioniert ist (siehe installControls).
	rebuildObjectControls {
		var roomHeight = 260;
		var top = roomHeight + 10;
		var width = controlsView.bounds.width;
		var height = controlsView.bounds.height - top;

		objectControlsView !? { objectControlsView.remove };
		objectControlsView = CompositeView(controlsView, Rect(0, top, width, height));
		objectControlsView.decorator = FlowLayout(objectControlsView.bounds.insetBy(10, 10));

		if(selectedSoundObject.isNil) {
			StaticText(objectControlsView, 170@40).string_("Kein Soundobjekt ausgewählt");
		} {
			this.buildSoundParamControls;
			if(presetsDir.notNil) { this.buildPresetControls };
		};
	}

	// ein EZSlider pro editableParams-Eintrag des ausgewählten Sounds (siehe
	// Sound>>editableParams/setParam) -- funktioniert für jede Sound-Subklasse identisch,
	// ohne dass SpatialControlPanel deren konkrete Parameter kennen muss.
	buildSoundParamControls {
		var sound = selectedSoundObject.sound;

		StaticText(objectControlsView, 170@20).string_(sound.class.name.asString);
		sound.class.editableParams.do { |pair|
			var key = pair[0];
			var spec = pair[1];
			EZSlider(objectControlsView, 170@24, key.asString, spec,
				{ |ez| sound.setParam(key, ez.value) }, sound.perform(key));
		};
	}

	// Preset-Bibliothek fürs ausgewählte Soundobjekt (nur falls presetsDir gesetzt ist, siehe
	// rebuildObjectControls) -- PopUpMenu listet vorhandene Presets aus presetsDir,
	// "Preset speichern" schreibt die aktuellen Regler-Werte neu weg, "Preset laden" wendet
	// ein ausgewähltes Preset live an (SoundPresetLibrary), aber nur wenn dessen gespeicherte
	// soundClass zum ausgewählten Sound passt -- sonst nur ein Post-Hinweis, kein Fehler.
	buildPresetControls {
		var sound = selectedSoundObject.sound;
		var nameField = TextField(objectControlsView, 170@24);
		var presetMenu = PopUpMenu(objectControlsView, 170@24);
		var refreshPresetMenu = { presetMenu.items = SoundPresetLibrary.listNames(presetsDir) };

		refreshPresetMenu.value;

		Button(objectControlsView, 170@24).states_([["Preset speichern"]]).action_({
			if(nameField.string.size > 0) {
				SoundPresetLibrary.save(presetsDir, nameField.string, sound);
				refreshPresetMenu.value;
			};
		});

		Button(objectControlsView, 170@24).states_([["Preset laden"]]).action_({
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

		orchestra.soundObjects.do { |soundObject|
			var screenPos = this.worldToScreen(bounds, soundObject.pos);
			Pen.fillColor = if(soundObject === selectedSoundObject) { Color.yellow } { Color.red };
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
