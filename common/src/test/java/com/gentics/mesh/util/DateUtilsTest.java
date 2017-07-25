package com.gentics.mesh.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class DateUtilsTest {

	@Test
	public void testDate() {
		assertNotNull(DateUtils.fromISO8601("2017-07-25T12:40:00Z"));
		assertNotNull(DateUtils.fromISO8601("2017-07-25T12:40:00+00:00"));
		assertNotNull(DateUtils.fromISO8601("2017-07-25T12:40:00+01:00"));
		assertNotNull(DateUtils.fromISO8601("2017-07-25T12:40:00"));
	}
}
