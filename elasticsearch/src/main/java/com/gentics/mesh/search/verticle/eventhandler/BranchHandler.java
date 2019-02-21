package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshProjectElementEventModel;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.verticle.MessageEvent;
import io.reactivex.Flowable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;
import static com.gentics.mesh.search.verticle.eventhandler.Util.toRequests;

@Singleton
public class BranchHandler implements EventHandler {

	private final NodeIndexHandler nodeIndexHandler;

	private final MeshHelper helper;

	@Inject
	public BranchHandler(NodeIndexHandler nodeIndexHandler, MeshHelper helper) {
		this.nodeIndexHandler = nodeIndexHandler;
		this.helper = helper;
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		MeshProjectElementEventModel model = requireType(MeshProjectElementEventModel.class, messageEvent.message);
		Map<String, IndexInfo> map = helper.getDb().transactional(tx -> {
			Project project = helper.getBoot().projectRoot().findByUuid(model.getProject().getUuid());
			Branch branch = project.getBranchRoot().findByUuid(model.getUuid());
			return nodeIndexHandler.getIndices(project, branch).runInExistingTx(tx);
		}).runInNewTx();

		return toRequests(map);
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(BRANCH_CREATED, BRANCH_UPDATED, BRANCH_DELETED);
	}
}
