package com.gentics.cailun.verticle.file;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.test.AbstractProjectRestVerticleTest;

public class FileVerticleTest extends AbstractProjectRestVerticleTest {

	@Autowired
	private FileVerticle fileVerticle;

	@Override
	public AbstractProjectRestVerticle getVerticle() {
		return fileVerticle;
	}

	@Test
	public void testReadFile() {
		fail("Not yet implemented");
	}

}
