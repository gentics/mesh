package com.gentics.mesh.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;

public abstract class AbstractBasicObjectTest extends AbstractDBTest implements BasicObjectTestcases {

	@Before
	public void setup() throws Exception {
		setupData();
	}

	protected void testPermission(Permission perm, GenericVertex<?> node) {
		role().addPermissions(node, perm);
		assertTrue(role().hasPermission(perm, node));
		assertTrue(getRequestUser().hasPermission(node, perm));
		role().revokePermissions(node, perm);
		assertFalse(role().hasPermission(perm, node));
		assertFalse(getRequestUser().hasPermission(node, perm));
	}

}
