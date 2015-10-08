package com.gentics.mesh.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.ext.web.RoutingContext;

public abstract class AbstractBasicObjectTest extends AbstractBasicDBTest implements BasicObjectTestcases {

	protected void testPermission(GraphPermission perm, GenericVertex<?> node) {
		try (Trx tx = db.trx()) {
			role().grantPermissions(node, perm);
			tx.success();
		}

		try (Trx tx = db.trx()) {
			RoutingContext rc = getMockedRoutingContext("");
			InternalActionContext ac = InternalActionContext.create(rc);
			assertTrue(role().hasPermission(perm, node));
			assertTrue(getRequestUser().hasPermission(ac, node, perm));
			role().revokePermissions(node, perm);
			rc.data().clear();
			assertFalse(role().hasPermission(perm, node));
			assertFalse(getRequestUser().hasPermission(ac, node, perm));
		}
	}

}
