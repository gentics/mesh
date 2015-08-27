package com.gentics.mesh.cli;

import static com.gentics.mesh.cli.MeshNameProvider.getName;
import static com.gentics.mesh.cli.MeshNameProvider.getRandomName;
import static com.gentics.mesh.cli.MeshNameProvider.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class MeshNameProviderTest {

	@Test
	public void testGetRandomName() {
		assertNotNull(getRandomName());
		for (int i = 0; i < 10000; i++) {
			assertNotNull(getRandomName());
		}
	}

	@Test
	public void testGetName() {
		String name = getName();
		assertFalse(StringUtils.isEmpty(name));
		assertEquals(name, getName());
		reset();
		String newName = getName();
		assertFalse(StringUtils.isEmpty(newName));
		assertEquals(newName, getName());
		assertNotEquals(newName, name);
	}
}
