package com.gentics.mesh.demo;

import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

public class DemoDumpGeneratorTest {

	private BootstrapInitializer boot;

	private SearchProvider searchProvider;

	private Database db;

	private static DemoDumpGenerator generator = new DemoDumpGenerator();

	@BeforeClass
	public static void cleanupFolders() throws Exception {
		generator.cleanup();
		generator.init();
	}

	@Before
	public void setup() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
		MeshComponent meshDagger = MeshInternal.create(new MeshOptions());
		boot = meshDagger.boot();
		searchProvider = meshDagger.searchProvider();
		db = meshDagger.database();
	}

	@Test
	public void testSetup() throws Exception {
		generator.dump();
		try (Tx tx = db.tx()) {
			assertTrue(boot.meshRoot().getProjectRoot().findByName("demo").getNodeRoot().computeCount() > 0);
			User user = boot.meshRoot().getUserRoot().findByUsername("webclient");
			assertNotNull("The webclient user should have been created but could not be found.", user);
			assertFalse("The webclient user should also have at least one group assigned to it.", !user.getGroups().iterator().hasNext());
			Group group = user.getGroups().iterator().next();
			Role role = group.getRoles().iterator().next();
			assertNotNull("The webclient group should also have a role assigned to it", role);

			assertTrue("The webclient role has not read permission on the user.", role.hasPermission(GraphPermission.READ_PERM, user));
			assertTrue("The webclient user has no permission on itself.", user.hasPermission(user, GraphPermission.READ_PERM));
			assertTrue("The webclient user has no read permission on the user root node..", user.hasPermission(boot.meshRoot().getUserRoot(),
					GraphPermission.READ_PERM));

			assertTrue("We expected to find at least 5 nodes.", boot.meshRoot().getNodeRoot().computeCount() > 5);

			// Verify that the uuids have been updated
			assertNotNull(boot.meshRoot().getNodeRoot().findByUuid("df8beb3922c94ea28beb3922c94ea2f6"));

			// Verify that all documents are stored in the index
			for (Node node : boot.meshRoot().getNodeRoot().findAll()) {
				NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(boot.meshRoot().getLanguageRoot().findByLanguageTag("en"));
				String languageTag = "en";
				String projectUuid = node.getProject().getUuid();
				String branchUuid = node.getProject().getInitialBranch().getUuid();
				String schemaContainerVersionUuid = container.getSchemaContainerVersion().getUuid();
				ContainerType type = PUBLISHED;
				String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid, schemaContainerVersionUuid, type);
				String documentId = NodeGraphFieldContainer.composeDocumentId(node.getUuid(), languageTag);
				if (searchProvider.getDocument(indexName, documentId).blockingGet() == null) {
					String msg = "The search document for node {" + node.getUuid() + "} container {" + languageTag
							+ "} could not be found within index {" + indexName + "} - {" + documentId + "}";
					fail(msg);
				}
			}
		}
	}
}
