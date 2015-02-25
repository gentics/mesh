package com.gentics.cailun.verticle.file;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class FileVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private FileVerticle fileVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return fileVerticle;
	}

	@Test
	public void testReadFile() {
		fail("Not yet implemented");
	}

}
