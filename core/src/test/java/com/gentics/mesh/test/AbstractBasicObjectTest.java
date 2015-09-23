package com.gentics.mesh.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;

public abstract class AbstractBasicObjectTest extends AbstractBasicDBTest implements BasicObjectTestcases {

	protected void testPermission(GraphPermission perm, GenericVertex<?> node) {
		role().grantPermissions(node, perm);
		assertTrue(role().hasPermission(perm, node));
		assertTrue(getRequestUser().hasPermission(node, perm));
		role().revokePermissions(node, perm);
		assertFalse(role().hasPermission(perm, node));
		assertFalse(getRequestUser().hasPermission(node, perm));
	}

}
