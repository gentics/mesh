package com.gentics.mesh.migration;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
import com.gentics.mesh.core.migration.impl.BranchMigrationImpl;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;

import com.gentics.mesh.hibernate.data.dao.NodeDaoImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.metric.MetricsService;
import org.hibernate.Session;

/**
 * Branch migrator implementation.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibBranchMigration extends BranchMigrationImpl {

	private final ContentStorage contentStorage;

	@Inject
	public HibBranchMigration(Database db, BinaryUploadHandlerImpl nodeFieldAPIHandler, MetricsService metrics,
							  Provider<EventQueueBatch> batchProvider, MeshOptions options, ContentStorage contentStorage, RequestDelegator delegator) {
		super(db, nodeFieldAPIHandler, metrics, batchProvider, options, delegator);
		this.contentStorage = contentStorage;
	}

	@Override
	public List<? extends Node> beforeBatchMigration(List<? extends Node> nodes) {
		NodeDaoImpl nodeDao = HibernateTx.get().nodeDao();
		// load nodes with their content edges and tags to the persistence context
		List<? extends Node> hibNodes = nodeDao.loadNodesWithEdgesAndTags(nodes.stream().map(n -> (UUID) n.getId()).collect(Collectors.toList()));

		// load all draft and published containers
		Set<HibNodeFieldContainerEdgeImpl> edges = hibNodes.stream()
				.flatMap(node -> ((HibNodeImpl) node).getContentEdges().stream())
				.filter(edge -> edge.getType().equals(ContainerType.DRAFT) || edge.getType().equals(ContainerType.PUBLISHED))
				.collect(Collectors.toSet());

		contentStorage.findMany(edges);

		// increase jdbc batch size
		Session session = HibernateTx.get().entityManager().unwrap(Session.class);
		session.setJdbcBatchSize(nodes.size());

		return hibNodes;
	}
}
