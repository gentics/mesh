package com.gentics.mesh.rest;

import com.gentics.mesh.core.rest.node.NodeRequestParameters;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.method.AdminClientMethods;
import com.gentics.mesh.rest.method.AuthClientMethods;
import com.gentics.mesh.rest.method.GroupClientMethods;
import com.gentics.mesh.rest.method.NodeClientMethods;
import com.gentics.mesh.rest.method.ProjectClientMethods;
import com.gentics.mesh.rest.method.RoleClientMethods;
import com.gentics.mesh.rest.method.SchemaClientMethods;
import com.gentics.mesh.rest.method.SearchClientMethods;
import com.gentics.mesh.rest.method.TagClientMethods;
import com.gentics.mesh.rest.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.method.UserClientMethods;
import com.gentics.mesh.rest.method.WebRootClientMethods;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;

public interface MeshRestClient extends NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods, WebRootClientMethods,
		SchemaClientMethods, GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods, SearchClientMethods, AdminClientMethods {

	static MeshRestClient create(String host, int port, Vertx vertx) {
		return new MeshRestClientImpl(host, port, vertx);
	}

	static MeshRestClient create(String host, Vertx vertx) {
		return new MeshRestClientImpl(host, vertx);
	}

	HttpClient getClient();

	String getCookie();

	MeshRestClient setLogin(String username, String password);

	ClientSchemaStorage getClientSchemaStorage();

	String getQuery(QueryParameterProvider... parameters);

	MeshRestClient setClientSchemaStorage(ClientSchemaStorage schemaStorage);

	MeshRestClient setCookie(String cookie);

	default MeshRestClient setSessionId(String id) {
		return setCookie(MeshOptions.MESH_SESSION_KEY + "=" + id);
	}

	void close();


}
