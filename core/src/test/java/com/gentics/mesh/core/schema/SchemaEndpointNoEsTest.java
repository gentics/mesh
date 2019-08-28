package com.gentics.mesh.core.schema;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.junit.Test;

import java.util.Arrays;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.BROKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@MeshTestSetting(elasticsearch = BROKEN, testSize = FULL, startServer = true)
public class SchemaEndpointNoEsTest extends AbstractMeshTest {

	@Test
	public void testCreate() {
		SchemaCreateRequest createRequest = FieldUtil.createMinimalValidSchemaCreateRequest();

		expect(SCHEMA_CREATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName(createRequest.getName()).uuidNotNull();
		});

		SchemaResponse restSchema = call(() -> client().createSchema(createRequest));
		awaitEvents();

		try (Tx tx = tx()) {
			assertThat(createRequest).matches(restSchema);
			assertThat(restSchema.getPermissions()).hasPerm(CREATE, READ, UPDATE, DELETE);

			SchemaContainer schemaContainer = boot().schemaContainerRoot().findByUuid(restSchema.getUuid());
			assertNotNull(schemaContainer);
			assertEquals("Name does not match with the requested name", createRequest.getName(), schemaContainer.getName());
		}
	}

	@Test
	public void testUpdate() {
		SchemaCreateRequest createRequest = new SchemaCreateRequest()
			.setName("testSchema")
			.setFields(Arrays.asList(FieldUtil.createStringFieldSchema("stringField")));
		SchemaResponse created = call(() -> client().createSchema(createRequest));
		SchemaUpdateRequest updateRequest = created.toUpdateRequest()
			.setFields(Arrays.asList(FieldUtil.createBooleanFieldSchema("booleanField")));

		call(() -> client().updateSchema(created.getUuid(), updateRequest));
	}
}
