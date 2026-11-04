package com.gentics.mesh.plugin.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.Vertx;

/**
 * Test that updating an existing schema from the plugin resources keeps the
 * <code>noIndex</code> flags currently set in Mesh (SUP-20316).
 */
public class MeshPluginServiceTest {

	private static final String SCHEMA_NAME = "noindex_test";

	private static final String SCHEMA_UUID = "4b7a2d5f1c3e4e0f8a9b6c5d4e3f2a1b";

	private static Vertx vertx;

	private MeshRestClient client;

	private ArgumentCaptor<SchemaUpdateRequest> updateRequest;

	@BeforeClass
	public static void setupOnce() {
		vertx = Vertx.vertx();
	}

	@AfterClass
	public static void tearDown() {
		vertx.close();
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setup() {
		client = mock(MeshRestClient.class);
		updateRequest = ArgumentCaptor.forClass(SchemaUpdateRequest.class);

		MeshRequest<GenericMessageResponse> updateResult = mock(MeshRequest.class);
		when(updateResult.toSingle()).thenReturn(Single.just(new GenericMessageResponse("schema_update_no_difference_detected")));
		when(client.updateSchema(eq(SCHEMA_UUID), updateRequest.capture(), any(ParameterProvider[].class))).thenReturn(updateResult);
	}

	/**
	 * Flags set in Mesh, but absent or different in the resource JSON, must be
	 * kept; fields unknown to Mesh keep the flag from the resource JSON.
	 */
	@Test
	public void testUpdateKeepsNoIndexFromMesh() {
		mockExistingSchema(true, true, false);

		SchemaUpdateRequest request = ensureSchemaAndGetUpdateRequest();

		assertThat(request.getNoIndex()).as("Schema noIndex").isTrue();
		assertThat(request.getField("indexedInMesh").isNoIndex()).as("Field indexedInMesh noIndex").isTrue();
		assertThat(request.getField("notIndexedInMesh").isNoIndex()).as("Field notIndexedInMesh noIndex").isFalse();
		assertThat(request.getField("newField").isNoIndex()).as("Field newField noIndex").isTrue();
	}

	/**
	 * Schema noIndex disabled in Mesh must not be switched on by the resource
	 * JSON, and must be sent as is.
	 */
	@Test
	public void testUpdateKeepsDisabledNoIndexFromMesh() {
		mockExistingSchema(false, false, false);

		SchemaUpdateRequest request = ensureSchemaAndGetUpdateRequest();

		assertThat(request.getNoIndex()).as("Schema noIndex").isFalse();
		assertThat(request.getField("indexedInMesh").isNoIndex()).as("Field indexedInMesh noIndex").isFalse();
		assertThat(request.getField("notIndexedInMesh").isNoIndex()).as("Field notIndexedInMesh noIndex").isFalse();
		assertThat(request.getField("newField").isNoIndex()).as("Field newField noIndex").isTrue();
	}

	/**
	 * Let the client return the schema as it currently exists in Mesh.
	 *
	 * @param schemaNoIndex noIndex flag of the schema
	 * @param indexedInMeshNoIndex noIndex flag of the field <code>indexedInMesh</code>
	 * @param notIndexedInMeshNoIndex noIndex flag of the field <code>notIndexedInMesh</code>
	 */
	@SuppressWarnings("unchecked")
	private void mockExistingSchema(boolean schemaNoIndex, boolean indexedInMeshNoIndex, boolean notIndexedInMeshNoIndex) {
		SchemaResponse existing = new SchemaResponse();
		existing.setUuid(SCHEMA_UUID);
		existing.setName(SCHEMA_NAME);
		existing.setNoIndex(schemaNoIndex);
		existing.setFields(Arrays.asList(
			new StringFieldSchemaImpl().setName("indexedInMesh").setNoIndex(indexedInMeshNoIndex),
			new StringFieldSchemaImpl().setName("notIndexedInMesh").setNoIndex(notIndexedInMeshNoIndex)));

		SchemaListResponse schemas = new SchemaListResponse();
		schemas.add(existing);

		MeshRequest<SchemaListResponse> findResult = mock(MeshRequest.class);
		when(findResult.toSingle()).thenReturn(Single.just(schemas));
		when(client.findSchemas(any(ParameterProvider[].class))).thenReturn(findResult);
	}

	/**
	 * Run the schema initialization and return the update request sent to Mesh.
	 *
	 * @return The captured update request
	 */
	private SchemaUpdateRequest ensureSchemaAndGetUpdateRequest() {
		MeshPluginService service = new MeshPluginService(vertx, client, new File("."), "/noindex-schemas/");

		TestObserver<SchemaListResponse> observer = service.ensureSchemasGlobal("", Collections.singletonList(SCHEMA_NAME)).test();
		observer.awaitTerminalEvent(10, TimeUnit.SECONDS);
		observer.assertNoErrors().assertComplete();

		verify(client).updateSchema(eq(SCHEMA_UUID), any(SchemaUpdateRequest.class), any(ParameterProvider[].class));
		return updateRequest.getValue();
	}
}