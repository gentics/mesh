package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.ProjectRoot;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.root.TagFamily;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.MeshRootService;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.data.service.SchemaService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.test.SpringTestConfiguration;

@ContextConfiguration(classes = { SpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class OGMTest {

	@Autowired
	private TagService tagService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private ProjectService projectService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private MeshRootService meshRootService;

	@Test
	public void testOGM() {
		MeshRoot root = meshRootService.create();
		GroupRoot groupRoot = root.createGroupRoot();
		ProjectRoot projectRoot = root.createProjectRoot();

		Project project = projectRoot.create("testproject");

		TagFamilyRoot tagFamilyRoot = project.create();
		TagFamily tagFamily = tagFamilyRoot.create("basic");

		Tag tag = tagFamily.create("dummyTag");
		System.out.println(tag.getId());
		SchemaRoot schemaRoot = schemaService.createRoot();
		System.out.println(groupRoot.getUuid());
		System.out.println(groupRoot.getId());
		root.setGroupRoot(groupRoot);

		System.out.println(root.getGroupRoot().getId());
		System.out.println(root.getGroupRoot().getUuid());

		Schema schema = schemaRoot.create("test");
		schema.setDescription("description");
		schemaRoot.addSchema(schema);

		schema = schemaService.findByName("test");
		assertNotNull(schema);
		assertEquals("description", schema.getDescription());

		root.getProjectRoot().addProject(project);
		//		tag.setSchema(schema);
		//		Schema loadedSchema = tag.getSchema();
		//		System.out.println(loadedSchema.getDescription());

	}
}
