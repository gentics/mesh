package com.gentics.mesh.test.context;

import io.vertx.core.json.JsonArray;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;

/**
 * This is an abstract class for parametrized tests using the
 * <a href="https://github.com/minimaxir/big-list-of-naughty-strings">big list of naughty strings.</a>
 */
@RunWith(Parameterized.class)
public abstract class AbstractNaughtyStringTest extends AbstractMeshTest {

	@Parameters
	public static Iterable<Object[]> strings() throws IOException {
		String rawInput = IOUtils.toString(
			AbstractNaughtyStringTest.class.getResourceAsStream("/json/blns.json"),
			"utf-8"
		);

		return new JsonArray(rawInput).stream()
			.map(str -> new Object[]{str})
			::iterator;
	}

	@Parameter
	public String input;
}
