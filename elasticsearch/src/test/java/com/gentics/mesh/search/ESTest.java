package com.gentics.mesh.search;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import net.lingala.zip4j.exception.ZipException;

public class ESTest {

	@Test
	@Ignore
	public void testExec() throws IOException, ZipException {
		ElasticsearchBundleManager.start();
		System.in.read();
	}

}
