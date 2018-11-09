package com.gentics.mesh.etc.config.env;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class SubOptionTest {
	private static final String ENV_KEY = "TEST_ENV_KEY";
	private static final String ENV_VALUE = "TEST_ENV_VALUE";

	@Rule
	public EnvironmentVariables environmentVariables = new EnvironmentVariables();

	@Test
	public void overrideSubOptions() {
		environmentVariables.set(ENV_KEY, ENV_VALUE);
		SubOption root = new SubOption();
		root.subOption = new SubOption2();
		root.subOption.subOption = new SubOption();
		assertNull(root.value);
		assertNull(root.subOption.value);
		assertNull(root.subOption.subOption.value);
		root.overrideWithEnv();
		assertEquals(ENV_VALUE, root.value);
		assertEquals(ENV_VALUE, root.subOption.value);
		assertEquals(ENV_VALUE, root.subOption.subOption.value);
	}


	private class SubOption implements Option {
		@EnvironmentVariable(name = ENV_KEY, description = "")
		private String value;

		private SubOption2 subOption;

	}

	private class SubOption2 implements Option {
		@EnvironmentVariable(name = ENV_KEY, description = "")
		private String value;

		private SubOption subOption;

	}
}
