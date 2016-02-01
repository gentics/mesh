package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.TagGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.core.data.root.impl.LanguageRootImpl;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.root.impl.MicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueBatchImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueEntryImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueImpl;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.VersionUtil;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class that will handle index creation and database migration.
 */
public class DatabaseHelper {

	private static final Logger log = LoggerFactory.getLogger(DatabaseHelper.class);

	protected Database database;

	public DatabaseHelper(Database database) {
		this.database = database;
	}

	/**
	 * Migrate all vertices of the root type.
	 * 
	 * @param rootVertex
	 * @param clazzOfT
	 */
	private <T extends MeshCoreVertex<?,T>> void migrateType(RootVertex<T> rootVertex, Class<? extends T> clazzOfT) {
		try (Trx trx = database.trx()) {
			for (T vertex : rootVertex.findAll()) {
				log.info(
						"Migrating vertex type for vertex {" + vertex.getImpl().getId() + "/" + vertex.getUuid() + " to " + clazzOfT.getSimpleName());
				database.setVertexType(vertex.getElement(), clazzOfT);
			}
			trx.success();
		}
	}

	/**
	 * Compare the stored database version with the version that is defined in the {@link MeshRootImpl} class. A database version is needed when the current
	 * database version is smaller than the implementation version.
	 * 
	 * @return
	 */
	private boolean isMigrationNeeded() {
		try (NoTrx tx = database.noTrx()) {
			MeshRoot meshRoot = tx.getGraph().v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
			if (meshRoot == null) {
				return false;
			}
			String dbVersion = meshRoot.getDatabaseVersion();
			log.info("Mesh Database version {" + MeshRootImpl.DATABASE_VERSION + "}");
			log.info("Current Database version {" + dbVersion + "}");
			if (StringUtils.isEmpty(dbVersion) || VersionUtil.compareVersions(MeshRootImpl.DATABASE_VERSION, dbVersion) > 0) {
				log.info("Database migration needed");
				return true;
			}
		}
		log.info("Skipping Database migration");
		return false;
	}

	/**
	 * Check if a migration is needed and invoke the database migration.
	 */
	public void migrate() {
		log.info("Starting migration of vertex types");
		if (!isMigrationNeeded()) {
			return;
		}

		try (Trx tx = database.trx()) {
			MeshRoot meshRoot = tx.getGraph().v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);

			// Add shortcut edges from role to users of this group
			for (User user : meshRoot.getUserRoot().findAll()) {
				for (Role role : user.getRoles()) {
					user.getImpl().setLinkOutTo(role.getImpl(), ASSIGNED_TO_ROLE);
				}
			}
			tx.success();
		}

		// 1. Add types
		try (NoTrx tx = database.noTrx()) {
			for (Vertex vertex : tx.getGraph().getVertices()) {
				String typeKey = vertex.getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
				try {
					Class<?> clazz = getClass().getClassLoader().loadClass(typeKey);
					database.addVertexType(clazz);
				} catch (ClassNotFoundException e) {
					log.error("Could not find class for type key {" + typeKey + "} within classpath. Omitting migration.");
				}
			}
		}

		// 2. Assign types
		try (Trx tx = database.trx()) {
			for (Vertex vertex : tx.getGraph().getVertices()) {
				String typeKey = vertex.getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
				try {
					Class<?> clazz = getClass().getClassLoader().loadClass(typeKey);
					database.setVertexType(vertex, clazz);
					tx.success();
				} catch (ClassNotFoundException e) {
					log.error("Could not find class for type key {" + typeKey + "} within classpath. Omitting migration.");
					tx.failure();
				}
			}
		}

		//		try (NoTrx trx = database.noTrx()) {
		//			MeshRoot meshRoot = trx.getGraph().v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
		//			if (meshRoot != null) {
		//				migrateType(meshRoot.getLanguageRoot(), LanguageImpl.class);
		//				migrateType(meshRoot.getNodeRoot(), NodeImpl.class);
		//				migrateType(meshRoot.getTagRoot(), TagImpl.class);
		//				migrateType(meshRoot.getSchemaContainerRoot(), SchemaContainerImpl.class);
		//				migrateType(meshRoot.getProjectRoot(), ProjectImpl.class);
		//				migrateType(meshRoot.getTagFamilyRoot(), TagFamilyImpl.class);
		//
		//				migrateType(meshRoot.getRoleRoot(), RoleImpl.class);
		//				migrateType(meshRoot.getGroupRoot(), GroupImpl.class);
		//				migrateType(meshRoot.getUserRoot(), UserImpl.class);
		//			}
		//		}

		try (NoTrx trx = database.noTrx()) {
			MeshRoot meshRoot = trx.getGraph().v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
			meshRoot.setDatabaseVersion(MeshRootImpl.DATABASE_VERSION);
		}
	}

	/**
	 * Initialize the database indices and types.
	 */
	public void init() {

		log.info("Creating database indices. This may take a few seconds...");
		database.addVertexType(MeshRootImpl.class);

		// Edges
		GraphRelationships.checkIndices(database);
		GraphPermission.checkIndices(database);

		// Aggregation nodes
		MeshRootImpl.checkIndices(database);
		GroupRootImpl.checkIndices(database);
		UserRootImpl.checkIndices(database);
		RoleRootImpl.checkIndices(database);
		TagRootImpl.checkIndices(database);
		NodeRootImpl.checkIndices(database);
		TagFamilyRootImpl.checkIndices(database);
		LanguageRootImpl.checkIndices(database);
		ProjectRootImpl.checkIndices(database);
		SchemaContainerRootImpl.checkIndices(database);
		MicroschemaContainerRootImpl.checkIndices(database);

		// Nodes
		SearchQueueImpl.checkIndices(database);
		SearchQueueBatchImpl.checkIndices(database);
		SearchQueueEntryImpl.checkIndices(database);
		ProjectImpl.checkIndices(database);

		// Fields
		NodeGraphFieldContainerImpl.checkIndices(database);
		StringGraphFieldListImpl.checkIndices(database);
		NodeGraphFieldListImpl.checkIndices(database);
		TagGraphFieldContainerImpl.checkIndices(database);
		MicronodeGraphFieldListImpl.checkIndices(database);

		LanguageImpl.checkIndices(database);
		GroupImpl.checkIndices(database);
		RoleImpl.checkIndices(database);
		UserImpl.checkIndices(database);
		NodeImpl.checkIndices(database);
		MicronodeImpl.checkIndices(database);
		TagImpl.checkIndices(database);
		TagFamilyImpl.checkIndices(database);
		SchemaContainerImpl.checkIndices(database);
		MicroschemaContainerImpl.checkIndices(database);

	}

}
