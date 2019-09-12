package com.gentics.mesh.plugin.mustread;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

public class MustReadPluginIT {

	@Test
	public void testIt() {
		for (File f : new File("target").listFiles()) {
			System.out.println(f);
		}
		fail("Fail");
	}
}
