package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toRequests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
import com.gentics.mesh.search.verticle.MessageEvent;

import io.reactivex.Flowable;

@Singleton
public class BranchEventHandler implements EventHandler {

	private final NodeIndexHandlerImpl nodeIndexHandler;

	private final MeshHelper helper;

	@Inject
	public BranchEventHandler(NodeIndexHandlerImpl nodeIndexHandler, MeshHelper helper) {
		this.nodeIndexHandler = nodeIndexHandler;
		this.helper = helper;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			MeshProjectElementEventModel model = requireType(MeshProjectElementEventModel.class, messageEvent.message);
			if (messageEvent.event == BRANCH_DELETED) {
				//NodeGraphFieldContainer.composeIndexName(model.getProject().getUuid(), model.getUuid(), "*", DRAFT);
				//NodeGraphFieldContainer.composeIndexName(model.getProject().getUuid(), model.getUuid(), "*", PUBLISH);
				//TODO Implement the drop of the node indices. We need to drop all indices of the branch.
				return Flowable.empty();
			} else {
				Map<String, IndexInfo> map = helper.getDb().transactional(tx -> {
					Project project = Tx.get().data().projectDao().findByUuid(model.getProject().getUuid());
					Branch branch = project.getBranchRoot().findByUuid(model.getUuid());
					return nodeIndexHandler.getIndices(project, branch).runInExistingTx(tx);
				}).runInNewTx();

				return toRequests(map);
			}
		});
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(BRANCH_CREATED, BRANCH_UPDATED, BRANCH_DELETED);
	}
}
