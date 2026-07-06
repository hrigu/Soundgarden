// Test für SteadyMoveRule. Ausführen über run_tests.scd.
TestSteadyMoveRule : UnitTest {

	test_stateAtOriginRemainsUnchanged {
		var rule = SteadyMoveRule.new;
		var pos = rule.next(#[0, 0, 0], 0, 0.033);

		this.assertEquals(pos, #[0, 0, 0], "bleibt am Ursprung, unabhängig von t/dt");
	}

	test_arbitraryPositionRemainsUnchangedRegardlessOfTAndDt {
		var rule = SteadyMoveRule.new;
		var pos = rule.next(#[1.5, -2.5, 0.5], 42, 0.5);

		this.assertEquals(pos, #[1.5, -2.5, 0.5], "bleibt an beliebiger Position, auch bei großem t/dt");
	}
}
