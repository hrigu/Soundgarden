// SoundObjectControlsView — dynamischer rechter Bereich von SpatialControlPanel fürs
// aktuell ausgewählte Soundobjekt, extrahiert aus SpatialControlPanel (Intent 55): Titel,
// Solo-Button, Sample-Waveform-Preview (nur bei SampleSound) und ein EZSlider pro
// editableParams-Eintrag des Sounds, optional (presetsDir) eine Preset-Bibliothek zum
// Speichern/Laden. Baut sich bei jedem rebuild(selectedSoundObject) komplett neu auf (bei
// Selektionswechsel und nach dem Laden eines Presets, damit die Regler den geladenen Werten
// folgen) — kein Zustand über den jeweils aktuell ausgewählten Sound hinaus.
SoundObjectControlsView {
	var <parentView;  // controlsView von SpatialControlPanel — Eltern-View für ScrollView/CompositeView
	var <>top;        // y-Offset innerhalb von parentView, ab dem dieser Bereich beginnt
	var <>presetsDir;
	var soloMuteController;
	var scrollView;
	var contentView;
	var samplePreviewView;
	var <selectedSoundObject;

	*new { |aParentView, aTop, aPresetsDir, aSoloMuteController|
		^super.new.init(aParentView, aTop, aPresetsDir, aSoloMuteController);
	}

	init { |aParentView, aTop, aPresetsDir, aSoloMuteController|
		parentView = aParentView;
		top = aTop;
		presetsDir = aPresetsDir;
		soloMuteController = aSoloMuteController;
	}

	// baut den kompletten Bereich für soundObject neu auf (nil = keine Selektion, zeigt
	// stattdessen einen Platzhaltertext).
	rebuild { |soundObject|
		var width = parentView.bounds.width;
		var height = parentView.bounds.height - top;
		var visibleWidth = width - 16;
		var sound;
		var sliderCount;
		var contentHeight;
		var titleHeight = 32;
		var soloHeight = 36;
		var samplePreviewHeight = 0;
		var sliderHeight = 36;
		var presetHeight = if(presetsDir.notNil) { 180 } { 0 };
		var paddingHeight = 80;

		selectedSoundObject = soundObject;

		contentView !? { contentView.remove };
		scrollView !? { scrollView.remove };
		samplePreviewView = nil;
		scrollView = ScrollView(parentView, Rect(0, top, width, height));
		scrollView.hasBorder_(false);
		scrollView.hasHorizontalScroller_(false);
		scrollView.hasVerticalScroller_(true);

		sound = selectedSoundObject !? { selectedSoundObject.sound };
		sliderCount = if(sound.notNil) { sound.class.editableParams.size } { 0 };
		samplePreviewHeight = this.previewHeightFor(sound);
		contentHeight = height.max(
			titleHeight + soloHeight + samplePreviewHeight + (sliderCount * sliderHeight)
			+ presetHeight + paddingHeight
		);
		contentView = CompositeView(scrollView, Rect(0, 0, visibleWidth, contentHeight));
		contentView.decorator = FlowLayout(contentView.bounds.insetBy(16, 16));

		if(selectedSoundObject.isNil) {
			StaticText(contentView, 340@40).string_("Kein Soundobjekt ausgewählt");
		} {
			this.buildSoundParamControls;
			if(presetsDir.notNil) { this.buildPresetControls };
		};
		^this
	}

	// ein EZSlider pro editableParams-Eintrag des ausgewählten Sounds.
	buildSoundParamControls {
		var sound = selectedSoundObject.sound;
		var controlWidth = 380@26;
		var title = StaticText(contentView, 380@22);
		var soloButton;

		title.string = sound.class.name.asString;
		title.font = Font.default.copy.size_(15).boldVariant;
		title.align = \left;
		contentView.decorator.nextLine;

		soloButton = Button(contentView, 380@24).states_([
			["Nur dieses Objekt hören"],
			["Solo aktiv — andere stumm"]
		]).value_(soloMuteController.soloSelectedOnly.asInteger).action_({ |button|
			soloMuteController.setSoloSelectedOnly(button.value == 1, selectedSoundObject);
		});
		contentView.decorator.nextLine;

		if(sound.isKindOf(SampleSound)) {
			this.buildSamplePreviewControls(sound);
		};

		sound.class.editableParams.do { |pair|
			var key = pair[0];
			var spec = pair[1];
			EZSlider(contentView, controlWidth, key.asString, spec,
				{ |ez|
					sound.setParam(key, ez.value);
					this.refreshSamplePreview;
				}, sound.perform(key), false, 120, 56);
			contentView.decorator.nextLine;
		};
	}

	previewHeightFor { |sound|
		if(sound.isKindOf(SampleSound)) {
			^138
		};
		^0
	}

	buildSamplePreviewControls { |sound|
		var fileLabel = StaticText(contentView, 380@20);
		var preview = sound.samplePreview;

		fileLabel.string = sound.sampleFileName;
		fileLabel.align = \left;
		contentView.decorator.nextLine;

		if(preview[\error].notNil) {
			var errorLabel = StaticText(contentView, 380@36);
			errorLabel.string = preview[\error];
			errorLabel.stringColor = Color.gray(0.35);
			errorLabel.align = \left;
			contentView.decorator.nextLine;
		} {
			samplePreviewView = UserView(contentView, 380@92);
			samplePreviewView.background = Color.gray(0.97);
			samplePreviewView.drawFunc = { this.drawSamplePreview(samplePreviewView, sound, preview) };
			contentView.decorator.nextLine;
		};
	}

	refreshSamplePreview {
		samplePreviewView !? { samplePreviewView.refresh };
		^this
	}

	drawSamplePreview { |aView, sound, preview|
		var bounds = aView.bounds;
		var peaks = preview[\peaks] ? [];
		var range = this.sampleSelectionRangeFor(sound, preview);
		var startX = bounds.width * range[0];
		var endX = bounds.width * range[1];
		var selectionWidth = (endX - startX).max(3);
		var selectionRect = Rect(startX, 0, selectionWidth, bounds.height);
		var overlayHeight = 12;
		var midY = bounds.height * 0.5;

		Pen.fillColor = Color.gray(0.97);
		Pen.fillRect(bounds);

		Pen.fillColor = Color.red(0.95, 0.18);
		Pen.fillRect(Rect(startX, 0, selectionWidth, overlayHeight));

		Pen.fillColor = Color.red(1.0, 0.12);
		Pen.fillRect(selectionRect);

		Pen.strokeColor = Color.gray(0.75);
		Pen.width = 1;
		Pen.line(0 @ midY, bounds.width @ midY);
		Pen.stroke;

		Pen.strokeColor = Color.black.alpha_(0.75);
		Pen.width = 1;
		peaks.size.do { |index|
			var x = if(peaks.size <= 1) { bounds.width * 0.5 } { index / (peaks.size - 1) * bounds.width };
			var halfHeight = ((peaks[index] ? 0) * (bounds.height * 0.36)).max(1);
			Pen.line(x @ (midY - halfHeight), x @ (midY + halfHeight));
			Pen.stroke;
		};

		Pen.strokeColor = Color.red(0.85);
		Pen.width = 2;
		Pen.line(startX @ 0, startX @ bounds.height);
		Pen.line((startX + selectionWidth) @ 0, (startX + selectionWidth) @ bounds.height);
		Pen.stroke;

		Pen.strokeColor = Color.gray(0.6);
		Pen.width = 1;
		Pen.addRect(bounds.insetBy(0.5, 0.5));
		Pen.stroke;
	}

	sampleSelectionRangeFor { |sound, preview|
		var start = sound.startFrac.clip(0, 1);
		var previewDuration = preview[\duration] ? 0;
		var end = 1.0;

		if(sound.duration > 0 and: { previewDuration > 0 }) {
			end = (start + (sound.duration / previewDuration)).clip(start, 1.0);
		};

		^[start, end]
	}

	// Preset-Bibliothek fürs ausgewählte Soundobjekt -- umfasst seit Intent 46 nicht mehr nur
	// den Sound, sondern das ganze SoundObject (Klang + Position + Bewegungsregel, siehe
	// SoundObjectPresetLibrary).
	buildPresetControls {
		var sound = selectedSoundObject.sound;
		var nameField = TextField(contentView, 380@24);
		var presetMenu = PopUpMenu(contentView, 380@24);
		var refreshPresetMenu = {
			presetMenu.items = SoundObjectPresetLibrary.listNamesForSoundClass(presetsDir,
				sound.class)
		};

		refreshPresetMenu.value;
		contentView.decorator.nextLine;

		Button(contentView, 380@24).states_([["Preset speichern"]]).action_({
			if(nameField.string.size > 0) {
				SoundObjectPresetLibrary.save(presetsDir, nameField.string, selectedSoundObject);
				refreshPresetMenu.value;
			};
		});
		contentView.decorator.nextLine;

		Button(contentView, 380@24).states_([["Preset laden"]]).action_({
			var name = presetMenu.item;
			var preset = if(name.notNil) { SoundObjectPresetLibrary.load(presetsDir, name) } { nil };

			if(preset.notNil) {
				if(preset[\soundClass] == sound.class.name) {
					SoundObjectPresetLibrary.applyTo(selectedSoundObject, preset);
					this.rebuild(selectedSoundObject);
				} {
					("Preset '" ++ name ++ "' passt nicht zu " ++ sound.class.name.asString
						++ " -- übersprungen").postln;
				};
			};
		});
		contentView.decorator.nextLine;
	}

	// Aufräumen bei SpatialControlPanel>>stop — entspricht dem Nullen von
	// objectScrollView/objectControlsView im bisherigen Code (Fenster selbst schliesst die
	// Kind-Views ohnehin, dies ist nur fürs Vermeiden toter Referenzen gedacht).
	free {
		scrollView = nil;
		contentView = nil;
		samplePreviewView = nil;
		^this
	}
}
