package com.gentics.mesh.core.data.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.MeshRootService;
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
	private GroupService groupService;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private MeshRootService meshRootService;

	@Test
	public void testOGM() {

		GroupRoot groupRoot = groupService.createRoot();
		MeshRoot root = meshRootService.create();
		Tag tag = tagService.create();
		System.out.println(tag.getId());
		SchemaRoot schemaRoot = schemaService.createRoot();
		System.out.println(groupRoot.getUuid());
		System.out.println(groupRoot.getId());
		root.setGroupRoot(groupRoot);

		System.out.println(root.getGroupRoot().getId());
		System.out.println(root.getGroupRoot().getUuid());

		Schema schema = schemaService.create("test");
		schema.setDescription("description");

		schemaRoot.addSchema(schema);
		tag.setSchema(schema);
		Schema loadedSchema = tag.getSchema();
		System.out.println(loadedSchema.getDescription());

	}
}
