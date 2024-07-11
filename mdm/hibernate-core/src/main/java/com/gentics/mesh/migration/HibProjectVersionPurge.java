package com.gentics.mesh.migration;

import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import jakarta.persistence.EntityManager;

import com.gentics.mesh.contentoperation.ContentKey;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;
import com.gentics.mesh.hibernate.data.dao.NodeDaoImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerVersionsEdgeImpl;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.util.CollectionUtil;
import com.gentics.mesh.util.DateUtils;

import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container versions purge handler implementation.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibProjectVersionPurge implements ProjectVersionPurgeHandler {

	private static final Logger log = LoggerFactory.getLogger(HibProjectVersionPurge.class);

	private NodeDaoImpl nodeDao;
	private ContentDaoImpl contentDao;
	private ContentStorage contentStorage;
	private Database db;
	private MeshOptions options;
	private Provider<BulkActionContext> bulkProvider;

	@Inject
	public HibProjectVersionPurge(NodeDaoImpl nodeDao, ContentDaoImpl contentDao, ContentStorage contentStorage, Database db, MeshOptions options, Provider<BulkActionContext> bulkProvider) {
		this.nodeDao = nodeDao;
		this.contentDao = contentDao;
		this.contentStorage = contentStorage;
		this.db = db;
		this.options = options;
		this.bulkProvider = bulkProvider;
	}

	/**
	 * Create a purge completable.
	 */
	public Completable purgeVersions(Project project, ZonedDateTime maxAge) {
		return Completable.fromAction(() -> purgeVersionsInternal(project, maxAge));
	}

	private void purgeVersionsInternal(Project project, ZonedDateTime maxAge) {
		ArrayDeque<UUID> nodesUuid = db.tx(() -> nodeDao.findAllUuids(project).collect(Collectors.toCollection(ArrayDeque::new)));

		AtomicInteger deletedCount = new AtomicInteger();
		while (!nodesUuid.isEmpty()) {
			db.tx(() -> {
				EntityManager em = HibernateTx.get().entityManager();
				Set<Node> nodesBatch = fetchNextNodeBatch(nodesUuid);
				List<HibNodeFieldContainerImpl> currentContainers = contentDao.getFieldsContainers(nodesBatch, ContainerType.INITIAL);
				do {
					// 1. find containers key to be deleted in next loop iteration
					Set<ContentKey> nextContentKeys = SplittingUtils.splitAndMergeInSet(
							currentContainers.stream().map(NodeFieldContainer::getId).collect(Collectors.toSet()), 
							HibernateUtil.inQueriesLimitForSplitting(1), 
							slice -> em.createNamedQuery("containerversions.findNextByIds", HibNodeFieldContainerVersionsEdgeImpl.class)
								.setParameter("contentUuids", slice)
								.getResultStream()
								.map(edge -> ContentKey.fromContentUUIDAndVersion(edge.getNextContentUuid(), edge.getNextVersion()))
								.collect(Collectors.toSet()));

					// 2. delete current purgeable containers
					List<NodeFieldContainer> toPurge = currentContainers.stream()
							.filter(container -> isPurgeable(container, maxAge))
							.collect(Collectors.toList());
					contentDao.purge(toPurge, bulkProvider.get());
					deletedCount.addAndGet(toPurge.size());
					if (toPurge.size() > 0) {
						log.info("Deleted containers: " + deletedCount);
					}

					// 3. load containers to be deleted in next loop iteration
					currentContainers = contentStorage.findMany(nextContentKeys);
				} while (!currentContainers.isEmpty());
			});
		}
	}

	private Set<Node> fetchNextNodeBatch(Queue<UUID> queue) {
		int batchSize = options.getVersionPurgeMaxBatchSize();
		List<UUID> nodesUuid = CollectionUtil.pollMany(queue, batchSize);
		return new HashSet<>(nodeDao.loadNodesWithEdges(nodesUuid));
	}

	private boolean isPurgeable(NodeFieldContainer container, ZonedDateTime maxAge) {
		Long editTs = container.getLastEditedTimestamp();
		ZonedDateTime editDate = DateUtils.toZonedDateTime(editTs);
		if (maxAge != null && editDate.isAfter(maxAge)) {
			log.info("Version {" + container.getUuid() + "}@{" + container.getVersion() + "} is not purgable since it was edited {" + editDate
					+ "} which is newer than {" + maxAge + "}");
			return false;
		}

		return contentDao.isPurgeable(container);
	}
}
