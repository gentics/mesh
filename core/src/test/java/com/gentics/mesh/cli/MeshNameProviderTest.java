package com.gentics.mesh.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class MeshNameProviderTest {

	@Test
	public void testGetRandomName() {
		assertNotNull(MeshNameProvider.getInstance().getRandomName());
		for (int i = 0; i < 30000; i++) {
			assertNotNull(MeshNameProvider.getInstance().getRandomName());
		}
	}

	@Test
	public void testGetName() throws Exception {
		String name = MeshNameProvider.getInstance().getName();
		System.out.println("Got name: {" + name + "}");
		assertFalse(StringUtils.isEmpty(name));
		assertEquals(name, MeshNameProvider.getInstance().getName());
		MeshNameProvider.getInstance().reset();
		String newName = MeshNameProvider.getInstance().getName();
		assertFalse(StringUtils.isEmpty(newName));
		assertEquals(newName, MeshNameProvider.getInstance().getName());
		assertNotEquals(newName, name);
	}
}
