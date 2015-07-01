package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.NodeService;
import com.gentics.mesh.core.data.service.MeshRootService;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.data.service.SchemaContainerService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.test.SpringTestConfiguration;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class OGMTest {

	@Autowired
	private TagService tagService;

	@Autowired
	private SchemaContainerService schemaService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private NodeService nodeService;

	@Autowired
	private MeshRootService meshRootService;

	@Test
	public void testOGM() {
		MeshRoot root = meshRootService.create();
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
//		schema.setDescription("description");
		schemaRoot.addSchemaContainer(schema);

		schema = schemaService.findByName("test");
		assertNotNull(schema);
//		assertEquals("description", schema.getDescription());

		root.getProjectRoot().addProject(project);
		// tag.setSchema(schema);
		// Schema loadedSchema = tag.getSchema();
		// System.out.println(loadedSchema.getDescription());

	}
}
