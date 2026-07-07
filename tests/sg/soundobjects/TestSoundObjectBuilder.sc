// Test-Doubles für SoundObjectBuilder: reine Marker-Klassen mit denselben <>-Setter-Namen wie
// InsectSound/Movable, ohne deren Server-/Bewegungslogik — Merge-/Klon-Semantik kommt ohnehin
// von Event (SC-Core), getestet wird hier nur die *build-Instanziierung selbst.
FakeSoundForSoundObjectBuilderTest {
	var <>wingRate;
	var <>amp;
}

FakeMovableForSoundObjectBuilderTest {
	var <>moveRule;
	var <>pos;
}

// Test für SoundObjectBuilder. Ausführen über run_tests.scd.
TestSoundObjectBuilder : UnitTest {

	test_buildAppliesSoundParamsViaSetters {
		var built = SoundObjectBuilder.build((wingRate: 280, amp: 0.3), (),
			soundClass: FakeSoundForSoundObjectBuilderTest);

		this.assertEquals(built.sound.wingRate, 280);
		this.assertEquals(built.sound.amp, 0.3);
	}

	test_buildAppliesMovableParamsViaSetters {
		var moveRule = \someMoveRule;
		var built = SoundObjectBuilder.build((), (moveRule: moveRule),
			movableClass: FakeMovableForSoundObjectBuilderTest);

		this.assertEquals(built.movable.moveRule, moveRule);
	}

	test_buildDefaultsToInsectSoundAndMovable {
		var built = SoundObjectBuilder.build((wingRate: 280), (roomRadius: 4));

		this.assert(built.sound.isKindOf(InsectSound),
			"ohne soundClass-Override wird InsectSound gebaut");
		this.assert(built.movable.isKindOf(Movable),
			"ohne movableClass-Override wird Movable gebaut");
		this.assertEquals(built.sound.wingRate, 280);
		this.assertEquals(built.movable.roomRadius, 4);
	}

	test_buildPassesBinauralizerThrough {
		var binauralizer = \someBinauralizer;
		var built = SoundObjectBuilder.build((), (), binauralizer);

		this.assertEquals(built.binauralizer, binauralizer);
	}

	test_buildWithoutBinauralizerUsesSoundObjectsDefault {
		var built = SoundObjectBuilder.build((), ());

		this.assert(built.binauralizer.isKindOf(Binauralizer),
			"ohne binauralizer greift SoundObjects eigenes Fallback-Verhalten");
	}

	test_variantViaEventMergeOverridesOnlyGivenParams {
		var baseParams = (wingRate: 280, amp: 0.3);
		var variantParams = baseParams ++ (amp: 0.05);
		var built = SoundObjectBuilder.build(variantParams, (),
			soundClass: FakeSoundForSoundObjectBuilderTest);

		this.assertEquals(built.sound.wingRate, 280,
			"von der Basis übernommen, nicht in der Variante überschrieben");
		this.assertEquals(built.sound.amp, 0.05, "in der Variante überschrieben");
		this.assertEquals(baseParams.amp, 0.3,
			"Event>>++ liefert ein neues Event, die Basis bleibt unverändert");
	}

	test_twoBuildCallsFromSameParamsAreIndependentInstances {
		var soundParams = (wingRate: 280);
		var movableParams = (moveRule: \someMoveRule);
		var first = SoundObjectBuilder.build(soundParams, movableParams,
			soundClass: FakeSoundForSoundObjectBuilderTest,
			movableClass: FakeMovableForSoundObjectBuilderTest);
		var second = SoundObjectBuilder.build(soundParams, movableParams,
			soundClass: FakeSoundForSoundObjectBuilderTest,
			movableClass: FakeMovableForSoundObjectBuilderTest);

		first.sound.wingRate = 999;
		first.movable.pos = [1, 2, 3];

		this.assertEquals(second.sound.wingRate, 280,
			"kein geteilter Sound-State zwischen zwei build-Aufrufen");
		this.assert(second.movable.pos != [1, 2, 3],
			"kein geteilter Movable-State zwischen zwei build-Aufrufen");
	}
}
