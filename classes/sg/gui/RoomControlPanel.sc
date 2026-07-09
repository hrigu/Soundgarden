// RoomControlPanel — dünner Demo-Wrapper um SpatialControlPanel. Hält genau eine interne
// Instanz und bietet das kleine API new/show/hide/free, damit Demos nicht die ganze
// SpatialControlPanel-Lifecycle-Logik direkt kennen müssen.
RoomControlPanel {
	var <>room;
	var <>viewRadius;
	var <>moveSpeed;
	var <>rotateSpeed;
	var <>editable;
	var <>presetsDir;
	var <panel;

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
	}

	buildPanel {
		^SpatialControlPanel.new(room, viewRadius, moveSpeed, rotateSpeed, editable, presetsDir);
	}

	ensurePanel {
		if(panel.isNil or: { panel.window.isNil } or: { panel.window.isClosed }) {
			panel = this.buildPanel;
		};
		^panel
	}

	show {
		this.ensurePanel.show;
		^this
	}

	hide {
		panel !? {
			panel.hide;
		};
		^this
	}

	free {
		panel !? {
			panel.free;
			panel = nil;
		};
		^this
	}
}
