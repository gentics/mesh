package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

public interface NavigationClientMethods {

	Future<NavigationResponse> loadNavigation(String projectName, String uuid, QueryParameterProvider... parameters);

}
