package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.root.MeshRoot;
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
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;

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

	public void migrate() {

		log.info("Starting migration of vertex types");
		try (Trx trx = database.trx()) {
			MeshRoot meshRoot = trx.getGraph().v().has(MeshRootImpl.class).nextOrDefault(MeshRootImpl.class, null);
			if (meshRoot != null) {
				for (Language language : meshRoot.getLanguageRoot().findAll()) {
					log.info("Migrating vertex type for vertex {" + language.getImpl().getId() + "/" + language.getUuid() + " to "
							+ LanguageImpl.class.getSimpleName());
					database.setVertexType(language.getElement(), LanguageImpl.class);
				}
				trx.success();
			}
		}

		//		for (Node node : MeshRoot.getInstance().getNodeRoot().findAll()) {
		//			database.setVertexType(node.getElement(), NodeImpl.class);
		//		}
		//		for (Tag tag : MeshRoot.getInstance().getTagRoot().findAll()) {
		//			database.setVertexType(tag.getElement(), TagImpl.class);
		//		}
		//		for (Project project : MeshRoot.getInstance().getProjectRoot().findAll()) {
		//			database.setVertexType(project.getElement(), ProjectImpl.class);
		//		}
		//		for (SchemaContainer schemaContainer : MeshRoot.getInstance().getSchemaContainerRoot().findAll()) {
		//			database.setVertexType(schemaContainer.getElement(), SchemaContainerImpl.class);
		//		}
		//		for (TagFamily tagFamily : MeshRoot.getInstance().getTagFamilyRoot().findAll()) {
		//			database.setVertexType(tagFamily.getElement(), TagFamilyImpl.class);
		//		}
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
