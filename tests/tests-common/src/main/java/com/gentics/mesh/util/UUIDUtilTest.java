package com.gentics.mesh.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Some tests for the uuid utils.
 */
public class UUIDUtilTest {

	@Test
	public void testIsUUID() {
		String validUUID = "dd5e85cebb7311e49640316caf57479f";
		String uuid = UUIDUtil.randomUUID();
		System.out.println(uuid);
		assertTrue("The uuid {" + uuid + "} is not a valid uuid.", UUIDUtil.isUUID(uuid));
		assertFalse(UUIDUtil.isUUID(""));
		assertFalse(UUIDUtil.isUUID("123"));
		assertFalse(UUIDUtil.isUUID("-1"));
		assertFalse(UUIDUtil.isUUID("1"));
		assertFalse(UUIDUtil.isUUID("0"));
		assertFalse(UUIDUtil.isUUID("/test/1235.html"));
		assertTrue(UUIDUtil.isUUID(validUUID));
		assertFalse(UUIDUtil.isUUID(validUUID.replace("f", "z")));
	}

	@Test
	public void testConversion() {
		String fullUuid = "8fdfd492-e005-4503-875e-13d73c633b2c";
		String shortUuid = UUIDUtil.toShortUuid(fullUuid);
		assertEquals("8fdfd492e0054503875e13d73c633b2c", shortUuid);
		assertEquals("8fdfd492e0054503875e13d73c633b2c", UUIDUtil.toShortUuid(shortUuid));
		assertEquals(fullUuid, UUIDUtil.toFullUuid(shortUuid));
		assertEquals(fullUuid, UUIDUtil.toFullUuid(fullUuid));
	}
}
