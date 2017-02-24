package com.gentics.mesh.test.context;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.test.TestDataProvider;

import io.vertx.ext.web.RoutingContext;

public abstract class AbstractMeshTest implements TestHelperMethods {

	@Rule
	@ClassRule
	public static MeshTestContext testContext = new MeshTestContext();

	@Override
	public MeshTestContext getTestContext() {
		return testContext;
	}

	/**
	 * Drop all indices and create a new index using the current data.
	 * 
	 * @throws Exception
	 */
	protected void recreateIndices() throws Exception {
		// We potentially modified existing data thus we need to drop all indices and create them and reindex all data
		MeshInternal.get().searchProvider().clear();
		// We need to call init() again in order create missing indices for the created test data
		for (IndexHandler handler : MeshInternal.get().indexHandlerRegistry().getHandlers()) {
			handler.init().await();
		}
		IndexHandlerRegistry registry = MeshInternal.get().indexHandlerRegistry();
		for (IndexHandler handler : registry.getHandlers()) {
			handler.reindexAll().await();
		}
	}

	public String getJson(Node node) throws Exception {
		InternalActionContext ac = mockActionContext("lang=en&version=draft");
		ac.data().put(RouterStorage.PROJECT_CONTEXT_KEY, TestDataProvider.PROJECT_NAME);
		return JsonUtil.toJson(node.transformToRest(ac, 0).toBlocking().value());
	}

	protected void testPermission(GraphPermission perm, MeshCoreVertex<?, ?> element) {
		RoutingContext rc = mockRoutingContext();

		try (Tx tx = db().tx()) {
			role().grantPermissions(element, perm);
			tx.success();
		}

		try (Tx tx = db().tx()) {
			assertTrue("The role {" + role().getName() + "} does not grant permission on element {" + element.getUuid()
					+ "} although we granted those permissions.", role().hasPermission(perm, element));
			assertTrue(
					"The user has no {" + perm.getRestPerm().getName() + "} permission on node {" + element.getUuid()
							+ "/" + element.getClass().getSimpleName() + "}",
					getRequestUser().hasPermission(element, perm));
		}

		try (Tx tx = db().tx()) {
			role().revokePermissions(element, perm);
			rc.data().clear();
			tx.success();
		}

		try (Tx tx = db().tx()) {
			boolean hasPerm = role().hasPermission(perm, element);
			assertFalse("The user's role {" + role().getName() + "} still got {" + perm.getRestPerm().getName()
					+ "} permission on node {" + element.getUuid() + "/" + element.getClass().getSimpleName()
					+ "} although we revoked it.", hasPerm);

			hasPerm = getRequestUser().hasPermission(element, perm);
			assertFalse("The user {" + getRequestUser().getUsername() + "} still got {" + perm.getRestPerm().getName()
					+ "} permission on node {" + element.getUuid() + "/" + element.getClass().getSimpleName()
					+ "} although we revoked it.", hasPerm);
		}
	}

}
