// RoomParamsView — statischer oberer Bereich von SpatialControlPanel, extrahiert aus
// SpatialControlPanel (Intent 55): Regler für Room-/Hall-Parameter (size/height/surface/mix,
// spread/inputBandwidth/tailBalance) sowie ein Objekt-übergreifender ReverbSend-Regler, der
// reverbMix auf allen registrierten Binauralizern gleichzeitig setzt. Optional (scenesDir)
// zusätzlich ein Bereich zum Speichern/Laden ganzer Raum-Szenen über RoomSceneLibrary (Intent
// 46) — analog zur Objekt-Preset-UI in SoundObjectControlsView, nur für den ganzen Room statt
// ein einzelnes Soundobjekt.
//
// Der Inhalt läuft seit Intent 46 in einer ScrollView (wie SoundObjectControlsView): eine
// erste Version mit fester CompositeView-Höhe hat den Szenen-Bereich bei zu knapp bemessener
// SpatialControlPanel>>installControls-Höhe unerreichbar abgeschnitten (siehe Challenges) --
// scrollbar macht das robust gegen jede künftige Erweiterung, statt Pixelhöhen zu erraten.
//
// build ist idempotent (entfernt eine vorherige view/scrollView zuerst) und wird nach
// erfolgreichem Szenen-Laden erneut aufgerufen, damit die Regler die neu geladenen Werte
// zeigen (siehe onSceneLoaded/SpatialControlPanel>>onSceneLoaded).
RoomParamsView {
	var <parentView;  // controlsView von SpatialControlPanel — Eltern-View
	var <rect;        // Position/Grösse innerhalb von parentView
	var <room;
	var <>scenesDir;
	var <>onSceneLoaded;  // Callback ({|room| ...}), feuert nach erfolgreichem Szenen-Laden
	var scrollView;
	var view;

	*new { |aParentView, aRect, aRoom, aScenesDir|
		^super.new.init(aParentView, aRect, aRoom, aScenesDir);
	}

	init { |aParentView, aRect, aRoom, aScenesDir|
		parentView = aParentView;
		rect = aRect;
		room = aRoom;
		scenesDir = aScenesDir;
	}

	build {
		// grosszügig bemessen (3 Header + 8 Slider fürs Room/Hall-Grundgerüst, optional 1
		// weiterer Header + Namensfeld + Dropdown + 2 Buttons fürs Szenen-Speichern/Laden) --
		// bewusst mit Puffer, damit ScrollView im Normalfall gar nicht scrollen muss.
		var baseHeight = 340;
		var sceneHeight = if(scenesDir.notNil) { 170 } { 0 };
		var contentHeight = rect.height.max(baseHeight + sceneHeight);
		var visibleWidth = rect.width - 16;

		view !? { view.remove };
		scrollView !? { scrollView.remove };
		scrollView = ScrollView(parentView, rect);
		scrollView.hasBorder_(false);
		scrollView.hasHorizontalScroller_(false);
		scrollView.hasVerticalScroller_(true);

		view = CompositeView(scrollView, Rect(0, 0, visibleWidth, contentHeight));
		view.decorator = FlowLayout(view.bounds.insetBy(16, 16));
		this.installControls;
		if(scenesDir.notNil) { this.installSceneControls };
		^this
	}

	installControls {
		var controlWidth = 380@26;
		var headerWidth = 380@22;
		var addGroupHeader = { |title|
			var label = StaticText(view, headerWidth);
			label.string = title;
			label.font = Font.default.copy.size_(15).boldVariant;
			label.align = \left;
			view.decorator.nextLine;
		};
		var makeSlider = { |label, initValue, spec, action|
			var slider = EZSlider(view, controlWidth, label, spec,
				{ |ez| action.(ez.value) }, initValue, false, 120, 56);
			view.decorator.nextLine;
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

	// Bereich zum Speichern/Laden ganzer Raum-Szenen (Intent 46) -- nur aufgebaut, wenn
	// scenesDir gesetzt ist (analog presetsDir/SoundObjectControlsView).
	installSceneControls {
		var controlWidth = 380@26;
		var headerWidth = 380@22;
		var label = StaticText(view, headerWidth);
		var nameField = TextField(view, controlWidth);
		var sceneMenu = PopUpMenu(view, controlWidth);
		var refreshSceneMenu = { sceneMenu.items = RoomSceneLibrary.listNames(scenesDir) };

		label.string = "Szene";
		label.font = Font.default.copy.size_(15).boldVariant;
		label.align = \left;
		view.decorator.nextLine;

		refreshSceneMenu.value;
		view.decorator.nextLine;

		Button(view, controlWidth).states_([["Szene speichern"]]).action_({
			if(nameField.string.size > 0) {
				RoomSceneLibrary.save(scenesDir, nameField.string, room);
				refreshSceneMenu.value;
			};
		});
		view.decorator.nextLine;

		Button(view, controlWidth).states_([["Szene laden"]]).action_({
			var name = sceneMenu.item;
			var sceneEvent = if(name.notNil) { RoomSceneLibrary.load(scenesDir, name) } { nil };

			if(sceneEvent.notNil) {
				RoomSceneLibrary.applyTo(room, sceneEvent);
				this.build;
				onSceneLoaded.(room);
			};
		});
		view.decorator.nextLine;
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
}
