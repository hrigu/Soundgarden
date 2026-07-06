// KeyboardListenerControl — bewegt/dreht einen Listener kontinuierlich per Tastatur, solange
// eine Taste gehalten wird. Braucht ein fokussiertes Fenster (SuperCollider-Standardidiom für
// Tastatur-Input — keyDownAction/keyUpAction feuern nur, wenn das Fenster den Fokus hat).
// Tastenbelegung: W/S vor/zurück, A/D seitlich, Q/E drehen (im Uhrzeigersinn).
KeyboardListenerControl {
	var <>listener;
	var <>moveSpeed;    // Meter/Sekunde
	var <>rotateSpeed;  // Grad/Sekunde
	var <window;
	var heldKeys;
	var routine;

	*new { |listener, moveSpeed = 2, rotateSpeed = 90|
		^super.new.init(listener, moveSpeed, rotateSpeed);
	}

	init { |aListener, aMoveSpeed, aRotateSpeed|
		listener = aListener;
		moveSpeed = aMoveSpeed;
		rotateSpeed = aRotateSpeed;
		heldKeys = Set.new;
	}

	// öffnet das Kontrollfenster und startet die Tick-Routine. updateRate: wie oft pro
	// Sekunde die gehaltenen Tasten in Bewegung umgesetzt werden.
	play { |updateRate = 30|
		var dt = 1.0 / updateRate;

		window = Window.new("Listener-Steuerung (W/S/A/D/Q/E)", Rect(100, 100, 320, 120));
		window.view.keyDownAction = { |v, char| heldKeys.add(char.asString.toLower) };
		window.view.keyUpAction = { |v, char| heldKeys.remove(char.asString.toLower) };
		window.front;

		routine = Routine({
			loop {
				if(heldKeys.includes("w")) { listener.moveForward(moveSpeed * dt) };
				if(heldKeys.includes("s")) { listener.moveBackward(moveSpeed * dt) };
				if(heldKeys.includes("a")) { listener.strafeLeft(moveSpeed * dt) };
				if(heldKeys.includes("d")) { listener.strafeRight(moveSpeed * dt) };
				if(heldKeys.includes("q")) { listener.rotate(rotateSpeed.neg * dt) };
				if(heldKeys.includes("e")) { listener.rotate(rotateSpeed * dt) };
				dt.wait;
			}
		}).play;

		^this
	}

	// beendet das Ticken und schließt das Fenster.
	stop {
		routine !? { routine.stop };
		window !? { window.close };
		heldKeys = Set.new;
	}
}
