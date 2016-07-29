package com.gentics.mesh.test;

import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.AbstractIsolatedBasicDBTest;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.graphdb.Tx;

import io.vertx.ext.web.RoutingContext;

public abstract class AbstractBasicIsolatedObjectTest extends AbstractIsolatedBasicDBTest implements BasicObjectTestcases {

	protected void testPermission(GraphPermission perm, MeshCoreVertex<?, ?> element) {
		RoutingContext rc = getMockedRoutingContext();
		InternalActionContext ac = InternalActionContext.create(rc);

		try (Tx tx = db.tx()) {
			role().grantPermissions(element, perm);
			tx.success();
		}

		try (Tx tx = db.tx()) {
			assertTrue("The role {" + role().getName() + "} does not grant permission on element {" + element.getUuid()
					+ "} although we granted those permissions.", role().hasPermission(perm, element));
			assertTrue("The user has no {" + perm.getSimpleName() + "} permission on node {" + element.getUuid() + "/" + element.getType() + "}",
					getRequestUser().hasPermissionAsync(ac, element, perm).toBlocking().value());
		}

		try (Tx tx = db.tx()) {
			role().revokePermissions(element, perm);
			rc.data().clear();
			tx.success();
		}

		try (Tx tx = db.tx()) {
			boolean hasPerm = role().hasPermission(perm, element);
			assertFalse("The user's role {" + role().getName() + "} still got {" + perm.getSimpleName() + "} permission on node {" + element.getUuid()
					+ "/" + element.getType() + "} although we revoked it.", hasPerm);

			hasPerm = getRequestUser().hasPermissionAsync(ac, element, perm).toBlocking().value();
			assertFalse("The user {" + getRequestUser().getUsername() + "} still got {" + perm.getSimpleName() + "} permission on node {"
					+ element.getUuid() + "/" + element.getType() + "} although we revoked it.", hasPerm);
		}
	}

}
