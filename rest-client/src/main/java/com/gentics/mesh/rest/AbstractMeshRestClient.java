package com.gentics.mesh.rest;

import com.gentics.mesh.rest.method.AuthClientMethods;
import com.gentics.mesh.rest.method.GroupClientMethods;
import com.gentics.mesh.rest.method.NodeClientMethods;
import com.gentics.mesh.rest.method.ProjectClientMethods;
import com.gentics.mesh.rest.method.RoleClientMethods;
import com.gentics.mesh.rest.method.TagClientMethods;
import com.gentics.mesh.rest.method.TagFamilyClientMethods;
import com.gentics.mesh.rest.method.UserClientMethods;

import io.vertx.core.http.HttpClient;

public abstract class AbstractMeshRestClient implements NodeClientMethods, TagClientMethods, ProjectClientMethods, TagFamilyClientMethods, GroupClientMethods, UserClientMethods, RoleClientMethods, AuthClientMethods {

	public static final String BASEURI = "/api/v1";
	public static final int DEFAULT_PORT = 8080;

	protected HttpClient client;



}
