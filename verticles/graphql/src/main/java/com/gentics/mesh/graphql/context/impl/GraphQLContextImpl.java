package com.gentics.mesh.graphql.context.impl;

import static com.gentics.mesh.core.rest.error.Errors.missingPerm;

import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.ExceptionWhileDataFetching;
import graphql.schema.DataFetchingEnvironment;
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
	public boolean hasReadPerm(NodeContent content, ContainerType type) {
		NodeGraphFieldContainer container = content.getContainer();
		if (container != null) {
			return hasReadPerm(container, type);
		} else {
			return true;
		}
	}

	@Override
	public boolean hasReadPerm(NodeGraphFieldContainer container, ContainerType type) {
		return getUser().hasReadPermission(container, getBranch().getUuid(), type.getHumanCode());
	}

	@Override
	public NodeGraphFieldContainer requiresReadPermSoft(NodeGraphFieldContainer container, DataFetchingEnvironment env, ContainerType type) {
		if (container == null) {
			return null;
		}
		if (hasReadPerm(container, type)) {
			return container;
		} else {
			PermissionException error = new PermissionException("node", container.getParentNode().getUuid());
			env.getExecutionContext()
				.addError(new ExceptionWhileDataFetching(env.getFieldTypeInfo().getPath(), error, env.getField().getSourceLocation()));
		}

		return null;
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
