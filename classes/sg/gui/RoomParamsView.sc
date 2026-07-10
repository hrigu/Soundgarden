// RoomParamsView — statischer oberer Bereich von SpatialControlPanel, extrahiert aus
// SpatialControlPanel (Intent 55): Regler für Room-/Hall-Parameter (size/height/surface/mix,
// spread/inputBandwidth/tailBalance) sowie ein Objekt-übergreifender ReverbSend-Regler, der
// reverbMix auf allen registrierten Binauralizern gleichzeitig setzt. Anders als
// SoundObjectControlsView baut sich dieser Bereich nur einmal auf (build) — die Regler selbst
// bleiben über die Lebensdauer des Panels bestehen, nur ihre Werte ändern sich.
RoomParamsView {
	var <parentView;  // controlsView von SpatialControlPanel — Eltern-View
	var <rect;        // Position/Grösse innerhalb von parentView
	var <room;
	var view;

	*new { |aParentView, aRect, aRoom|
		^super.new.init(aParentView, aRect, aRoom);
	}

	init { |aParentView, aRect, aRoom|
		parentView = aParentView;
		rect = aRect;
		room = aRoom;
	}

	build {
		view = CompositeView(parentView, rect);
		view.decorator = FlowLayout(view.bounds.insetBy(16, 16));
		this.installControls;
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
