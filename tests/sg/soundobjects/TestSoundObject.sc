// Test-Doubles für SoundObject: zeichnen nur auf, wie oft stop aufgerufen wird, ohne
// echten Server/Synth zu brauchen.
FakeSoundForSoundObjectTest {
	var <stopCount;
	var <>amp;

	*new { ^super.new.init }

	init { stopCount = 0 }

	setParam { |key, value| this.perform((key ++ "_").asSymbol, value) }

	stop { stopCount = stopCount + 1 }
}

FakeBinauralizerForSoundObjectTest {
	var <stopCount;

	*new { ^super.new.init }

	init { stopCount = 0 }

	stop { stopCount = stopCount + 1 }
}

// Test für SoundObject. Ausführen über run_tests.scd.
TestSoundObject : UnitTest {

	// Bug-Report (Intent 59-Folgearbeit): in oeuvre/messiaen_birds/messiaen_birds.scd kann die
	// individuelle Lebensdauer-Routine eines Vogels UND der Stück-Ende-Cue (~timeline.at(totalDur,
	// ...)) fadeOutAndStop auf demselben SoundObject auslösen, wenn die zufällige Lebensdauer
	// über das Stückende hinausreicht -- beide Routinen laufen unabhängig voneinander weiter.
	// Ohne Schutz führte das zu einem doppelten stop() und damit "/n_free Node ... not found"
	// im Server-Log (zweiter free-Aufruf auf einen bereits freigegebenen Node).
	test_stopIsIdempotent {
		var sound = FakeSoundForSoundObjectTest.new;
		var binauralizer = FakeBinauralizerForSoundObjectTest.new;
		var so = SoundObject.new(Movable.new, sound, binauralizer);

		so.stop;
		so.stop;
		so.stop;

		this.assertEquals(sound.stopCount, 1,
			"sound.stop darf nur beim ersten Aufruf tatsächlich ausgeführt werden");
		this.assertEquals(binauralizer.stopCount, 1,
			"binauralizer.stop ebenfalls nur einmal");
	}

}
