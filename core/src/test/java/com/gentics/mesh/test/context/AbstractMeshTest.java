package com.gentics.mesh.test.context;

import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.search.DummySearchProvider;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.test.TestDataProvider;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public abstract class AbstractMeshTest implements TestHelperMethods {

	@Rule
	@ClassRule
	public static MeshTestContext testContext = new MeshTestContext();

	protected void testPermission(GraphPermission perm, MeshCoreVertex<?, ?> element) {
		RoutingContext rc = getMockedRoutingContext();

		try (Tx tx = db().tx()) {
			role().grantPermissions(element, perm);
			tx.success();
		}

		try (Tx tx = db().tx()) {
			assertTrue("The role {" + role().getName() + "} does not grant permission on element {" + element.getUuid()
					+ "} although we granted those permissions.", role().hasPermission(perm, element));
			assertTrue("The user has no {" + perm.getRestPerm().getName() + "} permission on node {" + element.getUuid() + "/"
					+ element.getClass().getSimpleName() + "}", getRequestUser().hasPermission(element, perm));
		}

		try (Tx tx = db().tx()) {
			role().revokePermissions(element, perm);
			rc.data().clear();
			tx.success();
		}

		try (Tx tx = db().tx()) {
			boolean hasPerm = role().hasPermission(perm, element);
			assertFalse("The user's role {" + role().getName() + "} still got {" + perm.getRestPerm().getName() + "} permission on node {"
					+ element.getUuid() + "/" + element.getClass().getSimpleName() + "} although we revoked it.", hasPerm);

			hasPerm = getRequestUser().hasPermission(element, perm);
			assertFalse("The user {" + getRequestUser().getUsername() + "} still got {" + perm.getRestPerm().getName() + "} permission on node {"
					+ element.getUuid() + "/" + element.getClass().getSimpleName() + "} although we revoked it.", hasPerm);
		}
	}

	@Override
	public Database db() {
		return MeshInternal.get().database();
	}

	protected BootstrapInitializer boot() {
		return MeshInternal.get().boot();
	}

	public TestDataProvider data() {
		return testContext.getData();
	}

	public Role role() {
		return data().role();
	}

	public User getRequestUser() {
		return data().getUserInfo().getUser().reframe(MeshAuthUserImpl.class);
	}

	public User user() {
		return data().user();
	}

	public MeshRoot meshRoot() {
		return data().getMeshRoot();
	}

	public Group group() {
		return data().getUserInfo().getGroup();
	}

	public MeshRestClient client() {
		return testContext.getClient();
	}

	public DummySearchProvider dummySearchProvider() {
		return testContext.getDummySearchProvider();
	}

	@Override
	public Project project() {
		return data().getProject();
	}

	@Override
	public int port() {
		return testContext.getPort();
	}

	@Override
	public Node folder(String key) {
		return data().getFolder(key);
	}

	@Override
	public Node content(String key) {
		return data().getContent(key);
	}

	@Override
	public TagFamily tagFamily(String key) {
		TagFamily family = data().getTagFamily(key);
		family.reload();
		return family;
	}

	@Override
	public Tag tag(String key) {
		Tag tag = data().getTag(key);
		tag.reload();
		return tag;
	}

	@Override
	public Language english() {
		return data().getEnglish();
	}

	@Override
	public Language german() {
		return data().getGerman();
	}

	@Override
	public SchemaContainer schemaContainer(String key) {
		SchemaContainer container = data().getSchemaContainer(key);
		container.reload();
		return container;
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

	public Vertx vertx() {
		return testContext.getVertx();
	}

	public SearchQueueBatch createBatch() {
		return MeshInternal.get().searchQueue().create();
	}

	public Map<String, User> users() {
		return data().getUsers();
	}

}
