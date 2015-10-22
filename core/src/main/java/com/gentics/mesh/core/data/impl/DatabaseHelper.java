package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.core.data.root.impl.LanguageRootImpl;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueBatchImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueEntryImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueImpl;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class that will handle index creation.
 */
public class DatabaseHelper {

	private static final Logger log = LoggerFactory.getLogger(DatabaseHelper.class);

	protected Database database;

	public DatabaseHelper(Database database) {
		this.database = database;
	}

	private <T extends GenericVertex<?>> void migrateType(RootVertex<T> rootVertex, Class<? extends T> clazzOfT) {
		try (Trx trx = database.trx()) {
			for (T vertex : rootVertex.findAll()) {
				log.info(
						"Migrating vertex type for vertex {" + vertex.getImpl().getId() + "/" + vertex.getUuid() + " to " + clazzOfT.getSimpleName());
				database.setVertexType(vertex.getElement(), clazzOfT);
			}
			trx.success();
		}
	}

	public void migrate() {
		log.info("Starting migration of vertex types");
		try (Trx trx = database.trx()) {
			for (VertexFrame vertex : trx.getGraph().v()) {
				String typeKey = vertex.getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
				try {
					Class<?> clazz = getClass().getClassLoader().loadClass(typeKey);
					database.setVertexType(vertex.getElement(), clazz);
				} catch (ClassNotFoundException e) {
					log.error("Could not find class for type key {" + typeKey + "} within classpath. Omitting migration.");
				}
			}
			trx.success();
		}

		try (NoTrx trx = database.noTrx()) {
			MeshRoot meshRoot = trx.getGraph().v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
			if (meshRoot != null) {
				migrateType(meshRoot.getLanguageRoot(), LanguageImpl.class);
				migrateType(meshRoot.getNodeRoot(), NodeImpl.class);
				migrateType(meshRoot.getTagRoot(), TagImpl.class);
				migrateType(meshRoot.getSchemaContainerRoot(), SchemaContainerImpl.class);
				migrateType(meshRoot.getProjectRoot(), ProjectImpl.class);
				migrateType(meshRoot.getTagFamilyRoot(), TagFamilyImpl.class);

				migrateType(meshRoot.getRoleRoot(), RoleImpl.class);
				migrateType(meshRoot.getGroupRoot(), GroupImpl.class);
				migrateType(meshRoot.getUserRoot(), UserImpl.class);
			}
		}
	}

	public void init() {

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

		LanguageImpl.checkIndices(database);
		GroupImpl.checkIndices(database);
		RoleImpl.checkIndices(database);
		UserImpl.checkIndices(database);
		NodeImpl.checkIndices(database);
		TagImpl.checkIndices(database);
		TagFamilyImpl.checkIndices(database);
		SchemaContainerImpl.checkIndices(database);

	}

}
