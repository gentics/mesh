package com.gentics.mesh.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gentics.mesh.util.VersionUtil;

public class VersionUtilTest {

	@Test
	public void testCompareVersions() throws Exception {
		assertEquals(0, VersionUtil.compareVersions("1.0.0", "1.0.0"));
		assertEquals(-1, VersionUtil.compareVersions("1.0.0", "1.0.1"));
		assertEquals(-1, VersionUtil.compareVersions("0.9.28", "0.10.0"));
		assertEquals(1, VersionUtil.compareVersions("1.0.1", "1.0.0"));
		assertEquals(1, VersionUtil.compareVersions("0.10.0","0.9.28"));
	}

	@Test(expected = NullPointerException.class)
	public void testName() throws Exception {
		VersionUtil.compareVersions(null, null);
	}
}
