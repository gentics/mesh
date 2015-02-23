package com.gentics.cailun.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Some tests for the uuid utils.
 * 
 * @author johannes2
 *
 */
public class UUIDUtilTest {

	@Test
	public void testIsUUID() {
		String validUUID = "dd5e85cebb7311e49640316caf57479f";
		assertFalse(UUIDUtil.isUUID(""));
		assertFalse(UUIDUtil.isUUID("123"));
		assertFalse(UUIDUtil.isUUID("-1"));
		assertFalse(UUIDUtil.isUUID("1"));
		assertFalse(UUIDUtil.isUUID("0"));
		assertFalse(UUIDUtil.isUUID("/test/1235.html"));
		assertTrue(UUIDUtil.isUUID(validUUID));
		assertFalse(UUIDUtil.isUUID(validUUID.replace("f", "z")));
	}
}
