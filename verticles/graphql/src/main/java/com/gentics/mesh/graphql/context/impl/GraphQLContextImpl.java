package com.gentics.mesh.graphql.context.impl;

import static com.gentics.mesh.core.rest.error.Errors.missingPerm;

import java.util.Optional;

import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * @see GraphQLContext
 */
public class GraphQLContextImpl extends InternalRoutingActionContextImpl implements GraphQLContext {

	public GraphQLContextImpl(RoutingContext rc, HttpServerConfig config) {
		super(rc, config);
	}

	@Override
	public <T extends HibCoreElement<?>> T requiresPerm(T element, InternalPermission... permission) {
		UserDao userDao = Tx.get().userDao();
		for (InternalPermission perm : permission) {
			if (userDao.hasPermission(getUser(), element, perm)) {
				return element;
			}
		}
		throw missingPerm(element.getTypeInfo().getType().name().toLowerCase(), element.getUuid());
	}

	@Override
	public boolean hasReadPerm(NodeContent content, ContainerType type) {
		HibNodeFieldContainer container = content.getContainer();
		if (container != null) {
			return hasReadPerm(container, type);
		} else {
			return true;
		}
	}

	public boolean hasReadPerm(HibNodeFieldContainer container, ContainerType type) {
		Tx tx = Tx.get();
		return tx.userDao().hasReadPermission(getUser(), container, tx.getBranch(this).getUuid(), type.getHumanCode());
	}

	@Override
	public Optional<GraphQLError> requiresReadPermSoft(HibNodeFieldContainer container, DataFetchingEnvironment env, ContainerType type) {
		if (container == null || hasReadPerm(container, type)) {
			return Optional.empty();
		} else {
			ContentDao contentDao = Tx.get().contentDao();
			PermissionException error = new PermissionException("node", contentDao.getNode(container).getUuid());
			return Optional.of(new ExceptionWhileDataFetching(env.getExecutionStepInfo().getPath(), error, env.getField().getSourceLocation()));
		}
	}

	@Override
	public String branchName() {
		return Tx.get().getBranch(this).getName();
	}

	@Override
	public String branchUuid() {
		return Tx.get().getBranch(this).getUuid();
	}

	@Override
	public String projectName() {
		return Tx.get().getProject(this).getName();
	}

	@Override
	public String projectUuid() {
		return Tx.get().getProject(this).getUuid();
	}

	@Override
	public JsonObject principal() {
		return getMeshAuthUser().principal();
	}

}
