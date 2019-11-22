package com.gentics.mesh.context.impl;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.parameter.ParameterProvider;

public class LocalGraphQLContextImpl extends LocalActionContextImpl<GraphQLResponse> implements GraphQLContext {

	public LocalGraphQLContextImpl(MeshAuthUser user, ParameterProvider... parameters) {
		super(user, GraphQLResponse.class, parameters);
	}

}
