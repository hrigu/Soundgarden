// Test für Movable>>moveTo. Ausführen über run_tests.scd.
TestMovable : UnitTest {

	test_moveToSetsPositionDirectly {
		var movable = Movable.new([0, 2, 0], SteadyMoveRule.new, roomRadius: 5);
		movable.moveTo([1, 1, 1]);

		this.assertEquals(movable.pos, [1, 1, 1], "pos wird direkt auf newPos gesetzt");
	}

	test_moveToIgnoresRoomRadius {
		var movable = Movable.new([0, 0, 0], SteadyMoveRule.new, roomRadius: 5);
		movable.moveTo([100, 0, 0]);

		this.assertEquals(movable.pos, [100, 0, 0],
			"moveTo platziert auch weit außerhalb von roomRadius, ohne Rückführung");
	}
}
