package com.gentics.mesh.graphql.context.impl;

import static com.gentics.mesh.core.rest.error.Errors.missingPerm;

import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Tx;
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
	public <T extends HibCoreElement> T requiresPerm(T element, InternalPermission... permission) {
		UserDaoWrapper userDao = Tx.get().data().userDao();
		for (InternalPermission perm : permission) {
			if (userDao.hasPermission(getUser(), element, perm)) {
				return element;
			}
		}
		throw missingPerm(element.getTypeInfo().getType().name().toLowerCase(), element.getUuid());
	}

	@Override
	public boolean hasReadPerm(NodeContent content) {
		NodeGraphFieldContainer container = content.getContainer();
		if (container != null) {
			return hasReadPerm(container);
		} else {
			return true;
		}
	}

	@Override
	public boolean hasReadPerm(NodeGraphFieldContainer container) {
		ContentDaoWrapper contentDao = Tx.get().data().contentDao();
		HibNode node = contentDao.getNode(container);
		Object nodeId = node.getId();
		UserDaoWrapper userDao = Tx.get().data().userDao();

		if (userDao.hasPermissionForId(getUser(), nodeId, InternalPermission.READ_PERM)) {
			return true;
		}

		boolean isPublished = container.isPublished(getBranch().getUuid());
		if (isPublished && userDao.hasPermissionForId(getUser(), nodeId, InternalPermission.READ_PUBLISHED_PERM)) {
			return true;
		}
		return false;
	}

	@Override
	public NodeGraphFieldContainer requiresReadPermSoft(NodeGraphFieldContainer container, DataFetchingEnvironment env) {
		ContentDaoWrapper contentDao = Tx.get().data().contentDao();
		if (container == null) {
			return null;
		}
		if (hasReadPerm(container)) {
			return container;
		} else {
			PermissionException error = new PermissionException("node", contentDao.getNode(container).getUuid());
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
