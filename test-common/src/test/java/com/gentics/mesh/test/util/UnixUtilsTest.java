package com.gentics.mesh.test.util;

import static org.apache.commons.lang3.SystemUtils.IS_OS_UNIX;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class UnixUtilsTest {

	@Before
	public void isUnix() {
		assumeTrue(IS_OS_UNIX);
	}

	@Test
	public void testGetUid() throws IOException {
		int uid = UnixUtils.getUid();
		assertNotEquals(0, uid);
	}

}
