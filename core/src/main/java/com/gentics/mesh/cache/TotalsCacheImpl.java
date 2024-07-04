package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UNTAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_BRANCH_ASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_BRANCH_UNASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_REFERENCE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_LATEST_BRANCH_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_VERSION_PURGE_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_BRANCH_ASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_BRANCH_UNASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;

import java.time.temporal.ChronoUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TotalsCacheImpl extends AbstractMeshCache<String, Long> implements TotalsCache {

	private static final Logger log = LoggerFactory.getLogger(TotalsCacheImpl.class);

	private static final long CACHE_SIZE = 10000;

	private static final MeshEvent EVENTS[] = {
			CLUSTER_NODE_JOINED,
			CLUSTER_DATABASE_CHANGE_STATUS,
			NODE_CONTENT_CREATED,
			NODE_CONTENT_DELETED,
			NODE_CREATED,
			NODE_UPDATED,
			NODE_UNPUBLISHED,
			NODE_DELETED,
			NODE_MOVED,
			NODE_PUBLISHED,
			NODE_UNPUBLISHED,
			NODE_REFERENCE_UPDATED,
			NODE_TAGGED,
			NODE_UNTAGGED,
			BRANCH_CREATED,
			BRANCH_TAGGED,
			BRANCH_UNTAGGED,
			BRANCH_UPDATED,
			BRANCH_DELETED,
			GROUP_CREATED,
			GROUP_UPDATED,
			GROUP_USER_ASSIGNED,
			GROUP_USER_UNASSIGNED,
			GROUP_ROLE_ASSIGNED,
			GROUP_ROLE_UNASSIGNED,
			GROUP_DELETED,
			MICROSCHEMA_BRANCH_ASSIGN,
			MICROSCHEMA_BRANCH_UNASSIGN,
			MICROSCHEMA_CREATED,
			MICROSCHEMA_DELETED,
			MICROSCHEMA_UPDATED,
			PROJECT_MICROSCHEMA_ASSIGNED,
			PROJECT_MICROSCHEMA_UNASSIGNED,
			PROJECT_CREATED,
			PROJECT_DELETED,
			PROJECT_UPDATED,
			PROJECT_VERSION_PURGE_FINISHED,
			PROJECT_LATEST_BRANCH_UPDATED,
			SCHEMA_BRANCH_ASSIGN,
			SCHEMA_BRANCH_UNASSIGN,
			SCHEMA_CREATED,
			SCHEMA_DELETED,
			SCHEMA_UPDATED,
			TAG_CREATED,
			TAG_DELETED,
			TAG_UPDATED,
			TAG_FAMILY_CREATED,
			TAG_FAMILY_DELETED,
			TAG_FAMILY_UPDATED,
			USER_CREATED,
			USER_DELETED,
			USER_UPDATED
		};

	@Inject
	public TotalsCacheImpl(EventAwareCacheFactory factory, CacheRegistry registry, MeshOptions options) {
		super(createCache(factory), registry, CACHE_SIZE);
	}

	private static EventAwareCache<String, Long> createCache(EventAwareCacheFactory factory) {
		return factory.<String, Long>builder()
			.events(EVENTS)
			.action((event, cache) -> {
				if (log.isDebugEnabled()) {
					log.debug("Clearing totals store due to received event from {" + event.address() + "}");
				}
				cache.invalidate();
			})
			.expireAfter(30, ChronoUnit.MINUTES)
			.maxSize(CACHE_SIZE)
			.name("totals")
			.build();
	}
}
