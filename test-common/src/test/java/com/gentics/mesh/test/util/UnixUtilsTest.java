package com.gentics.mesh.test.util;

import static org.junit.Assert.assertNotEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.test.util.UnixUtils;

public class UnixUtilsTest {

	@Test
	public void testGetUid() throws IOException {
		int uid = UnixUtils.getUid();
		assertNotEquals(0, uid);
	}
}
