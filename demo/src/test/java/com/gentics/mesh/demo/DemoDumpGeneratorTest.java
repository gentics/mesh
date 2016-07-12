package com.gentics.mesh.demo;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.util.RxDebugger;

@ContextConfiguration(classes = { DemoDumpConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class DemoDumpGeneratorTest {

	static {
		System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "dump");
	}

	@Autowired
	private BootstrapInitializer boot;

	@Autowired
	private DemoDataProvider dataProvider;

	@Autowired
	private SearchProvider searchProvider;

	@Autowired
	private Database db;

	private static DemoDumpGenerator generator = new DemoDumpGenerator();

	@BeforeClass
	public static void cleanupFolders() throws IOException {
		generator.cleanup();
	}

	@Test
	public void testSetup() throws Exception {
		new RxDebugger().start();
		generator.invokeDump(boot, dataProvider);
		NoTrx tx = db.noTrx();
		assertTrue(boot.meshRoot().getProjectRoot().findByName("demo").toBlocking().single().getNodeRoot().findAll().size() > 0);
		User user = boot.meshRoot().getUserRoot().findByUsername("webclient");
		assertNotNull("The webclient user should have been created but could not be found.", user);
		assertFalse("The webclient user should also have at least one group assigned to it.", user.getGroups().isEmpty());
		Group group = user.getGroups().get(0);
		Role role = group.getRoles().get(0);
		assertNotNull("The webclient group should also have a role assigned to it", role);

		assertTrue("The webclient role has not read permission on the user.", role.hasPermission(GraphPermission.READ_PERM, user));
		assertTrue("The webclient user has no permission on itself.", user.hasPermission(user, GraphPermission.READ_PERM));
		assertTrue("The webclient user has no read permission on the user root node..",
				user.hasPermission(boot.meshRoot().getUserRoot(), GraphPermission.READ_PERM));

		assertTrue("We expected to find at least 5 nodes.", boot.meshRoot().getNodeRoot().findAll().size() > 5);
		for (Node node : boot.meshRoot().getNodeRoot().findAll()) {
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(boot.meshRoot().getLanguageRoot().findByLanguageTag("en"));
			String languageTag = container.getLanguage().getLanguageTag();
			String indexName = NodeIndexHandler.getIndexName(node.getProject().getUuid(), node.getProject().getLatestRelease().getUuid(), "draft");
			String documentType = NodeIndexHandler.getDocumentType(container.getSchemaContainerVersion());
			String documentId = NodeIndexHandler.composeDocumentId(node, languageTag);
			if (searchProvider.getDocument(indexName, documentType, documentId).toBlocking().single() == null) {
				String msg = "The search document for node {" + node.getUuid() + "} container {" + languageTag + "} could not be found within index {"
						+ indexName + "} - {" + documentType + "} - {" + documentId + "}";
				fail(msg);
			}
		}
		tx.close();

	}
}
