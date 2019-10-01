package com.gentics.mesh.plugin.comment.testclient;

import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.util.UUIDUtil;

public class TestClientUtil {
	public static SchemaResponse createSchemaResponse(SchemaCreateRequest request) {
		return createSchemaResponse(request.getName());
	}

	public static SchemaResponse createSchemaResponse(String schemaName) {
		SchemaResponse response = new SchemaResponse();
		response.setName(schemaName);
		response.setUuid(UUIDUtil.randomUUID());
		return response;
	}
}
