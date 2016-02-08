package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class SchemaDiffVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testDiff() throws HttpStatusCodeErrorException, Exception {
		String name = "new-name";
		SchemaContainer schema = schemaContainer("content");
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName(name);
		request.setDisplayField("name");
		request.setSegmentField("filename");
		

		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("name");
		nameFieldSchema.setLabel("Name");
		nameFieldSchema.setRequired(true);
		request.addField(nameFieldSchema);

		StringFieldSchema filenameFieldSchema = new StringFieldSchemaImpl();
		filenameFieldSchema.setName("filename");
		filenameFieldSchema.setLabel("Filename");
		request.addField(filenameFieldSchema);

		StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
		titleFieldSchema.setName("title");
		titleFieldSchema.setLabel("Title");
		request.addField(titleFieldSchema);

		HtmlFieldSchema contentFieldSchema = new HtmlFieldSchemaImpl();
		contentFieldSchema.setName("content");
		contentFieldSchema.setLabel("Content");
		request.addField(contentFieldSchema);

		request.setContainer(false);

		Future<SchemaChangesListModel> future = getClient().diffSchema(schema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		SchemaChangesListModel changes = future.result();
		assertNotNull(changes);
		assertThat(changes.getChanges()).isNotEmpty();
		//		assertEquals(request.getName(), restSchema.getName());
		//		schema.reload();
		//		assertEquals("The name of the schema was not updated", name, schema.getName());
		//		SchemaContainer reloaded = boot.schemaContainerRoot().findByUuid(schema.getUuid()).toBlocking().first();
		//		assertEquals("The name should have been updated", name, reloaded.getName());

	}

}
