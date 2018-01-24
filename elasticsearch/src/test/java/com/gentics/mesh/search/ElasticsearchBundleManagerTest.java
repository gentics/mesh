package com.gentics.mesh.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import net.lingala.zip4j.exception.ZipException;

public class ElasticsearchBundleManagerTest {

	@Test
	public void testExec() throws IOException, ZipException, InterruptedException {
		Process p = ElasticsearchBundleManager.start();
		//TODO assert process is started
		Thread.sleep(4000);
		assertTrue(p.isAlive());
		p.destroy();
		Thread.sleep(2000);
		assertFalse(p.isAlive());
		//TODO assert for dangling process
	}

}
