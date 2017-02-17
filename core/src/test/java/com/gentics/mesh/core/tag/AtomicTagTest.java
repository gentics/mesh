package com.gentics.mesh.core.tag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, useTinyDataset = true, startServer = false)
public class AtomicTagTest extends AbstractMeshTest {

	@Test
	public void testTagCreation() throws Exception {
		try (NoTx noTx = db().noTx()) {
			MeshRoot meshRoot = boot().meshRoot();
			User user = meshRoot.getUserRoot().create("test", null);
			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			assertNotNull(languageRoot);
			languageRoot.create("Deutsch", "de");
			languageRoot.create("English", "en");

			SchemaCreateRequest schema = FieldUtil.createMinimalValidSchemaCreateRequest();
			SchemaContainer schemaContainer = meshRoot.getSchemaContainerRoot().create(schema, user);

			meshRoot.getTagFamilyRoot();
			meshRoot.getTagRoot();

			ProjectRoot projectRoot = meshRoot.getProjectRoot();
			Project project = projectRoot.create("dummy", user, schemaContainer.getLatestVersion());
			TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
			TagFamily tagFamily = tagFamilyRoot.create("basic", user);

			Tag tag = tagFamily.create("dummyName", project, user);
			String uuid = tag.getUuid();
			assertNotNull(tag);
			assertEquals("dummyName", tag.getName());
			tag.setName("renamed tag");
			assertEquals("renamed tag", tag.getName());

			Tag reloadedTag = boot().tagRoot().findByUuid(uuid);
			assertNotNull(reloadedTag);
			assertEquals("renamed tag", reloadedTag.getName());
		}
	}
}
