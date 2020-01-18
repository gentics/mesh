package com.gentics.mesh.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;

import org.junit.Test;

public class FilesystemUtilTest {

	@Test
	public void testDirectIOCheck() {
		assertFalse(FilesystemUtil.supportsDirectIO(Paths.get("/tank1/test/.check_fs_support")));
		assertTrue(FilesystemUtil.supportsDirectIO(Paths.get("target/.check_fs_support")));
	}
}
