package com.gentics.mesh.graphql;

import com.gentics.mesh.core.rest.error.GenericRestException;

public class GraphQLQueryError extends GenericRestException {

	private static final long serialVersionUID = -920303920492146283L;

	protected GraphQLQueryError(String message) {
		super(message);
	}

}
