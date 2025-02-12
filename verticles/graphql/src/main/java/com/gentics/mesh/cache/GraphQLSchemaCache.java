package com.gentics.mesh.cache;

import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_BRANCH_ASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_BRANCH_UNASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_BRANCH_ASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_BRANCH_UNASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.impl.EventAwareCacheFactory;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.etc.config.MeshOptions;

import graphql.schema.GraphQLSchema;

/**
 * Cache for instances of {@link GraphQLSchema}, which are used to handle GraphQL requests
 */
@Singleton
public class GraphQLSchemaCache extends AbstractMeshCache<String, GraphQLSchema> {
	/**
	 * Events, which will trigger invalidation of the cache.
	 * The {@link GraphQLSchema} instances in the cache depend on a project, a branch and the schemas/microschemas, which
	 * are assigned to the project/branch.
	 */
	private static final MeshEvent EVENTS[] = {
		PROJECT_DELETED,
		PROJECT_UPDATED,
		PROJECT_SCHEMA_ASSIGNED,
		PROJECT_SCHEMA_UNASSIGNED,
		PROJECT_MICROSCHEMA_ASSIGNED,
		PROJECT_MICROSCHEMA_UNASSIGNED,
		BRANCH_DELETED,
		BRANCH_UPDATED,
		SCHEMA_BRANCH_ASSIGN,
		SCHEMA_BRANCH_UNASSIGN,
		MICROSCHEMA_BRANCH_ASSIGN,
		MICROSCHEMA_BRANCH_UNASSIGN,
		SCHEMA_DELETED,
		SCHEMA_UPDATED,
		MICROSCHEMA_DELETED,
		MICROSCHEMA_UPDATED
	};

	/**
	 * Create the instance
	 * @param factory cache factory
	 * @param registry cache registry
	 * @param options mesh options
	 */
	@Inject
	public GraphQLSchemaCache(EventAwareCacheFactory factory, CacheRegistry registry, MeshOptions options) {
		super(createCache(factory, options.getGraphQLOptions().getSchemaCacheSize()), registry, options.getGraphQLOptions().getSchemaCacheSize());
	}

	/**
	 * Create the cache instance
	 * @param factory cache factory
	 * @return cache instance
	 */
	private static EventAwareCache<String, GraphQLSchema> createCache(EventAwareCacheFactory factory, long cacheSize) {
		return factory.<String, GraphQLSchema>builder()
			.events(EVENTS)
			.action((event, cache) -> {
				cache.invalidate();
			})
			.name("graphql_schema")
			.maxSize(cacheSize)
			.build();
	}
}
