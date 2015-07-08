package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;

public interface SchemaClientMethods {

	Future<SchemaResponse> createSchema(SchemaCreateRequest request);

	Future<SchemaResponse> findSchemaByUuid(String uuid);

	Future<SchemaResponse> updateSchema(String uuid, SchemaUpdateRequest request);

	Future<GenericMessageResponse> deleteSchema(String uuid);

	Future<SchemaResponse> addSchemaToProject(String schemaUuid, String projectUuid);

	Future<SchemaListResponse> findSchemas(PagingInfo pagingInfo);

	Future<SchemaResponse> removeSchemaFromProject(String schemaUuid, String projectUuid);

}
