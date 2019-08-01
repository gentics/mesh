package com.gentics.mesh.graphql.context.impl;

import static com.gentics.mesh.core.rest.error.Errors.missingPerm;

import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.graphql.context.GraphQLContext;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @see GraphQLContext
 */
public class GraphQLContextImpl extends InternalRoutingActionContextImpl implements GraphQLContext {

	public GraphQLContextImpl(RoutingContext rc) {
		super(rc);
	}

	@Override
	public <T extends MeshCoreVertex<?, ?>> T requiresPerm(T vertex, GraphPermission... permission) {
		for (GraphPermission perm : permission) {
			if (getUser().hasPermission(vertex, perm)) {
				return vertex;
			}
		}
		throw missingPerm(vertex.getTypeInfo().getType().name().toLowerCase(), vertex.getUuid());
	}

	@Override
	public String branchName() {
		return getBranch().getName();
	}

	@Override
	public String branchUuid() {
		return getBranch().getUuid();
	}

	@Override
	public String projectName() {
		return getProject().getName();
	}

	@Override
	public String projectUuid() {
		return getProject().getUuid();
	}

	@Override
	public JsonObject principal() {
		return getUser().principal();
	}

}
