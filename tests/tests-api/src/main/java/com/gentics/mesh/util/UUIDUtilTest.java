package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.Test;

public class UUIDUtilTest {

	@Test
	public void testToFromBytes() {
		UUID uuid = UUIDUtil.toJavaUuid(UUIDUtil.randomUUID());
		byte[] bytes = UUIDUtil.toBytes(uuid);
		assertNotNull(bytes);
		UUID copyUUid = UUIDUtil.toJavaUuid(bytes);
		assertEquals(uuid, copyUUid);
	}

	@Test
	public void testUuidGenerator() {
		assertNotNull(UUIDUtil.randomUUID().toUpperCase());
	}
}
