package com.gentics.mesh.core;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.test.SpringTestConfiguration;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class OGMTest {

	@Autowired
	private BootstrapInitializer boot;

	@Test
	public void testOGM() {
		MeshRoot root = boot.createMeshRoot();
		GroupRoot groupRoot = root.createGroupRoot();
		ProjectRoot projectRoot = root.createProjectRoot();

		LanguageRoot languageRoot = root.createLanguageRoot();
		Language defaultLanguage = languageRoot.create("English", "en");

		Project project = projectRoot.create("testproject");
		TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
		TagFamily tagFamily = tagFamilyRoot.create("basic");

		Tag tag = tagFamily.create("dummyTag");

		SchemaContainerRoot schemaRoot = project.getSchemaRoot();
		System.out.println(groupRoot.getUuid());
		root.setGroupRoot(groupRoot);

		System.out.println(root.getGroupRoot().getUuid());

		SchemaContainer schema = schemaRoot.create("test");
		schemaRoot.addSchemaContainer(schema);

		schema = schemaRoot.findByName("test");
		assertNotNull(schema);

		root.getProjectRoot().addProject(project);

	}
}
