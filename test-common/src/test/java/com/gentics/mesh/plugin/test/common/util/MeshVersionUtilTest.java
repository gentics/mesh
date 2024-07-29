package com.gentics.mesh.plugin.test.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.MeshVersion;

public class MeshVersionUtilTest {

	@Test
	public void testIsLTSVersion() {
		assertTrue(MeshVersionUtil.isOSS("1.5.0"));
		assertTrue(MeshVersionUtil.isOSS("1.5.1"));
		assertFalse(MeshVersionUtil.isOSS("1.4.9"));
		assertTrue(MeshVersionUtil.isOSS("1.4.8"));
		assertFalse(MeshVersionUtil.isOSS("1.3.4"));
	}

	@Test
	public void testMatchingImage() {
		String image = MeshVersionUtil.getMatchingImage();
		if (MeshVersionUtil.isOSS(MeshVersion.getPlainVersion())) {
			assertThat(image).startsWith("gentics/mesh");
		} else {
			assertThat(image).startsWith("docker");
		}
	}
}
