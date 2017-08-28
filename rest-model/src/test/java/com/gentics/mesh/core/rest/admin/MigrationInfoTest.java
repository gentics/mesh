package com.gentics.mesh.core.rest.admin;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MigrationInfoTest {

	@Test
	public void testCompareInfo() {
		MigrationInfo infoA = new MigrationInfo();
		infoA.setStartDate("2017-08-28T13:13:43Z");
		MigrationInfo infoB = new MigrationInfo();
		infoB.setStartDate("2017-08-28T13:13:44Z");
		assertEquals(-1, infoA.compareTo(infoB));
		assertEquals(1, infoB.compareTo(infoA));

		// Check if both are equal
		infoA.setStartDate(infoB.getStartDate());
		assertEquals(0, infoA.compareTo(infoB));
	}
	
}
