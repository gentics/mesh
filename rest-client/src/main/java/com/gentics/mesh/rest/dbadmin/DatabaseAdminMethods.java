package com.gentics.mesh.rest.dbadmin;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

public interface DatabaseAdminMethods {

	MeshRequest<EmptyResponse> stopDatabase();

	MeshRequest<EmptyResponse> startDatabase();
}
