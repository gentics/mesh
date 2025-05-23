package com.gentics.mesh.core.actions.impl;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.DAOActionContext;
import com.gentics.mesh.core.action.NodeDAOActions;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see NodeDAOActions
 */
@Singleton
public class NodeDAOActionsImpl implements NodeDAOActions {

	@Inject
	public NodeDAOActionsImpl() {
	}

	@Override
	public HibNode loadByUuid(DAOActionContext ctx, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		NodeDao nodeDao = ctx.tx().nodeDao();
		if (perm == null) {
			return nodeDao.findByUuid(ctx.project(), uuid);
		} else {
			return nodeDao.loadObjectByUuid(ctx.project(), ctx.ac(), uuid, perm, errorIfNotFound);
		}
	}

	@Override
	public HibNode loadByName(DAOActionContext ctx, String name, InternalPermission perm, boolean errorIfNotFound) {
		NodeDao nodeDao = ctx.tx().nodeDao();
		if (perm == null) {
			return nodeDao.findByName(ctx.project(), name);
		} else {
			throw new RuntimeException("Not supported");
		}
	}

	@Override
	public Page<? extends HibNode> loadAll(DAOActionContext ctx, PagingParameters pagingInfo) {
		NodeDao nodeDao = ctx.tx().nodeDao();
		return nodeDao.findAll(ctx.project(), ctx.ac(), pagingInfo);
	}

	@Override
	public Page<? extends HibNode> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, Predicate<HibNode> extraFilter) {
		NodeDao nodeDao = ctx.tx().nodeDao();
		return nodeDao.findAll(ctx.project(), ctx.ac(), pagingInfo, extraFilter);
	}

	@Override
	public boolean update(Tx tx, HibNode node, InternalActionContext ac, EventQueueBatch batch) {
		NodeDao nodeDao = tx.nodeDao();
		return nodeDao.update(tx.getProject(ac), node, ac, batch);
	}

	@Override
	public HibNode create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		NodeDao nodeDao = tx.nodeDao();
		HibProject project = tx.getProject(ac);
		return nodeDao.create(project, ac, batch, uuid);
	}

	@Override
	public void delete(Tx tx, HibNode node) {
		NodeDao nodeDao = tx.nodeDao();
		nodeDao.delete(node, false, true);
	}

	@Override
	public NodeResponse transformToRestSync(Tx tx, HibNode node, InternalActionContext ac, int level, String... languageTags) {
		NodeDao nodeDao = tx.nodeDao();
		return nodeDao.transformToRestSync(node, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tx tx, InternalActionContext ac, HibNode node) {
		NodeDao nodeDao = tx.nodeDao();
		return nodeDao.getAPIPath(node, ac);
	}

	@Override
	public String getETag(Tx tx, InternalActionContext ac, HibNode node) {
		NodeDao nodeDao = tx.nodeDao();
		return nodeDao.getETag(node, ac);
	}

	@Override
	public Page<? extends HibNode> loadAll(DAOActionContext ctx, PagingParameters pagingInfo, FilterOperation<?> extraFilter) {
		Stream<? extends HibNode> stream = ctx.tx().nodeDao().findAllStream(ctx.project(), ctx.ac(), InternalPermission.READ_PERM, pagingInfo, Optional.ofNullable(extraFilter));
		if (PersistingRootDao.shouldPage(pagingInfo)) {
			return new PageImpl<>(stream.collect(Collectors.toList()), pagingInfo, 
					ctx.tx().nodeDao().countAllContent(ctx.project(), ctx.ac(), ctx.ac().getNodeParameters().getLanguageList(ctx.tx().data().options()), ContainerType.DRAFT, Optional.ofNullable(extraFilter)));
		} else {
			return new DynamicStreamPageImpl<>(stream, pagingInfo, true);
		}
	}

}
