package com.gentics.mesh.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.Trx;

public abstract class AbstractBasicObjectTest extends AbstractDBTest implements BasicObjectTestcases {

	@Autowired
	private DatabaseService databaseService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@After
	public void cleanup() {
		BootstrapInitializer.clearReferences();
//			databaseService.getDatabase().clear();
		databaseService.getDatabase().reset();
	}

	protected void testPermission(GraphPermission perm, GenericVertex<?> node) {
		try (Trx tx = db.trx()) {
			role().grantPermissions(node, perm);
			assertTrue(role().hasPermission(perm, node));
			assertTrue(getRequestUser().hasPermission(node, perm));
			role().revokePermissions(node, perm);
			assertFalse(role().hasPermission(perm, node));
			assertFalse(getRequestUser().hasPermission(node, perm));
		}
	}

}
