package com.gentics.mesh.test.assertj;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.test.context.MeshTestContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshTestContextAssert extends AbstractAssert<MeshTestContextAssert, MeshTestContext> {

	private static final Logger log = LoggerFactory.getLogger(MeshTestContextAssert.class);

	public MeshTestContextAssert(MeshTestContext actual) {
		super(actual, MeshTestContextAssert.class);
	}

	public void hasUploads(int expected) throws IOException {
		String dir = actual.getOptions().getUploadOptions().getDirectory();
		long count = Files.walk(Paths.get(dir))
			.filter(Files::isRegularFile)
			.peek(log::info)
			.count();
		assertEquals("The upload folder did not contain the expected amount of files.", expected, count);
	}

}
