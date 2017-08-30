package com.gentics.mesh.distributed.containers;

import static org.junit.Assert.assertNotEquals;

import java.io.IOException;

import org.junit.Test;

public class TestUnixUtils {

	@Test
	public void testGetUid() throws IOException {
		int uid = UnixUtils.getUid();
		assertNotEquals(0, uid);
	}
}
