package com.gentics.mesh.core.tag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.test.AbstractDBTest;

public class AtomicTagTest extends AbstractDBTest {

	@Test
	public void testTagCreation() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			MeshRoot meshRoot = boot.meshRoot();
			User user = meshRoot.getUserRoot().create("test", null);
			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			assertNotNull(languageRoot);
			languageRoot.create("Deutsch", "de");
			languageRoot.create("English", "en");

			Schema schema = new SchemaModel();
			schema.setName("folder");
			schema.setDisplayField("name");
			meshRoot.getSchemaContainerRoot().create(schema, user);

			meshRoot.getTagFamilyRoot();
			meshRoot.getTagRoot();

			ProjectRoot projectRoot = meshRoot.getProjectRoot();
			Project project = projectRoot.create("dummy", user, schemaContainer("folder").getLatestVersion());
			TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
			TagFamily tagFamily = tagFamilyRoot.create("basic", user);

			Tag tag = tagFamily.create("dummyName", project, user);
			String uuid = tag.getUuid();
			assertNotNull(tag);
			assertEquals("dummyName", tag.getName());
			tag.setName("renamed tag");
			assertEquals("renamed tag", tag.getName());

			Tag reloadedTag = boot.tagRoot().findByUuid(uuid).toBlocking().first();
			assertNotNull(reloadedTag);
			assertNotNull(reloadedTag.getFieldContainers());
			assertEquals(1, reloadedTag.getFieldContainers().size());
			assertEquals("renamed tag", reloadedTag.getName());
		}
	}
}
