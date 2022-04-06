package com.gentics.mesh.util;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;

public class UUIDUtilTest {

	@Test
	public void testToFromBytes() {
		UUID uuid = UUIDUtil.toJavaUuid(UUIDUtil.randomUUID());
		byte[] bytes = UUIDUtil.toBytes(uuid);
		UUID copyUUid = UUIDUtil.toJavaUuid(bytes);
		assertEquals(uuid, copyUUid);
	}

}
