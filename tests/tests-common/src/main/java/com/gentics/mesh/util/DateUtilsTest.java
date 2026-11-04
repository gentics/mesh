package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Instant;

import org.junit.Test;

public class DateUtilsTest {

	@Test
	public void testDate() {
		assertNotNull(DateUtils.fromISO8601("2017-07-25T12:40:00Z"));
		assertNotNull(DateUtils.fromISO8601("2017-07-25T12:40:00+00:00"));
		assertNotNull(DateUtils.fromISO8601("2017-07-25T12:40:00+01:00"));
		assertNotNull(DateUtils.fromISO8601("2017-07-25T12:40:00"));
		assertNull(DateUtils.fromISO8601("Bogus"));
		assertNull(DateUtils.fromISO8601("0"));
		assertNull(DateUtils.fromISO8601("1"));
		assertNull(DateUtils.fromISO8601("42"));
	}

	@Test
	public void testFromInstant() {
		Instant now = Instant.now();
		assertNull(DateUtils.instantToMillis(null, false));
		assertNull(DateUtils.instantToMillis(null, true));
		assertNull(DateUtils.instantToMillis(Instant.ofEpochMilli(0), false));
		assertEquals(DateUtils.instantToMillis(Instant.ofEpochMilli(0), true), Long.valueOf(0));
		assertEquals(DateUtils.instantToMillis(now, false), Long.valueOf(now.toEpochMilli()));
		assertEquals(DateUtils.instantToMillis(now, true), Long.valueOf(now.toEpochMilli()));
	}
}
