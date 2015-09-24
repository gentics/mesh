package com.gentics.mesh.graphdb;

import org.junit.Test;

import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;

public class DatabaseTest extends AbstractBasicDBTest {

	@Test
	public void testReload() {
		user().reload();
	}
}
