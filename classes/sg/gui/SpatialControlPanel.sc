// SpatialControlPanel — gemeinsames GUI für Spatial-Audio-Demos: ein Fenster mit
// editierbarer Raum-Draufsicht links (SpaceCanvas) und einem rechten Bedienbereich,
// aufgeteilt in einen statischen Bereich für Room-/Hall-Regler (RoomParamsView) und einen
// dynamischen Bereich für das aktuell ausgewählte Soundobjekt (SoundObjectControlsView).
// Solo/Mute-Zustand kommt von SoloMuteController. SpatialControlPanel selbst ist seit Intent
// 55 nur noch der Orchestrator: Fenster-Lifecycle, komponiert die vier genannten Bausteine
// und verdrahtet Selektionsänderungen zwischen SpaceCanvas und den beiden rechten Panels.
SpatialControlPanel {
	var <>room;
	var <>viewRadius;
	var <>moveSpeed;
	var <>rotateSpeed;
	var <>editable;
	var <>presetsDir;
	var <>scenesDir;
	var <window;
	var controlsView;
	var spaceCanvas;
	var roomParamsView;
	var objectControlsView;
	var routine;
	var soloMuteController;

	*new { |room, viewRadius = 8, moveSpeed = 2, rotateSpeed = 90, editable = true, presetsDir,
			scenesDir|
		^super.new.init(room, viewRadius, moveSpeed, rotateSpeed, editable, presetsDir,
			scenesDir);
	}

	init { |aRoom, aViewRadius, aMoveSpeed, aRotateSpeed, anEditable, aPresetsDir, aScenesDir|
		room = aRoom;
		viewRadius = aViewRadius;
		moveSpeed = aMoveSpeed;
		rotateSpeed = aRotateSpeed;
		editable = anEditable;
		presetsDir = aPresetsDir;
		scenesDir = aScenesDir;
		soloMuteController = SoloMuteController.new(room.orchestra);
	}

	// aktuell ausgewähltes Soundobjekt — SpaceCanvas ist die einzige Quelle der Wahrheit
	// dafür (siehe onCanvasSelect), Panel hält keine eigene Kopie.
	selectedSoundObject {
		^spaceCanvas.selectedSoundObject
	}

	play { |updateRate = 20|
		var dt = 1.0 / updateRate;
		var leftWidth = 460;
		var totalWidth = 860;
		var totalHeight = 760;

		window = Window.new("Spatial-Control-Panel", Rect(100, 100, totalWidth, totalHeight));
		controlsView = CompositeView(window, Rect(leftWidth, 0, totalWidth - leftWidth, totalHeight));

		spaceCanvas = SpaceCanvas.new(room, viewRadius, moveSpeed, rotateSpeed, editable);
		spaceCanvas.onSelect = { |soundObject| this.onCanvasSelect(soundObject) };
		spaceCanvas.build(window, Rect(0, 0, leftWidth, totalHeight));
		this.installKeyActions;
		this.installControls;
		window.front;

		routine = Routine({
			loop {
				spaceCanvas.tick(dt);
				dt.wait;
			}
		}).play(AppClock);

		^this
	}

	show {
		if(window.isNil or: { window.isClosed }) {
			this.play;
		} {
			window.visible_(true);
			window.front;
		};
		^this
	}

	hide {
		window !? {
			if(window.isClosed.not) {
				window.visible_(false);
			};
		};
		^this
	}

	installKeyActions {
		window.view.keyDownAction = { |aView, char|
			spaceCanvas.keyDown(char);
		};
		window.view.keyUpAction = { |aView, char|
			spaceCanvas.keyUp(char);
		};
	}

	// rechter Bedienbereich: oben statische Room-/Hall-Regler (+ optional Szenen-Speichern/
	// Laden, siehe scenesDir), unten dynamische Regler fürs ausgewählte Soundobjekt.
	installControls {
		// 360 reichte für Room-/Hall-/ReverbSend-Regler allein; der optionale Szenen-Bereich
		// (Header + Namensfeld + Dropdown + 2 Buttons, Intent 46) braucht spürbar mehr Platz --
		// 480 lässt beides samt Rand komfortabel Platz (siehe RoomParamsView>>installControls/
		// installSceneControls für die genaue Zeilenzahl).
		var roomHeight = 480;

		roomParamsView = RoomParamsView.new(controlsView,
			Rect(0, 0, controlsView.bounds.width, roomHeight), room, scenesDir);
		roomParamsView.onSceneLoaded = { this.onSceneLoaded };
		roomParamsView.build;

		objectControlsView = SoundObjectControlsView.new(controlsView, roomHeight + 10, presetsDir,
			soloMuteController);
		objectControlsView.rebuild(this.selectedSoundObject);
	}

	// von SpaceCanvas aufgerufen, wenn sich die Selektion (Klick auf ein Soundobjekt) ändert.
	onCanvasSelect { |soundObject|
		soloMuteController.restoreIfActive;
		soloMuteController.applySoloState(soundObject);
		objectControlsView !? { objectControlsView.rebuild(soundObject) };
	}

	// von RoomParamsView aufgerufen, nachdem eine Szene erfolgreich geladen wurde (Intent 46):
	// die bisherige Selektion existiert danach nicht mehr (RoomSceneLibrary.applyTo hat alle
	// Soundobjekte neu aufgebaut) -- Auswahl zurücksetzen und den Objekt-Regler-Bereich auf
	// "kein Soundobjekt ausgewählt" zurückfahren, statt ein totes Soundobjekt weiter anzuzeigen.
	onSceneLoaded {
		spaceCanvas.clearSelection;
		objectControlsView !? { objectControlsView.rebuild(nil) };
	}

	setSoloSelectedOnly { |aBool|
		soloMuteController.setSoloSelectedOnly(aBool, this.selectedSoundObject);
		^this
	}

	stop {
		soloMuteController.reset;
		routine !? { routine.stop };
		spaceCanvas !? { spaceCanvas.stop };
		objectControlsView !? { objectControlsView.free };
		objectControlsView = nil;
		window !? { window.close };
	}

	free {
		this.stop;
		^this
	}
}
