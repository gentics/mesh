package com.gentics.mesh.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.graphdb.DatabaseService;

public abstract class AbstractBasicObjectTest extends AbstractDBTest implements BasicObjectTestcases {

	@Autowired
	private DatabaseService databaseService;

	@Before
	public void setup() throws Exception {
		databaseService.getDatabase().clear();
		databaseService.getDatabase().reset();
		setupData();
	}

	protected void testPermission(GraphPermission perm, GenericVertex<?> node) {
		role().grantPermissions(node, perm);
		assertTrue(role().hasPermission(perm, node));
		assertTrue(getRequestUser().hasPermission(node, perm));
		role().revokePermissions(node, perm);
		assertFalse(role().hasPermission(perm, node));
		assertFalse(getRequestUser().hasPermission(node, perm));
	}

}
