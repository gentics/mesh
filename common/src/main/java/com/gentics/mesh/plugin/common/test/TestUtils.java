package com.gentics.mesh.plugin.common.test;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class TestUtils {

	public static String loadResource(String resource) {
		try {
			return IOUtils.toString(TestUtils.class.getResourceAsStream(resource), Charsets.UTF_8).trim();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
