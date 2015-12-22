package com.gentics.mesh.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.ext.web.RoutingContext;

public abstract class AbstractBasicObjectTest extends AbstractBasicDBTest implements BasicObjectTestcases {

	protected void testPermission(GraphPermission perm, MeshCoreVertex<?, ?> element) {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);

		try (Trx tx = db.trx()) {
			role().grantPermissions(element, perm);
			tx.success();
		}

		try (Trx tx = db.trx()) {
			assertTrue("The role {" + role().getName() + "} does not grant permission on element {" + element.getUuid()
					+ "} although we granted those permissions.", role().hasPermission(perm, element));
			assertTrue("The user has no {" + perm.getSimpleName() + "} permission on node {" + element.getUuid() + "/" + element.getType() + "}",
					getRequestUser().hasPermissionAsync(ac, element, perm).toBlocking().first());
		}

		try (Trx tx = db.trx()) {
			role().revokePermissions(element, perm);
			rc.data().clear();
		}

		try (Trx tx = db.trx()) {
			assertFalse("The user's role {" + role().getName() + "} still got {" + perm.getSimpleName() + "} permission on node {" + element.getUuid()
					+ "/" + element.getType() + "} although we revoked it.", role().hasPermission(perm, element));

			boolean hasPerm = getRequestUser().hasPermissionAsync(ac, element, perm).toBlocking().first();
			assertFalse("The user {" + getRequestUser().getUsername() + "} still got {" + perm.getSimpleName() + "} permission on node {"
					+ element.getUuid() + "/" + element.getType() + "} although we revoked it.", hasPerm);
		}
	}

}
