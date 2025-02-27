package com.gentics.mesh.migration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
import com.gentics.mesh.core.migration.impl.NodeMigrationImpl;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.distributed.MasterInfoProvider;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.gentics.mesh.hibernate.data.dao.BinaryDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;
import com.gentics.mesh.hibernate.data.dao.NodeDaoImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.loader.DataLoaders;
import com.gentics.mesh.metric.MetricsService;

/**
 * Extends the base node migration to prepare data to optimize the migration
 */
@Singleton
public class HibNodeMigration extends NodeMigrationImpl {

	private static final Logger log = LoggerFactory.getLogger(HibNodeMigration.class);

	@Inject
	public HibNodeMigration(Database db, BinaryUploadHandlerImpl nodeFieldAPIHandler, MetricsService metrics, Provider<EventQueueBatch> batchProvider, WriteLock writeLock, MeshOptions options, MasterInfoProvider masterInfoProvider) {
		super(db, nodeFieldAPIHandler, metrics, batchProvider, writeLock, options, masterInfoProvider);
	}

	@Override
	public void afterContextPrepared(NodeMigrationActionContextImpl context) {
		// initialize all lazy loaded fields that will be called during the migration
		context.getFromVersion().getChanges().forEach(HibSchemaChange::getRestProperties);
	}

	@Override
	public void beforeBatchMigration(List<? extends HibNodeFieldContainer> containerList, InternalActionContext ac) {
		// preload all nodes (and their edges) in the persistence context with one query
		// so that during the migration we don't need to fetch it again
		List<UUID> nodeUuids = containerList.stream()
				.map(HibNodeFieldContainerImpl.class::cast)
				.map(container -> (UUID) container.get(CommonContentColumn.NODE, () -> null))
				.collect(Collectors.toList());

		NodeDaoImpl nodeDao = HibernateTx.get().nodeDao();
		List<? extends HibNode> nodes = nodeDao.loadNodesWithEdges(nodeUuids);

		// preload binary fields from the containers
		BinaryDaoImpl binaryDao = HibernateTx.get().binaryDao();
		binaryDao.loadBinaryFields(containerList);

		// preload list field values
		ContentDaoImpl contentDao = HibernateTx.get().contentDao();
		contentDao.loadListFields(containerList);

		// wire parent loader, so that we use a single query to fetch the parent of all the nodes
		// since the parent of the node won't change during the migration, it is safe to do so
		ContentInterceptor contentInterceptor = HibernateTx.get().getContentInterceptor();
		contentInterceptor.initializeDataLoaders(nodes, ac, Collections.singletonList(DataLoaders.Loader.PARENT_LOADER));

		// increase the jdbc batch size
		Session session = HibernateTx.get().entityManager().unwrap(Session.class);
		session.setJdbcBatchSize(containerList.size());
	}

	@Override
	public void bulkPurge(List<HibNodeFieldContainer> toPurge) {
		HibernateTx tx = HibernateTx.get();
		ContentDaoImpl contentDao = tx.contentDao();
		contentDao.purge(toPurge);
	}
}
